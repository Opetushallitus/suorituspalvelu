import { Box } from '@mui/material';
import { OphButton, OphTypography } from '@opetushallitus/oph-design-system';
import { EditOutlined } from '@mui/icons-material';
import { useTranslations } from '@/hooks/useTranslations';
import type { AvainArvo } from '@/types/ui-types';
import { TooltipIcon } from '../TooltipIcon';
import { styled } from '@/lib/theme';

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

const TextButton = styled(OphButton)(({ theme }) => ({
  fontWeight: theme.typography.body1.fontWeight,
  display: 'inline',
  border: 'none',
  margin: 0,
  padding: 0,
  verticalAlign: 'top',
  marginLeft: theme.spacing(1),
}));

const MuutoshistoriaButton = ({
  showMuutoshistoria,
}: {
  showMuutoshistoria: () => void;
}) => {
  const { t } = useTranslations();

  return (
    <TextButton variant="text" onClick={showMuutoshistoria}>
      {t('opiskelijavalinnan-tiedot.nayta-muutoshistoria')}
    </TextButton>
  );
};

export const AvainArvoDisplay = ({
  avainArvo,
  startYliajoEdit,
  showMuutoshistoria,
}: {
  avainArvo: AvainArvo;
  showMuutoshistoria?: () => void;
  startYliajoEdit?: (avainarvo: {
    avain: string;
    arvo: string;
    selite: string;
  }) => void;
}) => {
  const { t } = useTranslations();
  const labelId = `avainarvo-label-${avainArvo.avain}`;
  const alkuperainenArvo = avainArvo.metadata.arvoEnnenYliajoa ?? '';

  const hasChangedArvo =
    avainArvo.metadata.yliajo && avainArvo.metadata.yliajo.arvo !== null;
  const arvonSelitteet = avainArvo.metadata.selitteet ?? [];
  const hasMuutoshistoria = Boolean(avainArvo.metadata.yliajo);

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
      <OphTypography component="span" aria-labelledby={labelId}>
        {hasChangedArvo ? (
          <>
            <span>{avainArvo.arvo}</span>
            <AlkuperainenArvo
              arvo={alkuperainenArvo}
              selitteet={arvonSelitteet}
            />{' '}
          </>
        ) : (
          <ArvoAndSelitteet arvo={avainArvo.arvo} selitteet={arvonSelitteet} />
        )}
        {hasMuutoshistoria && (
          <MuutoshistoriaButton
            showMuutoshistoria={() => {
              showMuutoshistoria?.();
            }}
          />
        )}
      </OphTypography>
    </Box>
  );
};
