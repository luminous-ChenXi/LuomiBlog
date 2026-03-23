-- =============================================
-- LuomiBlog 数据库初始化脚本
-- 版本: V1.0
-- 适用: MySQL 8.0+
-- 字符集: utf8mb4
-- 说明: 完整的LuomiBlog数据库初始化脚本，包含所有表结构和基础数据
-- =============================================

-- 删除已存在的数据库（谨慎使用）
DROP DATABASE IF EXISTS `luomiblog`;

-- 创建数据库
CREATE DATABASE `luomiblog` 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE `luomiblog`;

-- =============================================
-- 1. 权限系统表
-- =============================================

-- 角色表
CREATE TABLE IF NOT EXISTS `roles` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(64) NOT NULL COMMENT '角色编码：visitor/member/blogger/admin',
  `name` VARCHAR(64) NOT NULL COMMENT '角色名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
  `level` INT NOT NULL DEFAULT 0 COMMENT '角色等级（0访客 1会员 10博主 100管理员）',
  `is_system` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否系统内置角色',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_roles_code` (`code`),
  UNIQUE KEY `uk_roles_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS `permissions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(128) NOT NULL COMMENT '权限编码，如 article:create',
  `name` VARCHAR(128) NOT NULL COMMENT '权限名称',
  `type` ENUM('menu','button','api','data') NOT NULL DEFAULT 'api' COMMENT '权限类型',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父权限ID',
  `menu_path` VARCHAR(255) DEFAULT NULL COMMENT '菜单路径',
  `menu_icon` VARCHAR(64) DEFAULT NULL COMMENT '菜单图标',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '权限描述',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permissions_code` (`code`),
  KEY `idx_permissions_parent` (`parent_id`),
  KEY `idx_permissions_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS `role_permissions` (
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`role_id`, `permission_id`),
  KEY `idx_role_permissions_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 管理面板菜单表
CREATE TABLE IF NOT EXISTS `admin_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(64) NOT NULL COMMENT '菜单名称',
  `code` VARCHAR(64) NOT NULL COMMENT '菜单编码',
  `path` VARCHAR(255) NOT NULL COMMENT '路由路径',
  `component` VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
  `icon` VARCHAR(64) DEFAULT NULL COMMENT '菜单图标',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父菜单ID',
  `permission_code` VARCHAR(128) DEFAULT NULL COMMENT '所需权限编码',
  `visible_roles` JSON DEFAULT NULL COMMENT '可见角色列表',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
  `is_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_menu_code` (`code`),
  KEY `idx_admin_menu_parent` (`parent_id`),
  KEY `idx_admin_menu_permission` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理面板菜单表';

-- =============================================
-- 2. 用户相关表
-- =============================================

