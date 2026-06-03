# Doctor Visit Entry (Indoor / IPD)

## 1. Purpose

The Doctor Visit Entry module records **daily doctor rounds/visits** for admitted (IPD) patients. It supports **clinical tracking**, **visit-based billing**, and **medico‑legal documentation**. Each visit:
- May generate one or more **doctor visit charges**.  
- Can trigger downstream orders for **lab tests**, **medications**, or **procedures**.  
- Is referenced by **Nurse Module**, **IPD Billing**, and **Medical Records / EHR**.

## 2. Pre‑Requisites

- **Active IPD Admission** for the patient (see `[admission-ipd.md](admission-ipd.md)`).  
- **Doctor assignment** (Under Doctor / Consultant) captured during admission.  
- **Bed allocation** present and valid.  
- **User Roles**:
  - **Doctor (Primary / Consultant)** – can create and approve visits.  
  - **Authorized Nurse / Ward Nurse** – may capture visit details on behalf of a doctor (where policy allows), but cannot finalize/approve in configurations that require explicit doctor confirmation.  

## 3. Features

- Admission-wise **doctor visit entry** (one record per visit).  
- **Auto-fetch patient and bed details** when Admission ID / Patient ID is selected.  
- **Visit type selection**:
  - Regular Round  
  - Emergency Visit  
  - Consultant Visit / Second Opinion  
- **Automatic visit charge calculation** based on:
  - Doctor category (consultant, resident, etc.).  
  - Department and configured visit tariff.  
  - Visit Type (regular vs emergency vs consultant).  
- **Doctor notes & instructions**:
  - Clinical notes for the visit.  
  - Explicit nurse instructions visible in Nurse Module.  
- **Visit history tracking** per admission.  
- **IPD Billing integration** to push visit charges.  

## 4. User Interface

### 4.1 Form View – Doctor Visit Entry

**Basic Identification**
- **Patient ID (Search / Scan)**  
  - Search supports Admission ID, UHID, or Patient ID.  
  - Required; on selection auto-fills admission context.  
- **Patient Name (Auto)** – read-only from patient master.  
- **UHID / Patient ID (Auto)** – read-only, as per hospital configuration.  
- **Admission ID / IPD No (Auto)** – active admission for the patient.  
- **Bed No / Ward (Auto)** – from current bed allocation.  

**Visit Details**
- **Visit Date & Time**  
  - Default: Current date/time.  
  - Must not exceed current time.  
  - Back-dating allowed only with specific permission; all such entries are fully audited.  
- **Doctor Name**  
  - Default: Logged-in doctor when role = Doctor.  
  - Alternatively selectable from Doctor Master (for shared terminals).  
- **Doctor ID** – auto-populated, read-only once selected.  
- **Visit Type** – Enum: at least `Regular`, `Emergency`, `Consultant`.  
- **Visit Charge (Auto)**  
  - Derived from doctor/department tariff and visit type.  
  - Override allowed only with specific permission and full audit.  
- **Remarks / Clinical Notes** – long text for assessment and plan.  
- **Doctor Instructions (For Nurse)** – long text for nursing instructions (e.g., vitals frequency, medications, positioning).

**Actions**
- **Save Visit** – creates visit record, and if configured, posts charge to IPD Billing.  
- **Print Visit Note** (optional) – formatted note for charting.  

### 4.2 List View – Doctor Visit History

Columns:
- Visit No / Visit ID  
- Visit Date & Time  
- Doctor Name  
- Visit Type  
- Charge Amount  
- Entered By  
- Status (`Billed`, `Pending`, `Voided`)  

Row-level actions:
- View  
- Edit (before billing only, and/or within configured time window)  
- Print Visit Note  

Filters:
- Date range  
- Doctor / Department  
- Visit Type  
- Billing Status  

## 5. Business Rules

- **Active Admission Only**  
  - Visits can be recorded only for patients with an active IPD admission.  
  - Attempts to record visits for discharged/non-admitted patients are blocked.  
- **Charge Derivation**  
  - Visit Charge is determined by:
    - Doctor category (consultant vs resident).  
    - Doctor’s department and configured visit tariff.  
    - Visit Type (regular, emergency, consultant) and any time-of-day rules.  
- **One regular visit per doctor per day** (configurable)  
  - System warns or blocks if a second Regular visit is entered by the same doctor for the same patient/date (as per configuration).  
  - Emergency and Consultant visits are not constrained by this rule.  
- **Billing Lock**  
  - Once a visit is billed (charge posted and not reversed), the record becomes read-only.  
  - Corrections must be done via void/reversal with mandatory reason and full audit.  
- **Role-based Editing**  
  - Nurses can be allowed to enter visit data but may require doctor review/approval before billing.  

## 6. Edge Cases & Exceptions

- **Doctor reassignment**  
  - When Under Doctor is changed at the admission level, new visits are associated with the new doctor; historical visits retain original doctor.  
- **Back-dated entries**  
  - Allowed only for authorized roles; the system records both visit timestamp and entry timestamp and may flag back-dates for audit.  
- **Duplicate visit on same date**  
  - For same patient, doctor, and Regular type on the same date, system shows a warning or blocks, as configured.  
- **Emergency outside normal shift**  
  - Emergency visits outside configured shift hours are specifically flagged (and may carry special tariffs or audit rules).  

## 7. Error Handling

- Invalid Patient / Admission ID → block save with clear message.  
- Discharged / non-admitted patient → visit entry blocked; user is directed to appropriate OPD or new admission workflow.  
- Missing tariff configuration → admin alert; either block visit save or allow save with **Missing Charge** status for billing to rectify.  
- Unauthorized roles → access denied, visit entry form hidden.  

## 8. System Actions & Audit

- Auto-generate **Visit ID** for each visit.  
- Auto-calculate and optionally auto-post **Visit Charge** to IPD Billing.  
- Maintain detailed **audit trail**:
  - User, timestamp, action type (create/update/void).  
  - Before/after values for key fields (visit type, charge, doctor, date/time).  
- Notify **Nurse Module** (dashboards or alerts) of **new or updated doctor instructions** so nurses can document compliance in follow-up notes.  

