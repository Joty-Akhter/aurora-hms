import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';

/**
 * Raw gateway URL from env (may point at localhost while the SPA is served from another host).
 */
function envGatewayBase(): string {
  return (
    import.meta.env.VITE_API_BASE_URL?.trim() ||
    import.meta.env.VITE_AUTH_SERVICE_URL?.trim() ||
    ''
  );
}

/**
 * Resolve the axios `baseURL` for all `/api/**` traffic (data + auth).
 *
 * - Prefer `VITE_API_BASE_URL`, then `VITE_AUTH_SERVICE_URL`.
 * - Empty string = same origin (Vite dev proxies `/api` → `VITE_DEV_PROXY_TARGET`).
 * - If the page is **not** on localhost but env still points at **localhost/127.0.0.1**, ignore that
 *   and use same-origin so `/api/config/modules` and the rest hit the SPA host (e.g. nginx → gateway),
 *   instead of the browser calling `http://localhost:8081` from a public site.
 */
function resolveGatewayBaseUrl(): string {
  const raw = envGatewayBase();
  if (!raw || typeof window === 'undefined') {
    return raw;
  }
  try {
    const u = new URL(raw, window.location.origin);
    const baseIsLoopback = u.hostname === 'localhost' || u.hostname === '127.0.0.1';
    const pageIsLoopback =
      window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
    if (baseIsLoopback && !pageIsLoopback) {
      return '';
    }
  } catch {
    return raw;
  }
  return raw;
}

export const API_GATEWAY_BASE_URL = resolveGatewayBaseUrl();

const API_BASE_URL = API_GATEWAY_BASE_URL;

class ApiService {
  private api: AxiosInstance;

  constructor() {
    this.api = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor for adding auth token
    this.api.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('accessToken');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        const userStr = localStorage.getItem('user');
        if (userStr) {
          try {
            const user = JSON.parse(userStr) as { id?: string };
            if (user?.id) {
              config.headers['X-User-Id'] = user.id;
            }
          } catch {
            /* ignore malformed user cache */
          }
        }
        const orgId = localStorage.getItem('currentOrganizationId');
        if (orgId) {
          config.headers['X-Organization-Id'] = orgId;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor for handling errors
    // 401: unauthenticated — refresh token flow below.
    // 403: forbidden (authenticated but lacking permission) — handled per screen (e.g. prescription alerts)
    // or route to /forbidden?reason=api from callers that need it; no global 403 redirect (avoids breaking
    // components that parse 403 inline).
    this.api.interceptors.response.use(
      (response) => response,
      async (error) => {
        const originalRequest = error.config;

        // If 401 and not already retried, try to refresh token
        if (error.response?.status === 401 && !originalRequest._retry) {
          const requestUrl = String(originalRequest.url || '');
          if (requestUrl.includes('/api/public/')) {
            return Promise.reject(error);
          }
          originalRequest._retry = true;

          try {
            const refreshToken = localStorage.getItem('refreshToken');
            if (refreshToken) {
              // Use API Gateway for refresh token
              const authApi = axios.create({
                baseURL: API_BASE_URL,
                headers: { 'Content-Type': 'application/json' },
              });
              
              const response = await authApi.post('/api/auth/refresh', {
                refreshToken,
              });

              const { accessToken, refreshToken: newRefreshToken } = response.data;
              localStorage.setItem('accessToken', accessToken);
              localStorage.setItem('refreshToken', newRefreshToken);

              originalRequest.headers.Authorization = `Bearer ${accessToken}`;
              return this.api(originalRequest);
            }
          } catch (refreshError) {
            // Refresh failed, logout user
            console.error('Token refresh failed:', refreshError);
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('user');
            const path = window.location.pathname;
            if (!path.startsWith('/book-appointment') && !path.startsWith('/login')) {
              window.location.href = '/login';
            }
            return Promise.reject(refreshError);
          }
        }

        return Promise.reject(error);
      }
    );
  }

  async get<T>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.api.get<T>(url, config);
  }

  async post<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.api.post<T>(url, data, config);
  }

  async put<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.api.put<T>(url, data, config);
  }

  async patch<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.api.patch<T>(url, data, config);
  }

  async delete<T>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.api.delete<T>(url, config);
  }
}

export default new ApiService();

