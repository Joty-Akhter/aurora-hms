# Nurse Module (Indoor / IPD)

## 1. Overview

**Description**  
The Nurse Module manages all nursing and bedside operational activities for **indoor (IPD) patients** after admission. It enables nurses and ward staff to place and track lab and medicine requisitions, process medicine returns, record patient follow-up observations, and monitor real-time bed status, in close integration with Admission, Lab, Pharmacy, Bed Management, and Billing modules.

**Scope**  
This module is available **only for patients with an active IPD admission** and is primarily used by **Nurse / Ward Nurse** roles, with limited participation from **Doctors** (e.g., Doctor Visit Entry).

**Goals**
- Ensure **timely execution** of doctors’ orders (labs, medicines, procedures).  
- Maintain **accurate, time-stamped nursing documentation** for medico-legal and clinical purposes.  
- Provide **real-time visibility** into bed occupancy and patient location.  
- Integrate seamlessly with **IPD Billing**, **Lab**, **Pharmacy**, and **Bed Management** so that all clinical actions are properly chargeable and auditable.

---

## 2. Pre‑Requisites

### 2.1 Patient Admission
- Patient must have an **Active IPD Admission**.  
- A valid **Admission ID / IPD No** exists and is linked to:
  - Patient ID / Registration No  
  - Bed / Ward / Room  
  - Under Doctor / Department  

### 2.2 Bed Management
- A **Bed is assigned** to the patient.  
- Bed status must be `Occupied` and linked to the active Admission.  
- Bed configuration and tariffs are managed in the Bed Management module.

### 2.3 User Roles & Permissions
- Logged-in user must have an appropriate role:
  - **Nurse / Ward Nurse** – primary user of Nurse Module screens.  
  - **Doctor** – for Doctor Visit Entry and viewing nurse notes.  
  - **Administrator** – for specific bed status overrides (e.g., maintenance).
- Access control must ensure:
  - Nurses can **create and update** nursing records within allowed timeframes.  
  - Doctors can **view** nursing documentation, and enter doctor visits, but cannot modify nurse-entered notes.  
  - Billing users can **view read‑only** information from the Nurse Module.

### 2.4 Doctor Assignment
- `Under Doctor` must be assigned at Admission.  
- For Doctor Visit Entry, the visiting doctor must exist in the Doctor master and be active.

---

## 3. Nurse Module – Menu Structure

The Nurse Module exposes the following menu items, usually per ward/bed context:

1. **Lab Requisition (Indoor Patient-wise)**  
2. **Medicine Requisition (Indoor Patient-wise)**  
3. **Medicine Return (Indoor Patient-wise)**  
4. **Patient Follow-Up (Nurse Notes & Vitals)**  
5. **Indoor Bed Status Check (Graphical View)**  
6. **Doctor Visit Entry (Indoor Patient)** – often grouped under clinical/doctor menus but functionally part of post-admission IPD workflows.

Each menu item is accessible only when:
- The patient is selected and has **Active IPD Admission**, or  
- The context is a **ward/bed** from which an admitted patient is selected.

---

## 4. Lab Requisition (Indoor)

### 4.1 Purpose
To request **laboratory diagnostic tests** for admitted patients based on the doctor’s orders, and track their status through the Lab workflow.

### 4.2 Key Features
- **Patient-wise Lab Requisition** tied to Admission ID.  
- **Test selection** from Diagnostic / Lab Test Master.  
- **Urgent test marking** to prioritize in Lab processing.  
- **Auto-link with Lab Workflow**:
  - Requisitions feed into Lab sample collection and result entry screens.  
- **Requisition status tracking**:
  - `Pending`, `Sample Collected`, `In Progress`, `Completed`, `Cancelled` (status set by Lab module).

### 4.3 User Interface

#### 4.3.1 Form View – Lab Requisition (Indoor)

**Identification Section**
- **Patient ID (Search)**  
  - Type: Search / scan (supports Admission ID, UHID, or Bed-based lookup).  
  - Required: Yes.  
  - Behavior: On selection, auto-fetches:
    - Admission ID  
    - Patient Name  
    - Bed No / Ward  
    - Under Doctor  
- **Patient Name (Auto)**  
  - Read-only; fetched from patient/master.  
- **Bed No / Ward (Auto)**  
  - Read-only; fetched from active admission/bed allocation.  
- **Doctor Name (Auto)**  
  - Read-only; defaults to `Under Doctor` for this admission.  

