
# 🔹 Core Accounting Features – Full Breakdown  
This document defines the core financial accounting capabilities of EasyOps across companies/branches, including General Ledger, Journal Entries, Chart of Accounts, Trial Balance, Profit & Loss, Balance Sheet, Cash Flow, period close, multi-currency, controls/approvals, and integrations. It specifies required fields, workflows, validations, reports, and governance.

## Table of Contents
- 1. Overview
- 1.1 User Roles & Permissions (summary)
- 1.2 Data Model (high-level)
- 1.3 Accounting Configurations (global)
- 2. Chart of Accounts (CoA) Management
  - 2.1 CoA Data Model (detailed)
  - 2.2 CoA Operations & Governance
- 3. Journal Entries
- 4. General Ledger (GL)
  - 4.1 Intercompany & Consolidation
- 5. Trial Balance
- 6. Profit & Loss Statement
  - 6.1 COGS Detail
- 7. Balance Sheet Generation
- 8. Cash Flow Statement
- 9. Double-Entry Bookkeeping System
  - 9.1 Purpose & Principles
  - 9.2 Posting Engine Requirements
  - 9.3 Validations & Controls
  - 9.4 Error Handling & Recovery
  - 9.5 Edge Cases
- 10. Fiscal Year & Period Locking
- 11. Bank Reconciliation
- 12. Auxiliary and Cross-Cutting Capabilities
- 13. Period Close & Reconciliation
- 14. Opening Balances & Migration
- 15. Multi-Currency Enhancements
- 16. Controls, Approvals, and Audit
- 17. Integrations & APIs (summary)
- 18. Reporting Enhancements
- 19. Non-Functional Requirements (see cross-cutting)
- Appendix
  - A. Worked Journal Example
  - B. Glossary (selected)
  - C. References

---

## 1. Overview
- Purpose: Define core financial accounting capabilities powering GL, subledgers, and statutory reporting.
- Out of scope: Detailed AR/AP, Fixed Assets, Payroll (covered in dedicated docs) except where they auto-post to GL.
- Cross-cutting: See `requirements/cross-cutting/` for RBAC, audit, data, and platform NFRs.

---

## 1.1 User Roles & Permissions (summary)
- Accountant: create/approve journals (as per role), reconciliations, reports
- Finance Manager/Controller: approve, period lock/unlock, adjustments
- Auditor (read-only): full audit logs, drill-down to vouchers
- Branch Accountant: scoped access to branch/warehouse/company

---

## 1.2 Data Model (high-level)
- Account (GL): code, name, type, currency, parent, active
- Journal Entry (Header): voucher no, date, period, branch, status, attachments
- Journal Line: account, debit, credit, narration, reference, currency, fx rate
- Posting Reference: source module (Sales, Purchase, Payroll, Inventory), source id
- Period/Fiscal Year: start/end, status (open/closed), lock flags
- Bank Statement Line: date, description, debit/credit, balance, match status

---

## 1.3 Accounting Configurations (global)
- Fiscal calendar: start month, period definitions (monthly/13-period), holidays
- Numbering sequences: separate sequences per voucher type, branch, fiscal year
- Default accounts: retained earnings, exchange gain/loss, rounding, suspense
- Dimensions: enablement and required rules (branch, department, cost center, project)
- Posting rules: allow backdated days, future-dated days, weekend/holiday rules
- Consolidation: multi-company setup, translation method, elimination policies

---

## 2. **Chart of Accounts (CoA): Structure & Governance**
- Purpose
  - Define a complete, hierarchical accounting structure that standardizes postings, reporting, and controls across companies/branches.

- Scope
  - GL accounts used by all subledgers (AR, AP, Inventory, Payroll, Fixed Assets) and manual journals
  - Reporting mappings for P&L/Balance Sheet/Cash Flow; tax and regulatory categorizations

- Structure & Types
  - Primary Types: Asset, Liability, Equity, Income, Expense
  - Subtypes/Groups (examples): Current Assets, Non-Current Assets, Current Liabilities, Non-Current Liabilities, Operating Income, Non-Operating Income, COGS, Operating Expenses
  - Hierarchy: parent → child accounts up to configurable depth (e.g., 4–6 levels)

- Numbering & Hierarchy Rules
  - Configurable numbering scheme (e.g., 1xxx Assets, 2xxx Liabilities, 3xxx Equity, 4xxx Income, 5xxx Expenses)
  - Auto-suggest next code within group; reserved ranges; prevent collisions across companies when shared CoA is enabled
  - Parent-child constraints: children must inherit or be compatible with parent type

- Account Master (Fields)
  - Identity: Code (unique, hierarchical), Name (localized), Short Name, Description
  - Classification: Type, Subtype/Group, Normal Balance (Debit/Credit), Level (depth)
  - Status & Lifecycle: Draft/Active/Inactive, Effective From/To, Lock Reason
  - Currency: Account currency policy (functional/reporting), Allow foreign currency postings (Y/N)
  - Posting Controls: Posting Allowed (Y/N), Reconciliation Required (Y/N), Allow Manual Posting (Y/N)
  - Flags: Cash/Bank, Control Account, Tax Account, Suspense Account, Rounding Account
  - Dimensions Allowed: Branch, Department, Cost Center, Project, Product/Item, Salesperson (booleans or policy set)
  - Opening Balances: per company/branch and currency with effective date
  - External Mappings: Local GAAP code, statutory mapping id, integration codes (e.g., Tally, QuickBooks)
  - Documentation: Notes, Policy Attachments, Owner/Steward

