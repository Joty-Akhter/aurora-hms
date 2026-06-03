
# 🔹 Sales & Receivables – Detailed Requirements

## Table of Contents
- 1. Purpose & Scope
- 2. Data Model & Mappings
- 3. Customer Invoices
- 4. Credit Notes & Sales Returns
- 5. Approvals & Delivery/Invoice Match
- 6. Receivables Tracking, Statements & Aging
- 7. Collections & Dunning
- 8. Receipts & Allocations
- 9. Advances & Deposits
- 10. Recurring Invoices & Subscriptions
- 11. Write-offs & Bad Debt Provisioning
- 12. Multi-Currency & Branch Handling
- 13. Integrations & APIs
- 14. Reports & KPIs
- 15. Validations & Edge Cases

---

## 1. Purpose & Scope
- Manage revenue-to-cash lifecycle from invoicing to collection with controls and accurate GL postings
- In-scope: invoices, returns/credits, approvals, tracking, aging/statements, collections, receipts, advances, write-offs
- Out-of-scope: pricing logic and fulfillment (covered in Sales/Inventory), but linked

---

## 2. Data Model & Mappings
- Entities
  - Customer: Code, Name, Addresses, Contacts, Payment Terms, Credit Limit, Default Currency, Tax IDs
  - Invoice: Number, Dates (invoice, due), Customer, Delivery links, Lines (item/service, qty, price, tax, discount), Currency, Dimensions
  - Credit Note: Reference to invoice/delivery, reason, amounts
  - Receipt: Mode, Bank/Cash, Amount, Date, References (invoices), Batch Id
  - Advance/Deposit: Customer, amount, date, application status
- GL Mappings
  - AR Control Account (per customer group or company)
  - Revenue accounts by line item; Tax output; Deferred revenue (if applicable)
  - FX gain/loss; Receipt clearing account (if batching)
  - Dimensions: Branch, Department, Cost Center, Project, Salesperson

---

## 3. Customer Invoices
- Capture
  - Modes: manual, order/delivery flip, API; Proforma → Final
  - Fields: Invoice No (unique per customer), Invoice Date, Due Date/Terms, Currency, Exchange Rate, Attachments
  - Lines: item/service, qty, unit price, discount, tax codes, dimensions
- Posting
  - Dr AR Control (by customer); Cr Revenue; Cr Tax Output (if applicable)
  - Multi-currency: lock document rate; store transaction and functional amounts
- Statuses
  - Draft → Submitted → Approved → Posted → Partially Paid → Settled → Cancelled/Void (with audit)

---

## 4. Credit Notes & Sales Returns
- Credit Notes
  - Reduce receivable: Dr Revenue/Tax (as applicable); Cr AR Control
  - Reference to invoice(s); reason codes; attachments
- Returns
  - With Inventory: stock receipt and costing; pricing policies for refunds
  - Application to open invoices; partial applications supported

---

## 5. Approvals & Delivery/Invoice Match
- Matching
  - Compare delivery quantities and prices to invoice; enforce tolerances
  - Exceptions routed to approvers with reasons
- Approvals
  - Thresholds by amount/customer/discount; multi-level approvals; SoD enforcement

---

## 6. Receivables Tracking, Statements & Aging
- Tracking
  - Outstanding per invoice; balances by customer; status (pending/partial/overdue/paid)
- Statements
  - Opening, invoices, receipts, credits, closing; email/export; multi-branch consolidation
- Aging
  - Buckets: 0–30, 31–60, 61–90, 91–120, 120+
  - Filters by customer/branch; priority view for collections

---

## 7. Collections & Dunning
- Dunning Policies
  - Stages (friendly, reminder, final, legal); templates per stage; frequency controls
- Actions
  - Email/SMS reminders; call tasks; promise-to-pay tracking; dispute management
- KPIs
  - Promise kept rate; response rate; days to collect

---

## 8. Receipts & Allocations
- Receipts
  - Modes: cash, bank transfer, cheque, card, gateway; references and attachments
  - Posting: Dr Bank/Cash (or Receipt Clearing); Cr AR Control
- Allocation
  - Auto-apply by oldest, exact match, or manual; partial allocations; multi-invoice receipts
- Reconciliation
  - Link to bank reconciliation; handle rejects/chargebacks

---

## 9. Advances & Deposits
- Recording
  - Dr Bank/Cash; Cr Customer Advance
- Application
  - Suggest on invoice creation; support refunds (Dr Customer Advance; Cr Bank)
- Reporting
  - Unapplied advances by customer with aging

---

## 10. Recurring Invoices & Subscriptions
- Schedules
  - Frequency, start/end, proration rules; auto email/pdf
- Posting
  - Auto-generate and post as per schedule; renewal notifications

---

## 11. Write-offs & Bad Debt Provisioning
- Write-offs
  - Approval workflow; thresholds; posting: Dr Bad Debt Expense; Cr AR Control
- Provisioning
  - Expected credit loss model or rule-based; periodic journal entries; reversal policy

---

## 12. Multi-Currency & Branch Handling
- Multi-Currency
  - Document currency with locked rate; realized FX on settlement; revaluation handled in GL
- Branch/Company
  - Postings scoped by branch/company; central receipt handling with inter-branch allocations

---

## 13. Integrations & APIs
- Integrations
  - Sales/Orders, Delivery/Inventory, Bank/Cash, Tax engine, CRM (collections tasks)
- APIs
  - Invoices: create/validate/approve/post; list/filter; attach
  - Receipts: create/allocate/approve/post; batch operations
  - Credits/Returns: create/apply/list
  - Webhooks: invoice.posted, receipt.posted, credit.applied, dunning.stage.changed

---

## 14. Reports & KPIs
- Reports
  - AR Aging, Customer Statements, Unapplied Advances, Credit Note Register, Dunning Effectiveness
- KPIs
  - DSO (Days Sales Outstanding), Collection Rate, Dispute Rate, Write-off %, Promise-to-Pay adherence

---

## 15. Validations & Edge Cases
- Validations
  - Unique invoice per customer; tolerance checks for price/qty/tax; mandatory mappings; balanced postings
  - Dimension completeness; period locks; duplicate detection
- Edge Cases
  - Prepayments before invoice; credit-only accounts; currency adjustments on partial settlements
  - Backdated invoices after close (require approvals); subscriptions proration and cancellations

---
