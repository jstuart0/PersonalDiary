/**
 * Share to Facebook Modal Component
 *
 * Modal for sharing diary entries to Facebook.
 */

import React, { useState } from 'react';
import { XMarkIcon } from '@heroicons/react/24/outline';
import { Modal } from '@/components/common/Modal';
import { Button } from '@/components/common/Button';
import { Alert } from '@/components/common/Alert';
import { DecryptedEntry } from '@/types';
import { facebookService } from '@/services/facebook';

// ============================================================================
// Types
// ============================================================================

export interface ShareToFacebookModalProps {
  entry: DecryptedEntry;
  isOpen: boolean;
  onClose: () => void;
}

// ============================================================================
// Component
// ============================================================================

export function ShareToFacebookModal({ entry, isOpen, onClose }: ShareToFacebookModalProps) {
  const [isSharing, setIsSharing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  /**
   * Handle share
   */
  const handleShare = async () => {
    setIsSharing(true);
    setError(null);

    try {
      // Authenticate with Facebook
      const { accessToken } = await facebookService.authenticateWithPopup();

      // Post to Facebook
      await facebookService.postToFacebook(entry.content, accessToken);

      setSuccess(true);

      // Close after 2 seconds
      setTimeout(() => {
        onClose();
        setSuccess(false);
      }, 2000);
    } catch (err) {
      console.error('Failed to share to Facebook:', err);
      setError(err instanceof Error ? err.message : 'Failed to share to Facebook');
    } finally {
      setIsSharing(false);
    }
  };

  /**
   * Handle close
   */
  const handleClose = () => {
    if (!isSharing) {
      setError(null);
      setSuccess(false);
      onClose();
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} size="md">
      <div className="flex flex-col h-full">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-lg font-semibold">Share to Facebook</h2>
          <Button
            variant="ghost"
            size="sm"
            onClick={handleClose}
            disabled={isSharing}
            aria-label="Close"
          >
            <XMarkIcon className="h-5 w-5" />
          </Button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-4">
          {/* Preview */}
          <div>
            <label className="block text-sm font-medium mb-2">Preview</label>
            <div className="p-4 border rounded-lg bg-muted/50">
              <p className="text-sm whitespace-pre-wrap line-clamp-6">{entry.content}</p>
            </div>
          </div>

          {/* Info */}
          <Alert variant="info">
            <p className="text-sm">
              This entry will be posted to your Facebook timeline as "Only Me" (private).
            </p>
          </Alert>

          {/* Error */}
          {error && (
            <Alert variant="destructive">
              <p className="text-sm">{error}</p>
            </Alert>
          )}

          {/* Success */}
          {success && (
            <Alert variant="success">
              <p className="text-sm">Successfully shared to Facebook!</p>
            </Alert>
          )}
        </div>

        {/* Actions */}
        <div className="flex items-center justify-end gap-2 p-6 border-t">
          <Button variant="outline" onClick={handleClose} disabled={isSharing}>
            Cancel
          </Button>
          <Button onClick={handleShare} loading={isSharing} disabled={success}>
            {success ? 'Shared!' : 'Share to Facebook'}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
