import { ophColors } from '@opetushallitus/oph-design-system';
import { OphTypography } from '@opetushallitus/oph-design-system';
import { PaperWithTopColor } from '@/components/PaperWithTopColor';
import { LabeledInfoItem } from '../LabeledInfoItem';
import { useTranslations } from '@/hooks/useTranslations';
import { formatFinnishDateTime } from '@/lib/common';
import type { IVastaanottoUI } from '@/types/backend';
import { Stack } from '@mui/material';

export const VastaanottoPaper = ({
  vastaanotto,
}: {
  vastaanotto: IVastaanottoUI;
}) => {
  const { t, translateKielistetty } = useTranslations();

  const oppilaitokset = vastaanotto.hakukohdeOppilaitokset
    .map((o) => `${translateKielistetty(o.nimi)} (${o.oid})`)
    .join(', ');

  return (
    <PaperWithTopColor
      topColor={ophColors.orange3}
      data-test-id="vastaanotto-paper"
    >
      <OphTypography variant="h5" component="h3" sx={{ marginBottom: 2 }}>
        {translateKielistetty(vastaanotto.hakukohdeNimi)}
      </OphTypography>
      <Stack spacing={2}>
        <Stack direction="row" spacing={4}>
          <LabeledInfoItem
            label={t('vastaanotto.haku')}
            value={translateKielistetty(vastaanotto.hakuNimi)}
          />
          <LabeledInfoItem
            label={t('vastaanotto.oppilaitos')}
            value={oppilaitokset}
          />
        </Stack>
        <Stack direction="row" spacing={4}>
          <LabeledInfoItem
            label={t('vastaanotto.tila')}
            value={t(
              `vastaanotto-action.${vastaanotto.vastaanottoAction.toLowerCase()}`,
            )}
          />
          <LabeledInfoItem
            label={t('vastaanotto.vastaanottoaika')}
            value={formatFinnishDateTime(vastaanotto.vastaanottoaika)}
          />
        </Stack>
      </Stack>
    </PaperWithTopColor>
  );
};
