const DOMAIN = 'http://localhost';

export const getConfiguration = async () => {
    const suorituspalveluBackendUrl = `${DOMAIN}/suorituspalvelu-backend`;

    return {
        routes: {
            yleiset: {
                raamitUrl:`${DOMAIN}/virkailija-raamit/apply-raamit.js`,
                casLoginUrl: `${DOMAIN}/cas/login`,
            },
            suorituspalvelu: {
                loginUrl: `${suorituspalveluBackendUrl}/login`,
                oppijanTiedotUrl: `${suorituspalveluBackendUrl}/ui/tiedot`,
            }
        }
    };
}

export const configPromise = getConfiguration();