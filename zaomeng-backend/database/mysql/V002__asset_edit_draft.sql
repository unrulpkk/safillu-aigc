-- 图片编辑草稿属于资产，不随创作对话的删除丢失。未执行 AI 图片处理。
USE `zaomeng_aigc_20261103`;
CREATE TABLE aigc_asset_edit_draft (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL,
  workspace_id BIGINT NOT NULL,
  asset_id BIGINT NOT NULL,
  tool VARCHAR(32) NOT NULL COMMENT '图片编辑工具名称',
  parameters JSON NOT NULL COMMENT '强度、目标分辨率和编辑说明，不含文件或密钥',
  mask_asset_id BIGINT NULL COMMENT '涂抹蒙版资产 ID',
  revision BIGINT NOT NULL DEFAULT 1,
  creator VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updater VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  deleted BIT(1) NOT NULL DEFAULT b'0',
  active_tool VARCHAR(32) GENERATED ALWAYS AS (CASE WHEN deleted=b'0' THEN tool ELSE NULL END) STORED,
  PRIMARY KEY (id),
  UNIQUE KEY uk_asset_edit_tool(tenant_id,workspace_id,asset_id,active_tool),
  CONSTRAINT fk_edit_owner FOREIGN KEY(tenant_id,workspace_id,asset_id) REFERENCES aigc_asset(tenant_id,workspace_id,id),
  CONSTRAINT fk_edit_mask FOREIGN KEY(tenant_id,workspace_id,mask_asset_id) REFERENCES aigc_asset(tenant_id,workspace_id,id),
  CONSTRAINT ck_edit_revision CHECK(revision>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产图片编辑草稿，按工具保存';
