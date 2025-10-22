import { Stack } from '@mui/material';
import { OphButton } from '@opetushallitus/oph-design-system';
import { Add } from '@mui/icons-material';
import { useTranslations } from '@/hooks/useTranslations';
import { EditSuoritusPaper } from './EditSuoritusPaper';
import { useSuoritusManager } from '@/lib/suoritusManager';

export const PerusopetusSuoritusAdder = ({
  henkiloOID,
}: {
  henkiloOID: string;
}) => {
  const { t } = useTranslations();

  const {
    suoritusPaperRef,
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
        }}
      >
        {t('muokkaus.suoritus.lisaa')}
      </OphButton>
      {suoritusFields && mode === 'new' && (
        <EditSuoritusPaper
          mode={mode}
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
