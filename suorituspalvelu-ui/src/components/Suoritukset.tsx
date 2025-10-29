import {
  Autocomplete,
  Box,
  Stack,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
} from '@mui/material';
import {
  OphButton,
  OphFormFieldWrapper,
  OphSelect,
  OphSelectFormField,
  OphTypography,
} from '@opetushallitus/oph-design-system';
import { useTranslate } from '@tolgee/react';
import { SuorituksetKoulutustyypeittain } from './SuorituksetKoulutustyypeittain';
import { useRef, useState, type Ref } from 'react';
import type {
  OppijanTiedot,
  SuoritusFields,
  Suoritusvaihtoehdot,
} from '@/types/ui-types';
import { SuorituksetAikajarjestyksessa } from './SuorituksetAikajarjestyksessa';
import { Add } from '@mui/icons-material';
import { PaperWithTopColor } from './PaperWithTopColor';
import { useTranslations } from '@/hooks/useTranslations';
import { InfoItemRow } from './InfoItemRow';
import { StripedTable } from './StripedTable';
import { styled } from '@/lib/theme';
import { useKoodistoOptions } from '@/lib/koodisto-queries';
import {
  queryOptionsGetSuoritusvaihtoehdot,
  useOppilaitoksetOptions,
} from '@/lib/suorituspalvelu-queries';
import { QuerySuspenseBoundary } from './QuerySuspenseBoundary';
import { Form } from 'react-router';
import { DatePicker } from './DatePicker';
import { useApiSuspenseQuery } from '@/lib/http-client';
import { useMutation } from '@tanstack/react-query';
import { saveSuoritus } from '@/lib/suorituspalvelu-service';

type SuoritusOrder = 'koulutustyypeittain' | 'uusin-ensin';

type SelectOption = {
  label: string;
  value: string;
};

const StyledSelect = styled(OphSelect)({
  minWidth: '180px',
  maxWidth: '300px',
});

const ArvosanaSelect = ({ name }: { name: string }) => {
  return <StyledSelect options={useKoodistoOptions('arvosanat')} name={name} />;
};

const ArvosanaRow = ({
  name,
  title,
  lisatietoOptions,
}: {
  name: string;
  title: string;
  lisatietoOptions?: Array<SelectOption>;
}) => {
  return (
    <TableRow>
      <TableCell>{title}</TableCell>
      <TableCell>
        {lisatietoOptions && (
          <StyledSelect name={`${name}.lisatieto`} options={lisatietoOptions} />
        )}
      </TableCell>
      <TableCell>
        <ArvosanaSelect name={`${name}.arvosana`} />
      </TableCell>
      <TableCell>
        <ArvosanaSelect name={`${name}.valinnainen`} />
      </TableCell>
    </TableRow>
  );
};

const ArvosanatTable = ({
  suoritusvaihtoehdot,
}: {
  suoritusvaihtoehdot: Suoritusvaihtoehdot;
}) => {
  const { t, translateKielistetty } = useTranslations();

  const { oppiaineet, aidinkielenOppimaarat, vieraatKielet } =
    suoritusvaihtoehdot;
  return (
    <StripedTable
      sx={{
        '& .MuiTableCell-root': {
          paddingY: 1,
        },
      }}
    >
      <TableHead>
        <TableRow>
          <TableCell>{t('oppija.oppiaine')}</TableCell>
          <TableCell>{t('oppija.lisatieto-kieli')}</TableCell>
          <TableCell>{t('oppija.arvosana')}</TableCell>
          <TableCell>{t('oppija.valinnainen')}</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {oppiaineet.map((oppiaine) => {
          let lisatietoOptions: Array<SelectOption> | undefined = undefined;
          if (oppiaine.isAidinkieli) {
            lisatietoOptions = aidinkielenOppimaarat.map((am) => ({
              label: translateKielistetty(am.nimi),
              value: am.arvo,
            }));
          } else if (oppiaine.isKieli) {
            lisatietoOptions = vieraatKielet.map((vk) => ({
              label: translateKielistetty(vk.nimi),
              value: vk.arvo,
            }));
          }

          return (
            <ArvosanaRow
              key={oppiaine.arvo}
              name={oppiaine.arvo}
              title={translateKielistetty(oppiaine.nimi)}
              lisatietoOptions={lisatietoOptions}
            />
          );
        })}
      </TableBody>
    </StripedTable>
  );
};

const EMPTY_SUORITUS: SuoritusFields = {
  oppijaOid: '',
  oppilaitosOid: '',
  tyyppi: '',
  suorituskieli: '',
  yksilollistetty: '1',
  oppiaineet: [],
};

