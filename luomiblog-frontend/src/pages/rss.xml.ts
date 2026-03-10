import type { APIRoute } from 'astro';

export const GET: APIRoute = async () => {
  const site = 'https://luminouschenxi.com';
  
  const rss = `<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom" xmlns:content="http://purl.org/rss/1.0/modules/content/">
  <channel>
    <title>LuomiBlog - AI知识库博客</title>
    <link>${site}</link>
    <description>程序员向AI原生增强型知识库博客，分享技术文章与学习心得</description>
    <language>zh-CN</language>
    <lastBuildDate>${new Date().toUTCString()}</lastBuildDate>
    <atom:link href="${site}/rss.xml" rel="self" type="application/rss+xml"/>
    <image>
      <url>${site}/favicon.svg</url>
      <title>LuomiBlog</title>
      <link>${site}</link>
    </image>
    <item>
      <title>Spring Boot 3.2 新特性探索</title>
      <link>${site}/article/spring-boot-3-2-features</link>
      <guid isPermaLink="true">${site}/article/spring-boot-3-2-features</guid>
      <pubDate>Thu, 28 Feb 2026 00:00:00 GMT</pubDate>
      <author>chenxi@luminouschenxi.net (辰汐)</author>
      <category>Spring Boot</category>
      <category>Java</category>
      <description><![CDATA[Spring Boot 3.2 带来了许多令人兴奋的新特性，包括虚拟线程支持、RestClient、JdbcClient 等。本文将详细介绍这些新特性并给出实战示例。]]></description>
    </item>
    <item>
      <title>基于 Astro 的静态博客性能优化实践</title>
      <link>${site}/article/astro-performance-optimization</link>
      <guid isPermaLink="true">${site}/article/astro-performance-optimization</guid>
      <pubDate>Wed, 05 Mar 2026 00:00:00 GMT</pubDate>
      <author>chenxi@luminouschenxi.net (辰汐)</author>
      <category>Astro</category>
      <category>性能优化</category>
      <description><![CDATA[Astro 是一个现代化的静态站点生成器，本文将分享在使用 Astro 构建博客时的性能优化经验。]]></description>
    </item>
    <item>
      <title>基于阿里云百炼构建 RAG 知识库系统</title>
      <link>${site}/article/aliyun-bailian-rag</link>
      <guid isPermaLink="true">${site}/article/aliyun-bailian-rag</guid>
      <pubDate>Fri, 20 Dec 2025 00:00:00 GMT</pubDate>
      <author>chenxi@luminouschenxi.net (辰汐)</author>
      <category>AI</category>
      <category>RAG</category>
      <description><![CDATA[本文介绍如何利用阿里云百炼平台快速搭建一个基于 RAG（检索增强生成）的知识库问答系统。]]></description>
    </item>
  </channel>
</rss>`;

  return new Response(rss, {
    headers: {
      'Content-Type': 'application/xml; charset=utf-8'
    }
  });
};
