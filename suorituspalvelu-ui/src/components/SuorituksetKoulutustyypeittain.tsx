'use client';
import { Box, Stack } from '@mui/material';
import { useMemo } from 'react';
import { OppijaResponse } from '@/api';
import { useTranslate } from '@tolgee/react';
import { ophColors, OphTypography } from '@opetushallitus/oph-design-system';
import { LabeledInfoItem } from './LabeledInfoItem';
import {
  LukionOppiaine,
  PerusopetuksenOppiaine,
  YOKoe,
} from '@/types/ui-types';
import { formatDate } from 'date-fns';
import { ListTable, ListTableColumn } from './ListTable';
import { styled } from '@/lib/theme';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { KorkeakouluSuoritusPaper } from './KorkeakouluSuoritusPaper';

const UnorderedList = styled('ul')(({ theme }) => ({
  margin: 0,
  paddingLeft: theme.spacing(2),
}));

const LabeledSuoritusSection = ({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) => {
  return (
    <Box sx={{ marginBottom: 4 }}>
      <OphTypography variant="h4" component="h3" sx={{ marginBottom: 2 }}>
        {label}
      </OphTypography>
      <Stack gap={4}>{children}</Stack>
    </Box>
  );
};

const YoKokeetTable = ({ yoKokeet }: { yoKokeet: Array<YOKoe> }) => {
  const { t } = useTranslate();

  const cols: Array<ListTableColumn<YOKoe>> = useMemo(
    () => [
      {
        key: 'aine',
        title: t('oppija.yo-kokeen-aine'),
        render: (row) => row.aine,
      },
      {
        key: 'taso',
        title: t('oppija.yo-kokeen-taso'),
        render: (row) => row.taso,
      },
      {
        key: 'arvosana',
        title: t('oppija.yo-kokeen-arvosana'),
        render: (row) => row.arvosana,
      },
      {
        key: 'yhteispistemaara',
        title: t('oppija.yo-kokeen-yhteispistemaara'),
        render: (row) => row.yhteispistemaara.toString(),
      },
      {
        key: 'tutkintokerta',
        title: t('oppija.yo-kokeen-tutkintokerta'),
        render: (row) => formatDate(row.tutkintokerta, 'd.M.y'),
      },
    ],
    [t],
  );

  return <ListTable columns={cols} rows={yoKokeet} rowKeyProp="aine" />;
};

const PerusopetusOppiaineetTable = ({
  oppiaineet,
}: {
  oppiaineet: Array<PerusopetuksenOppiaine>;
}) => {
  const { t } = useTranslate();

  const hasArvosana = oppiaineet.some((oppiaine) => oppiaine.arvosana);
  const hasValinnainen = oppiaineet.some((oppiaine) => oppiaine.valinnainen);

  const columns = useMemo(() => {
    const cols: Array<ListTableColumn<PerusopetuksenOppiaine>> = [
      {
        key: 'nimi',
        title: t('oppija.oppiaine'),
        render: (row) => row.nimi,
      } as ListTableColumn<PerusopetuksenOppiaine>,
    ];

    if (hasArvosana) {
      cols.push({
        key: 'arvosana',
        title: t('oppija.arvosana'),
        render: (row) => row.arvosana,
      } as ListTableColumn<PerusopetuksenOppiaine>);
    }

    if (hasValinnainen) {
      cols.push({
        key: 'valinnainen',
        title: t('oppija.valinnainen'),
        render: (row) => row.valinnainen,
      } as ListTableColumn<PerusopetuksenOppiaine>);
    }

    return cols;
  }, [hasArvosana, hasValinnainen, t]);

  return <ListTable columns={columns} rows={oppiaineet} rowKeyProp="nimi" />;
};

const LukionOppiaineetList = ({
  oppiaineet,
}: {
  oppiaineet: Array<LukionOppiaine>;
}) => {
  const { t } = useTranslate();
  return (
    <LabeledInfoItem
      label={t('oppija.oppiaineet')}
      value={
        <UnorderedList>
          {oppiaineet.map((oppiaine) => (
            <li key={oppiaine.nimi}>{oppiaine.nimi}</li>
          ))}
        </UnorderedList>
      }
    />
  );
};

export const SuorituksetKoulutustyypeittain = ({
  tiedot,
}: {
  tiedot: OppijaResponse;
}) => {
  const { t } = useTranslate();

  return (
    <Stack spacing={4}>
      <LabeledSuoritusSection label={t('oppija.korkeakoulutus')}>
        {tiedot?.kkTutkinnot?.map((kkTutkinto) => (
          <KorkeakouluSuoritusPaper
            key={kkTutkinto.tutkinto}
            korkeakouluSuoritus={kkTutkinto}
          />
        ))}
      </LabeledSuoritusSection>
      <LabeledSuoritusSection label={t('oppija.lukiokoulutus')}>
        {tiedot?.yoTutkinto && (
          <SuoritusInfoPaper
            suorituksenNimi={t('oppija.ylioppilastutkinto')}
            valmistumispaiva={tiedot.yoTutkinto.valmistumispaiva}
            topColor={ophColors.blue2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={tiedot.yoTutkinto} />
            <YoKokeetTable yoKokeet={tiedot.yoTutkinto.yoKokeet} />
          </SuoritusInfoPaper>
        )}
        {tiedot?.lukionOppimaara && (
          <SuoritusInfoPaper
            suorituksenNimi={t('oppija.lukion-oppimaara')}
            valmistumispaiva={tiedot.lukionOppimaara.valmistumispaiva}
            topColor={ophColors.blue2}
          >
            <SuorituksenPerustiedotIndicator
              perustiedot={tiedot.lukionOppimaara}
            />
            <LukionOppiaineetList
              oppiaineet={tiedot.lukionOppimaara.oppiaineet}
            />
          </SuoritusInfoPaper>
        )}
        {tiedot?.lukionOppiaineenOppimaarat.map((oppimaara) => (
          <SuoritusInfoPaper
            key={oppimaara.oppilaitos.oid}
            suorituksenNimi={t('oppija.lukion-oppiaineen-oppimaara')}
            valmistumispaiva={oppimaara.valmistumispaiva}
            topColor={ophColors.blue2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={oppimaara} />
            <LukionOppiaineetList oppiaineet={oppimaara.oppiaineet} />
          </SuoritusInfoPaper>
        ))}
        {tiedot.diaTutkinto && (
          <SuoritusInfoPaper
            suorituksenNimi={t('oppija.dia-tutkinto')}
            valmistumispaiva={tiedot.diaTutkinto.valmistumispaiva}
            topColor={ophColors.blue2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={tiedot.diaTutkinto} />
          </SuoritusInfoPaper>
        )}
        {tiedot.diaVastaavuusTodistus && (
          <SuoritusInfoPaper
            suorituksenNimi={t('oppija.dia-vastaavuustodistus')}
            valmistumispaiva={tiedot.diaVastaavuusTodistus.valmistumispaiva}
            topColor={ophColors.blue2}
          >
            <SuorituksenPerustiedotIndicator
              perustiedot={tiedot.diaVastaavuusTodistus}
            />
          </SuoritusInfoPaper>
        )}
        {tiedot.ebTutkinto && (
          <SuoritusInfoPaper
            suorituksenNimi={t('oppija.eb-tutkinto')}
            valmistumispaiva={tiedot.ebTutkinto.valmistumispaiva}
            topColor={ophColors.blue2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={tiedot.ebTutkinto} />
          </SuoritusInfoPaper>
        )}
        {tiedot.ibTutkinto && (
          <SuoritusInfoPaper
            suorituksenNimi={t('oppija.ib-tutkinto')}
            valmistumispaiva={tiedot.ibTutkinto.valmistumispaiva}
            topColor={ophColors.blue2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={tiedot.ibTutkinto} />
          </SuoritusInfoPaper>
        )}
        {tiedot.preIB && (
          <SuoritusInfoPaper
            suorituksenNimi={t('oppija.pre-ib')}
            valmistumispaiva={tiedot.preIB.valmistumispaiva}
            topColor={ophColors.blue2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={tiedot.preIB} />
          </SuoritusInfoPaper>
        )}
      </LabeledSuoritusSection>
      <LabeledSuoritusSection label={t('oppija.ammatillinen-koulutus')}>
        {tiedot.ammatillisetTutkinnot.map((tutkinto) => (
          <SuoritusInfoPaper
            key={tutkinto.nimi}
            suorituksenNimi={tutkinto.nimi}
            valmistumispaiva={tutkinto.valmistumispaiva}
            topColor={ophColors.green2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={tutkinto} />
          </SuoritusInfoPaper>
        ))}
        {tiedot.ammattitutkinnot.map((tutkinto) => (
          <SuoritusInfoPaper
            key={tutkinto.nimi}
            suorituksenNimi={tutkinto.nimi}
            valmistumispaiva={tutkinto.valmistumispaiva}
            topColor={ophColors.green2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={tutkinto} />
          </SuoritusInfoPaper>
        ))}
        {tiedot.erikoisammattitutkinnot.map((tutkinto) => (
          <SuoritusInfoPaper
            key={tutkinto.nimi}
            suorituksenNimi={tutkinto.nimi}
            valmistumispaiva={tutkinto.valmistumispaiva}
            topColor={ophColors.green2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={tutkinto} />
          </SuoritusInfoPaper>
        ))}
      </LabeledSuoritusSection>
      <LabeledSuoritusSection label={t('oppija.tuva')}>
        {tiedot.tuvat.map((tutkinto) => (
          <SuoritusInfoPaper
            key={tutkinto.oppilaitos.oid}
            suorituksenNimi={tutkinto.oppilaitos.nimi}
            valmistumispaiva={tutkinto.valmistumispaiva}
            topColor={ophColors.yellow2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={tutkinto} />
          </SuoritusInfoPaper>
        ))}
      </LabeledSuoritusSection>
      <LabeledSuoritusSection label={t('oppija.vapaa-sivistystyo')}>
        {tiedot.vapaanSivistystyonKoulutukset.map((koulutus) => (
          <SuoritusInfoPaper
            key={koulutus.oppilaitos.oid}
            suorituksenNimi={koulutus.oppilaitos.nimi}
            valmistumispaiva={koulutus.valmistumispaiva}
            topColor={ophColors.cyan1}
          >
            <SuorituksenPerustiedotIndicator perustiedot={koulutus} />
          </SuoritusInfoPaper>
        ))}
      </LabeledSuoritusSection>
      <LabeledSuoritusSection label={t('oppija.perusopetus')}>
        {tiedot.perusopetuksenOppimaarat.map((oppimaara) => (
          <SuoritusInfoPaper
            key={oppimaara.oppilaitos.oid}
            suorituksenNimi={t('oppija.perusopetuksen-oppimaara')}
            valmistumispaiva={oppimaara.valmistumispaiva}
            topColor={ophColors.cyan2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={oppimaara} />
            <Stack direction="row" sx={{ alignItems: 'center', gap: 1 }}>
              <LabeledInfoItem
                label={t('oppija.luokka')}
                value={oppimaara.luokka}
              />
              <LabeledInfoItem
                label={t('oppija.yksilollistetty')}
                value={oppimaara.yksilollistetty ? t('kylla') : t('ei')}
              />
            </Stack>

            <PerusopetusOppiaineetTable oppiaineet={oppimaara.oppiaineet} />
          </SuoritusInfoPaper>
        ))}
        {tiedot.nuortenPerusopetuksenOppiaineenOppimaarat.map((oppiaine) => (
          <SuoritusInfoPaper
            key={oppiaine.oppilaitos.oid}
            suorituksenNimi={t(
              'oppija.nuorten-perusopetuksen-oppiaineen-oppimaara',
            )}
            valmistumispaiva={oppiaine.valmistumispaiva}
            topColor={ophColors.cyan2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={oppiaine} />
            <PerusopetusOppiaineetTable oppiaineet={oppiaine.oppiaineet} />
          </SuoritusInfoPaper>
        ))}
        {tiedot.perusopetuksenOppiaineenOppimaarat.map((oppiaine) => (
          <SuoritusInfoPaper
            key={oppiaine.oppilaitos.oid}
            suorituksenNimi={t('oppija.perusopetuksen-oppiaineen-oppimaara')}
            valmistumispaiva={oppiaine.valmistumispaiva}
            topColor={ophColors.cyan2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={oppiaine} />
            <PerusopetusOppiaineetTable oppiaineet={oppiaine.oppiaineet} />
          </SuoritusInfoPaper>
        ))}
        {tiedot.aikuistenPerusopetuksenOppimaarat.map((oppimaara) => (
          <SuoritusInfoPaper
            key={oppimaara.oppilaitos.oid}
            suorituksenNimi={t('oppija.aikuisten-perusopetuksen-oppimaara')}
            valmistumispaiva={oppimaara.valmistumispaiva}
            topColor={ophColors.cyan2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={oppimaara} />
            <PerusopetusOppiaineetTable oppiaineet={oppimaara.oppiaineet} />
          </SuoritusInfoPaper>
        ))}
      </LabeledSuoritusSection>
    </Stack>
  );
};
