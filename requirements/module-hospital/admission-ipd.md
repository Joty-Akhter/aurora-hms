# Admission Module (IPD) – Patient Admission

## 1. Overview

**Description**:  
The IPD Admission Module manages the complete indoor patient admission workflow. It ensures correct patient identification, captures admission, bed, guardian, doctor, and corporate information, and prepares the patient record for downstream IPD billing, clinical care, and discharge processes.

**Purpose**:  
- **Standardize** the indoor admission process across all wards and departments.  
- **Ensure data integrity** for patient identity, bed allocation, treating doctor, and corporate contracts.  
- **Provide a clear UI workflow** for admitting staff, minimizing errors and duplicate admissions.  
- **Enable seamless integration** with IPD Billing, Doctor Visit, OT, Pharmacy, and Patient Health Records.

**Pre‑Requisite Modules / Masters**:
- **Patient Registration Module**  
  - Ability to create and update patient demographic records.  
  - Shared registration modal usable across OPD, IPD, and Emergency.
- **Bed Management**  
  - Bed master with status and default charges.  
  - Real‑time bed availability tracking (vacant/occupied/blocked/etc.).  
- **Doctor and Department Masters**  
  - Referring and admitting doctors with mapped departments.  
- **Corporate / Payer Master**  
  - Corporate clients, contract/payment types, and discount rules (used by Billing).

---

## 2. High‑Level Admission Flow

1. User navigates to **Admission → IPD Admission** menu.  
2. System opens the **Patient Admission Screen** (Admission Start Screen).  
3. User enters **Registration No / Mobile / Patient ID**.  
4. System determines:
   - **New Patient** → Open **Patient Registration Modal**.  
   - **Existing Patient** → Auto‑fill patient data from master.  
5. User completes remaining admission sections:
   - Bed Information  
   - Guardian Information  
   - Doctor Information  
   - Corporate & Additional Info  
6. User clicks **Save**.  
7. System validates data, checks **bed availability** and **duplicate active admission**, and if valid:
   - Creates **Admission Record** (IPD episode).  
   - Locks relevant corporate selection and links to **IPD Billing**.  
   - Updates **Bed status** to occupied with reference to the admission.

---

## 3. User Interface – Admission Start Screen

**Screen Name**: `IPD Admission – Start`

**Purpose**:  
Entry point to start an admission by uniquely identifying or creating a patient.

**Fields / Controls**:
- **Registration No**  
  - **Type**: Text with search / lookup  
  - **Required**: Yes (unless Patient ID or Mobile used as alternate search input)  
  - **Behavior**:
    - Accepts Registration No or Patient ID format as per configuration.  
    - Supports search by Registration No / Patient ID.
- **Mobile No**  
  - **Type**: Text (phone)  
  - **Required**: No (alternative search key)  
  - **Behavior**:
    - When entered with Registration No/Patient ID, system may use both to narrow search.  
- **Patient Type**  
  - **Type**: Enum  
  - **Values**: `General`, `Corporate`  
  - **Required**: Yes  
  - **Behavior**:
    - Defaults to `General`.  
    - If `Corporate`, the Corporate section becomes mandatory in Step‑5.
- **Package**  
  - **Type**: Boolean (`Yes` / `No`)  
  - **Required**: Yes  
  - **Behavior**:
    - If `Yes`, **Package Name** becomes required.
- **Package Name**  
  - **Type**: Lookup / dropdown  
  - **Required**: Conditionally required when **Package = Yes**  
  - **Behavior**:
    - Source: Package master (if implemented in Billing/Packages module).  
- **Registration Button** (`Search / Start Admission`)  
  - **Action**:
    - On click, system evaluates if patient exists:  
      - Not found → opens **Patient Registration Modal** (Step‑1A).  
      - Found → auto‑fills patient data (Step‑1B) and moves focus to Bed section.

**UI Behavior**:
- All mandatory fields should be marked clearly (e.g., `*`).  
- Inline validation messages displayed beneath or next to each field.  
- Error banners used only for blocking errors (e.g., system failures).

---

## 4. Admission Step‑1: Patient Identification

### 4.1 Scenario A – New Patient

**Trigger Condition**:  
- `Registration No / Patient ID` not found in Patient Registration master, OR  
- No matching patient found by Mobile No (where applicable).

