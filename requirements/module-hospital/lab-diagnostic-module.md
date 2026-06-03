## LAB & Diagnostic Module – Patient-ID Wise Process

### 1. Overview

**Description**  
The LAB & Diagnostic Module manages end-to-end diagnostic workflows (laboratory tests, microbiology, and basic imaging such as USG & X‑Ray) in a **patient‑centric, Patient‑ID wise** manner. Every action – order, sample, barcode, test, result, report, and audit entry – is strictly linked to a **single Patient ID**, ensuring traceability, safety, and legal defensibility.

**Primary Objective**  
Provide a robust, auditable, Patient‑ID wise diagnostic workflow that:

- Eliminates orphan samples, results, and reports.
- Ensures that all diagnostic activities are traceable to the correct patient and clinical encounter.
- Supports integration with analyzers and imaging systems while maintaining a consistent Patient‑ID based data model.

**Core Rule (Very Important)**  
- **All LAB activities must be linked to a single Patient ID.**  
- **No sample, barcode, report, or result can exist without a valid Patient ID.**

> If Patient ID is missing or invalid, the system must block: order creation, barcode generation, sample collection, result entry, verification, and report publication.

---

### 2. Core Identifiers (Patient‑Centric Model)

#### 2.1 Identifier Definitions

| Identifier            | Level            | Purpose / Description                                                               | Key Rules                                                                                  |
|-----------------------|------------------|--------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| **Patient ID**        | Patient          | Master identity of the patient (OPD / IPD / Emergency).                             | Mandatory parent for **all** lab and imaging data. One patient may have multiple visits.   |
| **Visit / Encounter ID** | Clinical Visit | Specific visit/admission under which orders are raised (OPD visit, IPD admission).  | Links lab orders to clinical context, billing rules, and treating doctor.                  |
| **Lab Order ID**      | Order / Requisition | Represents a diagnostic request from a doctor (one or many tests).             | Must be linked to Patient ID and Visit ID. No result without a valid Lab Order.            |
| **Sample ID**         | Specimen         | Represents a physical sample (tube/container) collected from the patient.           | One Lab Order can generate one or more Sample IDs. Each Sample ID gets a unique barcode.   |
| **Report ID**         | Report           | Final, consolidated lab/imaging report (may group multiple tests/samples).          | Each Report ID must be linked to Patient ID, Visit ID, and one or more Lab Orders.         |

#### 2.2 Parent–Child Relationship

- **Patient ID is the root parent of everything.**
  - Patient ID → Visit/Encounter ID(s)
  - Visit/Encounter ID → Lab Order ID(s)
  - Lab Order ID → Sample ID(s)
  - Sample ID → Test(s) → Parameter Result(s)
  - Report ID consolidates one or more orders/tests for the same Patient ID.
- System must enforce referential integrity:
  - Cannot create a Lab Order without a valid Patient ID and (where applicable) Visit ID.
  - Cannot create a Sample ID without a Lab Order.
  - Cannot create a Report ID without at least one verified test result belonging to that Patient ID.

---

### 3. LAB Setup & Configuration (Patient‑Independent Masters)

These are **one‑time or infrequently changed master configurations**, not tied to a specific patient. They are required before operational workflows can start.

#### 3.1 Barcode Sticker Configuration

- Define **barcode format**:
  - Components (Patient ID, Sample ID, Sample Type, Order ID, Lab Location Code, Date/Time, Checksum).
  - Encoding standard (e.g., Code‑128, QR).
  - Font size, label dimensions, label count per sheet/roll.
- Rules:
  - Barcode content must uniquely identify the Sample ID and implicitly link to the Patient ID.
  - No two active samples may share the same barcode.
  - Re‑print must always generate the **same** barcode for an existing Sample ID and log the reprint.
  - Configuration must allow:
    - Prefix/suffix per lab location (e.g., main lab vs satellite lab).
    - Optional masking of Patient Name on barcode labels to protect privacy.

#### 3.2 Vacutainer & Sample Container Setup

- Maintain master for:
  - Vacutainer types (e.g., EDTA, Fluoride, Plain, Citrate, Heparin).
  - Container types (e.g., sterile container, swab, blood culture bottle, urine container).
  - Color code, volume, manufacturer code, and storage conditions.
