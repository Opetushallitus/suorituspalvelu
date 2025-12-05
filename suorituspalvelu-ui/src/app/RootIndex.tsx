import { redirect } from 'react-router';

export const clientLoader = async () => {
  return redirect('/henkilo');
};

export default function RootPage() {
  return null;
}
