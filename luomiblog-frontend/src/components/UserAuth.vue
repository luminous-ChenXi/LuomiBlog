<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useUserStore, clearAuth } from '../stores/user';
import type { User } from '../types/api';

const userStore = useUserStore();
const isMenuOpen = ref(false);
const isClient = ref(false);

// 只在客户端渲染后显示登录状态，避免 hydration 不匹配
const showAuthenticated = computed(() => {
  return isClient.value && userStore.isAuthenticated.value;
});

const showUnauthenticated = computed(() => {
  return isClient.value && !userStore.isAuthenticated.value;
});

const handleLoginClick = () => {
  // 触发打开登录弹窗事件
  window.dispatchEvent(new CustomEvent('open-login-modal'));
};

const handleRegisterClick = () => {
  // 触发打开注册弹窗事件
  window.dispatchEvent(new CustomEvent('open-register-modal'));
};

const handleLogout = () => {
  clearAuth();
  isMenuOpen.value = false;
  // 刷新页面以更新状态
  window.location.reload();
};

const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value;
};

// 点击外部关闭菜单
const handleClickOutside = (e: MouseEvent) => {
  const target = e.target as HTMLElement;
  if (!target.closest('.user-auth-wrapper')) {
    isMenuOpen.value = false;
  }
};

onMounted(() => {
  isClient.value = true;
  document.addEventListener('click', handleClickOutside);
});
</script>

<template>
  <div class="user-auth-wrapper">
    <!-- 服务端渲染时显示占位，避免 hydration 不匹配 -->
    <div v-if="!isClient" class="auth-links-placeholder">
      <span class="auth-link-placeholder">登录</span>
      <span class="auth-divider">or</span>
      <span class="auth-link-placeholder">注册</span>
    </div>

    <!-- 未登录状态 -->
    <div v-else-if="showUnauthenticated" class="auth-links">
      <button class="auth-link login-link" @click="handleLoginClick">
        登录
      </button>
      <span class="auth-divider">or</span>
      <button class="auth-link register-link" @click="handleRegisterClick">
        注册
      </button>
      <span class="new-tag">NEW</span>
    </div>

    <!-- 已登录状态 -->
    <div v-else-if="showAuthenticated" class="user-menu-wrapper">
      <button class="user-menu-trigger" @click="toggleMenu">
        <div class="user-avatar">
          {{ userStore.user.value?.nickname?.charAt(0) || userStore.user.value?.username.charAt(0) || 'U' }}
        </div>
        <span class="user-name">{{ userStore.user.value?.nickname || userStore.user.value?.username }}</span>
        <svg 
          class="dropdown-arrow" 
          :class="{ open: isMenuOpen }"
          viewBox="0 0 24 24" 
          fill="none" 
          stroke="currentColor" 
          stroke-width="2"
        >
          <path d="m6 9 6 6 6-6"/>
        </svg>
      </button>

      <!-- 下拉菜单 -->
      <div v-if="isMenuOpen" class="user-dropdown-menu">
        <a href="/user" class="dropdown-item">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/>
            <circle cx="12" cy="7" r="4"/>
          </svg>
          个人中心
        </a>
        <a v-if="userStore.user.value?.role && ['admin', 'blogger'].includes(userStore.user.value.role.toLowerCase())" 
           href="/admin" 
           class="dropdown-item"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect width="18" height="18" x="3" y="3" rx="2" ry="2"/>
            <line x1="3" x2="21" y1="9" y2="9"/>
            <line x1="9" x2="9" y1="21" y2="9"/>
          </svg>
          管理后台
        </a>
        <div class="dropdown-divider"></div>
        <button class="dropdown-item logout-item" @click="handleLogout">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" x2="9" y1="12" y2="12"/>
          </svg>
          退出登录
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-auth-wrapper {
  position: relative;
}

/* 服务端渲染占位符 */
.auth-links-placeholder {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  opacity: 0.6;
}

.auth-link-placeholder {
  padding: 0.5rem 1rem;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-tertiary, #999);
}

/* 未登录状态 */
.auth-links {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.auth-link {
  padding: 0.5rem 1rem;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.2s ease;
  cursor: pointer;
  border: none;
  background: transparent;
}

.login-link {
  color: var(--color-text, #333);
}

.login-link:hover {
  background: var(--color-bg-secondary, #f5f5f5);
}

.register-link {
  background: linear-gradient(135deg, #F9A8C8 0%, #E87A9F 100%);
  color: white;
}

.register-link:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.auth-divider {
  color: var(--color-text-tertiary, #999);
  font-size: 0.75rem;
}

.new-tag {
  padding: 0.125rem 0.375rem;
  background: linear-gradient(135deg, #F9A8C8 0%, #E87A9F 100%);
  color: white;
  font-size: 0.625rem;
  font-weight: 600;
  border-radius: 4px;
  margin-left: 0.25rem;
}

/* 已登录状态 */
.user-menu-wrapper {
  position: relative;
}

.user-menu-trigger {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.15);
  cursor: pointer;
  transition: all 0.2s ease;
}

.user-menu-trigger:hover {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.4);
}

:global(.navbar.scrolled) .user-menu-trigger {
  border: 1px solid var(--color-border, #e5e5e5);
  background: var(--color-card-bg, white);
}

:global(.navbar.scrolled) .user-menu-trigger:hover {
  border-color: var(--color-primary, #F9A8C8);
  box-shadow: 0 2px 8px rgba(249, 168, 200, 0.15);
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #F9A8C8 0%, #E87A9F 100%);
  color: white;
  font-size: 0.75rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-name {
  font-size: 0.875rem;
  font-weight: 500;
  color: white;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.navbar.scrolled) .user-name {
  color: var(--color-text, #333);
}

.dropdown-arrow {
  width: 16px;
  height: 16px;
  color: rgba(255, 255, 255, 0.8);
  transition: transform 0.2s ease;
}

:global(.navbar.scrolled) .dropdown-arrow {
  color: var(--color-text-secondary, #666);
}

.dropdown-arrow.open {
  transform: rotate(180deg);
}

/* 下拉菜单 */
.user-dropdown-menu {
  position: absolute;
  top: calc(100% + 0.5rem);
  right: 0;
  min-width: 180px;
  background: var(--color-card, white);
  border: 1px solid var(--color-border, #e5e5e5);
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  padding: 0.5rem;
  z-index: 100;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 0.75rem;
  border-radius: 8px;
  font-size: 0.875rem;
  color: var(--color-text, #333);
  text-decoration: none;
  transition: all 0.2s ease;
  cursor: pointer;
  border: none;
  background: transparent;
  width: 100%;
  text-align: left;
}

.dropdown-item:hover {
  background: var(--color-bg-secondary, #f5f5f5);
}

.dropdown-item svg {
  width: 18px;
  height: 18px;
  color: var(--color-text-secondary, #666);
}

.dropdown-divider {
  height: 1px;
  background: var(--color-border, #e5e5e5);
  margin: 0.5rem 0;
}

.logout-item {
  color: #dc2626;
}

.logout-item:hover {
  background: #fef2f2;
}

.logout-item svg {
  color: #dc2626;
}
</style>