- Map **Test / Panel → Required Vacutainer(s) / Container(s)**:
  - One test may require multiple containers (e.g., blood + urine).
  - One container may serve multiple compatible tests.
  - Mapping may be organized by **Test Group** and **Vacutainer Group** (e.g., CBC panel → EDTA tube; Biochemistry panel → Plain/Serum tube) for efficient configuration.
- Business rules:
  - At sample collection, system should show required container types based on selected tests.
  - If selected container is not configured for a test, system should warn or block collection.

#### 3.3 Test & Parameter Master Setup

- **Test Master**
  - Test Code, Test Name, Department (Biochemistry, Hematology, Microbiology, Serology, Virology, Pathology, USG, X‑Ray, Other).
  - Sample Type (Serum, Plasma, Whole Blood, Urine, Stool, Sputum, Swab, CSF, etc.).
  - Turnaround Time (TAT) – standard and priority.
  - Method (e.g., ELISA, Chemiluminescence, Manual, Culture, Ultrasound).
  - Panic value definitions and critical alerts configuration.
  - Billing link (charge code, tariff group).
  - Active/Inactive flag and effective date range.
- **Parameter Master**
  - Parameter Code, Name, Unit of Measure.
  - Reference ranges by:
    - Age group, Sex, Patient category (e.g., pediatric, adult, pregnancy).
  - Interpretation flags:
    - High/Low/Critical indicators.
    - Auto‑comments or interpretation notes (optional).
  - Display order in report and grouping (e.g., CBC panel sections).

#### 3.4 Analyzer / Channel Setup

- Analyzer Master:
  - Analyzer ID, Name, Type (Biochemistry, Hematology, etc.), Manufacturer, Model.
  - Communication protocol (e.g., ASTM, HL7).
  - Online/Offline mode, Interface status.
- Channel/Test Mapping:
  - Map analyzer channels to internal Test/Parameter codes.
  - Maintain factor/conversion rules if analyzer units differ from reporting units.
- Rules:
  - Results from analyzer must always be mapped to **Patient ID + Sample ID + Parameter**.
  - If mapping fails, result should land in an **“Unmapped/Validation Queue”** and cannot be finalized until corrected.

#### 3.5 Lab Signature / Pathologist Configuration

- Maintain master for:
  - Pathologists and authorized signatories (Name, Qualification, Registration Number, Signature image, Digital certificate details where applicable).
  - Department and scope of authorization (e.g., Biochemistry reports, Histopathology, Microbiology, Imaging).
- Configurable rules:
  - Certain tests or departments may require **double verification** (e.g., technician entry + pathologist approval).
  - Only configured signatories may digitally sign reports, with full audit.

#### 3.6 External Institute / Outsource Lab Master

- Maintain master for external reference labs used for outsourced tests:
  - Institute ID, Name, Address, Contact details.
  - Tests offered (mapping to internal Test Master).
  - Expected TAT per test or category.
  - Transport/courier rules.
  - Billing/pricing for outsourced tests.
- For each test in Test Master:
  - Flag as **In‑House** or **Outsourced**.
  - If outsourced: link to External Institute.
- Used by the Out Sample workflow (see Section 4.7).

#### 3.7 USG & X‑Ray Format Templates

- Template library for imaging reports:
  - Report headers (lab name, address, license numbers).
  - Structured sections (Clinical Indication, Technique, Findings, Impression/Conclusion).
  - Department‑specific templates (e.g., obstetric USG, abdomen USG, chest X‑Ray).
- Each template must support:
  - Free‑text areas and standardized pick‑lists.
  - Consultant signature and registration details.
  - Versioning with effective dates to preserve historical format.

---

### 4. Patient‑ID Wise LAB Workflow (End‑to‑End)

The following steps define the **core operational workflow**. Each step must enforce Patient‑ID linkage and maintain status transitions for **Lab Order**, **Sample**, and **Report**.

#### 4.1 Step 1 – Patient Registration (Pre‑Requisite)

- Patient is registered through OPD / IPD / Emergency registration.
- System generates or fetches:
  - **Patient ID** (master identifier).
  - **Visit / Encounter ID** (for current visit/admission).
- Rules:
  - **No Lab Order, Barcode, Sample, Result, or Report can be created without a valid Patient ID.**
  - If Lab front desk tries to create an order without Patient ID:
    - System must block the action and prompt to search/create a patient.

#### 4.2 Step 2 – Lab Order Entry (Doctor / Requesting Unit)

