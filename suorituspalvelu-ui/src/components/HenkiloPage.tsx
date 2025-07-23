'use client';
import { Opiskeluoikeus } from '@/api';
import { FullSpinner } from '@/components/FullSpinner';
import { useOppija } from '@/queries';
import { Box, Stack } from '@mui/material';
import { ophColors, OphTypography } from '@opetushallitus/oph-design-system';
import { Suspense } from 'react';
import { PaperWithTopColor } from './PaperWithTopColor';
import { useTranslate } from '@tolgee/react';
import { LabeledInfoItem } from './LabeledInfoItem';
import { Circle } from '@mui/icons-material';
import { currentFinnishDate, isInRange } from '@/lib/time-utils';
import { formatDate } from 'date-fns';
import { NDASH } from '@/common';

const VoimassaoloBadge = ({
  voimassaolonAlku,
  voimassaolonLoppu,
}: {
  voimassaolonAlku?: string;
  voimassaolonLoppu?: string;
}) => {
  const { t } = useTranslate();

  const alkuDate = voimassaolonAlku ? new Date(voimassaolonAlku) : undefined;
  const loppuDate = voimassaolonLoppu ? new Date(voimassaolonLoppu) : undefined;

  const isVoimassa = isInRange(
    currentFinnishDate(),
    voimassaolonAlku,
    voimassaolonLoppu,
  );

  return (
    <Stack sx={{ alignItems: 'center', flexDirection: 'row', gap: 2 }}>
      <Box>
        {alkuDate ? formatDate(alkuDate, 'd.M.y') : ''}
        {` ${NDASH} `}
        {loppuDate ? formatDate(loppuDate, 'd.M.y') : ''}
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

const Opiskeluoikeudet = ({
  opiskeluoikeudet,
}: {
  opiskeluoikeudet: Array<Opiskeluoikeus>;
}) => {
  const { t } = useTranslate();
  return (
    <Box>
      <OphTypography variant="h2" component="h5" sx={{ marginBottom: 2 }}>
        {t('oppija.opiskeluoikeudet')}
      </OphTypography>
      {opiskeluoikeudet.map((oo) => {
        return (
          <PaperWithTopColor key={`${oo.tutkinto}-${oo.oppilaitos.oid}`} topColor={ophColors.red1}>
            <Stack direction="column" gap={1}>
              <OphTypography variant="label" sx={{ marginBottom: 1 }}>
                {oo.tutkinto}
              </OphTypography>
              <Stack
                direction="row"
                gap={1}
                sx={{
                  '& > *': { flex: '1 1 auto' },
                }}
              >
                <LabeledInfoItem
                  label={t('oppija.oppilaitos')}
                  value={oo.oppilaitos.nimi}
                />
                <LabeledInfoItem
                  label={t('oppija.voimassaolo')}
                  value={
                    <VoimassaoloBadge
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
    </Box>
  );
};

const OppijaContent = ({ oppijaNumero }: { oppijaNumero: string }) => {
  const { data: tiedot } = useOppija(oppijaNumero);
  return (
    <Stack sx={{ margin: 2, gap: 2 }}>
      <Opiskeluoikeudet opiskeluoikeudet={tiedot?.opiskeluoikeudet} />
    </Stack>
  );
};

export default function HenkiloPage({
  oppijaNumero,
}: {
  oppijaNumero: string;
}) {
  return (
    <div>
      <Suspense fallback={<FullSpinner />}>
        <OppijaContent oppijaNumero={oppijaNumero} />
      </Suspense>
    </div>
  );
}
