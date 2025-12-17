import { groupBy, mapValues, pipe, prop, sortBy } from 'remeda';
import type { AvainArvo } from '@/types/ui-types';

export const OPISKELIJAVALINTADATA_GROUPS = [
  'yleinen',
  'suoritukset',
  'lisapistekoulutukset',
  'perusopetuksen-oppiaineet',
  'lisatyt',
] as const;

export type OpiskelijavalintaDataGroups =
  (typeof OPISKELIJAVALINTADATA_GROUPS)[number];

export const getOpiskelijavalintaGroup = (
  item: AvainArvo,
): OpiskelijavalintaDataGroups => {
  const key = item.avain;
  if (key.startsWith('PK_ARVOSANA') || key.startsWith('PERUSKOULU_ARVOSANA')) {
    return 'perusopetuksen-oppiaineet';
  } else if (
    key.startsWith('lisapistekoulutus_') ||
    key.startsWith('LISAKOULUTUS_')
  ) {
    return 'lisapistekoulutukset';
  } else if (
    key.endsWith('_suoritettu') ||
    key.toLowerCase().endsWith('_suoritusvuosi') ||
    key.endsWith('_TILA')
  ) {
    return 'suoritukset';
  } else if (item.metadata.yliajo && item.metadata.arvoEnnenYliajoa == null) {
    return 'lisatyt';
  } else {
    return 'yleinen';
  }
};

export const sortGroups = (
  groups: Partial<
    Record<OpiskelijavalintaDataGroups, Array<AvainArvo> | undefined>
  >,
) => {
  return mapValues(groups, (items, group) => {
    const alphabeticallySorted = sortBy(items ?? [], prop('avain'));
    if (group === 'perusopetuksen-oppiaineet') {
      return alphabeticallySorted.sort((a, b) => {
        // Järjestetään oppiaine ennen sen arvosanaa
        const aIsBsOppiaine =
          a.avain.includes('_OPPIAINE') && a.avain.includes(b.avain);
        const bIsAsOppiaine =
          b.avain.includes('_OPPIAINE') && b.avain.includes(a.avain);

        if (aIsBsOppiaine) {
          return -1;
        } else if (bIsAsOppiaine) {
          return 1;
        } else {
          return 0;
        }
      });
    } else if (group === 'suoritukset') {
      const order = [
        'perustutkinto_suoritettu',
        'PK_TILA',
        'peruskoulu_suoritusvuosi',
        'PK_SUORITUSVUOSI',
        'ammatillinen_suoritettu',
        'AM_TILA',
        'lukio_suoritettu',
        'LK_TILA',
        'yo-tutkinto_suoritettu',
        'YO_TILA',
      ];
      return alphabeticallySorted.sort((a, b) => {
        const aIndex = order.indexOf(a.avain);
        const bIndex = order.indexOf(b.avain);
        if (aIndex === -1 && bIndex === -1) return 0;
        if (aIndex === -1) return 1;
        if (bIndex === -1) return -1;
        return aIndex - bIndex;
      });
    }
    return alphabeticallySorted;
  });
};

export const groupAndSortAvainarvot = (
  avainarvot: Array<AvainArvo>,
  avainArvoFilter?: (avainArvo: AvainArvo) => boolean,
) => {
  return pipe(
    avainarvot,
    ($) => (avainArvoFilter ? $.filter(avainArvoFilter) : $),
    groupBy((item) => getOpiskelijavalintaGroup(item)),
    (grouped) => sortGroups(grouped),
  );
};
