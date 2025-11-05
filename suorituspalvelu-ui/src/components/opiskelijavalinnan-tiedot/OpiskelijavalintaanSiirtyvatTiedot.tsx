import { Stack } from '@mui/material';
import { OphButton } from '@opetushallitus/oph-design-system';
import { useApiSuspenseQuery } from '@/lib/http-client';
import { queryOptionsGetValintadata } from '@/lib/suorituspalvelu-queries';
import { useTranslations } from '@/hooks/useTranslations';
import { AccordionBox } from '@/components/AccordionBox';
import { useState } from 'react';
import type { YliajoParams } from '@/types/ui-types';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { saveYliajot } from '@/lib/suorituspalvelu-service';
import { SpinnerModal } from '@/components/SpinnerModal';
import { Add } from '@mui/icons-material';
import { AvainArvotSection } from './AvainArvotSection';
import { YliajoEditModal, type YliajoMode } from './YliajoEditModal';
import { YliajoErrorModal } from './YliajoErrorModal';

export type AvainarvoRyhma = 'uudet-avainarvot' | 'vanhat-avainarvot';

const DUMMY_HAKU_OID = '1.2.246.562.29.00000000000000000000';

const EMPTY_YLIAJO = {
  avain: '',
  arvo: '',
  selite: '',
} as const;

export const OpiskelijavalintaanSiirtyvatTiedot = ({
  avainarvoRyhma,
  oppijaNumero,
}: {
  avainarvoRyhma: AvainarvoRyhma;
  oppijaNumero: string;
}) => {
  const { data: valintadata } = useApiSuspenseQuery(
    queryOptionsGetValintadata({ oppijaNumero, hakuOid: DUMMY_HAKU_OID }),
  );

  const { t } = useTranslations();

  const [mode, setMode] = useState<YliajoMode>('edit');

  const [yliajo, setYliajo] = useState<YliajoParams | null>(null);

  const queryClient = useQueryClient();

  const yliajoMutation = useMutation({
    mutationFn: (updatedYliajo: YliajoParams) => {
      return saveYliajot({
        hakuOid: DUMMY_HAKU_OID,
        henkiloOid: oppijaNumero,
        yliajot: [
          {
            arvo: updatedYliajo.arvo,
            avain: updatedYliajo.avain,
            selite: updatedYliajo.selite,
          },
        ],
      });
    },
    onSuccess: () => {
      setYliajo(null);
      queryClient.resetQueries(
        queryOptionsGetValintadata({ oppijaNumero, hakuOid: DUMMY_HAKU_OID }),
      );
    },
  });

  return (
    <>
      {yliajoMutation.isPending ? (
        <SpinnerModal
          open={yliajoMutation.isPending}
          title={t('opiskelijavalinnan-tiedot.tallennetaan-yliajoa')}
        />
      ) : yliajoMutation.isError ? (
        <YliajoErrorModal
          error={yliajoMutation.error}
          onClose={() => yliajoMutation.reset()}
        />
      ) : (
        <YliajoEditModal
          mode={mode}
          henkiloOid={oppijaNumero}
          yliajo={yliajo}
          setYliajo={setYliajo}
          saveYliajo={yliajoMutation.mutate}
        />
      )}
      <AccordionBox
        id="opiskelijavalintaan-siirtyvat-tiedot"
        title={t(
          'opiskelijavalinnan-tiedot.suorituspalvelusta-opiskelijavalintaan-siirtyvat-tiedot',
        )}
      >
        <AvainArvotSection
          avainarvot={valintadata.avainArvot}
          startYliajoEdit={
            avainarvoRyhma === 'uudet-avainarvot'
              ? (yliajoParams) => {
                  setMode('edit');
                  setYliajo({
                    arvo: yliajoParams.arvo,
                    avain: yliajoParams.avain,
                    selite: yliajoParams.selite,
                  });
                }
              : undefined
          }
          avainArvoFilter={(avainArvo) =>
            avainarvoRyhma === 'uudet-avainarvot'
              ? !avainArvo.metadata.duplikaatti
              : avainArvo.metadata.duplikaatti
          }
        />
        {avainarvoRyhma === 'uudet-avainarvot' && (
          <Stack direction="row" sx={{ justifyContent: 'flex-end' }}>
            <OphButton
              startIcon={<Add />}
              variant="outlined"
              onClick={() => {
                setMode('add');
                setYliajo(EMPTY_YLIAJO);
              }}
            >
              {t('opiskelijavalinnan-tiedot.lisaa-kentta')}
            </OphButton>
          </Stack>
        )}
      </AccordionBox>
    </>
  );
};
