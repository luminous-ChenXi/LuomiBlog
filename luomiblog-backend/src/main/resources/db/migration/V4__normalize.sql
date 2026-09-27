-- =============================================
-- V4：数据库规整（索引去冗/补齐、ngram 全文索引、审计字段、FK、安全审计日志表）
-- 适用数据库：MySQL 8.0+
--
-- ⚠️ 注意：本脚本为存量数据库手工升级脚本（无 Flyway，需人工执行一次）；
--    全新安装请直接执行 db/schema.sql，无需运行本脚本。
--
-- 幂等性说明：MySQL 8.0 不支持 DROP INDEX IF EXISTS / ADD COLUMN IF NOT EXISTS，
-- 本脚本统一用 information_schema 检查 + PREPARE/EXECUTE 生成 DDL，重复执行安全。
-- （唯一例外：ngram 全文索引重建无法从 information_schema 判断分词器，
--   重复执行只会多一次无副作用的重建。）
--
-- 待办（下个停写窗口执行，本轮不做）：
--   表 `article` 改名 `articles`（涉及实体 @Table、全部原生 SQL、双源同步链路，
--   需要停写窗口配合，与 V4 解耦单独发布）。
-- =============================================

-- ------------------------------------------------------------------
-- 0. 前置检查（只读，不改数据）：FK 建立前的孤儿数据检查
--    若下列查询返回 > 0，必须先清理孤儿行，否则对应 FK 会创建失败：
--
-- article_tags.article_id 孤儿（文章不存在）：
--   SELECT at.id FROM article_tags at LEFT JOIN article a ON at.article_id = a.id WHERE a.id IS NULL;
-- article_tags.tag_id 孤儿（标签不存在）：
--   SELECT at.id FROM article_tags at LEFT JOIN tags t ON at.tag_id = t.id WHERE t.id IS NULL;
-- ------------------------------------------------------------------

-- ------------------------------------------------------------------
-- 1. 清理 5 个冗余索引（被唯一键最左前缀覆盖，纯属写放大）
-- ------------------------------------------------------------------

