import { ref, computed } from 'vue';
import { API_BASE_URL, API_CONFIG } from '../config/api';
import { isBackendAvailable, setBackendAvailable } from '../utils/api';

export interface HealthCheckResponse {
  status: 'healthy' | 'degraded' | 'unhealthy' | 'needs_reinstall' | 'not_installed';
  database: 'connected' | 'disconnected' | 'not_configured' | 'connected_no_tables';
  installLock: boolean;
  hasData: boolean;
  version: string;
  timestamp: string;
  message: string;
  suggestions: string[];
  components: {
    database: {
      status: string;
      message: string;
      error?: string;
    };
    cache: {
      status: string;
      message: string;
    };
    fileSystem: {
      status: string;
      message: string;
    };
  };
}

const backendStatus = ref<HealthCheckResponse | null>(null);
const isChecking = ref(false);
const lastCheckTime = ref<Date | null>(null);
const checkError = ref<string | null>(null);

function clearLocalAuth() {
  if (typeof localStorage !== 'undefined') {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }
}

export const useBackendStatus = () => {
  const isHealthy = computed(() => backendStatus.value?.status === 'healthy');
  const isDegraded = computed(() => backendStatus.value?.status === 'degraded');
  const isUnavailable = computed(() =>
    !backendStatus.value ||
    backendStatus.value.status === 'unhealthy' ||
    backendStatus.value.database === 'disconnected'
  );
  const needsInstall = computed(() =>
    backendStatus.value?.status === 'not_installed' ||
    backendStatus.value?.status === 'needs_reinstall'
  );

  const statusMessage = computed(() => {
    if (!backendStatus.value) {
      return '正在检查后端服务状态...';
    }
    return backendStatus.value.message;
  });

  const statusType = computed(() => {
    if (!backendStatus.value) return 'info';

    switch (backendStatus.value.status) {
      case 'healthy':
        return 'success';
      case 'degraded':
        return 'warning';
      case 'needs_reinstall':
      case 'not_installed':
        return 'info';
      case 'unhealthy':
      default:
        return 'error';
    }
  });

  const shouldShowBanner = computed(() => {
    return backendStatus.value?.status !== 'healthy';
  });

  const canUseFeatures = computed(() => {
    if (!backendStatus.value) return {
      login: false,
      register: false,
      like: false,
      comment: false,
      admin: false,
      aiAssistant: false,
      search: false,
      readOnly: true
    };

    const status = backendStatus.value.status;
    const dbConnected = backendStatus.value.database === 'connected';
    const healthy = status === 'healthy' && dbConnected;
    const degraded = status === 'degraded';

    return {
      login: healthy || degraded,
      register: healthy,
      like: healthy,
      comment: healthy,
      admin: healthy,
      aiAssistant: healthy,
      search: healthy || degraded,
      readOnly: true
    };
  });

  const checkBackendStatus = async (silent = false) => {
    if (isChecking.value) return;

    isChecking.value = true;
    checkError.value = null;

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(
        () => controller.abort(),
        API_CONFIG.healthCheckTimeout
      );

      const response = await fetch(`${API_BASE_URL}/api/health`, {
        signal: controller.signal,
        headers: {
          'Accept': 'application/json'
        }
      });

      clearTimeout(timeoutId);

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const result = await response.json();

      if (result.code === 200 && result.data) {
        const wasUnavailable = !isBackendAvailable();
        backendStatus.value = result.data;
        lastCheckTime.value = new Date();
        setBackendAvailable(true);
        if (wasUnavailable && !silent) {
          window.dispatchEvent(new CustomEvent('backend-status-changed', {
            detail: { available: true }
          }));
        }
      } else {
        throw new Error(result.message || 'Invalid response');
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      checkError.value = errorMessage;

      const wasAvailable = isBackendAvailable();
      setBackendAvailable(false);

      if (wasAvailable) {
        clearLocalAuth();
        window.dispatchEvent(new CustomEvent('auth-state-changed', {
          detail: { authenticated: false }
        }));
        window.dispatchEvent(new CustomEvent('backend-status-changed', {
          detail: { available: false }
        }));
      }

      backendStatus.value = {
        status: 'unhealthy',
        database: 'disconnected',
        installLock: false,
        hasData: false,
        version: 'unknown',
        timestamp: new Date().toISOString(),
        message: '无法连接到后端服务，博客内容仍可正常浏览',
        suggestions: [
          '博客文章来自静态文件，仍可正常阅读',
          '需要后端的功能（登录、评论、点赞等）暂不可用',
          '请稍后重试或联系管理员'
        ],
        components: {
          database: {
            status: 'down',
            message: '数据库连接失败',
            error: errorMessage
          },
          cache: {
            status: 'unknown',
            message: '无法检查'
          },
          fileSystem: {
            status: 'unknown',
            message: '无法检查'
          }
        }
      };

      if (!silent) {
        console.warn('[BackendStatus] 后端健康检查失败:', errorMessage);
      }
    } finally {
      isChecking.value = false;
    }
  };

  const pingBackend = async (): Promise<boolean> => {
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(
        () => controller.abort(),
        API_CONFIG.pingTimeout
      );

      const response = await fetch(`${API_BASE_URL}/api/health/ping`, {
        signal: controller.signal
      });

      clearTimeout(timeoutId);
      const available = response.ok;

      if (available !== isBackendAvailable()) {
        setBackendAvailable(available);
        if (!available) {
          clearLocalAuth();
          window.dispatchEvent(new CustomEvent('auth-state-changed', {
            detail: { authenticated: false }
          }));
        }
        window.dispatchEvent(new CustomEvent('backend-status-changed', {
          detail: { available }
        }));
      }

      return available;
    } catch {
      if (isBackendAvailable()) {
        setBackendAvailable(false);
        clearLocalAuth();
        window.dispatchEvent(new CustomEvent('auth-state-changed', {
          detail: { authenticated: false }
        }));
        window.dispatchEvent(new CustomEvent('backend-status-changed', {
          detail: { available: false }
        }));
      }
      return false;
    }
  };

  return {
    backendStatus,
    isChecking,
    lastCheckTime,
    checkError,

    isHealthy,
    isDegraded,
    isUnavailable,
    needsInstall,
    statusMessage,
    statusType,
    shouldShowBanner,
    canUseFeatures,

    checkBackendStatus,
    pingBackend
  };
};

export { backendStatus, isChecking, lastCheckTime, checkError };
