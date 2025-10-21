import { deleteSuoritus, saveSuoritus } from '@/lib/suorituspalvelu-service';
import {
  type PerusopetuksenOppiaineenOppimaara,
  type PerusopetuksenOppimaara,
  type PerusopetusOppiaineFields,
  type SuoritusFields,
} from '@/types/ui-types';
import {
  useMutation,
  useQueryClient,
  type UseMutationResult,
} from '@tanstack/react-query';
import React, { useEffect, useMemo, useState } from 'react';
import { queryOptionsGetOppija } from '@/lib/suorituspalvelu-queries';
import { SuoritusMutationStatusIndicator } from '@/components/SuoritusMutationStatusIndicator';
import { useGlobalConfirmationModal } from '@/components/ConfirmationModal';
import { useTranslations } from '@/hooks/useTranslations';

export type SuoritusOperation = 'add' | 'edit' | 'delete';

export type SuoritusMutateParams = {
  operation: SuoritusOperation;
  versioTunniste?: string;
};

export type SuoritusMutationResult = UseMutationResult<
  unknown,
  Error,
  SuoritusMutateParams,
  unknown
>;

const SuoritusManagerContext = React.createContext<ReturnType<
  typeof useSuoritusManagerState
> | null>(null);

export const SuoritusManagerProvider = ({
  children,
}: {
  children: React.ReactNode;
}) => {
  const suoritusState = useSuoritusManagerState();
  return (
    <SuoritusManagerContext value={suoritusState}>
      <SuoritusMutationStatusIndicator
        mode={suoritusState.mode}
        mutation={suoritusState.suoritusMutation}
      />
      {children}
    </SuoritusManagerContext>
  );
};

const createNewSuoritusFields = (
  base: Partial<SuoritusFields> = {},
): SuoritusFields => {
  return {
    versioTunniste: '',
    oppijaOid: '',
    oppilaitosOid: '',
    tyyppi: 'perusopetuksenoppimaara',
    tila: 'suorituksentila_valmis',
    suorituskieli: 'FI',
    yksilollistetty: '1',
    oppiaineet: [],
    ...base,
  };
};

const createEditableSuoritusFields = ({
  oppijaOid,
  suoritus,
}: {
  oppijaOid: string;
  suoritus: PerusopetuksenOppimaara | PerusopetuksenOppiaineenOppimaara;
}): SuoritusFields => {
  return {
    versioTunniste:
      'versioTunniste' in suoritus && suoritus.versioTunniste
        ? suoritus.versioTunniste
        : suoritus.tunniste,
    oppijaOid,
    oppilaitosOid: suoritus.oppilaitos.oid,
    tila: suoritus.tila,
    tyyppi: suoritus.suoritustyyppi,
    valmistumispaiva: suoritus.valmistumispaiva
      ? new Date(suoritus.valmistumispaiva)
      : undefined,
    suorituskieli: suoritus.suorituskieli,
    yksilollistetty:
      'yksilollistaminen' in suoritus
        ? (suoritus.yksilollistaminen?.arvo ?? '').toString()
        : '',
    oppiaineet:
      'oppiaineet' in suoritus
        ? suoritus.oppiaineet.map((oa) => ({
            koodi: oa.koodi ?? '',
            kieli: oa.kieli,
            arvosana: oa.arvosana,
            valinnaisetArvosanat: oa.valinnaisetArvosanat,
          }))
        : [],
  };
};

const useSuoritusManagerState = () => {
  const { t } = useTranslations();
  const [oppijaOid, setOppijaOid] = useState<string | undefined>(undefined);
  const [suoritusState, setSuoritusState] = useState<SuoritusFields | null>(
    null,
  );

  const { showConfirmation } = useGlobalConfirmationModal();

  const [mode, setMode] = useState<SuoritusOperation>('add');

  const queryClient = useQueryClient();

  const suoritusMutation = useMutation({
    mutationFn: async ({ operation, versioTunniste }: SuoritusMutateParams) => {
      if ((operation === 'add' || operation === 'edit') && suoritusState) {
        setOppijaOid(suoritusState.oppijaOid);
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
        setMode('delete');
        if (!versioTunniste) {
          throw new Error(
            'Versiotunniste puuttuu! Ei voida poistaa suoritusta.',
          );
        }
        return deleteSuoritus(versioTunniste);
      }
    },
    onSuccess: () => {
      if (oppijaOid) {
        queryClient.invalidateQueries(queryOptionsGetOppija(oppijaOid));
        queryClient.refetchQueries(queryOptionsGetOppija(oppijaOid));
      }
      setSuoritusState(null);
    },
  });

  return useMemo(
    () => ({
      suoritusFields: suoritusState,
      mode,
      suoritusMutation,
      setOppijaOid,
      startSuoritusAdd: () => {
        if (!suoritusState) {
          suoritusMutation.reset();
          setMode('add');
          setSuoritusState(
            createNewSuoritusFields({
              oppijaOid,
            }),
          );
        }
      },
      startSuoritusEdit: (
        suoritus: PerusopetuksenOppimaara | PerusopetuksenOppiaineenOppimaara,
      ) => {
        suoritusMutation.reset();
        setMode('edit');
        if (oppijaOid) {
          setSuoritusState(
            createEditableSuoritusFields({ oppijaOid, suoritus }),
          );
        }
      },
      stopSuoritusModify: () => {
        setSuoritusState(null);
      },
      onSuoritusChange: (updatedFields: Partial<SuoritusFields>) => {
        setSuoritusState((prev) =>
          prev ? { ...prev, ...updatedFields } : prev,
        );
      },
      onOppiaineChange: (changedOppiaine: PerusopetusOppiaineFields) => {
        setSuoritusState((previousSuoritus) => {
          if (previousSuoritus) {
            const newOppiaineet = previousSuoritus.oppiaineet ?? [];
            const existingOppiaineIndex = newOppiaineet.findIndex(
              (oa) => oa.koodi === changedOppiaine.koodi,
            );
            if (existingOppiaineIndex === -1) {
              newOppiaineet.push(changedOppiaine);
            } else {
              newOppiaineet[existingOppiaineIndex] = changedOppiaine;
            }
            return {
              ...previousSuoritus,
              oppiaineet: newOppiaineet,
            };
          }
          return previousSuoritus;
        });
      },
      saveSuoritus: () => {
        suoritusMutation.mutate({ operation: 'edit' });
      },
      deleteSuoritus: (versioTunniste?: string) => {
        showConfirmation({
          title: t('muokkaus.poisto-vahvistus'),
          onConfirm: () => {
            suoritusMutation.mutate({ operation: 'delete', versioTunniste });
          },
        });
      },
    }),
    [suoritusState, setSuoritusState, suoritusMutation, mode, setOppijaOid, t],
  );
};

export const useSuoritusManager = ({ oppijaOid }: { oppijaOid: string }) => {
  const context = React.use(SuoritusManagerContext);
  if (!context) {
    throw new Error(
      'useSuoritusManager must be used within a SuoritusManagerProvider',
    );
  }

  useEffect(() => {
    context.setOppijaOid(oppijaOid);
  }, [oppijaOid]);

  return context;
};
