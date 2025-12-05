import { type ComponentRef, type ComponentType } from 'react';
import { ophColors } from '@opetushallitus/oph-design-system';
import {
  styled as muiStyled,
  type Theme,
  type ThemeOptions,
} from '@mui/material/styles';
import { shouldForwardProp } from '@mui/system/createStyled';

export { ophColors } from '@opetushallitus/oph-design-system';

// MUI:sta (Emotionista) puuttuu styled-componentsin .attrs
// Tällä voi asettaa oletus-propsit ilman, että tarvii luoda välikomponenttia
/* eslint-disable @typescript-eslint/no-explicit-any */
export function withDefaultProps<P extends React.ComponentPropsWithoutRef<any>>(
  Component: ComponentType<P>,
  defaultProps: Partial<P>,
  displayName: string = 'ComponentWithDefaultProps',
) {
  const ComponentWithDefaultProps = (
    props: P & { ref?: React.Ref<ComponentRef<ComponentType<P>>> },
  ) => <Component {...defaultProps} {...props} />;

  ComponentWithDefaultProps.displayName = displayName;
  return ComponentWithDefaultProps;
}

const withTransientProps = (propName: string) =>
  // Emotion doesn't support transient props by default so add support manually
  shouldForwardProp(propName) && !propName.startsWith('$');

export const styled: typeof muiStyled = (
  tag: Parameters<typeof muiStyled>[0],
  options: Parameters<typeof muiStyled>[1] = {},
) => {
  return muiStyled(tag, {
    shouldForwardProp: (propName: string) =>
      (!options.shouldForwardProp || options.shouldForwardProp(propName)) &&
      withTransientProps(propName),
    ...options,
  });
};

export const DEFAULT_BOX_BORDER = `2px solid ${ophColors.grey100}`;

export const notLarge = (theme: Theme) => theme.breakpoints.down('lg');

export const THEME_OVERRIDES: ThemeOptions = {
  components: {
    MuiDialog: {
      defaultProps: {
        fullWidth: true,
      },
      styleOverrides: {
        paper: ({ theme }) => ({
          minHeight: '200px',
          borderRadius: '2px',
          boxShadow: '2px 2px 8px 0px rgba(0,0,0,0.17)',
          padding: theme.spacing(3),
        }),
      },
    },
    MuiDialogTitle: {
      defaultProps: {
        variant: 'h2',
      },
      styleOverrides: {
        root: ({ theme }) => ({
          padding: theme.spacing(0, 0, 2, 0),
        }),
      },
    },
    MuiDialogContent: {
      styleOverrides: {
        root: {
          padding: 0,
        },
      },
    },
    MuiDialogActions: {
      styleOverrides: {
        root: ({ theme }) => ({
          padding: theme.spacing(2, 0, 0, 0),
        }),
      },
    },
  },
};
