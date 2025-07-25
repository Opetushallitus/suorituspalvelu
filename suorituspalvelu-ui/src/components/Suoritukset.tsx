import { OppijaResponse } from '@/api';
import { Box, Stack, ToggleButton, ToggleButtonGroup } from '@mui/material';
import { OphTypography } from '@opetushallitus/oph-design-system';
import { useTranslate } from '@tolgee/react';
import { SuorituksetKoulutustyypeittain } from './SuorituksetKoulutustyypeittain';
import { useState } from 'react';

type SuoritusOrder = 'koulutustyypeittain' | 'uusin-ensin';

export function Suoritukset({ tiedot }: { tiedot: OppijaResponse }) {
  const { t } = useTranslate();

  const [suoritusOrder, setSuoritusOrder] = useState<SuoritusOrder>(
    'koulutustyypeittain',
  );

  return (
    <Box data-test-id="suoritukset">
      <Stack
        direction="row"
        sx={{ justifyContent: 'space-between', alignItems: 'flex-start' }}
      >
        <OphTypography variant="h3" component="h2" sx={{ marginBottom: 2 }}>
          {t('oppija.suoritukset')}
        </OphTypography>
        <ToggleButtonGroup
          value={suoritusOrder}
          exclusive
          onChange={(_event, newValue) => {
            setSuoritusOrder(newValue);
          }}
        >
          <ToggleButton value="koulutustyypeittain">
            {t('oppija.koulutustyypeittain')}
          </ToggleButton>
          <ToggleButton value="uusin-ensin">
            {t('oppija.uusin-ensin')}
          </ToggleButton>
        </ToggleButtonGroup>
      </Stack>
      {suoritusOrder === 'koulutustyypeittain' ? (
        <SuorituksetKoulutustyypeittain tiedot={tiedot} />
      ) : (
        <div>TODO: Suoritukset uusin ensin -järjestyksessä</div>
      )}
    </Box>
  );
}
