-- =============================================
-- LuomiBlog 数据库初始化脚本
-- 版本: V1.2
-- 适用: MySQL 8.0+
-- 字符集 utf8mb4
-- =============================================

-- =============================================
-- 1. 权限系统表
-- =============================================

CREATE TABLE IF NOT EXISTS `roles` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(64) NOT NULL COMMENT '角色编码：visitor/member/blogger/admin',
  `name` VARCHAR(64) NOT NULL COMMENT '角色名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
  `level` INT NOT NULL DEFAULT 0 COMMENT '角色等级（0访客 1会员 10博主 100管理员）访客 1会员 10博主 100管理员）',
  `is_system` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否系统内置角色',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_roles_code` (`code`),
  UNIQUE KEY `uk_roles_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

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

CREATE TABLE IF NOT EXISTS `role_permissions` (
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`role_id`, `permission_id`),
  KEY `idx_role_permissions_permission` (`permission_id`),
  CONSTRAINT `fk_role_permissions_roles` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_role_permissions_permissions` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

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
  KEY `idx_users_deleted` (`deleted_at`),
  CONSTRAINT `fk_users_roles` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户主表';

CREATE TABLE IF NOT EXISTS `user_roles` (
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_user_roles_role` (`role_id`),
  CONSTRAINT `fk_user_roles_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_roles_roles` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS `visitors` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `visitor_id` VARCHAR(64) NOT NULL COMMENT '访客唯一标识',
  `ip_address` VARCHAR(64) NOT NULL COMMENT 'IP地址',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'User-Agent',
  `device_type` VARCHAR(32) DEFAULT NULL COMMENT '设备类型',
  `browser` VARCHAR(64) DEFAULT NULL COMMENT '浏览器',
  `os` VARCHAR(64) DEFAULT NULL COMMENT '操作系统',
  `country` VARCHAR(64) DEFAULT NULL COMMENT '国家',
  `city` VARCHAR(64) DEFAULT NULL COMMENT '城市',
  `first_visit_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次访问时间',
  `last_visit_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后访问时间',
  `visit_count` INT NOT NULL DEFAULT 1 COMMENT '访问次数',
  `expired_at` DATETIME NOT NULL COMMENT '访客标识过期时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_visitors_visitor_id` (`visitor_id`),
  KEY `idx_visitors_ip` (`ip_address`),
  KEY `idx_visitors_expired` (`expired_at`),
  KEY `idx_visitors_last_visit` (`last_visit_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客表';

-- =============================================
-- 3. 内容管理表
-- =============================================

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
  UNIQUE KEY `uk_tags_name` (`name`),
  UNIQUE KEY `uk_tags_slug` (`slug`),
  KEY `idx_tags_type` (`type`),
  KEY `idx_tags_deleted` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

CREATE TABLE IF NOT EXISTS `article` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(255) NOT NULL COMMENT '文章标题',
  `slug` VARCHAR(191) NOT NULL COMMENT 'URL唯一标识',
  `content` LONGTEXT NOT NULL COMMENT 'MD/MDX完整内容',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '人工编写摘要',
  `ai_summary` TEXT DEFAULT NULL COMMENT 'AI生成摘要',
  `knowledge_points` VARCHAR(512) DEFAULT NULL COMMENT 'AI拆解知识点',
  `author_id` BIGINT DEFAULT NULL COMMENT '作者ID',
  `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
  `cover_image_id` BIGINT DEFAULT NULL COMMENT '封面图附件ID',
  `language` ENUM('zh','en','ja') NOT NULL DEFAULT 'zh' COMMENT '语言',
  `status` ENUM('draft','published','archived') NOT NULL DEFAULT 'draft' COMMENT '状态',
  `is_top` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `view_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `like_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞次数',
  `comment_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '评论数量',
  `word_count` INT UNSIGNED DEFAULT NULL COMMENT '字数统计',
  `reading_time` INT UNSIGNED DEFAULT NULL COMMENT '预计阅读时间（分钟）',
  `version` VARCHAR(20) DEFAULT '1.0.0' COMMENT '当前版本号',
  `allow_suggestions` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否允许编辑建议',
  `allow_comments` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许评论',
  `sync_bailian` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否同步到百炼',
  `bailian_doc_id` VARCHAR(128) DEFAULT NULL COMMENT '百炼文档标识',
  `is_original` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否原创',
  `source_url` VARCHAR(512) DEFAULT NULL COMMENT '原文链接（转载时填写）',
  `source_name` VARCHAR(120) DEFAULT NULL COMMENT '文章来源名称',
  `difficulty_level` TINYINT UNSIGNED DEFAULT 1 COMMENT '难度等级 1-5',
  `is_recommended` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否推荐',
  `seo_title` VARCHAR(255) DEFAULT NULL COMMENT 'SEO标题',
  `seo_keywords` VARCHAR(500) DEFAULT NULL COMMENT 'SEO关键词',
  `seo_description` VARCHAR(1000) DEFAULT NULL COMMENT 'SEO描述',
  `content_hash` VARCHAR(64) DEFAULT NULL COMMENT '内容SHA256哈希（用于双源冲突检测）',
  `file_path` VARCHAR(512) DEFAULT NULL COMMENT 'MD文件路径（双源架构）',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
  `published_at` DATETIME DEFAULT NULL COMMENT '发布时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_slug` (`slug`),
  KEY `idx_article_author_status` (`author_id`, `status`),
  KEY `idx_article_category_status` (`category_id`, `status`),
  KEY `idx_article_language_status` (`language`, `status`),
  KEY `idx_article_published` (`status`, `published_at`),
  KEY `idx_article_top_published` (`is_top`, `published_at`),
  KEY `idx_article_deleted` (`deleted_at`),
  FULLTEXT KEY `ft_article_title` (`title`),
  FULLTEXT KEY `ft_article_content` (`content`, `ai_summary`),
  CONSTRAINT `fk_article_users` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_article_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章主表';

CREATE TABLE IF NOT EXISTS `article_tags` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `tag_id` BIGINT NOT NULL COMMENT '标签ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_tag` (`article_id`, `tag_id`),
  KEY `idx_article_tags_tag` (`tag_id`),
  KEY `idx_article_tags_article` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签关联表';

CREATE TABLE IF NOT EXISTS `attachments` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `filename` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `storage_path` VARCHAR(512) NOT NULL COMMENT '存储路径',
  `file_url` VARCHAR(512) NOT NULL COMMENT '访问URL',
  `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
  `mime_type` VARCHAR(128) DEFAULT NULL COMMENT 'MIME类型',
  `file_type` ENUM('image','video','audio','document','other') NOT NULL DEFAULT 'other' COMMENT '文件类型',
  `biz_type` ENUM('avatar','article_cover','comment_image','system') NOT NULL COMMENT '业务类型',
  `biz_id` BIGINT DEFAULT NULL COMMENT '业务ID',
  `uploader_id` BIGINT DEFAULT NULL COMMENT '上传者ID',
  `width` INT DEFAULT NULL COMMENT '图片宽度',
  `height` INT DEFAULT NULL COMMENT '图片高度',
  `is_violation` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否违规',
  `storage_provider` VARCHAR(32) DEFAULT 'local' COMMENT '存储提供商',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_attachments_biz` (`biz_type`, `biz_id`),
  KEY `idx_attachments_uploader` (`uploader_id`),
  KEY `idx_attachments_type` (`file_type`),
  KEY `idx_attachments_deleted` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件表';

-- =============================================
-- 4. 评论与互动表
-- =============================================

CREATE TABLE IF NOT EXISTS `article_comments` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID',
  `root_id` BIGINT DEFAULT NULL COMMENT '根评论ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '评论者ID',
  `visitor_id` VARCHAR(64) DEFAULT NULL COMMENT '访客标识',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `html_content` TEXT DEFAULT NULL COMMENT '渲染后的HTML',
  `like_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数',
  `reply_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '回复数',
  `is_top` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `status` ENUM('pending','approved','rejected','deleted') NOT NULL DEFAULT 'pending' COMMENT '状态',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'User-Agent',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_comments_article_status` (`article_id`, `status`),
  KEY `idx_comments_parent` (`parent_id`),
  KEY `idx_comments_root` (`root_id`),
  KEY `idx_comments_user` (`user_id`),
  KEY `idx_comments_visitor` (`visitor_id`),
  KEY `idx_comments_created` (`created_at`),
  KEY `idx_comments_deleted` (`deleted_at`),
  CONSTRAINT `fk_comments_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comments_parent` FOREIGN KEY (`parent_id`) REFERENCES `article_comments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comments_root` FOREIGN KEY (`root_id`) REFERENCES `article_comments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章评论表';

CREATE TABLE IF NOT EXISTS `comment_likes` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `comment_id` BIGINT NOT NULL COMMENT '评论ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `visitor_id` VARCHAR(64) DEFAULT NULL COMMENT '访客标识',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_likes_user` (`comment_id`, `user_id`),
  UNIQUE KEY `uk_comment_likes_visitor` (`comment_id`, `visitor_id`),
  KEY `idx_comment_likes_comment` (`comment_id`),
  CONSTRAINT `fk_comment_likes_comment` FOREIGN KEY (`comment_id`) REFERENCES `article_comments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_likes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞数';

CREATE TABLE IF NOT EXISTS `article_likes` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `visitor_id` VARCHAR(64) DEFAULT NULL COMMENT '访客标识',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_likes_user` (`article_id`, `user_id`),
  UNIQUE KEY `uk_article_likes_visitor` (`article_id`, `visitor_id`),
  KEY `idx_article_likes_article` (`article_id`),
  KEY `idx_article_likes_created` (`created_at`),
  CONSTRAINT `fk_article_likes_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_article_likes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章点赞数';

-- =============================================
-- 5. 文章版本与协作编辑表
-- =============================================

CREATE TABLE IF NOT EXISTS `article_versions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `version` VARCHAR(20) NOT NULL COMMENT '版本号（如"1.0.0"）',
  `title` VARCHAR(255) NOT NULL COMMENT '文章标题',
  `content` LONGTEXT NOT NULL COMMENT 'MD/MDX完整内容',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  `change_log` TEXT DEFAULT NULL COMMENT '变更说明',
  `diff_content` JSON DEFAULT NULL COMMENT '差异内容 {"old":"xxx","new":"xxx"}',
  `editor_id` BIGINT DEFAULT NULL COMMENT '编辑者ID',
  `editor_type` ENUM('author','suggester','system') NOT NULL DEFAULT 'author' COMMENT '编辑者类型',
  `word_count` INT UNSIGNED DEFAULT NULL COMMENT '字数统计',
  `is_rollback` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为回滚版本',
  `rollback_from_version` VARCHAR(20) DEFAULT NULL COMMENT '回滚来源版本号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_versions_article_version` (`article_id`, `version`),
  KEY `idx_versions_article` (`article_id`),
  KEY `idx_versions_editor` (`editor_id`),
  KEY `idx_versions_created` (`created_at`),
  CONSTRAINT `fk_versions_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_versions_editor` FOREIGN KEY (`editor_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章版本历史表';

CREATE TABLE IF NOT EXISTS `article_suggestions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `suggester_id` BIGINT DEFAULT NULL COMMENT '建议者ID',
  `suggester_name` VARCHAR(64) DEFAULT NULL COMMENT '建议者昵称（未登录访客）',
  `suggester_email` VARCHAR(180) DEFAULT NULL COMMENT '建议者邮箱（未登录访客）',
  `type` ENUM('typo','content','format','other') NOT NULL DEFAULT 'content' COMMENT '建议类型',
  `chapter_id` VARCHAR(64) DEFAULT NULL COMMENT '章节ID（定位修改位置）',
  `position` INT DEFAULT NULL COMMENT '字符位置',
  `original_text` TEXT DEFAULT NULL COMMENT '原文内容',
  `suggested_text` TEXT NOT NULL COMMENT '建议修改内容',
  `reason` TEXT DEFAULT NULL COMMENT '修改理由',
  `status` ENUM('pending','approved','rejected','applied') NOT NULL DEFAULT 'pending' COMMENT '审核状态',
  `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核者ID',
  `review_comment` TEXT DEFAULT NULL COMMENT '审核意见',
  `applied_version` VARCHAR(20) DEFAULT NULL COMMENT '应用到的版本号',
  `reviewed_at` DATETIME DEFAULT NULL COMMENT '审核时间',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_suggestions_article_status` (`article_id`, `status`),
  KEY `idx_suggestions_suggester` (`suggester_id`),
  KEY `idx_suggestions_status` (`status`),
  KEY `idx_suggestions_created` (`created_at`),
  CONSTRAINT `fk_suggestions_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_suggestions_suggester` FOREIGN KEY (`suggester_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_suggestions_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章编辑建议表';

CREATE TABLE IF NOT EXISTS `article_locks` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `user_id` BIGINT NOT NULL COMMENT '锁定者ID',
  `lock_token` VARCHAR(64) NOT NULL COMMENT '锁定令牌',
  `locked_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '锁定时间',
  `expire_at` DATETIME NOT NULL COMMENT '过期时间',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_locks_article` (`article_id`),
  KEY `idx_locks_user` (`user_id`),
  KEY `idx_locks_expire` (`expire_at`),
  CONSTRAINT `fk_locks_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_locks_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章编辑锁表';

-- =============================================
-- 6. AI系统表
-- =============================================

CREATE TABLE IF NOT EXISTS `user_behavior` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `visitor_id` VARCHAR(64) DEFAULT NULL COMMENT '访客标识',
  `behavior_type` ENUM('VIEW','LIKE','SHARE','AI_ASK','AI_FEEDBACK','SEARCH','COMMENT') NOT NULL COMMENT '行为类型',
  `article_id` BIGINT DEFAULT NULL COMMENT '关联文章ID',
  `comment_id` BIGINT DEFAULT NULL COMMENT '关联评论ID',
  `ai_answer_id` VARCHAR(64) DEFAULT NULL COMMENT '百炼工作流ID',
  `feedback_type` ENUM('helpful','not_helpful','suggestion') DEFAULT NULL COMMENT '反馈类型',
  `feedback_tags` VARCHAR(255) DEFAULT NULL COMMENT '反馈标签',
  `feedback_content` TEXT DEFAULT NULL COMMENT '反馈内容',
  `query_text` VARCHAR(500) DEFAULT NULL COMMENT '搜索/提问内容',
  `answer_text` TEXT DEFAULT NULL COMMENT 'AI回答内容',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'User-Agent',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_behavior_user_type` (`user_id`, `behavior_type`),
  KEY `idx_behavior_article_type` (`article_id`, `behavior_type`),
  KEY `idx_behavior_visitor` (`visitor_id`),
  KEY `idx_behavior_ai_answer` (`ai_answer_id`),
  KEY `idx_behavior_feedback_type` (`feedback_type`),
  KEY `idx_behavior_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为表';

CREATE TABLE IF NOT EXISTS `ai_feedback_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `behavior_id` BIGINT NOT NULL COMMENT '关联行为ID',
  `article_id` BIGINT NOT NULL COMMENT '关联文章ID',
  `ai_answer_id` VARCHAR(64) NOT NULL COMMENT '百炼工作流ID',
  `feedback_type` ENUM('helpful','not_helpful') NOT NULL COMMENT '反馈类型',
  `feedback_tags` JSON DEFAULT NULL COMMENT '反馈标签数组',
  `feedback_content` TEXT DEFAULT NULL COMMENT '反馈内容',
  `processed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已处理',
  `processed_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `bailian_notified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否通知百炼',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_feedback_behavior` (`behavior_id`),
  KEY `idx_feedback_article` (`article_id`),
  KEY `idx_feedback_ai_answer` (`ai_answer_id`),
  KEY `idx_feedback_processed` (`processed`),
  CONSTRAINT `fk_feedback_behavior` FOREIGN KEY (`behavior_id`) REFERENCES `user_behavior` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_feedback_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI反馈日志表';

CREATE TABLE IF NOT EXISTS `ai_model_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(128) NOT NULL COMMENT '配置名称',
  `model_type` ENUM('bailian','openai','claude','local') NOT NULL DEFAULT 'bailian' COMMENT '模型类型',
  `model_name` VARCHAR(128) NOT NULL COMMENT '模型名称',
  `api_key` VARCHAR(512) NOT NULL COMMENT 'API密钥（加密存储）',
  `api_url` VARCHAR(512) DEFAULT NULL COMMENT 'API地址',
  `timeout` INT NOT NULL DEFAULT 30 COMMENT '超时时间（秒）',
  `max_tokens` INT DEFAULT NULL COMMENT '最大Token数',
  `temperature` DECIMAL(3,2) DEFAULT 0.7 COMMENT '温度参数',
  `quota_daily` INT DEFAULT NULL COMMENT '每日配额限制',
  `quota_used` INT NOT NULL DEFAULT 0 COMMENT '已使用配额',
  `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认配置',
  `status` ENUM('active','inactive','error') NOT NULL DEFAULT 'active' COMMENT '状态',
  `ext_config` JSON DEFAULT NULL COMMENT '扩展配置',
  `last_error` TEXT DEFAULT NULL COMMENT '最后错误信息',
  `last_used_at` DATETIME DEFAULT NULL COMMENT '最后使用时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_config_name` (`name`),
  KEY `idx_ai_config_type` (`model_type`),
  KEY `idx_ai_config_status` (`status`),
  KEY `idx_ai_config_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表';

-- =============================================
-- 7. 系统配置表
-- =============================================

CREATE TABLE IF NOT EXISTS `user_notifications` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '接收者用户ID',
  `sender_id` BIGINT DEFAULT NULL COMMENT '发送者ID',
  `notify_type` ENUM('system','comment_reply','suggestion_approved','suggestion_rejected','article_published','feedback_processed') NOT NULL COMMENT '通知类型',
  `related_id` BIGINT DEFAULT NULL COMMENT '关联ID',
  `title` VARCHAR(255) NOT NULL COMMENT '通知标题',
  `content` TEXT DEFAULT NULL COMMENT '通知内容',
  `action_url` VARCHAR(512) DEFAULT NULL COMMENT '操作链接',
  `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
  `read_at` DATETIME DEFAULT NULL COMMENT '阅读时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notifications_user` (`user_id`),
  KEY `idx_notifications_type` (`notify_type`),
  KEY `idx_notifications_read` (`is_read`),
  KEY `idx_notifications_created` (`created_at`),
  CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_notifications_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知表';

CREATE TABLE IF NOT EXISTS `system_config` (
  `id` BIGINT PRIMARY KEY,
  `site_name` VARCHAR(120) NOT NULL DEFAULT 'LuomiBlog' COMMENT '站点名称',
  `site_description` VARCHAR(500) DEFAULT NULL COMMENT '站点描述',
  `site_logo` VARCHAR(512) DEFAULT NULL COMMENT '站点Logo',
  `site_favicon` VARCHAR(512) DEFAULT NULL COMMENT '站点Favicon',
  `default_language` ENUM('zh','en','ja') NOT NULL DEFAULT 'zh' COMMENT '默认语言',
  `default_theme` ENUM('light','dark','auto') NOT NULL DEFAULT 'auto' COMMENT '默认主题',
  `registration_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否开放注册',
  `comment_audit` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '评论是否需要审核',
  `visitor_comment` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否允许访客评论',
  `ai_moderation_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用AI审核',
  `max_upload_size` BIGINT NOT NULL DEFAULT 5242880 COMMENT '最大上传大小（字节）',
  `max_image_width` INT DEFAULT 2048 COMMENT '图片最大宽度',
  `max_image_height` INT DEFAULT 2048 COMMENT '图片最大高度',
  `icp` VARCHAR(100) DEFAULT NULL COMMENT 'ICP备案号',
  `analytics_code` TEXT DEFAULT NULL COMMENT '统计代码',
  `custom_css` TEXT DEFAULT NULL COMMENT '自定义CSS',
  `custom_js` TEXT DEFAULT NULL COMMENT '自定义JS',
  `seo_title` VARCHAR(255) DEFAULT NULL COMMENT 'SEO标题',
  `seo_keywords` VARCHAR(500) DEFAULT NULL COMMENT 'SEO关键词',
  `seo_description` VARCHAR(1000) DEFAULT NULL COMMENT 'SEO描述',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT DEFAULT NULL COMMENT '最后更新者ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

CREATE TABLE IF NOT EXISTS `share_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `platform` ENUM('twitter','wechat','weibo','copy','other') NOT NULL COMMENT '分享平台',
  `share_summary` VARCHAR(500) DEFAULT NULL COMMENT '分享摘要',
  `share_url` VARCHAR(512) DEFAULT NULL COMMENT '分享链接',
  `visitor_id` VARCHAR(64) DEFAULT NULL COMMENT '访客标识',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_share_article` (`article_id`),
  KEY `idx_share_platform` (`platform`),
  KEY `idx_share_user` (`user_id`),
  KEY `idx_share_created` (`created_at`),
  CONSTRAINT `fk_share_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_share_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享记录表';

CREATE TABLE IF NOT EXISTS `i18n_strings` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `key` VARCHAR(255) NOT NULL COMMENT '字符串键名',
  `language` ENUM('zh','en','ja') NOT NULL DEFAULT 'zh' COMMENT '语言',
  `value` TEXT NOT NULL COMMENT '翻译内容',
  `context` VARCHAR(100) DEFAULT NULL COMMENT '使用场景',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_i18n_key_language` (`key`, `language`),
  KEY `idx_i18n_language` (`language`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='国际化字符串表';

-- =============================================
-- 8. 统计系统表
-- =============================================

CREATE TABLE IF NOT EXISTS `access_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `visitor_id` VARCHAR(64) NOT NULL COMMENT '访客标识',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `page_path` VARCHAR(255) NOT NULL COMMENT '页面路径',
  `referrer` VARCHAR(500) DEFAULT NULL COMMENT '来源页面',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'User-Agent',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  `country` VARCHAR(50) DEFAULT NULL COMMENT '国家',
  `city` VARCHAR(100) DEFAULT NULL COMMENT '城市',
  `device_type` ENUM('desktop','mobile','tablet') DEFAULT 'desktop' COMMENT '设备类型',
  `browser` VARCHAR(64) DEFAULT NULL COMMENT '浏览器',
  `os` VARCHAR(64) DEFAULT NULL COMMENT '操作系统',
  `enter_time` DATETIME NOT NULL COMMENT '页面进入时间',
  `leave_time` DATETIME DEFAULT NULL COMMENT '页面离开时间',
  `duration` INT UNSIGNED DEFAULT 0 COMMENT '停留时长（秒）',
  `is_bounce` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否跳出',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_access_date` (`enter_time`),
  KEY `idx_access_page` (`page_path`, `enter_time`),
  KEY `idx_access_visitor` (`visitor_id`, `enter_time`),
  KEY `idx_access_session` (`session_id`, `enter_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访问日志表';

CREATE TABLE IF NOT EXISTS `daily_stats` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stat_date` DATE NOT NULL COMMENT '统计日期',
  `pv` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览器',
  `uv` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '独立访客',
  `session_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '会话数',
  `avg_duration` INT UNSIGNED DEFAULT 0 COMMENT '平均停留时长（秒）',
  `bounce_rate` DECIMAL(5,2) DEFAULT 0.00 COMMENT '跳出率（%）',
  `new_visitor_ratio` DECIMAL(5,2) DEFAULT 0.00 COMMENT '新访客占比（%）',
  `top_pages` JSON DEFAULT NULL COMMENT '热门页面TOP10',
  `sources` JSON DEFAULT NULL COMMENT '来源分布',
  `devices` JSON DEFAULT NULL COMMENT '设备分布',
  `locations` JSON DEFAULT NULL COMMENT '地域分布TOP10',
  `browsers` JSON DEFAULT NULL COMMENT '浏览器分布',
  `hourly_distribution` JSON DEFAULT NULL COMMENT '24小时分布',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stats_date` (`stat_date`),
  KEY `idx_stats_date_range` (`stat_date`, `pv`, `uv`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日统计表';

-- =============================================
-- 9. 收藏功能表
-- =============================================

CREATE TABLE IF NOT EXISTS `article_favorites` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `folder_name` VARCHAR(100) DEFAULT '默认收藏夹' COMMENT '收藏夹名称',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '收藏备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_favorites_user_article` (`user_id`, `article_id`),
  KEY `idx_favorites_article` (`article_id`),
  KEY `idx_favorites_user_folder` (`user_id`, `folder_name`),
  KEY `idx_favorites_created` (`created_at`),
  CONSTRAINT `fk_favorites_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_favorites_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章收藏夹';

-- =============================================
-- 10. 芙贝币（签到奖励系统）表
-- =============================================

CREATE TABLE IF NOT EXISTS `user_coins` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `balance` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前余额',
  `total_earned` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计获得',
  `total_spent` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计消费',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coins_user` (`user_id`),
  CONSTRAINT `fk_coins_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户芙贝币账户表';

CREATE TABLE IF NOT EXISTS `user_checkins` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `checkin_date` DATE NOT NULL COMMENT '签到日期',
  `coin_reward` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '获得芙贝币数量',
  `consecutive_days` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '连续签到天数',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT '签到IP',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_checkins_user_date` (`user_id`, `checkin_date`),
  KEY `idx_checkins_user` (`user_id`),
  KEY `idx_checkins_date` (`checkin_date`),
  CONSTRAINT `fk_checkins_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户签到记录表';

CREATE TABLE IF NOT EXISTS `coin_transactions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `transaction_type` ENUM('checkin','reward','spend','transfer','system') NOT NULL COMMENT '交易类型',
  `amount` INT NOT NULL COMMENT '变动金额（正数收入，负数支出）',
  `balance_after` INT UNSIGNED NOT NULL COMMENT '变动后余额',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '交易描述',
  `related_id` BIGINT DEFAULT NULL COMMENT '关联ID（如文章ID、评论ID等）',
  `related_type` VARCHAR(64) DEFAULT NULL COMMENT '关联类型',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT '操作IP',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_transactions_user` (`user_id`),
  KEY `idx_transactions_type` (`transaction_type`),
  KEY `idx_transactions_created` (`created_at`),
  CONSTRAINT `fk_transactions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='芙贝币交易记录表';

-- =============================================
-- 11. 用户扩展信息表
-- =============================================

CREATE TABLE IF NOT EXISTS `user_profiles` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `identity_title` VARCHAR(64) DEFAULT NULL COMMENT '身份头衔（如：资深开发者、全栈工程师等）',
  `identity_color` VARCHAR(20) DEFAULT NULL COMMENT '身份标识颜色',
  `company` VARCHAR(120) DEFAULT NULL COMMENT '公司/组织',
  `job_title` VARCHAR(64) DEFAULT NULL COMMENT '职位',
  `github_url` VARCHAR(255) DEFAULT NULL COMMENT 'GitHub链接',
  `twitter_url` VARCHAR(255) DEFAULT NULL COMMENT 'Twitter链接',
  `weibo_url` VARCHAR(255) DEFAULT NULL COMMENT '微博链接',
  `wechat_id` VARCHAR(64) DEFAULT NULL COMMENT '微信号（仅自己可见）',
  `qq_number` VARCHAR(20) DEFAULT NULL COMMENT 'QQ号（仅自己可见）',
  `skills` JSON DEFAULT NULL COMMENT '技能标签数量["Java","Vue","AI"]',
  `interests` JSON DEFAULT NULL COMMENT '兴趣标签数组',
  `social_links` JSON DEFAULT NULL COMMENT '其他社交链接',
  `is_profile_public` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '个人资料是否公开',
  `show_email` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否显示邮箱',
  `show_wechat` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否显示微信',
  `show_qq` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否显示QQ',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_profile_user` (`user_id`),
  CONSTRAINT `fk_profile_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户扩展资料表';

-- =============================================
-- 12. 赞赏系统表（Phase 2）
-- =============================================

CREATE TABLE IF NOT EXISTS `article_rewards` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `reward_type` ENUM('wechat','alipay') NOT NULL COMMENT '赞赏类型',
  `qr_image_id` BIGINT NOT NULL COMMENT '收款二维码图片ID',
  `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rewards_article` (`article_id`),
  CONSTRAINT `fk_rewards_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章赞赏设置表';

CREATE TABLE IF NOT EXISTS `reward_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `reward_type` ENUM('wechat','alipay') NOT NULL COMMENT '赞赏方式',
  `amount` DECIMAL(10,2) DEFAULT NULL COMMENT '赞赏金额（可选）',
  `message` VARCHAR(255) DEFAULT NULL COMMENT '留言',
  `visitor_id` VARCHAR(64) DEFAULT NULL COMMENT '访客标识',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_reward_records_article` (`article_id`),
  KEY `idx_reward_records_created` (`created_at`),
  CONSTRAINT `fk_reward_records_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赞赏记录表';

-- =============================================
-- 13. 评论@功能相关表
-- =============================================

CREATE TABLE IF NOT EXISTS `comment_mentions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `comment_id` BIGINT NOT NULL COMMENT '评论ID',
  `mentioned_user_id` BIGINT NOT NULL COMMENT '被@用户ID',
  `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
  `read_at` DATETIME DEFAULT NULL COMMENT '阅读时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mentions_comment_user` (`comment_id`, `mentioned_user_id`),
  KEY `idx_mentions_user` (`mentioned_user_id`),
  KEY `idx_mentions_unread` (`mentioned_user_id`, `is_read`),
  CONSTRAINT `fk_mentions_comment` FOREIGN KEY (`comment_id`) REFERENCES `article_comments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_mentions_user` FOREIGN KEY (`mentioned_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论@用户记录表';

-- =============================================
-- 14. 芙贝币打赏记录表
-- =============================================

CREATE TABLE IF NOT EXISTS `coin_reward_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `user_id` BIGINT NOT NULL COMMENT '打赏用户ID',
  `author_id` BIGINT NOT NULL COMMENT '作者ID',
  `amount` INT UNSIGNED NOT NULL COMMENT '打赏芙贝币数量',
  `message` VARCHAR(255) DEFAULT NULL COMMENT '打赏留言',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_coin_rewards_article` (`article_id`),
  KEY `idx_coin_rewards_user` (`user_id`),
  KEY `idx_coin_rewards_author` (`author_id`),
  KEY `idx_coin_rewards_created` (`created_at`),
  CONSTRAINT `fk_coin_rewards_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_coin_rewards_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_coin_rewards_author` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='芙贝币打赏记录表';



