import { LukionOppiaine, LukioSuoritus, YOKoe } from '@/types/ui-types';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { ophColors } from '@opetushallitus/oph-design-system';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { useTranslate } from '@tolgee/react';
import { LabeledInfoItem } from './LabeledInfoItem';
import { styled } from '@/lib/theme';
import { ListTable, ListTableColumn } from './ListTable';
import { formatDate } from 'date-fns';
import { useMemo } from 'react';

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
        title: t('oppija.yo-kokeen-arvosana'),
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
      {'oppiaineet' in suoritus && (
        <LukionOppiaineetList oppiaineet={suoritus.oppiaineet} />
      )}
      {'yoKokeet' in suoritus && <YoKokeetTable yoKokeet={suoritus.yoKokeet} />}
    </SuoritusInfoPaper>
  );
};
