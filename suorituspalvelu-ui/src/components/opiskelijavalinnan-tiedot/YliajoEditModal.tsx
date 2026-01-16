import { Stack } from '@mui/material';
import {
  OphButton,
  OphFormFieldWrapper,
  OphInput,
} from '@opetushallitus/oph-design-system';
import { useTranslations } from '@/hooks/useTranslations';
import type { AvainArvo } from '@/types/ui-types';
import { OphModal } from '@/components/OphModal';
import { useCallback } from 'react';
import { DeleteOutline } from '@mui/icons-material';
import { useYliajoManager } from '@/lib/yliajoManager';

export const YliajoEditModal = ({
  avainArvot,
}: {
  avainArvot: Array<AvainArvo>;
}) => {
  const { t } = useTranslations();

  const {
    yliajoFields: yliajo,
    mode,
    stopYliajoEdit,
    onYliajoChange,
    saveYliajo,
    deleteYliajo,
  } = useYliajoManager();

  const onClose = useCallback(() => {
    stopYliajoEdit();
  }, [stopYliajoEdit]);

  const originalAvainArvo = avainArvot.find((a) => a.avain === yliajo?.avain);

  return (
    yliajo && (
      <OphModal
        open={yliajo !== null}
        onClose={onClose}
        title={
          mode === 'add'
            ? t('opiskelijavalinnan-tiedot.lisaa-kentta')
            : t('opiskelijavalinnan-tiedot.muokkaa-kenttaa')
        }
        maxWidth="sm"
        actions={
          <>
            {mode === 'edit' &&
              originalAvainArvo?.metadata.yliajo?.arvo != null && (
                <OphButton
                  variant="outlined"
                  startIcon={<DeleteOutline />}
                  onClick={() => deleteYliajo(yliajo.avain)}
                >
                  {t('opiskelijavalinnan-tiedot.poista-yliajo')}
                </OphButton>
              )}
            <OphButton variant="outlined" onClick={onClose}>
              {t('peruuta')}
            </OphButton>
            <OphButton
              variant="contained"
              onClick={() => {
                if (yliajo) {
                  saveYliajo(yliajo);
                }
              }}
            >
              {t('opiskelijavalinnan-tiedot.tallenna')}
            </OphButton>
          </>
        }
      >
        <Stack sx={{ alignItems: 'flex-start', gap: 2, overflow: 'visible' }}>
          {mode === 'add' && (
            <OphFormFieldWrapper
              label={t('opiskelijavalinnan-tiedot.avain')}
              sx={{ minWidth: '50%', overflow: 'visible' }}
              renderInput={({ labelId }) => (
                <OphInput
                  value={yliajo?.avain ?? ''}
                  inputProps={{ 'aria-labelledby': labelId }}
                  onChange={(event) => {
                    onYliajoChange({
                      avain: event.target.value ?? '',
                    });
                  }}
                />
              )}
            />
          )}
          <OphFormFieldWrapper
            label={
              mode === 'add'
                ? t('opiskelijavalinnan-tiedot.arvo')
                : yliajo?.avain
            }
            sx={{ minWidth: '50%', overflow: 'visible' }}
            renderInput={({ labelId }) => (
              <OphInput
                value={yliajo?.arvo ?? ''}
                inputProps={{ 'aria-labelledby': labelId }}
                onChange={(event) => {
                  onYliajoChange({
                    arvo: event.target.value ?? '',
                  });
                }}
              />
            )}
          />
          <OphFormFieldWrapper
            sx={{ alignSelf: 'stretch' }}
            label={t('opiskelijavalinnan-tiedot.selite')}
            helperText={t('opiskelijavalinnan-tiedot.selite-aputeksti')}
            renderInput={({ labelId }) => (
              <OphInput
                value={yliajo?.selite ?? ''}
                inputProps={{ 'aria-labelledby': labelId }}
                onChange={(event) => {
                  onYliajoChange({
                    selite: event.target.value ?? '',
                  });
                }}
                multiline={true}
                minRows={3}
              />
            )}
          />
        </Stack>
      </OphModal>
    )
  );
};
