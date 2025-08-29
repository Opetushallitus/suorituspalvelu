import { formatFinnishDate, NDASH } from '@/lib/common';
import { Circle } from '@mui/icons-material';
import { Box, Stack } from '@mui/material';
import { ophColors, OphTypography } from '@opetushallitus/oph-design-system';
import { useTranslations } from '@/hooks/useTranslations';
import { PaperWithTopColor } from './PaperWithTopColor';
import { LabeledInfoItem } from './LabeledInfoItem';
import { Opiskeluoikeus } from '@/types/ui-types';
import { OppilaitosInfoItem } from '@/components/OppilaitosInfoItem';

const VoimassaoloIndicator = ({
  opiskeluoikeus,
}: {
  opiskeluoikeus: Opiskeluoikeus;
}) => {
  const { t, translateKielistetty } = useTranslations();

  const tilaIndicatorColor = () => {
    switch (opiskeluoikeus.supaTila.toString()) {
      case 'VOIMASSA':
        return ophColors.green3;
      case 'EI_VOIMASSA':
        return ophColors.orange3;
      case 'PAATTYNYT':
        return ophColors.red2;
    }
  };

  const tilaTranslationKey = () => {
    switch (opiskeluoikeus.supaTila.toString()) {
      case 'VOIMASSA':
        return 'oppija.voimassa';
      case 'EI_VOIMASSA':
        return 'oppija.ei-voimassa';
      case 'PAATTYNYT':
        return 'oppija.paattynyt';
      default:
        throw new Error('Tuntematon SUPA-tila');
    }
  };

  return (
    <Stack sx={{ alignItems: 'center', flexDirection: 'row', gap: 2 }}>
      <Box>
        {formatFinnishDate(opiskeluoikeus.voimassaolonAlku)}
        {` ${NDASH} `}
        {formatFinnishDate(opiskeluoikeus.voimassaolonLoppu)}
      </Box>
      <Stack sx={{ alignItems: 'center', flexDirection: 'row', gap: 0.5 }}>
        <Circle
          sx={{
            fontSize: '18px',
            color: tilaIndicatorColor(),
          }}
        />
        <OphTypography>{t(tilaTranslationKey())}</OphTypography>
        <OphTypography>
          ({translateKielistetty(opiskeluoikeus.virtaTila)})
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
                <OphTypography
                  variant="label"
                  component="h3"
                  sx={{ marginBottom: 1 }}
                >
                  {translateKielistetty(oo.nimi)}
                </OphTypography>
                <Stack direction="row">
                  <OppilaitosInfoItem oppilaitos={oo.oppilaitos} />
                  <LabeledInfoItem
                    label={t('oppija.voimassaolo')}
                    value={<VoimassaoloIndicator opiskeluoikeus={oo} />}
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
