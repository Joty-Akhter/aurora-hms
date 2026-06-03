# Hospital Module – Integration with Accounting, Inventory, and HR Services

## 1. Overview

The Hospital Module operates alongside existing platform services: **Accounting**, **Inventory**, and **HR**. This document defines the integration points, data flows, and responsibilities. The Hospital module **does not duplicate** functionality that these services provide; it consumes and produces data through defined interfaces.

**Principle:** Hospital-specific workflows (billing, pharmacy, lab, ambulance, fixed assets, etc.) own their transactional data; Accounting, Inventory, and HR own master data and enterprise-wide processes. Integration ensures consistency, auditability, and a single source of truth.

---

## 2. Accounting Service Integration

### 2.1 What Accounting Service Provides

- **Chart of Accounts (COA)** – GL structure (Assets, Liabilities, Equity, Revenue, Expenses).
- **Voucher Types** – Journal, Payment, Receipt, Contra, etc.
- **Cost Centers & Revenue Centers** – Department/unit mapping for cost and revenue allocation.
- **Bank & Cash Accounts** – For collections, refunds, and vendor payments.
- **Tax Configuration** – VAT/GST, withholding tax, service-wise tax.
- **Financial Periods** – Open/close periods; period lock for posting.
- **Voucher Posting** – Create and post financial vouchers from source transactions.

### 2.2 Hospital → Accounting: Data Flows

| Hospital Module | Trigger | Data Sent to Accounting | Purpose |
|-----------------|---------|--------------------------|---------|
| **Billing** | OPD/IPD bill creation, payment, refund, discount, write-off | Revenue GL, Receivable GL, Cash/Bank GL, Cost Center, Revenue Center | Post revenue, receivables, collections, refunds |
| **Pharmacy** | Sale, purchase receive, return, stock adjustment | Revenue GL, Stock GL, COGS GL, Creditor GL, Cash/Bank | Post sales, purchase, returns, adjustments |
| **Fixed Assets** | Asset acquisition, depreciation, disposal | Asset GL, Accumulated Depreciation GL, Depreciation Expense, P&L (gain/loss) | Post asset and depreciation entries |
| **Ambulance** | Trip charge, collection | Revenue GL, Receivable GL, Cash/Bank | Post ambulance revenue and collections |
| **Blood Bank** | Blood issue to patient | Revenue GL, Receivable GL (via Billing) | Post blood bank charges |
| **Canteen** | Sales, refunds | Revenue GL, Cash/Bank, Inventory | Post canteen revenue |
| **Hospital Main Store** | GRN, issue, return, adjustment | Stock GL, Creditor GL, COGS, Cost Center | Post store movements and valuation |

### 2.3 Configuration Requirements (Hospital Side)

- **Service/Item → GL Mapping** – Each billable service (Clinical Chart), medicine, bed type, etc. mapped to default Revenue GL and Cost Center.
- **Payment Mode → Bank/Cash Account** – Cash, Card, Cheque, bKash, etc. mapped to settlement accounts.
- **Posting Strategy** – Summarized (daily batch) vs detailed (per transaction) as configured.
- **Voucher Reference** – Hospital transaction ID (Invoice No, Receipt No, etc.) stored in voucher reference for traceability.

### 2.4 Configuration Requirements (Accounting Side)

- **Chart of Accounts** – Hospital-specific segments (Department, Service Line, Branch) where applicable.
- **Cost Centers** – ICU, OT, Laboratory, Radiology, Pharmacy, etc.
- **Revenue Centers** – OPD Consultation, IPD Bed, Diagnostics, OT, Pharmacy, Ambulance, etc.
- **Credit, Refund, Write-off Policies** – Link to Billing approval matrix.

### 2.5 Integration Patterns

- **Synchronous** – Hospital creates voucher via API; Accounting validates and posts; returns voucher number.
- **Asynchronous** – Hospital queues posting request; Accounting batch processes; Hospital polls status or receives callback.
- **Reconciliation** – Hospital maintains transaction summary; Accounting provides GL balance; periodic reconciliation by Cost Center, Revenue Center, and reference.

