/**
 * Main App Component
 *
 * Root component with routing and global providers.
 */

import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AppProviders, useAuth } from './context';
import { AuthPage } from './pages/AuthPage';
import { TimelinePage } from './pages/TimelinePage';
import { PageLoading } from './components/common/Loading';
import { InstallPrompt, UpdatePrompt, OfflineIndicator } from './components/pwa';

// ============================================================================
// Protected Route Component
// ============================================================================

interface ProtectedRouteProps {
  children: React.ReactNode;
}

function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return <PageLoading />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/auth" replace />;
  }

  return <>{children}</>;
}

// ============================================================================
// App Routes Component
// ============================================================================

function AppRoutes() {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/auth" element={<AuthPage />} />

      {/* Protected Routes */}
      <Route
        path="/timeline"
        element={
          <ProtectedRoute>
            <TimelinePage />
          </ProtectedRoute>
        }
      />

      {/* Redirect root to timeline or auth */}
      <Route path="/" element={<Navigate to="/timeline" replace />} />

      {/* 404 - Redirect to timeline */}
      <Route path="*" element={<Navigate to="/timeline" replace />} />
    </Routes>
  );
}

// ============================================================================
// Main App Component
// ============================================================================

function App() {
  return (
    <BrowserRouter>
      <AppProviders>
        <AppRoutes />
        <InstallPrompt />
        <UpdatePrompt />
        <OfflineIndicator />
      </AppProviders>
    </BrowserRouter>
  );
}

export default App;
