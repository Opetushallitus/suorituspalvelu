import React, { useState } from 'react';
import { TableBody, TableCell, TableRow } from '@mui/material';
import { OphButton, ophColors } from '@opetushallitus/oph-design-system';
import { DEFAULT_BOX_BORDER, styled } from '@/lib/theme';
import { ExpandMore } from '@mui/icons-material';
import { useTranslations } from '@/hooks/useTranslations';
import { isTruthy } from 'remeda';
import { toId } from '@/lib/common';

const AccordionHeaderCell = styled(TableCell)(({ theme }) => ({
  ...theme.typography.h5,
  display: 'flex',
  justifyContent: 'flex-start',
  gap: theme.spacing(1),
  alignItems: 'center',
  paddingLeft: 0,
}));

function hasNonEmptyChildren(children: React.ReactNode) {
  const filteredChildren = React.Children.toArray(children).filter((child) =>
    isTruthy(child),
  );
  return filteredChildren.length > 0;
}

export const AccordionTableItem = ({
  title,
  children,
  headingCells,
  cellStyle,
}: {
  title: string;
  headingCells: Array<React.ReactNode>;
  children: React.ReactNode;
  cellStyle?: React.CSSProperties;
}) => {
  const { t } = useTranslations();

  const [isOpen, setIsOpen] = useState(false);

  const headerId = `accordion-table-header-${toId(title)}`;
  const contentId = `accordion-table-content-${toId(title)}`;

  const toggleButtonTitle = isOpen
    ? `${t('oppija.nayta')} ${title}`
    : `${t('oppija.piilota')} ${title}`;

  const columnCount = (headingCells?.length ?? 0) + 1;

  const hasChildren = hasNonEmptyChildren(children);

  return (
    <>
      <TableBody>
        <TableRow
          sx={{
            width: '100%',
            borderTop: DEFAULT_BOX_BORDER,
          }}
        >
          {hasChildren ? (
            <AccordionHeaderCell id={headerId}>
              <OphButton
                variant="text"
                aria-label={toggleButtonTitle}
                sx={{ fontWeight: 400 }}
                startIcon={
                  <ExpandMore
                    sx={{
                      transform: isOpen ? 'rotate(180deg)' : 'none',
                      color: ophColors.grey900,
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
          ) : (
            <TableCell>{title}</TableCell>
          )}
          {headingCells}
        </TableRow>
      </TableBody>
      {hasChildren && (
        <TableBody
          sx={{
            minHeight: 0,
            display: isOpen ? 'table-row-group' : 'none',
          }}
        >
          <TableRow>
            <TableCell
              id={contentId}
              role="region"
              colSpan={columnCount}
              sx={cellStyle}
              aria-labelledby={headerId}
            >
              {children}
            </TableCell>
          </TableRow>
        </TableBody>
      )}
    </>
  );
};
