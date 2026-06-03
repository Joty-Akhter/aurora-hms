# Employee loans — Detailed guide

This article expands on **Section 4** of the [master HR user manual](../EASYOPS-HR-USER-MANUAL.md). It is aimed at **HR and Finance** users who administer loans end to end.

---

## 1. Loan categories (LC-04)

### 1.1 What categories control

- **Code and name** — Identify the product (e.g. Emergency Loan, Staff Loan, Salary Advance).
- **Type** — Distinguish **term loans** from **salary advance** (workflow and policy may differ).
- **Limits** — Optional **max principal** and **max tenure** per category.
- **Interest method** — `NONE`, `FLAT`, or `REDUCING_BALANCE`, plus **annual rate** where required for flat/reducing.
- **Active flag** — Inactive categories are not offered for new applications (per implementation).

### 1.2 Where to view categories

- **Loan register** (`/hr/loans`) includes a **Loan categories** table showing interest method and rate fields for the current organization.

### 1.3 Schedule behavior at disbursement

When a loan is **disbursed**, the system:

1. Reads the **category** linked to the loan.
2. Builds **monthly installments** using the interest method (see master manual §4.2).
3. Sets **outstanding balance** to the **sum of scheduled installment amounts** (principal + interest combined in each installment line).

Operators do **not** manually edit individual installment amounts at disbursement in the standard flow; **recalculate due dates** (AD-03) adjusts **dates**, not principal/interest split, except where business rules skip or leave rows unchanged.

---

## 2. Organization loan settings

**Path:** `/hr/loans/settings`

Typical settings include:

- **Global caps** — Max principal, minimum tenure (e.g. six months employment rule as policy).
- **Single active loan** enforcement (when enabled).
- **Salary advance** — Shortcuts (e.g. skip Finance approval) per policy flags.
- **Holiday calendar behavior (AD-03)** — Whether to **shift** installment due dates off weekends/holidays and **how** (next vs previous business day).

**Save** after changes. Settings affect **new schedules** and **recalculate** operations, not necessarily historical rows unless you run recalc.

---

## 3. Applications and workflow

**Paths:**

- List: `/hr/loans/applications`
- New: `/hr/loans/applications/new`
- Detail: `/hr/loans/applications/:applicationId`

### 3.1 Creating an application

1. Choose **employee** and **category**.
2. Enter **requested amount** and **tenure** (within limits).
3. Add **notes** and optional **attachment references** (URLs or document ids — integrated file upload may vary by deployment).
4. Save as draft or submit per your process.

### 3.2 Approval path

Depending on configuration:

- **HR** may approve first, then **Finance**, or a single step.
- **Salary advance** may use a shortened path.
- Approvers may enter **override** reasons and **decision expiries** when policy allows.
- **Clarify** and **delegate** actions keep the application moving without losing history.

**Audit:** Application actions are listed via **combined audit** on the loan when a loan exists.

---

## 4. Disbursement and loan account

After approval, the loan account is **pending disbursement** until funds are recorded.

1. Open the **loan** from the register (`/hr/loans` → row → detail).
2. Record **disbursement** with amount and date (full or partial per policy).
3. System generates the **schedule** and **notifies** the employee (in-app) when the employee has a linked user.

---

## 5. Repayments

From **loan detail**:

- **Manual repayments** — Record payments not taken via payroll.
- **Payroll** — Recoveries are driven by payroll runs; see [payroll-loan-recovery.md](payroll-loan-recovery.md).
- **Installment skip (RP-01)** — Skipped installments keep **status** stable; **AD-03 recalc** does not overwrite skipped rows.

---

## 6. Settlement at exit

When an employee leaves with an **outstanding balance**:

1. **Start settlement** from the loan (or equivalent API) with separation effective date.
2. **Allocate** repayments from **PF settlement**, **final salary**, or **other dues** — **priority order** may be configured for validation.
3. **Shortfall / write-off** — Record per policy; **legal workflow** metadata may be captured for audit.

**PF hint:** Available PF balance may be suggested when EPF data exists — see [HR EPF user manual](../../HR-EPF-USER-MANUAL.md).

---

## 7. Holiday due dates (AD-03)

### 7.1 Per-loan recalc

From loan detail, recalculate **unpaid** installment due dates according to **org holiday settings** and the HR holiday calendar. **Fully paid** installments and **SKIPPED** rows are not changed.

### 7.2 Bulk recalc (all loans)

From the **loan register**, run **Recalculate all loans (AD-03)** after confirmation.

- Processes **active** and **settlement-pending** loans with a **disbursement date**.
- Each loan runs in its own **transaction**; failures are listed with **loan id** and message on screen.
- An **organization-level audit** row summarizes counts.

### 7.3 When shifting is off

If **shift installment due dates for holidays** is **disabled**, recalc still **re-baselines** dates to the nominal **disbursement + month** curve. This is acceptable until **manual per-installment due-date editing** exists (future product decision).

---

## 8. Accounting export and COA (PI-05)

From the register:

- **Accounting export** — JSON/CSV style extracts for **disbursements** and **repayments** by date range.
- **COA mappings** — Optional **debit/credit account codes** per event type for finance reconciliation.

**Org audit** (`/hr/loans/org-audit`) lists **COA mapping replace** events.

---

## 9. Reporting

From the register you can typically access:

- Summary cards (active loans, pending disbursement, settlement pending, totals).
- **Arrears** and **settlement exit** style reports (per implementation).
- **CSV exports** where provided.

---

## 10. Best practices

- **Reconcile** payroll loan deductions periodically using **payroll recoveries** and **cross-check**.
- **Review org audit** after bulk operations or COA changes.
- **Document overrides** in approvals (reasons and expiry) for auditability.
- **Train** Finance on **idempotent** payroll confirm — safe retries vs duplicate postings.

---

*Back to [Knowledge base home](../README.md) · [Master manual](../EASYOPS-HR-USER-MANUAL.md)*
