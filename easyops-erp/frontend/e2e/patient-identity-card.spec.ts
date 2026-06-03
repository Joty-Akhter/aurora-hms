import { test, expect } from '@playwright/test';
import { installLoginSuccessMocks } from './fixtures/login-api-mocks';

test.describe('Patient identity card (Phase 1 smoke)', () => {
  test('new patient registration shows print flow and navigates to list', async ({ page }) => {
    await installLoginSuccessMocks(page);

    await page.route(
      (url) => url.pathname === '/api/hospital/patients' && url.search === '',
      async (route) => {
        if (route.request().method() !== 'POST') {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            patientId: '11111111-1111-4111-8111-111111111111',
            mrn: 'HOSP-2026-000001',
            fullName: 'Jane Doe',
            dateOfBirth: '1990-01-01',
            patientStatus: 'ACTIVE',
            identityCardStatus: 'ISSUED',
            identityCardNumber: 'HOSP-2026-000001',
            identityCardId: '22222222-2222-4222-8222-222222222222',
          }),
        });
      }
    );

    await page.route(
      (url) =>
        url.pathname === '/api/hospital/patients/11111111-1111-4111-8111-111111111111/identity-card/reprint',
      async (route) => {
        if (route.request().method() !== 'POST') {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            patientId: '11111111-1111-4111-8111-111111111111',
            mrn: 'HOSP-2026-000001',
            cardId: '22222222-2222-4222-8222-222222222222',
            cardNumber: 'HOSP-2026-000001',
            title: 'Patient identity card',
            action: 'PRINT',
            html: '<html><body><h1>Patient Identity Card</h1></body></html>',
          }),
        });
      }
    );

    // 1st dialog is the post-registration alert; 2nd dialog is print confirm.
    let dialogCount = 0;
    page.on('dialog', async (dialog) => {
      dialogCount += 1;
      if (dialog.type() === 'confirm') {
        await dialog.dismiss();
      } else {
        await dialog.accept();
      }
    });

    await page.goto('/login');
    await page.locator('#usernameOrEmail').fill('admin');
    await page.locator('#password').fill('Admin123!');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL(/\/dashboard$/);

    await page.goto('/hospital/patients/new');
    await page.locator('input[placeholder*="Given and family name"]').fill('Jane Doe');
    await page.locator('input[type="date"]').fill('1990-01-01');
    await page.getByRole('button', { name: 'Register Patient' }).click();

    await expect(page).toHaveURL(/\/hospital\/patients$/);
    expect(dialogCount).toBeGreaterThanOrEqual(2);
  });
});
