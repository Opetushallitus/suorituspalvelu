import { useTranslations } from '@/hooks/useTranslations';
import { styled } from '@/lib/theme';
import { TableBody, TableCell, TableRow } from '@mui/material';
import { sumBy } from 'remeda';

const SemiBold = styled('span')({
  fontWeight: '600',
});

export const KokonaislaajuusRow = ({
  osat,
  maxKokonaislaajuus,
}: {
  osat: Array<{ laajuus?: number }>;
  maxKokonaislaajuus: number;
}) => {
  const { t } = useTranslations();
  const totalLaajuus = sumBy(osat, (osa) => osa.laajuus ?? 0);
  return (
    <TableBody>
      <TableRow>
        <TableCell />
        <TableCell>
          {t('oppija.lyhenne-yhteensa')}{' '}
          <SemiBold>
            {totalLaajuus} / {maxKokonaislaajuus}{' '}
            {t('oppija.lyhenne-osaamispiste')}
          </SemiBold>
        </TableCell>
        <TableCell />
      </TableRow>
    </TableBody>
  );
};
