import { expect, Page, test } from '@playwright/test';

/**
 * Logs in on the Keycloak page (dev realm users: password = username).
 * With organizations enabled Keycloak asks for the username first and for the password on a second page.
 */
async function loginAs(page: Page, username: string): Promise<void> {
  await page.goto('/');
  await page.getByRole('button', { name: 'Studio-Login' }).click();
  await page.locator('#username').fill(username);
  await page.locator('#kc-login').click();
  await page.locator('#password').fill(username);
  await page.locator('#kc-login').click();
  await expect(page.getByRole('button', { name: 'Abmelden' })).toBeVisible();
}

test('studio admin of studio A sees her studio and can log out', async ({ page }) => {
  await loginAs(page, 'admin-a');

  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Studio A');
  await expect(page.locator('.welcome')).toContainText('Anna Admin');
  await expect(page.locator('.welcome')).toContainText('Studio-Administrator');

  await page.getByRole('button', { name: 'Abmelden' }).click();
  await expect(page.getByRole('button', { name: 'Studio-Login' })).toBeVisible();
});

test('photographer of studio A', async ({ page }) => {
  await loginAs(page, 'foto-a');

  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Studio A');
  await expect(page.locator('.welcome')).toContainText('Fotograf');
});

test('studio admin of studio B sees studio B', async ({ page }) => {
  await loginAs(page, 'admin-b');

  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Studio B');
});

test('platform operator has no studio', async ({ page }) => {
  await loginAs(page, 'operator');

  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Kein Zugriff');
});

test('studio area requires login', async ({ page }) => {
  await page.goto('/studio');

  await expect(page.locator('#username')).toBeVisible();
});
