import { OphTypography } from '@opetushallitus/oph-design-system';
import { AmmatillinenSuoritus, TutkinnonOsanOsaAlue } from '@/types/ui-types';
import { useTranslations } from '@/hooks/useTranslations';
import {
  Box,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
} from '@mui/material';
import { isEmpty, sumBy } from 'remeda';
import { AccordionTableItem } from './AccordionTable';
import { StripedTable } from './StripedTable';
import { styled } from '@/lib/theme';
import { formatDate } from 'date-fns';

const OsaAlueetTable = ({
  osaAlueet,
}: {
  osaAlueet: Array<TutkinnonOsanOsaAlue>;
}) => {
  const { t, translateKielistetty } = useTranslations();
  return (
    !isEmpty(osaAlueet) && (
      <StripedTable
        sx={{
          tableLayout: 'fixed',
          '& .MuiTableCell-root': {
            '&:nth-of-type(2)': {
              width: '200px',
            },
            '&:nth-of-type(3)': {
              width: '184px',
            },
          },
        }}
      >
        <TableHead>
          <TableRow>
            <TableCell>{t('oppija.osa-alue')}</TableCell>
            <TableCell>
              {t('oppija.laajuus-yksikolla', {
                unit: t('oppija.lyhenne-osaamispiste'),
              })}
            </TableCell>
            <TableCell>{t('oppija.arvosana')}</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {osaAlueet.map((osaAlue) => {
            const nimi = translateKielistetty(osaAlue.nimi);
            return (
              <TableRow key={nimi}>
                <TableCell>{nimi}</TableCell>
                <TableCell>{osaAlue.laajuus}</TableCell>
                <TableCell>{osaAlue.arvosana}</TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </StripedTable>
    )
  );
};

const StyledOsatTable = styled(Table)({
  tableLayout: 'fixed',
  width: '100%',
  '& .MuiTableCell-root': {
    borderBottom: 'none',
    height: '48px',
    '&:nth-of-type(2), &:nth-of-type(3)': {
      width: '200px',
    },
  },
});

const SemiBold = styled('span')({
  fontWeight: '600',
});

const TOTAL_LAAJUUS_MAX = 35;

const KokonaislaajuusRow = ({
  ytot,
}: {
  ytot: Array<{ laajuus?: number }>;
}) => {
  const { t } = useTranslations();
  const totalLaajuus = sumBy(ytot, (yto) => yto.laajuus ?? 0);
  return (
    <TableBody>
      <TableRow>
        <TableCell></TableCell>
        <TableCell>
          {t('oppija.lyhenne-yhteensa')}{' '}
          <SemiBold>
            {totalLaajuus} / {TOTAL_LAAJUUS_MAX}{' '}
            {t('oppija.lyhenne-osaamispiste')}
          </SemiBold>
        </TableCell>
        <TableCell></TableCell>
      </TableRow>
    </TableBody>
  );
};

export function YhteisetTutkinnonOsatTable({
  suoritus,
}: {
  suoritus: AmmatillinenSuoritus;
}) {
  const { t, translateKielistetty } = useTranslations();

  return (
    'ytot' in suoritus &&
    !isEmpty(suoritus.ytot) && (
      <>
        <StyledOsatTable>
          <TableHead>
            <TableRow>
              <TableCell>{t('oppija.yhteiset-tutkinnon-osat')}</TableCell>
              <TableCell>
                {t('oppija.laajuus-yksikolla', {
                  unit: t('oppija.lyhenne-osaamispiste'),
                })}
              </TableCell>
              <TableCell>{t('oppija.arvosana')}</TableCell>
            </TableRow>
          </TableHead>
          {suoritus.ytot.map((tutkinnonOsa) => {
            const vahvistusPvmId = `vahvistuspvm-${tutkinnonOsa.tunniste}`;
            return (
              <AccordionTableItem
                key={tutkinnonOsa.tunniste}
                title={translateKielistetty(tutkinnonOsa.nimi)}
                headingCells={[
                  <TableCell key="laajuus">{tutkinnonOsa.laajuus}</TableCell>,
                  <TableCell key="arvosana">
                    {translateKielistetty(tutkinnonOsa.arvosana)}
                  </TableCell>,
                ]}
              >
                <Box sx={{ marginLeft: 2 }}>
                  <Stack direction="row" spacing={2} sx={{ marginBottom: 2 }}>
                    <OphTypography variant="label" id={vahvistusPvmId}>
                      {t('oppija.vahvistuspvm')}
                    </OphTypography>
                    <OphTypography aria-labelledby={vahvistusPvmId}>
                      {tutkinnonOsa.vahvistuspaiva
                        ? formatDate(tutkinnonOsa.vahvistuspaiva, 'd.M.y')
                        : '-'}
                    </OphTypography>
                  </Stack>
                  <OsaAlueetTable osaAlueet={tutkinnonOsa.osaAlueet} />
                </Box>
              </AccordionTableItem>
            );
          })}
          <KokonaislaajuusRow ytot={suoritus.ytot} />
        </StyledOsatTable>
      </>
    )
  );
}