const EditSuoritusPaper = ({
  suoritus = EMPTY_SUORITUS,
  onSave,
  onDelete,
  ref,
}: {
  suoritus?: SuoritusFields;
  onSave: (suoritus: SuoritusFields) => void;
  onDelete: () => void;
  ref: Ref<HTMLDivElement> | null;
}) => {
  const { t, translateKielistetty } = useTranslations();
  const oppilaitoksetOptions = useOppilaitoksetOptions();

  const { data: suoritusvaihtoehdot } = useApiSuspenseQuery(
    queryOptionsGetSuoritusvaihtoehdot(),
  );

  const suoritusTyyppiOptions =
    suoritusvaihtoehdot?.suoritusTyypit.map((tyyppi) => ({
      label: translateKielistetty(tyyppi.nimi),
      value: tyyppi.arvo,
    })) ?? [];

  const suorituskieliOptions =
    suoritusvaihtoehdot?.suoritusKielet.map((kieli) => ({
      label: translateKielistetty(kieli.nimi),
      value: kieli.arvo,
    })) ?? [];

  const yksilollistaminenOptions =
    suoritusvaihtoehdot?.yksilollistaminen.map((y) => ({
      label: translateKielistetty(y.nimi),
      value: y.arvo.toString(),
    })) ?? [];

  const [valmistumispaiva, setValmistumispaiva] = useState<Date | null>(() =>
    suoritus?.valmistumispaiva ? new Date(suoritus.valmistumispaiva) : null,
  );
  return (
    <PaperWithTopColor ref={ref}>
      <Stack
        component={Form}
        sx={{ gap: 1 }}
        onSubmit={(event) => {
          event.preventDefault();
          const formData = new FormData(event.currentTarget);
          onSave({
            tyyppi: formData.get('tyyppi') as string,
            oppilaitosOid: formData.get('oppilaitos') as string,
            oppijaOid: suoritus?.oppijaOid || '',
            tila: formData.get('tila') as string,
            valmistumispaiva: valmistumispaiva ?? undefined,
            koulusivistyskieli: formData.get('koulusivistyskieli') as string,
            suorituskieli: formData.get('suorituskieli') as string,
            yksilollistetty: formData.get('yksilollistetty') as string,
            oppiaineet: [],
          });
        }}
      >
        <InfoItemRow slotAmount={1} spacing={2}>
          <OphSelectFormField
            name="tyyppi"
            label={t('muokkaus.suoritus.tyyppi')}
            options={suoritusTyyppiOptions}
            required={true}
            sx={{ flex: 1 }}
            defaultValue={suoritus?.tyyppi}
          />
        </InfoItemRow>
        <InfoItemRow slotAmount={3} spacing={2}>
          <OphFormFieldWrapper
            label={t('muokkaus.suoritus.oppilaitos')}
            sx={{ flex: 1 }}
            renderInput={({ labelId }) => {
              return (
                <Autocomplete
                  id={labelId}
                  options={oppilaitoksetOptions}
                  renderInput={(params) => (
                    <TextField name="oppilaitos" {...params} />
                  )}
                />
              );
            }}
          />
          <OphSelectFormField
            name="tila"
            label={t('muokkaus.suoritus.tila')}
            options={useKoodistoOptions('suorituksentila')}
            required={true}
            sx={{ flex: 1 }}
            defaultValue={suoritus?.tila}
          />
          <DatePicker
            label={t('muokkaus.suoritus.valmistumispaiva')}
            name="valmistumispaiva"
            onChange={(date) => {
              setValmistumispaiva(date);
            }}
            value={valmistumispaiva}
          />
        </InfoItemRow>
        <InfoItemRow slotAmount={3} spacing={2}>
          <OphSelectFormField
            name="suorituskieli"
            label={t('muokkaus.suoritus.suorituskieli')}
            sx={{ flex: 1 }}
            required={true}
            defaultValue={suoritus?.suorituskieli}
            options={suorituskieliOptions}
          />
          <OphSelectFormField
            name="koulusivistyskieli"
            label={t('muokkaus.suoritus.koulusivistyskieli')}
            sx={{ flex: 1 }}
            required={true}
            defaultValue={suoritus?.koulusivistyskieli}
            options={suorituskieliOptions}
          />
          <OphSelectFormField
            name="yksilollistetty"
            label={t('muokkaus.suoritus.yksilollistetty')}
            sx={{ flex: 1 }}
            required={true}
            options={yksilollistaminenOptions}
            defaultValue={suoritus?.yksilollistetty}
          />
        </InfoItemRow>
        <ArvosanatTable suoritusvaihtoehdot={suoritusvaihtoehdot} />
        <Stack direction="row" sx={{ justifyContent: 'flex-end', gap: 2 }}>
          <OphButton variant="outlined" onClick={onDelete}>
            {t('muokkaus.poista')}
          </OphButton>
          <OphButton variant="contained" type="submit">
            {t('muokkaus.tallenna')}
          </OphButton>
        </Stack>
      </Stack>
    </PaperWithTopColor>
  );
};

