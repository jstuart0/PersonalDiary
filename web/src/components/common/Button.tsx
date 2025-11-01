/**
 * Button Component
 *
 * Accessible, styled button with multiple variants.
 */

import React, { ButtonHTMLAttributes, forwardRef } from 'react';
import { motion, HTMLMotionProps } from 'framer-motion';

// ============================================================================
// Types
// ============================================================================

type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'destructive';
type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'className'> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  isLoading?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
  className?: string;
  fullWidth?: boolean;
}

// ============================================================================
// Styles
// ============================================================================

const baseStyles =
  'inline-flex items-center justify-center font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:opacity-50 disabled:pointer-events-none';

const variantStyles: Record<ButtonVariant, string> = {
  primary: 'bg-primary text-primary-foreground hover:bg-primary/90',
  secondary: 'bg-secondary text-secondary-foreground hover:bg-secondary/80',
  outline: 'border border-input bg-background hover:bg-accent hover:text-accent-foreground',
  ghost: 'hover:bg-accent hover:text-accent-foreground',
  destructive: 'bg-destructive text-destructive-foreground hover:bg-destructive/90'
};

const sizeStyles: Record<ButtonSize, string> = {
  sm: 'h-9 px-3 rounded-md text-sm',
  md: 'h-10 px-4 py-2 rounded-md',
  lg: 'h-11 px-8 rounded-md text-lg'
};

// ============================================================================
// Component
// ============================================================================

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      variant = 'primary',
      size = 'md',
      isLoading = false,
      leftIcon,
      rightIcon,
      children,
      disabled,
      className = '',
      fullWidth = false,
      type = 'button',
      ...props
    },
    ref
  ) => {
    const classes = [
      baseStyles,
      variantStyles[variant],
      sizeStyles[size],
      fullWidth && 'w-full',
      className
    ]
      .filter(Boolean)
      .join(' ');

    return (
      <motion.button
        ref={ref}
        type={type}
        className={classes}
        disabled={disabled || isLoading}
        whileTap={{ scale: 0.98 }}
        {...props}
      >
        {isLoading && (
          <svg
            className="mr-2 h-4 w-4 animate-spin"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
        )}
        {!isLoading && leftIcon && <span className="mr-2">{leftIcon}</span>}
        {children}
        {rightIcon && <span className="ml-2">{rightIcon}</span>}
      </motion.button>
    );
  }
);

Button.displayName = 'Button';

// ============================================================================
// Icon Button
// ============================================================================

interface IconButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'className'> {
  icon: React.ReactNode;
  'aria-label': string;
  size?: ButtonSize;
  variant?: ButtonVariant;
  className?: string;
}

export const IconButton = forwardRef<HTMLButtonElement, IconButtonProps>(
  ({ icon, size = 'md', variant = 'ghost', className = '', ...props }, ref) => {
    const sizeClass = {
      sm: 'h-8 w-8',
      md: 'h-10 w-10',
      lg: 'h-12 w-12'
    }[size];

    return (
      <Button
        ref={ref}
        variant={variant}
        className={`${sizeClass} p-0 ${className}`}
        {...props}
      >
        {icon}
      </Button>
    );
  }
);

IconButton.displayName = 'IconButton';
