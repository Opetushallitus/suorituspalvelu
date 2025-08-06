import { castToArray, ValueOf } from '@/lib/common';
import { OppijanTiedot, Suoritus } from '@/types/ui-types';
import { useMemo } from 'react';
import { isPlainObject, isString, omit } from 'remeda';
import { useTranslations } from './useTranslations';

export function useSuorituksetFlattened(
  oppijanTiedot: OppijanTiedot,
  sortByDate: boolean = false,
) {
  const { t, translateKielistetty } = useTranslations();
  const unsortedSuoritukset = useMemo(() => {
    const suoritusTiedot = omit(oppijanTiedot, [
      'oppijaNumero',
      'opiskeluoikeudet',
    ]);

    const result: Array<Suoritus> = [];

    const addValue = <V extends ValueOf<typeof suoritusTiedot>>(
      v: V,
      koulutustyyppi: Suoritus['koulutustyyppi'],
      nimi?: string,
    ) => {
      const arrayValue = castToArray(v);
      arrayValue.forEach((suoritus) => {
        if (suoritus && 'nimi' in suoritus) {
          if (isPlainObject(suoritus.nimi)) {
            return result.push({
              ...suoritus,
              koulutustyyppi,
              nimi: translateKielistetty(suoritus.nimi),
              key: suoritus?.tunniste,
            } as Suoritus);
          } else if (isString(suoritus.nimi)) {
            return result.push({
              ...suoritus,
              koulutustyyppi,
              nimi: suoritus.nimi,
              key: suoritus?.tunniste,
            } as Suoritus);
          }
        }
        return result.push({
          ...suoritus,
          koulutustyyppi,
          nimi: nimi,
          key: suoritus?.tunniste,
        } as Suoritus);
      });
    };
    addValue(suoritusTiedot.kkTutkinnot, 'korkeakoulutus');
    addValue(
      suoritusTiedot.yoTutkinto,
      'lukio',
      t('oppija.ylioppilastutkinto'),
    );
    addValue(
      suoritusTiedot.lukionOppimaara,
      'lukio',
      t('oppija.lukion-oppimaara'),
    );
    addValue(
      suoritusTiedot.lukionOppiaineenOppimaarat,
      'lukio',
      t('oppija.lukion-oppiaineen-oppimaara'),
    );
    addValue(suoritusTiedot.diaTutkinto, 'lukio', t('oppija.dia-tutkinto'));
    addValue(
      suoritusTiedot.diaVastaavuusTodistus,
      'lukio',
      t('oppija.dia-vastaavuustodistus'),
    );
    addValue(suoritusTiedot.ebTutkinto, 'lukio', t('oppija.eb-tutkinto'));
    addValue(suoritusTiedot.ibTutkinto, 'lukio', t('oppija.ib-tutkinto'));
    addValue(suoritusTiedot.preIB, 'lukio', t('oppija.pre-ib'));
    addValue(suoritusTiedot.ammatillisetPerusTutkinnot, 'ammatillinen');
    addValue(suoritusTiedot.ammattitutkinnot, 'ammatillinen');
    addValue(suoritusTiedot.erikoisammattitutkinnot, 'ammatillinen');
    addValue(suoritusTiedot.vapaanSivistystyonKoulutukset, 'vapaa-sivistystyo');
    addValue(suoritusTiedot.tuvat, 'tuva', t('oppija.tuva'));
    addValue(
      suoritusTiedot.perusopetuksenOppimaarat,
      'perusopetus',
      t('oppija.perusopetuksen-oppimaara'),
    );
    addValue(
      suoritusTiedot.perusopetuksenOppimaara78Luokkalaiset,
      'perusopetus',
      t('oppija.perusopetuksen-oppimaara'),
    );
    addValue(
      suoritusTiedot.nuortenPerusopetuksenOppiaineenOppimaarat,
      'perusopetus',
      t('oppija.nuorten-perusopetuksen-oppiaineen-oppimaara'),
    );
    addValue(
      suoritusTiedot.perusopetuksenOppiaineenOppimaarat,
      'perusopetus',
      t('oppija.perusopetuksen-oppiaineen-oppimaara'),
    );
    addValue(
      suoritusTiedot.aikuistenPerusopetuksenOppimaarat,
      'perusopetus',
      t('oppija.aikuisten-perusopetuksen-oppimaara'),
    );
    return result;
  }, [oppijanTiedot, t, translateKielistetty]);

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
