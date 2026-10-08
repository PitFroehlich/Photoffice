import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.beforeEach(async ({ page }) => {
  await loginAs(page, 'admin-a');
  await page.getByRole('link', { name: 'UI-Bausteine' }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('UI-Bausteine');
});

test('search filters the list and shows the empty state', async ({ page }) => {
  await expect(page.getByText('1 – 5 von 23')).toBeVisible();

  await page.getByLabel('Name oder Ort suchen').fill('Leipzig');
  await expect(page.getByText('1 – 3 von 3')).toBeVisible();

  await page.getByLabel('Name oder Ort suchen').fill('xyz');
  await expect(page.getByText('Keine Treffer')).toBeVisible();
});

test('delete asks for confirmation', async ({ page }) => {
  await page.getByRole('button', { name: 'Anna Bauer löschen' }).click();
  await expect(page.getByRole('dialog')).toContainText('„Anna Bauer“ wird endgültig gelöscht.');
  await page.getByRole('button', { name: 'Abbrechen' }).click();
  await expect(page.getByText('1 – 5 von 23')).toBeVisible();

  await page.getByRole('button', { name: 'Anna Bauer löschen' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Löschen' }).click();
  await expect(page.getByText('„Anna Bauer“ wurde gelöscht.')).toBeVisible();
  await expect(page.getByText('1 – 5 von 22')).toBeVisible();
});

test('form shows validation messages and saves valid input', async ({ page }) => {
  await page.getByRole('button', { name: 'Speichern' }).click();
  await expect(page.getByText('Pflichtfeld').first()).toBeVisible();

  await page.getByLabel('Name', { exact: true }).fill('Zoe Test');
  await page.getByLabel('E-Mail', { exact: true }).fill('kein-email');
  await page.getByRole('button', { name: 'Speichern' }).click();
  await expect(page.getByText('Bitte eine gültige E-Mail-Adresse eingeben')).toBeVisible();

  await page.getByLabel('E-Mail', { exact: true }).fill('zoe@example.test');
  await page.getByRole('button', { name: 'Speichern' }).click();
  await expect(page.getByText('„Zoe Test“ wurde gespeichert.')).toBeVisible();
});

test('error message from a problem detail', async ({ page }) => {
  await page.getByRole('button', { name: 'Fehlermeldung zeigen' }).click();
  await expect(page.getByText('Beispiel: Diese E-Mail-Adresse ist bereits vergeben.')).toBeVisible();
});
