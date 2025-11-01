/**
 * Login Form Component
 *
 * User login with email and password.
 */

import React, { useState } from 'react';
import { EnvelopeIcon } from '@heroicons/react/24/outline';
import { Button } from '@/components/common/Button';
import { Input } from '@/components/common/Input';
import { Alert } from '@/components/common/Alert';
import { useAuth } from '@/context';

// ============================================================================
// Component
// ============================================================================

interface LoginFormProps {
  onSuccess?: () => void;
  onSwitchToSignup?: () => void;
}

export function LoginForm({ onSuccess, onSwitchToSignup }: LoginFormProps) {
  const { login, error, clearError, isLoading } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  /**
   * Validate form
   */
  const validate = (): boolean => {
    const errors: Record<string, string> = {};

    if (!email) {
      errors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      errors.email = 'Please enter a valid email';
    }

    if (!password) {
      errors.password = 'Password is required';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  /**
   * Handle form submission
   */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    clearError();

    if (!validate()) return;

    const success = await login({
      email,
      password,
      rememberMe
    });

    if (success && onSuccess) {
      onSuccess();
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {/* Error Alert */}
      {error && (
        <Alert variant="error" title="Login Failed">
          {error}
        </Alert>
      )}

      {/* Email Input */}
      <Input
        label="Email"
        type="email"
        value={email}
        onChange={(e) => {
          setEmail(e.target.value);
          if (validationErrors.email) {
            setValidationErrors((prev) => ({ ...prev, email: '' }));
          }
        }}
        error={validationErrors.email}
        placeholder="you@example.com"
        leftIcon={<EnvelopeIcon className="h-5 w-5" />}
        autoComplete="email"
        required
        disabled={isLoading}
      />

      {/* Password Input */}
      <Input
        label="Password"
        type="password"
        value={password}
        onChange={(e) => {
          setPassword(e.target.value);
          if (validationErrors.password) {
            setValidationErrors((prev) => ({ ...prev, password: '' }));
          }
        }}
        error={validationErrors.password}
        placeholder="Enter your password"
        autoComplete="current-password"
        required
        disabled={isLoading}
      />

      {/* Remember Me Checkbox */}
      <div className="flex items-center justify-between">
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={rememberMe}
            onChange={(e) => setRememberMe(e.target.checked)}
            disabled={isLoading}
            className="rounded border-input focus:ring-2 focus:ring-ring focus:ring-offset-2"
          />
          <span>Remember me</span>
        </label>

        <button
          type="button"
          className="text-sm text-primary hover:underline"
          disabled={isLoading}
        >
          Forgot password?
        </button>
      </div>

      {/* Submit Button */}
      <Button type="submit" fullWidth isLoading={isLoading}>
        {isLoading ? 'Logging in...' : 'Log In'}
      </Button>

      {/* Switch to Signup */}
      {onSwitchToSignup && (
        <p className="text-center text-sm text-muted-foreground">
          Don't have an account?{' '}
          <button
            type="button"
            onClick={onSwitchToSignup}
            className="text-primary hover:underline font-medium"
            disabled={isLoading}
          >
            Sign up
          </button>
        </p>
      )}
    </form>
  );
}
