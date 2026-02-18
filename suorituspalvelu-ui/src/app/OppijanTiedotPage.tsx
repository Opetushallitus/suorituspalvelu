import { useTranslations } from '@/hooks/useTranslations';
import {
  formatFinnishDate,
  formatHenkiloNimi,
  isHenkilotunnus,
} from '@/lib/common';
import type { OppijanTiedot } from '@/types/ui-types';
import { Stack } from '@mui/material';
import { OphTypography } from '@opetushallitus/oph-design-system';
import { LabeledInfoItem } from '../components/LabeledInfoItem';
import { ExternalLink } from '../components/ExternalLink';
import { useConfig } from '@/lib/configuration';
import { TiedotTabNavi } from '../components/TiedotTabNavi';
import { Outlet, type OppijaContext } from 'react-router';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { ResultPlaceholder } from '@/components/ResultPlaceholder';
import {
  queryOptionsGetOppija,
  useKayttaja,
  useOppija,
} from '@/lib/suorituspalvelu-queries';
import { useEffect } from 'react';
import { queryClient } from '@/lib/queryClient';
import { useOppijaNumeroParamState } from '@/hooks/useOppijanumeroParamState';
import { Box } from '@mui/system';
import { useSelectedTiedotTab } from '@/hooks/useSelectedTiedotTab';

const OppijanumeroLink = ({ oppijaNumero }: { oppijaNumero: string }) => {
  const config = useConfig();
  return (
    <ExternalLink
      href={config.routes.yleiset.oppijaNumeroLinkUrl + oppijaNumero}
    >
      {oppijaNumero}
    </ExternalLink>
  );
};

export const OppijanTiedotContent = ({ tiedot }: { tiedot: OppijanTiedot }) => {
  const { t } = useTranslations();
  const config = useConfig();
  const { data: kayttaja } = useKayttaja();

  const henkiloNimi = formatHenkiloNimi(tiedot, t);

  const tiedotTab = useSelectedTiedotTab();

  return (
    <Stack spacing={3} sx={{ padding: 2 }}>
      <title>{`${t('suorituspalvelu')} - ${t('oppija.otsikko')} - ${henkiloNimi}`}</title>
      <Stack spacing={2}>
        <OphTypography variant="h3" component="h2">
          {henkiloNimi}{' '}
          {tiedot.henkiloTunnus && (
            <span style={{ fontWeight: 'normal' }}>
              ({tiedot.henkiloTunnus})
            </span>
          )}
        </OphTypography>
        <Stack direction="row">
          <LabeledInfoItem
            label={t('oppija.syntymaaika')}
            value={formatFinnishDate(tiedot?.syntymaAika)}
          />
          <LabeledInfoItem
            label={t('oppija.oppijanumero')}
            value={<OppijanumeroLink oppijaNumero={tiedot.oppijaNumero} />}
          />
          <LabeledInfoItem
            label={t('oppija.henkiloOid')}
            value={tiedot?.henkiloOID}
          />
        </Stack>
        {kayttaja.isRekisterinpitaja && (
          <Box sx={{ alignSelf: 'flex-start' }}>
            <ExternalLink
              href={`${config.routes.yleiset.koskiOppijaLinkUrl}${tiedot.oppijaNumero}`}
            >
              {t('oppija.avaa-koski-jarjestelmassa')}
            </ExternalLink>
          </Box>
        )}
      </Stack>
      <TiedotTabNavi />
      <QuerySuspenseBoundary key={tiedotTab}>
        <Outlet
          context={
            { oppijaNumero: tiedot.oppijaNumero } satisfies OppijaContext
          }
        />
      </QuerySuspenseBoundary>
    </Stack>
  );
};

const OppijanTiedotWrapper = ({
  oppijaTunniste,
}: {
  oppijaTunniste?: string;
}) => {
  const { data: tiedot } = useOppija(oppijaTunniste);

  const foundOppijaNumero = tiedot?.oppijaNumero;

  const { setOppijaNumero } = useOppijaNumeroParamState();

  useEffect(() => {
    if (foundOppijaNumero) {
      if (isHenkilotunnus(oppijaTunniste)) {
        // Asetetaan oppijanumero-querylle sama data, ettei tarvitse noutaa uudelleen
        queryClient.setQueryData(
          queryOptionsGetOppija(foundOppijaNumero).queryKey,
          tiedot,
        );
      }
      // Asetetaan oppijanumero-parametri URL:iin, jos oppija löytyi
      setOppijaNumero(foundOppijaNumero, { replace: true });
    }
  }, [foundOppijaNumero, oppijaTunniste, tiedot, setOppijaNumero]);

  const { t } = useTranslations();
  return tiedot ? (
    <OppijanTiedotContent tiedot={tiedot} />
  ) : (
    <ResultPlaceholder
      text={t('search.henkiloa-ei-loytynyt', { oppijaTunniste })}
    />
  );
};

export const OppijanTiedotPage = ({
  oppijaTunniste,
}: {
  oppijaTunniste?: string;
}) => {
  return (
    /* React-router käyttää React transitiota navigoitaessa, ja jos näkymän päivityksen aiheuttaa transitio, 
    suspensen fallback-elementtiä ei näytetä, vaan vanha data pysyy näkyvissä (https://react.dev/reference/react/Suspense#caveats). 
    React-query käyttää suspensea fallback-animaation näyttämiseen kun noudetaan dataa. 
    Asetetaan key, jotta komponentti resetoituu ja näytetään latausanimaatio vaihdettaessa henkilöä.
     */
    <QuerySuspenseBoundary key={oppijaTunniste}>
      <OppijanTiedotWrapper oppijaTunniste={oppijaTunniste} />
    </QuerySuspenseBoundary>
  );
};
