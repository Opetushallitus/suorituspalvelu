import React, { useState } from 'react';
import { Box, TableBody, TableCell, TableRow } from '@mui/material';
import { OphButton, ophColors } from '@opetushallitus/oph-design-system';
import { DEFAULT_BOX_BORDER, styled } from '@/lib/theme';
import { ExpandMore } from '@mui/icons-material';
import { useTranslations } from '@/hooks/useTranslations';
import { truthyReactNodes, toId } from '@/lib/common';

const AccordionHeaderCell = styled(TableCell)(({ theme }) => ({
  ...theme.typography.h5,
  gap: theme.spacing(1),
}));

const TableRowAccordionBody = styled(TableBody)({
  '& > tr': {
    width: '100%',
    border: 'none',
    '& > .MuiTableCell-root': {
      minHeight: 0,
      lineHeight: '24px',
      width: '100%',
      border: 'none',
    },
  },
  '& .table-row-accordion-content': {
    borderBottom: DEFAULT_BOX_BORDER,
    '& > .MuiTableCell-root': {
      paddingLeft: '40px',
      paddingRight: 0,
      backgroundColor: ophColors.white,
    },
  },
});

/**
 * TableRowAccordion yhdistää taulukon rivin ja Accordion-komponentin toiminnallisuutta.
 * Komponentti näyttää taulukkorivin, jonka ensimmäinen solu on accordion-otsikko, jota klikkaamalla
 * voidaan avata tai sulkea accordionin sisältö. Semanttisesti komponentissa on kaksi TableBody-elementin
 * sisällä kaksi TableRow-elementtiä:
 * - Ensimmäinen TableRow-elementti sisältää Accordion-elementin otsikkorivin.
 * - Avattava sisältö näytetään toisessa TableRow-elementissä yhdessä solussa, joka on koko taulukon levyinen.
 *
 *
 * @param title Otsikko, joka näytetään accordion-otsikkorivin ensimmäisessä solussa.
 * @param otherCells Accordion-otsikkorivin muut solut, jotka näytetään otsikkosolun jälkeen.
 * @param children Sisältö, joka näytetään avattavassa osiossa. Jos tyhjä, ei näytetä accordionia lainkaan.
 * @param contentCellStyle Tyylimäärittelyt avattavan sisällön solulle.
 */
export const TableRowAccordion = ({
  title,
  children,
  otherCells,
}: {
  title: string;
  otherCells: Array<React.ReactNode>;
  children: React.ReactNode;
  contentCellStyle?: React.CSSProperties;
}) => {
  const { t } = useTranslations();

  const [isOpen, setIsOpen] = useState(false);

  const headerId = `accordion-table-header-${toId(title)}`;
  const contentId = `accordion-table-content-${toId(title)}`;

  const toggleButtonTitle = isOpen
    ? `${t('oppija.nayta')} ${title}`
    : `${t('oppija.piilota')} ${title}`;

  const columnCount = (otherCells?.length ?? 0) + 1;

  const hasChildren =
    // eslint-disable-next-line @eslint-react/no-children-to-array
    truthyReactNodes(React.Children.toArray(children)).length > 0;

  return (
    <TableRowAccordionBody>
      <TableRow>
        {hasChildren ? (
          <AccordionHeaderCell id={headerId}>
            <OphButton
              variant="text"
              aria-label={toggleButtonTitle}
              sx={{
                fontWeight: 400,
                padding: 0,
                display: 'inline-flex',
                textAlign: 'left',
              }}
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
        {otherCells}
      </TableRow>
      {hasChildren && (
        <TableRow
          className="table-row-accordion-content"
          sx={{ display: isOpen ? 'table-row' : 'none' }}
        >
          <TableCell colSpan={columnCount}>
            <Box id={contentId} role="region" aria-labelledby={headerId}>
              {children}
            </Box>
          </TableCell>
        </TableRow>
      )}
    </TableRowAccordionBody>
  );
};
