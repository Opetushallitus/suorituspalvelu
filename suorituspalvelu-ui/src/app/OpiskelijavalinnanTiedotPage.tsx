import { Box, Stack } from '@mui/material';
import {
  ophColors,
  OphFormFieldWrapper,
  OphTypography,
} from '@opetushallitus/oph-design-system';
import { useApiSuspenseQuery } from '@/lib/http-client';
import { queryOptionsGetValintadata } from '@/lib/suorituspalvelu-queries';
import type { Route } from './+types/OpiskelijavalinnanTiedotPage';
import { groupBy } from 'remeda';
import { useTranslations } from '@/hooks/useTranslations';
import { AccordionBox } from '@/components/AccordionBox';

const OPISKELIJAVALINTADATA_GROUPS = [
  'yleinen',
  'suoritukset',
  'lisapistekoulutukset',
  'perusopetuksen-oppiaineet',
] as const;

type OpiskelijavalintaDataGroups =
  (typeof OPISKELIJAVALINTADATA_GROUPS)[number];

const getOpiskelijavalintaGroup = (
  key: string,
): OpiskelijavalintaDataGroups => {
  if (key.startsWith('PK_') || key.startsWith('PERUSKOULU_')) {
    return 'perusopetuksen-oppiaineet';
  } else if (key.startsWith('lisapistekoulutus_')) {
    return 'lisapistekoulutukset';
  } else if (key.endsWith('_suoritettu') || key.endsWith('_suoritusvuosi')) {
    return 'suoritukset';
  } else {
    return 'yleinen';
  }
};

export default function OpiskelijavalinnanTiedotPage({
  params,
}: Route.ComponentProps) {
  const { data: valintadata } = useApiSuspenseQuery(
    queryOptionsGetValintadata({ oppijaNumero: params.oppijaNumero }),
  );

  const groupedValintaData = groupBy(valintadata.avainArvot, (item) =>
    getOpiskelijavalintaGroup(item.avain),
  );
  const { t } = useTranslations();

  return (
    <Stack spacing={3}>
      <AccordionBox
        id="opiskelijavalintaan-siirtyvat-tiedot"
        title={t(
          'opiskelijavalinnan-tiedot.suorituspalvelusta-opiskelijavalintaan-siirtyvat-tiedot',
        )}
      >
        {OPISKELIJAVALINTADATA_GROUPS.map((group) => {
          const items = groupedValintaData[group];
          return (
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
                  <OphFormFieldWrapper
                    key={avainArvo.avain}
                    sx={{ flex: '0 0 calc(50% - 16px)', margin: '4px' }}
                    label={avainArvo.avain}
                    renderInput={() => (
                      <OphTypography>{avainArvo.arvo}</OphTypography>
                    )}
                  />
                ))}
              </Stack>
            </Box>
          );
        })}
      </AccordionBox>
    </Stack>
  );
}
