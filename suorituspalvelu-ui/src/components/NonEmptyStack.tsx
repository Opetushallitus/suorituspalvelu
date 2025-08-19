import { Stack, StackProps } from '@mui/material';
import React from 'react';
import { isTruthy } from 'remeda';

export const NonEmptyStack = ({ children, ...props }: StackProps) => {
  const filteredChildren = React.Children.toArray(children).filter((child) =>
    isTruthy(child),
  );

  return filteredChildren.length > 0 ? (
    <Stack {...props}>{filteredChildren}</Stack>
  ) : null;
};
