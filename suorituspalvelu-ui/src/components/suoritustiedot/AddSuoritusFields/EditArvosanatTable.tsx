import { StripedTable } from '@/components/StripedTable';
import { useTranslations } from '@/hooks/useTranslations';
import type { Suoritusvaihtoehdot } from '@/types/ui-types';
import { TableBody, TableCell, TableHead, TableRow } from '@mui/material';
import { EditArvosanaRow, type SelectOption } from './EditArvosanaRow';

export const EditArvosanatTable = ({
  suoritusvaihtoehdot,
}: {
  suoritusvaihtoehdot: Suoritusvaihtoehdot;
}) => {
  const { t, translateKielistetty } = useTranslations();

  const { oppiaineet, aidinkielenOppimaarat, vieraatKielet } =
    suoritusvaihtoehdot;
  return (
    <StripedTable
      sx={{
        '& .MuiTableCell-root': {
          paddingY: 1,
        },
      }}
    >
      <TableHead>
        <TableRow>
          <TableCell>{t('oppija.oppiaine')}</TableCell>
          <TableCell>{t('oppija.lisatieto-kieli')}</TableCell>
          <TableCell>{t('oppija.arvosana')}</TableCell>
          <TableCell>{t('oppija.valinnainen')}</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {oppiaineet.map((oppiaine) => {
          let lisatietoOptions: Array<SelectOption> | undefined = undefined;
          if (oppiaine.isAidinkieli) {
            lisatietoOptions = aidinkielenOppimaarat.map((am) => ({
              label: translateKielistetty(am.nimi),
              value: am.arvo,
            }));
          } else if (oppiaine.isKieli) {
            lisatietoOptions = vieraatKielet.map((vk) => ({
              label: translateKielistetty(vk.nimi),
              value: vk.arvo,
            }));
          }

          return (
            <EditArvosanaRow
              key={oppiaine.arvo}
              name={oppiaine.arvo}
              title={translateKielistetty(oppiaine.nimi)}
              lisatietoOptions={lisatietoOptions}
            />
          );
        })}
      </TableBody>
    </StripedTable>
  );
};
