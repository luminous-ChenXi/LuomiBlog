import type { App } from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import { initAuth } from './stores/user';
import { ApiError, API_ERROR_CODES } from './config/api';

export default (app: App) => {
  app.use(ElementPlus);

  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
  }

  initAuth();

  if (typeof window !== 'undefined') {
    window.addEventListener('error', (event) => {
      if (event.message?.includes('runtime.lastError') ||
          event.message?.includes('message port closed')) {
        event.preventDefault();
        return true;
      }

      if (event.message?.includes('Failed to fetch') ||
          event.message?.includes('NetworkError') ||
          event.message?.includes('net::ERR_')) {
        console.warn('[Global] 网络错误已静默处理:', event.message);
        event.preventDefault();
        return true;
      }

      return false;
    });

    window.addEventListener('unhandledrejection', (event) => {
      if (event.reason?.message?.includes('runtime.lastError') ||
          event.reason?.message?.includes('message port closed')) {
        event.preventDefault();
        return;
      }

      if (event.reason instanceof ApiError) {
        event.preventDefault();

        if (event.reason.code === API_ERROR_CODES.NETWORK_ERROR ||
            event.reason.code === API_ERROR_CODES.TIMEOUT) {
          console.warn('[Global] 后端服务不可用，已静默处理');
        } else if (event.reason.code === API_ERROR_CODES.AUTH_ERROR) {
          console.info('[Global] 认证已过期');
        } else {
          console.warn('[Global] API 错误:', event.reason.message);
        }
        return;
      }

      if (event.reason?.message?.includes('Failed to fetch') ||
          event.reason?.message?.includes('NetworkError') ||
          event.reason?.name === 'AbortError') {
        event.preventDefault();
        console.warn('[Global] 网络请求错误已静默处理');
        return;
      }
    });
  }
};
