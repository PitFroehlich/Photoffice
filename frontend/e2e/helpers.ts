import { expect, Page } from '@playwright/test';

/**
 * Logs in on the Keycloak page (dev realm users: password = username).
 * With organizations enabled Keycloak asks for the username first and for the password on a second page.
 */
export async function loginAs(page: Page, username: string, password = username): Promise<void> {
  await page.goto('/');
  await page.getByRole('button', { name: 'Studio-Login' }).click();
  await page.locator('#username').fill(username);
  await page.locator('#kc-login').click();
  await page.locator('#password').fill(password);
  await page.locator('#kc-login').click();
  await expect(page).toHaveURL(/\/studio/);
}

export async function logout(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Benutzermenü' }).click();
  await page.getByRole('menuitem', { name: 'Abmelden' }).click();
}
