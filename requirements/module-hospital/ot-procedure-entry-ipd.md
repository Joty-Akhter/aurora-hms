# OT Procedure Entry (Indoor / IPD – Operation Theatre)

## 1. Overview

**Description**  
The OT Procedure Entry module records and manages **Operation Theatre (OT) procedures for admitted IPD patients**. It captures OT scheduling, room allocation, surgeon and assistant details, actual start/finish times, charge calculation, and billing integration. It is the **system of record** for:
- OT-related billing charges  
- Surgeon/assistant payment and settlement  
- Medico‑legal procedure records  
- OT utilization and performance reporting

**Scope**  
This module operates **only for patients with an active IPD admission** and is used primarily by **OT staff, anesthetists, and hospital administrators**, with downstream consumers in Billing, Finance, and MIS.

---

## 2. Pre‑Requisites

Before using OT Procedure Entry, the following must exist and be correctly configured:

- **Active Patient Admission (IPD)**  
  - Patient has an active IPD admission (see `admission-ipd.md`).  
  - Admission is linked to a bed/ward and an Under Doctor.
- **Bed Allocation**  
  - Valid bed assigned; bed status = `Occupied`.  
- **Doctor Master (Surgeon & Assistants)**  
  - Surgeons, anesthetists, and assistant doctors are defined in Doctor Master and marked Active.  
- **OT Room Management**  
  - OT rooms defined in OT Room / Location master with status and capacity.  
- **Operation / Procedure Master**  
  - Catalog of standard operations/procedures with codes, descriptions, and base tariffs.  
- **User Role & Permissions**  
  - Roles such as **OT Staff**, **OT Admin**, or **Hospital Admin** granted access to create/edit OT entries.  
  - Billing/Finance roles have read-only access for reconciliation and settlement.

---

## 3. Features

- **Admission-wise OT Procedure Entry**  
  - Each OT entry is tied to a specific active IPD admission.  
- **Auto-fetch Patient & Bed Details**  
  - Selecting Patient/Admission automatically fills patient demographics, bed, and Under Doctor.  
- **OT Room Scheduling**  
  - Capture OT date, room, and scheduled time window; prevent double-booking conflicts.  
- **Operation Time Tracking**  
  - Record actual start and end time; auto-calculate total OT duration.  
- **Multi-doctor Involvement**  
  - Support multiple surgeons/assistants/anesthetists with role-specific charges and payment tags.  
- **Automatic OT Charge Calculation**  
  - Compute OT charges based on operation type, OT duration, and doctor roles as per tariff rules.  
- **Doctor-wise Payment Tagging**  
  - Flag which line items are considered for surgeon/assistant settlement and track payment status.  
- **Invoice / IPD Billing Linkage**  
  - Push OT charges to IPD Billing and lock entries after invoicing.  

---

## 4. User Interface – OT Procedure Entry

### 4.1 Form View – OT Procedure Entry

#### 4.1.1 Patient Information Section

Fields:
- **Patient ID (Search)**  
  - Type: Search (supports Admission ID / IPD No / UHID).  
  - Required: Yes.  
  - Behavior:
    - On selection, system auto-fetches:
      - Patient Name  
      - Address  
      - Gender  
      - Age  
      - Admission Date  
      - Bed No / Ward / Room  
      - Under Doctor Code / Name  
    - Only **active IPD admissions** are allowed.
- **Patient Name (Auto)** – read-only.  
- **Address (Auto)** – read-only.  
- **Gender (Auto)** – read-only.  
- **Age (Auto)** – read-only.  
- **Admission Date (Auto)** – read-only, from admission record.  
- **Bed No (Auto)** – read-only, from current bed allocation.  
- **Under Doctor Code / Name (Auto)** – read-only, from admission.

#### 4.1.2 Operation Theatre Information

Fields:
- **OT Date \***  
  - Type: Date  
  - Required: Yes  
  - Validation: Must be a valid date; typically ≥ admission date.  
- **OT Room No \***  
  - Type: Lookup (OT Room master)  
  - Required: Yes  
  - Validation: Room must be Active and available for OT use.  
- **OT Serial No (Auto)**  
  - Type: Auto-generated string/number per configured sequence.  
  - Required: Yes  
  - Behavior: Assigned on first save; globally unique or room/date scoped as per configuration.  
- **Start Time \***  
  - Type: Time  
  - Required: Yes  
  - Validation:
    - Must be a valid time.  
    - In combination with OT Date and Room, used for overlap checks.  
- **End Time \***  
  - Type: Time  
  - Required: Yes (except where OT is in progress or if configuration allows late entry).  
  - Validation:
    - Must be > Start Time.  
- **Total Time (Auto-calculated)**  
  - Type: Duration (HH:MM or minutes).  
  - Behavior:
    - Calculated as `End Time – Start Time`.  
    - Updated on change of Start/End Time.  
- **Operation Name \***  
  - Type: Lookup (Operation/Procedure master).  
  - Required: Yes.  
