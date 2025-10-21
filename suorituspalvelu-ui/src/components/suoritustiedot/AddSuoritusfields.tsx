import { Stack } from '@mui/material';
import { OphButton } from '@opetushallitus/oph-design-system';
import { useRef } from 'react';
import { Add } from '@mui/icons-material';
import { useTranslations } from '@/hooks/useTranslations';
import { EditSuoritusPaper } from './EditSuoritusPaper';
import { useSuoritusManager } from '@/lib/suoritusManager';

export const AddSuoritusFields = ({ henkiloOID }: { henkiloOID: string }) => {
  const { t } = useTranslations();
  const suoritusPaperRef = useRef<HTMLDivElement | null>(null);

  const {
    mode,
    suoritusFields,
    saveSuoritus,
    onSuoritusChange,
    onOppiaineChange,
    startSuoritusAdd,
    stopSuoritusModify,
  } = useSuoritusManager({ oppijaOid: henkiloOID });

  return (
    <Stack
      direction="column"
      spacing={2}
      sx={{ alignItems: 'flex-start', marginBottom: 2 }}
    >
      <OphButton
        variant="outlined"
        startIcon={<Add />}
        onClick={() => {
          startSuoritusAdd();
          setTimeout(() => {
            suoritusPaperRef.current?.scrollIntoView({
              behavior: 'smooth',
            });
          }, 20);
        }}
      >
        {t('muokkaus.suoritus.lisaa')}
      </OphButton>
      {suoritusFields && mode === 'add' && (
        <EditSuoritusPaper
          suoritus={suoritusFields}
          ref={suoritusPaperRef}
          onSuoritusChange={onSuoritusChange}
          onOppiaineChange={onOppiaineChange}
          onSave={() => {
            saveSuoritus();
          }}
          onCancel={() => {
            stopSuoritusModify();
          }}
        />
      )}
    </Stack>
  );
};