**Test Details Section**
- **Test Code**  
  - Type: Lookup (Lab Test Master).  
  - Required: Yes (per line).  
- **Test Name**  
  - Type: Auto-populated from Test Code; read-only.  
- **Urgent (Yes/No)**  
  - Type: Boolean per requisition or per test line (as per design).  
  - Behavior: If `Yes`, Lab queue flags the requisition as urgent.  
- **Remarks**  
  - Type: Text (optional).  

**Meta Section**
- **Requisition Date and Time**  
  - Type: DateTime  
  - Default: Current date and time.  
  - Editable: Configurable (back-dating may require special permission).  

**Actions**
- **Save** – creates a Lab Requisition record linked to admission.  
- **List View** – navigates to requisition history for the selected patient/admission.

#### 4.3.2 List View – Lab Requisition History

Columns:
- Requisition No  
- Requisition Date & Time  
- Patient ID / Admission ID  
- Patient Name  
- Bed No / Ward  
- Test Count  
- Status (Pending / Sample Collected / In Progress / Completed / Cancelled)  
- Print (icon/button)

Row-level actions:
- View / Print Requisition  
- (Optional) Cancel Requisition – if allowed and no sample collected yet.  

Filters:
- Date range  
- Ward / Bed  
- Status  
- Patient / Admission ID  

### 4.4 Business Rules
- Patient **must be admitted** with an active IPD admission.  
- Only **active admissions** are eligible; discharged or cancelled admissions are blocked.  
- **Duplicate test on the same day**:
  - If the same test is requested again on the same calendar day for the same patient/admission:
    - System must prompt a **confirmation warning** (e.g., "This test has already been requested today. Do you still want to proceed?").  
- **Urgent tests**:
  - Urgent flag is passed to the Lab system; Lab may:
    - Prioritize in worklists.  
    - Apply different TAT or charges as per configuration.  

### 4.5 Error Handling
- Invalid Patient / Admission ID → clear inline + banner error.  
- Patient without active IPD admission → block requisition, show guidance text.  
- Missing required test details → inline validation.  
- Integration/Lab service failure → user-friendly message; log technical details.

---

## 5. Medicine Requisition (Indoor)

### 5.1 Purpose
To request **medications** from Pharmacy for indoor patients as per doctor’s medication orders and nursing administration schedules.

### 5.2 Key Features
- **Patient-wise medicine requisition** tied to Admission ID.  
- **Stock availability check** against Pharmacy inventory.  
- **Auto-link with IPD Pharmacy Issue** workflow.  
- **Partial issue support**:
  - Pharmacy may issue quantities partially based on stock.  

### 5.3 User Interface

#### 5.3.1 Form View – Medicine Requisition (Indoor)

**Identification Section**
- **Patient ID (Search)**  
  - Required; behaves as in Lab Requisition.  
- **Patient Name (Auto)**  
  - Read-only; auto from patient master.  
- **Bed No (Auto)**  
  - Read-only; from admission/bed allocation.  

**Medicine Details (Line Items)**
- **Medicine Code**  
  - Type: Lookup (Drug / Item master).  
  - Required: Yes.  
- **Medicine Name**  
  - Auto from Medicine Code; read-only.  
- **Quantity**  
  - Type: Decimal / Integer  
  - Required: Yes.  
  - Validation: > 0 and within allowed limits vs doctor order.  
- **Frequency**  
  - Type: Enum / text (e.g., OD, BD, TID, QID, SOS, custom).  
  - Required: Yes (if captured at requisition level).  
- **Duration**  
  - Type: Integer + unit (days).  
  - Required: Yes (if modeled).  
- **Remarks**  
  - Optional text field.

**Actions**
- **Save** – creates Medicine Requisition.  
- **List** – navigates to requisition list for the patient/admission.

#### 5.3.2 List View – Medicine Requisition History

Columns:
- Requisition No  
- Medicine Name (or summary)  
- Requested Qty  
- Issued Qty  
- Status (Pending / Partially Issued / Fully Issued / Cancelled)  

Row-level actions:
- View details  
- (Optional) Cancel requisition (before issue).  

### 5.4 Business Rules
- **Quantity cannot exceed the doctor’s order**:
  - System should validate cumulative requested quantity vs active medication orders (if medication order module is implemented).  
- **Stock shortage**:
  - If requested quantity exceeds available stock:
    - System shows a **warning** with available quantity.  
    - Pharmacy may partially issue, and status becomes `Partially Issued`.  