- **Operation Type**  
  - Type: Enum/dropdown (e.g., Major, Minor, Super Major, Emergency, Day Care).  
  - Required: As per tariff rules; used to derive charge rules.  
- **Primary Surgeon Name \***  
  - Type: Lookup (Doctor Master – surgeon).  
  - Required: Yes.  

#### 4.1.3 OT Procedure / Doctor Grid

The grid captures line-level combinations of procedure and doctor roles for charging and payment.

Columns:
- **Code**  
  - Description: Procedure or Doctor code.  
  - Behavior: Can reference:
    - Operation/procedure code, or  
    - Doctor code depending on row type (if mixed model is used).  
- **Description**  
  - Description: Procedure description or role (e.g., Primary Surgeon, Assistant, Anesthetist).  
  - Required: Yes.  
- **Dr. Code**  
  - Description: Doctor code from Doctor Master.  
  - Required: For doctor-based rows.  
- **Dr. Name**  
  - Description: Auto-filled doctor name; read-only.  
- **Charge**  
  - Type: Decimal  
  - Required: Yes (if chargeable).  
  - Validation: ≥ 0.  
- **NoU (Number of Units)**  
  - Type: Integer/Decimal  
  - Required: Yes (default 1).  
  - Validation: > 0.  
- **Total**  
  - Type: Decimal (auto-calculated)  
  - Behavior: `Total = Charge × NoU`.  
- **For Dr.**  
  - Type: Boolean flag  
  - Description: Indicates that this line is considered for **doctor payment/settlement**.  
- **Paid**  
  - Type: Enum / Boolean  
  - Values: e.g., `Pending`, `Partially Paid`, `Paid` (configurable).  
  - Behavior: Updated by Settlement/Finance module, usually read-only in OT screen.  
- **Action**  
  - UI controls: **Add**, **Edit**, **Delete** row as per permissions and billing state.

#### 4.1.4 Footer Section

Fields:
- **Remarks**  
  - Type: Long text; optional; used for intraoperative notes or administrative remarks (not a substitute for full clinical OT note).  
- **Grand Total (Auto)**  
  - Type: Decimal  
  - Behavior: Sum of `Total` for all grid rows.  

Actions:
- **Save**  
  - Create or update OT Entry; perform validation, overlap checks, and calculation.  
- **List**  
  - Navigate to OT Procedure List (history).

---

## 5. OT Procedure List – List View

### 5.1 Columns

- Ref No (OT Ref / OT Serial No)  
- Ref Date  
- Patient ID / Admission ID  
- Patient Name  
- Bed No / Ward  
- OT Date  
- Start Time  
- Finish Time (End Time)  
- Invoice No / Billing Status  
- Edit (subject to rules)  
- Delete / Cancel (subject to rules)  

### 5.2 Actions & Filters

- **Actions per row**:
  - View details  
  - Edit (only before invoice generation or within allowed window)  
  - Cancel OT entry (if not billed and per policy)  
- **Filters**:
  - Date range (OT Date)  
  - OT Room  
  - Surgeon  
  - Patient / Admission ID  
  - Billing Status (Not billed / Partially billed / Billed)  

---

## 6. Business Rules

1. **Active Admission Required**  
   - Patient must have an active IPD admission to create an OT entry.  
   - Discharged or cancelled admissions are not eligible.

2. **OT Room Double Booking Prevention**  
   - For a given OT Room and OT Date, the system must ensure that the **time interval (Start–End)** for a new entry does not overlap with any other **non-cancelled** OT entry for the same room.  
   - On overlap, the system should:
     - Block save, and  
     - Show a clear “Time overlap / room already booked” error.

3. **Time Consistency**  
   - End Time must be **strictly greater** than Start Time.  
   - Total Time is always derived from Start and End; user cannot directly edit duration.  

4. **Charge Calculation Rules**  
   - Base OT charges are derived from:
     - Operation Type  
     - Operation Name / Procedure Code  
     - OT duration (if duration-based tariffs exist)  
   - Doctor-wise charges (for surgeon/assistant/anesthetist) are calculated based on:
     - Doctor role in procedure  
     - Configured surgeon/assistant fee schedules.  

5. **Invoice Locking**  
   - Once OT charges have been **invoiced in IPD Billing**, the corresponding OT entry becomes **read-only** for:
     - Operation details  
     - Times and OT Room  
     - Charge amounts  
   - Any corrections require:
     - A controlled amendment/voiding workflow, and  
     - Appropriate billing adjustments.

6. **Doctor Payment Flag & Settlement**  
   - The **For Dr.** flag determines which lines are exposed to the **OT/Doctor Settlement** module.  
   - The **Paid** status is updated only by Settlement/Finance roles; OT users generally cannot mark payments as Paid.  

7. **Deletion Constraints**  
   - Deletion (hard delete) is **not allowed** once any billing or settlement activity exists.  
   - Prior to billing, a soft **Cancel** may be allowed, with mandatory reason and full audit trail.

