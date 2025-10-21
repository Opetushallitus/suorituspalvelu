import { useEffect, useState } from 'react';
import { type TolgeeInstance, TolgeeProvider } from '@tolgee/react';
import { initTolgee } from '@/localization/tolgee-config';
import { OphThemeProvider } from '@opetushallitus/oph-design-system/theme';
import { UntranslatedFullSpinner } from './FullSpinner';
import { type Language } from '@/types/ui-types';
import { THEME_OVERRIDES } from '@/lib/theme';
import { Box } from '@mui/material';
import { getAsiointiKieli } from '@/lib/suorituspalvelu-service';

export function LocalizationProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const [lang, setLang] = useState<Language | null>(null);
  const [tolgee, setTolgee] = useState<TolgeeInstance | null>(null);
  const [error, setError] = useState<Error | null>(null);

  // Prerender epäonnistuu, jos asiointikieli noudetaan server-puolella, koska URL:t on pelkkiä polkuja.
  // Kääritään asiointikielen noutaminen useEffectiin, jolloin se suoritetaan pelkästään client-puolella.
  useEffect(() => {
    (async () => {
      let k: Language = 'fi';
      try {
        k = await getAsiointiKieli();
      } catch (e) {
        console.error(
          'Asiointikielen noutaminen epäonnistui, käytetään oletuskieltä (suomi)',
        );
        console.error(e);
      }
      setLang(k);
      try {
        setTolgee(await initTolgee(k));
      } catch (e) {
        setError(e as Error);
      }
    })();
  }, []);

  if (error) {
    return <Box>Tolgee-käännöspalvelun alustaminen epäonnistui!</Box>;
  }

  return lang && tolgee ? (
    <TolgeeProvider tolgee={tolgee} fallback={<UntranslatedFullSpinner />}>
      <OphThemeProvider variant="oph" lang={lang} overrides={THEME_OVERRIDES}>
        {children}
      </OphThemeProvider>
    </TolgeeProvider>
  ) : (
    <UntranslatedFullSpinner />
  );
}
