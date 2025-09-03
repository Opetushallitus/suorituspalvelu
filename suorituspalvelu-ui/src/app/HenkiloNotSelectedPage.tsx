import { ResultPlaceholder } from '@/components/ResultPlaceholder';
import { useTranslations } from '@/hooks/useTranslations';

export default function HenkiloNotSelectedPage() {
  const { t } = useTranslations();
  return <ResultPlaceholder text={t('valitse-henkilo')} />;
}
