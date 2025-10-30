import {
  type RouteConfig,
  index,
  layout,
  route,
} from '@react-router/dev/routes';

export default [
  layout('./HenkiloSearchLayout.tsx', [
    index('./HenkiloNotSelectedPage.tsx'),
    route('/henkilo/:oppijaNumero', './HenkiloPageLayout.tsx', [
      index('./HenkiloPageIndex.tsx'),
      route('suoritustiedot', './SuoritustiedotPage.tsx'),
      route('opiskelijavalinnan-tiedot', './OpiskelijavalinnanTiedotPage.tsx'),
    ]),
  ]),
  route('*', './NotFoundPage.tsx'),
] satisfies RouteConfig;
