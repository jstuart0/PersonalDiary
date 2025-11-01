/**
 * Entry Management E2E Tests
 */

import { test, expect } from '@playwright/test';

test.describe('Entry Management', () => {
  test.beforeEach(async ({ page }) => {
    // Login first
    await page.goto('/auth');
    await page.getByLabel(/email/i).fill('test@example.com');
    await page.getByLabel(/password/i).fill('SecurePassword123!');
    await page.getByRole('button', { name: /sign in/i }).click();

    await expect(page).toHaveURL('/timeline');
  });

  test('should create a new entry', async ({ page }) => {
    await page.getByRole('button', { name: /new entry/i }).click();

    // Fill in entry content
    await page.getByPlaceholder(/what's on your mind/i).fill('This is my first diary entry!');

    // Add a tag
    await page.getByRole('button', { name: /add tag/i }).click();
    await page.getByPlaceholder(/enter tag/i).fill('personal');
    await page.keyboard.press('Enter');

    // Save entry
    await page.getByRole('button', { name: /save entry/i }).click();

    // Entry should appear in timeline
    await expect(page.getByText('This is my first diary entry!')).toBeVisible();
    await expect(page.getByText('#personal')).toBeVisible();
  });

  test('should edit an existing entry', async ({ page }) => {
    // Create an entry first
    await page.getByRole('button', { name: /new entry/i }).click();
    await page.getByPlaceholder(/what's on your mind/i).fill('Original content');
    await page.getByRole('button', { name: /save entry/i }).click();

    // Open entry menu and click edit
    await page.getByRole('button', { name: /entry actions/i }).first().click();
    await page.getByRole('button', { name: /edit/i }).click();

    // Update content
    await page.getByPlaceholder(/what's on your mind/i).clear();
    await page.getByPlaceholder(/what's on your mind/i).fill('Updated content');
    await page.getByRole('button', { name: /save entry/i }).click();

    // Updated content should appear
    await expect(page.getByText('Updated content')).toBeVisible();
    await expect(page.getByText('Original content')).not.toBeVisible();
  });

  test('should delete an entry', async ({ page }) => {
    // Create an entry first
    await page.getByRole('button', { name: /new entry/i }).click();
    await page.getByPlaceholder(/what's on your mind/i).fill('Entry to delete');
    await page.getByRole('button', { name: /save entry/i }).click();

    // Open entry menu and click delete
    await page.getByRole('button', { name: /entry actions/i }).first().click();
    await page.getByRole('button', { name: /delete/i }).click();

    // Confirm deletion
    page.on('dialog', (dialog) => dialog.accept());

    // Entry should be removed
    await expect(page.getByText('Entry to delete')).not.toBeVisible();
  });

  test('should view entry details', async ({ page }) => {
    // Create an entry first
    await page.getByRole('button', { name: /new entry/i }).click();
    await page.getByPlaceholder(/what's on your mind/i).fill('Detailed entry');
    await page.getByRole('button', { name: /save entry/i }).click();

    // Click on entry to view details
    await page.getByText('Detailed entry').click();

    // Detail modal should open
    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByText('Detailed entry')).toBeVisible();
  });
});
