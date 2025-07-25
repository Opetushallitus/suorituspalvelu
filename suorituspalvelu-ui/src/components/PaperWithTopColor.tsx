import { styled } from '@/lib/theme';
import { Paper } from '@mui/material';

export const PaperWithTopColor = styled(Paper)<{ topColor?: string }>(
  ({ theme, topColor }) => ({
    borderTop: `4px solid ${topColor ?? theme.palette.primary.main}`,
    width: '100%',
    position: 'relative',
    borderRadius: '3px',
    padding: theme.spacing(2.5),
    [theme.breakpoints.down('xs')]: {
      padding: theme.spacing(1),
    },
  }),
);
