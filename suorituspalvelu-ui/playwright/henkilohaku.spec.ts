import { expect } from '@playwright/test';
import { test } from './lib/fixtures';

const OPPIJAT = [
  {
    oppijaNumero: '1.2.3.4',
    etunimet: 'Olli',
    sukunimi: 'Oppija',
    hetu: '123456-7890',
    oppilaitosOid: '1',
  },
  {
    oppijaNumero: '2.3.4.5',
    etunimet: 'Maija',
    sukunimi: 'Mallikas',
    hetu: '098765-4321',
    oppilaitosOid: '2',
  },
];

test.describe('Henkilö-haku', () => {
  test.beforeEach(async ({ page }) => {
    await page.route('**/ui/haku/oppijat*', async (route) => {
      const url = new URL(route.request().url());
      const tunnisteParam = url.searchParams.get('tunniste');

      await route.fulfill({
        json: {
          oppijat: OPPIJAT.filter(
            (oppija) => oppija.oppijaNumero === tunnisteParam,
          ),
        },
      });
    });
  });

  test('suodattaa oppijanumerolla', async ({ page }) => {
    await page.goto('');
    const searchInput = page.getByRole('textbox', { name: 'Hae Henkilöä' });

    const henkiloNavi = page.getByRole('navigation', {
      name: 'Henkilövalitsin',
    });
    const henkiloLinks = henkiloNavi.getByRole('link');
    await expect(henkiloLinks).toHaveCount(0);

    await searchInput.fill('1.2.3.4');

    await expect(henkiloLinks).toHaveCount(1);
    await expect(henkiloLinks.first()).toHaveText('Olli Oppija123456-7890');
  });
});