const EditableSuoritukset = ({
  editableSuoritukset,
  onSave,
  onDelete,
  lastRef,
}: {
  onSave: (suoditus: SuoritusFields, index: number) => void;
  onDelete: (suoritus: SuoritusFields, index: number) => void;
  editableSuoritukset: Array<SuoritusFields>;
  lastRef: Ref<HTMLDivElement> | null;
}) => {
  return (
    <Stack sx={{ marginTop: 4, gap: 4 }}>
      <QuerySuspenseBoundary>
        {editableSuoritukset.map((suoritus, index) => {
          return (
            <EditSuoritusPaper
              // eslint-disable-next-line @eslint-react/no-array-index-key
              key={index}
              suoritus={suoritus}
              onSave={(data) => {
                onSave(data, index);
              }}
              onDelete={() => {
                onDelete(suoritus, index);
              }}
              ref={index === editableSuoritukset.length - 1 ? lastRef : null}
            />
          );
        })}
      </QuerySuspenseBoundary>
    </Stack>
  );
};

export function Suoritukset({
  oppijanTiedot,
}: {
  oppijanTiedot: OppijanTiedot;
}) {
  const { t } = useTranslate();

  const [suoritusOrder, setSuoritusOrder] = useState<SuoritusOrder>(
    'koulutustyypeittain',
  );

  const [editableSuoritukset, setEditableSuoritukset] = useState<
    Array<SuoritusFields>
  >([]);

  const lastEditableSuoritusRef = useRef<HTMLDivElement | null>(null);

  const suoritusMutation = useMutation({
    mutationFn: async (suoritus: SuoritusFields) => {
      console.log('Tallennetaan suoritus:', suoritus);
      await saveSuoritus({
        oppijaOid: oppijanTiedot.henkiloOID,
        oppilaitosOid: suoritus.oppilaitosOid || '',
        tyyppi: suoritus.tyyppi,
        suorituskieli: suoritus.suorituskieli,
        yksilollistetty: suoritus.yksilollistetty,
        valmistumispaiva: suoritus.valmistumispaiva,
        oppiaineet: [],
      });
    },
    onError: (error) => {
      console.error('Suorituksen tallennus epäonnistui:', error);
    },
  });

  return (
    <Box data-test-id="suoritukset">
      <Stack
        direction="row"
        sx={{ justifyContent: 'space-between', alignItems: 'flex-start' }}
      >
        <OphTypography
          variant="h3"
          component="h2"
          sx={{ marginBottom: 2, alignSelf: 'flex-end' }}
        >
          {t('oppija.suoritukset')}
        </OphTypography>
        <ToggleButtonGroup
          sx={{ marginBottom: suoritusOrder === 'uusin-ensin' ? 2 : 0 }}
          value={suoritusOrder}
          exclusive
          onChange={(_event, newValue) => {
            if (!newValue) {
              return;
            }
            setSuoritusOrder(newValue);
          }}
        >
          <ToggleButton value="koulutustyypeittain">
            {t('oppija.koulutustyypeittain')}
          </ToggleButton>
          <ToggleButton value="uusin-ensin">
            {t('oppija.uusin-ensin')}
          </ToggleButton>
        </ToggleButtonGroup>
      </Stack>
      <Stack
        direction="row"
        sx={{ justifyContent: 'flex-start', marginTop: 2, marginBottom: 2 }}
      >
        <OphButton
          variant="outlined"
          startIcon={<Add />}
          onClick={() => {
            setEditableSuoritukset((prev) => [...prev, EMPTY_SUORITUS]);
            setTimeout(() => {
              lastEditableSuoritusRef.current?.scrollIntoView({
                behavior: 'smooth',
              });
            }, 50);
          }}
        >
          {t('muokkaus.suoritus.lisaa')}
        </OphButton>
      </Stack>
      <EditableSuoritukset
        editableSuoritukset={editableSuoritukset}
        lastRef={lastEditableSuoritusRef}
        onSave={(data) => {
          suoritusMutation.mutate(data);
        }}
        onDelete={(suoritus, i) => {
          setEditableSuoritukset((prev) => {
            const newSuoritukset = [...prev];
            newSuoritukset.splice(i, 1);
            return newSuoritukset;
          });
          console.log('deleted suoritus', suoritus, i);
        }}
      />
      {suoritusOrder === 'koulutustyypeittain' ? (
        <SuorituksetKoulutustyypeittain oppijanTiedot={oppijanTiedot} />
      ) : (
        <SuorituksetAikajarjestyksessa oppijanTiedot={oppijanTiedot} />
      )}
    </Box>
  );
}
