import { expect, Page, test } from '@playwright/test';
import { loginAs, logout } from './helpers';

// Studio profile and legal texts (#20). Tests that change data restore the previous values (demo data).

const unique = () => Date.now().toString(36);

async function openProfile(page: Page, user = 'admin-a'): Promise<void> {
  await loginAs(page, user);
  await page.getByRole('link', { name: 'Studio-Profil' }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Studio-Profil');
}

async function openLegalTexts(page: Page, tab: string): Promise<void> {
  await page.getByRole('link', { name: 'Rechtstexte' }).first().click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Rechtstexte');
  await page.getByRole('tab', { name: tab }).click();
}

test('format errors are shown while typing and nothing is saved', async ({ page }) => {
  await openProfile(page);
  await expect(page.getByLabel('Anzeigename')).not.toHaveValue('');

  await page.getByLabel('IBAN').fill('DE89 3704 0044 0532 0130 01');
  await expect(page.getByText('Bitte eine gültige IBAN eingeben (Prüfsumme)')).toBeVisible();
  await page.getByLabel('IBAN').fill('DE89 3704 0044 0532 0130 00');
  await expect(page.getByText('Bitte eine gültige IBAN eingeben (Prüfsumme)')).toHaveCount(0);

  await page.getByLabel('USt-IdNr. / UID').fill('DE12345');
  await expect(
    page.getByText('z. B. DE123456789, ATU12345678 oder CHE-123.456.789 MWST'),
  ).toBeVisible();

  await page.getByLabel('PLZ').fill('1060');
  await expect(page.getByText('5 Ziffern für Deutschland')).toBeVisible();
  await page.getByLabel('Land').click();
  await page.getByRole('option', { name: 'Österreich' }).click();
  await expect(page.getByText('5 Ziffern für Deutschland')).toHaveCount(0);

  await page.getByRole('button', { name: 'Speichern' }).click();
  await expect(page.getByText('Das Studio-Profil wurde gespeichert.')).toHaveCount(0);
});

test('change the profile, the backend normalises the values', async ({ page }) => {
  await openProfile(page);
  const phone = page.getByLabel('Telefon');
  const vatId = page.getByLabel('USt-IdNr. / UID');
  const previousPhone = await phone.inputValue();
  const previousVatId = await vatId.inputValue();

  const newPhone = `030 ${Date.now() % 10_000_000}`;
  await phone.fill(newPhone);
  await vatId.fill('de 987 654 321');
  await page.getByRole('button', { name: 'Speichern' }).click();
  await expect(page.getByText('Das Studio-Profil wurde gespeichert.')).toBeVisible();
  await expect(vatId).toHaveValue('DE987654321');

  await page.reload();
  await expect(phone).toHaveValue(newPhone);
  await expect(page.getByText(/Zuletzt gespeichert: \d\d\.\d\d\.\d{4}/)).toBeVisible();

  // Restore the dev data
  await phone.fill(previousPhone);
  await vatId.fill(previousVatId);
  await page.getByRole('button', { name: 'Speichern' }).click();
  await expect(page.getByText('Das Studio-Profil wurde gespeichert.')).toBeVisible();
});

test('legal text with live preview; studio B does not see it', async ({ page }) => {
  const marker = `E2E ${unique()}`;
  await openProfile(page);
  await openLegalTexts(page, 'Datenschutz');
  const editor = page.getByLabel('Datenschutzerklärung (Markdown)');
  const previous = await editor.inputValue();
  const preview = page.getByRole('region', { name: 'Vorschau Datenschutzerklärung' });

  await editor.fill(`# Datenschutz ${marker}\n\n**Wichtig** <script>alert(1)</script>`);
  await expect(preview.getByRole('heading', { name: `Datenschutz ${marker}` })).toBeVisible();
  await expect(preview.locator('strong')).toHaveText('Wichtig');
  await expect(preview.getByText('<script>alert(1)</script>')).toBeVisible();
  await expect(page.getByText('Ungespeicherte Änderungen')).toBeVisible();

  await page.getByRole('button', { name: 'Datenschutz speichern' }).click();
  await expect(page.getByText('„Datenschutzerklärung“ wurde gespeichert.')).toBeVisible();

  await logout(page);
  await loginAs(page, 'admin-b');
  await openLegalTexts(page, 'Datenschutz');
  await expect(page.getByText(marker)).toHaveCount(0);
  await logout(page);

  // Restore the previous text of studio A
  await loginAs(page, 'admin-a');
  await openLegalTexts(page, 'Datenschutz');
  await expect(editor).toHaveValue(new RegExp(marker));
  await editor.fill(previous);
  await page.getByRole('button', { name: 'Datenschutz speichern' }).click();
  await expect(
    page.getByText(/„Datenschutzerklärung“ wurde (gespeichert|entfernt)\./),
  ).toBeVisible();
});

test('an empty imprint gets an outline from the studio profile', async ({ page }) => {
  await loginAs(page, 'admin-b');
  await openLegalTexts(page, 'Impressum');
  const editor = page.getByLabel('Impressum (Markdown)');
  test.skip((await editor.inputValue()) !== '', 'Studio B already has an imprint');

  await page.getByRole('button', { name: 'Gliederung einfügen' }).click();
  await expect(editor).toHaveValue(/^# Impressum\n\n\*\*.+\*\*/);
  await expect(
    page
      .getByRole('region', { name: 'Vorschau Impressum' })
      .getByRole('heading', { name: 'Impressum' }),
  ).toBeVisible();
  // Not saved – the imprint of studio B stays empty
});

test('photographers see profile and legal texts read-only', async ({ page }) => {
  await openProfile(page, 'foto-a');
  await expect(
    page.getByText('Nur Studio-Administratoren können das Studio-Profil ändern.'),
  ).toBeVisible();
  await expect(page.getByLabel('Anzeigename')).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Speichern' })).toHaveCount(0);

  await openLegalTexts(page, 'AGB');
  await expect(
    page.getByText('Nur Studio-Administratoren können die Rechtstexte ändern.'),
  ).toBeVisible();
  await expect(page.locator('textarea')).toHaveCount(0);
});

test('studio B sees its own profile, not the one of studio A', async ({ page }) => {
  await openProfile(page);
  const nameOfA = await page.getByLabel('Anzeigename').inputValue();
  await logout(page);

  await openProfile(page, 'admin-b');
  await expect(page.getByLabel('Anzeigename')).not.toHaveValue(nameOfA);
});
