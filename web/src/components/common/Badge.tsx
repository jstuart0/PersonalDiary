/**
 * Badge Component
 *
 * Small status indicators and labels.
 */

import React, { HTMLAttributes } from 'react';

// ============================================================================
// Types
// ============================================================================

type BadgeVariant = 'default' | 'primary' | 'secondary' | 'success' | 'warning' | 'destructive';

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant;
  className?: string;
}

// ============================================================================
// Styles
// ============================================================================

const variantStyles: Record<BadgeVariant, string> = {
  default: 'bg-muted text-muted-foreground',
  primary: 'bg-primary text-primary-foreground',
  secondary: 'bg-secondary text-secondary-foreground',
  success: 'bg-green-500 text-white',
  warning: 'bg-yellow-500 text-white',
  destructive: 'bg-destructive text-destructive-foreground'
};

// ============================================================================
// Component
// ============================================================================

export function Badge({ variant = 'default', className = '', children, ...props }: BadgeProps) {
  return (
    <span
      className={[
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold transition-colors',
        variantStyles[variant],
        className
      ]
        .filter(Boolean)
        .join(' ')}
      {...props}
    >
      {children}
    </span>
  );
}

// ============================================================================
// Encryption Badge Component
// ============================================================================

import { EncryptionTier } from '@/types';
import { ShieldCheckIcon, LockClosedIcon } from '@heroicons/react/24/solid';

interface EncryptionBadgeProps {
  tier: EncryptionTier;
  className?: string;
  showIcon?: boolean;
}

export function EncryptionBadge({ tier, className = '', showIcon = true }: EncryptionBadgeProps) {
  const isE2E = tier === EncryptionTier.E2E;

  return (
    <Badge
      variant={isE2E ? 'primary' : 'secondary'}
      className={`gap-1 ${className}`}
      title={
        isE2E
          ? 'End-to-End Encrypted - Only you can read your entries'
          : 'User-Controlled Encryption - Encrypted with your password'
      }
    >
      {showIcon && (isE2E ? <ShieldCheckIcon className="h-3 w-3" /> : <LockClosedIcon className="h-3 w-3" />)}
      {isE2E ? 'E2E' : 'UCE'}
    </Badge>
  );
}
