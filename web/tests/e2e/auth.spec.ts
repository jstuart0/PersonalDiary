/**
 * Authentication E2E Tests
 */

import { test, expect } from '@playwright/test';

test.describe('Authentication', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should show login page for unauthenticated users', async ({ page }) => {
    await expect(page).toHaveURL('/auth');
    await expect(page.getByRole('heading', { name: /sign in/i })).toBeVisible();
  });

  test('should allow user to sign up with E2E encryption', async ({ page }) => {
    await page.getByRole('button', { name: /sign up/i }).click();

    await page.getByLabel(/email/i).fill('test@example.com');
    await page.getByLabel(/password/i).fill('SecurePassword123!');

    // Select E2E encryption
    await page.getByRole('radio', { name: /end-to-end/i }).click();

    await page.getByRole('button', { name: /create account/i }).click();

    // Should show recovery codes
    await expect(page.getByText(/recovery codes/i)).toBeVisible();
  });

  test('should allow user to sign up with UCE encryption', async ({ page }) => {
    await page.getByRole('button', { name: /sign up/i }).click();

    await page.getByLabel(/email/i).fill('test2@example.com');
    await page.getByLabel(/password/i).fill('SecurePassword123!');

    // Select UCE encryption
    await page.getByRole('radio', { name: /user-controlled/i }).click();

    await page.getByRole('button', { name: /create account/i }).click();

    // Should redirect to timeline
    await expect(page).toHaveURL('/timeline');
  });

  test('should allow user to login', async ({ page }) => {
    await page.getByLabel(/email/i).fill('test@example.com');
    await page.getByLabel(/password/i).fill('SecurePassword123!');

    await page.getByRole('button', { name: /sign in/i }).click();

    // Should redirect to timeline
    await expect(page).toHaveURL('/timeline');
  });

  test('should show error for invalid credentials', async ({ page }) => {
    await page.getByLabel(/email/i).fill('wrong@example.com');
    await page.getByLabel(/password/i).fill('WrongPassword');

    await page.getByRole('button', { name: /sign in/i }).click();

    await expect(page.getByText(/invalid credentials/i)).toBeVisible();
  });
});
