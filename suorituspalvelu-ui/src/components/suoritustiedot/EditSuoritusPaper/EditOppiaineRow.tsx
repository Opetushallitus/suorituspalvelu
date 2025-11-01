import { useTranslations } from '@/hooks/useTranslations';
import { styled } from '@/lib/theme';
import type { PerusopetusOppiaineFields, SelectOption } from '@/types/ui-types';
import { Add, DeleteOutline } from '@mui/icons-material';
import { Stack, TableCell, TableRow } from '@mui/material';
import { OphButton, OphSelect } from '@opetushallitus/oph-design-system';

const StyledSelect = styled(OphSelect)({
  minWidth: '180px',
});

export const EditOppiaineRow = ({
  value,
  onChange,
  title,
  arvosanaOptions,
  kieliOptions,
}: {
  value: PerusopetusOppiaineFields;
  onChange: (newOppiaineValues: PerusopetusOppiaineFields) => void;
  title: string;
  arvosanaOptions: Array<SelectOption>;
  kieliOptions?: Array<SelectOption>;
}) => {
  const { t } = useTranslations();

  const valinnaisetArvosanat = value.valinnaisetArvosanat ?? [''];

  return (
    <TableRow>
      <TableCell sx={{ verticalAlign: 'top', lineHeight: '36px' }}>
        {title}
      </TableCell>
      <TableCell sx={{ verticalAlign: 'top' }}>
        {kieliOptions && (
          <StyledSelect
            options={kieliOptions}
            value={value.kieli ?? ''}
            clearable={true}
            placeholder={t('muokkaus.suoritus.oppiaine-placeholder')}
            aria-label={t('muokkaus.suoritus.kieli-label', { oppiaine: title })}
            onChange={(e) => {
              onChange({ ...value, kieli: e.target.value });
            }}
          />
        )}
      </TableCell>
      <TableCell sx={{ verticalAlign: 'top' }}>
        <StyledSelect
          options={arvosanaOptions}
          value={value.arvosana ?? ''}
          clearable={true}
          placeholder={t('muokkaus.suoritus.arvosana-placeholder')}
          aria-label={t('muokkaus.suoritus.arvosana-label', {
            oppiaine: title,
          })}
          onChange={(e) => onChange({ ...value, arvosana: e.target.value })}
        />
      </TableCell>
      <TableCell>
        <Stack spacing={1} sx={{ alignItems: 'flex-start' }}>
          {valinnaisetArvosanat.map((valinnainenArvosana, index) => (
            // eslint-disable-next-line @eslint-react/no-array-index-key
            <Stack key={index} direction="row">
              <StyledSelect
                options={arvosanaOptions}
                value={valinnainenArvosana ?? ''}
                placeholder={t('muokkaus.suoritus.arvosana-placeholder')}
                clearable={true}
                aria-label={t('muokkaus.suoritus.valinnainen-arvosana-label', {
                  oppiaine: title,
                  number: index + 1,
                })}
                onChange={(e) => {
                  if (valinnaisetArvosanat) {
                    const newValinnaiset = [...valinnaisetArvosanat];
                    newValinnaiset[index] = e.target.value;
                    onChange({
                      ...value,
                      valinnaisetArvosanat: newValinnaiset,
                    });
                  }
                }}
              />
              <OphButton
                sx={{
                  flexShrink: 1,
                  flexGrow: 0,
                  visibility: index === 0 ? 'hidden' : 'visible',
                }}
                startIcon={<DeleteOutline />}
                onClick={() => {
                  onChange({
                    ...value,
                    valinnaisetArvosanat: value.valinnaisetArvosanat?.filter(
                      (_, i) => i !== index,
                    ),
                  });
                }}
              />
            </Stack>
          ))}
          <OphButton
            startIcon={<Add />}
            sx={{ padding: 0 }}
            onClick={() => {
              onChange({
                ...value,
                valinnaisetArvosanat: [...valinnaisetArvosanat, ''],
              });
            }}
          >
            {t('muokkaus.suoritus.lisaa-valinnainen')}
          </OphButton>
        </Stack>
      </TableCell>
    </TableRow>
  );
};
