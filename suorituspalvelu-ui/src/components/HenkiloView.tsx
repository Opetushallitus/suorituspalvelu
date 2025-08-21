'use client';
import { useOppija } from '@/queries';
import { Stack } from '@mui/material';
import { Opiskeluoikeudet } from './Opiskeluoikeudet';
import { Suoritukset } from './Suoritukset';
import { LabeledInfoItem } from './LabeledInfoItem';
import { useTranslations } from '@/hooks/useTranslations';
import { OphTypography } from '@opetushallitus/oph-design-system';
import { ExternalLink } from './ExternalLink';
import { useConfig } from '@/configuration';
import { formatFinnishDate } from '@/lib/common';

const OppijanumeroLink = ({ oppijaNumero }: { oppijaNumero: string }) => {
  const config = useConfig();
  return (
    <ExternalLink
      href={config.routes.yleiset.oppijaNumeroLinkUrl + oppijaNumero}
    >
      {oppijaNumero}
    </ExternalLink>
  );
};

export const HenkiloView = ({ oppijaNumero }: { oppijaNumero: string }) => {
  const { data: tiedot } = useOppija(oppijaNumero);
  const { t } = useTranslations();
  return (
    <Stack spacing={6} sx={{ margin: 2 }}>
      <title>{`${t('suorituspalvelu')} - ${t('oppija.otsikko')} - ${tiedot.nimi}`}</title>
      <Stack spacing={2}>
        <OphTypography variant="h3" component="h2">
          {tiedot.nimi}{' '}
          <span style={{ fontWeight: 'normal' }}>({tiedot.henkiloTunnus})</span>
        </OphTypography>
        <Stack direction="row">
          <LabeledInfoItem
            label={t('oppija.syntymaaika')}
            value={formatFinnishDate(tiedot?.syntymaAika)}
          />
          <LabeledInfoItem
            label={t('oppija.oppijanumero')}
            value={<OppijanumeroLink oppijaNumero={tiedot.oppijaNumero} />}
          />
          <LabeledInfoItem
            label={t('oppija.henkiloOid')}
            value={tiedot?.henkiloOID}
          />
        </Stack>
      </Stack>
      <Opiskeluoikeudet opiskeluoikeudet={tiedot?.opiskeluoikeudet} />
      <Suoritukset oppijanTiedot={tiedot} />
    </Stack>
  );
};
