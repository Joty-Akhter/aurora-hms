# HR Employee Loans and Salary Advance

## Overview

This document specifies requirements for **employee loans** and **salary advance** in EasyOps HR: eligibility, loan categories, limits, lifecycle (application through closure), repayment—including partial payments and payroll integration—independence from Provident Fund (PF) for eligibility and loan amount calculation, and **settlement of outstanding balances** when an employee resigns (including adjustment against PF settlement, salary, or other dues).

### Scope

- **Loan master and categories**: Configurable loan types (e.g., Emergency Loan, Staff Loan, Motorcycle Loan) and a distinct **Salary Advance** path where business rules differ.
- **Eligibility rules**: Minimum employment tenure, active employment status, and policy constraints (e.g., single active loan).
- **Loan application and approval**: Workflow, limits, disbursement tracking, and contractual terms (principal, interest if any, tenure, installment).
- **Repayment**: Scheduled deductions, partial prepayments, combining installments, balance tracking.
- **Payroll integration**: Feeding approved installment amounts into payroll as loan recovery; blocking or adjusting recovery on termination.
- **Resignation and settlement**: Outstanding loan recovery from PF settlement, final salary, or other dues per policy.

### Key Objectives

- **Policy-driven lending**: Centralized rules (tenure, caps, one-loan-at-a-time) with auditable overrides where authorized.
- **Accurate balances**: Principal and outstanding balance visible to HR, finance, and (where enabled) the employee.
- **Seamless payroll**: Loan recovery as first-class input to payroll runs, aligned with [salary_management_requirements.md](salary_management_requirements.md) (e.g., loan repayment components).
- **Clear exit handling**: No ambiguity on how remaining debt is collected at separation.

### Out of Scope (covered elsewhere)

- **PF contribution rules and PF account ledger**: [payroll_benefits_administration.md](payroll_benefits_administration.md) — this module only **consumes** PF settlement totals or approved adjustment amounts for loan recovery at exit.
- **General payroll calculation engine**: Payroll run lifecycle and payslip layout — see payroll and salary management docs; loans supply **recovery amounts** and **status**.
- **External banking / disbursement file formats**: May be added later; initial scope assumes disbursement is recorded in-system and reconciled manually or via accounting integration.

---

## Table of Contents

