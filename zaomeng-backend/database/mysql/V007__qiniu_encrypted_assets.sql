USE zaomeng_aigc_20261103;
UPDATE infra_file_config SET config=JSON_SET(config,'$.encryptContent',true,'$.encryptionKeyReference','file:/run/secrets/storage/content_key') WHERE id=880002 AND storage=20 AND master=b'0';
