import { castToArray } from '@/lib/common';
import type { IPerusopetuksenOppiaineUI } from '@/types/backend';
import type {
  OppijanTiedot,
  PerusopetuksenOppiaine,
  Suoritus,
} from '@/types/ui-types';
import { useMemo } from 'react';
import { groupBy, isTruthy } from 'remeda';

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

    const addSuoritukset = (
      v: Suoritus | Array<Suoritus> | null | undefined,
    ) => {
      const arrayValue = castToArray(v);
      arrayValue?.forEach((suoritus) => {
        if (suoritus) {
          result.push(suoritus);
        }
      });
    };

    addSuoritukset(
      oppijanTiedot.kkTutkinnot?.map((suoritus) => ({
        ...suoritus,
        koulutustyyppi: 'korkeakoulutus',
      })),
    );

    addSuoritukset(
      [
        ...oppijanTiedot.yoTutkinnot,
        oppijanTiedot.lukionOppimaara,
        ...oppijanTiedot.lukionOppiaineenOppimaarat,
        oppijanTiedot.diaTutkinto,
        oppijanTiedot.diaVastaavuusTodistus,
      ]
        .filter(isTruthy)
        .map((suoritus) => ({
          ...suoritus,
          koulutustyyppi: 'lukio',
        })),
    );

    addSuoritukset(
      oppijanTiedot.ebTutkinto && {
        ...oppijanTiedot.ebTutkinto,
        koulutustyyppi: 'eb',
      },
    );

    addSuoritukset(
      oppijanTiedot.ibTutkinto && {
        ...oppijanTiedot.ibTutkinto,
        koulutustyyppi: 'ib',
      },
    );

    addSuoritukset(
      oppijanTiedot.preIB && {
        ...oppijanTiedot.preIB,
        koulutustyyppi: 'lukio',
      },
    );

    addSuoritukset(
      [
        ...oppijanTiedot.ammatillisetPerusTutkinnot,
        ...oppijanTiedot.ammattitutkinnot,
        ...oppijanTiedot.erikoisammattitutkinnot,
        ...oppijanTiedot.telmat,
      ].map((suoritus) => ({
        ...suoritus,
        koulutustyyppi: 'ammatillinen',
      })),
    );

    addSuoritukset(
      oppijanTiedot.tuvat.map((suoritus) => ({
        ...suoritus,
        koulutustyyppi: 'tuva',
      })),
    );

    addSuoritukset(
      oppijanTiedot.vapaaSivistystyoKoulutukset.map((suoritus) => ({
        ...suoritus,
        koulutustyyppi: 'vapaa-sivistystyo',
      })),
    );

    addSuoritukset(
      oppijanTiedot.perusopetuksenOppimaarat.map((suoritus) => {
        return {
          ...suoritus,
          isEditable: suoritus.syotetty,
          koulutustyyppi: 'perusopetus',
          suoritustyyppi: 'perusopetuksenoppimaara',
          // Oppiaineet-listassa voi tulla samalle oppiaineelle useita arvosanarivejä, jotka täytyy yhdistää
          oppiaineet: convertPerusopetusOppiaineet(suoritus.oppiaineet),
        };
      }),
    );

    addSuoritukset(
      [oppijanTiedot.perusopetuksenOppimaara78Luokkalaiset]
        .filter(isTruthy)
        .map((suoritus) => ({
          ...suoritus,
          koulutustyyppi: 'perusopetus',
          isEditable: false,
        })),
    );

    addSuoritukset(
      oppijanTiedot.perusopetuksenOppiaineenOppimaarat.map((suoritus) => {
        return {
          ...suoritus,
          isEditable: suoritus.syotetty,
          koulutustyyppi: 'perusopetus',
          suoritustyyppi: 'perusopetuksenoppiaineenoppimaara',
          // Oppiaineet-listassa voi tulla samalle oppiaineelle useita arvosanarivejä, jotka täytyy yhdistää
          oppiaineet: convertPerusopetusOppiaineet(suoritus.oppiaineet),
        };
      }),
    );

    return result;
  }, [oppijanTiedot]);
}
