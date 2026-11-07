"""Run on the Docker host after startup. Mutates only the selected Safillu installation.

Usage: python docker/smoke-test.py [env-file] [creator-url] [admin-url]
Never prints passwords/tokens; writes a sanitized result and private resume state.
"""
import base64
import hashlib
import http.cookiejar
import json
import os
from pathlib import Path
import secrets
import subprocess
import sys
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = sys.argv[1] if len(sys.argv) > 1 else '.env.local'
WEB = sys.argv[2] if len(sys.argv) > 2 else 'http://localhost:8088'
ADMIN = sys.argv[3] if len(sys.argv) > 3 else 'http://localhost:8089'
env = dict(line.split('=', 1) for line in (ROOT / ENV_FILE).read_text().splitlines() if line and not line.startswith('#') and '=' in line)
runtime = ROOT / env.get('SAFILLU_RUNTIME_DIR', '.runtime')
accounts = json.loads((runtime / 'secrets/initial-accounts.json').read_text())
checks = []


def check(name, condition):
    checks.append({'name': name, 'passed': bool(condition)})
    if not condition:
        raise AssertionError(name)


def request(base, path, method='GET', body=None, token=None, content_type=None):
    headers = {'tenant-id': '1'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    if body is not None and not isinstance(body, bytes):
        body = json.dumps(body).encode()
        headers['Content-Type'] = 'application/json'
    if content_type:
        headers['Content-Type'] = content_type
    try:
        with urllib.request.urlopen(urllib.request.Request(base + path, data=body, headers=headers, method=method), timeout=30) as response:
            return response.status, response.read(), dict(response.headers)
    except urllib.error.HTTPError as error:
        return error.code, error.read(), dict(error.headers)


def api(base, path, method='GET', body=None, token=None):
    status, raw, _ = request(base, path, method, body, token)
    try:
        result = json.loads(raw)
    except Exception:
        raise AssertionError(f'Non-JSON response at {path}: HTTP {status}') from None
    if status != 200 or result.get('code') != 0:
        raise AssertionError(f'API failure at {path}: HTTP {status}, code {result.get("code")}')
    return result.get('data')


def login(account, base):
    return api(base, '/admin-api/system/auth/login', 'POST', account)['accessToken']


def sql(query):
    # Password stays inside the MySQL container, never in argv or report output.
    command = ['docker', 'compose', '--env-file', ENV_FILE, 'exec', '-T', 'mysql', 'sh', '-ec',
               'MYSQL_PWD=$(cat /run/secrets/mysql_root) exec mysql -uroot --batch --skip-column-names ' + env.get('DB_NAME', 'safillu')]
    result = subprocess.run(command, cwd=ROOT, input=query, text=True, capture_output=True, check=True)
    return result.stdout.strip()


def run():
    for name, base in [('creator', WEB), ('admin', ADMIN)]:
        status, raw, _ = request(base, '/')
        check(name + ' HTML loads', status == 200 and b'Safillu' in raw)
    check('Google unavailable without credentials', api(WEB, '/admin-api/aigc/auth/google/config')['enabled'] is False)
    admin = login(accounts['admin'], ADMIN)
    operator = login(accounts['operator'], ADMIN)
    permissions = api(ADMIN, '/admin-api/system/auth/get-permission-info', token=admin)
    check('Native admin roles and menus load', 'super_admin' in permissions['roles'] and len(permissions['menus']) >= 3)
    check('Admin native user management', api(ADMIN, '/admin-api/system/user/page?pageNo=1&pageSize=10', token=admin)['total'] >= 2)
    ops_permissions = api(ADMIN, '/admin-api/system/auth/get-permission-info', token=operator)
    check('Operator role isolated', 'aigc_content_operator' in ops_permissions['roles'] and 'super_admin' not in ops_permissions['roles'])
    api(ADMIN, '/admin-api/aigc/explore/manage', token=operator)
    check('Operator reads content management', True)
    status, raw, _ = request(ADMIN, '/admin-api/system/user/page?pageNo=1&pageSize=10', token=operator)
    check('Operator cannot manage users', status == 403 or json.loads(raw).get('code') == 403)
    check('Platform tenant has all native modules', sql('SELECT package_id FROM system_tenant WHERE id=1;') == '0')
    check('Native menu seed complete', int(sql('SELECT COUNT(*) FROM system_menu WHERE deleted=0;')) > 100)
    check('Fresh local storage is master; R2 not activated', sql('SELECT id FROM infra_file_config WHERE master=1 AND deleted=0;') == '1')
    check('Empty model key disabled', sql('SELECT status FROM ai_api_key WHERE id=870001;') == '1')
    state_file = runtime / 'docker-smoke-state.json'
    if state_file.exists():
        state = json.loads(state_file.read_text())
        user = login(state['account'], WEB)
        check('User login survives restart', True)
        snapshot = api(WEB, '/admin-api/aigc/bootstrap', token=user)
        check('Conversation survives restart', any(x['id'] == state['conversation'] for x in snapshot['sessions']))
        status, raw, _ = request(WEB, f'/admin-api/aigc/assets/{state["asset"]}/content', token=user)
        check('Uploaded file survives restart', status == 200 and hashlib.sha256(raw).hexdigest() == state['sha256'])
        check('Credentials unchanged', state['secretsDigest'] == secret_digest())
    else:
        account = {'username': 'dockerqa' + str(int(time.time())), 'password': secrets.token_urlsafe(12), 'nickname': 'Docker QA'}
        api(ADMIN, '/admin-api/system/user/create', 'POST', account, admin)
        user = login(account, WEB)
        check('Native administrator creates user; password login succeeds', True)
        snapshot = api(WEB, '/admin-api/aigc/bootstrap', token=user)
        check('No fake available models', snapshot['capabilities'] == [])
        check('New user has no existing assets/tasks', snapshot['records'] == [] and snapshot['tasks'] == [])
        conversation = str(api(WEB, '/admin-api/aigc/conversations', 'POST', {'title': 'Safillu Docker persistence test'}, user))
        check('Create conversation using real API', conversation.isdigit())
        original = base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jx1sAAAAASUVORK5CYII=')
        boundary = 'SafilluBoundary' + secrets.token_hex(8)
        payload = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="safillu-smoke.png"\r\nContent-Type: image/png\r\n\r\n').encode() + original + f'\r\n--{boundary}--\r\n'.encode()
        status, raw, _ = request(WEB, '/admin-api/aigc/assets/upload', 'POST', payload, user, 'multipart/form-data; boundary=' + boundary)
        result = json.loads(raw)
        check('Real upload creates asset', status == 200 and result.get('code') == 0)
        asset = str(result['data'])
        status, raw, _ = request(WEB, f'/admin-api/aigc/assets/{asset}/content', token=user)
        check('Authenticated content matches original', status == 200 and raw == original)
        status, raw, _ = request(WEB, f'/admin-api/aigc/assets/{asset}/content')
        check('Anonymous user cannot read private upload', status in (401, 403) or (raw.startswith(b'{') and json.loads(raw).get('code') in (401, 403)))
        state = {'account': account, 'conversation': conversation, 'asset': asset,
                 'sha256': hashlib.sha256(original).hexdigest(), 'secretsDigest': secret_digest()}
        fd = os.open(state_file, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, 'w') as file:
            json.dump(state, file)
    check('No host ports for MySQL/Redis/backend', all(not json.loads(subprocess.check_output(['docker', 'inspect', '--format', '{{json .HostConfig.PortBindings}}', env.get('COMPOSE_PROJECT_NAME', 'safillu') + '-' + service + '-1']).decode()) for service in ['mysql', 'redis', 'backend']))
    print(json.dumps({'passed': len(checks), 'checks': checks}, ensure_ascii=False, indent=2))


def secret_digest():
    value = b''.join((runtime / 'secrets' / name).read_bytes() for name in ['mysql_root', 'db_app', 'redis_password', 'data_key', 'initial-accounts.json', 'storage/content_key'])
    return hashlib.sha256(value).hexdigest()


if __name__ == '__main__':
    try:
        run()
    except Exception as error:
        print(json.dumps({'passed': sum(c['passed'] for c in checks), 'checks': checks,
                          'failure': str(error) if isinstance(error, AssertionError) else type(error).__name__}, ensure_ascii=False, indent=2))
        sys.exit(1)
