import { ExternalLink } from '@/components/ExternalLink';
import { LabeledInfoItem } from '@/components/LabeledInfoItem';
import { useConfig } from '@/lib/configuration';
import { useTranslations } from '@/hooks/useTranslations';
import { getOppilaitosLinkUrl } from '@/lib/getOppilaitosLink';
import type { SuorituksenPerustiedot } from '@/types/ui-types';

export function OppilaitosInfoItem({
  oppilaitos,
  sx,
}: {
  oppilaitos: SuorituksenPerustiedot['oppilaitos'];
  sx?: React.CSSProperties;
}) {
  const { t, translateKielistetty } = useTranslations();
  const config = useConfig();

  return (
    <LabeledInfoItem
      sx={sx}
      label={t('oppija.oppilaitos')}
      value={
        <ExternalLink href={getOppilaitosLinkUrl(config, oppilaitos.oid)}>
          {translateKielistetty(oppilaitos.nimi)}
        </ExternalLink>
      }
    />
  );
}
