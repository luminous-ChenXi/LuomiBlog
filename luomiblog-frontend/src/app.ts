import type { App } from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import { initAuth } from './stores/user';

export default (app: App) => {
  // 注册 Element Plus
  app.use(ElementPlus);

  // 注册所有 Element Plus 图标
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
  }

  // 初始化用户认证状态
  initAuth();

  // 过滤浏览器扩展引起的错误
  if (typeof window !== 'undefined') {
    window.addEventListener('error', (event) => {
      // 忽略浏览器扩展相关的错误
      if (event.message?.includes('runtime.lastError') ||
          event.message?.includes('message port closed')) {
        event.preventDefault();
        console.warn('浏览器扩展错误已忽略:', event.message);
        return true;
      }
      return false;
    });

    // 拦截未处理的 Promise 错误
    window.addEventListener('unhandledrejection', (event) => {
      if (event.reason?.message?.includes('runtime.lastError') ||
          event.reason?.message?.includes('message port closed')) {
        event.preventDefault();
        console.warn('浏览器扩展 Promise 错误已忽略:', event.reason.message);
      }
    });
  }
};
