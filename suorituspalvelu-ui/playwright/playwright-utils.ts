import { Locator, Page } from '@playwright/test';

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
