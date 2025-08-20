import { expect } from '@playwright/test';
import { test } from './lib/fixtures';
import { selectOption } from './lib/playwrightUtils';

const OPPIJAT = [
  {
    oppijaNumero: '1',
    nimi: 'Olli Oppija',
    hetu: '123456-7890',
    oppilaitosOid: '1',
  },
  {
    oppijaNumero: '2',
    nimi: 'Maija Mallikas',
    hetu: '098765-4321',
    oppilaitosOid: '2',
  },
];

test.describe('Henkilö-haku', () => {
  test.beforeEach(async ({ page }) => {
    await page.route('**/ui/oppijat*', async (route) => {
      const url = new URL(route.request().url());
      const oppijaParam = url.searchParams.get('oppija');
      const oppilaitosParam = url.searchParams.get('oppilaitos');

      await route.fulfill({
        json: {
          oppijat: OPPIJAT.filter(
            (oppija) =>
              oppija.nimi.includes(oppijaParam ?? '') &&
              (!oppilaitosParam || oppilaitosParam === oppija.oppilaitosOid),
          ),
        },
      });
    });

    await page.route(`**/ui/oppilaitokset`, async (route) => {
      await route.fulfill({
        json: {
          oppilaitokset: [
            { oid: '1', nimi: { fi: 'Oppilaitos 1' } },
            { oid: '2', nimi: { fi: 'Oppilaitos 2' } },
          ],
        },
      });
    });
  });

  test('suodattaa nimellä ja oppilaitoksella', async ({ page }) => {
    await page.goto('');
    const searchInput = page.getByRole('textbox', { name: 'Hae Henkilöä' });

    await searchInput.fill('Olli');
    await selectOption({
      page,
      name: 'Oppilaitos',
      option: 'Oppilaitos 1',
    });

    const henkiloNavi = page.getByRole('navigation', {
      name: 'Oppijavalitsin',
    });

    const henkiloLinks = henkiloNavi.getByRole('link');
    await expect(henkiloLinks).toHaveCount(1);
    await expect(henkiloLinks.first()).toHaveText('Olli Oppija123456-7890');

    await selectOption({
      page,
      name: 'Oppilaitos',
      option: 'Oppilaitos 2',
    });

    await expect(henkiloLinks).toHaveCount(0);
  });
});
