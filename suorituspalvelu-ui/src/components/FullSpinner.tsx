import { CircularProgress } from '@mui/material';
import { useTranslate } from '@tolgee/react';

export const FullSpinner = ({ ariaLabel }: { ariaLabel?: string }) => {
  const { t } = useTranslate();
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
      <CircularProgress aria-label={ariaLabel ?? t('ladataan')} />
    </div>
  );
};
