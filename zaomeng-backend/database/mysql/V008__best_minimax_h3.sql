-- Native Safillu gateway model; provider credentials remain Docker secret references.
USE zaomeng_aigc_20261103;
INSERT INTO ai_model(id,key_id,name,model,platform,type,sort,status,creator)
VALUES(870018,870001,'Best MiniMax H3','best-minimax-h3','OpenAI',4,0,0,'safillu-bootstrap')
ON DUPLICATE KEY UPDATE name=VALUES(name);
INSERT INTO aigc_model_capability(id,ai_model_id,operation,parameter_schema,default_parameters,limits_config,billing_unit,points_per_unit,status,creator)
VALUES(870018,870018,'VIDEO','{}',
 '{"ratio":"16:9","resolution":"480P","adapter":"best-h3"}',
 '{"maxCount":1,"maxRefs":9,"maxAudioRefs":3,"maxVideoRefs":3,"minSeconds":4,"maxSeconds":15,"ratios":["16:9"],"resolutions":["480P","768P","1080P","2K"]}',
 'SECOND',1,'ENABLED','safillu-bootstrap')
ON DUPLICATE KEY UPDATE ai_model_id=VALUES(ai_model_id);
