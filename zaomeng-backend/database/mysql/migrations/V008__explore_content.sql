-- Dedicated operational content; never publish private workspace assets by reference.
USE `zaomeng_aigc_20261103`;
CREATE TABLE IF NOT EXISTS aigc_explore_media (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id BIGINT NOT NULL,
 infra_file_id BIGINT NOT NULL,
 kind VARCHAR(16) NOT NULL,
 mime_type VARCHAR(100) NOT NULL,
 file_name VARCHAR(220) NOT NULL,
 byte_size BIGINT NOT NULL,
 creator VARCHAR(64) NOT NULL DEFAULT '',
 create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 deleted BIT(1) NOT NULL DEFAULT b'0',
 KEY idx_explore_media_tenant (tenant_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='探索专用运营媒体';
CREATE TABLE IF NOT EXISTS aigc_explore_content (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id BIGINT NOT NULL,
 title_i18n JSON NOT NULL,
 description_i18n JSON NOT NULL,
 operation VARCHAR(16) NOT NULL DEFAULT 'IMAGE',
 prompt TEXT NOT NULL,
 parameters JSON NOT NULL,
 cover_media_id BIGINT NULL,
 media_id BIGINT NULL,
 sort_order INT NOT NULL DEFAULT 0,
 status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
 download_enabled BIT(1) NOT NULL DEFAULT b'1',
 revision INT NOT NULL DEFAULT 1,
 creator VARCHAR(64) NOT NULL DEFAULT '',
 updater VARCHAR(64) NOT NULL DEFAULT '',
 create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
 deleted BIT(1) NOT NULL DEFAULT b'0',
 KEY idx_explore_published (tenant_id,deleted,status,sort_order,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='探索运营内容';
INSERT INTO system_role(name,code,sort,status,type,remark,tenant_id)
SELECT '探索内容运营','aigc_content_operator',50,0,2,'仅管理当前租户的探索内容',1
WHERE NOT EXISTS (SELECT 1 FROM system_role WHERE tenant_id=1 AND code='aigc_content_operator' AND deleted=b'0');
