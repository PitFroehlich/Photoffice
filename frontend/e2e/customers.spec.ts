import { expect, Page, test } from '@playwright/test';
import { loginAs } from './helpers';

const unique = () => Date.now().toString(36);

async function openCustomers(page: Page, user = 'admin-a'): Promise<void> {
  await loginAs(page, user);
  await page.getByRole('link', { name: 'Kunden' }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Kunden');
}

async function createCustomer(page: Page, firstName: string, lastName: string, email: string): Promise<void> {
  await page.getByRole('link', { name: 'Neuer Kunde' }).click();
  await page.getByLabel('Vorname').fill(firstName);
  await page.getByLabel('Nachname').fill(lastName);
  await page.getByLabel('E-Mail').fill(email);
  await page.getByRole('button', { name: 'Speichern' }).click();
  await expect(page.getByText(`„${firstName} ${lastName}“ wurde angelegt.`)).toBeVisible();
}

test('lists the seeded customers of studio A with search', async ({ page }) => {
  await openCustomers(page);

  await expect(page.getByRole('link', { name: 'Becker, Julia', exact: true })).toBeVisible();
  await page.getByLabel('Name, E-Mail oder Ort suchen').fill('hamburg');
  await expect(page.getByRole('link', { name: 'Neumann, Thomas', exact: true })).toBeVisible();
  await expect(page.locator('tr[mat-row]')).toHaveCount(1);
  await expect(page).toHaveURL(/suche=hamburg/);
});

test('create, find, edit and delete a customer', async ({ page }) => {
  const id = unique();
  const email = `e2e-${id}@example.test`;
  await openCustomers(page);

  await createCustomer(page, 'Erika', `Test${id}`, email);

  await page.getByLabel('Name, E-Mail oder Ort suchen').fill(`Test${id}`);
  await page.getByRole('link', { name: `Test${id}, Erika`, exact: true }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText(`Erika Test${id}`);
  await page.getByLabel('Ort').fill('Görlitz');
  await page.getByRole('button', { name: 'Speichern' }).click();
  await expect(page.getByText(`„Erika Test${id}“ wurde gespeichert.`)).toBeVisible();

  await page.getByLabel('Name, E-Mail oder Ort suchen').fill(`Test${id}`);
  await expect(page.getByRole('cell', { name: 'Görlitz' })).toBeVisible();
  await page.getByRole('button', { name: `Test${id}, Erika löschen` }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Löschen' }).click();
  await expect(page.getByText(`„Erika Test${id}“ wurde gelöscht.`)).toBeVisible();
  await expect(page.getByText('Keine Treffer')).toBeVisible();
});

test('duplicate e-mail is shown at the field', async ({ page }) => {
  await openCustomers(page);
  await page.getByRole('link', { name: 'Neuer Kunde' }).click();
  await page.getByLabel('Vorname').fill('Doppelt');
  await page.getByLabel('Nachname').fill('Becker');
  await page.getByLabel('E-Mail').fill('JULIA.BECKER@example.test');
  await page.getByRole('button', { name: 'Speichern' }).click();

  await expect(page.getByText('Ein Kunde mit der E-Mail-Adresse julia.becker@example.test existiert bereits.')).toBeVisible();
});

test('studio B does not see the customers of studio A', async ({ page }) => {
  await openCustomers(page, 'admin-b');

  await expect(page.getByRole('link', { name: 'Vogel, Karin', exact: true })).toBeVisible();
  await page.getByLabel('Name, E-Mail oder Ort suchen').fill('Becker');
  await expect(page.getByText('Keine Treffer')).toBeVisible();

  await page.goto('/studio/kunden/c0000000-0000-4000-8000-000000000001');
  await expect(page.getByText('Der Kunde wurde nicht gefunden.')).toBeVisible();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Kunden');
});

test('format rules are checked while typing', async ({ page }) => {
  await openCustomers(page);
  await page.getByRole('link', { name: 'Neuer Kunde' }).click();

  await page.getByLabel('Vorname').fill('Julia2');
  await expect(page.getByText('Nur Buchstaben, Leerzeichen, Bindestrich, Apostroph und Punkt')).toBeVisible();
  await page.getByLabel('Telefon').fill('abc');
  await expect(page.getByText('Nur Ziffern, Leerzeichen und + - / ( ), mindestens 5 Zeichen')).toBeVisible();
  await page.getByLabel('PLZ').fill('12a');
  await expect(page.getByText('4 oder 5 Ziffern')).toBeVisible();
  await page.getByLabel('Straße und Hausnummer').fill('Lindenstraße');
  await expect(page.getByText('Straße mit Hausnummer, z. B. Lindenstraße 4 oder Am Markt 1/2')).toBeVisible();
  await page.getByLabel('E-Mail').fill('julia@example');
  await expect(page.getByText('Bitte eine gültige E-Mail-Adresse eingeben, z. B. name@beispiel.de')).toBeVisible();

  await page.getByRole('button', { name: 'Speichern' }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Neuer Kunde');
});
