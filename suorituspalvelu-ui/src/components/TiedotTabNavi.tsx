import { useActiveTiedotTab } from '@/hooks/useActiveTiedotTab';
import { useTranslations } from '@/hooks/useTranslations';
import { useKayttaja } from '@/lib/suorituspalvelu-queries';
import { DEFAULT_BOX_BORDER, ophColors, styled } from '@/lib/theme';
import { Link, useSearchParams } from 'react-router';

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

export const TiedotTabNavi = () => {
  const { t } = useTranslations();
  const activeTab = useActiveTiedotTab();
  const [searchParams] = useSearchParams();

  const { data: kayttaja } = useKayttaja();

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
        {(kayttaja.isRekisterinpitaja ||
          activeTab === 'opiskelijavalinnan-tiedot') && (
          <StyledTab
            $active={activeTab === 'opiskelijavalinnan-tiedot'}
            to={{
              pathname: 'opiskelijavalinnan-tiedot',
              search: searchWithPrefix,
            }}
            prefetch="intent"
          >
            {t('tabs.opiskelijavalinnan-tiedot')}
          </StyledTab>
        )}
      </StyledTabs>
    </StyledContainer>
  );
};
