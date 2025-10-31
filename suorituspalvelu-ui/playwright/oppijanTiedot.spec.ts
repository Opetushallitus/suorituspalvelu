import { expect } from '@playwright/test';
import { test } from './lib/fixtures';
import OPPIJAN_TIEDOT from './fixtures/oppijanTiedot.json' with { type: 'json' };
import VALINTA_DATA from './fixtures/valintaData.json' with { type: 'json' };

const OPPIJANUMERO = OPPIJAN_TIEDOT.oppijaNumero;

test.describe('Oppijan tiedot', () => {
  test.beforeEach(async ({ page }) => {
    await page.clock.setFixedTime(new Date('2025-01-01T12:00:00Z'));
    await page.route(`**/ui/tiedot/${OPPIJANUMERO}`, async (route) => {
      await route.fulfill({
        json: OPPIJAN_TIEDOT,
      });
    });

    await page.route(`**/ui/rajain/oppilaitokset`, async (route) => {
      await route.fulfill({
        json: {
          oppilaitokset: [],
        },
      });
    });

    await page.route(
      `**/ui/valintadata?oppijaNumero=${OPPIJANUMERO}`,
      async (route) => {
        await route.fulfill({
          json: VALINTA_DATA,
        });
      },
    );

    await page.goto(`/suorituspalvelu/henkilo/${OPPIJANUMERO}`);
  });

  test('näyttää henkilötiedot', async ({ page }) => {
    await expect(page).toHaveTitle(
      'Suorituspalvelu - Henkilön tiedot - Olli Oppija',
    );

    await expect(
      page.getByRole('heading', { name: 'Olli Oppija (010296-1230)' }),
    ).toBeVisible();

    await expect(page.getByLabel('Syntymäaika')).toHaveText('1.1.2030');

    const oppijaNumeroLink = page.getByLabel('Oppijanumero').getByRole('link');
    await expect(oppijaNumeroLink).toHaveText(OPPIJANUMERO);
    await expect(page.getByLabel('Henkilö-OID')).toHaveText(OPPIJANUMERO);
  });

  test('navigointi välilehtien välillä toimii', async ({ page }) => {
    // Oletuksena näytetään suoritustiedot
    await expect(page).toHaveURL(
      `/suorituspalvelu/henkilo/${OPPIJANUMERO}/suoritustiedot`,
    );

    await page.getByRole('link', { name: 'Opiskelijavalinnan tiedot' }).click();
    await expect(page).toHaveURL(
      `/suorituspalvelu/henkilo/${OPPIJANUMERO}/opiskelijavalinnan-tiedot`,
    );

    await page.getByRole('link', { name: 'Suoritustiedot' }).click();
    await expect(page).toHaveURL(
      `/suorituspalvelu/henkilo/${OPPIJANUMERO}/suoritustiedot`,
    );
  });
});
