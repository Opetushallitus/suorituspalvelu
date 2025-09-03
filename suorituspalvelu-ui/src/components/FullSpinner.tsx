import { useTranslations } from '@/hooks/useTranslations';
import { CircularProgress } from '@mui/material';

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
      <CircularProgress aria-label={ariaLabel ?? 'Ladataan...'} />
    </div>
  );
};

export const FullSpinner = ({ ariaLabel }: { ariaLabel?: string }) => {
  const { t } = useTranslations();
  return <UntranslatedFullSpinner ariaLabel={ariaLabel ?? t('ladataan')} />;
};
