import { BackendFetch, DevTools, Tolgee, TolgeeInstance } from '@tolgee/react';
import { FormatIcu } from '@tolgee/format-icu';
import { configPromise, isTest, localTranslations } from '@/configuration';

const NAMESPACE = 'suorituspalvelu';
const REVALIDATE_TIME_SECONDS = 10 * 60;

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
        fi: () => import('./messages/fi.json'),
        sv: () => import('./messages/sv.json'),
        en: () => import('./messages/en.json'),
      },
    });
  } else {
    tg = tg
      .use(
        BackendFetch({
          prefix: config.routes.yleiset.lokalisointiUrl,
          next: {
            revalidate: REVALIDATE_TIME_SECONDS,
          },
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
