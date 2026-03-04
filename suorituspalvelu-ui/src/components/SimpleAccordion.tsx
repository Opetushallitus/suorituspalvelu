import { ExpandMore } from '@mui/icons-material';
import { Box } from '@mui/material';
import {
  OphButton,
  ophColors,
  OphTypography,
} from '@opetushallitus/oph-design-system';
import { useId, useState } from 'react';

const TRANSITION_DURATION_MS = 200;

export const SimpleAccordion = ({
  titleOpen,
  titleClosed,
  ariaLabelOpen,
  ariaLabelClosed,
  titleVariant = 'body1',
  titleComponent = 'label',
  children,
}: {
  titleOpen: string;
  titleClosed: string;
  ariaLabelClosed?: string;
  ariaLabelOpen?: string;
  titleVariant?: 'body1' | 'label' | 'h1' | 'h2' | 'h3' | 'h4' | 'h5';
  titleComponent?: 'label' | 'h1' | 'h2' | 'h3' | 'h4' | 'h5';
  children: React.ReactNode;
}) => {
  const [isOpen, setIsOpen] = useState(false);

  const accordionId = useId();
  const contentId = `SimpleAccordionContent_${accordionId}`;

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'flex-start',
        '&.MuiButton-icon': {
          marginRight: 0.5,
        },
      }}
    >
      <OphButton
        variant="text"
        sx={{ paddingX: 0 }}
        startIcon={
          <ExpandMore
            sx={{
              transform: isOpen ? 'rotate(-180deg)' : 'none',
              transition: `transform ${TRANSITION_DURATION_MS}ms ease`,
              color: ophColors.black,
            }}
          />
        }
        onClick={() => setIsOpen((open) => !open)}
        aria-controls={contentId}
        aria-expanded={isOpen ? 'true' : 'false'}
        aria-label={
          isOpen
            ? (ariaLabelOpen ?? titleOpen)
            : (ariaLabelClosed ?? titleClosed)
        }
      >
        <OphTypography
          component={titleComponent}
          variant={titleVariant}
          sx={{
            color: ophColors.blue2,
            fontWeight: 'normal',
            cursor: 'pointer',
          }}
        >
          {isOpen ? titleOpen : titleClosed}
        </OphTypography>
      </OphButton>
      <Box
        id={contentId}
        aria-hidden={!isOpen}
        sx={{
          width: '100%',
          display: 'grid',
          gridTemplateRows: isOpen ? '1fr' : '0fr',
          transition: `${TRANSITION_DURATION_MS}ms grid-template-rows ease`,
        }}
      >
        <Box sx={{ overflow: 'hidden' }}>{children}</Box>
      </Box>
    </Box>
  );
};
