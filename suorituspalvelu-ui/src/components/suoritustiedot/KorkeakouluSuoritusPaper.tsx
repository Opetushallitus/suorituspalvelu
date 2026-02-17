import { ophColors } from '@opetushallitus/oph-design-system';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import type { KorkeakouluSuoritus } from '@/types/ui-types';
import { useTranslations } from '@/hooks/useTranslations';
import { isEmptyish } from 'remeda';
import { SimpleAccordion } from '../SimpleAccordion';
import { Table, TableCell, TableHead, TableRow } from '@mui/material';
import { isEmpty } from 'remeda';
import { TableRowAccordion } from '@/components/TableRowAccordion';
import { DEFAULT_BOX_BORDER, styled } from '@/lib/theme';
import { isKielistetty } from '@/lib/translation-utils';

const FIXED_COLUMN_WIDTH_PX = 150;

const StyledOpintosuorituksetTable = styled(Table)(({ theme }) => ({
  tableLayout: 'fixed',
  '& .MuiTableCell-root': {
    border: 'none',
    width: `${FIXED_COLUMN_WIDTH_PX}px`,
    padding: theme.spacing(1.5, 2),
    '&:nth-of-type(1)': {
      width: `calc(100% - ${FIXED_COLUMN_WIDTH_PX * 2}px)`,
    },
  },
  '& > thead > tr': {
    '& .MuiTableCell-root': {
      backgroundColor: ophColors.white,
      borderBottom: DEFAULT_BOX_BORDER,
      whiteSpace: 'nowrap',
    },
  },
  '& > tbody': {
    '& > tr:not(.table-row-accordion-content)': {
      border: 'none',
      '&:hover': {
        backgroundColor: ophColors.lightBlue2,
      },
    },
    '&:nth-of-type(even)': {
      backgroundColor: ophColors.grey50,
    },
    '&:nth-of-type(odd)': {
      backgroundColor: ophColors.white,
    },
  },
}));

export function OpintosuorituksetTable({
  opintosuoritukset,
  level = 0,
}: {
  opintosuoritukset: KorkeakouluSuoritus['opintojaksot'];
  level?: number;
}) {
  const { t, translateKielistetty } = useTranslations();

  return (
    !isEmpty(opintosuoritukset) && (
      <StyledOpintosuorituksetTable sx={{}}>
        <TableHead>
          <TableRow>
            <TableCell>{t('oppija.opintojakso')}</TableCell>
            <TableCell>
              {t('oppija.laajuus-yksikolla', {
                unit: t('oppija.lyhenne-opintopiste'),
              })}
            </TableCell>
            <TableCell>{t('oppija.arvosana')}</TableCell>
          </TableRow>
        </TableHead>
        {opintosuoritukset.map((opintosuoritus) => {
          const arvosana = opintosuoritus.arvosana;
          return (
            <TableRowAccordion
              key={opintosuoritus.tunniste}
              title={translateKielistetty(opintosuoritus.nimi)}
              otherCells={[
                <TableCell key="laajuus">{opintosuoritus.laajuus}</TableCell>,
                <TableCell key="arvosana">
                  {isKielistetty(arvosana)
                    ? translateKielistetty(arvosana)
                    : arvosana}
                </TableCell>,
              ]}
            >
              {!isEmpty(opintosuoritus.opintojaksot) && (
                <OpintosuorituksetTable
                  opintosuoritukset={opintosuoritus.opintojaksot}
                  level={level + 1}
                />
              )}
            </TableRowAccordion>
          );
        })}
      </StyledOpintosuorituksetTable>
    )
  );
}

export const KorkeakouluSuoritusPaper = ({
  suoritus,
}: {
  suoritus?: KorkeakouluSuoritus | undefined;
}) => {
  const { t } = useTranslations();
  return (
    suoritus && (
      <SuoritusInfoPaper suoritus={suoritus} topColor={ophColors.red1}>
        <SuorituksenPerustiedotIndicator perustiedot={suoritus} />
        {!isEmptyish(suoritus?.opintojaksot) && (
          <SimpleAccordion
            titleClosed={t('oppija.nayta-opintosuoritukset')}
            titleOpen={t('oppija.piilota-opintosuoritukset')}
          >
            <OpintosuorituksetTable opintosuoritukset={suoritus.opintojaksot} />
          </SimpleAccordion>
        )}
      </SuoritusInfoPaper>
    )
  );
};
