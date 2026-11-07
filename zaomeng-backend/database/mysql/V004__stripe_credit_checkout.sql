-- Reuse yudao payment registry/order names. No new user, password or duplicate payment system.
USE zaomeng_aigc_20261103;
CREATE TABLE IF NOT EXISTS pay_app (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,app_key VARCHAR(64) NOT NULL,name VARCHAR(100) NOT NULL,status INT NOT NULL DEFAULT 0,
 remark VARCHAR(500),order_notify_url VARCHAR(1024),refund_notify_url VARCHAR(1024),transfer_notify_url VARCHAR(1024),
 creator VARCHAR(64) NOT NULL DEFAULT '',create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),updater VARCHAR(64) NOT NULL DEFAULT '',update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),deleted BIT NOT NULL DEFAULT b'0',UNIQUE KEY uk_pay_app_key(app_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_channel (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,code VARCHAR(32) NOT NULL,status INT NOT NULL DEFAULT 0,fee_rate DOUBLE DEFAULT 0,remark VARCHAR(500),app_id BIGINT NOT NULL,config LONGTEXT NOT NULL,tenant_id BIGINT NOT NULL DEFAULT 0,
 creator VARCHAR(64) NOT NULL DEFAULT '',create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),updater VARCHAR(64) NOT NULL DEFAULT '',update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),deleted BIT NOT NULL DEFAULT b'0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_order (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,app_id BIGINT NOT NULL,channel_id BIGINT,channel_code VARCHAR(32),user_id BIGINT,user_type INT,
 merchant_order_id VARCHAR(64) NOT NULL,subject VARCHAR(128) NOT NULL,body VARCHAR(256),notify_url VARCHAR(1024),price INT NOT NULL,
 channel_fee_rate DOUBLE,channel_fee_price INT,status INT NOT NULL DEFAULT 0,user_ip VARCHAR(50),expire_time DATETIME(3),success_time DATETIME(3),extension_id BIGINT,no VARCHAR(64),refund_price INT NOT NULL DEFAULT 0,channel_user_id VARCHAR(191),channel_order_no VARCHAR(191),
 creator VARCHAR(64) NOT NULL DEFAULT '',create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),updater VARCHAR(64) NOT NULL DEFAULT '',update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),deleted BIT NOT NULL DEFAULT b'0',UNIQUE KEY uk_pay_order_merchant(app_id,merchant_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_order_extension (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,no VARCHAR(64) NOT NULL,order_id BIGINT NOT NULL,channel_id BIGINT NOT NULL,channel_code VARCHAR(32) NOT NULL,user_ip VARCHAR(50),status INT NOT NULL DEFAULT 0,channel_extras LONGTEXT,channel_error_code VARCHAR(128),channel_error_msg VARCHAR(512),channel_notify_data LONGTEXT,
 creator VARCHAR(64) NOT NULL DEFAULT '',create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),updater VARCHAR(64) NOT NULL DEFAULT '',update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),deleted BIT NOT NULL DEFAULT b'0',UNIQUE KEY uk_pay_ext_no(no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO pay_app(id,app_key,name,status,remark) VALUES(880001,'zaomeng-credit','造梦积分充值',0,'Stripe Checkout，正式收款需配置账户与上线价格') ON DUPLICATE KEY UPDATE name=VALUES(name);
INSERT INTO pay_channel(id,code,status,app_id,config,tenant_id,remark) VALUES(880001,'stripe',1,880001,'{"secretReference":"/run/secrets/stripe","adapter":"zaomeng-checkout"}',1,'密钥仅存 Docker secret；不在数据库保存明文') ON DUPLICATE KEY UPDATE remark=VALUES(remark);

CREATE TABLE aigc_credit_pack (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,code VARCHAR(64) NOT NULL,name VARCHAR(100) NOT NULL,environment VARCHAR(8) NOT NULL,currency CHAR(3) NOT NULL,amount_minor INT NOT NULL,credit_points DECIMAL(18,4) NOT NULL,enabled BIT NOT NULL DEFAULT b'0',sort_order INT NOT NULL DEFAULT 0,
 creator VARCHAR(64) NOT NULL DEFAULT '',create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),updater VARCHAR(64) NOT NULL DEFAULT '',update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),deleted BIT NOT NULL DEFAULT b'0',
 UNIQUE KEY uk_credit_pack_code(code),CONSTRAINT ck_credit_pack CHECK(environment IN ('TEST','LIVE') AND amount_minor>0 AND credit_points>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE aigc_credit_purchase (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT NOT NULL,workspace_id BIGINT NOT NULL,user_id BIGINT NOT NULL,
 purchase_no VARCHAR(64) NOT NULL,client_request_id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,pack_id BIGINT NOT NULL,pack_name VARCHAR(100) NOT NULL,
 environment VARCHAR(8) NOT NULL,currency CHAR(3) NOT NULL,amount_minor INT NOT NULL,credit_points DECIMAL(18,4) NOT NULL,
 pay_order_id BIGINT NOT NULL,pay_extension_id BIGINT NOT NULL,checkout_session_id VARCHAR(191) COLLATE utf8mb4_bin,checkout_url TEXT,payment_intent_id VARCHAR(191),
 expires_time DATETIME(3) NOT NULL,status VARCHAR(24) NOT NULL DEFAULT 'CREATING',credited BIT NOT NULL DEFAULT b'0',billing_hold BIT NOT NULL DEFAULT b'0',refund_minor INT NOT NULL DEFAULT 0,paid_time DATETIME(3),last_error VARCHAR(500),
 creator VARCHAR(64) NOT NULL DEFAULT '',create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),updater VARCHAR(64) NOT NULL DEFAULT '',update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),deleted BIT NOT NULL DEFAULT b'0',
 UNIQUE KEY uk_purchase_scope(tenant_id,workspace_id,id),UNIQUE KEY uk_purchase_request(tenant_id,workspace_id,client_request_id),UNIQUE KEY uk_purchase_no(purchase_no),UNIQUE KEY uk_purchase_checkout(checkout_session_id),
 KEY idx_purchase_history(tenant_id,workspace_id,id),KEY idx_purchase_intent(payment_intent_id),
 CONSTRAINT fk_purchase_workspace FOREIGN KEY(tenant_id,workspace_id) REFERENCES aigc_workspace(tenant_id,id),
 CONSTRAINT fk_purchase_pay_order FOREIGN KEY(pay_order_id) REFERENCES pay_order(id),
 CONSTRAINT fk_purchase_pack FOREIGN KEY(pack_id) REFERENCES aigc_credit_pack(id),
 CONSTRAINT ck_purchase_amount CHECK(environment IN ('TEST','LIVE') AND amount_minor>0 AND credit_points>0 AND refund_minor>=0),
 CONSTRAINT ck_purchase_status CHECK(status IN ('CREATING','OPEN','PROCESSING','PAID','EXPIRED','FAILED','REVIEW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE aigc_stripe_event (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,event_id VARCHAR(191) COLLATE utf8mb4_bin NOT NULL,event_type VARCHAR(100) NOT NULL,livemode BIT NOT NULL,provider_reference VARCHAR(191),purchase_id BIGINT,handled BIT NOT NULL DEFAULT b'0',summary VARCHAR(500),
 create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),handled_time DATETIME(3),UNIQUE KEY uk_stripe_event(event_id,livemode),KEY idx_stripe_reference(provider_reference,event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- Explicit test-only catalogue. No live prices are invented or activated.
INSERT INTO aigc_credit_pack(code,name,environment,currency,amount_minor,credit_points,enabled,sort_order) VALUES
('test-starter','测试体验包','TEST','usd',500,500,b'1',1),
('test-creator','测试创作包','TEST','usd',2000,2000,b'1',2),
('test-studio','测试工作室包','TEST','usd',5000,5000,b'1',3);
