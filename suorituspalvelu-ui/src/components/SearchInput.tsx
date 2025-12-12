import { useTranslations } from '@/hooks/useTranslations';
import { styled } from '@/lib/theme';
import { Close, Search } from '@mui/icons-material';
import { InputAdornment } from '@mui/material';
import {
  OphButton,
  ophColors,
  OphInputFormField,
} from '@opetushallitus/oph-design-system';
import { useCallback, useEffect, useRef, useState } from 'react';

type SearchInputProps = Omit<
  React.ComponentProps<typeof OphInputFormField>,
  'endAdornment' | 'onChange' | 'value'
> & {
  onClear?: () => void;
  onChange?: (value: string) => void;
  debounceMs?: number;
  value: string;
};

const StyledAdornment = styled(InputAdornment)(({ theme }) => ({
  '&.MuiInputAdornment-root': {
    margin: 0,
    padding: theme.spacing(0, 1),
    '& .SearchInput_searchIcon': {
      color: ophColors.grey300,
    },
    '& .MuiButton-root': {
      margin: 0,
      padding: '0 3px 0 0',
    },
  },
}));

export const SearchInput = ({
  onClear,
  onChange,
  debounceMs = 300,
  value,
  ...props
}: SearchInputProps) => {
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [localValue, setLocalValue] = useState(value);

  useEffect(() => {
    // Sync external value changes only when not debouncing
    if (value !== localValue && !timeoutRef.current) {
      // eslint-disable-next-line @eslint-react/hooks-extra/no-direct-set-state-in-use-effect
      setLocalValue(value);
    }
  }, [value, localValue]);

  useEffect(() => {
    return () => {
      // Clear timeout when unmounting component so don't change state after component is unmounted
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
        timeoutRef.current = null;
      }
    };
  }, []);

  const handleChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
      const newValue = event.target.value;
      timeoutRef.current = setTimeout(() => {
        onChange?.(newValue);
        timeoutRef.current = null;
      }, debounceMs);
      setLocalValue(newValue);
    },
    [setLocalValue, debounceMs, onChange],
  );

  const { t } = useTranslations();

  return (
    <OphInputFormField
      endAdornment={
        <StyledAdornment position="end">
          {localValue ? (
            <OphButton
              startIcon={<Close />}
              aria-label={t('search.tyhjenna')}
              onClick={() => {
                onClear?.();
                if (timeoutRef.current) {
                  clearTimeout(timeoutRef.current);
                  timeoutRef.current = null;
                  setLocalValue('');
                }
              }}
            />
          ) : null}
          <Search className="SearchInput_searchIcon" />
        </StyledAdornment>
      }
      value={localValue}
      onChange={handleChange}
      {...props}
    />
  );
};
