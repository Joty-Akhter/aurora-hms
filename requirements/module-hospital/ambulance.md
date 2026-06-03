# Ambulance Module

## 1. Overview

**Description**  
The Ambulance Module manages ambulance trip booking, billing, collection, and due tracking for patient transport services. It integrates with Patient Registration, Billing, and the Driver & Vehicle configuration in Hospital Operations.

**Objectives**

- Record ambulance trips with patient, route, distance, and charge details.
- Support flexible charging (distance-based, flat rate, trip type).
- Maintain ambulance ledger for charges and collections.
- Integrate with OPD/IPD billing for due collection and corporate/insurance handling.
- Provide trip and revenue reporting.

**Scope**

- Ambulance trip/booking entry.
- Charge calculation (distance, time, vehicle type, trip type).
- Ambulance ledger (charges, collections, dues).
- Due collection (standalone or as part of patient release/indoor due).
- Integration with Billing and Clinical Chart.
- Reporting and audit.

**Pre-requisites**

- **Driver & Vehicle Master**, **Ambulance Type Master**, **Trip Type Master** – Configured in [Hospital Operations](hospital-operations.md) (Section 3.11).
- **Clinical Chart** – Ambulance charge items with rates (see [clinical-tariff-from-clinical-txt](clinical-tariff-from-clinical-txt.md)).

---

## 2. Master Data (Reference from Hospital Operations)

### 2.1 Ambulance Type Master

- BLS (Basic Life Support), ALS (Advanced Life Support), ICU Ambulance, Neonatal Ambulance, General Transport.
- Per type: Equipment, default tariff, minimum charge.

### 2.2 Trip Type Master

- Patient Transfer (internal), Inter-facility Transfer, Emergency Call, Supply/Logistics Trip.
- Per type: Billing rules (billable vs non-billable), Payer defaults (patient, corporate, internal).

### 2.3 Vehicle & Driver Master

- Vehicles: Registration number, type, capacity, equipment.
- Drivers: License, contact, shift, assigned vehicle.

---

## 3. Core Workflows

### 3.1 Ambulance Trip / Booking Entry

- **Trip Record**
  - **Receipt No \*** – Unique trip/receipt number.
  - **Receipt Date \***, **Patient/Reg No \***, **Patient ID** (if registered), **Patient Name \***
  - **Contact** – Mobile, Address.
  - **Gender**, **Age**
  - **Driver Name \***, **Ambulance No \*** (Vehicle registration)
  - **Destination From \***, **Destination To \***
  - **Meter From**, **Meter To** – Odometer readings (optional; for distance calculation).
  - **Total Distance \*** – KM (manual or derived from meter).
  - **From Date \***, **From Time \***, **To Date \***, **To Time \*** – Trip start and end.
  - **Trip Type** – Emergency, Inter-facility, Internal transfer, etc.
  - **Ambulance Type** – BLS, ALS, ICU, etc.
  - **Referring Doctor** – Ref Dr Code, Ref Dr Name (optional).
  - **Consultant** – Consultant Code, Consultant Name (optional).
  - **Remarks**

### 3.2 Charge Calculation

- **Total Charge \*** – Calculated based on:
  - **Distance-based** – Rate per KM × Total Distance (with minimum charge).
  - **Flat rate** – Fixed charge per trip type or ambulance type.
  - **Time-based** – Hourly rate (optional).
  - **Clinical Chart** – Ambulance charge items selected by distance slab or trip type (e.g., 500, 700, 1,000, 1,500, 2,000 for different distances).
- **Less** – Discount applied (with reason and approval if above threshold).
- **Net Payable** – Total Charge − Less.
- **Advance / Collection at Trip**
  - Cash, Card, Cheque amounts.
  - Card No, Card Bank, Cheque No, Cheque Bank, Cheque Date.
- **Due Amount** – Net Payable − (Cash + Card + Cheque + Advance).

### 3.3 Ambulance Ledger

- **Ledger Entry Types**
  - **Charge** – Trip charge posted (from Ambulance Trip entry).
  - **Collection** – Payment received (cash, card, cheque).
  - **Adjustment** – Less/discount, write-off (with approval).
  - **Patient Release / Indoor Due Collection** – Ambulance due collected as part of IPD discharge settlement.
- **Ledger Fields**
  - Ref No, Ref Date, Receipt No (trip reference), Patient ID, Patient Name.
  - Total Charge, Less, Receive Amt (collection), Due Amt.
  - Cash Amt, Card Amt, Cheque Amt, Prev Receipt No, Prev Receipt Date (for linking collections to trip).
  - Input From – 'Collection', 'Patient Release', 'Indoor Due Collection', etc.
  - User, Entry Date/Time, Remarks.
- **Running Balance** – Due = Sum(Total Charge − Less − Receive Amt) per trip/receipt.

### 3.4 Due Collection

- **Standalone Collection**
  - Search by Receipt No, Patient ID, Patient Name, or Reg No.
  - Display outstanding ambulance dues.
  - Enter collection (Cash, Card, Cheque); update ledger; print receipt.
- **Indoor Due Collection**
  - Ambulance dues included in IPD patient due calculation when configured.
  - Collected at discharge along with clinical and pharmacy dues.
  - Ambulance ledger updated with Input From = 'Indoor Due Collection' or 'Patient Release'.
- **OPD Due Collection**
  - Ambulance dues for OPD patients collected via OPD due collection screen.
  - Link to OPD ticket/visit where applicable.

### 3.5 Billing Integration

- Ambulance charges posted to **Ambulance Ledger** (this module).
- For OPD/IPD billing integration:
  - Ambulance charge may be added to Patient Ledger (SalesLedger equivalent) for consolidated patient due.
  - Or remain in Ambulance Ledger only, with due collection from Ambulance console.
- **Clinical Chart** – Ambulance items (e.g., Ambulance Charge 500, 700, 1,000) used for tariff and discount group.
- **Corporate / Insurance** – Trip type and patient type determine payer; charges may be billed to corporate/insurance as per Billing module rules.

---

## 4. Integration

### 4.1 Patient Registration

- Patient ID, Name, Age, Gender, Mobile, Address – from Patient Master when patient is registered.
- For walk-in/unregistered: capture manually; optional quick registration.

### 4.2 Billing

- Ambulance dues may be aggregated in **Indoor Patient Due** (see [billing.md](billing.md) – Indoor Patient Due Calculation).
- Discount and approval rules from Billing module.
- Payment modes and receipt numbering aligned with Billing.

### 4.3 Hospital Operations

- Driver & Vehicle Master, Ambulance Type Master, Trip Type Master – used for dropdowns and charge rules.
- Fuel and maintenance (separate; may link vehicle to trip for cost tracking).

### 4.4 Accounts

- Ambulance revenue posted to GL (via Billing or direct posting as configured).
- Cost center mapping for ambulance cost/revenue analysis.

---

## 5. Reporting

- **Trip List** – By date range, patient, driver, ambulance.
- **Due Collection Report** – Outstanding ambulance dues by patient/receipt.
- **Revenue Report** – Collection vs charge by period, trip type, ambulance type.
- **Driver-wise Trip Report** – Trips and revenue per driver.
- **Vehicle-wise Utilization** – Trips per vehicle.
- Export to Excel/PDF.

---

## 6. Non-Functional & Audit

- **Audit Trail** – All trip create, edit, collection, and adjustment actions logged.
- **Role-Based Access** – Trip entry, collection, adjustment, report – separate permissions.
- **Receipt Numbering** – Unique, configurable series per branch/unit.
- **Concurrent Access** – Safe handling of collection against same trip (locking or conflict resolution).
