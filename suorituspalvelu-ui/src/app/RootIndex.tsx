import { queryClient } from '@/lib/queryClient';
import { queryOptionsGetKayttaja } from '@/lib/suorituspalvelu-queries';
import { redirect } from 'react-router';

export const clientLoader = async () => {
  const kayttaja = await queryClient.ensureQueryData(queryOptionsGetKayttaja());
  if (kayttaja?.isOrganisaationKatselija) {
    throw redirect('/tarkastus');
  } else {
    throw redirect('/henkilo');
  }
};

export default function RootPage() {
  return null;
}
