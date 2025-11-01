/**
 * Offline Indicator Component
 *
 * Shows network status and offline mode indicator.
 */

import React, { useState, useEffect } from 'react';
import { WifiIcon, SignalSlashIcon } from '@heroicons/react/24/outline';
import { Alert } from '@/components/common/Alert';

// ============================================================================
// Component
// ============================================================================

export function OfflineIndicator() {
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [showBanner, setShowBanner] = useState(false);

  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      setShowBanner(true);

      // Hide banner after 3 seconds
      setTimeout(() => setShowBanner(false), 3000);
    };

    const handleOffline = () => {
      setIsOnline(false);
      setShowBanner(true);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // Show banner initially if offline
    if (!navigator.onLine) {
      setShowBanner(true);
    }

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  if (!showBanner) {
    return null;
  }

  return (
    <div className="fixed top-4 left-4 right-4 md:left-1/2 md:-translate-x-1/2 md:w-96 z-50 animate-slide-down">
      <Alert variant={isOnline ? 'success' : 'warning'}>
        <div className="flex items-center gap-2">
          {isOnline ? (
            <>
              <WifiIcon className="h-5 w-5" />
              <span className="text-sm font-medium">Back Online</span>
            </>
          ) : (
            <>
              <SignalSlashIcon className="h-5 w-5" />
              <span className="text-sm font-medium">
                You're Offline - Changes will sync when you're back online
              </span>
            </>
          )}
        </div>
      </Alert>
    </div>
  );
}
