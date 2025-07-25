'use client';
import { Box, Stack } from '@mui/material';
import { use } from 'react';
import { OppijaResponse } from '@/api';
import { useTranslate } from '@tolgee/react';
import {
  ophColors,
  OphLink,
  OphTypography,
} from '@opetushallitus/oph-design-system';
import { LabeledInfoItem } from './LabeledInfoItem';
import { PaperWithTopColor } from './PaperWithTopColor';
import { getOppilaitosLinkUrl } from '@/lib/getOppilaitosLink';
import { configPromise } from '@/configuration';
import { SuorituksenPerustiedot, SuorituksenTila } from '@/types/ui-types';
import { CheckCircle, DoNotDisturb, HourglassTop } from '@mui/icons-material';
import { formatDate } from 'date-fns';

const OppijaInfoPaper = ({
  suorituksenNimi,
  valmistumispaiva,
  headingLevel = 'h3',
  topColor,
  children,
}: {
  valmistumispaiva?: Date;
  suorituksenNimi: string;
  headingLevel?: 'h2' | 'h3' | 'h4' | 'h5';
  topColor: string;
  children: React.ReactNode;
}) => {
  return (
    <PaperWithTopColor topColor={topColor}>
      <OphTypography
        variant="h5"
        component={headingLevel}
        sx={{ marginBottom: 2 }}
      >
        {suorituksenNimi}{' '}
        {valmistumispaiva && (
          <OphTypography variant="body1" component="span">
            ({formatDate(valmistumispaiva, 'y')})
          </OphTypography>
        )}
      </OphTypography>
      <Stack spacing={2}>{children}</Stack>
    </PaperWithTopColor>
  );
};

const SuorituksenTilaIcon = ({ tila }: { tila: SuorituksenTila }) => {
  switch (tila) {
    case 'VALMIS':
      return <CheckCircle sx={{ color: ophColors.green2 }} />;
    case 'KESKEN':
      return <HourglassTop sx={{ color: ophColors.yellow1 }} />;
    case 'KESKEYTYNYT':
      return <DoNotDisturb sx={{ color: ophColors.orange3 }} />;
    default:
      return null;
  }
};

const SuorituksenTilaIndicator = ({ tila }: { tila: SuorituksenTila }) => {
  const { t } = useTranslate();

  return (
    <Stack direction="row" sx={{ alignItems: 'center', gap: 1 }}>
      <SuorituksenTilaIcon tila={tila} />
      <OphTypography>{t(`suorituksen-tila.${tila}`)}</OphTypography>
    </Stack>
  );
};

const ValmistumispaivaIndicator = ({
  valmistumispaiva,
}: {
  valmistumispaiva?: Date;
}) => {
  return (
    <OphTypography>
      {valmistumispaiva ? formatDate(valmistumispaiva, 'd.M.y') : '-'}
    </OphTypography>
  );
};

