import { isAfter, isBefore } from 'date-fns';
import { TZDate } from '@date-fns/tz';
import { isNullish } from 'remeda';

export const isInRange = (
  current: Date | string | number,
  start?: Date | string | number,
  end?: Date | string | number,
) => {
  return (
    (isNullish(start) || isAfter(current, start)) &&
    (isNullish(end) || isBefore(current, end))
  );
};

export const toFinnishDate = (date: Date) =>
  new TZDate(date, 'Europe/Helsinki');

export const currentFinnishDate = () => {
  return toFinnishDate(new Date());
};
