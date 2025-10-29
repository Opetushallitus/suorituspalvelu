import { useTranslations } from '@/hooks/useTranslations';
import { styled } from '@/lib/theme';
import type { PerusopetusOppiaineFields } from '@/types/ui-types';
import { Add, DeleteOutline } from '@mui/icons-material';
import { Stack, TableCell, TableRow, type SelectProps } from '@mui/material';
import { OphButton, OphSelect } from '@opetushallitus/oph-design-system';

export type SelectOption = {
  label: string;
  value: string;
};

const StyledSelect = styled(OphSelect)({
  minWidth: '180px',
});

const ARVOSANA_OPTIONS: Array<SelectOption> = [
  { label: '10', value: '10' },
  { label: '9', value: '9' },
  { label: '8', value: '8' },
  { label: '7', value: '7' },
  { label: '6', value: '6' },
  { label: '5', value: '5' },
  { label: '4', value: '4' },
];

const ArvosanaSelect = ({ value, onChange }: SelectProps<string>) => {
  return (
    <StyledSelect
      options={ARVOSANA_OPTIONS}
      value={value}
      onChange={onChange}
    />
  );
};

export const EditOppiaineRow = ({
  value,
  onChange,
  title,
  kieliOptions,
}: {
  value: PerusopetusOppiaineFields;
  onChange: (newOppiaineValues: PerusopetusOppiaineFields) => void;
  title: string;
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
            onChange={(e) => {
              if (e.target.value) {
                onChange({ ...value, kieli: e.target.value });
              }
            }}
          />
        )}
      </TableCell>
      <TableCell sx={{ verticalAlign: 'top' }}>
        <ArvosanaSelect
          value={value.arvosana ?? ''}
          onChange={(e) => onChange({ ...value, arvosana: e.target.value })}
        />
      </TableCell>
      <TableCell>
        <Stack spacing={1} sx={{ alignItems: 'flex-start' }}>
          {valinnaisetArvosanat.map((valinnainenArvosana, index) => (
            // eslint-disable-next-line @eslint-react/no-array-index-key
            <Stack key={index} direction="row">
              <ArvosanaSelect
                value={valinnainenArvosana ?? ''}
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
