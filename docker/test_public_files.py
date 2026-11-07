import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('public_files', Path(__file__).with_name('check-public-files.py'))
guard = importlib.util.module_from_spec(spec)
spec.loader.exec_module(guard)


class PublicFilesTests(unittest.TestCase):
    def test_runtime_and_secret_trees_are_private(self):
        for path in ['.runtime/secrets/provider_key', 'deploy/secrets/google/client_secret', 'x/.git/config',
                     'x/.openai/hosting.json', 'x/node_modules/key.js']:
            self.assertTrue(guard.private_path(path), path)

    def test_private_overrides_are_blocked(self):
        for path in ['.env.local', '.env.prod.local', 'x/config.local.json', 'x/application-dev.yaml',
                     'x/application-upstream.local.yaml', 'x/maven-settings.xml', 'x/key.pem']:
            self.assertTrue(guard.private_path(path), path)

    def test_only_reviewed_environment_templates_are_allowed(self):
        for path in guard.PUBLIC_ENV:
            self.assertFalse(guard.private_path(path), path)
        self.assertTrue(guard.private_path('another-project/.env.prod'))

    def test_source_and_sanitized_reports_are_allowed(self):
        for path in ['src/SecretConfig.java', 'docs/test-results.json', 'docs/credentials.md',
                     'docs/images/media-cache-verified.png', 'x/application-local.example.yaml']:
            self.assertFalse(guard.private_path(path), path)


if __name__ == '__main__':
    unittest.main()
