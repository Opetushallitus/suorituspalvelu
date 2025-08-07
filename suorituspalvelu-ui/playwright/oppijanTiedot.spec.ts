import { expect, Locator, Page } from '@playwright/test';
import { test } from './fixtures';
import OPPIJAN_TIEDOT from './fixtures/oppijanTiedot.json';
import { NDASH } from '@/lib/common';
import { checkTable } from './playwright-utils';

const OPPIJANUMERO = OPPIJAN_TIEDOT.oppijaNumero;

type Perustiedot = {
  title: string;
  oppilaitos: string;
  tila: string;
  valmistusmipaiva: string;
  suorituskieli?: string;
};

type SuoritusSpec = {
  perustiedot: Perustiedot;
  additionalChecks?: (paper: Locator) => Promise<void>;
};

async function expectPerustiedot(paper: Locator, perustiedot: Perustiedot) {
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
}

async function expectSuoritukset(
  page: Page,
  suoritusSpecs: Array<SuoritusSpec>,
) {
  await expect(
    page.getByRole('heading', { name: 'Suoritukset' }),
  ).toBeVisible();
  const suoritusPapers = page.getByTestId('suoritus-paper');

  for (const [index, spec] of suoritusSpecs.entries()) {
    const paper = suoritusPapers.nth(index);
    await expectPerustiedot(paper, spec.perustiedot);
    await spec.additionalChecks?.(paper);
  }
}

const expectList = async (list: Locator, items: Array<string>) => {
  await expect(list).toHaveCount(items.length);
  for (let i = 0; i < items.length; i++) {
    await expect(list.nth(i)).toHaveText(items[i] as string);
  }
};

const KORKEAKOULU_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Kasvatust. maist., kasvatustiede',
    oppilaitos: 'Tampereen yliopisto',
    tila: 'Suoritus kesken',
    valmistusmipaiva: '-',
  },
  additionalChecks: async (paper) => {
    await expect(paper.getByLabel('Hakukohde')).toHaveText(
      'Maisterihaku, luokanopettaja (opetus suomeksi), kasvatustieteiden maisteriohjelma, kasvatustieteen maisteri (2v)',
    );
  },
};

const YOTUTKINTO_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Ylioppilastutkinto (2019)',
    oppilaitos: 'Ylioppilastutkintolautakunta',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '1.6.2019',
  },
  additionalChecks: async (paper) => {
    const yoTable = paper.getByRole('table');
    await checkTable(yoTable, [
      ['Aine', 'Taso', 'Arvosana', 'Yhteispistemäärä', 'Tutkintokerta'],
      ['Psykologia', 'Ainemuotoinen reaali', 'E', '28', '21.12.2018'],
      ['Englanti', 'Pitkä oppimäärä (KIELI)', 'E', '259', '1.6.2019'],
      ['Matematiikka', 'Lyhyt oppimäärä (MA)', 'C', '23', '1.6.2019'],
      ['Suomi', 'Äidinkieli', 'C', '49', '1.6.2019'],
      ['Historia', 'Ainemuotoinen reaali', 'M', '25', '1.6.2019'],
      ['Yhteiskuntaoppi', 'Ainemuotoinen reaali', 'E', '32', '1.6.2019'],
    ]);
  },
};

const LUKION_OPPIMAARA_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Lukion oppimäärä (2024)',
    oppilaitos: 'Ylioppilastutkintolautakunta',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '31.12.2024',
  },
  additionalChecks: async (paper) => {
    const oppiaineListItems = paper
      .getByLabel('Oppiaineet')
      .getByRole('listitem');

    await expectList(oppiaineListItems, [
      'Äidinkieli ja kirjallisuus',
      'Uskonto/Elämänkatsomustieto',
    ]);
  },
};

