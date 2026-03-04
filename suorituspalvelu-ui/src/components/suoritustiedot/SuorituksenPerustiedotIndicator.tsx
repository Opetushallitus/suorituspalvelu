import { Stack } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { ophColors, OphTypography } from '@opetushallitus/oph-design-system';
import { LabeledInfoItem } from '../LabeledInfoItem';
import {
  type SuorituksenPerustiedot,
  type SuorituksenTila,
} from '@/types/ui-types';
import { CheckCircle, DoNotDisturb, HourglassTop } from '@mui/icons-material';
import { useTranslations } from '@/hooks/useTranslations';
import { OppilaitosInfoItem } from '@/components/OppilaitosInfoItem';
import { formatFinnishDate } from '@/lib/common';
import { isKielistetty } from '@/lib/translation-utils';

const SuorituksenTilaIcon = ({ tila }: { tila: SuorituksenTila }) => {
  switch (tila) {
    case 'VALMIS':
      return <CheckCircle sx={{ color: ophColors.green2 }} />;
    case 'KESKEN':
      return <HourglassTop sx={{ color: ophColors.yellow1 }} />;
    case 'KESKEYTYNYT':
      return <DoNotDisturb sx={{ color: ophColors.orange3 }} />;
    default:
      return null;
  }
};

const SuorituksenTilaIndicator = ({ tila }: { tila: SuorituksenTila }) => {
  const { t } = useTranslate();

  return (
    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
      <SuorituksenTilaIcon tila={tila} />
      <OphTypography>
        {t('suoritus')} {t(`suorituksen-tila.${tila}`)}
      </OphTypography>
    </Stack>
  );
};

const ValmistumispaivaIndicator = ({
  valmistumispaiva,
}: {
  valmistumispaiva?: string;
}) => {
  return (
    <OphTypography>
      {valmistumispaiva ? formatFinnishDate(valmistumispaiva) : '-'}
    </OphTypography>
  );
};

export const SuorituksenPerustiedotIndicator = ({
  perustiedot,
}: {
  perustiedot: SuorituksenPerustiedot;
}) => {
  const { t, translateKielistetty } = useTranslations();

  const suorituskieli = perustiedot.suorituskieli
    ? isKielistetty(perustiedot.suorituskieli)
      ? translateKielistetty(perustiedot.suorituskieli)
      : t(`kieli.${perustiedot.suorituskieli}`)
    : '-';

  return (
    <Stack direction="row">
      <OppilaitosInfoItem oppilaitos={perustiedot.oppilaitos} />
      <LabeledInfoItem
        label={t('oppija.tila')}
        value={<SuorituksenTilaIndicator tila={perustiedot.tila} />}
      />
      <LabeledInfoItem
        label={t('oppija.valmistumispaiva')}
        value={
          <ValmistumispaivaIndicator
            valmistumispaiva={perustiedot.valmistumispaiva}
          />
        }
      />
      <LabeledInfoItem
        label={t('oppija.suorituskieli')}
        value={suorituskieli}
      />
    </Stack>
  );
};
