## 🔹 Purchasing & Supplier Management – Detailed Requirements

### Table of Contents
- 1. Purpose & Scope
- 2. Supplier Master & Products
- 3. Purchase Orders (PO) & Items
- 4. Receiving (GRN) & Quality
- 5. Invoices & Payments
- 6. Supplier Performance
- 7. Reporting & KPIs
- 8. Integrations
- 9. Validations & Edge Cases

---

### 1. Purpose & Scope
- Manage suppliers, purchase orders, receiving, invoicing, and payments with quality and performance tracking

---

### 2. Supplier Master & Products
#### 2.1 Supplier Master
- Supplier ID, Name/Company, Code, Contacts, Address, Tax IDs, Payment Terms, Lead Time, Type, Status, Created/Updated
#### 2.2 Supplier Products
- Supplier Product ID, Supplier link, Product/SKU, Supplier SKU, Unit Price/Currency, MOQ, Lead Time, Tax, Status, Created/Updated

---

### 3. Purchase Orders (PO) & Items
#### 3.1 PO Header
- PO ID/Number, Supplier, Order/Expected dates, Status, Ordered/Approved by, Amount/Currency/Tax, Terms, Notes
#### 3.2 PO Items
- PO Item ID, Product/SKU, Qty/UoM, Price/Tax/Discount, Expected Date, Received Qty, Status, Notes
#### 3.3 Flows
1) Create PO (manual/auto from reorder) → Approve → Send to supplier
2) Amendments tracked with versions; approval for price/qty changes
3) Close when fully received/invoiced; handle partials

---

### 4. Receiving (GRN) & Quality
#### 4.1 GRN
- GRN ID/Number, PO link, Received date/time, Warehouse, Receiver, Items (qty, batch/serial), Condition, Notes
#### 4.2 Quality
- QA status (pass/fail/hold), reasons; quarantine and disposition (restock/return/scrap)
#### 4.3 Flows
- Dock → Inspect/QA → Putaway → Update stock; return-to-vendor if failed

---

### 5. Invoices & Payments
#### 5.1 Supplier Invoices
- Invoice ID/Number, Supplier/PO, Dates, Amount/Currency/Tax, Payment Status, Notes
#### 5.2 Supplier Payments
- Payment ID, Invoice/Supplier, Dates, Amount/Currency, Method, Reference, Approved/Processed by, Notes
#### 5.3 Matching
- 2-way/3-way match (PO↔Invoice or PO↔GRN↔Invoice); tolerances; exception workflow

---

### 6. Supplier Performance
- On-time %, defect rate, lead time accuracy, responsiveness; last updated
- Scorecards and corrective action tracking

---

### 7. Reporting & KPIs
- Reports: PO status, GRN vs PO, invoice reconciliation, payment status, supplier performance
- KPIs: on-time delivery %, defect rate %, average lead time, PO cycle time

---

### 8. Integrations
- Inventory (GRN putaway), Accounting (AP posting, payments), ERP/BI; sync status, timestamps, error logs

---

### 9. Validations & Edge Cases
- Validations: unique PO number, supplier currency terms, tax codes set, 3-way match tolerances
- Edge Cases: partials and backorders, dropship POs, vendor-managed inventory, consignment stock

