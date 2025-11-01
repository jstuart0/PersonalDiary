/**
 * Search Bar Component
 *
 * Search input with suggestions and filters.
 */

import React, { useState, useRef, useEffect } from 'react';
import {
  MagnifyingGlassIcon,
  XMarkIcon,
  FunnelIcon
} from '@heroicons/react/24/outline';
import { Input } from '@/components/common/Input';
import { Button } from '@/components/common/Button';

// ============================================================================
// Types
// ============================================================================

export interface SearchBarProps {
  value: string;
  onChange: (value: string) => void;
  onSearch: (query: string) => void;
  onFilterClick?: () => void;
  suggestions?: string[];
  placeholder?: string;
  showFilters?: boolean;
}

// ============================================================================
// Component
// ============================================================================

export function SearchBar({
  value,
  onChange,
  onSearch,
  onFilterClick,
  suggestions = [],
  placeholder = 'Search entries...',
  showFilters = true
}: SearchBarProps) {
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);
  const inputRef = useRef<HTMLInputElement>(null);
  const suggestionsRef = useRef<HTMLDivElement>(null);

  /**
   * Handle input change
   */
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value;
    onChange(newValue);
    setShowSuggestions(newValue.trim().length > 0 && suggestions.length > 0);
    setSelectedIndex(-1);
  };

  /**
   * Handle submit
   */
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (value.trim()) {
      onSearch(value);
      setShowSuggestions(false);
    }
  };

  /**
   * Handle clear
   */
  const handleClear = () => {
    onChange('');
    setShowSuggestions(false);
    inputRef.current?.focus();
  };

  /**
   * Handle suggestion click
   */
  const handleSuggestionClick = (suggestion: string) => {
    onChange(suggestion);
    onSearch(suggestion);
    setShowSuggestions(false);
  };

  /**
   * Handle keyboard navigation
   */
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!showSuggestions || suggestions.length === 0) return;

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setSelectedIndex((prev) => Math.min(prev + 1, suggestions.length - 1));
        break;

      case 'ArrowUp':
        e.preventDefault();
        setSelectedIndex((prev) => Math.max(prev - 1, -1));
        break;

      case 'Enter':
        if (selectedIndex >= 0) {
          e.preventDefault();
          handleSuggestionClick(suggestions[selectedIndex]);
        }
        break;

      case 'Escape':
        setShowSuggestions(false);
        setSelectedIndex(-1);
        break;
    }
  };

  /**
   * Close suggestions when clicking outside
   */
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        suggestionsRef.current &&
        !suggestionsRef.current.contains(event.target as Node) &&
        inputRef.current &&
        !inputRef.current.contains(event.target as Node)
      ) {
        setShowSuggestions(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <form onSubmit={handleSubmit} className="relative">
      <div className="flex gap-2">
        {/* Search Input */}
        <div className="relative flex-1">
          <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
            <MagnifyingGlassIcon className="h-5 w-5 text-muted-foreground" />
          </div>

          <input
            ref={inputRef}
            type="text"
            value={value}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            placeholder={placeholder}
            className="w-full pl-10 pr-10 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
          />

          {value && (
            <button
              type="button"
              onClick={handleClear}
              className="absolute inset-y-0 right-0 flex items-center pr-3"
              aria-label="Clear search"
            >
              <XMarkIcon className="h-5 w-5 text-muted-foreground hover:text-foreground" />
            </button>
          )}

          {/* Suggestions Dropdown */}
          {showSuggestions && suggestions.length > 0 && (
            <div
              ref={suggestionsRef}
              className="absolute z-50 w-full mt-1 bg-popover border rounded-lg shadow-lg max-h-60 overflow-y-auto"
            >
              {suggestions.map((suggestion, index) => (
                <button
                  key={index}
                  type="button"
                  onClick={() => handleSuggestionClick(suggestion)}
                  className={`w-full px-4 py-2 text-left hover:bg-accent transition-colors ${
                    index === selectedIndex ? 'bg-accent' : ''
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <MagnifyingGlassIcon className="h-4 w-4 text-muted-foreground" />
                    <span className="text-sm">{suggestion}</span>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Filter Button */}
        {showFilters && onFilterClick && (
          <Button
            type="button"
            variant="outline"
            onClick={onFilterClick}
            leftIcon={<FunnelIcon className="h-5 w-5" />}
          >
            Filters
          </Button>
        )}

        {/* Search Button */}
        <Button type="submit" disabled={!value.trim()}>
          Search
        </Button>
      </div>
    </form>
  );
}
