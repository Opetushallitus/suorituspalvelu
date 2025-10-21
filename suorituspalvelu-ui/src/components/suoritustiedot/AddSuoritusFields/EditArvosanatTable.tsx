import { StripedTable } from '@/components/StripedTable';
import { useTranslations } from '@/hooks/useTranslations';
import type { SuoritusFields, Suoritusvaihtoehdot } from '@/types/ui-types';
import { TableBody, TableCell, TableHead, TableRow } from '@mui/material';
import { EditOppiaineRow, type SelectOption } from './EditOppiaineRow';

export const EditArvosanatTable = ({
  suoritus,
  suoritusvaihtoehdot,
  setSuoritus,
}: {
  suoritus: SuoritusFields;
  suoritusvaihtoehdot: Suoritusvaihtoehdot;
  setSuoritus: React.Dispatch<React.SetStateAction<SuoritusFields | null>>;
}) => {
  const { t, translateKielistetty } = useTranslations();

  const { oppiaineet, aidinkielenOppimaarat, vieraatKielet, arvosanat } =
    suoritusvaihtoehdot;

  const aidinKieliOptions = aidinkielenOppimaarat.map((am) => ({
    label: translateKielistetty(am.nimi),
    value: am.arvo,
  }));

  const vierasKieliOptions = vieraatKielet.map((vk) => ({
    label: translateKielistetty(vk.nimi),
    value: vk.arvo,
  }));

  const arvosanaOptions: Array<SelectOption> = arvosanat.map((a) => ({
    label: a.arvo?.toString() ?? '',
    value: a.arvo?.toString() ?? '',
  }));

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
          let kieliOptions: Array<SelectOption> | undefined = undefined;
          if (oppiaine.isAidinkieli) {
            kieliOptions = aidinKieliOptions;
          } else if (oppiaine.isKieli) {
            kieliOptions = vierasKieliOptions;
          }

          return (
            <EditOppiaineRow
              key={oppiaine.arvo}
              value={
                suoritus.oppiaineet.find(
                  (oa) => oa.koodi === oppiaine.arvo,
                ) ?? { koodi: oppiaine.arvo, arvosana: '' }
              }
              onChange={(changedOppiaine) => {
                setSuoritus((previousSuoritus) => {
                  if (previousSuoritus) {
                    const newOppiaineet = previousSuoritus.oppiaineet ?? [];
                    const existingOppiaineIndex = newOppiaineet.findIndex(
                      (oa) => oa.koodi === changedOppiaine.koodi,
                    );
                    if (existingOppiaineIndex === -1) {
                      newOppiaineet.push(changedOppiaine);
                    } else {
                      newOppiaineet[existingOppiaineIndex] = changedOppiaine;
                    }
                    return {
                      ...previousSuoritus,
                      oppiaineet: newOppiaineet,
                    };
                  }
                  return previousSuoritus;
                });
              }}
              arvosanaOptions={arvosanaOptions}
              kieliOptions={kieliOptions}
              title={translateKielistetty(oppiaine.nimi)}
            />
          );
        })}
      </TableBody>
    </StripedTable>
  );
};
