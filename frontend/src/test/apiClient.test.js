import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient, { generateCorrelationId } from '../api/apiClient';
import { ERROR_CODES } from '../api/errorCodes';
import { errorBus } from '../api/errorBus';

describe('Shared API Client (SAD Section 4.3, 4.4, 4.5)', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('configures the base URL as /api/v1', () => {
    expect(apiClient.defaults.baseURL).toBe('/api/v1');
  });

  it('generates a valid UUID string for correlation ID', () => {
    const cid = generateCorrelationId();
    expect(cid).toBeDefined();
    expect(typeof cid).toBe('string');
    expect(cid.length).toBeGreaterThan(10);
  });

  it('injects Bearer token in request headers if present in localStorage', async () => {
    localStorage.setItem('auth_token', 'mock-jwt-token-12345');

    // Simulate request interceptor execution
    const config = {
      headers: {},
    };

    const handler = apiClient.interceptors.request.handlers[0].fulfilled;
    const modifiedConfig = handler(config);

    expect(modifiedConfig.headers['Authorization']).toBe('Bearer mock-jwt-token-12345');
    expect(modifiedConfig.headers['X-Correlation-Id']).toBeDefined();
  });

  it('normalizes error responses to the SAD 4.4 standard schema', async () => {
    const errorListener = vi.fn();
    const unsubscribe = errorBus.subscribe(errorListener);

    const mockAxiosError = {
      config: {
        headers: { 'X-Correlation-Id': 'test-corr-id-999' },
      },
      response: {
        status: 422,
        data: {
          timestamp: '2026-10-14T10:00:00Z',
          status: 422,
          errorCode: ERROR_CODES.VALIDATION_ERROR,
          message: 'Claim date exceeds 30-day limit',
          details: ['incidentDate cannot be older than 30 days'],
          correlationId: 'test-corr-id-999',
        },
      },
    };

    const errorHandler = apiClient.interceptors.response.handlers[0].rejected;

    await expect(errorHandler(mockAxiosError)).rejects.toMatchObject({
      status: 422,
      errorCode: ERROR_CODES.VALIDATION_ERROR,
      message: 'Claim date exceeds 30-day limit',
      details: ['incidentDate cannot be older than 30 days'],
      correlationId: 'test-corr-id-999',
    });

    // Check that error was broadcast to ErrorBus
    expect(errorListener).toHaveBeenCalledWith(
      expect.objectContaining({
        errorCode: ERROR_CODES.VALIDATION_ERROR,
        correlationId: 'test-corr-id-999',
      })
    );

    unsubscribe();
  });

  it('clears token on 401 unauthorized session expiry', async () => {
    localStorage.setItem('auth_token', 'expired-token');

    const mock401Error = {
      config: { headers: {} },
      response: {
        status: 401,
        data: {
          errorCode: ERROR_CODES.AUTH_UNAUTHORIZED,
          message: 'Session expired after 15 minutes of inactivity',
        },
      },
    };

    const errorHandler = apiClient.interceptors.response.handlers[0].rejected;

    try {
      await errorHandler(mock401Error);
    } catch (err) {
      // expected rejection
    }

    expect(localStorage.getItem('auth_token')).toBeNull();
  });
});