- Control Accounts & Subledger Linkage
  - Control Types: Customer (AR), Vendor (AP), Inventory, Fixed Asset, Payroll Clearing, Tax Payable/Receivable
  - Manual Posting Policy on Control Accounts: Block/Warn/Allow with approval thresholds
  - Subledger reconciliation required flag and reporting tie-out views

- Tax & Withholding Accounts
  - Support multiple tax regimes (VAT/GST, withholding, sales tax) with rate and jurisdiction linkage
  - Input vs Output tax accounts, reverse charge scenarios, deferred tax accounts (if applicable)

- Localization & Reporting Mapping
  - IFRS/Local GAAP mapping tables for statutory statements
  - Reporting lines and sort order; ability to override per company while inheriting defaults

- Validation & Data Quality Rules
  - Unique code within CoA; prevent circular parent references
  - Type consistency between parent and child; normal balance alignment
  - Mandatory dimensions by account type or specific account (e.g., project required for WIP accounts)
  - Prevent posting to inactive or locked accounts; prevent deactivation with open balances or future-dated postings

- Data Protection & Access
  - Role-based visibility: hide restricted accounts (e.g., executive compensation) from non-authorized roles
  - Field-level protections for editing classification and external mappings

### 2.1 CoA Data Model (detailed)
- Entities
  - Account
    - Fields: Code, Name, Localized Names, Description, Type, Subtype/Group, Level, Parent, Normal Balance, Currency Policy, Posting Allowed, Reconciliation Required, Manual Posting Policy, Flags, Dimensions Allowed, Opening Balances, Status, Effective From/To, External Mappings, Notes, Attachments, Owner
  - ReportingMap
    - Fields: Account Code, Reporting Line Id, Statement Type (P&L/BS/CF), Sort Order, Effective From/To, Company Override (Y/N)
  - PostingRule (optional)
    - Fields: Account/Group, Condition (module, document type, dimension pattern), Posting Allowed (Y/N), Required Dimensions
  - DimensionPolicy
    - Fields: Account/Group, Dimension (Branch/Dept/Cost Center/Project/etc.), Rule (required/optional/blocked), Default Source

- Constraints & Behaviors
  - On change of parent/type: validate existing children; require migration plan
  - On merge of accounts: re-point historical entries, preserve audit, maintain aliases
  - Versioning: effective-dated changes to names/mappings without breaking historical reports

### 2.2 CoA Operations & Governance
- Create/Edit/Deactivate
  - Maker-checker workflow for new accounts and classification changes
  - Real-time validation with preview of postings and mapping impact
- Bulk Operations
  - Import/Export via CSV/Excel with validation reports and partial acceptance
  - Bulk rename, regroup, merge/split with automated re-mapping and audit
- Templates
  - Industry templates (Trading, Manufacturing, Services, Non-Profit) with regional tax presets
  - Company onboarding wizard to select template and customize ranges
- Versioning & Effective Dating
  - Schedule changes (e.g., code renumbering) to take effect on future date/period
  - Maintain alias codes for legacy references; redirect lookups
- Governance
  - CoA Steward role; approval thresholds for sensitive changes
  - Period lock interaction: restrict structural changes in locked periods
  - Change log with who/when/what (before/after values), exportable
- Access Control
  - Rights to view/post/edit by account group and branch/company
  - Segregation of duties checks for CoA maintenance vs posting roles
- APIs & Integrations
  - Read endpoints for CoA, reporting maps, and dimension policies
  - Webhooks on account.created/updated/deactivated; idempotent imports

- Edge Cases
  - Country-specific statutory accounts requiring fixed codes and mappings
  - Multi-company shared CoA with company-specific overrides
  - Sunset of accounts with residual balances; automated transfer to successor accounts

---

## 3. **Journal Entries**
- Purpose
  - Record and post financial transactions to the general ledger with full controls, approvals, and auditability. Support manual and system-generated entries.

- Types
  - Manual Journal Entry (one-off)
  - Recurring Journal (templates with schedules)
  - Reversing Journal (auto-reverse on specified date/period)
  - Accruals/Provisions (month-end with reversal)
  - Opening/Closing Entries (migration, year-end)
  - System Generated (auto-posted from Sales, Purchase, Payroll, Inventory, Fixed Assets)

- Header Fields
  - Voucher No. (sequence per company/branch, per fiscal year)
  - Date, Fiscal Period, Company/Branch
  - Status (Draft, Submitted, Approved/Posted, Reversed, Rejected)
  - Source Module (Manual, Sales, Purchase, Payroll, Inventory, FixedAssets, API)
  - Reference (Document No., Notes)
  - Attachments (supporting docs)
  - Created By, Approved By, Posted By, Timestamps

- Line Fields
  - Account (Code, Name)
  - Dimension Tags (Branch, Department, Cost Center, Project, Product/Item, Salesperson)
  - Debit Amount, Credit Amount (in transaction currency)
  - Currency (transaction), FX Rate to functional/reporting currency
  - Narration/Description (line-level)
  - Reference Link (source document id)

- Derived/Meta Fields
  - Functional/Base Amounts, Reporting Currency Amounts
  - Posting Batch Id, Correlation Id (trace across modules)

