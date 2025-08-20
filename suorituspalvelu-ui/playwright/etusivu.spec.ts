import { expect } from '@playwright/test';
import { test } from './lib/fixtures';

test.describe('Savutestit', () => {
  test.beforeEach(async ({ page }) => {
    await page.route(`**/ui/oppilaitokset`, async (route) => {
      await route.fulfill({
        json: {
          oppilaitokset: [],
        },
      });
    });
  });

  test('Sivu latautuu', async ({ page }) => {
    await page.goto('');
    await expect(page).toHaveTitle('Suorituspalvelu');
    await expect(
      page.getByRole('heading', { name: 'Suorituspalvelu' }),
    ).toBeVisible();
  });
});
