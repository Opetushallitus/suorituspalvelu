import { use } from 'react';

export const isTest = import.meta.env.VITE_TEST === 'true';

export const localTranslations =
  import.meta.env.VITE_LOCAL_TRANSLATIONS === 'true';

export const getConfiguration = async () => {
  const DOMAIN = '';

  const suorituspalveluBackendUrl = `${DOMAIN}/suorituspalvelu/api`;

  return {
    routes: {
      yleiset: {
        raamitUrl: `${DOMAIN}/virkailija-raamit/apply-raamit.js`,
        casLoginUrl: `${DOMAIN}/cas/login`,
        lokalisointiUrl: `${DOMAIN}/lokalisointi/tolgee`,
        organisaatioLinkUrl: `${DOMAIN}/organisaatio-service/lomake`,
        oppijaNumeroLinkUrl: `${DOMAIN}/henkilo-ui/virkailija/`,
      },
      suorituspalvelu: {
        kayttajanTiedotUrl: `${suorituspalveluBackendUrl}/ui/kayttaja`,
        oppijanTiedotUrl: `${suorituspalveluBackendUrl}/ui/tiedot`,
        oppijatSearchUrl: `${suorituspalveluBackendUrl}/ui/oppijat`,
        oppilaitoksetUrl: `${suorituspalveluBackendUrl}/ui/oppilaitokset`,
      },
    },
  };
};

export const configPromise = getConfiguration();

export const useConfig = () => use(configPromise);
