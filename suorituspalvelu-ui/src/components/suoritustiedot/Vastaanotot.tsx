import { Box, Stack } from '@mui/material';
import { OphTypography } from '@opetushallitus/oph-design-system';
import { sortBy } from 'remeda';
import { VastaanottoPaper } from './VastaanottoPaper';
import { VanhaVastaanottoPaper } from './VanhaVastaanottoPaper';
import { useOppijanVastaanotot } from '@/lib/suorituspalvelu-queries';
import { useTranslations } from '@/hooks/useTranslations';

export function Vastaanotot({ oppijaNumero }: { oppijaNumero: string }) {
  const { t } = useTranslations();
  const { data: vastaanottoData } = useOppijanVastaanotot(oppijaNumero);

  const hasVastaanotot =
    (vastaanottoData?.vastaanotot?.length ?? 0) > 0 ||
    (vastaanottoData?.vanhatVastaanotot?.length ?? 0) > 0;

  if (!hasVastaanotot) {
    return null;
  }

  return (
    <Box>
      <OphTypography variant="h3" component="h2" sx={{ marginBottom: 2 }}>
        {t('vastaanotto.vastaanotot')}
      </OphTypography>
      <Stack spacing={4}>
        {sortBy(vastaanottoData?.vastaanotot ?? [], [
          (v) => v.vastaanottoaika,
          'desc',
        ]).map((vastaanotto) => (
          <VastaanottoPaper
            key={`${vastaanotto.hakuOid}-${vastaanotto.hakukohdeOid}`}
            vastaanotto={vastaanotto}
          />
        ))}
        {sortBy(vastaanottoData?.vanhatVastaanotot ?? [], [
          (v) => v.vastaanottoaika,
          'desc',
        ]).map((vastaanotto) => (
          <VanhaVastaanottoPaper
            key={`${vastaanotto.hakukohdeNimi}-${vastaanotto.vastaanottoaika}`}
            vastaanotto={vastaanotto}
          />
        ))}
      </Stack>
    </Box>
  );
}
