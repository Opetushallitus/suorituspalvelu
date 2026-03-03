import { expect } from '@playwright/test';
import { test } from './lib/fixtures';
import OPPIJAN_TIEDOT from './fixtures/oppijanTiedot.json' with { type: 'json' };
import { NDASH } from '@/lib/common';
import {
  DIA_TUTKINTO_SUORITUS,
  DIA_VASTAAVUUSTODISTUS_SUORITUS,
  EB_SUORITUS,
  expectSuoritukset,
  HEVOSTALOUDEN_PERUSTUTKINTO_SUORITUS,
  IB_SUORITUS,
  TUTKINTOON_JOHTAVA_KORKEAKOULU_SUORITUS,
  LUKION_OPPIAINEEN_OPPIMAARA_SUORITUS,
  LUKION_OPPIMAARA_SUORITUS,
  MAANMITTAUSALAN_PERUSTUTKINTO_SUORITUS,
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
  TUTKINTOON_JOHTAMATON_KORKEAKOULU_SUORITUS,
} from './lib/suoritusTestUtils';
import { expectLabeledValues } from './lib/playwrightUtils';

const OPPIJANUMERO = OPPIJAN_TIEDOT.oppijaNumero;

test.describe('Suoritustiedot', () => {
  test.beforeEach(async ({ page }) => {
    await page.clock.setFixedTime(new Date('2025-01-01T12:00:00Z'));
    await page.route('**/ui/tiedot', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          json: OPPIJAN_TIEDOT,
        });
      }
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

    await expectLabeledValues(opiskeluoikeusPaper, [
      {
        label: 'Oppilaitos',
        value: 'Tampereen yliopisto',
      },
      {
        label: 'Voimassaolo',
        value: `1.8.2001 ${NDASH} 11.12.2025Voimassa(aktiivinen)`,
      },
      {
        label: 'Sektori',
        value: 'YO',
      },
      {
        label: 'Tutkintotaso',
        value: 'Ylempi korkeakoulututkinto',
      },
    ]);
  });

  test('Suoritukset koulutustyypeittäin', async ({ page }) => {
    await page
      .getByRole('button', {
        name: 'Näytä tutkintoon johtamattomat korkeakoulusuoritukset',
      })
      .click();

    await expectSuoritukset(page, [
      TUTKINTOON_JOHTAVA_KORKEAKOULU_SUORITUS,
      TUTKINTOON_JOHTAMATON_KORKEAKOULU_SUORITUS,
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
      PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA_SUORITUS,
    ]);
  });
});
