import {
  EBOppiaine,
  IBOppiaine,
  LukionOppiaine,
  LukioSuoritus,
  YOKoe,
} from '@/types/ui-types';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { ophColors } from '@opetushallitus/oph-design-system';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { useTranslate } from '@tolgee/react';
import { LabeledInfoItem } from './LabeledInfoItem';
import { DEFAULT_BOX_BORDER, styled } from '@/lib/theme';
import { ListTable, ListTableColumn, TableHeaderCell } from './ListTable';
import { formatDate } from 'date-fns';
import { useMemo } from 'react';
import { TableBody, TableCell, TableHead, TableRow } from '@mui/material';
import { StripedTable } from './StripedTable';

const UnorderedList = styled('ul')(({ theme }) => ({
  margin: 0,
  paddingLeft: theme.spacing(2),
}));

const LukionOppiaineetList = ({
  oppiaineet,
}: {
  oppiaineet: Array<LukionOppiaine>;
}) => {
  const { t } = useTranslate();
  return (
    <LabeledInfoItem
      label={t('oppija.oppiaineet')}
      value={
        <UnorderedList>
          {oppiaineet.map((oppiaine) => (
            <li key={oppiaine.nimi}>{oppiaine.nimi}</li>
          ))}
        </UnorderedList>
      }
    />
  );
};

const IndentedCell = styled(TableCell)(({ theme }) => ({
  textIndent: theme.spacing(1),
}));

const EBOppiaineetTable = ({
  oppiaineet,
}: {
  oppiaineet: Array<EBOppiaine>;
}) => {
  const { t } = useTranslate();

  return (
    <StripedTable stripeTarget="body">
      <TableHead>
        <TableRow sx={{ borderBottom: DEFAULT_BOX_BORDER }}>
          <TableHeaderCell key="nimi" title={t('oppija.oppiaine')} />
          <TableHeaderCell
            key="suorituskieli"
            title={t('oppija.suorituskieli')}
          />
          <TableHeaderCell key="laajuus" title={t('oppija.laajuus-vvt')} />
          <TableHeaderCell key="arvosana" title={t('oppija.arvosana')} />
        </TableRow>
      </TableHead>
      {oppiaineet.map((row) => (
        <TableBody key={row.nimi}>
          <TableRow>
            <TableCell>{row.nimi}</TableCell>
            <TableCell>{row.suorituskieli}</TableCell>
            <TableCell>{row.laajuus}</TableCell>
            <TableCell />
          </TableRow>
          <TableRow>
            <IndentedCell colSpan={3}>{t('oppija.eb-written')}</IndentedCell>
            <TableCell>{row.written?.arvosana}</TableCell>
          </TableRow>
          <TableRow>
            <IndentedCell colSpan={3}>{t('oppija.eb-oral')}</IndentedCell>
            <TableCell>{row.oral?.arvosana}</TableCell>
          </TableRow>
          <TableRow>
            <IndentedCell colSpan={3}>{t('oppija.eb-final')}</IndentedCell>
            <TableCell>{row.final?.arvosana}</TableCell>
          </TableRow>
        </TableBody>
      ))}
    </StripedTable>
  );
};

const IBOppiaineetTable = ({
  oppiaineet,
}: {
  oppiaineet: Array<IBOppiaine>;
}) => {
  const { t } = useTranslate();

  return (
    <StripedTable stripeTarget="row">
      <TableHead>
        <TableRow sx={{ borderBottom: DEFAULT_BOX_BORDER }}>
          <TableHeaderCell title={t('oppija.oppiaine')} />
          <TableHeaderCell title={t('oppija.laajuus-vvt')} />
          <TableHeaderCell title={t('oppija.predicted-grade')} />
          <TableHeaderCell title={t('oppija.arvosana')} />
        </TableRow>
      </TableHead>
      {oppiaineet.map((oppiaine) => (
        <TableBody key={oppiaine.nimi}>
          <TableRow>
            <TableCell colSpan={4}>{oppiaine.nimi}</TableCell>
          </TableRow>
          {oppiaine.suoritukset.map((suoritus) => (
            <TableRow key={suoritus.nimi}>
              <IndentedCell>{suoritus.nimi}</IndentedCell>
              <TableCell>{suoritus.laajuus}</TableCell>
              <TableCell>{suoritus.predictedGrade}</TableCell>
              <TableCell>{suoritus.arvosana}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      ))}
    </StripedTable>
  );
};

function LukionOppiaineet({ suoritus }: { suoritus: LukioSuoritus }) {
  if ('oppiaineet' in suoritus) {
    switch (suoritus.koulutustyyppi) {
      case 'eb':
        return <EBOppiaineetTable oppiaineet={suoritus.oppiaineet} />;
      case 'ib':
        return <IBOppiaineetTable oppiaineet={suoritus.oppiaineet} />;
      default:
        return <LukionOppiaineetList oppiaineet={suoritus.oppiaineet} />;
    }
  }
}

const YoKokeetTable = ({ yoKokeet }: { yoKokeet: Array<YOKoe> }) => {
  const { t } = useTranslate();

  const cols: Array<ListTableColumn<YOKoe>> = useMemo(
    () => [
      {
        key: 'aine',
        title: t('oppija.yo-kokeen-aine'),
        render: (row) => row.aine,
      },
      {
        key: 'taso',
        title: t('oppija.yo-kokeen-taso'),
        render: (row) => row.taso,
      },
      {
        key: 'arvosana',
        title: t('oppija.arvosana'),
        render: (row) => row.arvosana,
      },
      {
        key: 'yhteispistemaara',
        title: t('oppija.yo-kokeen-yhteispistemaara'),
        render: (row) => row.yhteispistemaara.toString(),
      },
      {
        key: 'tutkintokerta',
        title: t('oppija.yo-kokeen-tutkintokerta'),
        render: (row) => formatDate(row.tutkintokerta, 'd.M.y'),
      },
    ],
    [t],
  );

  return <ListTable columns={cols} rows={yoKokeet} rowKeyProp="aine" />;
};

export const LukioSuoritusPaper = ({
  suoritus,
}: {
  suoritus: LukioSuoritus;
}) => {
  return (
    <SuoritusInfoPaper
      suorituksenNimi={suoritus.nimi}
      valmistumispaiva={suoritus.valmistumispaiva}
      topColor={ophColors.blue2}
    >
      <SuorituksenPerustiedotIndicator perustiedot={suoritus} />
      <LukionOppiaineet suoritus={suoritus} />
      {'yoKokeet' in suoritus && <YoKokeetTable yoKokeet={suoritus.yoKokeet} />}
    </SuoritusInfoPaper>
  );
};