- Validations & Rules
  - Balanced Entry: sum(Debits) = sum(Credits) at functional currency; validate at transaction currency where applicable
  - Period Controls: posting date within open period; block or warn on locked periods per role
  - Account Controls: postings allowed; control accounts manual-post policy (block/warn/allow)
  - Dimension Policies: required/optional/blocked per account; validate completeness
  - FX Rules: rate effective date, source hierarchy (manual, daily table, provider API); rounding and gain/loss handling
  - Duplicates/Idempotency: detect duplicate vouchers by hash (date, amount, lines, reference) when importing/API
  - Sequence Integrity: voucher numbering unique per sequence scope
  - Attachments: allowed file types, max size; require for certain entry types (e.g., adjustments)

- Lifecycle & Workflow
  - Draft → Submitted → Approved/Posted → (Optional) Reversed
  - Maker-checker approvals based on thresholds (amount, accounts used, dimensions)
  - Reversal creates linked contra journal with auto-generated narration and date

- Features
  - Templates and Recurrence: monthly/quarterly schedules, working-day shift rules
  - Copy/Clone existing journal; mass-edit lines; CSV import with validation preview
  - Allocations: split amounts across dimensions by percentage or driver (e.g., headcount)
  - Attachments and comments at header and line level; audit trail of changes
  - Drill-down and drill-back: from ledger to journal and to originating document
  - Bulk Post/Unpost (with permissions) and batch processing for large volumes
  - Error handling with retry for system-generated postings; dead-letter queue for failures

- Integrations & APIs
  - REST endpoints: create, validate, submit, approve/post, reverse, list, export
  - Webhooks/events: journal.created, journal.posted, journal.reversed, journal.failed
  - Import: CSV/Excel with mapping templates; API idempotency keys

- Edge Cases
  - Mixed-currency lines; multi-rate within same voucher (warn/allow per policy)
  - Backdated corrections after close (require approval and audit justification)
  - Very high line counts (performance: pagination in UI, streamed imports)

---

## 4. **General Ledger (GL)**
- Purpose
  - Provide detailed account-wise transaction history and running balances with drill-down to source documents for any date range.

- Note on Bank/Cash GL Mappings
  - Bank and cash operations use GL mappings defined in `Module-Accounting/bank_cash_management.md` (e.g., bank GL account, charges, interest, FX gain/loss, clearing/in‑transit). Ensure these mappings are set to see correct ledger postings and drill-downs.

- Columns/Fields
  - Date, Voucher No., Account Code, Account Name
  - Description/Narration, Reference (Invoice/Bill/PO/etc.)
  - Debit Amount, Credit Amount, Running Balance (Debit/Credit)
  - Dimensions (Branch, Department, Cost Center, Project), User/Role, Attachment flag
  - Currency (transaction, functional), FX rate, Source Module/Document ID

- Filters & Parameters
  - Date range or Fiscal Period; account(s) or account ranges; account type/group
  - Entity scope (Company/Branch) and dimensions; user; source module
  - Include/Exclude: opening entries, reversing entries, system journals
  - Currency view: transaction currency, base/reporting currency

- Features
  - Real-time ledger with running balances; drill-down to voucher and original document
  - Grouping and subtotaling by day, account, dimensions; pivot-style summaries
  - Multi-currency display with on-the-fly conversion to reporting currency
  - Saved views and scheduled exports; print-friendly statements
  - Performance optimizations for large datasets (paging, server-side filters)

- Validations & Rules
  - Display only posted/approved entries by default; toggle to include drafts (if permitted)
  - Respect period locks; prevent edits via ledger screen
  - Show normal balance orientation while keeping raw Dr/Cr amounts

- Edge Cases
  - Accounts with high-volume postings; lazy loading and incremental totals
  - Backdated entries affecting running balances; recompute as-of balances
  - Split transactions spanning multiple dimensions and currencies

### 4.1 Intercompany & Consolidation
- Intercompany Accounts: due-to/due-from per company with auto balancing
- Elimination Entries: configurable rules at consolidation level; period-end process
- Translation: FX translation for subsidiaries; CTA postings as per policy
- Reporting: consolidated TB, elimination reports, and intercompany mismatch report

---

## 5. **Trial Balance**
- Purpose
  - Provide a period-wise snapshot of all GL account balances ensuring Debits = Credits, with the ability to compare unadjusted and adjusted figures.

- Columns/Fields
  - Account Code, Account Name, Account Type (Asset/Liability/Equity/Income/Expense)
  - Opening Balance (Debit, Credit)
  - Period Activity (Debits, Credits)
  - Adjustments (Debits, Credits) – manual and system adjustments
  - Closing Balance (Debit, Credit)
  - Optional Columns: Branch/Company, Currency, Dimensions (Department, Cost Center, Project), Notes

- Filters & Parameters
  - Fiscal Year and Period selection; support date-range mode
  - Entity scope: Company, Branch, Warehouse (where applicable)
  - Include/Exclude: zero-balance accounts, inactive accounts, control accounts
  - Level of detail: leaf accounts only vs rolled-up parent groups
  - Currency presentation: base currency, transaction currency, reporting currency

- Features
  - Unadjusted vs Adjusted Trial Balance views with toggles and side-by-side comparison
  - Drill-down from balances to ledger entries and source documents
  - Automatic balancing check with variance highlight if Debits ≠ Credits
  - Multi-entity consolidation view with elimination adjustments (if enabled)
  - Multi-currency handling with FX revaluation impact indicators for monetary accounts
  - Save report variants (with filters) and schedule delivery (email/exports)
  - Export to CSV/Excel/PDF; print-friendly layout with totals/subtotals

- Validations & Rules
  - Period must be open/closed as per user role; no backdated adjustments without permission
  - Balances computed using opening + activity + adjustments = closing
  - Respect account normal balance for display but preserve raw Dr/Cr columns
  - Dimension completeness checks if required by account configuration

