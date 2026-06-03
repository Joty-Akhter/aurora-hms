# Procedure Entry (Indoor / IPD – Medical, Surgical & Service Procedures)

## 1. Overview

**Description**  
The Procedure Entry module records all **medical, minor surgical, and service procedures** performed for an **admitted (IPD) patient** outside or in addition to core OT operations. These entries directly contribute to:
- IPD billing and interim/final invoices  
- Doctor fees and professional charges  
- OT/service charges where applicable  
- Clinical and operational reporting

**Scope**  
Procedure Entry is available **only for active IPD admissions**. It is typically used by **Nurses, OT staff, and Admin/billing roles** to capture bedside procedures, ward procedures, minor OT procedures, diagnostic services, and chargeable nursing services that are not fully covered by other specialized modules.

---

## 2. Navigation

- **Menu Path**: `Indoor → Procedure Entry`  
- **Access Control**:
  - Visible only to users with appropriate roles/permissions (e.g., Nurse, OT Staff, Billing/Admin).  
  - Records can only be created for **currently admitted** (active IPD) patients.

---

## 3. Pre‑Requisites

Before using the Procedure Entry module, the following must be in place:

- **Active IPD Admission**  
  - Patient has an active admission; no entries allowed for discharged/cancelled admissions.  
- **Bed Assignment**  
  - Patient must have a bed assigned; bed status = `Occupied`.  
- **Doctor Master Configuration**  
  - Doctors who may be tagged to procedures (e.g., consultant, surgeon, anesthetist) exist and are Active.  
- **Procedure / Service Master Configuration**  
  - Procedure and service catalog with:
    - Procedure Code, Name/Description  
    - Default Unit, Charge, and applicable departments/roles  
    - Flags for corporate eligibility, discount rules, etc.  
- **Role Permissions**  
  - Appropriate permissions for **Nurse / OT / Admin** (e.g., create, edit, delete, approve).  

---

## 4. Procedure Entry Screen – Header Section

### 4.1 Patient / Admission Context

**Search & Selection**
- **Patient Search Input**  
  - Searchable by:
    - Patient ID / UHID  
    - Admission No / IPD No  
  - Only **currently admitted** patients are returned in search results.  
- On selecting a patient/admission, the system auto-fills header information.

### 4.2 Auto‑Fetched Fields (Read‑Only)

The following fields are **read‑only** and populated from Admission and Patient masters:
- **Patient ID**  
- **Patient Name**  
- **Address**  
- **Gender**  
- **Age**  
- **Admission Date**  
- **Admission Time**  
- **Bed No / Ward / Room**  
- **Department** (derived from Under Doctor/Department mapping)  

**Business Rules**
- Only **active admissions** are considered.  
- If multiple active admissions per patient are not allowed (per Admission rules), the selection is unique; otherwise, user must choose the correct admission context.

---

## 5. Procedure Entry – Detail Grid

Each row in the grid represents a **single procedure/service instance** to be billed.

### 5.1 Grid Columns

- **Code (Procedure Code)**  
  - Type: Lookup (Procedure/Service master).  
  - Required: Yes.  
  - Behavior: On selection, auto-fills Description, default Unit, and default Charge (if configured).  

- **Description (Procedure Name)**  
  - Type: Text (auto from master; optionally editable subject to permissions).  
  - Required: Yes.  

- **Dr. Code**  
  - Type: Lookup (Doctor master).  
  - Required: Optional/required based on procedure type and configuration.  
  - Behavior: Limits selection to Active doctors; may default to Under Doctor.  

- **Dr. Name**  
  - Type: Text (auto).  
  - Behavior: Auto-filled from Dr. Code; read-only.  

- **Charge (Unit Price)**  
  - Type: Decimal.  
  - Required: Yes.  
  - Behavior:
    - Defaulted from Procedure master.  
    - Editable only where policy allows (e.g., with discount/override permissions).  
  - Validation: `Charge ≥ 0`.  

- **Unit**  
  - Type: Text / Enum (e.g., session, day, procedure, hour).  
  - Required: Yes (from master).  

- **NoU (Number of Units)**  
  - Type: Integer/Decimal.  
  - Required: Yes.  
  - Default: 1.  
  - Validation: `NoU > 0`.  

- **Total**  
  - Type: Decimal (auto-calculated).  
  - Calculation Rule:  
    - `Total = Charge × NoU`.  
  - Non-editable; recalculates on change to Charge or NoU.  

- **Action**  
  - Row actions for **Add**, **Edit**, **Remove** (subject to billing state and permissions).  

### 5.2 Procedure Selection Logic

- **Procedure Selection**  
  - User selects procedures via search or dropdown from Procedure master.  
  - Supports search by code, name, or department.  

- **Charge Auto‑Fetch**  
  - When Code is selected, Charge and Unit are auto-fetched from master.  
  - If corporate tariffs exist, applicable corporate charge is determined in billing, but base charge is stored here.  

