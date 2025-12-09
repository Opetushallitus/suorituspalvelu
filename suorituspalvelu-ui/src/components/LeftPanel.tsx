import { Stack } from '@mui/material';
import React from 'react';
import { DEFAULT_BOX_BORDER, styled } from '@/lib/theme';

const StyledPanel = styled(Stack)({
  width: '19vw',
  minWidth: '350px',
  display: 'block',
  height: '100vh',
  borderRight: DEFAULT_BOX_BORDER,
  top: 0,
  position: 'sticky',
});

export const LeftPanel = ({ children }: { children: React.ReactNode }) => {
  return (
    <StyledPanel>
      <Stack
        sx={{
          height: '100%',
          flexShrink: 0,
          alignItems: 'stretch',
        }}
      >
        {children}
      </Stack>
    </StyledPanel>
  );
};
