import type { APIRoute } from 'astro';

export const GET: APIRoute = async () => {
  const site = 'https://luminouschenxi.com';
  const now = new Date().toISOString();
  
  const atom = `<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom" xml:lang="zh-CN">
  <title>LuomiBlog - AI知识库博客</title>
  <subtitle>程序员向AI原生增强型知识库博客，分享技术文章与学习心得</subtitle>
  <link href="${site}" rel="alternate" type="text/html"/>
  <link href="${site}/atom.xml" rel="self" type="application/atom+xml"/>
  <id>${site}/</id>
  <updated>${now}</updated>
  <author>
    <name>辰汐</name>
    <email>chenxi@luminouschenxi.net</email>
    <uri>${site}</uri>
  </author>
  <logo>${site}/favicon.svg</logo>
  <icon>${site}/favicon.svg</icon>
  <rights>© 2026 LuomiBlog. All rights reserved.</rights>
  
  <entry>
    <title>Spring Boot 3.2 新特性探索</title>
    <link href="${site}/article/spring-boot-3-2-features" rel="alternate" type="text/html"/>
    <id>${site}/article/spring-boot-3-2-features</id>
    <published>2026-02-28T00:00:00Z</published>
    <updated>2026-02-28T00:00:00Z</updated>
    <author>
      <name>辰汐</name>
      <email>chenxi@luminouschenxi.net</email>
    </author>
    <category term="Spring Boot"/>
    <category term="Java"/>
    <summary>Spring Boot 3.2 带来了许多令人兴奋的新特性，包括虚拟线程支持、RestClient、JdbcClient 等。本文将详细介绍这些新特性并给出实战示例。</summary>
    <content type="html"><![CDATA[
      <p>Spring Boot 3.2 是 Spring Boot 3.x 系列的一个重要版本，带来了许多令人期待的新特性。</p>
      <p>本文将深入探讨虚拟线程支持、RestClient、JdbcClient 等新特性。</p>
    ]]></content>
  </entry>
  
  <entry>
    <title>基于 Astro 的静态博客性能优化实践</title>
    <link href="${site}/article/astro-performance-optimization" rel="alternate" type="text/html"/>
    <id>${site}/article/astro-performance-optimization</id>
    <published>2026-03-05T00:00:00Z</published>
    <updated>2026-03-05T00:00:00Z</updated>
    <author>
      <name>辰汐</name>
      <email>chenxi@luminouschenxi.net</email>
    </author>
    <category term="Astro"/>
    <category term="性能优化"/>
    <summary>Astro 是一个现代化的静态站点生成器，本文将分享在使用 Astro 构建博客时的性能优化经验。</summary>
    <content type="html"><![CDATA[
      <p>Astro 是一个现代化的静态站点生成器，以其独特的 Islands 架构而闻名。</p>
      <p>本文将分享在使用 Astro 构建博客时的一些性能优化经验。</p>
    ]]></content>
  </entry>
  
  <entry>
    <title>基于阿里云百炼构建 RAG 知识库系统</title>
    <link href="${site}/article/aliyun-bailian-rag" rel="alternate" type="text/html"/>
    <id>${site}/article/aliyun-bailian-rag</id>
    <published>2025-12-20T00:00:00Z</published>
    <updated>2025-12-20T00:00:00Z</updated>
    <author>
      <name>辰汐</name>
      <email>chenxi@luminouschenxi.net</email>
    </author>
    <category term="AI"/>
    <category term="RAG"/>
    <summary>本文介绍如何利用阿里云百炼平台快速搭建一个基于 RAG（检索增强生成）的知识库问答系统。</summary>
    <content type="html"><![CDATA[
      <p>RAG（Retrieval-Augmented Generation）是一种结合检索和生成的技术。</p>
      <p>本文将介绍如何使用阿里云百炼平台构建 RAG 知识库系统。</p>
    ]]></content>
  </entry>
</feed>`;

  return new Response(atom, {
    headers: {
      'Content-Type': 'application/atom+xml; charset=utf-8'
    }
  });
};
