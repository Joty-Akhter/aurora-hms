# Appendix — HR routes and loan APIs (reference)

This appendix lists **representative** frontend routes and **API prefixes** for the HR module. **Base URLs** depend on deployment (API gateway, e.g. port `8081`). Paths are stable in code; always verify in Swagger for your version: `/swagger-ui.html` on `hr-service` (default port **8096** in dev).

---

## 1. Frontend routes (SPA)

| Route | Screen |
|-------|--------|
| `/hr/loans` | Loan register |
| `/hr/loans/settings` | Organization loan settings |
| `/hr/loans/org-audit` | Org-level loan audit |
| `/hr/loans/payroll-recoveries` | Payroll recovery anomalies / cross-check |
| `/hr/loans/applications` | Loan applications |
| `/hr/loans/applications/new` | New application |
| `/hr/loans/applications/:applicationId` | Application detail |
| `/hr/loans/:loanId` | Loan detail |
| `/hr/my-loans` | Employee self-service loans |
| `/hr/my-loans/:loanId` | Employee loan detail |
| `/hr/provident-fund/organization-policy` | EPF organization policy (rates, PF wage limits, eligibility) |

---

## 2. Loan-related API prefixes (`hr-service`)

| Prefix | Controller area |
|--------|-------------------|
| `/api/hr/loans` | Org settings, categories |
| `/api/hr/loans/applications` | Applications |
| `/api/hr/loans/accounts` | Loan accounts, disbursement, repayments, installments, AD-03 recalc |
| `/api/hr/loans/payroll-recoveries` | Preview, confirm, anomalies |
| `/api/hr/loans/reports` | Reports, CSV, accounting export |
| `/api/hr/loans/audit` | Loan audit, `/combined`, `/org` |
| `/api/hr/loans/self` | Employee self-service (`my-loans`, notifications) |
| `/api/hr/loans/.../settlement/...` | Settlement endpoints |
| `/api/hr/epf/organization-policy` | GET/PUT organization EPF policy (see EPF user manual) |
| `/api/hr/payroll/runs/{id}/accounting-export` | Payroll amounts for finance; optional per-component GL codes when set on salary components |

**Headers:** `X-User-Id` is required for authenticated calls; **organization** is passed as query parameters or path as implemented per endpoint.

---

## 3. Configuration (reminders)

| Property | Purpose |
|----------|---------|
| `hr.loans.reminder.enabled` | Enable/disable **due-soon** reminder job |
| `hr.loans.reminder.cron` | Cron schedule (default daily 07:00) |
| `hr.loans.reminder.days-ahead` | How far ahead to look for due installments |
| `hr.loans.reminder.cooldown-days` | Dedupe window for the same installment reminder |

---

*Back to [Knowledge base home](../README.md)*
