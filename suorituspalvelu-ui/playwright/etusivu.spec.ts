import { expect } from '@playwright/test';
import { test } from './lib/fixtures';

test.describe('Etusivu', () => {
  test.beforeEach(async ({ page }) => {
    await page.route(`**/ui/rajain/oppilaitokset`, async (route) => {
      await route.fulfill({
        json: {
          oppilaitokset: [],
        },
      });
    });
  });

  test('Juuresta ohjautuu henkilöhakuun', async ({ page }) => {
    await page.goto('');
    await expect(page).toHaveURL((url) => url.toString().endsWith('/henkilo'));
  });

  test('Sivu latautuu käyttäjän asiointikielellä', async ({ page }) => {
    await page.goto('');
    await expect(page).toHaveTitle('Suorituspalvelu');
    await expect(
      page.getByRole('heading', { name: 'Suorituspalvelu' }),
    ).toBeVisible();

    await page.route(`**/ui/kayttaja`, async (route) => {
      await route.fulfill({
        json: {
          asiointiKieli: 'en',
        },
      });
    });

    await page.goto('');
    await expect(page).toHaveTitle('Study record service');
    await expect(
      page.getByRole('heading', { name: 'Study record service' }),
    ).toBeVisible();
  });
});