const LUKION_OPPIAINEEN_OPPIMAARA_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Lukion oppiaineen oppimäärä (2024)',
    oppilaitos: 'Ylioppilastutkintolautakunta',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '31.12.2024',
  },
  additionalChecks: async (paper) => {
    const oppiaineListItems = paper
      .getByLabel('Oppiaineet')
      .getByRole('listitem');

    await expectList(oppiaineListItems, [
      'Äidinkieli ja kirjallisuus, suomi äidinkielenä',
      'Matematiikka, lyhyt oppimäärä, valinnainen',
    ]);
  },
};

const DIA_TUTKINTO_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'DIA-tutkinto (2024)',
    oppilaitos: 'Ylioppilastutkintolautakunta',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '31.12.2024',
    suorituskieli: 'suomi',
  },
};

const DIA_VASTAAVUUSTODISTUS_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'DIA-vastaavuustodistus (2024)',
    oppilaitos: 'Ylioppilastutkintolautakunta',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '31.12.2024',
    suorituskieli: 'suomi',
  },
  additionalChecks: async (paper) => {
    const diaTable = paper.getByRole('table');
    await checkTable(diaTable, [
      ['Oppiaine', 'Laajuus (vvt)', 'Keskiarvo'],
      ['Kielet, kirjallisuus, taide'],
      ['A-kieli, englanti', '3', '8.5'],
      ['Historia', '2', '8.5'],
      ['Matematiikka ja luonnontieteet'],
      ['Matematiikka', '3', '6'],
      ['Kuvataide', '3', '8.5'],
    ]);
  },
};

const EB_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'EB-tutkinto (European Baccalaureate) (2024)',
    oppilaitos: 'Ylioppilastutkintolautakunta',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '31.12.2024',
    suorituskieli: 'suomi',
  },
  additionalChecks: async (paper) => {
    const ebTable = paper.getByRole('table');
    await checkTable(ebTable, [
      ['Oppiaine', 'Suorituskieli', 'Laajuus (vvt)', 'Arvosana'],
      ['Mathematics', 'englanti', '4', ''],
      ['Written', '8.67'],
      ['Oral', ''],
      ['Final', '8.67'],
      ['First language, ranska', 'englanti', '3', ''],
      ['Written', '8.67'],
      ['Oral', '8.67'],
      ['Final', '8.67'],
      ['Second language, saksa', 'englanti', '3', ''],
      ['Written', '8.67'],
      ['Oral', '8.67'],
      ['Final', '8.67'],
    ]);
  },
};

const IB_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'IB-tutkinto (International Baccalaureate) (2024)',
    oppilaitos: 'Ylioppilastutkintolautakunta',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '31.12.2024',
    suorituskieli: 'suomi',
  },
  additionalChecks: async (paper) => {
    const ibTable = paper.getByRole('table');
    await checkTable(ibTable, [
      ['Oppiaine', 'Laajuus (vvt)', 'Predicted grade', 'Arvosana'],
      ['Studies in language and literature'],
      ['Language A: literature, suomi', '9', '7', '7'],
      ['Language A: language and literature, englanti', '6', '7', '7'],
      ['Individuals and societies'],
      ['History', '3', '7', '7'],
      ['Psychology', '3', '7', '7'],
      ['Experimental sciences'],
      ['Biology', '3', '7', '7'],
      ['Mathematics'],
      ['Mathematical studies', '3', '7', '7'],
    ]);
  },
};

const PRE_IB_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Pre-IB (2024)',
    oppilaitos: 'Ylioppilastutkintolautakunta',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '31.12.2024',
    suorituskieli: 'suomi',
  },
};
const PUUTARHA_ALAN_PERUSTUTKINTO_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Puutarha-alan perustutkinto (2024)',
    oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '31.12.2024',
    suorituskieli: 'suomi',
  },
};

const HEVOSTALOUDEN_PERUSTUTKINTO_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Hevostalouden perustutkinto (2024)',
    oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '31.12.2024',
    suorituskieli: 'suomi',
  },
  additionalChecks: async (paper) => {
    await expect(paper.getByLabel('Suoritustapa')).toHaveText('Näyttötutkinto');
  },
};

