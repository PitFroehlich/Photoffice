import { expect, Page, test } from '@playwright/test';
import { loginAs } from './helpers';

const unique = () => Date.now().toString(36);

async function openPriceList(page: Page, user = 'admin-a'): Promise<void> {
  await loginAs(page, user);
  await page.getByRole('link', { name: 'Preisliste' }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Preisliste');
}

test('shows the seeded price list of studio A', async ({ page }) => {
  await openPriceList(page);

  await expect(page.getByTestId('vat-rate')).toHaveText('19 %');
  const prints = page.getByRole('table', { name: 'Abzüge' });
  await expect(prints.getByRole('row', { name: /Matt 13 × 18 cm 2,90 €/ })).toBeVisible();
  await expect(
    prints.getByRole('row', { name: /Fine Art 30 × 45 cm 24,90 € Inaktiv/ }),
  ).toBeVisible();
  await expect(
    page
      .getByRole('table', { name: 'Downloads' })
      .getByRole('row', { name: /Web 2048 px max\. 2\.048 px 4,90 €/ }),
  ).toBeVisible();
  await expect(
    page
      .getByRole('table', { name: 'Downloads' })
      .getByRole('row', { name: /Original Original 9,90 €/ }),
  ).toBeVisible();
  await expect(
    page
      .getByRole('table', { name: 'Download-Pakete' })
      .getByRole('row', { name: /^10 Downloads 10 Bilder Original 69,00 €/ }),
  ).toBeVisible();
  await expect(
    page.getByRole('table', { name: 'Versandarten' }).getByText('Standardversand'),
  ).toBeVisible();
});

test('create, edit and delete a print', async ({ page }) => {
  const paper = `E2E-${unique()}`;
  await openPriceList(page);

  await page.getByRole('link', { name: 'Abzug hinzufügen' }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Neuer Abzug');
  await page.getByLabel('Papier').fill(paper);
  await page.getByLabel('Format', { exact: true }).fill('9 × 13 cm');
  await page.getByLabel('Preis').fill('1,5');
  await page.getByRole('button', { name: 'Speichern' }).click();
  await expect(page.getByText(`„Abzug ${paper}, 9 × 13 cm“ wurde angelegt.`)).toBeVisible();

  const prints = page.getByRole('table', { name: 'Abzüge' });
  await expect(
    prints.getByRole('row', { name: new RegExp(`${paper} 9 × 13 cm 1,50 €`) }),
  ).toBeVisible();

  await prints.getByRole('link', { name: paper, exact: true }).click();
  await page.getByLabel('Preis').fill('1,75');
  await page.getByText('Kunden anbieten (aktiv)').click();
  await page.getByRole('button', { name: 'Speichern' }).click();
  await expect(page.getByText(`„Abzug ${paper}, 9 × 13 cm“ wurde gespeichert.`)).toBeVisible();
  await expect(
    prints.getByRole('row', { name: new RegExp(`${paper} 9 × 13 cm 1,75 € Inaktiv`) }),
  ).toBeVisible();

  await page.getByRole('button', { name: `Abzug ${paper}, 9 × 13 cm löschen` }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Löschen' }).click();
  await expect(page.getByText(`„Abzug ${paper}, 9 × 13 cm“ wurde gelöscht.`)).toBeVisible();
  await expect(prints.getByText(paper)).toHaveCount(0);
});

test('an existing paper × format combination is shown at the field', async ({ page }) => {
  await openPriceList(page);
  await page.getByRole('link', { name: 'Abzug hinzufügen' }).click();
  await page.getByLabel('Papier').fill('matt');
  await page.getByLabel('Format', { exact: true }).fill('13 × 18 cm');
  await page.getByLabel('Preis').fill('3');
  await page.getByRole('button', { name: 'Speichern' }).click();

  await expect(page.getByText('Den Abzug „matt, 13 × 18 cm“ gibt es bereits.')).toBeVisible();
});

test('invalid prices are rejected in the form', async ({ page }) => {
  await openPriceList(page);
  await page.getByRole('link', { name: 'Versandart hinzufügen' }).click();
  await page.getByLabel('Name').fill('Ungültig');
  await page.getByLabel('Kosten').fill('4,999');
  await page.getByRole('button', { name: 'Speichern' }).click();

  await expect(
    page.getByText('Bitte einen Betrag zwischen 0,00 und 100.000,00 eingeben, z. B. 12,90'),
  ).toBeVisible();
});

test('create and delete a download package and a shipping method', async ({ page }) => {
  const id = unique();
  await openPriceList(page);

  await page.getByRole('link', { name: 'Paket hinzufügen' }).click();
  await page.getByLabel('Name').fill(`Galerie ${id}`);
  await page.getByLabel('Inhalt').click();
  await page.getByRole('option', { name: 'Ganze Galerie' }).click();
  await expect(page.getByLabel('Anzahl Bilder')).toHaveCount(0);
  await page.getByLabel('Paketpreis').fill('199');
  await page.getByRole('button', { name: 'Speichern' }).click();
  await expect(page.getByText(`„Galerie ${id}“ wurde angelegt.`)).toBeVisible();
  await expect(
    page.getByRole('table', { name: 'Download-Pakete' }).getByRole('row', {
      name: new RegExp(`Galerie ${id} Ganze Galerie Original 199,00 €`),
    }),
  ).toBeVisible();

  await page.getByRole('link', { name: 'Versandart hinzufügen' }).click();
  await page.getByLabel('Name').fill(`Kurier ${id}`);
  await page.getByLabel('Kosten').fill('9,90');
  await page.getByRole('button', { name: 'Speichern' }).click();
  await expect(page.getByText(`„Kurier ${id}“ wurde angelegt.`)).toBeVisible();

  for (const name of [`Galerie ${id}`, `Kurier ${id}`]) {
    await page.getByRole('button', { name: `${name} löschen` }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Löschen' }).click();
    await expect(page.getByText(`„${name}“ wurde gelöscht.`)).toBeVisible();
  }
});

test('change the VAT rate', async ({ page }) => {
  await openPriceList(page);

  await page.getByRole('link', { name: 'Steuersatz ändern' }).click();
  await page.getByLabel('Umsatzsteuer').fill('7');
  await page.getByRole('button', { name: 'Speichern' }).click();
  await expect(page.getByTestId('vat-rate')).toHaveText('7 %');

  // Restore the dev data
  await page.getByRole('link', { name: 'Steuersatz ändern' }).click();
  await page.getByLabel('Umsatzsteuer').fill('19');
  await page.getByRole('button', { name: 'Speichern' }).click();
  await expect(page.getByTestId('vat-rate')).toHaveText('19 %');
});

test('photographers see the price list read-only', async ({ page }) => {
  await openPriceList(page, 'foto-a');

  await expect(
    page.getByText('Nur Studio-Administratoren können die Preisliste ändern.'),
  ).toBeVisible();
  await expect(
    page.getByRole('table', { name: 'Abzüge' }).getByText('Glänzend').first(),
  ).toBeVisible();
  await expect(page.getByRole('link', { name: 'Abzug hinzufügen' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: /löschen$/ })).toHaveCount(0);
});

test('studio B does not see the price list of studio A', async ({ page }) => {
  await openPriceList(page, 'admin-b');

  await expect(page.getByTestId('vat-rate')).toHaveText('0 %');
  await expect(
    page.getByRole('table', { name: 'Versandarten' }).getByText('Briefversand'),
  ).toBeVisible();
  await expect(page.getByText('Standardversand')).toHaveCount(0);

  await page.goto('/studio/preisliste/produkte/e1000000-0000-4000-8000-000000000003');
  await expect(page.getByText('Das Produkt wurde nicht gefunden.')).toBeVisible();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Preisliste');
});

test('download variants: create freely named sizes, variants in use cannot be deleted', async ({
  page,
}) => {
  const id = unique();
  await openPriceList(page);

  await page.getByRole('link', { name: 'Download hinzufügen' }).click();
  await page.getByLabel('Name der Variante').fill(`Druck ${id}`);
  await page.getByLabel('Maximale Kantenlänge').fill('100');
  await expect(page.getByText('Mindestens 200')).toBeVisible();
  await page.getByLabel('Maximale Kantenlänge').fill('4000');
  await page.getByLabel('Preis je Bild').fill('14,90');
  await page.getByRole('button', { name: 'Speichern' }).click();
  await expect(page.getByText(`„Download Druck ${id}“ wurde angelegt.`)).toBeVisible();
  await expect(
    page
      .getByRole('table', { name: 'Downloads' })
      .getByRole('row', { name: new RegExp(`Druck ${id} max\\. 4\\.000 px 14,90 €`) }),
  ).toBeVisible();

  // "Original" is used by the packages of the dev data
  await page.getByRole('button', { name: 'Download Original löschen' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Löschen' }).click();
  await expect(page.getByText(/wird vom Paket „10 Downloads“ verwendet/)).toBeVisible();

  await page.getByRole('button', { name: `Download Druck ${id} löschen` }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Löschen' }).click();
  await expect(page.getByText(`„Download Druck ${id}“ wurde gelöscht.`)).toBeVisible();
});
