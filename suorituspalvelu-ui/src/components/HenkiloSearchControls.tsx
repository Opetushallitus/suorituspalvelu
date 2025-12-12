import { useTranslations } from '@/hooks/useTranslations';
import { useCallback, useEffect } from 'react';
import { SearchInput } from './SearchInput';
import { StyledSearchControls } from './StyledSearchControls';
import { useLocation, useNavigate } from 'react-router';
import type { SearchNavigationState } from '@/types/navigation';
import { useOppijaNumeroParamState } from '@/hooks/useOppijanumeroParamState';
import { isHenkilotunnus, isOppijaNumero } from '@/lib/common';
import { isEmptyish } from 'remeda';
import { useNotifications } from './NotificationProvider';

const useHenkiloSearchTermState = () => {
  const location = useLocation();
  const navigate = useNavigate();

  const setHenkiloSearchTerm = useCallback(
    (value: string) => {
      const newLocation = { ...location };
      if (
        isEmptyish(value) ||
        !isHenkilotunnus(value) ||
        !isOppijaNumero(value)
      ) {
        const pathParts = location.pathname.split('/');
        pathParts.splice(2);
        newLocation.pathname = pathParts.join('/');
      }
      navigate(newLocation, {
        state: {
          ...(location.state as SearchNavigationState),
          henkiloSearchTerm: value,
        },
      });
    },
    [location, navigate],
  );

  const henkiloSearchTerm = (location.state as SearchNavigationState)
    ?.henkiloSearchTerm;

  return [henkiloSearchTerm, setHenkiloSearchTerm] as const;
};

export const HenkiloSearchControls = () => {
  const { t } = useTranslations();

  const { oppijaNumero } = useOppijaNumeroParamState();
  const [henkiloSearchTerm, setHenkiloSearchTerm] = useHenkiloSearchTermState();

  const { showNotification } = useNotifications();

  const onChange = useCallback(
    (value: string) => {
      setHenkiloSearchTerm(value);
    },
    [setHenkiloSearchTerm],
  );

  const onClear = useCallback(() => {
    setHenkiloSearchTerm('');
  }, [setHenkiloSearchTerm]);

  useEffect(() => {
    if (isHenkilotunnus(oppijaNumero)) {
      setHenkiloSearchTerm('');
      showNotification({
        message: t('search.henkilotunnukseen-linkitys-kielletty'),
        type: 'error',
      });
    } else if (henkiloSearchTerm === undefined && !isEmptyish(oppijaNumero)) {
      setHenkiloSearchTerm(oppijaNumero);
    }
  }, [
    oppijaNumero,
    henkiloSearchTerm,
    t,
    setHenkiloSearchTerm,
    showNotification,
  ]);

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
