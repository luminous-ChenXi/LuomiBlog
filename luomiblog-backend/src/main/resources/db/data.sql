-- =============================================
-- LuomiBlog 数据库初始数据脚本
-- 版本: V1.2
-- =============================================

USE luomiblog;

-- =============================================
-- 1. 初始化角色数据
-- =============================================

INSERT INTO `roles` (`id`, `code`, `name`, `description`, `level`, `is_system`) VALUES
(1, 'visitor', '访客', '未登录的匿名用户', 0, 1),
(2, 'member', '会员', '注册登录的用户', 1, 1),
(3, 'blogger', '博主', '可以写文章的用户', 10, 1),
(4, 'admin', '管理员', '全站管理员', 100, 1);

-- =============================================
-- 2. 初始化权限数据
-- =============================================

INSERT INTO `permissions` (`code`, `name`, `type`, `description`, `sort_order`) VALUES
-- 文章权限
('article:view', '查看文章', 'api', '查看文章列表和详情', 1),
('article:create', '创建文章', 'api', '创建新文章', 2),
('article:update', '更新文章', 'api', '更新自己的文章', 3),
('article:delete', '删除文章', 'api', '删除文章', 4),
('article:manage', '管理文章', 'api', '管理所有文章', 5),
-- 评论权限
('comment:view', '查看评论', 'api', '查看评论列表', 10),
('comment:create', '创建评论', 'api', '发表评论', 11),
('comment:delete', '删除评论', 'api', '删除评论', 12),
('comment:manage', '管理评论', 'api', '审核和管理评论', 13),
-- 用户权限
('user:view', '查看用户', 'api', '查看用户信息', 20),
('user:update', '更新用户', 'api', '更新用户信息', 21),
('user:manage', '管理用户', 'api', '管理所有用户', 22),
-- 系统权限
('system:view', '查看数据概览', 'api', '查看统计数据', 30),
('system:config', '系统配置', 'api', '修改系统配置', 31),
-- 协作权限
('suggestion:create', '提交建议', 'api', '提交编辑建议', 40),
('suggestion:review', '审核建议', 'api', '审核编辑建议', 41),
-- 收藏权限
('favorite:create', '收藏文章', 'api', '收藏文章', 50),
('favorite:delete', '取消收藏', 'api', '取消收藏文章', 51),
('favorite:view', '查看收藏', 'api', '查看收藏列表', 52),
-- 芙贝币权限
('coin:view', '查看芙贝币', 'api', '查看芙贝币余额和记录', 60),
('checkin:create', '每日签到', 'api', '每日签到获取芙贝币', 61),
-- 赞赏/打赏权限
('reward:view', '查看赞赏', 'api', '查看赞赏二维码', 70),
('reward:config', '配置赞赏', 'api', '配置文章赞赏', 71),
('reward:give', '打赏文章', 'api', '使用芙贝币打赏文章', 72),
-- 点赞权限
('article:like', '点赞文章', 'api', '点赞文章', 80),
('comment:like', '点赞评论', 'api', '点赞评论', 81),
-- 文章状态管理权限
('article:publish', '发布文章', 'api', '更改文章发布状态', 90),
('article:unpublish', '取消发布', 'api', '取消文章发布状态', 91);

-- =============================================
-- 3. 分配角色权限
-- =============================================

-- 访客：查看文章/评论、点赞、收藏、查看赞赏
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 1, id FROM `permissions` WHERE `code` IN (
  'article:view', 'comment:view',
  'article:like', 'comment:like',
  'favorite:create', 'favorite:delete', 'favorite:view',
  'reward:view'
);

-- 会员：继承访客权限 + 评论、提交建议、签到、打赏（使用芙贝币）
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 2, id FROM `permissions` WHERE `code` IN (
  'article:view', 'comment:view', 'comment:create',
  'article:like', 'comment:like',
  'user:view', 'user:update', 'suggestion:create',
  'favorite:create', 'favorite:delete', 'favorite:view',
  'coin:view', 'checkin:create',
  'reward:view', 'reward:give'
);

