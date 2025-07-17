import { test, expect } from '@playwright/test';

test.describe('Smoke Test', () => {
  test('Homepage loads successfully', async ({ page }) => {
    await page.goto('http://localhost:3000');
    await expect(page).toHaveTitle(/suorituspalvelu/i);
    await expect(page.locator('body')).toBeVisible();
  });
});
