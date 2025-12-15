import React from 'react';

import { Tooltip } from '@mui/material';

import { InfoOutline } from '@mui/icons-material';
import { ophColors } from '@opetushallitus/oph-design-system';
import { styled } from '@/lib/theme';

const Root = styled('div')(({ theme }) => ({
  display: 'inline-block',
  verticalAlign: 'top',
  paddingInline: '1px',
  '& .MuiTooltip-tooltip': {
    backgroundColor: ophColors.white,
    cursor: 'auto',
    userSelect: 'all',
    color: ophColors.grey900,
    padding: theme.spacing(1.5),
    filter: `drop-shadow(0px 2px 4px rgba(0, 0, 0, 0.5))`,
    maxWidth: '450px',
  },
  '& .MuiTooltip-arrow': {
    color: ophColors.white,
  },
  '& .MuiSvgIcon-root': {
    color: ophColors.blue2,
  },
}));

type Props = {
  children: React.JSX.Element | string;
  icon?: React.JSX.Element;
  ariaLabel?: string;
};

export const TooltipIcon = ({ children, icon, ariaLabel }: Props) => {
  return (
    <Root>
      <Tooltip
        slotProps={{
          popper: {
            disablePortal: true,
            onClick: (e) => e.stopPropagation(),
          },
        }}
        describeChild
        arrow
        title={children}
      >
        <span
          role="img"
          aria-label={ariaLabel}
          tabIndex={0}
          style={{ display: 'inline-flex', alignItems: 'center' }}
        >
          {icon ?? <InfoOutline />}
        </span>
      </Tooltip>
    </Root>
  );
};