- Edge Cases
  - Accounts with both debit and credit activity in period
  - Inactive or locked accounts with historical balances
  - Suspense/rounding accounts highlighted when non-zero

---

## 6. **Profit & Loss Statement**
- Purpose
  - Present financial performance over a period with revenue, costs, and expenses leading to net profit, supporting multi-entity and multi-currency reporting.

- Composition & Lines
  - Revenue: Sales Income, Other Income
  - Cost of Goods Sold (COGS): direct materials, direct labor, manufacturing overheads, purchase returns/discounts adjustments
  - Gross Profit
  - Operating Expenses: Admin, Selling, Marketing, Finance
  - Operating Profit
  - Non-Operating Income/Expenses
  - Net Profit Before Tax, Tax Provision, Net Profit After Tax
  - Mapping from CoA to P&L reporting lines with configurable groupings and sort order

- Columns/Fields
  - Current Period Amount, Prior Period/Year Amount (for comparison)
  - Variance (absolute, %), Year-to-Date (YTD), Trailing Twelve Months (TTM)
  - Dimensions (Branch, Department, Cost Center, Project), Currency (functional/reporting)

- Filters & Parameters
  - Fiscal period/date range; company/branch scope; dimension filters
  - Presentation: with/without other income, expense classification method (nature/function)
  - Currency: base vs reporting currency; show transaction currency where applicable

- Features
  - Comparative views (this period vs last period/year; Budget vs Actual)
  - Drill-down from P&L lines to trial balance and ledger entries
  - Multi-entity consolidation with elimination adjustments; intercompany elimination disclosure
  - Multi-currency translation per policy; show FX impact on income statement
  - Saved layouts and scheduled delivery; export to CSV/Excel/PDF

- Validations & Rules
  - P&L roll-up validates to trial balance totals for the same scope/period
  - Complete mapping coverage: all posting accounts assigned to a reporting line
  - Expense presentation consistent (nature vs function) across periods unless reclassified with disclosure

- Edge Cases
  - Mixed classification (nature/function) in some regions; configurable policy
  - Negative revenue or expense reclassifications highlighted with notes

### 6.1 COGS Detail
- 1) COGS Calculation Methods
  - Fields
    - Calculation Method (FIFO, LIFO, Weighted Average, Specific Identification)
    - Product / SKU, Unit Cost, Purchase Date, Sale Date
  - Features
    - Auto COGS posting at time of sale
    - Support multiple costing methods with policy enforcement
    - Product category-wise COGS; method switch with adjustment entries

- 2) Direct Material Costs
  - Fields
    - Raw Material ID, Purchase Cost, Freight & Import Duties, Storage/Handling Costs, Batch / Lot Number
  - Features
    - Automatic allocation of raw material cost to finished goods
    - Integration with Inventory; batch-wise costing

- 3) Direct Labor Costs
  - Fields
    - Employee / Worker ID, Work Order / Production Batch, Hours Worked, Hourly Rate / Salary, Overtime Cost
  - Features
    - Labor allocation to production orders; job costing reports; payroll integration

- 4) Manufacturing Overheads (if applicable)
  - Fields
    - Overhead Type (Electricity, Rent, Maintenance, Depreciation), Amount, Allocation Basis (Machine Hours, Labor Hours, Units Produced)
  - Features
    - Overhead absorption policies; multiple allocation bases; department/factory-wise tracking

- 5) Purchase Returns & Discounts Adjustment
  - Fields
    - Purchase Return ID, Supplier Name, Returned Quantity, Value Adjusted, Discounts Received
  - Features
    - Auto adjustment in COGS; link to supplier accounts; debit note linkage

- 6) COGS vs Revenue Analysis
  - Fields
    - Product / Category, Revenue (per sale), COGS (per sale), Gross Margin %
  - Features
    - Real-time gross profit; product-wise profitability; trend analysis

- 7) COGS Accounting & Reporting
  - Fields
    - GL Accounts (Inventory, COGS, Purchases, Sales), Journal Entries, Fiscal Year / Period
  - Features
    - Auto COGS posting during sales; period-end inventory adjustments; P&L integration; COGS statements

- 8) Advanced Features
  - Features
    - Multi-Currency COGS Tracking; Multi-Warehouse Costing; Cost Variance (Standard vs Actual); Forecasted COGS; Budget integration

---

## 7. **Balance Sheet**
- Purpose
  - Present financial position at a point in time with assets, liabilities, and equity, supporting standalone and consolidated views.

- Composition & Lines
  - Assets: Current, Non-Current (Fixed/Intangible/Other)
  - Liabilities: Current, Non-Current
  - Equity: Share Capital, Reserves, Retained Earnings, CTA
  - Mappings from CoA accounts to BS lines with configurable groupings

- Filters & Parameters
  - As-of date or fiscal period; standalone vs consolidated; company/branch scope
  - Level of detail: summarized groups vs account-level expansion
  - Currency presentation: base vs reporting; translation method for consolidation

- Features
  - Comparative view (current vs prior period/year)
  - Drill-down to trial balance and ledger from each line
  - Consolidation with eliminations and translation; CTA disclosure
  - Ratio indicators: Working Capital, Current Ratio, Debt-to-Equity, Quick Ratio
  - Export to CSV/Excel/PDF; saved layouts and scheduled delivery

