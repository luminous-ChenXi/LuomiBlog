import type { APIRoute } from 'astro';
import { getSiteUrl } from '../config/site';
import { getFeedItems, escapeXml, escapeCdata } from '../utils/feed';

// 作者邮箱为站主固定值（已确认保留）
const AUTHOR_EMAIL = 'chenxi@luminouschenxi.net';
const AUTHOR_NAME = '辰汐';

export const GET: APIRoute = async ({ site }) => {
  const siteUrl = getSiteUrl(site);
  const items = await getFeedItems(20);
  const now = new Date().toISOString();

  const entries = items.map(item => {
    const link = `${siteUrl}/article/${item.slug}`;
    const categoryLines = item.categories.length
      ? item.categories.map(c => `    <category term="${escapeXml(c)}"/>`).join('\n') + '\n'
      : '';
    return `  <entry>
    <title>${escapeXml(item.title)}</title>
    <link href="${link}" rel="alternate" type="text/html"/>
    <id>${link}</id>
    <published>${item.pubDate.toISOString()}</published>
    <updated>${item.updatedDate.toISOString()}</updated>
    <author>
      <name>${AUTHOR_NAME}</name>
      <email>${AUTHOR_EMAIL}</email>
    </author>
${categoryLines}    <summary>${escapeXml(item.description)}</summary>
    <content type="html"><![CDATA[${escapeCdata(item.description)}]]></content>
  </entry>`;
  }).join('\n');

  const atom = `<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom" xml:lang="zh-CN">
  <title>LuomiBlog - AI知识库博客</title>
  <subtitle>程序员向AI原生增强型知识库博客，分享技术文章与学习心得</subtitle>
  <link href="${siteUrl}" rel="alternate" type="text/html"/>
  <link href="${siteUrl}/atom.xml" rel="self" type="application/atom+xml"/>
  <id>${siteUrl}/</id>
  <updated>${now}</updated>
  <author>
    <name>${AUTHOR_NAME}</name>
    <email>${AUTHOR_EMAIL}</email>
    <uri>${siteUrl}</uri>
  </author>
  <logo>${siteUrl}/favicon.svg</logo>
  <icon>${siteUrl}/favicon.svg</icon>
  <rights>© ${new Date().getFullYear()} LuomiBlog. All rights reserved.</rights>
${entries}
</feed>`;

  return new Response(atom, {
    headers: {
      'Content-Type': 'application/atom+xml; charset=utf-8'
    }
  });
};
