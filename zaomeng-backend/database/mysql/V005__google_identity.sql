-- Reuse yudao social identity tables. Google subjects are case-sensitive and may exceed 32 characters.
USE zaomeng_aigc_20261103;
ALTER TABLE system_social_user MODIFY openid VARCHAR(255) NOT NULL;
ALTER TABLE system_social_user ADD COLUMN aigc_google_subject VARCHAR(255) COLLATE utf8mb4_bin GENERATED ALWAYS AS (CASE WHEN type=50 AND deleted=b'0' THEN openid ELSE NULL END) STORED, ADD UNIQUE KEY uk_google_subject(tenant_id,aigc_google_subject);
ALTER TABLE system_social_user_bind ADD COLUMN aigc_google_identity BIGINT GENERATED ALWAYS AS (CASE WHEN social_type=50 AND deleted=b'0' THEN social_user_id ELSE NULL END) STORED, ADD COLUMN aigc_google_owner BIGINT GENERATED ALWAYS AS (CASE WHEN social_type=50 AND deleted=b'0' THEN user_id ELSE NULL END) STORED, ADD UNIQUE KEY uk_google_identity_owner(tenant_id,user_type,aigc_google_identity),ADD UNIQUE KEY uk_google_owner_identity(tenant_id,user_type,aigc_google_owner);
