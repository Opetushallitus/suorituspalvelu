'use client';
import { Box, Stack } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { OphTypography } from '@opetushallitus/oph-design-system';
import { KorkeakouluSuoritusPaper } from './KorkeakouluSuoritusPaper';
import { LukioSuoritusPaper } from './LukioSuoritusPaper';
import { AmmatillinenSuoritusPaper } from './AmmatillinenSuoritusPaper';
import { VapaaSivistystyoSuoritusPaper } from './VapaaSivistystyoSuoritusPaper';
import { TuvaSuoritusPaper } from './TuvaSuoritusPaper';
import { PerusopetusSuoritusPaper } from './PerusopetusSuoritusPaper';
import { OppijanTiedot } from '@/types/ui-types';
import { useSuorituksetFlattened } from '@/hooks/useSuorituksetFlattened';

function LabeledSuoritusSection({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <Box sx={{ marginBottom: 4 }}>
      <OphTypography variant="h4" component="h3" sx={{ marginBottom: 2 }}>
        {label}
      </OphTypography>
      <Stack gap={4}>{children}</Stack>
    </Box>
  );
}

export function SuorituksetKoulutustyypeittain({
  oppijanTiedot,
}: {
  oppijanTiedot: OppijanTiedot;
}) {
  const { t } = useTranslate();

  const suoritukset = useSuorituksetFlattened(oppijanTiedot);

  return (
    <Stack spacing={4}>
      <LabeledSuoritusSection label={t('oppija.korkeakoulutus')}>
        {suoritukset
          .filter((s) => s.koulutustyyppi === 'korkeakoulutus')
          ?.map((suoritus) => (
            <KorkeakouluSuoritusPaper key={suoritus.key} suoritus={suoritus} />
          ))}
      </LabeledSuoritusSection>
      <LabeledSuoritusSection label={t('oppija.lukiokoulutus')}>
        {suoritukset
          .filter((s) => s.koulutustyyppi === 'lukio')
          ?.map((suoritus) => (
            <LukioSuoritusPaper key={suoritus.key} suoritus={suoritus} />
          ))}
      </LabeledSuoritusSection>
      <LabeledSuoritusSection label={t('oppija.ammatillinen-koulutus')}>
        {suoritukset
          .filter((s) => s.koulutustyyppi === 'ammatillinen')
          ?.map((suoritus) => (
            <AmmatillinenSuoritusPaper key={suoritus.key} suoritus={suoritus} />
          ))}
      </LabeledSuoritusSection>
      <LabeledSuoritusSection label={t('oppija.tuva')}>
        {suoritukset
          .filter((s) => s.koulutustyyppi === 'tuva')
          ?.map((suoritus) => (
            <TuvaSuoritusPaper key={suoritus.key} suoritus={suoritus} />
          ))}
      </LabeledSuoritusSection>
      <LabeledSuoritusSection label={t('oppija.vapaa-sivistystyo')}>
        {suoritukset
          .filter((s) => s.koulutustyyppi === 'vapaa-sivistystyo')
          ?.map((suoritus) => (
            <VapaaSivistystyoSuoritusPaper
              key={suoritus.key}
              suoritus={suoritus}
            />
          ))}
      </LabeledSuoritusSection>
      <LabeledSuoritusSection label={t('oppija.perusopetus')}>
        {suoritukset
          .filter((s) => s.koulutustyyppi === 'perusopetus')
          ?.map((suoritus) => (
            <PerusopetusSuoritusPaper key={suoritus.key} suoritus={suoritus} />
          ))}
      </LabeledSuoritusSection>
    </Stack>
  );
}
