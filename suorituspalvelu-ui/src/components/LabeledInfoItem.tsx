import { Box } from '@mui/material';
import React, { useId } from 'react';
import { styled } from '@/lib/theme';

const InfoLabel = styled('label')(({ theme }) => ({
  ...theme.typography.label,
}));

export const LabeledInfoItem = ({
  label,
  value,
}: {
  label: string;
  value: React.ReactNode;
}) => {
  const labelId = useId();
  return (
    <div>
      <InfoLabel id={labelId}>{label}</InfoLabel>
      <Box aria-labelledby={labelId}>{value}</Box>
    </div>
  );
};
