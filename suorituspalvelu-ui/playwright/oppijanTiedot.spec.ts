import { expect } from '@playwright/test';
import { test } from './lib/fixtures';
import OPPIJAN_TIEDOT from './fixtures/oppijanTiedot.json' with { type: 'json' };
import { NDASH } from '@/lib/common';
import {
  AIKUISTEN_PERUSOPETUKSEN_OPPIMAARA_SUORITUS,
  DIA_TUTKINTO_SUORITUS,
  DIA_VASTAAVUUSTODISTUS_SUORITUS,
  EB_SUORITUS,
  expectSuoritukset,
  HEVOSTALOUDEN_PERUSTUTKINTO_SUORITUS,
  IB_SUORITUS,
  KORKEAKOULU_SUORITUS,
  LUKION_OPPIAINEEN_OPPIMAARA_SUORITUS,
  LUKION_OPPIMAARA_SUORITUS,
  MAANMITTAUSALAN_PERUSTUTKINTO_SUORITUS,
  NUORTEN_PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA_SUORITUS,
  PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA_SUORITUS,
  PERUSOPETUKSEN_OPPIMAARA_78LUOKKA_SUORITUS,
  PERUSOPETUKSEN_OPPIMAARA_SUORITUS,
  PRE_IB_SUORITUS,
  PUUTARHA_ALAN_PERUSTUTKINTO_SUORITUS,
  TALOUS_JA_HENKILOSTOALAN_ERIKOISAMMATTITUTKINTO_SUORITUS,
  TELMA_SUORITUS,
  TUVA_SUORITUS,
  VAPAA_SIVISTYSTYO_SUORITUS,
  YOTUTKINTO_SUORITUS,
} from './lib/suoritusTestUtils';

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

    await page.goto(`/suorituspalvelu/henkilo/${OPPIJANUMERO}`);
  });

  test('Henkilötiedot', async ({ page }) => {
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

  test('Opiskeluoikeudet', async ({ page }) => {
    await expect(
      page.getByRole('heading', { name: 'Opiskeluoikeudet' }),
    ).toBeVisible();

    const opiskeluoikeusPapers = page.getByTestId('opiskeluoikeus-paper');

    await expect(opiskeluoikeusPapers).toHaveCount(1);
    const opiskeluoikeusPaper = opiskeluoikeusPapers.first();

    await expect(
      opiskeluoikeusPaper.getByRole('heading', {
        name: 'Kasvatust. maist., kasvatustiede',
      }),
    ).toBeVisible();

    await expect(opiskeluoikeusPaper.getByLabel('Oppilaitos')).toHaveText(
      'Tampereen yliopisto',
    );
    await expect(opiskeluoikeusPaper.getByLabel('Voimassaolo')).toHaveText(
      `1.8.2001 ${NDASH} 11.12.2025Voimassa(aktiivinen)`,
    );
  });

  test('Suoritukset koulutustyypeittäin', async ({ page }) => {
    await expectSuoritukset(page, [
      KORKEAKOULU_SUORITUS,
      YOTUTKINTO_SUORITUS,
      LUKION_OPPIMAARA_SUORITUS,
      LUKION_OPPIAINEEN_OPPIMAARA_SUORITUS,
      DIA_TUTKINTO_SUORITUS,
      DIA_VASTAAVUUSTODISTUS_SUORITUS,
      EB_SUORITUS,
      IB_SUORITUS,
      PRE_IB_SUORITUS,
      PUUTARHA_ALAN_PERUSTUTKINTO_SUORITUS,
      HEVOSTALOUDEN_PERUSTUTKINTO_SUORITUS,
      MAANMITTAUSALAN_PERUSTUTKINTO_SUORITUS,
      TALOUS_JA_HENKILOSTOALAN_ERIKOISAMMATTITUTKINTO_SUORITUS,
      TELMA_SUORITUS,
      TUVA_SUORITUS,
      VAPAA_SIVISTYSTYO_SUORITUS,
      PERUSOPETUKSEN_OPPIMAARA_SUORITUS,
      PERUSOPETUKSEN_OPPIMAARA_78LUOKKA_SUORITUS,
      NUORTEN_PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA_SUORITUS,
      PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA_SUORITUS,
      AIKUISTEN_PERUSOPETUKSEN_OPPIMAARA_SUORITUS,
    ]);
  });

  test('Suoritukset aikajärjestyksessä', async ({ page }) => {
    await page.getByRole('button', { name: 'Uusin ensin' }).click();
    await expectSuoritukset(page, [
      KORKEAKOULU_SUORITUS,
      LUKION_OPPIMAARA_SUORITUS,
      LUKION_OPPIAINEEN_OPPIMAARA_SUORITUS,
      DIA_TUTKINTO_SUORITUS,
      DIA_VASTAAVUUSTODISTUS_SUORITUS,
      EB_SUORITUS,
      IB_SUORITUS,
      PRE_IB_SUORITUS,
      PUUTARHA_ALAN_PERUSTUTKINTO_SUORITUS,
      HEVOSTALOUDEN_PERUSTUTKINTO_SUORITUS,
      YOTUTKINTO_SUORITUS,
      MAANMITTAUSALAN_PERUSTUTKINTO_SUORITUS,
      TALOUS_JA_HENKILOSTOALAN_ERIKOISAMMATTITUTKINTO_SUORITUS,
      TELMA_SUORITUS,
      TUVA_SUORITUS,
      PERUSOPETUKSEN_OPPIMAARA_SUORITUS,
      PERUSOPETUKSEN_OPPIMAARA_78LUOKKA_SUORITUS,
      NUORTEN_PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA_SUORITUS,
      PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA_SUORITUS,
      AIKUISTEN_PERUSOPETUKSEN_OPPIMAARA_SUORITUS,
      VAPAA_SIVISTYSTYO_SUORITUS,
    ]);
  });
});
