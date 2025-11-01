/**
 * Authentication Context
 *
 * Manages user authentication state, login, signup, and logout.
 * Integrates with encryption service to initialize keys on login.
 */

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { User, AuthTokens, LoginCredentials, SignupData, EncryptionTier } from '@/types';
import { apiClient } from '@/services/api/client';
import { EncryptionServiceFactory } from '@/services/encryption';
import { db } from '@/services/storage';

// ============================================================================
// Types
// ============================================================================

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;

  login: (credentials: LoginCredentials) => Promise<boolean>;
  signup: (data: SignupData) => Promise<{ success: boolean; recoveryCodes?: string[] }>;
  logout: () => Promise<void>;
  clearError: () => void;
}

// ============================================================================
// Context
// ============================================================================

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// ============================================================================
// Provider Component
// ============================================================================

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Initialize - check for existing session
  useEffect(() => {
    const initAuth = async () => {
      try {
        if (apiClient.isAuthenticated()) {
          // Fetch current user
          const response = await apiClient.get<User>('/auth/me');
          if (response.success && response.data) {
            setUser(response.data);
          } else {
            apiClient.clearTokens();
          }
        }
      } catch (err) {
        console.error('Auth initialization error:', err);
        apiClient.clearTokens();
      } finally {
        setIsLoading(false);
      }
    };

    initAuth();
  }, []);

  // Listen for logout events
  useEffect(() => {
    const handleLogout = () => {
      setUser(null);
    };

    window.addEventListener('auth:logout', handleLogout);
    return () => window.removeEventListener('auth:logout', handleLogout);
  }, []);

  /**
   * Login user
   */
  const login = async (credentials: LoginCredentials): Promise<boolean> => {
    setIsLoading(true);
    setError(null);

    try {
      // Call login endpoint
      const response = await apiClient.post<{
        user: User;
        tokens: AuthTokens;
      }>('/auth/login', {
        email: credentials.email,
        password: credentials.password
      });

      if (!response.success || !response.data) {
        setError(response.error?.message || 'Login failed');
        return false;
      }

      const { user: userData, tokens } = response.data;

      // Store tokens
      apiClient.setTokens(tokens);

      // Initialize encryption service
      await EncryptionServiceFactory.createService(
        userData.encryptionTier,
        credentials.password
      );

      // Initialize database
      await db.init();

      setUser(userData);
      return true;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
      return false;
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * Signup new user
   */
  const signup = async (
    data: SignupData
  ): Promise<{ success: boolean; recoveryCodes?: string[] }> => {
    setIsLoading(true);
    setError(null);

    try {
      // Initialize encryption service to generate keys
      const encryptionService = await EncryptionServiceFactory.createService(
        data.encryptionTier,
        data.password
      );

      let signupPayload: any = {
        email: data.email,
        password: data.password,
        encryptionTier: data.encryptionTier
      };

      let recoveryCodes: string[] | undefined;

      // For E2E tier, generate recovery codes and include public key
      if (data.encryptionTier === EncryptionTier.E2E) {
        const e2eService = encryptionService as any;
        const publicKey = await e2eService.getPublicKey();
        signupPayload.publicKey = publicKey;

        // Generate recovery codes
        const { RecoveryCodeManager } = await import('@/services/encryption');
        recoveryCodes = RecoveryCodeManager.generateCodes(10);

        // Hash codes for server storage
        const hashedCodes = await RecoveryCodeManager.hashCodes(recoveryCodes);
        signupPayload.recoveryCodes = hashedCodes;
      }

      // For UCE tier, include encrypted master key
      if (data.encryptionTier === EncryptionTier.UCE) {
        const uceService = encryptionService as any;
        const encryptedMasterKey = await uceService.getEncryptedMasterKey();
        signupPayload.encryptedMasterKey = encryptedMasterKey;
      }

      // Call signup endpoint
      const response = await apiClient.post<{
        user: User;
        tokens: AuthTokens;
      }>('/auth/signup', signupPayload);

      if (!response.success || !response.data) {
        setError(response.error?.message || 'Signup failed');
        return { success: false };
      }

      const { user: userData, tokens } = response.data;

      // Store tokens
      apiClient.setTokens(tokens);

      // Initialize database
      await db.init();

      setUser(userData);
      return { success: true, recoveryCodes };
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Signup failed');
      return { success: false };
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * Logout user
   */
  const logout = async (): Promise<void> => {
    setIsLoading(true);

    try {
      // Call logout endpoint
      await apiClient.post('/auth/logout');
    } catch (err) {
      console.error('Logout error:', err);
    }

    // Clear tokens
    apiClient.clearTokens();

    // Clear encryption keys
    EncryptionServiceFactory.clearInstance();

    // Clear database (optional - based on settings)
    // await db.clearAll();

    setUser(null);
    setIsLoading(false);
  };

  /**
   * Clear error message
   */
  const clearError = () => {
    setError(null);
  };

  const value: AuthContextType = {
    user,
    isAuthenticated: !!user,
    isLoading,
    error,
    login,
    signup,
    logout,
    clearError
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// ============================================================================
// Hook
// ============================================================================

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
