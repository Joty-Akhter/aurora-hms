# EasyOps HR — User manual (hr-service)

This is the **master user manual** for the Human Resources features delivered by the **HR microservice** (`hr-service`) in EasyOps ERP. It complements the dedicated manuals for [**Salary**](../../HR-SALARY-USER-MANUAL.md) and [**EPF**](../../HR-EPF-USER-MANUAL.md).

---

## 1. Purpose and scope

### 1.1 What this manual covers

- **Organization-scoped HR operations** in EasyOps: employees, departments, positions, attendance, leave, payroll (coordination), **employee loans** and salary advance, **provident fund** (see EPF manual), benefits, reimbursements, and selected reports.
- **How to use the web application** (typical menu paths under **HR**), what each area is for, and **safe operational practices** (approvals, audit, idempotency).
- **Cross-links** to API-oriented behavior where it affects users (e.g. loan recovery idempotency per payroll run).

### 1.2 What this manual does not replace

- **Full product documentation** for every screen in the SPA (labels may evolve).
- **Legal or tax advice** — statutory rules remain your organization’s responsibility.
- **Database or deployment runbooks** — see DevOps / `CLAUDE.md` for developers.

### 1.3 Audience

| Role | Typical use |
|------|-------------|
| **HR Administrator** | Employee master, departments, positions, loan policy, applications |
| **Finance approver** | Loan application approval (when workflow includes Finance), COA mappings for accounting export |
| **Payroll operator** | Payroll runs, loan recovery preview/confirm, anomaly review |
| **Employee** | Self-service: my loans, notifications, PF self-service, salary view (per policy) |

---

## 2. Prerequisites

- **Organization context** — You work inside a selected **organization** (tenant). Data is never shared across organizations.
- **Permissions** — Menus may appear with broad **HR view**; sensitive actions (loan management, payroll confirm) require appropriate **RBAC** permissions. If an action fails with **403 Forbidden**, your role needs the corresponding `hr_loans_*` or payroll permission — contact your administrator.
- **Employee ↔ user link** — **My loans** and **loan notifications** require the employee record to be linked to a **user account** (`user id`).

---

## 3. Navigating the HR module (frontend)

The main HR area is reached from the app menu (**HR**). Common routes:

| Route | Purpose |
|-------|---------|
| `/hr/dashboard` | HR dashboard |
| `/hr/employees` | Employee directory and profiles |
| `/hr/loans` | **Loan register**, filters, reports, accounting export, bulk holiday recalc (AD-03), COA (PI-05) |
| `/hr/loans/applications` | Loan **application** list |
| `/hr/loans/applications/new` | Create application |
| `/hr/loans/applications/:applicationId` | Application detail — submit, approve, reject, clarify, delegate |
| `/hr/loans/settings` | **Organization loan policy** (caps, tenure, salary-advance shortcuts, etc.) |
| `/hr/loans/org-audit` | **Org-level loan audit** (COA replace, bulk AD-03 summary) |
| `/hr/loans/payroll-recoveries` | Payroll **reversal** alerts, **cross-check** vs payslip lines |
| `/hr/loans/:loanId` | **Loan detail** — installments, repayments (including reversals), combined audit |
| `/hr/my-loans` | **Employee** — own loans + notification feed |
| `/hr/payroll`, `/hr/payroll-runs` | Payroll operations |
| `/hr/salary` | Salary management |
| `/hr/provident-fund/*` | EPF — see EPF user manual |
| `/hr/provident-fund/organization-policy` | **EPF organization policy** — employee/employer rates, PF wage ceiling/floor, employment eligibility |

> **Tip:** Register route **`/hr/loans/org-audit`** is defined **above** `/hr/loans/:loanId` so the path `org-audit` is not mistaken for a loan UUID.

---

## 4. Employee loans — overview

### 4.1 Concepts

- **Loan category** — Defines type (e.g. term loan vs **salary advance**), limits, and **interest method** (none, flat, reducing balance). Categories are maintained per organization.
- **Application** — Employee (or HR on behalf) submits a request; **workflow** may include HR and Finance steps with clarifications and delegation.
- **Loan account** — After approval, a **loan account** tracks principal, **installment schedule**, repayments, and status (e.g. active, settlement pending, closed).
- **Repayment** — Manual payments, **payroll deductions**, or **exit settlement** (PF, final salary, other dues) per policy.

### 4.2 Interest and schedules (LC-04)

At **disbursement**, the system builds an installment schedule from the category’s **interest method**:

| Method | Behavior (simplified) |
|--------|------------------------|
| **NONE** | Equal **principal** per month; no interest component. |
| **FLAT** | Interest on the **full principal** for the loan tenure; total repayment is split into **equal** installment amounts. |
| **REDUCING_BALANCE** | **EMI-style** amortization using the annual rate on the category. |

Holiday/weekend **shifting** (AD-03) is controlled by **organization loan settings** and applies when building or **recalculating** due dates.

