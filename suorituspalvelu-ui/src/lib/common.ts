export const DEFAULT_NUQS_OPTIONS = {
  history: 'push',
  clearOnDefault: true,
  defaultValue: '',
} as const;

export const NDASH = '\u2013';

export const EMPTY_OBJECT = Object.freeze({});
export const EMPTY_ARRAY = Object.freeze([]) as Array<never>;
export const EMPTY_STRING_SET = Object.freeze(new Set<string>());

export function castToArray<T>(args: T) {
  return (Array.isArray(args) ? args : [args]) as T extends Array<unknown>
    ? T
    : Array<T>;
}

export type ValueOf<T> = T[keyof T];
