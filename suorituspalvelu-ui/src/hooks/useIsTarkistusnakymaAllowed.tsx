import { useKayttaja } from '@/lib/suorituspalvelu-queries';

export const useIsTarkistusnakymaAllowed = () => {
  const { data: kayttaja } = useKayttaja();
  return kayttaja?.isOrganisaationKatselija || kayttaja?.isRekisterinpitaja;
};
