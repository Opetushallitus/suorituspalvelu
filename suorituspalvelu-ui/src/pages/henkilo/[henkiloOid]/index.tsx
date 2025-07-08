import { useRouter } from 'next/router';

export default function Page() {
  const router = useRouter();
  const { henkiloOid } = router.query;
  return <p>Henkilo: {henkiloOid}</p>;
}
