'use client';

import { useEffect, useState } from 'react';
import { TolgeeInstance, TolgeeProvider } from '@tolgee/react';
import { initTolgee } from '@/localization/tolgee-config';
import { OphNextJsThemeProvider } from '@opetushallitus/oph-design-system/next/theme';
import { UntranslatedFullSpinner } from './FullSpinner';
import { getAsiointiKieli } from '@/api';
import { Language } from '@/types/ui-types';
import { THEME_OVERRIDES } from '@/lib/theme';

export function LocalizationProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const [lang, setLang] = useState<Language | null>(null);
  const [tolgee, setTolgee] = useState<TolgeeInstance | null>(null);

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
        console.error(e);
        throw Error('Tolgee-käännöspalvelun alustaminen epäonnistui!');
      }
    })();
  }, []);

  return lang && tolgee ? (
    <TolgeeProvider tolgee={tolgee} fallback={<UntranslatedFullSpinner />}>
      <OphNextJsThemeProvider
        variant="oph"
        lang={lang}
        overrides={THEME_OVERRIDES}
      >
        {children}
      </OphNextJsThemeProvider>
    </TolgeeProvider>
  ) : (
    <UntranslatedFullSpinner />
  );
}
