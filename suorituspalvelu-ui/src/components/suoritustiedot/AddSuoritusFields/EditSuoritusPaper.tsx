import { Autocomplete, Stack, TextField } from '@mui/material';
import {
  OphButton,
  ophColors,
  OphFormFieldWrapper,
  OphSelectFormField,
} from '@opetushallitus/oph-design-system';
import { type Ref } from 'react';
import type { SuoritusFields } from '@/types/ui-types';
import { PaperWithTopColor } from '@/components/PaperWithTopColor';
import { useTranslations } from '@/hooks/useTranslations';
import { InfoItemRow } from '@/components/InfoItemRow';
import { useKoodistoOptions } from '@/lib/koodisto-queries';
import {
  queryOptionsGetSuoritusvaihtoehdot,
  useOppilaitoksetOptions,
} from '@/lib/suorituspalvelu-queries';
import { DatePicker } from '@/components/DatePicker';
import { useApiSuspenseQuery } from '@/lib/http-client';
import { EditArvosanatTable } from './EditArvosanatTable';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';

const EditSuoritusContent = ({
  suoritus,
  onSave,
  setSuoritus,
}: {
  suoritus: SuoritusFields;
  onSave: () => void;
  setSuoritus: React.Dispatch<React.SetStateAction<SuoritusFields | null>>;
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
      <InfoItemRow slotAmount={1} spacing={2}>
        <OphSelectFormField
          name="tyyppi"
          label={t('muokkaus.suoritus.tyyppi')}
          options={suoritusTyyppiOptions}
          required={true}
          value={suoritus?.tyyppi}
          onChange={(event) => {
            setSuoritus((prev) =>
              prev ? { ...prev, tyyppi: event.target.value } : null,
            );
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
                  setSuoritus((prev) =>
                    prev
                      ? { ...prev, oppilaitosOid: newValue?.value ?? '' }
                      : null,
                  );
                }}
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
          value={suoritus?.tila}
          onChange={(event) => {
            setSuoritus((prev) =>
              prev ? { ...prev, tila: event.target.value } : null,
            );
          }}
        />
        <DatePicker
          label={t('muokkaus.suoritus.valmistumispaiva')}
          name="valmistumispaiva"
          value={suoritus?.valmistumispaiva}
          onChange={(date) => {
            setSuoritus((prev) =>
              prev ? { ...prev, valmistumispaiva: date ?? undefined } : null,
            );
          }}
        />
      </InfoItemRow>
      <InfoItemRow slotAmount={3} spacing={2}>
        <OphSelectFormField
          name="suorituskieli"
          label={t('muokkaus.suoritus.suorituskieli')}
          sx={{ flex: 1, maxWidth: '300px' }}
          required={true}
          options={suorituskieliOptions}
          value={suoritus?.suorituskieli}
          onChange={(event) => {
            setSuoritus((prev) =>
              prev ? { ...prev, suorituskieli: event.target.value } : null,
            );
          }}
        />
        <OphSelectFormField
          name="koulusivistyskieli"
          label={t('muokkaus.suoritus.koulusivistyskieli')}
          sx={{ flex: 1, maxWidth: '300px' }}
          required={true}
          options={suorituskieliOptions}
          value={suoritus?.koulusivistyskieli}
          onChange={(event) => {
            setSuoritus((prev) =>
              prev ? { ...prev, koulusivistyskieli: event.target.value } : null,
            );
          }}
        />
        <OphSelectFormField
          name="yksilollistetty"
          label={t('muokkaus.suoritus.yksilollistetty')}
          sx={{ flex: 2 }}
          required={true}
          options={yksilollistaminenOptions}
          value={suoritus?.yksilollistetty}
          onChange={(event) => {
            setSuoritus((prev) =>
              prev ? { ...prev, yksilollistetty: event.target.value } : null,
            );
          }}
        />
      </InfoItemRow>
      <EditArvosanatTable
        suoritus={suoritus}
        suoritusvaihtoehdot={suoritusvaihtoehdot}
        setSuoritus={setSuoritus}
      />
      <Stack direction="row" sx={{ justifyContent: 'flex-end', gap: 2 }}>
        <OphButton variant="outlined" onClick={() => setSuoritus(null)}>
          {t('muokkaus.poista')}
        </OphButton>
        <OphButton variant="contained" onClick={onSave}>
          {t('muokkaus.tallenna')}
        </OphButton>
      </Stack>
    </Stack>
  );
};

export const EditSuoritusPaper = ({
  suoritus,
  onSave,
  setSuoritus,
  ref,
}: {
  suoritus: SuoritusFields | null;
  setSuoritus: React.Dispatch<React.SetStateAction<SuoritusFields | null>>;
  onSave: () => void;
  ref: Ref<HTMLDivElement> | null;
}) => {
  return (
    suoritus && (
      <PaperWithTopColor ref={ref} topColor={ophColors.cyan2}>
        <QuerySuspenseBoundary>
          <EditSuoritusContent
            onSave={onSave}
            suoritus={suoritus}
            setSuoritus={setSuoritus}
          />
        </QuerySuspenseBoundary>
      </PaperWithTopColor>
    )
  );
};
