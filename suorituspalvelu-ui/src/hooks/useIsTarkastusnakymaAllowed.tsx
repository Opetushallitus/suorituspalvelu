import { useKayttaja } from '@/lib/suorituspalvelu-queries';

export const useIsTarkastusnakymaAllowed = () => {
  const { data: kayttaja } = useKayttaja();
  return kayttaja?.isOrganisaationKatselija || kayttaja?.isRekisterinpitaja;
};
