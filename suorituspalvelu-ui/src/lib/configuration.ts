import { use } from 'react';

export const isTest = import.meta.env.VITE_TEST === 'true';

export const localTranslations =
  import.meta.env.VITE_LOCAL_TRANSLATIONS === 'true';

const DOMAIN = typeof window !== 'undefined' ? window.location.origin : '';

export const getConfiguration = async () => {
  const suorituspalveluBackendUrl = `${DOMAIN}/suorituspalvelu/api`;

  return {
    routes: {
      yleiset: {
        raamitUrl: `${DOMAIN}/virkailija-raamit/apply-raamit.js`,
        casLoginUrl: `${DOMAIN}/cas/login`,
        lokalisointiUrl: `${DOMAIN}/lokalisointi/tolgee`,
        organisaatioLinkUrl: `${DOMAIN}/organisaatio-service/lomake`,
        oppijaNumeroLinkUrl: `${DOMAIN}/henkilo-ui/virkailija/`,
        koskiOppijaLinkUrl: `${DOMAIN}/koski/oppija/`,
      },
      suorituspalvelu: {
        kayttajanTiedotUrl: `${suorituspalveluBackendUrl}/ui/kayttaja`,
        oppijanTiedotUrl: `${suorituspalveluBackendUrl}/ui/tiedot`,
        oppilaitoksenOppijatSearchUrl: `${suorituspalveluBackendUrl}/ui/rajain/oppilaitoksenoppijat`,
        vuodetUrl: `${suorituspalveluBackendUrl}/ui/rajain/vuodet`,
        luokatUrl: `${suorituspalveluBackendUrl}/ui/rajain/luokat`,
        oppilaitoksetUrl: `${suorituspalveluBackendUrl}/ui/rajain/oppilaitokset`,
        suoritusvaihtoehdotUrl: `${suorituspalveluBackendUrl}/ui/tallennasuoritusvaihtoehdot`,
        oppilaitosvaihtoehdotUrl: `${suorituspalveluBackendUrl}/ui/tallennasuoritusoppilaitokset`,
        perusopetuksenOppimaaratUrl: `${suorituspalveluBackendUrl}/ui/perusopetuksenoppimaarat`,
        perusopetuksenOppiaineenOppimaaratUrl: `${suorituspalveluBackendUrl}/ui/perusopetuksenoppiaineenoppimaarat`,
        versioDeleteUrl: `${suorituspalveluBackendUrl}/ui/versiot`,
        valintadataUrl: `${suorituspalveluBackendUrl}/ui/valintadata`,
        tallennaYliajotUrl: `${suorituspalveluBackendUrl}/ui/tallennayliajot`,
        poistaYliajoUrl: `${suorituspalveluBackendUrl}/ui/poistayliajo`,
        oppijanHautUrl: `${suorituspalveluBackendUrl}/ui/oppijanhaut`,
        valintadataHistoriaUrl: `${suorituspalveluBackendUrl}/ui/valintadatahistoria`,
      },
    },
  };
};

export const configPromise = getConfiguration();

export const useConfig = () => use(configPromise);

export function getConfigUrl(
  routeString: string,
  params: Record<string, string | boolean | number> = {},
): string {
  let route = routeString;
  Object.entries(params).forEach(
    (entry: [string, string | number | boolean]) => {
      const value = '' + entry[1];
      const placeholder = `{${entry[0]}}`;
      if (!route.includes(placeholder)) {
        console.warn(
          `Placeholder ${placeholder} not found in route ${routeString}. Using value: ${value}`,
        );
      }

      route = route.replace(`{${entry[0]}}`, value);
    },
  );

  if (/\{[^}]+\}/.test(route)) {
    throw new Error(
      `Not all placeholders were replaced in route ${routeString}. Result: ${route}`,
    );
  }
  return route;
}
