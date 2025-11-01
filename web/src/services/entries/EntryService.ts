/**
 * Entry Service
 *
 * Handles creation, updating, and deletion of diary entries.
 * Manages encryption and sync operations.
 */

import { Entry, DecryptedEntry, SyncStatus, EntrySource, SyncOperation } from '@/types';
import { db } from '@/services/storage';
import { EncryptionService } from '@/services/encryption';

// ============================================================================
// Entry Service Class
// ============================================================================

export class EntryService {
  private static instance: EntryService;
  private encryptionService: EncryptionService;

  private constructor() {
    this.encryptionService = EncryptionService.getInstance();
  }

  static getInstance(): EntryService {
    if (!EntryService.instance) {
      EntryService.instance = new EntryService();
    }
    return EntryService.instance;
  }

  /**
   * Create new entry
   */
  async createEntry(
    userId: string,
    content: string,
    tags: string[] = [],
    source: EntrySource = EntrySource.DIARY,
    mediaIds: string[] = []
  ): Promise<Entry> {
    // Encrypt content
    const encryptedContent = await this.encryptionService.encryptContent(content);

    // Generate content hash for deduplication
    const contentHash = await this.generateContentHash(content);

    // Create entry
    const entry: Entry = {
      id: this.generateId(),
      userId,
      encryptedContent,
      contentHash,
      source,
      tags,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      mediaIds,
      syncStatus: SyncStatus.PENDING,
      lastSyncedAt: undefined
    };

    // Save to IndexedDB
    await db.saveEntry(entry);

    // Save tags
    for (const tag of tags) {
      await db.saveTag(tag);
    }

    // Add to sync queue
    await this.addToSyncQueue('create', entry);

    return entry;
  }

  /**
   * Update existing entry
   */
  async updateEntry(
    entryId: string,
    content: string,
    tags: string[]
  ): Promise<Entry> {
    // Get existing entry
    const existingEntry = await db.getEntry(entryId);
    if (!existingEntry) {
      throw new Error('Entry not found');
    }

    // Encrypt new content
    const encryptedContent = await this.encryptionService.encryptContent(content);

    // Generate new content hash
    const contentHash = await this.generateContentHash(content);

    // Update entry
    const updatedEntry: Entry = {
      ...existingEntry,
      encryptedContent,
      contentHash,
      tags,
      updatedAt: new Date().toISOString(),
      syncStatus: SyncStatus.PENDING
    };

    // Save to IndexedDB
    await db.saveEntry(updatedEntry);

    // Save new tags
    for (const tag of tags) {
      await db.saveTag(tag);
    }

    // Add to sync queue
    await this.addToSyncQueue('update', updatedEntry);

    return updatedEntry;
  }

  /**
   * Delete entry (soft delete)
   */
  async deleteEntry(entryId: string): Promise<void> {
    const entry = await db.getEntry(entryId);
    if (!entry) {
      throw new Error('Entry not found');
    }

    // Soft delete
    await db.deleteEntry(entryId);

    // Add to sync queue
    await this.addToSyncQueue('delete', { ...entry, deletedAt: new Date().toISOString() });
  }

  /**
   * Get entry by ID and decrypt
   */
  async getEntry(entryId: string): Promise<DecryptedEntry | null> {
    const entry = await db.getEntry(entryId);
    if (!entry || entry.deletedAt) {
      return null;
    }

    // Decrypt content
    const content = await this.encryptionService.decryptContent(entry.encryptedContent);

    return {
      ...entry,
      content
    };
  }

  /**
   * Get all entries for user and decrypt
   */
  async getAllEntries(userId: string): Promise<DecryptedEntry[]> {
    const entries = await db.getAllEntries(userId);

    // Filter out deleted entries and decrypt
    const decryptedEntries = await Promise.all(
      entries
        .filter((entry) => !entry.deletedAt)
        .map(async (entry) => {
          try {
            const content = await this.encryptionService.decryptContent(entry.encryptedContent);
            return { ...entry, content } as DecryptedEntry;
          } catch (error) {
            console.error('Failed to decrypt entry:', entry.id, error);
            return null;
          }
        })
    );

    // Filter out failed decryptions and sort by date (newest first)
    return decryptedEntries
      .filter((entry): entry is DecryptedEntry => entry !== null)
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  }

  /**
   * Get entries by date range
   */
  async getEntriesByDateRange(
    userId: string,
    startDate: string,
    endDate: string
  ): Promise<DecryptedEntry[]> {
    const entries = await db.getEntriesByDateRange(userId, startDate, endDate);

    const decryptedEntries = await Promise.all(
      entries.map(async (entry) => {
        const content = await this.encryptionService.decryptContent(entry.encryptedContent);
        return { ...entry, content } as DecryptedEntry;
      })
    );

    return decryptedEntries.sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    );
  }

  /**
   * Get entries by tag
   */
  async getEntriesByTag(userId: string, tag: string): Promise<DecryptedEntry[]> {
    const allEntries = await this.getAllEntries(userId);
    return allEntries.filter((entry) => entry.tags.includes(tag));
  }

  /**
   * Search entries (client-side)
   */
  async searchEntries(userId: string, query: string): Promise<DecryptedEntry[]> {
    const allEntries = await this.getAllEntries(userId);

    // Simple text search (will be enhanced with Fuse.js)
    const lowerQuery = query.toLowerCase();
    return allEntries.filter((entry) =>
      entry.content.toLowerCase().includes(lowerQuery) ||
      entry.tags.some((tag) => tag.toLowerCase().includes(lowerQuery))
    );
  }

  /**
   * Get entry count
   */
  async getEntryCount(userId: string): Promise<number> {
    return db.getEntryCount(userId);
  }

  /**
   * Get all tags
   */
  async getAllTags(): Promise<string[]> {
    return db.getAllTags();
  }

  /**
   * Get popular tags
   */
  async getPopularTags(limit: number = 10): Promise<string[]> {
    return db.getPopularTags(limit);
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

  /**
   * Generate unique ID
   */
  private generateId(): string {
    return `entry_${Date.now()}_${Math.random().toString(36).substring(2, 11)}`;
  }

  /**
   * Generate content hash for deduplication
   */
  private async generateContentHash(content: string): Promise<string> {
    const encoder = new TextEncoder();
    const data = encoder.encode(content);
    const hashBuffer = await crypto.subtle.digest('SHA-256', data);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map((b) => b.toString(16).padStart(2, '0')).join('');
  }

  /**
   * Add operation to sync queue
   */
  private async addToSyncQueue(
    type: 'create' | 'update' | 'delete',
    entry: Entry
  ): Promise<void> {
    const operation: SyncOperation = {
      id: this.generateId(),
      type,
      entityType: 'entry',
      entityId: entry.id,
      data: entry,
      createdAt: new Date().toISOString(),
      retryCount: 0
    };

    await db.addToSyncQueue(operation);
  }

  /**
   * Check for duplicate content
   */
  async checkDuplicate(userId: string, content: string): Promise<Entry | null> {
    const contentHash = await this.generateContentHash(content);
    const allEntries = await db.getAllEntries(userId);

    return allEntries.find(
      (entry) => entry.contentHash === contentHash && !entry.deletedAt
    ) || null;
  }
}

// Export singleton instance
export const entryService = EntryService.getInstance();
