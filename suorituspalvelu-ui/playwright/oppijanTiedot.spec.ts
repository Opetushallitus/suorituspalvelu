import { expect, Locator } from '@playwright/test';
import { test } from './fixtures';
import OPPIJAN_TIEDOT from './fixtures/oppijanTiedot.json';
import { NDASH } from '@/lib/common';

const OPPIJANUMERO = OPPIJAN_TIEDOT.oppijaNumero;

type Perustiedot = {
  title: string;
  oppilaitos: string;
  tila: string;
  valmistusmipaiva: string;
  suorituskieli?: string;
};

const expectPerustiedot = async (paper: Locator, perustiedot: Perustiedot) => {
  await expect(
    paper.getByRole('heading', { name: perustiedot.title }),
  ).toBeVisible();
  await expect(paper.getByLabel('Oppilaitos')).toHaveText(
    perustiedot.oppilaitos,
  );
  await expect(paper.getByLabel('Tila')).toHaveText(perustiedot.tila);
  await expect(paper.getByLabel('Valmistumispäivä')).toHaveText(
    perustiedot.valmistusmipaiva,
  );
  if (perustiedot.suorituskieli) {
    await expect(paper.getByLabel('Suorituskieli')).toHaveText(
      perustiedot.suorituskieli,
    );
  }
};

const expectList = async (list: Locator, items: Array<string>) => {
  await expect(list).toHaveCount(items.length);
  for (let i = 0; i < items.length; i++) {
    await expect(list.nth(i)).toHaveText(items[i] as string);
  }
};

