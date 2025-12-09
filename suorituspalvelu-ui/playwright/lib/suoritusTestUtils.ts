import { expect, type Locator, type Page } from '@playwright/test';
import { checkTable, expectList } from './playwrightUtils';
import { NDASH } from '@/lib/common';

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

export const KORKEAKOULU_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: `Kasvatust. maist., kasvatustiede (2025 ${NDASH} kesken)`,
    oppilaitos: 'Tampereen yliopisto',
    tila: 'Suoritus kesken',
    valmistusmipaiva: '-',
  },
};

export const YOTUTKINTO_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Ylioppilastutkinto (2019)',
    oppilaitos: 'Ylioppilastutkintolautakunta',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '1.6.2019',
  },
  additionalChecks: async (paper) => {
    const yoTable = paper.getByRole('table');
    await checkTable(yoTable, [
      ['Aine', 'Arvosana', 'Yhteispistemäärä', 'Tutkintokerta'],
      ['Psykologia', 'E', '28', '21.12.2018'],
      ['Englanti', 'E', '259', '1.6.2019'],
      ['Matematiikka', 'C', '23', '1.6.2019'],
      ['Suomi', 'C', '49', '1.6.2019'],
      ['Historia', 'M', '25', '1.6.2019'],
      ['Yhteiskuntaoppi', 'E', '32', '1.6.2019'],
    ]);
  },
};

export const LUKION_OPPIMAARA_SUORITUS: SuoritusSpec = {
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

export const LUKION_OPPIAINEEN_OPPIMAARA_SUORITUS: SuoritusSpec = {
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

export const DIA_TUTKINTO_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'DIA-tutkinto (2024)',
    oppilaitos: 'Ylioppilastutkintolautakunta',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '31.12.2024',
    suorituskieli: 'suomi',
  },
};

export const DIA_VASTAAVUUSTODISTUS_SUORITUS: SuoritusSpec = {
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
      ['A-kieli, englanti', '3', '8,5'],
      ['Historia', '2', '8,5'],
      ['Matematiikka ja luonnontieteet'],
      ['Matematiikka', '3', '6'],
      ['Kuvataide', '3', '8,5'],
    ]);
  },
};

export const EB_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'EB-tutkinto (2024)',
    oppilaitos: 'Ylioppilastutkintolautakunta',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '31.12.2024',
    suorituskieli: 'suomi',
  },
  additionalChecks: async (paper) => {
    const ebTable = paper.getByRole('table');
    await checkTable(ebTable, [
      ['Oppiaine', 'Suorituskieli', 'Laajuus (vvt)', 'Arvosana'],
      ['Mathematics fi', 'englanti', '4', ''],
      ['Written', '8,67'],
      ['Oral', ''],
      ['Final', '8,67'],
      ['First language, ranska fi', 'englanti', '3', ''],
      ['Written', '8,67'],
      ['Oral', '8,67'],
      ['Final', '8,67'],
      ['Second language, saksa fi', 'englanti', '3', ''],
      ['Written', '8,67'],
      ['Oral', '8,67'],
      ['Final', '8,67'],
    ]);
  },
};

export const IB_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'IB-tutkinto (2024)',
    oppilaitos: 'Ylioppilastutkintolautakunta',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '31.12.2024',
    suorituskieli: 'suomi',
  },
  additionalChecks: async (paper) => {
    const ibTable = paper.getByRole('table');
    await checkTable(ibTable, [
      ['Oppiaine', 'Laajuus (vvt)', 'Predicted grade', 'Arvosana'],
      ['Studies in language and literature fi'],
      ['Language A: literature, suomi fi', '9', '7', '7'],
      ['Language A: language and literature, englanti fi', '6', '7', '7'],
      ['Individuals and societies fi'],
      ['History fi', '3', '7', '7'],
      ['Psychology fi', '3', '7', '7'],
      ['Experimental sciences fi'],
      ['Biology fi', '3', '7', '7'],
      ['Mathematics fi'],
      ['Mathematical studies fi', '3', '7', '7'],
    ]);
  },
};