- Only **Pharmacy-approved** requisitions can be issued:
  - If a pharmacy approval workflow is configured, requisition status must be `Approved` before issue.  

### 5.5 Error Handling
- Invalid or inactive medicine codes → inline error.  
- Patient without active admission → block with clear error.  
- Integration errors with Pharmacy → generic message + logged details.

---

## 6. Medicine Return (Indoor)

### 6.1 Purpose
To process **returns of unused or excess medicines** already issued to indoor patients, updating stock and patient billing accordingly.

### 6.2 Key Features
- **Return against previously issued requisitions / issues**.  
- **Automatic stock adjustment** in Pharmacy inventory.  
- **Refund or adjustment** in patient IPD bill.  

### 6.3 User Interface

#### 6.3.1 Form View – Medicine Return

**Identification Section**
- **Patient ID (Search)**  
  - As per prior modules.  

**Issue Reference Section**
- **Medicine Issue No**  
  - Type: Lookup for prior Pharmacy Issues against this admission.  
  - Required: Yes.  
  - Behavior: On selection, auto-fills:
    - Medicine Name  
    - Issued Qty  
    - Balance returnable quantity.  

**Return Details**
- **Medicine Name (Auto)**  
  - Read-only.  
- **Issued Qty (Auto)**  
  - Read-only.  
- **Return Qty**  
  - Type: Decimal / Integer  
  - Required: Yes.  
  - Validation: `0 < Return Qty ≤ (Issued Qty – Already Returned Qty)`.  
- **Reason**  
  - Type: Text / Enum (e.g., Not used, Dose changed, Adverse reaction, Other).  
  - Required: Yes.  

**Actions**
- **Save** – posts medicine return, updates stock and billing.  

#### 6.3.2 List View – Medicine Returns / Issues

Columns (illustrative):
- Requisition / Issue No  
- Medicine Name  
- Requested Qty  
- Issued Qty  
- Returned Qty  
- Status  

### 6.4 Business Rules
- **Return Qty ≤ Issued Qty** (taking into account prior returns).  
- **Expired medicines** cannot be returned to usable stock:
  - If expiry date has passed, system must block or route to **wastage** flow instead of normal return.  
- **Approved returns update billing automatically**:
  - Credit/refund or adjustment is posted to the patient’s IPD bill as per Billing rules.  

### 6.5 Error Handling
- Invalid Issue No or mismatched patient → blocking error.  
- Attempt to return more than issued quantity → validation error.  
- Integration failure to Pharmacy/Billing → user-friendly error and rollback of return.

---

## 7. Patient Follow-Up (Nurse Notes & Vitals)

### 7.1 Purpose
To record **regular nursing observations**, vital signs, and compliance with doctor instructions at defined intervals (e.g., shift-wise).

### 7.2 Key Features
- **Shift-wise follow-up entry** (Morning/Evening/Night or configurable).  
- **Vital signs recording**:
  - BP, Pulse, Temperature, Oxygen Saturation, etc.  
- **Nursing notes** describing patient condition and care provided.  
- Capture of **Doctor instruction compliance** (e.g., medications given, procedures done).  

### 7.3 User Interface

#### 7.3.1 Form View – Patient Follow-Up

**Identification**
- **Patient ID / Admission ID**  
  - Searchable; required.  
- **Patient Name (Auto)**  
  - Read-only.  

**Observation Details**
- **Date & Time**  
  - Type: DateTime  
  - Behavior:  
    - Auto-filled with current time.  
    - Back-date allowed only with specific permission.  
- **BP**  
  - Type: Text or structured (Systolic/Diastolic).  
- **Pulse**  
  - Type: Numeric (bpm).  
- **Temperature**  
  - Type: Numeric (with unit °C/°F).  
- **Oxygen Level (SpO₂)**  
  - Type: Numeric (%).  
- **Remarks (Nursing Notes)**  
  - Type: Long text; required or strongly encouraged.  
- **Next Follow-up Time**  
  - Type: DateTime; optional; used for scheduling next round/check.  

**Actions**
- **Save** – creates a follow-up record linked to admission.  

#### 7.3.2 List / Timeline View

Displays chronological list of follow-ups:
- Date & Time  
- Nurse Name / Entered By  
- Vitals summary (BP, Pulse, Temp, SpO₂)  
- Key remarks preview  

Allows doctors and other clinicians to quickly see nursing trends.

### 7.4 Business Rules
- **Follow-up entries are non-editable after Save**:
  - Only addendum / correction notes allowed via separate workflow (if implemented).  
