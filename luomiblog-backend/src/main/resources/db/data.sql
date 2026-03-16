-- =============================================
-- LuomiBlog 鏁版嵁搴撳垵濮嬫暟鎹剼鏈?
-- 鐗堟湰: V1.2
-- =============================================

-- =============================================
-- 1. 鍒濆鍖栬鑹叉暟鎹?
-- =============================================

INSERT INTO `roles` (`id`, `code`, `name`, `description`, `level`, `is_system`) VALUES
(1, 'visitor', '璁垮', '鏈櫥褰曠殑鍖垮悕鐢ㄦ埛', 0, 1),
(2, 'member', '浼氬憳', '娉ㄥ唽鐧诲綍鐨勭敤鎴?, 1, 1),
(3, 'blogger', '鍗氫富', '鍙互鍐欐枃绔犵殑鐢ㄦ埛', 10, 1),
(4, 'admin', '绠＄悊鍛?, '鍏ㄧ珯绠＄悊鍛?, 100, 1);

-- =============================================
-- 2. 鍒濆鍖栨潈闄愭暟鎹?
-- =============================================

INSERT INTO `permissions` (`code`, `name`, `type`, `description`, `sort_order`) VALUES
-- 鏂囩珷鏉冮檺
('article:view', '鏌ョ湅鏂囩珷', 'api', '鏌ョ湅鏂囩珷鍒楄〃鍜岃鎯?, 1),
('article:create', '鍒涘缓鏂囩珷', 'api', '鍒涘缓鏂版枃绔?, 2),
('article:update', '鏇存柊鏂囩珷', 'api', '鏇存柊鑷繁鐨勬枃绔?, 3),
('article:delete', '鍒犻櫎鏂囩珷', 'api', '鍒犻櫎鏂囩珷', 4),
('article:manage', '绠＄悊鏂囩珷', 'api', '绠＄悊鎵€鏈夋枃绔?, 5),
-- 璇勮鏉冮檺
('comment:view', '鏌ョ湅璇勮', 'api', '鏌ョ湅璇勮鍒楄〃', 10),
('comment:create', '鍒涘缓璇勮', 'api', '鍙戣〃璇勮', 11),
('comment:delete', '鍒犻櫎璇勮', 'api', '鍒犻櫎璇勮', 12),
('comment:manage', '绠＄悊璇勮', 'api', '瀹℃牳鍜岀鐞嗚瘎璁?, 13),
-- 鐢ㄦ埛鏉冮檺
('user:view', '鏌ョ湅鐢ㄦ埛', 'api', '鏌ョ湅鐢ㄦ埛淇℃伅', 20),
('user:update', '鏇存柊鐢ㄦ埛', 'api', '鏇存柊鐢ㄦ埛淇℃伅', 21),
('user:manage', '绠＄悊鐢ㄦ埛', 'api', '绠＄悊鎵€鏈夌敤鎴?, 22),
-- 绯荤粺鏉冮檺
('system:view', '鏌ョ湅鏁版嵁姒傝', 'api', '鏌ョ湅缁熻鏁版嵁', 30),
('system:config', '绯荤粺閰嶇疆', 'api', '淇敼绯荤粺閰嶇疆', 31),
-- 鍗忎綔鏉冮檺
('suggestion:create', '鎻愪氦寤鸿', 'api', '鎻愪氦缂栬緫寤鸿', 40),
('suggestion:review', '瀹℃牳寤鸿', 'api', '瀹℃牳缂栬緫寤鸿', 41),
-- 鏀惰棌鏉冮檺
('favorite:create', '鏀惰棌鏂囩珷', 'api', '鏀惰棌鏂囩珷', 50),
('favorite:delete', '鍙栨秷鏀惰棌', 'api', '鍙栨秷鏀惰棌鏂囩珷', 51),
('favorite:view', '鏌ョ湅鏀惰棌', 'api', '鏌ョ湅鏀惰棌鍒楄〃', 52),
-- 鑺欒礉甯佹潈闄?
('coin:view', '鏌ョ湅鑺欒礉甯?, 'api', '鏌ョ湅鑺欒礉甯佷綑棰濆拰璁板綍', 60),
('checkin:create', '姣忔棩绛惧埌', 'api', '姣忔棩绛惧埌鑾峰彇鑺欒礉甯?, 61),
-- 璧炶祻/鎵撹祻鏉冮檺
('reward:view', '鏌ョ湅璧炶祻', 'api', '鏌ョ湅璧炶祻浜岀淮鐮?, 70),
('reward:config', '閰嶇疆璧炶祻', 'api', '閰嶇疆鏂囩珷璧炶祻', 71),
('reward:give', '鎵撹祻鏂囩珷', 'api', '浣跨敤鑺欒礉甯佹墦璧忔枃绔?, 72),
-- 鐐硅禐鏉冮檺
('article:like', '鐐硅禐鏂囩珷', 'api', '鐐硅禐鏂囩珷', 80),
('comment:like', '鐐硅禐璇勮', 'api', '鐐硅禐璇勮', 81),
-- 鏂囩珷鐘舵€佺鐞嗘潈闄?
('article:publish', '鍙戝竷鏂囩珷', 'api', '鏇存敼鏂囩珷鍙戝竷鐘舵€?, 90),
('article:unpublish', '鍙栨秷鍙戝竷', 'api', '鍙栨秷鏂囩珷鍙戝竷鐘舵€?, 91);

-- =============================================
-- 3. 鍒嗛厤瑙掕壊鏉冮檺
-- =============================================

-- 璁垮锛氭煡鐪嬫枃绔?璇勮銆佺偣璧炪€佹敹钘忋€佹煡鐪嬭禐璧?
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 1, id FROM `permissions` WHERE `code` IN (
  'article:view', 'comment:view',
  'article:like', 'comment:like',
  'favorite:create', 'favorite:delete', 'favorite:view',
  'reward:view'
);

-- 浼氬憳锛氱户鎵胯瀹㈡潈闄?+ 璇勮銆佹彁浜ゅ缓璁€佺鍒般€佹墦璧忥紙浣跨敤鑺欒礉甯侊級
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 2, id FROM `permissions` WHERE `code` IN (
  'article:view', 'comment:view', 'comment:create',
  'article:like', 'comment:like',
  'user:view', 'user:update', 'suggestion:create',
  'favorite:create', 'favorite:delete', 'favorite:view',
  'coin:view', 'checkin:create',
  'reward:view', 'reward:give'
);

-- 鍗氫富锛氱户鎵夸細鍛樻潈闄?+ 鍐欐枃绔犮€佺鐞嗘枃绔犮€佹洿鏀规枃绔犵姸鎬併€佹暟鎹瑙堛€佸悗鍙拌闂?
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

-- 绠＄悊鍛橈細鎵€鏈夋潈闄?
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 4, id FROM `permissions`;

-- =============================================
-- 4. 鍒濆鍖栫鐞嗛潰鏉胯彍鍗?
-- =============================================

INSERT INTO `admin_menu` (`name`, `code`, `path`, `icon`, `permission_code`, `visible_roles`, `sort_order`) VALUES
('鏁版嵁姒傝', 'dashboard', '/admin', 'LayoutDashboard', 'system:view', '["admin","blogger"]', 1),
('鏂囩珷绠＄悊', 'article_manage', '/admin/articles', 'FileText', 'article:manage', '["admin","blogger"]', 2),
('璇勮绠＄悊', 'comment_manage', '/admin/comments', 'MessageSquare', 'comment:manage', '["admin","blogger"]', 3),
('缂栬緫寤鸿', 'suggestions', '/admin/suggestions', 'GitPullRequest', 'suggestion:review', '["admin","blogger"]', 4),
('鐢ㄦ埛绠＄悊', 'user_manage', '/admin/users', 'Users', 'user:manage', '["admin"]', 5),
('闄勪欢绠＄悊', 'attachments', '/admin/attachments', 'Image', 'article:manage', '["admin","blogger"]', 6),
('鑺欒礉甯佺鐞?, 'coin_manage', '/admin/coins', 'Coins', 'system:config', '["admin"]', 7),
('绯荤粺璁剧疆', 'system_settings', '/admin/settings', 'Settings', 'system:config', '["admin"]', 8),
('AI閰嶇疆', 'ai_config', '/admin/ai', 'Cpu', 'system:config', '["admin"]', 9),
('鏁版嵁缁熻', 'analytics', '/admin/analytics', 'BarChart3', 'system:view', '["admin","blogger"]', 10);

-- =============================================
-- 5. 鍒濆鍖栫郴缁熼厤缃?
-- =============================================

INSERT INTO `system_config` (
  `id`, `site_name`, `site_description`, `default_language`, `default_theme`,
  `registration_enabled`, `comment_audit`, `visitor_comment`,
  `max_upload_size`, `max_image_width`, `max_image_height`
) VALUES (
  1,
  'LuomiBlog',
  '涓€涓熀浜?Astro + Vue + SpringBoot 鐨?AI 鐭ヨ瘑搴撳崥瀹?,
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
-- 6. 鍒濆鍖栭粯璁ゅ垎绫?
-- =============================================

INSERT INTO `article_category` (`name`, `slug`, `description`, `sort_order`) VALUES
('鎶€鏈垎浜?, 'tech', '鎶€鏈枃绔犮€佸紑鍙戠粡楠屽垎浜?, 1),
('鐢熸椿闅忕瑪', 'life', '鏃ュ父鐢熸椿銆佷釜浜烘劅鎮?, 2),
('瀛︿範绗旇', 'notes', '瀛︿範杩囩▼涓殑绗旇鏁寸悊', 3),
('椤圭洰鎬荤粨', 'projects', '椤圭洰寮€鍙戞€荤粨涓庡鐩?, 4);

-- =============================================
-- 7. 鍒濆鍖栭粯璁ゆ爣绛?
-- =============================================

INSERT INTO `tags` (`name`, `slug`, `type`, `description`) VALUES
('Astro', 'astro', 'system', 'Astro 闈欐€佺珯鐐圭敓鎴愬櫒'),
('Vue', 'vue', 'system', 'Vue.js 鍓嶇妗嗘灦'),
('SpringBoot', 'springboot', 'system', 'Spring Boot 鍚庣妗嗘灦'),
('AI', 'ai', 'system', '浜哄伐鏅鸿兘鐩稿叧'),
('RAG', 'rag', 'system', '妫€绱㈠寮虹敓鎴?),
('MySQL', 'mysql', 'system', 'MySQL 鏁版嵁搴?),
('鍓嶇', 'frontend', 'system', '鍓嶇寮€鍙?),
('鍚庣', 'backend', 'system', '鍚庣寮€鍙?),
('DevOps', 'devops', 'system', '杩愮淮閮ㄧ讲');

-- =============================================
-- 8. 鍒濆鍖栧浗闄呭寲瀛楃涓?
-- =============================================

INSERT INTO `i18n_strings` (`key`, `language`, `value`, `context`) VALUES
-- 閫氱敤
('site.name', 'zh', 'LuomiBlog', 'site'),
('site.name', 'en', 'LuomiBlog', 'site'),
('site.name', 'ja', 'LuomiBlog', 'site'),
('nav.home', 'zh', '棣栭〉', 'nav'),
('nav.home', 'en', 'Home', 'nav'),
('nav.home', 'ja', '銉涖兗銉?, 'nav'),
('nav.archive', 'zh', '褰掓。', 'nav'),
('nav.archive', 'en', 'Archive', 'nav'),
('nav.archive', 'ja', '銈兗銈偆銉?, 'nav'),
('nav.about', 'zh', '鍏充簬', 'nav'),
('nav.about', 'en', 'About', 'nav'),
('nav.about', 'ja', '銇仱銇勩仸', 'nav'),
-- 鏂囩珷
('article.read_more', 'zh', '闃呰鏇村', 'article'),
('article.read_more', 'en', 'Read More', 'article'),
('article.read_more', 'ja', '缍氥亶銈掕銈€', 'article'),
('article.min_read', 'zh', '鍒嗛挓闃呰', 'article'),
('article.min_read', 'en', 'min read', 'article'),
('article.min_read', 'ja', '鍒嗐仹瑾倎銇俱仚', 'article'),
('article.original', 'zh', '鍘熷垱', 'article'),
('article.original', 'en', 'Original', 'article'),
('article.original', 'ja', '銈儶銈搞儕銉?, 'article'),
('article.reprint', 'zh', '杞浇', 'article'),
('article.reprint', 'en', 'Reprint', 'article'),
('article.reprint', 'ja', '杌㈣級', 'article'),
('article.difficulty', 'zh', '闅惧害', 'article'),
('article.difficulty', 'en', 'Difficulty', 'article'),
('article.difficulty', 'ja', '闆ｆ槗搴?, 'article'),
('article.recommended', 'zh', '鎺ㄨ崘', 'article'),
('article.recommended', 'en', 'Recommended', 'article'),
('article.recommended', 'ja', '銇娿仚銇欍倎', 'article'),
('article.word_count', 'zh', '瀛楁暟', 'article'),
('article.word_count', 'en', 'Words', 'article'),
('article.word_count', 'ja', '鏂囧瓧鏁?, 'article'),
('article.view_count', 'zh', '闃呰', 'article'),
('article.view_count', 'en', 'Views', 'article'),
('article.view_count', 'ja', '闁茶Η', 'article'),
('article.like_count', 'zh', '鐐硅禐', 'article'),
('article.like_count', 'en', 'Likes', 'article'),
('article.like_count', 'ja', '銇勩亜銇?, 'article'),
-- 璇勮
('comment.title', 'zh', '璇勮', 'comment'),
('comment.title', 'en', 'Comments', 'comment'),
('comment.title', 'ja', '銈炽儭銉炽儓', 'comment'),
('comment.placeholder', 'zh', '鍐欎笅浣犵殑鎯虫硶...', 'comment'),
('comment.placeholder', 'en', 'Write your thoughts...', 'comment'),
('comment.placeholder', 'ja', '銇傘仾銇熴伄鑰冦亪銈掓浉銇勩仸銇忋仩銇曘亜...', 'comment'),
('comment.submit', 'zh', '鍙戣〃璇勮', 'comment'),
('comment.submit', 'en', 'Post Comment', 'comment'),
('comment.submit', 'ja', '銈炽儭銉炽儓銈掓姇绋?, 'comment'),
-- AI鍔╂墜
('ai.title', 'zh', 'AI 鍔╂墜', 'ai'),
('ai.title', 'en', 'AI Assistant', 'ai'),
('ai.title', 'ja', 'AI 銈偡銈广偪銉炽儓', 'ai'),
('ai.placeholder', 'zh', '鏈夐棶棰橀棶鎴戝惂...', 'ai'),
('ai.placeholder', 'en', 'Ask me anything...', 'ai'),
('ai.placeholder', 'ja', '浣曘仹銈傝仦銇勩仸銇忋仩銇曘亜...', 'ai'),
('ai.tag', 'zh', 'RAG 澧炲己', 'ai'),
('ai.tag', 'en', 'RAG Enhanced', 'ai'),
('ai.tag', 'ja', 'RAG 寮峰寲', 'ai'),
-- 鏀惰棌
('favorite.title', 'zh', '鎴戠殑鏀惰棌', 'favorite'),
('favorite.title', 'en', 'My Favorites', 'favorite'),
('favorite.title', 'ja', '銇婃皸銇叆銈?, 'favorite'),
('favorite.add', 'zh', '鏀惰棌鏂囩珷', 'favorite'),
('favorite.add', 'en', 'Add to Favorites', 'favorite'),
('favorite.add', 'ja', '銇婃皸銇叆銈娿伀杩藉姞', 'favorite'),
('favorite.remove', 'zh', '鍙栨秷鏀惰棌', 'favorite'),
('favorite.remove', 'en', 'Remove from Favorites', 'favorite'),
('favorite.remove', 'ja', '銇婃皸銇叆銈娿亱銈夊墛闄?, 'favorite'),
-- 鑺欒礉甯?绛惧埌
('coin.name', 'zh', '鑺欒礉甯?, 'coin'),
('coin.name', 'en', 'Furbe Coins', 'coin'),
('coin.name', 'ja', '銉曘儷銉欍偝銈ゃ兂', 'coin'),
('coin.balance', 'zh', '浣欓', 'coin'),
('coin.balance', 'en', 'Balance', 'coin'),
('coin.balance', 'ja', '娈嬮珮', 'coin'),
('checkin.title', 'zh', '姣忔棩绛惧埌', 'coin'),
('checkin.title', 'en', 'Daily Check-in', 'coin'),
('checkin.title', 'ja', '姣庢棩銉併偋銉冦偗銈ゃ兂', 'coin'),
('checkin.button', 'zh', '绛惧埌', 'coin'),
('checkin.button', 'en', 'Check In', 'coin'),
('checkin.button', 'ja', '銉併偋銉冦偗銈ゃ兂', 'coin'),
('checkin.success', 'zh', '绛惧埌鎴愬姛锛佽幏寰?{coins} 鑺欒礉甯?, 'coin'),
('checkin.success', 'en', 'Check-in successful! Earned {coins} coins', 'coin'),
('checkin.success', 'ja', '銉併偋銉冦偗銈ゃ兂鎴愬姛锛亄coins}銈炽偆銉崇嵅寰?, 'coin'),
('checkin.consecutive', 'zh', '杩炵画绛惧埌 {days} 澶?, 'coin'),
('checkin.consecutive', 'en', '{days} days streak', 'coin'),
('checkin.consecutive', 'ja', '{days}鏃ラ€ｇ稓銉併偋銉冦偗銈ゃ兂', 'coin'),
-- 鐢ㄦ埛璧勬枡
('profile.title', 'zh', '涓汉璧勬枡', 'profile'),
('profile.title', 'en', 'Profile', 'profile'),
('profile.title', 'ja', '銉椼儹銉曘偅銉笺儷', 'profile'),
('profile.identity', 'zh', '韬唤澶磋', 'profile'),
('profile.identity', 'en', 'Identity Title', 'profile'),
('profile.identity', 'ja', '韬垎銈裤偆銉堛儷', 'profile'),
('profile.company', 'zh', '鍏徃', 'profile'),
('profile.company', 'en', 'Company', 'profile'),
('profile.company', 'ja', '浼氱ぞ', 'profile'),
('profile.job', 'zh', '鑱屼綅', 'profile'),
('profile.job', 'en', 'Job Title', 'profile'),
('profile.job', 'ja', '鑱蜂綅', 'profile'),
('profile.skills', 'zh', '鎶€鑳?, 'profile'),
('profile.skills', 'en', 'Skills', 'profile'),
('profile.skills', 'ja', '銈广偔銉?, 'profile'),
('profile.website', 'zh', '涓汉缃戠珯', 'profile'),
('profile.website', 'en', 'Website', 'profile'),
('profile.website', 'ja', '銈︺偋銉栥偟銈ゃ儓', 'profile'),
-- 璧炶祻
('reward.title', 'zh', '璧炶祻', 'reward'),
('reward.title', 'en', 'Reward', 'reward'),
('reward.title', 'ja', '鍫遍叕', 'reward'),
('reward.wechat', 'zh', '寰俊鏀粯', 'reward'),
('reward.wechat', 'en', 'WeChat Pay', 'reward'),
('reward.wechat', 'ja', 'WeChat Pay', 'reward'),
('reward.alipay', 'zh', '鏀粯瀹?, 'reward'),
('reward.alipay', 'en', 'Alipay', 'reward'),
('reward.alipay', 'ja', 'Alipay', 'reward'),
('reward.message', 'zh', '鐣欒█', 'reward'),
('reward.message', 'en', 'Message', 'reward'),
('reward.message', 'ja', '銉°儍銈汇兗銈?, 'reward'),
-- 璇勮@
('mention.title', 'zh', '鎻愬埌鎴?, 'mention'),
('mention.title', 'en', 'Mentions', 'mention'),
('mention.title', 'ja', '銉°兂銈枫儳銉?, 'mention'),
('mention.notify', 'zh', '{user} 鍦ㄨ瘎璁轰腑鎻愬埌浜嗕綘', 'mention'),
('mention.notify', 'en', '{user} mentioned you in a comment', 'mention'),
('mention.notify', 'ja', '{user}銇屻偝銉°兂銉堛仹銇傘仾銇熴伀瑷€鍙娿仐銇俱仐銇?, 'mention');
