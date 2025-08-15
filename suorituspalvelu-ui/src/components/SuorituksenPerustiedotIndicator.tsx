'use client';
import { Stack } from '@mui/material';
import { use } from 'react';
import { useTranslate } from '@tolgee/react';
import {
  ophColors,
  OphLink,
  OphTypography,
} from '@opetushallitus/oph-design-system';
import { LabeledInfoItem } from './LabeledInfoItem';
import { getOppilaitosLinkUrl } from '@/lib/getOppilaitosLink';
import { configPromise } from '@/configuration';
import { SuorituksenPerustiedot, SuorituksenTila } from '@/types/ui-types';
import { CheckCircle, DoNotDisturb, HourglassTop } from '@mui/icons-material';
import { formatDate } from 'date-fns';
import { useTranslations } from '@/hooks/useTranslations';
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
      <OphTypography>{t(`suorituksen-tila.${tila}`)}</OphTypography>
    </Stack>
  );
};

const ValmistumispaivaIndicator = ({
  valmistumispaiva,
}: {
  valmistumispaiva?: Date;
}) => {
  return (
    <OphTypography>
      {valmistumispaiva ? formatDate(valmistumispaiva, 'd.M.y') : '-'}
    </OphTypography>
  );
};

export const SuorituksenPerustiedotIndicator = ({
  perustiedot,
}: {
  perustiedot: SuorituksenPerustiedot;
}) => {
  const { t, translateKielistetty } = useTranslations();
  const config = use(configPromise);

  return (
    <Stack direction="row">
      <LabeledInfoItem
        label={t('oppija.oppilaitos')}
        value={
          <OphLink
            component="a"
            href={getOppilaitosLinkUrl(config, perustiedot.oppilaitos.oid)}
          >
            {isKielistetty(perustiedot.oppilaitos.nimi)
              ? translateKielistetty(perustiedot.oppilaitos.nimi)
              : perustiedot.oppilaitos.nimi}
          </OphLink>
        }
      />
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
      {perustiedot.suorituskieli && (
        <LabeledInfoItem
          label={t('oppija.suorituskieli')}
          value={perustiedot.suorituskieli}
        />
      )}
    </Stack>
  );
};
