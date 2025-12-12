import { deleteSuoritus, saveSuoritus } from '@/lib/suorituspalvelu-service';
import {
  type PerusopetuksenOppiaineenOppimaarat,
  type PerusopetuksenOppimaara,
  type PerusopetusOppiaineFields,
  type SuoritusFields,
} from '@/types/ui-types';
import {
  useMutation,
  useQueryClient,
  type UseMutationResult,
} from '@tanstack/react-query';
import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  queryOptionsGetOppija,
  queryOptionsGetValintadata,
} from '@/lib/suorituspalvelu-queries';
import { SuoritusMutationStatusIndicator } from '@/components/SuoritusMutationStatusIndicator';
import { useGlobalConfirmationModal } from '@/components/ConfirmationModal';
import { useTranslations } from '@/hooks/useTranslations';
import { useConfirmNavigation } from '@/hooks/useConfirmNavigation';

export type SuoritusMutationOperation = 'save' | 'delete';

export type SuoritusEditMode = 'new' | 'existing';

export type SuoritusMutateParams = {
  operation: SuoritusMutationOperation;
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
        operation={suoritusState.operation}
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
    tila: 'VALMIS',
    valmistumispaiva: new Date(),
    suorituskieli: 'FI',
    luokka: '',
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
  suoritus: PerusopetuksenOppimaara | PerusopetuksenOppiaineenOppimaarat;
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
    luokka: 'luokka' in suoritus ? suoritus.luokka : '',
  };
};

