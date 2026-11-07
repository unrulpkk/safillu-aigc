"""Fresh, isolated Safillu installation. Never import another project's users/secrets."""
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import sys
from urllib.parse import urlparse

SECRET_ROOT = Path(os.environ.get('SECRET_ROOT', '/run/secrets'))
SQL_ROOT = Path(os.environ.get('SQL_ROOT', '/bootstrap/sql'))
OLD_SCHEMA = 'zaomeng_aigc_20261103'


# The first public release removes a personal bucket/domain from a historical seed.
# A completed, exact legacy migration is acknowledged without replaying its SQL or
# changing the installed storage configuration. All other checksum changes fail.
SANITIZED_LEGACY_MIGRATIONS = {
    'V006__qiniu_file_storage.sql': ('21b56436cc2818b25f70725e0b2b8af8ff3ad92d4bdd70913319f23e01122eeb', 'd8d7ae7907d83f558522d189656a4f1ef132fecce969b84f31eb4df4e3d94bfe'),
}


def accepts_sanitized_legacy(name, previous, current, done):
    return bool(done) and SANITIZED_LEGACY_MIGRATIONS.get(name) == (previous, current)


def protect(path, private=False):
    if os.name != 'nt':
        os.chown(path, 0, 10001)
        os.chmod(path, 0o750 if path.is_dir() else (0o600 if private else 0o640))


def create_secret(name, value, private=False):
    path = SECRET_ROOT / name
    path.parent.mkdir(parents=True, exist_ok=True)
    # O_EXCL protects credentials when two launchers run concurrently.
    try:
        fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    except FileExistsError:
        pass
    else:
        with os.fdopen(fd, 'w', encoding='utf-8') as stream:
            stream.write(value + '\n')
    protect(path, private)


def init_secrets():
    SECRET_ROOT.mkdir(parents=True, exist_ok=True)
    protect(SECRET_ROOT)
    for name in ('google', 'stripe', 'storage', 'storage/r2'):
        path = SECRET_ROOT / name
        path.mkdir(parents=True, exist_ok=True)
        protect(path)
    for name in ('mysql_root', 'db_app', 'redis_password', 'data_key'):
        create_secret(name, secrets.token_hex(16), private=name == 'mysql_root')
    create_secret('storage/content_key', base64.b64encode(secrets.token_bytes(32)).decode())
    create_secret('provider_key', '')
    accounts = {
        'admin': {'username': 'admin', 'password': secrets.token_urlsafe(12)},
        'operator': {'username': 'contentops', 'password': secrets.token_urlsafe(12)},
        'note': 'Initial passwords only. Changing a password in Safillu does not update this file.'
    }
    create_secret('initial-accounts.json', json.dumps(accounts, ensure_ascii=False, indent=2), True)
    # Incoming provider credentials are mounted to the unprivileged Java process.
    for path in SECRET_ROOT.rglob('*'):
        if path.is_file():
            protect(path, path.name in ('initial-accounts.json', 'mysql_root'))
    print('Secrets ready; existing credentials preserved. Initial accounts: /run/secrets/initial-accounts.json')


def sql_statements(text):
    """Split our MySQL DDL/DML scripts; handle quoted semicolons and comments."""
    buf, quote, index = [], None, 0
    while index < len(text):
        char = text[index]
        if quote:
            buf.append(char)
            if char == '\\' and quote != '`' and index + 1 < len(text):
                index += 1
                buf.append(text[index])
            elif char == quote:
                if index + 1 < len(text) and text[index + 1] == quote:
                    index += 1
                    buf.append(text[index])
                else:
                    quote = None
        elif char in "'\"`":
            quote = char
            buf.append(char)
        elif text.startswith('/*', index):
            end = text.find('*/', index + 2)
            if end < 0:
                raise RuntimeError('Unterminated SQL comment')
            index = end + 1
            buf.append(' ')
        elif char == '#' or (text.startswith('--', index) and (index + 2 == len(text) or text[index + 2].isspace())):
            end = text.find('\n', index)
            index = len(text) if end < 0 else end
            buf.append('\n')
        elif char == ';':
            statement = ''.join(buf).strip()
            if statement:
                yield statement
            buf = []
        else:
            buf.append(char)
        index += 1
    if quote:
        raise RuntimeError('Unterminated SQL string')
    if ''.join(buf).strip():
        yield ''.join(buf).strip()


def identifier(value):
    if not re.fullmatch(r'[A-Za-z][A-Za-z0-9_]{0,47}', value):
        raise RuntimeError('Database and username must be SQL identifiers, 1-48 characters')
    return value


