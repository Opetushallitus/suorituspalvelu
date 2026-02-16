import { queryClient } from '@/lib/queryClient';
import { queryOptionsGetKayttaja } from '@/lib/suorituspalvelu-queries';
import { redirect } from 'react-router';

export const clientLoader = async () => {
  const kayttaja = await queryClient.ensureQueryData(queryOptionsGetKayttaja());
  if (kayttaja?.isHakeneidenKatselija || kayttaja?.isRekisterinpitaja) {
    throw redirect(`/henkilo`);
  } else if (kayttaja?.isOrganisaationKatselija) {
    throw redirect(`/tarkastus`);
  }
};

export default function RootPage() {
  return null;
}
