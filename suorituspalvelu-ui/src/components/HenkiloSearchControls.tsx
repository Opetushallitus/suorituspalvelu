import { useTranslations } from '@/hooks/useTranslations';
import { useCallback } from 'react';
import { SearchInput } from './SearchInput';
import { useOppijaTunnisteParamState } from '@/hooks/useOppijanumeroParamState';
import { StyledSearchControls } from './StyledSearchControls';

export const HenkiloSearchControls = () => {
  const { t } = useTranslations();

  const { oppijaTunniste, setOppijaTunniste } = useOppijaTunnisteParamState();

  const onClear = useCallback(() => {
    setOppijaTunniste('');
  }, [setOppijaTunniste]);

  const onChange = useCallback(
    (value: string) => {
      setOppijaTunniste(value);
    },
    [setOppijaTunniste],
  );

  return (
    <StyledSearchControls>
      <SearchInput
        sx={{
          flex: 1,
          maxWidth: '400px',
        }}
        label={t('search.hae-henkilo')}
        value={oppijaTunniste ?? ''}
        placeholder={t('search.henkilo-input-placeholder')}
        onClear={onClear}
        onChange={onChange}
      />
    </StyledSearchControls>
  );
};
