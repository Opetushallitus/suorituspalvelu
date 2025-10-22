import { type PerusopetusSuoritus } from '@/types/ui-types';
import { PerusopetusSuoritusEditor } from './PerusopetusSuoritusEditor';
import { PerusopetusSuoritusDisplay } from './PerusopetusSuoritusDisplay';

export const PerusopetusSuoritusPaper = ({
  henkiloOID,
  suoritus,
}: {
  henkiloOID: string;
  suoritus: PerusopetusSuoritus;
}) => {
  return suoritus.isEditable &&
    'suoritustyyppi' in suoritus &&
    (suoritus.suoritustyyppi === 'perusopetuksenoppimaara' ||
      suoritus.suoritustyyppi === 'perusopetuksenoppiaineenoppimaara') ? (
    <PerusopetusSuoritusEditor suoritus={suoritus} henkiloOID={henkiloOID} />
  ) : (
    <PerusopetusSuoritusDisplay suoritus={suoritus} />
  );
};
