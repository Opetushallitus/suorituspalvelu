import { useCallback } from 'react';
import { translateKielistetty } from '@/lib/translation-utils';
import { type TFnType, useTolgee, useTranslate } from '@tolgee/react';
import type { Kielistetty, Language } from '@/types/ui-types';

export type TFunction = TFnType;

export const useTranslations = () => {
  const { getLanguage } = useTolgee(['language']);
  const { t } = useTranslate();

  const translateNimi = useCallback(
    (translateable?: Kielistetty) => {
      return translateable
        ? translateKielistetty(translateable, getLanguage() as Language)
        : '';
    },
    [getLanguage],
  );

  return {
    t,
    translateKielistetty: translateNimi,
    getLanguage: getLanguage as () => Language,
  };
};