- Actor: Doctor, nurse, or authorized requester.
- System captures:
  - Patient ID (mandatory).
  - Visit/Encounter ID (mandatory where applicable).
  - Ordering Doctor, Department, Location (OPD/IPD/Ward/Emergency).
  - Test(s)/Panel(s) requested and priority (Routine / Stat / Emergency).
  - Clinical notes / provisional diagnosis (optional but recommended).
- Outputs:
  - **Lab Order ID** generated.
  - Status: **Order Created / Pending Collection**.
- Rules:
  - System must validate that requested tests are active and correctly configured.
  - Mandatory checks:
    - Duplicate orders within defined time window can be flagged (configurable).
    - For restricted tests, require approval (e.g., senior consultant or pathologist).

#### 4.3 Step 3 – Barcode & Sample ID Generation (Patient‑ID Wise)

- Based on Lab Order ID and configured tests:
  - System determines required **Sample Types** and **Containers**.
  - For each sample, system generates a unique **Sample ID**.
  - Barcode label(s) are generated, containing at minimum:
    - Patient ID (masked or full per configuration).
    - Patient Name (optional).
    - Sample ID.
    - Sample Type.
    - Collection location.
    - Date/Time or encoded timestamp.
- Rules:
  - One patient can have multiple Sample IDs across one or more Lab Orders.
  - Re‑printing:
    - Allowed for damaged/lost labels but must be logged with user, date/time, and reason.
  - Barcodes must be scannable at all downstream steps (collection, sending, receiving, analyzer, result entry).

#### 4.4 Step 4 – Sample Collection

- Actor: Phlebotomist / Nursing staff / Collection center operator.
- Workflow:
  - Search/scan using Patient ID, Visit ID, or Lab Order ID.
  - Confirm patient identity at bedside or collection point (minimum: name + age + Patient ID or per hospital policy).
  - Collect samples per the generated Sample IDs and required containers.
  - Attach barcode labels to physical containers.
  - Capture collection details:
    - Collected by, Date/Time, Location (ward/OPD/collection center).
    - Any pre‑analytical notes (e.g., non‑fasting, hemolyzed appearance, insufficient sample).
- Status:
  - Sample Status: **Collected**.
  - Lab Order Status: **Partially Collected** or **Fully Collected**, depending on number of samples.
- Rules:
  - System must prevent marking sample as collected without valid Sample ID/Barcode.
  - If patient refuses or sample cannot be collected:
    - Order/tests can be marked as **Collection Failed / Not Done** with reason.

#### 4.5 Step 5 – Sample Dispatch / Send to Lab

- Actor: Collection center / Ward nurse / Transport staff.
- Workflow:
  - Group one or more samples into a **dispatch batch** (optional but recommended).
  - Record:
    - From Location, To Lab (e.g., Main Lab, Reference Lab).
    - Dispatch Date/Time, Dispatched By.
  - Scan Sample IDs to include them in the batch.
- Status:
  - Sample Status: **In Transit**.
- Rules:
  - Tracking continues via **Patient ID + Sample ID**.
  - System should allow printing of dispatch manifest with all Sample IDs and Patient IDs (or masked IDs as per policy).

#### 4.6 Step 6 – Lab Receive

- Actor: Lab receiving desk.
- Workflow:
  - Scan Sample ID / Barcode upon arrival.
  - System displays:
    - Patient ID, Name (per access policy).
    - Sample Type, Ordered Tests, Dispatch details.
  - Lab staff verifies:
    - Correct Patient ID and sample labeling.
    - Correct sample type and container.
    - Sample condition (leakage, clotting, insufficient volume, wrong container, etc.).
- Possible actions:
  - **Accept Sample**:
    - Sample Status: **Received**.
  - **Reject Sample**:
    - Capture rejection reason (configurable master list).
    - Decide whether to:
      - Mark tests as **Cancelled (Sample Rejected)**, or
      - Trigger **Re‑collection Required** status for specific tests.
- Rules:
  - Rejection must be logged **patient‑wise and sample‑wise**.
  - Notifications can be sent to ward/ordering doctor for rejected samples.

#### 4.7 Step 6a – Out Sample / External Lab (Outsourced Tests)

When tests are performed by an external reference lab rather than in-house, the system shall support the full out-sample workflow while maintaining **Patient‑ID wise traceability**.

