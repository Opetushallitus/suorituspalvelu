import { useTranslations } from '@/hooks/useTranslations';
import {
  formatFinnishDate,
  formatHenkiloNimi,
  isHenkilotunnus,
} from '@/lib/common';
import type { OppijanTiedot } from '@/types/ui-types';
import { Stack } from '@mui/material';
import { OphTypography } from '@opetushallitus/oph-design-system';
import { LabeledInfoItem } from '../components/LabeledInfoItem';
import { ExternalLink } from '../components/ExternalLink';
import { useConfig } from '@/lib/configuration';
import { TiedotTabNavi } from '../components/TiedotTabNavi';
import { Outlet, type OppijaContext } from 'react-router';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { ResultPlaceholder } from '@/components/ResultPlaceholder';
import {
  queryOptionsGetOppija,
  useOppija,
} from '@/lib/suorituspalvelu-queries';
import { useEffect } from 'react';
import { queryClient } from '@/lib/queryClient';
import { useOppijaNumeroParamState } from '@/hooks/useOppijanumeroParamState';

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

export const OppijanTiedotContent = ({ tiedot }: { tiedot: OppijanTiedot }) => {
  const { t } = useTranslations();

  return (
    <Stack spacing={3} sx={{ padding: 2 }}>
      <title>{`${t('suorituspalvelu')} - ${t('oppija.otsikko')} - ${formatHenkiloNimi(tiedot, t)}`}</title>
      <Stack spacing={2}>
        <OphTypography variant="h3" component="h2">
          {formatHenkiloNimi(tiedot, t)}{' '}
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
      <TiedotTabNavi />
      <QuerySuspenseBoundary>
        <Outlet
          context={
            { oppijaNumero: tiedot.oppijaNumero } satisfies OppijaContext
          }
        />
      </QuerySuspenseBoundary>
    </Stack>
  );
};

export const OppijanTiedotPage = ({
  oppijaTunniste,
}: {
  oppijaTunniste?: string;
}) => {
  const { data: tiedot } = useOppija(oppijaTunniste);

  const foundOppijaNumero = tiedot?.oppijaNumero;

  const { setOppijaNumero } = useOppijaNumeroParamState();

  useEffect(() => {
    if (foundOppijaNumero) {
      if (isHenkilotunnus(oppijaTunniste)) {
        queryClient.setQueryData(
          queryOptionsGetOppija(foundOppijaNumero).queryKey,
          tiedot,
        );
      }
      setOppijaNumero(foundOppijaNumero, { replace: true });
    }
  }, [foundOppijaNumero, oppijaTunniste, tiedot]);

  const { t } = useTranslations();
  return (
    <>
      {tiedot ? (
        <OppijanTiedotContent tiedot={tiedot} />
      ) : (
        <ResultPlaceholder
          text={t('search.henkiloa-ei-loytynyt', { oppijaTunniste })}
        />
      )}
    </>
  );
};