def migrate(db, files, schema):
    with db.cursor() as cur:
        cur.execute('SHOW TABLES')
        tables = {row[0] for row in cur.fetchall()}
        if tables and 'safillu_schema_history' not in tables:
            raise RuntimeError('Refusing an existing database without Safillu installation history; use a NEW DB_NAME')
        cur.execute('CREATE TABLE IF NOT EXISTS safillu_schema_history ('
                    'name VARCHAR(150) PRIMARY KEY, checksum CHAR(64) NOT NULL, '
                    'completed INT NOT NULL DEFAULT 0, done BOOLEAN NOT NULL DEFAULT FALSE, '
                    'updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)')
        db.commit()
        for file in files:
            text = file.read_text(encoding='utf-8-sig').replace('\r\n', '\n')
            checksum = hashlib.sha256(text.encode()).hexdigest()
            cur.execute('SELECT checksum,completed,done FROM safillu_schema_history WHERE name=%s', (file.name,))
            previous = cur.fetchone()
            if previous and previous[0] != checksum:
                if not accepts_sanitized_legacy(file.name, previous[0], checksum, previous[2]):
                    raise RuntimeError('Applied migration checksum changed: ' + file.name)
                cur.execute('UPDATE safillu_schema_history SET checksum=%s WHERE name=%s AND checksum=%s AND done=TRUE',
                            (checksum, file.name, previous[0]))
                db.commit()
            if previous and previous[2]:
                continue
            completed = previous[1] if previous else 0
            cur.execute('INSERT IGNORE INTO safillu_schema_history(name,checksum) VALUES(%s,%s)', (file.name, checksum))
            db.commit()
            for step, statement in enumerate(sql_statements(text.replace(OLD_SCHEMA, schema)), 1):
                if step <= completed:
                    continue
                upper = statement.upper()
                # Each DML and its checkpoint commit together. DDL can safely resume.
                if not re.match(r'^(USE\s|START TRANSACTION$|COMMIT$)', upper):
                    statement = re.sub(r'^CREATE TABLE\s+(?!IF NOT EXISTS\b)', 'CREATE TABLE IF NOT EXISTS ', statement, flags=re.I)
                    added = re.findall(r'\bADD COLUMN\s+`?(\w+)`?', statement, re.I)
                    table = re.match(r'ALTER TABLE\s+`?(\w+)`?', statement, re.I)
                    already = False
                    if table and added:
                        cur.execute('SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s', (schema, table[1]))
                        already = set(added) <= {row[0] for row in cur.fetchall()}
                    if not already:
                        try:
                            cur.execute(statement)
                        except Exception as exc:
                            db.rollback()
                            raise RuntimeError(f'Migration failed: {file.name}, statement {step}, DB error {exc.args[0]}') from None
                cur.execute('UPDATE safillu_schema_history SET completed=%s WHERE name=%s', (step, file.name))
                db.commit()
            cur.execute('UPDATE safillu_schema_history SET done=TRUE WHERE name=%s', (file.name,))
            db.commit()
            print('Applied ' + file.name)


def initial_accounts(db):
    import bcrypt
    with db.cursor() as cur:
        cur.execute("SELECT done FROM safillu_schema_history WHERE name='__initial_accounts_v1'")
        if cur.fetchone():
            return
        accounts = json.loads((SECRET_ROOT / 'initial-accounts.json').read_text())
        for key, role in [('admin', 'super_admin'), ('operator', 'aigc_content_operator')]:
            account = accounts[key]
            cur.execute('SELECT id FROM system_users WHERE tenant_id=1 AND username=%s AND deleted=0', (account['username'],))
            if cur.fetchone():
                raise RuntimeError('Refusing to overwrite an existing initial account')
            password_hash = bcrypt.hashpw(account['password'].encode(), bcrypt.gensalt(rounds=10)).decode()
            cur.execute('INSERT INTO system_users(username,password,nickname,status,tenant_id,creator) VALUES(%s,%s,%s,0,1,%s)',
                        (account['username'], password_hash, 'Safillu Admin' if key == 'admin' else 'Safillu Content', 'safillu-installer'))
            user = cur.lastrowid
            cur.execute('SELECT id FROM system_role WHERE code=%s AND tenant_id=1 AND deleted=0', (role,))
            role_id = cur.fetchone()[0]
            cur.execute('INSERT INTO system_user_role(user_id,role_id,tenant_id) VALUES(%s,%s,1)', (user, role_id))
        cur.execute("UPDATE system_tenant SET name='Safillu',contact_name='Safillu' WHERE id=1")
        cur.execute("UPDATE system_oauth2_client SET name='Safillu Web' WHERE id=1")
        cur.execute("UPDATE infra_file_config SET name='Safillu local persistent storage' WHERE id=1")
        # A fresh deployment must not advertise unavailable/empty provider credentials.
        cur.execute('UPDATE ai_api_key SET status=1 WHERE id=870001')
        cur.execute("INSERT INTO safillu_schema_history(name,checksum,completed,done) VALUES('__initial_accounts_v1',%s,1,TRUE)", ('0' * 64,))
        db.commit()
    print('Native administrator, content operator and full platform menus initialized.')


def configure_provider(db):
    configured = bool((SECRET_ROOT / 'provider_key').read_text().strip())
    with db.cursor() as cur:
        cur.execute('UPDATE ai_api_key SET status=%s WHERE id=870001', (0 if configured else 1,))
    db.commit()
    print('Model provider ' + ('enabled.' if configured else 'disabled: no provider key installed.'))


