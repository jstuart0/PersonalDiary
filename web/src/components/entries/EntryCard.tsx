/**
 * Entry Card Component
 *
 * Displays a single diary entry in the timeline.
 */

import React, { useState } from 'react';
import { format, formatDistanceToNow } from 'date-fns';
import {
  EllipsisHorizontalIcon,
  PencilIcon,
  TrashIcon,
  ShareIcon,
  PhotoIcon
} from '@heroicons/react/24/outline';
import { DecryptedEntry } from '@/types';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { Card } from '@/components/common/Card';

// ============================================================================
// Types
// ============================================================================

export interface EntryCardProps {
  entry: DecryptedEntry;
  onEdit?: (entry: DecryptedEntry) => void;
  onDelete?: (entryId: string) => void;
  onShare?: (entry: DecryptedEntry) => void;
  onClick?: (entry: DecryptedEntry) => void;
}

// ============================================================================
// Component
// ============================================================================

export function EntryCard({
  entry,
  onEdit,
  onDelete,
  onShare,
  onClick
}: EntryCardProps) {
  const [showMenu, setShowMenu] = useState(false);

  /**
   * Format entry date
   */
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const daysDiff = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24));

    if (daysDiff < 7) {
      return formatDistanceToNow(date, { addSuffix: true });
    }

    return format(date, 'MMMM d, yyyy');
  };

  /**
   * Get preview text
   */
  const getPreviewText = (content: string, maxLength: number = 300): string => {
    if (content.length <= maxLength) {
      return content;
    }

    return content.substring(0, maxLength).trim() + '...';
  };

  /**
   * Handle card click
   */
  const handleClick = () => {
    if (onClick) {
      onClick(entry);
    }
  };

  /**
   * Handle edit
   */
  const handleEdit = (e: React.MouseEvent) => {
    e.stopPropagation();
    setShowMenu(false);
    if (onEdit) {
      onEdit(entry);
    }
  };

  /**
   * Handle delete
   */
  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation();
    setShowMenu(false);
    if (onDelete && confirm('Are you sure you want to delete this entry?')) {
      onDelete(entry.id);
    }
  };

  /**
   * Handle share
   */
  const handleShare = (e: React.MouseEvent) => {
    e.stopPropagation();
    setShowMenu(false);
    if (onShare) {
      onShare(entry);
    }
  };

  const hasMedia = entry.mediaIds && entry.mediaIds.length > 0;

  return (
    <Card
      className="hover:shadow-md transition-shadow cursor-pointer relative"
      onClick={handleClick}
    >
      {/* Header */}
      <div className="flex items-start justify-between mb-3">
        <div className="flex-1">
          <time className="text-sm text-muted-foreground">
            {formatDate(entry.createdAt)}
          </time>
          {entry.createdAt !== entry.updatedAt && (
            <span className="ml-2 text-xs text-muted-foreground italic">
              (edited)
            </span>
          )}
        </div>

        {/* Actions Menu */}
        <div className="relative">
          <Button
            variant="ghost"
            size="sm"
            onClick={(e) => {
              e.stopPropagation();
              setShowMenu(!showMenu);
            }}
            aria-label="Entry actions"
          >
            <EllipsisHorizontalIcon className="h-5 w-5" />
          </Button>

          {showMenu && (
            <>
              <div
                className="fixed inset-0 z-10"
                onClick={(e) => {
                  e.stopPropagation();
                  setShowMenu(false);
                }}
              />
              <div className="absolute right-0 mt-1 w-48 rounded-md shadow-lg bg-popover border z-20">
                <div className="py-1">
                  {onEdit && (
                    <button
                      onClick={handleEdit}
                      className="flex items-center w-full px-4 py-2 text-sm hover:bg-accent"
                    >
                      <PencilIcon className="h-4 w-4 mr-2" />
                      Edit
                    </button>
                  )}
                  {onShare && (
                    <button
                      onClick={handleShare}
                      className="flex items-center w-full px-4 py-2 text-sm hover:bg-accent"
                    >
                      <ShareIcon className="h-4 w-4 mr-2" />
                      Share to Facebook
                    </button>
                  )}
                  {onDelete && (
                    <button
                      onClick={handleDelete}
                      className="flex items-center w-full px-4 py-2 text-sm text-destructive hover:bg-accent"
                    >
                      <TrashIcon className="h-4 w-4 mr-2" />
                      Delete
                    </button>
                  )}
                </div>
              </div>
            </>
          )}
        </div>
      </div>

      {/* Content */}
      <div className="prose prose-sm max-w-none mb-4">
        <p className="whitespace-pre-wrap">{getPreviewText(entry.content)}</p>
      </div>

      {/* Media indicator */}
      {hasMedia && (
        <div className="flex items-center gap-1 text-sm text-muted-foreground mb-3">
          <PhotoIcon className="h-4 w-4" />
          <span>{entry.mediaIds.length} {entry.mediaIds.length === 1 ? 'photo' : 'photos'}</span>
        </div>
      )}

      {/* Tags */}
      {entry.tags.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {entry.tags.map((tag) => (
            <Badge key={tag} variant="secondary" className="text-xs">
              #{tag}
            </Badge>
          ))}
        </div>
      )}

      {/* Source badge */}
      {entry.source !== 'diary' && (
        <div className="absolute top-4 right-4">
          <Badge variant="outline" className="text-xs capitalize">
            {entry.source}
          </Badge>
        </div>
      )}
    </Card>
  );
}
