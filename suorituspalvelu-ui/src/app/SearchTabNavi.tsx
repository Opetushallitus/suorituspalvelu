import { DEFAULT_BOX_BORDER, styled } from '@/lib/theme';
import { OphButton, ophColors } from '@opetushallitus/oph-design-system';
import React from 'react';
import { Link, useLocation } from 'react-router';
import { useTranslations } from '@/hooks/useTranslations';
import { useSelectedSearchTab } from '@/hooks/useSelectedSearchTab';
import { join, pipe, splice, split } from 'remeda';
import { useIsTarkastusnakymaAllowed } from '@/hooks/useIsTarkastusnakymaAllowed';

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

const TABS = ['henkilo', 'tarkastus'];

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

  const isTarkastusNakymaAllowed = useIsTarkastusnakymaAllowed();

  // näytetään tarkastus-välilehti vain jos käyttäjällä oikeus käyttää sitä
  if (
    tabName === 'tarkastus' &&
    selectedTabName !== 'tarkastus' &&
    !isTarkastusNakymaAllowed
  ) {
    return null;
  }

  return (
    <OphButton
      component={Link}
      variant={selectedTabName === tabName ? 'contained' : 'text'}
      to={{ pathname, search: location.search }}
    >
      {t(`search.tabs.${tabName}`)}
    </OphButton>
  );
};

export const SearchTabNavi = () => {
  const { t } = useTranslations();

  return (
    <StyledNavi aria-label={t('search.navigaatio')}>
      {TABS.map((tabName) => {
        return (
          <React.Fragment key={tabName}>
            <TabButton tabName={tabName} />
          </React.Fragment>
        );
      })}
    </StyledNavi>
  );
};
