"""Export reproducible source deployment inputs, excluding all local/private state."""
import fnmatch
import os
from pathlib import Path
import tarfile

ROOT = Path(__file__).resolve().parents[1]
EXCLUDE_DIRS = {'.git', 'node_modules', 'target', '.idea', '.vscode', '.runtime',
                'secrets', 'backups', '__pycache__', 'qa', 'test-results', 'logs',
                'dist-prod', 'dist-zaomeng', '.image'}
EXCLUDE_FILES = ['*.log', '*.pyc', '*.zip', '*.tar.gz', '*.pem', '*.key', '*.p12',
                 'application-local.y*', 'application-dev.y*', 'application-*.local.y*',
                 'maven-settings.xml', 'settings.xml', '.flattened-pom.xml']


def inputs():
    for name in ['README.md', 'README.zh-CN.md', 'README.es.md',
                 'docs/credentials.md', 'docs/credentials.zh-CN.md', 'docs/credentials.es.md',
                 'docs/test-report.md', 'docs/test-report.zh-CN.md', 'docs/test-report.es.md', 'docs/test-results.json',
                 'LICENSE', 'compose.yaml', '.dockerignore', '.gitignore', '.gitattributes', '.env.example', '.env.prod.example', 'start.ps1', 'start.sh']:
        yield ROOT / name
    for prefix in ['docker', 'docs/images', 'lumina-studio/dist', 'zaomeng-admin', 'zaomeng-backend/database/mysql',
                   'zaomeng-backend/server']:
        base = ROOT / prefix
        for folder, dirs, names in os.walk(base):
            dirs[:] = [d for d in dirs if d not in EXCLUDE_DIRS]
            for name in names:
                path = Path(folder) / name
                if valid_input(path, base, prefix):
                    yield path
    for name in ['nginx.conf', 'admin-nginx.conf']:
        yield ROOT / 'zaomeng-backend/deploy' / name


def valid_input(path, base, prefix):
    relative = path.relative_to(base)
    if prefix == 'docker' and relative.parts[0] == 'validation' and relative.as_posix() != 'validation/email-registration-test.py':
        return False
    if prefix == 'zaomeng-admin' and relative.as_posix() == 'ZAOMENG.md':
        return False
    if not path.is_file() or EXCLUDE_DIRS.intersection(relative.parts):
        return False
    if prefix.endswith('/server') and relative.parts[0] not in {
        'pom.xml', 'LICENSE', 'lombok.config', 'yudao-dependencies', 'yudao-framework', 'yudao-server',
        'yudao-module-system', 'yudao-module-infra', 'yudao-module-aigc'}:
        return False
    if any(fnmatch.fnmatch(path.name, pattern) for pattern in EXCLUDE_FILES):
        return False
    if path.name.startswith('.env') and not (prefix == 'zaomeng-admin' and path.name in ('.env', '.env.prod')):
        return False
    return True


if __name__ == '__main__':
    output = ROOT / 'safillu-docker-source.tar.gz'
    files = sorted(set(inputs()))
    with tarfile.open(output, 'w:gz') as archive:
        for path in files:
            archive.add(path, arcname=path.relative_to(ROOT).as_posix(), recursive=False)
    print(f'{output}: {len(files)} files, {output.stat().st_size // 1024 // 1024} MiB; no local configuration or secrets.')
