import { castToArray, ValueOf } from '@/lib/common';
import { OppijanTiedot, Suoritus } from '@/types/ui-types';
import { useMemo } from 'react';
import { omit } from 'remeda';

export function useSuorituksetFlattened(
  oppijanTiedot: OppijanTiedot,
  sortByDate: boolean = false,
) {
  const unsortedSuoritukset = useMemo(() => {
    const suoritusTiedot = omit(oppijanTiedot, [
      'nimi',
      'oppijaNumero',
      'henkiloTunnus',
      'syntymaAika',
      'opiskeluoikeudet',
      'henkiloOID',
    ]);

    const result: Array<Suoritus> = [];

    const addValue = <V extends ValueOf<typeof suoritusTiedot>>(
      v: V,
      koulutustyyppi: Suoritus['koulutustyyppi'],
    ) => {
      const arrayValue = castToArray(v);
      arrayValue.forEach((suoritus) => {
        return result.push({
          ...suoritus,
          koulutustyyppi,
          key: suoritus?.tunniste,
        } as Suoritus);
      });
    };
    addValue(suoritusTiedot.kkTutkinnot, 'korkeakoulutus');
    addValue(suoritusTiedot.yoTutkinto, 'lukio');
    addValue(suoritusTiedot.lukionOppimaara, 'lukio');
    addValue(suoritusTiedot.lukionOppiaineenOppimaarat, 'lukio');
    addValue(suoritusTiedot.diaTutkinto, 'lukio');
    addValue(suoritusTiedot.diaVastaavuusTodistus, 'lukio');
    addValue(suoritusTiedot.ebTutkinto, 'eb');
    addValue(suoritusTiedot.ibTutkinto, 'ib');
    addValue(suoritusTiedot.preIB, 'lukio');
    addValue(suoritusTiedot.ammatillisetPerusTutkinnot, 'ammatillinen');
    addValue(suoritusTiedot.ammattitutkinnot, 'ammatillinen');
    addValue(suoritusTiedot.erikoisammattitutkinnot, 'ammatillinen');

    addValue(suoritusTiedot.telmat, 'ammatillinen');
    addValue(suoritusTiedot.tuvat, 'tuva');
    addValue(suoritusTiedot.vapaaSivistystyoKoulutukset, 'vapaa-sivistystyo');
    addValue(suoritusTiedot.perusopetuksenOppimaarat, 'perusopetus');
    addValue(
      suoritusTiedot.perusopetuksenOppimaara78Luokkalaiset,
      'perusopetus',
    );
    addValue(
      suoritusTiedot.nuortenPerusopetuksenOppiaineenOppimaarat,
      'perusopetus',
    );
    addValue(suoritusTiedot.perusopetuksenOppiaineenOppimaarat, 'perusopetus');
    addValue(suoritusTiedot.aikuistenPerusopetuksenOppimaarat, 'perusopetus');
    return result;
  }, [oppijanTiedot]);

  return useMemo(() => {
    if (sortByDate) {
      return unsortedSuoritukset.sort((a, b) => {
        if (a.valmistumispaiva && b.valmistumispaiva) {
          if (a.valmistumispaiva === b.valmistumispaiva) {
            return 0;
          }
          return a.valmistumispaiva < b.valmistumispaiva ? 1 : -1;
        }
        if (a.tila === 'KESKEN' && !a.valmistumispaiva) {
          return -1;
        } else if (b.tila === 'KESKEN' && !b.valmistumispaiva) {
          return 1;
        } else if (a.valmistumispaiva) {
          return -1;
        } else if (b.valmistumispaiva) {
          return 1;
        }
        return 0;
      });
    }
    return unsortedSuoritukset;
  }, [unsortedSuoritukset, sortByDate]);
}
