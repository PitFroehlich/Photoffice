import { APIRequestContext, Page, expect, test } from '@playwright/test';
import { loginAs, logout } from './helpers';

/**
 * Own account (issue #46, ADR 0011): "Profil bearbeiten" and "Passwort ändern" in the user menu lead to the Keycloak
 * pages (application-initiated actions) and back to the page the user came from; the Keycloak account console is off.
 * Uses freshly created Keycloak users (bootstrap admin admin/admin), so the dev users stay unchanged.
 * Requires the local stack (docker compose, backend with profile "dev", npm start).
 */
const keycloakUrl = process.env['E2E_KEYCLOAK_URL'] ?? 'http://localhost:8180';
const adminApi = `${keycloakUrl}/admin/realms/photoffice`;

interface TestUser {
  id: string;
  username: string;
  password: string;
}

async function adminHeaders(request: APIRequestContext) {
  const response = await request.post(
    `${keycloakUrl}/realms/master/protocol/openid-connect/token`,
    {
      form: {
        grant_type: 'password',
        client_id: 'admin-cli',
        username: 'admin',
        password: 'admin',
      },
    },
  );
  return { Authorization: `Bearer ${(await response.json()).access_token}` };
}

/** Creates an enabled user with password, realm role and (for studio users) membership in "Studio A". */
async function createUser(
  request: APIRequestContext,
  role: 'photographer' | 'platform-admin',
): Promise<TestUser> {
  const headers = await adminHeaders(request);
  const username = `konto-${role}-${Date.now()}`;
  const password = 'Start-123';
  const created = await request.post(`${adminApi}/users`, {
    headers,
    data: {
      username,
      email: `${username}@studio-a.test`,
      emailVerified: true,
      firstName: 'Karla',
      lastName: 'Konto',
      enabled: true,
      credentials: [{ type: 'password', value: password, temporary: false }],
    },
  });
  expect(created.status()).toBe(201);
  const id = created.headers()['location'].split('/').pop()!;
  const realmRole = await (await request.get(`${adminApi}/roles/${role}`, { headers })).json();
  await request.post(`${adminApi}/users/${id}/role-mappings/realm`, { headers, data: [realmRole] });
  if (role === 'photographer') {
    const organizations: { id: string; alias: string }[] = await (
      await request.get(`${adminApi}/organizations`, { headers })
    ).json();
    const studioA = organizations.find((organization) => organization.alias === 'studio-a')!;
    const member = await request.post(`${adminApi}/organizations/${studioA.id}/members`, {
      headers: { ...headers, 'Content-Type': 'application/json' },
      data: JSON.stringify(id),
    });
    expect(member.ok(), await member.text()).toBe(true);
  }
  return { id, username, password };
}

async function deleteUser(request: APIRequestContext, user: TestUser | undefined): Promise<void> {
  if (user) {
    await request.delete(`${adminApi}/users/${user.id}`, { headers: await adminHeaders(request) });
  }
}

async function openUserMenuItem(page: Page, item: string): Promise<void> {
  await page.getByRole('button', { name: 'Benutzermenü' }).click();
  await page.getByRole('menuitem', { name: item }).click();
}

test.describe('own account', () => {
  let studioUser: TestUser | undefined;
  let operator: TestUser | undefined;

  test.afterAll(async ({ request }) => {
    await deleteUser(request, studioUser);
    await deleteUser(request, operator);
  });

  test('studio user changes name and password via the user menu', async ({ page, request }) => {
    studioUser = await createUser(request, 'photographer');
    await loginAs(page, studioUser.username, studioUser.password);
    await page.getByRole('link', { name: 'Kunden' }).click();
    await expect(page).toHaveURL(/\/studio\/kunden$/);
    await expect(page.locator('.user-name')).toHaveText('Karla Konto');

    // Edit profile: name editable, e-mail address read-only
    await openUserMenuItem(page, 'Profil bearbeiten');
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Profil bearbeiten');
    await expect(page.locator('#email')).toBeDisabled();
    await expect(page.getByText('Ihre E-Mail-Adresse ist Ihre Anmeldung')).toBeVisible();
    await page.locator('#lastName').fill('Neumann');
    await page.locator('#kc-submit').click();

    // Back on the same page with the new name
    await expect(page).toHaveURL(/\/studio\/kunden$/);
    await expect(page.locator('.user-name')).toHaveText('Karla Neumann');
    await expect(page.getByText('Ihr Profil wurde gespeichert.')).toBeVisible();

    // Cancel leads back as well, nothing changes
    await openUserMenuItem(page, 'Passwort ändern');
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Passwort ändern');
    await page.locator('#kc-cancel').click();
    await expect(page).toHaveURL(/\/studio\/kunden$/);
    await expect(page.getByText('Ihr Passwort wurde geändert.')).toBeHidden();

    // Change password
    await openUserMenuItem(page, 'Passwort ändern');
    await page.locator('#password-new').fill('Neu-Geheim-456');
    await page.locator('#password-confirm').fill('Neu-Geheim-456');
    await page.locator('#kc-submit').click();
    await expect(page).toHaveURL(/\/studio\/kunden$/);
    await expect(page.getByText('Ihr Passwort wurde geändert.')).toBeVisible();

    // Login with the new password
    await logout(page);
    await loginAs(page, studioUser.username, 'Neu-Geheim-456');
    await expect(page.locator('.user-name')).toHaveText('Karla Neumann');
  });

  test('platform operator edits the profile from the platform area', async ({ page, request }) => {
    operator = await createUser(request, 'platform-admin');
    await loginAs(page, operator.username, operator.password);
    await page.getByRole('link', { name: 'Studios' }).click();
    await expect(page).toHaveURL(/\/plattform\/studios$/);

    await openUserMenuItem(page, 'Profil bearbeiten');
    await page.locator('#firstName').fill('Kai');
    await page.locator('#kc-submit').click();

    await expect(page).toHaveURL(/\/plattform\/studios$/);
    await expect(page.locator('.user-name')).toHaveText('Kai Konto');
  });

  test('the Keycloak account console is switched off', async ({ page }) => {
    await page.goto(`${keycloakUrl}/realms/photoffice/account`);
    await expect(page.getByText('Seite nicht gefunden')).toBeVisible();
  });
});
