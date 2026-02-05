import {
  useOppilaitoksenOppijatSearchResult,
  useOppilaitoksenOppijatSearchParamsState,
} from '@/hooks/useOppilaitoksenOppijatSearch';
import { NavLink } from 'react-router';
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
import { sortBy } from 'remeda';

const SidebarContent = () => {
  const { searchParams, setSearchParams, hasValidSearchParams } =
    useOppilaitoksenOppijatSearchParamsState();

  const { data, totalCount } = useOppilaitoksenOppijatSearchResult();

  const oppijatSorted = sortBy(data ?? [], (oppija) => oppija.sukunimi ?? '');

  const { t } = useTranslate();

  const { oppijaNumero } = useOppijaNumeroParamState();

  const searchTab = useSelectedSearchTab();
  const tiedotTab = useSelectedTiedotTab();

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
            {oppijatSorted?.map((oppija) => {
              const henkiloNimi = formatHenkiloNimi(oppija, t);
              const luokat = oppija.luokat?.join(', ');
              const henkiloTunnus = oppija.hetu;
              const ariaLabel = `${henkiloNimi}${
                henkiloTunnus
                  ? ', ' + t('sivupalkki.henkilotunnus') + ': ' + henkiloTunnus
                  : ''
              }${luokat ? ', ' + t('sivupalkki.luokka') + ': ' + luokat : ''}`;
              return (
                <NavLink
                  key={oppija.oppijaNumero}
                  prefetch="intent"
                  aria-label={ariaLabel}
                  className={
                    oppijaNumero === oppija.oppijaNumero
                      ? NAV_LIST_SELECTED_ITEM_CLASS
                      : ''
                  }
                  to={{
                    pathname: `/${searchTab}/${oppija.oppijaNumero}/${tiedotTab ?? ''}`,
                    search: new URLSearchParams(searchParams).toString(),
                  }}
                >
                  <OphTypography variant="label" color="inherit">
                    {formatHenkiloNimi(oppija, t)}
                  </OphTypography>
                  <Stack
                    direction="row"
                    sx={{ justifyContent: 'space-between', gap: 2 }}
                  >
                    <OphTypography color={ophColors.black}>
                      {' '}
                      {oppija.hetu}
                    </OphTypography>
                    <OphTypography> {luokat}</OphTypography>
                  </Stack>
                </NavLink>
              );
            })}
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
