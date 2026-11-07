import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('bootstrap', Path(__file__).with_name('bootstrap.py'))
bootstrap = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bootstrap)


class BootstrapTests(unittest.TestCase):
    def test_sql_strings_and_comments(self):
        sql = "-- ignore ;\nINSERT INTO t VALUES('a;b', 'it\\'s', 'two''quotes'); /* ; */ SELECT `semi;col`; # x;\nSELECT 3;"
        statements = list(bootstrap.sql_statements(sql))
        self.assertEqual(len(statements), 3)
        self.assertIn("'a;b'", statements[0])
        self.assertEqual(statements[1], 'SELECT `semi;col`')
        self.assertEqual(statements[2], 'SELECT 3')

    def test_secret_restart_preserves_credentials(self):
        with tempfile.TemporaryDirectory() as tmp:
            previous = bootstrap.SECRET_ROOT
            bootstrap.SECRET_ROOT = Path(tmp)
            original_protect = bootstrap.protect
            bootstrap.protect = lambda *args, **kwargs: None  # Host-independent filesystem permissions.
            try:
                bootstrap.init_secrets()
                before = {p.relative_to(tmp): p.read_bytes() for p in Path(tmp).rglob('*') if p.is_file()}
                bootstrap.init_secrets()
                after = {p.relative_to(tmp): p.read_bytes() for p in Path(tmp).rglob('*') if p.is_file()}
                self.assertEqual(before, after)
                accounts = json.loads((Path(tmp) / 'initial-accounts.json').read_text())
                self.assertNotEqual(accounts['admin']['password'], accounts['operator']['password'])
                self.assertEqual(len(accounts['admin']['password']), 16)
                self.assertEqual((Path(tmp) / 'provider_key').read_text().strip(), '')
            finally:
                bootstrap.SECRET_ROOT = previous
                bootstrap.protect = original_protect

    def test_only_completed_exact_legacy_seed_is_acknowledged(self):
        name, (previous, current) = next(iter(bootstrap.SANITIZED_LEGACY_MIGRATIONS.items()))
        self.assertTrue(bootstrap.accepts_sanitized_legacy(name, previous, current, True))
        self.assertFalse(bootstrap.accepts_sanitized_legacy(name, previous, current, False))
        self.assertFalse(bootstrap.accepts_sanitized_legacy(name, '0' * 64, current, True))
        self.assertFalse(bootstrap.accepts_sanitized_legacy(name, previous, '0' * 64, True))
        self.assertFalse(bootstrap.accepts_sanitized_legacy('another.sql', previous, current, True))

    def test_reject_database_injection(self):
        for value in ['mysql; DROP DATABASE x', '../safillu', '', 'safillu`']:
            with self.assertRaises(RuntimeError):
                bootstrap.identifier(value)

    def test_all_migration_scripts_parse(self):
        root = Path(__file__).resolve().parents[1] / 'zaomeng-backend/database/mysql'
        count = 0
        for file in root.rglob('*.sql'):
            statements = list(bootstrap.sql_statements(file.read_text(encoding='utf-8-sig')))
            self.assertTrue(statements, file.name)
            self.assertFalse(any(s.upper().startswith(('DROP ', 'TRUNCATE ', 'DELIMITER ')) for s in statements), file.name)
            count += len(statements)
        self.assertGreater(count, 700)


if __name__ == '__main__':
    unittest.main()
