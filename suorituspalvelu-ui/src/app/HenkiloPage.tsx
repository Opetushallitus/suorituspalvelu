import { Stack } from '@mui/material';
import { Opiskeluoikeudet } from '@/components/Opiskeluoikeudet';
import { Suoritukset } from '@/components/Suoritukset';
import { LabeledInfoItem } from '@/components/LabeledInfoItem';
import { useTranslations } from '@/hooks/useTranslations';
import { OphTypography } from '@opetushallitus/oph-design-system';
import { useConfig } from '@/lib/configuration';
import { ExternalLink } from '@/components/ExternalLink';
import { formatFinnishDate } from '@/lib/common';
import type { Route } from './+types/HenkiloPage';
import { useOppija } from '@/lib/suorituspalvelu-queries';

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

export default function HenkiloPage({ params }: Route.ComponentProps) {
  const { data: tiedot } = useOppija(params.oppijaNumero);
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
}
