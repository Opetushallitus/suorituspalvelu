import { StripedTable } from '@/components/StripedTable';
import { useTranslations } from '@/hooks/useTranslations';
import type { TutkinnonOsanOsaAlue } from '@/types/ui-types';
import { TableBody, TableCell, TableHead, TableRow } from '@mui/material';
import { isKielistetty } from '@/lib/translation-utils';

export const OsittaisenOsaAlueetTable = ({
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
          <TableCell>{t('oppija.korotettu-arvosana')}</TableCell>
          <TableCell>{t('oppija.korotus')}</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {osaAlueet.map((osaAlue) => {
          const nimi = translateKielistetty(osaAlue.nimi);
          return (
            <TableRow key={`${nimi}-${osaAlue.laajuus}`}>
              <TableCell>{nimi}</TableCell>
              <TableCell>{osaAlue.laajuus}</TableCell>
              <TableCell>
                {osaAlue.korotettu
                  ? isKielistetty(osaAlue.arvosana)
                    ? translateKielistetty(osaAlue.arvosana)
                    : osaAlue.arvosana
                  : undefined}
              </TableCell>
              <TableCell>
                {osaAlue.korotettu
                  ? t(`oppija.korotus-${osaAlue.korotettu.toLowerCase()}`)
                  : undefined}
              </TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    </StripedTable>
  );
};
