import { type Page } from '@playwright/test';
import { test, expect } from './lib/fixtures';
import OPPIJAN_TIEDOT from './fixtures/oppijanTiedot.json' with { type: 'json' };
import OPPILAITOKSET from './fixtures/oppilaitokset.json' with { type: 'json' };
import OPPIJAT from './fixtures/oppilaitoksenOppijat.json' with { type: 'json' };
import VALINTA_DATA from './fixtures/valintaData.json' with { type: 'json' };
import { selectOption } from './lib/playwrightUtils';

const OPPIJANUMERO = OPPIJAN_TIEDOT.oppijaNumero;
const OPPILAITOS_OID = OPPILAITOKSET.oppilaitokset[0]?.oid ?? '';
const OPPILAITOS_NIMI = OPPILAITOKSET.oppilaitokset[0]?.nimi.fi ?? '';

const getOppilaitosSelect = (page: Page) => page.getByLabel('Oppilaitos');

const getVuosiSelect = (page: Page) => page.getByLabel('Valmistumisvuosi');

const getLuokkaSelect = (page: Page) => page.getByLabel('Luokka');

const getSuodatusInput = (page: Page) =>
  page.getByPlaceholder('Suodata nimellä tai hetulla');

const getHenkilotSidebar = (page: Page) => page.getByTestId('henkilot-sidebar');

