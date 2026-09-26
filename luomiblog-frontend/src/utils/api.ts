// API 客户端

import type {
  ApiResponse,
  PageResponse,
  Article,
  Tag,
  Category,
  Comment,
  User,
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  ChenxiConfig,
  ChenxiExchangeRequest,
  AIAskRequest,
  AIAskResponse,
  InstallStatusResponse,
  EnvironmentCheckResponse,
  DatabaseConfigRequest,
  AdminAccountRequest,
  SiteConfigRequest,
  SmtpConfigRequest,
  FaviconConfigRequest,
  HealthCheckResponse
} from '../types/api';
import { API_BASE_URL } from '../config/api';
import { setToken } from '../stores/user';

// 请求配置
interface RequestConfig extends RequestInit {
  params?: Record<string, string | number | boolean | undefined>;
}

// 获取 Token
function getToken(): string | null {
  if (typeof localStorage !== 'undefined') {
    return localStorage.getItem('token');
  }
  return null;
}

// 静默替换本地登录态（滑动续期下发的新令牌）
function applyRefreshedToken(newToken: string): void {
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('token', newToken);
  }
  setToken(newToken);
}

// 构建 URL
function buildUrl(path: string, params?: Record<string, string | number | boolean | undefined>): string {
  const url = new URL(path, API_BASE_URL);
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        url.searchParams.append(key, String(value));
      }
    });
  }
  return url.toString();
}

// 发送请求
async function request<T>(path: string, config: RequestConfig = {}): Promise<T> {
  const { params, ...fetchConfig } = config;
  const url = buildUrl(path, params);

  // 设置默认 headers
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((fetchConfig.headers as Record<string, string>) || {})
  };

  // 添加认证 Token
  const token = getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    ...fetchConfig,
    headers
  });

  // 辰汐会话滑动续期：后端在令牌活跃使用且剩余寿命不足一半时，
  // 通过 X-New-Token 响应头下发新令牌，这里静默替换本地登录态
  const newToken = response.headers.get('X-New-Token');
  if (newToken) {
    applyRefreshedToken(newToken);
  }

  // 处理响应
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || `HTTP ${response.status}`);
  }

  const result: ApiResponse<T> = await response.json();

  if (result.code !== 200) {
    throw new Error(result.message);
  }

  return result.data;
}

// 通用 POST 请求（用于未封装到 api 对象的接口）
export function post<T>(path: string, data: unknown): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    body: JSON.stringify(data)
  });
}

