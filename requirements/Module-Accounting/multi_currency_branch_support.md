## 🔹 Multi-Currency & Multi-Branch Support – Detailed Requirements

### Table of Contents
- 1. Purpose & Scope
- 2. Currency Model & Exchange Rates
- 3. Posting, Conversion, and Revaluation
- 4. Multi-Branch and Multi-Company Structure
- 5. Inter-Branch and Intercompany Transactions
- 6. Consolidation & Translation
- 7. Reporting & Analytics
- 8. Controls, Approvals, and Security
- 9. Integrations & APIs
- 10. Validations & Edge Cases

---

### 1. Purpose & Scope
- Enable accurate accounting across multiple currencies, branches, and companies, including posting, revaluation, inter-branch/intercompany flows, and consolidated reporting
- In-scope: currencies and rates, FX gains/losses, branch/company scoping, eliminations and translation, reporting

---

### 2. Currency Model & Exchange Rates
- Currency Model
  - Company Functional Currency; optional Group Reporting Currency
  - Transaction Currency at journal line level; account currency policy (functional/reporting)
- Exchange Rates
  - Rate Types: Spot, Average, Month-End, Custom
  - Sources: Manual table, Provider API (ECB/etc.), File import
  - Effective-dated with history; approval for overrides; audit trail
- Rate Application
  - Document-level lock when required (e.g., invoices); voucher-level rate consistency policy
  - Fallback hierarchy: document rate → daily table → provider → manual entry (with approval)

---

### 3. Posting, Conversion, and Revaluation
- Posting & Conversion
  - Convert transaction amounts to functional and reporting currencies at posting
  - Precision per currency; rounding with tolerance and dedicated rounding accounts
  - Realized FX gain/loss on settlements (AR/AP, bank)
- Revaluation
  - Period-end revaluation of monetary balances using period-end rate
  - Post unrealized FX gains/losses; reversal policy next period or roll-forward
- Disclosures
  - Separate reporting of realized vs unrealized FX; drill to source

---

### 4. Multi-Branch and Multi-Company Structure
- Organization Model
  - Company → Branch/Business Unit → Warehouse/Cost Centers
  - Scope/visibility and posting rights restricted by user role and branch/company
- Dimensions
  - Branch as a required dimension on postings when enabled; defaulting and lock policies by account
- Shared CoA
  - Shared chart across companies with company-specific overrides (mappings, numbering ranges)

---

### 5. Inter-Branch and Intercompany Transactions
- Inter-Branch (within company)
  - Mirror entries for due-to/due-from between branches; auto-balancing rules
  - Settlement and netting options; elimination in management reporting if desired
- Intercompany (across companies)
  - Due-to/due-from accounts per counterparty; configurable pricing and FX rules
  - Auto-generation of reciprocal entries via workflow or API
- Cross-Currency
  - Apply FX at transaction time; recognized gains/losses on settlement

---

### 6. Consolidation & Translation
- Consolidation
  - Combine multiple companies/branches; elimination of intercompany balances and transactions
  - Consolidation journals separate from entity ledgers; audit and versioning
- Translation
  - Translate subsidiaries to group currency using IAS/IFRS policies
  - CTA (Cumulative Translation Adjustment) accounts managed and disclosed

---

### 7. Reporting & Analytics
- Standard Reports
  - Trial Balance, P&L, Balance Sheet in functional and reporting currency
  - Branch-wise and company-wise P&L/BS; FX impact reports
- Features
  - Filters by company/branch; date/period; currency view
  - Drill-down to ledger and voucher; schedule exports; saved views

---

### 8. Controls, Approvals, and Security
- Approvals
  - Rate overrides; revaluation postings; intercompany auto-generation
- Access
  - Role- and branch-scoped permissions; sensitive FX accounts masked
- SoD
  - Separate rate maintenance vs posting approval responsibilities

---

### 9. Integrations & APIs
- APIs
  - Rates: create/update/list; bulk import; provider sync
  - Journals: post with transaction currency; revaluation run trigger
  - Consolidation: run job, list eliminations, export consolidated TB/FS
- Webhooks
  - rate.updated, revaluation.completed, consolidation.completed

---

### 10. Validations & Edge Cases
- Validations
  - Missing rate detection; block/warn per policy; effective date coverage
  - Voucher rate consistency; currency compatibility with account policies
  - Intercompany requires counterpart definition; prevent orphaned due-to/due-from
- Edge Cases
  - Hyperinflation handling (indexation, parallel reporting currency)
  - Partial settlements and revaluation sequencing
  - Backdated postings after revaluation; trigger re-statement checks

---

## Appendix

### A. Worked Examples

#### A.1 Period-End Revaluation
- Scenario: USD AR balance 10,000; functional currency BDT. Booking rate 110, month-end rate 112.
- Revaluation: Unrealized FX loss = (112-110)*10,000 = 20,000 BDT.
- Journal: Dr Unrealized FX Loss 20,000; Cr AR Revaluation Reserve 20,000 (policy-dependent account names).

#### A.2 Inter-Branch Transfer (Same Company)
- Scenario: Branch A pays 500,000 BDT on behalf of Branch B.
- Journals (auto-mirrored):
  - Branch A: Dr Due from Branch B 500,000; Cr Bank 500,000
  - Branch B: Dr Expense/Payable 500,000; Cr Due to Branch A 500,000

#### A.3 Intercompany Sale (Cross-Currency)
- Scenario: Company X (USD) sells 50,000 USD goods to Company Y (BDT) at 111 BDT/USD.
- Journals:
  - Company X: Dr AR (USD) 50,000; Cr Revenue (USD) 50,000
  - Company Y: Dr Inventory/Expense (BDT) 5,550,000; Cr AP (BDT) 5,550,000
- Settlement rate 112 → realized FX recognized on both sides per policy.

#### A.4 Consolidation Translation
- Scenario: Subsidiary functional currency USD; group reporting BDT.
- Translation: P&L at average rate; BS at closing rate; CTA to equity.

### B. Glossary (focused)
- Functional Currency: Base currency used for entity posting/reporting.
- Reporting Currency: Group currency used for consolidated reporting.
- Transaction Currency: Currency of the original transaction.
- Revaluation: Period-end FX adjustment of monetary balances.
- Realized/Unrealized FX: FX recognized on settlement / at revaluation.
- CTA: Cumulative Translation Adjustment from consolidation translation.
- Due to/Due from: Inter-branch or intercompany balancing accounts.

### C. References
- See `Module-Accounting/core_accounting_features.md` (Sections 14–16)
- See `requirements/cross-cutting/data_localization.md` (currency, timezone)
- See `requirements/cross-cutting/integrations.md` (rate providers)

