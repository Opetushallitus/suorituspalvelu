import { castToArray } from '@/lib/common';
import type {
  OppijanTiedot,
  PerusopetuksenOppiaine,
  Suoritus,
} from '@/types/ui-types';
import { useMemo } from 'react';
import { groupBy, isTruthy, omit } from 'remeda';

export function useSuorituksetFlattened(
  oppijanTiedot: OppijanTiedot,
  sortByDate: boolean = false,
) {
  return useMemo(() => {
    const suoritusTiedot = omit(oppijanTiedot, [
      'nimi',
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
      [
        ...suoritusTiedot.perusopetuksenOppimaarat,
        suoritusTiedot.perusopetuksenOppimaara78Luokkalaiset,
        ...suoritusTiedot.nuortenPerusopetuksenOppiaineenOppimaarat,
        ...suoritusTiedot.perusopetuksenOppiaineenOppimaarat,
        ...suoritusTiedot.aikuistenPerusopetuksenOppimaarat,
      ]
        .filter(isTruthy)
        .map((suoritus) => {
          if ('oppiaineet' in suoritus) {
            const groupedOppiaineet = groupBy(
              suoritus.oppiaineet,
              (oppiaine) => oppiaine.tunniste + (oppiaine.kieli ?? ''),
            );
            return {
              ...suoritus,
              koulutustyyppi: 'perusopetus',
              // Oppiaineet-listassa voi tulla samalle oppiaineelle useita arvosanarivejä, jotka täytyy yhdistää
              oppiaineet: Object.values(groupedOppiaineet).map?.(
                (oppiaineet) => {
                  return oppiaineet?.reduce((mergedOppiaine, curr) => {
                    const baseOppiaine: PerusopetuksenOppiaine = {
                      ...mergedOppiaine,
                      nimi: curr.nimi,
                      tunniste: curr.tunniste,
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
                        `Pakolliselle oppiaineelle löytyi duplikaatti-arvosanoja (tunniste: ${curr.tunniste}, kieli: ${curr.kieli})`,
                      );
                    }
                    return {
                      ...baseOppiaine,
                      arvosana: curr.arvosana,
                    };
                  }, {} as PerusopetuksenOppiaine);
                },
              ),
            };
          } else {
            return { ...suoritus, koulutustyyppi: 'perusopetus' };
          }
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