### 4.3 Standard HR workflows (summary)

1. **Configure** categories and org settings (`/hr/loans/settings`).
2. **Apply** — Employee or HR creates an application; approvers act on the workflow.
3. **Disburse** — Finance/HR records disbursement on the loan account; schedule is generated.
4. **Collect** — Repayments via manual entry, payroll, or settlement.
5. **Review** — Use register reports, **loan audit** on the account, and **org audit** for bulk/COA events.

**Detailed procedures:** [topics/employee-loans-detailed.md](topics/employee-loans-detailed.md).

---

## 5. Payroll and loan recovery

Loan recoveries feed into payroll as **deductions** against scheduled installments (subject to payroll configuration — see salary manual for **LOAN_REPAYMENT** components).

**Time and attendance (LOP, overtime):** When daily attendance or approved timesheets exist for the pay period, **Populate from Salary** can roll up working days, leave, LOP, and overtime into each employee’s payroll row. See the salary manual ([HR-SALARY-USER-MANUAL.md](../../HR-SALARY-USER-MANUAL.md)), section **4.2b**.

- **Preview** recoveries for a period / run before finalizing payroll.
- **Confirm** posting for a payroll run — operation is **idempotent per loan and run** (safe to retry).
- **Anomalies** — Review **reversal** events and **cross-check** payslip vs loan postings when something looks wrong.

**Detailed procedures:** [topics/payroll-loan-recovery.md](topics/payroll-loan-recovery.md).

**Requirements mapping (INT-35–INT-39):** See [INTEGRATION_LOANS_PAYROLL_INT-35-39.md](../../INTEGRATION_LOANS_PAYROLL_INT-35-39.md) in `hr-service` for how loan recovery, preview/confirm, LOP/OT, and stop-recovery behavior are implemented in the API.

---

## 6. Employee self-service

- **My loans** (`/hr/my-loans`) — Lists the logged-in employee’s loans (when employee ↔ user mapping exists) and shows **in-app notifications** for application events, disbursement, settlement required, and **payment-due reminders** (also driven by a scheduled job — see topic article).
- **Historical payslips / salary** — Covered in the salary manual where applicable.

**Detailed procedures:** [topics/employee-self-service-hr.md](topics/employee-self-service-hr.md).

---

## 7. Audit and compliance

- **Loan account audit** — Timeline of disbursements, schedule changes, repayments, payroll reversals, skips, settlement, etc. (loan detail).
- **Combined audit** — Includes **application workflow** actions when the loan was created from an application.
- **Organization loan audit** — COA mapping replacements and **bulk holiday recalculation** summaries (not tied to a single loan id).

Emails/SMS are **not** part of the in-app notification feed; outbound channels depend on future platform integration.

---

## 8. Troubleshooting

| Symptom | What to check |
|---------|----------------|
| **403** on loan APIs | RBAC: user needs `hr_loans_view` / `hr_loans_manage` (or legacy `hr` + `manage`) as appropriate. |
| **My loans** empty | Employee must be linked to the logged-in **user**; correct **organization** selected. |
| **Bulk AD-03** shows failures | Open the **failure table** on the register after bulk run; follow links to loan detail. |
| **Payroll recovery** duplicate concern | Confirm uses **idempotent** keys; use **anomalies** and **cross-check** screens. |
| **Settlement PF hint** missing | EPF account / balance may be absent — see EPF manual; amounts can be entered per settlement policy. |

---

## 9. Related manuals and requirements

| Document | Location |
|----------|----------|
| Salary user manual | [../../HR-SALARY-USER-MANUAL.md](../../HR-SALARY-USER-MANUAL.md) |
| EPF user manual | [../../HR-EPF-USER-MANUAL.md](../../HR-EPF-USER-MANUAL.md) |
| Loans ↔ payroll (INT-35–INT-39) | [../INTEGRATION_LOANS_PAYROLL_INT-35-39.md](../INTEGRATION_LOANS_PAYROLL_INT-35-39.md) |
| Integration patterns (INT-40–INT-43) | [../INTEGRATION_INT-40-43.md](../INTEGRATION_INT-40-43.md) |
| Business requirements (loans) | Repository `requirements/Module-HR/employee_loans_requirements.md` |

---

## 10. Glossary

| Term | Meaning |
|------|---------|
| **AD-03** | Optional holiday/weekend adjustment of installment **due dates**. |
| **COA / PI-05** | Chart of accounts **mapping hints** for accounting **export** (not automatic GL posting). |
| **RP-05** | Controlled **payroll reversal** of loan postings, with audit trail. |
| **RE-02 / RE-03** | Employee self-service view and **in-app notifications** for loans. |
| **ST-01 … ST-05** | Exit **settlement** lifecycle (start, allocate, shortfall, close, etc.). |

---

*End of master user manual. For topic deep-dives, see [README.md](README.md).*
