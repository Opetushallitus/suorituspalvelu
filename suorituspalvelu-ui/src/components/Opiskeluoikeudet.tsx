import { Opiskeluoikeus } from '@/api';
import { NDASH } from '@/common';
import { currentFinnishDate, isInRange } from '@/lib/time-utils';
import { Circle } from '@mui/icons-material';
import { Box, Stack } from '@mui/material';
import {
  ophColors,
  OphLink,
  OphTypography,
} from '@opetushallitus/oph-design-system';
import { useTranslate } from '@tolgee/react';
import { formatDate } from 'date-fns';
import { PaperWithTopColor } from './PaperWithTopColor';
import { use } from 'react';
import { configPromise } from '@/configuration';
import { LabeledInfoItem } from './LabeledInfoItem';
import { getOppilaitosLinkUrl } from '@/lib/getOppilaitosLink';

const VoimassaoloIndicator = ({
  voimassaolonAlku,
  voimassaolonLoppu,
}: {
  voimassaolonAlku?: Date;
  voimassaolonLoppu?: Date;
}) => {
  const { t } = useTranslate();

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
  const { t } = useTranslate();
  const config = use(configPromise);
  return (
    <Box data-test-id="opiskeluoikeudet">
      <OphTypography variant="h3" component="h2" sx={{ marginBottom: 2 }}>
        {t('oppija.opiskeluoikeudet')}
      </OphTypography>
      <Stack gap={4}>
        {opiskeluoikeudet.map((oo) => {
          return (
            <PaperWithTopColor
              key={`${oo.tutkinto}-${oo.oppilaitos.oid}`}
              topColor={ophColors.red1}
            >
              <Stack
                direction="column"
                gap={1}
                data-test-id="opiskeluoikeus-paper"
              >
                <OphTypography variant="label" sx={{ marginBottom: 1 }}>
                  {oo.tutkinto}
                </OphTypography>
                <Stack direction="row" gap={1}>
                  <LabeledInfoItem
                    label={t('oppija.oppilaitos')}
                    value={
                      <OphLink
                        href={getOppilaitosLinkUrl(config, oo.oppilaitos.oid)}
                      >
                        {oo.oppilaitos.nimi}
                      </OphLink>
                    }
                  />
                  <LabeledInfoItem
                    label={t('oppija.voimassaolo')}
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
