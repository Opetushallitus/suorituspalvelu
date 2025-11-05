import { Box, Stack, ToggleButton, ToggleButtonGroup } from '@mui/material';
import {
  OphButton,
  ophColors,
  OphFormFieldWrapper,
  OphInput,
  OphTypography,
} from '@opetushallitus/oph-design-system';
import { FetchError, useApiSuspenseQuery } from '@/lib/http-client';
import { queryOptionsGetValintadata } from '@/lib/suorituspalvelu-queries';
import type { Route } from './+types/OpiskelijavalinnanTiedotPage';
import { groupBy, mapValues, pipe, prop, sortBy } from 'remeda';
import { useTranslations } from '@/hooks/useTranslations';
import { AccordionBox } from '@/components/AccordionBox';
import React, { useCallback, useMemo, useState } from 'react';
import {
  isGenericBackendErrorResponse,
  type AvainArvo,
  type YliajoParams,
} from '@/types/ui-types';
import { styled } from '@/lib/theme';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { ErrorView } from '@/components/ErrorView';
import { ErrorAlert } from '@/components/ErrorAlert';
import { Add, EditOutlined } from '@mui/icons-material';
import { OphModal } from '@/components/OphModal';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { saveYliajot } from '@/lib/suorituspalvelu-service';
import { SpinnerModal } from '@/components/SpinnerModal';

const OPISKELIJAVALINTADATA_GROUPS = [
  'yleinen',
  'suoritukset',
  'lisapistekoulutukset',
  'perusopetuksen-oppiaineet',
  'lisatyt',
] as const;

type AvainarvoRyhma = 'uudet-avainarvot' | 'vanhat-avainarvot';

type OpiskelijavalintaDataGroups =
  (typeof OPISKELIJAVALINTADATA_GROUPS)[number];

const getOpiskelijavalintaGroup = (
  item: AvainArvo,
): OpiskelijavalintaDataGroups => {
  const key = item.avain;
  if (key.startsWith('PK_ARVOSANA') || key.startsWith('PERUSKOULU_ARVOSANA')) {
    return 'perusopetuksen-oppiaineet';
  } else if (
    key.startsWith('lisapistekoulutus_') ||
    key.startsWith('LISAKOULUTUS_')
  ) {
    return 'lisapistekoulutukset';
  } else if (
    key.endsWith('_suoritettu') ||
    key.toLowerCase().endsWith('_suoritusvuosi') ||
    key.endsWith('_TILA')
  ) {
    return 'suoritukset';
  } else if (item.metadata.arvoEnnenYliajoa == null) {
    return 'lisatyt';
  } else {
    return 'yleinen';
  }
};

const sortGroups = (
  groups: Partial<
    Record<OpiskelijavalintaDataGroups, Array<AvainArvo> | undefined>
  >,
) => {
  return mapValues(groups, (items, group) => {
    const alphabeticallySorted = sortBy(items ?? [], prop('avain'));
    if (group === 'perusopetuksen-oppiaineet') {
      return alphabeticallySorted.sort((a, b) => {
        // Järjestetään oppiaine ennen sen arvosanaa
        const aIsBsOppiaine =
          a.avain.includes('_OPPIAINE') && a.avain.includes(b.avain);
        const bIsAsOppiaine =
          b.avain.includes('_OPPIAINE') && b.avain.includes(a.avain);

        if (aIsBsOppiaine) {
          return -1;
        } else if (bIsAsOppiaine) {
          return 1;
        } else {
          return 0;
        }
      });
    } else if (group === 'suoritukset') {
      const order = [
        'perustutkinto_suoritettu',
        'PK_TILA',
        'peruskoulu_suoritusvuosi',
        'PK_SUORITUSVUOSI',
        'ammatillinen_suoritettu',
        'AM_TILA',
        'lukio_suoritettu',
        'LK_TILA',
        'yo-tutkinto_suoritettu',
        'YO_TILA',
      ];
      return alphabeticallySorted.sort((a, b) => {
        const aIndex = order.indexOf(a.avain);
        const bIndex = order.indexOf(b.avain);
        if (aIndex === -1 && bIndex === -1) return 0;
        if (aIndex === -1) return 1;
        if (bIndex === -1) return -1;
        return aIndex - bIndex;
      });
    }
    return alphabeticallySorted;
  });
};

