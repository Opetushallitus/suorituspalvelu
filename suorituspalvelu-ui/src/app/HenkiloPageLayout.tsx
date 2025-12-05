import { Stack } from '@mui/material';
import { LabeledInfoItem } from '@/components/LabeledInfoItem';
import { useTranslations } from '@/hooks/useTranslations';
import { OphTypography } from '@opetushallitus/oph-design-system';
import { useConfig } from '@/lib/configuration';
import { ExternalLink } from '@/components/ExternalLink';
import { formatFinnishDate, formatHenkiloNimi } from '@/lib/common';
import type { Route } from './+types/HenkiloPageLayout';
import { useOppija } from '@/lib/suorituspalvelu-queries';
import { TiedotTabNavi } from '@/components/TiedotTabNavi';
import { Outlet } from 'react-router';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { HenkilotSidebar } from '@/components/HenkilotSidebar';
import { SearchControls } from '@/components/SearchControls';
import { Header } from '@/components/Header';
import { PageLayout } from '@/components/PageLayout';
import { NavigationSpinner } from './NavigationSpinner';
import { useSelectedSearchTab } from '@/hooks/useSelectedSearchTab';
import { ResultPlaceholder } from '@/components/ResultPlaceholder';
import { SearchTabNavi } from './SearchTabNavi';

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

const HenkiloContent = ({ oppijaNumero }: { oppijaNumero: string }) => {
  const { data: tiedot } = useOppija(oppijaNumero);
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
      <Outlet />
    </Stack>
  );
};

export default function HenkiloPageLayout({ params }: Route.ComponentProps) {
  const { t } = useTranslations();

  const { oppijaNumero } = params;
  const selectedSearchTab = useSelectedSearchTab();

  return (
    <PageLayout header={<Header title={t('suorituspalvelu')} />}>
      <SearchTabNavi />
      <SearchControls />
      <Stack direction="row">
        {selectedSearchTab === 'tarkistus' && <HenkilotSidebar />}
        <main style={{ flexGrow: 1 }}>
          <NavigationSpinner>
            <QuerySuspenseBoundary>
              {oppijaNumero ? (
                <HenkiloContent oppijaNumero={oppijaNumero} />
              ) : (
                <ResultPlaceholder
                  text={t(
                    selectedSearchTab === 'henkilo'
                      ? 'search.hae-henkilo'
                      : 'search.hae-ja-valitse-henkilo',
                  )}
                />
              )}
            </QuerySuspenseBoundary>
          </NavigationSpinner>
        </main>
      </Stack>
    </PageLayout>
  );
}
