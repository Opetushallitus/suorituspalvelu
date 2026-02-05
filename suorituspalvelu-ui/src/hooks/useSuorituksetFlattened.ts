import { castToArray } from '@/lib/common';
import type { IPerusopetuksenOppiaineUI } from '@/types/backend';
import type {
  OppijanTiedot,
  PerusopetuksenOppiaine,
  SuorituksenPerustiedot,
  Suoritus,
} from '@/types/ui-types';
import { useMemo } from 'react';
import { groupBy, isTruthy, sortBy } from 'remeda';

export function sortSuoritukset<T extends SuorituksenPerustiedot>(
  suoritukset: Array<T>,
) {
  return sortBy(suoritukset, [(s) => s.aloituspaiva ?? '', 'desc']);
}

const convertPerusopetusOppiaineet = (
  oppiaineet: Array<IPerusopetuksenOppiaineUI>,
): Array<PerusopetuksenOppiaine> => {
  const groupedOppiaineet = groupBy(
    oppiaineet,
    (oppiaine) => oppiaine.koodi + (oppiaine.kieli ?? ''),
  );
  const result = Object.values(groupedOppiaineet).map?.((oppiaineGroup) => {
    return oppiaineGroup?.reduce((mergedOppiaine, curr) => {
      const baseOppiaine: PerusopetuksenOppiaine = {
        ...mergedOppiaine,
        nimi: curr.nimi,
        tunniste: curr.tunniste,
        koodi: curr.koodi,
        kieli: curr.kieli,
      };
      if (curr.valinnainen) {
        return {
          ...baseOppiaine,
          valinnaisetArvosanat: [
            ...(mergedOppiaine?.valinnaisetArvosanat ?? []),
            curr.arvosana,
          ],
        };
      }
      if (mergedOppiaine.arvosana && curr.arvosana) {
        throw new Error(
          `Pakolliselle oppiaineelle löytyi duplikaatti-arvosanoja (tunniste: ${curr.tunniste}, koodi: ${curr.koodi}, kieli: ${curr.kieli})`,
        );
      }
      return {
        ...baseOppiaine,
        arvosana: curr.arvosana,
      };
    }, {} as PerusopetuksenOppiaine);
  });

  return result;
};

export function useSuorituksetFlattened(oppijanTiedot: OppijanTiedot) {
  return useMemo(() => {
    const result: Array<Suoritus> = [];

    const addSortedSuoritukset = <T extends SuorituksenPerustiedot>(
      v: T | Array<T> | undefined | null,
      mapSuoritus: (suoritus: T) => Suoritus,
    ) => {
      const arrayValue = sortSuoritukset(castToArray(v).filter(isTruthy));
      arrayValue?.forEach((suoritus) => {
        if (suoritus) {
          result.push(mapSuoritus(suoritus as T));
        }
      });
    };

    addSortedSuoritukset(oppijanTiedot.kkTutkinnot, (s) => ({
      ...s,
      koulutustyyppi: 'korkeakoulutus',
    }));
    addSortedSuoritukset(oppijanTiedot.yoTutkinnot, (s) => ({
      ...s,
      koulutustyyppi: 'lukio',
    }));
    addSortedSuoritukset(oppijanTiedot.lukionOppimaara, (s) => ({
      ...s,
      koulutustyyppi: 'lukio',
    }));
    addSortedSuoritukset(oppijanTiedot.lukionOppiaineenOppimaarat, (s) => ({
      ...s,
      koulutustyyppi: 'lukio',
    }));
    addSortedSuoritukset(oppijanTiedot.diaTutkinto, (s) => ({
      ...s,
      koulutustyyppi: 'lukio',
    }));
    addSortedSuoritukset(oppijanTiedot.diaVastaavuusTodistus, (s) => ({
      ...s,
      koulutustyyppi: 'lukio',
    }));
    addSortedSuoritukset(oppijanTiedot.ebTutkinto, (s) => ({
      ...s,
      koulutustyyppi: 'eb',
    }));
    addSortedSuoritukset(oppijanTiedot.ibTutkinto, (s) => ({
      ...s,
      koulutustyyppi: 'ib',
    }));
    addSortedSuoritukset(oppijanTiedot.preIB, (s) => ({
      ...s,
      koulutustyyppi: 'lukio',
    }));
    addSortedSuoritukset(oppijanTiedot.ammatillisetPerusTutkinnot, (s) => ({
      ...s,
      koulutustyyppi: 'ammatillinen',
    }));
    addSortedSuoritukset(oppijanTiedot.ammattitutkinnot, (s) => ({
      ...s,
      koulutustyyppi: 'ammatillinen',
    }));
    addSortedSuoritukset(oppijanTiedot.erikoisammattitutkinnot, (s) => ({
      ...s,
      koulutustyyppi: 'ammatillinen',
    }));
    addSortedSuoritukset(oppijanTiedot.telmat, (s) => ({
      ...s,
      koulutustyyppi: 'ammatillinen',
    }));
    addSortedSuoritukset(oppijanTiedot.tuvat, (s) => ({
      ...s,
      koulutustyyppi: 'tuva',
    }));
    addSortedSuoritukset(oppijanTiedot.vapaaSivistystyoKoulutukset, (s) => ({
      ...s,
      koulutustyyppi: 'vapaa-sivistystyo',
    }));
    addSortedSuoritukset(oppijanTiedot.perusopetuksenOppimaarat, (s) => ({
      ...s,
      isEditable: s.syotetty,
      koulutustyyppi: 'perusopetus',
      suoritustyyppi: 'perusopetuksenoppimaara',
      // Oppiaineet-listassa voi tulla samalle oppiaineelle useita arvosanarivejä, jotka täytyy yhdistää
      oppiaineet: convertPerusopetusOppiaineet(s.oppiaineet),
    }));

    addSortedSuoritukset(
      oppijanTiedot.perusopetuksenOppimaara78Luokkalaiset,
      (s) => ({
        ...s,
        koulutustyyppi: 'perusopetus',
        isEditable: false,
      }),
    );

    addSortedSuoritukset(
      oppijanTiedot.perusopetuksenOppiaineenOppimaarat,
      (s) => ({
        ...s,
        isEditable: s.syotetty,
        koulutustyyppi: 'perusopetus',
        suoritustyyppi: 'perusopetuksenoppiaineenoppimaara',
        // Oppiaineet-listassa voi tulla samalle oppiaineelle useita arvosanarivejä, jotka täytyy yhdistää
        oppiaineet: convertPerusopetusOppiaineet(s.oppiaineet),
      }),
    );

    return result;
  }, [oppijanTiedot]);
}
