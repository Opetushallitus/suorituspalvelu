import { expect } from '@playwright/test';
import { test } from './lib/fixtures';
import OPPIJAN_TIEDOT from './fixtures/oppijanTiedot.json' with { type: 'json' };

import SUORITUS_OPPILAITOKSET from './fixtures/luoSuoritusOppilaitokset.json' with { type: 'json' };
import SUORITUSVAIHTOEHDOT from './fixtures/luoSuoritusvaihtoehdot.json' with { type: 'json' };
import {
  selectDateInDatePicker,
  selectOption,
  startEditSuoritus,
  startAddSuoritus,
} from './lib/playwrightUtils';

const OPPIJANUMERO = OPPIJAN_TIEDOT.oppijaNumero;

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
    const addSuoritusForm = await startAddSuoritus(page);

    await expect(addSuoritusForm.getByLabel('Tyyppi')).toHaveText(
      'Perusopetuksen oppimäärä',
    );
    await expect(addSuoritusForm.getByLabel('Oppilaitos')).toHaveText('');
    await expect(addSuoritusForm.getByLabel('Tila')).toHaveText(
      'Suoritus valmis',
    );
    await expect(
      addSuoritusForm.getByLabel('Valmistumispäivä').locator('input'),
    ).toHaveValue('01.01.2025');
    await expect(addSuoritusForm.getByLabel('Suorituskieli')).toHaveText(
      'suomi',
    );
    await expect(addSuoritusForm.getByLabel('Luokka')).toHaveValue('');
    await expect(addSuoritusForm.getByLabel('Yksilöllistetty')).toHaveText(
      'Perusopetuksen oppimäärä',
    );

    await expect(
      addSuoritusForm.getByLabel(
        'Oppimäärä oppiaineelle Äidinkieli ja kirjallisuus',
      ),
    ).toHaveText('-');
    await expect(
      addSuoritusForm.getByLabel(
        'Arvosana oppiaineelle Äidinkieli ja kirjallisuus',
      ),
    ).toHaveText('-');
    await expect(
      addSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle Äidinkieli ja kirjallisuus',
      ),
    ).toHaveText('-');

    await expect(
      addSuoritusForm.getByLabel('Oppimäärä oppiaineelle A1-kieli'),
    ).toHaveText('-');
    await expect(
      addSuoritusForm.getByLabel('Arvosana oppiaineelle A1-kieli'),
    ).toHaveText('-');
    await expect(
      addSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle A1-kieli',
      ),
    ).toHaveText('-');

    await expect(
      addSuoritusForm.getByLabel('Oppimäärä oppiaineelle A2-kieli'),
    ).toHaveText('-');
    await expect(
      addSuoritusForm.getByLabel('Arvosana oppiaineelle A2-kieli'),
    ).toHaveText('-');
    await expect(
      addSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle A2-kieli',
      ),
    ).toHaveText('-');

    await expect(
      addSuoritusForm.getByLabel('Oppimäärä oppiaineelle B1-kieli'),
    ).toHaveText('-');
    await expect(
      addSuoritusForm.getByLabel('Arvosana oppiaineelle B1-kieli'),
    ).toHaveText('-');
    await expect(
      addSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle B1-kieli',
      ),
    ).toHaveText('-');

    await expect(
      addSuoritusForm.getByLabel('Oppimäärä oppiaineelle B2-kieli'),
    ).toHaveText('-');
    await expect(
      addSuoritusForm.getByLabel('Arvosana oppiaineelle B2-kieli'),
    ).toHaveText('-');
    await expect(
      addSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle B2-kieli',
      ),
    ).toHaveText('-');

    await expect(
      addSuoritusForm.getByLabel('Oppimäärä oppiaineelle Matematiikka'),
    ).toBeHidden();
    await expect(
      addSuoritusForm.getByLabel('Arvosana oppiaineelle Matematiikka'),
    ).toHaveText('-');
    await expect(
      addSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle Matematiikka',
      ),
    ).toHaveText('-');

    await expect(
      addSuoritusForm.getByLabel('Oppimäärä oppiaineelle Biologia'),
    ).toBeHidden();
    await expect(
      addSuoritusForm.getByLabel('Arvosana oppiaineelle Biologia'),
    ).toHaveText('-');
    await expect(
      addSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle Biologia',
      ),
    ).toHaveText('-');

    await expect(
      addSuoritusForm.getByLabel('Oppimäärä oppiaineelle Maantieto'),
    ).toBeHidden();
    await expect(
      addSuoritusForm.getByLabel('Arvosana oppiaineelle Maantieto'),
    ).toHaveText('-');
    await expect(
      addSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle Maantieto',
      ),
    ).toHaveText('-');

    const peruutaButton = addSuoritusForm.getByRole('button', {
      name: 'Peruuta',
    });
    await expect(peruutaButton).toBeVisible();
    await expect(
      addSuoritusForm.getByRole('button', { name: 'Tallenna' }),
    ).toBeVisible();

    await peruutaButton.click();
    await expect(addSuoritusForm).toBeHidden();
  });

  test('peruuttaminen pyytää vahvistuksen, jos muutoksia', async ({ page }) => {
    const addSuoritusForm = await startAddSuoritus(page);
    await addSuoritusForm.getByLabel('Luokka').fill('9A');

    const peruutaButton = addSuoritusForm.getByRole('button', {
      name: 'Peruuta',
    });
    await peruutaButton.click();

    const vahvistaModal = page.getByRole('dialog', {
      name: 'Haluatko peruuttaa suorituksen lisäämisen?',
    });
    await expect(vahvistaModal).toBeVisible();

    await vahvistaModal.getByRole('button', { name: 'Ei' }).click();
    await expect(vahvistaModal).toBeHidden();

    await expect(addSuoritusForm).toBeVisible();

    await peruutaButton.click();

    const vahvistaPeruutaButton = vahvistaModal.getByRole('button', {
      name: 'Kyllä',
    });
    await expect(vahvistaPeruutaButton).toBeVisible();
    await vahvistaPeruutaButton.click();

    await expect(vahvistaModal).toBeHidden();
    await expect(addSuoritusForm).toBeHidden();
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

    const addSuoritusForm = await startAddSuoritus(page);

    await selectOption({
      page,
      name: 'Tyyppi',
      option: 'Perusopetuksen oppimäärä',
      locator: addSuoritusForm,
    });

    await selectOption({
      page,
      name: 'Oppilaitos',
      option: 'Hailuodon peruskoulu (1.2.246.562.10.46678824486)',
      exactOption: false,
      locator: addSuoritusForm,
    });

    await selectOption({
      page,
      name: 'Tila',
      option: 'Suoritus valmis',
      locator: addSuoritusForm,
    });

    await selectDateInDatePicker(
      addSuoritusForm.getByLabel('Valmistumispäivä'),
      '01.06.2024',
    );

    await selectOption({
      page,
      name: 'Suorituskieli',
      option: 'suomi',
      locator: addSuoritusForm,
    });

    await addSuoritusForm.getByLabel('Luokka').fill('9A');

    await selectOption({
      page,
      name: 'Yksilöllistetty',
      option: 'Perusopetuksen oppimäärä',
      locator: addSuoritusForm,
    });

    const [saveRequest] = await Promise.all([
      page.waitForRequest('**/ui/perusopetuksenoppimaarat'), // tallennus
      page.waitForRequest(`**/ui/tiedot/${OPPIJANUMERO}`), // tietojen uudelleenlataus
      addSuoritusForm.getByRole('button', { name: 'Tallenna' }).click(),
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

    await expect(addSuoritusForm).toBeHidden();
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

    const addSuoritusForm = await startAddSuoritus(page);

    await selectOption({
      page,
      name: 'Tyyppi',
      option: 'Perusopetuksen oppimäärä',
      locator: addSuoritusForm,
    });

    await Promise.all([
      page.waitForRequest('**/ui/perusopetuksenoppimaarat'), // tallennus
      addSuoritusForm.getByRole('button', { name: 'Tallenna' }).click(),
    ]);

    const errorDialog = await page
      .getByRole('dialog')
      .filter({ hasText: 'Suorituksen tallennus epäonnistui' });

    await expect(errorDialog).toBeVisible();

    await errorDialog.getByRole('button', { name: 'Sulje' }).click();

    await expect(addSuoritusForm).toBeVisible();
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
              oppiaineKoodiArvo: 'BI',
              virheAvaimet: [
                'backend-virhe.oppiaine.arvosana.tyhja',
                'backend-virhe.oppiaine.kieli.maaritelty',
              ],
            },
          ],
        },
      });
    });

    const addSuoritusForm = await startAddSuoritus(page);

    await selectOption({
      page,
      name: 'Tyyppi',
      option: 'Perusopetuksen oppimäärä',
      locator: addSuoritusForm,
    });

    await Promise.all([
      page.waitForRequest('**/ui/perusopetuksenoppimaarat'), // tallennus
      addSuoritusForm.getByRole('button', { name: 'Tallenna' }).click(),
    ]);

    const errorDialog = await page
      .getByRole('dialog')
      .filter({ hasText: 'Suorituksen tallennus epäonnistui' });

    await expect(errorDialog).toBeVisible();

    await expect(errorDialog).toContainText('Oppilaitos ei saa olla tyhjä!');

    const oppiaineMaErrors = errorDialog.getByLabel('Matematiikka', {
      exact: true,
    });
    await expect(oppiaineMaErrors).toContainText('Oppiaine ei saa olla tyhjä!');

    const oppiaineBioErrors = errorDialog.getByLabel('Biologia', {
      exact: true,
    });
    await expect(oppiaineBioErrors).toContainText(
      'Arvosana ei saa olla tyhjä!',
    );
    await expect(oppiaineBioErrors).toContainText(
      'Oppiaineen kieli-oppimäärä on määritelty, vaikka oppiaine ei ole kieli!',
    );

    await errorDialog.getByRole('button', { name: 'Sulje' }).click();

    await expect(addSuoritusForm).toBeVisible();
  });

  test('näyttää vahvistusmodaalin, jos ollaan jo muokkaamassa suoritusta', async ({
    page,
  }) => {
    const editSuoritusForm = await startEditSuoritus(page);
    const addSuoritusForm = page.getByRole('region', {
      name: 'Lisää suoritus',
    });

    const addButton = page.getByRole('button', { name: 'Lisää suoritus' });
    await addButton.click();

    const confirmationModal = page.getByRole('dialog', {
      name: 'Haluatko keskeyttää suorituksen muokkaamisen?',
    });
    await expect(confirmationModal).toBeVisible();

    await confirmationModal.getByRole('button', { name: 'Ei' }).click();
    await expect(confirmationModal).toBeHidden();
    await expect(addSuoritusForm).toBeHidden();
    await expect(editSuoritusForm).toBeVisible();
    await addButton.click();

    await confirmationModal.getByRole('button', { name: 'Kyllä' }).click();
    await expect(confirmationModal).toBeHidden();
    await expect(addSuoritusForm).toBeVisible();
    await expect(editSuoritusForm).toBeHidden();
  });
});
