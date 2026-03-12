<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { isAuthenticated, getUser } from '../stores/user';

interface Props {
  requireAuth?: boolean;
  requireAdmin?: boolean;
  redirectTo?: string;
}

const props = withDefaults(defineProps<Props>(), {
  requireAuth: true,
  requireAdmin: false,
  redirectTo: '/login'
});

const isAuthorized = ref(false);
const isLoading = ref(true);

onMounted(() => {
  checkAuth();
});

const checkAuth = () => {
  isLoading.value = true;
  
  // 检查是否需要登录
  if (props.requireAuth && !isAuthenticated()) {
    ElMessage.warning('请先登录');
    window.location.href = props.redirectTo;
    return;
  }
  
  // 检查是否需要管理员权限
  if (props.requireAdmin) {
    const user = getUser();
    if (!user || user.role !== 'ADMIN') {
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
</style>
