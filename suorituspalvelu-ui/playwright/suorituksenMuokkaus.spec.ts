import { expect } from '@playwright/test';
import { test } from './lib/fixtures';
import OPPIJAN_TIEDOT from './fixtures/oppijanTiedot.json' with { type: 'json' };

import SUORITUS_OPPILAITOKSET from './fixtures/luoSuoritusOppilaitokset.json' with { type: 'json' };
import SUORITUSVAIHTOEHDOT from './fixtures/luoSuoritusvaihtoehdot.json' with { type: 'json' };
import {
  selectDateInDatePicker,
  selectOption,
  startAddSuoritus,
  startEditSuoritus,
} from './lib/playwrightUtils';

const OPPIJANUMERO = OPPIJAN_TIEDOT.oppijaNumero;

test.describe('Suorituksen muokkaus', () => {
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

  test('lomake näyttää kaikki kentät oikeilla arvoilla', async ({ page }) => {
    const editSuoritusForm = await startEditSuoritus(page);

    await expect(editSuoritusForm.getByLabel('Tyyppi')).toHaveText(
      'Perusopetuksen oppimäärä',
    );
    await expect(editSuoritusForm.getByLabel('Oppilaitos')).toHaveValue(
      'Esimerkkioppilaitos fi (1.2.3.4)',
    );
    await expect(editSuoritusForm.getByLabel('Tila')).toHaveText(
      'Suoritus valmis',
    );
    await expect(
      editSuoritusForm.getByLabel('Valmistumispäivä').locator('input'),
    ).toHaveValue('01.06.2016');
    await expect(editSuoritusForm.getByLabel('Suorituskieli')).toHaveText(
      'suomi',
    );
    await expect(editSuoritusForm.getByLabel('Luokka')).toHaveValue('9A');
    await expect(editSuoritusForm.getByLabel('Yksilöllistetty')).toHaveText(
      'Perusopetuksen osittain yksilöllistetty oppimäärä',
    );

    await expect(
      editSuoritusForm.getByLabel(
        'Oppimäärä oppiaineelle Äidinkieli ja kirjallisuus',
      ),
    ).toHaveText('Suomen kieli ja kirjallisuus');
    await expect(
      editSuoritusForm.getByLabel(
        'Arvosana oppiaineelle Äidinkieli ja kirjallisuus',
      ),
    ).toHaveText('9');
    await expect(
      editSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle Äidinkieli ja kirjallisuus',
      ),
    ).toHaveText('-');

    await expect(
      editSuoritusForm.getByLabel('Oppimäärä oppiaineelle A1-kieli'),
    ).toHaveText('englanti');
    await expect(
      editSuoritusForm.getByLabel('Arvosana oppiaineelle A1-kieli'),
    ).toHaveText('9');
    await expect(
      editSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle A1-kieli',
      ),
    ).toHaveText('-');

    await expect(
      editSuoritusForm.getByLabel('Oppimäärä oppiaineelle A2-kieli'),
    ).toHaveText('-');
    await expect(
      editSuoritusForm.getByLabel('Arvosana oppiaineelle A2-kieli'),
    ).toHaveText('-');
    await expect(
      editSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle A2-kieli',
      ),
    ).toHaveText('-');

    await expect(
      editSuoritusForm.getByLabel('Oppimäärä oppiaineelle B1-kieli'),
    ).toHaveText('ruotsi');
    await expect(
      editSuoritusForm.getByLabel('Arvosana oppiaineelle B1-kieli'),
    ).toHaveText('9');
    await expect(
      editSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle B1-kieli',
      ),
    ).toHaveText('-');

    await expect(
      editSuoritusForm.getByLabel('Oppimäärä oppiaineelle B2-kieli'),
    ).toHaveText('saksa');
    await expect(
      editSuoritusForm.getByLabel('Arvosana oppiaineelle B2-kieli'),
    ).toHaveText('-');
    await expect(
      editSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle B2-kieli',
      ),
    ).toHaveText('9');

    await expect(
      editSuoritusForm.getByLabel('Oppimäärä oppiaineelle Matematiikka'),
    ).toBeHidden();
    await expect(
      editSuoritusForm.getByLabel('Arvosana oppiaineelle Matematiikka'),
    ).toHaveText('9');
    await expect(
      editSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle Matematiikka',
      ),
    ).toHaveText('-');

    await expect(
      editSuoritusForm.getByLabel('Oppimäärä oppiaineelle Biologia'),
    ).toBeHidden();
    await expect(
      editSuoritusForm.getByLabel('Arvosana oppiaineelle Biologia'),
    ).toHaveText('9');
    await expect(
      editSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle Biologia',
      ),
    ).toHaveText('-');

    await expect(
      editSuoritusForm.getByLabel('Oppimäärä oppiaineelle Maantieto'),
    ).toBeHidden();
    await expect(
      editSuoritusForm.getByLabel('Arvosana oppiaineelle Maantieto'),
    ).toHaveText('9');
    await expect(
      editSuoritusForm.getByLabel(
        'Valinnainen arvosana 1 oppiaineelle Maantieto',
      ),
    ).toHaveText('-');

    const peruutaButton = editSuoritusForm.getByRole('button', {
      name: 'Peruuta',
    });
    await expect(peruutaButton).toBeVisible();
    await expect(
      editSuoritusForm.getByRole('button', { name: 'Tallenna' }),
    ).toBeVisible();

    await peruutaButton.click();
    await expect(editSuoritusForm).toBeHidden();
  });

  test('peruuttaminen pyytää vahvistuksen, jos muutoksia', async ({ page }) => {
    const editSuoritusForm = await startEditSuoritus(page);
    await editSuoritusForm.getByLabel('Luokka').fill('9A');

    const peruutaButton = editSuoritusForm.getByRole('button', {
      name: 'Peruuta',
    });

    await selectOption({
      page,
      name: 'Tila',
      option: 'Suoritus kesken',
      locator: editSuoritusForm,
    });

    await peruutaButton.click();

    const vahvistaModal = page.getByRole('dialog', {
      name: 'Haluatko peruuttaa suorituksen muokkaamisen?',
    });
    await expect(vahvistaModal).toBeVisible();

    await vahvistaModal.getByRole('button', { name: 'Ei' }).click();
    await expect(vahvistaModal).toBeHidden();

    await expect(editSuoritusForm).toBeVisible();

    await peruutaButton.click();

    const vahvistaPeruutaButton = vahvistaModal.getByRole('button', {
      name: 'Kyllä',
    });
    await expect(vahvistaPeruutaButton).toBeVisible();
    await vahvistaPeruutaButton.click();

    await expect(vahvistaModal).toBeHidden();
    await expect(editSuoritusForm).toBeHidden();
  });

  test('onnistunut tallennus lähettää suorituksen tallennuspyynnön ja lataa suoritustiedot uudelleen', async ({
    page,
  }) => {
    await page.route('**/ui/perusopetuksenoppimaarat', async (route) => {
      await route.fulfill({
        status: 200,
      });
    });

    const editSuoritusForm = await startEditSuoritus(page);

    await selectOption({
      page,
      name: 'Tyyppi',
      option: 'Perusopetuksen oppimäärä',
      locator: editSuoritusForm,
    });

    await selectOption({
      page,
      name: 'Oppilaitos',
      option: 'Hailuodon peruskoulu (1.2.246.562.10.46678824486)',
      exactOption: false,
      locator: editSuoritusForm,
    });

    await selectOption({
      page,
      name: 'Tila',
      option: 'Suoritus valmis',
      locator: editSuoritusForm,
    });

    await selectDateInDatePicker(
      editSuoritusForm.getByLabel('Valmistumispäivä'),
      '01.06.2024',
    );

    await selectOption({
      page,
      name: 'Suorituskieli',
      option: 'suomi',
      locator: editSuoritusForm,
    });

    await editSuoritusForm.getByLabel('Luokka').fill('9A');

    await selectOption({
      page,
      name: 'Yksilöllistetty',
      option: 'Perusopetuksen oppimäärä',
      locator: editSuoritusForm,
    });

    const [saveRequest] = await Promise.all([
      page.waitForRequest('**/ui/perusopetuksenoppimaarat'), // tallennus
      page.waitForRequest(`**/ui/tiedot/${OPPIJANUMERO}`), // tietojen uudelleenlataus
      editSuoritusForm.getByRole('button', { name: 'Tallenna' }).click(),
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

    await expect(editSuoritusForm).toBeHidden();
  });

  test('epäonnistunut tallennus näyttää palvelimelta tulevan virheilmoituksen', async ({
    page,
  }) => {
    await page.route('**/ui/perusopetuksenoppimaarat', async (route) => {
      await route.fulfill({
        status: 500,
        body: 'Virhe palvelimelta',
      });
    });

    const lisaaSuoritusForm = await startEditSuoritus(page);

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

    const lisaaSuoritusForm = await startEditSuoritus(page);

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

  test('näyttää vahvistusmodaalin, jos ollaan jo lisäämässä suoritusta', async ({
    page,
  }) => {
    const addSuoritusForm = await startAddSuoritus(page);
    const editSuoritusForm = page.getByRole('region', {
      name: 'Muokkaa suoritusta',
    });

    const editButton = page.getByRole('button', { name: 'Muokkaa suoritusta' });
    await editButton.click();

    const confirmationModal = page.getByRole('dialog', {
      name: 'Haluatko keskeyttää suorituksen lisäämisen?',
    });
    await expect(confirmationModal).toBeVisible();

    await confirmationModal.getByRole('button', { name: 'Ei' }).click();
    await expect(confirmationModal).toBeHidden();
    await expect(addSuoritusForm).toBeVisible();
    await expect(editSuoritusForm).toBeHidden();
    await editButton.click();

    await confirmationModal.getByRole('button', { name: 'Kyllä' }).click();
    await expect(confirmationModal).toBeHidden();
    await expect(addSuoritusForm).toBeHidden();
    await expect(editSuoritusForm).toBeVisible();
  });

  test('poistaminen lähettää poistopyynnön ja päivittää näkymän', async ({
    page,
  }) => {
    const PERUSOPETUKSEN_OPPIMAARA_VERSIOTUNNISTE =
      '10b37fec-ce8d-49c8-858d-6d0d2786fb40';

    await page.route(
      `**/ui/versiot/${PERUSOPETUKSEN_OPPIMAARA_VERSIOTUNNISTE}`,
      async (route) => {
        await route.fulfill({
          status: 200,
        });
      },
    );

    const deleteButton = page.getByRole('button', {
      name: 'Poista suoritus',
    });
    await deleteButton.click();

    const confirmationModal = page.getByRole('dialog', {
      name: 'Haluatko varmasti poistaa suorituksen?',
    });
    await expect(confirmationModal).toBeVisible();

    await Promise.all([
      page.waitForRequest(
        `**/ui/versiot/${PERUSOPETUKSEN_OPPIMAARA_VERSIOTUNNISTE}`,
      ), // poisto
      page.waitForRequest(`**/ui/tiedot/${OPPIJANUMERO}`), // tietojen uudelleenlataus
      confirmationModal.getByRole('button', { name: 'Kyllä' }).click(),
    ]);
  });
});
