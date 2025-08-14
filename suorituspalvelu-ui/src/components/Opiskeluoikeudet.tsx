import { NDASH } from '@/lib/common';
import { currentFinnishDate, isInRange } from '@/lib/time-utils';
import { Circle } from '@mui/icons-material';
import { Box, Stack } from '@mui/material';
import {
  ophColors,
  OphLink,
  OphTypography,
} from '@opetushallitus/oph-design-system';
import { useTranslations } from '@/hooks/useTranslations';
import { formatDate } from 'date-fns';
import { PaperWithTopColor } from './PaperWithTopColor';
import { use } from 'react';
import { configPromise } from '@/configuration';
import { LabeledInfoItem } from './LabeledInfoItem';
import { getOppilaitosLinkUrl } from '@/lib/getOppilaitosLink';
import { Opiskeluoikeus } from '@/types/ui-types';

const VoimassaoloIndicator = ({
  voimassaolonAlku,
  voimassaolonLoppu,
}: {
  voimassaolonAlku?: Date;
  voimassaolonLoppu?: Date;
}) => {
  const { t } = useTranslations();

  const isVoimassa = isInRange(
    currentFinnishDate(),
    voimassaolonAlku,
    voimassaolonLoppu,
  );

  return (
    <Stack sx={{ alignItems: 'center', flexDirection: 'row', gap: 2 }}>
      <Box>
        {voimassaolonAlku ? formatDate(voimassaolonAlku, 'd.M.y') : ''}
        {` ${NDASH} `}
        {voimassaolonLoppu ? formatDate(voimassaolonLoppu, 'd.M.y') : ''}
      </Box>
      <Stack sx={{ alignItems: 'center', flexDirection: 'row', gap: 0.5 }}>
        <Circle
          sx={{
            fontSize: '18px',
            color: isVoimassa ? ophColors.green3 : ophColors.orange3,
          }}
        />
        <OphTypography>
          {isVoimassa ? t('oppija.voimassa') : t('oppija.ei-voimassa')}
        </OphTypography>
      </Stack>
    </Stack>
  );
};

export const Opiskeluoikeudet = ({
  opiskeluoikeudet,
}: {
  opiskeluoikeudet: Array<Opiskeluoikeus>;
}) => {
  const { t, translateKielistetty } = useTranslations();
  const config = use(configPromise);
  return (
    <Box data-test-id="opiskeluoikeudet">
      <OphTypography variant="h3" component="h2" sx={{ marginBottom: 2 }}>
        {t('oppija.opiskeluoikeudet')}
      </OphTypography>
      <Stack spacing={4}>
        {opiskeluoikeudet.map((oo) => {
          return (
            <PaperWithTopColor key={oo.tunniste} topColor={ophColors.red1}>
              <Stack
                direction="column"
                spacing={1}
                data-test-id="opiskeluoikeus-paper"
              >
                <OphTypography variant="label" sx={{ marginBottom: 1 }}>
                  {translateKielistetty(oo.nimi)}
                </OphTypography>
                <Stack direction="row" spacing={1}>
                  <LabeledInfoItem
                    label={t('oppija.oppilaitos')}
                    sx={{ flexBasis: '50%' }}
                    value={
                      <OphLink
                        href={getOppilaitosLinkUrl(config, oo.oppilaitos.oid)}
                        component="a"
                      >
                        {translateKielistetty(oo.oppilaitos.nimi)}
                      </OphLink>
                    }
                  />
                  <LabeledInfoItem
                    label={t('oppija.voimassaolo')}
                    sx={{ flexBasis: '50%' }}
                    value={
                      <VoimassaoloIndicator
                        voimassaolonAlku={oo.voimassaolonAlku}
                        voimassaolonLoppu={oo.voimassaolonLoppu}
                      />
                    }
                  />
                </Stack>
              </Stack>
            </PaperWithTopColor>
          );
        })}
      </Stack>
    </Box>
  );
};
