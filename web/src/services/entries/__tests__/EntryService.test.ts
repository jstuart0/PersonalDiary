/**
 * Entry Service Tests
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { EntryService } from '../EntryService';
import { EntrySource, SyncStatus } from '@/types';

describe('EntryService', () => {
  let entryService: EntryService;
  const mockUserId = 'user_123';

  beforeEach(() => {
    entryService = EntryService.getInstance();
  });

  describe('createEntry', () => {
    it('creates a new entry with encrypted content', async () => {
      const content = 'Test entry content';
      const tags = ['test', 'diary'];

      const entry = await entryService.createEntry(
        mockUserId,
        content,
        tags,
        EntrySource.DIARY
      );

      expect(entry).toBeDefined();
      expect(entry.userId).toBe(mockUserId);
      expect(entry.tags).toEqual(tags);
      expect(entry.source).toBe(EntrySource.DIARY);
      expect(entry.syncStatus).toBe(SyncStatus.PENDING);
      expect(entry.encryptedContent).toBeDefined();
      expect(entry.encryptedContent).not.toBe(content); // Should be encrypted
    });

    it('generates unique IDs for each entry', async () => {
      const entry1 = await entryService.createEntry(mockUserId, 'Entry 1');
      const entry2 = await entryService.createEntry(mockUserId, 'Entry 2');

      expect(entry1.id).not.toBe(entry2.id);
    });

    it('generates content hash for deduplication', async () => {
      const content = 'Test content';
      const entry = await entryService.createEntry(mockUserId, content);

      expect(entry.contentHash).toBeDefined();
      expect(entry.contentHash.length).toBe(64); // SHA-256 hash
    });
  });

  describe('updateEntry', () => {
    it('updates entry content and tags', async () => {
      // Create initial entry
      const originalEntry = await entryService.createEntry(
        mockUserId,
        'Original content',
        ['original']
      );

      // Update entry
      const updatedContent = 'Updated content';
      const updatedTags = ['updated', 'test'];

      const updatedEntry = await entryService.updateEntry(
        originalEntry.id,
        updatedContent,
        updatedTags
      );

      expect(updatedEntry.id).toBe(originalEntry.id);
      expect(updatedEntry.tags).toEqual(updatedTags);
      expect(updatedEntry.syncStatus).toBe(SyncStatus.PENDING);
      expect(updatedEntry.updatedAt).not.toBe(originalEntry.createdAt);
    });
  });

  describe('deleteEntry', () => {
    it('soft deletes an entry', async () => {
      const entry = await entryService.createEntry(mockUserId, 'To be deleted');

      await entryService.deleteEntry(entry.id);

      const deletedEntry = await entryService.getEntry(entry.id);
      expect(deletedEntry).toBeNull(); // Should return null for deleted entries
    });
  });

  describe('searchEntries', () => {
    it('searches entries by content', async () => {
      await entryService.createEntry(mockUserId, 'Hello world');
      await entryService.createEntry(mockUserId, 'Goodbye world');
      await entryService.createEntry(mockUserId, 'Something else');

      const results = await entryService.searchEntries(mockUserId, 'world');

      expect(results.length).toBe(2);
    });

    it('searches entries by tags', async () => {
      await entryService.createEntry(mockUserId, 'Entry 1', ['travel']);
      await entryService.createEntry(mockUserId, 'Entry 2', ['work']);
      await entryService.createEntry(mockUserId, 'Entry 3', ['travel', 'food']);

      const results = await entryService.searchEntries(mockUserId, 'travel');

      expect(results.length).toBe(2);
    });
  });

  describe('getEntriesByTag', () => {
    it('returns entries with specific tag', async () => {
      await entryService.createEntry(mockUserId, 'Entry 1', ['work']);
      await entryService.createEntry(mockUserId, 'Entry 2', ['personal']);
      await entryService.createEntry(mockUserId, 'Entry 3', ['work', 'project']);

      const results = await entryService.getEntriesByTag(mockUserId, 'work');

      expect(results.length).toBe(2);
    });
  });
});
