'use client';

import { Button, Stack } from '@mui/material';
import {
  KeyboardDoubleArrowRight as KeyboardDoubleArrowRightIcon,
  KeyboardDoubleArrowLeft as KeyboardDoubleArrowLeftIcon,
} from '@mui/icons-material';
import React from 'react';
import { DEFAULT_BOX_BORDER, ophColors, styled } from '@/lib/theme';
import { OphButton } from '@opetushallitus/oph-design-system';
import { useTranslate } from '@tolgee/react';

const MINIMIZED_PANEL_WIDTH = '100px';

const StyledPanel = styled('div')({
  width: '17vw',
  minWidth: '300px',
  display: 'block',
  height: '100vh',
  borderRight: DEFAULT_BOX_BORDER,
  top: 0,
  position: 'sticky',
  '&.minimized': {
    minWidth: MINIMIZED_PANEL_WIDTH,
    width: 'auto',
  },
});

const ExpandButton = styled(OphButton)(({ theme }) => ({
  minWidth: '100%',
  width: '100%',
  position: 'relative',
  fontWeight: 'normal',
  height: '100%',
  color: ophColors.blue2,
  display: 'flex',
  flexDirection: 'column',
  justifyContent: 'flex-start',
  alignItems: 'center',
  padding: theme.spacing(1, 0),
  border: 0,
  borderRadius: 0,
  '& .MuiButton-icon': {
    margin: 0,
  },
  '&:hover': {
    backgroundColor: ophColors.lightBlue2,
  },
}));

export const LeftPanel = ({
  isOpen,
  setIsOpen,
  children,
}: {
  isOpen: boolean;
  setIsOpen: (open: boolean) => void;
  children: React.ReactNode;
}) => {
  const { t } = useTranslate();

  return (
    <StyledPanel className={isOpen ? '' : 'minimized'}>
      {isOpen ? (
        <Stack
          sx={{
            height: '100%',
            flexShrink: 0,
            alignItems: 'stretch',
            paddingLeft: 2,
          }}
        >
          <Button
            sx={{
              alignSelf: 'flex-end',
              '&.MuiButton-root': {
                padding: 1,
                paddingBottom: 0,
                paddingRight: 0.5,
                margin: 0,
                minWidth: 0,
                flexShrink: 0,
              },
            }}
            onClick={() => setIsOpen(false)}
            aria-label={t('sivupalkki.piilota')}
            startIcon={<KeyboardDoubleArrowLeftIcon />}
          />
          {children}
        </Stack>
      ) : (
        <ExpandButton
          onClick={() => setIsOpen(true)}
          aria-label={t('sivupalkki.nayta')}
          startIcon={<KeyboardDoubleArrowRightIcon />}
        >
          {t('sivupalkki.hakutulos')}
        </ExpandButton>
      )}
    </StyledPanel>
  );
};
