import { Box } from '@mui/material';
import { OphButton, OphTypography } from '@opetushallitus/oph-design-system';
import { EditOutlined } from '@mui/icons-material';
import { useTranslations } from '@/hooks/useTranslations';
import type { AvainArvo } from '@/types/ui-types';

export const AvainArvoDisplay = ({
  avainArvo,
  startYliajoEdit,
}: {
  avainArvo: AvainArvo;
  startYliajoEdit?: (avainarvo: {
    avain: string;
    arvo: string;
    selite: string;
  }) => void;
}) => {
  const { t } = useTranslations();
  const labelId = `avainarvo-label-${avainArvo.avain}`;

  const alkuperainenArvo = avainArvo.metadata.arvoEnnenYliajoa;

  return (
    <Box
      key={avainArvo.avain}
      sx={{
        display: 'flex',
        flexDirection: 'column',
        flex: '0 0 calc(50% - 16px)',
        gap: 0,
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center' }}>
        <OphTypography id={labelId} variant="label">
          {avainArvo.avain}
        </OphTypography>
        {startYliajoEdit && (
          <OphButton
            variant="text"
            aria-label={t('opiskelijavalinnan-tiedot.muokkaa-kenttaa-avain', {
              avain: avainArvo.avain,
            })}
            onClick={() =>
              startYliajoEdit?.({
                arvo: avainArvo.arvo,
                avain: avainArvo.avain,
                selite: avainArvo.metadata.yliajo?.selite ?? '',
              })
            }
            startIcon={<EditOutlined />}
          />
        )}
      </Box>
      <OphTypography aria-labelledby={labelId}>
        {avainArvo.arvo}{' '}
        {alkuperainenArvo
          ? `(${t('opiskelijavalinnan-tiedot.alkuperainen')}: ${alkuperainenArvo})`
          : ''}
      </OphTypography>
    </Box>
  );
};