- **Timestamp auto-generated**:
  - System must store both **entry timestamp** and **observation timestamp**.  
- **Doctor access**:
  - Doctor can **view** nurse’s notes and vitals but **cannot edit** them.  

### 7.5 Error Handling
- Missing mandatory fields (Patient, Date & Time, Remarks as configured) → inline errors.  
- Attempt to edit existing entry → blocked with appropriate message.  

---

## 8. Indoor Bed Status Check (Graphical View)

### 8.1 Purpose
To provide a **real-time, graphical overview** of bed availability and occupancy across wards, rooms, and cabins.

### 8.2 Key Features
- **Graphical bed layout** (ward-wise / floor-wise).  
- **Color-coded bed status** for quick interpretation.  
- **Quick patient lookup** from bed (click to open patient/admission summary).  

### 8.3 Bed Status Color Legend

- **Green** – `Free` (available for admission).  
- **Red** – `Occupied` (bed linked to active admission).  
- **Yellow** – `Reserved` (blocked for upcoming admission/procedure).  
- **Grey** – `Under Maintenance` (temporarily unavailable).  

> Exact colors may follow the hospital’s design system but semantics must remain consistent.

### 8.4 UI Behavior
- Screen loads ward/floor selection and displays beds as tiles or layout.  
- Hover / click on a bed shows:
  - Bed No / Room / Ward  
  - Current status  
  - If occupied: Patient Name, Admission ID, Under Doctor, Admission Date/Time.  
- Filters:
  - Ward, Room Type/Class, Status.  

### 8.5 Business Rules
- **Bed status updates in real-time** (or near real-time) on:
  - Admission, transfer, discharge, bed maintenance actions.  
- **Reserved beds auto-expire**:
  - If not used within configured time window, reservation lapses and bed reverts to `Free`.  
- Only **Admin (or authorized role)** can mark beds as `Under Maintenance` or change maintenance status.  

### 8.6 Error Handling
- Failure to fetch bed layout or status → show fallback list view and clear message.  
- Inconsistent state (e.g., bed marked free but linked to active admission) should be flagged for administrative review.

---

## 9. System Actions & Integrations (After Admission – Nurse Module)

### 9.1 System Actions
- **Auto-fetch patient data using Admission ID**:
  - All Nurse Module screens pull patient, bed, doctor, and corporate info from Admission.  
- **Sync lab requisitions with Lab module**:
  - Lab Requisition records flow to Lab for sample collection, result entry, and status updates.  
- **Sync medicine requisitions with Pharmacy**:
  - Medicine Requisition and Return link directly to Pharmacy issue/stock workflows.  
- **Update IPD bill in real-time**:
  - Doctor visits, lab tests, medicines, and other chargeable events push charges to IPD Billing.  
- **Maintain nurse activity audit logs**:
  - All key actions (requisition creation, follow-up entries, returns) are logged with user, timestamp, and context.

### 9.2 Notes
- Nurse module is **read-only for billing**:
  - Nurses can trigger billable actions, but cannot alter charges directly.  
- All actions are **time-stamped** and form part of **medico-legal documentation**.  

---

## 10. Doctor Visit Entry (Indoor Patient)

### 10.1 Purpose
The Doctor Visit Entry module records **daily doctor rounds/visits** for admitted IPD patients. It supports **clinical tracking**, **visit-based billing**, and **medico-legal documentation**. Each visit may:
- Generate **doctor visit charges**.  
- Trigger **orders** for labs, medicines, or procedures.  
- Be referenced by **Nurses** and **Billing** for actions and verification.

### 10.2 Pre‑Requisites
- **Active IPD Admission** exists for the patient.  
- **Doctor assignment** to admission (`Under Doctor` / Consultant).  
- **Bed allocation** present and valid.  
- **User Role**:
  - Doctor (Primary / Consultant) – can create and approve visits.  
  - Authorized Nurse – may enter visit on behalf of doctor, but **cannot finalize/approve** if separate approval flow is implemented.

### 10.3 Features
- **Admission-wise doctor visit entry** (per visit).  
- **Auto-fetch patient and bed details** from Admission ID.  
- **Visit type selection**:
  - Regular Round  
  - Emergency Visit  
  - Consultant Visit  
- **Doctor visiting charges auto-calculation**:
  - Based on doctor master, department, visit type, and configuration.  
- **Doctor notes & instructions**:
  - Clinical notes and explicit nurse instructions.  
