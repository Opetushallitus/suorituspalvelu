import { LukionOppiaine, LukioSuoritus } from '@/types/ui-types';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { ophColors } from '@opetushallitus/oph-design-system';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { useTranslate } from '@tolgee/react';
import { LabeledInfoItem } from './LabeledInfoItem';
import { styled } from '@/lib/theme';

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

export const LukioSuoritusPaper = ({
  nimi,
  lukioSuoritus,
}: {
  nimi: string;
  lukioSuoritus: LukioSuoritus;
}) => {
  return (
    <SuoritusInfoPaper
      suorituksenNimi={nimi}
      valmistumispaiva={lukioSuoritus.valmistumispaiva}
      topColor={ophColors.blue2}
    >
      <SuorituksenPerustiedotIndicator perustiedot={lukioSuoritus} />
      {'oppiaineet' in lukioSuoritus && (
        <LukionOppiaineetList oppiaineet={lukioSuoritus.oppiaineet} />
      )}
    </SuoritusInfoPaper>
  );
};
