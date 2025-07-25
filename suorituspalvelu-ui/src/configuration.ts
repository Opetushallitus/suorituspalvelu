const DOMAIN = 'http://localhost';

export const isTest = process.env.NEXT_PUBLIC_TEST === 'true';

export const localTranslations =
  process.env.NEXT_PUBLIC_LOCAL_TRANSLATIONS === 'true';

export const getConfiguration = async () => {
  const suorituspalveluBackendUrl = `${DOMAIN}/suorituspalvelu-backend`;

  return {
    routes: {
      yleiset: {
        raamitUrl: `${DOMAIN}/virkailija-raamit/apply-raamit.js`,
        casLoginUrl: `${DOMAIN}/cas/login`,
        lokalisointiUrl: `${DOMAIN}/lokalisointi/tolgee`,
        organisaatioLinkUrl: `${DOMAIN}/organisaatio-service/lomake`,
      },
      suorituspalvelu: {
        loginUrl: `${suorituspalveluBackendUrl}/login`,
        oppijanTiedotUrl: `${suorituspalveluBackendUrl}/ui/tiedot`,
        oppijatSearchUrl: `${suorituspalveluBackendUrl}/ui/oppijat`,
        oppilaitoksetUrl: `${suorituspalveluBackendUrl}/ui/oppilaitokset`,
      },
    },
  };
};

export const configPromise = getConfiguration();
