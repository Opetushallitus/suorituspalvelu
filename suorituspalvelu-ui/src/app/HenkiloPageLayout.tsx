import { Stack } from '@mui/material';
import { LabeledInfoItem } from '@/components/LabeledInfoItem';
import { useTranslations } from '@/hooks/useTranslations';
import { OphTypography } from '@opetushallitus/oph-design-system';
import { useConfig } from '@/lib/configuration';
import { ExternalLink } from '@/components/ExternalLink';
import {
  formatFinnishDate,
  formatHenkiloNimi,
  isHenkiloOid,
  isHenkilotunnus,
} from '@/lib/common';
import type { Route } from './+types/HenkiloPageLayout';
import {
  queryOptionsGetOppija,
  queryOptionsGetOppilaitokset,
  useOppija,
} from '@/lib/suorituspalvelu-queries';
import { TiedotTabNavi } from '@/components/TiedotTabNavi';
import {
  Navigate,
  Outlet,
  useLocation,
  useParams,
  type Location,
} from 'react-router';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { HenkilotSidebar } from '@/components/HenkilotSidebar';
import { SearchControls } from '@/components/SearchControls';
import { Header } from '@/components/Header';
import { PageLayout } from '@/components/PageLayout';
import { NavigationSpinner } from './NavigationSpinner';
import { useSelectedSearchTab } from '@/hooks/useSelectedSearchTab';
import { ResultPlaceholder } from '@/components/ResultPlaceholder';
import { SearchTabNavi } from './SearchTabNavi';
import { queryClient } from '@/lib/queryClient';
import { useOppijatSearchParamsState } from '@/hooks/useSearchOppijat';
import type { OppijanTiedot } from '@/types/ui-types';

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

const HenkiloContent = ({ tiedot }: { tiedot: OppijanTiedot }) => {
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

export async function clientLoader({
  params,
  request,
}: Route.ClientLoaderArgs) {
  const oppijaNumero = params.oppijaNumero;
  if (oppijaNumero) {
    queryClient.ensureQueryData(queryOptionsGetOppija(oppijaNumero));
  }
  const url = request.url;
  if (url.includes('/tarkistus')) {
    queryClient.ensureQueryData(queryOptionsGetOppilaitokset());
  }
}

const HenkiloOppijanumerolla = ({
  oppijaNumero,
  selectHenkiloText,
}: {
  oppijaNumero?: string;
  selectHenkiloText: string;
}) => {
  const { data: tiedot } = useOppija(oppijaNumero ?? '');
  return (
    <>
      {tiedot ? (
        <HenkiloContent tiedot={tiedot} />
      ) : (
        <ResultPlaceholder text={selectHenkiloText} />
      )}
    </>
  );
};

export const getOppijanumeroRedirectURL = (
  location: Location,
  oppijaNumero?: string | null,
) => {
  const newLocation = { ...location };
  const pathParts = location.pathname.split('/');

  if (oppijaNumero) {
    pathParts.splice(2, 1, oppijaNumero);
    pathParts.splice(3, 1, pathParts[3] ?? 'suoritustiedot');
  } else {
    pathParts.splice(2);
  }

  newLocation.pathname = pathParts.join('/');
  return newLocation.pathname === location.pathname ? null : newLocation;
};

const useUpdatedRedirectLocation = ({
  tunniste,
}: {
  tunniste?: string | null;
}) => {
  const location = useLocation();

  const { data } = useOppija(tunniste ?? '');

  return getOppijanumeroRedirectURL(location, data?.oppijaNumero);
};

const HenkiloTunnisteella = () => {
  const { tunniste } = useOppijatSearchParamsState();
  const { t } = useTranslations();
  const link = useUpdatedRedirectLocation({ tunniste });

  const { oppijaNumero } = useParams();

  if (link) {
    return <Navigate to={link} replace={true} />;
  }

  return tunniste && (isHenkilotunnus(tunniste) || isHenkiloOid(tunniste)) ? (
    <HenkiloOppijanumerolla
      oppijaNumero={oppijaNumero}
      selectHenkiloText={t('search.henkiloa-ei-loytynyt')}
    />
  ) : (
    <ResultPlaceholder text={t('search.ei-validi-tunniste')} />
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
              {selectedSearchTab === 'henkilo' && <HenkiloTunnisteella />}
              {selectedSearchTab === 'tarkistus' && (
                <HenkiloOppijanumerolla
                  oppijaNumero={oppijaNumero}
                  selectHenkiloText={t('search.hae-ja-valitse-henkilo')}
                />
              )}
            </QuerySuspenseBoundary>
          </NavigationSpinner>
        </main>
      </Stack>
    </PageLayout>
  );
}
