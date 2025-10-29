import { deleteSuoritus, saveSuoritus } from '@/lib/suorituspalvelu-service';
import type { SuoritusFields } from '@/types/ui-types';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';

export const useSuoritusState = (
  versioTunniste: string,
  { onSuccess }: { onSuccess?: () => void } = {},
) => {
  const [suoritusState, setSuoritusState] = useState<SuoritusFields | null>(
    null,
  );

  const suoritusMutation = useMutation({
    mutationFn: async (operation: 'save' | 'delete') => {
      if (operation === 'save' && suoritusState) {
        return saveSuoritus({
          versioTunniste,
          oppijaOid: suoritusState.oppijaOid,
          oppilaitosOid: suoritusState.oppilaitosOid,
          tyyppi: suoritusState.tyyppi,
          suorituskieli: suoritusState.suorituskieli,
          yksilollistetty: suoritusState.yksilollistetty,
          valmistumispaiva: suoritusState.valmistumispaiva,
          oppiaineet: suoritusState.oppiaineet,
        });
      }
      if (operation === 'delete') {
        return deleteSuoritus(versioTunniste);
      }
    },
    onSuccess: () => {
      if (onSuccess) {
        onSuccess();
      }
    },
  });

  return {
    suoritus: suoritusState,
    setSuoritus: setSuoritusState,
    suoritusMutation,
  };
};