-- article_tags.idx_article_tags_article（被 uk_article_tag(article_id, tag_id) 前缀覆盖）
SET @ddl = (SELECT IF(
  NOT EXISTS (SELECT 1 FROM information_schema.statistics
              WHERE table_schema = DATABASE() AND table_name = 'article_tags' AND index_name = 'idx_article_tags_article'),
  'SELECT 1 AS noop',
  'ALTER TABLE `article_tags` DROP INDEX `idx_article_tags_article`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- article_likes.idx_article_likes_article（被 uk_article_likes_user(article_id, user_id) 前缀覆盖）
SET @ddl = (SELECT IF(
  NOT EXISTS (SELECT 1 FROM information_schema.statistics
              WHERE table_schema = DATABASE() AND table_name = 'article_likes' AND index_name = 'idx_article_likes_article'),
  'SELECT 1 AS noop',
  'ALTER TABLE `article_likes` DROP INDEX `idx_article_likes_article`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- comment_likes.idx_comment_likes_comment（被 uk_comment_likes_user(comment_id, user_id) 前缀覆盖）
SET @ddl = (SELECT IF(
  NOT EXISTS (SELECT 1 FROM information_schema.statistics
              WHERE table_schema = DATABASE() AND table_name = 'comment_likes' AND index_name = 'idx_comment_likes_comment'),
  'SELECT 1 AS noop',
  'ALTER TABLE `comment_likes` DROP INDEX `idx_comment_likes_comment`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- article_versions.idx_versions_article（被 uk_versions_article_version(article_id, version) 前缀覆盖）
SET @ddl = (SELECT IF(
  NOT EXISTS (SELECT 1 FROM information_schema.statistics
              WHERE table_schema = DATABASE() AND table_name = 'article_versions' AND index_name = 'idx_versions_article'),
  'SELECT 1 AS noop',
  'ALTER TABLE `article_versions` DROP INDEX `idx_versions_article`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- daily_stats.idx_stats_date_range（被 uk_stats_date(stat_date) 覆盖，pv/uv 列选择性不构成索引价值）
SET @ddl = (SELECT IF(
  NOT EXISTS (SELECT 1 FROM information_schema.statistics
              WHERE table_schema = DATABASE() AND table_name = 'daily_stats' AND index_name = 'idx_stats_date_range'),
  'SELECT 1 AS noop',
  'ALTER TABLE `daily_stats` DROP INDEX `idx_stats_date_range`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------------
-- 2. 补齐 3 个缺失索引
-- ------------------------------------------------------------------

-- article_likes(user_id, created_at)：我的点赞列表/近期点赞
SET @ddl = (SELECT IF(
  EXISTS (SELECT 1 FROM information_schema.statistics
          WHERE table_schema = DATABASE() AND table_name = 'article_likes' AND index_name = 'idx_article_likes_user_created'),
  'SELECT 1 AS noop',
  'ALTER TABLE `article_likes` ADD INDEX `idx_article_likes_user_created` (`user_id`, `created_at`)'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- comment_likes(user_id, created_at)
SET @ddl = (SELECT IF(
  EXISTS (SELECT 1 FROM information_schema.statistics
          WHERE table_schema = DATABASE() AND table_name = 'comment_likes' AND index_name = 'idx_comment_likes_user_created'),
  'SELECT 1 AS noop',
  'ALTER TABLE `comment_likes` ADD INDEX `idx_comment_likes_user_created` (`user_id`, `created_at`)'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- attachments(created_at)：后台附件按时间排序/清理
SET @ddl = (SELECT IF(
  EXISTS (SELECT 1 FROM information_schema.statistics
          WHERE table_schema = DATABASE() AND table_name = 'attachments' AND index_name = 'idx_attachments_created'),
  'SELECT 1 AS noop',
  'ALTER TABLE `attachments` ADD INDEX `idx_attachments_created` (`created_at`)'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------------
-- 3. 全文索引改用 ngram 分词器（中文检索必需）
--    注：重复执行仅多一次无副作用的重建
-- ------------------------------------------------------------------

SET @ddl = (SELECT IF(
  NOT EXISTS (SELECT 1 FROM information_schema.statistics
              WHERE table_schema = DATABASE() AND table_name = 'article' AND index_name = 'ft_article_title'),
  'ALTER TABLE `article` ADD FULLTEXT INDEX `ft_article_title` (`title`) WITH PARSER ngram',
  'ALTER TABLE `article` DROP INDEX `ft_article_title`, ADD FULLTEXT INDEX `ft_article_title` (`title`) WITH PARSER ngram'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(
  NOT EXISTS (SELECT 1 FROM information_schema.statistics
              WHERE table_schema = DATABASE() AND table_name = 'article' AND index_name = 'ft_article_content'),
  'ALTER TABLE `article` ADD FULLTEXT INDEX `ft_article_content` (`content`, `ai_summary`) WITH PARSER ngram',
  'ALTER TABLE `article` DROP INDEX `ft_article_content`, ADD FULLTEXT INDEX `ft_article_content` (`content`, `ai_summary`) WITH PARSER ngram'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------------
-- 4. article.status 显式 DEFAULT 'draft'（对齐 schema.sql 口径，天然可重复执行）
-- ------------------------------------------------------------------

ALTER TABLE `article` MODIFY COLUMN `status` ENUM('draft','published','archived') NOT NULL DEFAULT 'draft' COMMENT '状态';

-- ------------------------------------------------------------------
-- 5. 补审计字段（5 处）
-- ------------------------------------------------------------------

-- permissions.updated_at
SET @ddl = (SELECT IF(
  EXISTS (SELECT 1 FROM information_schema.columns
          WHERE table_schema = DATABASE() AND table_name = 'permissions' AND column_name = 'updated_at'),
  'SELECT 1 AS noop',
  'ALTER TABLE `permissions` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- attachments.updated_at
SET @ddl = (SELECT IF(
  EXISTS (SELECT 1 FROM information_schema.columns
          WHERE table_schema = DATABASE() AND table_name = 'attachments' AND column_name = 'updated_at'),
  'SELECT 1 AS noop',
  'ALTER TABLE `attachments` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- article_rewards.updated_at
SET @ddl = (SELECT IF(
  EXISTS (SELECT 1 FROM information_schema.columns
          WHERE table_schema = DATABASE() AND table_name = 'article_rewards' AND column_name = 'updated_at'),
  'SELECT 1 AS noop',
  'ALTER TABLE `article_rewards` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- comment_mentions.updated_at
SET @ddl = (SELECT IF(
  EXISTS (SELECT 1 FROM information_schema.columns
          WHERE table_schema = DATABASE() AND table_name = 'comment_mentions' AND column_name = 'updated_at'),
  'SELECT 1 AS noop',
  'ALTER TABLE `comment_mentions` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- user_profiles.created_at（仅建时间，无 ON UPDATE）
SET @ddl = (SELECT IF(
  EXISTS (SELECT 1 FROM information_schema.columns
          WHERE table_schema = DATABASE() AND table_name = 'user_profiles' AND column_name = 'created_at'),
  'SELECT 1 AS noop',
  'ALTER TABLE `user_profiles` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------------
-- 6. article_tags 补 2 个物理 FK（ON DELETE CASCADE）
--    执行前先跑文件头的孤儿数据检查；FK 已存在时跳过
-- ------------------------------------------------------------------

SET @ddl = (SELECT IF(
  EXISTS (SELECT 1 FROM information_schema.table_constraints
          WHERE table_schema = DATABASE() AND table_name = 'article_tags'
            AND constraint_name = 'fk_article_tags_article' AND constraint_type = 'FOREIGN KEY'),
  'SELECT 1 AS noop',
  'ALTER TABLE `article_tags` ADD CONSTRAINT `fk_article_tags_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(
  EXISTS (SELECT 1 FROM information_schema.table_constraints
          WHERE table_schema = DATABASE() AND table_name = 'article_tags'
            AND constraint_name = 'fk_article_tags_tag' AND constraint_type = 'FOREIGN KEY'),
  'SELECT 1 AS noop',
  'ALTER TABLE `article_tags` ADD CONSTRAINT `fk_article_tags_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------------
-- 7. 新增安全审计日志表
-- ------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `login_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（未定位到账号时为 NULL）',
  `username` VARCHAR(64) DEFAULT NULL COMMENT '尝试登录的用户名/邮箱',
  `login_type` ENUM('password','chenxi_sso','totp') NOT NULL DEFAULT 'password' COMMENT '登录方式',
  `success` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否成功',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'User-Agent',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_login_logs_user` (`user_id`, `created_at`),
  KEY `idx_login_logs_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

CREATE TABLE IF NOT EXISTS `admin_operation_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人用户ID',
  `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人用户名（冗余，防用户删除）',
  `module` VARCHAR(64) NOT NULL COMMENT '业务模块（article/user/comment/system...）',
  `action` VARCHAR(64) NOT NULL COMMENT '操作动作（create/update/delete/...）',
  `target_type` VARCHAR(64) DEFAULT NULL COMMENT '目标对象类型',
  `target_id` VARCHAR(64) DEFAULT NULL COMMENT '目标对象ID',
  `detail` JSON DEFAULT NULL COMMENT '操作详情',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT '操作IP',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_op_logs_operator` (`operator_id`, `created_at`),
  KEY `idx_admin_op_logs_module` (`module`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理操作日志表（写入点下个工作包接入）';
