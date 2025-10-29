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
import type { OppijanTiedot } from '@/types/ui-types';
import { SuorituksetAikajarjestyksessa } from './SuorituksetAikajarjestyksessa';
import { Add } from '@mui/icons-material';
import { PaperWithTopColor } from './PaperWithTopColor';
import { useTranslations } from '@/hooks/useTranslations';
import { InfoItemRow } from './InfoItemRow';
import { StripedTable } from './StripedTable';
import { styled } from '@/lib/theme';
import { useKoodistoOptions } from '@/lib/koodisto-queries';
import { useOppilaitoksetOptions } from '@/lib/suorituspalvelu-queries';
import { QuerySuspenseBoundary } from './QuerySuspenseBoundary';
import { Form } from 'react-router';
import { DatePicker } from './DatePicker';

type SuoritusOrder = 'koulutustyypeittain' | 'uusin-ensin';

type EditableSuoritus = {
  tyyppi: string;
  oppilaitos?: string;
  tila: string;
  suorituskieli: string;
  koulusivistyskieli: string;
  yksilollistetty: string;
  valmistumispaiva?: string;
};

type SelectOption = {
  label: string;
  value: string;
};

const EMPTY_SUORITUS: EditableSuoritus = {
  tyyppi: 'peruskoulu',
  tila: 'suorituksentila_kesken',
  koulusivistyskieli: '',
  suorituskieli: '',
  yksilollistetty: 'yksilollistaminen_ei',
};

const useAidinkieliOptions = () => {
  return useKoodistoOptions('aidinkielijakirjallisuus');
};

const getSuoritusTyyppiOptions = () => {
  return [
    {
      label: 'Peruskoulu',
      value: 'peruskoulu',
    },
    {
      label: 'Perusopetuksen lisäopetus',
      value: 'perusopetuksen_lisaopetuss',
    },
  ];
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

const useKieliOptions = () => {
  return useKoodistoOptions('kieli');
};

const ArvosanatTable = () => {
  const { t } = useTranslations();
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
        <ArvosanaRow
          name="aidinkieli"
          title="Äidinkieli ja kirjallisuus"
          lisatietoOptions={useAidinkieliOptions()}
        />
        <ArvosanaRow
          name="a1_kieli1"
          title="A1-kieli"
          lisatietoOptions={useKieliOptions()}
        />
        <ArvosanaRow
          name="a1_kieli2"
          title="A1-kieli"
          lisatietoOptions={useKieliOptions()}
        />
        <ArvosanaRow
          name="a2_kieli1"
          title="A2-kieli"
          lisatietoOptions={useKieliOptions()}
        />
        <ArvosanaRow
          name="a2_kieli2"
          title="A2-kieli"
          lisatietoOptions={useKieliOptions()}
        />
        <ArvosanaRow
          name="b1_kieli1"
          title="B1-kieli"
          lisatietoOptions={useKieliOptions()}
        />
        <ArvosanaRow
          name="b1_kieli2"
          title="B1-kieli"
          lisatietoOptions={useKieliOptions()}
        />
        <ArvosanaRow
          name="b2_kieli1"
          title="B2-kieli"
          lisatietoOptions={useKieliOptions()}
        />
        <ArvosanaRow
          name="b2_kieli2"
          title="B2-kieli"
          lisatietoOptions={useKieliOptions()}
        />
        <ArvosanaRow name="matematiikka" title="Matematiikka" />
        <ArvosanaRow name="biologia" title="Biologia" />
        <ArvosanaRow name="maantieto" title="Maantieto" />
        <ArvosanaRow name="fysiikka" title="Fysiikka" />
        <ArvosanaRow name="kemia" title="Kemia" />
        <ArvosanaRow name="terveystieto" title="Terveystieto" />
        <ArvosanaRow
          name="uskonto_tai_et"
          title="Uskonto tai elämänkatsomustieto"
        />
        <ArvosanaRow name="historia" title="Historia" />
        <ArvosanaRow name="yhteiskuntaoppi" title="Yhteiskuntaoppi" />
        <ArvosanaRow name="musiikki" title="Musiikki" />
        <ArvosanaRow name="kuvataide" title="Kuvataide" />
        <ArvosanaRow name="kasityo" title="Käsityö" />
        <ArvosanaRow name="liikunta" title="Liikunta" />
        <ArvosanaRow name="kotitalous" title="Kotitalous" />
      </TableBody>
    </StripedTable>
  );
};

const EditSuoritusPaper = ({
  suoritus = EMPTY_SUORITUS,
  onSave,
  onDelete,
  ref,
}: {
  suoritus?: EditableSuoritus;
  onSave: (suoritus: EditableSuoritus) => void;
  onDelete: () => void;
  ref: Ref<HTMLDivElement> | null;
}) => {
  const { t } = useTranslations();
  const oppilaitoksetOptions = useOppilaitoksetOptions();

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
            oppilaitos: formData.get('oppilaitos') as string,
            tila: formData.get('tila') as string,
            valmistumispaiva: formData.get('valmistumispaiva') as string,
            koulusivistyskieli: formData.get('koulusivistyskieli') as string,
            suorituskieli: formData.get('suorituskieli') as string,
            yksilollistetty: formData.get('yksilollistetty') as string,
          });
        }}
      >
        <InfoItemRow slotAmount={1} spacing={2}>
          <OphSelectFormField
            name="tyyppi"
            label={t('muokkaus.suoritus.tyyppi')}
            options={getSuoritusTyyppiOptions()}
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
            options={useKoodistoOptions('oppilaitoksenopetuskieli')}
          />
          <OphSelectFormField
            name="koulusivistyskieli"
            label={t('muokkaus.suoritus.koulusivistyskieli')}
            sx={{ flex: 1 }}
            required={true}
            defaultValue={suoritus?.koulusivistyskieli}
            options={useKoodistoOptions('oppilaitoksenopetuskieli')}
          />
          <OphSelectFormField
            name="yksilollistetty"
            label={t('muokkaus.suoritus.yksilollistetty')}
            sx={{ flex: 1 }}
            required={true}
            options={useKoodistoOptions('yksilollistaminen')}
            defaultValue={suoritus?.yksilollistetty}
          />
        </InfoItemRow>
        <ArvosanatTable />
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
  onSave: (suoditus: EditableSuoritus, index: number) => void;
  onDelete: (suoritus: EditableSuoritus, index: number) => void;
  editableSuoritukset: Array<EditableSuoritus>;
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
    Array<EditableSuoritus>
  >([]);

  const lastEditableSuoritusRef = useRef<HTMLDivElement | null>(null);

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
        onSave={(data, i) => {
          alert('Tiedot tallennettu');
          console.log('saved data', data, i);
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
