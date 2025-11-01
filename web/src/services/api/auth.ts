/**
 * Authentication API Endpoints
 */

import { apiClient } from './client';
import {
  LoginCredentials,
  SignupData,
  AuthTokens,
  User,
  ApiResponse,
  RecoveryCode
} from '@/types';

export const authApi = {
  /**
   * Sign up new user
   */
  async signup(data: SignupData): Promise<ApiResponse<{ user: User; tokens: AuthTokens; recoveryCodes?: string[] }>> {
    return apiClient.post('/auth/signup', data);
  },

  /**
   * Log in existing user
   */
  async login(credentials: LoginCredentials): Promise<ApiResponse<{ user: User; tokens: AuthTokens }>> {
    const response = await apiClient.post('/auth/login', credentials);

    if (response.success && response.data) {
      apiClient.setTokens(response.data.tokens);
    }

    return response;
  },

  /**
   * Log out user
   */
  async logout(): Promise<ApiResponse<void>> {
    const response = await apiClient.post('/auth/logout');
    apiClient.clearTokens();
    return response;
  },

  /**
   * Refresh access token
   */
  async refresh(refreshToken: string): Promise<ApiResponse<AuthTokens>> {
    return apiClient.post('/auth/refresh', { refreshToken });
  },

  /**
   * Get current user profile
   */
  async me(): Promise<ApiResponse<User>> {
    return apiClient.get('/auth/me');
  },

  /**
   * Request password reset
   */
  async requestPasswordReset(email: string): Promise<ApiResponse<void>> {
    return apiClient.post('/auth/password-reset/request', { email });
  },

  /**
   * Reset password with token
   */
  async resetPassword(token: string, newPassword: string): Promise<ApiResponse<void>> {
    return apiClient.post('/auth/password-reset/confirm', {
      token,
      newPassword
    });
  },

  /**
   * Verify recovery code (E2E tier)
   */
  async verifyRecoveryCode(code: string): Promise<ApiResponse<{ valid: boolean }>> {
    return apiClient.post('/auth/recovery-code/verify', { code });
  },

  /**
   * Get recovery codes status (E2E tier)
   */
  async getRecoveryCodes(): Promise<ApiResponse<RecoveryCode[]>> {
    return apiClient.get('/auth/recovery-codes');
  },

  /**
   * Regenerate recovery codes (E2E tier)
   */
  async regenerateRecoveryCodes(): Promise<ApiResponse<{ codes: string[] }>> {
    return apiClient.post('/auth/recovery-codes/regenerate');
  }
};
