import { Box, Stack } from '@mui/material';
import { useTranslations } from '@/hooks/useTranslations';
import { EditSuoritusPaper } from './EditSuoritusPaper';
import { useSuoritusManager } from '@/lib/suoritusManager';
import {
  type PerusopetuksenOppiaineenOppimaara,
  type PerusopetuksenOppimaara,
} from '@/types/ui-types';
import { OphButton } from '@opetushallitus/oph-design-system';
import { PerusopetusSuoritusDisplay } from './PerusopetusSuoritusDisplay';

export const PerusopetusSuoritusEditor = ({
  henkiloOID,
  suoritus: suoritusProp,
}: {
  henkiloOID: string;
  suoritus: PerusopetuksenOppimaara | PerusopetuksenOppiaineenOppimaara;
}) => {
  const { t } = useTranslations();

  const {
    suoritusPaperRef,
    suoritusFields,
    onSuoritusChange,
    onOppiaineChange,
    saveSuoritus,
    deleteSuoritus,
    startSuoritusEdit,
    stopSuoritusModify,
    mode,
  } = useSuoritusManager({ oppijaOid: henkiloOID });

  return (
    <Box>
      <Stack
        direction="column"
        spacing={2}
        sx={{ alignItems: 'flex-start', marginBottom: 2 }}
      >
        {suoritusFields && mode === 'existing' ? (
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
        ) : (
          <PerusopetusSuoritusDisplay
            suoritus={suoritusProp}
            actions={
              <Stack
                direction="row"
                spacing={2}
                sx={{ justifyContent: 'flex-end' }}
              >
                <OphButton
                  variant="outlined"
                  onClick={() => {
                    deleteSuoritus(suoritusProp.versioTunniste);
                  }}
                >
                  {t('muokkaus.suoritus.poista')}
                </OphButton>
                <OphButton
                  variant="contained"
                  onClick={() => {
                    startSuoritusEdit(suoritusProp);
                  }}
                >
                  {t('muokkaus.suoritus.muokkaa')}
                </OphButton>
              </Stack>
            }
          />
        )}
      </Stack>
    </Box>
  );
};