---

## 3. Inventory Service Integration

### 3.1 What Inventory Service Provides

- **Item Master** – Shared product/item database (medicines, consumables, equipment).
- **Store/Location Master** – Central Store, Outlet Pharmacies, Departmental stores.
- **Stock Ledger** – Quantity and valuation by item, store, batch.
- **Purchase Order, GRN** – Procurement workflow (optional; Hospital Main Store may own requisition, Inventory owns PO/GRN).
- **Stock Movement** – In, Out, Transfer, Return, Adjustment.
- **Valuation** – FIFO, Weighted Average, or other method.
- **Reorder & Min/Max** – Stock level alerts.

### 3.2 Hospital → Inventory: Data Flows

| Hospital Module | Trigger | Data Sent to Inventory | Purpose |
|-----------------|---------|--------------------------|---------|
| **Pharmacy** | Receive from supplier, transfer between pharmacies, issue to patient/department, return, adjustment | Item, Qty, Store, Batch, Expiry, Rate | Update stock ledger; maintain pharmacy stock |
| **Hospital Main Store** | Requisition approval, GRN, issue to department, return, adjustment | Item, Qty, Store, Department, Batch, Rate | Update main store stock; department issues |
| **Blood Bank** | Blood collection, component preparation, issue, discard | Unit/Component, Qty, Location, Expiry | Track blood and component stock (if Inventory supports special item types) |
| **Fixed Assets** | Asset received via purchase | Asset item, Qty, Location (optional) | Create asset record; some assets may be tracked in store before capitalization |
| **Nurse Module** | Medicine requisition, return | Item, Qty, Ward/Department | Issue/return to department; Inventory may track ward stock |

### 3.3 Inventory → Hospital: Data Flows

| Inventory Service | Data Provided to Hospital | Purpose |
|-------------------|---------------------------|---------|
| **Stock Balance** | Item, Store, Qty, Batch, Expiry | Real-time stock check for pharmacy dispensing, requisition validation |
| **Item Master** | Item ID, Code, Name, Unit, Category, Reorder Level | Dropdowns, validation, pricing lookup |
| **GRN / Purchase Receipt** | Receipt ID, Item, Qty, Batch, Rate | Trigger pharmacy/store receive; Fixed Assets asset creation |
| **Low Stock Alert** | Item, Store, Current Qty, Reorder Level | Dashboard, requisition suggestions |

### 3.4 Configuration Requirements

- **Item Master** – Shared between Pharmacy, Main Store, and Inventory; Hospital modules reference Item ID.
- **Store Mapping** – Central Store, OPD Pharmacy, IPD Pharmacy, Ward stores, etc. mapped in Inventory.
- **Batch/Expiry** – Required for medicines; optional for consumables.
- **Valuation Method** – Aligned with Accounting (FIFO/Weighted Average).

### 3.5 Integration Patterns

- **API** – Hospital calls Inventory API for stock check, receive, issue, transfer.
- **Event-Driven** – Inventory publishes stock update events; Hospital subscribes for real-time UI updates.
- **Shared Database** – Where Inventory and Hospital share schema; direct table access with defined boundaries (less preferred for microservices).

---

## 4. HR Service Integration

### 4.1 What HR Service Provides

- **Employee Master** – Employee ID, Name, Designation, Department, Grade, Contact.
- **Department Master** – Organizational structure.
- **Designation Master** – Job titles and grades.
- **Attendance & Leave** – Shift, roster, leave balance, leave applications.
- **Payroll** – Salary structure, components, pay register (if in scope).
- **Approval Matrix** – Approvers by department, grade, or role.

### 4.2 Hospital → HR: Data Flows

