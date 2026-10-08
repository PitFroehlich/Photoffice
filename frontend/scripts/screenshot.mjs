// Screenshot helper for demos (docs/demos). Requires the running local stack.
// Usage: node scripts/screenshot.mjs <path> <output.png> [--login <dev-user>] [--wait <css-selector>]
// Example: node scripts/screenshot.mjs /studio ../docs/demos/0006-studio-login/02-studio.png --login admin-a
import { chromium } from '@playwright/test';

const [path, output, ...rest] = process.argv.slice(2);
if (!path || !output) {
  console.error('Usage: node scripts/screenshot.mjs <path> <output.png> [--login <dev-user>] [--wait <selector>]');
  process.exit(1);
}
const option = (name) => {
  const index = rest.indexOf(name);
  return index >= 0 ? rest[index + 1] : undefined;
};
const baseUrl = process.env.E2E_BASE_URL ?? 'http://localhost:4200';
const user = option('--login');
const waitFor = option('--wait');

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1100, height: 700 } });
try {
  if (user) {
    // Dev realm: password = username; Keycloak asks for username and password on separate pages
    await page.goto(baseUrl + '/');
    await page.getByRole('button', { name: 'Studio-Login' }).click();
    await page.locator('#username').fill(user);
    await page.locator('#kc-login').click();
    await page.locator('#password').fill(user);
    await page.locator('#kc-login').click();
    await page.getByRole('button', { name: 'Abmelden' }).waitFor();
  }
  await page.goto(baseUrl + path);
  if (waitFor) {
    await page.locator(waitFor).first().waitFor();
  } else {
    await page.waitForLoadState('networkidle');
  }
  await page.screenshot({ path: output, fullPage: true });
  console.log('Saved', output);
} finally {
  await browser.close();
}
