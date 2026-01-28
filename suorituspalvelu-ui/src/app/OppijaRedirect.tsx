import { queryClient } from '@/lib/queryClient';
import { queryOptionsGetKayttaja } from '@/lib/suorituspalvelu-queries';
import { redirect } from 'react-router';
import type { Route } from './+types/OppijaRedirect';

export const clientLoader = async ({ params }: Route.ClientLoaderArgs) => {
  const kayttaja = await queryClient.ensureQueryData(queryOptionsGetKayttaja());
  if (kayttaja?.isOrganisaationKatselija) {
    throw redirect(`/tarkastus/${params.henkiloOid}`);
  } else {
    throw redirect(`/henkilo/${params.henkiloOid}`);
  }
};

export default function OppijaRedirect() {
  return null;
}
