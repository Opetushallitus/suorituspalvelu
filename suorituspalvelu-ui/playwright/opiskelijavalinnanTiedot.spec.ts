import { test, expect } from './lib/fixtures';
import { expectLabeledValues, selectOption } from './lib/playwrightUtils';
import OPPIJAN_TIEDOT from './fixtures/oppijanTiedot.json' with { type: 'json' };
import VALINTA_DATA from './fixtures/valintaData.json' with { type: 'json' };
import OPPIJAN_HAUT from './fixtures/oppijanHaut.json' with { type: 'json' };
import VALINTA_DATA_FOR_SECOND_HAKU from './fixtures/valintaDataForSecondHaku.json' with { type: 'json' };
import type { IHaku } from '@/types/backend';

const OPPIJANUMERO = OPPIJAN_TIEDOT.oppijaNumero;
const FIRST_HAKU = OPPIJAN_HAUT?.haut?.[0] as IHaku;
const SECOND_HAKU = OPPIJAN_HAUT?.haut?.[1] as IHaku;

test.describe('Opiskelijavalinnan tiedot', () => {
  test.beforeEach(async ({ page }) => {
    await page.clock.setFixedTime(new Date('2025-01-01T12:00:00Z'));

    await page.route('**/ui/tiedot', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          json: OPPIJAN_TIEDOT,
        });
      }
    });

    await page.route(
      (url) => url.href.includes(`/ui/oppijanhaut/${OPPIJANUMERO}`),
      async (route) => {
        await route.fulfill({
          json: {
            haut: [FIRST_HAKU], // Default to single haku for existing tests
          },
        });
      },
    );

    await page.route(
      (url) =>
        url.pathname.includes('/ui/valintadata') &&
        url.searchParams.get('oppijaNumero') === OPPIJANUMERO,
      (route, request) => {
        const url = new URL(request.url());
        const hakuParam = url.searchParams.get('hakuOid');
        if (hakuParam === FIRST_HAKU.hakuOid) {
          return route.fulfill({
            json: VALINTA_DATA,
          });
        } else if (hakuParam === SECOND_HAKU.hakuOid) {
          return route.fulfill({
            json: VALINTA_DATA_FOR_SECOND_HAKU,
          });
        }
      },
    );
  });

  test('näyttää uudet avainarvot', async ({ page }) => {
    await page.goto(
      `/suorituspalvelu/henkilo/${OPPIJANUMERO}/opiskelijavalinnan-tiedot`,
    );
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
    await page.goto(
      `/suorituspalvelu/henkilo/${OPPIJANUMERO}/opiskelijavalinnan-tiedot`,
    );
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
    await page.goto(
      `/suorituspalvelu/henkilo/${OPPIJANUMERO}/opiskelijavalinnan-tiedot`,
    );
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
      hakuOid: FIRST_HAKU.hakuOid,
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
    await page.goto(
      `/suorituspalvelu/henkilo/${OPPIJANUMERO}/opiskelijavalinnan-tiedot`,
    );
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
      hakuOid: FIRST_HAKU.hakuOid,
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
    await page.goto(
      `/suorituspalvelu/henkilo/${OPPIJANUMERO}/opiskelijavalinnan-tiedot`,
    );
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

    await page.goto(
      `/suorituspalvelu/henkilo/${OPPIJANUMERO}/opiskelijavalinnan-tiedot`,
    );

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

  test('poistaa muokkauksen onnistuneesti', async ({ page }) => {
    let deletedYliajoParams: {
      oppijaNumero?: string;
      hakuOid?: string;
      avain?: string;
    } = {};
    await page.route('**/ui/poistayliajo*', async (route) => {
      const url = new URL(route.request().url());
      deletedYliajoParams = {
        oppijaNumero: url.searchParams.get('oppijaNumero') || undefined,
        hakuOid: url.searchParams.get('hakuOid') || undefined,
        avain: url.searchParams.get('avain') || undefined,
      };
      await route.fulfill({
        status: 200,
        json: {},
      });
    });

    await page.route('**/ui/valintadata*', async (route) => {
      await route.fulfill({
        status: 200,
        json: {
          avainArvot: [
            {
              avain: 'perusopetuksen_kieli',
              arvo: 'SV',
              metadata: {
                arvoEnnenYliajoa: 'FI',
                duplikaatti: false,
                yliajo: {
                  avain: 'perusopetuksen_kieli',
                  arvo: '10',
                  selite: '',
                  henkiloOid: OPPIJANUMERO,
                  virkailijaOid: '1.2.246.562.24.00000000001',
                },
              },
            },
          ],
        },
      });
    });

    await page.goto(
      `/suorituspalvelu/henkilo/${OPPIJANUMERO}/opiskelijavalinnan-tiedot`,
    );

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
      page.waitForRequest('**/ui/poistayliajo*'),
      page.waitForRequest((request) =>
        request.url().includes(`/ui/valintadata?oppijaNumero=${OPPIJANUMERO}`),
      ),
      editModal.getByRole('button', { name: 'Poista muokkaus' }).click(),
    ]);

    expect(deletedYliajoParams.oppijaNumero).toBe(OPPIJANUMERO);
    expect(deletedYliajoParams.hakuOid).toBe(FIRST_HAKU.hakuOid);
    expect(deletedYliajoParams.avain).toBe('perusopetuksen_kieli');

    await expect(editModal).toBeHidden();
  });

  test('poistaa lisätyn kentän onnistuneesti', async ({ page }) => {
    let deletedYliajoParams: {
      oppijaNumero?: string;
      hakuOid?: string;
      avain?: string;
    } = {};
    await page.route('**/ui/poistayliajo*', async (route) => {
      const url = new URL(route.request().url());
      deletedYliajoParams = {
        oppijaNumero: url.searchParams.get('oppijaNumero') || undefined,
        hakuOid: url.searchParams.get('hakuOid') || undefined,
        avain: url.searchParams.get('avain') || undefined,
      };
      await route.fulfill({
        status: 200,
        json: {},
      });
    });

    await page.route('**/ui/valintadata*', async (route) => {
      await route.fulfill({
        status: 200,
        json: {
          ...VALINTA_DATA,
          avainArvot: [
            ...VALINTA_DATA.avainArvot,
            {
              avain: 'uusi_kentta',
              arvo: 'testiArvo',
              metadata: {
                arvoEnnenYliajoa: null,
                duplikaatti: false,
                yliajo: {
                  avain: 'uusi_kentta',
                  arvo: 'testiArvo',
                  selite: 'Lisätty testissä',
                },
              },
            },
          ],
        },
      });
    });

    await page.goto(
      `/suorituspalvelu/henkilo/${OPPIJANUMERO}/opiskelijavalinnan-tiedot`,
    );

    await page.getByRole('button', { name: 'Uudet avainarvot' }).click();

    const tiedot = page.getByRole('region', {
      name: 'Suorituspalvelusta opiskelijavalintaan siirtyvät tiedot',
    });

    await tiedot
      .getByRole('button', { name: 'Muokkaa kenttää uusi_kentta' })
      .click();

    const editModal = page.getByRole('dialog', {
      name: 'Muokkaa kenttää',
    });
    await expect(editModal).toBeVisible();

    await Promise.all([
      page.waitForRequest('**/ui/poistayliajo*'),
      page.waitForRequest((request) =>
        request.url().includes(`/ui/valintadata?oppijaNumero=${OPPIJANUMERO}`),
      ),
      editModal.getByRole('button', { name: 'Poista kenttä' }).click(),
    ]);

    expect(deletedYliajoParams.oppijaNumero).toBe(OPPIJANUMERO);
    expect(deletedYliajoParams.avain).toBe('uusi_kentta');

    await expect(editModal).toBeHidden();
  });

  test('näyttää virheilmoituksen, jos lisätyn kentän poisto epäonnistuu', async ({
    page,
  }) => {
    await page.route('**/ui/poistayliajo*', async (route) => {
      await route.fulfill({
        status: 500,
        body: 'Backend-virhe',
      });
    });

    await page.route('**/ui/valintadata*', async (route) => {
      await route.fulfill({
        status: 200,
        json: {
          ...VALINTA_DATA,
          avainArvot: [
            ...VALINTA_DATA.avainArvot,
            {
              avain: 'uusi_kentta',
              arvo: 'testiArvo',
              metadata: {
                arvoEnnenYliajoa: null,
                duplikaatti: false,
                yliajo: {
                  avain: 'uusi_kentta',
                  arvo: 'testiArvo',
                  selite: 'Lisätty testissä',
                },
              },
            },
          ],
        },
      });
    });

    await page.goto(
      `/suorituspalvelu/henkilo/${OPPIJANUMERO}/opiskelijavalinnan-tiedot`,
    );

    await page.getByRole('button', { name: 'Uudet avainarvot' }).click();

    const tiedot = page.getByRole('region', {
      name: 'Suorituspalvelusta opiskelijavalintaan siirtyvät tiedot',
    });

    await tiedot
      .getByRole('button', { name: 'Muokkaa kenttää uusi_kentta' })
      .click();

    const editModal = page.getByRole('dialog', {
      name: 'Muokkaa kenttää',
    });
    await expect(editModal).toBeVisible();

    await Promise.all([
      page.waitForRequest('**/ui/poistayliajo*'),
      editModal.getByRole('button', { name: 'Poista kenttä' }).click(),
    ]);

    await expect(editModal).toBeHidden();

    const errorModal = page.getByRole('dialog', {
      name: 'Kentän muokkauksen poistaminen epäonnistui',
    });
    await expect(errorModal).toBeVisible();
    await expect(errorModal).toContainText('Backend-virhe');

    await errorModal.getByRole('button', { name: 'Sulje' }).click();
    await expect(errorModal).toBeHidden();
  });

  test('hakua voi vaihtaa, jolloin näytetään valitun haun avainarvot', async ({
    page,
  }) => {
    await page.route(
      (url) => url.href.includes(`/ui/oppijanhaut/${OPPIJANUMERO}`),
      async (route) => {
        await route.fulfill({
          json: OPPIJAN_HAUT,
        });
      },
    );

    await page.goto(
      `/suorituspalvelu/henkilo/${OPPIJANUMERO}/opiskelijavalinnan-tiedot`,
    );

    // Ei hakua valittuna oletuksena
    const hakuDropdown = page.getByLabel('Haku, jota tiedot koskevat');
    await expect(hakuDropdown).toHaveText('');

    await selectOption({
      page,
      name: 'Haku, jota tiedot koskevat',
      option: 'Yhteishaku ammatilliseen koulutukseen',
    });

    const tiedot = page.getByRole('region', {
      name: 'Suorituspalvelusta opiskelijavalintaan siirtyvät tiedot',
    });
    await expect(tiedot).toBeVisible();

    await expectLabeledValues(tiedot, [
      { label: 'perusopetuksen_kieli', value: 'FI' },
      { label: 'lukio_suoritettu', value: 'false' },
    ]);

    await selectOption({
      page,
      name: 'Haku, jota tiedot koskevat',
      option: 'Lukion kevään 2025 yhteishaku',
    });

    await expectLabeledValues(tiedot, [
      { label: 'perusopetuksen_kieli', value: 'SV' },
      { label: 'lukio_suoritettu', value: 'true' },
    ]);

    expect(page.url()).toContain(`haku=${SECOND_HAKU.hakuOid}`);
  });

  test('kun on vain yksi haku, valitaan se automaattisesti', async ({
    page,
  }) => {
    await page.goto(
      `/suorituspalvelu/henkilo/${OPPIJANUMERO}/opiskelijavalinnan-tiedot`,
    );

    await page.waitForURL(
      (url) =>
        url.pathname.includes('opiskelijavalinnan-tiedot') &&
        url.searchParams.get('haku') === FIRST_HAKU.hakuOid,
    );

    const tiedot = page.getByRole('region', {
      name: 'Suorituspalvelusta opiskelijavalintaan siirtyvät tiedot',
    });
    await expect(tiedot).toBeVisible();

    await expectLabeledValues(tiedot, [
      { label: 'perusopetuksen_kieli', value: 'FI' },
    ]);
  });

  test('valitaan URL-parametrin mukainen haku', async ({ page }) => {
    // Mock multiple haut
    await page.route(
      (url) => url.href.includes(`/ui/oppijanhaut/${OPPIJANUMERO}`),
      async (route) => {
        await route.fulfill({
          json: OPPIJAN_HAUT,
        });
      },
    );

    await page.goto(
      `/suorituspalvelu/henkilo/${OPPIJANUMERO}/opiskelijavalinnan-tiedot?haku=${SECOND_HAKU.hakuOid}`,
    );

    const tiedot = page.getByRole('region', {
      name: 'Suorituspalvelusta opiskelijavalintaan siirtyvät tiedot',
    });

    const hakuDropdown = page.getByLabel('Haku, jota tiedot koskevat');
    await expect(hakuDropdown).toHaveText('Lukion kevään 2025 yhteishaku');

    await expectLabeledValues(tiedot, [
      { label: 'perusopetuksen_kieli', value: 'SV' },
    ]);
  });

  test('näytetään virhe modaalissa, jos URL-parametrin haku-oid ei valittavissa', async ({
    page,
  }) => {
    await page.goto(
      `/suorituspalvelu/henkilo/${OPPIJANUMERO}/opiskelijavalinnan-tiedot?haku=${SECOND_HAKU.hakuOid}`,
    );

    const tiedot = page.getByRole('region', {
      name: 'Suorituspalvelusta opiskelijavalintaan siirtyvät tiedot',
    });

    await expect(tiedot).toBeHidden();
    await expect(page.getByLabel('Haku, jota tiedot koskevat')).toHaveText(
      SECOND_HAKU.hakuOid,
    );
    await expect(page.getByText('Valittu haku on virheellinen')).toBeVisible();
  });

  test('näyttää hakemukselta tulevat tiedot oikein', async ({ page }) => {
    await page.goto(
      `/suorituspalvelu/henkilo/${OPPIJANUMERO}/opiskelijavalinnan-tiedot`,
    );
    await page
      .getByRole('button', {
        name: 'Hakemuksesta opiskelijavalintaan tulevat tiedot',
      })
      .click();

    const hakemuksestaTulevatTiedot = page.getByRole('region', {
      name: 'Hakemuksesta opiskelijavalintaan tulevat tiedot',
    });
    await expect(hakemuksestaTulevatTiedot).toBeVisible();

    await expectLabeledValues(hakemuksestaTulevatTiedot, [
      {
        label: 'hakemuksesta_test',
        value: '123',
      },
    ]);
  });

  test('ei näytä hakemukselta tulevia tietoja, jos niitä ei ole', async ({
    page,
  }) => {
    // Override the route to return data without hakemus fields
    await page.route(
      (url) =>
        url.pathname.includes('/ui/valintadata') &&
        url.searchParams.get('oppijaNumero') === OPPIJANUMERO,
      (route) => {
        return route.fulfill({
          json: {
            avainArvot: VALINTA_DATA.avainArvot.filter(
              (item) => !item.metadata.arvoOnHakemukselta,
            ),
          },
        });
      },
    );

    await page.goto(
      `/suorituspalvelu/henkilo/${OPPIJANUMERO}/opiskelijavalinnan-tiedot`,
    );

    const hakemukseltaTiedotButton = page.getByRole('button', {
      name: 'Hakemuksesta opiskelijavalintaan tulevat tiedot',
    });
    await expect(hakemukseltaTiedotButton).toBeHidden();
  });
});
