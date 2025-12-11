import {
  type RouteConfig,
  index,
  layout,
  route,
} from '@react-router/dev/routes';

const tiedotRoutes = (id: string) => [
  route('suoritustiedot', './SuoritustiedotPage.tsx', {
    id: `${id}-suoritustiedot`,
  }),
  route('opiskelijavalinnan-tiedot', './OpiskelijavalinnanTiedotPage.tsx', {
    id: `${id}-opiskelijavalinnan-tiedot`,
  }),
];

export default [
  layout('./RootLayout.tsx', [
    index('./RootIndex.tsx'),
    route(
      '/henkilo/:oppijaNumero?',
      './HenkiloLayout.tsx',
      tiedotRoutes('henkilo'),
    ),
    route(
      '/tarkastus/:oppijaNumero?',
      './TarkastusLayout.tsx',
      tiedotRoutes('tarkastus'),
    ),
  ]),
  route('*', './NotFoundPage.tsx'),
] satisfies RouteConfig;
