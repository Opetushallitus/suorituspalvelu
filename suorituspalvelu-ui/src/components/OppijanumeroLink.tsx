import type { IKayttajaSuccessResponse } from '@/types/backend';
import { useConfig } from '@/lib/configuration';
import { ExternalLink } from './ExternalLink';

export const OppijanumeroLink = ({
  oppijaNumero,
  kayttaja,
}: {
  oppijaNumero: string;
  kayttaja: IKayttajaSuccessResponse;
}) => {
  const config = useConfig();

  if (!kayttaja.isRekisterinpitaja && !kayttaja.isHakeneidenKatselija) {
    return <>{oppijaNumero}</>;
  }

  const queryString =
    !kayttaja.isRekisterinpitaja && kayttaja.isHakeneidenKatselija
      ? '?permissionCheckService=ATARU'
      : '';

  return (
    <ExternalLink
      href={`${config.routes.yleiset.oppijaNumeroLinkUrl}${oppijaNumero}${queryString}`}
    >
      {oppijaNumero}
    </ExternalLink>
  );
};
