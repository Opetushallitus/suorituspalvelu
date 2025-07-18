import { BackendFetch, DevTools, Tolgee } from '@tolgee/react';
import { FormatIcu } from '@tolgee/format-icu';
import { configPromise, isTest, localTranslations } from '@/configuration';

const NAMESPACE = 'suorituspalvelu';

const REVALIDATE_TIME_SECONDS = 10 * 60;

export async function tolgeeBase() {
  const config = await configPromise;
  const tg = Tolgee()
    .use(FormatIcu())
    .updateDefaults({
      availableLanguages: ['fi', 'sv', 'en'],
      defaultLanguage: 'fi',
    });

  if (isTest || localTranslations) {
    return tg
      .updateDefaults({
        staticData: {
          fi: () => import('./messages/fi.json'),
          sv: () => import('./messages/sv.json'),
          en: () => import('./messages/en.json'),
        },
      })
      .init();
  } else {
    return tg
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
      })
      .init();
  }
}

export const tolgeePromise = tolgeeBase();
