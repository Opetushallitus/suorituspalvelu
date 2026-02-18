import { StripedTable } from '@/components/StripedTable';
import { useTranslations } from '@/hooks/useTranslations';
import type { TutkinnonOsanOsaAlue } from '@/types/ui-types';
import { TableBody, TableCell, TableHead, TableRow } from '@mui/material';

export const OsaAlueetTable = ({
  osaAlueet,
}: {
  osaAlueet: Array<TutkinnonOsanOsaAlue>;
}) => {
  const { t, translateKielistetty } = useTranslations();
  return (
    <StripedTable>
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
