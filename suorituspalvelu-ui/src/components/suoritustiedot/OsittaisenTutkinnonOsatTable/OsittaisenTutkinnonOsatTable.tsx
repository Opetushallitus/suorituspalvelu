import type { AmmatillinenTutkinnonOsa } from '@/types/ui-types';
import { useTranslations } from '@/hooks/useTranslations';
import { Table, TableCell, TableHead, TableRow } from '@mui/material';
import { isEmpty } from 'remeda';
import { TableRowAccordion } from '@/components/TableRowAccordion';
import { DEFAULT_BOX_BORDER, styled } from '@/lib/theme';
import { isKielistetty } from '@/lib/translation-utils';
import { OsittaisenOsaAlueetTable } from './OsittaisenOsaAlueetTable';

const FIXED_COLUMN_WIDTH_PX = 150;

const StyledOsatTable = styled(Table)({
  tableLayout: 'fixed',
  '.MuiTableCell-root': {
    border: 'none',
    '&:nth-of-type(1)': {
      width: `calc(100% - ${FIXED_COLUMN_WIDTH_PX * 3}px)`,
    },
    '&:nth-of-type(2), &:nth-of-type(3), &:nth-of-type(4)': {
      width: `${FIXED_COLUMN_WIDTH_PX}px`,
    },
  },
  '& > tbody, & > thead': {
    borderBottom: DEFAULT_BOX_BORDER,
  },
});

export function OsittaisenTutkinnonOsatTable({
  tutkinnonOsat,
  title,
  testId,
}: {
  title: string;
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
            <TableCell>{t('oppija.korotettu-arvosana')}</TableCell>
            <TableCell>{t('oppija.korotus')}</TableCell>
          </TableRow>
        </TableHead>
        {tutkinnonOsat.map((tutkinnonOsa) => {
          const arvosana = tutkinnonOsa.arvosana;
          const arvosanaContent = isKielistetty(arvosana)
            ? translateKielistetty(arvosana)
            : arvosana;
          return (
            <TableRowAccordion
              key={tutkinnonOsa.tunniste}
              title={translateKielistetty(tutkinnonOsa.nimi)}
              otherCells={[
                <TableCell key="laajuus">
                  {tutkinnonOsa.korotettu ? tutkinnonOsa.laajuus : undefined}
                </TableCell>,
                <TableCell key="korotettu-arvosana">
                  {tutkinnonOsa.korotettu ? arvosanaContent : undefined}
                </TableCell>,
                <TableCell key="korotus">
                  {tutkinnonOsa.korotettu
                    ? t(
                        `oppija.korotus-${tutkinnonOsa.korotettu.toLowerCase()}`,
                      )
                    : undefined}
                </TableCell>,
              ]}
            >
              {!isEmpty(tutkinnonOsa.osaAlueet) && (
                <OsittaisenOsaAlueetTable osaAlueet={tutkinnonOsa.osaAlueet} />
              )}
            </TableRowAccordion>
          );
        })}
      </StyledOsatTable>
    )
  );
}
