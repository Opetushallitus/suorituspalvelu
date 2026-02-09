import { type Page } from '@playwright/test';
import { test, expect } from './lib/fixtures';
import OPPIJAN_TIEDOT from './fixtures/oppijanTiedot.json' with { type: 'json' };
import OPPILAITOKSET from './fixtures/oppilaitokset.json' with { type: 'json' };
import OPPIJAT from './fixtures/oppilaitoksenOppijat.json' with { type: 'json' };
import VALINTA_DATA from './fixtures/valintaData.json' with { type: 'json' };
import { selectOption, stubKayttajaResponse } from './lib/playwrightUtils';

const OPPIJANUMERO = OPPIJAN_TIEDOT.oppijaNumero;

const HETU = OPPIJAN_TIEDOT.henkiloTunnus;
const OPPILAITOS_OID = OPPILAITOKSET.oppilaitokset[0]?.oid ?? '';
const OPPILAITOS_NIMI = OPPILAITOKSET.oppilaitokset[0]?.nimi.fi ?? '';

const getOppilaitosSelect = (page: Page) => page.getByLabel('Oppilaitos');

const getVuosiSelect = (page: Page) => page.getByLabel('Valmistumisvuosi');

const getLuokkaSelect = (page: Page) => page.getByLabel('Luokka');

const getHenkilotSidebar = (page: Page) => page.getByTestId('henkilot-sidebar');

const getSuodatusInput = (page: Page) =>
  getHenkilotSidebar(page).getByPlaceholder('Suodata nimellä tai hetulla');

