import { ophColors, styled } from '@/lib/theme';
import { Box, Table, TableCell, TableProps } from '@mui/material';

export const StripedTableHeaderCell = styled(TableCell)(({ theme }) => ({
  padding: theme.spacing(0, 0, 1, 2),
  textAlign: 'left',
}));

const StyledTable = styled(Table, {
  shouldForwardProp: (prop) => prop !== 'stripeTarget',
})<{ stripeTarget: 'row' | 'body' }>(({ theme, stripeTarget = 'row' }) => ({
  width: '100%',
  '& .MuiTableCell-root': {
    padding: theme.spacing(0, 0, 0, 2),
    textAlign: 'left',
    whiteSpace: 'pre-wrap',
    height: '42px',
    borderWidth: 0,
  },
  [stripeTarget === 'body' ? '& .MuiTableBody-root' : '& .MuiTableRow-root']: {
    '&:nth-of-type(even)': {
      '.MuiTableCell-root': {
        backgroundColor: ophColors.grey50,
      },
    },
    '&:nth-of-type(odd)': {
      '.MuiTableCell-root': {
        backgroundColor: ophColors.white,
      },
    },
  },
  '& .MuiTableRow-root:hover': {
    '& .MuiTableCell-root': {
      backgroundColor: ophColors.lightBlue2,
    },
  },
}));

export const TableWrapper = styled(Box)(({ theme }) => ({
  position: 'relative',
  display: 'block',
  overflowX: 'auto',
  rowGap: theme.spacing(1),
  alignSelf: 'stretch',
}));

export const StripedTable = (
  props: TableProps & { stripeTarget?: 'row' | 'body' },
) => (
  <TableWrapper tabIndex={0}>
    <StyledTable {...props} stripeTarget={props.stripeTarget ?? 'row'} />
  </TableWrapper>
);
