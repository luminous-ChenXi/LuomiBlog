// @ts-check
import { defineConfig } from 'astro/config';

import vue from '@astrojs/vue';
import tailwindcss from '@tailwindcss/vite';
import partytown from '@astrojs/partytown';

// https://astro.build/config
export default defineConfig({
  integrations: [
    vue({
      appEntrypoint: '/src/app.ts'
    }),
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
    }
  },

  // 构建配置
  build: {
    format: 'file',
    chunkSizeWarningLimit: 1000
  },

  // 开发服务器配置
  server: {
    port: 4321,
    host: true
  },

  // 站点配置（部署时修改）
  site: 'http://localhost:4321',

  // 输出模式：静态生成
  output: 'static',

  // 国际化配置
  i18n: {
    defaultLocale: 'zh',
    locales: ['zh', 'en', 'ja'],
    routing: {
      prefixDefaultLocale: false
    }
  }
});
