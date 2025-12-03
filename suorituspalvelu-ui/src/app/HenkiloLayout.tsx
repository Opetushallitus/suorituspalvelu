import { Box } from '@mui/material';
import { useTranslations } from '@/hooks/useTranslations';
import { isHenkiloOid, isHenkilotunnus } from '@/lib/common';
import type { Route } from './+types/HenkiloLayout';
import {
  queryOptionsGetOppija,
  useOppija,
} from '@/lib/suorituspalvelu-queries';
import {
  Navigate,
  redirect,
  useLocation,
  useParams,
  type Location,
  type ShouldRevalidateFunctionArgs,
} from 'react-router';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { HenkiloSearchControls } from '@/components/SearchControls';
import { ResultPlaceholder } from '@/components/ResultPlaceholder';
import { queryClient } from '@/lib/queryClient';
import { useOppijatSearchParamsState } from '@/hooks/useSearchOppijat';
import { OppijanTiedotPage } from './OppijanTiedotPage';
import {
  getActiveTiedotTab,
  setActiveTiedotTab,
} from '@/hooks/useActiveTiedotTab';

/**
 * Palauttaa uuden Location-objektin löydetyn oppijanumeron perusteella uudelleenohjausta varten.
 *
 * @param location React-routerin nykyinen Location-objekti
 * @param oppijaNumero Uusi oppijanumero, jolla löytyi oppijan tiedot
 * @returns Uusi Location-objekti, jota on muokattu muuttuneen oppijanumeron mukaisesti, tai null jos sijainti ei muuttunut.
 */
export const getRedirectedOppijanumeroLocation = (
  location: Location,
  oppijaNumero?: string | null,
) => {
  const newLocation = { ...location };
  const pathParts = location.pathname.split('/');

  if (oppijaNumero) {
    // Asetetaan oppijanumero polkuun
    pathParts.splice(2, 1, oppijaNumero);
  } else {
    // Poistetaan kaikki polun osat oppijanumerosta lähtien, jos ei oppijanumeroa
    pathParts.splice(2);
  }

  newLocation.pathname = pathParts.join('/');

  if (newLocation.pathname === location.pathname) {
    return null;
  } else {
    return newLocation;
  }
};

export async function shouldRevalidate({
  currentUrl,
  nextUrl,
}: ShouldRevalidateFunctionArgs) {
  return currentUrl.pathname !== nextUrl.pathname;
}

export const clientLoader = async ({
  params,
  request,
}: Route.ClientLoaderArgs) => {
  const oppijaNumero = params.oppijaNumero;
  const url = new URL(request.url);

  if (oppijaNumero) {
    let changed = false;
    const tiedotTab = getActiveTiedotTab(url.pathname);

    if (!tiedotTab) {
      url.pathname = setActiveTiedotTab(url.pathname, 'suoritustiedot');
      changed = true;
    }

    const tunniste = url.searchParams.get('tunniste');

    // Asetetaan tyhjä tunniste vain jos oppijaNumero on henkiloOid
    if (!tunniste && isHenkiloOid(oppijaNumero)) {
      console.log('Adding tunniste param', oppijaNumero);
      url.searchParams.set('tunniste', oppijaNumero);
      changed = true;
    }

    if (changed) {
      console.log('changed');
      return redirect(url.toString());
    }
    queryClient.ensureQueryData(queryOptionsGetOppija(oppijaNumero));
  }
};

const useRedirectedLocation = (tunniste?: string | null) => {
  const location = useLocation();
  const { data } = useOppija(tunniste ?? '');

  return getRedirectedOppijanumeroLocation(location, data?.oppijaNumero);
};

const HenkiloTunnisteella = () => {
  const { tunniste } = useOppijatSearchParamsState();
  const { t } = useTranslations();
  const redirectTo = useRedirectedLocation(tunniste);
  const { oppijaNumero } = useParams();

  if (redirectTo) {
    return <Navigate to={redirectTo} replace={true} />;
  }

  if (!tunniste || (!isHenkiloOid(tunniste) && !isHenkilotunnus(tunniste))) {
    return <ResultPlaceholder text={t('search.ei-validi-tunniste')} />;
  }

  return <OppijanTiedotPage oppijaNumero={oppijaNumero} />;
};

export default function HenkiloLayout() {
  return (
    <>
      <HenkiloSearchControls />
      <Box component="main">
        <QuerySuspenseBoundary>
          <HenkiloTunnisteella />
        </QuerySuspenseBoundary>
      </Box>
    </>
  );
}
