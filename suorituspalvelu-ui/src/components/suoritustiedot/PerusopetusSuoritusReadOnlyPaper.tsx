import {
  type Kielistetty,
  type PerusopetuksenOppiaine,
  type PerusopetusSuoritus,
  type Yksilollistaminen,
} from '@/types/ui-types';
import { ophColors } from '@opetushallitus/oph-design-system';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import {
  Stack,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
  useTheme,
} from '@mui/material';
import { LabeledInfoItem } from '@/components/LabeledInfoItem';
import { useTranslations } from '@/hooks/useTranslations';
import { useMemo } from 'react';
import { StripedTable } from '../StripedTable';
import { isKielistetty } from '@/lib/translation-utils';
import { InfoItemRow } from '@/components/InfoItemRow';
import { isEmpty } from 'remeda';

const Luokkatiedot = ({
  oppimaara,
}: {
  oppimaara: {
    koulusivistyskieli?: string;
    luokka?: string;
    yksilollistaminen?: Yksilollistaminen;
    yksilollistetty?: boolean;
  };
}) => {
  const { t, translateKielistetty } = useTranslations();
  return (
    <InfoItemRow slotAmount={4}>
      {oppimaara.koulusivistyskieli && (
        <LabeledInfoItem
          label={t('oppija.koulusivistyskieli')}
          value={t(`kieli.${oppimaara.koulusivistyskieli}`)}
        />
      )}
      {oppimaara.luokka && (
        <LabeledInfoItem label={t('oppija.luokka')} value={oppimaara.luokka} />
      )}
      {/* TODO: Poista tämä kun 7,8-luokkalaiset käyttää yksilollistaminen-kenttää */}
      {oppimaara.yksilollistetty && (
        <LabeledInfoItem
          label={t('oppija.yksilollistetty')}
          value={oppimaara.yksilollistetty ? t('kylla') : t('ei')}
        />
      )}
      {oppimaara.yksilollistaminen && (
        <LabeledInfoItem
          label={t('oppija.yksilollistetty')}
          value={translateKielistetty(oppimaara.yksilollistaminen.nimi)}
        />
      )}
    </InfoItemRow>
  );
};

const OppiaineValue = ({
  value,
}: {
  value: Kielistetty | string | Array<string> | number | undefined;
}) => {
  const { translateKielistetty } = useTranslations();
  switch (true) {
    case isKielistetty(value):
      return translateKielistetty(value);
    case Array.isArray(value):
      return (
        <Stack spacing={1}>
          {value.map((v, i) => (
            // eslint-disable-next-line @eslint-react/no-array-index-key
            <Typography key={i} sx={{ lineHeight: '16px' }}>
              {v}
            </Typography>
          ))}
        </Stack>
      );
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
  const hasValinnainen = oppiaineet.some(
    (oppiaine) => oppiaine?.valinnaisetArvosanat,
  );

  const theme = useTheme();

  const columnStyles: React.CSSProperties = {
    verticalAlign: 'top',
    paddingTop: theme.spacing(1.5),
    paddingBottom: theme.spacing(1.5),
    lineHeight: '16px',
  };

  const columns = useMemo(() => {
    const cols: Array<{
      key: keyof (typeof oppiaineet)[number];
      title: string;
      style: React.CSSProperties;
    }> = [
      {
        key: 'nimi',
        title: t('oppija.oppiaine'),
        style: columnStyles,
      },
    ];

    if (hasArvosana) {
      cols.push({
        key: 'arvosana',
        title: t('oppija.arvosana'),
        style: { textAlign: 'center', ...columnStyles },
      });
    }

    if (hasValinnainen) {
      cols.push({
        key: 'valinnaisetArvosanat',
        title: t('oppija.valinnainen'),
        style: { textAlign: 'center', ...columnStyles },
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

export const PerusopetusSuoritusReadOnlyPaper = ({
  suoritus,
  actions,
}: {
  suoritus: PerusopetusSuoritus;
  actions?: React.ReactNode;
}) => {
  return (
    <SuoritusInfoPaper suoritus={suoritus} topColor={ophColors.cyan2}>
      <SuorituksenPerustiedotIndicator perustiedot={suoritus} />
      {'luokka' in suoritus && <Luokkatiedot oppimaara={suoritus} />}
      {'oppiaineet' in suoritus && !isEmpty(suoritus.oppiaineet) && (
        <PerusopetusOppiaineetTable oppiaineet={suoritus.oppiaineet} />
      )}
      {actions}
    </SuoritusInfoPaper>
  );
};
