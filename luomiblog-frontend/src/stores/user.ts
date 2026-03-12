import { ref, computed } from 'vue';
import type { User, AuthResponse } from '../types/api';

// 用户状态
const user = ref<User | null>(null);
const token = ref<string | null>(null);
const isAuthenticated = computed(() => !!token.value && !!user.value);

// 从 localStorage 恢复状态
function initAuth() {
  if (typeof localStorage !== 'undefined') {
    const savedToken = localStorage.getItem('token');
    const savedUser = localStorage.getItem('user');
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
  }
}

// 设置认证信息
function setAuth(auth: AuthResponse) {
  token.value = auth.token;
  user.value = auth.user;
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('token', auth.token);
    localStorage.setItem('user', JSON.stringify(auth.user));
  }
}

// 清除认证信息
function clearAuth() {
  token.value = null;
  user.value = null;
  if (typeof localStorage !== 'undefined') {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }
}

// 获取 Token
function getToken(): string | null {
  return token.value;
}

// 获取当前用户
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
  getToken,
  getUser
});

// 导出单例方法供非组件使用
export { initAuth, setAuth, clearAuth, getToken, getUser };