const SuorituksenPerustiedotIndicator = ({
  perustiedot,
}: {
  perustiedot: SuorituksenPerustiedot;
}) => {
  const { t } = useTranslate();
  const config = use(configPromise);

  return (
    <Stack gap={1} direction="row">
      <LabeledInfoItem
        label={t('oppija.oppilaitos')}
        value={
          <OphLink
            href={getOppilaitosLinkUrl(config, perustiedot.oppilaitos.oid)}
          >
            {perustiedot.oppilaitos.nimi}
          </OphLink>
        }
      />
      <LabeledInfoItem
        label={t('oppija.tila')}
        value={<SuorituksenTilaIndicator tila={perustiedot.tila} />}
      />
      <LabeledInfoItem
        label={t('oppija.valmistumispaiva')}
        value={
          <ValmistumispaivaIndicator
            valmistumispaiva={perustiedot.valmistumispaiva}
          />
        }
      />
      {perustiedot.suorituskieli && (
        <LabeledInfoItem
          label={t('oppija.suorituskieli')}
          value={perustiedot.suorituskieli}
        />
      )}
    </Stack>
  );
};

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
          <OppijaInfoPaper
            key={kkTutkinto.tutkinto}
            suorituksenNimi={kkTutkinto.tutkinto}
            valmistumispaiva={kkTutkinto.valmistumispaiva}
            topColor={ophColors.red1}
          >
            <SuorituksenPerustiedotIndicator perustiedot={kkTutkinto} />
            <Stack direction="row" sx={{ alignItems: 'center', gap: 1 }}>
              <LabeledInfoItem
                label={t('oppija.hakukohde')}
                value={kkTutkinto.hakukohde.nimi}
              />
            </Stack>
          </OppijaInfoPaper>
        ))}
      </LabeledSuoritusSection>
      <LabeledSuoritusSection label={t('oppija.lukiokoulutus')}>
        {tiedot?.yoTutkinto && (
          <OppijaInfoPaper
            suorituksenNimi={t('oppija.ylioppilastutkinto')}
            valmistumispaiva={tiedot.yoTutkinto.valmistumispaiva}
            topColor={ophColors.blue2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={tiedot.yoTutkinto} />
          </OppijaInfoPaper>
        )}
        {tiedot?.lukionOppimaara && (
          <OppijaInfoPaper
            suorituksenNimi={t('oppija.lukion-oppimaara')}
            valmistumispaiva={tiedot.lukionOppimaara.valmistumispaiva}
            topColor={ophColors.blue2}
          >
            <SuorituksenPerustiedotIndicator
              perustiedot={tiedot.lukionOppimaara}
            />
          </OppijaInfoPaper>
        )}
        {tiedot?.lukionOppiaineenOppimaarat.map((oppimaara) => (
          <OppijaInfoPaper
            key={oppimaara.oppilaitos.oid}
            suorituksenNimi={t('oppija.lukion-oppiaineen-oppimaara')}
            valmistumispaiva={oppimaara.valmistumispaiva}
            topColor={ophColors.blue2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={oppimaara} />
          </OppijaInfoPaper>
        ))}
        {tiedot.diaTutkinto && (
          <OppijaInfoPaper
            suorituksenNimi={t('oppija.dia-tutkinto')}
            valmistumispaiva={tiedot.diaTutkinto.valmistumispaiva}
            topColor={ophColors.blue2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={tiedot.diaTutkinto} />
          </OppijaInfoPaper>
        )}
        {tiedot.diaVastaavuusTodistus && (
          <OppijaInfoPaper
            suorituksenNimi={t('oppija.dia-vastaavuustodistus')}
            valmistumispaiva={tiedot.diaVastaavuusTodistus.valmistumispaiva}
            topColor={ophColors.blue2}
          >
            <SuorituksenPerustiedotIndicator
              perustiedot={tiedot.diaVastaavuusTodistus}
            />
          </OppijaInfoPaper>
        )}
        {tiedot.ebTutkinto && (
          <OppijaInfoPaper
            suorituksenNimi={t('oppija.eb-tutkinto')}
            valmistumispaiva={tiedot.ebTutkinto.valmistumispaiva}
            topColor={ophColors.blue2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={tiedot.ebTutkinto} />
          </OppijaInfoPaper>
        )}
        {tiedot.ibTutkinto && (
          <OppijaInfoPaper
            suorituksenNimi={t('oppija.ib-tutkinto')}
            valmistumispaiva={tiedot.ibTutkinto.valmistumispaiva}
            topColor={ophColors.blue2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={tiedot.ibTutkinto} />
          </OppijaInfoPaper>
        )}
        {tiedot.preIB && (
          <OppijaInfoPaper
            suorituksenNimi={t('oppija.pre-ib')}
            valmistumispaiva={tiedot.preIB.valmistumispaiva}
            topColor={ophColors.blue2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={tiedot.preIB} />
          </OppijaInfoPaper>
        )}
      </LabeledSuoritusSection>
      <LabeledSuoritusSection label={t('oppija.ammatillinen-koulutus')}>
        {tiedot.ammatillisetTutkinnot.map((tutkinto) => (
          <OppijaInfoPaper
            key={tutkinto.nimi}
            suorituksenNimi={tutkinto.nimi}
            valmistumispaiva={tutkinto.valmistumispaiva}
            topColor={ophColors.green2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={tutkinto} />
          </OppijaInfoPaper>
        ))}
      </LabeledSuoritusSection>
      <LabeledSuoritusSection label={t('oppija.tuva')}>
        {tiedot.tuvat.map((tutkinto) => (
          <OppijaInfoPaper
            key={tutkinto.oppilaitos.oid}
            suorituksenNimi={tutkinto.oppilaitos.nimi}
            valmistumispaiva={tutkinto.valmistumispaiva}
            topColor={ophColors.yellow2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={tutkinto} />
          </OppijaInfoPaper>
        ))}
      </LabeledSuoritusSection>
      <LabeledSuoritusSection label={t('oppija.vapaa-sivistystyo')}>
        {tiedot.vapaanSivistystyonKoulutukset.map((koulutus) => (
          <OppijaInfoPaper
            key={koulutus.oppilaitos.oid}
            suorituksenNimi={koulutus.oppilaitos.nimi}
            valmistumispaiva={koulutus.valmistumispaiva}
            topColor={ophColors.cyan1}
          >
            <SuorituksenPerustiedotIndicator perustiedot={koulutus} />
          </OppijaInfoPaper>
        ))}
      </LabeledSuoritusSection>
      <LabeledSuoritusSection label={t('oppija.perusopetus')}>
        {tiedot.perusopetuksenOppimaarat.map((oppimaara) => (
          <OppijaInfoPaper
            key={oppimaara.oppilaitos.oid}
            suorituksenNimi={t('oppija.perusopetuksen-oppimaara')}
            valmistumispaiva={oppimaara.valmistumispaiva}
            topColor={ophColors.cyan2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={oppimaara} />
          </OppijaInfoPaper>
        ))}
        {tiedot.nuortenPerusopetuksenOppiaineenOppimaarat.map((oppiaine) => (
          <OppijaInfoPaper
            key={oppiaine.oppilaitos.oid}
            suorituksenNimi={t(
              'oppija.nuorten-perusopetuksen-oppiaineen-oppimaara',
            )}
            valmistumispaiva={oppiaine.valmistumispaiva}
            topColor={ophColors.cyan2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={oppiaine} />
          </OppijaInfoPaper>
        ))}
        {tiedot.perusopetuksenOppiaineenOppimaarat.map((oppiaine) => (
          <OppijaInfoPaper
            key={oppiaine.oppilaitos.oid}
            suorituksenNimi={t('oppija.perusopetuksen-oppiaineen-oppimaara')}
            valmistumispaiva={oppiaine.valmistumispaiva}
            topColor={ophColors.cyan2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={oppiaine} />
          </OppijaInfoPaper>
        ))}
        {tiedot.aikuistenPerusopetuksenOppimaarat.map((oppimaara) => (
          <OppijaInfoPaper
            key={oppimaara.oppilaitos.oid}
            suorituksenNimi={t('oppija.aikuisten-perusopetuksen-oppimaara')}
            valmistumispaiva={oppimaara.valmistumispaiva}
            topColor={ophColors.cyan2}
          >
            <SuorituksenPerustiedotIndicator perustiedot={oppimaara} />
          </OppijaInfoPaper>
        ))}
      </LabeledSuoritusSection>
    </Stack>
  );
};
