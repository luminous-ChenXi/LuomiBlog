<template>
  <transition name="slide-down">
    <div
      v-if="shouldShowBanner && showBanner"
      class="backend-status-banner"
      :class="bannerClass"
    >
      <div class="banner-content">
        <el-icon class="banner-icon" :size="18">
          <Warning v-if="statusType === 'warning'" />
          <CircleClose v-else-if="statusType === 'error'" />
          <InfoFilled v-else />
        </el-icon>

        <span class="banner-message">{{ statusMessage }}</span>

        <el-button
          v-if="needsInstall"
          type="primary"
          size="small"
          @click="goToInstall"
        >
          前往安装
        </el-button>

        <el-button
          v-else-if="isUnavailable"
          type="primary"
          size="small"
          :loading="isChecking"
          @click="retryCheck"
        >
          重试连接
        </el-button>

        <el-button
          v-if="suggestions.length > 0"
          type="info"
          size="small"
          text
          @click="showDetails = !showDetails"
        >
          {{ showDetails ? '收起' : '详情' }}
        </el-button>

        <el-button
          class="close-btn"
          type="info"
          size="small"
          text
          @click="dismissBanner"
        >
          <el-icon><Close /></el-icon>
        </el-button>
      </div>

      <transition name="fade">
        <div v-if="showDetails && suggestions.length > 0" class="banner-details">
          <div class="details-title">建议操作：</div>
          <ul class="details-list">
            <li v-for="(suggestion, index) in suggestions" :key="index">
              {{ suggestion }}
            </li>
          </ul>

          <div v-if="backendStatus?.components?.database?.error" class="error-details">
            <div class="error-title">错误详情：</div>
            <code>{{ backendStatus.components.database.error }}</code>
          </div>
        </div>
      </transition>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Warning, CircleClose, InfoFilled, Close } from '@element-plus/icons-vue';
import { useBackendStatus } from '../composables/useBackendStatus';

const {
  backendStatus,
  isChecking,
  isUnavailable,
  needsInstall,
  statusMessage,
  statusType,
  shouldShowBanner,
  checkBackendStatus
} = useBackendStatus();

const showBanner = ref(true);
const showDetails = ref(false);
let checkInterval: number | null = null;

const bannerClass = computed(() => {
  return `banner-${statusType.value}`;
});

const suggestions = computed(() => {
  return backendStatus.value?.suggestions || [];
});

const goToInstall = () => {
  window.location.href = '/install';
};

const retryCheck = async () => {
  await checkBackendStatus();
  if (backendStatus.value?.status === 'healthy') {
    ElMessage.success('后端服务已恢复正常');
  } else {
    ElMessage.warning('后端服务仍不可用');
  }
};

const dismissBanner = () => {
  showBanner.value = false;
  if (checkInterval) {
    clearInterval(checkInterval);
    checkInterval = null;
  }
  setTimeout(() => {
    startPeriodicCheck();
  }, 60000);
};

const startPeriodicCheck = () => {
  checkBackendStatus(true);
  checkInterval = window.setInterval(() => {
    checkBackendStatus(true);
    if (backendStatus.value?.status === 'healthy' && !showBanner.value) {
      showBanner.value = true;
    }
  }, 30000);
};

onMounted(() => {
  startPeriodicCheck();
});

onUnmounted(() => {
  if (checkInterval) {
    clearInterval(checkInterval);
  }
});
</script>

<style scoped>
.backend-status-banner {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
  padding: 12px 24px;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease-in-out;
}

.banner-content {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.banner-icon {
  flex-shrink: 0;
}

.banner-message {
  flex: 1;
  font-size: 14px;
  line-height: 1.5;
}

.close-btn {
  margin-left: auto;
}

.banner-details {
  max-width: 1400px;
  margin: 12px auto 0;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.details-title,
.error-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.details-list {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  line-height: 1.8;
}

.error-details {
  margin-top: 12px;
  padding: 12px;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 6px;
}

.error-details code {
  font-size: 12px;
  color: inherit;
  opacity: 0.9;
}

.banner-warning {
  background: rgba(245, 158, 11, 0.95);
  color: #fff;
  border-bottom: 1px solid rgba(245, 158, 11, 0.5);
}

.banner-error {
  background: rgba(239, 68, 68, 0.95);
  color: #fff;
  border-bottom: 1px solid rgba(239, 68, 68, 0.5);
}

.banner-info {
  background: rgba(59, 130, 246, 0.95);
  color: #fff;
  border-bottom: 1px solid rgba(59, 130, 246, 0.5);
}

.banner-success {
  background: rgba(34, 197, 94, 0.95);
  color: #fff;
  border-bottom: 1px solid rgba(34, 197, 94, 0.5);
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease-in-out;
}

.slide-down-enter-from,
.slide-down-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: all 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

:deep(.el-button--primary) {
  background: rgba(255, 255, 255, 0.95);
  border-color: rgba(255, 255, 255, 0.95);
  color: #1a1a2e;
  font-weight: 500;
}

:deep(.el-button--primary:hover) {
  background: #fff;
  border-color: #fff;
  color: #1a1a2e;
}

:deep(.el-button--info.is-text) {
  color: inherit;
  opacity: 0.8;
}

:deep(.el-button--info.is-text:hover) {
  opacity: 1;
  background: rgba(255, 255, 255, 0.1);
}
</style>
