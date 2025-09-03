import {
  BackendFetch,
  DevTools,
  Tolgee,
  type TolgeeInstance,
} from '@tolgee/react';
import { FormatIcu } from '@tolgee/format-icu';
import { configPromise, isTest, localTranslations } from '@/configuration';

const NAMESPACE = 'suorituspalvelu';

let tolgeeInstance: TolgeeInstance | null = null;
export async function initTolgee(
  defaultLanguage?: string,
): Promise<TolgeeInstance> {
  const config = await configPromise;

  if (tolgeeInstance) {
    return tolgeeInstance;
  }
  let tg = Tolgee()
    .use(FormatIcu())
    .updateDefaults({
      availableLanguages: ['fi', 'sv', 'en'],
      defaultLanguage: defaultLanguage ?? 'fi',
    });

  if (isTest || localTranslations) {
    tg = tg.updateDefaults({
      staticData: {
        fi: () => import('./messages/fi.json').then((mod) => mod.default),
        sv: () => import('./messages/sv.json').then((mod) => mod.default),
        en: () => import('./messages/en.json').then((mod) => mod.default),
      },
    });
  } else {
    tg = tg
      .use(
        BackendFetch({
          prefix: config.routes.yleiset.lokalisointiUrl,
        }),
      )
      .use(DevTools())
      .updateDefaults({
        defaultNs: NAMESPACE,
        ns: [NAMESPACE],
        projectId: 11100,
      });
  }

  tolgeeInstance = tg.init();
  tolgeeInstance.on('error', (error) => {
    console.error(error);
  });
  return tolgeeInstance;
}
