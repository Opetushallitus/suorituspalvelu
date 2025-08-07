'use client';

import {
  Box,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
} from '@mui/material';
import React from 'react';
import { ophColors } from '@opetushallitus/oph-design-system';
import { DEFAULT_BOX_BORDER, styled } from '@/lib/theme';
import { useTranslate } from '@tolgee/react';
import { EMPTY_ARRAY } from '@/lib/common';

import { memo } from 'react';

const StyledHeaderCell = styled(TableCell)(({ theme }) => ({
  padding: theme.spacing(0, 0, 1, 2),
  '&:last-child': {
    paddingRight: theme.spacing(2),
  },
  textAlign: 'left',
  'button:focus': {
    color: ophColors.blue2,
  },
}));

export const TableHeaderCell = memo(function TableHeaderCell({
  title,
  style,
}: {
  title?: React.ReactNode;
  style?: React.CSSProperties;
}) {
  return <StyledHeaderCell sx={style}>{title}</StyledHeaderCell>;
});

export type KeysMatching<O, T> = {
  [K in keyof O]: O[K] extends T ? K : never;
}[keyof O & string];

export interface ListTableColumn<R> {
  // Sarakkeen otsikko
  title?: string;
  // Sarakkeen avain. Käytetään Reactin key-attribuuttina sekä järjestettäessä kentän tunnisteena. Järjestettäessä tulee siis olla polku Row-olioon.
  key: string;
  // Funktio, joka renderöi solun sisällön.
  render: (props: R) => React.ReactNode;
  // Tavallisen (ei-otsikkosolu) tyylimäärittelyt
  style?: React.CSSProperties;
}

export interface Row {
  [key: string]: unknown;
}

const StyledTable = styled(Table)({
  width: '100%',
});

const StyledTableBody = styled(TableBody)(({ theme }) => ({
  '& .MuiTableCell-root': {
    padding: theme.spacing(0, 0, 0, 2),
    textAlign: 'left',
    whiteSpace: 'pre-wrap',
    height: '42px',
    borderWidth: 0,
  },
  '& .MuiTableRow-root': {
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
    '&:hover': {
      '.MuiTableCell-root': {
        backgroundColor: ophColors.lightBlue2,
      },
    },
  },
}));
export type ListTablePaginationProps = {
  page: number;
  setPage: (page: number) => void;
  pageSize: number;
  label?: string;
};

interface ListTableProps<T> extends React.ComponentProps<typeof StyledTable> {
  columns?: Array<ListTableColumn<T>>;
  rows?: Array<T>;
  sort?: string;
  setSort?: (sort: string) => void;
  translateHeader?: boolean;
  rowKeyProp: keyof T;
}

const TableWrapper = styled(Box)(({ theme }) => ({
  position: 'relative',
  display: 'block',
  overflowX: 'auto',
  rowGap: theme.spacing(1),
  alignSelf: 'stretch',
}));

export function ListTable<T>({
  columns = EMPTY_ARRAY as Array<ListTableColumn<T>>,
  rows = EMPTY_ARRAY as Array<T>,
  rowKeyProp,
  translateHeader = true,
  ...props
}: ListTableProps<T>) {
  const { t } = useTranslate();

  return (
    <Stack spacing={1} sx={{ alignItems: 'center', width: '100%' }}>
      <TableWrapper tabIndex={0}>
        <StyledTable {...props}>
          <TableHead>
            <TableRow sx={{ borderBottom: DEFAULT_BOX_BORDER }}>
              {columns.map((columnProps) => {
                const { key, title, style } = columnProps;
                return (
                  <TableHeaderCell
                    key={key.toString()}
                    title={translateHeader ? t(title ?? '') : title}
                    style={style}
                  />
                );
              })}
            </TableRow>
          </TableHead>
          <StyledTableBody>
            {rows.map((rowProps) => {
              const rowId = rowProps?.[rowKeyProp] as string;
              return (
                <TableRow key={rowId}>
                  {columns.map(({ key: columnKey, render, style }) => {
                    return (
                      <TableCell key={columnKey.toString()} sx={style}>
                        {render(rowProps)}
                      </TableCell>
                    );
                  })}
                </TableRow>
              );
            })}
          </StyledTableBody>
        </StyledTable>
      </TableWrapper>
    </Stack>
  );
}
