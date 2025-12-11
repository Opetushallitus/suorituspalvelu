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

export const HenkiloSearchControls = () => {
  const { t } = useTranslations();

  const location = useLocation();
  const navigate = useNavigate();
  const locationState = location.state as SearchNavigationState | undefined;

  const { oppijaNumero } = useOppijaNumeroParamState();

  const henkiloSearchTerm = locationState?.henkiloSearchTerm;

  const { showNotification } = useNotifications();

  const onChange = useCallback(
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
          ...locationState,
          henkiloSearchTerm: value,
        },
      });
    },
    [location, navigate],
  );

  const onClear = useCallback(() => {
    onChange('');
  }, [location, navigate, onChange]);

  useEffect(() => {
    if (henkiloSearchTerm === undefined && !isEmptyish(oppijaNumero)) {
      {
        navigate(location, {
          state: {
            ...location.state,
            henkiloSearchTerm: oppijaNumero,
          },
        });
      }
    }
    if (isHenkilotunnus(oppijaNumero)) {
      onClear();
      showNotification({
        message: t('search.henkilotunnukseen-linkitys-kielletty'),
        type: 'error',
      });
    }
  }, [locationState, oppijaNumero, t]);

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
