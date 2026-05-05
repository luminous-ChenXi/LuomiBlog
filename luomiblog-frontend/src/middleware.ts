import { defineMiddleware } from 'astro:middleware';

const securityHeaders: Record<string, string> = {
  'X-Content-Type-Options': 'nosniff',
  'X-Frame-Options': 'SAMEORIGIN',
  'X-XSS-Protection': '1; mode=block',
  'Referrer-Policy': 'strict-origin-when-cross-origin',
  'Permissions-Policy': 'camera=(), microphone=(), geolocation=()',
};

const BACKEND_DEPENDENT_PATHS = ['/admin', '/user'];

async function checkBackendHealth(): Promise<boolean> {
  try {
    const apiBaseUrl = import.meta.env.PUBLIC_API_URL || 'http://localhost:8080';
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 3000);
    const response = await fetch(`${apiBaseUrl}/api/health/ping`, {
      signal: controller.signal,
    });
    clearTimeout(timeoutId);
    return response.ok;
  } catch {
    return false;
  }
}

export const onRequest = defineMiddleware(async (context, next) => {
  const pathname = context.url.pathname;

  const isBackendDependent = BACKEND_DEPENDENT_PATHS.some(
    (p) => pathname === p || pathname.startsWith(`${p}/`)
  );

  if (isBackendDependent) {
    const backendOk = await checkBackendHealth();
    if (!backendOk) {
      const html = `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>服务不可用 - LuomiBlog</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      background: var(--color-bg, #fafafa);
      color: var(--color-text, #1a1a2e);
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      padding: 24px;
    }
    :root {
      --color-bg: #fafafa;
      --color-text: #1a1a2e;
      --color-text-secondary: #6b7280;
      --color-brand-primary: #ff6b9d;
      --color-border: #e5e7eb;
    }
    @media (prefers-color-scheme: dark) {
      :root {
        --color-bg: #0f0f1a;
        --color-text: #e5e7eb;
        --color-text-secondary: #9ca3af;
        --color-border: #374151;
      }
    }
    .container {
      text-align: center;
      max-width: 480px;
    }
    .icon {
      width: 64px;
      height: 64px;
      margin: 0 auto 24px;
      color: var(--color-text-secondary);
    }
    h1 {
      font-size: 24px;
      font-weight: 700;
      margin-bottom: 12px;
    }
    p {
      color: var(--color-text-secondary);
      font-size: 15px;
      line-height: 1.7;
      margin-bottom: 8px;
    }
    .hint {
      font-size: 13px;
      color: var(--color-text-secondary);
      opacity: 0.8;
    }
    a {
      color: var(--color-brand-primary);
      text-decoration: none;
    }
    a:hover {
      text-decoration: underline;
    }
    .btn {
      display: inline-block;
      margin-top: 24px;
      padding: 10px 24px;
      background: var(--color-brand-primary);
      color: white;
      border-radius: 8px;
      text-decoration: none;
      font-weight: 500;
      transition: opacity 0.2s ease;
    }
    .btn:hover {
      opacity: 0.9;
      text-decoration: none;
    }
  </style>
</head>
<body>
  <div class="container">
    <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
      <circle cx="12" cy="12" r="10"/>
      <line x1="12" y1="8" x2="12" y2="12"/>
      <line x1="12" y1="16" x2="12.01" y2="16"/>
    </svg>
    <h1>后端服务不可用</h1>
    <p>此页面需要后端服务支持，当前无法连接到后端服务。</p>
    <p class="hint">博客文章仍可正常浏览，但登录、管理等需要后端的功能暂不可用。</p>
    <a href="/" class="btn">返回首页</a>
  </div>
</body>
</html>`;
      return new Response(html, {
        status: 503,
        headers: { 'Content-Type': 'text/html; charset=utf-8' },
      });
    }
  }

  const response = await next();

  for (const [key, value] of Object.entries(securityHeaders)) {
    response.headers.set(key, value);
  }

  if (pathname.startsWith('/_astro/')) {
    response.headers.set('Cache-Control', 'public, max-age=31536000, immutable');
  }

  if (pathname.startsWith('/admin') ||
      pathname.startsWith('/install') ||
      pathname.startsWith('/user')) {
    response.headers.set('Cache-Control', 'no-store');
    if (!response.headers.has('Content-Security-Policy')) {
      response.headers.set(
        'Content-Security-Policy',
        "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com https://cdn.jsdelivr.net; img-src 'self' data: https:; connect-src 'self' http://localhost:* https://*; frame-ancestors 'none'"
      );
    }
  }

  return response;
});
