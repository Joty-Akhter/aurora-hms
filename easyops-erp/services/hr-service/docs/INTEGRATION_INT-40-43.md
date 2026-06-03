# HR integration contracts (INT-40–INT-43)

This document describes **synchronous REST** integration behavior in `hr-service` for salary, payroll, accounting export, and loans. Async messaging is not used in these paths today; consumers should treat APIs as **request/response** with the headers and keys below.

## Correlation and traceability (INT-42)

| Mechanism | Behavior |
|-----------|----------|
| **Request header** | Clients may send `X-Correlation-Id` or `X-Request-Id` (first non-empty wins). Max length 128 characters. |
| **Response header** | Every response includes `X-Correlation-Id` (generated UUID if the client did not send one). |
| **Logging** | The same value is stored in SLF4J **MDC** under key `correlationId` for log correlation. |
| **Payroll accounting export DTO** | `GET /api/hr/payroll/runs/{id}/accounting-export` returns `PayrollAccountingExportDto` with `correlationId`, `payrollRunId`, `payPeriodStart`, `payPeriodEnd`, and line-level component references for audit. |
| **Accounting journal JSON** | Outbound journal payloads include `correlationId` (when present), `referenceId`, and for payroll posts `payrollRunId`. |

## Idempotency (INT-41)

| Operation | Idempotency key | Notes |
|-----------|-----------------|-------|
| **Post payroll journal to accounting** | `PAYROLL-{payrollRunId}` | Same as `referenceId` in the journal body; also sent as HTTP header `Idempotency-Key` to the accounting service. Retries should reuse the same key. |
| **Post EPF journal to accounting** | `EPF-{organizationId}-{month}-{year}` | Matches `referenceId` in the journal body; also sent as `Idempotency-Key`. |
| **Loan PAYROLL repayment** | `(loanId, payrollRunId)` | Enforced in HR data: duplicate `recordPayrollRepayment` for the same pair returns **409 Conflict**. |
| **Populate payroll from salary** | N/A | Recomputes `PayrollDetail` for a **DRAFT** run only; not a duplicate-safe “post” in the accounting sense. |

## Failure semantics (INT-43)

| Area | Behavior |
|------|----------|
| **In-process HR transactions** | `PayrollService.processPayrollRun` / `approvePayrollRun` run in a **single transaction** with loan `confirmPayrollDeductions`. If loan posting fails, the payroll status change **rolls back** (no partial HR state). |
| **Populate from salary** | Runs in one transaction; failures roll back payroll detail rows for that operation. |
| **Accounting HTTP calls** | `AccountingFinanceIntegrationService` calls are **all-or-nothing per request**: if the accounting service returns an error, **no** in-memory “half journal” is stored in HR (the journal lives in accounting). Clients should **retry with the same** `Idempotency-Key` and `X-Correlation-Id` to avoid duplicate business posts if the accounting service implements deduplication. |
| **EPF policy (INT-43)** | When `hr.payroll.fail-on-missing-epf-policy=true`, `populate-from-salary` fails if no `hr.epf_organization_policy` row exists for the org. |
| **Tax slabs (INT-43)** | When `hr.payroll.fail-on-missing-tax-slabs=true`, `populate-from-salary` fails if the salary component set includes a statutory **INCOME_TAX** line but there are no active `tax_slabs` for the calendar year of the pay period end. Default is **false** (tax computes as 0 when slabs are missing). |
| **External payroll HTTP** | Not required for statutory engines; see **Payroll engine location** below. |

## API summary (INT-40)

| Endpoint | Purpose |
|----------|---------|
| `GET /api/hr/payroll/runs/{id}/accounting-export` | Read-only export for journals and reconciliation. Lines include optional **expense** / **liability** COA codes from salary component master when set (**INT-20**); supports **detail** vs **summary** views for posting (**INT-21**). |
| `POST /api/hr/payroll/runs/{id}/post-to-accounting` | Posts payroll journal (uses export + headers above). |
| `POST /api/hr/payroll/runs/{id}/post-epf-to-accounting` | Posts EPF journal for the period. |
| `POST /api/hr/loans/payroll-recoveries/confirm` | Idempotent loan ledger posting for a payroll run (also invoked automatically from payroll process/approve). |

For error handling, clients should rely on **HTTP status codes** and response bodies from `hr-service` and downstream services; payloads are JSON unless noted otherwise.

## Payroll engine location and optional external payroll-service

| Topic | Behavior |
|-------|----------|
| **Canonical payroll** | Salary assignment, payroll run lifecycle, `populate-from-salary`, statutory engines (**`EpfPayrollCalculationService`**, **`StatutoryPayrollCalculationService`**), payslip, and accounting export all run **inside `hr-service`**. |
| **`PayrollIntegrationService`** | Historically called a separate **`payroll-service`** (`services.payroll.base-url`). That is **optional**. |
| **`hr.integration.external-payroll-service.enabled`** | Default **`false`**. When false, `PayrollIntegrationService` **does not** HTTP POST/PUT to an external payroll service; it only persists HR data (e.g. linking `EpfContribution` to `payrollRunId`). Set **`true`** only if you deploy a service that implements those callbacks. |
| **Tax / ESI** | Income tax uses **`tax_slabs`** for the pay period year; ESI uses **`ESI_WAGE`**-tagged earnings and configurable rates/ceiling under **`hr.payroll.*`**. |

See also: `StatutoryPayrollCalculationService`, `EpfPayrollCalculationService`, `PayrollCalculationService`.
