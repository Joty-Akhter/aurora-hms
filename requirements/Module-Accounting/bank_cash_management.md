## 🔹 Bank & Cash Management – Detailed Requirements

### Table of Contents
- 1. Purpose & Scope
- 2. Bank Accounts Master
- 3. Cash (Main & Petty) Management
- 4. Receipts & Payments (Bank/Cash)
- 5. Inter-Account Transfers
- 6. Bank Deposits (Cash/Cheque)
- 7. Cheque Management & Printing
- 8. Bank Connectivity & Statements
- 9. Bank Reconciliation (Auto/Manual)
- 10. Daily Cash Position & Liquidity Forecast
- 11. Controls, Approvals, and Security
- 12. Integrations & APIs
- 13. Reports & KPIs
- 14. Validations & Edge Cases

---

### 1. Purpose & Scope
- Provide complete control over bank and cash processes: master data, payments/receipts, deposits, transfers, reconciliation, and liquidity insights
- In-scope: multiple bank accounts, petty cash, cheques, statement imports/APIs, auto-matching rules, GL integration
- Out-of-scope: detailed AP/AR invoicing (covered in subledgers), but this module posts/settles them

---

### 2. Bank Accounts Master
- Fields
  - Bank Name, Account Nickname
  - Account Number/IBAN, Branch, IFSC/SWIFT/BIC, Routing
  - Account Type (Current, Savings, Overdraft, Virtual), Currency
  - Opening Balance (date, amount), Reconciliation Start Date
  - Signatories (names/roles), Daily Limits (payment/withdrawal), Checkbook ranges
  - Default Flags (primary for payments/receipts), Company/Branch scope
  - GL Mappings:
    - Bank GL Account (CoA)
    - Reconciliation Required (Y/N)
    - Statement In-Transit/Clearing Account (optional)
    - Bank Charges Expense Account
    - Interest Income Account / Interest Expense Account
    - FX Gain/Loss Account (for foreign currency accounts)
    - Default Dimensions (Branch/Dept/Cost Center/Project) and lock policy
  - Numbering/Sequences: Payment, Receipt, Cheque series
- Features
  - Versioned changes with audit (e.g., limits, signatories)
  - Deactivate/close account with validation (no open items)
  - External references (bank portal id, virtual accounts)

---

### 3. Cash (Main & Petty) Management
- Cash Boxes
  - Fields: Cash Box ID, Location/Branch, Custodian, Currency, Opening Float, Limits
  - GL Mappings: Cash GL Account (CoA), Replenishment Offset Account (e.g., Bank)
  - Features: Open/close shifts; handover process; shortage/overage capture
- Petty Cash
  - Fields: Fund Name/ID, Custodian, Purpose, Allowed categories, Replenishment threshold
  - Optional Category Mappings: Default Expense Accounts per category
  - Workflows: Request → Approval → Disbursement → Expense booking → Replenish
  - Replenishment journal with attachment of summary + receipts

---

### 4. Receipts & Payments (Bank/Cash)
- Receipt
  - Fields: Date, Mode (Bank Transfer, Cheque, Cash, Gateway), Payer, Reference, Amount, Currency, Dimensions, Attachment
  - GL Mappings: Receipt Clearing Account (if using batch settlements)
  - Features: Allocate to invoices (AR), advance receipts, FX handling, auto-post to GL
- Payment
  - Fields: Date, Mode (Bank Transfer, Cheque, Cash), Payee, Reference, Amount, Currency, Dimensions, Attachment
  - GL Mappings: Payment Clearing Account (if using payment proposals/batches)
  - Features: Vendor bill settlement (AP), partial payments, payment proposals, payment batches
- Controls
  - Approval thresholds; segregation of duties; dual control for bank releases

---

### 5. Inter-Account Transfers
- Fields: From Account, To Account, Date, Amount, Currency, FX rate (if cross-currency), Reference, Narrative
- GL Mappings: Intercompany Due-To/Due-From Accounts (if crossing companies)
- Features
  - Internal transfers (bank→bank, cash→bank, bank→cash)
  - Cross-currency transfers with FX posting (gain/loss) if needed
  - Approval workflow; auto GL postings; prevents circular duplicates

---

### 6. Bank Deposits (Cash/Cheque)
- Cash Deposit
  - Fields: Deposit Slip No, Bank Account, Date, Amount, Preparer, Attachment
  - Features: Batch deposits from cash box; printable deposit slip; status (Prepared/Deposited/Posted)
- Cheque Deposit
  - Fields: Cheque Nos (batch), Bank Account, Date, Total Amount, Bank Cutoff Date, Attachment
  - Features: Deposit list generation, clearing status updates, bounce handling

---

### 7. Cheque Management & Printing
- Cheque Inventory
  - Fields: Book Series, Range (start/end), Assigned To Account, Status (Unused/Issued/Void)
- Cheque Issue
  - Fields: Cheque No, Payee, Amount, Date, Account, Purpose, Status (Issued/Cleared/Bounced/Stopped)
  - GL Mappings: Cheque Payable Clearing Account; Bounce/Charge Fee Account
  - Features: Stop payment, reissue, bounce workflows with charges posting
- Printing
  - Templates per bank; alignment controls; reprint with watermark

---

### 8. Bank Connectivity & Statements
- Statement Sources
  - File import: CSV, Excel, MT940/ISO20022 (where supported)
  - API connectivity (bank integration) with OAuth/keys; polling/scheduled fetch
- Mapping
  - Per-bank column mappings; saved profiles; duplication checks
- Security
  - Encrypted storage of credentials; access control for downloads/imports