test.describe('Tarkastusnäkymä', () => {
  test.beforeEach(async ({ page }) => {
    await page.clock.setFixedTime(new Date('2025-01-01T12:00:00Z'));

    await stubKayttajaResponse(page, {
      isOrganisaationKatselija: true,
      isRekisterinpitaja: false,
    });

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
      const url = new URL(route.request().url());
      const oppilaitosOid = url.pathname.split('/').pop();

      // Toisella oppilaitoksella vähemmän vuosia
      if (oppilaitosOid === OPPILAITOS_OID) {
        await route.fulfill({ json: { vuodet: ['2024', '2023', '2022'] } });
      } else {
        await route.fulfill({ json: { vuodet: ['2024', '2023'] } });
      }
    });

    await page.route(`**/ui/rajain/luokat/**`, async (route) => {
      await route.fulfill({
        json: { luokat: ['9A', '9B', '9C'] },
      });
    });
  });

  test('valitsee automaattisesti ainoan oppilaitoksen', async ({ page }) => {
    await page.route(`**/ui/rajain/oppilaitokset`, async (route) => {
      await route.fulfill({
        json: {
          oppilaitokset: [OPPILAITOKSET.oppilaitokset[0]],
        },
      });
    });
    await page.goto('tarkastus');

    await expect(page).toHaveURL(
      (url) => url.searchParams.get('oppilaitos') === OPPILAITOS_OID,
    );
    await expect(getOppilaitosSelect(page)).toHaveText(OPPILAITOS_NIMI);
  });

  test('vuosi- ja luokkavalinnat on disabloitu ilman oppilaitosvalintaa', async ({
    page,
  }) => {
    await page.route(`**/ui/rajain/oppilaitokset`, async (route) => {
      await route.fulfill({
        json: OPPILAITOKSET,
      });
    });

    await page.goto('tarkastus');

    const vuosiSelect = getVuosiSelect(page);
    const luokkaSelect = getLuokkaSelect(page);

    await expect(vuosiSelect).toBeDisabled();
    await expect(luokkaSelect).toBeDisabled();
  });

  test('oppilaitoksen valinta lataa oppijoiden listan', async ({ page }) => {
    await page.goto('tarkastus');
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

    await expect(henkilotSidebar.getByText('4 henkilöä')).toBeVisible();

    const navigationLinks = henkilotSidebar
      .getByRole('navigation', { name: 'Henkilövalitsin' })
      .getByRole('link');

    await expect(navigationLinks.nth(0)).toHaveText(
      'Maija Mallikas 030498-1232 9A',
    );
    await expect(navigationLinks.nth(0)).toHaveAccessibleName(
      'Maija Mallikas, henkilötunnus: 030498-1232, luokka: 9A',
    );
    await expect(navigationLinks.nth(1)).toHaveText(
      'Matti Meikäläinen 020397-1231 9B',
    );
    await expect(navigationLinks.nth(1)).toHaveAccessibleName(
      'Matti Meikäläinen, henkilötunnus: 020397-1231, luokka: 9B',
    );
    await expect(navigationLinks.nth(2)).toHaveText(
      'Matti Myöhäinen 030397-2342 9B',
    );
    await expect(navigationLinks.nth(2)).toHaveAccessibleName(
      'Matti Myöhäinen, henkilötunnus: 030397-2342, luokka: 9B',
    );
    await expect(navigationLinks.nth(3)).toHaveText('Olli Oppija 010296-1230');
    await expect(navigationLinks.nth(3)).toHaveAccessibleName(
      'Olli Oppija, henkilötunnus: 010296-1230',
    );
  });

  test('vuoden vaihtaminen päivittää oppijoiden listan', async ({ page }) => {
    await page.goto('tarkastus');
    await selectOption({
      name: 'Oppilaitos',
      page,
      option: OPPILAITOS_NIMI,
    });

    await expect(page.getByText('4 henkilöä')).toBeVisible();

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
    await page.goto('tarkastus');

    await selectOption({
      name: 'Oppilaitos',
      page,
      option: OPPILAITOS_NIMI,
    });

    const henkilotSidebar = getHenkilotSidebar(page);

    await expect(henkilotSidebar.getByText('4 henkilöä')).toBeVisible();

    const suodatusInput = getSuodatusInput(page);

    await suodatusInput.fill('Olli');

    await expect(henkilotSidebar.getByText('1 henkilö')).toBeVisible();
    await expect(
      henkilotSidebar.getByRole('link', { name: 'Olli Oppija' }),
    ).toBeVisible();
  });

  test('suodatuskenttä tyhjentää suodatuksen', async ({ page }) => {
    await page.goto('tarkastus');

    await selectOption({ page, name: 'Oppilaitos', option: OPPILAITOS_NIMI });

    const henkilotSidebar = getHenkilotSidebar(page);
    const navigationLinks = henkilotSidebar
      .getByRole('navigation', { name: 'Henkilövalitsin' })
      .getByRole('link');

    await expect(henkilotSidebar.getByText('4 henkilöä')).toBeVisible();
    await expect(navigationLinks).toHaveCount(4);

    const suodatusInput = getSuodatusInput(page);

    await suodatusInput.fill('Olli');
    await expect(henkilotSidebar.getByText('1 henkilö')).toBeVisible();
    await expect(navigationLinks).toHaveCount(1);
    await expect(navigationLinks.nth(0)).toHaveText('Olli Oppija 010296-1230');

    await suodatusInput.clear();

    await expect(henkilotSidebar.getByText('4 henkilöä')).toBeVisible();
    await expect(navigationLinks).toHaveCount(4);
  });

  test('oppijan valinta näyttää oppijan tiedot', async ({ page }) => {
    await page.goto('tarkastus');

    await selectOption({
      name: 'Oppilaitos',
      page,
      option: OPPILAITOS_NIMI,
    });

    const sidebar = getHenkilotSidebar(page);
    await expect(sidebar.getByText('4 henkilöä')).toBeVisible();

    await page
      .getByRole('link', { name: 'Olli Oppija, henkilötunnus: 010296-1230' })
      .click();

    await expect(page).toHaveURL((url) =>
      url.pathname.includes(`tarkastus/${OPPIJANUMERO}`),
    );

    await expect(
      page.getByRole('heading', { name: 'Olli Oppija (010296-1230)' }),
    ).toBeVisible();
  });

  test('näyttää placeholder-tekstin ilman valittua oppijaa', async ({
    page,
  }) => {
    await page.goto('tarkastus');
    await expect(page.getByText('Hae ja valitse henkilö')).toBeVisible();
  });

  test('säilyttää hakuparametrit navigoitaessa oppijan tietoihin', async ({
    page,
  }) => {
    await page.goto('tarkastus');

    await selectOption({ name: 'Oppilaitos', page, option: OPPILAITOS_NIMI });
    await selectOption({ page, name: 'Luokka', option: '9A' });

    const sidebar = getHenkilotSidebar(page);
    await expect(sidebar.getByText('4 henkilöä')).toBeVisible();

    await sidebar
      .getByRole('link', { name: 'Olli Oppija, henkilötunnus: 010296-1230' })
      .click();

    await expect(page).toHaveURL(
      (url) =>
        url.searchParams.get('oppilaitos') === OPPILAITOS_OID &&
        url.searchParams.get('vuosi') === '2025' &&
        url.searchParams.get('luokka') === '9A',
    );
    await page.goto('tarkastus');
  });

  test('oppilaitoksen vaihtaminen tyhjentää luokan ja suodatuksen mutta säilyttää vuoden', async ({
    page,
  }) => {
    await page.goto('tarkastus');

    await selectOption({
      name: 'Oppilaitos',
      page,
      option: OPPILAITOS_NIMI,
    });

    await expect(
      getHenkilotSidebar(page).getByText('4 henkilöä'),
    ).toBeVisible();

    await selectOption({ page, name: 'Valmistumisvuosi', option: '2023' });
    await selectOption({ page, name: 'Luokka', option: '9A' });
    const suodatusInput = getSuodatusInput(page);
    await suodatusInput.fill('Olli');
    await expect(page.getByText('1 henkilö')).toBeVisible();

    await selectOption({
      name: 'Oppilaitos',
      page,
      option: 'Tampereen normaalikoulu',
    });

    await expect(page).toHaveURL(
      (url) =>
        url.searchParams.get('luokka') === null &&
        url.searchParams.get('vuosi') === '2023',
    );
    await expect(suodatusInput).toHaveValue('');
  });

  test('oppilaitoksen vaihtaminen resetoi vuoden jos valittu vuosi ei ole saatavilla', async ({
    page,
  }) => {
    await page.goto('tarkastus');

    await selectOption({
      name: 'Oppilaitos',
      page,
      option: OPPILAITOS_NIMI,
    });

    // Valitaan vuosi 2022
    await selectOption({ page, name: 'Valmistumisvuosi', option: '2022' });
    await expect(page).toHaveURL(
      (url) => url.searchParams.get('vuosi') === '2022',
    );

    // Vaihdetaan toiseen oppilaitokseen jolle vuosi 2022 ei tarjolla
    await selectOption({
      name: 'Oppilaitos',
      page,
      option: 'Tampereen normaalikoulu',
    });

    // Vuosivalinta resetoituu tähän vuoteen
    await expect(page).toHaveURL(
      (url) => url.searchParams.get('vuosi') === '2025',
    );
    await expect(getVuosiSelect(page)).toHaveText('2025');
  });

  test('vuoden vaihtaminen tyhjentää luokan ja suodatuksen', async ({
    page,
  }) => {
    await page.goto('tarkastus');

    await selectOption({ name: 'Oppilaitos', page, option: OPPILAITOS_NIMI });

    await expect(page.getByText('4 henkilöä')).toBeVisible();

    await selectOption({ page, name: 'Luokka', option: '9A' });

    const suodatusInput = getSuodatusInput(page);
    await suodatusInput.fill('Olli');
    await expect(page.getByText('1 henkilö')).toBeVisible();

    await selectOption({
      page,
      name: 'Valmistumisvuosi',
      option: '2024',
    });

    await expect(page).toHaveURL(
      (url) => url.searchParams.get('luokka') === null,
    );
    await expect(suodatusInput).toHaveValue('');
  });

  test('näytetään oppijan tiedot oppijanumerolla navigoitaessa', async ({
    page,
  }) => {
    await page.goto(`tarkastus/${OPPIJANUMERO}`);

    await expect(
      page.getByRole('heading', { name: 'Olli Oppija (010296-1230)' }),
    ).toBeVisible();
  });

  test('Näytetään virhe, jos henkiön tietoihin yritetään navigoida henkilötunnuksella', async ({
    page,
  }) => {
    await page.goto(`tarkastus/${HETU}`);

    await expect(
      page.getByText(`Tunniste ${HETU} ei ole oppijanumero.`),
    ).toBeVisible();

    await expect(page).toHaveURL((url) =>
      url.pathname.endsWith(`tarkastus/${HETU}`),
    );

    await expect(
      page.getByRole('heading', { name: 'Olli Oppija (010296-1230)' }),
    ).toBeHidden();
  });

  test('automaattiset valinnat eivät luo ylimääräisiä selainhistorian askeleita', async ({
    page,
  }) => {
    // Stub single oppilaitos to trigger auto-select
    await page.route(`**/ui/rajain/oppilaitokset`, async (route) => {
      await route.fulfill({
        json: {
          oppilaitokset: [OPPILAITOKSET.oppilaitokset[0]],
        },
      });
    });

    // navigoidaan henkilösivulle
    // eslint-disable-next-line playwright/no-networkidle
    await page.goto('henkilo', { waitUntil: 'networkidle' });
    await expect(page).toHaveURL((url) => url.pathname.endsWith('henkilo'));

    // navigoidaan tarkastusnäkymään jolloin useEffect säätää oppilaitoksen ja vuoden
    await page.goto('tarkastus');

    // odotetaan että valinnat ilmestyvät urliin
    await expect(page).toHaveURL(
      (url) =>
        url.searchParams.get('oppilaitos') === OPPILAITOS_OID &&
        url.searchParams.get('vuosi') === '2025',
    );

    // navigoidaan takaisiin ja varmistetaan ettei tullut ylimääräisiä askeleita
    await page.goBack();

    await expect(page).toHaveURL((url) => url.pathname.endsWith('henkilo'));
  });

  test('oppilaitoksen vaihtaminen tyhjentää valitun henkilön', async ({
    page,
  }) => {
    await page.goto('tarkastus');

    await selectOption({ name: 'Oppilaitos', page, option: OPPILAITOS_NIMI });

    const sidebar = getHenkilotSidebar(page);
    await expect(sidebar.getByText('4 henkilöä')).toBeVisible();

    // Valitaan henkilö
    await page
      .getByRole('link', { name: 'Olli Oppija, henkilötunnus: 010296-1230' })
      .click();

    await expect(page).toHaveURL((url) =>
      url.pathname.includes(`tarkastus/${OPPIJANUMERO}`),
    );
    await expect(
      page.getByRole('heading', { name: 'Olli Oppija (010296-1230)' }),
    ).toBeVisible();

    // Vaihdetaan oppilaitos
    await selectOption({
      name: 'Oppilaitos',
      page,
      option: 'Tampereen normaalikoulu',
    });

    // Henkilö pitäisi olla tyhjennetty
    await expect(page).toHaveURL((url) => !url.pathname.includes(OPPIJANUMERO));
    await expect(
      page.getByRole('heading', { name: 'Olli Oppija (010296-1230)' }),
    ).toBeHidden();
    await expect(page.getByText('Hae ja valitse henkilö')).toBeVisible();
  });

  test('vuoden vaihtaminen tyhjentää valitun henkilön', async ({ page }) => {
    await page.goto('tarkastus');

    await selectOption({ name: 'Oppilaitos', page, option: OPPILAITOS_NIMI });

    const sidebar = getHenkilotSidebar(page);
    await expect(sidebar.getByText('4 henkilöä')).toBeVisible();

    // Valitaan henkilö
    await page
      .getByRole('link', { name: 'Olli Oppija, henkilötunnus: 010296-1230' })
      .click();

    await expect(page).toHaveURL((url) =>
      url.pathname.includes(`tarkastus/${OPPIJANUMERO}`),
    );
    await expect(
      page.getByRole('heading', { name: 'Olli Oppija (010296-1230)' }),
    ).toBeVisible();

    // Vaihdetaan vuosi
    await selectOption({ page, name: 'Valmistumisvuosi', option: '2024' });

    // Henkilö pitäisi olla tyhjennetty
    await expect(page).toHaveURL((url) => !url.pathname.includes(OPPIJANUMERO));
    await expect(
      page.getByRole('heading', { name: 'Olli Oppija (010296-1230)' }),
    ).toBeHidden();
    await expect(page.getByText('Hae ja valitse henkilö')).toBeVisible();
  });

  test('luokan vaihtaminen tyhjentää valitun henkilön', async ({ page }) => {
    await page.goto('tarkastus');

    await selectOption({ name: 'Oppilaitos', page, option: OPPILAITOS_NIMI });

    const sidebar = getHenkilotSidebar(page);
    await expect(sidebar.getByText('4 henkilöä')).toBeVisible();

    // Valitaan henkilö
    await page
      .getByRole('link', { name: 'Olli Oppija, henkilötunnus: 010296-1230' })
      .click();

    await expect(page).toHaveURL((url) =>
      url.pathname.includes(`tarkastus/${OPPIJANUMERO}`),
    );
    await expect(
      page.getByRole('heading', { name: 'Olli Oppija (010296-1230)' }),
    ).toBeVisible();

    // Vaihdetaan luokka
    await selectOption({ page, name: 'Luokka', option: '9A' });

    // Henkilö pitäisi olla tyhjennetty
    await expect(page).toHaveURL((url) => !url.pathname.includes(OPPIJANUMERO));
    await expect(
      page.getByRole('heading', { name: 'Olli Oppija (010296-1230)' }),
    ).toBeHidden();
    await expect(page.getByText('Hae ja valitse henkilö')).toBeVisible();
  });
});