- Validations & Rules
  - Assets = Liabilities + Equity check with variance highlight
  - Retained earnings roll-forward reconciles to prior period + P&L
  - Mapping completeness checks for all active accounts

- Edge Cases
  - Negative inventory or bank overdrafts presentation rules
  - Zero or missing mappings flagged in a validation report

---

## 8. **Cash Flow Statement**
- Purpose
  - Show cash movements over a period categorized into operating, investing, and financing activities, supporting direct and indirect methods.

- Composition
  - Operating: receipts from customers, payments to suppliers/employees, interest/taxes
  - Investing: purchase/sale of assets, investments
  - Financing: loans drawdown/repayment, equity, dividends

- Filters & Parameters
  - Period/date range, company/branch scope, method (direct/indirect)
  - Include/exclude non-cash and non-operating adjustments

- Features
  - Indirect method builder starting from Net Profit with adjustments (depreciation, working capital changes)
  - Direct method with bank/ledger classification rules and mapping templates
  - Reconciliation to bank movement; variance analysis vs cash ledger
  - Export to CSV/Excel/PDF; saved layouts and scheduled delivery

- Validations & Rules
  - Operating + Investing + Financing = Net cash movement
  - Net cash movement reconciles to change in cash/bank balances
  - Classification rule conflicts flagged for review

- Edge Cases
  - Multi-currency cash translation impacts
  - Non-cash transactions (barters, revaluations) excluded or separately disclosed

---

## 9. **Double-Entry Bookkeeping**
- Every Debit = Credit
- Features:
  - Real-Time Balancing
  - Error Alerts
  - Audit Log

### 9.1 Purpose & Principles
- Purpose
  - Guarantee accounting integrity across all postings with a provable, auditable mechanism that enforces Debits = Credits at every level.
- Principles
  - Every transaction posts at least two lines (debit and credit) and balances at header level
  - No orphan lines: lines must belong to a valid journal header in a valid period
  - Immutable posted entries; changes via reversal/correcting entries only
  - Traceability: every line references its source (module, document, user, time)

### 9.2 Posting Engine Requirements
- Balancing & Currency
  - Validate balance in functional currency; support additional checks in reporting currency
  - Multi-currency lines converted using effective FX rate; rounding posted to configured accounts
- Atomicity & Idempotency
  - Post all lines or none; transactional commit with rollback on failure
  - Idempotent posting using correlation id/hash to prevent duplicates from replays
- Concurrency & Sequencing
  - Prevent race conditions on sequences and period state; optimistic concurrency on journals
  - Batch posting support with retries and dead-letter queue for failures
- Performance
  - High-volume posting via streaming/bulk APIs; background processing for heavy batches

### 9.3 Validations & Controls
- Structural
  - Sum(debits) = Sum(credits) per journal; no zero-value lines unless allowed by policy
  - Accounts must be active, posting-allowed, not locked; dimensions must satisfy account policy
- Period & Policy
  - Posting date within open period; warn/block per role on exceptions
  - Control accounts: manual-post policy enforced (block/warn/allow);
  - FX rate availability and validity by date/source; fallback hierarchy

### 9.4 Error Handling & Recovery
- Validation errors returned with actionable messages (account, line no., reason)
- Partial-import handling: accept valid lines into draft, reject invalid lines with report
- Auto-created corrective entries for rounding differences within tolerance; escalate otherwise
- Comprehensive audit: who/when/what, previous vs new values, correlation id

### 9.5 Edge Cases
- Mixed-currency vouchers with line-level rates; enforce single functional currency balance
- Backdated postings after revaluation or close; require approval and auto-detect re-statement impact
- Precision/rounding differences across currencies; tolerance and rounding accounts
- Posting reversals that affect dependent reports (e.g., cash flow classification) with re-tagging

---

## 10. **Fiscal Year and Period Locking**
- Purpose
  - Control accounting periods, ensure integrity of financial statements, and prevent unauthorized backdated changes.

- Parameters
  - Fiscal year start/end; period definitions (monthly/13-period); holiday calendar
  - Lock levels: soft lock (warn), hard lock (block), admin override
  - Role-based permissions for close/reopen; approval chains

- Features
  - Close period with automated checks (unposted journals, pending approvals, unreconciled bank items)
  - Auto year-end closing entries (P&L to Retained Earnings) with reversal options
  - Reopen period with audit justification and approver trail
  - Period status dashboard; bulk close across entities

- Validations & Rules
  - Posting to locked periods blocked unless explicit override
  - Structural changes (CoA edits) restricted during/after close
  - Foreign currency revaluation required before close (if applicable)

- Edge Cases
  - Different close calendars per company/branch
  - Leap periods and adjustments for 52/53-week years

---

## 11. **Bank Reconciliation**
- Purpose
  - Reconcile bank statement lines with ledger entries to ensure cash and bank balances are accurate.

- Columns/Fields
  - Statement Date, Description, Reference, Debit, Credit, Statement Balance
  - Book Entry (Voucher No., Date, Account), Amount, Match Status, Variance
  - Fees/Interest identification, Notes, Attachment link

- Filters & Parameters
  - Account selection (bank/cash), date range/statement period
  - Match modes: auto, suggested, manual; tolerance thresholds
  - Include/exclude already reconciled items; multi-currency view

- Features
  - Statement import (CSV/Excel/API); mapping templates; duplicate detection
  - Auto-matching rules by amount/date/reference; configurable tolerance
  - Create adjustments: bank charges, interest, FX differences
  - Partial and many-to-one/one-to-many matches; split/merge support
  - Reconciliation summary with unreconciled items and aging
  - Export reconciliation report and audit trail

