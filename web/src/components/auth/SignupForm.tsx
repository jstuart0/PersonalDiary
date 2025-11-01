/**
 * Signup Form Component
 *
 * User registration with encryption tier selection.
 */

import React, { useState } from 'react';
import { EnvelopeIcon, ShieldCheckIcon, LockClosedIcon } from '@heroicons/react/24/outline';
import { Button } from '@/components/common/Button';
import { Input } from '@/components/common/Input';
import { Alert } from '@/components/common/Alert';
import { Card } from '@/components/common/Card';
import { EncryptionTier } from '@/types';
import { useAuth } from '@/context';
import { validatePassword } from '@/services/encryption';

// ============================================================================
// Component
// ============================================================================

interface SignupFormProps {
  onSuccess?: (recoveryCodes?: string[]) => void;
  onSwitchToLogin?: () => void;
}

export function SignupForm({ onSuccess, onSwitchToLogin }: SignupFormProps) {
  const { signup, error, clearError, isLoading } = useAuth();

  const [step, setStep] = useState<'tier' | 'details'>('tier');
  const [tier, setTier] = useState<EncryptionTier | null>(null);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [acceptTerms, setAcceptTerms] = useState(false);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  /**
   * Validate tier selection
   */
  const validateTierSelection = (): boolean => {
    if (!tier) {
      setValidationErrors({ tier: 'Please select an encryption tier' });
      return false;
    }
    return true;
  };

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
    } else {
      const passwordValidation = validatePassword(password);
      if (!passwordValidation.valid) {
        errors.password = passwordValidation.errors.join(', ');
      }
    }

    if (password !== confirmPassword) {
      errors.confirmPassword = 'Passwords do not match';
    }

    if (!acceptTerms) {
      errors.terms = 'You must accept the terms and conditions';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  /**
   * Handle tier selection
   */
  const handleTierSelected = (selectedTier: EncryptionTier) => {
    setTier(selectedTier);
    setValidationErrors({});
    setStep('details');
  };

  /**
   * Handle form submission
   */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    clearError();

    if (!tier || !validate()) return;

    const result = await signup({
      email,
      password,
      encryptionTier: tier
    });

    if (result.success && onSuccess) {
      onSuccess(result.recoveryCodes);
    }
  };

  /**
   * Render tier selection
   */
  if (step === 'tier') {
    return (
      <div className="space-y-6">
        <div className="text-center">
          <h2 className="text-2xl font-bold">Choose Your Encryption</h2>
          <p className="mt-2 text-muted-foreground">
            Select how your diary entries will be encrypted. This cannot be changed later.
          </p>
        </div>

        {validationErrors.tier && (
          <Alert variant="error">{validationErrors.tier}</Alert>
        )}

        <div className="grid gap-4">
          {/* E2E Tier */}
          <Card
            padding="none"
            hover
            className="cursor-pointer transition-all border-2 hover:border-primary"
            onClick={() => handleTierSelected(EncryptionTier.E2E)}
          >
            <div className="p-6">
              <div className="flex items-start gap-4">
                <div className="flex-shrink-0">
                  <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary/10">
                    <ShieldCheckIcon className="h-6 w-6 text-primary" />
                  </div>
                </div>
                <div className="flex-1">
                  <h3 className="text-lg font-semibold">End-to-End Encrypted (E2E)</h3>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Maximum privacy - only you can read your entries
                  </p>
                  <ul className="mt-3 space-y-1 text-sm text-muted-foreground">
                    <li>✓ Keys never leave your device</li>
                    <li>✓ Complete privacy - server cannot decrypt</li>
                    <li>✓ Recovery codes for account recovery</li>
                    <li>⚠ Lost password = lost data permanently</li>
                  </ul>
                  <div className="mt-4">
                    <span className="inline-flex items-center gap-1 text-sm font-medium text-primary">
                      Recommended for maximum privacy
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </Card>

          {/* UCE Tier */}
          <Card
            padding="none"
            hover
            className="cursor-pointer transition-all border-2 hover:border-primary"
            onClick={() => handleTierSelected(EncryptionTier.UCE)}
          >
            <div className="p-6">
              <div className="flex items-start gap-4">
                <div className="flex-shrink-0">
                  <div className="flex h-12 w-12 items-center justify-center rounded-full bg-secondary/10">
                    <LockClosedIcon className="h-6 w-6 text-secondary-foreground" />
                  </div>
                </div>
                <div className="flex-1">
                  <h3 className="text-lg font-semibold">User-Controlled Encryption (UCE)</h3>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Balanced approach with password recovery
                  </p>
                  <ul className="mt-3 space-y-1 text-sm text-muted-foreground">
                    <li>✓ Encrypted with your password</li>
                    <li>✓ Server-side search capabilities</li>
                    <li>✓ Password recovery available</li>
                    <li>⚠ Server can decrypt with your password</li>
                  </ul>
                  <div className="mt-4">
                    <span className="inline-flex items-center gap-1 text-sm font-medium text-muted-foreground">
                      Better for convenience
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </Card>
        </div>

        {onSwitchToLogin && (
          <p className="text-center text-sm text-muted-foreground">
            Already have an account?{' '}
            <button
              type="button"
              onClick={onSwitchToLogin}
              className="text-primary hover:underline font-medium"
            >
              Log in
            </button>
          </p>
        )}
      </div>
    );
  }

  /**
   * Render signup form
   */
  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {/* Back Button */}
      <button
        type="button"
        onClick={() => setStep('tier')}
        className="text-sm text-muted-foreground hover:text-foreground"
        disabled={isLoading}
      >
        ← Back to encryption selection
      </button>

      {/* Selected Tier Badge */}
      <Alert variant="info">
        <strong>Selected:</strong> {tier === EncryptionTier.E2E ? 'End-to-End Encryption (E2E)' : 'User-Controlled Encryption (UCE)'}
      </Alert>

      {/* Error Alert */}
      {error && (
        <Alert variant="error" title="Signup Failed">
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
        hint="At least 12 characters with uppercase, lowercase, number, and special character"
        placeholder="Create a strong password"
        autoComplete="new-password"
        required
        disabled={isLoading}
      />

      {/* Confirm Password Input */}
      <Input
        label="Confirm Password"
        type="password"
        value={confirmPassword}
        onChange={(e) => {
          setConfirmPassword(e.target.value);
          if (validationErrors.confirmPassword) {
            setValidationErrors((prev) => ({ ...prev, confirmPassword: '' }));
          }
        }}
        error={validationErrors.confirmPassword}
        placeholder="Re-enter your password"
        autoComplete="new-password"
        required
        disabled={isLoading}
      />

      {/* Terms Checkbox */}
      <div>
        <label className="flex items-start gap-2 text-sm">
          <input
            type="checkbox"
            checked={acceptTerms}
            onChange={(e) => {
              setAcceptTerms(e.target.checked);
              if (validationErrors.terms) {
                setValidationErrors((prev) => ({ ...prev, terms: '' }));
              }
            }}
            disabled={isLoading}
            className="mt-0.5 rounded border-input focus:ring-2 focus:ring-ring focus:ring-offset-2"
          />
          <span>
            I accept the{' '}
            <a href="/terms" className="text-primary hover:underline" target="_blank" rel="noopener noreferrer">
              Terms of Service
            </a>{' '}
            and{' '}
            <a href="/privacy" className="text-primary hover:underline" target="_blank" rel="noopener noreferrer">
              Privacy Policy
            </a>
          </span>
        </label>
        {validationErrors.terms && (
          <p className="mt-1 text-sm text-destructive">{validationErrors.terms}</p>
        )}
      </div>

      {/* Submit Button */}
      <Button type="submit" fullWidth isLoading={isLoading}>
        {isLoading ? 'Creating Account...' : 'Create Account'}
      </Button>

      {/* Switch to Login */}
      {onSwitchToLogin && (
        <p className="text-center text-sm text-muted-foreground">
          Already have an account?{' '}
          <button
            type="button"
            onClick={onSwitchToLogin}
            className="text-primary hover:underline font-medium"
            disabled={isLoading}
          >
            Log in
          </button>
        </p>
      )}
    </form>
  );
}