const MAANMITTAUSALAN_PERUSTUTKINTO_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Maanmittausalan ammattitutkinto (2017)',
    oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '1.6.2017',
    suorituskieli: 'suomi',
  },
};

const TALOUS_JA_HENKILOSTOALAN_ERIKOISAMMATTITUTKINTO_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Talous- ja henkilöstöalan erikoisammattitutkinto (2017)',
    oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '1.6.2017',
    suorituskieli: 'suomi',
  },
};

const TELMA_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Työhön ja itsenäiseen elämään valmentava koulutus (2017)',
    oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '1.6.2017',
    suorituskieli: 'suomi',
  },
};

const TUVA_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Tutkintokoulutukseen valmentava koulutus (2017)',
    oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '1.6.2017',
    suorituskieli: 'suomi',
  },
  additionalChecks: async (paper) => {
    await expect(paper.getByLabel('Suoritettu')).toHaveText('38 vk');
  },
};

const VAPAA_SIVISTYSTYO_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Kansanopistojen vapaan sivistystyön koulutus oppivelvollisille',
    oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
    tila: 'Suoritus keskeytynyt',
    valmistusmipaiva: '-',
    suorituskieli: 'suomi',
  },
  additionalChecks: async (paper) => {
    await expect(paper.getByLabel('Suoritettu')).toHaveText('38 op');
  },
};

const PERUSOPETUKSEN_OPPIMAARA_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Perusopetuksen oppimäärä (2016)',
    oppilaitos: 'Keltinmäen koulu',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '1.6.2016',
    suorituskieli: 'suomi',
  },
  additionalChecks: async (paper) => {
    await expect(paper.getByLabel('Luokka')).toHaveText('9A');
    await expect(paper.getByLabel('Yksilöllistetty')).toHaveText('Ei');

    const oppiaineetTable = paper.getByRole('table');
    await checkTable(oppiaineetTable, [
      ['Oppiaine', 'Arvosana', 'Valinnainen'],
      ['Äidinkieli ja kirjallisuus, suomen kieli ja kirjallisuus', '9', ''],
      ['A1-kieli, englanti', '9', ''],
      ['B1-kieli, ruotsi', '9', 'S'],
      ['B2-kieli, saksa', '', 'S'],
      ['Matematiikka', '9', ''],
      ['Biologia', '9', ''],
      ['Maantieto', '9', ''],
      ['Fysiikka', '9', ''],
      ['Kemia', '9', ''],
      ['Terveystieto', '9', ''],
      ['Uskonto tai elämänkatsomustieto', '9', ''],
    ]);
  },
};

const PERUSOPETUKSEN_OPPIMAARA_78LUOKKA_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Perusopetuksen oppimäärä (2016)',
    oppilaitos: 'Keltinmäen koulu',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '1.6.2016',
    suorituskieli: 'suomi',
  },
  additionalChecks: async (paper) => {
    await expect(paper.getByLabel('Koulusivistyskieli')).toHaveText('suomi');
    await expect(paper.getByLabel('Luokka')).toHaveText('9A');
    await expect(paper.getByLabel('Yksilöllistetty')).toHaveText('Ei');
  },
};

const NUORTEN_PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Nuorten perusopetuksen oppiaineen oppimäärä (2016)',
    oppilaitos: 'Keltinmäen koulu',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '1.6.2016',
    suorituskieli: 'suomi',
  },
  additionalChecks: async (paper) => {
    const oppiaineetTable = paper.getByRole('table');
    await checkTable(oppiaineetTable, [
      ['Oppiaine', 'Arvosana'],
      ['Biologia', '9'],
      ['Historia', '8'],
    ]);
  },
};

const PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Perusopetuksen oppiaineen oppimäärä (2016)',
    oppilaitos: 'Keltinmäen koulu',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '1.6.2016',
    suorituskieli: 'suomi',
  },
  additionalChecks: async (paper) => {
    const oppiaineetTable = paper.getByRole('table');
    await checkTable(oppiaineetTable, [
      ['Oppiaine', 'Arvosana'],
      ['matematiikka', '9'],
    ]);
  },
};

