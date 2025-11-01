/**
 * Context Providers Export
 *
 * Provides a single combined provider for all app contexts.
 */

import React, { ReactNode } from 'react';
import { AuthProvider } from './AuthContext';
import { EncryptionProvider } from './EncryptionContext';
import { SyncProvider } from './SyncContext';
import { SettingsProvider } from './SettingsContext';

export * from './AuthContext';
export * from './EncryptionContext';
export * from './SyncContext';
export * from './SettingsContext';

// ============================================================================
// Combined Provider
// ============================================================================

interface AppProvidersProps {
  children: ReactNode;
}

/**
 * Combines all app context providers in the correct order:
 * Auth -> Settings -> Encryption -> Sync
 */
export function AppProviders({ children }: AppProvidersProps) {
  return (
    <AuthProvider>
      <SettingsProvider>
        <EncryptionProvider>
          <SyncProvider>{children}</SyncProvider>
        </EncryptionProvider>
      </SettingsProvider>
    </AuthProvider>
  );
}
