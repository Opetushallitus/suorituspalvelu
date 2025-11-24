import {
  useOppilaitoksenOppijatSearch,
  useOppijatSearchURLParams,
  useOppijatSearchParamsState,
} from '@/hooks/useSearchOppijat';
import { Link, useParams } from 'react-router';
import { QuerySuspenseBoundary } from './QuerySuspenseBoundary';
import { LeftPanel } from './LeftPanel';
import { useState } from 'react';
import { NAV_LIST_SELECTED_ITEM_CLASS, NavigationList } from './NavigationList';
import { useTranslate } from '@tolgee/react';
import { ophColors, OphTypography } from '@opetushallitus/oph-design-system';
import { formatHenkiloNimi } from '@/lib/common';
import { useSelectedSearchTab } from '@/hooks/useSelectedSearchTab';
import { SearchInput } from './SearchInput';
import { Box, Stack } from '@mui/material';

const HenkilotSidebarContent = () => {
  const params = useOppijatSearchURLParams();
  const { data } = useOppilaitoksenOppijatSearch();

  const { setSearchParams, hasEmptySearchParams } =
    useOppijatSearchParamsState();

  const { t } = useTranslate();

  const { oppijaNumero } = useParams();

  const searchTab = useSelectedSearchTab();

  return hasEmptySearchParams ? (
    <div></div>
  ) : (
    <Stack spacing={1.5} sx={{ paddingLeft: 2 }}>
      <Box sx={{ paddingRight: 2, paddingTop: 1 }}>
        <SearchInput
          sx={{ width: '100%' }}
          value={params.tunniste ?? ''}
          onClear={() => {
            setSearchParams({ tunniste: '' });
          }}
          onChange={(e) => {
            setSearchParams({ tunniste: e.target.value });
          }}
        />
      </Box>
      <OphTypography variant="body2">
        {t('sivupalkki.henkilo-maara', {
          count: data?.length ?? 0,
        })}
      </OphTypography>
      <NavigationList tabIndex={0} aria-label={t('sivupalkki.navigaatio')}>
        {data?.map((oppija) => (
          <Link
            key={oppija.oppijaNumero}
            className={
              oppijaNumero === oppija.oppijaNumero
                ? NAV_LIST_SELECTED_ITEM_CLASS
                : ''
            }
            to={{
              pathname: `/${searchTab}/${oppija.oppijaNumero}`,
              search: new URLSearchParams(params).toString(),
            }}
          >
            <OphTypography variant="label" color="inherit">
              {formatHenkiloNimi(oppija, t)}
            </OphTypography>
            <OphTypography color={ophColors.black}>{oppija.hetu}</OphTypography>
          </Link>
        ))}
      </NavigationList>
    </Stack>
  );
};

export function HenkilotSidebar() {
  const [isOpen, setIsOpen] = useState(true);

  return (
    <LeftPanel isOpen={isOpen} setIsOpen={setIsOpen}>
      <QuerySuspenseBoundary>
        <HenkilotSidebarContent />
      </QuerySuspenseBoundary>
    </LeftPanel>
  );
}
