"""Reject private deployment state and embedded repositories in the Git index."""
import re
import subprocess
from pathlib import PurePosixPath

PUBLIC_ENV = {
    '.env.example', '.env.prod.example', '.env.local.example',
    'lumina-studio/.env.prod', 'lumina-studio/.env.local.example',
    'zaomeng-admin/.env', 'zaomeng-admin/.env.prod', 'zaomeng-admin/.env.local.example',
}
PRIVATE_PARTS = {'.git', '.openai', '.image', '.ssh', '.aws', 'secrets', '.secrets',
                 'backups', 'node_modules', 'target', 'logs', 'dist-prod', 'dist-zaomeng'}


def private_path(name):
    path = PurePosixPath(name)
    if any(part in PRIVATE_PARTS or part.startswith('.runtime') for part in path.parts):
        return True
    if path.name.startswith('.env') and name not in PUBLIC_ENV:
        return True
    if re.match(r'application-(dev|local)(\.|-)', path.name) and not path.name.endswith('.example.yaml'):
        return True
    if re.search(r'\.local\.(json|ya?ml|properties)$', path.name):
        return True
    if path.name in {'maven-settings.xml', 'settings.xml', 'http-client.private.env.json'}:
        return True
    return path.name.endswith(('.pem', '.key', '.p12', '.pfx', '.keystore', '.tar.gz', '.zip', '.har'))


def main():
    result = subprocess.run(['git', 'ls-files', '--stage', '-z'], capture_output=True, check=True)
    entries = [entry for entry in result.stdout.split(b'\0') if entry]
    failures = []
    for entry in entries:
        metadata, raw_name = entry.split(b'\t', 1)
        name = raw_name.decode('utf-8')
        if metadata.startswith(b'160000 ') or private_path(name):
            failures.append(name)
    if failures:
        raise SystemExit('Private files or embedded repositories in Git index:\n' + '\n'.join(failures))
    print(f'Public file boundary passed: {len(entries)} tracked paths, no private state or Git links.')


if __name__ == '__main__':
    main()
