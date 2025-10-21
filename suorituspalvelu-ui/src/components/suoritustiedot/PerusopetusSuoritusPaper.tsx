import { type PerusopetusSuoritus } from '@/types/ui-types';
import { PerusopetusSuoritusEditablePaper } from './PerusopetusSuoritusEditablePaper';
import { PerusopetusSuoritusReadOnlyPaper } from './PerusopetusSuoritusReadOnlyPaper';

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
    <PerusopetusSuoritusEditablePaper
      suoritus={suoritus}
      henkiloOID={henkiloOID}
    />
  ) : (
    <PerusopetusSuoritusReadOnlyPaper suoritus={suoritus} />
  );
};
