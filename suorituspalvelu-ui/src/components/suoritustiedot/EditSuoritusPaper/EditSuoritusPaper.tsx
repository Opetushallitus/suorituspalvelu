import { Autocomplete, Stack, TextField, Typography } from '@mui/material';
import {
  OphButton,
  ophColors,
  OphFormFieldWrapper,
  OphInputFormField,
  OphSelectFormField,
} from '@opetushallitus/oph-design-system';
import { type Ref } from 'react';
import type {
  PerusopetusOppiaineFields,
  SuoritusFields,
} from '@/types/ui-types';
import { PaperWithTopColor } from '@/components/PaperWithTopColor';
import { useTranslations } from '@/hooks/useTranslations';
import { InfoItemRow } from '@/components/InfoItemRow';
import {
  queryOptionsGetSuoritusvaihtoehdot,
  useSuoritusOppilaitosOptions,
} from '@/lib/suorituspalvelu-queries';
import { DatePicker } from '@/components/DatePicker';
import { useApiSuspenseQuery } from '@/lib/http-client';
import { EditArvosanatTable } from './EditArvosanatTable';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import type { SuoritusEditMode } from '@/lib/suoritusManager';

const OppilaitosField = ({
  label,
  value,
  onChange,
}: {
  label: string;
  value: string;
  onChange: (newValue: string) => void;
}) => {
  const { data: oppilaitoksetOptions = [], isLoading } =
    useSuoritusOppilaitosOptions();

  const { t } = useTranslations();

  return (
    <OphFormFieldWrapper
      label={label}
      sx={{ flex: 2 }}
      renderInput={({ labelId }) => {
        return (
          <Autocomplete
            options={oppilaitoksetOptions}
            loading={isLoading}
            loadingText={t('ladataan-oppilaitoksia')}
            filterOptions={(options, state) =>
              options.filter((o) => o.label.includes(state.inputValue))
            }
            value={oppilaitoksetOptions.find((o) => o.value === value) || null}
            onChange={(_, newValue) => {
              onChange(newValue?.value ?? '');
            }}
            renderInput={(params) => (
              <TextField aria-labelledby={labelId} {...params} />
            )}
          />
        );
      }}
    />
  );
};

const EditSuoritusContent = ({
  mode,
  suoritus,
  onSave,
  onCancel,
  onSuoritusChange,
  onOppiaineChange,
}: {
  mode: SuoritusEditMode;
  suoritus: SuoritusFields;
  onSave: () => void;
  onCancel?: () => void;
  onSuoritusChange: (updatedFields: Partial<SuoritusFields>) => void;
  onOppiaineChange: (changedOppiaine: PerusopetusOppiaineFields) => void;
}) => {
  const { t, translateKielistetty } = useTranslations();

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

  const tilaOptions =
    suoritusvaihtoehdot?.suoritusTilat.map((tila) => ({
      label: translateKielistetty(tila.nimi),
      value: tila.arvo,
    })) ?? [];

  return (
    <Stack sx={{ gap: 1 }}>
      <Typography variant="h5">
        {mode === 'new'
          ? t('muokkaus.suoritus.otsikko-lisaa')
          : t('muokkaus.suoritus.otsikko-muokkaa')}
      </Typography>
      <InfoItemRow slotAmount={1} spacing={2}>
        <OphSelectFormField
          name="tyyppi"
          label={t('muokkaus.suoritus.tyyppi')}
          options={suoritusTyyppiOptions}
          required={true}
          value={suoritus?.tyyppi}
          onChange={(event) => {
            onSuoritusChange({ tyyppi: event.target.value });
          }}
          sx={{ flex: 1 }}
          defaultValue={suoritus?.tyyppi}
        />
      </InfoItemRow>
      <InfoItemRow slotAmount={3} spacing={2}>
        <OppilaitosField
          label={t('muokkaus.suoritus.oppilaitos')}
          value={suoritus?.oppilaitosOid}
          onChange={(newValue) => {
            onSuoritusChange({ oppilaitosOid: newValue });
          }}
        />
        <OphSelectFormField
          label={t('muokkaus.suoritus.tila')}
          options={tilaOptions}
          required={true}
          sx={{ flex: 1 }}
          value={suoritus?.tila ?? ''}
          onChange={(event) => {
            onSuoritusChange({ tila: event.target.value });
          }}
        />
        <DatePicker
          label={t('muokkaus.suoritus.valmistumispaiva')}
          value={suoritus?.valmistumispaiva}
          onChange={(date) => {
            onSuoritusChange({ valmistumispaiva: date ?? undefined });
          }}
        />
      </InfoItemRow>
      <InfoItemRow slotAmount={3} spacing={2}>
        <OphSelectFormField
          label={t('muokkaus.suoritus.suorituskieli')}
          required={true}
          sx={{ flex: 1 }}
          options={suorituskieliOptions}
          value={suoritus?.suorituskieli}
          onChange={(event) => {
            onSuoritusChange({ suorituskieli: event.target.value });
          }}
        />
        <OphInputFormField
          label={t('muokkaus.suoritus.luokka')}
          sx={{ flex: 1 }}
          required={true}
          value={suoritus?.luokka}
          onChange={(event) => {
            onSuoritusChange({ luokka: event.target.value });
          }}
        />
        <OphSelectFormField
          label={t('muokkaus.suoritus.yksilollistetty')}
          sx={{ flex: 2 }}
          required={true}
          options={yksilollistaminenOptions}
          value={suoritus?.yksilollistetty}
          onChange={(event) => {
            onSuoritusChange({ yksilollistetty: event.target.value });
          }}
        />
      </InfoItemRow>
      <EditArvosanatTable
        suoritus={suoritus}
        suoritusvaihtoehdot={suoritusvaihtoehdot}
        onOppiaineChange={onOppiaineChange}
      />
      <Stack direction="row" sx={{ justifyContent: 'flex-end', gap: 2 }}>
        <OphButton variant="outlined" onClick={onCancel}>
          {t('peruuta')}
        </OphButton>
        <OphButton variant="contained" onClick={onSave}>
          {t('muokkaus.suoritus.tallenna')}
        </OphButton>
      </Stack>
    </Stack>
  );
};

export const EditSuoritusPaper = ({
  mode,
  suoritus,
  onSave,
  onCancel,
  onSuoritusChange,
  onOppiaineChange,
  ref,
}: {
  mode: SuoritusEditMode;
  suoritus: SuoritusFields;
  onSuoritusChange: (updatedFields: Partial<SuoritusFields>) => void;
  onOppiaineChange: (changedOppiaine: PerusopetusOppiaineFields) => void;
  onSave: () => void;
  onCancel?: () => void;
  ref: Ref<HTMLDivElement> | null;
}) => {
  return (
    <PaperWithTopColor ref={ref} topColor={ophColors.cyan2}>
      <QuerySuspenseBoundary>
        <EditSuoritusContent
          mode={mode}
          onSave={onSave}
          onCancel={onCancel}
          suoritus={suoritus}
          onSuoritusChange={onSuoritusChange}
          onOppiaineChange={onOppiaineChange}
        />
      </QuerySuspenseBoundary>
    </PaperWithTopColor>
  );
};
