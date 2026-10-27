import axios from 'axios';
import { ERROR_CODES, ERROR_DESCRIPTIONS } from './errorCodes';
import { errorBus } from './errorBus';

/**
 * Generate a standard UUID v4 for request correlation tracking (SAD Section 4.4 & 4.5)
 */
export function generateCorrelationId() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

/**
 * Shared Axios instance with base path /api/v1 per SAD Section 4.3
 */
const apiClient = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

let inMemoryToken = null;

export function setAuthToken(token) {
  inMemoryToken = token || null;
}

export function getAuthToken() {
  return inMemoryToken;
}

const unauthorizedListeners = new Set();

export function onUnauthorized(callback) {
  unauthorizedListeners.add(callback);
  return () => {
    unauthorizedListeners.delete(callback);
  };
}

/**
 * Request Interceptor:
 * 1. Attaches X-Correlation-Id header for distributed tracing.
 * 2. Attaches Bearer JWT token from in-memory token store if user session exists.
 */
apiClient.interceptors.request.use(
  (config) => {
    if (!config.headers) {
      config.headers = {};
    }
    // Inject Correlation-ID if not already supplied
    if (!config.headers['X-Correlation-Id']) {
      config.headers['X-Correlation-Id'] = generateCorrelationId();
    }

    // Inject Bearer token strictly from in-memory storage (never localStorage/sessionStorage)
    if (inMemoryToken && !config.headers.Authorization && !config.headers['Authorization']) {
      config.headers['Authorization'] = `Bearer ${inMemoryToken}`;
    }

    // Let the browser/axios set the multipart boundary automatically for FormData
    if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
      delete config.headers['Content-Type'];
    }

    return config;
  },
  (error) => Promise.reject(error)
);

/**
 * Response Interceptor:
 * Normalizes backend error responses to the SAD Section 4.4 standard schema:
 * { timestamp, status, errorCode, message, details[], correlationId }
 */
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const correlationId =
      error.config?.headers?.['X-Correlation-Id'] ||
      error.response?.headers?.['x-correlation-id'] ||
      'unknown';

    let normalizedError = {
      timestamp: new Date().toISOString(),
      status: error.response?.status || 0,
      errorCode: ERROR_CODES.SYSTEM_ERROR,
      message: 'Network error or service unavailable.',
      details: [],
      correlationId,
      rawError: error,
    };

    if (error.response?.data && typeof error.response.data === 'object') {
      const data = error.response.data;
      normalizedError = {
        timestamp: data.timestamp || new Date().toISOString(),
        status: error.response.status,
        errorCode: data.errorCode || mapStatusToErrorCode(error.response.status),
        message: data.message || ERROR_DESCRIPTIONS[data.errorCode] || error.message,
        details: Array.isArray(data.details) ? data.details : [],
        correlationId: data.correlationId || correlationId,
        rawError: error,
      };
    } else if (error.response) {
      normalizedError.status = error.response.status;
      normalizedError.errorCode = mapStatusToErrorCode(error.response.status);
      normalizedError.message =
        ERROR_DESCRIPTIONS[normalizedError.errorCode] || `HTTP ${error.response.status} error`;
    }

    // Special handling for 401 Session Expiry (SAD INS-SR-005: 15 min idle timeout)
    if (normalizedError.status === 401) {
      // Clear in-memory token on confirmed 401 to prevent stale state
      setAuthToken(null);
      unauthorizedListeners.forEach((fn) => {
        try {
          fn(normalizedError);
        } catch (e) {
          console.error('Error in onUnauthorized listener', e);
        }
      });
    }

    // Broadcast through ErrorBus for centralized UI feedback
    errorBus.emit(normalizedError);

    return Promise.reject(normalizedError);
  }
);

/**
 * Fallback mapping from HTTP status code to SAD error codes
 */
function mapStatusToErrorCode(status) {
  switch (status) {
    case 400:
    case 422:
      return ERROR_CODES.VALIDATION_ERROR;
    case 401:
      return ERROR_CODES.AUTH_UNAUTHORIZED;
    case 423:
      return ERROR_CODES.AUTH_LOCKED;
    case 403:
      return ERROR_CODES.ACCESS_FORBIDDEN;
    case 404:
      return ERROR_CODES.NOT_FOUND;
    case 409:
      return ERROR_CODES.STATE_CONFLICT;
    case 402:
      return ERROR_CODES.PAYMENT_FAILED;
    case 429:
      return ERROR_CODES.RATE_LIMITED;
    case 500:
    default:
      return ERROR_CODES.SYSTEM_ERROR;
  }
}

export default apiClient;
