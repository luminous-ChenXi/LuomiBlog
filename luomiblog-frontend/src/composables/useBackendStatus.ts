import { ref, computed } from 'vue';
import { API_BASE_URL } from '../config/api';

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

// 全局状态
const backendStatus = ref<HealthCheckResponse | null>(null);
const isChecking = ref(false);
const lastCheckTime = ref<Date | null>(null);
const checkError = ref<string | null>(null);

// 计算属性
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
    // 只在非健康状态下显示横幅
    return backendStatus.value?.status !== 'healthy';
  });

  const canUseFeatures = computed(() => {
    // 判断哪些功能可用
    if (!backendStatus.value) return {
      login: false,
      register: false,
      like: false,
      comment: false,
      admin: false,
      readOnly: true
    };

    const status = backendStatus.value.status;
    const dbConnected = backendStatus.value.database === 'connected';

    return {
      login: status === 'healthy' && dbConnected,
      register: status === 'healthy' && dbConnected,
      like: status === 'healthy' && dbConnected,
      comment: status === 'healthy' && dbConnected,
      admin: status === 'healthy' && dbConnected,
      readOnly: true // 总是可以阅读
    };
  });

  // 检查后端状态
  const checkBackendStatus = async (silent = false) => {
    if (isChecking.value) return;
    
    isChecking.value = true;
    checkError.value = null;

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000); // 5秒超时

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
        backendStatus.value = result.data;
        lastCheckTime.value = new Date();
      } else {
        throw new Error(result.message || 'Invalid response');
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      checkError.value = errorMessage;
      
      // 设置一个默认的不可用状态
      backendStatus.value = {
        status: 'unhealthy',
        database: 'disconnected',
        installLock: false,
        hasData: false,
        version: 'unknown',
        timestamp: new Date().toISOString(),
        message: '无法连接到后端服务，仅提供基础浏览功能',
        suggestions: [
          '检查后端服务是否已启动',
          '检查网络连接',
          '刷新页面重试'
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
        console.warn('后端健康检查失败:', errorMessage);
      }
    } finally {
      isChecking.value = false;
    }
  };

  // 简单的 ping 检查（用于快速检测）
  const pingBackend = async (): Promise<boolean> => {
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 3000);

      const response = await fetch(`${API_BASE_URL}/api/health/ping`, {
        signal: controller.signal
      });

      clearTimeout(timeoutId);
      return response.ok;
    } catch {
      return false;
    }
  };

  return {
    // 状态
    backendStatus,
    isChecking,
    lastCheckTime,
    checkError,
    
    // 计算属性
    isHealthy,
    isDegraded,
    isUnavailable,
    needsInstall,
    statusMessage,
    statusType,
    shouldShowBanner,
    canUseFeatures,
    
    // 方法
    checkBackendStatus,
    pingBackend
  };
};

// 导出全局状态引用，方便在组件外使用
export { backendStatus, isChecking, lastCheckTime, checkError };