const AIKUISTEN_PERUSOPETUKSEN_OPPIMAARA_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Aikuisten perusopetuksen oppimäärä (2016)',
    oppilaitos: 'Keltinmäen koulu',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '1.6.2016',
    suorituskieli: 'suomi',
  },
  additionalChecks: async (paper) => {
    const oppiaineetTable = paper.getByRole('table');
    await checkTable(oppiaineetTable, [
      ['Oppiaine', 'Arvosana', 'Valinnainen'],
      ['Äidinkieli ja kirjallisuus, suomen kieli ja kirjallisuus', '9', ''],
      ['A1-kieli, englanti', '9', ''],
      ['B1-kieli, ruotsi', '9', ''],
      ['B2-kieli, saksa', '9', ''],
      ['Matematiikka', '', '10'],
      ['Biologia', '', '9'],
      ['Maantieto', '', '8'],
      ['Fysiikka', '', '9'],
    ]);
  },
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
    await expectSuoritukset(page, [
      KORKEAKOULU_SUORITUS,
      YOTUTKINTO_SUORITUS,
      LUKION_OPPIMAARA_SUORITUS,
      LUKION_OPPIAINEEN_OPPIMAARA_SUORITUS,
      DIA_TUTKINTO_SUORITUS,
      DIA_VASTAAVUUSTODISTUS_SUORITUS,
      EB_SUORITUS,
      IB_SUORITUS,
      PRE_IB_SUORITUS,
      PUUTARHA_ALAN_PERUSTUTKINTO_SUORITUS,
      HEVOSTALOUDEN_PERUSTUTKINTO_SUORITUS,
      MAANMITTAUSALAN_PERUSTUTKINTO_SUORITUS,
      TALOUS_JA_HENKILOSTOALAN_ERIKOISAMMATTITUTKINTO_SUORITUS,
      TELMA_SUORITUS,
      TUVA_SUORITUS,
      VAPAA_SIVISTYSTYO_SUORITUS,
      PERUSOPETUKSEN_OPPIMAARA_SUORITUS,
      PERUSOPETUKSEN_OPPIMAARA_78LUOKKA_SUORITUS,
      NUORTEN_PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA_SUORITUS,
      PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA_SUORITUS,
      AIKUISTEN_PERUSOPETUKSEN_OPPIMAARA_SUORITUS,
    ]);
  });

  test('Suoritukset aikajärjestyksessä', async ({ page }) => {
    await page.getByRole('button', { name: 'Uusin ensin' }).click();
    await expectSuoritukset(page, [
      KORKEAKOULU_SUORITUS,
      LUKION_OPPIMAARA_SUORITUS,
      LUKION_OPPIAINEEN_OPPIMAARA_SUORITUS,
      DIA_TUTKINTO_SUORITUS,
      DIA_VASTAAVUUSTODISTUS_SUORITUS,
      EB_SUORITUS,
      IB_SUORITUS,
      PRE_IB_SUORITUS,
      PUUTARHA_ALAN_PERUSTUTKINTO_SUORITUS,
      HEVOSTALOUDEN_PERUSTUTKINTO_SUORITUS,
      YOTUTKINTO_SUORITUS,
      MAANMITTAUSALAN_PERUSTUTKINTO_SUORITUS,
      TALOUS_JA_HENKILOSTOALAN_ERIKOISAMMATTITUTKINTO_SUORITUS,
      TELMA_SUORITUS,
      TUVA_SUORITUS,
      PERUSOPETUKSEN_OPPIMAARA_SUORITUS,
      PERUSOPETUKSEN_OPPIMAARA_78LUOKKA_SUORITUS,
      NUORTEN_PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA_SUORITUS,
      PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA_SUORITUS,
      AIKUISTEN_PERUSOPETUKSEN_OPPIMAARA_SUORITUS,
      VAPAA_SIVISTYSTYO_SUORITUS,
    ]);
  });
});
