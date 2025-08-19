import { OphTypography } from '@opetushallitus/oph-design-system';
import {
  AmmatillinenTutkinnonOsa,
  TutkinnonOsanOsaAlue,
} from '@/types/ui-types';
import { useTranslations } from '@/hooks/useTranslations';
import {
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  useTheme,
} from '@mui/material';
import { isEmpty, sumBy } from 'remeda';
import { TableBodyAccordion } from './TableBodyAccordion';
import { StripedTable } from './StripedTable';
import { styled } from '@/lib/theme';
import { isKielistetty } from '@/lib/translation-utils';
import { formatFinnishDate } from '@/lib/common';

const FIXED_COLUMN_WIDTH = '190px';

const OsaAlueetTable = ({
  osaAlueet,
}: {
  osaAlueet: Array<TutkinnonOsanOsaAlue>;
}) => {
  const { t, translateKielistetty } = useTranslations();
  const theme = useTheme();
  return (
    <StripedTable
      sx={{
        marginTop: 2,
        tableLayout: 'fixed',
        '& .MuiTableCell-root': {
          '&:nth-of-type(2)': {
            width: FIXED_COLUMN_WIDTH,
          },
          '&:nth-of-type(3)': {
            // Osa-alueet on toisen taulukon sisällä, jossa on paddingia, mutta laajuus ja arvosana
            // -sarakkeet halutaan silti ulomman taulukon sarakkeeiden kanssa samaan linjaan
            width: `calc(${FIXED_COLUMN_WIDTH} - ${theme.spacing(2.5)})`,
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
  );
};

const StyledOsatTable = styled(Table)({
  // Tarvitaan table-elementille luokka, jotta tyylit ei valu sisäkkäiseen osa-alueiden taulukkoon
  '&.tutkinnon-osat-table': {
    tableLayout: 'fixed',
    '& .MuiTableCell-root': {
      borderBottom: 'none',
      height: '48px',
      '&:nth-of-type(2), &:nth-of-type(3)': {
        width: FIXED_COLUMN_WIDTH,
      },
    },
  },
});

const SemiBold = styled('span')({
  fontWeight: '600',
});

const KokonaislaajuusRow = ({
  osat,
  maxKokonaislaajuus,
}: {
  osat: Array<{ laajuus?: number }>;
  maxKokonaislaajuus: number;
}) => {
  const { t } = useTranslations();
  const totalLaajuus = sumBy(osat, (osa) => osa.laajuus ?? 0);
  return (
    <TableBody>
      <TableRow>
        <TableCell></TableCell>
        <TableCell>
          {t('oppija.lyhenne-yhteensa')}{' '}
          <SemiBold>
            {totalLaajuus} / {maxKokonaislaajuus}{' '}
            {t('oppija.lyhenne-osaamispiste')}
          </SemiBold>
        </TableCell>
        <TableCell></TableCell>
      </TableRow>
    </TableBody>
  );
};

export function TutkinnonOsatTable({
  tutkinnonOsat,
  maxKokonaislaajuus,
  title,
  testId,
}: {
  title: string;
  maxKokonaislaajuus: number;
  tutkinnonOsat: Array<AmmatillinenTutkinnonOsa>;
  testId?: string;
}) {
  const { t, translateKielistetty } = useTranslations();

  return (
    !isEmpty(tutkinnonOsat) && (
      <StyledOsatTable className="tutkinnon-osat-table" data-test-id={testId}>
        <TableHead>
          <TableRow>
            <TableCell>{title}</TableCell>
            <TableCell>
              {t('oppija.laajuus-yksikolla', {
                unit: t('oppija.lyhenne-osaamispiste'),
              })}
            </TableCell>
            <TableCell>{t('oppija.arvosana')}</TableCell>
          </TableRow>
        </TableHead>
        {tutkinnonOsat.map((tutkinnonOsa) => {
          const vahvistusPvmId = `vahvistuspvm-${tutkinnonOsa.tunniste}`;
          const arvosana = tutkinnonOsa.arvosana;
          return (
            <TableBodyAccordion
              key={tutkinnonOsa.tunniste}
              contentCellStyle={{ paddingLeft: '46px' }}
              title={translateKielistetty(tutkinnonOsa.nimi)}
              otherCells={[
                <TableCell key="laajuus">{tutkinnonOsa.laajuus}</TableCell>,
                <TableCell key="arvosana">
                  {isKielistetty(arvosana)
                    ? translateKielistetty(arvosana)
                    : arvosana}
                </TableCell>,
              ]}
            >
              {'vahvistuspaiva' in tutkinnonOsa &&
                tutkinnonOsa.vahvistuspaiva && (
                  <Stack direction="row" spacing={2} sx={{ marginBottom: 1 }}>
                    <OphTypography variant="label" id={vahvistusPvmId}>
                      {t('oppija.vahvistuspvm')}
                    </OphTypography>
                    <OphTypography aria-labelledby={vahvistusPvmId}>
                      {formatFinnishDate(tutkinnonOsa.vahvistuspaiva)}
                    </OphTypography>
                  </Stack>
                )}
              {!isEmpty(tutkinnonOsa.osaAlueet) && (
                <OsaAlueetTable osaAlueet={tutkinnonOsa.osaAlueet} />
              )}
            </TableBodyAccordion>
          );
        })}
        <KokonaislaajuusRow
          osat={tutkinnonOsat}
          maxKokonaislaajuus={maxKokonaislaajuus}
        />
      </StyledOsatTable>
    )
  );
}
