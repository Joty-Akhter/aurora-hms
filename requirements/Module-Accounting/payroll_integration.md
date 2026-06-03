## 🔹 Payroll Integration – Detailed Requirements

### Table of Contents
- 1. Purpose & Scope
- 2. Data Model & Mappings
- 3. Payroll Journal Posting (Salaries)
- 4. Payroll Liabilities (Taxes, Funds, Statutory)
- 5. Payments & Disbursements (Bank/Cash)
- 6. Expense Claims & Reimbursements
- 7. Bonuses, Allowances, and One-Off Adjustments
- 8. Integration with HR/Payroll Systems
- 9. Reports & Reconciliations
- 10. Validations & Edge Cases

---

### 1. Purpose & Scope
- Post payroll results from HR/Payroll into Accounting with clear mappings, controls, and auditability
- In-scope: salary accrual/posting, statutory liabilities, payments, reimbursements, and adjustments
- Out-of-scope: payroll calculation logic (handled by HR/Payroll)

---

### 2. Data Model & Mappings
- Entities
  - Payroll Run: Period, Company/Branch, Run Type (Regular/Off-cycle), Approved By, Posted Status
  - Employee: ID, Name, Department, Cost Center, Bank Details (for payment), Active Status
  - Payroll Lines: Earnings (basic, allowances, bonus), Deductions (tax, PF, loans), Employer Contributions (PF, social security)
- GL Mappings
  - Earnings → Expense accounts (by component)
  - Deductions → Liability accounts (Tax Payable, PF Payable, Loan Payable)
  - Employer Contributions → Expense (employer) + Liability (fund payable)
  - Net Pay → Salary Payable (clearing)
  - Dimensions: Department, Cost Center, Project (as applicable)

---

### 3. Payroll Journal Posting (Salaries)
- Accrual at Month-End (if required)
  - Dr Salary Expense (components by dimension)
  - Cr Salary Payable (net pay)
  - Cr Tax Payable, Cr PF Payable, Cr Other Deduction Payables
- Posting on Finalization (if no accrual step)
  - Same as above, posted on approval day instead of month-end
- Reversal
  - Automatic reversal of accrual on first day of next period if used
- Fields
  - Run ID, Period, Posting Date, Company/Branch, Dimensions, Attachment (payroll summary)

---

### 4. Payroll Liabilities (Taxes, Funds, Statutory)
- Recognition
  - Deductions from employees and employer contributions booked as liabilities
- Settlement
  - On payment to authority/fund:
    - Dr Liability (e.g., Tax Payable)
    - Cr Bank
- Scheduling
  - Due dates per regime; reminders and approval workflow
- Reporting
  - Liability aging; payment status; compliance forms support (export)

---

### 5. Payments & Disbursements (Bank/Cash)
- Salary Payment Batch
  - Inputs: Approved payroll run; filter by branch/department; bank account selection
  - Output: Payment file (bank format) or internal batch; posting on release:
    - Dr Salary Payable
    - Cr Bank
- Exceptions
  - Failed payments list and re-issue workflow; partial payments handling

---

### 6. Expense Claims & Reimbursements
- Process
  - Request → Approval → Payment → Posting
- Posting
  - Dr Expense (by category/dimension)
  - Cr Employee Reimbursement Payable (on approval)
  - Dr Employee Reimbursement Payable; Cr Bank (on payment)
- Features
  - Policy-based limits; attachments; per diem handling; integration with HR expense module

---

### 7. Bonuses, Allowances, and One-Off Adjustments
- Posting
  - Part of payroll run or off-cycle; mapped to dedicated expense accounts
  - Accrual/reversal if recognized in different period than payment
- Approvals
  - Threshold-based approval chain; audit trail

---

### 8. Integration with HR/Payroll Systems
- Data Sync
  - Employee master (ID, department, cost center, bank), payroll components, run results
- Methods
  - Import (CSV/Excel) with validation preview; API-based integration with idempotency keys
- Security & Audit
  - Scoped tokens; change logs; reconciliation between HR totals and accounting postings

---

### 9. Reports & Reconciliations
- Reports
  - Payroll expense by department/cost center; employer contribution summaries
  - Liability balances and payments (tax, PF, social security)
- Reconciliations
  - Payroll summary vs posted journals; liabilities vs payment registers

---

### 10. Validations & Edge Cases
- Validations
  - Balanced postings; dimension completeness; mapping existence for all components
  - Employee status checks (inactive/terminated handling)
- Edge Cases
  - Off-cycle runs; retroactive adjustments; multi-currency payroll; split payment accounts

---

