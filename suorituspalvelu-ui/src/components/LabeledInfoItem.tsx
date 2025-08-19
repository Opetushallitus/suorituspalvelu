import { Box } from '@mui/material';
import React, { useId } from 'react';
import { styled } from '@/lib/theme';

const InfoLabel = styled('label')(({ theme }) => ({
  ...theme.typography.label,
}));

export const LabeledInfoItem = ({
  label,
  value,
  sx,
}: {
  label: string;
  value: React.ReactNode;
  sx?: React.CSSProperties;
}) => {
  const labelId = useId();
  return (
    <Box sx={{ flex: '1 0 0', marginRight: 1, ...sx }}>
      <InfoLabel id={labelId}>{label}</InfoLabel>
      <Box aria-labelledby={labelId}>{value}</Box>
    </Box>
  );
};
