import { APIRequestContext, Page, expect, test } from '@playwright/test';

/**
 * Studio list of the platform area (issue #45): server-side search (name, slug, admin e-mail), filters, paging and
 * the state in the URL. Requires the local stack (docker compose, backend with profile "dev").
 * Keycloak pages are only addressed via element ids.
 */
const keycloak = 'http://localhost:8180/realms/photoffice';
const backend = 'http://localhost:8080/api';

async function operatorToken(request: APIRequestContext): Promise<string> {
  const response = await request.post(`${keycloak}/protocol/openid-connect/token`, {
    form: { grant_type: 'password', client_id: 'photoffice-dev-cli', username: 'operator', password: 'operator' },
  });
  return (await response.json()).access_token;
}

async function registerStudio(request: APIRequestContext, token: string, name: string, slug: string, adminEmail: string) {
  const response = await request.post(`${backend}/platform/tenants`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { name, slug, adminEmail },
  });
  expect(response.status()).toBe(201);
}

async function loginAsOperator(page: Page): Promise<void> {
  await page.goto('/');
  await page.getByRole('link', { name: 'Plattform-Login' }).click();
  await page.locator('#username').fill('operator');
  await page.locator('#kc-login').click();
  await page.locator('#password').fill('operator');
  await page.locator('#kc-login').click();
  await expect(page).toHaveURL(/\/plattform\/studios$/);
}

/** Selects an option of a mat-select and waits until its panel is closed (reopening too early would toggle it). */
async function choose(page: Page, label: string, option: string): Promise<void> {
  await page.getByRole('combobox', { name: label }).click();
  await page.getByRole('option', { name: option, exact: true }).click();
  await expect(page.getByRole('listbox')).toHaveCount(0);
}

const rows = (page: Page) => page.locator('tr[mat-row]');
const header = (page: Page) => page.locator('app-page-header');

test('search, filter and page through the studios; the state survives a reload', async ({ page, request }) => {
  const marker = `liste${Date.now()}`;
  const token = await operatorToken(request);
  await registerStudio(request, token, `Atelier Licht ${marker}`, `${marker}-atelier`, `inhaber@${marker}-atelier.test`);
  await registerStudio(request, token, `Bildwerk ${marker}`, `${marker}-bildwerk`, `inhaber@${marker}-bildwerk.test`);
  // Fails: the address belongs to the admin of Studio A
  await registerStudio(request, token, `Conflict ${marker}`, `${marker}-konflikt`, 'admin@studio-a.test');
  await loginAsOperator(page);

  // Search by name part (case-insensitive) – all three, sorted by name
  await page.getByLabel('Name, Kürzel oder Admin-E-Mail suchen').fill(marker.toUpperCase());
  await expect(page).toHaveURL(new RegExp(`suche=${marker.toUpperCase()}`));
  await expect(rows(page)).toHaveCount(3);
  await expect(rows(page).first()).toContainText(`Atelier Licht ${marker}`);
  await expect(header(page)).toContainText(`3 Treffer für „${marker.toUpperCase()}“`);

  // Only failed onboardings
  await expect(page.locator(`tr[data-slug="${marker}-konflikt"]`)).toContainText('Fehlgeschlagen', {
    timeout: 30_000,
  });
  await choose(page, 'Onboarding', 'Fehlgeschlagen');
  await expect(page).toHaveURL(/onboarding=fehlgeschlagen/);
  await expect(rows(page)).toHaveCount(1);
  await expect(rows(page).first()).toContainText(`Conflict ${marker}`);
  await choose(page, 'Onboarding', 'Alle');
  await expect(rows(page)).toHaveCount(3);

  // Paging: two per page, second page has one studio; the URL keeps it across a reload
  await page.goto(`/plattform/studios?suche=${marker}&anzahl=2`);
  await expect(rows(page)).toHaveCount(2);
  await page.getByRole('button', { name: 'Nächste Seite' }).click();
  await expect(page).toHaveURL(/seite=1/);
  await expect(rows(page)).toHaveCount(1);
  await expect(rows(page).first()).toContainText(`Conflict ${marker}`);
  await page.reload();
  await expect(rows(page)).toHaveCount(1);
  await expect(page.getByLabel('Name, Kürzel oder Admin-E-Mail suchen')).toHaveValue(marker);
  await expect(page.locator('.mat-mdc-paginator-range-label')).toHaveText('3 – 3 von 3');

  // Edit and come back to the same view
  await rows(page).first().getByRole('link', { name: `Conflict ${marker} bearbeiten` }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText(`Conflict ${marker} bearbeiten`);
  await page.getByRole('link', { name: 'Abbrechen' }).click();
  await expect(page).toHaveURL(new RegExp(`suche=${marker}&seite=1&anzahl=2`));
  await expect(rows(page)).toHaveCount(1);

  // Search by slug (the admin e-mail is only searchable until onboarding completes – see PlatformTenantListTests)
  await page.goto('/plattform/studios');
  await page.getByLabel('Name, Kürzel oder Admin-E-Mail suchen').fill(`${marker}-bildwerk`);
  await expect(rows(page)).toHaveCount(1);
  await expect(rows(page).first()).toContainText(`Bildwerk ${marker}`);

  // No hits
  await page.getByLabel('Name, Kürzel oder Admin-E-Mail suchen').fill(`${marker}-gibt-es-nicht`);
  await expect(page.getByText('Keine Treffer')).toBeVisible();
  await expect(rows(page)).toHaveCount(0);
});

test('only failed onboardings from the header link', async ({ page, request }) => {
  const marker = `fehler${Date.now()}`;
  const token = await operatorToken(request);
  await registerStudio(request, token, `Fehler ${marker}`, `${marker}-studio`, 'admin@studio-a.test');
  await loginAsOperator(page);

  await expect(header(page)).toContainText('mit fehlgeschlagenem Onboarding', { timeout: 30_000 });
  await page.getByRole('button', { name: 'Nur fehlgeschlagene anzeigen' }).click();
  await expect(page).toHaveURL(/onboarding=fehlgeschlagen/);
  await expect(header(page)).toContainText('gefunden');
  // All rows on the page are failed onboardings
  await expect(rows(page).first()).toContainText('Fehlgeschlagen');
  const count = await rows(page).count();
  await expect(rows(page).filter({ hasText: 'Fehlgeschlagen' })).toHaveCount(count);

  // Failed test studios pile up locally: narrow down to ours, the filter stays
  await page.getByLabel('Name, Kürzel oder Admin-E-Mail suchen').fill(marker);
  await expect(page).toHaveURL(new RegExp(`suche=${marker}&onboarding=fehlgeschlagen`));
  await expect(rows(page)).toHaveCount(1);
  await expect(page.locator(`tr[data-slug="${marker}-studio"]`)).toContainText('Fehlgeschlagen');
});
