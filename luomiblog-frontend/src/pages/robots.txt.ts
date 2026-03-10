import type { APIRoute } from 'astro';

export const GET: APIRoute = async () => {
  const site = 'https://luminouschenxi.com';
  
  const robots = `User-agent: *
Allow: /

# Sitemap
Sitemap: ${site}/sitemap.xml

# RSS Feeds
Sitemap: ${site}/rss.xml
Sitemap: ${site}/atom.xml

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
