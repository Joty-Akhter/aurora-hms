## 🔹 Sales & Customer Management – Detailed Requirements

### Table of Contents
- 1. Purpose & Scope
- 2. Customer Master & Accounts
- 3. Sales Orders & Items
- 4. Shipments & Delivery
- 5. Invoices & Payments
- 6. Customer Interactions & Feedback
- 7. Reporting & KPIs
- 8. Integrations
- 9. Validations & Edge Cases

---

### 1. Purpose & Scope
- Manage customers, orders, shipments, invoicing, and payments with clear integrations to Inventory and Accounting

---

### 2. Customer Master & Accounts
#### 2.1 Customer Master
- Customer ID/Name/Code, Contacts, Address, Type, Credit Limit, Terms, Tax IDs, Status, Created/Updated
#### 2.2 Customer Accounts
- Account ID, Customer link, Balance, Outstanding, Credit usage, Last payment, Status, Notes

---

### 3. Sales Orders & Items
#### 3.1 SO Header
- SO ID/Number, Customer, Order/Expected dates, Status, Ordered/Approved by, Amount/Currency/Tax, Terms, Notes
#### 3.2 SO Items
- Item ID, Product/SKU, Qty/UoM, Price/Tax/Discount, Expected date, Shipped Qty, Status, Notes
#### 3.3 Flows
1) Create SO (manual/API) → Approve → Allocate stock
2) Pick/Pack/Ship; update shipments and tracking
3) Handle partials/returns; close order

---

### 4. Shipments & Delivery
- Shipment ID/Number, SO link, Dispatch location, Shipped/Expected dates, Carrier/Tracking, Delivered/Received by, Status, Notes

---

### 5. Invoices & Payments
- Invoice: ID/Number, SO/Customer, Dates, Amount/Currency/Tax, Payment Status, Notes
- Payment: ID, Invoice/Customer, Dates, Amount/Currency, Method, Reference, Approved/Processed by, Notes

---

### 6. Customer Interactions & Feedback
- Interactions (call/email/meeting/ticket): timestamps, notes, follow-ups, owner, status
- Feedback/Complaints: type, description, status, SLA, resolution notes

---

### 7. Reporting & KPIs
- Reports: sales summary, aging, collections, delivery/shipments, feedback
- KPIs: order cycle time, on-time delivery %, return rate, collection efficiency

---

### 8. Integrations
- Inventory (allocations, shipments), Accounting (AR, invoices, payments), ERP/CRM; sync with timestamps and error logs

---

### 9. Validations & Edge Cases
- Validations: customer credit/terms checks, tax code presence, duplicate SO detection
- Edge Cases: backorders, split shipments, dropship, prepayments and credit memos

