import React, { useState } from 'react';
import { Box, TableBody, TableCell, TableRow } from '@mui/material';
import { OphButton, ophColors } from '@opetushallitus/oph-design-system';
import { DEFAULT_BOX_BORDER, styled } from '@/lib/theme';
import { ExpandMore } from '@mui/icons-material';
import { useTranslations } from '@/hooks/useTranslations';
import { truthyReactChildren, toId } from '@/lib/common';

const AccordionHeaderCell = styled(TableCell)(({ theme }) => ({
  ...theme.typography.h5,
  display: 'flex',
  justifyContent: 'flex-start',
  gap: theme.spacing(1),
  alignItems: 'center',
  paddingLeft: 0,
}));

/**
 * TableBodyAccordion yhdistää taulukon rivin ja Accordion-komponentin toiminnallisuutta.
 * Komponentti näyttää taulukkorivin, jonka ensimmäinen solu on accordion-otsikko, jota klikkaamalla
 * voidaan avata tai sulkea accordionin sisältö. Semanttisesti komponentissa on kaksi tbody-elementtiä:
 * yksi accordion-otsikkoriville ja toinen accordionin sisällölle.
 * Avattava sisältö näytetään yhdessä solussa, joka on koko taulukon levyinen.
 *
 * @param title Otsikko, joka näytetään accordion-otsikkorivin ensimmäisessä solussa.
 * @param otherCells Accordion-otsikkorivin muut solut, jotka näytetään otsikkosolun jälkeen.
 * @param children Sisältö, joka näytetään avattavassa osiossa. Jos tyhjä, ei näytetä accordionia lainkaan.
 * @param contentCellStyle Tyylimäärittelyt avattavan sisällön solulle.
 */
export const TableBodyAccordion = ({
  title,
  children,
  otherCells,
  contentCellStyle,
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

  const hasChildren = truthyReactChildren(children).length > 0;

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
          {otherCells}
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
            <TableCell colSpan={columnCount} sx={contentCellStyle}>
              <Box id={contentId} role="region" aria-labelledby={headerId}>
                {children}
              </Box>
            </TableCell>
          </TableRow>
        </TableBody>
      )}
    </>
  );
};
