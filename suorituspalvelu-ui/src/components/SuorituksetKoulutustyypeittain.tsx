'use client';
import { Box, Stack } from '@mui/material';
import { OppijaResponse } from '@/api';
import { useTranslate } from '@tolgee/react';
import { ophColors, OphTypography } from '@opetushallitus/oph-design-system';
import { LabeledInfoItem } from './LabeledInfoItem';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { KorkeakouluSuoritusPaper } from './KorkeakouluSuoritusPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { LukioSuoritusPaper } from './LukioSuoritusPaper';

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
          <KorkeakouluSuoritusPaper
            key={kkTutkinto.tutkinto}
            korkeakouluSuoritus={kkTutkinto}
          />
        ))}
      </LabeledSuoritusSection>
      <LabeledSuoritusSection label={t('oppija.lukiokoulutus')}>
        {tiedot?.yoTutkinto && (
          <LukioSuoritusPaper
            nimi={t('oppija.ylioppilastutkinto')}
            lukioSuoritus={tiedot.yoTutkinto}
          />
        )}
        {tiedot?.lukionOppimaara && (
          <LukioSuoritusPaper
            nimi={t('oppija.lukion-oppimaara')}
            lukioSuoritus={tiedot.lukionOppimaara}
          />
        )}
        {tiedot?.lukionOppiaineenOppimaarat.map((oppimaara) => (
          <LukioSuoritusPaper
            key={oppimaara.oppilaitos.oid}
            nimi={t('oppija.lukion-oppiaineen-oppimaara')}
            lukioSuoritus={oppimaara}
          />
        ))}
        {tiedot.diaTutkinto && (
          <LukioSuoritusPaper
            nimi={t('oppija.dia-tutkinto')}
            lukioSuoritus={tiedot.diaTutkinto}
          />
        )}
        {tiedot.diaVastaavuusTodistus && (
          <LukioSuoritusPaper
            nimi={t('oppija.dia-vastaavuustodistus')}
            lukioSuoritus={tiedot.diaVastaavuusTodistus}
          />
        )}
        {tiedot.ebTutkinto && (
          <LukioSuoritusPaper
            nimi={t('oppija.eb-tutkinto')}
            lukioSuoritus={tiedot.ebTutkinto}
          />
        )}
        {tiedot.ibTutkinto && (
          <LukioSuoritusPaper
            nimi={t('oppija.ib-tutkinto')}
            lukioSuoritus={tiedot.ibTutkinto}
          />
        )}
        {tiedot.preIB && (
          <LukioSuoritusPaper
            nimi={t('oppija.pre-ib')}
            lukioSuoritus={tiedot.preIB}
          />
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
          </SuoritusInfoPaper>
        ))}
      </LabeledSuoritusSection>
    </Stack>
  );
};
