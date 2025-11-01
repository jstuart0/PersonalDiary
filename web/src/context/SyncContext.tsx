/**
 * Sync Context
 *
 * Manages synchronization of local data with backend API.
 * Handles background sync, conflict resolution, and offline queue.
 */

import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { Entry, SyncOperation, SyncStatus } from '@/types';
import { db } from '@/services/storage';
import { apiClient } from '@/services/api/client';
import { useAuth } from './AuthContext';
import { useEncryption } from './EncryptionContext';

// ============================================================================
// Types
// ============================================================================

interface SyncContextType {
  isSyncing: boolean;
  pendingOperations: number;
  lastSyncAt: string | null;
  lastError: string | null;

  syncNow: () => Promise<void>;
  queueEntry: (entry: Entry, operation: 'create' | 'update' | 'delete') => Promise<void>;
  getUnsyncedCount: () => Promise<number>;
}

// ============================================================================
// Context
// ============================================================================

const SyncContext = createContext<SyncContextType | undefined>(undefined);

// ============================================================================
// Provider Component
// ============================================================================

interface SyncProviderProps {
  children: ReactNode;
  autoSyncInterval?: number; // minutes
}

export function SyncProvider({ children, autoSyncInterval = 5 }: SyncProviderProps) {
  const { isAuthenticated, user } = useAuth();
  const { isReady: encryptionReady } = useEncryption();

  const [isSyncing, setIsSyncing] = useState(false);
  const [pendingOperations, setPendingOperations] = useState(0);
  const [lastSyncAt, setLastSyncAt] = useState<string | null>(null);
  const [lastError, setLastError] = useState<string | null>(null);

  /**
   * Update pending operations count
   */
  const updatePendingCount = useCallback(async () => {
    if (!user) return;

    try {
      const unsyncedEntries = await db.getUnsyncedEntries(user.id);
      const syncQueue = await db.getSyncQueue();
      setPendingOperations(unsyncedEntries.length + syncQueue.length);
    } catch (err) {
      console.error('Error updating pending count:', err);
    }
  }, [user]);

  // Update pending count when user changes
  useEffect(() => {
    updatePendingCount();
  }, [updatePendingCount]);

  /**
   * Queue an entry operation for sync
   */
  const queueEntry = useCallback(
    async (entry: Entry, operation: 'create' | 'update' | 'delete') => {
      const syncOp: SyncOperation = {
        id: crypto.randomUUID(),
        type: operation,
        entityType: 'entry',
        entityId: entry.id,
        data: entry,
        createdAt: new Date().toISOString(),
        retryCount: 0
      };

      await db.addToSyncQueue(syncOp);
      await updatePendingCount();
    },
    [updatePendingCount]
  );

  /**
   * Sync all pending operations
   */
  const syncNow = useCallback(async () => {
    if (!isAuthenticated || !encryptionReady || !user || isSyncing) {
      return;
    }

    setIsSyncing(true);
    setLastError(null);

    try {
      // Get all pending operations
      const operations = await db.getSyncQueue();

      if (operations.length === 0) {
        setLastSyncAt(new Date().toISOString());
        return;
      }

      // Process operations in batches
      const batchSize = 10;
      for (let i = 0; i < operations.length; i += batchSize) {
        const batch = operations.slice(i, i + batchSize);

        await Promise.all(
          batch.map(async (op) => {
            try {
              await processSyncOperation(op);
              await db.removeSyncOperation(op.id);
            } catch (err) {
              console.error('Sync operation failed:', op, err);
              // Update retry count
              op.retryCount++;
              op.lastError = err instanceof Error ? err.message : 'Unknown error';
              await db.addToSyncQueue(op);
            }
          })
        );
      }

      setLastSyncAt(new Date().toISOString());
      await updatePendingCount();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Sync failed';
      setLastError(errorMessage);
      console.error('Sync error:', err);
    } finally {
      setIsSyncing(false);
    }
  }, [isAuthenticated, encryptionReady, user, isSyncing, updatePendingCount]);

  /**
   * Process individual sync operation
   */
  const processSyncOperation = async (op: SyncOperation) => {
    if (op.entityType === 'entry') {
      const entry = op.data as Entry;

      switch (op.type) {
        case 'create':
          await apiClient.post('/entries', entry);
          break;

        case 'update':
          await apiClient.put(`/entries/${entry.id}`, entry);
          break;

        case 'delete':
          await apiClient.delete(`/entries/${entry.id}`);
          break;
      }
    }
  };

  /**
   * Get unsynced count
   */
  const getUnsyncedCount = useCallback(async (): Promise<number> => {
    if (!user) return 0;

    const unsyncedEntries = await db.getUnsyncedEntries(user.id);
    const syncQueue = await db.getSyncQueue();
    return unsyncedEntries.length + syncQueue.length;
  }, [user]);

  // Auto-sync interval
  useEffect(() => {
    if (!isAuthenticated || !encryptionReady || autoSyncInterval <= 0) {
      return;
    }

    const interval = setInterval(() => {
      syncNow();
    }, autoSyncInterval * 60 * 1000);

    return () => clearInterval(interval);
  }, [isAuthenticated, encryptionReady, autoSyncInterval, syncNow]);

  // Listen for online/offline events
  useEffect(() => {
    const handleOnline = () => {
      console.log('Back online - syncing...');
      syncNow();
    };

    window.addEventListener('online', handleOnline);
    return () => window.removeEventListener('online', handleOnline);
  }, [syncNow]);

  const value: SyncContextType = {
    isSyncing,
    pendingOperations,
    lastSyncAt,
    lastError,
    syncNow,
    queueEntry,
    getUnsyncedCount
  };

  return <SyncContext.Provider value={value}>{children}</SyncContext.Provider>;
}

// ============================================================================
// Hook
// ============================================================================

export function useSync(): SyncContextType {
  const context = useContext(SyncContext);
  if (context === undefined) {
    throw new Error('useSync must be used within a SyncProvider');
  }
  return context;
}