- **External Institute Master**
  - Maintain master for external/reference labs:
    - Institute ID, Name, Address, Contact (phone, email).
    - Tests offered (mapping to internal Test Master).
    - Expected turnaround time (TAT) per test or category.
    - Transport/courier rules (pickup frequency, delivery expectations).
    - Billing/pricing configuration for outsourced tests.
  - For each test in the Test Master:
    - Flag as **In‑House** or **Outsourced**.
    - If outsourced: link to External Institute and optionally override TAT.

- **Sending Samples Out**
  - Actor: Lab staff / dispatch operator.
  - Workflow:
    - Identify samples/tests marked for external lab (via Test Master configuration).
    - Create **Out Sample Batch**:
      - External Institute, Dispatch Date/Time, Dispatched By.
      - List of Sample IDs (and hence Patient IDs) included.
    - Print dispatch manifest with:
      - Patient ID (masked or full per policy), Sample ID, Test(s), External Institute.
    - Record courier/transport reference if applicable.
  - Status:
    - Sample Status: **Sent to External Lab** (or equivalent).
    - Test Status: **Pending External Result**.
  - Rules:
    - Every out sample must remain linked to **Patient ID + Lab Order ID + Sample ID**.
    - System must prevent sending a sample out without valid Patient ID and Sample ID.

- **Receiving & Posting Results from External Lab**
  - Actor: Lab staff receiving external reports.
  - Workflow:
    - Receive result (paper report, electronic file, or manual entry).
    - Match result to correct **Patient ID + Sample ID + Test** (e.g., via barcode, requisition number, or manual search).
    - Enter or import parameter values into the system.
    - System applies same validation as in-house:
      - Reference ranges, panic flags, critical alerts.
    - Pathologist/authorized user verifies and signs.
  - Status:
    - Test Status: **Result Entered (Draft)** → **Verified** → **Reported**.
  - Rules:
    - Results from external lab **cannot** be posted without matching a valid Lab Order and Sample ID (and hence Patient ID).
    - If matching fails, result must be held in an **Unmatched External Results Queue** until corrected.
    - Source of result (external institute name, receive date) must be stored and visible on the report.

- **Patient ID Linkage & Traceability**
  - All out-sample data must be queryable by Patient ID.
  - Audit trail must record:
    - When sample was sent out, to which institute.
    - When result was received and by whom.
    - Any manual corrections or re-entry.
  - Reports must clearly indicate which tests were performed externally (e.g., "Performed at: XYZ Reference Lab").

- **TAT & Reporting (Optional)**
  - System may track:
    - Expected TAT vs actual TAT for external tests.
    - Pending external results by institute, by age (days since dispatch).
  - Dashboards or worklists for:
    - Samples awaiting return from external lab.
    - Overdue external results.

---

### 5. Patient‑ID Wise Test Processing & Result Management

#### 5.1 Step 7 – Test Execution

- Types of test processing:
  - **Analyzer‑based** (Biochemistry, Hematology, Virology, Immunology, etc.).
  - **Manual Entry** (e.g., microscopy, slide reading, manual tests).
  - **Microbiology** (culture, sensitivity, special stains).
  - **Cross‑matching and Blood Bank** (if within scope).
  - **USG & X‑Ray** (basic imaging).
- Rules:
  - All analyzer or manual results must be stored in the structure:
    - **Patient ID → Visit ID → Lab Order ID → Sample ID → Test → Parameter → Result**.
  - For analyzer integration:
    - Incoming results must match a **Sample ID** (and hence Patient ID).
    - If mapping fails, result must be held in an **Unassigned Queue** and cannot be auto‑posted.
  - System should support **“In Process”** status per test:
    - Test Status: Pending → In Process → Result Entered → Verified → Reported.

#### 5.2 Step 8 – Result Entry (Draft)

- Actor: Lab technician / authorized data entry user.
- Workflow:
  - Open patient results via:
    - Worklist by analyzer, department, test, or Patient ID.
  - Enter / review parameter values for each test:
    - Numeric values, qualitative values (Positive/Negative/Reactive/Non‑Reactive), and comments.
  - System automatically:
    - Evaluates reference ranges and flags **High / Low / Critical / Panic** values.
    - Highlights panic values visually in the UI.
  - Technician may:
    - Save as **Draft** (unverified).
    - Request repeat test / re‑run (with reason).
- Status:
  - Test Status: **Result Entered (Draft)**.
- Rules:
  - Draft results are **not visible** on patient‑facing reports or portals.
  - Panic values must optionally trigger alerts/notifications to responsible clinician as per configuration.

