/**
 * Settings Context
 *
 * Manages application settings and preferences.
 * Persists settings to IndexedDB.
 */

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { AppSettings } from '@/types';
import { db } from '@/services/storage';
import { useAuth } from './AuthContext';

// ============================================================================
// Types
// ============================================================================

interface SettingsContextType {
  settings: AppSettings;
  isLoading: boolean;

  updateSettings: (partial: Partial<AppSettings>) => Promise<void>;
  resetSettings: () => Promise<void>;
}

// ============================================================================
// Default Settings
// ============================================================================

const DEFAULT_SETTINGS: AppSettings = {
  theme: 'system',
  autoSync: true,
  syncInterval: 5, // minutes
  clearCacheOnLogout: false,
  integrations: {
    facebook: false,
    instagram: false
  }
};

// ============================================================================
// Context
// ============================================================================

const SettingsContext = createContext<SettingsContextType | undefined>(undefined);

// ============================================================================
// Provider Component
// ============================================================================

interface SettingsProviderProps {
  children: ReactNode;
}

export function SettingsProvider({ children }: SettingsProviderProps) {
  const { user, isAuthenticated } = useAuth();
  const [settings, setSettings] = useState<AppSettings>(DEFAULT_SETTINGS);
  const [isLoading, setIsLoading] = useState(true);

  // Load settings when user changes
  useEffect(() => {
    const loadSettings = async () => {
      if (!isAuthenticated || !user) {
        setSettings(DEFAULT_SETTINGS);
        setIsLoading(false);
        return;
      }

      try {
        await db.init();
        const storedSettings = await db.getSettings(user.id);

        if (storedSettings) {
          setSettings(storedSettings);
        } else {
          // Save default settings
          await db.saveSettings(user.id, DEFAULT_SETTINGS);
          setSettings(DEFAULT_SETTINGS);
        }
      } catch (err) {
        console.error('Error loading settings:', err);
        setSettings(DEFAULT_SETTINGS);
      } finally {
        setIsLoading(false);
      }
    };

    loadSettings();
  }, [isAuthenticated, user]);

  // Apply theme setting
  useEffect(() => {
    const applyTheme = () => {
      const root = document.documentElement;

      if (settings.theme === 'system') {
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        root.classList.toggle('dark', prefersDark);
      } else {
        root.classList.toggle('dark', settings.theme === 'dark');
      }
    };

    applyTheme();

    // Listen for system theme changes
    if (settings.theme === 'system') {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      const handler = () => applyTheme();
      mediaQuery.addEventListener('change', handler);
      return () => mediaQuery.removeEventListener('change', handler);
    }
  }, [settings.theme]);

  /**
   * Update settings
   */
  const updateSettings = async (partial: Partial<AppSettings>) => {
    if (!user) return;

    const newSettings = { ...settings, ...partial };
    setSettings(newSettings);

    try {
      await db.saveSettings(user.id, newSettings);
    } catch (err) {
      console.error('Error saving settings:', err);
    }
  };

  /**
   * Reset to default settings
   */
  const resetSettings = async () => {
    if (!user) return;

    setSettings(DEFAULT_SETTINGS);

    try {
      await db.saveSettings(user.id, DEFAULT_SETTINGS);
    } catch (err) {
      console.error('Error resetting settings:', err);
    }
  };

  const value: SettingsContextType = {
    settings,
    isLoading,
    updateSettings,
    resetSettings
  };

  return <SettingsContext.Provider value={value}>{children}</SettingsContext.Provider>;
}

// ============================================================================
// Hook
// ============================================================================

export function useSettings(): SettingsContextType {
  const context = useContext(SettingsContext);
  if (context === undefined) {
    throw new Error('useSettings must be used within a SettingsProvider');
  }
  return context;
}
