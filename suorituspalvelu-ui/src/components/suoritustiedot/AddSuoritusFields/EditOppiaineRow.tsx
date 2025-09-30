import { styled } from '@/lib/theme';
import type { OppiaineFields } from '@/types/ui-types';
import { TableCell, TableRow, type SelectProps } from '@mui/material';
import { OphSelect } from '@opetushallitus/oph-design-system';

export type SelectOption = {
  label: string;
  value: string;
};

const StyledSelect = styled(OphSelect)({
  minWidth: '180px',
  maxWidth: '300px',
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
  value: OppiaineFields;
  onChange: (newOppiaineValues: OppiaineFields) => void;
  title: string;
  kieliOptions?: Array<SelectOption>;
}) => {
  return (
    <TableRow>
      <TableCell>{title}</TableCell>
      <TableCell>
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
      <TableCell>
        <ArvosanaSelect
          value={value.arvosana ?? ''}
          onChange={(e) => onChange({ ...value, arvosana: e.target.value })}
        />
      </TableCell>
      <TableCell></TableCell>
    </TableRow>
  );
};
