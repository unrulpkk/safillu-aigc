-- Existing framework AI registry names; these tables were not enabled in V001.
-- Credential value is a Docker secret reference, never the provider secret itself.
USE zaomeng_aigc_20261103;
CREATE TABLE IF NOT EXISTS ai_api_key (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,name VARCHAR(100) NOT NULL,
 api_key VARCHAR(2048) NOT NULL,platform VARCHAR(50) NOT NULL,url VARCHAR(255) NOT NULL,status TINYINT NOT NULL DEFAULT 0,
 creator VARCHAR(64) NOT NULL DEFAULT '',create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
 updater VARCHAR(64) NOT NULL DEFAULT '',update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,deleted BIT NOT NULL DEFAULT b'0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_model (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,key_id BIGINT NOT NULL,name VARCHAR(100) NOT NULL,model VARCHAR(100) NOT NULL,
 platform VARCHAR(50) NOT NULL,type INT NOT NULL,sort INT NOT NULL DEFAULT 0,status TINYINT NOT NULL DEFAULT 0,
 temperature DOUBLE NULL,max_tokens INT NULL,max_contexts INT NULL,input_modalities JSON NULL,reasoning_config JSON NULL,
 creator VARCHAR(64) NOT NULL DEFAULT '',create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
 updater VARCHAR(64) NOT NULL DEFAULT '',update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,deleted BIT NOT NULL DEFAULT b'0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO ai_api_key(id,name,api_key,platform,url,status,creator)
VALUES(870001,'Safillu 项目密钥','file:/run/secrets/provider_key','OpenAI','https://api.safillu.com',0,'zaomeng-bootstrap')
ON DUPLICATE KEY UPDATE url=VALUES(url);
INSERT INTO ai_model(id,key_id,name,model,platform,type,sort,status,creator) VALUES
(870011,870001,'Qwen Image 2.1','Qwen-Image-2.1','OpenAI',2,1,0,'zaomeng-bootstrap'),
(870012,870001,'Z-Image Base','Z-Image-Base','OpenAI',2,2,0,'zaomeng-bootstrap'),
(870013,870001,'Qwen Image 2.1 Edit','Qwen-Image-2.1-Edit','OpenAI',2,3,0,'zaomeng-bootstrap'),
(870014,870001,'MiniMax H3','MiniMax-H3','OpenAI',4,4,0,'zaomeng-bootstrap'),
(870015,870001,'MiniMax H3 SelfLift','MiniMax-H3-SelfLift','OpenAI',4,5,0,'zaomeng-bootstrap'),
(870016,870001,'Qwen3 语音设计','Qwen3-TTS-VoiceDesign','OpenAI',3,6,0,'zaomeng-bootstrap'),
(870017,870001,'YuE2 背景音乐','YuE2-BGM','OpenAI',3,7,0,'zaomeng-bootstrap')
ON DUPLICATE KEY UPDATE name=VALUES(name);
INSERT INTO aigc_model_capability(id,ai_model_id,operation,parameter_schema,default_parameters,limits_config,billing_unit,points_per_unit,status,creator) VALUES
(870011,870011,'IMAGE','{}','{"ratio":"1:1","resolution":"1K","adapter":"image"}','{"maxCount":4,"maxRefs":0,"ratios":["1:1","16:9","9:16","4:3","3:4","3:2","2:3","21:9"],"resolutions":["1K","2K"]}','IMAGE',2,'ENABLED','zaomeng-bootstrap'),
(870012,870012,'IMAGE','{}','{"ratio":"1:1","resolution":"1K","adapter":"image"}','{"maxCount":4,"maxRefs":0,"ratios":["1:1","16:9","9:16","4:3","3:4","3:2","2:3","21:9"],"resolutions":["1K","2K"]}','IMAGE',2,'ENABLED','zaomeng-bootstrap'),
(870013,870013,'IMAGE','{}','{"ratio":"1:1","resolution":"1K","adapter":"edit"}','{"maxCount":1,"minRefs":1,"maxRefs":3,"ratios":["1:1","16:9","9:16","4:3","3:4","3:2","2:3","21:9"],"resolutions":["1K","2K"]}','IMAGE',2,'ENABLED','zaomeng-bootstrap'),
(870014,870014,'VIDEO','{}','{"ratio":"16:9","resolution":"768P","adapter":"video"}','{"maxCount":1,"maxRefs":9,"minSeconds":4,"maxSeconds":15,"ratios":["16:9","9:16","1:1","4:3","3:4","21:9"],"resolutions":["768P","1080P","2K"]}','SECOND',3,'ENABLED','zaomeng-bootstrap'),
(870015,870015,'VIDEO','{}','{"ratio":"16:9","resolution":"480P","adapter":"selflift"}','{"maxCount":1,"maxRefs":9,"minSeconds":4,"maxSeconds":15,"ratios":["16:9","9:16","1:1","4:3","3:4","21:9"],"resolutions":["480P","768P","1080P","2K"]}','SECOND',1,'ENABLED','zaomeng-bootstrap'),
(870016,870016,'AUDIO','{}','{"adapter":"speech"}','{"maxCount":1,"maxRefs":0,"maxPrompt":4000}','REQUEST',5,'ENABLED','zaomeng-bootstrap'),
(870017,870017,'MUSIC','{}','{"adapter":"music","musicSeconds":30}','{"maxCount":1,"maxRefs":0,"maxSeconds":120}','REQUEST',5,'ENABLED','zaomeng-bootstrap')
ON DUPLICATE KEY UPDATE limits_config=VALUES(limits_config);
