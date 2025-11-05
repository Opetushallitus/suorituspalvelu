import { Box, Stack, ToggleButton, ToggleButtonGroup } from '@mui/material';
import {
  ophColors,
  OphFormFieldWrapper,
  OphTypography,
} from '@opetushallitus/oph-design-system';
import { FetchError, useApiSuspenseQuery } from '@/lib/http-client';
import { queryOptionsGetValintadata } from '@/lib/suorituspalvelu-queries';
import type { Route } from './+types/OpiskelijavalinnanTiedotPage';
import { groupBy, mapValues, pipe, prop, sortBy } from 'remeda';
import { useTranslations } from '@/hooks/useTranslations';
import { AccordionBox } from '@/components/AccordionBox';
import React, { useState } from 'react';
import {
  isGenericBackendErrorResponse,
  type AvainArvo,
} from '@/types/ui-types';
import { styled } from '@/lib/theme';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { ErrorView } from '@/components/ErrorView';
import { ErrorAlert } from '@/components/ErrorAlert';

const OPISKELIJAVALINTADATA_GROUPS = [
  'yleinen',
  'suoritukset',
  'lisapistekoulutukset',
  'perusopetuksen-oppiaineet',
] as const;

type AvainarvoRyhma = 'uudet-avainarvot' | 'vanhat-avainarvot';

type OpiskelijavalintaDataGroups =
  (typeof OPISKELIJAVALINTADATA_GROUPS)[number];

const getOpiskelijavalintaGroup = (
  key: string,
): OpiskelijavalintaDataGroups => {
  if (key.startsWith('PK_ARVOSANA') || key.startsWith('PERUSKOULU_ARVOSANA')) {
    return 'perusopetuksen-oppiaineet';
  } else if (
    key.startsWith('lisapistekoulutus_') ||
    key.startsWith('LISAKOULUTUS_')
  ) {
    return 'lisapistekoulutukset';
  } else if (
    key.endsWith('_suoritettu') ||
    key.toLowerCase().endsWith('_suoritusvuosi') ||
    key.endsWith('_TILA')
  ) {
    return 'suoritukset';
  } else {
    return 'yleinen';
  }
};

const sortGroups = (
  groups: Partial<
    Record<OpiskelijavalintaDataGroups, Array<AvainArvo> | undefined>
  >,
) => {
  return mapValues(groups, (items, group) => {
    const alphabeticallySorted = sortBy(items ?? [], prop('avain'));
    if (group === 'perusopetuksen-oppiaineet') {
      return alphabeticallySorted.sort((a, b) => {
        // Järjestetään oppiaine ennen sen arvosanaa
        const aIsBsOppiaine =
          a.avain.includes('_OPPIAINE') && a.avain.includes(b.avain);
        const bIsAsOppiaine =
          b.avain.includes('_OPPIAINE') && b.avain.includes(a.avain);

        if (aIsBsOppiaine) {
          return -1;
        } else if (bIsAsOppiaine) {
          return 1;
        } else {
          return 0;
        }
      });
    } else if (group === 'suoritukset') {
      const order = [
        'perustutkinto_suoritettu',
        'PK_TILA',
        'peruskoulu_suoritusvuosi',
        'PK_SUORITUSVUOSI',
        'ammatillinen_suoritettu',
        'AM_TILA',
        'lukio_suoritettu',
        'LK_TILA',
        'yo-tutkinto_suoritettu',
        'YO_TILA',
      ];
      return alphabeticallySorted.sort((a, b) => {
        const aIndex = order.indexOf(a.avain);
        const bIndex = order.indexOf(b.avain);
        if (aIndex === -1 && bIndex === -1) return 0;
        if (aIndex === -1) return 1;
        if (bIndex === -1) return -1;
        return aIndex - bIndex;
      });
    }
    return alphabeticallySorted;
  });
};

const BreakFlex = styled('div')({
  flexBasis: '100%',
  height: 0,
});

