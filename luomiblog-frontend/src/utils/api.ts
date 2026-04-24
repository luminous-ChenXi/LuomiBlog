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
  SiteConfigRequest,
  SmtpConfigRequest,
  FaviconConfigRequest,
  HealthCheckResponse,
  AdminUser,
  AdminUserUpdateRequest,
  AdminRoleChangeRequest,
  AdminStatusChangeRequest,
  AdminResetPasswordRequest
} from '../types/api';

import { API_BASE_URL, API_CONFIG, API_ERROR_CODES, ApiError } from '../config/api';

interface RequestConfig extends RequestInit {
  params?: Record<string, string | number | boolean | undefined>;
  timeout?: number;
  retries?: number;
  requireBackend?: boolean;
  silent?: boolean;
}

const backendAvailable = { value: true };
let lastBackendCheck = 0;
const BACKEND_CHECK_COOLDOWN = 10000;

function getToken(): string | null {
  if (typeof localStorage !== 'undefined') {
    return localStorage.getItem('token');
  }
  return null;
}

function buildUrl(path: string, params?: Record<string, string | number | boolean | undefined>): string {
  if (path.startsWith('http://') || path.startsWith('https://')) {
    const url = new URL(path);
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          url.searchParams.append(key, String(value));
        }
      });
    }
    return url.toString();
  }
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

function classifyError(error: unknown): ApiError {
  if (error instanceof ApiError) return error;

  if (error instanceof DOMException && error.name === 'AbortError') {
    return new ApiError('请求超时，请稍后重试', API_ERROR_CODES.TIMEOUT);
  }

  if (error instanceof TypeError && error.message.includes('fetch')) {
    return new ApiError('网络连接失败，请检查网络', API_ERROR_CODES.NETWORK_ERROR);
  }

  const message = error instanceof Error ? error.message : '未知错误';
  return new ApiError(message, API_ERROR_CODES.UNKNOWN);
}

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

async function request<T>(path: string, config: RequestConfig = {}): Promise<T> {
  const {
    params,
    timeout = API_CONFIG.timeout,
    retries = API_CONFIG.retryCount,
    requireBackend = true,
    silent = false,
    ...fetchConfig
  } = config;

  if (requireBackend && !backendAvailable.value) {
    const now = Date.now();
    if (now - lastBackendCheck < BACKEND_CHECK_COOLDOWN) {
      throw new ApiError('后端服务不可用，请稍后重试', API_ERROR_CODES.NETWORK_ERROR);
    }
  }

  const url = buildUrl(path, params);

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((fetchConfig.headers as Record<string, string>) || {})
  };

  const token = getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  let lastError: ApiError | null = null;

  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), timeout);

      const response = await fetch(url, {
        ...fetchConfig,
        headers,
        signal: controller.signal
      });

      clearTimeout(timeoutId);

      if (!backendAvailable.value) {
        backendAvailable.value = true;
        lastBackendCheck = Date.now();
        if (!silent) {
          console.info('[API] 后端服务已恢复');
        }
      }

      if (response.status === 401) {
        if (typeof localStorage !== 'undefined') {
          localStorage.removeItem('token');
          localStorage.removeItem('user');
        }
        throw new ApiError('登录已过期，请重新登录', API_ERROR_CODES.AUTH_ERROR, 401);
      }

      if (response.status === 404) {
        throw new ApiError('请求的资源不存在', API_ERROR_CODES.NOT_FOUND, 404);
      }

      if (response.status === 429) {
        throw new ApiError('请求过于频繁，请稍后重试', API_ERROR_CODES.RATE_LIMIT, 429);
      }

      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new ApiError(
          error.message || `服务器错误 (${response.status})`,
          response.status >= 500 ? API_ERROR_CODES.SERVER_ERROR : API_ERROR_CODES.UNKNOWN,
          response.status
        );
      }

      const result: ApiResponse<T> = await response.json();

      if (result.code !== 200) {
        throw new ApiError(result.message || '请求失败', API_ERROR_CODES.SERVER_ERROR, result.code);
      }

      return result.data;
    } catch (error) {
      lastError = classifyError(error);

      if (lastError.code === API_ERROR_CODES.AUTH_ERROR ||
          lastError.code === API_ERROR_CODES.NOT_FOUND ||
          lastError.code === API_ERROR_CODES.RATE_LIMIT) {
        throw lastError;
      }

      if (attempt < retries && lastError.isRetryable) {
        if (!silent) {
          console.warn(`[API] 请求失败，第 ${attempt + 1} 次重试...`, lastError.message);
        }
        await delay(API_CONFIG.retryDelay * (attempt + 1));
        continue;
      }

      if (lastError.code === API_ERROR_CODES.NETWORK_ERROR ||
          lastError.code === API_ERROR_CODES.TIMEOUT ||
          lastError.code === API_ERROR_CODES.SERVER_ERROR) {
        backendAvailable.value = false;
        lastBackendCheck = Date.now();
      }

      throw lastError;
    }
  }

  throw lastError || new ApiError('请求失败', API_ERROR_CODES.UNKNOWN);
}