// API 方法
export const api = {
  // 认证相关
  auth: {
    login: (data: LoginRequest) =>
      request<AuthResponse>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify(data)
      }),

    register: (data: RegisterRequest) =>
      request<AuthResponse>('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify(data)
      }),

    logout: () =>
      request<void>('/api/auth/logout', { method: 'POST' }),

    me: () =>
      request<User>('/api/auth/me')
  },

  // 辰汐通行证登录（标准 OIDC 授权码 + PKCE 公共客户端）
  chenxi: {
    // 登录配置（公开门牌信息，前端根据 enabled 决定是否展示登录入口）
    config: () =>
      request<ChenxiConfig>('/api/auth/chenxi/config'),

    // 授权码 + PKCE 校验器换取本站会话，响应结构与登录接口一致
    exchange: (data: ChenxiExchangeRequest) =>
      request<AuthResponse>('/api/auth/chenxi/exchange', {
        method: 'POST',
        body: JSON.stringify(data)
      })
  },

  // 文章相关
  articles: {
    getList: (page = 0, size = 10, categoryId?: number) =>
      request<PageResponse<Article>>('/api/articles', {
        params: { page, size, categoryId }
      }),

    getBySlug: (slug: string) =>
      request<Article>(`/api/articles/${slug}`),

    getById: (id: number) =>
      request<Article>(`/api/articles/id/${id}`),

    search: (keyword: string) =>
      request<Article[]>('/api/articles/search', {
        params: { keyword }
      }),

    like: (id: number) =>
      request<void>(`/api/articles/${id}/like`, { method: 'POST' })
  },

  // 分类相关
  categories: {
    getList: () =>
      request<Category[]>('/api/categories'),

    getTree: () =>
      request<Category[]>('/api/categories/tree')
  },

  // 标签相关
  tags: {
    getList: () =>
      request<Tag[]>('/api/tags'),

    getBySlug: (slug: string) =>
      request<Tag>(`/api/tags/${slug}`)
  },

  // 评论相关
  comments: {
    getByArticle: (articleId: number, page = 0, size = 10) =>
      request<PageResponse<Comment>>('/api/comments', {
        params: { articleId, page, size }
      }),

    create: (data: Partial<Comment>) =>
      request<Comment>('/api/comments', {
        method: 'POST',
        body: JSON.stringify(data)
      }),

    like: (id: number) =>
      request<void>(`/api/comments/${id}/like`, { method: 'POST' })
  },

  // AI 相关
  ai: {
    ask: (data: AIAskRequest) =>
      request<AIAskResponse>('/api/ai/ask', {
        method: 'POST',
        body: JSON.stringify(data)
      }),

    feedback: (answerId: string, useful: boolean) =>
      request<void>('/api/ai/feedback', {
        method: 'POST',
        body: JSON.stringify({ answerId, useful })
      })
  },

  // 安装相关
  install: {
    getStatus: () =>
      request<InstallStatusResponse>('/api/install/status'),

    checkEnvironment: () =>
      request<EnvironmentCheckResponse>('/api/install/check-environment', {
        method: 'POST'
      }),

    testDatabase: (data: DatabaseConfigRequest) =>
      request<{ success: boolean; message: string }>('/api/install/test-database', {
        method: 'POST',
        body: JSON.stringify(data)
      }),

    checkDatabase: (data: DatabaseConfigRequest) =>
      request<{
        connected: boolean;
        message: string;
        mysqlVersion: string;
        databaseName: string;
        hasExistingData: boolean;
        existingDataMessage: string;
        existingTables: string[];
        needsReinstallOptions: boolean;
        logs: string[];
      }>('/api/install/check-database', {
        method: 'POST',
        body: JSON.stringify(data)
      }),

    executeSql: (data: DatabaseConfigRequest) =>
      request<{ success: boolean; message: string }>('/api/install/execute-sql', {
        method: 'POST',
        body: JSON.stringify(data)
      }),

    createAdmin: (data: AdminAccountRequest) =>
      request<{ success: boolean; message: string }>('/api/install/create-admin', {
        method: 'POST',
        body: JSON.stringify(data)
      }),

    saveSiteConfig: (data: SiteConfigRequest) =>
      request<{ success: boolean; message: string }>('/api/install/site-config', {
        method: 'POST',
        body: JSON.stringify(data)
      }),

    testSmtp: (data: SmtpConfigRequest) =>
      request<{ success: boolean; message: string }>('/api/install/test-smtp', {
        method: 'POST',
        body: JSON.stringify(data)
      }),

    saveSmtpConfig: (data: SmtpConfigRequest) =>
      request<{ success: boolean; message: string }>('/api/install/smtp-config', {
        method: 'POST',
        body: JSON.stringify(data)
      }),

    saveFaviconConfig: (data: FaviconConfigRequest) =>
      request<{ success: boolean; message: string }>('/api/install/favicon-config', {
        method: 'POST',
        body: JSON.stringify(data)
      }),

    complete: () =>
      request<{ success: boolean; message: string }>('/api/install/complete', {
        method: 'POST'
      }),

    // 需要已认证的 ADMIN token（请求包装器自动附带），confirm 必须为 "REINSTALL"
    verifyReinstall: (password: string, confirm: string) =>
      request<{ success: boolean; message: string; needsOptions?: boolean }>('/api/install/verify-reinstall', {
        method: 'POST',
        body: JSON.stringify({ password, confirm })
      }),

    getReinstallOptions: () =>
      request<{
        needsOptions: boolean;
        options: Array<{ code: string; name: string; description: string }>;
        warning: string | null;
      }>('/api/install/reinstall-options', {
        method: 'GET'
      }),

    // 需要已认证的 ADMIN token；confirm 为 "REINSTALL"，fresh_install 时必须为 "DROP_ALL_TABLES"
    executeReinstall: (option: string, confirm: string, database?: DatabaseConfigRequest) =>
      request<{ success: boolean; message: string; option: string }>('/api/install/reinstall', {
        method: 'POST',
        body: JSON.stringify({ option, confirm, database })
      })
  },

  // 健康检查
  health: {
    check: () =>
      request<HealthCheckResponse>('/api/health'),

    ping: () =>
      request<string>('/api/health/ping')
  },

  // 站点配置
  site: {
    getConfig: () =>
      request<{
        siteName: string;
        siteDescription: string;
        siteLogo: string;
        siteFavicon: string;
        defaultLanguage: string;
        defaultTheme: string;
        icp: string;
        seoTitle: string;
        seoKeywords: string;
        seoDescription: string;
      }>('/api/site/config'),

    getFavicon: () =>
      request<string>('/api/site/favicon')
  }
};

export default api;