---

### 9. Bank Reconciliation (Auto/Manual)
- Matching Data Model
  - Statement Line: Date, Description, Reference, Debit, Credit, Balance, Bank Ref
  - Book Entry: Voucher No, Date, Account, Amount, Dimensions, Source Module
- Auto-Matching Rules
  - Exact amount/date window; normalized reference; counterparty heuristics
  - Configurable tolerance (amount, days), priority ordering, rule simulator
- Manual Matching & Adjustments
  - Many-to-one and one-to-many matches; write-offs with thresholds and approvals
  - Create adjustments (bank fees, interest, FX) with auto-posted journals
- Workflow
  - Import/Fetch → Auto-Match → Review Exceptions → Post Adjustments → Reconcile → Lock Period
- Outputs
  - Reconciliation report (unreconciled list, aging), variance to statement balance, audit trail

---

### 10. Daily Cash Position & Liquidity Forecast
- Cash Position (as-of)
  - Fields: Cash on Hand, Bank Balances per account, Total Cash, Available vs Restricted
  - Features: Branch/Company filters; include/exclude in-transit items
- Short-Term Forecast (7/14/30 days)
  - Inputs: Open AR (expected receipts), AP (due payments), payroll, tax, recurring items
  - Scenarios: best/likely/worst with adjustable assumptions; what-if transfers
  - Outputs: project cash gaps; alerts for expected shortfalls

---

### 11. Controls, Approvals, and Security
- Approvals
  - Payment approvals by thresholds and vendor types; dual control for bank release
  - Transfer approvals; cheque overrides (stop payment, reissue)
- Access
  - Role-based permissions per bank/cash box; masking sensitive account numbers
- Audit
  - Immutable logs of create/edit/approve/post/reverse; attachment history
- Period Locks
  - Block reconciliations/edits in locked periods; exception workflow

---

### 12. Integrations & APIs
- Upstream/Downstream
  - AR/AP modules for settlements; GL for postings; Cash Flow report integration
  - Payment gateways (where applicable); bank portals/APIs; treasury tools
- APIs
  - Receipts/Payments/Transfers: create/validate/approve/post
  - Statements: upload/list/fetch (if connected)
  - Reconciliation: start/match/adjust/export
- Webhooks
  - payment.posted, receipt.posted, statement.imported, reconciliation.completed

---

### 13. Reports & KPIs
- Reports
  - Cash book (daily/period), bank book, petty cash ledger
  - Bank reconciliation summary/detail; unreconciled items aging
  - Cheque issuance/clearing; deposit logs; payment batches
- KPIs
  - Days to reconcile, % auto-matched, unreconciled items > 30 days, cash coverage days

---

### 14. Validations & Edge Cases
- Validations
  - Unique account + number; cannot deactivate accounts with pending items
  - Payments must pass approval & limit checks; require attachments per policy
  - Statement imports: duplicate detection; mapping completeness
  - Reconciliation: balance to statement within tolerance; explain variances
- Edge Cases
  - Cross-currency transfers with mid-rate variance; partial matches across many transactions
  - Statement line reversals/chargebacks; duplicate or out-of-order statements
  - Cheque bounce after prior reconciliation (auto-unmatch and reversal)

---

## Appendix

### A. Worked Examples

#### A.1 Bank Reconciliation Example
- Scenario: Reconcile bank statement ending balance 50,000 with book balance 49,500
- Statement items: Interest credit 200, Bank charges 50, Outstanding cheques 1,000, Deposits in transit 250
- Adjustments: Post interest (Dr Bank 200, Cr Interest Income 200); Post charges (Dr Bank Charges 50, Cr Bank 50)
- Result: Book balance = 49,500 + 200 - 50 = 49,650; Statement balance = 50,000 - 1,000 + 250 = 49,250; Variance = 400 (investigate)

#### A.2 Petty Cash Replenishment
- Scenario: Petty cash fund of 5,000 with 800 remaining; expenses: Office supplies 1,200, Transport 1,000, Meals 1,000
- Journal: Dr Petty Cash 4,200, Cr Bank 4,200; Dr Office Supplies 1,200, Dr Transport 1,000, Dr Meals 1,000, Cr Petty Cash 3,200
- Result: Fund restored to 5,000; expenses properly categorized

#### A.3 Cross-Currency Transfer
- Scenario: Transfer 10,000 USD from USD account to EUR account at rate 0.85
- Journal: Dr Bank EUR 8,500, Cr Bank USD 10,000, Cr FX Gain 1,500 (if rate favorable)
- Result: USD account debited, EUR account credited, FX impact posted

### B. Glossary (Bank & Cash)
- Bank Reconciliation: Process of matching bank statement with book records
- Clearing: Time between cheque deposit and funds availability
- Float: Time between payment issue and bank debit
- In-Transit: Items recorded in books but not yet on bank statement
- Outstanding: Cheques issued but not yet cleared by bank
- Petty Cash: Small cash fund for minor expenses
- Reconciliation Variance: Difference between bank and book balances
- Stop Payment: Instruction to bank to not honor a specific cheque
- Sweep Account: Automatic transfer of excess funds between accounts
- Virtual Account: Bank account for specific customer/vendor payments

### C. References
- See `Module-Accounting/core_accounting_features.md` (Bank Reconciliation section)
- See `Module-Accounting/sales_receivables.md` (Receipts integration)
- See `Module-Accounting/purchasing_payables.md` (Payments integration)
- See `requirements/cross-cutting/security_compliance.md` (Access controls)
- See `requirements/cross-cutting/integrations.md` (Bank APIs)

---

