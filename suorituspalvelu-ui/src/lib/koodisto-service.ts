import type { Kielistetty, Language } from '@/types/ui-types';
import { client } from './http-client';
import { configPromise, getConfigUrl } from '@/lib/configuration';

export type Koodi = {
  koodiUri: string;
  nimi: Kielistetty;
  koodiArvo: string;
};

type CodeElement = {
  koodiUri: string;
  koodiArvo: string;
  metadata: [{ nimi: string; kieli: string }];
};

const getTranslatedNimi = (
  language: Language,
  metadata: [{ nimi: string; kieli: string }],
): string => {
  const matchingData = metadata.find(
    (m: { nimi: string; kieli: string }) => m.kieli.toLowerCase() === language,
  );
  return matchingData ? matchingData.nimi : '';
};

const mapToKoodi = (k: CodeElement): Koodi => {
  const translated = {
    fi: getTranslatedNimi('fi', k.metadata),
    sv: getTranslatedNimi('sv', k.metadata),
    en: getTranslatedNimi('en', k.metadata),
  };
  return { koodiUri: k.koodiUri, nimi: translated, koodiArvo: k.koodiArvo };
};

export async function getKoodit(koodisto: string): Promise<Array<Koodi>> {
  const configuration = await configPromise;
  const response = await client.get<Array<CodeElement>>(
    getConfigUrl(configuration.routes.yleiset.koodistoKooditUrl, { koodisto }),
  );
  return response.data.map(mapToKoodi);
}
