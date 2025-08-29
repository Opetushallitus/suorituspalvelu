import { Box, Typography } from '@mui/material';
import { ophColors } from '@opetushallitus/oph-design-system';
import { DEFAULT_BOX_BORDER, styled } from '@/lib/theme';
import { responsivePadding } from '@/lib/responsivePadding';
import { PageContent } from './PageContent';

export type HeaderProps = {
  title: React.ReactNode;
};

const HeaderContent = styled(PageContent)(({ theme }) => ({
  display: 'flex',
  alignItems: 'center',
  columnGap: theme.spacing(2),
  ...responsivePadding(theme),
}));

export function Header({ title }: HeaderProps) {
  return (
    <Box
      component="header"
      sx={{
        position: 'relative',
        backgroundColor: ophColors.white,
        width: '100%',
        borderBottom: DEFAULT_BOX_BORDER,
      }}
    >
      <HeaderContent>
        <Typography variant="h2" component="h1">
          {title}
        </Typography>
      </HeaderContent>
    </Box>
  );
}