const AvainArvotSection = ({
  avainarvot,
  avainArvoFilter,
}: {
  avainarvot: Array<AvainArvo>;
  avainArvoFilter: (avainArvo: AvainArvo) => boolean;
}) => {
  const { t } = useTranslations();

  const groupedAvainarvot = pipe(
    avainarvot,
    ($) => $.filter(avainArvoFilter),
    groupBy((item) => getOpiskelijavalintaGroup(item.avain)),
    (grouped) => sortGroups(grouped),
  );

  return (
    <Stack sx={{ gap: 2 }}>
      {OPISKELIJAVALINTADATA_GROUPS.map((group) => {
        const items = groupedAvainarvot[group];
        return (
          items && (
            <Box key={group}>
              {group !== 'yleinen' && (
                <Box
                  sx={{
                    width: '100%',
                    borderBottom: `1px solid ${ophColors.grey300}`,
                    marginBottom: 3,
                  }}
                >
                  <OphTypography variant="h5">
                    {t(`opiskelijavalinnan-tiedot.ryhmat.${group}`)}
                  </OphTypography>
                </Box>
              )}
              <Stack
                direction="row"
                sx={{
                  maxWidth: '100%',
                  flexWrap: 'wrap',
                  justifyContent: 'flex-start',
                  gap: 2,
                }}
              >
                {items?.map((avainArvo) => (
                  <React.Fragment key={avainArvo.avain}>
                    {avainArvo.avain.includes('_OPPIAINE') && <BreakFlex />}
                    <OphFormFieldWrapper
                      key={avainArvo.avain}
                      sx={{ flex: '0 0 calc(50% - 16px)', margin: '4px' }}
                      label={avainArvo.avain}
                      renderInput={({ labelId }) => (
                        <OphTypography aria-labelledby={labelId}>
                          {avainArvo.arvo}
                        </OphTypography>
                      )}
                    />
                  </React.Fragment>
                ))}
              </Stack>
            </Box>
          )
        );
      })}
    </Stack>
  );
};

const OpiskelijavalinnanTiedotPageContent = ({
  oppijaNumero,
}: {
  oppijaNumero: string;
}) => {
  const { data: valintadata } = useApiSuspenseQuery(
    queryOptionsGetValintadata({ oppijaNumero }),
  );

  const { t } = useTranslations();

  const [avainarvoRyhma, setAvainarvoRyhma] =
    useState<AvainarvoRyhma>('uudet-avainarvot');

  return (
    <Stack spacing={3}>
      <ToggleButtonGroup
        sx={{ alignSelf: 'flex-end' }}
        value={avainarvoRyhma}
        exclusive
        onChange={(_event, newValue) => {
          if (newValue) {
            setAvainarvoRyhma(newValue);
          }
        }}
      >
        <ToggleButton value="uudet-avainarvot">
          {t('opiskelijavalinnan-tiedot.uudet-avainarvot')}
        </ToggleButton>
        <ToggleButton value="vanhat-avainarvot">
          {t('opiskelijavalinnan-tiedot.vanhat-avainarvot')}
        </ToggleButton>
      </ToggleButtonGroup>
      <AccordionBox
        id="opiskelijavalintaan-siirtyvat-tiedot"
        title={t(
          'opiskelijavalinnan-tiedot.suorituspalvelusta-opiskelijavalintaan-siirtyvat-tiedot',
        )}
      >
        <AvainArvotSection
          avainarvot={valintadata.avainArvot}
          avainArvoFilter={(avainArvo) =>
            avainarvoRyhma === 'uudet-avainarvot'
              ? !avainArvo.metadata.duplikaatti
              : avainArvo.metadata.duplikaatti
          }
        />
      </AccordionBox>
    </Stack>
  );
};

const ErrorFallback = ({
  reset,
  error,
}: {
  reset: () => void;
  error: Error;
}) => {
  const { t } = useTranslations();
  if (error instanceof FetchError) {
    const jsonBody = error.jsonBody;
    if (isGenericBackendErrorResponse(jsonBody)) {
      return (
        <ErrorAlert
          title={t('opiskelijavalinnan-tiedot.virhe-valintadatan-haussa')}
          message={jsonBody.virheAvaimet.map((virhe) => t(virhe))}
        />
      );
    }
  }

  return <ErrorView error={error} reset={reset} />;
};

export default function OpiskelijavalinnanTiedotPage({
  params,
}: Route.ComponentProps) {
  return (
    <QuerySuspenseBoundary ErrorFallback={ErrorFallback}>
      <OpiskelijavalinnanTiedotPageContent oppijaNumero={params.oppijaNumero} />
    </QuerySuspenseBoundary>
  );
}
