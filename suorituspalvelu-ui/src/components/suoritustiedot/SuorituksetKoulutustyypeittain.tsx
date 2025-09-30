import { Box, Stack } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { OphTypography } from '@opetushallitus/oph-design-system';
import { KorkeakouluSuoritusPaper } from './KorkeakouluSuoritusPaper';
import { LukioSuoritusPaper } from './LukioSuoritusPaper';
import { AmmatillinenSuoritusPaper } from './AmmatillinenSuoritusPaper';
import { VapaaSivistystyoSuoritusPaper } from './VapaaSivistystyoSuoritusPaper';
import { TuvaSuoritusPaper } from './TuvaSuoritusPaper';
import { PerusopetusSuoritusPaper } from './PerusopetusSuoritusPaper';
import type { OppijanTiedot } from '@/types/ui-types';
import { useSuorituksetFlattened } from '@/hooks/useSuorituksetFlattened';
import React from 'react';

function SuoritusSection({
  heading,
  children,
}: {
  heading: string;
  children: React.ReactNode;
}) {
  return (
    // eslint-disable-next-line @eslint-react/no-children-count
    React.Children.count(children) > 0 && (
      <Box sx={{ marginBottom: 4 }}>
        <OphTypography variant="h4" component="h3" sx={{ marginBottom: 2 }}>
          {heading}
        </OphTypography>
        <Stack spacing={4}>{children}</Stack>
      </Box>
    )
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
      <SuoritusSection heading={t('oppija.korkeakoulutus')}>
        {suoritukset
          .filter((s) => s.koulutustyyppi === 'korkeakoulutus')
          ?.map((suoritus) => (
            <KorkeakouluSuoritusPaper
              key={suoritus.tunniste}
              suoritus={suoritus}
            />
          ))}
      </SuoritusSection>
      <SuoritusSection heading={t('oppija.lukiokoulutus')}>
        {suoritukset
          .filter(
            (s) =>
              s.koulutustyyppi === 'lukio' ||
              s.koulutustyyppi === 'eb' ||
              s.koulutustyyppi === 'ib',
          )
          ?.map((suoritus) => (
            <LukioSuoritusPaper key={suoritus.tunniste} suoritus={suoritus} />
          ))}
      </SuoritusSection>
      <SuoritusSection heading={t('oppija.ammatillinen-koulutus')}>
        {suoritukset
          .filter((s) => s.koulutustyyppi === 'ammatillinen')
          ?.map((suoritus) => (
            <AmmatillinenSuoritusPaper
              key={suoritus.tunniste}
              suoritus={suoritus}
            />
          ))}
      </SuoritusSection>
      <SuoritusSection heading={t('oppija.tuva')}>
        {suoritukset
          .filter((s) => s.koulutustyyppi === 'tuva')
          ?.map((suoritus) => (
            <TuvaSuoritusPaper key={suoritus.tunniste} suoritus={suoritus} />
          ))}
      </SuoritusSection>
      <SuoritusSection heading={t('oppija.vapaa-sivistystyo')}>
        {suoritukset
          .filter((s) => s.koulutustyyppi === 'vapaa-sivistystyo')
          ?.map((suoritus) => (
            <VapaaSivistystyoSuoritusPaper
              key={suoritus.tunniste}
              suoritus={suoritus}
            />
          ))}
      </SuoritusSection>
      <SuoritusSection heading={t('oppija.perusopetus')}>
        {suoritukset
          .filter((s) => s.koulutustyyppi === 'perusopetus')
          ?.map((suoritus) => (
            <PerusopetusSuoritusPaper
              key={suoritus.tunniste}
              suoritus={suoritus}
            />
          ))}
      </SuoritusSection>
    </Stack>
  );
}
