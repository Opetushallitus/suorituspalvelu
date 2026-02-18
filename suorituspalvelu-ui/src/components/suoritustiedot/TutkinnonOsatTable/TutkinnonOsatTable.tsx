import type { AmmatillinenTutkinnonOsa } from '@/types/ui-types';
import { useTranslations } from '@/hooks/useTranslations';
import { Table, TableCell, TableHead, TableRow } from '@mui/material';
import { isEmpty } from 'remeda';
import { TableRowAccordion } from '@/components/TableRowAccordion';
import { styled } from '@/lib/theme';
import { isKielistetty } from '@/lib/translation-utils';
import { KokonaislaajuusRow } from './KokonaislaajuusRow';
import { FIXED_COLUMN_WIDTH, OsaAlueetTable } from './OsaAlueetTable';

const StyledOsatTable = styled(Table)({
  tableLayout: 'fixed',
  '& > thead, & > tbody': {
    '& > tr > .MuiTableCell-root': {
      borderBottom: 'none',
      height: '48px',
      '&:nth-of-type(2), &:nth-of-type(3)': {
        width: FIXED_COLUMN_WIDTH,
      },
    },
  },
});

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
      <StyledOsatTable data-test-id={testId}>
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
          const arvosana = tutkinnonOsa.arvosana;
          return (
            <TableRowAccordion
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
              {!isEmpty(tutkinnonOsa.osaAlueet) && (
                <OsaAlueetTable osaAlueet={tutkinnonOsa.osaAlueet} />
              )}
            </TableRowAccordion>
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
