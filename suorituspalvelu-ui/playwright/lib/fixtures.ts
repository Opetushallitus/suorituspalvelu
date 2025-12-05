import { test as base } from '@playwright/test';
import { stubKayttajaResponse } from './playwrightUtils';

export const test = base.extend<object>({
  page: async ({ page }, use) => {
    await page.route('favicon.ico', async (route) => {
      await route.fulfill();
    });
    await page.route('apply-raamit.js', async (route) => {
      await route.fulfill();
    });

    await stubKayttajaResponse(page, {
      asiointiKieli: 'fi',
      isRekisterinpitaja: true,
      isOrganisaationKatselija: false,
    });

    await page.route(`**/ui/oppilaitokset`, async (route) => {
      await route.fulfill({
        json: {
          oppilaitokset: [],
        },
      });
    });

    await use(page);
  },
});

export { expect } from '@playwright/test';
