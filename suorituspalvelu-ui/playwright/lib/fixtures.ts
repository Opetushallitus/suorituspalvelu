import { test as base } from '@playwright/test';
import { stubKayttajaResponse } from './playwrightUtils';

function isFirefox(): boolean {
  return test.info().project.name === 'firefox';
}

export const test = base.extend<object>({
  page: async ({ page }, use) => {
    const originalGoto = page.goto.bind(page);

    // Workaround for NS_BINDING_ABORTED error in Firefox, which can occur when navigating away from a page before it finishes loading.
    // https://github.com/microsoft/playwright/issues/20749
    page.goto = async function (url, options) {
      try {
        return await originalGoto(url, options);
      } catch (error: unknown) {
        if (
          isFirefox() &&
          (error as { message?: string })?.message?.includes(
            'NS_BINDING_ABORTED',
          )
        ) {
          return null;
        }
        throw error;
      }
    };

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

    await page.route(`**/ui/vastaanotot`, async (route) => {
      await route.fulfill({
        json: {
          vastaanotot: [],
          vanhatVastaanotot: [],
        },
      });
    });

    await use(page);
  },
});

export { expect } from '@playwright/test';
