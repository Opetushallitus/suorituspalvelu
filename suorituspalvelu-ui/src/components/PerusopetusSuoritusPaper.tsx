import { PerusopetusSuoritus } from '@/types/ui-types';
import { ophColors } from '@opetushallitus/oph-design-system';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { Stack } from '@mui/material';
import { LabeledInfoItem } from './LabeledInfoItem';
import { useTranslate } from '@tolgee/react';

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
    </SuoritusInfoPaper>
  );
};
