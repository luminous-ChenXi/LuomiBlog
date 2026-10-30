-- =============================================
-- V5：登录防爆破状态持久化（审计 P1-4）
-- 适用数据库：MySQL 8.0+
--
-- ⚠️ 注意：本脚本为存量数据库手工升级脚本（无 Flyway，需人工执行一次）；
--    全新安装请直接执行 db/schema.sql，无需运行本脚本。
--
-- 背景：原防爆破锁定/失败计数全部存内存（MemoryCacheService）：
--   1) 重启即清零，攻击者可等一次重启或主动打挂进程重置计数；
--   2) 账号锁键含 IP（账号:IP），换 IP 即绕过账号锁；
--   3) 无 IP 维度独立锁定，无升级拉黑。
-- 本表三维度互补封堵：
--   ACCOUNT_IP：单账号+单 IP（沿用原参数 10 次/30min → 锁 30min）；
--   ACCOUNT：跨 IP 账号维度（20 次/60min → 锁 30min，换 IP 不再绕过）；
--   IP_ONLY：纯 IP 维度（30 次/60min，锁定时长按累计锁定次数逐次翻倍、24h 封顶；
--   刻意不做 365 天永久拉黑——博客访客常共享出口 NAT，永久封 IP 误伤不可控）。
-- 锁定到期自动解（locked_until 判定）；过期行由登录失败路径惰性清理，防表膨胀。
--
-- 兼容性说明：原内存中的存量锁定/失败计数不迁移，升级部署后清零属预期。
--
-- 幂等性说明：CREATE TABLE IF NOT EXISTS，重复执行安全。
-- 字段与 entity/AuthLockState.java 逐列对齐（BUG-086：ddl-auto=update 与手写 SQL 双源约束，
-- 枚举值顺序与 Java enum 声明顺序一致，勿调整）。
-- =============================================

CREATE TABLE IF NOT EXISTS `auth_lock_states` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `lock_scope` ENUM('ACCOUNT_IP','ACCOUNT','IP_ONLY') NOT NULL DEFAULT 'ACCOUNT_IP' COMMENT '锁定维度：账号+IP/跨IP账号/纯IP',
  `identifier` VARCHAR(255) NOT NULL COMMENT '维度键：ACCOUNT_IP=账号:IP 原串；ACCOUNT=账号小写；IP_ONLY=客户端IP',
  `failed_count` INT NOT NULL DEFAULT 0 COMMENT '当前失败计数窗口内的失败次数',
  `window_started_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '失败计数窗口起点，过期后计数惰性清零重开',
  `locked_until` DATETIME DEFAULT NULL COMMENT '锁定截止时间（NULL/已过期=未锁定，到期自动解）',
  `lock_count` INT NOT NULL DEFAULT 0 COMMENT '累计锁定次数（IP_ONLY 升级锁依据）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_lock_scope_identifier` (`lock_scope`, `identifier`),
  KEY `idx_auth_lock_locked_until` (`locked_until`),
  KEY `idx_auth_lock_window` (`window_started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录防爆破锁定状态表（DB 持久化，重启不清零）';
