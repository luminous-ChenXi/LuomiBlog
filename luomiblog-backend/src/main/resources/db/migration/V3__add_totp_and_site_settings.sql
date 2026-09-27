-- =============================================
-- V3：管理端站长开关（注册邮箱验证 + 登录 2FA）所需结构
-- 适用数据库：MySQL 8.0+
--
-- ⚠️ 注意：本脚本必须且只能执行一次！
--    仅适用于由旧版建表脚本创建、需要升级到当前 db/schema.sql 结构的存量数据库；
--    全新安装请直接执行 db/schema.sql，无需运行本脚本。
-- =============================================

-- 用户表：TOTP 两步验证字段
ALTER TABLE `users` ADD COLUMN `totp_secret` VARCHAR(64) DEFAULT NULL COMMENT 'TOTP 两步验证密钥（Base32）';
ALTER TABLE `users` ADD COLUMN `totp_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已绑定 TOTP 两步验证';
ALTER TABLE `users` ADD COLUMN `recovery_codes` TEXT DEFAULT NULL COMMENT '2FA 还原码（BCrypt 哈希 JSON 数组）';

-- 站点设置键值表（WordPress 式）
CREATE TABLE IF NOT EXISTS `site_settings` (
  `name` VARCHAR(191) NOT NULL COMMENT '设置键',
  `value` TEXT DEFAULT NULL COMMENT '设置值',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站点设置键值表';

INSERT IGNORE INTO `site_settings` (`name`, `value`, `updated_at`) VALUES
('registration.email_verify_required', 'false', NOW()),
('login.totp_required', 'false', NOW()),
('smtp.host', '', NOW()),
('smtp.port', '587', NOW()),
('smtp.username', '', NOW()),
('smtp.password', '', NOW()),
('smtp.ssl', 'true', NOW()),
('smtp.from', '', NOW());
