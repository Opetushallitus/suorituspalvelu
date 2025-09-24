import { ExternalLink } from '@/components/ExternalLink';
import { LabeledInfoItem } from '@/components/LabeledInfoItem';
import { useConfig } from '@/configuration';
import { useTranslations } from '@/hooks/useTranslations';
import { getOppilaitosLinkUrl } from '@/lib/getOppilaitosLink';
import type { SuorituksenPerustiedot } from '@/types/ui-types';

export function OppilaitosInfoItem({
  oppilaitos,
}: {
  oppilaitos: SuorituksenPerustiedot['oppilaitos'];
}) {
  const { t, translateKielistetty } = useTranslations();
  const config = useConfig();

  return (
    <LabeledInfoItem
      label={t('oppija.oppilaitos')}
      value={
        <ExternalLink href={getOppilaitosLinkUrl(config, oppilaitos.oid)}>
          {translateKielistetty(oppilaitos.nimi)}
        </ExternalLink>
      }
    />
  );
}
