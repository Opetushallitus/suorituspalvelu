import { styled } from '@/lib/theme';
import { Close, Search } from '@mui/icons-material';
import { InputAdornment } from '@mui/material';
import {
  OphButton,
  ophColors,
  OphInputFormField,
} from '@opetushallitus/oph-design-system';

type SearchInputProps = Omit<
  React.ComponentProps<typeof OphInputFormField>,
  'endAdornment'
> & {
  onClear: () => void;
};

const StyledAdornment = styled(InputAdornment)(({ theme }) => ({
  '&.MuiInputAdornment-root': {
    margin: 0,
    padding: theme.spacing(0, 1),
  },
}));

const ClearButton = styled(OphButton)({
  '&.MuiButton-root': {
    margin: 0,
    padding: '0 3px 0 0',
  },
});

export const SearchInput = ({ onClear, ...props }: SearchInputProps) => {
  return (
    <OphInputFormField
      endAdornment={
        <StyledAdornment position="end">
          {props.value ? (
            <ClearButton startIcon={<Close />} onClick={onClear} />
          ) : null}
          <Search sx={{ '&.MuiSvgIcon-root': { color: ophColors.grey300 } }} />
        </StyledAdornment>
      }
      {...props}
    />
  );
};
