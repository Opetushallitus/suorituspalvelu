import { DEFAULT_BOX_BORDER, styled } from '@/lib/theme';
import { Stack } from '@mui/material';

export const StyledSearchControls = styled(Stack)(({ theme }) => ({
  flexDirection: 'row',
  gap: theme.spacing(2),
  margin: theme.spacing(2, 2, 0, 2),
  paddingBottom: theme.spacing(2),
  borderBottom: DEFAULT_BOX_BORDER,
}));
