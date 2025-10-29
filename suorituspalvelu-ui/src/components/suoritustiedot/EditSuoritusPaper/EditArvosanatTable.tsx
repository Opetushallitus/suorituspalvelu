import { StripedTable } from '@/components/StripedTable';
import { useTranslations } from '@/hooks/useTranslations';
import type {
  PerusopetusOppiaineFields,
  SuoritusFields,
  Suoritusvaihtoehdot,
} from '@/types/ui-types';
import { TableBody, TableCell, TableHead, TableRow } from '@mui/material';
import { EditOppiaineRow, type SelectOption } from './EditOppiaineRow';

export const EditArvosanatTable = ({
  suoritus,
  suoritusvaihtoehdot,
  onOppiaineChange,
}: {
  suoritus: SuoritusFields;
  suoritusvaihtoehdot: Suoritusvaihtoehdot;
  onOppiaineChange: (changedOppiaine: PerusopetusOppiaineFields) => void;
}) => {
  const { t, translateKielistetty } = useTranslations();

  const {
    oppiaineet: syotettavatOppiaineet,
    aidinkielenOppimaarat,
    vieraatKielet,
    arvosanat,
  } = suoritusvaihtoehdot;

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
        {syotettavatOppiaineet.map((oppiaine) => {
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
              onChange={onOppiaineChange}
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
