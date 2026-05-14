const { test, expect } = require('@playwright/test');

test('login-query-subscribe-remove flow', async ({ page }) => {
  const base = process.env.TEST_BASE_URL || 'http://localhost:8000';

  await page.goto(base);
  await page.screenshot({ path: 'tests/playwright/screenshot-before.png' });

  // Simple flow: navigate to login, fill, submit
  await page.goto(`${base}/login.html`);
  await page.fill('input[name="email"]', 'ci+test@example.com');
  await page.fill('input[name="password"]', 'testpassword');
  await page.click('button[type="submit"]');

  // Wait for redirect or token set
  await page.waitForTimeout(1000);

  // Capture network
  const harPath = 'tests/playwright/network.har';
  await page.context().tracing.start({ screenshots: true, snapshots: true });

  // Try to fetch songs via the UI's JS
  await page.goto(base);
  await page.waitForTimeout(1000);

  // End tracing
  await page.context().tracing.stop({ path: 'tests/playwright/trace.zip' });

  // Save a final screenshot
  await page.screenshot({ path: 'tests/playwright/screenshot-after.png' });
});
