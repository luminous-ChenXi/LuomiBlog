import { ref, computed } from 'vue';
import { ApiError, API_ERROR_CODES } from '../config/api';
import { isBackendAvailable, setBackendAvailable } from '../utils/api';
import type { ApiErrorCode } from '../config/api';

interface UseApiOptions<T> {
  defaultValue?: T;
  requireBackend?: boolean;
  silent?: boolean;
  onSuccess?: (data: T) => void;
  onError?: (error: ApiError) => void;
}

interface UseApiReturn<T> {
  data: ReturnType<typeof ref<T | null>>;
  error: ReturnType<typeof ref<ApiError | null>>;
  isLoading: ReturnType<typeof ref<boolean>>;
  isBackendDown: ReturnType<typeof computed<boolean>>;
  errorMessage: ReturnType<typeof computed<string>>;
  execute: (apiCall: () => Promise<T>) => Promise<T | null>;
  reset: () => void;
}

export const useApi = <T>(options: UseApiOptions<T> = {}): UseApiReturn<T> => {
  const {
    defaultValue,
    requireBackend = true,
    silent = false,
    onSuccess,
    onError
  } = options;

  const data = ref<T | null>(defaultValue ?? null) as ReturnType<typeof ref<T | null>>;
  const error = ref<ApiError | null>(null);
  const isLoading = ref(false);

  const isBackendDown = computed(() => {
    return error.value?.code === API_ERROR_CODES.NETWORK_ERROR ||
           error.value?.code === API_ERROR_CODES.TIMEOUT ||
           error.value?.code === API_ERROR_CODES.SERVER_ERROR;
  });

  const errorMessage = computed(() => {
    if (!error.value) return '';

    switch (error.value.code) {
      case API_ERROR_CODES.NETWORK_ERROR:
        return '无法连接到服务器，请检查网络连接';
      case API_ERROR_CODES.TIMEOUT:
        return '请求超时，请稍后重试';
      case API_ERROR_CODES.AUTH_ERROR:
        return '登录已过期，请重新登录';
      case API_ERROR_CODES.RATE_LIMIT:
        return '操作过于频繁，请稍后再试';
      case API_ERROR_CODES.NOT_FOUND:
        return '请求的资源不存在';
      case API_ERROR_CODES.SERVER_ERROR:
        return '服务器暂时不可用，请稍后重试';
      default:
        return error.value.message || '操作失败，请重试';
    }
  });

  const execute = async (apiCall: () => Promise<T>): Promise<T | null> => {
    if (requireBackend && !isBackendAvailable()) {
      const backendError = new ApiError(
        '后端服务不可用，部分功能暂时无法使用',
        API_ERROR_CODES.NETWORK_ERROR
      );
      error.value = backendError;
      onError?.(backendError);
      return null;
    }

    isLoading.value = true;
    error.value = null;

    try {
      const result = await apiCall();
      data.value = result;
      onSuccess?.(result);
      return result;
    } catch (err) {
      const apiError = err instanceof ApiError
        ? err
        : new ApiError(
            err instanceof Error ? err.message : '未知错误',
            API_ERROR_CODES.UNKNOWN
          );

      error.value = apiError;

      if (apiError.code === API_ERROR_CODES.NETWORK_ERROR ||
          apiError.code === API_ERROR_CODES.TIMEOUT) {
        setBackendAvailable(false);
      }

      if (!silent) {
        onError?.(apiError);
      }

      return null;
    } finally {
      isLoading.value = false;
    }
  };

  const reset = () => {
    data.value = defaultValue ?? null;
    error.value = null;
    isLoading.value = false;
  };

  return {
    data,
    error,
    isLoading,
    isBackendDown,
    errorMessage,
    execute,
    reset
  };
};

export const useSafeApiCall = () => {
  const call = async <T>(
    apiCall: () => Promise<T>,
    fallback?: T
  ): Promise<T | null> => {
    try {
      return await apiCall();
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.code === API_ERROR_CODES.NETWORK_ERROR ||
            err.code === API_ERROR_CODES.TIMEOUT) {
          setBackendAvailable(false);
        }
      }
      return fallback ?? null;
    }
  };

  return { call };
};

export const getErrorMessage = (error: unknown): string => {
  if (error instanceof ApiError) {
    switch (error.code) {
      case API_ERROR_CODES.NETWORK_ERROR:
        return '无法连接到服务器';
      case API_ERROR_CODES.TIMEOUT:
        return '请求超时';
      case API_ERROR_CODES.AUTH_ERROR:
        return '请先登录';
      case API_ERROR_CODES.RATE_LIMIT:
        return '操作过于频繁';
      default:
        return error.message;
    }
  }
  if (error instanceof Error) {
    return error.message;
  }
  return '操作失败';
};
