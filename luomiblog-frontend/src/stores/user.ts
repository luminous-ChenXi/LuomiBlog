import { ref, computed } from 'vue';
import type { User, AuthResponse } from '../types/api';

const user = ref<User | null>(null);
const token = ref<string | null>(null);
const refreshToken = ref<string | null>(null);
const isAuthenticated = computed(() => !!token.value && !!user.value);

const REFRESH_TOKEN_KEY = 'refreshToken';

function initAuth() {
  if (typeof localStorage !== 'undefined') {
    const savedToken = localStorage.getItem('token');
    const savedUser = localStorage.getItem('user');
    const savedRefreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
    if (savedToken) {
      token.value = savedToken;
    }
    if (savedUser) {
      try {
        user.value = JSON.parse(savedUser);
      } catch {
        user.value = null;
      }
    }
    if (savedRefreshToken) {
      refreshToken.value = savedRefreshToken;
    }
  }

  if (typeof window !== 'undefined') {
    window.addEventListener('auth-state-changed', ((event: CustomEvent) => {
      if (!event.detail?.authenticated) {
        token.value = null;
        user.value = null;
      }
    }) as EventListener);

    // 辰汐会话滑动续期：api 层静默替换 localStorage 后，同步内存态令牌
    window.addEventListener('auth-token-renewed', ((event: CustomEvent) => {
      if (event.detail?.token) {
        token.value = event.detail.token;
      }
    }) as EventListener);

    // [BUG-050 修复] 后端离线不再清登录态：令牌失效由 401 统一裁决
  }
}

function setAuth(auth: AuthResponse) {
  token.value = auth.token;
  user.value = auth.user;
  refreshToken.value = auth.refreshToken ?? null;
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('token', auth.token);
    localStorage.setItem('user', JSON.stringify(auth.user));
    if (auth.refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, auth.refreshToken);
    } else {
      localStorage.removeItem(REFRESH_TOKEN_KEY);
    }
  }
}

function clearAuth() {
  token.value = null;
  user.value = null;
  refreshToken.value = null;
  if (typeof localStorage !== 'undefined') {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  }
}

/**
 * 真正的登出：先把 access/refresh 令牌交后端吊销（BUG-012 修复），
 * 再清除本地状态。后端不可达时也要保证本地登出成功（幂等）。
 */
async function logout(): Promise<void> {
  const currentRefreshToken = refreshToken.value;
  try {
    const { api } = await import('../utils/api');
    await api.auth.logout(currentRefreshToken);
  } catch {
    // 网络失败不阻塞登出：本地状态必须清除
  } finally {
    clearAuth();
  }
}

function getToken(): string | null {
  return token.value;
}

function getUser(): User | null {
  return user.value;
}

export const useUserStore = () => ({
  user: computed(() => user.value),
  token: computed(() => token.value),
  isAuthenticated,
  initAuth,
  setAuth,
  clearAuth,
  logout,
  getToken,
  getUser
});

export { initAuth, setAuth, clearAuth, logout, getToken, getUser, isAuthenticated };
