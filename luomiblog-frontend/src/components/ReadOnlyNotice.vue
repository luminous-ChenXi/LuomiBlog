<template>
  <div v-if="isUnavailable && showNotice" class="read-only-notice">
    <el-alert
      :title="title"
      :type="alertType"
      :closable="true"
      @close="showNotice = false"
    >
      <template #default>
        <div class="notice-content">
          <p>{{ message }}</p>
          <div v-if="suggestions.length > 0" class="suggestions">
            <span class="suggestion-label">建议：</span>
            <span class="suggestion-item" v-for="(suggestion, index) in suggestions" :key="index">
              {{ suggestion }}{{ index < suggestions.length - 1 ? '、' : '' }}
            </span>
          </div>
        </div>
      </template>
    </el-alert>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useBackendStatus } from '../composables/useBackendStatus';

const props = defineProps<{
  context?: 'article' | 'comment' | 'like' | 'login' | 'general';
}>();

const { isUnavailable, backendStatus } = useBackendStatus();
const showNotice = ref(true);

const alertType = computed(() => {
  if (!backendStatus.value) return 'info';
  switch (backendStatus.value.status) {
    case 'degraded':
      return 'warning';
    case 'unhealthy':
    default:
      return 'error';
  }
});

const title = computed(() => {
  switch (props.context) {
    case 'article':
      return '文章浏览模式';
    case 'comment':
      return '评论功能受限';
    case 'like':
      return '点赞功能受限';
    case 'login':
      return '登录功能受限';
    default:
      return '功能受限';
  }
});

const message = computed(() => {
  if (!backendStatus.value) {
    return '后端服务连接失败，部分功能暂时不可用。';
  }
  
  switch (props.context) {
    case 'article':
      return '您可以正常阅读文章，但部分交互功能（如点赞、评论）暂时不可用。';
    case 'comment':
      return '暂时无法发表评论，请稍后再试。';
    case 'like':
      return '暂时无法点赞，请稍后再试。';
    case 'login':
      return '暂时无法登录，请稍后再试。';
    default:
      return backendStatus.value.message || '部分功能暂时不可用。';
  }
});

const suggestions = computed(() => {
  return backendStatus.value?.suggestions?.slice(0, 2) || [];
});
</script>

<style scoped>
.read-only-notice {
  margin-bottom: 16px;
}

.notice-content {
  font-size: 13px;
  line-height: 1.6;
}

.notice-content p {
  margin: 0 0 8px 0;
}

.suggestions {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.suggestion-label {
  font-weight: 500;
}

.suggestion-item {
  display: inline;
}
</style>
