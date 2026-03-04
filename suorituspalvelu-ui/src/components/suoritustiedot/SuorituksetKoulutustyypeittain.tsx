import { Box, Stack } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { OphTypography } from '@opetushallitus/oph-design-system';
import { KorkeakouluSuoritusPaper } from './KorkeakouluSuoritusPaper';
import { LukioSuoritusPaper } from './LukioSuoritusPaper';
import { AmmatillinenSuoritusPaper } from './AmmatillinenSuoritusPaper';
import { VapaaSivistystyoSuoritusPaper } from './VapaaSivistystyoSuoritusPaper';
import { TuvaSuoritusPaper } from './TuvaSuoritusPaper';
import { PerusopetusSuoritusPaper } from './PerusopetusSuoritusPaper';
import type { OppijanTiedot, Suoritus } from '@/types/ui-types';
import { useSuorituksetFlattened } from '@/hooks/useSuorituksetFlattened';
import React from 'react';
import { partition } from 'remeda';
import { useTranslations } from '@/hooks/useTranslations';
import { truthyReactNodes } from '@/lib/common';
import { SimpleAccordion } from '../SimpleAccordion';

function SuorituksetSection({
  heading,
  children,
}: {
  heading: string;
  children: React.ReactNode;
}) {
  const hasChildren =
    // eslint-disable-next-line @eslint-react/no-children-to-array
    truthyReactNodes(React.Children.toArray(children)).length > 0;

  return (
    hasChildren && (
      <Box sx={{ marginBottom: 4 }}>
        <OphTypography variant="h4" component="h3" sx={{ marginBottom: 2 }}>
          {heading}
        </OphTypography>
        <Stack spacing={4}>{children}</Stack>
      </Box>
    )
  );
}

const KorkeakouluSuorituksetSection = ({
  suoritukset,
}: {
  suoritukset: Array<Suoritus>;
}) => {
  const { t } = useTranslations();

  const korkeakouluSuoritukset = suoritukset.filter(
    (s) => s.koulutustyyppi === 'korkeakoulutus',
  );

  const [tutkintoonJohtavat, tutkintoonJohtamattomat] = partition(
    korkeakouluSuoritukset,
    (s) => s.isTutkintoonJohtava,
  );

  return (
    <>
      <SuorituksetSection
        heading={t('oppija.tutkintoon-johtavat-kk-suoritukset')}
      >
        {tutkintoonJohtavat.map((suoritus) => (
          <KorkeakouluSuoritusPaper
            key={suoritus.tunniste}
            suoritus={suoritus}
          />
        ))}
      </SuorituksetSection>
      {tutkintoonJohtamattomat.length > 0 && (
        <SimpleAccordion
          titleOpen={t('oppija.tutkintoon-johtamattomat-kk-suoritukset')}
          titleClosed={t('oppija.tutkintoon-johtamattomat-kk-suoritukset')}
          titleVariant="h4"
          titleComponent="h3"
        >
          <Stack spacing={4} sx={{ paddingTop: 2 }}>
            {tutkintoonJohtamattomat.map((suoritus) => (
              <KorkeakouluSuoritusPaper
                key={suoritus.tunniste}
                suoritus={suoritus}
              />
            ))}
          </Stack>
        </SimpleAccordion>
      )}
    </>
  );
};

export function SuorituksetKoulutustyypeittain({
  oppijanTiedot,
}: {
  oppijanTiedot: OppijanTiedot;
}) {
  const { t } = useTranslate();

  const suoritukset = useSuorituksetFlattened(oppijanTiedot);

  return (
    <Stack spacing={4}>
      <KorkeakouluSuorituksetSection suoritukset={suoritukset} />
      <SuorituksetSection heading={t('oppija.lukiokoulutus')}>
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
      </SuorituksetSection>
      <SuorituksetSection heading={t('oppija.ammatillinen-koulutus')}>
        {suoritukset
          .filter((s) => s.koulutustyyppi === 'ammatillinen')
          ?.map((suoritus) => (
            <AmmatillinenSuoritusPaper
              key={suoritus.tunniste}
              suoritus={suoritus}
            />
          ))}
      </SuorituksetSection>
      <SuorituksetSection heading={t('oppija.tuva')}>
        {suoritukset
          .filter((s) => s.koulutustyyppi === 'tuva')
          ?.map((suoritus) => (
            <TuvaSuoritusPaper key={suoritus.tunniste} suoritus={suoritus} />
          ))}
      </SuorituksetSection>
      <SuorituksetSection heading={t('oppija.vapaa-sivistystyo')}>
        {suoritukset
          .filter((s) => s.koulutustyyppi === 'vapaa-sivistystyo')
          ?.map((suoritus) => (
            <VapaaSivistystyoSuoritusPaper
              key={suoritus.tunniste}
              suoritus={suoritus}
            />
          ))}
      </SuorituksetSection>
      <SuorituksetSection heading={t('oppija.perusopetus')}>
        {suoritukset
          .filter((s) => s.koulutustyyppi === 'perusopetus')
          ?.map((suoritus) => (
            <PerusopetusSuoritusPaper
              key={suoritus.tunniste}
              henkiloOID={oppijanTiedot.henkiloOID}
              suoritus={suoritus}
            />
          ))}
      </SuorituksetSection>
    </Stack>
  );
}
