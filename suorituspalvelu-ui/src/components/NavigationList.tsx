import { ophColors, styled } from '@/lib/theme';

export const NAV_LIST_SELECTED_ITEM_CLASS = 'navigation-list--item-selected';

export const NavigationList = styled('nav')(({ theme }) => ({
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'stretch',
  width: '100%',
  textAlign: 'left',
  overflowY: 'auto',
  height: 'auto',
  gap: theme.spacing(0.5),
  paddingRight: theme.spacing(2),
  '& > a': {
    display: 'block',
    padding: theme.spacing(0.5, 1.5),
    cursor: 'pointer',
    color: ophColors.blue2,
    textDecoration: 'none',
    borderRadius: '0',
    '&:nth-of-type(even)': {
      backgroundColor: ophColors.grey50,
    },
    [`&:hover, &.${NAV_LIST_SELECTED_ITEM_CLASS}`]: {
      backgroundColor: ophColors.lightBlue2,
    },
    '&:focus-visible': {
      outlineOffset: '-2px',
    },
  },
}));
