-- Verified email identities map to native Yudao users. No passwords or codes here.
CREATE TABLE IF NOT EXISTS aigc_email_identity (
  tenant_id BIGINT NOT NULL,
  email VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  user_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (tenant_id, email),
  UNIQUE KEY uk_email_user (tenant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
