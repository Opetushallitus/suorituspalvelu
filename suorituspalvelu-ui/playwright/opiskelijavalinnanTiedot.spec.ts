import { expect } from '@playwright/test';
import { test } from './lib/fixtures';
import { expectLabeledValues } from './lib/playwrightUtils';
import OPPIJAN_TIEDOT from './fixtures/oppijanTiedot.json' with { type: 'json' };
import VALINTA_DATA from './fixtures/valintaData.json' with { type: 'json' };

const OPPIJANUMERO = OPPIJAN_TIEDOT.oppijaNumero;

test.describe('Opiskelijavalinnan tiedot', () => {
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

    await page.route(
      `**/ui/valintadata?oppijaNumero=${OPPIJANUMERO}`,
      async (route) => {
        await route.fulfill({
          json: VALINTA_DATA,
        });
      },
    );

    await page.goto(
      `/suorituspalvelu/henkilo/${OPPIJANUMERO}/opiskelijavalinnan-tiedot`,
    );
  });

  test('näyttää uudet avainarvot', async ({ page }) => {
    await page.getByRole('button', { name: 'Uudet avainarvot' }).click();

    const tiedot = page.getByRole('region', {
      name: 'Suorituspalvelusta opiskelijavalintaan siirtyvät tiedot',
    });
    await expect(tiedot).toBeVisible();

    await expectLabeledValues(tiedot, [
      // General fields
      { label: 'perusopetuksen_kieli', value: 'FI' },
      // Suoritukset
      { label: 'perustutkinto_suoritettu', value: 'true' },
      { label: 'peruskoulu_suoritusvuosi', value: '2016' },
      { label: 'ammatillinen_suoritettu', value: 'true' },
      { label: 'lukio_suoritettu', value: 'false' },
      { label: 'yo_tutkinto_suoritettu', value: 'false' },
      // Lisäpistekoulutus
      { label: 'lisapistekoulutus_opisto', value: 'false' },
      { label: 'lisapistekoulutus_telma', value: 'false' },
      // Perusopetuksen oppiaineet
      { label: 'PERUSKOULU_ARVOSANA_AI', value: '9' },
      { label: 'PERUSKOULU_ARVOSANA_AI_OPPIAINEEN_KIELI', value: 'AI1' },
      { label: 'PERUSKOULU_ARVOSANA_MA', value: '9' },
      { label: 'PERUSKOULU_ARVOSANA_A1', value: '8' },
      { label: 'PERUSKOULU_ARVOSANA_A1_OPPIAINEEN_KIELI', value: 'EN' },
      { label: 'PERUSKOULU_ARVOSANA_B1', value: '8' },
      { label: 'PERUSKOULU_ARVOSANA_B1_OPPIAINEEN_KIELI', value: 'SV' },
      { label: 'PERUSKOULU_ARVOSANA_B2', value: '9' },
      { label: 'PERUSKOULU_ARVOSANA_B2_OPPIAINEEN_KIELI', value: 'DE' },
      { label: 'PERUSKOULU_ARVOSANA_AOM', value: '8' },
      { label: 'PERUSKOULU_ARVOSANA_AOM_OPPIAINEEN_KIELI', value: 'FI' },
      { label: 'PERUSKOULU_ARVOSANA_BI', value: '9' },
      { label: 'PERUSKOULU_ARVOSANA_FY', value: '9' },
      { label: 'PERUSKOULU_ARVOSANA_GE', value: '9' },
      { label: 'PERUSKOULU_ARVOSANA_HI', value: '8' },
      { label: 'PERUSKOULU_ARVOSANA_KE', value: '7' },
      { label: 'PERUSKOULU_ARVOSANA_KO', value: '8' },
      { label: 'PERUSKOULU_ARVOSANA_KS', value: '9' },
      { label: 'PERUSKOULU_ARVOSANA_KT', value: '10' },
      { label: 'PERUSKOULU_ARVOSANA_KU', value: '8' },
      { label: 'PERUSKOULU_ARVOSANA_LI', value: '9' },
      { label: 'PERUSKOULU_ARVOSANA_MU', value: '7' },
      { label: 'PERUSKOULU_ARVOSANA_TE', value: '8' },
      { label: 'PERUSKOULU_ARVOSANA_YH', value: '10' },
    ]);
  });

  test('näyttää vanhat avainarvot', async ({ page }) => {
    await page.getByRole('button', { name: 'Vanhat avainarvot' }).click();

    const tiedot = page.getByRole('region', {
      name: 'Suorituspalvelusta opiskelijavalintaan siirtyvät tiedot',
    });

    await expect(tiedot).toBeVisible();

    await expectLabeledValues(tiedot, [
      // Suoritukset (duplicate keys)
      { label: 'PK_TILA', value: 'true' },
      { label: 'PK_SUORITUSVUOSI', value: '2016' },
      { label: 'AM_TILA', value: 'true' },
      { label: 'LK_TILA', value: 'false' },
      { label: 'YO_TILA', value: 'false' },
      // Lisäpistekoulutus (duplicate keys)
      { label: 'LISAKOULUTUS_OPISTO', value: 'false' },
      { label: 'LISAKOULUTUS_TELMA', value: 'false' },
      // Perusopetuksen oppiaineet (duplicate keys)
      { label: 'PK_ARVOSANA_AI', value: '9' },
      { label: 'PK_ARVOSANA_AI_OPPIAINE', value: 'AI1' },
      { label: 'PK_ARVOSANA_MA', value: '9' },
      { label: 'PK_ARVOSANA_A1', value: '8' },
      { label: 'PK_ARVOSANA_A1_OPPIAINE', value: 'EN' },
      { label: 'PK_ARVOSANA_B1', value: '8' },
      { label: 'PK_ARVOSANA_B1_OPPIAINE', value: 'SV' },
      { label: 'PK_ARVOSANA_B2', value: '9' },
      { label: 'PK_ARVOSANA_B2_OPPIAINE', value: 'DE' },
      { label: 'PK_ARVOSANA_AOM', value: '8' },
      { label: 'PK_ARVOSANA_BI', value: '9' },
      { label: 'PK_ARVOSANA_FY', value: '9' },
      { label: 'PK_ARVOSANA_GE', value: '9' },
      { label: 'PK_ARVOSANA_HI', value: '8' },
      { label: 'PK_ARVOSANA_KE', value: '7' },
      { label: 'PK_ARVOSANA_KO', value: '8' },
      { label: 'PK_ARVOSANA_KS', value: '9' },
      { label: 'PK_ARVOSANA_KU', value: '8' },
      { label: 'PK_ARVOSANA_LI', value: '9' },
      { label: 'PK_ARVOSANA_MU', value: '7' },
      { label: 'PK_ARVOSANA_TE', value: '8' },
      { label: 'PK_ARVOSANA_YH', value: '10' },
    ]);
  });
});
