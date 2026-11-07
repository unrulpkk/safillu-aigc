-- Yudao TenantDO.PACKAGE_ID_SYSTEM = 0. The existing deployment is the
-- platform tenant, not a customer restricted to a fixed package menu list.
-- This restores native menu assignment for newly created platform modules.
-- Users and content operators still require their original roles/permissions.
UPDATE system_tenant
SET package_id = 0, updater = 'platform-integration'
WHERE id = 1 AND name = '造梦' AND package_id = 1 AND deleted = 0;
