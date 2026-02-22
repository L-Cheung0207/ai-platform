-- 删除 Flyway 中 V10 的失败迁移记录，以便重新执行
-- 在 MySQL 中执行: mysql -u root -p ai_platform < scripts/fix-flyway-failed-migration.sql
DELETE FROM flyway_schema_history WHERE version = '10' AND success = 0;
