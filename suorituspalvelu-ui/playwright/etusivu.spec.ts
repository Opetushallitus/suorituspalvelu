import { expect } from '@playwright/test';
import { test } from './lib/fixtures';
import { stubKayttajaResponse } from './lib/playwrightUtils';

test.describe('Etusivu', () => {
  test.beforeEach(async ({ page }) => {
    await page.route(`**/ui/rajain/oppilaitokset`, async (route) => {
      await route.fulfill({
        json: {
          oppilaitokset: [],
        },
      });
    });
  });

  test.describe('rekisterinpitäjä', () => {
    test.beforeEach(async ({ page }) => {
      await stubKayttajaResponse(page, {
        isRekisterinpitaja: true,
        isOrganisaationKatselija: false,
      });
      await page.goto('');
    });

    test('ohjautuu juuresta henkilöhakuun', async ({ page }) => {
      await stubKayttajaResponse(page, {
        isRekisterinpitaja: true,
        isOrganisaationKatselija: false,
      });
      await page.goto('');
      await expect(page).toHaveURL((url) =>
        url.toString().endsWith('/henkilo'),
      );
    });

    test('näytetään molemmat hakunäkymä-välilehdet', async ({ page }) => {
      const searchTabNavi = page.getByRole('navigation', {
        name: 'Oppijoiden hakunäkymän valitsin',
      });

      await expect(searchTabNavi).toBeVisible();

      await expect(
        searchTabNavi.getByRole('link', { name: 'Henkilöhaku' }),
      ).toBeVisible();
      await expect(
        searchTabNavi.getByRole('link', { name: 'Tarkastusnäkymä' }),
      ).toBeVisible();
    });
  });

  test.describe('tavallinen virkailija', () => {
    test.beforeEach(async ({ page }) => {
      await stubKayttajaResponse(page, {
        isRekisterinpitaja: false,
        isOrganisaationKatselija: false,
      });
    });
    test('ohjautuu juuresta henkilöhakuun', async ({ page }) => {
      await stubKayttajaResponse(page, {
        isRekisterinpitaja: false,
        isOrganisaationKatselija: false,
      });
      await page.goto('');
      await expect(page).toHaveURL((url) =>
        url.toString().endsWith('/henkilo'),
      );
    });

    test('näytetään vain henkilöhaku-välilehti', async ({ page }) => {
      await page.goto('henkilo');

      const searchTabNavi = page.getByRole('navigation', {
        name: 'Oppijoiden hakunäkymän valitsin',
      });

      await expect(searchTabNavi).toBeVisible();

      await expect(
        searchTabNavi.getByRole('link', { name: 'Henkilöhaku' }),
      ).toBeVisible();
      await expect(
        searchTabNavi.getByRole('link', { name: 'Tarkastusnäkymä' }),
      ).toBeHidden();
    });

    test('suora navigointi tarkastusnäkymään näyttää käyttöoikeusvirheen', async ({
      page,
    }) => {
      await page.goto('tarkastus');

      const searchTabNavi = page.getByRole('navigation', {
        name: 'Oppijoiden hakunäkymän valitsin',
      });

      await expect(searchTabNavi).toBeVisible();
      await expect(
        searchTabNavi.getByRole('link', { name: 'Henkilöhaku' }),
      ).toBeVisible();
      await expect(
        searchTabNavi.getByRole('link', { name: 'Tarkastusnäkymä' }),
      ).toBeVisible();

      await expect(
        page.getByText('Sinulla ei ole oikeuksia tarkastusnäkymään'),
      ).toBeVisible();
    });
  });

  test.describe('organisaation katselija', () => {
    test.beforeEach(async ({ page }) => {
      await stubKayttajaResponse(page, {
        isRekisterinpitaja: false,
        isOrganisaationKatselija: true,
      });
    });

    test('ohjautuu juuresta tarkastusnäkymään', async ({ page }) => {
      await stubKayttajaResponse(page, {
        isRekisterinpitaja: false,
        isOrganisaationKatselija: true,
      });
      await page.goto('');
      await expect(page).toHaveURL((url) =>
        url.toString().endsWith('/tarkastus'),
      );
    });

    test('näytetään vain Tarkastusnäkymä-välilehti', async ({ page }) => {
      const searchTabNavi = page.getByRole('navigation', {
        name: 'Oppijoiden hakunäkymän valitsin',
      });
      await page.goto('tarkastus');

      await expect(searchTabNavi).toBeVisible();

      await expect(
        searchTabNavi.getByRole('link', { name: 'Henkilöhaku' }),
      ).toBeHidden();
      await expect(
        searchTabNavi.getByRole('link', { name: 'Tarkastusnäkymä' }),
      ).toBeVisible();
    });

    test('suora navigointi henkilöhakuun näyttää käyttöoikeusvirheen', async ({
      page,
    }) => {
      await page.goto('henkilo');

      const searchTabNavi = page.getByRole('navigation', {
        name: 'Oppijoiden hakunäkymän valitsin',
      });

      await expect(searchTabNavi).toBeVisible();
      await expect(
        searchTabNavi.getByRole('link', { name: 'Henkilöhaku' }),
      ).toBeVisible();
      await expect(
        searchTabNavi.getByRole('link', { name: 'Tarkastusnäkymä' }),
      ).toBeVisible();

      await expect(
        page.getByText('Sinulla ei ole oikeuksia henkilöhakuun'),
      ).toBeVisible();
    });
  });

  test('Sivu latautuu käyttäjän asiointikielellä', async ({ page }) => {
    await page.goto('');
    await expect(page).toHaveTitle('Suorituspalvelu');
    await expect(
      page.getByRole('heading', { name: 'Suorituspalvelu' }),
    ).toBeVisible();

    await stubKayttajaResponse(page, { asiointiKieli: 'en' });

    await page.goto('');
    await expect(page).toHaveTitle('Study record service');
    await expect(
      page.getByRole('heading', { name: 'Study record service' }),
    ).toBeVisible();
  });
});
