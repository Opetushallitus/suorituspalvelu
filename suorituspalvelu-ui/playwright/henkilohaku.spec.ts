import { type Page } from '@playwright/test';
import { test, expect } from './lib/fixtures';
import OPPIJAN_TIEDOT from './fixtures/oppijanTiedot.json' with { type: 'json' };

const OPPIJANUMERO = OPPIJAN_TIEDOT.oppijaNumero;
const HETU = OPPIJAN_TIEDOT.henkiloTunnus;

const NOT_FOUND_OPPIJANUMERO = '1.2.246.562.24.00000000';
const NOT_FOUND_HETU = '123456-9999';

const getOppijaHeading = (page: Page) =>
  page.getByRole('heading', { name: 'Olli Oppija' });

const getSearchInput = (page: Page) =>
  page.getByRole('textbox', { name: 'Hae Henkilö' });

test.describe('Henkilöhaku', () => {
  test.beforeEach(async ({ page }) => {
    await page.route('**/ui/tiedot', async (route) => {
      if (route.request().method() === 'POST') {
        const tunniste = (await route.request().postDataJSON())?.tunniste;
        if (tunniste === OPPIJANUMERO || tunniste === HETU) {
          await route.fulfill({
            json: OPPIJAN_TIEDOT,
          });
        } else {
          await route.fulfill({ status: 410 });
        }
      }
    });
  });

  test('näyttää ilmoituksen, jos ei henkilö löytynyt tai hakutermi ei validi', async ({
    page,
  }) => {
    await page.goto('henkilo');
    const searchInput = getSearchInput(page);

    // Nimellä hakeminen ei ole mahdollista
    await searchInput.fill('Olli Oppija');
    await expect(
      page.getByText(
        'Etsi henkilöä syöttämällä oppijanumero tai henkilötunnus.',
      ),
    ).toBeVisible();
    await expect(page.getByText('Henkilöä ei löytynyt')).toBeHidden();
    await expect(getOppijaHeading(page)).toBeHidden();

    await searchInput.fill(NOT_FOUND_OPPIJANUMERO);
    await expect(page.getByText('Henkilöä ei löytynyt')).toBeVisible();
    await expect(getOppijaHeading(page)).toBeHidden();
    await expect(page).toHaveURL((url) => url.toString().endsWith(`/henkilo`));
    await searchInput.fill(NOT_FOUND_HETU);
    await expect(page.getByText('Henkilöä ei löytynyt')).toBeVisible();
    await expect(getOppijaHeading(page)).toBeHidden();
    await expect(page).toHaveURL((url) => url.toString().endsWith(`/henkilo`));
  });

  test('suodattaa oppijanumerolla ja muuttaa URL:a, jos henkilö löytyy', async ({
    page,
  }) => {
    await page.goto('henkilo');
    const searchInput = getSearchInput(page);

    await searchInput.fill(OPPIJANUMERO);
    await expect(page).toHaveURL((url) =>
      url.toString().endsWith(`henkilo/${OPPIJANUMERO}/suoritustiedot`),
    );

    await expect(getOppijaHeading(page)).toBeVisible();
  });

  test('näyttää virheen ja poistaa henkilötunnuksen URL:sta, jos yrittää navigoida suoraan henkilötunnuksella', async ({
    page,
  }) => {
    await page.goto(`henkilo/${HETU}`);

    await expect(
      page
        .getByRole('alert')
        .filter({ hasText: 'Suora linkitys henkilötunnukseen on kielletty!' }),
    ).toBeVisible();

    await expect(page).toHaveURL((url) => url.toString().endsWith('/henkilo'));

    await expect(
      page.getByText(
        'Etsi henkilöä syöttämällä oppijanumero tai henkilötunnus.',
      ),
    ).toBeVisible();

    await expect(getOppijaHeading(page)).toBeHidden();
  });

  test('suodattaa henkilötunnuksella ja näyttää oppijanumeron URL:ssä, jos oppija löytyy', async ({
    page,
  }) => {
    await page.goto('henkilo');
    const searchInput = getSearchInput(page);
    await searchInput.fill(HETU);

    await expect(page).toHaveURL((url) =>
      url.toString().endsWith(`henkilo/${OPPIJANUMERO}/suoritustiedot`),
    );

    await expect(getOppijaHeading(page)).toBeVisible();
  });
});
