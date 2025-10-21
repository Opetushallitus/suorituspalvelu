import { deleteSuoritus, saveSuoritus } from '@/lib/suorituspalvelu-service';
import type { SuoritusFields } from '@/types/ui-types';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';

type SuoritusOperation = 'save' | 'delete';

type SuoritusMutateParams = {
  operation: SuoritusOperation;
  versioTunniste?: string;
};

export const useSuoritusState = ({
  onSuccess,
}: { onSuccess?: () => void } = {}) => {
  const [suoritusState, setSuoritusState] = useState<SuoritusFields | null>(
    null,
  );

  const [mode, setMode] = useState<SuoritusOperation>('save');

  const suoritusMutation = useMutation({
    mutationFn: async ({ operation, versioTunniste }: SuoritusMutateParams) => {
      if (operation === 'save' && suoritusState) {
        setMode('save');
        return saveSuoritus({
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
        if (!versioTunniste) {
          throw new Error(
            'Versiotunniste puuttuu! Ei voida poistaa suoritusta.',
          );
        }
        setMode('delete');
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
    mode,
  };
};
