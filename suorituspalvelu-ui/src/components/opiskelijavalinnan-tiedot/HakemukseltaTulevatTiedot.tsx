import { useTranslations } from '@/hooks/useTranslations';
import { AccordionBox } from '@/components/AccordionBox';
import { AvainArvotSection } from './AvainArvotSection';
import { prop, sortBy } from 'remeda';
import type { ValintaData } from '@/types/ui-types';

export const HakemukseltaTulevatTiedot = ({
  valintaData,
}: {
  valintaData: ValintaData;
}) => {
  const { t } = useTranslations();

  const avainArvotHakemukselta = valintaData.avainArvot.filter(
    (avainArvo) => avainArvo.metadata.arvoOnHakemukselta,
  );

  return (
    <AccordionBox
      id="hakemukselta-tulevat-tiedot"
      title={t(
        'opiskelijavalinnan-tiedot.hakemuksesta-opiskelijavalintaan-tulevat-tiedot',
      )}
      defaultExpanded={false}
    >
      <AvainArvotSection
        avainarvot={sortBy(avainArvotHakemukselta, prop('avain'))}
      />
    </AccordionBox>
  );
};
