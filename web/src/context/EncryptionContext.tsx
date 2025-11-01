/**
 * Encryption Context
 *
 * Provides access to encryption service for encrypting/decrypting content.
 * Automatically initializes when user logs in.
 */

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { EncryptionTier } from '@/types';
import { IEncryptionService, EncryptionServiceFactory } from '@/services/encryption';
import { useAuth } from './AuthContext';

// ============================================================================
// Types
// ============================================================================

interface EncryptionContextType {
  service: IEncryptionService | null;
  tier: EncryptionTier | null;
  isReady: boolean;

  encryptContent: (content: string) => Promise<string>;
  decryptContent: (encrypted: string) => Promise<string>;
  encryptMedia: (data: ArrayBuffer) => Promise<ArrayBuffer>;
  decryptMedia: (encrypted: ArrayBuffer) => Promise<ArrayBuffer>;
  generateContentHash: (content: string) => Promise<string>;
}

// ============================================================================
// Context
// ============================================================================

const EncryptionContext = createContext<EncryptionContextType | undefined>(undefined);

// ============================================================================
// Provider Component
// ============================================================================

interface EncryptionProviderProps {
  children: ReactNode;
}

export function EncryptionProvider({ children }: EncryptionProviderProps) {
  const { user, isAuthenticated } = useAuth();
  const [service, setService] = useState<IEncryptionService | null>(null);
  const [tier, setTier] = useState<EncryptionTier | null>(null);
  const [isReady, setIsReady] = useState(false);

  // Initialize encryption service when user is authenticated
  useEffect(() => {
    if (isAuthenticated && user) {
      const existingService = EncryptionServiceFactory.getInstance();
      if (existingService) {
        setService(existingService);
        setTier(existingService.getTier());
        setIsReady(true);
      }
    } else {
      setService(null);
      setTier(null);
      setIsReady(false);
    }
  }, [isAuthenticated, user]);

  /**
   * Encrypt content
   */
  const encryptContent = async (content: string): Promise<string> => {
    if (!service) {
      throw new Error('Encryption service not initialized');
    }
    return service.encryptContent(content);
  };

  /**
   * Decrypt content
   */
  const decryptContent = async (encrypted: string): Promise<string> => {
    if (!service) {
      throw new Error('Encryption service not initialized');
    }
    return service.decryptContent(encrypted);
  };

  /**
   * Encrypt media
   */
  const encryptMedia = async (data: ArrayBuffer): Promise<ArrayBuffer> => {
    if (!service) {
      throw new Error('Encryption service not initialized');
    }
    return service.encryptMedia(data);
  };

  /**
   * Decrypt media
   */
  const decryptMedia = async (encrypted: ArrayBuffer): Promise<ArrayBuffer> => {
    if (!service) {
      throw new Error('Encryption service not initialized');
    }
    return service.decryptMedia(encrypted);
  };

  /**
   * Generate content hash
   */
  const generateContentHash = async (content: string): Promise<string> => {
    if (!service) {
      throw new Error('Encryption service not initialized');
    }
    return service.generateContentHash(content);
  };

  const value: EncryptionContextType = {
    service,
    tier,
    isReady,
    encryptContent,
    decryptContent,
    encryptMedia,
    decryptMedia,
    generateContentHash
  };

  return <EncryptionContext.Provider value={value}>{children}</EncryptionContext.Provider>;
}

// ============================================================================
// Hook
// ============================================================================

export function useEncryption(): EncryptionContextType {
  const context = useContext(EncryptionContext);
  if (context === undefined) {
    throw new Error('useEncryption must be used within an EncryptionProvider');
  }
  return context;
}
