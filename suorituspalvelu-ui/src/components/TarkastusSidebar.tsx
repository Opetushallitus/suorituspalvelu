import {
  useOppilaitoksenOppijatSearchResult,
  useOppilaitoksenOppijatSearchParamsState,
} from '@/hooks/useOppilaitoksenOppijatSearch';
import { NavLink, useLocation } from 'react-router';
import { QuerySuspenseBoundary } from './QuerySuspenseBoundary';
import { LeftPanel } from './LeftPanel';
import { useCallback } from 'react';
import { NAV_LIST_SELECTED_ITEM_CLASS, NavigationList } from './NavigationList';
import { useTranslate } from '@tolgee/react';
import { ophColors, OphTypography } from '@opetushallitus/oph-design-system';
import { formatHenkiloNimi } from '@/lib/common';
import { useSelectedSearchTab } from '@/hooks/useSelectedSearchTab';
import { SearchInput } from './SearchInput';
import { Box, Stack } from '@mui/material';
import { useSelectedTiedotTab } from '@/hooks/useSelectedTiedotTab';
import { useOppijaNumeroParamState } from '@/hooks/useOppijanumeroParamState';
import type { SearchNavigationState } from '@/types/navigation';
import { omit } from 'remeda';
import type { OppijaSearchItem } from '@/types/ui-types';

const HenkiloLink = ({ oppija }: { oppija: OppijaSearchItem }) => {
  const { t } = useTranslate();
  const searchTab = useSelectedSearchTab();
  const tiedotTab = useSelectedTiedotTab();

  const { searchParams } = useOppilaitoksenOppijatSearchParamsState();

  const { oppijaNumero } = useOppijaNumeroParamState();

  const henkiloNimi = formatHenkiloNimi(oppija, t);
  const luokat = oppija.luokat?.join(', ');
  const henkiloTunnus = oppija.hetu;

  const ariaLabel = `${henkiloNimi}${
    henkiloTunnus
      ? ', ' + t('sivupalkki.henkilotunnus') + ': ' + henkiloTunnus
      : ''
  }${luokat ? ', ' + t('sivupalkki.luokka') + ': ' + luokat : ''}`;

  const location = useLocation();
  const locationState = location.state as SearchNavigationState;

  return (
    <NavLink
      prefetch="intent"
      aria-label={ariaLabel}
      className={
        oppijaNumero === oppija.oppijaNumero ? NAV_LIST_SELECTED_ITEM_CLASS : ''
      }
      to={{
        pathname: `/${searchTab}/${oppijaNumero}/${tiedotTab ?? ''}`,
        search: new URLSearchParams(
          omit(searchParams, ['suodatus']),
        ).toString(),
      }}
      state={locationState}
    >
      <OphTypography variant="label" color="inherit">
        {henkiloNimi}
      </OphTypography>
      <Stack direction="row" sx={{ justifyContent: 'space-between', gap: 2 }}>
        <OphTypography color={ophColors.black}> {oppija.hetu}</OphTypography>
        <OphTypography> {luokat}</OphTypography>
      </Stack>
    </NavLink>
  );
};

const SidebarContent = () => {
  const { searchParams, setSearchParams, hasValidSearchParams } =
    useOppilaitoksenOppijatSearchParamsState();

  const { data, totalCount } = useOppilaitoksenOppijatSearchResult();

  const { t } = useTranslate();

  const onClear = useCallback(() => {
    setSearchParams({ suodatus: '' });
  }, [setSearchParams]);

  const onChange = useCallback(
    (value: string) => {
      setSearchParams({ suodatus: value });
    },
    [setSearchParams],
  );

  return (
    <Stack
      spacing={1.5}
      sx={{ paddingLeft: 2, paddingTop: 2, height: '100%' }}
      data-test-id="henkilot-sidebar"
    >
      {hasValidSearchParams ? (
        <>
          {totalCount !== 0 && (
            <Box sx={{ paddingRight: 2 }}>
              <SearchInput
                sx={{ width: '100%' }}
                placeholder={t('sivupalkki.suodata-nimella-tai-hetulla')}
                value={searchParams.suodatus ?? ''}
                onClear={onClear}
                onChange={onChange}
              />
            </Box>
          )}
          <OphTypography variant="body2">
            {t('sivupalkki.henkilo-maara', {
              count: data?.length ?? 0,
            })}
          </OphTypography>
          <NavigationList tabIndex={0} aria-label={t('sivupalkki.navigaatio')}>
            {data?.map((oppija) => (
              <HenkiloLink key={oppija.oppijaNumero} oppija={oppija} />
            ))}
          </NavigationList>
        </>
      ) : (
        <OphTypography variant="body2">
          {t('search.valitse-oppilaitos')}
        </OphTypography>
      )}
    </Stack>
  );
};

export function TarkastusSidebar() {
  return (
    <LeftPanel>
      <QuerySuspenseBoundary>
        <SidebarContent />
      </QuerySuspenseBoundary>
    </LeftPanel>
  );
}
