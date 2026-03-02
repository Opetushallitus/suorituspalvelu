import { ophColors } from '@opetushallitus/oph-design-system';
import { OphTypography } from '@opetushallitus/oph-design-system';
import { PaperWithTopColor } from '@/components/PaperWithTopColor';
import { LabeledInfoItem } from '../LabeledInfoItem';
import { useTranslations } from '@/hooks/useTranslations';
import { formatFinnishDateTime } from '@/lib/common';
import type { IVanhaVastaanottoUI } from '@/types/backend';
import { Stack } from '@mui/material';

export const VanhaVastaanottoPaper = ({
  vastaanotto,
}: {
  vastaanotto: IVanhaVastaanottoUI;
}) => {
  const { t } = useTranslations();

  return (
    <PaperWithTopColor
      topColor={ophColors.orange3}
      data-test-id="vanha-vastaanotto-paper"
    >
      <OphTypography variant="h5" component="h3" sx={{ marginBottom: 2 }}>
        {vastaanotto.hakukohdeNimi}
      </OphTypography>
      <Stack spacing={2}>
        <LabeledInfoItem
          label={t('vastaanotto.vastaanottoaika')}
          value={formatFinnishDateTime(vastaanotto.vastaanottoaika)}
        />
      </Stack>
    </PaperWithTopColor>
  );
};