const BreakFlex = styled('div')({
  flexBasis: '100%',
  height: 0,
});

const EditableField = ({
  avainArvo,
  startYliajoEdit,
}: {
  avainArvo: AvainArvo;
  startYliajoEdit?: (avainarvo: {
    avain: string;
    arvo: string;
    selite: string;
  }) => void;
}) => {
  const { t } = useTranslations();
  const labelId = `avainarvo-label-${avainArvo.avain}`;

  const alkuperainenArvo = avainArvo.metadata.arvoEnnenYliajoa;

  return (
    <Box
      key={avainArvo.avain}
      sx={{
        display: 'flex',
        flexDirection: 'column',
        flex: '0 0 calc(50% - 16px)',
        gap: 0,
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center' }}>
        <OphTypography id={labelId} variant="label">
          {avainArvo.avain}
        </OphTypography>
        {startYliajoEdit && (
          <OphButton
            variant="text"
            onClick={() =>
              startYliajoEdit?.({
                arvo: avainArvo.arvo,
                avain: avainArvo.avain,
                selite: avainArvo.metadata.yliajo?.selite ?? '',
              })
            }
            startIcon={<EditOutlined />}
          />
        )}
      </Box>
      <OphTypography aria-labelledby={labelId}>
        {avainArvo.arvo}{' '}
        {alkuperainenArvo
          ? `(${t('opiskelijavalinnan-tiedot.alkuperainen')}: ${alkuperainenArvo})`
          : ''}
      </OphTypography>
    </Box>
  );
};

const AvainArvotSection = ({
  avainarvot,
  avainArvoFilter,
  startYliajoEdit,
}: {
  avainarvot: Array<AvainArvo>;
  avainArvoFilter: (avainArvo: AvainArvo) => boolean;
  startYliajoEdit?: (avainarvo: {
    avain: string;
    arvo: string;
    selite: string;
  }) => void;
}) => {
  const { t } = useTranslations();

  const groupedAvainarvot = useMemo(
    () =>
      pipe(
        avainarvot,
        ($) => $.filter(avainArvoFilter),
        groupBy((item) => getOpiskelijavalintaGroup(item)),
        (grouped) => sortGroups(grouped),
      ),
    [avainarvot, avainArvoFilter],
  );

  return (
    <Stack sx={{ gap: 3 }}>
      {OPISKELIJAVALINTADATA_GROUPS.map((group) => {
        const items = groupedAvainarvot[group];
        return (
          items && (
            <Box key={group}>
              {group !== 'yleinen' && (
                <Box
                  sx={{
                    width: '100%',
                    borderBottom: `1px solid ${ophColors.grey300}`,
                    marginBottom: 2,
                  }}
                >
                  <OphTypography variant="h5">
                    {t(`opiskelijavalinnan-tiedot.ryhmat.${group}`)}
                  </OphTypography>
                </Box>
              )}
              <Stack
                direction="row"
                sx={{
                  maxWidth: '100%',
                  flexWrap: 'wrap',
                  justifyContent: 'flex-start',
                  gap: 2,
                }}
              >
                {items?.map((avainArvo, index) => (
                  <React.Fragment key={avainArvo.avain}>
                    {avainArvo.avain.includes('_OPPIAINE') && index !== 0 && (
                      <BreakFlex />
                    )}
                    <EditableField
                      avainArvo={avainArvo}
                      startYliajoEdit={startYliajoEdit}
                    />
                  </React.Fragment>
                ))}
              </Stack>
            </Box>
          )
        );
      })}
    </Stack>
  );
};

type YliajoMode = 'add' | 'edit';

const YliajoErrorModal = ({
  error,
  onClose,
}: {
  error: Error;
  onClose: () => void;
}) => {
  const { t } = useTranslations();

  let message: React.ReactNode = error.message;

  if (error instanceof FetchError) {
    const responseJSON = error.jsonBody;
    if (isGenericBackendErrorResponse(responseJSON)) {
      message = (
        <Box>
          {responseJSON.virheAvaimet.map((virhe) => (
            <p key={virhe}>{t(virhe)}</p>
          ))}
        </Box>
      );
    }
  }

  return (
    <OphModal
      open={true}
      onClose={onClose}
      title={t('opiskelijavalinnan-tiedot.yliajon-tallennus-epaonnistui')}
    >
      {message}
    </OphModal>
  );
};

const YliajoEditModal = ({
  mode,
  yliajo,
  setYliajo,
  saveYliajo,
}: {
  mode: YliajoMode;
  henkiloOid: string;
  yliajo: YliajoParams | null;
  setYliajo: (yliajo: YliajoParams | null) => void;
  saveYliajo: (updatedYliajo: YliajoParams) => void;
}) => {
  const { t } = useTranslations();

  const onClose = useCallback(() => {
    setYliajo(null);
  }, [setYliajo]);

  return (
    yliajo && (
      <OphModal
        open={yliajo !== null}
        onClose={onClose}
        title={
          mode === 'add'
            ? t('opiskelijavalinnan-tiedot.lisaa-kentta')
            : t('opiskelijavalinnan-tiedot.muokkaa-kenttaa')
        }
        maxWidth="sm"
        actions={
          <>
            <OphButton variant="outlined" onClick={onClose}>
              {t('peruuta')}
            </OphButton>
            <OphButton
              variant="contained"
              onClick={() => {
                if (yliajo) {
                  saveYliajo(yliajo);
                }
              }}
            >
              {t('opiskelijavalinnan-tiedot.tallenna')}
            </OphButton>
          </>
        }
      >
        <Stack sx={{ alignItems: 'flex-start', gap: 2, overflow: 'visible' }}>
          {mode === 'add' && (
            <OphFormFieldWrapper
              label={t('opiskelijavalinnan-tiedot.avain')}
              sx={{ minWidth: '50%', overflow: 'visible' }}
              renderInput={() => (
                <OphInput
                  value={yliajo?.avain ?? ''}
                  onChange={(event) => {
                    setYliajo({
                      ...yliajo,
                      avain: event.target.value ?? '',
                    });
                  }}
                />
              )}
            />
          )}
          <OphFormFieldWrapper
            label={
              mode === 'add'
                ? t('opiskelijavalinnan-tiedot.arvo')
                : yliajo?.avain
            }
            sx={{ minWidth: '50%', overflow: 'visible' }}
            renderInput={() => (
              <OphInput
                value={yliajo?.arvo ?? ''}
                onChange={(event) => {
                  setYliajo({
                    ...yliajo,
                    arvo: event.target.value ?? '',
                  });
                }}
              />
            )}
          />
          <OphFormFieldWrapper
            sx={{ alignSelf: 'stretch' }}
            label={t('opiskelijavalinnan-tiedot.selite')}
            helperText={t('opiskelijavalinnan-tiedot.selite-aputeksti')}
            renderInput={() => (
              <OphInput
                value={yliajo?.selite ?? ''}
                onChange={(event) => {
                  setYliajo({
                    ...yliajo,
                    selite: event.target.value ?? '',
                  });
                }}
                multiline={true}
                minRows={3}
              />
            )}
          />
        </Stack>
      </OphModal>
    )
  );
};

const DUMMY_HAKU_OID = '1.2.246.562.29.00000000000000000000';

const EMPTY_YLIAJO = {
  avain: '',
  arvo: '',
  selite: '',
} as const;

const OpiskelijavalinnanTiedotPageContent = ({
  oppijaNumero,
}: {
  oppijaNumero: string;
}) => {
  const { data: valintadata } = useApiSuspenseQuery(
    queryOptionsGetValintadata({ oppijaNumero, hakuOid: DUMMY_HAKU_OID }),
  );

  const { t } = useTranslations();

  const [mode, setMode] = useState<YliajoMode>('edit');

  const [avainarvoRyhma, setAvainarvoRyhma] =
    useState<AvainarvoRyhma>('uudet-avainarvot');

  const [yliajo, setYliajo] = useState<YliajoParams | null>(null);

  const queryClient = useQueryClient();

  const yliajoMutation = useMutation({
    mutationFn: (updatedYliajo: YliajoParams) => {
      return saveYliajot({
        hakuOid: DUMMY_HAKU_OID,
        henkiloOid: oppijaNumero,
        virkailijaOid: oppijaNumero,
        yliajot: [
          {
            arvo: updatedYliajo.arvo,
            avain: updatedYliajo.avain,
            selite: updatedYliajo.selite,
          },
        ],
      });
    },
    onSuccess: () => {
      setYliajo(null);
      queryClient.resetQueries(
        queryOptionsGetValintadata({ oppijaNumero, hakuOid: DUMMY_HAKU_OID }),
      );
    },
  });

  return (
    <Stack spacing={3}>
      {yliajoMutation.isPending ? (
        <SpinnerModal
          open={yliajoMutation.isPending}
          title={t('opiskelijavalinnan-tiedot.tallennetaan-yliajoa')}
        />
      ) : yliajoMutation.isError ? (
        <YliajoErrorModal
          error={yliajoMutation.error}
          onClose={() => yliajoMutation.reset()}
        />
      ) : (
        <YliajoEditModal
          mode={mode}
          henkiloOid={oppijaNumero}
          yliajo={yliajo}
          setYliajo={setYliajo}
          saveYliajo={yliajoMutation.mutate}
        />
      )}
      <ToggleButtonGroup
        sx={{ alignSelf: 'flex-end' }}
        value={avainarvoRyhma}
        exclusive
        onChange={(_event, newValue) => {
          if (newValue) {
            setAvainarvoRyhma(newValue);
          }
        }}
      >
        <ToggleButton value="uudet-avainarvot">
          {t('opiskelijavalinnan-tiedot.uudet-avainarvot')}
        </ToggleButton>
        <ToggleButton value="vanhat-avainarvot">
          {t('opiskelijavalinnan-tiedot.vanhat-avainarvot')}
        </ToggleButton>
      </ToggleButtonGroup>
      <AccordionBox
        id="opiskelijavalintaan-siirtyvat-tiedot"
        title={t(
          'opiskelijavalinnan-tiedot.suorituspalvelusta-opiskelijavalintaan-siirtyvat-tiedot',
        )}
      >
        <AvainArvotSection
          avainarvot={valintadata.avainArvot}
          startYliajoEdit={
            avainarvoRyhma === 'uudet-avainarvot'
              ? (yliajoParams) => {
                  setMode('edit');
                  setYliajo({
                    arvo: yliajoParams.arvo,
                    avain: yliajoParams.avain,
                    selite: yliajoParams.selite,
                  });
                }
              : undefined
          }
          avainArvoFilter={(avainArvo) =>
            avainarvoRyhma === 'uudet-avainarvot'
              ? !avainArvo.metadata.duplikaatti
              : avainArvo.metadata.duplikaatti
          }
        />
        <Stack direction="row" sx={{ justifyContent: 'flex-end' }}>
          <OphButton
            startIcon={<Add />}
            variant="outlined"
            onClick={() => {
              setMode('add');
              setYliajo(EMPTY_YLIAJO);
            }}
          >
            {t('opiskelijavalinnan-tiedot.lisaa-kentta')}
          </OphButton>
        </Stack>
      </AccordionBox>
    </Stack>
  );
};

const ErrorFallback = ({
  reset,
  error,
}: {
  reset: () => void;
  error: Error;
}) => {
  const { t } = useTranslations();
  if (error instanceof FetchError) {
    const jsonBody = error.jsonBody;
    if (isGenericBackendErrorResponse(jsonBody)) {
      return (
        <ErrorAlert
          title={t('opiskelijavalinnan-tiedot.virhe-valintadatan-haussa')}
          message={jsonBody.virheAvaimet.map((virhe) => t(virhe))}
        />
      );
    }
  }

  return <ErrorView error={error} reset={reset} />;
};

export default function OpiskelijavalinnanTiedotPage({
  params,
}: Route.ComponentProps) {
  return (
    <QuerySuspenseBoundary ErrorFallback={ErrorFallback}>
      <OpiskelijavalinnanTiedotPageContent oppijaNumero={params.oppijaNumero} />
    </QuerySuspenseBoundary>
  );
}
