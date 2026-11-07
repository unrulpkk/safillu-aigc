"""Installer refusal tests. Only permitted on a Compose project named *smoke* or *qa*."""
import json
from pathlib import Path
import re
import runpy
import subprocess
import sys

helpers = runpy.run_path(str(Path(__file__).with_name('smoke-test.py')), run_name='helpers')
env, root, env_file, sql = (helpers[k] for k in ['env', 'ROOT', 'ENV_FILE', 'sql'])
project = env.get('COMPOSE_PROJECT_NAME', '')
if not any(word in project for word in ['smoke', 'qa']):
    raise SystemExit('Refusing guard tests outside an explicitly named smoke/qa installation')
compose = ['docker', 'compose', '--env-file', env_file]
results = []


def guard(name, command, expected):
    result = subprocess.run(command, cwd=root, capture_output=True, text=True)
    passed = result.returncode != 0 and expected in (result.stdout + result.stderr)
    results.append({'name': name, 'passed': passed})
    if not passed:
        raise AssertionError(name)


try:
    guard('Applied migration checksum mismatch is refused', compose + [
        'run', '--rm', '--no-deps', '--entrypoint', 'sh', 'db-init', '-ec',
        "printf '\n-- fault injection\n' >> /bootstrap/sql/01_framework_schema.sql; python /bootstrap/bootstrap.py database"
    ], 'Applied migration checksum changed')
    schema = env.get('DB_NAME', 'safillu') + '_guard'
    if not re.fullmatch(r'[A-Za-z][A-Za-z0-9_]{0,63}', schema):
        raise AssertionError('Invalid guard schema name')
    if sql(f"SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='{schema}';") != '0':
        raise AssertionError('Guard schema already exists; refusing to overwrite it')
    sql(f'CREATE DATABASE `{schema}`; CREATE TABLE `{schema}`.sentinel(id INT PRIMARY KEY); INSERT INTO `{schema}`.sentinel VALUES(7);')
    try:
        guard('An unrelated existing database is refused', compose + [
            'run', '--rm', '--no-deps', '-e', 'DB_NAME=' + schema, 'db-init', 'database'
        ], 'Refusing an existing database')
        passed = sql(f'SELECT id FROM `{schema}`.sentinel;') == '7'
        results.append({'name': 'Existing database sentinel stays unchanged', 'passed': passed})
        if not passed:
            raise AssertionError('Existing database was modified')
    finally:
        # This exact schema was created above by this test, after checking absence.
        sql(f'DROP DATABASE `{schema}`;')
    print(json.dumps({'passed': len(results), 'checks': results}, indent=2))
except Exception as error:
    print(json.dumps({'checks': results, 'failure': str(error) if isinstance(error, AssertionError) else type(error).__name__}, indent=2))
    sys.exit(1)
