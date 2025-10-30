import { redirect, type ClientLoaderFunctionArgs } from 'react-router';

export const clientLoader = ({ request }: ClientLoaderFunctionArgs) => {
  const url = new URL(request.url);
  return redirect(`suoritustiedot${url.search}`);
};

// This component should never render due to the redirect,
// but React Router requires a component to be exported
export default function HenkiloPageIndex() {
  return null;
}
