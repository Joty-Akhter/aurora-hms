# Loans ↔ Payroll (INT-35–INT-39)

This document maps **salary_management_requirements** items **INT-35 through INT-39** to concrete APIs and services in `hr-service`. It closes the “integration story” gap at the contract level: operations and finance can rely on these behaviors without using Adminer-only data paths.

## INT-35 — Loan recovery on payslip

| Requirement | Implementation |
|-------------|------------------|
| Deduction with category **Loan Repayment** may have amount **fed from the loan module**; payroll merges into payslip. | `SalaryComponentCategory.LOAN_REPAYMENT` components are **not** computed from `EmployeeSalaryDetail` rows in `PayrollCalculationService.populatePayrollFromSalary`. Instead, after the main component loop, the first **DEDUCTION** line with that category receives **`LoanPayrollRecoveryService.getRecoveryAmountForEmployee(organizationId, employeeId, periodEnd, payrollRunId)`**, capped by outstanding balance and installments due through period end. |

Configure **one** LOAN_REPAYMENT deduction component per org (recommended); payroll uses the first match in dependency order.

## INT-36 — Loan module contract (amount per period)

| Requirement | Implementation |
|-------------|------------------|
| Loan module provides **recovery amount per employee per period**; payroll does **not** compute EMI logic. | Recovery is derived from **`EmployeeLoan`** + **`LoanInstallment`** schedules: `computeRecoveryDueThrough` sums remaining scheduled amounts for installments with `dueDate <= periodEnd`, capped by `outstandingBalance`. **Payroll does not implement amortization/EMI formulas**—only installment and arrears logic owned by the loan subsystem. |

Optional **`loanId`** is implicit: preview APIs return **one line per active loan** (`LoanRecoveryLineDto.loanId`).

## INT-37 — Stop recovery when loan ends

| Requirement | Implementation |
|-------------|------------------|
| When a loan is fully recovered or stopped, feed is **zero**; payroll does not deduct beyond that. | No active loan → `getRecoveryAmountForEmployee` returns **0**. **Closed/paid** loans are not `ACTIVE`, so they do not contribute. Balance cap: recovery `min(scheduled, outstandingBalance)`. **Idempotency:** if a **PAYROLL** repayment was already recorded for `(loanId, payrollRunId)`, preview returns **0** / `alreadyPostedForRun` so payroll confirmation does not double-post. |

## INT-38 — Unpaid leave / LOP (Could)

| Requirement | Implementation |
|-------------|------------------|
| Time/attendance provides **LOP days**; payroll may reduce salary. | `PayrollAttendanceRollupService` + timesheet/attendance data feed **`PayrollCalculationService`**: optional **LOP_DED** deduction and basic-derived LOP amount when roll-up has data. Proration for base salary uses **`ProrationRule`** on components (ES-28). |

## INT-39 — Overtime (Could)

| Requirement | Implementation |
|-------------|------------------|
| Time module provides **approved overtime hours**; payroll posts an earning line. | Same roll-up path: **`PayrollAttendanceRollupService`** supplies `overtimeHours`; payroll adds **OT_PAY** earning (when component exists) using basic-derived rate × hours. |

## APIs (operational)

| Endpoint | Purpose |
|----------|---------|
| `GET /api/hr/loans/payroll-recoveries?organizationId=&periodStart=&periodEnd=&payrollRunId=` | Preview recovery **per loan** (INT-36/37; idempotency when `payrollRunId` set). |
| `POST /api/hr/loans/payroll-recoveries/confirm?organizationId=&payrollRunId=` | Post PAYROLL repayments (idempotent per loan+run). Also invoked from payroll process/approve where applicable. |
| `GET /api/hr/loans/payroll-recoveries/cross-check?organizationId=&payrollRunId=` | RP-05: payslip loan lines vs loan ledger. |

See also: `docs/knowledge-base/topics/payroll-loan-recovery.md`.
