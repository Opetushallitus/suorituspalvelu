import { useKayttaja } from '@/lib/suorituspalvelu-queries';

export const useIsHenkilohakuAllowed = () => {
  const { data: kayttaja } = useKayttaja();
  return kayttaja?.isHakeneidenKatselija || kayttaja?.isRekisterinpitaja;
};
