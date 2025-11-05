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
      (url) =>
        url.href.includes(`/ui/valintadata?oppijaNumero=${OPPIJANUMERO}`),
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
      // Suoritukset
      { label: 'PK_TILA', value: 'true' },
      { label: 'PK_SUORITUSVUOSI', value: '2016' },
      { label: 'AM_TILA', value: 'true' },
      { label: 'LK_TILA', value: 'false' },
      { label: 'YO_TILA', value: 'false' },
      // Lisäpistekoulutus
      { label: 'LISAKOULUTUS_OPISTO', value: 'false' },
      { label: 'LISAKOULUTUS_TELMA', value: 'false' },
      // Perusopetuksen oppiaineet
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

  test('muokkaa kentän arvoa onnistuneesti', async ({ page }) => {
    let savedYliajoData: unknown = null;
    await page.route('**/ui/tallennayliajot', async (route) => {
      savedYliajoData = route.request().postDataJSON();
      await route.fulfill({
        status: 200,
        json: {},
      });
    });

    await page.getByRole('button', { name: 'Uudet avainarvot' }).click();

    const tiedot = page.getByRole('region', {
      name: 'Suorituspalvelusta opiskelijavalintaan siirtyvät tiedot',
    });

    await tiedot
      .getByRole('button', { name: 'Muokkaa kenttää perusopetuksen_kieli' })
      .click();

    const editModal = page.getByRole('dialog', {
      name: 'Muokkaa kenttää',
    });
    await expect(editModal).toBeVisible();

    const arvoInput = editModal.getByLabel('perusopetuksen_kieli');
    await expect(arvoInput).toHaveValue('FI');

    await arvoInput.fill('SV');

    const seliteInput = editModal.getByLabel('Selite');
    await seliteInput.fill('Muutettu testissä');

    await Promise.all([
      page.waitForRequest('**/ui/tallennayliajot'),
      page.waitForRequest((request) =>
        request.url().includes(`/ui/valintadata?oppijaNumero=${OPPIJANUMERO}`),
      ),
      editModal.getByRole('button', { name: 'Tallenna' }).click(),
    ]);

    expect(savedYliajoData).toMatchObject({
      henkiloOid: OPPIJANUMERO,
      hakuOid: '1.2.246.562.29.00000000000000000000',
      yliajot: [
        {
          avain: 'perusopetuksen_kieli',
          arvo: 'SV',
          selite: 'Muutettu testissä',
        },
      ],
    });

    await expect(editModal).toBeHidden();
  });

  test('lisää uuden kentän onnistuneesti', async ({ page }) => {
    let savedYliajoData: unknown = null;
    await page.route('**/ui/tallennayliajot', async (route) => {
      savedYliajoData = route.request().postDataJSON();
      await route.fulfill({
        status: 200,
        json: {},
      });
    });

    await page.getByRole('button', { name: 'Uudet avainarvot' }).click();

    await page.getByRole('button', { name: 'Lisää kenttä' }).click();

    const addModal = page.getByRole('dialog', {
      name: 'Lisää kenttä',
    });
    await expect(addModal).toBeVisible();

    await addModal.getByLabel('Avain').fill('uusi_kentta');
    await addModal.getByLabel('Arvo').fill('testiArvo');
    await addModal.getByLabel('Selite').fill('Lisätty testissä');

    await Promise.all([
      page.waitForRequest('**/ui/tallennayliajot'),
      page.waitForRequest((request) =>
        request.url().includes(`/ui/valintadata?oppijaNumero=${OPPIJANUMERO}`),
      ),
      addModal.getByRole('button', { name: 'Tallenna' }).click(),
    ]);

    expect(savedYliajoData).toMatchObject({
      henkiloOid: OPPIJANUMERO,
      hakuOid: '1.2.246.562.29.00000000000000000000',
      yliajot: [
        {
          avain: 'uusi_kentta',
          arvo: 'testiArvo',
          selite: 'Lisätty testissä',
        },
      ],
    });

    await expect(addModal).toBeHidden();
  });

  test('peruuttaa kentän muokkauksen', async ({ page }) => {
    await page.getByRole('button', { name: 'Uudet avainarvot' }).click();

    const tiedot = page.getByRole('region', {
      name: 'Suorituspalvelusta opiskelijavalintaan siirtyvät tiedot',
    });

    await tiedot
      .getByRole('button', { name: 'Muokkaa kenttää perusopetuksen_kieli' })
      .click();

    const editModal = page.getByRole('dialog', {
      name: 'Muokkaa kenttää',
    });
    await expect(editModal).toBeVisible();

    await editModal.getByRole('button', { name: 'Peruuta' }).click();

    await expect(editModal).toBeHidden();
  });

  test('näyttää virheilmoituksen, jos tallennus epäonnistuu', async ({
    page,
  }) => {
    await page.route('**/ui/tallennayliajot', async (route) => {
      await route.fulfill({
        status: 400,
        json: {
          virheAvaimet: ['backend-virhe.arvo.ei_validi'],
        },
      });
    });

    await page.getByRole('button', { name: 'Uudet avainarvot' }).click();

    const tiedot = page.getByRole('region', {
      name: 'Suorituspalvelusta opiskelijavalintaan siirtyvät tiedot',
    });

    await tiedot
      .getByRole('button', { name: 'Muokkaa kenttää perusopetuksen_kieli' })
      .click();

    const editModal = page.getByRole('dialog', {
      name: 'Muokkaa kenttää',
    });
    await expect(editModal).toBeVisible();

    await Promise.all([
      page.waitForRequest('**/ui/tallennayliajot'),
      editModal.getByRole('button', { name: 'Tallenna' }).click(),
    ]);

    await expect(editModal).toBeHidden();

    const errorModal = page.getByRole('dialog', {
      name: 'Kentän tallentaminen epäonnistui',
    });
    await expect(errorModal).toBeVisible();
    await expect(errorModal).toContainText('Arvo ei ole validi');

    await errorModal.getByRole('button', { name: 'Sulje' }).click();
    await expect(errorModal).toBeHidden();
  });
});
