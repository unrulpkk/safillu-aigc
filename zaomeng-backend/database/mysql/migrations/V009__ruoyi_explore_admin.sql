USE `zaomeng_aigc_20261103`;
-- Native Ruoyi menu and button permissions. No user receives a role here.
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show)
VALUES(890010,'内容运营','',1,10,0,'/aigc','ep:picture','','',0,b'1',b'1',b'1'),
(890011,'探索内容','aigc:explore:query',2,10,890010,'explore','ep:picture','aigc/explore/index','AigcExploreContent',0,b'1',b'1',b'1'),
(890012,'维护探索内容','aigc:explore:manage',3,10,890011,'','','','',0,b'1',b'1',b'1')
ON DUPLICATE KEY UPDATE name=VALUES(name),permission=VALUES(permission),component=VALUES(component),component_name=VALUES(component_name),status=0,deleted=b'0';
INSERT INTO system_role_menu(role_id,menu_id,tenant_id)
SELECT r.id,m.id,1 FROM system_role r JOIN system_menu m ON m.id IN (890010,890011,890012)
WHERE r.tenant_id=1 AND r.code='aigc_content_operator' AND r.deleted=b'0'
AND NOT EXISTS(SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.id AND rm.menu_id=m.id AND rm.tenant_id=1 AND rm.deleted=b'0');
UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(CAST(menu_ids AS JSON),'$',890010) WHERE id=1 AND NOT JSON_CONTAINS(CAST(menu_ids AS JSON),'890010');
UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(CAST(menu_ids AS JSON),'$',890011) WHERE id=1 AND NOT JSON_CONTAINS(CAST(menu_ids AS JSON),'890011');
UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(CAST(menu_ids AS JSON),'$',890012) WHERE id=1 AND NOT JSON_CONTAINS(CAST(menu_ids AS JSON),'890012');