-- 博主：继承会员权限 + 写文章、管理文章、更改文章状态、数据概览、后台访问
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 3, id FROM `permissions` WHERE `code` IN (
  'article:view', 'article:create', 'article:update', 'article:delete',
  'article:publish', 'article:unpublish',
  'comment:view', 'comment:create', 'comment:delete', 'comment:manage',
  'article:like', 'comment:like',
  'user:view', 'user:update', 'system:view',
  'suggestion:create', 'suggestion:review',
  'favorite:create', 'favorite:delete', 'favorite:view',
  'coin:view', 'checkin:create',
  'reward:view', 'reward:give', 'reward:config',
  'article:manage', 'comment:manage'
);

-- 管理员：所有权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 4, id FROM `permissions`;

-- =============================================
-- 4. 初始化管理面板菜单
-- =============================================

INSERT INTO `admin_menu` (`name`, `code`, `path`, `icon`, `permission_code`, `visible_roles`, `sort_order`) VALUES
('数据概览', 'dashboard', '/admin', 'LayoutDashboard', 'system:view', '["admin","blogger"]', 1),
('文章管理', 'article_manage', '/admin/articles', 'FileText', 'article:manage', '["admin","blogger"]', 2),
('评论管理', 'comment_manage', '/admin/comments', 'MessageSquare', 'comment:manage', '["admin","blogger"]', 3),
('编辑建议', 'suggestions', '/admin/suggestions', 'GitPullRequest', 'suggestion:review', '["admin","blogger"]', 4),
('用户管理', 'user_manage', '/admin/users', 'Users', 'user:manage', '["admin"]', 5),
('附件管理', 'attachments', '/admin/attachments', 'Image', 'article:manage', '["admin","blogger"]', 6),
('芙贝币管理', 'coin_manage', '/admin/coins', 'Coins', 'system:config', '["admin"]', 7),
('系统设置', 'system_settings', '/admin/settings', 'Settings', 'system:config', '["admin"]', 8),
('AI配置', 'ai_config', '/admin/ai', 'Cpu', 'system:config', '["admin"]', 9),
('数据统计', 'analytics', '/admin/analytics', 'BarChart3', 'system:view', '["admin","blogger"]', 10);

-- =============================================
-- 5. 初始化系统配置
-- =============================================

INSERT INTO `system_config` (
  `id`, `site_name`, `site_description`, `default_language`, `default_theme`,
  `registration_enabled`, `comment_audit`, `visitor_comment`,
  `max_upload_size`, `max_image_width`, `max_image_height`
) VALUES (
  1,
  'LuomiBlog',
  '一个基于 Astro + Vue + SpringBoot 的 AI 知识库博客',
  'zh',
  'auto',
  1,
  1,
  0,
  5242880,
  2048,
  2048
);

-- =============================================
-- 6. 初始化默认分类
-- =============================================

INSERT INTO `article_category` (`name`, `slug`, `description`, `sort_order`) VALUES
('技术分享', 'tech', '技术文章、开发经验分享', 1),
('生活随笔', 'life', '日常生活、个人感悟', 2),
('学习笔记', 'notes', '学习过程中的笔记整理', 3),
('项目总结', 'projects', '项目开发总结与复盘', 4);

-- =============================================
-- 7. 初始化默认标签
-- =============================================

INSERT INTO `tags` (`name`, `slug`, `type`, `description`) VALUES
('Astro', 'astro', 'system', 'Astro 静态站点生成器'),
('Vue', 'vue', 'system', 'Vue.js 前端框架'),
('SpringBoot', 'springboot', 'system', 'Spring Boot 后端框架'),
('AI', 'ai', 'system', '人工智能相关'),
('RAG', 'rag', 'system', '检索增强生成'),
('MySQL', 'mysql', 'system', 'MySQL 数据库'),
('前端', 'frontend', 'system', '前端开发'),
('后端', 'backend', 'system', '后端开发'),
('DevOps', 'devops', 'system', '运维部署');

-- =============================================
-- 8. 初始化国际化字符串
-- =============================================

