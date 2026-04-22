<template>
  <slot v-if="!error" />
  <div v-else class="error-boundary">
    <div class="error-boundary-content">
      <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="error-icon"><circle cx="12" cy="12" r="10"/><line x1="12" x2="12" y1="8" y2="12"/><line x1="12" x2="12.01" y1="16" y2="16"/></svg>
      <h4 class="error-title">{{ title }}</h4>
      <p class="error-message">{{ message }}</p>
      <button v-if="showRetry" class="retry-btn" @click="handleRetry">
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
        重试
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onErrorCaptured } from 'vue';

const props = withDefaults(defineProps<{
  title?: string;
  message?: string;
  showRetry?: boolean;
}>(), {
  title: '组件加载失败',
  message: '该功能暂时不可用，请稍后重试',
  showRetry: true
});

const error = ref<Error | null>(null);

onErrorCaptured((err) => {
  error.value = err;
  console.warn('[ErrorBoundary] 捕获到组件错误:', err.message);
  return false;
});

const handleRetry = () => {
  error.value = null;
};
</script>

<style scoped>
.error-boundary {
  padding: 24px;
  border-radius: var(--radius-lg, 12px);
  border: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-card, #fff);
  text-align: center;
}

.error-boundary-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.error-icon {
  color: var(--color-text-muted, #9ca3af);
}

.error-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text, #1f2937);
}

.error-message {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-secondary, #6b7280);
  line-height: 1.5;
}

.retry-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: var(--radius-full, 9999px);
  border: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-card, #fff);
  color: var(--color-text-secondary, #6b7280);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease-in-out;
}

.retry-btn:hover {
  border-color: var(--color-brand-primary, #ff6b9d);
  color: var(--color-brand-primary, #ff6b9d);
}
</style>
