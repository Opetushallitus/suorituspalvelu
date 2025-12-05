import {
  type RouteConfig,
  index,
  layout,
  route,
} from '@react-router/dev/routes';

export default [
  layout('./RootLayout.tsx', [
    index('./RootIndex.tsx'),
    route(
      '/henkilo/:oppijaNumero?',
      './HenkiloPageLayout.tsx',
      { id: 'henkilo-layout' },
      [
        route('suoritustiedot', './SuoritustiedotPage.tsx', {
          id: 'henkilo-suoritustiedot',
        }),
        route(
          'opiskelijavalinnan-tiedot',
          './OpiskelijavalinnanTiedotPage.tsx',
          { id: 'henkilo-opiskelijavalinnan-tiedot' },
        ),
      ],
    ),
    route(
      '/tarkistus/:oppijaNumero?',
      './HenkiloPageLayout.tsx',
      { id: 'tarkistus-layout' },
      [
        route('suoritustiedot', './SuoritustiedotPage.tsx', {
          id: 'tarkistus-suoritustiedot',
        }),
        route(
          'opiskelijavalinnan-tiedot',
          './OpiskelijavalinnanTiedotPage.tsx',
          { id: 'tarkistus-opiskelijavalinnan-tiedot' },
        ),
      ],
    ),
  ]),
  route('*', './NotFoundPage.tsx'),
] satisfies RouteConfig;
