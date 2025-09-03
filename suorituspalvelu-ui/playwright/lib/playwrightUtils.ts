import { expect, type Locator, type Page } from '@playwright/test';
import { isFunction } from 'remeda';

export async function selectOption({
  page,
  locator,
  name,
  option,
}: {
  page: Page;
  name?: string;
  option: string;
  locator?: Locator;
}) {
  const combobox = (locator ?? page).getByRole(
    'combobox',
    name
      ? {
          name: new RegExp(`^${name}`),
        }
      : {},
  );

  await combobox.click();

  // Selectin listbox rendataan juuritasolle
  const listbox = page.locator('#select-menu').getByRole('listbox');

  await listbox.getByRole('option', { name: option, exact: true }).click();
}

export const checkRow = async (
  row: Locator,
  expectedValues: Array<string | ((cell: Locator) => Promise<void>)>,
  cellType: 'th' | 'td' | 'th,td' = 'th,td',
  exact: boolean = true,
) => {
  const cells = row.locator(cellType);
  for (const [index, expectedValue] of expectedValues.entries()) {
    const cell = cells.nth(index);
    if (isFunction(expectedValue)) {
      await expectedValue(cell);
    } else {
      if (exact) {
        await expect(cell).toHaveText(expectedValue);
      } else {
        await expect(cell).toContainText(expectedValue);
      }
    }
  }
};

export const checkTable = async (
  table: Locator,
  expectedValues: Array<Array<string | ((cell: Locator) => Promise<void>)>>,
) => {
  const rows = table.getByRole('row');
  await expect(rows).toHaveCount(expectedValues.length);

  for (const [rowIndex, expectedRow] of expectedValues.entries()) {
    const row = rows.nth(rowIndex);
    await checkRow(row, expectedRow);
  }
};

export const expectList = async (list: Locator, items: Array<string>) => {
  await expect(list).toHaveCount(items.length);
  for (let i = 0; i < items.length; i++) {
    await expect(list.nth(i)).toHaveText(items[i] as string);
  }
};
