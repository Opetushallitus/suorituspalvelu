import type { AmmatillinenTutkinnonOsa } from '@/types/ui-types';
import { useTranslations } from '@/hooks/useTranslations';
import { Table, TableCell, TableHead, TableRow } from '@mui/material';
import { isEmpty } from 'remeda';
import { TableRowAccordion } from '@/components/TableRowAccordion';
import { DEFAULT_BOX_BORDER, styled } from '@/lib/theme';
import { isKielistetty } from '@/lib/translation-utils';
import { KokonaislaajuusRow } from './KokonaislaajuusRow';
import { OsaAlueetTable } from './OsaAlueetTable';

const FIXED_COLUMN_WIDTH_PX = 150;

const StyledOsatTable = styled(Table)({
  tableLayout: 'fixed',
  '.MuiTableCell-root': {
    border: 'none',
    '&:nth-of-type(1)': {
      width: `calc(100% - ${FIXED_COLUMN_WIDTH_PX * 2}px)`,
    },
    '&:nth-of-type(2), &:nth-of-type(3)': {
      width: `${FIXED_COLUMN_WIDTH_PX}px`,
    },
  },
  '& > tbody, & > thead': {
    borderBottom: DEFAULT_BOX_BORDER,
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
