import {
  Stack,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
} from '@mui/material';
import { OphButton, OphTypography } from '@opetushallitus/oph-design-system';
import { useTranslations } from '@/hooks/useTranslations';
import { History } from '@mui/icons-material';
import type { AvainArvo } from '@/types/ui-types';
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

export const MuutoshistoriaModal = ({
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
