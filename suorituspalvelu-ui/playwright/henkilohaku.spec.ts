import { expect } from '@playwright/test';
import { test } from './lib/fixtures';
import OPPIJAN_TIEDOT from './fixtures/oppijanTiedot.json' with { type: 'json' };

const OPPIJANUMERO = OPPIJAN_TIEDOT.oppijaNumero;
const HENKILOTUNNUS = OPPIJAN_TIEDOT.henkiloTunnus;

test.describe('Henkilöhaku', () => {
  test.beforeEach(async ({ page }) => {
    await page.route('**/ui/tiedot', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          json: OPPIJAN_TIEDOT,
        });
      }
    });
  });

  test('näyttää ilmoituksen, jos ei henkilö löytynyt tai hakutermi ei validi', async ({
    page,
  }) => {
    await page.goto('');
    const searchInput = page.getByRole('textbox', { name: 'Hae Henkilö' });

    await searchInput.fill('Olli Oppija');

    // Nimellä hakeminen ei ole mahdollista
    await expect(
      page.getByText(
        'Etsi henkilöä syöttämällä oppijanumero tai henkilötunnus.',
      ),
    ).toBeVisible();
    await expect(page.getByText('Olli Oppija')).toBeHidden();

    await expect(page.getByText('Henkilöä ei löytynyt')).toBeHidden();
    await expect(page.getByText('Olli Oppija')).toBeHidden();

    await searchInput.fill('1.2.246.562.24.40483869850');

    await expect(page.getByText('Henkilöä ei löytynyt')).toBeVisible();
    await expect(page.getByText('Olli Oppija')).toBeHidden();

    await searchInput.fill('123456-9999');

    await expect(page.getByText('Henkilöä ei löytynyt')).toBeVisible();
    await expect(page.getByText('Olli Oppija')).toBeHidden();
  });

  test('suodattaa oppijanumerolla', async ({ page }) => {
    await page.goto('');
    const searchInput = page.getByRole('textbox', { name: 'Hae Henkilö' });

    await searchInput.fill(OPPIJANUMERO);

    await expect(page.getByText('Olli Oppija')).toBeVisible();
  });

  test('suodattaa henkilötunnuksella', async ({ page }) => {
    await page.goto('');
    const searchInput = page.getByRole('textbox', { name: 'Hae Henkilö' });

    await searchInput.fill(HENKILOTUNNUS);

    await expect(page.getByText('Olli Oppija')).toBeVisible();
  });
});
