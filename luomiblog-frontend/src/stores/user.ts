import { ref, computed } from 'vue';
import type { User, AuthResponse } from '../types/api';

const user = ref<User | null>(null);
const token = ref<string | null>(null);
const isAuthenticated = computed(() => !!token.value && !!user.value);

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

  if (typeof window !== 'undefined') {
    window.addEventListener('auth-state-changed', ((event: CustomEvent) => {
      if (!event.detail?.authenticated) {
        token.value = null;
        user.value = null;
      }
    }) as EventListener);

    window.addEventListener('backend-status-changed', ((event: CustomEvent) => {
      if (!event.detail?.available) {
        token.value = null;
        user.value = null;
      }
    }) as EventListener);
  }
}

function setAuth(auth: AuthResponse) {
  token.value = auth.token;
  user.value = auth.user;
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('token', auth.token);
    localStorage.setItem('user', JSON.stringify(auth.user));
  }
}

function clearAuth() {
  token.value = null;
  user.value = null;
  if (typeof localStorage !== 'undefined') {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
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
  getToken,
  getUser
});

export { initAuth, setAuth, clearAuth, getToken, getUser, isAuthenticated };
