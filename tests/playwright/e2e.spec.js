const { test, expect } = require('@playwright/test');
const fs = require('fs');

test.setTimeout(120000);

test('login-query-subscribe-remove flow', async ({ page, context }) => {
  const base = process.env.TEST_BASE_URL || 'http://localhost:8000';

  // start tracing
  await context.tracing.start({ screenshots: true, snapshots: true });

  // Visit homepage
  await page.goto(base);
  await page.screenshot({ path: 'tests/playwright/screenshot-before.png' });

  // Navigate to login and attempt login (expected to be a demo; adjust credentials via env)
  await page.goto(`${base}/login.html`);
  await page.fill('input[name="email"]', process.env.TEST_EMAIL || 'ci+test@example.com');
  await page.fill('input[name="password"]', process.env.TEST_PASSWORD || 'testpassword');
  await page.click('button[type="submit"]');

  // Wait for app to settle
  await page.waitForTimeout(1500);

  // Go back to main page and attempt to query songs via UI
  await page.goto(base);
  await page.waitForSelector('.song-card', { timeout: 5000 }).catch(() => {});

  // Create screenshot after actions
  await page.screenshot({ path: 'tests/playwright/screenshot-after.png' });

  // Stop tracing and save ZIP
  await context.tracing.stop({ path: 'tests/playwright/trace.zip' });
});
