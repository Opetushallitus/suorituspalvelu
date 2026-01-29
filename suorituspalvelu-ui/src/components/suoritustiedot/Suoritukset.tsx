import { Box, Stack } from '@mui/material';
import { OphTypography } from '@opetushallitus/oph-design-system';
import { SuorituksetKoulutustyypeittain } from './SuorituksetKoulutustyypeittain';
import type { OppijanTiedot } from '@/types/ui-types';
import { PerusopetusSuoritusAdder } from './PerusopetusSuoritusAdder';
import { useKayttaja } from '@/lib/suorituspalvelu-queries';
import { useTranslations } from '@/hooks/useTranslations';

export function Suoritukset({
  oppijanTiedot,
}: {
  oppijanTiedot: OppijanTiedot;
}) {
  const { t } = useTranslations();

  const { data: kayttaja } = useKayttaja();

  return (
    <Box data-test-id="suoritukset">
      <Stack
        direction="row"
        sx={{ justifyContent: 'space-between', alignItems: 'flex-start' }}
      >
        <OphTypography
          variant="h3"
          component="h2"
          sx={{ marginBottom: 2, alignSelf: 'flex-end' }}
        >
          {t('oppija.suoritukset')}
        </OphTypography>
      </Stack>
      {kayttaja.isRekisterinpitaja && (
        <PerusopetusSuoritusAdder henkiloOID={oppijanTiedot.henkiloOID} />
      )}
      <SuorituksetKoulutustyypeittain oppijanTiedot={oppijanTiedot} />
    </Box>
  );
}