-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(64) DEFAULT NULL COMMENT '用户名',
  `password` VARCHAR(255) DEFAULT NULL COMMENT 'BCrypt加密密码',
  `nickname` VARCHAR(120) DEFAULT NULL COMMENT '显示昵称',
  `email` VARCHAR(180) DEFAULT NULL COMMENT '邮箱地址',
  `email_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '邮箱是否验证',
  `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
  `website` VARCHAR(255) DEFAULT NULL COMMENT '个人网站',
  `signature` VARCHAR(255) DEFAULT NULL COMMENT '个性签名',
  `location` VARCHAR(120) DEFAULT NULL COMMENT '所在地',
  `bio` TEXT DEFAULT NULL COMMENT '个人简介',
  `role_id` BIGINT NOT NULL DEFAULT 1 COMMENT '角色ID',
  `status` ENUM('active','inactive','banned') NOT NULL DEFAULT 'active' COMMENT '账号状态',
  `settings` JSON DEFAULT NULL COMMENT '用户个性化设置',
  `last_login_ip` VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`),
  UNIQUE KEY `uk_users_email` (`email`),
  KEY `idx_users_role` (`role_id`),
  KEY `idx_users_status` (`status`),
  KEY `idx_users_deleted` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `user_roles` (
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_user_roles_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 访客表
CREATE TABLE IF NOT EXISTS `visitors` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `visitor_id` VARCHAR(64) NOT NULL COMMENT '访客唯一标识',
  `ip_address` VARCHAR(64) NOT NULL COMMENT 'IP地址',
  `user_agent` VARCHAR(512) DEFAULT NULL COMMENT '浏览器UA',
  `referer` VARCHAR(512) DEFAULT NULL COMMENT '来源页面',
  `first_visit_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次访问时间',
  `last_visit_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后访问时间',
  `visit_count` INT NOT NULL DEFAULT 1 COMMENT '访问次数',
  `country` VARCHAR(64) DEFAULT NULL COMMENT '国家',
  `city` VARCHAR(64) DEFAULT NULL COMMENT '城市',
  `is_blocked` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否被封禁',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_visitors_visitor_id` (`visitor_id`),
  KEY `idx_visitors_ip` (`ip_address`),
  KEY `idx_visitors_last_visit` (`last_visit_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客表';

-- =============================================
-- 3. 内容管理表
-- =============================================

-- 分类表
CREATE TABLE IF NOT EXISTS `categories` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '分类名称',
  `slug` VARCHAR(150) NOT NULL COMMENT 'URL标识',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '分类描述',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父分类ID',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
  `article_count` INT NOT NULL DEFAULT 0 COMMENT '文章数量',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_slug` (`slug`),
  KEY `idx_category_parent` (`parent_id`),
  KEY `idx_category_deleted` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章分类表';

-- 标签表
CREATE TABLE IF NOT EXISTS `tags` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(120) NOT NULL COMMENT '标签名称',
  `slug` VARCHAR(180) NOT NULL COMMENT 'URL标识',
  `type` ENUM('user','system','ai') NOT NULL DEFAULT 'user' COMMENT '标签类型',
  `article_count` INT NOT NULL DEFAULT 0 COMMENT '关联文章数',
  `usage_count` INT NOT NULL DEFAULT 0 COMMENT '使用次数',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '标签描述',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tags_slug` (`slug`),
  KEY `idx_tags_type` (`type`),
  KEY `idx_tags_deleted` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- 文章表
CREATE TABLE IF NOT EXISTS `article` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(255) NOT NULL COMMENT '文章标题',
  `slug` VARCHAR(255) NOT NULL COMMENT 'URL标识',
  `summary` VARCHAR(1000) DEFAULT NULL COMMENT '文章摘要',
  `content` LONGTEXT NOT NULL COMMENT '文章内容（Markdown）',
  `status` ENUM('draft','published','archived') NOT NULL DEFAULT 'draft' COMMENT '文章状态',
  `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
  `author_id` BIGINT NOT NULL COMMENT '作者ID',
  `cover_image` VARCHAR(512) DEFAULT NULL COMMENT '封面图片URL',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数',
  `word_count` INT NOT NULL DEFAULT 0 COMMENT '字数统计',
  `is_top` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `is_original` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否原创',
  `allow_comments` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '允许评论',
  `allow_suggestions` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '允许建议',
  `language` VARCHAR(10) NOT NULL DEFAULT 'zh' COMMENT '语言代码',
  `source_url` VARCHAR(512) DEFAULT NULL COMMENT '原文链接（转载时）',
  `password` VARCHAR(64) DEFAULT NULL COMMENT '访问密码',
  `meta_keywords` VARCHAR(500) DEFAULT NULL COMMENT 'SEO关键词',
  `meta_description` VARCHAR(1000) DEFAULT NULL COMMENT 'SEO描述',
  `file_path` VARCHAR(500) DEFAULT NULL COMMENT '本地文件路径',
  `last_sync_at` DATETIME DEFAULT NULL COMMENT '最后同步时间',
  `has_conflict` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否有冲突',
  `conflict_content` LONGTEXT DEFAULT NULL COMMENT '冲突内容',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_slug` (`slug`),
  KEY `idx_article_category` (`category_id`),
  KEY `idx_article_author` (`author_id`),
  KEY `idx_article_status` (`status`),
  KEY `idx_article_top` (`is_top`),
  KEY `idx_article_deleted` (`deleted_at`),
  KEY `idx_article_created` (`created_at`),
  FULLTEXT KEY `ft_article_title` (`title`),
  FULLTEXT KEY `ft_article_content` (`content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- 文章标签关联表
CREATE TABLE IF NOT EXISTS `article_tags` (
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `tag_id` BIGINT NOT NULL COMMENT '标签ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`article_id`, `tag_id`),
  KEY `idx_article_tags_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签关联表';

-- 附件表
CREATE TABLE IF NOT EXISTS `attachments` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT DEFAULT NULL COMMENT '关联文章ID',
  `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `file_name` VARCHAR(255) NOT NULL COMMENT '存储文件名',
  `file_path` VARCHAR(512) NOT NULL COMMENT '文件存储路径',
  `file_url` VARCHAR(512) NOT NULL COMMENT '文件访问URL',
  `file_type` VARCHAR(100) NOT NULL COMMENT '文件MIME类型',
  `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
  `storage_type` ENUM('local','cos','oss') NOT NULL DEFAULT 'local' COMMENT '存储类型',
  `uploader_id` BIGINT DEFAULT NULL COMMENT '上传者ID',
  `download_count` INT NOT NULL DEFAULT 0 COMMENT '下载次数',
  `is_image` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为图片',
  `image_width` INT DEFAULT NULL COMMENT '图片宽度',
  `image_height` INT DEFAULT NULL COMMENT '图片高度',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_attachments_article` (`article_id`),
  KEY `idx_attachments_uploader` (`uploader_id`),
  KEY `idx_attachments_deleted` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件表';

-- 评论表
CREATE TABLE IF NOT EXISTS `article_comments` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID（回复）',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（登录用户）',
  `visitor_id` VARCHAR(64) DEFAULT NULL COMMENT '访客ID（未登录）',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `is_top` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `status` ENUM('pending','approved','rejected') NOT NULL DEFAULT 'approved' COMMENT '审核状态',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` VARCHAR(512) DEFAULT NULL COMMENT '浏览器UA',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_comments_article` (`article_id`),
  KEY `idx_comments_parent` (`parent_id`),
  KEY `idx_comments_user` (`user_id`),
  KEY `idx_comments_status` (`status`),
  KEY `idx_comments_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 评论点赞表
CREATE TABLE IF NOT EXISTS `comment_likes` (
  `comment_id` BIGINT NOT NULL COMMENT '评论ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `visitor_id` VARCHAR(64) DEFAULT NULL COMMENT '访客ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`comment_id`, `user_id`, `visitor_id`),
  KEY `idx_comment_likes_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞表';

-- 文章点赞表
CREATE TABLE IF NOT EXISTS `article_likes` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `visitor_id` VARCHAR(64) DEFAULT NULL COMMENT '访客ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_likes` (`article_id`, `user_id`, `visitor_id`),
  KEY `idx_article_likes_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章点赞表';

-- 文章版本表
CREATE TABLE IF NOT EXISTS `article_versions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `version` INT NOT NULL COMMENT '版本号',
  `title` VARCHAR(255) NOT NULL COMMENT '标题',
  `content` LONGTEXT NOT NULL COMMENT '内容',
  `summary` VARCHAR(1000) DEFAULT NULL COMMENT '摘要',
  `change_log` VARCHAR(500) DEFAULT NULL COMMENT '变更说明',
  `editor_id` BIGINT DEFAULT NULL COMMENT '编辑者ID',
  `editor_type` ENUM('user','system') NOT NULL DEFAULT 'user' COMMENT '编辑者类型',
  `word_count` INT NOT NULL DEFAULT 0 COMMENT '字数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_version` (`article_id`, `version`),
  KEY `idx_article_versions_article` (`article_id`),
  KEY `idx_article_versions_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章版本表';

-- 文章建议表
CREATE TABLE IF NOT EXISTS `article_suggestions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `type` ENUM('typo','content','code','other') NOT NULL DEFAULT 'content' COMMENT '建议类型',
  `content` TEXT NOT NULL COMMENT '建议内容',
  `original_text` VARCHAR(1000) DEFAULT NULL COMMENT '原文内容',
  `suggested_text` VARCHAR(1000) DEFAULT NULL COMMENT '建议修改',
  `reporter_id` BIGINT DEFAULT NULL COMMENT '报告者ID',
  `reporter_email` VARCHAR(180) DEFAULT NULL COMMENT '报告者邮箱',
  `status` ENUM('pending','accepted','rejected') NOT NULL DEFAULT 'pending' COMMENT '处理状态',
  `handler_id` BIGINT DEFAULT NULL COMMENT '处理者ID',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `handle_note` VARCHAR(500) DEFAULT NULL COMMENT '处理备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_suggestions_article` (`article_id`),
  KEY `idx_suggestions_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章建议表';

-- 文章锁表（编辑锁定）
CREATE TABLE IF NOT EXISTS `article_locks` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `user_id` BIGINT NOT NULL COMMENT '锁定用户ID',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `locked_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` DATETIME NOT NULL COMMENT '过期时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_locks_article` (`article_id`),
  KEY `idx_article_locks_user` (`user_id`),
  KEY `idx_article_locks_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章编辑锁表';

-- =============================================
-- 4. 统计与日志表
-- =============================================

-- 用户行为表
CREATE TABLE IF NOT EXISTS `user_behavior` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `visitor_id` VARCHAR(64) DEFAULT NULL COMMENT '访客ID',
  `type` ENUM('view','like','comment','share','favorite','search') NOT NULL COMMENT '行为类型',
  `target_type` ENUM('article','comment','user','tag') NOT NULL COMMENT '目标类型',
  `target_id` BIGINT NOT NULL COMMENT '目标ID',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` VARCHAR(512) DEFAULT NULL COMMENT '浏览器UA',
  `referer` VARCHAR(512) DEFAULT NULL COMMENT '来源页面',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_behavior_user` (`user_id`),
  KEY `idx_behavior_type` (`type`),
  KEY `idx_behavior_target` (`target_type`, `target_id`),
  KEY `idx_behavior_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为表';

-- AI反馈日志表
CREATE TABLE IF NOT EXISTS `ai_feedback_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `article_id` BIGINT DEFAULT NULL COMMENT '关联文章ID',
  `query` TEXT NOT NULL COMMENT '用户查询',
  `response` TEXT NOT NULL COMMENT 'AI响应',
  `feedback_type` ENUM('helpful','not_helpful','report') DEFAULT NULL COMMENT '反馈类型',
  `feedback_content` VARCHAR(500) DEFAULT NULL COMMENT '反馈内容',
  `processing_time` INT DEFAULT NULL COMMENT '处理耗时（毫秒）',
  `model_used` VARCHAR(64) DEFAULT NULL COMMENT '使用的模型',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_feedback_user` (`user_id`),
  KEY `idx_ai_feedback_article` (`article_id`),
  KEY `idx_ai_feedback_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI反馈日志表';

-- AI模型配置表
CREATE TABLE IF NOT EXISTS `ai_model_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(64) NOT NULL COMMENT '配置名称',
  `provider` VARCHAR(64) NOT NULL COMMENT '提供商',
  `model` VARCHAR(64) NOT NULL COMMENT '模型名称',
  `api_key` VARCHAR(255) NOT NULL COMMENT 'API密钥',
  `api_url` VARCHAR(255) DEFAULT NULL COMMENT 'API地址',
  `temperature` DECIMAL(3,2) NOT NULL DEFAULT 0.70 COMMENT '温度参数',
  `max_tokens` INT NOT NULL DEFAULT 2000 COMMENT '最大令牌数',
  `timeout` INT NOT NULL DEFAULT 30 COMMENT '超时时间（秒）',
  `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认',
  `is_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_model_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表';

-- 用户通知表
CREATE TABLE IF NOT EXISTS `user_notifications` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `type` ENUM('system','comment','like','follow','suggestion') NOT NULL COMMENT '通知类型',
  `title` VARCHAR(255) NOT NULL COMMENT '通知标题',
  `content` TEXT DEFAULT NULL COMMENT '通知内容',
  `target_type` VARCHAR(64) DEFAULT NULL COMMENT '目标类型',
  `target_id` BIGINT DEFAULT NULL COMMENT '目标ID',
  `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
  `read_at` DATETIME DEFAULT NULL COMMENT '阅读时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notifications_user` (`user_id`),
  KEY `idx_notifications_read` (`is_read`),
  KEY `idx_notifications_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `system_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` VARCHAR(128) NOT NULL COMMENT '配置键',
  `config_value` TEXT DEFAULT NULL COMMENT '配置值',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '配置说明',
  `is_public` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否公开',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 分享记录表
CREATE TABLE IF NOT EXISTS `share_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `platform` VARCHAR(64) NOT NULL COMMENT '分享平台',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `visitor_id` VARCHAR(64) DEFAULT NULL COMMENT '访客ID',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_share_article` (`article_id`),
  KEY `idx_share_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享记录表';

-- 国际化字符串表
CREATE TABLE IF NOT EXISTS `i18n_strings` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `key` VARCHAR(255) NOT NULL COMMENT '字符串键',
  `lang` VARCHAR(10) NOT NULL COMMENT '语言代码',
  `value` TEXT NOT NULL COMMENT '字符串值',
  `module` VARCHAR(64) DEFAULT 'common' COMMENT '所属模块',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_i18n_key_lang` (`key`, `lang`),
  KEY `idx_i18n_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='国际化字符串表';

-- 访问日志表
CREATE TABLE IF NOT EXISTS `access_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `visitor_id` VARCHAR(64) DEFAULT NULL COMMENT '访客ID',
  `ip_address` VARCHAR(64) NOT NULL COMMENT 'IP地址',
  `method` VARCHAR(10) NOT NULL COMMENT '请求方法',
  `path` VARCHAR(512) NOT NULL COMMENT '请求路径',
  `query` VARCHAR(1024) DEFAULT NULL COMMENT '查询参数',
  `user_agent` VARCHAR(512) DEFAULT NULL COMMENT '浏览器UA',
  `referer` VARCHAR(512) DEFAULT NULL COMMENT '来源页面',
  `status_code` INT NOT NULL COMMENT '响应状态码',
  `response_time` INT DEFAULT NULL COMMENT '响应时间（毫秒）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_access_user` (`user_id`),
  KEY `idx_access_ip` (`ip_address`),
  KEY `idx_access_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访问日志表';

-- 每日统计表
CREATE TABLE IF NOT EXISTS `daily_stats` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stat_date` DATE NOT NULL COMMENT '统计日期',
  `pv` INT NOT NULL DEFAULT 0 COMMENT '页面浏览量',
  `uv` INT NOT NULL DEFAULT 0 COMMENT '独立访客数',
  `new_users` INT NOT NULL DEFAULT 0 COMMENT '新用户数',
  `new_articles` INT NOT NULL DEFAULT 0 COMMENT '新文章数',
  `new_comments` INT NOT NULL DEFAULT 0 COMMENT '新评论数',
  `total_articles` INT NOT NULL DEFAULT 0 COMMENT '文章总数',
  `total_comments` INT NOT NULL DEFAULT 0 COMMENT '评论总数',
  `total_users` INT NOT NULL DEFAULT 0 COMMENT '用户总数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_daily_stats_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日统计表';

-- 文章收藏表
CREATE TABLE IF NOT EXISTS `article_favorites` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `folder_name` VARCHAR(100) DEFAULT '默认收藏夹' COMMENT '收藏夹名称',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_favorites` (`article_id`, `user_id`),
  KEY `idx_favorites_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章收藏表';

-- 用户积分表
CREATE TABLE IF NOT EXISTS `user_coins` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `balance` INT NOT NULL DEFAULT 0 COMMENT '当前余额',
  `total_earned` INT NOT NULL DEFAULT 0 COMMENT '累计获得',
  `total_spent` INT NOT NULL DEFAULT 0 COMMENT '累计消费',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_coins_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分表';

-- 用户签到表
CREATE TABLE IF NOT EXISTS `user_checkins` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `checkin_date` DATE NOT NULL COMMENT '签到日期',
  `consecutive_days` INT NOT NULL DEFAULT 1 COMMENT '连续签到天数',
  `coins_earned` INT NOT NULL DEFAULT 0 COMMENT '获得积分',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_checkins_user_date` (`user_id`, `checkin_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户签到表';

-- 积分交易记录表
CREATE TABLE IF NOT EXISTS `coin_transactions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `type` ENUM('earn','spend') NOT NULL COMMENT '交易类型',
  `amount` INT NOT NULL COMMENT '交易金额',
  `balance` INT NOT NULL COMMENT '交易后余额',
  `source` VARCHAR(64) NOT NULL COMMENT '来源：checkin/article/comment/etc',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '交易说明',
  `target_type` VARCHAR(64) DEFAULT NULL COMMENT '目标类型',
  `target_id` BIGINT DEFAULT NULL COMMENT '目标ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_coin_transactions_user` (`user_id`),
  KEY `idx_coin_transactions_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分交易记录表';

-- 用户资料扩展表
CREATE TABLE IF NOT EXISTS `user_profiles` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `github_id` VARCHAR(64) DEFAULT NULL COMMENT 'GitHub ID',
  `github_username` VARCHAR(64) DEFAULT NULL COMMENT 'GitHub用户名',
  `wechat_id` VARCHAR(64) DEFAULT NULL COMMENT '微信ID',
  `wechat_union_id` VARCHAR(64) DEFAULT NULL COMMENT '微信UnionID',
  `qq_id` VARCHAR(64) DEFAULT NULL COMMENT 'QQ ID',
  `weibo_id` VARCHAR(64) DEFAULT NULL COMMENT '微博ID',
  `google_id` VARCHAR(64) DEFAULT NULL COMMENT 'Google ID',
  `twitter_id` VARCHAR(64) DEFAULT NULL COMMENT 'Twitter ID',
  `email_notification` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '邮件通知',
  `comment_notification` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '评论通知',
  `like_notification` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '点赞通知',
  `follow_notification` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '关注通知',
  `system_notification` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '系统通知',
  `theme` VARCHAR(32) DEFAULT 'light' COMMENT '主题设置',
  `font_size` VARCHAR(16) DEFAULT 'medium' COMMENT '字体大小',
  `language` VARCHAR(10) DEFAULT 'zh' COMMENT '语言偏好',
  `timezone` VARCHAR(64) DEFAULT 'Asia/Shanghai' COMMENT '时区',
  `privacy_level` ENUM('public','friends','private') DEFAULT 'public' COMMENT '隐私级别',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_profiles_user` (`user_id`),
  UNIQUE KEY `uk_user_profiles_github` (`github_id`),
  UNIQUE KEY `uk_user_profiles_wechat` (`wechat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资料扩展表';

-- 文章打赏表
CREATE TABLE IF NOT EXISTS `article_rewards` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `is_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用打赏',
  `reward_count` INT NOT NULL DEFAULT 0 COMMENT '打赏次数',
  `reward_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '打赏金额',
  `wechat_qr` VARCHAR(512) DEFAULT NULL COMMENT '微信收款码',
  `alipay_qr` VARCHAR(512) DEFAULT NULL COMMENT '支付宝收款码',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_rewards_article` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章打赏配置表';

-- 打赏记录表
CREATE TABLE IF NOT EXISTS `reward_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '打赏用户ID',
  `visitor_id` VARCHAR(64) DEFAULT NULL COMMENT '访客ID',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '打赏金额',
  `message` VARCHAR(255) DEFAULT NULL COMMENT '留言',
  `payment_method` VARCHAR(32) DEFAULT NULL COMMENT '支付方式',
  `status` ENUM('pending','success','failed') NOT NULL DEFAULT 'pending' COMMENT '支付状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_reward_article` (`article_id`),
  KEY `idx_reward_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打赏记录表';

-- 评论@提及表
CREATE TABLE IF NOT EXISTS `comment_mentions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `comment_id` BIGINT NOT NULL COMMENT '评论ID',
  `mentioned_user_id` BIGINT NOT NULL COMMENT '被提及用户ID',
  `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_mentions` (`comment_id`, `mentioned_user_id`),
  KEY `idx_mentions_user` (`mentioned_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论@提及表';

-- 积分打赏记录表
CREATE TABLE IF NOT EXISTS `coin_reward_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `user_id` BIGINT NOT NULL COMMENT '打赏用户ID',
  `author_id` BIGINT NOT NULL COMMENT '作者ID',
  `amount` INT NOT NULL COMMENT '打赏积分',
  `message` VARCHAR(255) DEFAULT NULL COMMENT '留言',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_coin_reward_article` (`article_id`),
  KEY `idx_coin_reward_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分打赏记录表';

-- =============================================
-- 5. 添加外键约束
-- =============================================

-- 角色权限外键
ALTER TABLE `role_permissions` 
  ADD CONSTRAINT `fk_role_permissions_roles` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_role_permissions_permissions` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE;

-- 用户角色外键
ALTER TABLE `user_roles`
  ADD CONSTRAINT `fk_user_roles_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_user_roles_roles` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE;

-- 文章外键
ALTER TABLE `article`
  ADD CONSTRAINT `fk_article_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_article_author` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- 文章标签外键
ALTER TABLE `article_tags`
  ADD CONSTRAINT `fk_article_tags_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_article_tags_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE;

-- 评论外键
ALTER TABLE `article_comments`
  ADD CONSTRAINT `fk_comments_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_comments_parent` FOREIGN KEY (`parent_id`) REFERENCES `article_comments` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

-- 附件外键
ALTER TABLE `attachments`
  ADD CONSTRAINT `fk_attachments_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_attachments_uploader` FOREIGN KEY (`uploader_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

-- 文章版本外键
ALTER TABLE `article_versions`
  ADD CONSTRAINT `fk_article_versions_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_article_versions_editor` FOREIGN KEY (`editor_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

-- 文章建议外键
ALTER TABLE `article_suggestions`
  ADD CONSTRAINT `fk_suggestions_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_suggestions_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_suggestions_handler` FOREIGN KEY (`handler_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

-- 文章锁外键
ALTER TABLE `article_locks`
  ADD CONSTRAINT `fk_article_locks_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_article_locks_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- 文章点赞外键
ALTER TABLE `article_likes`
  ADD CONSTRAINT `fk_article_likes_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_article_likes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- 评论点赞外键
ALTER TABLE `comment_likes`
  ADD CONSTRAINT `fk_comment_likes_comment` FOREIGN KEY (`comment_id`) REFERENCES `article_comments` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_comment_likes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- 文章收藏外键
ALTER TABLE `article_favorites`
  ADD CONSTRAINT `fk_favorites_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_favorites_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- 分享记录外键
ALTER TABLE `share_records`
  ADD CONSTRAINT `fk_share_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_share_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

-- 用户积分外键
ALTER TABLE `user_coins`
  ADD CONSTRAINT `fk_user_coins_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- 用户签到外键
ALTER TABLE `user_checkins`
  ADD CONSTRAINT `fk_checkins_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- 积分交易外键
ALTER TABLE `coin_transactions`
  ADD CONSTRAINT `fk_coin_transactions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- 用户资料外键
ALTER TABLE `user_profiles`
  ADD CONSTRAINT `fk_user_profiles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- 文章打赏外键
ALTER TABLE `article_rewards`
  ADD CONSTRAINT `fk_article_rewards_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE;

-- 打赏记录外键
ALTER TABLE `reward_records`
  ADD CONSTRAINT `fk_reward_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_reward_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

-- 评论提及外键
ALTER TABLE `comment_mentions`
  ADD CONSTRAINT `fk_mentions_comment` FOREIGN KEY (`comment_id`) REFERENCES `article_comments` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_mentions_user` FOREIGN KEY (`mentioned_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- 积分打赏外键
ALTER TABLE `coin_reward_records`
  ADD CONSTRAINT `fk_coin_reward_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_coin_reward_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_coin_reward_author` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- 通知外键
ALTER TABLE `user_notifications`
  ADD CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- AI反馈外键
ALTER TABLE `ai_feedback_log`
  ADD CONSTRAINT `fk_ai_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_ai_feedback_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE SET NULL;

-- 用户行为外键
ALTER TABLE `user_behavior`
  ADD CONSTRAINT `fk_behavior_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

-- 访问日志外键
ALTER TABLE `access_log`
  ADD CONSTRAINT `fk_access_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

-- =============================================
-- 6. 插入基础数据
-- =============================================

-- 插入默认角色
INSERT INTO `roles` (`code`, `name`, `description`, `level`, `is_system`) VALUES
('visitor', '访客', '未登录访客', 0, 1),
('member', '会员', '普通注册用户', 1, 1),
('blogger', '博主', '内容创作者', 10, 1),
('admin', '管理员', '系统管理员', 100, 1);

-- 插入默认分类
INSERT INTO `categories` (`name`, `slug`, `description`, `sort_order`) VALUES
('未分类', 'uncategorized', '默认分类', 0),
('技术', 'technology', '技术相关文章', 1),
('生活', 'life', '生活随笔', 2),
('随笔', 'notes', '随想笔记', 3);

-- 插入系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `description`, `is_public`) VALUES
('site.name', 'LuomiBlog', '站点名称', 1),
('site.description', '一个基于 Astro + Vue + Spring Boot 的现代化博客系统', '站点描述', 1),
('site.keywords', '博客,技术,生活', '站点关键词', 1),
('site.logo', '/logo.png', '站点Logo', 1),
('site.favicon', '/favicon.ico', '站点图标', 1),
('site.icp', '', 'ICP备案号', 1),
('site.gongan', '', '公安备案号', 1),
('comment.enabled', 'true', '是否开启评论', 0),
('comment.need_approval', 'false', '评论是否需要审核', 0),
('article.allow_guest_comment', 'true', '允许游客评论', 0),
('ai.enabled', 'true', '是否启用AI功能', 0),
('ai.model', 'qwen-turbo', '默认AI模型', 0),
('upload.max_size', '10485760', '最大上传大小（字节）', 0),
('upload.allowed_types', 'image/*,application/pdf', '允许上传的文件类型', 0);

-- 插入默认管理员菜单
INSERT INTO `admin_menu` (`name`, `code`, `path`, `icon`, `sort_order`, `permission_code`) VALUES
('仪表盘', 'dashboard', '/admin/dashboard', 'dashboard', 1, 'admin:dashboard'),
('文章管理', 'articles', '/admin/articles', 'article', 2, 'admin:articles'),
('分类管理', 'categories', '/admin/categories', 'category', 3, 'admin:categories'),
('标签管理', 'tags', '/admin/tags', 'tag', 4, 'admin:tags'),
('评论管理', 'comments', '/admin/comments', 'comment', 5, 'admin:comments'),
('用户管理', 'users', '/admin/users', 'user', 6, 'admin:users'),
('附件管理', 'attachments', '/admin/attachments', 'attachment', 7, 'admin:attachments'),
('系统设置', 'settings', '/admin/settings', 'settings', 8, 'admin:settings');

-- =============================================
-- 初始化完成
-- =============================================
