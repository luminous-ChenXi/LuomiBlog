import type { APIRoute } from 'astro';
import { getSiteUrl } from '../config/site';
import { getFeedItems, escapeXml, escapeCdata } from '../utils/feed';

// 作者邮箱为站主固定值（已确认保留）
const AUTHOR_EMAIL = 'chenxi@luminouschenxi.net';
const AUTHOR_NAME = '辰汐';

export const GET: APIRoute = async ({ site }) => {
  const siteUrl = getSiteUrl(site);
  const items = await getFeedItems(20);

  const itemXml = items.map(item => {
    const link = `${siteUrl}/article/${item.slug}`;
    const categoryLines = item.categories.length
      ? item.categories.map(c => `      <category>${escapeXml(c)}</category>`).join('\n') + '\n'
      : '';
    return `    <item>
      <title>${escapeXml(item.title)}</title>
      <link>${link}</link>
      <guid isPermaLink="true">${link}</guid>
      <pubDate>${item.pubDate.toUTCString()}</pubDate>
      <author>${AUTHOR_EMAIL} (${AUTHOR_NAME})</author>
${categoryLines}      <description><![CDATA[${escapeCdata(item.description)}]]></description>
    </item>`;
  }).join('\n');

  const lastBuildDate = items[0]?.pubDate ? items[0].pubDate.toUTCString() : new Date().toUTCString();

  const rss = `<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom" xmlns:content="http://purl.org/rss/1.0/modules/content/">
  <channel>
    <title>LuomiBlog - AI知识库博客</title>
    <link>${siteUrl}</link>
    <description>程序员向AI原生增强型知识库博客，分享技术文章与学习心得</description>
    <language>zh-CN</language>
    <lastBuildDate>${lastBuildDate}</lastBuildDate>
    <atom:link href="${siteUrl}/rss.xml" rel="self" type="application/rss+xml"/>
    <image>
      <url>${siteUrl}/favicon.svg</url>
      <title>LuomiBlog</title>
      <link>${siteUrl}</link>
    </image>
${itemXml}
  </channel>
</rss>`;

  return new Response(rss, {
    headers: {
      'Content-Type': 'application/xml; charset=utf-8'
    }
  });
};