test.describe('Tarkistusnäkymä', () => {
  test.beforeEach(async ({ page }) => {
    await page.clock.setFixedTime(new Date('2025-01-01T12:00:00Z'));

    await page.route('**/ui/tiedot', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          json: OPPIJAN_TIEDOT,
        });
      }
    });

    await page.route(`**/ui/rajain/oppilaitokset`, async (route) => {
      await route.fulfill({
        json: OPPILAITOKSET,
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

    await page.route(`**/ui/rajain/oppilaitoksenoppijat?*`, async (route) => {
      const url = new URL(route.request().url());
      const oppilaitos = url.searchParams.get('oppilaitos');
      const vuosi = url.searchParams.get('vuosi');

      if (oppilaitos && vuosi) {
        await route.fulfill({
          json: OPPIJAT,
        });
      } else {
        await route.fulfill({
          json: { oppijat: [] },
        });
      }
    });

    await page.route(`**/ui/rajain/vuodet/**`, async (route) => {
      await route.fulfill({
        json: { vuodet: ['2024', '2023'] },
      });
    });

    await page.route(`**/ui/rajain/luokat/**`, async (route) => {
      await route.fulfill({
        json: { luokat: ['9A', '9B', '9C'] },
      });
    });

    await page.goto('tarkistus');
  });

  test('näyttää oikean otsikon ja sivupainikkeen', async ({ page }) => {
    await expect(page).toHaveTitle('Suorituspalvelu');
    await expect(
      page.getByRole('heading', { name: 'Suorituspalvelu' }),
    ).toBeVisible();

    await expect(page.getByRole('link', { name: 'Henkilöhaku' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Tarkistus' })).toBeVisible();
  });

  test('näyttää oppilaitoksen valintakentän', async ({ page }) => {
    const oppilaitosSelect = getOppilaitosSelect(page);
    await expect(oppilaitosSelect).toBeVisible();
    await expect(oppilaitosSelect).toBeEnabled();
  });

  test('valitsee automaattisesti ainoan oppilaitoksen', async ({ page }) => {
    await page.route(`**/ui/rajain/oppilaitokset`, async (route) => {
      await route.fulfill({
        json: {
          oppilaitokset: [OPPILAITOKSET.oppilaitokset[0]],
        },
      });
    });

    await page.reload();

    await expect(page).toHaveURL(
      (url) => url.searchParams.get('oppilaitos') === OPPILAITOS_OID,
    );
  });

  test('vuosi- ja luokkavalinnat on disabloitu ilman oppilaitosvalintaa', async ({
    page,
  }) => {
    await page.route(`**/ui/rajain/oppilaitokset`, async (route) => {
      await route.fulfill({
        json: OPPILAITOKSET,
      });
    });

    await page.reload();

    const vuosiSelect = getVuosiSelect(page);
    const luokkaSelect = getLuokkaSelect(page);

    await expect(vuosiSelect).toBeDisabled();
    await expect(luokkaSelect).toBeDisabled();
  });

  test('oppilaitoksen valinta lataa oppijoiden listan', async ({ page }) => {
    await selectOption({
      name: 'Oppilaitos',
      page,
      option: OPPILAITOS_NIMI,
    });

    // Odota että vuosi valitaan automaattisesti
    await expect(page).toHaveURL(
      (url) =>
        url.searchParams.get('oppilaitos') === OPPILAITOS_OID &&
        url.searchParams.get('vuosi') === '2025',
    );

    const henkilotSidebar = getHenkilotSidebar(page);

    await expect(henkilotSidebar.getByText('3 henkilöä')).toBeVisible();
    await expect(
      henkilotSidebar.getByRole('link', { name: 'Olli Oppija' }),
    ).toBeVisible();
    await expect(
      henkilotSidebar.getByRole('link', { name: 'Matti Meikäläinen' }),
    ).toBeVisible();
    await expect(
      henkilotSidebar.getByRole('link', { name: 'Maija Mallikas' }),
    ).toBeVisible();
  });

  test('vuoden vaihtaminen päivittää oppijoiden listan', async ({ page }) => {
    await selectOption({
      name: 'Oppilaitos',
      page,
      option: OPPILAITOS_NIMI,
    });

    await expect(page.getByText('3 henkilöä')).toBeVisible();

    await selectOption({
      page,
      name: 'Valmistumisvuosi',
      option: '2024',
    });

    await expect(page).toHaveURL(
      (url) => url.searchParams.get('vuosi') === '2024',
    );
  });

  test('suodatuskenttä suodattaa oppijoita nimellä', async ({ page }) => {
    await selectOption({
      name: 'Oppilaitos',
      page,
      option: OPPILAITOS_NIMI,
    });

    const henkilotSidebar = getHenkilotSidebar(page);

    await expect(henkilotSidebar.getByText('3 henkilöä')).toBeVisible();

    const suodatusInput = getSuodatusInput(page);

    await suodatusInput.fill('Olli');

    await expect(page).toHaveURL(
      (url) => url.searchParams.get('suodatus') === 'Olli',
    );

    await expect(henkilotSidebar.getByText('1 henkilö')).toBeVisible();
    await expect(
      henkilotSidebar.getByRole('link', { name: 'Olli Oppija' }),
    ).toBeVisible();
  });

  test('suodatuskenttä tyhjentää suodatuksen', async ({ page }) => {
    await selectOption({ page, name: 'Oppilaitos', option: OPPILAITOS_NIMI });

    const henkilotSidebar = getHenkilotSidebar(page);
    await expect(henkilotSidebar.getByText('3 henkilöä')).toBeVisible();

    const suodatusInput = getSuodatusInput(page);

    await suodatusInput.fill('Olli');
    await expect(page).toHaveURL(
      (url) => url.searchParams.get('suodatus') === 'Olli',
    );

    await suodatusInput.clear();
    await expect(page).toHaveURL((url) => {
      return url.searchParams.get('suodatus') === null;
    });
  });

  test('oppijan valinta näyttää oppijan tiedot', async ({ page }) => {
    await selectOption({
      name: 'Oppilaitos',
      page,
      option: OPPILAITOS_NIMI,
    });

    const sidebar = getHenkilotSidebar(page);
    await expect(sidebar.getByText('3 henkilöä')).toBeVisible();

    await page.getByRole('link', { name: 'Olli Oppija 010296-1230' }).click();

    await expect(page).toHaveURL((url) =>
      url.pathname.includes(`tarkistus/${OPPIJANUMERO}`),
    );

    await expect(
      page.getByRole('heading', { name: 'Olli Oppija (010296-1230)' }),
    ).toBeVisible();
  });

  test('näyttää placeholder-tekstin ilman valittua oppijaa', async ({
    page,
  }) => {
    await expect(page.getByText('Hae ja valitse henkilö')).toBeVisible();
  });

  test('säilyttää hakuparametrit navigoitaessa oppijan tietoihin', async ({
    page,
  }) => {
    await selectOption({ name: 'Oppilaitos', page, option: OPPILAITOS_NIMI });
    await selectOption({ page, name: 'Luokka', option: '9A' });

    const sidebar = getHenkilotSidebar(page);
    await expect(sidebar.getByText('3 henkilöä')).toBeVisible();

    await sidebar
      .getByRole('link', { name: 'Olli Oppija 010296-1230' })
      .click();

    await expect(page).toHaveURL(
      (url) =>
        url.searchParams.get('oppilaitos') === OPPILAITOS_OID &&
        url.searchParams.get('vuosi') === '2025' &&
        url.searchParams.get('luokka') === '9A',
    );
  });

  test('oppilaitoksen vaihtaminen tyhjentää luokan ja suodatuksen', async ({
    page,
  }) => {
    await selectOption({
      name: 'Oppilaitos',
      page,
      option: OPPILAITOS_NIMI,
    });

    await expect(
      getHenkilotSidebar(page).getByText('3 henkilöä'),
    ).toBeVisible();

    await selectOption({ page, name: 'Luokka', option: '9A' });
    const suodatusInput = getSuodatusInput(page);
    await suodatusInput.fill('Olli');

    await selectOption({
      name: 'Oppilaitos',
      page,
      option: 'Tampereen normaalikoulu',
    });

    await expect(page).toHaveURL(
      (url) =>
        url.searchParams.get('luokka') === null &&
        url.searchParams.get('suodatus') === null,
    );
  });

  test('vuoden vaihtaminen tyhjentää luokan ja suodatuksen', async ({
    page,
  }) => {
    await selectOption({ name: 'Oppilaitos', page, option: OPPILAITOS_NIMI });

    await expect(page.getByText('3 henkilöä')).toBeVisible();

    await selectOption({ page, name: 'Luokka', option: '9A' });

    const suodatusInput = getSuodatusInput(page);
    await suodatusInput.fill('Olli');

    await selectOption({
      page,
      name: 'Valmistumisvuosi',
      option: '2024',
    });

    await expect(page).toHaveURL(
      (url) =>
        url.searchParams.get('luokka') === null &&
        url.searchParams.get('suodatus') === null,
    );
  });
});
