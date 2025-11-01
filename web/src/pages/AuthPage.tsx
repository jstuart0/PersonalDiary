/**
 * Authentication Page
 *
 * Handles login and signup flows with recovery codes.
 */

import React, { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { LoginForm } from '@/components/auth/LoginForm';
import { SignupForm } from '@/components/auth/SignupForm';
import { RecoveryCodes } from '@/components/auth/RecoveryCodes';
import { useAuth } from '@/context';

// ============================================================================
// Component
// ============================================================================

type AuthMode = 'login' | 'signup' | 'recovery-codes';

export function AuthPage() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [mode, setMode] = useState<AuthMode>('login');
  const [recoveryCodes, setRecoveryCodes] = useState<string[]>([]);

  // Redirect if already authenticated
  if (isAuthenticated && mode !== 'recovery-codes') {
    return <Navigate to="/timeline" replace />;
  }

  /**
   * Handle successful signup
   */
  const handleSignupSuccess = (codes?: string[]) => {
    if (codes && codes.length > 0) {
      setRecoveryCodes(codes);
      setMode('recovery-codes');
    } else {
      navigate('/timeline');
    }
  };

  /**
   * Handle recovery codes confirmation
   */
  const handleRecoveryCodesComplete = () => {
    navigate('/timeline');
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary/5 via-background to-secondary/5 p-4">
      <div className="w-full max-w-md">
        {/* Logo/Brand */}
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold bg-gradient-to-r from-primary to-primary/60 bg-clip-text text-transparent">
            Personal Diary
          </h1>
          <p className="mt-2 text-muted-foreground">
            Your private, encrypted diary
          </p>
        </div>

        {/* Auth Forms */}
        <div className="bg-card rounded-lg shadow-lg border p-6">
          <AnimatePresence mode="wait">
            {mode === 'login' && (
              <motion.div
                key="login"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                transition={{ duration: 0.2 }}
              >
                <LoginForm
                  onSuccess={() => navigate('/timeline')}
                  onSwitchToSignup={() => setMode('signup')}
                />
              </motion.div>
            )}

            {mode === 'signup' && (
              <motion.div
                key="signup"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                transition={{ duration: 0.2 }}
              >
                <SignupForm
                  onSuccess={handleSignupSuccess}
                  onSwitchToLogin={() => setMode('login')}
                />
              </motion.div>
            )}

            {mode === 'recovery-codes' && (
              <motion.div
                key="recovery-codes"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                transition={{ duration: 0.2 }}
              >
                <RecoveryCodes
                  codes={recoveryCodes}
                  onComplete={handleRecoveryCodesComplete}
                />
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* Footer */}
        <div className="mt-8 text-center text-sm text-muted-foreground">
          <p>
            <a href="/privacy" className="hover:text-foreground underline" target="_blank" rel="noopener noreferrer">
              Privacy Policy
            </a>
            {' • '}
            <a href="/terms" className="hover:text-foreground underline" target="_blank" rel="noopener noreferrer">
              Terms of Service
            </a>
          </p>
        </div>
      </div>
    </div>
  );
}
