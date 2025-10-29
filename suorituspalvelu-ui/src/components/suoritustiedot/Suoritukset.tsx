import { Box, Stack, ToggleButton, ToggleButtonGroup } from '@mui/material';
import { OphTypography } from '@opetushallitus/oph-design-system';
import { useTranslate } from '@tolgee/react';
import { SuorituksetKoulutustyypeittain } from './SuorituksetKoulutustyypeittain';
import { useState } from 'react';
import type { OppijanTiedot } from '@/types/ui-types';
import { SuorituksetAikajarjestyksessa } from './SuorituksetAikajarjestyksessa';
import { AddSuoritusFields } from './AddSuoritusFields';

type SuoritusOrder = 'koulutustyypeittain' | 'uusin-ensin';

export function Suoritukset({
  oppijanTiedot,
}: {
  oppijanTiedot: OppijanTiedot;
}) {
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
        <OphTypography
          variant="h3"
          component="h2"
          sx={{ marginBottom: 2, alignSelf: 'flex-end' }}
        >
          {t('oppija.suoritukset')}
        </OphTypography>
        <ToggleButtonGroup
          sx={{ marginBottom: suoritusOrder === 'uusin-ensin' ? 2 : 0 }}
          value={suoritusOrder}
          exclusive
          onChange={(_event, newValue) => {
            if (!newValue) {
              return;
            }
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
      <AddSuoritusFields oppijanTiedot={oppijanTiedot} />
      {suoritusOrder === 'koulutustyypeittain' ? (
        <SuorituksetKoulutustyypeittain oppijanTiedot={oppijanTiedot} />
      ) : (
        <SuorituksetAikajarjestyksessa oppijanTiedot={oppijanTiedot} />
      )}
    </Box>
  );
}