| Hospital Module | Trigger | Data Sent to HR | Purpose |
|-----------------|---------|-----------------|---------|
| **Doctor Module** | Doctor registration | Employee ID, Doctor-specific attributes | Link doctor to employee for scheduling, payroll |
| **Nurse Module** | Nurse assignment to ward | Employee ID, Ward, Shift | Roster and duty display |
| **Ambulance** | Driver assignment to trip | Employee ID (driver), Trip details | Driver utilization, payroll linkage |
| **Hospital Operations** | User creation | Employee ID, Role, Department | Map system user to employee |
| **Billing / Approval** | Discount, refund, write-off approval | Approver Employee ID | Approval audit; may use HR approval matrix |

### 4.3 HR → Hospital: Data Flows

| HR Service | Data Provided to Hospital | Purpose |
|------------|---------------------------|---------|
| **Employee Master** | Employee ID, Name, Designation, Department, Grade | Doctor/Nurse/Staff dropdowns; approval matrix |
| **Department Master** | Department ID, Name, Parent | Department hierarchy; cost center mapping |
| **Roster / Duty** | Employee, Date, Shift, Ward/Unit | Dashboards; nurse station; doctor availability |
| **Leave Data** | Employee, Leave dates, Type | Block doctor appointments; nurse scheduling |
| **Designation / Grade** | Grade, Designation | Eligibility for benefits; approval levels |

### 4.4 Configuration Requirements

- **Employee ↔ User Mapping** – Hospital user account linked to HR Employee ID for single sign-on and role resolution.
- **Department Mapping** – Hospital departments (ICU, OT, Lab) aligned with HR departments for reporting.
- **Doctor/Staff as Employee** – Doctors and clinical staff are employees; HR owns master; Hospital references for scheduling, billing (doctor-wise charges), and productivity.

### 4.5 Integration Patterns

- **API** – Hospital fetches employee, department, roster, leave via HR API.
- **Sync** – Periodic sync of employee and department master to Hospital DB for performance (with HR as source of truth).
- **SSO / Identity** – User authentication may be delegated to HR/Identity provider; Hospital receives employee context in token.

---

## 5. Summary Matrix

| Service | Hospital Consumes | Hospital Produces | Key Modules |
|---------|-------------------|-------------------|-------------|
| **Accounting** | COA, Cost Center, Revenue Center, Bank accounts, Period status | Vouchers (revenue, receivable, cash, stock, asset, depreciation) | Billing, Pharmacy, Fixed Assets, Ambulance, Blood Bank, Canteen, Main Store |
| **Inventory** | Item master, Stock balance, GRN | Receive, Issue, Transfer, Return, Adjustment | Pharmacy, Main Store, Blood Bank, Nurse |
| **HR** | Employee, Department, Roster, Leave, Approval matrix | Doctor/Nurse linkage, Trip assignment, User–Employee mapping | Doctor, Nurse, Ambulance, Hospital Operations, Billing |

---

## 6. Non-Functional Considerations

- **Idempotency** – Accounting voucher creation must be idempotent (same hospital transaction ID → same voucher or skip).
- **Error Handling** – If Accounting/Inventory/HR is unavailable, Hospital may queue and retry; or block transaction with clear user message.
- **Audit** – All integration calls logged (request, response, status) for troubleshooting.
- **Versioning** – API contracts versioned; Hospital and services agree on supported versions.
- **Security** – Service-to-service authentication (e.g., API key, OAuth); sensitive data encrypted in transit.

---

## 7. Related Documentation

- [Hospital Operations](hospital-operations.md) – Section 3.5 HR & Accounts Configuration (Integration)
- [Billing](billing.md) – Section 6 Integration with Other Modules
- [Pharmacy](pharmacy.md) – Integration with Store/Inventory and Accounts
- [Fixed Assets](fixed-assets.md) – Section 5 Integration (Accounting, Inventory)
- [Ambulance](ambulance.md) – Section 4 Integration (Accounts)
- [Technical Requirements](technical-requirements.md) – Architecture and API design
