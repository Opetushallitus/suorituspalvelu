import { Autocomplete, Stack, TextField } from '@mui/material';
import {
  OphButton,
  ophColors,
  OphFormFieldWrapper,
  OphSelectFormField,
} from '@opetushallitus/oph-design-system';
import { useState, type Ref } from 'react';
import type { SuoritusFields } from '@/types/ui-types';
import { PaperWithTopColor } from '@/components/PaperWithTopColor';
import { useTranslations } from '@/hooks/useTranslations';
import { InfoItemRow } from '@/components/InfoItemRow';
import { useKoodistoOptions } from '@/lib/koodisto-queries';
import {
  queryOptionsGetSuoritusvaihtoehdot,
  useOppilaitoksetOptions,
} from '@/lib/suorituspalvelu-queries';
import { Form } from 'react-router';
import { DatePicker } from '@/components/DatePicker';
import { useApiSuspenseQuery } from '@/lib/http-client';
import { EditArvosanatTable } from './EditArvosanatTable';

export const EMPTY_SUORITUS: SuoritusFields = {
  oppijaOid: '',
  oppilaitosOid: '',
  tyyppi: '',
  suorituskieli: '',
  yksilollistetty: '1',
  oppiaineet: [],
};

export const EditSuoritusPaper = ({
  suoritus = EMPTY_SUORITUS,
  onSave,
  onDelete,
  ref,
}: {
  suoritus: SuoritusFields;
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
    <PaperWithTopColor ref={ref} topColor={ophColors.cyan2}>
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
        <EditArvosanatTable suoritusvaihtoehdot={suoritusvaihtoehdot} />
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
