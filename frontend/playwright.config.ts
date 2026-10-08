import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end tests against the locally running stack (see AGENTS.md, "End-to-end tests"):
 * docker compose up -d, backend with profile "dev", frontend via npm start.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  retries: 0,
  reporter: 'list',
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:4200',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
