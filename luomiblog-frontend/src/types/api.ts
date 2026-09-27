// API 类型定义

// 通用响应类型
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

// 分页响应类型
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// 用户类型
export interface User {
  id: number;
  username: string;
  email: string;
  nickname: string | null;
  avatar: string | null;
  bio: string | null;
  website: string | null;
  github: string | null;
  role: 'visitor' | 'member' | 'blogger' | 'admin';
  status: string;
  createdAt: string;
  updatedAt: string;
}

// 文章类型
export interface Article {
  id: number;
  title: string;
  slug: string;
  summary: string;
  aiSummary: string | null;
  content: string;
  contentHtml: string;
  categoryId: number | null;
  categoryName: string | null;
  authorId: number;
  authorName: string;
  authorAvatar: string | null;
  language: string;
  status: 'draft' | 'published' | 'archived';
  version: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  wordCount: number;
  readingTime: number;
  top: boolean;
  sortOrder: number;
  allowComments: boolean;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
  tags: Tag[];
}

// 标签类型
export interface Tag {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  color: string | null;
  usageCount: number;
}

// 分类类型
export interface Category {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  parentId: number | null;
  sortOrder: number;
  articleCount: number;
}

// 评论类型
export interface Comment {
  id: number;
  articleId: number;
  parentId: number | null;
  userId: number | null;
  visitorName: string | null;
  visitorEmail: string | null;
  visitorWebsite: string | null;
  content: string;
  contentHtml: string;
  status: 'pending' | 'approved' | 'rejected';
  isTop: boolean;
  likeCount: number;
  replyCount: number;
  createdAt: string;
  updatedAt: string;
  replies?: Comment[];
  user?: User;
}

// 登录请求
export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

// 注册请求
export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  nickname?: string;
}

// 认证响应
export interface AuthResponse {
  token: string;
  type: string;
  user: User;

  /* 注册邮箱验证：true 表示注册成功但需先验证邮箱（无 token，不可登录） */
  pendingEmailVerification?: boolean;
  challengeToken?: string;

  /* 登录 2FA 挑战态：true 表示密码已通过、需要 2FA（无 token） */
  twoFactorRequired?: boolean;
  /** true：首次登录强制绑定 TOTP */
  enrollment?: boolean;
  otpauthUri?: string;
  secret?: string;
  /** 绑定成功时一次性展示的还原码 */
  recoveryCodes?: string[];
}

// 辰汐通行证登录配置（公开"门牌"信息，公共客户端没有任何密钥）
export interface ChenxiConfig {
  enabled: boolean;
  issuer: string;
  clientId: string;
  redirectUri: string;
  scopes: string;
}

// 辰汐通行证授权码交换请求
export interface ChenxiExchangeRequest {
  code: string;
  codeVerifier: string;
}

// AI 问答请求
export interface AIAskRequest {
  articleId: number;
  question: string;
  language?: string;
}

// AI 问答响应
export interface AIAskResponse {
  answer: string;
  sources: string[];
  answerId: string;
}

// 安装状态响应
export interface InstallStatusResponse {
  installed: boolean;
  locked: boolean;
  hasData: boolean;
  message: string;
}

// 环境检测响应
export interface EnvironmentCheckResponse {
  allPassed: boolean;
  checks: {
    name: string;
    passed: boolean;
    message: string;
    suggestion: string;
    details?: string[];
  }[];
  logs?: string[];
}

// 数据库配置请求
export interface DatabaseConfigRequest {
  host: string;
  port: number;
  database: string;
  username: string;
  password: string;
}

// 管理员账号请求
export interface AdminAccountRequest {
  username: string;
  password: string;
  email: string;
  nickname?: string;
}

// 站点配置请求
export interface SiteConfigRequest {
  siteName: string;
  siteDescription?: string;
  defaultTheme?: string;
  defaultLanguage?: string;
  timezone?: string;
}

// SMTP配置请求
export interface SmtpConfigRequest {
  enabled: boolean;
  host: string;
  port: number;
  username: string;
  password: string;
  fromName: string;
  fromEmail: string;
  useSsl: boolean;
}

// 网站图标配置请求
export interface FaviconConfigRequest {
  type: 'svg' | 'url';
  content: string;
}

// 管理后台用户类型
export interface AdminUser {
  id: number;
  username: string;
  nickname: string | null;
  email: string;
  avatarUrl: string | null;
  bio: string | null;
  signature: string | null;
  location: string | null;
  website: string | null;
  role: string;
  roleName: string | null;
  roleId: number;
  status: string;
  emailVerified: boolean;
  lastLoginAt: string | null;
  lastLoginIp: string | null;
  articleCount: number;
  commentCount: number;
  createdAt: string;
  updatedAt: string;
}

// 管理员更新用户请求
export interface AdminUserUpdateRequest {
  nickname?: string;
  email?: string;
  bio?: string;
  signature?: string;
  location?: string;
  website?: string;
  avatarUrl?: string;
}

// 管理员变更角色请求
export interface AdminRoleChangeRequest {
  roleId: number;
}

// 管理员变更状态请求
export interface AdminStatusChangeRequest {
  status: 'active' | 'inactive' | 'banned';
}

// 管理员重置密码请求
export interface AdminResetPasswordRequest {
  newPassword: string;
}

// 站长功能开关状态（公开接口，注册/登录页据此决定流程）
export interface SiteFeatures {
  registrationEmailVerifyRequired: boolean;
  loginTotpRequired: boolean;
  smtpConfigured: boolean;
}

// SMTP 设置（管理后台视图，password 永不回传，仅 passwordSet 标记）
export interface SmtpSettings {
  host: string;
  port: number;
  username: string;
  ssl: boolean;
  from: string;
  passwordSet: boolean;
}

// 管理端站点设置
export interface AdminSettings {
  registrationEmailVerifyRequired: boolean;
  loginTotpRequired: boolean;
  smtpConfigured: boolean;
  smtp: SmtpSettings;
}

// 文章互动（浏览量上报/点赞切换）返回的统计负载
export interface ArticleStatsPayload {
  success: boolean;
  action: string;
  viewCount?: number;
  likeCount?: number;
  favoriteCount?: number;
  hasLiked?: boolean;
  hasFavorited?: boolean;
  message?: string;
}

// 我的收藏条目
export interface FavoriteItem {
  favoriteId: number;
  articleId: number;
  title: string;
  slug: string;
  summary: string | null;
  categoryName: string | null;
  authorName: string | null;
  viewCount: number | null;
  likeCount: number | null;
  folderName: string | null;
  favoritedAt: string;
}

// 我的收藏列表响应（含收藏夹清单与分页）
export interface MyFavoritesResponse {
  folders: string[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  items: FavoriteItem[];
}

// 健康检查响应
export interface HealthCheckResponse {
  status: 'healthy' | 'degraded' | 'unhealthy' | 'needs_reinstall' | 'not_installed';
  database: 'connected' | 'disconnected' | 'not_configured' | 'connected_no_tables';
  installLock: boolean;
  hasData: boolean;
  version: string;
  timestamp: string;
  message: string;
  suggestions: string[];
  components: {
    database: {
      status: string;
      message: string;
      error?: string;
    };
    cache: {
      status: string;
      message: string;
    };
    fileSystem: {
      status: string;
      message: string;
    };
  };
}
