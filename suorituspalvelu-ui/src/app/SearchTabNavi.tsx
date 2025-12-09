import { DEFAULT_BOX_BORDER, styled } from '@/lib/theme';
import { OphButton, ophColors } from '@opetushallitus/oph-design-system';
import React from 'react';
import { Link, useLocation } from 'react-router';
import { useTranslations } from '@/hooks/useTranslations';
import { useSelectedSearchTab } from '@/hooks/useSelectedSearchTab';
import { join, pipe, splice, split } from 'remeda';
import { useIsTarkastusnakymaAllowed } from '@/hooks/useIsTarkastusnakymaAllowed';
import { HAKU_QUERY_PARAM_NAME } from '@/lib/common';
import { useIsHenkilohakuAllowed } from '@/hooks/useIsHenkilohakuAllowed';

const TAB_BUTTON_HEIGHT = '48px';

const StyledNavi = styled('nav')({
  display: 'flex',
  flexDirection: 'row',
  justifyContent: 'flex-start',
  width: '100%',
  borderBottom: DEFAULT_BOX_BORDER,
  height: TAB_BUTTON_HEIGHT,
  '& .MuiButton-root': {
    borderRadius: 0,
    fontWeight: 'normal',
    height: TAB_BUTTON_HEIGHT,
    '&:hover': {
      borderColor: ophColors.blue2,
    },
  },
});

type SearchTabName = 'henkilo' | 'tarkastus';

const TabButton = ({ tabName }: { tabName: string }) => {
  const { t } = useTranslations();
  const selectedTabName = useSelectedSearchTab();

  const location = useLocation();

  // Vaihdetaan tabin nimi URL:n polkuun
  const pathname = pipe(
    location.pathname,
    split('/'),
    splice(1, 1, [tabName]),
    join('/'),
  );

  const preservedSearchParams = new URLSearchParams(location.search);
  preservedSearchParams.delete(HAKU_QUERY_PARAM_NAME);

  return (
    <OphButton
      component={Link}
      variant={selectedTabName === tabName ? 'contained' : 'text'}
      to={{ pathname, search: preservedSearchParams.toString() }}
    >
      {t(`search.tabs.${tabName}`)}
    </OphButton>
  );
};

const useVisibleTabs = () => {
  const isHenkilohakuAllowed = useIsHenkilohakuAllowed();
  const isTarkastusNakymaAllowed = useIsTarkastusnakymaAllowed();

  const selectedTabName = useSelectedSearchTab();

  const tabs: Array<SearchTabName> = [];

  if (selectedTabName === 'henkilo' || isHenkilohakuAllowed) {
    tabs.push('henkilo');
  }
  if (selectedTabName === 'tarkastus' || isTarkastusNakymaAllowed) {
    tabs.push('tarkastus');
  }

  return tabs;
};

export const SearchTabNavi = () => {
  const { t } = useTranslations();

  const tabs = useVisibleTabs();

  return (
    <StyledNavi aria-label={t('search.navigaatio')}>
      {tabs.map((tabName) => {
        return <TabButton key={tabName} tabName={tabName} />;
      })}
    </StyledNavi>
  );
};
