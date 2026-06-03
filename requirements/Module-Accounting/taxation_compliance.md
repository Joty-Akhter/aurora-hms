## 🔹 Taxation & Compliance – Detailed Requirements

### Table of Contents
- 1. Purpose & Scope
- 2. Tax Models & Configuration
- 3. Tax Calculation in Sales & Purchases
- 4. Withholding (TDS/TCS) & Certificates
- 5. Returns, Filing & Digital Integration
- 6. Input Tax Credit & Adjustments
- 7. GL Postings & Reconciliation
- 8. Reports & Audit Support
- 9. Validations & Edge Cases

---

### 1. Purpose & Scope
- Provide accurate tax computation, postings, returns, and digital filing support across sales, purchases, and payroll where applicable
- In-scope: VAT/GST/Sales Tax, withholding (TDS/TCS), e-filing, certificates, and audit

---

### 2. Tax Models & Configuration
- Tax Types
  - VAT/GST (input/output), Sales Tax, Withholding (TDS/TCS), Excise/Other (country-specific)
- Master Data
  - Tax codes, rates (%), effective dates, jurisdictions, exemptions
  - Product/Service tax categories; customer/vendor tax registration and place of supply
- Rules
  - Inclusive vs exclusive; zero-rated/exempt; reverse charge; composition schemes
  - Rate history and approval for changes; audit trail

---

### 3. Tax Calculation in Sales & Purchases
- Sales
  - Determine taxability (destination/place of supply, registration status)
  - Compute tax per line or invoice; apply tax code; rounding per jurisdiction
  - GL: Cr Output Tax; Dr AR with tax-inclusive invoices (or separate lines if exclusive)
- Purchases
  - Compute input tax; handle non-creditable portions (blocked credits)
  - GL: Dr Input Tax; Cr AP; capitalize/block as per rules where required
- Multi-Currency
  - Tax calculated in document currency; functional/reporting views maintained

---

### 4. Withholding (TDS/TCS) & Certificates
- Applicability
  - Based on vendor/customer type, thresholds, and categories (services/contract/etc.)
- Calculation
  - Compute withholding on taxable base; support surcharge/cess where applicable
- Postings
  - Dr Expense/Payable; Cr Withholding Payable (for TDS)
  - On remittance: Dr Withholding Payable; Cr Bank
- Certificates
  - Generate certificates; share with counterparty; maintain registry

---

### 5. Returns, Filing & Digital Integration
- Returns
  - Periodic returns (monthly/quarterly) with prefilled figures from journals
  - Reconciliation: Sales register vs return; Purchase register vs input credits
- Filing
  - Export files; API-based e-filing where supported; status tracking and error logs
- Compliance Calendar
  - Due dates; reminders; role-based tasks and approvals

---

### 6. Input Tax Credit & Adjustments
- Credits
  - Credit eligibility by item/category; credit blocks and partial credits
- Adjustments
  - Credit notes/debit notes, advances, bad-debt relief, reverse charge adjustments
- Carry Forward/Refunds
  - Track carry forward balances; refund application support

---

### 7. GL Postings & Reconciliation
- Postings
  - Output Tax (liability), Input Tax (asset), Withholding Payable
- Reconciliations
  - Tax ledgers vs sales/purchase registers; return vs ledger; withholding paid vs certificates

---

### 8. Reports & Audit Support
- Reports
  - Tax liability summary; input credit register; withholding register; exception reports
- Audit
  - Drill from returns to vouchers; export evidence packs (invoices, certificates)

---

### 9. Validations & Edge Cases
- Validations
  - Tax code presence; jurisdiction checks; threshold enforcement; duplicate invoice detection
- Edge Cases
  - Mixed tax rates on one invoice; rounding differences; backdated rate changes; credit reversals

---

