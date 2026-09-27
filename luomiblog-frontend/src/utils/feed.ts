import { getCollection } from 'astro:content';
import { API_BASE_URL } from '../config/api';

/**
 * RSS/Atom 订阅源数据：content collection + 后端 DB 双源合并
 * （与文章详情页同一套双源思路：本地 Markdown 为基底，DB 文章补齐/追加）
 */

export interface FeedItem {
  title: string;
  slug: string;
  pubDate: Date;
  updatedDate: Date;
  description: string;
  categories: string[];
}

interface DbArticle {
  title?: string;
  slug?: string;
  summary?: string;
  description?: string;
  categoryName?: string;
  category?: { name?: string };
  tags?: Array<{ name?: string }>;
  authorName?: string;
  author?: { nickname?: string; username?: string };
  publishedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

/** 从后端 DB 取最新文章（构建时后端不可用则回退空数组） */
async function getDbArticles(): Promise<FeedItem[]> {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000);
    const response = await fetch(`${API_BASE_URL}/api/articles?page=0&size=50`, {
      signal: controller.signal
    });
    clearTimeout(timeoutId);
    if (!response.ok) return [];

    const result = await response.json();
    if (result.code !== 200 || !result.data?.content) return [];

    return (result.data.content as DbArticle[])
      .filter(a => a.slug && a.title)
      .map(a => {
        const published = new Date(a.publishedAt || a.createdAt || Date.now());
        const updated = new Date(a.updatedAt || a.publishedAt || a.createdAt || Date.now());
        const categories = [
          a.categoryName || a.category?.name,
          ...(a.tags || []).map(t => t.name).filter((n): n is string => !!n)
        ].filter((n): n is string => !!n);
        return {
          title: a.title as string,
          slug: a.slug as string,
          pubDate: published,
          updatedDate: updated,
          description: a.summary || a.description || '',
          categories
        };
      })
      .filter(item => !Number.isNaN(item.pubDate.getTime()));
  } catch {
    return [];
  }
}

/** content collection 条目转 feed 项 */
async function getCollectionItems(): Promise<FeedItem[]> {
  const posts = await getCollection('blog');
  return posts.map(p => {
    const pubDate = new Date(p.data.pubDate);
    const categories = [p.data.category, ...p.data.tags].filter(
      (n): n is string => !!n
    );
    return {
      title: p.data.title,
      slug: p.slug,
      pubDate,
      updatedDate: pubDate,
      description: p.data.description || '',
      categories
    };
  });
}

/**
 * 合并双源：按 slug 去重（DB 记录优先，时间戳更真实），按发布时间倒序，取前 limit 篇
 */
export async function getFeedItems(limit = 20): Promise<FeedItem[]> {
  const [dbItems, localItems] = await Promise.all([getDbArticles(), getCollectionItems()]);

  const bySlug = new Map<string, FeedItem>();
  for (const item of localItems) {
    bySlug.set(item.slug, item);
  }
  for (const item of dbItems) {
    bySlug.set(item.slug, item); // DB 优先
  }

  return [...bySlug.values()]
    .filter(item => !Number.isNaN(item.pubDate.getTime()))
    .sort((a, b) => b.pubDate.getTime() - a.pubDate.getTime())
    .slice(0, limit);
}

/** XML 文本转义 */
export function escapeXml(text: string): string {
  const map: Record<string, string> = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&apos;' };
  return text.replace(/[&<>"']/g, m => map[m]);
}

/** CDATA 内容转义（仅 ]]></ 结束序列有风险） */
export function escapeCdata(text: string): string {
  return text.replace(/\]\]>/g, ']]]]><![CDATA[>');
}
