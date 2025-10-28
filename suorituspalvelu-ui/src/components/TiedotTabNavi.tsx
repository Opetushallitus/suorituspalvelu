import { useTranslations } from '@/hooks/useTranslations';
import { DEFAULT_BOX_BORDER, ophColors, styled } from '@/lib/theme';
import { Link, useLocation, useSearchParams } from 'react-router';
import { last } from 'remeda';

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

const StyledTab = styled(Link)<{ $active: boolean }>(({ $active }) => ({
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

const useActiveTiedotTab = () => {
  const location = useLocation();
  return last(location.pathname.split('/'));
};

export const TiedotTabNavi = () => {
  const { t } = useTranslations();
  const activeTab = useActiveTiedotTab();
  const [searchParams] = useSearchParams();

  // Convert URLSearchParams to search string
  const searchString = searchParams.toString();

  const searchWithPrefix = searchString ? `?${searchString}` : '';

  return (
    <StyledContainer>
      <StyledTabs>
        <StyledTab
          $active={activeTab === 'suoritustiedot'}
          to={{
            pathname: 'suoritustiedot',
            search: searchWithPrefix,
          }}
        >
          {t('tabs.suoritustiedot')}
        </StyledTab>
        <StyledTab
          $active={activeTab === 'opiskelijavalinnan-tiedot'}
          to={{
            pathname: 'opiskelijavalinnan-tiedot',
            search: searchWithPrefix,
          }}
        >
          {t('tabs.opiskelijavalinnan-tiedot')}
        </StyledTab>
      </StyledTabs>
    </StyledContainer>
  );
};