export const isBackendAvailable = () => backendAvailable.value;

export const setBackendAvailable = (available: boolean) => {
  backendAvailable.value = available;
  lastBackendCheck = Date.now();
};

export const api = {
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

  articles: {
    getList: (page = 0, size = 10, categoryId?: number) =>
      request<PageResponse<Article>>('/api/articles', {
        params: { page, size, categoryId }
      }),

    getBySlug: (slug: string) =>
      request<Article>(`/api/articles/${slug}`, { silent: true }),

    getById: (id: number) =>
      request<Article>(`/api/articles/id/${id}`),

    search: (keyword: string) =>
      request<Article[]>('/api/articles/search', {
        params: { keyword }
      }),

    like: (id: number, userId?: string | null, visitorId?: string) =>
      request<{ likeCount: number; action: string }>(`/api/articles/${id}/like`, {
        method: 'POST',
        body: JSON.stringify({ userId, visitorId })
      }),

    favorite: (id: number, token: string) =>
      request<{ action: string }>(`/api/articles/${id}/favorite`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      }),

    view: (id: number, data: { userId?: string | null; visitorId: string; userAgent: string }) =>
      request<void>(`/api/articles/${id}/view`, {
        method: 'POST',
        body: JSON.stringify(data),
        silent: true,
        requireBackend: false
      })
  },

  categories: {
    getList: () =>
      request<Category[]>('/api/categories'),

    getTree: () =>
      request<Category[]>('/api/categories/tree')
  },

  tags: {
    getList: () =>
      request<Tag[]>('/api/tags'),

    getBySlug: (slug: string) =>
      request<Tag>(`/api/tags/${slug}`)
  },

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

    verifyReinstall: (password: string) =>
      request<{ success: boolean; message: string; needsOptions?: boolean }>('/api/install/verify-reinstall', {
        method: 'POST',
        body: JSON.stringify({ password })
      }),

    getReinstallOptions: () =>
      request<{
        needsOptions: boolean;
        options: Array<{ code: string; name: string; description: string }>;
        warning: string | null;
      }>('/api/install/reinstall-options', {
        method: 'GET'
      }),

    executeReinstall: (option: string, database?: DatabaseConfigRequest) =>
      request<{ success: boolean; message: string; option: string }>('/api/install/reinstall', {
        method: 'POST',
        body: JSON.stringify({ option, database })
      })
  },

  health: {
    check: () =>
      request<HealthCheckResponse>('/api/health', {
        timeout: API_CONFIG.healthCheckTimeout,
        retries: 0,
        silent: true,
        requireBackend: false
      }),

    ping: () =>
      request<string>('/api/health/ping', {
        timeout: API_CONFIG.pingTimeout,
        retries: 0,
        silent: true,
        requireBackend: false
      })
  },

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
      }>('/api/site/config', { silent: true }),

    getFavicon: () =>
      request<string>('/api/site/favicon', { silent: true })
  },

  adminUsers: {
    getList: (page = 0, size = 20, search?: string, role?: string, status?: string) =>
      request<PageResponse<AdminUser>>('/api/admin/users', {
        params: { page, size, search, role, status }
      }),

    getById: (id: number) =>
      request<AdminUser>(`/api/admin/users/${id}`),

    update: (id: number, data: AdminUserUpdateRequest) =>
      request<AdminUser>(`/api/admin/users/${id}`, {
        method: 'PUT',
        body: JSON.stringify(data)
      }),

    changeRole: (id: number, data: AdminRoleChangeRequest) =>
      request<AdminUser>(`/api/admin/users/${id}/role`, {
        method: 'PUT',
        body: JSON.stringify(data)
      }),

    changeStatus: (id: number, data: AdminStatusChangeRequest) =>
      request<AdminUser>(`/api/admin/users/${id}/status`, {
        method: 'PUT',
        body: JSON.stringify(data)
      }),

    delete: (id: number) =>
      request<void>(`/api/admin/users/${id}`, {
        method: 'DELETE'
      }),

    resetPassword: (id: number, data: AdminResetPasswordRequest) =>
      request<void>(`/api/admin/users/${id}/reset-password`, {
        method: 'POST',
        body: JSON.stringify(data)
      })
  }
};

export default api;
