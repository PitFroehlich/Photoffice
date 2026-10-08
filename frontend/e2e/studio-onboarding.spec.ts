import { APIRequestContext, expect, test } from '@playwright/test';

/**
 * Studio onboarding (issue #24): the platform operator registers a studio, the first studio admin receives an
 * invitation e-mail (Mailpit), sets a password and logs in. Suspending the studio blocks access.
 * Requires the local stack incl. Mailpit (docker compose) and the backend with profile "dev".
 */
const keycloak = 'http://localhost:8180/realms/photoffice';
const backend = 'http://localhost:8080/api';
const mailpit = 'http://localhost:8025/api/v1';

async function operatorToken(request: APIRequestContext): Promise<string> {
  const response = await request.post(`${keycloak}/protocol/openid-connect/token`, {
    form: { grant_type: 'password', client_id: 'photoffice-dev-cli', username: 'operator', password: 'operator' },
  });
  return (await response.json()).access_token;
}

async function invitationLink(request: APIRequestContext, email: string): Promise<string> {
  let messageId: string | undefined;
  await expect
    .poll(async () => {
      const result = await (await request.get(`${mailpit}/search`, { params: { query: `to:${email}` } })).json();
      messageId = result.messages[0]?.ID;
      return messageId;
    })
    .toBeDefined();
  const message = await (await request.get(`${mailpit}/message/${messageId}`)).json();
  expect(message.Text).not.toContain('Passwort');
  return message.Text.match(/https?:\/\/\S+\/login-actions\/action-token\S+/)[0];
}

test('new studio: invited admin sets a password, logs in, loses access while suspended', async ({ page, request }) => {
  const slug = `studio-e2e-${Date.now()}`;
  const email = `inhaber@${slug}.test`;
  const auth = { Authorization: `Bearer ${await operatorToken(request)}` };

  // Platform operator registers the studio
  const created = await request.post(`${backend}/platform/tenants`, {
    headers: auth,
    data: { slug, name: 'Fotostudio E2E', adminEmail: email, adminFirstName: 'Maria', adminLastName: 'Müller' },
  });
  expect(created.status()).toBe(201);
  const studio = await created.json();
  await expect
    .poll(async () => {
      const studios = await (await request.get(`${backend}/platform/tenants`, { headers: auth })).json();
      return studios.find((s: { id: string }) => s.id === studio.id).onboardingStatus;
    })
    .toBe('COMPLETED');

  // Invitation e-mail → set password
  await page.goto(await invitationLink(request, email));
  await page.getByText('Click here to proceed').click();
  await page.locator('#password-new').fill('Geheim-123');
  await page.locator('#password-confirm').fill('Geheim-123');
  await page.locator('[type=submit]').click();
  await expect(page.getByText('Your account has been updated')).toBeVisible();
  await page.getByText('Back to Application').click();

  // Login with e-mail and new password
  await page.getByRole('button', { name: 'Studio-Login' }).click();
  await page.locator('#username').fill(email);
  await page.locator('#kc-login').click();
  await page.locator('#password').fill('Geheim-123');
  await page.locator('#kc-login').click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Fotostudio E2E');
  await expect(page.locator('.welcome')).toContainText('Maria Müller');
  await expect(page.locator('.welcome')).toContainText('Studio-Administrator');

  // Suspended studio: no access
  expect((await request.post(`${backend}/platform/tenants/${studio.id}/suspend`, { headers: auth })).status()).toBe(200);
  await page.reload();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Kein Zugriff');

  // Reactivated: access again
  expect((await request.post(`${backend}/platform/tenants/${studio.id}/reactivate`, { headers: auth })).status()).toBe(200);
  await page.reload();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Fotostudio E2E');
});
