import { type PerusopetusSuoritus } from '@/types/ui-types';
import { PerusopetusSuoritusEditor } from './PerusopetusSuoritusEditor';
import { PerusopetusSuoritusDisplay } from './PerusopetusSuoritusDisplay';
import { useKayttaja } from '@/lib/suorituspalvelu-queries';

export const PerusopetusSuoritusPaper = ({
  henkiloOID,
  suoritus,
}: {
  henkiloOID: string;
  suoritus: PerusopetusSuoritus;
}) => {
  const { data: kayttaja } = useKayttaja();

  return kayttaja.isRekisterinpitaja &&
    suoritus.isEditable &&
    'suoritustyyppi' in suoritus &&
    (suoritus.suoritustyyppi === 'perusopetuksenoppimaara' ||
      suoritus.suoritustyyppi === 'perusopetuksenoppiaineenoppimaara') ? (
    <PerusopetusSuoritusEditor suoritus={suoritus} henkiloOID={henkiloOID} />
  ) : (
    <PerusopetusSuoritusDisplay suoritus={suoritus} />
  );
};
