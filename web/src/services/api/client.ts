/**
 * API Client Service
 *
 * Provides HTTP client for communicating with backend API.
 * Handles authentication, error handling, and request/response transformation.
 */

import { ApiResponse, ApiError, AuthTokens } from '@/types';

// ============================================================================
// Configuration
// ============================================================================

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000/api/v1';
const API_TIMEOUT = 30000; // 30 seconds

// ============================================================================
// Token Management
// ============================================================================

class TokenManager {
  private static ACCESS_TOKEN_KEY = 'access_token';
  private static REFRESH_TOKEN_KEY = 'refresh_token';
  private static EXPIRY_KEY = 'token_expiry';

  static setTokens(tokens: AuthTokens): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, tokens.accessToken);
    localStorage.setItem(this.REFRESH_TOKEN_KEY, tokens.refreshToken);

    const expiryTime = Date.now() + tokens.expiresIn * 1000;
    localStorage.setItem(this.EXPIRY_KEY, expiryTime.toString());
  }

  static getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  static getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  static isTokenExpired(): boolean {
    const expiry = localStorage.getItem(this.EXPIRY_KEY);
    if (!expiry) return true;

    return Date.now() > parseInt(expiry);
  }

  static clearTokens(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.EXPIRY_KEY);
  }
}

// ============================================================================
// HTTP Client
// ============================================================================

export class ApiClient {
  private static instance: ApiClient;
  private isRefreshing = false;
  private refreshSubscribers: ((token: string) => void)[] = [];

  private constructor() {}

  static getInstance(): ApiClient {
    if (!ApiClient.instance) {
      ApiClient.instance = new ApiClient();
    }
    return ApiClient.instance;
  }

  /**
   * Make HTTP request with automatic token refresh
   */
  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<ApiResponse<T>> {
    const url = `${API_BASE_URL}${endpoint}`;

    // Add authentication header
    const token = TokenManager.getAccessToken();
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      ...options.headers
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    // Add timeout
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), API_TIMEOUT);

    try {
      const response = await fetch(url, {
        ...options,
        headers,
        signal: controller.signal
      });

      clearTimeout(timeoutId);

      // Handle 401 - Token expired
      if (response.status === 401 && token) {
        return this.handleTokenExpiration<T>(endpoint, options);
      }

      // Parse response
      const data = await response.json();

      if (!response.ok) {
        return {
          success: false,
          error: {
            code: data.code || 'API_ERROR',
            message: data.message || 'An error occurred',
            details: data.details
          }
        };
      }

      return {
        success: true,
        data: data as T
      };
    } catch (error) {
      clearTimeout(timeoutId);

      if (error instanceof Error) {
        if (error.name === 'AbortError') {
          return {
            success: false,
            error: {
              code: 'TIMEOUT',
              message: 'Request timed out'
            }
          };
        }

        return {
          success: false,
          error: {
            code: 'NETWORK_ERROR',
            message: error.message
          }
        };
      }

      return {
        success: false,
        error: {
          code: 'UNKNOWN_ERROR',
          message: 'An unknown error occurred'
        }
      };
    }
  }

  /**
   * Handle token expiration with refresh
   */
  private async handleTokenExpiration<T>(
    endpoint: string,
    options: RequestInit
  ): Promise<ApiResponse<T>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;

      try {
        const refreshToken = TokenManager.getRefreshToken();
        if (!refreshToken) {
          throw new Error('No refresh token');
        }

        const response = await this.post<AuthTokens>('/auth/refresh', {
          refreshToken
        });

        if (!response.success || !response.data) {
          throw new Error('Failed to refresh token');
        }

        TokenManager.setTokens(response.data);
        this.isRefreshing = false;

        // Notify all subscribers
        this.refreshSubscribers.forEach((callback) =>
          callback(response.data!.accessToken)
        );
        this.refreshSubscribers = [];

        // Retry original request
        return this.request<T>(endpoint, options);
      } catch (error) {
        this.isRefreshing = false;
        this.refreshSubscribers = [];
        TokenManager.clearTokens();

        // Redirect to login (will be handled by router)
        window.dispatchEvent(new Event('auth:logout'));

        return {
          success: false,
          error: {
            code: 'AUTH_EXPIRED',
            message: 'Authentication expired. Please log in again.'
          }
        };
      }
    } else {
      // Wait for token refresh
      return new Promise((resolve) => {
        this.refreshSubscribers.push((token) => {
          // Retry with new token
          this.request<T>(endpoint, {
            ...options,
            headers: {
              ...options.headers,
              Authorization: `Bearer ${token}`
            }
          }).then(resolve);
        });
      });
    }
  }

  /**
   * GET request
   */
  async get<T>(endpoint: string): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, { method: 'GET' });
  }

  /**
   * POST request
   */
  async post<T>(endpoint: string, data?: any): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined
    });
  }

  /**
   * PUT request
   */
  async put<T>(endpoint: string, data?: any): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined
    });
  }

  /**
   * PATCH request
   */
  async patch<T>(endpoint: string, data?: any): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: 'PATCH',
      body: data ? JSON.stringify(data) : undefined
    });
  }

  /**
   * DELETE request
   */
  async delete<T>(endpoint: string): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, { method: 'DELETE' });
  }

  /**
   * Upload file (multipart/form-data)
   */
  async upload<T>(endpoint: string, formData: FormData): Promise<ApiResponse<T>> {
    const token = TokenManager.getAccessToken();
    const headers: HeadersInit = {};

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    // Don't set Content-Type - browser will set it with boundary

    try {
      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: 'POST',
        headers,
        body: formData
      });

      const data = await response.json();

      if (!response.ok) {
        return {
          success: false,
          error: {
            code: data.code || 'UPLOAD_ERROR',
            message: data.message || 'Upload failed',
            details: data.details
          }
        };
      }

      return {
        success: true,
        data: data as T
      };
    } catch (error) {
      return {
        success: false,
        error: {
          code: 'NETWORK_ERROR',
          message: error instanceof Error ? error.message : 'Upload failed'
        }
      };
    }
  }

  /**
   * Download file
   */
  async download(endpoint: string): Promise<Blob | null> {
    const token = TokenManager.getAccessToken();
    const headers: HeadersInit = {};

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    try {
      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: 'GET',
        headers
      });

      if (!response.ok) {
        throw new Error('Download failed');
      }

      return response.blob();
    } catch (error) {
      console.error('Download error:', error);
      return null;
    }
  }

  /**
   * Set authentication tokens
   */
  setTokens(tokens: AuthTokens): void {
    TokenManager.setTokens(tokens);
  }

  /**
   * Clear authentication tokens
   */
  clearTokens(): void {
    TokenManager.clearTokens();
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    const token = TokenManager.getAccessToken();
    return !!token && !TokenManager.isTokenExpired();
  }
}

// Export singleton instance
export const apiClient = ApiClient.getInstance();
