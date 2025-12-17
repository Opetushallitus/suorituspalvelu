import { Box, Stack } from '@mui/material';
import { ophColors, OphTypography } from '@opetushallitus/oph-design-system';
import { useTranslations } from '@/hooks/useTranslations';
import type { AvainArvo } from '@/types/ui-types';
import React, { useMemo } from 'react';
import { styled } from '@/lib/theme';
import { AvainArvoDisplay } from './AvainArvoDisplay';
import {
  groupAndSortAvainarvot,
  OPISKELIJAVALINTADATA_GROUPS,
} from './opiskelijavalintadataUtils';

const BreakFlex = styled('div')({
  flexBasis: '100%',
  height: 0,
});

export const AvainArvotSection = ({
  avainarvot,
  avainArvoFilter,
  startYliajoEdit,
}: {
  avainarvot?: Array<AvainArvo>;
  avainArvoFilter?: (avainArvo: AvainArvo) => boolean;
  startYliajoEdit?: (avainarvo: {
    avain: string;
    arvo: string;
    selite: string;
  }) => void;
}) => {
  const { t } = useTranslations();

  const groupedAvainarvot = useMemo(
    () => groupAndSortAvainarvot(avainarvot ?? [], avainArvoFilter),
    [avainarvot, avainArvoFilter],
  );

  return (
    <Stack sx={{ gap: 3 }}>
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
                    marginBottom: 2,
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
                {items?.map((avainArvo, index) => (
                  <React.Fragment key={avainArvo.avain}>
                    {avainArvo.avain.includes('_OPPIAINE') && index !== 0 && (
                      <BreakFlex />
                    )}
                    <AvainArvoDisplay
                      avainArvo={avainArvo}
                      startYliajoEdit={startYliajoEdit}
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