def configure_r2(db):
    config_path = SECRET_ROOT / 'storage/r2/config.json'
    if not config_path.is_file():
        raise RuntimeError('Install storage/r2/config.json from the example first; Qiniu/local master is unchanged')
    config = json.loads(config_path.read_text())
    if not re.fullmatch(r'https://[a-f0-9]{32}(?:\.(?:eu|fedramp))?\.r2\.cloudflarestorage\.com', config.get('endpoint', '')):
        raise RuntimeError('A real Cloudflare R2 account endpoint is required; mock configuration is test-only')
    if not config.get('bucket') or config['bucket'].startswith('YOUR_'):
        raise RuntimeError('Configure an R2 bucket first')
    domain = config.get('domain', '').rstrip('/')
    parsed = urlparse(domain)
    if parsed.scheme != 'https' or not parsed.hostname or parsed.username or parsed.query or parsed.fragment or parsed.path or parsed.hostname.endswith('.r2.cloudflarestorage.com'):
        raise RuntimeError('Configure the public R2 HTTPS domain first')
    for key, ref in [('accessKey', 'access_key'), ('accessSecret', 'access_secret')]:
        if config.get(key) != f'file:/run/secrets/storage/r2/{ref}' or not (SECRET_ROOT / f'storage/r2/{ref}').read_text().strip():
            raise RuntimeError('R2 credentials must use private storage/r2 secret files')
    config.update(domain=domain, region='auto', enablePathStyleAccess=True, enablePublicAccess=True,
                  encryptContent=False)
    config.pop('encryptionKeyReference', None)
    with db.cursor() as cur:
        cur.execute('SELECT master,storage,config FROM infra_file_config WHERE id=880004')
        current = cur.fetchone()
        if current:
            previous = json.loads(current[2]) if isinstance(current[2], str) else current[2]
            if current[1] != 20 or any(previous.get(k) != config.get(k) for k in ('endpoint','bucket','domain','encryptContent','enablePublicAccess')):
                raise RuntimeError('Existing R2 configuration ID cannot be repointed or change encryption mode')
            print('Compatible R2 configuration already exists; no storage settings changed.')
            return
        cur.execute('INSERT INTO infra_file_config(id,name,storage,master,config) VALUES(880004,%s,20,0,%s) '
                    'ON DUPLICATE KEY UPDATE config=VALUES(config),deleted=0', ('Cloudflare R2 (pending roundtrip)', json.dumps(config)))
    db.commit()
    print('R2 installed as non-master. Enable R2_STORAGE_BOOTSTRAP for a verified switch; no master changed.')


def init_database(command):
    import pymysql
    schema = identifier(os.environ.get('DB_NAME', 'safillu'))
    username = identifier(os.environ.get('DB_USERNAME', 'safillu_app'))
    if username.lower() == 'root':
        raise RuntimeError('Application database username cannot be root')
    if len(username) > 32:
        raise RuntimeError('MySQL username cannot exceed 32 characters')
    db = pymysql.connect(host=os.environ.get('DB_HOST', 'mysql'), user='root',
                         password=(SECRET_ROOT / 'mysql_root').read_text().strip(), charset='utf8mb4')
    try:
        with db.cursor() as cur:
            cur.execute('SELECT GET_LOCK(%s,60)', ('safillu-install-' + schema,))
            if cur.fetchone()[0] != 1:
                raise RuntimeError('Another installer holds the database lock')
            cur.execute(f'CREATE DATABASE IF NOT EXISTS `{schema}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci')
            cur.execute(f'USE `{schema}`')
        if command == 'database':
            files = [SQL_ROOT / name for name in (
                '01_framework_schema.sql', 'V001__aigc_business_schema.sql',
                'V002__asset_edit_draft.sql', '02_framework_bootstrap.sql')]
            files += sorted(path for path in SQL_ROOT.glob('V*.sql')
                            if path.name not in ('V001__aigc_business_schema.sql', 'V002__asset_edit_draft.sql'))
            files += sorted((SQL_ROOT / 'migrations').glob('V*.sql'))
            migrate(db, files, schema)
            initial_accounts(db)
            with db.cursor() as cur:
                cur.execute(f'CREATE USER IF NOT EXISTS %s@\'%%\' IDENTIFIED BY %s', (username, (SECRET_ROOT / 'db_app').read_text().strip()))
                cur.execute(f'GRANT SELECT,INSERT,UPDATE,DELETE ON `{schema}`.* TO %s@\'%%\'', (username,))
            db.commit()
            print(f'Database {schema} ready. Existing data and passwords preserved.')
        elif command == 'provider':
            configure_provider(db)
        elif command == 'r2':
            configure_r2(db)
    finally:
        db.close()


if __name__ == '__main__':
    try:
        command = sys.argv[1] if len(sys.argv) > 1 else 'database'
        if command == 'secrets':
            init_secrets()
        elif command in ('database', 'provider', 'r2'):
            init_database(command)
        else:
            raise RuntimeError('Unknown bootstrap command')
    except Exception as exc:
        # Do not dump driver repr/queries, provider credentials or connection strings.
        print(str(exc) if isinstance(exc, RuntimeError) else f'Bootstrap failed: {type(exc).__name__}', file=sys.stderr)
        sys.exit(1)
