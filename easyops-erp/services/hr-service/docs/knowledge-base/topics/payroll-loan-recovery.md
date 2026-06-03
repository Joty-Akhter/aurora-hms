# Payroll loan recovery — Detailed guide

This article explains how **loan recoveries** interact with **payroll runs** in EasyOps HR, for **Payroll** and **Finance** users.

**Related:** [HR Salary user manual](../../HR-SALARY-USER-MANUAL.md) (components, runs, payslips).

**Requirements (INT-35–INT-39):** See [INTEGRATION_LOANS_PAYROLL_INT-35-39.md](../../INTEGRATION_LOANS_PAYROLL_INT-35-39.md) for the contract between the loan module and payroll (recovery amounts, idempotency, LOP/overtime cross-reference).

---

## 1. Concepts

- **Recovery amount** — The amount the loan module expects to deduct for an employee for a pay period, based on **installments due** and **arrears**, **capped** by outstanding balance.
- **Payroll run** — A finalized (or in-progress) payroll cycle with **period dates** and **payment date**.
- **Idempotency** — Posting loan repayments against a **payroll run** uses **loan + payroll run** as a key: the same recovery must not post twice for the same run.

---

## 2. Preview before finalize

1. Complete or prepare **payroll** with salary components that include **loan repayment** (see salary manual for `LOAN_REPAYMENT` category).
2. Use loan **recovery preview** APIs (or integrated UI) with **organization**, **period**, and optionally **payroll run id** to see expected amounts **per loan**.
3. When `payrollRunId` is supplied, amounts already **posted** for that run typically show as **zero** in preview (already recovered).

---

## 3. Confirm postings (post-payroll)

After payroll is **ready** for loan integration:

1. **Confirm** deductions for the payroll run — this **creates loan repayment transactions** in the loan ledger.
2. **Do not** double-click confirm repeatedly in production without a **business reason** — the service is **idempotent**, but operational clarity matters.

---

## 4. Reversals and anomalies (RP-05)

Sometimes a payroll deduction must be **reversed** (correcting a wrong run, duplicate, etc.):

- **Controlled reversal** — Use the **reversal** flow from **loan detail** (repayments area) so the **audit trail** stays complete.
- **Anomalies list** — `/hr/loans/payroll-recoveries` can surface **reversal alerts** and related messages for review.

### 4.1 Payslip vs loan cross-check

Use **payroll run id** (UUID) in the **cross-check** tool on the payroll recoveries page:

- Compares **payslip** loan line amounts to **loan module** postings.
- Surfaces **variances** for investigation (rounding, timing, or missing posting).

---

## 5. Operational checklist

| Step | Action |
|------|--------|
| 1 | Payroll run created with correct **period** |
| 2 | Loan recovery **preview** reviewed for key employees |
| 3 | **Confirm** loan postings when payroll is finalized |
| 4 | **Cross-check** if Finance reports mismatches |
| 5 | **Reverse** incorrectly posted payroll lines via **loan** reversal workflow, not ad-hoc DB edits |

---

## 6. Permissions

- **Read** recoveries / anomalies — typically `hr_loans` view permissions.
- **Confirm** postings — **manage**-level loan or payroll permissions (per deployment).

---

*Back to [Knowledge base home](../README.md) · [Master manual](../EASYOPS-HR-USER-MANUAL.md)*
