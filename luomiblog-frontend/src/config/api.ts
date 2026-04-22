export const API_BASE_URL = import.meta.env.PUBLIC_API_URL || 'http://localhost:8080';

export const API_CONFIG = {
  timeout: 8000,
  retryCount: 2,
  retryDelay: 1000,
  healthCheckInterval: 30000,
  healthCheckTimeout: 5000,
  pingTimeout: 3000,
} as const;

export const API_ERROR_CODES = {
  NETWORK_ERROR: 'NETWORK_ERROR',
  TIMEOUT: 'TIMEOUT',
  SERVER_ERROR: 'SERVER_ERROR',
  AUTH_ERROR: 'AUTH_ERROR',
  NOT_FOUND: 'NOT_FOUND',
  RATE_LIMIT: 'RATE_LIMIT',
  UNKNOWN: 'UNKNOWN',
} as const;

export type ApiErrorCode = typeof API_ERROR_CODES[keyof typeof API_ERROR_CODES];

export class ApiError extends Error {
  code: ApiErrorCode;
  status?: number;
  isRetryable: boolean;

  constructor(message: string, code: ApiErrorCode, status?: number) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
    this.isRetryable = code === API_ERROR_CODES.NETWORK_ERROR ||
                       code === API_ERROR_CODES.TIMEOUT ||
                       (status !== undefined && status >= 500 && status < 600);
  }
}
