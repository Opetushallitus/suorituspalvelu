import { Autocomplete, Stack, TextField, Typography } from '@mui/material';
import {
  OphButton,
  ophColors,
  OphFormFieldWrapper,
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
  useOppilaitoksetOptions,
} from '@/lib/suorituspalvelu-queries';
import { DatePicker } from '@/components/DatePicker';
import { useApiSuspenseQuery } from '@/lib/http-client';
import { EditArvosanatTable } from './EditArvosanatTable';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import type { SuoritusEditMode } from '@/lib/suoritusManager';

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
        <OphFormFieldWrapper
          label={t('muokkaus.suoritus.oppilaitos')}
          sx={{ flex: 2 }}
          renderInput={({ labelId }) => {
            return (
              <Autocomplete
                id={labelId}
                value={
                  oppilaitoksetOptions.find(
                    (o) => o.value === suoritus?.oppilaitosOid,
                  ) || null
                }
                onChange={(_, newValue) => {
                  onSuoritusChange({ oppilaitosOid: newValue?.value ?? '' });
                }}
                options={oppilaitoksetOptions}
                renderInput={(params) => <TextField {...params} />}
              />
            );
          }}
        />
        <OphSelectFormField
          label={t('muokkaus.suoritus.tila')}
          options={[
            {
              label: t('Suoritus') + ' ' + t('suorituksen-tila.VALMIS'),
              value: 'suorituksentila_valmis',
            },
          ]}
          required={true}
          sx={{ flex: 1 }}
          defaultValue="suorituksentila_valmis"
          value="suorituksentila_valmis"
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
          sx={{ flex: 1, maxWidth: '300px' }}
          required={true}
          options={suorituskieliOptions}
          value={suoritus?.suorituskieli}
          onChange={(event) => {
            onSuoritusChange({ suorituskieli: event.target.value });
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
