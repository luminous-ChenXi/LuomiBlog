// @ts-check
import { defineConfig } from 'astro/config';
import vue from '@astrojs/vue';
import tailwindcss from '@tailwindcss/vite';
import partytown from '@astrojs/partytown';
import mdx from '@astrojs/mdx';
import sitemap from '@astrojs/sitemap';
import remarkMath from 'remark-math';
import remarkToc from 'remark-toc';
import rehypeKatex from 'rehype-katex';
import remarkGfm from 'remark-gfm';

export default defineConfig({
  integrations: [
    vue({
      appEntrypoint: '/src/app.ts'
    }),
    mdx({
      remarkPlugins: [remarkGfm, remarkMath, [remarkToc, { tight: true, heading: '目录' }]],
      rehypePlugins: [rehypeKatex],
    }),
    sitemap(),
    partytown({
      config: {
        forward: ['dataLayer.push'],
      },
    })
  ],

  vite: {
    plugins: [tailwindcss()],
    ssr: {
      noExternal: ['element-plus']
    },
    build: {
      chunkSizeWarningLimit: 1000
    }
  },

  build: {
    format: 'file'
  },

  server: {
    port: 4321,
    host: true
  },

  site: 'https://luomiblog.com',

  output: 'static',

  markdown: {
    gfm: true,
    shikiConfig: {
      theme: 'github-dark',
      wrap: true,
    },
    remarkPlugins: [remarkGfm, remarkMath, [remarkToc, { tight: true, heading: '目录' }]],
    rehypePlugins: [rehypeKatex],
  },

  i18n: {
    defaultLocale: 'zh',
    locales: ['zh', 'en', 'ja'],
    routing: {
      prefixDefaultLocale: false
    }
  }
});
