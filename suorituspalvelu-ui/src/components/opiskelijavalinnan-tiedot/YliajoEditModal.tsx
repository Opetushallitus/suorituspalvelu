import { Stack } from '@mui/material';
import {
  OphButton,
  OphFormFieldWrapper,
  OphInput,
} from '@opetushallitus/oph-design-system';
import { useTranslations } from '@/hooks/useTranslations';
import type { AvainArvo, YliajoParams } from '@/types/ui-types';
import { OphModal } from '@/components/OphModal';
import { useCallback } from 'react';
import { DeleteOutline } from '@mui/icons-material';

export type YliajoMode = 'add' | 'edit';

export const YliajoEditModal = ({
  mode,
  yliajo,
  avainArvot,
  setYliajo,
  saveYliajo,
  deleteYliajo,
}: {
  mode: YliajoMode;
  henkiloOid: string;
  yliajo: YliajoParams | null;
  avainArvot: Array<AvainArvo>;
  setYliajo: (yliajo: YliajoParams | null) => void;
  saveYliajo: (updatedYliajo: YliajoParams) => void;
  deleteYliajo: (avain: string) => void;
}) => {
  const { t } = useTranslations();

  const onClose = useCallback(() => {
    setYliajo(null);
  }, [setYliajo]);

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
            {mode === 'edit' && originalAvainArvo?.metadata.yliajo && (
              <OphButton
                variant="outlined"
                startIcon={<DeleteOutline />}
                onClick={() => deleteYliajo(yliajo.avain)}
              >
                {originalAvainArvo?.metadata.arvoEnnenYliajoa !== null
                  ? t('opiskelijavalinnan-tiedot.poista-muokkaus')
                  : t('opiskelijavalinnan-tiedot.poista-kentta')}
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
                    setYliajo({
                      ...yliajo,
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
                  setYliajo({
                    ...yliajo,
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
                  setYliajo({
                    ...yliajo,
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