**System Action**:
- Automatically opens the **Patient Registration Modal** in overlay mode.  
- Freezes the underlying Admission form until registration is completed or canceled.

**Patient Registration Modal – Fields**:
- **Patient Name \***  
- **Father’s / Spouse Name \***  
- **Gender \***  
- **Age \*** (or Date of Birth, as per master design)  
- **Mobile No \***  
- **Address \***  
- **Religion**  
- **Occupation**  

> Detailed field definitions, validations, and behaviors follow the central **Patient Registration Module** specification and must not be duplicated here.

**Post Registration Behavior**:
- System generates a **Patient ID** (and/or Registration No as per design).  
- Closes the modal automatically on successful save.  
- Auto‑fills relevant patient fields in the Admission form:
  - Patient ID  
  - Patient Name  
  - Gender  
  - Age  
  - Mobile No  
  - Address  
  - Religion  
- Focus moves to **Bed Information** section.

### 4.2 Scenario B – Existing Patient

**Trigger Condition**:  
- Valid `Registration No / Patient ID` found in patient master (or unique match by Mobile No).

**System Action**:
- Auto‑fills patient information into read‑only fields:
  - Patient Name  
  - Gender  
  - Age  
  - Mobile No  
  - Address  
  - Religion  
- **Core identity fields are non‑editable** from the Admission screen. Edits must be done via the Patient Registration module.

**Business Rules**:
- If multiple patients match (e.g., same mobile number), system should:
  - Display a **patient selection list** with key demographics (name, age, gender, address, registration no).  
  - Force the user to pick one before proceeding.  
- If **patient is marked Deceased or Inactive** in master:
  - System must **block new admission** and show a clear error message with appropriate guidance.

---

## 5. Admission Step‑2: Bed Information

**Purpose**:  
To assign an available bed to the patient and calculate default bed and admission charges.

**Fields**:
- **Bed No \***  
  - **Type**: Select / lookup  
  - **Required**: Yes  
  - **Behavior**:
    - Shows only **available** beds as per Bed Management (e.g., filtered by ward, room, class if supported).  
    - May support advanced filters (ward, room type, class) depending on Bed Management design.
- **Bed Charge**  
  - **Type**: Decimal (read‑only by default)  
  - **Required**: Yes  
  - **Source**: Auto‑fetched from Bed Master.  
  - **Behavior**:
    - Auto‑populated when a bed is selected.  
    - Editable only if configuration allows manual override (traceable via audit logs).
- **Admission Charge**  
  - **Type**: Decimal  
  - **Required**: Yes (as per hospital configuration)  
  - **Behavior**:
    - Auto‑calculated from a rule engine or tariff configuration.  
    - May be manually editable subject to user role/permission and hospital policy.
- **Relatives Stay**  
  - **Type**: Boolean (`Yes` / `No`)  
  - **Required**: No  
  - **Behavior**:
    - If `Yes`, may influence additional bed/boarding charges (handled by Billing rules).

**Business Rules**:
- **One bed → one active patient**  
  - A bed can be assigned to **only one active, not‑yet‑discharged admission** at a time.  
  - Attempting to save admission when the bed becomes occupied concurrently must fail with clear error.
- **Bed availability validation**  
  - At **Save**, system revalidates bed status from Bed Management.  
  - If bed is no longer available (occupied/blocked), system blocks save and prompts user to reselect.  
- **Charge derivation**  
  - Bed Charge is always derived from the latest bed tariff for that bed.  
  - Admission Charge rule configuration must be documented in Billing/Tariff requirements.

---

## 6. Admission Step‑3: Guardian Information

**Purpose**:  
Capture responsible guardian/attendant details for consent, communication, and billing coordination.

**Fields**:
- **Guardian Name \***  
  - **Type**: Text  
  - **Required**: Yes  
- **Relation \***  
  - **Type**: Enum / text (e.g., Father, Mother, Spouse, Son, Daughter, Relative, Other)  
  - **Required**: Yes  
- **Phone \***  
  - **Type**: Text (phone)  
  - **Required**: Yes  
  - **Validation**: Valid phone format as per configuration.
- **Secondary Guardian (Optional)**  
  - **Guardian Name (2)** – optional  
  - **Relation (2)** – optional  
  - **Phone (2)** – optional  

**Business Rules**:
- At least **one primary guardian** must be captured for every IPD admission.  
- Secondary guardian is optional but must be complete (name + relation) if any field is filled.

