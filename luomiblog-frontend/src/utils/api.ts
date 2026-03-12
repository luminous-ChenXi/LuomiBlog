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
  AIAskRequest,
  AIAskResponse,
  InstallStatusResponse,
  EnvironmentCheckResponse,
  DatabaseConfigRequest,
  AdminAccountRequest,
  SiteConfigRequest
} from '../types/api';

// API 基础 URL
const API_BASE_URL = import.meta.env.PUBLIC_API_URL || 'http://localhost:8080';

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

    complete: () =>
      request<{ success: boolean; message: string }>('/api/install/complete', {
        method: 'POST'
      })
  }
};

export default api;