test.describe('Oppijan tiedot', () => {
  test.beforeEach(async ({ page }) => {
    await page.clock.setFixedTime(new Date('2025-01-01T12:00:00Z'));
    await page.route(`**/ui/tiedot/${OPPIJANUMERO}`, async (route) => {
      await route.fulfill({
        json: OPPIJAN_TIEDOT,
      });
    });

    await page.route(`**/ui/oppilaitokset`, async (route) => {
      await route.fulfill({
        json: {
          oppilaitokset: [],
        },
      });
    });

    await page.goto(`?oppijaNumero=${OPPIJANUMERO}`);
  });

  test('Näytetään opiskeluoikeudet', async ({ page }) => {
    await expect(
      page.getByRole('heading', { name: 'Opiskeluoikeudet' }),
    ).toBeVisible();

    const opiskeluoikeusPapers = page.getByTestId('opiskeluoikeus-paper');

    await expect(opiskeluoikeusPapers).toHaveCount(1);
    const opiskeluoikeusPaper = opiskeluoikeusPapers.first();

    await expect(opiskeluoikeusPaper).toContainText(
      'Kasvatust. maist., kasvatustiede',
    );

    await expect(opiskeluoikeusPaper.getByLabel('Oppilaitos')).toHaveText(
      'Tampereen yliopisto',
    );
    await expect(opiskeluoikeusPaper.getByLabel('Voimassaolo')).toHaveText(
      `1.8.2001 ${NDASH} 11.12.2025Voimassa`,
    );
  });

  test('Näytetään suoritukset', async ({ page }) => {
    await expect(
      page.getByRole('heading', { name: 'Suoritukset' }),
    ).toBeVisible();

    const suoritusPapers = page.getByTestId('suoritus-paper');

    const suoritusPaper1 = suoritusPapers.first();

    await expectPerustiedot(suoritusPaper1, {
      title: 'Kasvatust. maist., kasvatustiede',
      oppilaitos: 'Tampereen yliopisto',
      tila: 'Suoritus kesken',
      valmistusmipaiva: '-',
    });

    await expect(suoritusPaper1.getByLabel('Hakukohde')).toHaveText(
      'Maisterihaku, luokanopettaja (opetus suomeksi), kasvatustieteiden maisteriohjelma, kasvatustieteen maisteri (2v)',
    );

    const suoritusPaper2 = suoritusPapers.nth(1);

    await expectPerustiedot(suoritusPaper2, {
      title: 'Ylioppilastutkinto (2019)',
      oppilaitos: 'Ylioppilastutkintolautakunta',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2019',
      suorituskieli: 'suomi',
    });

    const suoritusPaper3 = suoritusPapers.nth(2);

    await expectPerustiedot(suoritusPaper3, {
      title: 'Lukion oppimäärä (2024)',
      oppilaitos: 'Ylioppilastutkintolautakunta',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '31.12.2024',
      suorituskieli: 'suomi',
    });

    let oppiaineListItems = suoritusPaper3
      .getByLabel('Oppiaineet')
      .getByRole('listitem');

    await expectList(oppiaineListItems, [
      'Äidinkieli ja kirjallisuus',
      'Uskonto/Elämänkatsomustieto',
    ]);

    const suoritusPaper4 = suoritusPapers.nth(3);

    await expectPerustiedot(suoritusPaper4, {
      title: 'Lukion oppiaineen oppimäärä (2024)',
      oppilaitos: 'Ylioppilastutkintolautakunta',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '31.12.2024',
      suorituskieli: 'suomi',
    });

    oppiaineListItems = suoritusPaper4
      .getByLabel('Oppiaineet')
      .getByRole('listitem');

    await expectList(oppiaineListItems, [
      'Äidinkieli ja kirjallisuus, suomi äidinkielenä',
      'Matematiikka, lyhyt oppimäärä, valinnainen',
    ]);

    const suoritusPaper5 = suoritusPapers.nth(4);

    await expectPerustiedot(suoritusPaper5, {
      title: 'DIA-tutkinto (2024)',
      oppilaitos: 'Ylioppilastutkintolautakunta',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '31.12.2024',
      suorituskieli: 'suomi',
    });

    const suoritusPaper6 = suoritusPapers.nth(5);

    await expectPerustiedot(suoritusPaper6, {
      title: 'DIA-vastaavuustodistus (2024)',
      oppilaitos: 'Ylioppilastutkintolautakunta',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '31.12.2024',
      suorituskieli: 'suomi',
    });

    const suoritusPaper7 = suoritusPapers.nth(6);
    await expectPerustiedot(suoritusPaper7, {
      title: 'EB-tutkinto (European Baccalaureate) (2024)',
      oppilaitos: 'Ylioppilastutkintolautakunta',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '31.12.2024',
      suorituskieli: 'suomi',
    });

    oppiaineListItems = suoritusPaper7
      .getByLabel('Oppiaineet')
      .getByRole('listitem');

    await expectList(oppiaineListItems, [
      'Mathematics',
      'First language, ranska',
      'Second language, saksa',
    ]);

    const suoritusPaper8 = suoritusPapers.nth(7);
    await expectPerustiedot(suoritusPaper8, {
      title: 'IB-tutkinto (International Baccalaureate) (2024)',
      oppilaitos: 'Ylioppilastutkintolautakunta',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '31.12.2024',
      suorituskieli: 'suomi',
    });

    oppiaineListItems = suoritusPaper8
      .getByLabel('Oppiaineet')
      .getByRole('listitem');

    await expectList(oppiaineListItems, [
      'Studies in language and literature',
      'Individuals and societies',
      'Experimental sciences',
      'Mathematics',
    ]);

    const suoritusPaper9 = suoritusPapers.nth(8);
    await expectPerustiedot(suoritusPaper9, {
      title: 'Pre-IB (2024)',
      oppilaitos: 'Ylioppilastutkintolautakunta',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '31.12.2024',
      suorituskieli: 'suomi',
    });

    const suoritusPaper10 = suoritusPapers.nth(9);
    await expectPerustiedot(suoritusPaper10, {
      title: 'Puutarha-alan perustutkinto (2024)',
      oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '31.12.2024',
      suorituskieli: 'suomi',
    });

    const suoritusPaper11 = suoritusPapers.nth(10);
    await expectPerustiedot(suoritusPaper11, {
      title: 'Hevostalouden perustutkinto (2024)',
      oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '31.12.2024',
      suorituskieli: 'suomi',
    });

    const suoritusPaper12 = suoritusPapers.nth(11);
    await expectPerustiedot(suoritusPaper12, {
      title: 'Maanmittausalan ammattitutkinto (2017)',
      oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2017',
      suorituskieli: 'suomi',
    });

    const suoritusPaper13 = suoritusPapers.nth(12);
    await expectPerustiedot(suoritusPaper13, {
      title: 'Talous- ja henkilöstöalan erikoisammattitutkinto (2017)',
      oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2017',
      suorituskieli: 'suomi',
    });

    const suoritusPaper14 = suoritusPapers.nth(13);
    await expectPerustiedot(suoritusPaper14, {
      title: 'Tutkintokoulutukseen valmentava koulutus (2017)',
      oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2017',
      suorituskieli: 'suomi',
    });

    const suoritusPaper15 = suoritusPapers.nth(14);
    await expectPerustiedot(suoritusPaper15, {
      title: 'Kansanopistojen vapaan sivistystyön koulutus oppivelvollisille',
      oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
      tila: 'Suoritus keskeytynyt',
      valmistusmipaiva: '-',
      suorituskieli: 'suomi',
    });

    const suoritusPaper16 = suoritusPapers.nth(15);
    await expectPerustiedot(suoritusPaper16, {
      title: 'Perusopetuksen oppimäärä (2016)',
      oppilaitos: 'Keltinmäen koulu',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2016',
      suorituskieli: 'suomi',
    });

    await expect(suoritusPaper16.getByLabel('Luokka')).toHaveText('9A');
    await expect(suoritusPaper16.getByLabel('Yksilöllistetty')).toHaveText(
      'Ei',
    );

    const suoritusPaper17 = suoritusPapers.nth(16);
    await expectPerustiedot(suoritusPaper17, {
      title: 'Perusopetuksen oppimäärä (2016)',
      oppilaitos: 'Keltinmäen koulu',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2016',
      suorituskieli: 'suomi',
    });

    await expect(suoritusPaper17.getByLabel('Luokka')).toHaveText('9A');
    await expect(suoritusPaper17.getByLabel('Yksilöllistetty')).toHaveText(
      'Ei',
    );

    const suoritusPaper18 = suoritusPapers.nth(17);
    await expectPerustiedot(suoritusPaper18, {
      title: 'Nuorten perusopetuksen oppiaineen oppimäärä (2016)',
      oppilaitos: 'Keltinmäen koulu',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2016',
      suorituskieli: 'suomi',
    });

    const suoritusPaper19 = suoritusPapers.nth(18);
    await expectPerustiedot(suoritusPaper19, {
      title: 'Perusopetuksen oppiaineen oppimäärä (2016)',
      oppilaitos: 'Keltinmäen koulu',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2016',
      suorituskieli: 'suomi',
    });

    const suoritusPaper20 = suoritusPapers.nth(19);
    await expectPerustiedot(suoritusPaper20, {
      title: 'Aikuisten perusopetuksen oppimäärä (2016)',
      oppilaitos: 'Keltinmäen koulu',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2016',
      suorituskieli: 'suomi',
    });
  });
});
