import { useTranslations } from '@/hooks/useTranslations';
import { useCallback } from 'react';
import { SearchInput } from './SearchInput';
import { StyledSearchControls } from './StyledSearchControls';
import { useHenkiloSearchTermState } from '@/hooks/useHenkiloSearchTermState';

export const HenkiloSearchControls = () => {
  const { t } = useTranslations();

  const [henkiloSearchTerm, setHenkiloSearchTerm] = useHenkiloSearchTermState();

  const onChange = useCallback(
    (value: string) => {
      setHenkiloSearchTerm(value);
    },
    [setHenkiloSearchTerm],
  );

  const onClear = useCallback(() => {
    setHenkiloSearchTerm('');
  }, [setHenkiloSearchTerm]);

  return (
    <StyledSearchControls>
      <SearchInput
        sx={{
          flex: 1,
          maxWidth: '400px',
        }}
        label={t('search.hae-henkilo')}
        value={henkiloSearchTerm ?? ''}
        placeholder={t('search.henkilo-input-placeholder')}
        onClear={onClear}
        onChange={onChange}
      />
    </StyledSearchControls>
  );
};
