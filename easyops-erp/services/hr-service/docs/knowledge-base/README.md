# EasyOps HR — Knowledge base

Welcome to the **HR knowledge base** for `hr-service`. Use this portal to find procedures, concepts, and links to detailed manuals.

---

## Start here

| Document | Audience | Contents |
|----------|----------|----------|
| [**EASYOPS-HR-USER-MANUAL.md**](EASYOPS-HR-USER-MANUAL.md) | All HR / Payroll / Finance users | End-to-end manual: roles, navigation, loans, payroll recovery, self-service, audit, troubleshooting |
| [**HR Salary — User Manual**](../../HR-SALARY-USER-MANUAL.md) | Payroll, HR Admin | Salary structures, components, assignments, payroll runs, payslips |
| [**HR EPF — User Manual**](../../HR-EPF-USER-MANUAL.md) | Payroll, HR Admin | Provident fund accounts, contributions, withdrawals, statements |
| [**Loans ↔ payroll (INT-35–INT-39)**](../INTEGRATION_LOANS_PAYROLL_INT-35-39.md) | Payroll, Finance, integrators | Contract: loan recovery in payslip, preview/confirm APIs, LOP/OT references |
| [**HR integration (INT-40–INT-43)**](../INTEGRATION_INT-40-43.md) | Integrators | Correlation, idempotency, accounting export behavior |

---

## Browse by topic

### Loans and salary advance

| Article | Description |
|---------|-------------|
| [topics/employee-loans-detailed.md](topics/employee-loans-detailed.md) | Categories, applications, disbursement, repayments, settlement, holiday calendar (AD-03), COA export (PI-05), org audit |

### Payroll integration with loans

| Article | Description |
|---------|-------------|
| [topics/payroll-loan-recovery.md](topics/payroll-loan-recovery.md) | Recovery preview, confirm, anomalies (RP-05), cross-check by payroll run |

### Employees and self-service

| Article | Description |
|---------|-------------|
| [topics/employee-self-service-hr.md](topics/employee-self-service-hr.md) | My loans, in-app loan notifications, payslips (cross-reference) |

### Reference

| Article | Description |
|---------|-------------|
| [topics/appendix-hr-routes-and-apis.md](topics/appendix-hr-routes-and-apis.md) | Frontend routes (`/hr/...`) and representative loan-related API prefixes |

---

## How to use this knowledge base

1. **New users** — Read [EASYOPS-HR-USER-MANUAL.md](EASYOPS-HR-USER-MANUAL.md) sections **1–2** (overview, roles, navigation).
2. **Loan administrators** — Loans sections in the master manual + [employee-loans-detailed.md](topics/employee-loans-detailed.md).
3. **Payroll operators** — [payroll-loan-recovery.md](topics/payroll-loan-recovery.md) + salary manual for runs and components.
4. **Employees** — [employee-self-service-hr.md](topics/employee-self-service-hr.md).

---

## Conventions

- **Requirement IDs** (e.g. RE-04, AD-03) are included where they help map business rules to documentation; they match `requirements/Module-HR/` in the repository.
- **API paths** are relative to the API gateway base URL (e.g. `/api/hr/...`). Authentication uses standard EasyOps headers (`X-User-Id`, organization context).
- **RBAC:** Fine-grained loan permissions (`hr_loans_*`) are enforced on the server; the UI may show menu items based on coarse `hr` + `view` — a **403** from the API means the user lacks permission for that action.

---

## Document control

| Version | Date | Notes |
|---------|------|-------|
| 1.0 | 2026-04-03 | Initial knowledge base portal + master manual + topic articles |
