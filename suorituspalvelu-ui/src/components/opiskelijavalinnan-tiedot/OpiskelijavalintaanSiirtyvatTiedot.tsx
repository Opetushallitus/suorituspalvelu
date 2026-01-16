import { Stack } from '@mui/material';
import { OphButton } from '@opetushallitus/oph-design-system';
import { useTranslations } from '@/hooks/useTranslations';
import { AccordionBox } from '@/components/AccordionBox';
import { Add } from '@mui/icons-material';
import { AvainArvotSection } from './AvainArvotSection';
import { YliajoEditModal } from './YliajoEditModal';
import { useYliajoManager } from '@/lib/yliajoManager';
import type { AvainArvo, ValintaData } from '@/types/ui-types';
import { useState } from 'react';
import { MuutoshistoriaModal } from './MuutoshistoriaModal';

export const OpiskelijavalintaanSiirtyvatTiedot = ({
  oppijaNumero,
  valintaData,
  hakuOid,
}: {
  oppijaNumero: string;
  hakuOid: string;
  valintaData: ValintaData;
}) => {
  const { t } = useTranslations();

  const {
    yliajoMutation,
    startYliajoEdit,
    startYliajoAdd,
    yliajoFields,
    deleteYliajo,
  } = useYliajoManager({
    henkiloOid: oppijaNumero,
  });

  const avainArvot =
    valintaData?.avainArvot.filter(
      (avainArvo) => !avainArvo.metadata.arvoOnHakemukselta,
    ) ?? [];

  const [selectedMuutoshistoriaAvainArvo, setSelectedMuutoshistoriaAvainArvo] =
    useState<AvainArvo | null>(null);

  return (
    <>
      {selectedMuutoshistoriaAvainArvo && hakuOid && oppijaNumero && (
        <MuutoshistoriaModal
          hakuOid={hakuOid}
          oppijaNumero={oppijaNumero}
          avainArvo={selectedMuutoshistoriaAvainArvo}
          deleteYliajo={deleteYliajo}
          onClose={() => setSelectedMuutoshistoriaAvainArvo(null)}
        />
      )}
      {!yliajoMutation.isPending && !yliajoMutation.isError && yliajoFields && (
        <YliajoEditModal avainArvot={avainArvot} />
      )}
      <AccordionBox
        id="opiskelijavalintaan-siirtyvat-tiedot"
        title={t(
          'opiskelijavalinnan-tiedot.suorituspalvelusta-opiskelijavalintaan-siirtyvat-tiedot',
        )}
      >
        <AvainArvotSection
          avainarvot={avainArvot}
          showMuutoshistoria={(avainArvo) => {
            setSelectedMuutoshistoriaAvainArvo(avainArvo);
          }}
          startYliajoEdit={(yliajoParams) => {
            startYliajoEdit({
              arvo: yliajoParams.arvo,
              avain: yliajoParams.avain,
              selite: yliajoParams.selite,
            });
          }}
        />
        <Stack direction="row" sx={{ justifyContent: 'flex-end' }}>
          <OphButton
            startIcon={<Add />}
            variant="outlined"
            onClick={() => {
              startYliajoAdd();
            }}
          >
            {t('opiskelijavalinnan-tiedot.lisaa-kentta')}
          </OphButton>
        </Stack>
      </AccordionBox>
    </>
  );
};
