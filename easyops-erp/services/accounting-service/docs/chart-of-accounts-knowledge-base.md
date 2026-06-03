# Chart of Accounts — Technical Knowledge Base (Accounting Service)

This document describes how the **Chart of Accounts (CoA)** is modeled, exposed, and integrated in the EasyOps **accounting-service**. It is intended for developers, integrators, and administrators.

---

## 1. Purpose

The chart of accounts defines every **GL (general ledger) account** for an **organization**. Journal entries, trial balance, and financial reports resolve amounts to these accounts. Each organization maintains its **own** chart (scoped by `organization_id`).

---

## 2. Data Model

### 2.1 Database

- **Table:** `accounting.chart_of_accounts`
- **Primary key:** `id` (UUID)
- **Uniqueness:** `(organization_id, account_code)` — account codes must be unique **per organization**, not globally.

### 2.2 Entity: `ChartOfAccounts`

Key fields:

| Field | Notes |
|--------|--------|
| `organizationId` | Required; tenant scope |
| `accountCode` | Up to 50 characters; alphanumeric codes used in APIs and integrations (e.g. `1030`, `CASH`, `EPF_PAYABLE`) |
| `accountName` | Display name |
| `parentAccountId` | Optional UUID linking to a parent account |
| `accountType` | One of: `ASSET`, `LIABILITY`, `EQUITY`, `REVENUE`, `EXPENSE` |
| `accountCategory` / `accountSubcategory` | Optional classification (e.g. Current Assets) |
| `level` | Hierarchy depth (1–5 typical); used for reporting structure |
| `isGroup` | `true` = summary/header account; `false` = posting account |
| `isSystemAccount` | `true` for template-seeded accounts; **cannot be deactivated** via service rules |
| `currency` | Default `USD` |
| `openingBalance` / `openingBalanceDate` / `currentBalance` | Balances |
| `isActive` | Soft lifecycle; inactive accounts are excluded where “active” lists are used |
| `allowManualEntry` | Usually `false` for group accounts |
| `description`, `taxType`, `tags` | Optional metadata |

### 2.3 Group vs posting accounts

- **Group** (`isGroup = true`): Used to organize the tree; **not** intended for posting journal lines in strict setups. Parent validation on **create** requires the parent to be a group.
- **Posting** (`isGroup = false`): Accounts that hold transactional balances.

Repository method `findPostingAccounts` returns accounts where `isGroup = false` and `isActive = true`.

---

## 3. Business Rules (Service Layer)

Implemented in `ChartOfAccountsService`:

| Rule | Behavior |
|------|------------|
| Create duplicate code | Throws if `account_code` already exists for the organization |
| Parent on create | If `parentAccountId` is set, parent must exist and `isGroup` must be `true` |
| Deactivate | Sets `isActive = false`; **blocked** if `isSystemAccount` is `true` |
| Update | Updates name, categories, description, tags, `allowManualEntry`; **does not** persist a change to `accountCode` from the request (code change is logged as a warning only; transaction checks are TODO) |
| Load standard template | `loadStandardCoA` **no-ops** if the organization already has **any** account (`count > 0`) |

### 3.1 Auto-seeding

When `getOrganizationAccounts(organizationId)` runs, if the organization has **zero** accounts, the service attempts `loadStandardCoA(organizationId, null)` once, then returns the list. Failures are logged but do not necessarily fail the read.

### 3.2 Caching

- `@Cacheable` on `getOrganizationAccounts` and `getActiveAccounts` with keys tied to `organizationId`.
- Mutations (`create`, `update`, `deactivate`, `loadStandardCoA`) use `@CacheEvict` for the affected organization.

---

## 4. Standard Template (`loadStandardCoA`)

The built-in template is created in Java (not the SQL seed file). It creates **posting and group** accounts with `isSystemAccount = true` and `allowManualEntry` set from `!isGroup`.

Approximate size: **48 accounts** (cash, AR, inventory, fixed assets, liabilities, equity, revenue, COGS, operating expenses). Codes and labels may differ from Liquibase demo data or the frontend fallback template.

---

## 5. REST API

**Base path:** `/api/accounting/coa`  
**Identity:** All endpoints require header **`X-User-Id`** (UUID of the acting user).

| Method | Path | RBAC | Description |
|--------|------|------|-------------|
| `POST` | `/api/accounting/coa` | `accounting` **manage** | Create account (`CoARequest` body) |
| `GET` | `/api/accounting/coa/organization/{organizationId}` | `accounting` view **or** manage | All accounts (sorted by code); may trigger auto-load |
| `GET` | `/api/accounting/coa/organization/{organizationId}/active` | view or manage | Active accounts only |
| `GET` | `/api/accounting/coa/organization/{organizationId}/posting` | view or manage | Posting accounts only |
| `GET` | `/api/accounting/coa/organization/{organizationId}/type/{accountType}` | view or manage | Filter by `accountType` |
| `GET` | `/api/accounting/coa/{accountId}` | view or manage (org from account) | Account by ID |
| `PUT` | `/api/accounting/coa/{accountId}` | manage | Update account |
| `DELETE` | `/api/accounting/coa/{accountId}?organizationId={uuid}` | manage | Deactivate (soft) |
| `POST` | `/api/accounting/coa/organization/{organizationId}/load-standard-coa` | manage | Load template (only when org has no accounts) |

RBAC is enforced by `AccountingRbacService`:

- **View:** any of resource action `accounting` + `view` **or** `accounting` + `manage`
- **Manage:** `accounting` + `manage` only

---

## 6. Integration: Journal Entries by `accountCode`

`JournalIntegrationController` (`/api/accounting/journal-entries`) accepts payloads where each line includes **`accountCode`** (not only `accountId`). The service resolves `organizationId` + `accountCode` via `ChartOfAccountsService.getAccountByCode`. Missing codes throw **Account not found**.

This is the pattern used by other services (e.g. HR payroll posting) when posting to predefined GL codes such as `6110`, `1030`, `2020`, `CASH`, `EPF_PAYABLE`.

---

## 7. Downstream Usage

- **Trial balance** (`TrialBalanceService`): Uses `chart_of_accounts` joined to `journal_lines`; typically **posting**, **active** accounts.
- **Financial reports** (`FinancialReportService`): Loads accounts by organization for P&amp;L, balance sheet, etc.

Align account **types** with report buckets (e.g. balance sheet vs profit and loss).

---

## 8. Frontend (React)

- Page: `frontend/src/pages/accounting/ChartOfAccounts.tsx`
- API wrapper: `frontend/src/services/accountingService.ts`
- If the list API returns empty non-objects, the UI may show a **client-side fallback** template (subset); production should rely on the API after org is seeded or template is loaded.

---

## 9. Related Repository Assets

- Liquibase test/demo data may insert additional accounts per organization (e.g. payroll GL accounts).
- A reference SQL template exists under `infrastructure/docker/postgres/backup/_coa_template_standard.sql.template` (import-oriented; not identical to the Java `loadStandardCoA` list).

---

## 10. Operational Notes

- **Gateway:** Client calls usually go through the API gateway; paths remain `/api/accounting/...`.
- **Consistency:** After manual DB edits, cache eviction may require a service restart or wait for TTL depending on deployment.
- **Deleting organizations:** Schema uses `ON DELETE CASCADE` from organizations where applicable; verify foreign keys for journal lines before removing accounts.

---

## Document history

- Introduced as internal knowledge base for accounting-service Chart of Accounts.
