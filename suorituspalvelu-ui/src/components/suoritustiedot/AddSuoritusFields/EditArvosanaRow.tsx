import { useKoodistoOptions } from '@/lib/koodisto-queries';
import { styled } from '@/lib/theme';
import { TableCell, TableRow } from '@mui/material';
import { OphSelect } from '@opetushallitus/oph-design-system';

export type SelectOption = {
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

export const EditArvosanaRow = ({
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
