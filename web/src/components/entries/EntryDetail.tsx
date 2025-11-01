/**
 * Entry Detail Modal
 *
 * Full view of a single diary entry with media gallery.
 */

import React, { useState, useEffect } from 'react';
import { format } from 'date-fns';
import {
  XMarkIcon,
  PencilIcon,
  TrashIcon,
  ShareIcon,
  ChevronLeftIcon,
  ChevronRightIcon
} from '@heroicons/react/24/outline';
import { DecryptedEntry } from '@/types';
import { Modal } from '@/components/common/Modal';
import { Button } from '@/components/common/Button';
import { Badge } from '@/components/common/Badge';
import { mediaService } from '@/services/media';

// ============================================================================
// Types
// ============================================================================

export interface EntryDetailProps {
  entry: DecryptedEntry;
  isOpen: boolean;
  onClose: () => void;
  onEdit?: (entry: DecryptedEntry) => void;
  onDelete?: (entryId: string) => void;
  onShare?: (entry: DecryptedEntry) => void;
  onNavigate?: (direction: 'prev' | 'next') => void;
}

// ============================================================================
// Component
// ============================================================================

export function EntryDetail({
  entry,
  isOpen,
  onClose,
  onEdit,
  onDelete,
  onShare,
  onNavigate
}: EntryDetailProps) {
  const [mediaUrls, setMediaUrls] = useState<string[]>([]);
  const [selectedMediaIndex, setSelectedMediaIndex] = useState(0);
  const [isLoadingMedia, setIsLoadingMedia] = useState(false);

  /**
   * Load and decrypt media
   */
  useEffect(() => {
    const loadMedia = async () => {
      if (!entry.mediaIds || entry.mediaIds.length === 0) {
        setMediaUrls([]);
        return;
      }

      setIsLoadingMedia(true);
      try {
        const urls: string[] = [];

        for (const mediaId of entry.mediaIds) {
          const blob = await mediaService.getMedia(mediaId);
          if (blob) {
            const url = URL.createObjectURL(blob);
            urls.push(url);
          }
        }

        setMediaUrls(urls);
      } catch (error) {
        console.error('Failed to load media:', error);
      } finally {
        setIsLoadingMedia(false);
      }
    };

    if (isOpen) {
      loadMedia();
    }

    // Cleanup URLs on unmount
    return () => {
      mediaUrls.forEach((url) => URL.revokeObjectURL(url));
    };
  }, [entry.mediaIds, isOpen]);

  /**
   * Handle keyboard navigation
   */
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!isOpen) return;

      if (e.key === 'Escape') {
        onClose();
      } else if (e.key === 'ArrowLeft' && onNavigate) {
        onNavigate('prev');
      } else if (e.key === 'ArrowRight' && onNavigate) {
        onNavigate('next');
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose, onNavigate]);

  /**
   * Handle actions
   */
  const handleEdit = () => {
    onClose();
    if (onEdit) {
      onEdit(entry);
    }
  };

  const handleDelete = () => {
    if (onDelete && confirm('Are you sure you want to delete this entry?')) {
      onDelete(entry.id);
      onClose();
    }
  };

  const handleShare = () => {
    onClose();
    if (onShare) {
      onShare(entry);
    }
  };

  const hasMedia = mediaUrls.length > 0;

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="lg">
      <div className="flex flex-col h-full max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b">
          <div className="flex-1">
            <time className="text-sm text-muted-foreground">
              {format(new Date(entry.createdAt), 'EEEE, MMMM d, yyyy \'at\' h:mm a')}
            </time>
            {entry.createdAt !== entry.updatedAt && (
              <div className="text-xs text-muted-foreground italic mt-1">
                Last edited {format(new Date(entry.updatedAt), 'MMM d, yyyy \'at\' h:mm a')}
              </div>
            )}
          </div>

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
        <div className="flex-1 overflow-y-auto p-6">
          {/* Media Gallery */}
          {hasMedia && (
            <div className="mb-6">
              <div className="relative rounded-lg overflow-hidden bg-muted">
                {isLoadingMedia ? (
                  <div className="aspect-video flex items-center justify-center">
                    <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" />
                  </div>
                ) : (
                  <>
                    <img
                      src={mediaUrls[selectedMediaIndex]}
                      alt={`Media ${selectedMediaIndex + 1}`}
                      className="w-full h-auto max-h-96 object-contain"
                    />

                    {/* Media Navigation */}
                    {mediaUrls.length > 1 && (
                      <>
                        <button
                          onClick={() =>
                            setSelectedMediaIndex((prev) =>
                              prev === 0 ? mediaUrls.length - 1 : prev - 1
                            )
                          }
                          className="absolute left-2 top-1/2 -translate-y-1/2 p-2 rounded-full bg-black/50 text-white hover:bg-black/70"
                          aria-label="Previous image"
                        >
                          <ChevronLeftIcon className="h-6 w-6" />
                        </button>
                        <button
                          onClick={() =>
                            setSelectedMediaIndex((prev) =>
                              prev === mediaUrls.length - 1 ? 0 : prev + 1
                            )
                          }
                          className="absolute right-2 top-1/2 -translate-y-1/2 p-2 rounded-full bg-black/50 text-white hover:bg-black/70"
                          aria-label="Next image"
                        >
                          <ChevronRightIcon className="h-6 w-6" />
                        </button>

                        {/* Dots indicator */}
                        <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-2">
                          {mediaUrls.map((_, index) => (
                            <button
                              key={index}
                              onClick={() => setSelectedMediaIndex(index)}
                              className={`w-2 h-2 rounded-full transition-colors ${
                                index === selectedMediaIndex
                                  ? 'bg-white'
                                  : 'bg-white/50'
                              }`}
                              aria-label={`Go to image ${index + 1}`}
                            />
                          ))}
                        </div>
                      </>
                    )}
                  </>
                )}
              </div>
            </div>
          )}

          {/* Entry Content */}
          <div className="prose prose-sm max-w-none">
            <p className="whitespace-pre-wrap">{entry.content}</p>
          </div>

          {/* Tags */}
          {entry.tags.length > 0 && (
            <div className="flex flex-wrap gap-2 mt-6">
              {entry.tags.map((tag) => (
                <Badge key={tag} variant="secondary">
                  #{tag}
                </Badge>
              ))}
            </div>
          )}

          {/* Source */}
          {entry.source !== 'diary' && (
            <div className="mt-6 pt-4 border-t">
              <div className="text-sm text-muted-foreground">
                Imported from{' '}
                <Badge variant="outline" className="capitalize">
                  {entry.source}
                </Badge>
              </div>
            </div>
          )}
        </div>

        {/* Actions */}
        <div className="flex items-center justify-between p-6 border-t">
          <div className="flex gap-2">
            {onNavigate && (
              <>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => onNavigate('prev')}
                  leftIcon={<ChevronLeftIcon className="h-4 w-4" />}
                >
                  Previous
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => onNavigate('next')}
                  rightIcon={<ChevronRightIcon className="h-4 w-4" />}
                >
                  Next
                </Button>
              </>
            )}
          </div>

          <div className="flex gap-2">
            {onEdit && (
              <Button
                variant="outline"
                onClick={handleEdit}
                leftIcon={<PencilIcon className="h-4 w-4" />}
              >
                Edit
              </Button>
            )}
            {onShare && (
              <Button
                variant="outline"
                onClick={handleShare}
                leftIcon={<ShareIcon className="h-4 w-4" />}
              >
                Share
              </Button>
            )}
            {onDelete && (
              <Button
                variant="ghost"
                onClick={handleDelete}
                className="text-destructive"
                leftIcon={<TrashIcon className="h-4 w-4" />}
              >
                Delete
              </Button>
            )}
          </div>
        </div>
      </div>
    </Modal>
  );
}
