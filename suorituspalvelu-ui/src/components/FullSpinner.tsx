import { useTranslations } from '@/hooks/useTranslations';
import { ophColors, styled } from '@/lib/theme';
import { CircularProgress } from '@mui/material';

const StyledCircularProgress = styled(CircularProgress)({
  color: ophColors.blue2,
});

export const UntranslatedFullSpinner = ({
  ariaLabel,
}: {
  ariaLabel?: string;
}) => {
  return (
    <div
      aria-label={ariaLabel}
      style={{
        position: 'relative',
        left: '0',
        top: '0',
        minHeight: '150px',
        maxHeight: '80vh',
        width: '100%',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
      }}
    >
      <StyledCircularProgress
        size={50}
        thickness={4}
        aria-label={ariaLabel ?? 'Ladataan...'}
      />
    </div>
  );
};

export const FullSpinner = ({ ariaLabel }: { ariaLabel?: string }) => {
  const { t } = useTranslations();
  return <UntranslatedFullSpinner ariaLabel={ariaLabel ?? t('ladataan')} />;
};