#### 5.3 Step 9 – Doctor (Pathologist) Verification

- Actor: Pathologist or authorized verifying doctor.
- Workflow:
  - Review drafted results **patient‑wise**:
    - View complete picture: current tests, relevant history, previous values, delta checks.
  - Verify or modify results where needed:
    - Any modification must be audited (old value, new value, user, timestamp, reason).
  - Apply digital signature:
    - Single or multiple signatories based on configuration.
- Status:
  - Test Status: **Verified**.
  - Once all required tests under a report grouping are verified:
    - Report Status: **Ready for Print/Publish**.
- Rules:
  - Only authorized roles may verify and sign.
  - System must enforce that **no report is generated without at least one verifying doctor (where required by policy).**

#### 5.4 Step 10 – Report Print / Publish

- Reporting options:
  - **Single Test Report** – for individual tests.
  - **Grouped Report** – panel or department‑wise (e.g., Complete Biochemistry, CBC, Culture report, Combined Lab report for a visit).
- Every report must display at minimum:
  - Patient ID, Patient Name, Age/Sex.
  - Visit/Encounter ID (where applicable).
  - Lab Order ID(s) included.
  - Sample Type and collection/receive times (where relevant).
  - Test names, parameter results, units, reference ranges, flags (High/Low/Critical).
  - Verifying doctor’s name, qualification, registration no., and signature.
- Status:
  - Report Status: **Reported / Published**.
- Rules:
  - Once published, reports should be **read‑only**, with any later corrections handled via an **amended report** mechanism.
  - System must support:
    - Reprint with full reprint log (who, when, where).
    - Controlled visibility in patient/doctor portals based on configuration.

---

### 6. Microbiology (Patient‑Wise)

#### 6.1 Data Structure & Workflow

- Data hierarchy:
  - **Patient ID**
    - Visit / Encounter
      - Lab Order / Sample
        - **Culture**
          - Organism(s)
            - Antibiotic Sensitivity Panel.
- For **Growth** cases:
  - Capture:
    - Organism name (standardized via master list).
    - **Colony count** (e.g., CFU/ml for urine culture; colony description for other specimens).
    - Number/type of organisms (mono‑/poly‑microbial).
    - Sensitivity results:
      - Antibiotic name.
      - Sensitivity (S/I/R or MIC values).
      - Comments/interpretation.
- For **No Growth**:
  - Final result recorded as **“No Growth”** with:
    - Incubation duration.
    - Medium used and relevant comments.
- Rules:
  - All culture results are strictly tied to **Patient ID + Sample ID**.
  - System must allow:
    - Interim reports (e.g., “Preliminary growth seen, identification pending”).
    - Final reports with audit trail of all interim updates.
  - For critical organisms or resistance patterns (e.g., MRSA, ESBL, MDR, XDR), system should:
    - Highlight as critical.
    - Optionally trigger infection control notifications.

---

### 7. USG & X‑Ray (Patient‑Wise Imaging)

#### 7.1 Imaging Order & Scheduling

- Imaging order is raised against:
  - Patient ID and Visit/Encounter ID.
  - Imaging modality (USG, X‑Ray).
  - Body part/region and clinical indication.
- Orders may be:
  - Scheduled (with date/time and room/technician).
  - Walk‑in/emergency (immediate).

#### 7.2 Image Acquisition & Reporting

- Image acquisition:
  - DICOM/PACS integration where available (configurable, may be in broader imaging scope).
  - Images stored under Study/Series referencing Patient ID and Visit ID.
- Reporting:
  - Findings are entered into configured templates (see Section 3.7).
  - Consultant radiologist/sonologist reviews images and finalizes report.
  - Digital signature & registration details must appear on report.
- Rules:
  - Imaging reports must be retrievable in the same **Patient‑ID wise history** view as lab results.
  - If imaging data is sourced from an external system, interface must still ensure Patient‑ID linkage and full audit trail of imported reports.

---

### 8. Patient‑ID Wise Status Flow (High‑Level)

The system shall support clear, trackable status progression at **Patient‑ID and Order/Sample/Report level**:

