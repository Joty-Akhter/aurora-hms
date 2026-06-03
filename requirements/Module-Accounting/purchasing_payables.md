
# 🔹 Purchasing & Payables – Detailed Requirements

## Table of Contents
- 1. Purpose & Scope
- 2. Data Model & Mappings
- 3. Supplier Bills/Invoices
- 4. Credit Notes/Debit Notes & Returns
- 5. Approvals & Three-Way Match
- 6. Payables Aging & Statements
- 7. Payment Planning & Execution
- 8. Advances & Prepayments
- 9. Partial Payments & Adjustments
- 10. Multi-Currency & Branch Handling
- 11. Integrations & APIs
- 12. Reports & KPIs
- 13. Validations & Edge Cases

---

## 1. Purpose & Scope
- Manage supplier obligations from invoice capture to settlement with controls, visibility, and accurate GL postings
- In-scope: invoices/bills, returns/notes, matching, approvals, aging, payments, advances, and reconciliations
- Out-of-scope: procurement negotiation and receiving logic (covered in Procurement/Inventory), but linked

---

## 2. Data Model & Mappings
- Entities
  - Supplier: Code, Name, Addresses, Contacts, Payment Terms, Credit Limit, Default Currency, Tax IDs, Withholding profile
  - Bill/Invoice: Number, Dates (bill, due), Supplier, PO/GRN links, Lines (item/service, qty, rate, tax, discount), Currency, Dimensions
  - Credit/Debit Note: Reference to bill/PO/GRN, reason, amounts
  - Payment: Mode, Bank/Cash, Amount, Date, References (bills/notes), Batch Id
  - Advance/Prepayment: Supplier, amount, date, application status
- GL Mappings
  - AP Control Account (per supplier group or company)
  - Expense/Inventory accounts by line type; Tax payable; Withholding payable
  - Gain/Loss FX accounts; Payment/Clearing accounts (for batches)
  - Dimensions: Branch, Department, Cost Center, Project

---

## 3. Supplier Bills/Invoices
- Capture
  - Modes: manual entry, OCR/import, PO flip (from approved PO + GRN), API
  - Fields: Bill No (unique per supplier), Bill Date, Due Date/Terms, Currency, Exchange Rate, Attachments
  - Lines: item/service, qty, rate, discount, tax codes, dimensions
- Posting
  - Dr Expense/Inventory/Asset; Dr Tax Input (if applicable); Cr AP Control (by supplier)
  - Multi-currency: book transaction currency and functional amounts; lock document rate when required
- Statuses
  - Draft → Submitted → Approved → Posted → Partially Paid → Settled → Cancelled/Void (with audit)

---

## 4. Credit Notes/Debit Notes & Returns
- Credit Notes (supplier issued)
  - Reduce liability: Dr AP Control; Cr Expense/Inventory/Tax as applicable
  - Reference to bill(s); reason codes; attachments
- Debit Notes (buyer issued for returns/short receipt)
  - Same effect on liability; integration with Inventory for stock returns
- Application
  - Apply to open bills; allow partial application; track unapplied credits

---

## 5. Approvals & Three-Way Match
- Matching
  - 2-way: PO ↔ Invoice (price/qty within tolerance)
  - 3-way: PO ↔ GRN ↔ Invoice (qty received vs billed; price variances)
  - Exceptions routed to approvers with reason capture
- Approvals
  - Thresholds by amount/supplier/category; multi-level approvals; SoD enforcement
- Holds
  - Payment hold on unmatched/exception invoices until resolved

---

## 6. Payables Aging & Statements
- Aging Buckets
  - 0–30, 31–60, 61–90, 91–120, 120+
- Supplier Statements
  - Opening balance, bills, payments, notes, closing balance; multi-currency view
- Features
  - Filters by supplier/branch; export/email statements; consolidate across branches

---

## 7. Payment Planning & Execution
- Planning
  - Payment proposals by due date, cash position, priority, discounts; simulate impact
- Execution
  - Batch payments (multiple suppliers); payment file generation (bank formats) or internal run
  - Posting on release: Dr AP Control; Cr Bank (or Cr Payment Clearing if using batches)
- Reconciliation
  - Match payments with bank statements; resolve rejects/chargebacks

---

## 8. Advances & Prepayments
- Recording
  - Dr Supplier Advance; Cr Bank
- Application
  - Apply to bills; auto-suggest on capture; support refunds (Dr Bank; Cr Supplier Advance)
- Reporting
  - Unapplied advances by supplier with aging

---

## 9. Partial Payments & Adjustments
- Partial Payments
  - Track remaining balance; support multiple payments per bill
- Discounts & Write-offs
  - Early payment discounts; small balance write-off with thresholds/approvals
- FX Differences
  - Recognize realized FX on settlement for foreign currency suppliers

---

## 10. Multi-Currency & Branch Handling
- Multi-Currency
  - Document currency with locked rate; functional/reporting conversion; realized FX on settlement
- Branch/Company
  - Postings scoped by branch/company; inter-branch allocations if central AP pays on behalf

---

## 11. Integrations & APIs
- Integrations
  - Procurement/PO, Inventory/GRN, Bank/Cash (payments), Tax engine, OCR
- APIs
  - Bills: create/validate/approve/post; list/filter; attach documents
  - Payments: propose/create/approve/post; batch operations
  - Notes/Returns: create/apply/list
  - Webhooks: bill.approved, bill.posted, payment.posted, note.applied

---

## 12. Reports & KPIs
- Reports
  - AP Aging, Supplier Statements, Unapplied Advances, Credit Note Register, Payment Batches
- KPIs
  - DPO (Days Payable Outstanding), % Invoices Matched, Early Payment Discounts Captured, Payment Rejections

---

## 13. Validations & Edge Cases
- Validations
  - Unique invoice per supplier; tolerance checks for price/qty/tax; mandatory mappings
  - Balanced postings; dimension completeness; period lock respect; duplicate detection
- Edge Cases
  - Prepayments before invoice; negative invoices/credit-only vendors; disputes and re-bills
  - Backdated invoices after close (require approvals); multi-currency adjustments on partial payments

---
