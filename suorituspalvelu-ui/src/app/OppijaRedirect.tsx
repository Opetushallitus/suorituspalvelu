import { queryClient } from '@/lib/queryClient';
import { queryOptionsGetKayttaja } from '@/lib/suorituspalvelu-queries';
import { redirect } from 'react-router';
import type { Route } from './+types/OppijaRedirect';

export const clientLoader = async ({ params }: Route.ClientLoaderArgs) => {
  const kayttaja = await queryClient.ensureQueryData(queryOptionsGetKayttaja());
  if (kayttaja?.isHakeneidenKatselija || kayttaja?.isRekisterinpitaja) {
    throw redirect(`/henkilo/${params.henkiloOid}`);
  } else if (kayttaja?.isOrganisaationKatselija) {
    throw redirect(`/tarkastus/${params.henkiloOid}`);
  }
};

export default function OppijaRedirect() {
  return null;
}
