'use client';
import { Box, Stack } from '@mui/material';
import { OppijaResponse } from '@/api';
import { useTranslate } from '@tolgee/react';
import { ophColors, OphTypography } from '@opetushallitus/oph-design-system';
import { LabeledInfoItem } from './LabeledInfoItem';
import {
  LukionOppiaine
} from '@/types/ui-types';
import { styled } from '@/lib/theme';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { KorkeakouluSuoritusPaper } from './KorkeakouluSuoritusPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';

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
