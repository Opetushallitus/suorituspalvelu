const DOMAIN = 'http://localhost';

export const getConfiguration = async () => {
  const suorituspalveluBackendUrl = `${DOMAIN}/suorituspalvelu-backend`;

  return {
    isTesting: process.env.NODE_ENV === 'test',
    localTranslations: process.env.NEXT_PUBLIC_LOCAL_TRANSLATIONS === 'true',
    routes: {
      yleiset: {
        raamitUrl: `${DOMAIN}/virkailija-raamit/apply-raamit.js`,
        casLoginUrl: `${DOMAIN}/cas/login`,
        lokalisointiUrl: `${DOMAIN}/lokalisointi/tolgee`,
      },
      suorituspalvelu: {
        loginUrl: `${suorituspalveluBackendUrl}/login`,
        oppijanTiedotUrl: `${suorituspalveluBackendUrl}/ui/tiedot`,
      },
    },
  };
};

export const configPromise = getConfiguration();
