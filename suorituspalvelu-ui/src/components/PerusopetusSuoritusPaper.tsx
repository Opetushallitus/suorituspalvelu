import {
  type Kielistetty,
  type PerusopetuksenOppiaine,
  type PerusopetusSuoritus,
} from '@/types/ui-types';
import { ophColors } from '@opetushallitus/oph-design-system';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { TableBody, TableCell, TableHead, TableRow } from '@mui/material';
import { LabeledInfoItem } from './LabeledInfoItem';
import { useTranslations } from '@/hooks/useTranslations';
import { useMemo } from 'react';
import { StripedTable } from './StripedTable';
import { isKielistetty } from '@/lib/translation-utils';
import { InfoItemRow } from './InfoItemRow';
import { isBoolean } from 'remeda';

const Luokkatiedot = ({
  oppimaara,
}: {
  oppimaara: {
    koulusivistyskieli?: string;
    luokka?: string;
    yksilollistetty?: boolean;
  };
}) => {
  const { t } = useTranslations();
  return (
    <InfoItemRow slotAmount={4}>
      {oppimaara.koulusivistyskieli && (
        <LabeledInfoItem
          label={t('oppija.koulusivistyskieli')}
          value={oppimaara.koulusivistyskieli}
        />
      )}
      <LabeledInfoItem label={t('oppija.luokka')} value={oppimaara.luokka} />
      <LabeledInfoItem
        label={t('oppija.yksilollistetty')}
        value={oppimaara.yksilollistetty ? t('kylla') : t('ei')}
      />
    </InfoItemRow>
  );
};

const OppiaineValue = ({
  value,
}: {
  value: Kielistetty | string | number | boolean | undefined;
}) => {
  const { translateKielistetty } = useTranslations();
  switch (true) {
    case isKielistetty(value):
      return translateKielistetty(value);
    case isBoolean(value):
      return value ? 'x' : '';
    default:
      return value;
  }
};

const PerusopetusOppiaineetTable = ({
  oppiaineet,
}: {
  oppiaineet: Array<PerusopetuksenOppiaine>;
}) => {
  const { t } = useTranslations();

  const hasArvosana = oppiaineet.some((oppiaine) => oppiaine.arvosana);
  const hasValinnainen = oppiaineet.some((oppiaine) => oppiaine?.valinnainen);

  const columns = useMemo(() => {
    const cols: Array<{
      key: keyof (typeof oppiaineet)[number];
      title: string;
      style?: React.CSSProperties;
    }> = [
      {
        key: 'nimi',
        title: t('oppija.oppiaine'),
      },
    ];

    if (hasArvosana) {
      cols.push({
        key: 'arvosana',
        title: t('oppija.arvosana'),
        style: { textAlign: 'center' },
      });
    }

    if (hasValinnainen) {
      cols.push({
        key: 'valinnainen',
        title: t('oppija.valinnainen'),
        style: { textAlign: 'center' },
      });
    }

    return cols;
  }, [hasArvosana, hasValinnainen, t]);

  return (
    <StripedTable>
      <TableHead>
        <TableRow>
          {columns.map((column) => (
            <TableCell key={column.key} style={column.style}>
              {column.title}
            </TableCell>
          ))}
        </TableRow>
      </TableHead>
      <TableBody>
        {oppiaineet.map((oppiaine) => (
          <TableRow key={oppiaine.tunniste}>
            {columns.map((column) => {
              const value = oppiaine[column.key];
              return (
                <TableCell key={column.key} style={column.style}>
                  <OppiaineValue value={value} />
                </TableCell>
              );
            })}
          </TableRow>
        ))}
      </TableBody>
    </StripedTable>
  );
};

export const PerusopetusSuoritusPaper = ({
  suoritus,
}: {
  suoritus: PerusopetusSuoritus;
}) => {
  return (
    <SuoritusInfoPaper suoritus={suoritus} topColor={ophColors.cyan2}>
      <SuorituksenPerustiedotIndicator perustiedot={suoritus} />
      {'luokka' in suoritus && <Luokkatiedot oppimaara={suoritus} />}
      {'oppiaineet' in suoritus && (
        <PerusopetusOppiaineetTable oppiaineet={suoritus.oppiaineet} />
      )}
    </SuoritusInfoPaper>
  );
};