---

## 7. Admission Step‑4: Doctor Information

**Purpose**:  
Identify the referring doctor and the primary treating doctor for the admission, along with the clinical department.

**Fields**:
- **Ref. By \***  
  - **Type**: Lookup (Doctor master / External doctor master)  
  - **Required**: Yes (configurable – may be optional in some setups)  
  - **Behavior**:
    - Supports doctors from internal doctor master and optionally external panel.  
- **Under Doctor \*** (Admitting / Treating Doctor)  
  - **Type**: Lookup (Doctor master)  
  - **Required**: Yes  
  - **Behavior**:
    - Drives default department, doctor visit charges, and OT/clinical workflows.
- **Department**  
  - **Type**: Lookup (Department master)  
  - **Required**: Yes  
  - **Behavior**:
    - Auto‑filled based on `Under Doctor`’s primary department.  
    - Editable only if user has specific permission or hospital allows override.
- **Operation Name**  
  - **Type**: Lookup / text (OT procedure master)  
  - **Required**: No  
  - **Behavior**:
    - Used when admission is for a planned surgery; may populate OT booking flows.

**Business Rules**:
- `Under Doctor` must be an **Active** doctor in doctor master.  
- Department cannot be left blank; if auto‑fill fails, user must choose one manually.  
- Operation Name is optional at admission but may become mandatory in OT workflows.

---

## 8. Admission Step‑5: Corporate & Additional Information

**Purpose**:  
Capture corporate/payer context and additional operational details linked to the admission.

**Fields**:
- **Corporate Client**  
  - **Type**: Boolean (`Yes` / `No`)  
  - **Required**: Yes  
  - **Behavior**:
    - If `Yes`, Corporate fields become mandatory and billing rules change in IPD Billing.
- **Corporate ID**  
  - **Type**: Lookup (Corporate / Payer master)  
  - **Required**: Required when **Corporate Client = Yes**  
  - **Behavior**:
    - Selection drives applicable corporate tariffs/discounts used by Billing.
- **Corporate Payment Type**  
  - **Type**: Enum (e.g., Cashless, Co‑pay, Reimbursement, Panel)  
  - **Required**: Required when **Corporate Client = Yes**  
- **MR ID**  
  - **Type**: Text / lookup  
  - **Required**: No  
  - **Description**: Medical Representative or corporate relationship manager identifier.
- **Attached Doctor / Room**  
  - **Type**: Lookup / text  
  - **Required**: No  
  - **Behavior**:
    - Used where corporates require mapping to specific rooms or consultants.
- **Ambulance Info**  
  - **Type**: Text / structured fields  
  - **Required**: No  
  - **Description**: Information on ambulance used, if applicable (vehicle, time, charges – detailed modeling may be in Transport/Logistics module).
- **Assist 1 / Assist 2**  
  - **Type**: Lookup (Doctor/technician) or text  
  - **Required**: No  
  - **Description**: Supporting clinical staff linked to this admission (esp. for OT).
- **Remarks**  
  - **Type**: Long text  
  - **Required**: No  
  - **Description**: Free‑text notes relevant to admission.

**Business Rule**:
- If **Corporate Client = Yes** → **Corporate discount and tariff rules** are applied in the **IPD Billing** module for all linked services and charges.  
- Corporate selection (Corporate ID, Payment Type) becomes **locked** after the first invoice/charge is posted (see detailed rule in section 10).

---

## 9. Actions and List View

### 9.1 Actions on Admission Form

- **Save**  
  - Creates a new **Admission Record** (IPD episode) after all validations pass.  
  - Assigns the selected bed and updates Bed status.  
  - Generates Admission No / IPD No as per numbering configuration.
- **Reset / Clear**  
  - Clears unsaved data in the form (if allowed) with confirmation prompt.  

### 9.2 Admission List Screen

**Purpose**:  
Provide an overview of current and historical admissions with quick access to downstream actions.

**Columns (indicative)**:
- Admission No / IPD No  
- Patient Name  
- Patient ID / Registration No  
- Bed / Room / Ward  
- Admission Date & Time  
- Under Doctor  
- Patient Type (General / Corporate)  
- Status (Admitted, Discharged, Cancelled, etc.)  

**Row‑Level Actions**:
- **Edit**  
  - Opens admission in edit mode subject to business rules (see Section 10).  
