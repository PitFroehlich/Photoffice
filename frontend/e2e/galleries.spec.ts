import { expect, Page, test } from '@playwright/test';
import { loginAs } from './helpers';

// Letters only – keeps names valid for all validation rules
const unique = () =>
  Date.now()
    .toString(36)
    .replace(/\d/g, (digit) => 'abcdefghij'[Number(digit)]);

async function openGalleries(page: Page, user = 'admin-a'): Promise<void> {
  await loginAs(page, user);
  await page.getByRole('link', { name: 'Galerien' }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Galerien');
}

test('lists the seeded galleries with status and filters', async ({ page }) => {
  await openGalleries(page);
  const table = page.getByRole('table', { name: 'Galerien' });

  await expect(table.getByRole('row', { name: /Hochzeit Becker Julia Becker Online/ })).toBeVisible();
  await expect(table.getByRole('row', { name: /Babybauch Schröder Felix Schröder Abgelaufen/ })).toBeVisible();

  await page.getByRole('radio', { name: 'Entwurf' }).click();
  await expect(table.getByRole('row', { name: /Familienshooting Wagner/ })).toBeVisible();
  await expect(table.getByRole('row', { name: /Hochzeit Becker/ })).toHaveCount(0);
  await expect(page).toHaveURL(/status=draft/);
});

test('create a gallery with customer and date, publish and take it offline, delete', async ({ page }) => {
  const name = `Testgalerie ${unique()}`;
  await openGalleries(page);

  await page.getByRole('link', { name: 'Neue Galerie' }).click();
  await page.getByLabel('Name der Galerie', { exact: true }).fill('!!!');
  await expect(page.getByText('Mindestens ein Buchstabe')).toBeVisible();
  await page.getByLabel('Name der Galerie', { exact: true }).fill(name);
  await page.getByLabel('Erreichbar bis (optional)').fill('31.02.2030');
  await page.getByLabel('Erreichbar bis (optional)').blur();
  await expect(page.getByText('Bitte ein Datum im Format TT.MM.JJJJ eingeben')).toBeVisible();
  await page.getByLabel('Erreichbar bis (optional)').fill('31.12.2030');
  await page.getByPlaceholder('Kunden suchen (Name, E-Mail, Ort)').fill('neum');
  await page.getByRole('option', { name: /Thomas Neumann/ }).click();
  await expect(page.locator('mat-chip-row', { hasText: 'Thomas Neumann' })).toBeVisible();
  await page.getByRole('button', { name: 'Speichern' }).click();

  await expect(page.getByText(`„${name}“ wurde angelegt.`)).toBeVisible();
  await expect(page.getByTestId('gallery-state')).toHaveText('Entwurf');
  await expect(page.getByText('Erreichbar bis 31.12.2030')).toBeVisible();

  await page.getByRole('button', { name: 'Veröffentlichen' }).click();
  await expect(page.getByText(`„${name}“ ist jetzt online.`)).toBeVisible();
  await expect(page.getByTestId('gallery-state')).toHaveText('Online');

  await page.getByRole('button', { name: 'Offline nehmen' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Offline nehmen' }).click();
  await expect(page.getByTestId('gallery-state')).toHaveText('Offline');

  await page.getByRole('button', { name: 'Löschen' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Löschen' }).click();
  await expect(page.getByText(`„${name}“ wurde gelöscht.`)).toBeVisible();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Galerien');
});

test('an expired gallery is shown as expired (read-only check, keeps the seed data)', async ({ page }) => {
  await openGalleries(page);
  await page.getByRole('link', { name: 'Babybauch Schröder', exact: true }).click();

  await expect(page.getByTestId('gallery-state')).toHaveText('Abgelaufen');
  await expect(page.getByText('Das Ablaufdatum ist überschritten')).toBeVisible();
});

test('customer page links to the galleries of the customer', async ({ page }) => {
  await loginAs(page, 'admin-a');
  await page.getByRole('link', { name: 'Kunden' }).click();
  await page.getByLabel('Name, E-Mail oder Ort suchen').fill('Koch');
  await page.getByRole('link', { name: 'Koch, Jan', exact: true }).click();
  await page.locator('app-page-header').getByRole('link', { name: 'Galerien' }).click();

  await expect(page.getByRole('row', { name: /Bewerbungsfotos Koch/ })).toBeVisible();
  await expect(page.getByText('Filter aufheben')).toBeVisible();
});

test('studio B does not see the galleries of studio A', async ({ page }) => {
  await openGalleries(page, 'admin-b');

  await expect(page.getByRole('row', { name: /Porträt Vogel/ })).toBeVisible();
  await expect(page.getByRole('row', { name: /Hochzeit Becker/ })).toHaveCount(0);
  await page.goto('/studio/galerien/f1000000-0000-4000-8000-000000000001');
  await expect(page.getByText('Die Galerie wurde nicht gefunden.')).toBeVisible();
});
