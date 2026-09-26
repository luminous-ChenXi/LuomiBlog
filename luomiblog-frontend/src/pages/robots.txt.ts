import type { APIRoute } from 'astro';
import { getSiteUrl } from '../config/site';

export const GET: APIRoute = async ({ site }) => {
  const siteUrl = getSiteUrl(site);

  const robots = `User-agent: *
Allow: /

# Sitemap
Sitemap: ${siteUrl}/sitemap.xml

# RSS Feeds
Sitemap: ${siteUrl}/rss.xml
Sitemap: ${siteUrl}/atom.xml

# Disallow admin and private routes
Disallow: /admin/
Disallow: /api/

# Crawl-delay for better server performance
Crawl-delay: 1
`;

  return new Response(robots, {
    headers: {
      'Content-Type': 'text/plain; charset=utf-8'
    }
  });
};
