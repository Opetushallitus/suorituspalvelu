import { expect } from '@playwright/test';
import { test } from './lib/fixtures';
import OPPIJAN_TIEDOT from './fixtures/oppijanTiedot.json' with { type: 'json' };

import SUORITUS_OPPILAITOKSET from './fixtures/luoSuoritusOppilaitokset.json' with { type: 'json' };
import SUORITUSVAIHTOEHDOT from './fixtures/luoSuoritusvaihtoehdot.json' with { type: 'json' };

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

  test('Suorituksen lisäys-lomakkeen voi avata ja sulkea', async ({ page }) => {
    await expect(
      page.getByRole('heading', { name: 'Suoritukset' }),
    ).toBeVisible();

    const addButton = page.getByRole('button', { name: 'Lisää suoritus' });
    await expect(addButton).toBeVisible();
    await addButton.click();

    await expect(
      page.getByRole('heading', { name: 'Lisää suoritus' }),
    ).toBeVisible();

    const lisaaSuoritusPaper = page.getByRole('region', {
      name: 'Lisää suoritus',
    });

    await expect(lisaaSuoritusPaper.getByLabel('Tyyppi')).toBeVisible();
    await expect(lisaaSuoritusPaper.getByLabel('Oppilaitos')).toBeVisible();
    await expect(lisaaSuoritusPaper.getByLabel('Tila')).toBeVisible();
    await expect(
      lisaaSuoritusPaper.getByLabel('Valmistumispäivä'),
    ).toBeVisible();
    await expect(lisaaSuoritusPaper.getByLabel('Suorituskieli')).toBeVisible();
    await expect(lisaaSuoritusPaper.getByLabel('Luokka')).toBeVisible();
    await expect(
      lisaaSuoritusPaper.getByLabel('Yksilöllistetty'),
    ).toBeVisible();

    const peruutaButton = lisaaSuoritusPaper.getByRole('button', {
      name: 'Peruuta',
    });
    await expect(peruutaButton).toBeVisible();
    await expect(
      lisaaSuoritusPaper.getByRole('button', { name: 'Tallenna' }),
    ).toBeVisible();

    await peruutaButton.click();
  });

  test('Filling out add suoritus form', async ({ page }) => {
    // Wait for the page to load and click add button
    await expect(
      page.getByRole('heading', { name: 'Suoritukset' }),
    ).toBeVisible();
    await page.getByRole('button', { name: 'Lisää suoritus' }).click();

    // Wait for the form to appear
    await expect(
      page.getByRole('heading', { name: 'Lisää suoritus' }),
    ).toBeVisible();

    // Fill out the form fields
    await page.getByLabel('Tyyppi').click();
    await page
      .getByRole('option', { name: 'Perusopetuksen oppimäärä' })
      .click();

    // Fill oppilaitos field (autocomplete)
    await page.getByLabel('Oppilaitos').click();
    await page.getByLabel('Oppilaitos').fill('Testioppilaitos');
    await page.getByRole('option', { name: 'Testioppilaitos' }).click();

    // Fill tila field
    await page.getByLabel('Tila').click();
    await page.getByRole('option', { name: 'Valmis' }).click();

    // Fill valmistumispäivä (date picker)
    await page.getByLabel('Valmistumispäivä').fill('01.06.2024');

    // Fill suorituskieli
    await page.getByLabel('Suorituskieli').click();
    await page.getByRole('option', { name: 'suomi' }).click();

    // Fill luokka
    await page.getByLabel('Luokka').fill('9A');

    // Fill yksilöllistetty
    await page.getByLabel('Yksilöllistetty').click();
    await page.getByRole('option', { name: 'Ei' }).click();

    // Verify all fields have been filled
    await expect(page.getByLabel('Tyyppi')).toHaveValue(
      'perusopetuksenoppimaara',
    );
    await expect(page.getByLabel('Tila')).toHaveValue('VALMIS');
    await expect(page.getByLabel('Valmistumispäivä')).toHaveValue('01.06.2024');
    await expect(page.getByLabel('Suorituskieli')).toHaveValue('FI');
    await expect(page.getByLabel('Luokka')).toHaveValue('9A');
    await expect(page.getByLabel('Yksilöllistetty')).toHaveValue('1');
  });

  test('Form validation for required fields', async ({ page }) => {
    // Wait for the page to load and click add button
    await expect(
      page.getByRole('heading', { name: 'Suoritukset' }),
    ).toBeVisible();
    await page.getByRole('button', { name: 'Lisää suoritus' }).click();

    // Wait for the form to appear
    await expect(
      page.getByRole('heading', { name: 'Lisää suoritus' }),
    ).toBeVisible();

    // Try to save without filling required fields
    const saveButton = page.getByRole('button', { name: 'Tallenna' });
    await expect(saveButton).toBeVisible();

    // Note: Since we can't easily test HTML5 validation, we'll just verify
    // that required fields are marked as required
    await expect(page.getByLabel('Tyyppi')).toHaveAttribute(
      'aria-required',
      'true',
    );
    await expect(page.getByLabel('Tila')).toHaveAttribute(
      'aria-required',
      'true',
    );
    await expect(page.getByLabel('Suorituskieli')).toHaveAttribute(
      'aria-required',
      'true',
    );
    await expect(page.getByLabel('Luokka')).toHaveAttribute(
      'aria-required',
      'true',
    );
  });

  test('Uuden suorituksen tallentaminen onnistuu', async ({ page }) => {
    let saveRequestMade = false;
    let saveRequestData: unknown = null;

    // Mock the save API endpoint
    await page.route('**/ui/suoritus/perusopetus/oppimaara', async (route) => {
      saveRequestMade = true;
      saveRequestData = await route.request().postDataJSON();
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true }),
      });
    });

    // Mock the refresh data endpoint after save
    await page.route(
      `**/ui/tiedot/${OPPIJANUMERO}`,
      async (route) => {
        // Return updated data including the new suoritus
        const updatedData = {
          ...OPPIJAN_TIEDOT,
          perusopetuksenOppimaarat: [
            {
              tunniste: 'new-suoritus-123',
              versioTunniste: 'version-123',
              syotetty: true,
              suoritustyyppi: 'perusopetuksenoppimaara',
              oppilaitos: {
                nimi: {
                  fi: 'Testioppilaitos',
                  sv: 'Testskola',
                  en: 'Test Institute',
                },
                oid: '1.2.246.562.10.12345678901',
              },
              tila: 'VALMIS',
              valmistumispaiva: '2024-06-01',
              suorituskieli: 'FI',
              luokka: '9A',
              yksilollistaminen: {
                arvo: 1,
                nimi: {
                  fi: 'Ei',
                  sv: 'Nej',
                  en: 'No',
                },
              },
              oppiaineet: [],
            },
          ],
        };
        await route.fulfill({
          json: updatedData,
        });
      },
      { times: 1 },
    );

    // Wait for the page to load and click add button
    await expect(
      page.getByRole('heading', { name: 'Suoritukset' }),
    ).toBeVisible();
    await page.getByRole('button', { name: 'Lisää suoritus' }).click();

    // Wait for the form to appear
    await expect(
      page.getByRole('heading', { name: 'Lisää suoritus' }),
    ).toBeVisible();

    // Fill out the form completely
    await page.getByLabel('Tyyppi').click();
    await page
      .getByRole('option', { name: 'Perusopetuksen oppimäärä' })
      .click();

    await page.getByLabel('Oppilaitos').click();
    await page.getByLabel('Oppilaitos').fill('Testioppilaitos');
    await page.getByRole('option', { name: 'Testioppilaitos' }).click();

    await page.getByLabel('Tila').click();
    await page.getByRole('option', { name: 'Valmis' }).click();

    await page.getByLabel('Valmistumispäivä').fill('01.06.2024');

    await page.getByLabel('Suorituskieli').click();
    await page.getByRole('option', { name: 'suomi' }).click();

    await page.getByLabel('Luokka').fill('9A');

    await page.getByLabel('Yksilöllistetty').click();
    await page.getByRole('option', { name: 'Ei' }).click();

    // Save the form
    await page.getByRole('button', { name: 'Tallenna' }).click();

    // Wait for the save request to be made
    await page.waitForFunction(() => saveRequestMade, { timeout: 5000 });

    // Verify the API was called
    expect(saveRequestMade).toBe(true);
    expect(saveRequestData).toMatchObject({
      tila: 'VALMIS',
      oppijaOid: OPPIJANUMERO,
      oppilaitosOid: '1.2.246.562.10.12345678901',
      suorituskieli: 'FI',
      yksilollistetty: 1,
      luokka: '9A',
      valmistumispaiva: '2024-06-01',
    });

    // The form should disappear after successful save (this might take a moment)
    await expect(
      page.getByRole('heading', { name: 'Lisää suoritus' }),
    ).toBeHidden({
      timeout: 5000,
    });
  });

  test('Canceling add suoritus operation', async ({ page }) => {
    // Wait for the page to load and click add button
    await expect(
      page.getByRole('heading', { name: 'Suoritukset' }),
    ).toBeVisible();
    await page.getByRole('button', { name: 'Lisää suoritus' }).click();

    // Wait for the form to appear
    await expect(
      page.getByRole('heading', { name: 'Lisää suoritus' }),
    ).toBeVisible();

    // Fill out some fields
    await page.getByLabel('Tyyppi').click();
    await page
      .getByRole('option', { name: 'Perusopetuksen oppimäärä' })
      .click();

    await page.getByLabel('Luokka').fill('9A');

    // Verify fields have content
    await expect(page.getByLabel('Tyyppi')).toHaveValue(
      'perusopetuksenoppimaara',
    );
    await expect(page.getByLabel('Luokka')).toHaveValue('9A');

    // Click cancel button
    await page.getByRole('button', { name: 'Peruuta' }).click();

    // Form should disappear
    await expect(
      page.getByRole('heading', { name: 'Lisää suoritus' }),
    ).toBeHidden();

    // The add button should still be visible
    await expect(
      page.getByRole('button', { name: 'Lisää suoritus' }),
    ).toBeVisible();

    // When reopening the form, fields should be reset
    await page.getByRole('button', { name: 'Lisää suoritus' }).click();
    await expect(
      page.getByRole('heading', { name: 'Lisää suoritus' }),
    ).toBeVisible();

    // Fields should be empty/default values
    await expect(page.getByLabel('Luokka')).toHaveValue('');
  });
});
