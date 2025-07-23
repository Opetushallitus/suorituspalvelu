import { expect } from '@playwright/test';
import { test } from './fixtures';

test.describe('Smoke Test', () => {
  test('Homepage loads successfully', async ({ page }) => {
    await page.goto('');
    await expect(page).toHaveTitle('Suorituspalvelu');
    await expect(page.locator('body')).toBeVisible();
  });
});
