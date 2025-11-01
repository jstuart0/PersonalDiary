/**
 * IndexedDB Database Service
 *
 * Provides offline storage for encrypted entries, media, and sync metadata.
 * Uses idb library for better Promise-based API.
 */

import { openDB, DBSchema, IDBPDatabase } from 'idb';
import {
  Entry,
  Media,
  StoredKeys,
  SyncOperation,
  IntegrationAccount,
  AppSettings
} from '@/types';

// ============================================================================
// Database Schema
// ============================================================================

interface DiaryDB extends DBSchema {
  // User encryption keys (encrypted at rest)
  keys: {
    key: string; // userId
    value: StoredKeys;
  };

  // Entries (encrypted content)
  entries: {
    key: string; // entryId
    value: Entry;
    indexes: {
      'by-user': string; // userId
      'by-created': string; // createdAt
      'by-sync-status': string; // syncStatus
      'by-source': string; // source
    };
  };

  // Media files (encrypted blobs)
  media: {
    key: string; // mediaId
    value: Media & { encryptedBlob?: ArrayBuffer };
    indexes: {
      'by-entry': string; // entryId
      'by-upload-status': string; // uploadStatus
    };
  };

  // Tags
  tags: {
    key: string; // tag name
    value: {
      name: string;
      count: number;
      lastUsed: string;
    };
  };

  // Sync queue
  sync_queue: {
    key: string; // operationId
    value: SyncOperation;
    indexes: {
      'by-type': string; // type
      'by-created': string; // createdAt
    };
  };

  // Integration accounts
  integrations: {
    key: string; // integrationId
    value: IntegrationAccount;
    indexes: {
      'by-user': string; // userId
      'by-platform': string; // platform
    };
  };

  // App settings
  settings: {
    key: string; // userId
    value: AppSettings;
  };

  // Search index metadata (for client-side search)
  search_meta: {
    key: string; // userId
    value: {
      lastIndexed: string;
      entryCount: number;
    };
  };
}

// ============================================================================
// Database Service
// ============================================================================

export class DatabaseService {
  private static instance: DatabaseService;
  private db: IDBPDatabase<DiaryDB> | null = null;
  private readonly DB_NAME = 'personal-diary';
  private readonly DB_VERSION = 1;

  private constructor() {}

  static getInstance(): DatabaseService {
    if (!DatabaseService.instance) {
      DatabaseService.instance = new DatabaseService();
    }
    return DatabaseService.instance;
  }

  /**
   * Initialize database connection
   */
  async init(): Promise<void> {
    if (this.db) return;

    this.db = await openDB<DiaryDB>(this.DB_NAME, this.DB_VERSION, {
      upgrade(db) {
        // Keys store
        if (!db.objectStoreNames.contains('keys')) {
          db.createObjectStore('keys');
        }

        // Entries store
        if (!db.objectStoreNames.contains('entries')) {
          const entryStore = db.createObjectStore('entries', { keyPath: 'id' });
          entryStore.createIndex('by-user', 'userId');
          entryStore.createIndex('by-created', 'createdAt');
          entryStore.createIndex('by-sync-status', 'syncStatus');
          entryStore.createIndex('by-source', 'source');
        }

        // Media store
        if (!db.objectStoreNames.contains('media')) {
          const mediaStore = db.createObjectStore('media', { keyPath: 'id' });
          mediaStore.createIndex('by-entry', 'entryId');
          mediaStore.createIndex('by-upload-status', 'uploadStatus');
        }

        // Tags store
        if (!db.objectStoreNames.contains('tags')) {
          db.createObjectStore('tags', { keyPath: 'name' });
        }

        // Sync queue store
        if (!db.objectStoreNames.contains('sync_queue')) {
          const syncStore = db.createObjectStore('sync_queue', { keyPath: 'id' });
          syncStore.createIndex('by-type', 'type');
          syncStore.createIndex('by-created', 'createdAt');
        }

        // Integrations store
        if (!db.objectStoreNames.contains('integrations')) {
          const intStore = db.createObjectStore('integrations', { keyPath: 'id' });
          intStore.createIndex('by-user', 'userId');
          intStore.createIndex('by-platform', 'platform');
        }

        // Settings store
        if (!db.objectStoreNames.contains('settings')) {
          db.createObjectStore('settings');
        }

        // Search metadata store
        if (!db.objectStoreNames.contains('search_meta')) {
          db.createObjectStore('search_meta');
        }
      }
    });
  }

