import { expect } from '@playwright/test';
import { test } from './fixtures';
import OPPIJAN_TIEDOT from './fixtures/oppijanTiedot.json';
import { NDASH } from '@/lib/common';

const OPPIJANUMERO = OPPIJAN_TIEDOT.oppijaNumero;

test.describe('Oppijan tiedot', () => {
  test.beforeEach(async ({ page }) => {
    await page.clock.setFixedTime(new Date('2025-01-01T12:00:00Z'));
    await page.route(`**/ui/tiedot/${OPPIJANUMERO}`, async (route) => {
      await route.fulfill({
        json: OPPIJAN_TIEDOT,
      });
    });

    await page.route(`**/ui/oppilaitokset`, async (route) => {
      await route.fulfill({
        json: {
          oppilaitokset: [],
        },
      });
    });

    await page.goto(`?oppijaNumero=${OPPIJANUMERO}`);
  });

  test('Näytetään opiskeluoikeudet', async ({ page }) => {
    await expect(
      page.getByRole('heading', { name: 'Opiskeluoikeudet' }),
    ).toBeVisible();
    await expect(
      page.getByText('Kasvatust. maist., kasvatustiede'),
    ).toBeVisible();

    const opiskeluoikeusPapers = page.getByTestId('opiskeluoikeus-paper');

    await expect(opiskeluoikeusPapers).toHaveCount(1);
    const opiskeluoikeusPaper = opiskeluoikeusPapers.first();
    await expect(opiskeluoikeusPapers).toContainText(
      'Kasvatust. maist., kasvatustiede',
    );

    await expect(opiskeluoikeusPaper.getByLabel('Oppilaitos')).toHaveText(
      'Tampereen yliopisto',
    );
    await expect(opiskeluoikeusPaper.getByLabel('Voimassaolo')).toHaveText(
      `1.8.2001 ${NDASH} 11.12.2025Voimassa`,
    );
  });

  test('Näytetään suoritukset', async ({ page }) => {
    await expect(
      page.getByRole('heading', { name: 'Suoritukset' }),
    ).toBeVisible();

    const suoritusPapers = page.getByTestId('suoritus-paper');

    const firstPaper = suoritusPapers.first();

    await expect(firstPaper).toContainText('Kasvatust. maist., kasvatustiede');
    await expect(firstPaper.getByLabel('Oppilaitos')).toHaveText(
      'Tampereen yliopisto',
    );
    await expect(firstPaper.getByLabel('Tila')).toHaveText('Suoritus kesken');
    await expect(firstPaper.getByLabel('Valmistumispäivä')).toHaveText('-');
    await expect(firstPaper.getByLabel('Hakukohde')).toHaveText(
      'Maisterihaku, luokanopettaja (opetus suomeksi), kasvatustieteiden maisteriohjelma, kasvatustieteen maisteri (2v)',
    );

    const secondPaper = suoritusPapers.nth(1);

    await expect(secondPaper).toContainText('Ylioppilastutkinto (2019)');
    await expect(secondPaper.getByLabel('Oppilaitos')).toHaveText(
      'Ylioppilastutkintolautakunta',
    );

    await expect(secondPaper.getByLabel('Tila')).toHaveText('Suoritus valmis');

    await expect(secondPaper.getByLabel('Valmistumispäivä')).toHaveText(
      '1.6.2019',
    );

    const thirdPaper = suoritusPapers.nth(2);

    await expect(thirdPaper).toContainText('Lukion oppimäärä (2024)');
    await expect(thirdPaper.getByLabel('Oppilaitos')).toHaveText(
      'Ylioppilastutkintolautakunta',
    );

    await expect(thirdPaper.getByLabel('Tila')).toHaveText('Suoritus valmis');

    await expect(thirdPaper.getByLabel('Valmistumispäivä')).toHaveText(
      '31.12.2024',
    );

    const oppiaineListItems = thirdPaper
      .getByLabel('Oppiaineet')
      .getByRole('listitem');

    await expect(oppiaineListItems).toHaveCount(2);
    await expect(oppiaineListItems.nth(0)).toHaveText(
      'Äidinkieli ja kirjallisuus',
    );
    await expect(oppiaineListItems.nth(1)).toHaveText(
      'Uskonto/Elämänkatsomustieto',
    );
  });
});
