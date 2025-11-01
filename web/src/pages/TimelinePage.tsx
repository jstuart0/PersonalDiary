/**
 * Timeline Page
 *
 * Main diary timeline showing all entries.
 */

import React, { useState, useEffect, useCallback } from 'react';
import { PlusIcon, MagnifyingGlassIcon } from '@heroicons/react/24/outline';
import { Button } from '@/components/common/Button';
import { Modal } from '@/components/common/Modal';
import { PageLoading, SkeletonCard } from '@/components/common/Loading';
import { EntryCard, EntryEditor, EntryDetail } from '@/components/entries';
import { SearchBar, SearchFilters } from '@/components/search';
import { ShareToFacebookModal } from '@/components/facebook';
import { useAuth, useEncryption, useSync } from '@/context';
import { DecryptedEntry, SearchFilters as ISearchFilters } from '@/types';
import { entryService } from '@/services/entries';
import { searchService } from '@/services/search';

// ============================================================================
// Component
// ============================================================================

export function TimelinePage() {
  const { user } = useAuth();
  const { isReady: encryptionReady } = useEncryption();
  const { isSyncing } = useSync();

  const [entries, setEntries] = useState<DecryptedEntry[]>([]);
  const [filteredEntries, setFilteredEntries] = useState<DecryptedEntry[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showNewEntry, setShowNewEntry] = useState(false);
  const [editingEntry, setEditingEntry] = useState<DecryptedEntry | null>(null);
  const [selectedEntry, setSelectedEntry] = useState<DecryptedEntry | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  // Search state
  const [searchQuery, setSearchQuery] = useState('');
  const [searchFilters, setSearchFilters] = useState<ISearchFilters>({ sortBy: 'date-newest' });
  const [showSearchFilters, setShowSearchFilters] = useState(false);
  const [searchSuggestions, setSearchSuggestions] = useState<string[]>([]);

  // Share state
  const [sharingEntry, setSharingEntry] = useState<DecryptedEntry | null>(null);

  /**
   * Load entries from IndexedDB
   */
  const loadEntries = useCallback(async () => {
    if (!user || !encryptionReady) return;

    try {
      setIsLoading(true);
      const userEntries = await entryService.getAllEntries(user.id);
      setEntries(userEntries);
      setFilteredEntries(userEntries);

      // Index entries for search
      searchService.indexEntries(userEntries);
    } catch (err) {
      console.error('Failed to load entries:', err);
    } finally {
      setIsLoading(false);
    }
  }, [user, encryptionReady]);

  useEffect(() => {
    loadEntries();
  }, [loadEntries]);

  /**
   * Handle search query change
   */
  const handleSearchChange = (query: string) => {
    setSearchQuery(query);

    // Update suggestions
    if (query.trim()) {
      const suggestions = searchService.getSuggestions(query);
      setSearchSuggestions(suggestions);
    } else {
      setSearchSuggestions([]);
    }
  };

  /**
   * Handle search
   */
  const handleSearch = (query: string) => {
    if (!query.trim() && !searchFilters.tags && !searchFilters.startDate && !searchFilters.endDate) {
      setFilteredEntries(entries);
      return;
    }

    const result = searchService.search({
      query: query.trim(),
      filters: searchFilters
    });

    setFilteredEntries(result.entries);
  };

  /**
   * Handle filter apply
   */
  const handleApplyFilters = (filters: ISearchFilters) => {
    setSearchFilters(filters);
    handleSearch(searchQuery);
  };

  /**
   * Handle create new entry
   */
  const handleCreateEntry = async (content: string, tags: string[]) => {
    if (!user) return;

    try {
      setIsSaving(true);
      await entryService.createEntry(user.id, content, tags);
      await loadEntries();
      setShowNewEntry(false);
    } catch (error) {
      console.error('Failed to create entry:', error);
      alert('Failed to save entry. Please try again.');
    } finally {
      setIsSaving(false);
    }
  };

  /**
   * Handle update entry
   */
  const handleUpdateEntry = async (content: string, tags: string[]) => {
    if (!editingEntry) return;

    try {
      setIsSaving(true);
      await entryService.updateEntry(editingEntry.id, content, tags);
      await loadEntries();
      setEditingEntry(null);
    } catch (error) {
      console.error('Failed to update entry:', error);
      alert('Failed to update entry. Please try again.');
    } finally {
      setIsSaving(false);
    }
  };

  /**
   * Handle delete entry
   */
  const handleDeleteEntry = async (entryId: string) => {
    try {
      await entryService.deleteEntry(entryId);
      await loadEntries();
      setSelectedEntry(null);
    } catch (error) {
      console.error('Failed to delete entry:', error);
      alert('Failed to delete entry. Please try again.');
    }
  };

  /**
   * Handle entry click to view detail
   */
  const handleViewEntry = (entry: DecryptedEntry) => {
    setSelectedEntry(entry);
  };

  /**
   * Handle navigate between entries
   */
  const handleNavigate = (direction: 'prev' | 'next') => {
    if (!selectedEntry) return;

    const currentIndex = entries.findIndex((e) => e.id === selectedEntry.id);
    if (currentIndex === -1) return;

    const newIndex = direction === 'prev' ? currentIndex - 1 : currentIndex + 1;
    if (newIndex >= 0 && newIndex < entries.length) {
      setSelectedEntry(entries[newIndex]);
    }
  };

  if (!user || !encryptionReady) {
    return <PageLoading />;
  }

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto p-4">
        <div className="space-y-4">
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
        </div>
      </div>
    );
  }

  const displayedEntries = filteredEntries;
  const availableTags = searchService.getAllTags();

  return (
    <>
      <div className="max-w-4xl mx-auto p-4 pb-20">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-3xl font-bold">My Diary</h1>
            <p className="text-muted-foreground mt-1">
              {searchQuery || Object.keys(searchFilters).some(k => searchFilters[k as keyof ISearchFilters])
                ? `${displayedEntries.length} of ${entries.length}`
                : entries.length}{' '}
              {entries.length === 1 ? 'entry' : 'entries'}
            </p>
          </div>
          <Button
            onClick={() => setShowNewEntry(true)}
            leftIcon={<PlusIcon className="h-5 w-5" />}
          >
            New Entry
          </Button>
        </div>

        {/* Search Bar */}
        <div className="mb-6">
          <SearchBar
            value={searchQuery}
            onChange={handleSearchChange}
            onSearch={handleSearch}
            onFilterClick={() => setShowSearchFilters(true)}
            suggestions={searchSuggestions}
          />
        </div>

        {/* Sync Status */}
        {isSyncing && (
          <div className="mb-4 text-sm text-muted-foreground text-center">
            Syncing...
          </div>
        )}

        {/* Empty State */}
        {entries.length === 0 && (
          <div className="text-center py-12">
            <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-muted">
              <PlusIcon className="h-8 w-8 text-muted-foreground" />
            </div>
            <h3 className="text-lg font-medium mb-2">No entries yet</h3>
            <p className="text-muted-foreground mb-6">
              Start your diary by creating your first entry
            </p>
            <Button onClick={() => setShowNewEntry(true)}>
              Create First Entry
            </Button>
          </div>
        )}

        {/* No Results State */}
        {entries.length > 0 && displayedEntries.length === 0 && (
          <div className="text-center py-12">
            <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-muted">
              <MagnifyingGlassIcon className="h-8 w-8 text-muted-foreground" />
            </div>
            <h3 className="text-lg font-medium mb-2">No entries found</h3>
            <p className="text-muted-foreground mb-6">
              Try adjusting your search or filters
            </p>
            <Button
              variant="outline"
              onClick={() => {
                setSearchQuery('');
                setSearchFilters({ sortBy: 'date-newest' });
                setFilteredEntries(entries);
              }}
            >
              Clear Search
            </Button>
          </div>
        )}

        {/* Entry List */}
        <div className="space-y-4">
          {displayedEntries.map((entry) => (
            <EntryCard
              key={entry.id}
              entry={entry}
              onClick={handleViewEntry}
              onEdit={(entry) => setEditingEntry(entry)}
              onDelete={handleDeleteEntry}
              onShare={(entry) => setSharingEntry(entry)}
            />
          ))}
        </div>
      </div>

      {/* Share to Facebook Modal */}
      {sharingEntry && (
        <ShareToFacebookModal
          entry={sharingEntry}
          isOpen={!!sharingEntry}
          onClose={() => setSharingEntry(null)}
        />
      )}

      {/* Search Filters Modal */}
      <SearchFilters
        isOpen={showSearchFilters}
        onClose={() => setShowSearchFilters(false)}
        filters={searchFilters}
        onApply={handleApplyFilters}
        availableTags={availableTags}
      />

      {/* New Entry Modal */}
      <Modal
        isOpen={showNewEntry}
        onClose={() => setShowNewEntry(false)}
        size="lg"
      >
        <EntryEditor
          onSave={handleCreateEntry}
          onCancel={() => setShowNewEntry(false)}
          isSaving={isSaving}
        />
      </Modal>

      {/* Edit Entry Modal */}
      <Modal
        isOpen={!!editingEntry}
        onClose={() => setEditingEntry(null)}
        size="lg"
      >
        {editingEntry && (
          <EntryEditor
            initialContent={editingEntry.content}
            initialTags={editingEntry.tags}
            onSave={handleUpdateEntry}
            onCancel={() => setEditingEntry(null)}
            isSaving={isSaving}
          />
        )}
      </Modal>

      {/* Entry Detail Modal */}
      {selectedEntry && (
        <EntryDetail
          entry={selectedEntry}
          isOpen={!!selectedEntry}
          onClose={() => setSelectedEntry(null)}
          onEdit={(entry) => {
            setSelectedEntry(null);
            setEditingEntry(entry);
          }}
          onDelete={handleDeleteEntry}
          onNavigate={handleNavigate}
        />
      )}
    </>
  );
}
