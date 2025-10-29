import { isEmpty, isNullish, omitBy } from 'remeda';
import { configPromise } from './configuration';
import { client } from './http-client';
import type {
  ILuoSuoritusDropdownDataSuccessResponse,
  IOppijanHakuSuccessResponse,
  IOppijanTiedotSuccessResponse,
  IOppilaitosSuccessResponse,
} from '@/types/backend';
import type { KayttajaTiedot, SuoritusFields } from '@/types/ui-types';
import { format } from 'date-fns';

export type OppijatSearchParams = {
  hakusana?: string;
  oppilaitos?: string;
  vuosi?: string;
  luokka?: string;
};

export const cleanSearchParams = (params: OppijatSearchParams) => {
  return omitBy(params, (value) => isEmpty(value) || value === '');
};

export const searchOppijat = async (params: OppijatSearchParams) => {
  const cleanParams = cleanSearchParams(params);
  if (isEmpty(cleanParams)) {
    return { oppijat: [] };
  }

  const config = await configPromise;
  const urlSearch = new URLSearchParams(omitBy(params, isNullish));

  const url = `${config.routes.suorituspalvelu.oppijatSearchUrl}?${urlSearch.toString()}`;

  const res = await client.get<IOppijanHakuSuccessResponse>(url);
  return res.data;
};

export const getOppija = async (oppijaNumero: string) => {
  const config = await configPromise;

  const res = await client.get<IOppijanTiedotSuccessResponse>(
    `${config.routes.suorituspalvelu.oppijanTiedotUrl}/${oppijaNumero}`,
  );

  return res.data;
};

export const getOppilaitokset = async () => {
  const config = await configPromise;

  const res = await client.get<IOppilaitosSuccessResponse>(
    config.routes.suorituspalvelu.oppilaitoksetUrl,
  );
  return res.data;
};

export const getKayttaja = async () => {
  const config = await configPromise;

  const res = await client.get<KayttajaTiedot>(
    config.routes.suorituspalvelu.kayttajanTiedotUrl,
  );
  return res.data;
};

export const getAsiointiKieli = async () => {
  const kayttaja = await getKayttaja();
  return kayttaja.asiointiKieli;
};

export const getSuoritusvaihtoehdot = async () => {
  const config = await configPromise;

  const res = await client.get<ILuoSuoritusDropdownDataSuccessResponse>(
    config.routes.suorituspalvelu.suoritusvaihtoehdotUrl,
  );
  return res.data;
};

export const saveSuoritus = async (
  suoritusFields: SuoritusFields,
): Promise<void> => {
  const config = await configPromise;

  const postData: Record<string, unknown> = {
    oppijaOid: suoritusFields.oppijaOid,
    oppilaitosOid: suoritusFields.oppilaitosOid,
    suorituskieli: suoritusFields.suorituskieli,
    yksilollistetty: parseInt(suoritusFields.yksilollistetty, 10),
    valmistumispaiva: suoritusFields.valmistumispaiva
      ? format(suoritusFields.valmistumispaiva, 'yyyy-MM-dd')
      : undefined,
  };

  const oppiaineet = suoritusFields.oppiaineet?.flatMap((oa) => {
    const pakolliset = oa.arvosana
      ? [
          {
            koodi: oa.koodi,
            kieli: oa.kieli,
            arvosana: parseInt(oa.arvosana, 10),
            valinnainen: false,
          },
        ]
      : [];

    const valinnaiset = (oa.valinnaisetArvosanat ?? []).map((arv) => ({
      koodi: oa.koodi,
      kieli: oa.kieli,
      arvosana: parseInt(arv, 10),
      valinnainen: true,
    }));

    return [...pakolliset, ...valinnaiset];
  });

  let url: string | null = null;
  if (suoritusFields.tyyppi === 'perusopetuksenoppimaara') {
    url = config.routes.suorituspalvelu.perusopetuksenOppimaaratUrl;
    postData.oppiaineet = oppiaineet;
  } else if (suoritusFields.tyyppi === 'perusopetuksenoppiaineenoppimaara') {
    url = config.routes.suorituspalvelu.perusopetuksenOppiaineenOppimaaratUrl;
    postData.oppiaine = oppiaineet?.[0];
  } else {
    throw new Error(`Tuntematon suoritustyyppi: ${suoritusFields.tyyppi}`);
  }

  await client.post(url, postData);
};

export const deleteSuoritus = async (versioTunniste: string) => {
  const config = await configPromise;
  const url =
    config.routes.suorituspalvelu.versioDeleteUrl + '/' + versioTunniste;
  const res = await client.delete(url);
  return res.data;
};
