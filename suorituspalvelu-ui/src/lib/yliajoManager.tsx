import { deleteYliajo, saveYliajot } from '@/lib/suorituspalvelu-service';
import type { YliajoParams } from '@/types/ui-types';
import {
  useMutation,
  useQueryClient,
  type UseMutationResult,
} from '@tanstack/react-query';
import React, { useEffect, useMemo, useState } from 'react';
import { queryOptionsGetValintadata } from '@/lib/suorituspalvelu-queries';
import { YliajoMutationStatusIndicator } from '@/components/opiskelijavalinnan-tiedot/YliajoMutationStatusIndicator';

export type YliajoMutationOperation = 'save' | 'delete';

export type YliajoEditMode = 'add' | 'edit';

export type YliajoMutateParams = {
  operation: YliajoMutationOperation;
  yliajoParams: YliajoParams;
};

export type YliajoMutationResult = UseMutationResult<
  unknown,
  Error,
  YliajoMutateParams,
  unknown
>;

const YliajoManagerContext = React.createContext<ReturnType<
  typeof useYliajoManagerState
> | null>(null);

export const YliajoManagerProvider = ({
  children,
  hakuOid,
}: {
  children: React.ReactNode;
  hakuOid: string;
}) => {
  const yliajoState = useYliajoManagerState({ hakuOid });
  return (
    <YliajoManagerContext value={yliajoState}>
      <YliajoMutationStatusIndicator
        operation={yliajoState.operation}
        mutation={yliajoState.yliajoMutation}
      />
      {children}
    </YliajoManagerContext>
  );
};

const EMPTY_YLIAJO: YliajoParams = {
  avain: '',
  arvo: '',
  selite: '',
} as const;

const useYliajoManagerState = ({ hakuOid }: { hakuOid: string }) => {
  const [henkiloOid, setHenkiloOid] = useState<string | undefined>(undefined);

  // Muokattavat yliajotiedot. Jos null, ei olla muokkaustilassa.
  const [yliajoState, setYliajoState] = useState<YliajoParams | null>(null);

  const [mutationOperation, setMutationOperation] =
    useState<YliajoMutationOperation | null>(null);

  const [mode, setMode] = useState<YliajoEditMode>('add');

  const queryClient = useQueryClient();

  const yliajoMutation = useMutation({
    mutationFn: async ({ operation, yliajoParams }: YliajoMutateParams) => {
      setMutationOperation(operation);
      if (!henkiloOid) {
        throw new Error('HenkiloOid puuttuu! Ei voida tallentaa yliajoa.');
      }

      if (operation === 'delete') {
        return deleteYliajo({
          henkiloOid,
          hakuOid,
          avain: yliajoParams.avain,
        });
      } else if (operation === 'save') {
        return saveYliajot({
          hakuOid,
          henkiloOid,
          yliajot: [
            {
              arvo: yliajoParams.arvo,
              avain: yliajoParams.avain,
              selite: yliajoParams.selite,
            },
          ],
        });
      }
    },
    onSuccess: () => {
      setYliajoState(null);
      if (henkiloOid) {
        queryClient.resetQueries(
          queryOptionsGetValintadata({ oppijaNumero: henkiloOid, hakuOid }),
        );
      }
    },
  });

  return useMemo(() => {
    return {
      yliajoFields: yliajoState,
      mode,
      operation: mutationOperation,
      yliajoMutation,
      setHenkiloOid,
      startYliajoEdit: (yliajoParams: YliajoParams) => {
        yliajoMutation.reset();
        setMode('edit');
        setYliajoState({
          arvo: yliajoParams.arvo,
          avain: yliajoParams.avain,
          selite: yliajoParams.selite,
        });
      },
      startYliajoAdd: () => {
        yliajoMutation.reset();
        setMode('add');
        setYliajoState(EMPTY_YLIAJO);
      },
      stopYliajoEdit: () => {
        setYliajoState(null);
      },
      onYliajoChange: (updatedFields: Partial<YliajoParams>) => {
        setYliajoState((prev) => (prev ? { ...prev, ...updatedFields } : prev));
      },
      saveYliajo: (yliajoParams: YliajoParams) => {
        yliajoMutation.mutate({ operation: 'save', yliajoParams });
      },
      deleteYliajo: (avain: string) => {
        yliajoMutation.mutate({
          operation: 'delete',
          yliajoParams: { avain, arvo: '', selite: '' },
        });
      },
    };
  }, [yliajoState, yliajoMutation, mode, mutationOperation]);
};

export const useYliajoManager = (params?: { henkiloOid?: string }) => {
  const context = React.use(YliajoManagerContext);
  if (!context) {
    throw new Error(
      'useYliajoManager must be used within a YliajoManagerProvider',
    );
  }

  useEffect(() => {
    if (params?.henkiloOid) {
      context.setHenkiloOid(params.henkiloOid);
    }
  }, [context, params?.henkiloOid]);

  return context;
};
