/**
 * Card Component
 *
 * Container component with consistent styling.
 */

import React, { HTMLAttributes, forwardRef } from 'react';

// ============================================================================
// Types
// ============================================================================

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  className?: string;
  hover?: boolean;
  padding?: 'none' | 'sm' | 'md' | 'lg';
}

interface CardHeaderProps extends HTMLAttributes<HTMLDivElement> {
  className?: string;
}

interface CardTitleProps extends HTMLAttributes<HTMLHeadingElement> {
  className?: string;
}

interface CardDescriptionProps extends HTMLAttributes<HTMLParagraphElement> {
  className?: string;
}

interface CardContentProps extends HTMLAttributes<HTMLDivElement> {
  className?: string;
}

interface CardFooterProps extends HTMLAttributes<HTMLDivElement> {
  className?: string;
}

// ============================================================================
// Components
// ============================================================================

export const Card = forwardRef<HTMLDivElement, CardProps>(
  ({ className = '', hover = false, padding = 'md', children, ...props }, ref) => {
    const paddingClasses = {
      none: '',
      sm: 'p-4',
      md: 'p-6',
      lg: 'p-8'
    }[padding];

    return (
      <div
        ref={ref}
        className={[
          'rounded-lg border bg-card text-card-foreground shadow-sm',
          hover && 'transition-shadow hover:shadow-md',
          paddingClasses,
          className
        ]
          .filter(Boolean)
          .join(' ')}
        {...props}
      >
        {children}
      </div>
    );
  }
);

Card.displayName = 'Card';

export const CardHeader = forwardRef<HTMLDivElement, CardHeaderProps>(
  ({ className = '', ...props }, ref) => (
    <div ref={ref} className={`flex flex-col space-y-1.5 ${className}`} {...props} />
  )
);

CardHeader.displayName = 'CardHeader';

export const CardTitle = forwardRef<HTMLHeadingElement, CardTitleProps>(
  ({ className = '', ...props }, ref) => (
    <h3
      ref={ref}
      className={`text-2xl font-semibold leading-none tracking-tight ${className}`}
      {...props}
    />
  )
);

CardTitle.displayName = 'CardTitle';

export const CardDescription = forwardRef<HTMLParagraphElement, CardDescriptionProps>(
  ({ className = '', ...props }, ref) => (
    <p ref={ref} className={`text-sm text-muted-foreground ${className}`} {...props} />
  )
);

CardDescription.displayName = 'CardDescription';

export const CardContent = forwardRef<HTMLDivElement, CardContentProps>(
  ({ className = '', ...props }, ref) => (
    <div ref={ref} className={`pt-0 ${className}`} {...props} />
  )
);

CardContent.displayName = 'CardContent';

export const CardFooter = forwardRef<HTMLDivElement, CardFooterProps>(
  ({ className = '', ...props }, ref) => (
    <div ref={ref} className={`flex items-center pt-0 ${className}`} {...props} />
  )
);

CardFooter.displayName = 'CardFooter';
