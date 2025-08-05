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

async function expectSuoritusPaper(
  papers: Locator,
  index: number,
  perustiedot: Perustiedot,
  additionalChecks?: (paper: Locator) => Promise<void>,
) {
  const paper = papers.nth(index);
  await expectPerustiedot(paper, perustiedot);
  await additionalChecks?.(paper);
}

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

  test('Opiskeluoikeudet', async ({ page }) => {
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

  test('Suoritukset koulutustyypeittäin', async ({ page }) => {
    await expect(
      page.getByRole('heading', { name: 'Suoritukset' }),
    ).toBeVisible();

    const suoritusPapers = page.getByTestId('suoritus-paper');

    await expectSuoritusPaper(
      suoritusPapers,
      0,
      {
        title: 'Kasvatust. maist., kasvatustiede',
        oppilaitos: 'Tampereen yliopisto',
        tila: 'Suoritus kesken',
        valmistusmipaiva: '-',
      },
      async (paper) => {
        await expect(paper.getByLabel('Hakukohde')).toHaveText(
          'Maisterihaku, luokanopettaja (opetus suomeksi), kasvatustieteiden maisteriohjelma, kasvatustieteen maisteri (2v)',
        );
      },
    );

    await expectSuoritusPaper(suoritusPapers, 1, {
      title: 'Ylioppilastutkinto (2019)',
      oppilaitos: 'Ylioppilastutkintolautakunta',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2019',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(
      suoritusPapers,
      2,
      {
        title: 'Lukion oppimäärä (2024)',
        oppilaitos: 'Ylioppilastutkintolautakunta',
        tila: 'Suoritus valmis',
        valmistusmipaiva: '31.12.2024',
        suorituskieli: 'suomi',
      },
      async (paper) => {
        const oppiaineListItems = paper
          .getByLabel('Oppiaineet')
          .getByRole('listitem');
        await expectList(oppiaineListItems, [
          'Äidinkieli ja kirjallisuus',
          'Uskonto/Elämänkatsomustieto',
        ]);
      },
    );

    await expectSuoritusPaper(
      suoritusPapers,
      3,
      {
        title: 'Lukion oppiaineen oppimäärä (2024)',
        oppilaitos: 'Ylioppilastutkintolautakunta',
        tila: 'Suoritus valmis',
        valmistusmipaiva: '31.12.2024',
        suorituskieli: 'suomi',
      },
      async (paper) => {
        const oppiaineListItems = paper
          .getByLabel('Oppiaineet')
          .getByRole('listitem');
        await expectList(oppiaineListItems, [
          'Äidinkieli ja kirjallisuus, suomi äidinkielenä',
          'Matematiikka, lyhyt oppimäärä, valinnainen',
        ]);
      },
    );

    await expectSuoritusPaper(suoritusPapers, 4, {
      title: 'DIA-tutkinto (2024)',
      oppilaitos: 'Ylioppilastutkintolautakunta',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '31.12.2024',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(suoritusPapers, 5, {
      title: 'DIA-vastaavuustodistus (2024)',
      oppilaitos: 'Ylioppilastutkintolautakunta',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '31.12.2024',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(
      suoritusPapers,
      6,
      {
        title: 'EB-tutkinto (European Baccalaureate) (2024)',
        oppilaitos: 'Ylioppilastutkintolautakunta',
        tila: 'Suoritus valmis',
        valmistusmipaiva: '31.12.2024',
        suorituskieli: 'suomi',
      },
      async (paper) => {
        const oppiaineListItems = paper
          .getByLabel('Oppiaineet')
          .getByRole('listitem');
        await expectList(oppiaineListItems, [
          'Mathematics',
          'First language, ranska',
          'Second language, saksa',
        ]);
      },
    );

    await expectSuoritusPaper(
      suoritusPapers,
      7,
      {
        title: 'IB-tutkinto (International Baccalaureate) (2024)',
        oppilaitos: 'Ylioppilastutkintolautakunta',
        tila: 'Suoritus valmis',
        valmistusmipaiva: '31.12.2024',
        suorituskieli: 'suomi',
      },
      async (paper) => {
        const oppiaineListItems = paper
          .getByLabel('Oppiaineet')
          .getByRole('listitem');
        await expectList(oppiaineListItems, [
          'Studies in language and literature',
          'Individuals and societies',
          'Experimental sciences',
          'Mathematics',
        ]);
      },
    );

    await expectSuoritusPaper(suoritusPapers, 8, {
      title: 'Pre-IB (2024)',
      oppilaitos: 'Ylioppilastutkintolautakunta',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '31.12.2024',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(suoritusPapers, 9, {
      title: 'Puutarha-alan perustutkinto (2024)',
      oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '31.12.2024',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(suoritusPapers, 10, {
      title: 'Hevostalouden perustutkinto (2024)',
      oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '31.12.2024',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(suoritusPapers, 11, {
      title: 'Maanmittausalan ammattitutkinto (2017)',
      oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2017',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(suoritusPapers, 12, {
      title: 'Talous- ja henkilöstöalan erikoisammattitutkinto (2017)',
      oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2017',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(suoritusPapers, 13, {
      title: 'Tutkintokoulutukseen valmentava koulutus (2017)',
      oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2017',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(suoritusPapers, 14, {
      title: 'Kansanopistojen vapaan sivistystyön koulutus oppivelvollisille',
      oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
      tila: 'Suoritus keskeytynyt',
      valmistusmipaiva: '-',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(
      suoritusPapers,
      15,
      {
        title: 'Perusopetuksen oppimäärä (2016)',
        oppilaitos: 'Keltinmäen koulu',
        tila: 'Suoritus valmis',
        valmistusmipaiva: '1.6.2016',
        suorituskieli: 'suomi',
      },
      async (paper) => {
        await expect(paper.getByLabel('Luokka')).toHaveText('9A');
        await expect(paper.getByLabel('Yksilöllistetty')).toHaveText('Ei');
      },
    );

    await expectSuoritusPaper(
      suoritusPapers,
      16,
      {
        title: 'Perusopetuksen oppimäärä (2016)',
        oppilaitos: 'Keltinmäen koulu',
        tila: 'Suoritus valmis',
        valmistusmipaiva: '1.6.2016',
        suorituskieli: 'suomi',
      },
      async (paper) => {
        await expect(paper.getByLabel('Luokka')).toHaveText('9A');
        await expect(paper.getByLabel('Yksilöllistetty')).toHaveText('Ei');
      },
    );

    await expectSuoritusPaper(suoritusPapers, 17, {
      title: 'Nuorten perusopetuksen oppiaineen oppimäärä (2016)',
      oppilaitos: 'Keltinmäen koulu',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2016',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(suoritusPapers, 18, {
      title: 'Perusopetuksen oppiaineen oppimäärä (2016)',
      oppilaitos: 'Keltinmäen koulu',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2016',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(suoritusPapers, 19, {
      title: 'Aikuisten perusopetuksen oppimäärä (2016)',
      oppilaitos: 'Keltinmäen koulu',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2016',
      suorituskieli: 'suomi',
    });
  });

  test('Suoritukset aikajärjestyksessä', async ({ page }) => {
    await expect(
      page.getByRole('heading', { name: 'Suoritukset' }),
    ).toBeVisible();

    await page.getByRole('button', { name: 'Uusin ensin' }).click();

    const suoritusPapers = page.getByTestId('suoritus-paper');

    await expectSuoritusPaper(
      suoritusPapers,
      0,
      {
        title: 'Kasvatust. maist., kasvatustiede',
        oppilaitos: 'Tampereen yliopisto',
        tila: 'Suoritus kesken',
        valmistusmipaiva: '-',
      },
      async (paper) => {
        await expect(paper.getByLabel('Hakukohde')).toHaveText(
          'Maisterihaku, luokanopettaja (opetus suomeksi), kasvatustieteiden maisteriohjelma, kasvatustieteen maisteri (2v)',
        );
      },
    );

    await expectSuoritusPaper(
      suoritusPapers,
      1,
      {
        title: 'Lukion oppimäärä (2024)',
        oppilaitos: 'Ylioppilastutkintolautakunta',
        tila: 'Suoritus valmis',
        valmistusmipaiva: '31.12.2024',
        suorituskieli: 'suomi',
      },
      async (paper) => {
        const oppiaineListItems = paper
          .getByLabel('Oppiaineet')
          .getByRole('listitem');

        await expectList(oppiaineListItems, [
          'Äidinkieli ja kirjallisuus',
          'Uskonto/Elämänkatsomustieto',
        ]);
      },
    );

    await expectSuoritusPaper(
      suoritusPapers,
      2,
      {
        title: 'Lukion oppiaineen oppimäärä (2024)',
        oppilaitos: 'Ylioppilastutkintolautakunta',
        tila: 'Suoritus valmis',
        valmistusmipaiva: '31.12.2024',
        suorituskieli: 'suomi',
      },
      async (paper) => {
        const oppiaineListItems = paper
          .getByLabel('Oppiaineet')
          .getByRole('listitem');
        await expectList(oppiaineListItems, [
          'Äidinkieli ja kirjallisuus, suomi äidinkielenä',
          'Matematiikka, lyhyt oppimäärä, valinnainen',
        ]);
      },
    );

    await expectSuoritusPaper(suoritusPapers, 3, {
      title: 'DIA-tutkinto (2024)',
      oppilaitos: 'Ylioppilastutkintolautakunta',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '31.12.2024',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(suoritusPapers, 4, {
      title: 'DIA-vastaavuustodistus (2024)',
      oppilaitos: 'Ylioppilastutkintolautakunta',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '31.12.2024',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(
      suoritusPapers,
      5,
      {
        title: 'EB-tutkinto (European Baccalaureate) (2024)',
        oppilaitos: 'Ylioppilastutkintolautakunta',
        tila: 'Suoritus valmis',
        valmistusmipaiva: '31.12.2024',
        suorituskieli: 'suomi',
      },
      async (paper) => {
        const oppiaineListItems = paper
          .getByLabel('Oppiaineet')
          .getByRole('listitem');
        await expectList(oppiaineListItems, [
          'Mathematics',
          'First language, ranska',
          'Second language, saksa',
        ]);
      },
    );

    await expectSuoritusPaper(
      suoritusPapers,
      6,
      {
        title: 'IB-tutkinto (International Baccalaureate) (2024)',
        oppilaitos: 'Ylioppilastutkintolautakunta',
        tila: 'Suoritus valmis',
        valmistusmipaiva: '31.12.2024',
        suorituskieli: 'suomi',
      },
      async (paper) => {
        const oppiaineListItems = paper
          .getByLabel('Oppiaineet')
          .getByRole('listitem');
        await expectList(oppiaineListItems, [
          'Studies in language and literature',
          'Individuals and societies',
          'Experimental sciences',
          'Mathematics',
        ]);
      },
    );

    await expectSuoritusPaper(suoritusPapers, 7, {
      title: 'Pre-IB (2024)',
      oppilaitos: 'Ylioppilastutkintolautakunta',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '31.12.2024',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(suoritusPapers, 8, {
      title: 'Puutarha-alan perustutkinto (2024)',
      oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '31.12.2024',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(suoritusPapers, 9, {
      title: 'Hevostalouden perustutkinto (2024)',
      oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '31.12.2024',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(suoritusPapers, 10, {
      title: 'Ylioppilastutkinto (2019)',
      oppilaitos: 'Ylioppilastutkintolautakunta',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2019',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(suoritusPapers, 11, {
      title: 'Maanmittausalan ammattitutkinto (2017)',
      oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2017',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(suoritusPapers, 12, {
      title: 'Talous- ja henkilöstöalan erikoisammattitutkinto (2017)',
      oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2017',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(suoritusPapers, 13, {
      title: 'Tutkintokoulutukseen valmentava koulutus (2017)',
      oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2017',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(
      suoritusPapers,
      14,
      {
        title: 'Perusopetuksen oppimäärä (2016)',
        oppilaitos: 'Keltinmäen koulu',
        tila: 'Suoritus valmis',
        valmistusmipaiva: '1.6.2016',
        suorituskieli: 'suomi',
      },
      async (paper) => {
        await expect(paper.getByLabel('Luokka')).toHaveText('9A');
        await expect(paper.getByLabel('Yksilöllistetty')).toHaveText('Ei');
      },
    );

    await expectSuoritusPaper(
      suoritusPapers,
      15,
      {
        title: 'Perusopetuksen oppimäärä (2016)',
        oppilaitos: 'Keltinmäen koulu',
        tila: 'Suoritus valmis',
        valmistusmipaiva: '1.6.2016',
        suorituskieli: 'suomi',
      },
      async (paper) => {
        await expect(paper.getByLabel('Luokka')).toHaveText('9A');
        await expect(paper.getByLabel('Yksilöllistetty')).toHaveText('Ei');
      },
    );

    await expectSuoritusPaper(suoritusPapers, 16, {
      title: 'Nuorten perusopetuksen oppiaineen oppimäärä (2016)',
      oppilaitos: 'Keltinmäen koulu',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2016',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(suoritusPapers, 17, {
      title: 'Perusopetuksen oppiaineen oppimäärä (2016)',
      oppilaitos: 'Keltinmäen koulu',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2016',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(suoritusPapers, 18, {
      title: 'Aikuisten perusopetuksen oppimäärä (2016)',
      oppilaitos: 'Keltinmäen koulu',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2016',
      suorituskieli: 'suomi',
    });

    await expectSuoritusPaper(suoritusPapers, 19, {
      title: 'Kansanopistojen vapaan sivistystyön koulutus oppivelvollisille',
      oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
      tila: 'Suoritus keskeytynyt',
      valmistusmipaiva: '-',
      suorituskieli: 'suomi',
    });
  });
});
