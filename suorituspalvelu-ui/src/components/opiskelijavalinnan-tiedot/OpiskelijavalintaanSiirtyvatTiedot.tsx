import { Stack } from '@mui/material';
import { OphButton } from '@opetushallitus/oph-design-system';
import { useApiSuspenseQuery } from '@/lib/http-client';
import { queryOptionsGetValintadata } from '@/lib/suorituspalvelu-queries';
import { useTranslations } from '@/hooks/useTranslations';
import { AccordionBox } from '@/components/AccordionBox';
import { Add } from '@mui/icons-material';
import { AvainArvotSection } from './AvainArvotSection';
import { YliajoEditModal } from './YliajoEditModal';
import { useYliajoManager } from '@/lib/yliajoManager';

export type AvainarvoRyhma = 'uudet-avainarvot' | 'vanhat-avainarvot';

const DUMMY_HAKU_OID = '1.2.246.562.29.00000000000000000000';

export const OpiskelijavalintaanSiirtyvatTiedot = ({
  avainarvoRyhma,
  oppijaNumero,
}: {
  avainarvoRyhma: AvainarvoRyhma;
  oppijaNumero: string;
}) => {
  const { data: valintadata } = useApiSuspenseQuery(
    queryOptionsGetValintadata({ oppijaNumero, hakuOid: DUMMY_HAKU_OID }),
  );

  const { t } = useTranslations();

  const { startYliajoEdit, startYliajoAdd, yliajoFields } = useYliajoManager({
    henkiloOid: oppijaNumero,
  });

  return (
    <>
      {yliajoFields && <YliajoEditModal avainArvot={valintadata.avainArvot} />}
      <AccordionBox
        id="opiskelijavalintaan-siirtyvat-tiedot"
        title={t(
          'opiskelijavalinnan-tiedot.suorituspalvelusta-opiskelijavalintaan-siirtyvat-tiedot',
        )}
      >
        <AvainArvotSection
          avainarvot={valintadata.avainArvot}
          startYliajoEdit={
            avainarvoRyhma === 'uudet-avainarvot'
              ? (yliajoParams) => {
                  startYliajoEdit({
                    arvo: yliajoParams.arvo,
                    avain: yliajoParams.avain,
                    selite: yliajoParams.selite,
                  });
                }
              : undefined
          }
          avainArvoFilter={(avainArvo) =>
            avainarvoRyhma === 'uudet-avainarvot'
              ? !avainArvo.metadata.duplikaatti
              : avainArvo.metadata.duplikaatti
          }
        />
        {avainarvoRyhma === 'uudet-avainarvot' && (
          <Stack direction="row" sx={{ justifyContent: 'flex-end' }}>
            <OphButton
              startIcon={<Add />}
              variant="outlined"
              onClick={() => {
                startYliajoAdd();
              }}
            >
              {t('opiskelijavalinnan-tiedot.lisaa-kentta')}
            </OphButton>
          </Stack>
        )}
      </AccordionBox>
    </>
  );
};