INSERT INTO `i18n_strings` (`key`, `language`, `value`, `context`) VALUES
-- 通用
('site.name', 'zh', 'LuomiBlog', 'site'),
('site.name', 'en', 'LuomiBlog', 'site'),
('site.name', 'ja', 'LuomiBlog', 'site'),
('nav.home', 'zh', '首页', 'nav'),
('nav.home', 'en', 'Home', 'nav'),
('nav.home', 'ja', 'ホーム', 'nav'),
('nav.archive', 'zh', '归档', 'nav'),
('nav.archive', 'en', 'Archive', 'nav'),
('nav.archive', 'ja', 'アーカイブ', 'nav'),
('nav.about', 'zh', '关于', 'nav'),
('nav.about', 'en', 'About', 'nav'),
('nav.about', 'ja', 'について', 'nav'),
-- 文章
('article.read_more', 'zh', '阅读更多', 'article'),
('article.read_more', 'en', 'Read More', 'article'),
('article.read_more', 'ja', '続きを読む', 'article'),
('article.min_read', 'zh', '分钟阅读', 'article'),
('article.min_read', 'en', 'min read', 'article'),
('article.min_read', 'ja', '分で読めます', 'article'),
('article.original', 'zh', '原创', 'article'),
('article.original', 'en', 'Original', 'article'),
('article.original', 'ja', 'オリジナル', 'article'),
('article.reprint', 'zh', '转载', 'article'),
('article.reprint', 'en', 'Reprint', 'article'),
('article.reprint', 'ja', '転載', 'article'),
('article.difficulty', 'zh', '难度', 'article'),
('article.difficulty', 'en', 'Difficulty', 'article'),
('article.difficulty', 'ja', '難易度', 'article'),
('article.recommended', 'zh', '推荐', 'article'),
('article.recommended', 'en', 'Recommended', 'article'),
('article.recommended', 'ja', 'おすすめ', 'article'),
('article.word_count', 'zh', '字数', 'article'),
('article.word_count', 'en', 'Words', 'article'),
('article.word_count', 'ja', '文字数', 'article'),
('article.view_count', 'zh', '阅读', 'article'),
('article.view_count', 'en', 'Views', 'article'),
('article.view_count', 'ja', '閲覧', 'article'),
('article.like_count', 'zh', '点赞', 'article'),
('article.like_count', 'en', 'Likes', 'article'),
('article.like_count', 'ja', 'いいね', 'article'),
-- 评论
('comment.title', 'zh', '评论', 'comment'),
('comment.title', 'en', 'Comments', 'comment'),
('comment.title', 'ja', 'コメント', 'comment'),
('comment.placeholder', 'zh', '写下你的想法...', 'comment'),
('comment.placeholder', 'en', 'Write your thoughts...', 'comment'),
('comment.placeholder', 'ja', 'あなたの考えを書いてください...', 'comment'),
('comment.submit', 'zh', '发表评论', 'comment'),
('comment.submit', 'en', 'Post Comment', 'comment'),
('comment.submit', 'ja', 'コメントを投稿', 'comment'),
-- AI助手
('ai.title', 'zh', 'AI 助手', 'ai'),
('ai.title', 'en', 'AI Assistant', 'ai'),
('ai.title', 'ja', 'AI アシスタント', 'ai'),
('ai.placeholder', 'zh', '有问题问我吧...', 'ai'),
('ai.placeholder', 'en', 'Ask me anything...', 'ai'),
('ai.placeholder', 'ja', '何でも聞いてください...', 'ai'),
('ai.tag', 'zh', 'RAG 增强', 'ai'),
('ai.tag', 'en', 'RAG Enhanced', 'ai'),
('ai.tag', 'ja', 'RAG 強化', 'ai'),
-- 收藏
('favorite.title', 'zh', '我的收藏', 'favorite'),
('favorite.title', 'en', 'My Favorites', 'favorite'),
('favorite.title', 'ja', 'お気に入り', 'favorite'),
('favorite.add', 'zh', '收藏文章', 'favorite'),
('favorite.add', 'en', 'Add to Favorites', 'favorite'),
('favorite.add', 'ja', 'お気に入りに追加', 'favorite'),
('favorite.remove', 'zh', '取消收藏', 'favorite'),
('favorite.remove', 'en', 'Remove from Favorites', 'favorite'),
('favorite.remove', 'ja', 'お気に入りから削除', 'favorite'),
-- 芙贝币/签到
('coin.name', 'zh', '芙贝币', 'coin'),
('coin.name', 'en', 'Furbe Coins', 'coin'),
('coin.name', 'ja', 'フルベコイン', 'coin'),
('coin.balance', 'zh', '余额', 'coin'),
('coin.balance', 'en', 'Balance', 'coin'),
('coin.balance', 'ja', '残高', 'coin'),
('checkin.title', 'zh', '每日签到', 'coin'),
('checkin.title', 'en', 'Daily Check-in', 'coin'),
('checkin.title', 'ja', '毎日チェックイン', 'coin'),
('checkin.button', 'zh', '签到', 'coin'),
('checkin.button', 'en', 'Check In', 'coin'),
('checkin.button', 'ja', 'チェックイン', 'coin'),
('checkin.success', 'zh', '签到成功！获得 {coins} 芙贝币', 'coin'),
('checkin.success', 'en', 'Check-in successful! Earned {coins} coins', 'coin'),
('checkin.success', 'ja', 'チェックイン成功！{coins}コイン獲得', 'coin'),
('checkin.consecutive', 'zh', '连续签到 {days} 天', 'coin'),
('checkin.consecutive', 'en', '{days} days streak', 'coin'),
('checkin.consecutive', 'ja', '{days}日連続チェックイン', 'coin'),
-- 用户资料
('profile.title', 'zh', '个人资料', 'profile'),
('profile.title', 'en', 'Profile', 'profile'),
('profile.title', 'ja', 'プロフィール', 'profile'),
('profile.identity', 'zh', '身份头衔', 'profile'),
('profile.identity', 'en', 'Identity Title', 'profile'),
('profile.identity', 'ja', '身分タイトル', 'profile'),
('profile.company', 'zh', '公司', 'profile'),
('profile.company', 'en', 'Company', 'profile'),
('profile.company', 'ja', '会社', 'profile'),
('profile.job', 'zh', '职位', 'profile'),
('profile.job', 'en', 'Job Title', 'profile'),
('profile.job', 'ja', '職位', 'profile'),
('profile.skills', 'zh', '技能', 'profile'),
('profile.skills', 'en', 'Skills', 'profile'),
('profile.skills', 'ja', 'スキル', 'profile'),
('profile.website', 'zh', '个人网站', 'profile'),
('profile.website', 'en', 'Website', 'profile'),
('profile.website', 'ja', 'ウェブサイト', 'profile'),
-- 赞赏
('reward.title', 'zh', '赞赏', 'reward'),
('reward.title', 'en', 'Reward', 'reward'),
('reward.title', 'ja', '報酬', 'reward'),
('reward.wechat', 'zh', '微信支付', 'reward'),
('reward.wechat', 'en', 'WeChat Pay', 'reward'),
('reward.wechat', 'ja', 'WeChat Pay', 'reward'),
('reward.alipay', 'zh', '支付宝', 'reward'),
('reward.alipay', 'en', 'Alipay', 'reward'),
('reward.alipay', 'ja', 'Alipay', 'reward'),
('reward.message', 'zh', '留言', 'reward'),
('reward.message', 'en', 'Message', 'reward'),
('reward.message', 'ja', 'メッセージ', 'reward'),
-- 评论@
('mention.title', 'zh', '提到我', 'mention'),
('mention.title', 'en', 'Mentions', 'mention'),
('mention.title', 'ja', 'メンション', 'mention'),
('mention.notify', 'zh', '{user} 在评论中提到了你', 'mention'),
('mention.notify', 'en', '{user} mentioned you in a comment', 'mention'),
('mention.notify', 'ja', '{user}がコメントであなたに言及しました', 'mention');
