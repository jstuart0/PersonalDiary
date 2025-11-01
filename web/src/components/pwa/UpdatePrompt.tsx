/**
 * PWA Update Prompt Component
 *
 * Notifies users when a new version is available.
 */

import React, { useState, useEffect } from 'react';
import { ArrowPathIcon } from '@heroicons/react/24/outline';
import { Button } from '@/components/common/Button';
import { Alert } from '@/components/common/Alert';

// ============================================================================
// Component
// ============================================================================

export function UpdatePrompt() {
  const [showPrompt, setShowPrompt] = useState(false);
  const [registration, setRegistration] = useState<ServiceWorkerRegistration | null>(null);

  useEffect(() => {
    // Listen for service worker updates
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.ready.then((reg) => {
        reg.addEventListener('updatefound', () => {
          const newWorker = reg.installing;

          if (newWorker) {
            newWorker.addEventListener('statechange', () => {
              if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                // New service worker is installed and waiting
                setRegistration(reg);
                setShowPrompt(true);
              }
            });
          }
        });
      });

      // Listen for messages from service worker
      navigator.serviceWorker.addEventListener('message', (event) => {
        if (event.data && event.data.type === 'UPDATE_AVAILABLE') {
          setShowPrompt(true);
        }
      });
    }
  }, []);

  /**
   * Handle update
   */
  const handleUpdate = () => {
    if (registration?.waiting) {
      // Tell the service worker to skip waiting
      registration.waiting.postMessage({ type: 'SKIP_WAITING' });

      // Reload the page when the new service worker is activated
      navigator.serviceWorker.addEventListener('controllerchange', () => {
        window.location.reload();
      });
    } else {
      // Fallback: just reload the page
      window.location.reload();
    }
  };

  if (!showPrompt) {
    return null;
  }

  return (
    <div className="fixed top-4 left-4 right-4 md:left-1/2 md:-translate-x-1/2 md:w-96 z-50">
      <Alert variant="info">
        <div className="flex items-center justify-between">
          <div className="flex-1">
            <h4 className="text-sm font-semibold mb-1">Update Available</h4>
            <p className="text-xs">
              A new version of the app is available
            </p>
          </div>
          <Button size="sm" onClick={handleUpdate} leftIcon={<ArrowPathIcon className="h-4 w-4" />}>
            Update
          </Button>
        </div>
      </Alert>
    </div>
  );
}