const useSuoritusManagerState = () => {
  const { t } = useTranslations();

  const [isDirty, setIsDirty] = useState(false);

  const [oppijaOid, setOppijaOid] = useState<string | undefined>(undefined);

  // Muokattavat suoritustiedot. Jos null, ei olla muokkaustilassa.
  const [suoritusState, setSuoritusState] = useState<SuoritusFields | null>(
    null,
  );

  const [mutationOperation, setMutationOperation] =
    useState<SuoritusMutationOperation | null>(null);

  const [mode, setMode] = useState<SuoritusEditMode>('new');

  const { showConfirmation } = useGlobalConfirmationModal();

  const queryClient = useQueryClient();

  const suoritusMutation = useMutation({
    mutationFn: async ({ operation, versioTunniste }: SuoritusMutateParams) => {
      setMutationOperation(operation);
      if (operation === 'delete') {
        if (!versioTunniste) {
          throw new Error(
            'Versiotunniste puuttuu! Ei voida poistaa suoritusta.',
          );
        }
        return deleteSuoritus(versioTunniste);
      } else if (operation === 'save' && suoritusState) {
        setOppijaOid(suoritusState.oppijaOid);
        return saveSuoritus({
          tila: suoritusState.tila,
          oppijaOid: suoritusState.oppijaOid,
          oppilaitosOid: suoritusState.oppilaitosOid,
          tyyppi: suoritusState.tyyppi,
          suorituskieli: suoritusState.suorituskieli,
          luokka: suoritusState.luokka,
          yksilollistetty: suoritusState.yksilollistetty,
          valmistumispaiva: suoritusState.valmistumispaiva,
          oppiaineet: suoritusState.oppiaineet,
        });
      }
    },
    onSuccess: () => {
      if (oppijaOid) {
        queryClient.resetQueries(queryOptionsGetOppija(oppijaOid));
        queryClient.resetQueries(
          queryOptionsGetValintadata({ oppijaNumero: oppijaOid }),
        );
      }
      if (mutationOperation !== 'delete') {
        setSuoritusState(null);
        setIsDirty(false);
      }
    },
  });

  useConfirmNavigation(isDirty, () => {
    setSuoritusState(null);
    setIsDirty(false);
  });

  const suoritusPaperRef = useRef<HTMLDivElement | null>(null);

  return useMemo(() => {
    const scrollToSuoritusPaper = () => {
      setTimeout(() => {
        suoritusPaperRef.current?.scrollIntoView({
          behavior: 'smooth',
        });
      }, 20);
    };

    const initializeSuoritusEditState = (
      suoritus?: PerusopetuksenOppimaara | PerusopetuksenOppiaineenOppimaarat,
    ) => {
      suoritusMutation.reset();
      setMode(suoritus ? 'existing' : 'new');
      if (oppijaOid) {
        setSuoritusState(
          suoritus
            ? createEditableSuoritusFields({ oppijaOid, suoritus })
            : createNewSuoritusFields({ oppijaOid }),
        );
      }
      setIsDirty(false);
      scrollToSuoritusPaper();
    };

    return {
      suoritusPaperRef,
      suoritusFields: suoritusState,
      mode,
      operation: mutationOperation,
      suoritusMutation,
      setOppijaOid,
      startSuoritusEdit: (
        suoritus?: PerusopetuksenOppimaara | PerusopetuksenOppiaineenOppimaarat,
      ) => {
        const newEditMode = suoritus ? 'existing' : 'new';

        if (suoritusState) {
          if (newEditMode !== mode) {
            showConfirmation({
              title:
                newEditMode === 'new'
                  ? t('muokkaus.suoritus.lisaa-uusi-muokattaessa.otsikko')
                  : t('muokkaus.suoritus.muokkaa-lisattaessa.otsikko'),
              content:
                newEditMode === 'new'
                  ? t('muokkaus.suoritus.lisaa-uusi-muokattaessa.sisalto')
                  : t('muokkaus.suoritus.muokkaa-lisattaessa.sisalto'),
              maxWidth: 'md',
              onConfirm: () => {
                initializeSuoritusEditState(suoritus);
              },
            });
          } else if (
            mode === 'existing' &&
            suoritus?.versioTunniste !== suoritusState?.versioTunniste
          ) {
            showConfirmation({
              title: t('muokkaus.suoritus.vaihda-suoritusta.otsikko'),
              content: t('muokkaus.suoritus.vaihda-suoritusta.sisalto'),
              maxWidth: 'md',
              onConfirm: () => {
                initializeSuoritusEditState(suoritus);
              },
            });
          } else {
            scrollToSuoritusPaper();
          }
        } else {
          initializeSuoritusEditState(suoritus);
        }
      },
      stopSuoritusEdit: () => {
        if (suoritusState && isDirty) {
          showConfirmation({
            title:
              mode === 'new'
                ? t('muokkaus.suoritus.peruuta-lisays.otsikko')
                : t('muokkaus.suoritus.peruuta-muokkaus.otsikko'),
            content:
              mode === 'new'
                ? t('muokkaus.suoritus.peruuta-lisays.sisalto')
                : t('muokkaus.suoritus.peruuta-muokkaus.sisalto'),
            maxWidth: 'md',
            onConfirm: () => {
              setSuoritusState(null);
              setIsDirty(false);
            },
          });
        } else {
          setSuoritusState(null);
          setIsDirty(false);
        }
      },
      onSuoritusChange: (updatedFields: Partial<SuoritusFields>) => {
        setSuoritusState((prev) =>
          prev ? { ...prev, ...updatedFields } : prev,
        );
        setIsDirty(true);
      },
      onOppiaineChange: (changedOppiaine: PerusopetusOppiaineFields) => {
        setSuoritusState((previousSuoritus) => {
          if (previousSuoritus) {
            let newOppiaineet = previousSuoritus.oppiaineet ?? [];
            const existingOppiaineIndex = newOppiaineet.findIndex(
              (oa) => oa.koodi === changedOppiaine.koodi,
            );
            if (existingOppiaineIndex === -1) {
              newOppiaineet = [...newOppiaineet, changedOppiaine];
            } else {
              newOppiaineet = [...newOppiaineet];
              newOppiaineet[existingOppiaineIndex] = changedOppiaine;
            }
            return {
              ...previousSuoritus,
              oppiaineet: newOppiaineet,
            };
          }
          return previousSuoritus;
        });
        setIsDirty(true);
      },
      saveSuoritus: () => {
        suoritusMutation.mutate({ operation: 'save' });
      },
      deleteSuoritus: (versioTunniste?: string) => {
        showConfirmation({
          title: t('muokkaus.suoritus.poisto-vahvistus'),
          onConfirm: () => {
            suoritusMutation.mutate({ operation: 'delete', versioTunniste });
          },
        });
      },
    };
  }, [
    suoritusState,
    setSuoritusState,
    suoritusMutation,
    mode,
    setOppijaOid,
    t,
    isDirty,
    showConfirmation,
    mutationOperation,
    oppijaOid,
  ]);
};

export const useSuoritusManager = (params?: { oppijaOid?: string }) => {
  const context = React.use(SuoritusManagerContext);
  if (!context) {
    throw new Error(
      'useSuoritusManager must be used within a SuoritusManagerProvider',
    );
  }

  useEffect(() => {
    if (params?.oppijaOid) {
      context.setOppijaOid(params.oppijaOid);
    }
  }, [context, params?.oppijaOid]);

  return context;
};
