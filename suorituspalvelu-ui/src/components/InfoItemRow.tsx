/**
 * Komponentti, joka näyttää flexbox-rivin listalle lapsikomponentteja, suodattaa pois tyhjät lapsikomponentit ja
 * mahdollistaa asettelun tietyn sarakemäärän mukaan, vaikka lapsia olisi vähemmän.
 *
 * @param children Lapset, jotka näytetään rivissä.
 * @param slotAmount (valinnainen) Määrittää montako saraketta rivissä on. Jos ei anneta, käytetään lapsikomponenttien määrää.
 * @param props Muut StackProps-ominaisuudet, paitsi 'direction', joka on aina 'row'.
 *
 * @returns Flexbox-rivi lapsikomponenteille tai null, jos lapsia ei ole.
 */
import { truthyReactChildren } from '@/lib/common';
import { Box, Stack, StackProps } from '@mui/material';
import React from 'react';
import { range } from 'remeda';

export const InfoItemRow = ({
  children,
  slotAmount,
  ...props
}: Omit<StackProps, 'direction'> & { slotAmount?: number }) => {
  const filteredChildren = truthyReactChildren(children);

  return filteredChildren.length > 0 ? (
    <Stack {...props} direction="row">
      {range(0, slotAmount ?? filteredChildren.length - 1).map(
        (index) =>
          filteredChildren[index] ?? <Box key={index} sx={{ flex: '1 0 0' }} />,
      )}
    </Stack>
  ) : null;
};
