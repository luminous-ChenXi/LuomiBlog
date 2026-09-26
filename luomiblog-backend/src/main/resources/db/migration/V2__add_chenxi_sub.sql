-- =============================================
-- 辰汐通行证登录支持：users 表新增 chenxi_sub 字段（手动执行）
-- 适用数据库：MySQL 8.0+
--
-- ⚠️ 注意：本脚本必须且只能执行一次！
--    1. MySQL 8.0 不支持 `ADD COLUMN IF NOT EXISTS` 语法（MariaDB 专用），
--       重复执行会因字段/索引已存在而直接报错。
--    2. 全新安装无需执行本脚本：安装向导执行的 db/schema.sql 已包含
--       chenxi_sub 字段与唯一索引；仅存量数据库升级时需要手动执行一次。
--
-- 字段说明：
--   chenxi_sub 是辰汐通行证（标准 OIDC）下发的全局唯一用户标识（sub），
--   是本站"影子账号"的锚点；本地注册的密码账号该字段为 NULL。
--   MySQL 的唯一索引允许多个 NULL 值，因此不影响普通账号。
-- =============================================

ALTER TABLE `users`
  ADD COLUMN `chenxi_sub` VARCHAR(64) DEFAULT NULL COMMENT '辰汐通行证sub（影子账号锚点）',
  ADD UNIQUE KEY `uk_users_chenxi_sub` (`chenxi_sub`);
