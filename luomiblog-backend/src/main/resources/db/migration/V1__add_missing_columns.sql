-- =============================================
-- 文章表（article）缺失字段补齐迁移脚本（手动执行）
-- 适用数据库：MySQL 8.0+
--
-- ⚠️ 注意：本脚本必须且只能执行一次！
--    1. MySQL 8.0 不支持 `ADD COLUMN IF NOT EXISTS` 语法（MariaDB 专用），
--       重复执行会因字段已存在而直接报错。
--    2. 仅适用于由旧版建表脚本创建、需要升级到当前 db/schema.sql 结构的存量数据库；
--       全新安装请直接执行 db/schema.sql，无需运行本脚本。
--
-- 字段类型与 db/schema.sql 及 Article 实体保持一致：
--   version 为 VARCHAR(20)（语义化版本号，如 '1.0.0'），与实体 String version 对应；
--   file_path 为 VARCHAR(512)，与 schema.sql 一致。
-- =============================================

ALTER TABLE `article` ADD COLUMN `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重';
ALTER TABLE `article` ADD COLUMN `ai_summary` TEXT DEFAULT NULL COMMENT 'AI生成摘要';
ALTER TABLE `article` ADD COLUMN `knowledge_points` VARCHAR(512) DEFAULT NULL COMMENT 'AI拆解知识点';
ALTER TABLE `article` ADD COLUMN `sync_bailian` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否同步到百炼';
ALTER TABLE `article` ADD COLUMN `bailian_doc_id` VARCHAR(128) DEFAULT NULL COMMENT '百炼文档标识';
ALTER TABLE `article` ADD COLUMN `content_hash` VARCHAR(64) DEFAULT NULL COMMENT '内容SHA256哈希（用于双源冲突检测）';
ALTER TABLE `article` ADD COLUMN `file_path` VARCHAR(512) DEFAULT NULL COMMENT 'MD文件路径（双源架构）';
ALTER TABLE `article` ADD COLUMN `version` VARCHAR(20) DEFAULT '1.0.0' COMMENT '当前版本号';
