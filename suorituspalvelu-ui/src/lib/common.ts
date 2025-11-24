import type { TFunction } from '@/hooks/useTranslations';
import type { Henkilo } from '@/types/ui-types';
import { formatDate } from 'date-fns';
import React from 'react';
import { isNullish, isTruthy } from 'remeda';

export const NDASH = '\u2013';

export const EMPTY_OBJECT = Object.freeze({});
export const EMPTY_ARRAY = Object.freeze([]) as Array<never>;
export const EMPTY_STRING_SET = Object.freeze(new Set<string>());

export function castToArray<T>(val: T) {
  if (isNullish(val)) {
    return [] as T extends Array<unknown> ? T : Array<T>;
  }
  return (Array.isArray(val) ? val : [val]) as T extends Array<unknown>
    ? T
    : Array<T>;
}

export type ValueOf<T> = T[keyof T];

export const pointToComma = (val?: string | number): string | undefined =>
  val?.toString().replace('.', ',');

export const formatFinnishDate = (date?: Date | string | null): string => {
  return date ? formatDate(date, 'd.M.y') : '';
};

export const formatYear = (date?: Date | string | null): string | undefined => {
  return date ? formatDate(date, 'y') : '';
};

export const removeWhiteSpace = (text?: string | null): string => {
  return text ? text.replace(/\s+/g, '-').trim() : '';
};

export const removeAccents = (text?: string | null): string => {
  return text ? text.normalize('NFD').replace(/[\u0300-\u036f]/g, '') : '';
};

export const toId = (text?: string | null): string => {
  return removeAccents(removeWhiteSpace(text)).toLowerCase();
};

export function truthyReactNodes(children: Array<React.ReactNode>) {
  return children.filter((child) => isTruthy(child));
}

export const formatHenkiloNimi = (henkilo: Henkilo, t: TFunction) => {
  const trimmedNimi =
    `${henkilo.etunimet ?? ''} ${henkilo.sukunimi ?? ''}`.trim();
  return trimmedNimi.length === 0 ? t('nimeton-henkilo') : trimmedNimi;
};

export const isHenkiloOid = (value?: string | null) =>
  Boolean(value && /^1\.2\.246\.562\.24\.\d+$/.test(value));

export const isHenkilotunnus = (value?: string | null) =>
  Boolean(value && /^\d{6}[a-zA-Z-]\d{3}\S{1}$/i.test(value));
