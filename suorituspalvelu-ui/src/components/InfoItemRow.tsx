import { Box, Stack, StackProps } from '@mui/material';
import React from 'react';
import { isTruthy, range } from 'remeda';

export const InfoItemRow = ({
  children,
  slotAmount,
  ...props
}: Omit<StackProps, 'direction'> & { slotAmount?: number }) => {
  const filteredChildren = React.Children.toArray(children).filter((child) =>
    isTruthy(child),
  );

  return filteredChildren.length > 0 ? (
    <Stack {...props} direction="row">
      {range(0, slotAmount ?? filteredChildren.length - 1).map(
        (index) =>
          filteredChildren[index] ?? <Box key={index} sx={{ flex: '1 0 0' }} />,
      )}
    </Stack>
  ) : null;
};