- Validations & Rules
  - Prevent matching across locked periods unless allowed
  - Require justification for write-offs above threshold; approvals
  - Ensure book balance = statement balance after reconciliation (within tolerance)

- Edge Cases
  - Reversed/returned transactions; chargebacks
  - Foreign currency accounts with conversion differences
  - Duplicate statement lines or misformatted imports

---

## 12. **Auxiliary and Cross-Cutting Capabilities**
- Purpose
  - Capture non-core but essential capabilities that enhance accounting usability, compliance, automation, and governance. Where detailed specs exist elsewhere, link rather than duplicate.

- Feature Areas
  - Multi-Currency Accounting (see section 15)
    - Post in transaction currency; convert to functional/reporting; FX revaluation and realized gains/losses
  - Tax & VAT/GST Handling (see `Module-Accounting/taxation_compliance.md`)
    - Tax codes/rates, input vs output tax, withholding; returns and e-invoicing (where applicable)
  - Audit Trail & Logs (see `requirements/cross-cutting/security_compliance.md`)
    - Immutable event logs for CRUD/approvals/postings with before/after values and correlation ids
  - Role-Based Access Control (see `requirements/cross-cutting/security_compliance.md`)
    - Role/permission matrix; field-level protections; SoD checks for sensitive actions
  - Recurring & Automated Vouchers
    - Schedules (monthly/quarterly/annual), workday shifts, holiday calendars; auto-post or submit-for-approval
  - Document & Attachment Management
    - File types/size limits, virus scan hooks, versioning, retention policies, secure URLs
  - Notifications & Scheduler
    - Email/SMS/in-app alerts for approvals, failures, period close milestones; digest summaries
  - Budgeting & Forecasting Hooks
    - Actuals vs budget variance tags; lock budget-based posting rules; export to planning tools
  - Dimensions & Analytics
    - Custom dimensions beyond Branch/Department/Cost Center/Project; validation policies and defaulting rules
  - Numbering, Templates & Printouts
    - Voucher numbering sequences per type/entity/year; printable templates with company branding
  - Localization & E-Documents
    - Multi-language labels for accounts/reports; electronic signatures; regional formats
  - Import/Export & Data Quality
    - CSV/Excel templates with validation; data quality dashboards; deduplication utilities

- Validations & Governance
  - Maker-checker for automation rules and numbering changes
  - Retention and purge policies for attachments and logs with legal holds
  - Change management: effective-dated configuration with audit and rollback plans

- Integration Pointers
  - Payroll: postings, provisions, and reconciliations
  - Inventory: COGS, adjustments, GR/IR, write-offs
  - Sales & Purchase: AR/AP invoices, credit/debit notes, tax
  - CRM/ERP: customer/vendor master sync, opportunities to revenue mapping

---

## 13. Period Close & Reconciliation (additions)
- Purpose
  - Ensure accurate and complete financials for a period through standardized checks, reconciliations, approvals, and locks.

- Pre-Close Checklist
  - Subledgers posted: Sales/AR, Purchase/AP, Inventory (COGS/adjustments), Payroll, Fixed Assets (deprn)
  - Suspense/clearing accounts reviewed (e.g., GR/IR, bank clearing)
  - FX revaluation executed for monetary balances; tax calculations posted
  - Recurring/reversing entries generated; accruals/provisions booked

- Close Process (Steps)
  1) Validate Unposted Items
    - Report of drafts/pending approvals by module; bulk post/approve with permissions
  2) Reconciliations
    - Bank reconciliation up to period end
    - Subledger tie-out: AR/AP/Inventory to GL with variance thresholds
    - Intercompany due-to/due-from reconciliation; propose eliminations
  3) Adjustments
    - Accruals, provisions, write-offs, impairment; attach supporting docs
  4) Review & Approvals
    - Controller review of TB variances vs prior period/budget
    - Maker-checker workflow for material adjustments
  5) Lock
    - Soft lock for final review; hard lock upon approval; auditor read-only access

- Reconciliation Reports
  - AR aging vs GL control; AP aging vs GL control; Inventory valuation vs GL; Bank unreconciled items; GR/IR clearing
  - Exception lists with drill-down and notes; certification sign-off capture

- Automation & Scheduling
  - Close calendar per company/branch; reminders/alerts; task assignments and completion tracking
  - Templates for standard close journals (depreciation, accruals) with schedules

- Validations & Rules
  - Prevent lock if critical reconciliations show variances beyond threshold (override with justification)
  - Require attachments for material journals; enforce dimension completeness for period close entries
  - Re-open requires approval and audit justification; changes tracked and reported

- Edge Cases
  - 52/53-week fiscal years; shortened/extended periods
  - Multi-company staggered close; consolidation alignment window
  - Late-arriving documents after close (handled via next-period adjustments with disclosure)

---

## 14. Opening Balances & Migration
- Purpose
  - Onboard entities with accurate opening balances and master data while preserving auditability and reconciliation to legacy systems.

- Scope & Data Sources
  - Trial balance openings by account/branch/currency
  - Subledger openings: AR (customer-level, invoice/aging), AP (vendor-level, bill/aging), Inventory (item/warehouse quantities and costs)
  - Bank statement starting balances; fixed assets register (if applicable)

