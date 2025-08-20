import { Box, Typography } from '@mui/material';
import { ListAlt } from '@mui/icons-material';
import React from 'react';
import { Avatar } from '@mui/material';
import { styled, ophColors } from '@/lib/theme';

export const IconCircle = styled(Avatar)({
  backgroundColor: ophColors.grey100,
  color: ophColors.grey500,
  width: '48px',
  height: '48px',
});

const Wrapper = styled(Box)(({ theme }) => ({
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  width: '100%',
  margin: theme.spacing(4),
  gap: theme.spacing(2),
}));

export const ResultPlaceholder = ({
  text,
  icon,
}: {
  text: React.ReactNode;
  icon?: React.ReactNode;
}) => {
  return (
    <Wrapper>
      <IconCircle>{icon ?? <ListAlt />}</IconCircle>
      <Typography component="div" variant="h3" sx={{ fontWeight: 'normal' }}>
        {text}
      </Typography>
    </Wrapper>
  );
};
