import { Box } from '@mui/material';
import { styled } from '@/lib/theme';

const MAX_WIDTH = '1920px';

export const PageContent = styled(Box)({
  maxWidth: MAX_WIDTH,
  margin: 'auto',
  width: '100%',
});
