import { useSelectedTiedotTab } from '@/hooks/useSelectedTiedotTab';
import { useTranslations } from '@/hooks/useTranslations';
import { setTiedotTabInPath } from '@/lib/navigationPathUtils';
import { useKayttaja } from '@/lib/suorituspalvelu-queries';
import { DEFAULT_BOX_BORDER, ophColors, styled } from '@/lib/theme';
import { NavLink, useLocation } from 'react-router';

const StyledContainer = styled('div')({
  borderBottom: DEFAULT_BOX_BORDER,
});

const StyledTabs = styled('nav')(({ theme }) => ({
  display: 'flex',
  flexDirection: 'row',
  columnGap: theme.spacing(2),
  rowGap: theme.spacing(1),
  flexWrap: 'wrap',
}));

const StyledTab = styled(NavLink)<{ $active: boolean }>(({ $active }) => ({
  color: ophColors.blue2,
  cursor: 'pointer',
  borderBottom: '3px solid',
  borderColor: $active ? ophColors.blue2 : 'transparent',
  textDecoration: 'none',
  '&:hover': {
    borderColor: ophColors.blue2,
  },
  '&:focus-visible': {
    outlineOffset: '3px',
  },
}));

export const TiedotTabNavi = () => {
  const { t } = useTranslations();
  const activeTab = useSelectedTiedotTab();

  const { data: kayttaja } = useKayttaja();

  const { pathname, search, state } = useLocation();

  return (
    <StyledContainer>
      <StyledTabs>
        <StyledTab
          $active={activeTab === 'suoritustiedot'}
          to={{
            pathname: setTiedotTabInPath(pathname, 'suoritustiedot'),
            search,
          }}
          state={state}
          prefetch="intent"
        >
          {t('tabs.suoritustiedot')}
        </StyledTab>
        {(kayttaja.isRekisterinpitaja ||
          activeTab === 'opiskelijavalinnan-tiedot') && (
          <StyledTab
            $active={activeTab === 'opiskelijavalinnan-tiedot'}
            to={{
              pathname: setTiedotTabInPath(
                pathname,
                'opiskelijavalinnan-tiedot',
              ),
              search,
            }}
            state={state}
            prefetch="intent"
          >
            {t('tabs.opiskelijavalinnan-tiedot')}
          </StyledTab>
        )}
      </StyledTabs>
    </StyledContainer>
  );
};
