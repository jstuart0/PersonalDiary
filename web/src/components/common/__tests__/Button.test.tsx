/**
 * Button Component Tests
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Button } from '../Button';

describe('Button', () => {
  it('renders button with text', () => {
    render(<Button>Click me</Button>);
    expect(screen.getByText('Click me')).toBeInTheDocument();
  });

  it('calls onClick when clicked', () => {
    const handleClick = vi.fn();
    render(<Button onClick={handleClick}>Click me</Button>);

    fireEvent.click(screen.getByText('Click me'));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('is disabled when disabled prop is true', () => {
    render(<Button disabled>Click me</Button>);
    const button = screen.getByText('Click me');
    expect(button).toBeDisabled();
  });

  it('shows loading state', () => {
    render(<Button loading>Click me</Button>);
    expect(screen.getByText('Click me')).toBeInTheDocument();
    // Loading spinner should be present
  });

  it('applies variant classes', () => {
    const { rerender } = render(<Button variant="primary">Button</Button>);
    expect(screen.getByText('Button')).toHaveClass('bg-primary');

    rerender(<Button variant="outline">Button</Button>);
    expect(screen.getByText('Button')).toHaveClass('border');
  });

  it('renders with left icon', () => {
    const Icon = () => <span data-testid="icon">Icon</span>;
    render(<Button leftIcon={<Icon />}>Button</Button>);
    expect(screen.getByTestId('icon')).toBeInTheDocument();
  });

  it('renders with right icon', () => {
    const Icon = () => <span data-testid="icon">Icon</span>;
    render(<Button rightIcon={<Icon />}>Button</Button>);
    expect(screen.getByTestId('icon')).toBeInTheDocument();
  });
});
