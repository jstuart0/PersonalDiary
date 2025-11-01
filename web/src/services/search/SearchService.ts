/**
 * Search Service
 *
 * Client-side search using Fuse.js for E2E tier.
 * Uses web worker for background processing to avoid blocking UI.
 */

import Fuse from 'fuse.js';
import { DecryptedEntry, SearchQuery, SearchResult, SearchFilters } from '@/types';

// ============================================================================
// Fuse.js Configuration
// ============================================================================

const FUSE_OPTIONS: Fuse.IFuseOptions<DecryptedEntry> = {
  keys: [
    {
      name: 'content',
      weight: 0.7
    },
    {
      name: 'tags',
      weight: 0.3
    }
  ],
  threshold: 0.4, // 0 = exact match, 1 = match anything
  includeScore: true,
  includeMatches: true,
  minMatchCharLength: 2,
  ignoreLocation: true
};

// ============================================================================
// Search Service Class
// ============================================================================

export class SearchService {
  private static instance: SearchService;
  private fuseInstance: Fuse<DecryptedEntry> | null = null;
  private entries: DecryptedEntry[] = [];

  private constructor() {}

  static getInstance(): SearchService {
    if (!SearchService.instance) {
      SearchService.instance = new SearchService();
    }
    return SearchService.instance;
  }

  /**
   * Index entries for search
   */
  indexEntries(entries: DecryptedEntry[]): void {
    this.entries = entries;
    this.fuseInstance = new Fuse(entries, FUSE_OPTIONS);
  }

  /**
   * Search entries
   */
  search(query: SearchQuery): SearchResult {
    const { query: searchText, filters, page = 1, perPage = 20 } = query;

    // Start with all entries
    let results = this.entries;

    // Apply text search if query is provided
    if (searchText && searchText.trim() !== '') {
      if (!this.fuseInstance) {
        this.indexEntries(this.entries);
      }

      const fuseResults = this.fuseInstance!.search(searchText);
      results = fuseResults.map((result) => result.item);
    }

    // Apply filters
    if (filters) {
      results = this.applyFilters(results, filters);
    }

    // Apply sorting
    results = this.applySorting(results, filters?.sortBy || 'date-newest');

    // Apply pagination
    const startIndex = (page - 1) * perPage;
    const endIndex = startIndex + perPage;
    const paginatedResults = results.slice(startIndex, endIndex);

    return {
      entries: paginatedResults,
      total: results.length,
      page,
      perPage
    };
  }

  /**
   * Get search suggestions (tags and common terms)
   */
  getSuggestions(partialQuery: string): string[] {
    if (!partialQuery || partialQuery.trim() === '') {
      return [];
    }

    const query = partialQuery.toLowerCase();
    const suggestions = new Set<string>();

    // Add matching tags
    this.entries.forEach((entry) => {
      entry.tags.forEach((tag) => {
        if (tag.toLowerCase().includes(query)) {
          suggestions.add(`#${tag}`);
        }
      });
    });

    // Add matching content snippets (first few words that match)
    if (this.fuseInstance) {
      const results = this.fuseInstance.search(query, { limit: 5 });
      results.forEach((result) => {
        if (result.matches) {
          result.matches.forEach((match) => {
            if (match.key === 'content' && match.value) {
              // Extract a snippet around the match
              const snippet = this.extractSnippet(match.value, query);
              if (snippet) {
                suggestions.add(snippet);
              }
            }
          });
        }
      });
    }

    return Array.from(suggestions).slice(0, 10);
  }

  /**
   * Get all unique tags
   */
  getAllTags(): string[] {
    const tags = new Set<string>();
    this.entries.forEach((entry) => {
      entry.tags.forEach((tag) => tags.add(tag));
    });
    return Array.from(tags).sort();
  }

  /**
   * Get popular tags (by usage count)
   */
  getPopularTags(limit: number = 10): string[] {
    const tagCounts = new Map<string, number>();

    this.entries.forEach((entry) => {
      entry.tags.forEach((tag) => {
        tagCounts.set(tag, (tagCounts.get(tag) || 0) + 1);
      });
    });

    return Array.from(tagCounts.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, limit)
      .map(([tag]) => tag);
  }

  /**
   * Get entries by tag
   */
  searchByTag(tag: string): DecryptedEntry[] {
    return this.entries.filter((entry) =>
      entry.tags.some((t) => t.toLowerCase() === tag.toLowerCase())
    );
  }

  /**
   * Get entries by date range
   */
  searchByDateRange(startDate: string, endDate: string): DecryptedEntry[] {
    return this.entries.filter((entry) => {
      const entryDate = new Date(entry.createdAt);
      const start = new Date(startDate);
      const end = new Date(endDate);
      return entryDate >= start && entryDate <= end;
    });
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

  /**
   * Apply filters to results
   */
  private applyFilters(entries: DecryptedEntry[], filters: SearchFilters): DecryptedEntry[] {
    let filtered = entries;

    // Filter by tags
    if (filters.tags && filters.tags.length > 0) {
      filtered = filtered.filter((entry) =>
        filters.tags!.some((tag) =>
          entry.tags.some((entryTag) => entryTag.toLowerCase() === tag.toLowerCase())
        )
      );
    }

    // Filter by date range
    if (filters.startDate) {
      const startDate = new Date(filters.startDate);
      filtered = filtered.filter((entry) => new Date(entry.createdAt) >= startDate);
    }

    if (filters.endDate) {
      const endDate = new Date(filters.endDate);
      filtered = filtered.filter((entry) => new Date(entry.createdAt) <= endDate);
    }

    // Filter by source
    if (filters.source) {
      filtered = filtered.filter((entry) => entry.source === filters.source);
    }

    return filtered;
  }

  /**
   * Apply sorting to results
   */
  private applySorting(
    entries: DecryptedEntry[],
    sortBy: 'relevance' | 'date-newest' | 'date-oldest'
  ): DecryptedEntry[] {
    const sorted = [...entries];

    switch (sortBy) {
      case 'date-newest':
        return sorted.sort(
          (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );

      case 'date-oldest':
        return sorted.sort(
          (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
        );

      case 'relevance':
      default:
        // Already sorted by relevance from Fuse.js
        return sorted;
    }
  }

  /**
   * Extract snippet around match
   */
  private extractSnippet(text: string, query: string, contextWords: number = 5): string | null {
    const words = text.split(/\s+/);
    const queryLower = query.toLowerCase();

    for (let i = 0; i < words.length; i++) {
      if (words[i].toLowerCase().includes(queryLower)) {
        const start = Math.max(0, i - contextWords);
        const end = Math.min(words.length, i + contextWords + 1);
        const snippet = words.slice(start, end).join(' ');
        return snippet;
      }
    }

    return null;
  }

  /**
   * Clear index
   */
  clear(): void {
    this.entries = [];
    this.fuseInstance = null;
  }

  /**
   * Get index stats
   */
  getStats(): { entryCount: number; tagCount: number } {
    const tags = new Set<string>();
    this.entries.forEach((entry) => {
      entry.tags.forEach((tag) => tags.add(tag));
    });

    return {
      entryCount: this.entries.length,
      tagCount: tags.size
    };
  }
}

// Export singleton instance
export const searchService = SearchService.getInstance();