- Templates & Fields
  - TB Template: Account Code, Account Name, Branch/Company, Currency, Debit, Credit, Notes
  - AR/AP Template: Party Code/Name, Doc No (optional), Due Date, Currency, Amount, Tax, Aging Bucket
  - Inventory Template: SKU, Description, UoM, Warehouse, Quantity, Unit Cost, Batch/Lot/Serial (optional), Valuation Method
  - Mapping Tables: legacy account/party/item codes to EasyOps codes

- Process (Steps)
  1) Prepare & Map
    - Import mapping tables; validate completeness and duplicates
  2) Import & Validate
    - Load TB and subledger files; run validations (accounts active, dimensions allowed, sums match)
  3) Post Openings
    - Auto-generate opening journals per module with source = Migration
    - Inventory openings post to inventory and offset to opening equity/suspense as per policy
  4) Reconcile
    - TB totals vs legacy TB; AR/AP aging totals vs control accounts; inventory valuation vs GL
  5) Sign-off
    - Controller certification; lock migration period; archive import artifacts

- Controls & Validations
  - Balanced TB import (Debit = Credit); currency conversions applied consistently
  - Control accounts postings follow manual-post policy; use opening-specific paths if blocked
  - Attachments required: legacy TB, aging reports, inventory valuation reports

- Cutover & Rollback
  - Cutover date defined; final delta entries to capture late transactions from legacy
  - Rollback plan with ability to reverse migration journals before go-live lock

- Edge Cases
  - Multi-currency openings with historical rates; revaluation immediately after go-live
  - Legacy data quality issues (duplicates, missing codes) handled via exception queue
  - Partial module onboarding (e.g., GL first, AR/AP later) with interim controls

---

## 15. Multi-Currency Enhancements
- Purpose
  - Support transactions in multiple currencies with accurate conversion, posting, revaluation, and reporting across functional and reporting currencies.

- Currency Model
  - Company Functional Currency; optional Group Reporting Currency
  - Transaction Currency at journal line level; account currency policy (functional/reporting)
  - FX Rate Types: Spot, Average, Month-End, Custom; effective-dated

- Rates & Sources
  - Rate Sources: manual table, provider API (e.g., ECB), uploaded files
  - Hierarchy and fallback rules; approvals for manual overrides; audit of changes

- Posting & Conversion
  - Convert transaction amounts to functional and reporting currencies at posting time
  - Rounding policy with tolerance and rounding accounts; per-currency precision settings
  - Realized FX gain/loss on settlement for AR/AP and bank

- Period-End Revaluation
  - Revalue monetary account balances using period-end rate; post unrealized FX gain/loss
  - Support reversal in next period or roll-forward; detailed revaluation report (before/after)
  - Exclusions/overrides for specific accounts or balances

- Reporting
  - Trial Balance, P&L, and Balance Sheet in functional and reporting currency
  - Disclosure of FX impacts (realized vs unrealized) with drill-down to source

- Validations & Rules
  - Missing rate detection with blocking/warning per policy; rate effective date must cover posting date
  - Consistent rate type across a voucher unless allowed; document-level lock of rate when required

- Edge Cases
  - Hyperinflationary adjustments (if applicable); parallel reporting currency restatement
  - Multi-currency vouchers with mixed lines; revaluation of partially settled items

---

## 16. Controls, Approvals, and Audit
- Purpose
  - Enforce governance over financial processes with approvals, segregation of duties, and complete auditability.

- Approvals & Thresholds
  - Maker-checker workflows for journals, revaluations, adjustments, period close
  - Thresholds by amount, account groups, and dimensions; multi-step approvals
  - Delegation, escalation, SLAs, and reminders; vacation/backup assignees

- Segregation of Duties (SoD)
  - Define conflicting role combinations (e.g., create + approve + post)
  - Prevent or flag transactions violating SoD; require compensating controls/approvals

- Access Control & Visibility
  - Role- and branch-scoped permissions for viewing/posting/editing
  - Sensitive accounts and fields masked for non-authorized users

- Audit Trail
  - Immutable logs of create/edit/approve/post/reverse with before/after values
  - Correlation IDs across modules; exportable logs and retention policies

- Period Locks & Exceptions
  - Soft/hard locks; override with justification and approver trail
  - Exception register capturing who, why, when, and impact

- Monitoring & Reporting
  - Control dashboards (pending approvals, exceptions, SoD violations)
  - Audit readiness reports (change logs, access review, control attestations)

- Validations & Rules
  - Approval required before posting for configured scenarios
  - Changes to CoA, rate tables, and numbering require higher-level approvals

- Edge Cases
  - Emergency posting procedures with post-facto approval
  - External auditor read-only access with scoped time-bound permissions

---

## 17. Integrations & APIs (summary)
- Purpose
  - Enable safe, performant, and auditable data exchange with external systems and other EasyOps modules.

- API Surface
  - Journals: create/validate/submit/approve/post/reverse/list/export
  - Ledgers & Reports: GL, TB, P&L, BS, CF with filtering and pagination
  - Master Data: Accounts (CoA), Reporting Maps, Dimension Policies, FX Rates

- Security & Governance
  - OAuth2/OIDC; API keys per client; scopes and rate limits; IP allowlists
  - Idempotency keys; request signatures; audit logs per API call

- Webhooks & Events
  - journal.created, journal.posted, journal.reversed, period.closed, revaluation.completed
  - Retries with backoff; signature verification; dead-letter queue

- Imports & Data Exchange
  - CSV/Excel templates for journals, FX rates, CoA; validation previews and partial acceptance
  - Bulk/batch APIs for high-volume operations with job status tracking