---

## 7. Edge Cases & Exceptions

- **Start Time Entered, End Time Missing**  
  - Save should be blocked if End Time is mandatory per configuration.  
  - If “in-progress” mode is supported, OT entry may be saved with Start Time only and marked `In Progress`; End Time must be captured before billing.

- **Emergency OT Outside Schedule**  
  - Emergency procedures may bypass standard OT schedule validations (e.g., outside normal hours) but:
    - Must be explicitly flagged as `Emergency`.  
    - May have special tariff rules and audit requirements.  

- **Same Doctor Added Multiple Times**  
  - For the same role, on the same OT entry:
    - System should warn or block duplicate doctor lines as per configuration.  
  - For different roles (e.g., Surgeon + Anesthetist):
    - Allowed if clinically and contractually valid.  

- **OT Entry Created but Invoice Not Generated**  
  - OT entries can remain in `Not Billed` state; billing team can later pick them up from IPD Billing.  
  - Reports should allow tracking of **unbilled OT entries** for financial completeness.  

- **Operation Cancelled After Save**  
  - If operation is cancelled after OT entry is created but before billing:
    - Entry status can be changed to `Cancelled` with mandatory reason.  
    - No OT charges should be posted.  
  - If cancellation occurs after partial or full billing:
    - Handled via billing reversal/refund policies and clearly audited.

---

## 8. Error Handling & Validation

- **Invalid Patient / Admission ID**  
  - Block OT entry creation; show “Invalid or inactive admission” message.  

- **Missing Mandatory OT Fields**  
  - OT Date, OT Room, Start Time, End Time, Operation Name, and Primary Surgeon are mandatory (unless configuration says otherwise).  
  - Inline validation messages must clearly indicate missing fields.  

- **Time Overlap Error**  
  - Detect overlapping OT bookings for the same room and date; show a clear error with reference to conflicting booking (Ref No, time).  

- **Duplicate OT Serial Number**  
  - Serial No must be unique; system should generate sequence server-side to avoid duplicates.  

- **Unauthorized Edit Attempt**  
  - If user lacks permission or record is billing-locked:
    - Do not allow edits.  
    - Show “Access denied or record locked due to billing” message.  

---

## 9. System Actions & Integrations

- **Auto-generate OT Serial / Reference Number**  
  - On first save, generate OT Ref No as per hospital’s numbering scheme (room-wise, date-wise, or global).  

- **Auto-calculate Duration & Charges**  
  - Compute Total Time and all line-level totals and grand total on-the-fly.  

- **Push OT Charges to IPD Invoice**  
  - On marking entry as ready for billing (or on Save, as configured), OT charges are pushed as line items to IPD Billing linked to:
    - Admission ID / IPD No  
    - Patient and primary surgeon / department  

- **Record Locking After Billing**  
  - Once linked billing entries are finalized, OT entry becomes read-only except via special amendment workflows.  

- **Log OT Usage for Reporting**  
  - Store data required for:
    - OT utilization (room-wise, surgeon-wise, specialty-wise)  
    - Turnaround times and occupancy  
    - Clinical and operational MIS  

---

## 10. Notes & Compliance

- OT data is **critical and audit-sensitive**:
  - Must be **tamper-evident** and fully auditable.  
  - Aligned with medico‑legal documentation standards and hospital policies.  
- **Deletion is not allowed after invoice generation**:
  - Only cancellation/void flows with full audit are permitted.  
- Primary consumers:
  - **IPD Billing** (charge calculation & invoices)  
  - **OT Settlement / Doctor Payment**  
  - **MIS, Quality & Compliance** teams for utilization and audit reports.  

---

## 11. Data Fields (Summary)

### 11.1 Basic Identification

| Field Name | Type   | Required | Description               | Validation / Notes                      |
|-----------|--------|----------|---------------------------|-----------------------------------------|
| OT Ref No | String | Yes      | OT reference/serial no.  | Auto-generated; unique per configuration |
| Patient ID | UUID  | Yes      | IPD admission reference   | Must reference **active** IPD admission |
| Admission ID / IPD No | UUID / String | Yes | Admission identifier | Read-only; derived from Patient selection |
| OT Date   | Date   | Yes      | Operation date            | Valid date; usually ≥ admission date    |
| OT Room   | Enum   | Yes      | OT room identifier        | From OT Room master; must be Active     |

### 11.2 Time & Charge

| Field Name  | Type  | Required | Description         | Validation / Notes                 |
|------------|-------|----------|---------------------|------------------------------------|
| Start Time | Time  | Yes      | OT start time       | Must be present; valid time        |
| End Time   | Time  | Yes      | OT end time         | Must be > Start Time               |
| Total Time | Time/Duration | Auto | Calculated duration  | System-calculated only             |
| Charge     | Decimal | Yes (per line) | OT/doctor line charge | ≥ 0; derived or manually set per rules |
| Grand Total | Decimal | Auto | Sum of all line charges | System-calculated only             |

