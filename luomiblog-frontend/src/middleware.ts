import { defineMiddleware } from 'astro:middleware';

const securityHeaders: Record<string, string> = {
  'X-Content-Type-Options': 'nosniff',
  'X-Frame-Options': 'SAMEORIGIN',
  'X-XSS-Protection': '1; mode=block',
  'Referrer-Policy': 'strict-origin-when-cross-origin',
  'Permissions-Policy': 'camera=(), microphone=(), geolocation=()',
};

export const onRequest = defineMiddleware(async (context, next) => {
  const response = await next();

  for (const [key, value] of Object.entries(securityHeaders)) {
    response.headers.set(key, value);
  }

  if (context.url.pathname.startsWith('/_astro/')) {
    response.headers.set('Cache-Control', 'public, max-age=31536000, immutable');
  }

  if (context.url.pathname.startsWith('/admin') ||
      context.url.pathname.startsWith('/install') ||
      context.url.pathname.startsWith('/user')) {
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
