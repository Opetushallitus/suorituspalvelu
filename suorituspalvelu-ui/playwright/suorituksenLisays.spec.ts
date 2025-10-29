import { expect, type Locator, type Page } from '@playwright/test';
import { test } from './lib/fixtures';
import OPPIJAN_TIEDOT from './fixtures/oppijanTiedot.json' with { type: 'json' };

import SUORITUS_OPPILAITOKSET from './fixtures/luoSuoritusOppilaitokset.json' with { type: 'json' };
import SUORITUSVAIHTOEHDOT from './fixtures/luoSuoritusvaihtoehdot.json' with { type: 'json' };
import { selectOption } from './lib/playwrightUtils';

const OPPIJANUMERO = OPPIJAN_TIEDOT.oppijaNumero;

const selectDateInDatePicker = async (picker: Locator, date: string) => {
  await picker.getByRole('textbox').fill(date);
};

const startSuoritusAdd = async (page: Page) => {
  await expect(
    page.getByRole('heading', { name: 'Suoritukset' }),
  ).toBeVisible();

  const addButton = page.getByRole('button', { name: 'Lisää suoritus' });
  await addButton.click();

  const lisaaSuoritusForm = page.getByRole('region', {
    name: 'Lisää suoritus',
  });
  await expect(
    lisaaSuoritusForm.getByRole('heading', { name: 'Lisää suoritus' }),
  ).toBeVisible();

  return lisaaSuoritusForm;
};

const startEditPerusopetuksenOppimaaraSuoritus = async (page: Page) => {
  await expect(
    page.getByRole('heading', { name: 'Suoritukset' }),
  ).toBeVisible();

  await page
    .getByRole('button', { name: 'Muokkaa suoritusta' })
    .first()
    .click();
};

