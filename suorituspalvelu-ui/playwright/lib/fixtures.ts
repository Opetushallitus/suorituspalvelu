import { test as base } from '@playwright/test';

export const test = base.extend<object>({
  page: async ({ page }, use) => {
    await page.route('favicon.ico', async (route) => {
      await route.fulfill();
    });

    await page.route(`**/ui/kayttaja`, async (route) => {
      await route.fulfill({
        json: {
          asiointiKieli: 'fi',
        },
      });
    });
    // eslint-disable-next-line react-hooks/rules-of-hooks
    await use(page);
  },
});

export { expect } from '@playwright/test';
