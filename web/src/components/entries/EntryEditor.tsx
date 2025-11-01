/**
 * Entry Editor Component
 *
 * Rich text editor for creating and editing diary entries.
 */

import React, { useState, useRef, useEffect } from 'react';
import { XMarkIcon, PhotoIcon, TagIcon } from '@heroicons/react/24/outline';
import { Button } from '@/components/common/Button';
import { Input } from '@/components/common/Input';
import { Badge } from '@/components/common/Badge';

// ============================================================================
// Types
// ============================================================================

export interface EntryEditorProps {
  initialContent?: string;
  initialTags?: string[];
  onSave: (content: string, tags: string[]) => Promise<void>;
  onCancel: () => void;
  onAttachMedia?: () => void;
  isSaving?: boolean;
}

// ============================================================================
// Component
// ============================================================================

export function EntryEditor({
  initialContent = '',
  initialTags = [],
  onSave,
  onCancel,
  onAttachMedia,
  isSaving = false
}: EntryEditorProps) {
  const [content, setContent] = useState(initialContent);
  const [tags, setTags] = useState<string[]>(initialTags);
  const [tagInput, setTagInput] = useState('');
  const [showTagInput, setShowTagInput] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Auto-focus on mount
  useEffect(() => {
    textareaRef.current?.focus();
  }, []);

  // Auto-resize textarea
  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = textarea.scrollHeight + 'px';
    }
  }, [content]);

  /**
   * Handle save
   */
  const handleSave = async () => {
    if (content.trim().length === 0) {
      return;
    }

    await onSave(content, tags);
  };

  /**
   * Add tag
   */
  const handleAddTag = (tagName: string) => {
    const normalized = tagName.trim().toLowerCase().replace(/[^a-z0-9-_]/g, '');

    if (normalized && !tags.includes(normalized)) {
      setTags([...tags, normalized]);
      setTagInput('');
    }
  };

  /**
   * Remove tag
   */
  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter((tag) => tag !== tagToRemove));
  };

  /**
   * Handle tag input
   */
  const handleTagInputKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && tagInput.trim()) {
      e.preventDefault();
      handleAddTag(tagInput);
    } else if (e.key === 'Escape') {
      setShowTagInput(false);
      setTagInput('');
    }
  };

  /**
   * Handle keyboard shortcuts
   */
  const handleKeyDown = (e: React.KeyboardEvent) => {
    // Cmd/Ctrl + Enter to save
    if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
      e.preventDefault();
      handleSave();
    }
    // Cmd/Ctrl + Shift + M to attach media
    else if ((e.metaKey || e.ctrlKey) && e.shiftKey && e.key === 'm' && onAttachMedia) {
      e.preventDefault();
      onAttachMedia();
    }
  };

  const canSave = content.trim().length > 0 && !isSaving;

  return (
    <div className="flex flex-col h-full bg-background" onKeyDown={handleKeyDown}>
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b">
        <h2 className="text-lg font-semibold">
          {initialContent ? 'Edit Entry' : 'New Entry'}
        </h2>
        <Button
          variant="ghost"
          size="sm"
          onClick={onCancel}
          disabled={isSaving}
          aria-label="Close editor"
        >
          <XMarkIcon className="h-5 w-5" />
        </Button>
      </div>

      {/* Content Area */}
      <div className="flex-1 overflow-y-auto p-4">
        <textarea
          ref={textareaRef}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="What's on your mind today?"
          className="w-full resize-none border-0 bg-transparent text-base outline-none focus:ring-0 min-h-[200px]"
          disabled={isSaving}
        />
      </div>

      {/* Tags Section */}
      <div className="border-t p-4">
        <div className="flex flex-wrap gap-2 mb-3">
          {tags.map((tag) => (
            <Badge
              key={tag}
              variant="secondary"
              className="cursor-pointer hover:bg-muted"
              onClick={() => handleRemoveTag(tag)}
            >
              #{tag}
              <XMarkIcon className="ml-1 h-3 w-3" />
            </Badge>
          ))}

          {showTagInput ? (
            <Input
              value={tagInput}
              onChange={(e) => setTagInput(e.target.value)}
              onKeyDown={handleTagInputKeyDown}
              onBlur={() => {
                if (tagInput.trim()) {
                  handleAddTag(tagInput);
                }
                setShowTagInput(false);
              }}
              placeholder="Enter tag..."
              className="w-32 h-7 text-sm"
              autoFocus
            />
          ) : (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setShowTagInput(true)}
              disabled={isSaving}
              className="h-7 px-2 text-sm"
            >
              <TagIcon className="h-4 w-4 mr-1" />
              Add tag
            </Button>
          )}
        </div>

        <div className="text-xs text-muted-foreground">
          Tip: Press Cmd/Ctrl + Enter to save
        </div>
      </div>

      {/* Action Bar */}
      <div className="flex items-center justify-between p-4 border-t bg-muted/50">
        <div className="flex gap-2">
          {onAttachMedia && (
            <Button
              variant="ghost"
              size="sm"
              onClick={onAttachMedia}
              disabled={isSaving}
              leftIcon={<PhotoIcon className="h-5 w-5" />}
            >
              Add Photos
            </Button>
          )}
        </div>

        <div className="flex gap-2">
          <Button
            variant="ghost"
            onClick={onCancel}
            disabled={isSaving}
          >
            Cancel
          </Button>
          <Button
            onClick={handleSave}
            disabled={!canSave}
            loading={isSaving}
          >
            Save Entry
          </Button>
        </div>
      </div>
    </div>
  );
}