1. **Patient Registration / Receipt Creation**  
2. **Lab Order Created** (tests requested)  
3. **Barcode & Sample ID Generated**  
4. **Sample Collected**  
5. **Sample Sent to Lab (In Transit)**  
6. **Sample Received in Lab**  
7. **Test In Process**  
8. **Result Entered (Draft)**  
9. **Doctor Verified (Signed)**  
10. **Report Published / Reported**  
11. **Report at Delivery Counter** (optional; when report is received at collection counter)  
12. **Delivered to Patient** (optional; when report is handed to patient/attendant)

For each stage, the system must:

- Maintain timestamps, user IDs, and locations.
- Allow operational dashboards and worklists (pending collection, in transit, pending receive, pending verification, pending delivery, etc.).
- Provide **patient‑wise and status‑wise** filters for monitoring TAT and bottlenecks.

---

### 9. Patient‑Wise Reports, Logs & Audit Trail

#### 9.1 Patient Lab History

- View all diagnostic activity **per patient**:
  - By date range, visit, department, test, or sample type.
  - Including both lab tests and imaging (USG/X‑Ray).
- Ability to:
  - Open and view historical reports (PDF/HTML).
  - Trend numeric parameters over time (e.g., Hb, Creatinine, Glucose).

#### 9.2 Operational & Audit Logs

- **Previous Reports & Reprint Log**
  - Maintain record of:
    - Original report generation (timestamp, user, location).
    - Every reprint (timestamp, user, device/location).
- **Result Modification Log**
  - For each changed result:
    - Old Value, New Value.
    - Changed By, Approved By (if required).
    - Date/Time and Reason for change.
- **Sample Rejection Log**
  - Rejection entries **patient‑wise and sample‑wise**:
    - Reason, Initiated by, Acknowledged by (if applicable).
    - Impacted tests and follow‑up actions (re‑collection, cancellation).

#### 9.3 Compliance & Traceability

- System must maintain a **full audit trail** for all high‑risk operations:
  - Creating, editing, or cancelling Lab Orders.
  - Generating, reprinting, or voiding barcodes.
  - Collecting, dispatching, receiving, and rejecting samples.
  - Entering, editing, and verifying results.
  - Publishing, amending, and reprinting reports.
- Audit data must be:
  - Searchable **by Patient ID**, Sample ID, Lab Order ID, and Report ID.
  - Exportable (based on role/permissions) for internal or regulatory audits.

---

### 10. Mandatory System Controls & Validations

The following controls are **non‑negotiable** and must be enforced at system level:

- **No Sample Without Patient ID**
  - System must block creation of Sample ID or barcode if Patient ID is missing or invalid.
- **No Result Without Lab Order**
  - Analyzer or manual results cannot be saved or finalized unless linked to a valid Lab Order ID and Sample ID.
- **No Report Without Doctor Verification**
  - System must prevent report publication unless:
    - All required tests are in **Verified** status.
    - At least one authorized verifying doctor has signed (as per configuration).
- **Single Patient Ownership**
  - A Sample ID, Lab Order ID, or Report ID can belong to **only one** Patient ID.
  - Any attempt to reassign must be treated as a high‑risk operation, requiring elevated permissions and full audit.
- **Access Control**
  - Role‑based access must control who can:
    - View lab results.
    - Enter/edit results.
    - Verify/sign reports.
    - Amend published reports.
- **Data Integrity & Consistency**
  - Deletion of core entities (Patient, Visit, Lab Order, Sample, Report) is **not allowed** once diagnostic activity exists; only status changes and logical voiding with full audit trail are permitted.

---

### 11. Non‑Functional & Integration Considerations (Lab/Diagnostic Scope)

- **Performance**
  - Lab worklists (pending collection, in process, pending verification) must load within defined performance targets (e.g., ≤ 3 seconds under normal load).
  - System should support high‑volume environments with thousands of samples per day using efficient indexing and pagination.
- **Analyzer / Device Integration**
  - Support for integration with multiple analyzers through:
    - A standard interface engine (e.g., HL7/ASTM over TCP/IP).
    - Configurable mapping tables between analyzer codes and internal Test/Parameter codes.
  - Interface failures (communication errors, mapping errors) must be logged and not silently drop results.
- **Reliability & Data Safety**
  - No loss of results is acceptable; intermediate results should be stored even if not yet verified.
  - System should recover gracefully from partial failures (e.g., network outages to analyzer) without losing linkage to Patient ID.
- **Security & Privacy**
  - Laboratory and imaging data are part of the patient’s EHR and must follow overall security policies:
    - Encryption at rest and in transit (as per project‑wide standards).
    - Strict role‑based access and audit logging.

