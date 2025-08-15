import React, { useState } from 'react';
import { TableBody, TableCell, TableRow } from '@mui/material';
import { OphButton, ophColors } from '@opetushallitus/oph-design-system';
import { DEFAULT_BOX_BORDER, styled } from '@/lib/theme';
import { ChevronRight } from '@mui/icons-material';
import { useTranslations } from '@/hooks/useTranslations';

const AccordionHeaderCell = styled(TableCell)(({ theme }) => ({
  ...theme.typography.h5,
  display: 'flex',
  justifyContent: 'flex-start',
  gap: theme.spacing(1),
  alignItems: 'center',
  paddingLeft: 0,
}));

export const AccordionTableItem = ({
  title,
  children,
  headingCells,
}: {
  title: string;
  headingCells: Array<React.ReactNode>;
  children: React.ReactNode;
}) => {
  const { t } = useTranslations();

  const [isOpen, setIsOpen] = useState(false);

  const headerId = `accordion-table-header-${title}`;
  const contentId = `accordion-table-content-${title}`;

  const toggleButtonTitle = isOpen
    ? `${t('oppija.nayta')} ${title}`
    : `${t('oppija.piilota')} ${title}`;

  const columnCount = (headingCells?.length ?? 0) + 1;

  return (
    <>
      <TableBody>
        <TableRow
          sx={{
            width: '100%',
            borderTop: DEFAULT_BOX_BORDER,
          }}
        >
          <AccordionHeaderCell>
            <OphButton
              id={headerId}
              variant="text"
              aria-label={toggleButtonTitle}
              sx={{ color: ophColors.black }}
              startIcon={
                <ChevronRight
                  sx={{
                    transform: isOpen ? 'rotate(90deg)' : 'none',
                    transition: 'transform 0.15s ease-in-out',
                  }}
                />
              }
              onClick={() => setIsOpen((open) => !open)}
              aria-controls={contentId}
              aria-expanded={isOpen ? 'true' : 'false'}
            >
              {title}
            </OphButton>
          </AccordionHeaderCell>
          {headingCells}
        </TableRow>
      </TableBody>
      <TableBody
        role="region"
        id={contentId}
        aria-labelledby={headerId}
        sx={{
          minHeight: 0,
          display: isOpen ? 'table-row-group' : 'none',
        }}
      >
        <TableRow>
          <TableCell colSpan={columnCount}>{children}</TableCell>
        </TableRow>
      </TableBody>
    </>
  );
};
