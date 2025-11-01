/**
 * Input Component
 *
 * Accessible, styled input with validation support.
 */

import React, { InputHTMLAttributes, forwardRef, useState } from 'react';
import { EyeIcon, EyeSlashIcon, ExclamationCircleIcon } from '@heroicons/react/24/outline';

// ============================================================================
// Types
// ============================================================================

interface InputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'size'> {
  label?: string;
  error?: string;
  hint?: string;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
  containerClassName?: string;
}

// ============================================================================
// Component
// ============================================================================

export const Input = forwardRef<HTMLInputElement, InputProps>(
  (
    {
      label,
      error,
      hint,
      leftIcon,
      rightIcon,
      type = 'text',
      className = '',
      containerClassName = '',
      id,
      disabled,
      ...props
    },
    ref
  ) => {
    const [showPassword, setShowPassword] = useState(false);
    const inputId = id || `input-${Math.random().toString(36).substr(2, 9)}`;

    const inputType = type === 'password' && showPassword ? 'text' : type;

    const hasError = Boolean(error);

    return (
      <div className={`w-full ${containerClassName}`}>
        {label && (
          <label
            htmlFor={inputId}
            className="block text-sm font-medium text-foreground mb-1.5"
          >
            {label}
          </label>
        )}

        <div className="relative">
          {leftIcon && (
            <div className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none">
              {leftIcon}
            </div>
          )}

          <input
            ref={ref}
            id={inputId}
            type={inputType}
            disabled={disabled}
            className={[
              'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm',
              'ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium',
              'placeholder:text-muted-foreground',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
              'disabled:cursor-not-allowed disabled:opacity-50',
              leftIcon && 'pl-10',
              (rightIcon || type === 'password' || hasError) && 'pr-10',
              hasError && 'border-destructive focus-visible:ring-destructive',
              className
            ]
              .filter(Boolean)
              .join(' ')}
            {...props}
          />

          {type === 'password' && (
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
              tabIndex={-1}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
            >
              {showPassword ? (
                <EyeSlashIcon className="h-5 w-5" />
              ) : (
                <EyeIcon className="h-5 w-5" />
              )}
            </button>
          )}

          {hasError && !rightIcon && type !== 'password' && (
            <div className="absolute right-3 top-1/2 -translate-y-1/2 text-destructive pointer-events-none">
              <ExclamationCircleIcon className="h-5 w-5" />
            </div>
          )}

          {rightIcon && type !== 'password' && !hasError && (
            <div className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none">
              {rightIcon}
            </div>
          )}
        </div>

        {hint && !error && <p className="mt-1.5 text-sm text-muted-foreground">{hint}</p>}

        {error && (
          <p className="mt-1.5 text-sm text-destructive" role="alert">
            {error}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';

// ============================================================================
// Textarea Component
// ============================================================================

interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
  hint?: string;
  containerClassName?: string;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ label, error, hint, className = '', containerClassName = '', id, ...props }, ref) => {
    const textareaId = id || `textarea-${Math.random().toString(36).substr(2, 9)}`;
    const hasError = Boolean(error);

    return (
      <div className={`w-full ${containerClassName}`}>
        {label && (
          <label
            htmlFor={textareaId}
            className="block text-sm font-medium text-foreground mb-1.5"
          >
            {label}
          </label>
        )}

        <textarea
          ref={ref}
          id={textareaId}
          className={[
            'flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm',
            'ring-offset-background',
            'placeholder:text-muted-foreground',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
            'disabled:cursor-not-allowed disabled:opacity-50',
            hasError && 'border-destructive focus-visible:ring-destructive',
            className
          ]
            .filter(Boolean)
            .join(' ')}
          {...props}
        />

        {hint && !error && <p className="mt-1.5 text-sm text-muted-foreground">{hint}</p>}

        {error && (
          <p className="mt-1.5 text-sm text-destructive" role="alert">
            {error}
          </p>
        )}
      </div>
    );
  }
);

Textarea.displayName = 'Textarea';