  /**
   * Close database connection
   */
  async close(): Promise<void> {
    if (this.db) {
      this.db.close();
      this.db = null;
    }
  }

  /**
   * Clear all data from database
   */
  async clearAll(): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    const tx = this.db.transaction(
      ['keys', 'entries', 'media', 'tags', 'sync_queue', 'integrations', 'settings', 'search_meta'],
      'readwrite'
    );

    await Promise.all([
      tx.objectStore('keys').clear(),
      tx.objectStore('entries').clear(),
      tx.objectStore('media').clear(),
      tx.objectStore('tags').clear(),
      tx.objectStore('sync_queue').clear(),
      tx.objectStore('integrations').clear(),
      tx.objectStore('settings').clear(),
      tx.objectStore('search_meta').clear()
    ]);

    await tx.done;
  }

  // ============================================================================
  // Keys Operations
  // ============================================================================

  async saveKeys(userId: string, keys: StoredKeys): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');
    await this.db.put('keys', keys, userId);
  }

  async getKeys(userId: string): Promise<StoredKeys | undefined> {
    if (!this.db) throw new Error('Database not initialized');
    return this.db.get('keys', userId);
  }

  async deleteKeys(userId: string): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');
    await this.db.delete('keys', userId);
  }

  // ============================================================================
  // Entry Operations
  // ============================================================================

  async saveEntry(entry: Entry): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');
    await this.db.put('entries', entry);
  }

  async getEntry(entryId: string): Promise<Entry | undefined> {
    if (!this.db) throw new Error('Database not initialized');
    return this.db.get('entries', entryId);
  }

  async getAllEntries(userId: string): Promise<Entry[]> {
    if (!this.db) throw new Error('Database not initialized');
    return this.db.getAllFromIndex('entries', 'by-user', userId);
  }

  async getEntriesByDateRange(
    userId: string,
    startDate: string,
    endDate: string
  ): Promise<Entry[]> {
    if (!this.db) throw new Error('Database not initialized');

    const allEntries = await this.getAllEntries(userId);
    return allEntries.filter(
      (entry) =>
        entry.createdAt >= startDate &&
        entry.createdAt <= endDate &&
        !entry.deletedAt
    );
  }

  async getEntriesBySource(userId: string, source: string): Promise<Entry[]> {
    if (!this.db) throw new Error('Database not initialized');

    const tx = this.db.transaction('entries', 'readonly');
    const index = tx.store.index('by-source');
    const entries = await index.getAll(source);

    return entries.filter((entry) => entry.userId === userId && !entry.deletedAt);
  }

  async getUnsyncedEntries(userId: string): Promise<Entry[]> {
    if (!this.db) throw new Error('Database not initialized');

    const allEntries = await this.getAllEntries(userId);
    return allEntries.filter(
      (entry) =>
        (entry.syncStatus === 'pending' || entry.syncStatus === 'failed') &&
        !entry.deletedAt
    );
  }

  async deleteEntry(entryId: string): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    // Soft delete - mark as deleted
    const entry = await this.getEntry(entryId);
    if (entry) {
      entry.deletedAt = new Date().toISOString();
      await this.saveEntry(entry);
    }
  }

  async hardDeleteEntry(entryId: string): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');
    await this.db.delete('entries', entryId);
  }

  async getEntryCount(userId: string): Promise<number> {
    if (!this.db) throw new Error('Database not initialized');
    const entries = await this.getAllEntries(userId);
    return entries.filter((e) => !e.deletedAt).length;
  }

  // ============================================================================
  // Media Operations
  // ============================================================================

  async saveMedia(media: Media & { encryptedBlob?: ArrayBuffer }): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');
    await this.db.put('media', media);
  }

  async getMedia(mediaId: string): Promise<(Media & { encryptedBlob?: ArrayBuffer }) | undefined> {
    if (!this.db) throw new Error('Database not initialized');
    return this.db.get('media', mediaId);
  }

  async getMediaByEntry(entryId: string): Promise<(Media & { encryptedBlob?: ArrayBuffer })[]> {
    if (!this.db) throw new Error('Database not initialized');
    return this.db.getAllFromIndex('media', 'by-entry', entryId);
  }

  async deleteMedia(mediaId: string): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');
    await this.db.delete('media', mediaId);
  }

  async getMediaStorageSize(): Promise<number> {
    if (!this.db) throw new Error('Database not initialized');

    const allMedia = await this.db.getAll('media');
    return allMedia.reduce((total, media) => total + media.fileSize, 0);
  }

  // ============================================================================
  // Tag Operations
  // ============================================================================

  async saveTag(name: string): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');

    const existing = await this.db.get('tags', name);
    if (existing) {
      existing.count++;
      existing.lastUsed = new Date().toISOString();
      await this.db.put('tags', existing);
    } else {
      await this.db.put('tags', {
        name,
        count: 1,
        lastUsed: new Date().toISOString()
      });
    }
  }

  async getAllTags(): Promise<string[]> {
    if (!this.db) throw new Error('Database not initialized');
    const tags = await this.db.getAll('tags');
    return tags.map((t) => t.name).sort();
  }

  async getPopularTags(limit: number = 10): Promise<string[]> {
    if (!this.db) throw new Error('Database not initialized');

    const tags = await this.db.getAll('tags');
    return tags
      .sort((a, b) => b.count - a.count)
      .slice(0, limit)
      .map((t) => t.name);
  }

  // ============================================================================
  // Sync Queue Operations
  // ============================================================================

  async addToSyncQueue(operation: SyncOperation): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');
    await this.db.put('sync_queue', operation);
  }

  async getSyncQueue(): Promise<SyncOperation[]> {
    if (!this.db) throw new Error('Database not initialized');
    return this.db.getAll('sync_queue');
  }

  async removeSyncOperation(operationId: string): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');
    await this.db.delete('sync_queue', operationId);
  }

  async clearSyncQueue(): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');
    await this.db.clear('sync_queue');
  }

  // ============================================================================
  // Settings Operations
  // ============================================================================

  async saveSettings(userId: string, settings: AppSettings): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');
    await this.db.put('settings', settings, userId);
  }

  async getSettings(userId: string): Promise<AppSettings | undefined> {
    if (!this.db) throw new Error('Database not initialized');
    return this.db.get('settings', userId);
  }

  // ============================================================================
  // Integration Operations
  // ============================================================================

  async saveIntegration(integration: IntegrationAccount): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');
    await this.db.put('integrations', integration);
  }

  async getIntegration(integrationId: string): Promise<IntegrationAccount | undefined> {
    if (!this.db) throw new Error('Database not initialized');
    return this.db.get('integrations', integrationId);
  }

  async getIntegrationsByUser(userId: string): Promise<IntegrationAccount[]> {
    if (!this.db) throw new Error('Database not initialized');
    return this.db.getAllFromIndex('integrations', 'by-user', userId);
  }

  async deleteIntegration(integrationId: string): Promise<void> {
    if (!this.db) throw new Error('Database not initialized');
    await this.db.delete('integrations', integrationId);
  }

  // ============================================================================
  // Utility Functions
  // ============================================================================

  /**
   * Get database storage estimate
   */
  async getStorageEstimate(): Promise<{ usage: number; quota: number }> {
    if ('storage' in navigator && 'estimate' in navigator.storage) {
      const estimate = await navigator.storage.estimate();
      return {
        usage: estimate.usage || 0,
        quota: estimate.quota || 0
      };
    }
    return { usage: 0, quota: 0 };
  }

  /**
   * Check if database is initialized
   */
  isInitialized(): boolean {
    return this.db !== null;
  }
}

// Export singleton instance
export const db = DatabaseService.getInstance();
