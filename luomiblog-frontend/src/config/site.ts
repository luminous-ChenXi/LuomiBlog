// 站点配置

// 获取站点 URL
// 优先级：PUBLIC_SITE_URL 环境变量 > Astro 站点配置（site）> 本地开发地址
export function getSiteUrl(site?: URL | string | null): string {
  const url =
    import.meta.env.PUBLIC_SITE_URL ||
    (typeof site === 'string' ? site : site?.toString()) ||
    '';
  return url.replace(/\/+$/, '') || 'http://localhost:4321';
}