1. [Stakeholder Business Rules (Authoritative)](#1-stakeholder-business-rules-authoritative)
2. [Loan Categories and Salary Advance](#2-loan-categories-and-salary-advance)
3. [Eligibility, Limits, and Constraints](#3-eligibility-limits-and-constraints)
4. [Loan Application, Approval, and Disbursement](#4-loan-application-approval-and-disbursement)
5. [Repayment and Balance Management](#5-repayment-and-balance-management)
6. [Payroll and Accounting Integration](#6-payroll-and-accounting-integration)
7. [Resignation, Termination, and Settlement](#7-resignation-termination-and-settlement)
8. [Administration, Configuration, and Overrides](#8-administration-configuration-and-overrides)
9. [Reporting, Notifications, and Audit](#9-reporting-notifications-and-audit)
10. [Non-Functional Requirements](#10-non-functional-requirements)
11. [API and UI Summary](#11-api-and-ui-summary)
12. [Implementation Plan (Phased)](#12-implementation-plan-phased)
13. [Implementation Status](#13-implementation-status)

---

## 1. Stakeholder Business Rules (Authoritative)

The following rules reflect agreed company policy and **shall** be enforceable via configuration (not hard-coded literals in application code, except safe defaults).

| Topic | Policy | Requirement ID |
|-------|--------|------------------|
| Minimum employment for loan eligibility | An employee must have worked for the company for **at least 6 months** before being eligible for a loan. | BR-01 |
| Multiple concurrent loans | An employee **cannot** have **more than one** active loan at the same time. (Salary advance rules may use the same or a stricter rule—see [Section 2](#2-loan-categories-and-salary-advance).) | BR-02 |
| Maximum loan amount | The maximum loan principal is **150,000 BDT** by default; the organization **shall** be able to **adjust** this cap (globally or per loan category) without code changes. | BR-03 |
| Partial payment | **Partial payments** toward the outstanding balance **are allowed**. An employee may also pay **two installments together** in a single payment period if desired. | BR-04 |
| Loan amount vs. PF balance | The **approved loan amount does not depend** on the employee’s **PF balance**. | BR-05 |
| Loan eligibility vs. PF balance | **Eligibility** for a loan **is not** dependent on the employee’s **PF balance**. | BR-06 |
| Loan categories | Multiple **loan categories** are required, including at minimum: **Emergency Loan**, **Staff Loan**, and **Motorcycle Loan** (configurable names/codes). | BR-07 |
| Salary advance | The system **shall** support a distinct **Salary Advance** offering (separate workflow or flags from term loans), subject to the same global constraints unless policy explicitly differs. | BR-08 |
| Resignation settlement | If an employee **resigns**, the **remaining loan** may be **adjusted** from **PF settlement**, **salary** (e.g., final pay), or **any other dues** (as defined in settlement workflow). | BR-09 |

---

## 2. Loan Categories and Salary Advance

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| LC-01 | The system shall maintain a **loan category** catalog per organization: **code**, **name**, **description**, **active** flag, **sort order**, and optional **category-specific** max principal and max tenure. | Must |
| LC-02 | Default categories shall be seedable or documentable as: **Emergency Loan**, **Staff Loan**, **Motorcycle Loan** (labels may be localized). | Must |
| LC-03 | **Salary Advance** shall be modeled as either: (a) a dedicated category with `type = SALARY_ADVANCE`, or (b) a separate entity linked to payroll periods—implementation choice—but **UI and reports** must clearly separate **term loans** from **salary advance**. | Must |
| LC-04 | Each category may define whether **interest** applies (none, flat, reducing balance) for future use; **initial release** may support **zero interest** with configurable text on the schedule. | Should |
| LC-05 | Category-level rules shall respect [BR-02](#1-stakeholder-business-rules-authoritative): if the global rule is “one active loan,” the system shall block a second facility regardless of category unless an authorized override is recorded. | Must |

---

## 3. Eligibility, Limits, and Constraints

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| EL-01 | On loan application, the system shall validate **continuous or recognized service** of **≥ 6 months** from **hire date** (or confirmed start per HR policy) per [BR-01](#1-stakeholder-business-rules-authoritative). | Must |
| EL-02 | The employee shall be **active** and not on **long-term suspension** disqualifying loans (configurable list of employment statuses). | Must |
| EL-03 | The system shall **not** use **PF balance** as an eligibility gate per [BR-06](#1-stakeholder-business-rules-authoritative). | Must |
| EL-04 | Requested principal shall not exceed the **configured maximum** (default **150,000 BDT** or category cap) per [BR-03](#1-stakeholder-business-rules-authoritative). | Must |
| EL-05 | The system shall enforce **at most one active loan** per employee per [BR-02](#1-stakeholder-business-rules-authoritative): statuses counted as “active” include *approved pending disbursement*, *disbursed recovering*, and *in arrears* (configurable). | Must |
| EL-06 | **Salary advance** may share the same “single facility” rule or a distinct rule (e.g., advance allowed only if no term loan); policy shall be **configurable**. | Should |

---

## 4. Loan Application, Approval, and Disbursement

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| AL-01 | Users shall create a **loan application** with: employee, category, requested amount, requested tenure (months), purpose/notes, application date, and attachment references (optional). | Must |
| AL-02 | The system shall compute **recommended installment** from principal ÷ tenure (or applying interest rules when enabled) and show **total scheduled recovery** for acknowledgment. | Must |
| AL-03 | **Approval workflow** shall support configurable steps (e.g., HR → Finance) with **approve**, **reject**, **request clarification**, and **delegation**; all actions audit-logged. | Must |
| AL-04 | Upon **approval**, the system shall generate a **loan account** with: principal, currency (BDT), start date, installment amount, remaining balance = principal, status = **ACTIVE** or **PENDING_DISBURSEMENT** per process. | Must |
| AL-05 | **Disbursement** shall be recorded with: date, amount (≤ approved principal), method/reference; partial disbursement shall be supported if policy allows, with principal adjusted accordingly. | Should |
| AL-06 | The approved principal shall **not** be derived from PF balance per [BR-05](#1-stakeholder-business-rules-authoritative). | Must |

---

## 5. Repayment and Balance Management

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| RP-01 | The system shall maintain **outstanding balance** and **schedule** of installments (due date, amount, status: due/paid/partial/skipped-with-reason). | Must |
| RP-02 | **Partial payments** shall reduce outstanding balance and shall be allocated per policy (e.g., principal first, or fees first if added later) per [BR-04](#1-stakeholder-business-rules-authoritative). | Must |
| RP-03 | The employee may remit an amount covering **two installments** in one period; the system shall mark the corresponding periods **paid** and adjust the schedule forward per [BR-04](#1-stakeholder-business-rules-authoritative). | Must |
| RP-04 | Payroll **deduction** for a period shall post as a **repayment transaction** linked to the loan account and reduce balance. | Must |
| RP-05 | Overpayment or duplicate payroll recovery shall be **detectable** (alerts) and **reversible** via controlled adjustment with audit trail. | Should |
| RP-06 | When balance reaches **zero**, loan status shall become **CLOSED** and the employee becomes eligible for a new loan subject to [EL-05](#3-eligibility-limits-and-constraints). | Must |

---

## 6. Payroll and Accounting Integration

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| PI-01 | Approved **monthly recovery** shall feed the payroll engine as a **deduction** line (aligned with salary component category **Loan Repayment** / recovery components in salary management). | Must |
| PI-02 | Payroll shall receive **per-period recovery amount** (may vary if partial prepayment or combined installments were applied outside payroll). | Must |
| PI-03 | If payroll is **not run** for a period, loan schedule shall show **arrears** and support **catch-up** in a later period without double-charging if already paid manually. | Must |
| PI-04 | Integration shall be **idempotent** for the same payroll period (replay-safe). | Must |
| PI-05 | Accounting (optional phase): post loan liability/receivable movements per chart of accounts; minimum requirement is **export or journal suggestion** for finance. | Could |

---

## 7. Resignation, Termination, and Settlement

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| ST-01 | On **resignation or termination** with an **outstanding balance**, the system shall flag the loan for **settlement** and block new applications until resolved (subject to override). | Must |
| ST-02 | Settlement shall support **recovery from PF settlement** per [BR-09](#1-stakeholder-business-rules-authoritative): interface with PF exit/settlement module to receive **available settlement amount** and allocate **up to** outstanding loan (with employee consent where legally required). | Must |
| ST-03 | Remaining balance after PF allocation may be recovered from **final salary** and **other dues** (bonus, leave encashment, etc.) per configurable priority order. | Must |
| ST-04 | If dues are insufficient, the system shall record **remaining debt** and **write-off / legal** path (configurable workflow) with full audit. | Should |
| ST-05 | Full and partial settlement transactions shall **close** or **reduce** the loan per [RP-06](#5-repayment-and-balance-management). | Must |

---

## 8. Administration, Configuration, and Overrides

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| AD-01 | Configurable parameters: **minimum tenure months** (default 6), **global max principal** (default 150,000 BDT), **currency**, **single active loan** enforcement, **salary advance** rules. | Must |
| AD-02 | **Authorized roles** may override limits with **reason**, **approver**, and **expiry** (optional) for exceptional approvals. | Should |
| AD-03 | **Holiday calendar** may shift installment due dates (optional). | Could |

---

## 9. Reporting, Notifications, and Audit

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| RE-01 | Reports: **active loans**, **closed loans**, **arrears**, **loan register by employee**, **by category**, **settlement at exit**. | Must |
| RE-02 | Employee self-service (phase 2): view **balance** and **schedule** (optional per org). | Should |
| RE-03 | Notifications: application submitted, approved/rejected, disbursement, payment due reminder, settlement required on resignation. | Should |
| RE-04 | **Audit trail** for all changes to principal, schedule, manual adjustments, and settlement allocations. | Must |

---

## 10. Non-Functional Requirements

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| NF-01 | All operations shall be **organization-scoped** (multi-tenant). | Must |
| NF-02 | Performance: loan list and detail views shall load within agreed HR module SLAs (&lt; 2 s typical). | Should |
| NF-02 (implementation) | **No automated performance or SLA tests** for loan list/detail are checked into this repository; the &lt;2s target is validated manually or via future tooling (e.g. k6, Playwright performance budgets, or CI smoke with thresholds). | Info |
| NF-03 | **Role-based access**: e.g., employee (apply/view own), HR (full), Finance (approve/settle), Payroll (read recovery). | Must |
| NF-03 (implementation) | **Granular permissions** `hr_loans_*` are defined in RBAC seed and enforced in `LoanRbacService` (with fallback to legacy `hr` + `view` / `manage` where appropriate). **Production** must assign these permissions to roles; the **SPA** nav still uses coarse `hr` + `view` for menu visibility—fine-grained denial is enforced by the API (403). | Info |

---

## 11. API and UI Summary

| Area | Capabilities |
|------|----------------|
| **Master data** | CRUD loan categories; org-level settings (caps, tenure, BR flags). |
| **Applications** | Create/list/filter applications; submit for approval; approve/reject. |
| **Loan accounts** | Get loan by id; list by employee; balance and schedule; record manual/partial payment. |
| **Payroll** | GET recovery due for period/employee; POST confirmation after payroll; arrears sync. |
| **Settlement** | Initiate exit settlement; allocate PF/salary/dues; close loan. |

**Implementation note:** As-built API prefixes, UI routes, phase completion, and known gaps are tracked in [Section 13](#13-implementation-status).

---

## 12. Implementation Plan (Phased)

This plan is written so each **phase** is a single, bounded slice of work: complete it, run checks, merge, then start the next. When using Cursor, paste **one phase block** as the task scope (plus “follow existing `hr-service` patterns and Liquibase conventions”).

### How to use with Cursor

1. Open `easyops-erp/services/hr-service` (and `database-versioning` for migrations).
2. Implement **only the current phase**; avoid starting the next phase in the same change set.
3. After each phase: run unit tests for `hr-service`, fix Liquibase checksum issues if any, and smoke-test APIs (or minimal UI if included in that phase).
4. Mark requirement IDs from the tables above as satisfied in `salary_management_implementation_status.md` or a dedicated loans checklist when you add one.

### Dependency overview

```text
Phase 1 (foundation)
    → Phase 2 (applications)
        → Phase 3 (loan account + schedule + manual repayment)
            → Phase 4 (payroll integration)     ─┐
            → Phase 5 (settlement / exit)        ├── can run in parallel after Phase 3 if teams split
Phase 6 (reporting + RBAC + polish)  ←────────┘  (after 4 & 5, or after 4 if 5 deferred)
```

---

### Phase 1 — Foundation: data model, org settings, categories

**Goal:** Persist loans domain in PostgreSQL and expose read/write APIs for configuration and categories—no full application workflow yet.

| Item | Detail |
|------|--------|
| **Delivers** | Liquibase changelog(s) for: `loan_organization_settings` (or equivalent), `loan_category` (with `category_type`: `TERM_LOAN` \| `SALARY_ADVANCE`), indexes, FK to `organization`. Seed or migration data for Emergency / Staff / Motorcycle + Salary Advance. |
| **Backend** | JPA entities, repositories, DTOs, REST controllers under `/api/hr/...` (match existing HR URL style). CRUD categories; GET/PATCH org loan settings (min tenure months default 6, max principal default 150000 BDT, currency, enforce single active loan, salary-advance policy flags). |
| **Validates** | AD-01, LC-01–LC-02, NF-01 (org scope on all queries). |
| **Tests** | Repository or slice tests for settings + categories; API tests for create/list/update. |
| **DoD** | Migrations apply on clean DB; default categories exist; settings readable per org. |

**Cursor task hint:** “Implement Phase 1 of `employee_loans_requirements.md`: Liquibase + entities + REST for loan categories and org loan settings only.”

---

### Phase 2 — Loan applications and approval (in-system)

**Goal:** Employees (or HR on behalf) can submit applications; approvers can approve/reject; eligibility rules enforced before submit.

| Item | Detail |
|------|--------|
| **Delivers** | Tables: `loan_application` (status: DRAFT, SUBMITTED, APPROVED, REJECTED, …), optional `loan_application_action` / audit rows. Service methods: validate EL-01–EL-05 (hire date, active status, max amount, single active loan). Simple linear workflow (e.g., SUBMITTED → APPROVED/REJECTED) with assigned approver role; v1 can be single-step approval if configurable hooks are left for later. |
| **Backend** | Endpoints: create/update application, submit, approve, reject, list/filter by org/employee/status. |
| **Validates** | AL-01, AL-03 (minimal), EL-01–EL-05, BR-01–BR-03, BR-06–BR-07. |
| **Frontend (optional in phase)** | Minimal HR page: application form + inbox for approvals—or API-only if UI is Phase 6. |
| **DoD** | Cannot submit when ineligible; cannot approve second active facility without override stub (AD-02 optional in later phase). |

**Cursor task hint:** “Implement Phase 2: loan application entity, eligibility validation, submit/approve/reject APIs per `employee_loans_requirements.md`; do not build payroll integration yet.”

---

### Phase 3 — Loan account, disbursement, schedule, manual repayments

**Goal:** On approval, create a **loan account** with principal, generate installment schedule (equal principal per month for v1 zero-interest), record disbursement, and support partial payments and “two installments in one period” via manual repayment API.

| Item | Detail |
|------|--------|
| **Delivers** | Tables: `employee_loan` (or `loan_account`), `loan_installment`, `loan_repayment_transaction` (amount, date, source: MANUAL, PAYROLL later). Generate schedule on approval/disbursement (AL-04). Record disbursement (AL-05 optional partial). |
| **Backend** | Approve flow creates loan + schedule; endpoints to record manual repayment allocating to installments (RP-02, RP-03); recompute outstanding balance; transition to CLOSED when zero (RP-06). |
| **Validates** | AL-02, AL-04–AL-06, RP-01–RP-03, RP-06; BR-04–BR-05. |
| **DoD** | Loan detail returns schedule + balance; manual payment reduces balance and marks installments; arrears visible if installment unpaid (prepare for PI-03). |

**Cursor task hint:** “Implement Phase 3: loan account + installment schedule generation + manual repayment transactions; still no payroll pull.”

---

### Phase 4 — Payroll integration

**Goal:** Payroll run can pull **recovery due** per employee/period and **post confirmation** idempotently so deductions become repayment transactions (RP-04, PI-01–PI-04).

| Item | Detail |
|------|--------|
| **Delivers** | Service: `getRecoveriesForPayroll(orgId, periodStart, periodEnd)` returning amounts per active loan; `confirmPayrollDeductions` with idempotency key `(loanId, payrollRunId)` or `(loanId, period)` to prevent double post. Integration with existing payroll/salary component type **Loan Repayment** where applicable. |
| **Backend** | REST (or internal Feign contracts) aligned with `PayrollCalculationService` / payroll run lifecycle in this repo—**inspect existing payroll APIs first** and add adapters rather than duplicating payroll logic inside loan service. |
| **Validates** | RP-04, PI-01–PI-04; INT-35–INT-39 directionally (document mapping in implementation status). |
| **DoD** | Re-running confirmation for same period does not double-charge; catch-up recovery supported without duplicate if manual payment already applied (PI-03). |

**Cursor task hint:** “Implement Phase 4: integrate employee loans recovery with existing payroll run in `hr-service` (and gateway routes if needed); idempotent confirmation.”

---

### Phase 5 — Resignation and settlement

**Goal:** Flag loans on separation; record settlement allocations from PF, final salary, and other dues (manual entry of amounts v1 if PF module not ready).

| Item | Detail |
|------|--------|
| **Delivers** | Loan status `SETTLEMENT_PENDING`; settlement record(s): source (PF_SETTLEMENT, FINAL_SALARY, OTHER_DUES), amount, date, reference. Block new applications until cleared (ST-01). |
| **Backend** | Endpoints: start settlement, allocate lines, close loan (ST-02–ST-05). If PF API exists, add client stub; else document **manual amount** entry with audit. |
| **Validates** | ST-01–ST-05, BR-09. |
| **DoD** | Outstanding balance can be cleared via combined allocations; shortfall recorded for ST-04 if implemented. |

**Cursor task hint:** “Implement Phase 5: exit settlement workflow for employee loans; integrate PF settlement amounts when available, else manual allocation with audit.”

---

### Phase 6 — Reporting, RBAC, notifications, optional ESS

**Goal:** Operational visibility and policy compliance; polish for production.

| Item | Detail |
|------|--------|
| **Delivers** | Reports or export endpoints: active loans, arrears, register by employee/category, settlement at exit (RE-01). RBAC on all endpoints (NF-03). Audit log on principal/schedule/adjustments (RE-04). Optional: email/in-app notifications (RE-03). Optional: employee self-service view of balance/schedule (RE-02). |
| **Frontend** | HR pages: loan register, loan detail, application list; optional employee portal slice. |
| **Validates** | RE-01, RE-03–RE-04, NF-02–NF-03; AD-02 if not done earlier. |
| **DoD** | Role matrix documented; critical paths audited; reports match loan tables. |

**Cursor task hint:** “Implement Phase 6: loans reporting APIs + RBAC + audit; add minimal HR UI lists/detail if missing.”

---

### Phase 7 (optional) — Accounting export

**Goal:** PI-05 — journal suggestions or CSV export for finance.

| Item | Detail |
|------|--------|
| **Delivers** | Export of disbursements/repayments by period; optional COA mapping table. |
| **DoD** | Finance can reconcile without direct DB access. |

---

## 13. Implementation Status

This section records **as-implemented** behavior in the EasyOps codebase (`easyops-erp`) as of the last review. It is the single place to see **done vs. partial vs. not started** against the phased plan in [Section 12](#12-implementation-plan-phased) and the requirement tables above.

### 13.1 Phase summary

| Phase | Theme | Status | Notes |
|-------|--------|--------|--------|
| **1** | Schema, org settings, categories | **Done** | Liquibase `086-hr-loan-categories-and-settings.sql`; REST under `/api/hr/loans` (settings, categories). |
| **2** | Applications & approval | **Done** | `087` + follow-on migrations; `LoanApplicationController` — create/update/submit; **multi-step** HR → Finance (or single-step / salary-advance shortcut); clarify & delegate; **AD-02** overrides; **AL-02** preview on `LoanApplicationDto`. |
| **3** | Loan account, disbursement, schedule, manual repayments | **Done** | `088`; `094` (RP-01 **SKIPPED** + `skip_reason`); equal-principal schedule; manual repayments; **installment skip**; partial disbursement. |
| **4** | Payroll integration | **Done** | `089`; `LoanPayrollRecoveryController` (preview + idempotent `confirm` + **anomalies**); **RP-05** payroll **reversal** + audit. |
| **5** | Resignation / settlement | **Done** | `090`; `093` write-off metadata; settlement + PF hint; ST-03 priority. |
| **6** | Reporting, RBAC, audit, HR UI | **Done (core)** | `091`; `LoanAuditController` including **`GET /audit/combined`**; HR UI: `LoanRegister`, `LoanDetail`, `LoanApplications`. |
| **7** | Accounting export (PI-05) | **Done** | `GET .../loans/reports/accounting-export` (JSON); CSV disbursements/repayments by period; suggested debit/credit labels; optional **COA mapping** (`GET/PUT .../accounting-coa-mappings`); UI on loan register. Automated GL posting remains future work. |

**Liquibase:** changelogs **086–096** are included from `easyops-erp/database-versioning/changelog/master-changelog.xml` (096 adds AD-03 holiday settings + PI-05 COA table).

**RBAC:** **`LoanRbacService`** with granular `hr_loans` actions (`014-hr-loans-rbac-permissions.sql`) and **fallback** to legacy `hr` + `view` / `manage`. `HrRbacService` is only used for delegation edge cases.

### 13.2 Requirement coverage (high level)

Some rows in this table are **updated** when loan-module work lands (e.g. LC-04, RE-02, RE-03). **Other rows are left as-is** even when their “Gaps / follow-ups” text still reads like an open list—**ST-02–ST-04**, **NF-02**, and similar refer to **broader HR / platform** scope (settlement automation, legal workflow, performance testing), not missing deliverables for a specific AD-03 or PI-05 change set. Treat those as **product backlog**, not as “still broken relative to holiday/COA features.”

| Area | Mostly satisfied | Gaps / follow-ups |
|------|------------------|-------------------|
| **BR / EL / LC** | Tenure, caps, categories, PF independence, **BR-08** salary-advance shortcut; **LC-04** interest method + rate on category; **disbursement** builds schedules: **NONE** (equal principal), **FLAT** (interest on full principal for tenure, equal combined payments), **REDUCING_BALANCE** (EMI amortization) | Fine-tuning (e.g. day-count conventions, regulatory rounding rules) may still need product sign-off. |
| **AL** | AL-01–AL-06; **AL-02** DTO preview fields; **AL-03** multi-step + clarify + delegate; **HR UI** `/hr/loans/applications/...` with overrides, expiries, **attachment references** (text/URLs) | **Gap:** optional **integrated file upload** to object storage (vs. pasting references manually). |
| **RP** | Partial payments, payroll, **RP-01** skip, **RP-05** reversal + idempotency | **HR UI:** payroll anomalies + cross-check (`/hr/loans/payroll-recoveries`); reversal from loan detail. Proactive duplicate detection vs. payroll engine (beyond reversals list) remains optional. |
| **PI** | PI-01–PI-04, PI-05 | General payroll accounting export separate from loan export. Optional COA mapping for export columns; full automated GL posting not in scope for “Could” minimum. |
| **ST** | ST-01, ST-05; allocations; shortfall; write-off fields; **ST-02** PF hint via **`ProvidentFundPfSettlementClient`** (EPF account balance) when linked | **ST-02–ST-03** deeper PF exit/settlement **API** integration if totals must come from a separate settlement run; **ST-04** legal task workflow. |
| **AD** | AD-01, **AD-02**, **AD-03** (optional weekend/holiday shift + recalc) | — |
| **RE** | RE-01; **RE-04** loan audit + **`/combined`** + org audit **`/audit/org`**; **RE-02** ESS **`/hr/my-loans`**; **RE-03** in-app feed: applications, **disbursement**, **settlement required**, **payment due soon** (daily scheduler + `hr.loans.reminder.*`) | **RE-03 gap:** **email/SMS** push (in-app only). |
| **NF** | NF-01; **NF-03** `hr_loans_*` + fallback | Per-org **role → permission** assignment in production; see NF-03 row in [Section 10](#10-non-functional-requirements). NF-02: no repo perf tests yet (see NF-02 implementation row). |

### 13.3 Audit model (RE-04)

- **`loan_audit_log`** — `GET /api/hr/loans/audit` — disbursement, schedule, repayments, settlement, shortfall, write-off, payroll reversal, installment skip, AD-03 per-loan recalc, PI-05 COA replace, AD-03 **bulk** org summary (`ENTITY_LOAN_ORG` / `BULK_HOLIDAY_RECALC_COMPLETED`).
- **`loan_application_action`** — `GET /api/hr/loans/applications/{id}/actions` — workflow (submit, approvals, clarify, delegate).
- **Combined** — `GET /api/hr/loans/audit/combined?organizationId=&loanId=` — both when the loan has `loan_application_id`.
- **Org-level** — `GET /api/hr/loans/audit/org?organizationId=` — `ENTITY_LOAN_ACCOUNTING_COA` (PI-05 COA replace) and `ENTITY_LOAN_ORG` (AD-03 bulk summary); not loan-scoped.

### 13.4 Key API prefixes (reference)

| Prefix | Purpose |
|--------|---------|
| `/api/hr/loans` | Org loan settings, categories (`LoanConfigurationController`). |
| `/api/hr/loans/applications` | Applications (`LoanApplicationController`). |
| `/api/hr/loans/accounts` | Accounts, disbursement, repayments, **installment skip**, **payroll reversal**, AD-03 per-loan + **bulk** `POST .../recalculate-holiday-dates/all` (`EmployeeLoanController`). |
| `/api/hr/loans/payroll-recoveries` | Preview, confirm, **anomalies** (`LoanPayrollRecoveryController`). |
| `/api/hr/loans/reports` | Reports + CSV + accounting export (`LoanReportingController`). |
| `/api/hr/loans/audit` | Loan audit; **`/combined`**; **`/org`** (org-level COA + bulk AD-03) (`LoanAuditController`). |
| `/api/hr/loans/.../settlement/...` | Settlement (`LoanSettlementController`). |

### 13.5 AD-03 semantics (holiday / weekend shift)

| Topic | Behavior |
|-------|----------|
| **Recalculate (per-loan or bulk)** | Adjusts only installments that are **not** `SKIPPED` and **not fully paid** (`paid_amount >= scheduled_amount`). **Partial** payments still get a due-date recalc (unpaid remainder remains on the same installment row). |
| **Skipped installments** | Rows with status `SKIPPED` (RP-01) are left unchanged so skip approvals stay stable. |
| **Dense calendar** | If sliding cannot reach a business day within **62** day-steps, the API returns **422** (`UNPROCESSABLE_ENTITY`) with an explanatory message (schedule build and recalc). |
| **Bulk** | `POST /api/hr/loans/accounts/recalculate-holiday-dates/all` processes **ACTIVE** and **SETTLEMENT_PENDING** loans that have a **disbursement date**; each loan in its own transaction; failures listed in the response; one org-level audit summary row. |
| **Shifting off** (`shiftInstallmentDueDatesForHolidays = false`) | Recalculate still runs for eligible rows and sets each due date from **disbursement date + sequence months** (nominal calendar monthiversary), via the same adjuster with shifting disabled—effectively **re-baselining** the schedule to that nominal curve. **Acceptable** while there is **no manual per-installment due-date editing**. |
| **Future (if manual due dates exist)** | If operators can edit individual installment due dates, AD-03 recalc may need a **different rule** (e.g., skip rows that were manually adjusted) or an explicit **flag** on installments—otherwise “recalculate” would overwrite those edits. Low priority until that feature exists. |

### 13.6 Frontend routes

| Route | Page |
|-------|------|
| `/hr/loans` | Loan register, reports, accounting export UI (incl. AD-03 bulk recalc + PI-05 COA; COA load is non-blocking) |
| `/hr/my-loans` | RE-02 ESS: own loans + RE-03 notification feed |
| `/hr/loans/settings` | Organization loan policy (incl. **salaryAdvanceSkipFinanceApproval**, skip finance, caps) |
| `/hr/loans/org-audit` | Org-level loan audit (PI-05 COA replace, AD-03 bulk summary) |
| `/hr/loans/payroll-recoveries` | RP-05 reversal alerts + payslip vs loan cross-check by payroll run |
| `/hr/loans/applications` | Application list (AL-02 columns) |
| `/hr/loans/applications/new` | New application (create draft) |
| `/hr/loans/applications/:applicationId` | Application detail: edit/submit/approve/reject/clarify/delegate; override expiries on approve |
| `/hr/loans/:loanId` | Loan detail (installments, repayments with reversal labels; **combined** audit) |

---

## Related Documentation

- [salary_management_requirements.md](salary_management_requirements.md) — salary components including loan repayment categorization.
- [payroll_benefits_administration.md](payroll_benefits_administration.md) — payroll runs and PF settlement.
- [salary_management_implementation_status.md](salary_management_implementation_status.md) — track implementation of loan integration items (e.g., INT-35–INT-39).

---

## Document Control

| Version | Date | Notes |
|---------|------|--------|
| 1.0 | 2026-04-02 | Initial requirements: standard features + stakeholder business rules |
| 1.1 | 2026-04-02 | Added phased implementation plan (Section 12) |
| 1.2 | 2026-04-02 | Added Section 13 (implementation status): phase table, requirement gaps, API/route reference |
| 1.3 | 2026-04-02 | Section 13 refreshed: multi-step approval, `LoanRbacService`, AD-02, BR-08, payroll reversal/anomalies, AL-02 DTO fields, RP-01 skip, RE-04 combined audit; Liquibase 086–094 |
| 1.4 | 2026-04-02 | HR UI: full application workflow, loan org settings, payroll recoveries page; loan detail repayment clarity; NF-02/NF-03 implementation notes |
| 1.5 | 2026-04-02 | §13.5: AD-03 when holiday shifting is off (nominal re-baseline; future manual due-date caveat) |
| 1.6 | 2026-04-02 | §13.2: scope note—unchanged “gap” rows vs wider HR (not AD-03/PI-05) |
| 1.7 | 2026-04-02 | §13.2/§13.4 refresh: LC-04/RE-02/RE-03/AL vs code; RE-03 partial; audit `/org` in API table |
| 1.8 | 2026-04-03 | LC-04 schedule engine (FLAT/REDUCING); RE-03 disburse/settlement/due reminders; ST-02 EPF client note; `hr.loans.reminder` |
