import { Page, expect, test } from '@playwright/test';
import { loginAs } from './helpers';

/**
 * Platform area (issue #35): the platform operator registers, suspends and reactivates studios and sees the
 * onboarding status including failures. Requires the local stack (docker compose, backend with profile "dev").
 * Keycloak pages are only addressed via element ids (texts depend on the login theme/locale).
 */

async function loginAsOperator(page: Page): Promise<void> {
  await page.goto('/');
  await page.getByRole('link', { name: 'Plattform-Login' }).click();
  await page.locator('#username').fill('operator');
  await page.locator('#kc-login').click();
  await page.locator('#password').fill('operator');
  await page.locator('#kc-login').click();
  await expect(page).toHaveURL(/\/plattform\/studios$/);
}

const row = (page: Page, slug: string) => page.locator(`tr[data-slug="${slug}"]`);

/** The list is paged (#45) and test studios pile up locally: search for the studio to find its row. */
async function searchFor(page: Page, term: string): Promise<void> {
  await page.getByLabel('Name, Kürzel oder Admin-E-Mail suchen').fill(term);
  await expect(page).toHaveURL(new RegExp(`suche=${encodeURIComponent(term)}`));
}

async function register(page: Page, name: string, slug: string, adminEmail: string): Promise<void> {
  await page.getByRole('link', { name: 'Studio registrieren' }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Studio registrieren');
  await page.getByLabel('Name', { exact: true }).fill(name);
  await page.getByLabel('Kürzel').fill(slug);
  await page.getByLabel('E-Mail').fill(adminEmail);
  await page.getByLabel('Vorname').fill('Maria');
  await page.getByLabel('Nachname').fill('Müller');
  await page.getByRole('button', { name: 'Registrieren' }).click();
}

test('platform operator lands in the platform area and sees the studios', async ({ page }) => {
  await loginAsOperator(page);

  await expect(page.locator('.area-name')).toHaveText('Plattform-Verwaltung');
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Studios');
  // "Studio " + letter only matches the names of the dev studios (slugs and e-mails contain no blank)
  await searchFor(page, 'Studio A');
  await expect(row(page, 'studio-a')).toContainText('Studio A');
  await expect(row(page, 'studio-a')).toContainText('Aktiv');
  await expect(row(page, 'studio-a')).toContainText('Abgeschlossen');
  await searchFor(page, 'Studio B');
  await expect(row(page, 'studio-b')).toContainText('Studio B');

  await page.getByRole('button', { name: 'Benutzermenü' }).click();
  await page.getByRole('menuitem', { name: 'Abmelden' }).click();
  await expect(page.getByRole('button', { name: 'Studio-Login' })).toBeVisible();
});

test('studio login of the platform operator also leads to the platform area', async ({ page }) => {
  await loginAs(page, 'operator');

  await expect(page).toHaveURL(/\/plattform\/studios$/);
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Studios');
});

test('studio users cannot enter the platform area', async ({ page }) => {
  await loginAs(page, 'admin-a');
  await expect(page.locator('.studio-name')).toHaveText('Studio A');

  await page.goto('/plattform');
  await expect(page).toHaveURL(/\/studio(\?.*)?$/);
  await expect(page.locator('.studio-name')).toHaveText('Studio A');
  await expect(page.locator('.area-name')).toHaveCount(0);
});

test('register a studio, onboarding completes, suspend and reactivate it', async ({ page }) => {
  const suffix = Date.now();
  const slug = `studio-e2e-${suffix}`;
  const name = `Fotostudio E2E ${suffix}`;
  await loginAsOperator(page);

  // Invalid slug is checked in the form, a taken slug at the field
  await page.getByRole('link', { name: 'Studio registrieren' }).click();
  await page.getByLabel('Kürzel').fill('Kein Kürzel!');
  await expect(page.getByText('3–63 Zeichen: Kleinbuchstaben')).toBeVisible();
  await page.getByRole('link', { name: 'Abbrechen' }).click();
  await register(page, 'Doppelt', 'studio-a', `doppelt-${suffix}@example.test`);
  await expect(page.getByText('Das Kürzel „studio-a“ ist bereits vergeben.')).toBeVisible();
  await page.getByRole('link', { name: 'Abbrechen' }).click();

  await register(page, name, slug, `inhaber@${slug}.test`);
  // The list shows the new studio (search for its slug, #45)
  await expect(page).toHaveURL(new RegExp(`/plattform/studios\\?suche=${slug}$`));
  await expect(page.getByText(`„${name}“ wurde registriert.`)).toBeVisible();
  await searchFor(page, slug);
  // The list reloads by itself until the onboarding (in the background) is complete
  await expect(row(page, slug)).toContainText('Abgeschlossen', { timeout: 30_000 });
  await expect(row(page, slug)).toContainText('Aktiv');

  await page.getByRole('button', { name: `${name} sperren` }).click();
  await expect(page.getByRole('dialog')).toContainText('Studio sperren?');
  await page.getByRole('dialog').getByRole('button', { name: 'Sperren' }).click();
  await expect(row(page, slug)).toContainText('Gesperrt');
  await expect(page.getByText(`„${name}“ ist gesperrt.`)).toBeVisible();

  await page.getByRole('button', { name: `${name} freischalten` }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Freischalten' }).click();
  await expect(row(page, slug)).toContainText('Aktiv');
});

test('failed onboarding is shown with its reason and can be retried', async ({ page }) => {
  const slug = `studio-konflikt-${Date.now()}`;
  await loginAsOperator(page);

  // admin@studio-a.test is the admin of Studio A – a user can only belong to one studio
  await register(page, 'Konflikt-Studio', slug, 'admin@studio-a.test');
  await searchFor(page, slug);
  await expect(row(page, slug)).toContainText('Fehlgeschlagen', { timeout: 30_000 });
  await expect(row(page, slug)).toContainText(
    'Die E-Mail-Adresse admin@studio-a.test gehört bereits zu Studio „Studio A“ (studio-a).',
  );
  await expect(row(page, slug)).toContainText('wird automatisch wiederholt');

  await row(page, slug).getByRole('button', { name: 'Onboarding von Konflikt-Studio erneut versuchen' }).click();
  await expect(page.getByText('Das Onboarding von „Konflikt-Studio“ wird erneut versucht.')).toBeVisible();
  await expect(row(page, slug)).toContainText('Fehlgeschlagen');
});

test('correct the admin of a failed onboarding, rename the studio, then only the name is editable', async ({
  page,
}) => {
  const slug = `studio-korrektur-${Date.now()}`;
  await loginAsOperator(page);

  await register(page, 'Korrektur-Studio', slug, 'admin@studio-a.test');
  await searchFor(page, slug);
  await expect(row(page, slug)).toContainText('Fehlgeschlagen', { timeout: 30_000 });

  // Issue #42: straight from the failure message to the form
  await row(page, slug).getByRole('link', { name: 'Daten korrigieren' }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Korrektur-Studio bearbeiten');
  await expect(page.getByRole('status')).toContainText('gehört bereits zu Studio „Studio A“');
  await expect(page.getByLabel('Kürzel')).toBeDisabled();
  await expect(page.getByLabel('Kürzel')).toHaveValue(slug);
  await expect(page.getByLabel('E-Mail')).toHaveValue('admin@studio-a.test');
  await expect(page.getByLabel('Vorname')).toHaveValue('Maria');

  await page.getByLabel('E-Mail').fill('kein-gueltiger-wert');
  await expect(page.getByText('Bitte eine gültige E-Mail-Adresse eingeben')).toBeVisible();
  await page.getByLabel('E-Mail').fill(`inhaber@${slug}.test`);
  await page.getByLabel('Name', { exact: true }).fill('Korrektur-Studio Neu');
  await page.getByRole('button', { name: 'Speichern' }).click();

  // Back to the list with the search kept in the URL
  await expect(page).toHaveURL(/\/plattform\/studios\?suche=/);
  await expect(
    page.getByText(
      '„Korrektur-Studio Neu“ wurde gespeichert. Das Onboarding wird mit den neuen Daten erneut versucht.',
    ),
  ).toBeVisible();
  await expect(row(page, slug)).toContainText('Abgeschlossen', { timeout: 30_000 });
  await expect(row(page, slug)).toContainText('Korrektur-Studio Neu');

  // After onboarding the admin data is gone; the name can still be changed
  await row(page, slug).getByRole('link', { name: 'Korrektur-Studio Neu bearbeiten' }).click();
  await expect(page.getByText('Das Onboarding ist abgeschlossen')).toBeVisible();
  await expect(page.getByLabel('E-Mail')).toHaveCount(0);
  await page.getByLabel('Name', { exact: true }).fill('Korrektur-Studio Final');
  await page.getByRole('button', { name: 'Speichern' }).click();
  await expect(page.getByText('„Korrektur-Studio Final“ wurde gespeichert.')).toBeVisible();
  await expect(row(page, slug)).toContainText('Korrektur-Studio Final');
});

test('unknown studio id in the edit url leads back to the list', async ({ page }) => {
  await loginAsOperator(page);

  await page.goto('/plattform/studios/00000000-0000-4000-8000-000000000000');
  await expect(page).toHaveURL(/\/plattform\/studios$/);
  await expect(page.getByText(/Kein Studio mit der ID/)).toBeVisible();
});
