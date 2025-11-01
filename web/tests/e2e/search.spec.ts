/**
 * Search E2E Tests
 */

import { test, expect } from '@playwright/test';

test.describe('Search', () => {
  test.beforeEach(async ({ page }) => {
    // Login first
    await page.goto('/auth');
    await page.getByLabel(/email/i).fill('test@example.com');
    await page.getByLabel(/password/i).fill('SecurePassword123!');
    await page.getByRole('button', { name: /sign in/i }).click();

    await expect(page).toHaveURL('/timeline');

    // Create some test entries
    const entries = [
      { content: 'Had a great day at the beach', tags: ['vacation', 'beach'] },
      { content: 'Productive work meeting today', tags: ['work'] },
      { content: 'Enjoyed a nice beach sunset', tags: ['vacation', 'beach'] }
    ];

    for (const entry of entries) {
      await page.getByRole('button', { name: /new entry/i }).click();
      await page.getByPlaceholder(/what's on your mind/i).fill(entry.content);

      for (const tag of entry.tags) {
        await page.getByRole('button', { name: /add tag/i }).click();
        await page.getByPlaceholder(/enter tag/i).fill(tag);
        await page.keyboard.press('Enter');
      }

      await page.getByRole('button', { name: /save entry/i }).click();
      await page.waitForTimeout(500); // Wait for entry to be saved
    }
  });

  test('should search entries by content', async ({ page }) => {
    await page.getByPlaceholder(/search entries/i).fill('beach');
    await page.getByRole('button', { name: /search/i }).click();

    // Should show only beach-related entries
    await expect(page.getByText('great day at the beach')).toBeVisible();
    await expect(page.getByText('beach sunset')).toBeVisible();
    await expect(page.getByText('work meeting')).not.toBeVisible();
  });

  test('should filter entries by tag', async ({ page }) => {
    // Open filters
    await page.getByRole('button', { name: /filters/i }).click();

    // Select work tag
    await page.getByText('#work').click();

    // Apply filters
    await page.getByRole('button', { name: /apply filters/i }).click();

    // Should show only work entries
    await expect(page.getByText('work meeting')).toBeVisible();
    await expect(page.getByText('beach')).not.toBeVisible();
  });

  test('should show suggestions while typing', async ({ page }) => {
    await page.getByPlaceholder(/search entries/i).fill('bea');

    // Suggestions should appear
    await expect(page.getByRole('button', { name: /beach/i })).toBeVisible();
  });

  test('should clear search results', async ({ page }) => {
    // Search for something
    await page.getByPlaceholder(/search entries/i).fill('work');
    await page.getByRole('button', { name: /search/i }).click();

    // Should show filtered results
    await expect(page.getByText('work meeting')).toBeVisible();
    await expect(page.getByText('1 of 3')).toBeVisible();

    // Clear search
    await page.getByRole('button', { name: /clear search/i }).click();

    // Should show all entries
    await expect(page.getByText('3 entries')).toBeVisible();
  });
});
