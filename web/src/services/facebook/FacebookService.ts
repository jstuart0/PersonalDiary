/**
 * Facebook Integration Service
 *
 * Handles Facebook OAuth and posting entries to Facebook.
 */

import { apiClient } from '@/services/api/client';
import { IntegrationAccount } from '@/types';

// ============================================================================
// Types
// ============================================================================

export interface FacebookAuthConfig {
  clientId: string;
  redirectUri: string;
  scope: string[];
}

export interface FacebookPost {
  message: string;
  privacy?: {
    value: 'SELF' | 'ALL_FRIENDS' | 'FRIENDS_OF_FRIENDS' | 'EVERYONE';
  };
}

// ============================================================================
// Facebook Service Class
// ============================================================================

export class FacebookService {
  private static instance: FacebookService;
  private readonly CLIENT_ID = import.meta.env.VITE_FACEBOOK_APP_ID || '';
  private readonly REDIRECT_URI = `${window.location.origin}/auth/facebook/callback`;
  private readonly SCOPES = ['public_profile', 'email', 'publish_to_groups'];

  private constructor() {}

  static getInstance(): FacebookService {
    if (!FacebookService.instance) {
      FacebookService.instance = new FacebookService();
    }
    return FacebookService.instance;
  }

  /**
   * Initialize Facebook SDK
   */
  async initializeFacebookSDK(): Promise<void> {
    return new Promise((resolve) => {
      // Check if SDK is already loaded
      if ((window as any).FB) {
        resolve();
        return;
      }

      // Load SDK
      (window as any).fbAsyncInit = () => {
        (window as any).FB.init({
          appId: this.CLIENT_ID,
          cookie: true,
          xfbml: true,
          version: 'v18.0'
        });

        resolve();
      };

      // Load SDK script
      const script = document.createElement('script');
      script.src = 'https://connect.facebook.net/en_US/sdk.js';
      script.async = true;
      script.defer = true;
      script.crossOrigin = 'anonymous';
      document.body.appendChild(script);
    });
  }

  /**
   * Open Facebook OAuth popup
   */
  async authenticateWithPopup(): Promise<{ accessToken: string; userId: string }> {
    await this.initializeFacebookSDK();

    return new Promise((resolve, reject) => {
      (window as any).FB.login(
        (response: any) => {
          if (response.authResponse) {
            resolve({
              accessToken: response.authResponse.accessToken,
              userId: response.authResponse.userID
            });
          } else {
            reject(new Error('User cancelled login or did not fully authorize.'));
          }
        },
        { scope: this.SCOPES.join(',') }
      );
    });
  }

  /**
   * Post entry to Facebook
   */
  async postToFacebook(entryContent: string, accessToken: string): Promise<string> {
    await this.initializeFacebookSDK();

    return new Promise((resolve, reject) => {
      (window as any).FB.api(
        '/me/feed',
        'POST',
        {
          message: entryContent,
          access_token: accessToken,
          privacy: { value: 'SELF' } // Post to "Only Me" by default
        },
        (response: any) => {
          if (response && !response.error) {
            resolve(response.id); // Returns post ID
          } else {
            reject(new Error(response.error?.message || 'Failed to post to Facebook'));
          }
        }
      );
    });
  }

  /**
   * Save Facebook integration
   */
  async saveIntegration(userId: string, accessToken: string): Promise<IntegrationAccount> {
    const response = await apiClient.post<IntegrationAccount>('/integrations/facebook', {
      userId,
      accessToken
    });

    if (!response.success || !response.data) {
      throw new Error(response.error?.message || 'Failed to save integration');
    }

    return response.data;
  }

  /**
   * Get user's Facebook integrations
   */
  async getIntegrations(userId: string): Promise<IntegrationAccount[]> {
    const response = await apiClient.get<IntegrationAccount[]>(
      `/integrations?userId=${userId}&platform=facebook`
    );

    if (!response.success || !response.data) {
      throw new Error(response.error?.message || 'Failed to get integrations');
    }

    return response.data;
  }

  /**
   * Revoke Facebook integration
   */
  async revokeIntegration(integrationId: string): Promise<void> {
    const response = await apiClient.delete(`/integrations/${integrationId}`);

    if (!response.success) {
      throw new Error(response.error?.message || 'Failed to revoke integration');
    }
  }

  /**
   * Check if Facebook integration is available
   */
  isAvailable(): boolean {
    return !!this.CLIENT_ID;
  }

  /**
   * Get Facebook user info
   */
  async getUserInfo(accessToken: string): Promise<any> {
    await this.initializeFacebookSDK();

    return new Promise((resolve, reject) => {
      (window as any).FB.api(
        '/me',
        { fields: 'id,name,email,picture', access_token: accessToken },
        (response: any) => {
          if (response && !response.error) {
            resolve(response);
          } else {
            reject(new Error(response.error?.message || 'Failed to get user info'));
          }
        }
      );
    });
  }
}

// Export singleton instance
export const facebookService = FacebookService.getInstance();
