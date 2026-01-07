import {
  Stack,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
} from '@mui/material';
import { OphButton, OphTypography } from '@opetushallitus/oph-design-system';
import { useTranslations } from '@/hooks/useTranslations';
import { AccordionBox } from '@/components/AccordionBox';
import { Add, History } from '@mui/icons-material';
import { AvainArvotSection } from './AvainArvotSection';
import { YliajoEditModal } from './YliajoEditModal';
import { useYliajoManager } from '@/lib/yliajoManager';
import type { AvainArvo, ValintaData } from '@/types/ui-types';
import { useState } from 'react';
import { useApiSuspenseQuery } from '@/lib/http-client';
import { queryOptionsGetValintadataHistoria } from '@/lib/suorituspalvelu-queries';
import { Box } from '@mui/system';
import { StripedTable } from '../StripedTable';
import { formatFinnishDateTime } from '@/lib/common';
import { QuerySuspenseBoundary } from '../QuerySuspenseBoundary';
import { OphModal } from '../OphModal';

const MuutoshistoriaContent = ({
  oppijaNumero,
  hakuOid,
  avainArvo,
  deleteYliajo,
  onClose,
}: {
  oppijaNumero: string;
  hakuOid: string;
  avainArvo: AvainArvo;
  deleteYliajo: (avain: string) => void;
  onClose: () => void;
}) => {
  const { t } = useTranslations();

  const { data } = useApiSuspenseQuery(
    queryOptionsGetValintadataHistoria({
      oppijaNumero,
      hakuOid,
      avain: avainArvo.avain,
    }),
  );

  const alkuperainenArvo = avainArvo.metadata.arvoEnnenYliajoa;

  return (
    <Stack>
      {avainArvo.metadata.yliajo?.arvo !== null && (
        <Box sx={{ paddingTop: 1, paddingBottom: 3 }}>
          {t('opiskelijavalinnan-tiedot.alkuperainen')}:{' '}
          {alkuperainenArvo ??
            t('opiskelijavalinnan-tiedot.muutoshistoria.ei-arvoa')}
          <OphButton
            startIcon={<History />}
            onClick={() => {
              deleteYliajo(avainArvo.avain);
              onClose();
            }}
          >
            {t('opiskelijavalinnan-tiedot.muutoshistoria.palauta')}
          </OphButton>
        </Box>
      )}
      <StripedTable>
        <TableHead>
          <TableRow>
            <TableCell>
              {t('opiskelijavalinnan-tiedot.muutoshistoria.muokattu')}
            </TableCell>
            <TableCell>
              {t('opiskelijavalinnan-tiedot.muutoshistoria.arvo')}
            </TableCell>
            <TableCell>
              {t('opiskelijavalinnan-tiedot.muutoshistoria.muutoksen-syy')}
            </TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {data.muutokset.map((muutos) => (
            <TableRow key={muutos.luotu}>
              <TableCell>
                <OphTypography>
                  {formatFinnishDateTime(muutos.luotu)}
                </OphTypography>
                <OphTypography>{muutos.virkailija}</OphTypography>
              </TableCell>
              <TableCell>{muutos.arvo}</TableCell>
              <TableCell>
                {muutos.arvo === null
                  ? t(
                      'opiskelijavalinnan-tiedot.muutoshistoria.yliajo-poistettu',
                    )
                  : muutos.selite}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </StripedTable>
    </Stack>
  );
};

const MuutoshistoriaModal = ({
  avainArvo,
  hakuOid,
  oppijaNumero,
  deleteYliajo,
  onClose,
}: {
  avainArvo: AvainArvo;
  hakuOid: string;
  oppijaNumero: string;
  deleteYliajo: (avain: string) => void;
  onClose: () => void;
}) => {
  const { t } = useTranslations();
  return (
    <OphModal
      open={Boolean(avainArvo)}
      onClose={onClose}
      title={avainArvo.avain}
      maxWidth="md"
      actions={
        <OphButton variant="contained" onClick={onClose}>
          {t('sulje')}
        </OphButton>
      }
    >
      <QuerySuspenseBoundary>
        <MuutoshistoriaContent
          avainArvo={avainArvo}
          hakuOid={hakuOid}
          oppijaNumero={oppijaNumero}
          deleteYliajo={deleteYliajo}
          onClose={onClose}
        />
      </QuerySuspenseBoundary>
    </OphModal>
  );
};

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
