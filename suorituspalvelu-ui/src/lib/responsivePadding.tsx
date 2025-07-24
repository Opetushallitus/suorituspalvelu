import { Theme } from '@mui/material';
import { notLarge } from './theme';

export const responsivePadding = (theme: Theme) => ({
  padding: theme.spacing(2, 4),
  [notLarge(theme)]: {
    padding: theme.spacing(2),
  },
});
