import {
  type RouteConfig,
  index,
  layout,
  route,
} from '@react-router/dev/routes';

export default [
  layout('./HenkiloSearchLayout.tsx', [
    index('./HenkiloNotSelectedPage.tsx'),
    route('/henkilo/:oppijaNumero', './HenkiloPage.tsx'),
  ]),
  route('*', './NotFoundPage.tsx'),
] satisfies RouteConfig;
