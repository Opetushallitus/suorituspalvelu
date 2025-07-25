import { PerusopetuksenOppiaine, PerusopetusSuoritus } from '@/types/ui-types';
import { ophColors } from '@opetushallitus/oph-design-system';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { Stack } from '@mui/material';
import { LabeledInfoItem } from './LabeledInfoItem';
import { useTranslate } from '@tolgee/react';
import { useMemo } from 'react';
import { ListTable, ListTableColumn } from './ListTable';

const Luokkatiedot = ({
  oppimaara,
}: {
  oppimaara: { luokka?: string; yksilollistetty?: boolean };
}) => {
  const { t } = useTranslate();
  return (
    <Stack direction="row" sx={{ alignItems: 'center', gap: 1 }}>
      <LabeledInfoItem label={t('oppija.luokka')} value={oppimaara.luokka} />
      <LabeledInfoItem
        label={t('oppija.yksilollistetty')}
        value={oppimaara.yksilollistetty ? t('kylla') : t('ei')}
      />
    </Stack>
  );
};

const PerusopetusOppiaineetTable = ({
  oppiaineet,
}: {
  oppiaineet: Array<PerusopetuksenOppiaine>;
}) => {
  const { t } = useTranslate();

  const hasArvosana = oppiaineet.some((oppiaine) => oppiaine.arvosana);
  const hasValinnainen = oppiaineet.some((oppiaine) => oppiaine.valinnainen);

  const columns = useMemo(() => {
    const cols: Array<ListTableColumn<PerusopetuksenOppiaine>> = [
      {
        key: 'nimi',
        title: t('oppija.oppiaine'),
        render: (row) => row.nimi,
      } as ListTableColumn<PerusopetuksenOppiaine>,
    ];

    if (hasArvosana) {
      cols.push({
        key: 'arvosana',
        title: t('oppija.arvosana'),
        render: (row) => row.arvosana,
      } as ListTableColumn<PerusopetuksenOppiaine>);
    }

    if (hasValinnainen) {
      cols.push({
        key: 'valinnainen',
        title: t('oppija.valinnainen'),
        render: (row) => row.valinnainen,
      } as ListTableColumn<PerusopetuksenOppiaine>);
    }

    return cols;
  }, [hasArvosana, hasValinnainen, t]);

  return <ListTable columns={columns} rows={oppiaineet} rowKeyProp="nimi" />;
};

export const PerusopetusSuoritusPaper = ({
  nimi,
  perusopetusSuoritus,
}: {
  nimi: string;
  perusopetusSuoritus: PerusopetusSuoritus;
}) => {
  return (
    <SuoritusInfoPaper
      key={perusopetusSuoritus.oppilaitos.oid}
      suorituksenNimi={nimi}
      valmistumispaiva={perusopetusSuoritus.valmistumispaiva}
      topColor={ophColors.cyan2}
    >
      <SuorituksenPerustiedotIndicator perustiedot={perusopetusSuoritus} />
      {'luokka' in perusopetusSuoritus && (
        <Luokkatiedot oppimaara={perusopetusSuoritus} />
      )}
      {'oppiaineet' in perusopetusSuoritus && (
        <PerusopetusOppiaineetTable
          oppiaineet={perusopetusSuoritus.oppiaineet}
        />
      )}
    </SuoritusInfoPaper>
  );
};
