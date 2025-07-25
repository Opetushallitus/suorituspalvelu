'use client';
import { Box, Stack } from '@mui/material';
import { OppijaResponse } from '@/api';
import { useTranslate } from '@tolgee/react';
import { OphTypography } from '@opetushallitus/oph-design-system';
import { KorkeakouluSuoritusPaper } from './KorkeakouluSuoritusPaper';
import { LukioSuoritusPaper } from './LukioSuoritusPaper';
import { AmmatillinenSuoritusPaper } from './AmmatillinenSuoritusPaper';
import { VapaaSivistystyoSuoritusPaper } from './VapaaSivistystyoSuoritusPaper';
import { TuvaSuoritusPaper } from './TuvaSuoritusPaper';
import { PerusopetusSuoritusPaper } from './PerusopetusSuoritusPaper';

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
          <AmmatillinenSuoritusPaper
            key={tutkinto.nimi}
            ammatillinenSuoritus={tutkinto}
          />
        ))}
        {tiedot.ammattitutkinnot.map((tutkinto) => (
          <AmmatillinenSuoritusPaper
            key={tutkinto.nimi}
            ammatillinenSuoritus={tutkinto}
          />
        ))}
        {tiedot.erikoisammattitutkinnot.map((tutkinto) => (
          <AmmatillinenSuoritusPaper
            key={tutkinto.nimi}
            ammatillinenSuoritus={tutkinto}
          />
        ))}
      </LabeledSuoritusSection>
      <LabeledSuoritusSection label={t('oppija.tuva')}>
        {tiedot.tuvat.map((tutkinto) => (
          <TuvaSuoritusPaper
            key={tutkinto.oppilaitos.oid}
            tuvaSuoritus={tutkinto}
          />
        ))}
      </LabeledSuoritusSection>
      <LabeledSuoritusSection label={t('oppija.vapaa-sivistystyo')}>
        {tiedot.vapaanSivistystyonKoulutukset.map((koulutus) => (
          <VapaaSivistystyoSuoritusPaper
            key={koulutus.oppilaitos.oid}
            vapaaSivistystyoSuoritus={koulutus}
          />
        ))}
      </LabeledSuoritusSection>
      <LabeledSuoritusSection label={t('oppija.perusopetus')}>
        {tiedot.perusopetuksenOppimaarat.map((oppimaara) => (
          <PerusopetusSuoritusPaper
            key={oppimaara.oppilaitos.oid}
            perusopetusSuoritus={oppimaara}
            nimi={t('oppija.perusopetuksen-oppimaara')}
          />
        ))}
        {tiedot.perusopetuksenOppimaara78Luokkalaiset && (
          <PerusopetusSuoritusPaper
            key={tiedot.perusopetuksenOppimaara78Luokkalaiset.oppilaitos.oid}
            perusopetusSuoritus={tiedot.perusopetuksenOppimaara78Luokkalaiset}
            nimi={t('oppija.perusopetuksen-oppimaara')}
          />
        )}
        {tiedot.nuortenPerusopetuksenOppiaineenOppimaarat.map((oppiaine) => (
          <PerusopetusSuoritusPaper
            key={oppiaine.oppilaitos.oid}
            perusopetusSuoritus={oppiaine}
            nimi={t('oppija.nuorten-perusopetuksen-oppiaineen-oppimaara')}
          />
        ))}
        {tiedot.perusopetuksenOppiaineenOppimaarat.map((oppiaine) => (
          <PerusopetusSuoritusPaper
            key={oppiaine.oppilaitos.oid}
            perusopetusSuoritus={oppiaine}
            nimi={t('oppija.perusopetuksen-oppiaineen-oppimaara')}
          />
        ))}
        {tiedot.aikuistenPerusopetuksenOppimaarat.map((oppimaara) => (
          <PerusopetusSuoritusPaper
            key={oppimaara.oppilaitos.oid}
            perusopetusSuoritus={oppimaara}
            nimi={t('oppija.aikuisten-perusopetuksen-oppimaara')}
          />
        ))}
      </LabeledSuoritusSection>
    </Stack>
  );
};
