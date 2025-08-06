import { Kielistetty, Language } from '@/types/ui-types';
import { isPlainObject } from 'remeda';

export function translateKielistetty(
  translated: Kielistetty,
  userLanguage: Language = 'fi',
): string {
  const prop = userLanguage;
  const translation = translated[prop];
  if (translation && translation?.trim().length > 0) {
    return translated[prop] || '';
  } else if (translated.fi && translated.fi.trim().length > 0) {
    return translated.fi;
  } else if (translated.en && translated.en.trim().length > 0) {
    return translated.en;
  }
  return translated.sv ?? '';
}

export function isKielistetty(value: unknown): value is Kielistetty {
  return (
    isPlainObject(value) &&
    (typeof value?.fi === 'string' ||
      typeof value?.sv === 'string' ||
      typeof value?.en === 'string')
  );
}
