-- 新项目的必要配置，不能导入到其他项目库；不创建默认管理员或默认密码。
USE `zaomeng_aigc_20261103`;
SET NAMES utf8mb4;
INSERT INTO system_tenant_package(id,name,status,menu_ids) VALUES(1,'个人创作',0,'[]');
INSERT INTO system_tenant(id,name,contact_name,status,websites,package_id,expire_time,account_count)
VALUES(1,'造梦','造梦',0,'[]',1,'2099-12-31 00:00:00',10000);
INSERT INTO infra_config(category,type,name,config_key,value,visible)
VALUES('aigc',1,'开放账号注册','system.user.register-enabled','true',b'0');
INSERT INTO system_oauth2_client(id,client_id,secret,name,logo,status,access_token_validity_seconds,refresh_token_validity_seconds,redirect_uris,authorized_grant_types,scopes,auto_approve_scopes,authorities,resource_ids)
VALUES(1,'default',SHA2(RANDOM_BYTES(32),256),'造梦网页','',0,1800,86400,'[]','["password","refresh_token"]','[]','[]','[]','[]');
INSERT INTO infra_file_config(id,name,storage,master,config)
VALUES(1,'造梦独立文件目录',10,b'1',JSON_OBJECT('@class','cn.iocoder.yudao.module.infra.framework.file.core.client.local.LocalFileClientConfig','basePath','/app/uploads','domain','http://127.0.0.1:48080'));
-- 注册并发由数据库约束兜底，删除账号后允许重新注册，但保留原账号历史。
ALTER TABLE system_users ADD COLUMN aigc_active_username VARCHAR(30)
GENERATED ALWAYS AS (CASE WHEN deleted=b'0' THEN username ELSE NULL END) STORED,
ADD UNIQUE KEY uk_aigc_user_account(tenant_id,aigc_active_username);
