/**
 * Alert Component
 *
 * Display important messages and notifications.
 */

import React, { HTMLAttributes } from 'react';
import {
  InformationCircleIcon,
  CheckCircleIcon,
  ExclamationTriangleIcon,
  XCircleIcon
} from '@heroicons/react/24/outline';

// ============================================================================
// Types
// ============================================================================

type AlertVariant = 'info' | 'success' | 'warning' | 'error';

interface AlertProps extends HTMLAttributes<HTMLDivElement> {
  variant?: AlertVariant;
  title?: string;
  className?: string;
}

// ============================================================================
// Styles and Icons
// ============================================================================

const variantStyles: Record<AlertVariant, string> = {
  info: 'bg-blue-50 dark:bg-blue-950 border-blue-200 dark:border-blue-800 text-blue-900 dark:text-blue-100',
  success:
    'bg-green-50 dark:bg-green-950 border-green-200 dark:border-green-800 text-green-900 dark:text-green-100',
  warning:
    'bg-yellow-50 dark:bg-yellow-950 border-yellow-200 dark:border-yellow-800 text-yellow-900 dark:text-yellow-100',
  error: 'bg-red-50 dark:bg-red-950 border-red-200 dark:border-red-800 text-red-900 dark:text-red-100'
};

const iconMap: Record<AlertVariant, React.ComponentType<{ className?: string }>> = {
  info: InformationCircleIcon,
  success: CheckCircleIcon,
  warning: ExclamationTriangleIcon,
  error: XCircleIcon
};

const iconColorMap: Record<AlertVariant, string> = {
  info: 'text-blue-600 dark:text-blue-400',
  success: 'text-green-600 dark:text-green-400',
  warning: 'text-yellow-600 dark:text-yellow-400',
  error: 'text-red-600 dark:text-red-400'
};

// ============================================================================
// Component
// ============================================================================

export function Alert({
  variant = 'info',
  title,
  className = '',
  children,
  ...props
}: AlertProps) {
  const Icon = iconMap[variant];

  return (
    <div
      className={[
        'rounded-lg border p-4',
        variantStyles[variant],
        className
      ]
        .filter(Boolean)
        .join(' ')}
      role="alert"
      {...props}
    >
      <div className="flex gap-3">
        <Icon className={`h-5 w-5 flex-shrink-0 mt-0.5 ${iconColorMap[variant]}`} />
        <div className="flex-1">
          {title && <h5 className="font-medium mb-1">{title}</h5>}
          <div className="text-sm">{children}</div>
        </div>
      </div>
    </div>
  );
}
