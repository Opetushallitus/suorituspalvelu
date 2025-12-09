import { Box } from '@mui/material';
import { OphButton, OphTypography } from '@opetushallitus/oph-design-system';
import { EditOutlined } from '@mui/icons-material';
import { useTranslations } from '@/hooks/useTranslations';
import type { AvainArvo } from '@/types/ui-types';
import { TooltipIcon } from '../TooltipIcon';

const ArvoAndSelitteet = ({
  arvo,
  selitteet,
}: {
  arvo: string;
  selitteet?: Array<string>;
}) => {
  const { t } = useTranslations();
  return (
    <OphTypography component="span">
      {arvo}
      {selitteet && selitteet?.length > 0 && (
        <span>
          {' '}
          <TooltipIcon
            ariaLabel={t('opiskelijavalinnan-tiedot.arvon-selitteet-label')}
          >
            <>
              {selitteet.map((selite) => (
                <OphTypography key={selite}>{selite}</OphTypography>
              ))}
            </>
          </TooltipIcon>
        </span>
      )}
    </OphTypography>
  );
};

const AlkuperainenArvo = ({
  arvo,
  selitteet,
}: {
  arvo: string;
  selitteet?: Array<string>;
}) => {
  const { t } = useTranslations();
  return (
    <OphTypography component="span">
      <span>{' ('}</span>
      {t('opiskelijavalinnan-tiedot.alkuperainen')}
      <span>{': '}</span>
      <ArvoAndSelitteet arvo={arvo} selitteet={selitteet} />
      <span>{')'}</span>
    </OphTypography>
  );
};

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
  const arvonSelitteet = avainArvo.metadata.selitteet ?? [];

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
        <OphTypography id={labelId} variant="label" component="label">
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
      <span aria-labelledby={labelId}>
        {alkuperainenArvo ? (
          <>
            <span>{avainArvo.arvo}</span>
            <AlkuperainenArvo
              arvo={alkuperainenArvo}
              selitteet={arvonSelitteet}
            />
          </>
        ) : (
          <ArvoAndSelitteet arvo={avainArvo.arvo} selitteet={arvonSelitteet} />
        )}
      </span>
    </Box>
  );
};
