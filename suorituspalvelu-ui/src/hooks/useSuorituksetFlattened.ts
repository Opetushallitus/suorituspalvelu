import { castToArray } from '@/lib/common';
import type { IPerusopetuksenOppiaine } from '@/types/backend';
import type {
  OppijanTiedot,
  PerusopetuksenOppiaine,
  Suoritus,
} from '@/types/ui-types';
import { useMemo } from 'react';
import { groupBy, isTruthy, omit } from 'remeda';

const convertPerusopetusOppiaineet = (
  oppiaineet: Array<IPerusopetuksenOppiaine>,
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

export function useSuorituksetFlattened(
  oppijanTiedot: OppijanTiedot,
  sortByDate: boolean = false,
) {
  return useMemo(() => {
    const suoritusTiedot = omit(oppijanTiedot, [
      'etunimet',
      'sukunimi',
      'oppijaNumero',
      'henkiloTunnus',
      'syntymaAika',
      'opiskeluoikeudet',
      'henkiloOID',
    ]);

    const result: Array<Suoritus> = [];

    const addSuoritukset = (
      v: Suoritus | Array<Suoritus> | null | undefined,
    ) => {
      const arrayValue = castToArray(v);
      arrayValue?.forEach((suoritus) => {
        if (suoritus) {
          return result.push(suoritus);
        }
      });
    };

    addSuoritukset(
      suoritusTiedot.kkTutkinnot?.map((suoritus) => ({
        ...suoritus,
        koulutustyyppi: 'korkeakoulutus',
      })),
    );

    addSuoritukset(
      [
        ...suoritusTiedot.yoTutkinnot,
        suoritusTiedot.lukionOppimaara,
        ...suoritusTiedot.lukionOppiaineenOppimaarat,
        suoritusTiedot.diaTutkinto,
        suoritusTiedot.diaVastaavuusTodistus,
      ]
        .filter(isTruthy)
        .map((suoritus) => ({
          ...suoritus,
          koulutustyyppi: 'lukio',
        })),
    );

    addSuoritukset(
      suoritusTiedot.ebTutkinto && {
        ...suoritusTiedot.ebTutkinto,
        koulutustyyppi: 'eb',
      },
    );

    addSuoritukset(
      suoritusTiedot.ibTutkinto && {
        ...suoritusTiedot.ibTutkinto,
        koulutustyyppi: 'ib',
      },
    );

    addSuoritukset(
      suoritusTiedot.preIB && {
        ...suoritusTiedot.preIB,
        koulutustyyppi: 'lukio',
      },
    );
    addSuoritukset(
      [
        ...suoritusTiedot.ammatillisetPerusTutkinnot,
        ...suoritusTiedot.ammattitutkinnot,
        ...suoritusTiedot.erikoisammattitutkinnot,
        ...suoritusTiedot.telmat,
      ].map((suoritus) => ({
        ...suoritus,
        koulutustyyppi: 'ammatillinen',
      })),
    );

    addSuoritukset(
      suoritusTiedot.tuvat.map((suoritus) => ({
        ...suoritus,
        koulutustyyppi: 'tuva',
      })),
    );

    addSuoritukset(
      suoritusTiedot.vapaaSivistystyoKoulutukset.map((suoritus) => ({
        ...suoritus,
        koulutustyyppi: 'vapaa-sivistystyo',
      })),
    );

    addSuoritukset(
      suoritusTiedot.perusopetuksenOppimaarat.map((suoritus) => {
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
      [
        suoritusTiedot.perusopetuksenOppimaara78Luokkalaiset
      ]
        .filter(isTruthy)
        .map((suoritus) => ({
          ...suoritus,
          koulutustyyppi: 'perusopetus',
          isEditable: false,
        })),
    );

    addSuoritukset(
      suoritusTiedot.perusopetuksenOppiaineenOppimaarat.map((suoritus) => {
        return {
          ...suoritus,
          isEditable: suoritus.syotetty,
          koulutustyyppi: 'perusopetus',
          suoritustyyppi: 'perusopetuksenopiaineenoppimaara',
          // Oppiaineet-listassa voi tulla samalle oppiaineelle useita arvosanarivejä, jotka täytyy yhdistää
          oppiaineet: convertPerusopetusOppiaineet(suoritus.oppiaineet),
        };
      }),
    );

    if (sortByDate) {
      return result.sort((a, b) => {
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
    return result;
  }, [oppijanTiedot, sortByDate]);
}