test.describe('Suorituksen lisäys', () => {
  test.beforeEach(async ({ page }) => {
    await page.clock.setFixedTime(new Date('2025-01-01T12:00:00Z'));

    await page.route(`**/ui/tiedot/${OPPIJANUMERO}`, async (route) => {
      await route.fulfill({
        json: OPPIJAN_TIEDOT,
      });
    });

    await page.route(`**/ui/oppilaitokset`, async (route) => {
      await route.fulfill({
        json: SUORITUS_OPPILAITOKSET,
      });
    });

    await page.route(`**/ui/luosuoritusvaihtoehdot`, async (route) => {
      await route.fulfill({
        json: SUORITUSVAIHTOEHDOT,
      });
    });

    await page.route(`**/ui/luosuoritusoppilaitokset`, async (route) => {
      await route.fulfill({
        json: SUORITUS_OPPILAITOKSET,
      });
    });

    await page.goto(`/suorituspalvelu/henkilo/${OPPIJANUMERO}`);
  });

  test('lomake näyttää kaikki kentät oletusarvoilla', async ({ page }) => {
    const lisaaSuoritusForm = await startSuoritusAdd(page);

    await expect(lisaaSuoritusForm.getByLabel('Tyyppi')).toHaveText(
      'Perusopetuksen oppimäärä',
    );
    await expect(lisaaSuoritusForm.getByLabel('Oppilaitos')).toHaveText('');
    await expect(lisaaSuoritusForm.getByLabel('Tila')).toHaveText(
      'Suoritus valmis',
    );
    await expect(
      lisaaSuoritusForm.getByLabel('Valmistumispäivä').locator('input'),
    ).toHaveValue('01.01.2025');
    await expect(lisaaSuoritusForm.getByLabel('Suorituskieli')).toHaveText(
      'suomi',
    );
    await expect(lisaaSuoritusForm.getByLabel('Luokka')).toHaveText('');
    await expect(lisaaSuoritusForm.getByLabel('Yksilöllistetty')).toHaveText(
      'Perusopetuksen oppimäärä',
    );

    await expect(
      lisaaSuoritusForm.getByLabel(
        'Oppimäärä oppiaineelle Äidinkieli ja kirjallisuus',
      ),
    ).toHaveText('-');
    await expect(
      lisaaSuoritusForm.getByLabel(
        'Arvosana oppiaineelle Äidinkieli ja kirjallisuus',
      ),
    ).toHaveText('-');
    await expect(
      lisaaSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle Äidinkieli ja kirjallisuus',
      ),
    ).toHaveText('-');

    await expect(
      lisaaSuoritusForm.getByLabel('Oppimäärä oppiaineelle A1-kieli'),
    ).toHaveText('-');
    await expect(
      lisaaSuoritusForm.getByLabel('Arvosana oppiaineelle A1-kieli'),
    ).toHaveText('-');
    await expect(
      lisaaSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle A1-kieli',
      ),
    ).toHaveText('-');

    await expect(
      lisaaSuoritusForm.getByLabel('Oppimäärä oppiaineelle A2-kieli'),
    ).toHaveText('-');
    await expect(
      lisaaSuoritusForm.getByLabel('Arvosana oppiaineelle A2-kieli'),
    ).toHaveText('-');
    await expect(
      lisaaSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle A2-kieli',
      ),
    ).toHaveText('-');

    await expect(
      lisaaSuoritusForm.getByLabel('Oppimäärä oppiaineelle B1-kieli'),
    ).toHaveText('-');
    await expect(
      lisaaSuoritusForm.getByLabel('Arvosana oppiaineelle B1-kieli'),
    ).toHaveText('-');
    await expect(
      lisaaSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle B1-kieli',
      ),
    ).toHaveText('-');

    await expect(
      lisaaSuoritusForm.getByLabel('Oppimäärä oppiaineelle Matematiikka'),
    ).toBeHidden();
    await expect(
      lisaaSuoritusForm.getByLabel('Arvosana oppiaineelle Matematiikka'),
    ).toHaveText('-');
    await expect(
      lisaaSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle Matematiikka',
      ),
    ).toHaveText('-');

    await expect(
      lisaaSuoritusForm.getByLabel('Oppimäärä oppiaineelle Biologia'),
    ).toBeHidden();
    await expect(
      lisaaSuoritusForm.getByLabel('Arvosana oppiaineelle Biologia'),
    ).toHaveText('-');
    await expect(
      lisaaSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle Biologia',
      ),
    ).toHaveText('-');

    await expect(
      lisaaSuoritusForm.getByLabel('Oppimäärä oppiaineelle Maantieto'),
    ).toBeHidden();
    await expect(
      lisaaSuoritusForm.getByLabel('Arvosana oppiaineelle Maantieto'),
    ).toHaveText('-');
    await expect(
      lisaaSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle Maantieto',
      ),
    ).toHaveText('-');

    const peruutaButton = lisaaSuoritusForm.getByRole('button', {
      name: 'Peruuta',
    });
    await expect(peruutaButton).toBeVisible();
    await expect(
      lisaaSuoritusForm.getByRole('button', { name: 'Tallenna' }),
    ).toBeVisible();

    await peruutaButton.click();
    await expect(lisaaSuoritusForm).toBeHidden();
  });

  test('peruuttaminen pyytää vahvistuksen, jos muutoksia', async ({ page }) => {
    const lisaaSuoritusForm = await startSuoritusAdd(page);
    await lisaaSuoritusForm.getByLabel('Luokka').fill('9A');

    const peruutaButton = lisaaSuoritusForm.getByRole('button', {
      name: 'Peruuta',
    });
    await peruutaButton.click();

    const vahvistaModal = page.getByRole('dialog', {
      name: 'Haluatko peruuttaa suorituksen lisäämisen?',
    });
    await expect(vahvistaModal).toBeVisible();

    await vahvistaModal.getByRole('button', { name: 'Ei' }).click();
    await expect(vahvistaModal).toBeHidden();

    await expect(lisaaSuoritusForm).toBeVisible();

    await peruutaButton.click();

    const vahvistaPeruutaButton = vahvistaModal.getByRole('button', {
      name: 'Kyllä',
    });
    await expect(vahvistaPeruutaButton).toBeVisible();
    await vahvistaPeruutaButton.click();

    await expect(vahvistaModal).toBeHidden();
    await expect(lisaaSuoritusForm).toBeHidden();
  });

  test('onnistunut tallennus lähettää suorituksen tallennuspyynnön ja lataa suoritustiedot uudelleen', async ({
    page,
  }) => {
    // Mock the save API endpoint
    await page.route('**/ui/perusopetuksenoppimaarat', async (route) => {
      await route.fulfill({
        status: 200,
      });
    });

    const lisaaSuoritusForm = await startSuoritusAdd(page);

    await selectOption({
      page,
      name: 'Tyyppi',
      option: 'Perusopetuksen oppimäärä',
      locator: lisaaSuoritusForm,
    });

    await selectOption({
      page,
      name: 'Oppilaitos',
      option: 'Hailuodon peruskoulu (1.2.246.562.10.46678824486)',
      exactOption: false,
      locator: lisaaSuoritusForm,
    });

    await selectOption({
      page,
      name: 'Tila',
      option: 'Suoritus valmis',
      locator: lisaaSuoritusForm,
    });

    await selectDateInDatePicker(
      lisaaSuoritusForm.getByLabel('Valmistumispäivä'),
      '01.06.2024',
    );

    await selectOption({
      page,
      name: 'Suorituskieli',
      option: 'suomi',
      locator: lisaaSuoritusForm,
    });

    await lisaaSuoritusForm.getByLabel('Luokka').fill('9A');

    await selectOption({
      page,
      name: 'Yksilöllistetty',
      option: 'Perusopetuksen oppimäärä',
      locator: lisaaSuoritusForm,
    });

    const [saveRequest] = await Promise.all([
      page.waitForRequest('**/ui/perusopetuksenoppimaarat'), // tallennus
      page.waitForRequest(`**/ui/tiedot/${OPPIJANUMERO}`), // tietojen uudelleenlataus
      lisaaSuoritusForm.getByRole('button', { name: 'Tallenna' }).click(),
    ]);

    const saveRequestData = saveRequest.postDataJSON();

    expect(saveRequestData).toMatchObject({
      tila: 'VALMIS',
      oppijaOid: OPPIJANUMERO,
      oppilaitosOid: '1.2.246.562.10.46678824486',
      suorituskieli: 'FI',
      yksilollistetty: 1,
      luokka: '9A',
      valmistumispaiva: '2024-06-01',
    });

    await expect(lisaaSuoritusForm).toBeHidden();
  });

  test('epäonnistunut tallennus näyttää palvelimelta tulevan virheilmoituksen', async ({
    page,
  }) => {
    // Mock the save API endpoint
    await page.route('**/ui/perusopetuksenoppimaarat', async (route) => {
      await route.fulfill({
        status: 500,
        body: 'Virhe palvelimelta',
      });
    });

    const lisaaSuoritusForm = await startSuoritusAdd(page);

    await selectOption({
      page,
      name: 'Tyyppi',
      option: 'Perusopetuksen oppimäärä',
      locator: lisaaSuoritusForm,
    });

    await Promise.all([
      page.waitForRequest('**/ui/perusopetuksenoppimaarat'), // tallennus
      lisaaSuoritusForm.getByRole('button', { name: 'Tallenna' }).click(),
    ]);

    const errorDialog = await page
      .getByRole('dialog')
      .filter({ hasText: 'Suorituksen tallennus epäonnistui' });

    await expect(errorDialog).toBeVisible();

    await errorDialog.getByRole('button', { name: 'Sulje' }).click();

    await expect(lisaaSuoritusForm).toBeVisible();
  });
  test('epäonnistunut tallennus näyttää palvelimelta palautuvat validointivirheet', async ({
    page,
  }) => {
    // Mock the save API endpoint
    await page.route('**/ui/perusopetuksenoppimaarat', async (route) => {
      await route.fulfill({
        status: 400,
        json: {
          yleisetVirheAvaimet: ['backend-virhe.oppilaitosoid.tyhja'],
          oppiaineKohtaisetVirheet: [
            {
              oppiaineKoodiArvo: 'MA',
              virheAvaimet: ['backend-virhe.oppiaine.tyhja'],
            },
            {
              oppiaineKoodiArvo: 'BIO',
              virheAvaimet: [
                'backend-virhe.oppiaine.arvosana.tyhja',
                'backend-virhe.oppiaine.kieli.maaritelty',
              ],
            },
          ],
        },
      });
    });

    const lisaaSuoritusForm = await startSuoritusAdd(page);

    await selectOption({
      page,
      name: 'Tyyppi',
      option: 'Perusopetuksen oppimäärä',
      locator: lisaaSuoritusForm,
    });

    await Promise.all([
      page.waitForRequest('**/ui/perusopetuksenoppimaarat'), // tallennus
      lisaaSuoritusForm.getByRole('button', { name: 'Tallenna' }).click(),
    ]);

    const errorDialog = await page
      .getByRole('dialog')
      .filter({ hasText: 'Suorituksen tallennus epäonnistui' });

    await expect(errorDialog).toBeVisible();

    await expect(errorDialog).toContainText('Oppilaitos ei saa olla tyhjä!');

    const oppiaineMaErrors = errorDialog.getByLabel('MA');
    await expect(oppiaineMaErrors).toContainText('Oppiaine ei saa olla tyhjä!');

    const oppiaineBioErrors = errorDialog.getByLabel('BIO');
    await expect(oppiaineBioErrors).toContainText(
      'Arvosana ei saa olla tyhjä!',
    );
    await expect(oppiaineBioErrors).toContainText(
      'Oppiaineen kieli-oppimäärä on määritelty, vaikka oppiaine ei ole kieli!',
    );

    await errorDialog.getByRole('button', { name: 'Sulje' }).click();

    await expect(lisaaSuoritusForm).toBeVisible();
  });

  test('näyttää vahvistusmodaalin, jos ollaan jo muokkaamassa suoritusta', async ({
    page,
  }) => {
    await startEditPerusopetuksenOppimaaraSuoritus(page);

    const addButton = page.getByRole('button', { name: 'Lisää suoritus' });
    await addButton.click();

    const lisaaSuoritusForm = page.getByRole('region', {
      name: 'Lisää suoritus',
    });

    const confirmationModal = page.getByRole('dialog', {
      name: 'Haluatko keskeyttää suorituksen muokkauksen?',
    });
    await expect(confirmationModal).toBeVisible();

    await confirmationModal.getByRole('button', { name: 'Ei' }).click();
    await expect(confirmationModal).toBeHidden();
    await expect(lisaaSuoritusForm).toBeHidden();
    await addButton.click();

    await confirmationModal.getByRole('button', { name: 'Kyllä' }).click();
    await expect(confirmationModal).toBeHidden();
    await expect(lisaaSuoritusForm).toBeVisible();
  });
});
