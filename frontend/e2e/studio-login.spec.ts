import { expect, test } from '@playwright/test';
import { loginAs, logout } from './helpers';

test('studio admin of studio A sees her studio and can log out', async ({ page }) => {
  await loginAs(page, 'admin-a');

  await expect(page.locator('.studio-name')).toHaveText('Studio A');
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Übersicht');
  await expect(page.locator('.welcome')).toContainText('Willkommen, Anna Admin');
  await expect(page.locator('mat-chip')).toContainText('Studio-Administrator');

  await logout(page);
  await expect(page.getByRole('button', { name: 'Studio-Login' })).toBeVisible();
});

test('photographer of studio A', async ({ page }) => {
  await loginAs(page, 'foto-a');

  await expect(page.locator('.studio-name')).toHaveText('Studio A');
  await expect(page.locator('mat-chip')).toContainText('Fotograf');
});

test('studio admin of studio B sees studio B', async ({ page }) => {
  await loginAs(page, 'admin-b');

  await expect(page.locator('.studio-name')).toHaveText('Studio B');
});

test('platform operator is led to the platform area (#35)', async ({ page }) => {
  await loginAs(page, 'operator');

  await expect(page).toHaveURL(/\/plattform\/studios$/);
});

test('studio area requires login', async ({ page }) => {
  await page.goto('/studio');

  await expect(page.locator('#username')).toBeVisible();
});
