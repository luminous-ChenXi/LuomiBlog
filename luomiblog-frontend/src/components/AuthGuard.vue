<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { useUserStore } from '../stores/user';
import { isBackendAvailable } from '../utils/api';

interface Props {
  requireAuth?: boolean;
  requireAdmin?: boolean;
  requireBackend?: boolean;
  redirectTo?: string;
}

const props = withDefaults(defineProps<Props>(), {
  requireAuth: true,
  requireAdmin: false,
  requireBackend: true,
  redirectTo: '/login'
});

const userStore = useUserStore();
const isAuthorized = ref(false);
const isLoading = ref(true);
const backendDown = ref(false);

onMounted(() => {
  checkAuth();
});

const checkAuth = () => {
  isLoading.value = true;

  if (props.requireBackend && !isBackendAvailable()) {
    backendDown.value = true;
    isLoading.value = false;
    return;
  }

  if (props.requireAuth && !userStore.isAuthenticated.value) {
    ElMessage.warning('请先登录');
    window.location.href = props.redirectTo;
    return;
  }

  if (props.requireAdmin) {
    const user = userStore.user.value;
    const role = user?.role?.toLowerCase();
    if (!user || (role !== 'admin' && role !== 'blogger')) {
      ElMessage.error('您没有权限访问此页面');
      window.location.href = '/';
      return;
    }
  }

  isAuthorized.value = true;
  isLoading.value = false;
};
</script>

<template>
  <div v-if="isLoading" class="auth-guard-loading">
    <div class="loading-spinner"></div>
    <p>正在验证权限...</p>
  </div>
  <div v-else-if="backendDown" class="backend-down-notice">
    <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" x2="12" y1="8" y2="12"/><line x1="12" x2="12.01" y1="16" y2="16"/></svg>
    <h3>后端服务不可用</h3>
    <p>此功能需要后端服务支持，当前后端服务不可用。</p>
    <p class="hint">请稍后重试，或返回<a href="/">首页</a>浏览文章。</p>
  </div>
  <slot v-else-if="isAuthorized" />
</template>

<style scoped>
.auth-guard-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  gap: 16px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-border);
  border-top-color: #ff6b9d;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.auth-guard-loading p {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.backend-down-notice {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  gap: 12px;
  text-align: center;
  padding: 40px 24px;
}

.backend-down-notice svg {
  color: var(--color-text-muted);
}

.backend-down-notice h3 {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.backend-down-notice p {
  color: var(--color-text-secondary);
  font-size: 14px;
  margin: 0;
  line-height: 1.6;
}

.backend-down-notice .hint {
  color: var(--color-text-muted);
  font-size: 13px;
}

.backend-down-notice a {
  color: var(--color-brand-primary);
  text-decoration: none;
}

.backend-down-notice a:hover {
  text-decoration: underline;
}
</style>
