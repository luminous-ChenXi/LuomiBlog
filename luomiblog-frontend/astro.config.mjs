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
    },
    build: {
      chunkSizeWarningLimit: 1000
    },
    // 开发环境代理：将 /api 请求转发到本地后端
    server: {
      proxy: {
        '/api': 'http://localhost:8080'
      }
    }
  },

  // 构建配置
  build: {
    format: 'file'
  },

  // 开发服务器配置
  server: {
    port: 4321,
    host: true
  },

  // 站点配置（部署时通过 PUBLIC_SITE_URL 环境变量修改）
  site: process.env.PUBLIC_SITE_URL || 'http://localhost:4321',

  // 输出模式：静态生成
  output: 'static'
});
