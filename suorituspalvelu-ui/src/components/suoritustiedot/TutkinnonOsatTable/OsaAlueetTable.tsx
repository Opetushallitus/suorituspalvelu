import { StripedTable } from '@/components/StripedTable';
import { useTranslations } from '@/hooks/useTranslations';
import type { TutkinnonOsanOsaAlue } from '@/types/ui-types';
import {
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  useTheme,
} from '@mui/material';

export const FIXED_COLUMN_WIDTH = '190px';

export const OsaAlueetTable = ({
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
            <TableRow key={`${nimi}-${osaAlue.laajuus}`}>
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