export const PRE_IB_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Pre-IB (2024)',
    oppilaitos: 'Ylioppilastutkintolautakunta',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '31.12.2024',
    suorituskieli: 'suomi',
  },
};
export const PUUTARHA_ALAN_PERUSTUTKINTO_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Puutarha-alan perustutkinto (2024)',
    oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '31.12.2024',
    suorituskieli: 'suomi',
  },
  additionalChecks: async (paper) => {
    const ytoTable = paper.getByTestId('yhteiset-tutkinnon-osat-table');
    await checkTable(ytoTable, [
      ['Yhteiset tutkinnon osat', 'Laajuus (osp)', 'Arvosana'],
      ['Viestintä- ja vuorovaikutusosaaminen', '11', 'Hyväksytty'],
      ['Matemaattis-luonnontieteellinen osaaminen', '11', 'Hyväksytty'],
      ['Yhteiskunta- ja työelämäosaaminen', '11', 'Hyväksytty'],
      ['', 'Yht. 33 / 35 osp', ''],
    ]);
    await ytoTable
      .getByRole('button', { name: 'Viestintä- ja vuorovaikutusosaaminen' })
      .click();
    const viestintaRegion = paper.getByRole('region', {
      name: 'Viestintä- ja vuorovaikutusosaaminen',
    });
    await expect(viestintaRegion).toBeVisible();
    const viestintaOsaAlueetTable = viestintaRegion.getByRole('table');
    await checkTable(viestintaOsaAlueetTable, [
      ['Osa-alue', 'Laajuus (osp)', 'Arvosana'],
      ['Viestintä ja vuorovaikutus äidinkielellä', '4', '1'],
      ['Viestintä ja vuorovaikutus toisella kotimaisella kielellä', '1', '1'],
      ['Toiminta digitaalisessa ympäristössä', '1', '1'],
    ]);

    await expect(
      ytoTable.getByRole('cell', {
        name: 'Matemaattis-luonnontieteellinen osaaminen',
      }),
    ).toBeVisible();

    await expect(
      ytoTable.getByRole('cell', {
        name: 'Yhteiskunta- ja työelämäosaaminen',
      }),
    ).toBeVisible();

    const ammatillisetTutkinnonOsatTable = paper.getByTestId(
      'ammatilliset-tutkinnon-osat-table',
    );

    await checkTable(ammatillisetTutkinnonOsatTable, [
      ['Ammatilliset tutkinnon osat', 'Laajuus (osp)', 'Arvosana'],
      ['Audiovisuaalisen kulttuurin perusteet', '11', '4'],
      ['Äänimaailman suunnittelu', '11', '4'],
      ['', 'Yht. 22 / 145 osp', ''],
    ]);
    await ammatillisetTutkinnonOsatTable
      .getByRole('button', { name: 'Audiovisuaalisen kulttuurin perusteet' })
      .click();
    const audiovisuaalisenKulttuurinPerusteetRegion = paper.getByRole(
      'region',
      {
        name: 'Audiovisuaalisen kulttuurin perusteet',
      },
    );

    const audioVisuaalisenKulltuurinOsaAlueetTable =
      audiovisuaalisenKulttuurinPerusteetRegion.getByRole('table');
    await checkTable(audioVisuaalisenKulltuurinOsaAlueetTable, [
      ['Osa-alue', 'Laajuus (osp)', 'Arvosana'],
      ['Audiovisuaalisen kulttuurin perusteet 1', '2', '1'],
      ['Audiovisuaalisen kulttuurin perusteet 2', '3', '1'],
    ]);
    await expect(
      ammatillisetTutkinnonOsatTable.getByRole('cell', {
        name: 'Äänimaailman suunnittelu',
      }),
    ).toBeVisible();
    await expect(
      ammatillisetTutkinnonOsatTable.getByRole('button', {
        name: 'Äänimaailman suunnittelu',
      }),
    ).toBeHidden();
  },
};

export const HEVOSTALOUDEN_PERUSTUTKINTO_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Hevostalouden perustutkinto (2024)',
    oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '31.12.2024',
    suorituskieli: 'suomi',
  },
  additionalChecks: async (paper) => {
    await expect(paper.getByLabel('Suoritustapa')).toHaveText('Näyttötutkinto');
    await expect(paper.getByLabel('Painotettu keskiarvo')).toHaveText('4,34');
  },
};

export const MAANMITTAUSALAN_PERUSTUTKINTO_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Maanmittausalan ammattitutkinto (2017)',
    oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '1.6.2017',
    suorituskieli: 'suomi',
  },
};

export const TALOUS_JA_HENKILOSTOALAN_ERIKOISAMMATTITUTKINTO_SUORITUS: SuoritusSpec =
  {
    perustiedot: {
      title: 'Talous- ja henkilöstöalan erikoisammattitutkinto (2017)',
      oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
      tila: 'Suoritus valmis',
      valmistusmipaiva: '1.6.2017',
      suorituskieli: 'suomi',
    },
  };

export const TELMA_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Työhön ja itsenäiseen elämään valmentava koulutus (2017)',
    oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '1.6.2017',
    suorituskieli: 'suomi',
  },
};

export const TUVA_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Tutkintokoulutukseen valmentava koulutus (2017)',
    oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '1.6.2017',
  },
  additionalChecks: async (paper) => {
    await expect(paper.getByLabel('Suoritettu')).toHaveText('38 vk');
  },
};

export const VAPAA_SIVISTYSTYO_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: `Vapaan sivistystyön koulutus (2023 ${NDASH} keskeytynyt)`,
    oppilaitos: 'Hämeen ammatti-instituutti, Lepaa',
    tila: 'Suoritus keskeytynyt',
    valmistusmipaiva: '-',
    suorituskieli: 'suomi',
  },
  additionalChecks: async (paper) => {
    await expect(paper.getByLabel('Suoritettu')).toHaveText('38 op');
  },
};

export const PERUSOPETUKSEN_OPPIMAARA_SUORITUS: SuoritusSpec = {
  perustiedot: {
    title: 'Perusopetuksen oppimäärä (2016)',
    oppilaitos: 'Keltinmäen koulu',
    tila: 'Suoritus valmis',
    valmistusmipaiva: '1.6.2016',
    suorituskieli: 'suomi',
  },
  additionalChecks: async (paper) => {
    await expect(paper.getByLabel('Luokka')).toHaveText('9A');
    await expect(paper.getByLabel('Yksilöllistetty')).toHaveText(
      'Perusopetuksen osittain yksilöllistetty oppimäärä',
    );

    const oppiaineetTable = paper.getByRole('table');
    await checkTable(oppiaineetTable, [
      ['Oppiaine', 'Arvosana', 'Valinnainen arvosana'],
      ['Äidinkieli ja kirjallisuus, suomen kieli ja kirjallisuus', '9', ''],
      ['A1-kieli, englanti', '9', ''],
      ['B1-kieli, ruotsi', '9', ''],
      ['B2-kieli, saksa', '', '9'],
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

export const PERUSOPETUKSEN_OPPIMAARA_78LUOKKA_SUORITUS: SuoritusSpec = {
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
  },
};

export const PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA_SUORITUS: SuoritusSpec = {
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

export async function expectSuoritukset(
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
