import type { Page, Route } from '@playwright/test';

const MOCK_USER_ID = 'e2e-user-1';
const MOCK_ORG_ID = 'e2e-org-1';

const loginJson = {
  accessToken: 'e2e-mock-access-token',
  refreshToken: 'e2e-mock-refresh-token',
  tokenType: 'Bearer',
  expiresIn: 3600,
  userId: MOCK_USER_ID,
  username: 'admin',
  email: 'admin@example.com',
  firstName: 'Admin',
  lastName: 'User',
  /** Lets the app pass ProtectedRoute for /dashboard before RBAC sync completes */
  roles: ['SYSTEM_ADMIN'] as string[],
  permissions: [] as string[],
};

const rbacRoleSystemAdmin = [
  {
    id: 'e2e-role-admin',
    name: 'System Admin',
    code: 'SYSTEM_ADMIN',
    description: 'E2E',
    isSystemRole: true,
    isActive: true,
    createdAt: '',
    updatedAt: '',
    permissions: [] as unknown[],
  },
];

const singleOrgCatalog = {
  content: [
    {
      id: MOCK_ORG_ID,
      code: 'E2E',
      name: 'E2E Organization',
    },
  ],
  totalElements: 1,
};

/**
 * Mocks auth + org catalog + RBAC so a login reaches /dashboard without a real API gateway.
 * Matches requests to any host (VITE_* may point at localhost:8081).
 */
export async function installLoginSuccessMocks(page: Page): Promise<void> {
  await page.route(
    (url) => url.pathname === '/api/auth/login',
    async (route: Route) => {
      if (route.request().method() !== 'POST') {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(loginJson),
      });
    }
  );

  await page.route(
    (url) =>
      url.pathname === '/api/organizations' &&
      url.searchParams.get('page') === '0' &&
      url.searchParams.get('size') === '2',
    async (route: Route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(singleOrgCatalog),
      });
    }
  );

  await page.route(
    (url) =>
      url.pathname.startsWith(`/api/rbac/authorization/users/${MOCK_USER_ID}/roles`) &&
      !url.pathname.includes('has-role'),
    async (route: Route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(rbacRoleSystemAdmin),
      });
    }
  );

  await page.route(
    (url) =>
      url.pathname.startsWith(`/api/rbac/authorization/users/${MOCK_USER_ID}/permissions`),
    async (route: Route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    }
  );
}

export async function installLoginFailureMock(page: Page): Promise<void> {
  await page.route(
    (url) => url.pathname === '/api/auth/login',
    async (route: Route) => {
      if (route.request().method() !== 'POST') {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Invalid username or password' }),
      });
    }
  );
}
