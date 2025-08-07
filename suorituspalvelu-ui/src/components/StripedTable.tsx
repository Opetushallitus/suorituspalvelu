import { DEFAULT_BOX_BORDER, ophColors, styled } from '@/lib/theme';
import { Box, Table, TableProps } from '@mui/material';

const StyledTable = styled(Table, {
  shouldForwardProp: (prop) => prop !== 'stripeGroup',
})<{ stripeGroup: 'row' | 'body' }>(({ theme, stripeGroup = 'row' }) => ({
  width: '100%',
  '& .MuiTableHead-root': {
    '& .MuiTableRow-root': {
      borderBottom: DEFAULT_BOX_BORDER,
    },
  },
  '& .MuiTableCell-root': {
    padding: theme.spacing(0, 0, 0, 2),
    textAlign: 'left',
    whiteSpace: 'pre-wrap',
    height: '42px',
    borderWidth: 0,
  },
  [stripeGroup === 'body' ? '& .MuiTableBody-root' : '& .MuiTableRow-root']: {
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
  props: TableProps & { stripeGroup?: 'row' | 'body' },
) => (
  <TableWrapper tabIndex={0}>
    <StyledTable {...props} stripeGroup={props.stripeGroup ?? 'row'} />
  </TableWrapper>
);
