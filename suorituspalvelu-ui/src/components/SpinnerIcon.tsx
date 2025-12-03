import { withDefaultProps } from '@/lib/theme';
import { CircularProgress } from '@mui/material';

export const SpinnerIcon = withDefaultProps(CircularProgress, {
  color: 'inherit',
  size: '24px',
});