- **Visit history tracking**:
  - Admission-wise chronological view.  
- **Auto-link with IPD Billing**:
  - Visits push visit charges to IPD bill, with billed/unbilled status.

### 10.4 User Interface

#### 10.4.1 Form View – Doctor Visit Entry

**Basic Information**
- **Patient ID (Search / Scan)**  
  - Required; may also search by Admission ID or Bed.  
- **Patient Name (Auto)**  
  - Read-only.  
- **UHID / Patient ID (Auto)**  
  - Where UHID and Admission ID differ, both may be shown.  
- **Bed No / Ward (Auto)**  
  - From current bed assignment.  

**Visit Details**
- **Visit Date & Time**  
  - Default: Current date & time.  
  - Editable: Back-dating allowed with permission and must be ≤ current time.  
- **Doctor Name**  
  - Default: Current logged-in doctor (if role Doctor).  
  - Alternately selectable from Doctor master (for shared terminals).  
- **Visit Type**  
  - Enum: `Regular`, `Emergency`, `Consultant`.  
- **Visit Charge (Auto)**  
  - Fetched from doctor/department tariff based on Visit Type.  
  - Editable only with adequate permissions and fully audit-logged.  
- **Remarks / Clinical Notes**  
  - Long text describing assessment, plan, etc.  
- **Doctor Instructions (For Nurse)**  
  - Long text; explicit instructions for nursing staff (e.g., monitoring frequency, interventions).

**Actions**
- **Save Visit**  
  - Creates Visit record; if billing auto-post is enabled, pushes charge to IPD billing.  
- **Print Visit Note (Optional)**  
  - Generates formatted note for charts/records.

#### 10.4.2 List View – Doctor Visit History

Columns:
- Visit No / Visit ID  
- Visit Date & Time  
- Doctor Name  
- Visit Type  
- Charge Amount  
- Entered By  
- Status (`Billed` / `Pending` / `Voided`)  

Row-level actions:
- **View** – all roles with appropriate permissions.  
- **Edit** – allowed **only before billing** or within limited time window, as per rules.  
- **Print Visit Note**.  

### 10.5 Business Rules
- Patient must have **active admission**; visits cannot be recorded for discharged patients.  
- **Visit charge auto-application** based on:
  - Doctor category (e.g., Consultant vs Resident).  
  - Visit Type (Regular vs Emergency vs Consultant).  
- **One regular visit per doctor per day** (configurable):
  - System should warn or block additional regular visits by the same doctor on the same day, based on configuration.  
- **Emergency visits** may be allowed multiple times per day without restriction.  
- Once **visit is billed**, record becomes **read-only**:
  - Only void/reversal flow (with reason and audit) is allowed; no direct deletion.  

### 10.6 Edge Cases & Exceptions
- **Doctor reassigned**:
  - When Under Doctor is changed at admission level, new visits are associated with the new doctor; historical visits retain original doctor.  
- **Missed visit entry / back-dating**:
  - Back-dated entries allowed only with elevated permissions.  
  - System records both visit date/time and entry timestamp.  
- **Duplicate visit same date**:
  - System shows warning when a second regular visit for same doctor & day is being entered.  
- **Emergency visit outside normal shift hours**:
  - System can flag such visits specially for review/audit.  

### 10.7 Error Handling
- **Invalid Patient ID / Admission ID** → clear error message; no visit created.  
- **Discharged patient** → visit entry blocked; suggest opening OPD or new admission if appropriate.  
- **Missing doctor charge / tariff configuration**:
  - System should raise an admin alert and may:
    - Block visit save, or  
    - Allow save but flag charge as missing for billing to resolve.  
- **Unauthorized role**:
  - If user without required role attempts access, system returns “Access Denied”.  

### 10.8 System Actions
- Auto-generate **Visit ID** for each visit.  
- Auto-calculate **Visit Charge**.  
- Push visit charge to **IPD Invoice/Billing** (if configured).  
- Log **doctor visit timestamp**, user, and device/session info.  
- Notify nurse (e.g., via dashboard or notification) of **new doctor instructions**.  

### 10.9 Notes
- Doctor visit data is **legally sensitive**; retention and access must follow regulatory requirements.  
- Visit records **cannot be hard-deleted**; only voiding with reason and proper audit is allowed.  
- Data is consumed by:
  - IPD Billing  
  - Medical Records / EHR  
  - Quality, Clinical Audit & Compliance teams  

