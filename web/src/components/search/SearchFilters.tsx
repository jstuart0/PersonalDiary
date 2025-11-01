/**
 * Search Filters Component
 *
 * Modal for advanced search filters.
 */

import React, { useState } from 'react';
import { XMarkIcon } from '@heroicons/react/24/outline';
import { Modal } from '@/components/common/Modal';
import { Button } from '@/components/common/Button';
import { Input } from '@/components/common/Input';
import { Badge } from '@/components/common/Badge';
import { SearchFilters as ISearchFilters, EntrySource } from '@/types';

// ============================================================================
// Types
// ============================================================================

export interface SearchFiltersProps {
  isOpen: boolean;
  onClose: () => void;
  filters: ISearchFilters;
  onApply: (filters: ISearchFilters) => void;
  availableTags: string[];
}

// ============================================================================
// Component
// ============================================================================

export function SearchFilters({
  isOpen,
  onClose,
  filters,
  onApply,
  availableTags
}: SearchFiltersProps) {
  const [localFilters, setLocalFilters] = useState<ISearchFilters>(filters);

  /**
   * Handle tag selection
   */
  const handleToggleTag = (tag: string) => {
    const tags = localFilters.tags || [];
    const newTags = tags.includes(tag)
      ? tags.filter((t) => t !== tag)
      : [...tags, tag];

    setLocalFilters({ ...localFilters, tags: newTags });
  };

  /**
   * Handle apply
   */
  const handleApply = () => {
    onApply(localFilters);
    onClose();
  };

  /**
   * Handle reset
   */
  const handleReset = () => {
    setLocalFilters({
      tags: [],
      startDate: undefined,
      endDate: undefined,
      source: undefined,
      sortBy: 'date-newest'
    });
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="md">
      <div className="flex flex-col h-full">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-lg font-semibold">Search Filters</h2>
          <Button
            variant="ghost"
            size="sm"
            onClick={onClose}
            aria-label="Close"
          >
            <XMarkIcon className="h-5 w-5" />
          </Button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {/* Tags Filter */}
          <div>
            <label className="block text-sm font-medium mb-3">
              Filter by Tags
            </label>
            <div className="flex flex-wrap gap-2">
              {availableTags.map((tag) => {
                const isSelected = localFilters.tags?.includes(tag) || false;
                return (
                  <Badge
                    key={tag}
                    variant={isSelected ? 'default' : 'outline'}
                    className="cursor-pointer hover:opacity-80"
                    onClick={() => handleToggleTag(tag)}
                  >
                    #{tag}
                  </Badge>
                );
              })}
            </div>
            {availableTags.length === 0 && (
              <p className="text-sm text-muted-foreground italic">
                No tags available
              </p>
            )}
          </div>

          {/* Date Range Filter */}
          <div>
            <label className="block text-sm font-medium mb-3">
              Date Range
            </label>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label htmlFor="start-date" className="block text-xs text-muted-foreground mb-1">
                  From
                </label>
                <Input
                  id="start-date"
                  type="date"
                  value={localFilters.startDate || ''}
                  onChange={(e) =>
                    setLocalFilters({
                      ...localFilters,
                      startDate: e.target.value || undefined
                    })
                  }
                />
              </div>
              <div>
                <label htmlFor="end-date" className="block text-xs text-muted-foreground mb-1">
                  To
                </label>
                <Input
                  id="end-date"
                  type="date"
                  value={localFilters.endDate || ''}
                  onChange={(e) =>
                    setLocalFilters({
                      ...localFilters,
                      endDate: e.target.value || undefined
                    })
                  }
                />
              </div>
            </div>
          </div>

          {/* Source Filter */}
          <div>
            <label className="block text-sm font-medium mb-3">
              Source
            </label>
            <div className="space-y-2">
              {['all', EntrySource.DIARY, EntrySource.FACEBOOK, EntrySource.INSTAGRAM].map(
                (source) => (
                  <label key={source} className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="source"
                      value={source}
                      checked={source === 'all' ? !localFilters.source : localFilters.source === source}
                      onChange={(e) =>
                        setLocalFilters({
                          ...localFilters,
                          source: e.target.value === 'all' ? undefined : (e.target.value as EntrySource)
                        })
                      }
                      className="h-4 w-4"
                    />
                    <span className="text-sm capitalize">
                      {source === 'all' ? 'All Sources' : source}
                    </span>
                  </label>
                )
              )}
            </div>
          </div>

          {/* Sort By */}
          <div>
            <label className="block text-sm font-medium mb-3">
              Sort By
            </label>
            <div className="space-y-2">
              {[
                { value: 'date-newest', label: 'Newest First' },
                { value: 'date-oldest', label: 'Oldest First' },
                { value: 'relevance', label: 'Relevance' }
              ].map((option) => (
                <label key={option.value} className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="sortBy"
                    value={option.value}
                    checked={localFilters.sortBy === option.value}
                    onChange={(e) =>
                      setLocalFilters({
                        ...localFilters,
                        sortBy: e.target.value as ISearchFilters['sortBy']
                      })
                    }
                    className="h-4 w-4"
                  />
                  <span className="text-sm">{option.label}</span>
                </label>
              ))}
            </div>
          </div>
        </div>

        {/* Actions */}
        <div className="flex items-center justify-between p-6 border-t">
          <Button variant="ghost" onClick={handleReset}>
            Reset
          </Button>
          <div className="flex gap-2">
            <Button variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button onClick={handleApply}>Apply Filters</Button>
          </div>
        </div>
      </div>
    </Modal>
  );
}
