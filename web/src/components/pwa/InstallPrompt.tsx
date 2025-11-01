/**
 * PWA Install Prompt Component
 *
 * Prompts users to install the PWA on supported browsers.
 */

import React, { useState, useEffect } from 'react';
import { XMarkIcon, ArrowDownTrayIcon } from '@heroicons/react/24/outline';
import { Button } from '@/components/common/Button';
import { Card } from '@/components/common/Card';

// ============================================================================
// Types
// ============================================================================

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

// ============================================================================
// Component
// ============================================================================

export function InstallPrompt() {
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [showPrompt, setShowPrompt] = useState(false);
  const [isInstalled, setIsInstalled] = useState(false);

  useEffect(() => {
    // Check if already installed
    if (window.matchMedia('(display-mode: standalone)').matches) {
      setIsInstalled(true);
      return;
    }

    // Check if user has dismissed the prompt before
    const dismissed = localStorage.getItem('pwa-install-dismissed');
    if (dismissed) {
      const dismissedDate = new Date(dismissed);
      const daysSinceDismissed = (Date.now() - dismissedDate.getTime()) / (1000 * 60 * 60 * 24);

      // Show prompt again after 7 days
      if (daysSinceDismissed < 7) {
        return;
      }
    }

    // Listen for beforeinstallprompt event
    const handleBeforeInstallPrompt = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e as BeforeInstallPromptEvent);
      setShowPrompt(true);
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);

    // Listen for app installed
    window.addEventListener('appinstalled', () => {
      setIsInstalled(true);
      setShowPrompt(false);
    });

    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    };
  }, []);

  /**
   * Handle install click
   */
  const handleInstall = async () => {
    if (!deferredPrompt) return;

    // Show the install prompt
    await deferredPrompt.prompt();

    // Wait for the user's response
    const { outcome } = await deferredPrompt.userChoice;

    if (outcome === 'accepted') {
      setShowPrompt(false);
    }

    // Clear the deferred prompt
    setDeferredPrompt(null);
  };

  /**
   * Handle dismiss
   */
  const handleDismiss = () => {
    setShowPrompt(false);
    localStorage.setItem('pwa-install-dismissed', new Date().toISOString());
  };

  if (isInstalled || !showPrompt) {
    return null;
  }

  return (
    <div className="fixed bottom-4 left-4 right-4 md:left-auto md:w-96 z-50 animate-slide-up">
      <Card className="shadow-lg">
        <div className="flex items-start gap-4 p-4">
          <div className="flex-shrink-0">
            <div className="w-12 h-12 rounded-lg bg-primary/10 flex items-center justify-center">
              <ArrowDownTrayIcon className="h-6 w-6 text-primary" />
            </div>
          </div>

          <div className="flex-1 min-w-0">
            <h3 className="text-sm font-semibold mb-1">Install Personal Diary</h3>
            <p className="text-xs text-muted-foreground mb-3">
              Install the app for quick access and offline use
            </p>

            <div className="flex gap-2">
              <Button size="sm" onClick={handleInstall}>
                Install
              </Button>
              <Button size="sm" variant="ghost" onClick={handleDismiss}>
                Not Now
              </Button>
            </div>
          </div>

          <button
            onClick={handleDismiss}
            className="flex-shrink-0 text-muted-foreground hover:text-foreground"
            aria-label="Close"
          >
            <XMarkIcon className="h-5 w-5" />
          </button>
        </div>
      </Card>
    </div>
  );
}
