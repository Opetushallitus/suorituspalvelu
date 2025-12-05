import { Accordion, AccordionSummary, AccordionDetails } from '@mui/material';
import { ExpandMore } from '@mui/icons-material';
import { DEFAULT_BOX_BORDER } from '@/lib/theme';
import { OphTypography } from '@opetushallitus/oph-design-system';

export const AccordionBox = ({
  id,
  title,
  children,
  headingComponent = 'h3',
  defaultExpanded = true,
}: {
  id: string;
  title: React.ReactNode;
  headingComponent?: 'h1' | 'h2' | 'h3' | 'h4' | 'h5';
  children: React.ReactNode;
  defaultExpanded?: boolean;
}) => {
  const headerId = `${id}-accordion-header`;
  const contentId = `${id}-accordion-content`;

  return (
    <Accordion
      defaultExpanded={defaultExpanded}
      sx={{
        border: DEFAULT_BOX_BORDER,
        borderTop: (theme) => `4px solid ${theme.palette.primary.main}`,
        borderRadius: '4px',
      }}
      slotProps={{ heading: { component: headingComponent } }}
    >
      <AccordionSummary
        expandIcon={<ExpandMore />}
        aria-controls={contentId}
        sx={{ paddingY: 1 }}
        id={headerId}
      >
        <OphTypography variant={headingComponent} component="span">
          {title}
        </OphTypography>
      </AccordionSummary>
      <AccordionDetails
        sx={{
          borderTop: DEFAULT_BOX_BORDER,
        }}
      >
        {children}
      </AccordionDetails>
    </Accordion>
  );
};