- Integrations
  - Accounting/Tax: e-invoicing portals, tax authorities, banks (statements, payments)
  - Sales & Purchase: AR/AP invoices, credit/debit notes
  - Inventory: COGS postings, adjustments, GR/IR
  - Payroll: payroll journals, provisions, reconciliations

- Reliability & Performance
  - SLAs for response times; pagination/streaming for large datasets
  - Circuit breakers and retries; monitoring/metrics dashboards

---

## 18. Reporting Enhancements
- Purpose
  - Provide fast, flexible, and trustworthy financial and operational reporting across GL and subledgers with drill-down and scheduling.

- Report Catalog (Accounting)
  - General Ledger, Trial Balance, P&L, Balance Sheet, Cash Flow
  - AR/AP aging (summary/detail), Bank reconciliation summaries, GR/IR analysis
  - Variance analysis (Actual vs Budget), Trend and TTM views, Dimension cubes

- Features
  - Filters: company/branch, period/date range, account ranges, dimensions, currency
  - Layouts: save custom column sets, groups, and sorting; shareable views
  - Drill: report → account/line → ledger → voucher → source document
  - Scheduling: email delivery and file exports (CSV/Excel/PDF) to recipients/groups
  - Row-level security and column masking based on roles and sensitivity
  - Calculation helpers: common KPIs and ratios; custom calculated columns

- Data Quality & Consistency
  - Snapshotting at close; as-of reporting for backdated adjustments with change logs
  - Re-computation triggers when late adjustments happen (with audit trail)

- Performance
  - Server-side aggregation; pagination and streaming for large outputs
  - Pre-computed caches for frequent views with freshness indicators

- Governance
  - Report definitions versioned; approval for shared/official reports
  - Data dictionary for fields and KPIs; lineage to source tables

---

## 19. Non-Functional Requirements (NFRs)
- Reference
  - See `requirements/cross-cutting/platform_nfrs.md` for global platform NFRs.

- Accounting-Specific Notes
  - Accuracy & Integrity
    - Double-entry invariants must never be violated; blocking checks over eventual reporting caches
  - Availability
    - Core posting and ledger queries meet agreed SLAs even during period close; graceful degradation for heavy reports
  - Performance Targets (guidelines)
    - Ledger query: p95 < 1.5s for 10K lines; TB/P&L/BS generation p95 < 3s for medium scope
  - Scalability
    - Handle millions of journal lines/month; sharding/partitioning strategy defined
  - Security
    - Row-level access by branch/company; field masking for sensitive accounts; encryption in transit/at rest
  - Observability
    - Correlation IDs across postings and queries; dashboards for posting failures, reconciliation variances
  - Backup & DR
    - Daily backups with point-in-time recovery; tested DR runbooks; RPO/RTO per platform NFRs

---

## Appendix

### A. Worked Journal Example
- Scenario: Accrue monthly rent expense of 10,000 and reverse next month
  - Date: 2025-01-31 (Period Jan-2025)
  - Lines:
    - Debit Expense: Rent (6xxx) 10,000  [Dept=Admin, Cost Center=HQ]
    - Credit Accrued Expenses (2xxx) 10,000
  - Flags: Reversing on 2025-02-01
  - Result: P&L reflects expense in Jan; liability reversed in Feb automatically

### B. Glossary (selected)
- GL: General Ledger (account-level transaction history and balances)
- CoA: Chart of Accounts (hierarchical structure of GL accounts)
- Control Account: GL account representing subledger totals (e.g., AR/AP)
- Suspense Account: Temporary holding account for unmatched or unclear transactions
- GR/IR: Goods Received/Invoice Received clearing account for PO goods vs bills
- Normal Balance: Expected balance side for an account (Debit or Credit)
- Functional Currency: Company’s base currency used for posting and reporting
- Reporting Currency: Optional group currency used for consolidated reporting
- Transaction Currency: Currency in which a transaction is denominated
- Monetary vs Non-Monetary: Monetary items are cash/receivables/payables; non-monetary are inventory, fixed assets
- Revaluation: Period-end FX adjustment of monetary balances using closing rate
- Realized FX: Gain/loss recognized when a foreign-currency item is settled
- Unrealized FX: Gain/loss from revaluing open foreign-currency balances at period end
- Accrual: Expense or revenue recognized before cash is paid/received
- Provision: Estimated liability of uncertain timing or amount (e.g., warranty)
- Voucher: A journal document identified by a sequence/number
- Period Lock: State in which postings to a period are restricted or blocked
- Soft/Hard Lock: Soft warns but allows by exception; hard fully blocks postings
- Maker-Checker: Approval control where creator and approver are different users
- SoD (Segregation of Duties): Control preventing conflicting responsibilities
- CTA: Cumulative Translation Adjustment in consolidation
- TB: Trial Balance (summary of account balances)
- YTD/TTM: Year-to-Date / Trailing Twelve Months
- COGS: Cost of Goods Sold
- Intercompany: Transactions between related legal entities in a group
- Elimination: Removal of intercompany effects in consolidated reporting
- Reversal: Automatic or manual journal that negates a prior entry
- Reclass: Journal that moves amounts between accounts without changing totals
- Write-off: Recognition of an amount as uncollectible or no longer an asset
- Idempotency: Property ensuring repeated API calls don’t create duplicates
- Correlation ID: Identifier linking related events/entries across systems

### C. References
- See `Module-Accounting/taxation_compliance.md`
- See `Module-Accounting/sales_receivables.md` and `purchasing_payables.md`
- See `requirements/cross-cutting/security_compliance.md` and `platform_nfrs.md`