- **Doctor Selection Per Procedure**  
  - Doctor can be selected on a **per-row basis**, allowing different doctors for different procedures on the same admission/date.  
  - Multiple doctors across different rows are allowed.  

---

## 6. Footer Section

Fields:
- **Total Item**  
  - Type: Auto count.  
  - Behavior: Count of grid rows (procedures/services) currently captured.  
- **Remarks**  
  - Type: Free-text, optional.  
  - Description: General notes about the procedure batch (not a substitute for clinical notes).  
- **Grand Total**  
  - Type: Decimal, auto-sum.  
  - Behavior: Sum of `Total` across all grid rows.  

---

## 7. Actions

- **Save**  
  - Saves the procedure entry in **Draft / Active** state for the given admission.  
  - Performs validation (patient admission, grid contents, non-zero charge/NoU).  
- **List**  
  - Navigates to Procedure List View (history) for searching and managing previous entries.  
- **Show / View**  
  - Opens selected record in read-only or editable mode depending on status and permissions.  

---

## 8. Procedure List View

### 8.1 Columns

- Ref No (Procedure Entry reference number)  
- Ref Date  
- Patient ID / Admission ID  
- Patient Name  
- User Name (who created the entry)  
- Invoice (Print icon / link to billing)  
- Edit  
- Delete / Cancel  

### 8.2 Business Rules (List View)

- **Edit**  
  - Allowed only **before final invoice generation** or while related charges remain un-finalized.  
- **Delete / Cancel**  
  - Allowed only for users with specific permission.  
  - Generally allowed only when:
    - No finalized billing exists for the referenced procedures, or  
    - A controlled reversal/refund workflow is used.  

---

## 9. Billing & Invoice Integration

- **Automatic Charge Posting**  
  - Procedure charges are **auto‑posted to IPD Invoice** as line items linked to:
    - Admission ID / IPD No  
    - Patient ID  
    - Procedure code and doctor (if applicable).  

- **Corporate Discount Handling**  
  - If the patient is marked as **Corporate** (see Admission/Corporate rules):  
    - Corporate tariffs/discounts apply in IPD Billing based on configured rules.  
    - Procedure Entry should provide necessary flags (e.g., whether a procedure is corporate-eligible).  

- **Role‑wise Discount Validation**  
  - Any change in Charge at entry time may be subject to:
    - Role-based limits (e.g., maximum discount).  
    - Approval workflows in Billing.  

- **Locking After Final Invoice Generation**  
  - Once the **final IPD invoice is generated/locked**:
    - Related Procedure Entries become read-only.  
    - New procedure entries for historical dates may require special override and billing adjustment.  

---

## 10. Audit & Control

- **Audit Trail**  
  - For each Procedure Entry:
    - Entry date/time and user are logged.  
    - All edits (before billing lock) and deletes/cancels are tracked with:
      - Who performed the action.  
      - When the action occurred.  
      - What fields changed (before/after).  
- **Traceability**  
  - Each procedure row is traceable to:
    - Admission, patient, department, and responsible doctor.  
    - Linked billing items and any refunds/reversals.  

---

## 11. Error Handling & Validation

- **Patient Not Admitted**  
  - If selected Patient ID / Admission No does not map to an active admission:
    - Block entry creation.  
    - Show a clear message (e.g., “Patient does not have an active IPD admission.”).  

- **Zero Charge or Zero NoU**  
  - If `Charge = 0` or `NoU = 0`:
    - Validation error on save, unless:
      - System supports explicit zero-charge procedures with configuration, in which case:
        - A warning/justification may be required.  

- **Duplicate Procedure at Same Time (Configurable)**  
  - If the same procedure is entered multiple times for the same patient, date, and (optionally) time:
    - System may:
      - Show a **warning** (“This procedure appears to be a duplicate.”), or  
      - Block as per configuration.  

- **General Validation**  
  - Missing mandatory fields in header (patient/admission) or grid (Code, Description, Charge, NoU) result in inline errors.  
  - Permission errors (e.g., unauthorized delete/edit) return “Access denied” and do not perform the action.  

---

## 12. Notes & Integrations

- **Supported Procedure Types**  
  - OT-related minor procedures (when not fully covered by OT module).  
  - Bedside procedures (e.g., central line insertion, catheterization).  
  - Diagnostic services not captured in dedicated Lab/Imaging modules (if configured this way).  
  - Nursing procedures/services (e.g., special nursing care packages).  

- **Module Integrations**  
  - **Doctor Visit Module** – procedures may be ordered during doctor visits; linkage can be recorded via Admission ID and timestamps.  
  - **OT Module** – OT procedures may reference or be complemented by separate Procedure Entry records for ancillary services.  
  - **IPD Billing** – consumes procedure lines for charge calculation and discount application.  

