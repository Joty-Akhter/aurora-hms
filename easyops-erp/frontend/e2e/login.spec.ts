import { test, expect } from '@playwright/test';
import { installLoginFailureMock, installLoginSuccessMocks } from './fixtures/login-api-mocks';

test.describe('Login', () => {
  test('successful login navigates to dashboard (mocked API)', async ({ page }) => {
    await installLoginSuccessMocks(page);

    await page.goto('/login');

    await expect(page.getByRole('heading', { name: 'Aurora HMS' })).toBeVisible();
    await page.locator('#usernameOrEmail').fill('admin');
    await page.locator('#password').fill('Admin123!');
    await page.getByRole('button', { name: 'Sign In' }).click();

    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByRole('heading', { name: /Welcome back/i })).toBeVisible({ timeout: 15_000 });
  });

  test('invalid credentials show an error (mocked API)', async ({ page }) => {
    await installLoginFailureMock(page);

    await page.goto('/login');
    await page.locator('#usernameOrEmail').fill('wrong');
    await page.locator('#password').fill('wrong');
    await page.getByRole('button', { name: 'Sign In' }).click();

    await expect(page.locator('.MuiAlert-root').first()).toContainText(/Invalid username or password/i);
    await expect(page).toHaveURL(/\/login/);
  });
});

test.describe('Login (live stack)', () => {
  test('successful login with real backend', async ({ page }) => {
    const user = process.env.E2E_USERNAME;
    const pass = process.env.E2E_PASSWORD;
    test.skip(!user || !pass, 'Set E2E_USERNAME and E2E_PASSWORD to run this test against a running API gateway.');

    await page.goto('/login');
    await page.locator('#usernameOrEmail').fill(user!);
    await page.locator('#password').fill(pass!);
    await page.getByRole('button', { name: 'Sign In' }).click();

    await expect(page).toHaveURL(/\/(dashboard|select-organization)/, { timeout: 30_000 });
  });
});