- **Invoice / Billing**  
  - Navigates to **IPD Billing** console with this admission context.  
- **Release / Discharge**  
  - Opens discharge workflow (separate specification) to release bed and close admission.

**Filters / Search**:
- Date range (Admission Date)  
- Patient Name / ID / Registration No  
- Doctor / Department  
- Bed / Ward / Room  
- Status  

---

## 10. Core Business Rules

1. **Patient must exist before admission**  
   - Admission cannot be saved without a valid Patient ID from the Patient Registration module.  
   - New patient creation must go through the shared Registration Modal.
2. **Bed availability validation**  
   - System must validate bed status at the moment of save.  
   - If bed is occupied/blocked/transferred, save must be blocked with a clear message.
3. **No duplicate active admission per patient**  
   - A patient cannot have more than one **active** IPD admission at the same time (per facility).  
   - Attempting to create a second active admission must show a blocking error, optionally listing existing active admission(s).
4. **Corporate selection locking**  
   - Once any **billing transaction** (charge, service, or invoice) has been posted against the admission:
     - `Corporate Client`, `Corporate ID`, and `Corporate Payment Type` are **locked**.  
     - Changes afterward require a special override workflow (if implemented) and full audit logging.
5. **Admission cannot be deleted once billing starts**  
   - If any financial transaction exists for the admission, **hard delete is not allowed**.  
   - Cancellation, if supported, must follow a separate reversal/cancellation workflow with audit trail.
6. **Audit and traceability**  
   - All changes to key fields (bed, under doctor, corporate fields) must be audit‑logged with timestamp, user, and old/new values.

---

## 11. Edge Cases & Exceptions

- **Duplicate active admission for the same patient**  
  - System must detect if an active admission exists for the patient at the same facility.  
  - UI should display:
    - Existing Admission No  
    - Bed, Under Doctor, Admission Date  
  - User is prevented from creating another active admission unless allowed by special override roles.
- **Bed occupied during save attempt**  
  - If bed status changes between selection and save (e.g., another user assigns it):  
    - Save operation fails.  
    - Error message: e.g., "Selected bed is no longer available. Please select another bed."  
    - User remains on form with data preserved, except bed fields which must be re‑selected.
- **Corporate ID missing when Corporate Client = Yes**  
  - Inline validation should prevent save and highlight Corporate ID and related fields.  
  - User must either:
    - Choose a valid Corporate ID, or  
    - Change **Corporate Client** to `No` (if allowed).
- **Patient not found**  
  - When search inputs do not match any patient, system opens the **Patient Registration Modal** and clearly indicates that a new patient will be created.

---

## 12. Error Handling & Validation

**Field‑Level Validation**:
- Mandatory fields marked with `*` and validated on blur and on Save:
  - Registration / Patient identification input (or chosen patient)  
  - Bed No  
  - Guardian Name, Relation, Phone  
  - Under Doctor, Department  
  - Corporate fields when **Corporate Client = Yes**  
- Inline messages e.g., "This field is required", "Invalid phone number", etc.

**Cross‑Field / Business Validation**:
- Check that:
  - Patient exists and is eligible for admission.  
  - No active admission exists for the same patient.  
  - Selected bed is available at save time.  
  - Corporate fields are consistent (Corporate Client vs Corporate ID/Payment Type).

**System Errors**:
- For unexpected internal errors (database, service failure, etc.):  
  - Display generic, user‑friendly error message (e.g., "Unable to complete admission. Please try again or contact support.").  
  - Log detailed technical information in server logs with correlation ID.

---

## 13. Integrations & Dependencies

- **IPD Billing**  
  - Admission record provides context (Admission No, Bed, Doctor, Corporate) to billing console.  
  - Corporate flags and IDs drive tariff and discount calculations.  
- **Doctor Visit / Rounds**  
  - Under Doctor and Department are used to list in‑patients per doctor/department for rounds.  
- **OT (Operation Theatre)**  
  - Operation Name and Assist fields help pre‑populate OT booking and scheduling.  
- **Pharmacy**  
  - Admission linkage ensures IPD medication issues are correctly mapped to patient and billing episode.  
- **Patient Health Records / EHR**  
  - Admission creates a new encounter/episode in the patient’s longitudinal record.  
- **Registration Modal Reusability**  
  - The Patient Registration Modal is a shared component across **OPD**, **IPD**, and **Emergency**, with consistent UI and validation behaviors.

