# Prescription Management

## Overview

#### 3.1.1 Feature Description
The Prescription Management feature serves as a critical component of the EHR system, providing healthcare providers with comprehensive electronic prescribing (e-prescribing) capabilities. This feature enables the creation, management, transmission, and tracking of electronic prescriptions throughout the entire medication ordering and fulfillment lifecycle - from initial prescription creation through pharmacy dispensing and medication refills.

The Prescription Management feature transforms traditional paper-based prescription writing into a digital, secure, and efficient system that supports evidence-based prescribing, improves medication safety, and enhances care coordination between providers and pharmacies. It serves as the primary mechanism for medication ordering, ensuring that prescriptions are accurately transmitted to pharmacies, drug interactions are identified and managed, and medication adherence is supported through refill management.

#### 3.1.2 Business Value and Benefits
The Prescription Management feature delivers significant value to healthcare organizations, providers, pharmacies, and patients:

**For Healthcare Providers:**
- **Improved Medication Safety**: Drug interaction checking, allergy alerts, and dosage validation help prevent medication errors and adverse drug events
- **Enhanced Efficiency**: Electronic prescription transmission eliminates phone calls and faxes, reducing administrative burden
- **Better Clinical Decision-Making**: Access to formulary information, drug information, and patient medication history enables informed prescribing decisions
- **Regulatory Compliance**: Electronic prescribing supports compliance with prescription regulations, controlled substance requirements, and quality measures
- **Reduced Prescription Errors**: Structured data entry and validation reduce illegible prescriptions and dosing errors
- **Time Savings**: Faster prescription creation and transmission compared to paper prescriptions

**For Healthcare Organizations:**
- **Operational Efficiency**: Reduced time spent on prescription-related phone calls and administrative tasks
- **Cost Reduction**: Decreased costs associated with paper prescriptions, phone calls, and prescription errors
- **Quality Improvement**: Improved medication safety and adherence metrics
- **Regulatory Compliance**: Support for e-prescribing mandates and quality reporting requirements
- **Data Analytics**: Structured prescription data enables medication utilization analysis and quality metrics
- **Risk Management**: Complete prescription documentation and audit trails support risk management

**For Pharmacies:**
- **Improved Efficiency**: Electronic receipt of prescriptions eliminates manual data entry and reduces errors
- **Faster Processing**: Prescriptions received electronically can be processed more quickly
- **Better Accuracy**: Structured electronic prescriptions reduce transcription errors
- **Enhanced Communication**: Electronic communication with prescribers for refill requests and clarifications
- **Inventory Management**: Earlier visibility into prescription orders supports inventory planning

**For Patients:**
- **Improved Safety**: Reduced medication errors and adverse drug events
- **Faster Service**: Prescriptions available at pharmacy when patient arrives
- **Better Adherence**: Easier refill process and medication reminders support adherence
- **Cost Transparency**: Formulary checking helps identify cost-effective medications
- **Convenience**: Reduced need for paper prescriptions and pharmacy phone calls

#### 3.1.3 Key Capabilities
The Prescription Management feature encompasses the following core capabilities:

1. **Electronic Prescription Creation**
   - Comprehensive prescription entry with medication selection, dosage specification, and instructions
   - Drug information lookup and formulary checking
   - Template-based prescription creation for efficiency
   - Prescription copying and modification
   - Batch prescription creation for multiple medications

2. **Drug Safety and Interaction Checking**
   - Real-time drug-drug interaction checking
   - Drug-allergy interaction checking
   - Drug-disease contraindication checking
   - Dosage appropriateness validation
   - Duplicate therapy detection
   - Age-appropriate and weight-based dosing validation

3. **Electronic Prescription Transmission**
   - Direct electronic transmission to pharmacies via e-prescribing networks (e.g., Surescripts)
   - Pharmacy selection and management
   - Prescription status tracking (sent, received, filled)
   - Transmission confirmation and error handling
   - Support for multiple transmission methods (electronic, fax, print)

4. **Prescription Refill Management**
   - Pharmacy-initiated refill requests
   - Patient-initiated refill requests (via portal if available)
   - Provider-initiated refills
   - Refill approval workflow
   - Refill history tracking
   - Automatic refill authorization (for eligible medications)

5. **Controlled Substances Management**
   - Enhanced security for Schedule II-V controlled substances
   - DEA number validation
   - State-specific controlled substance requirements
   - Prescription Drug Monitoring Program (PDMP) integration
   - Quantity and duration limits enforcement
   - No refills for Schedule II (unless state allows)

6. **Prescription History and Tracking**
   - Complete prescription history per patient
   - Prescription status tracking throughout lifecycle
   - Fill status and date tracking
   - Prescription modification and cancellation
   - Prescription replacement functionality

7. **Formulary and Drug Information**
   - Insurance formulary checking
   - Drug information database integration
   - Alternative medication suggestions
   - Cost information (if available)
   - Generic substitution options

8. **Integration with Patient Records**
   - Automatic medication list updates
   - Link prescriptions to diagnoses/problems
   - Include prescriptions in patient summary views
   - Medication history integration

#### 3.1.4 User Workflows
The Prescription Management feature supports several key clinical workflows:

**New Prescription Creation Workflow:**
1. Provider accesses patient record
2. Provider reviews patient's current medications and allergies
3. Provider selects medication from drug database
4. System checks for drug interactions and allergies
5. System displays alerts if interactions or allergies detected
6. Provider reviews alerts and acknowledges or overrides (with documentation)
7. Provider specifies dosage, frequency, route, and duration
8. Provider adds special instructions if needed
9. Provider selects pharmacy
10. Provider reviews prescription summary
11. Provider signs and sends prescription electronically
12. System transmits prescription to pharmacy
13. System confirms transmission and updates prescription status
14. Prescription appears in patient's medication list

**Prescription Refill Workflow:**
1. Pharmacy or patient requests refill
2. System routes refill request to prescribing provider
3. Provider reviews refill request
4. Provider reviews patient's current status and medications
5. Provider approves, denies, or modifies refill
6. If approved, system processes refill and notifies pharmacy
7. If denied, system notifies pharmacy with reason
8. Refill history updated

**Controlled Substance Prescription Workflow:**
1. Provider creates prescription for controlled substance
2. System validates DEA number
3. System checks PDMP (if integrated) for patient's controlled substance history
4. System displays PDMP information to provider
5. Provider reviews PDMP information
6. System enforces quantity and duration limits
7. Provider completes prescription with required information
8. System applies enhanced security measures
9. Prescription transmitted with controlled substance indicators
10. Prescription tracked with additional audit requirements

**Prescription Modification Workflow:**
1. Provider identifies need to modify existing prescription
2. Provider accesses prescription
3. Provider modifies prescription details (if not yet sent) or creates replacement prescription
4. System validates modifications
5. System checks for new interactions or issues
6. Provider signs modified or replacement prescription
7. System transmits to pharmacy
8. Original prescription cancelled or replaced (if applicable)

#### 3.1.5 Integration Points
The Prescription Management feature integrates with several other system components and external systems:

- **Patient Health Records**: Shares medication history and allergy information; receives updates when prescriptions are created or discontinued
- **E-Prescribing Networks (Surescripts)**: Transmits prescriptions to pharmacies and receives refill requests
- **Pharmacy Systems**: Receives prescriptions and sends refill requests; may receive fill status updates
- **Pharmacy Benefit Managers (PBM)**: Checks formulary and eligibility; may receive prior authorization requests
- **Prescription Drug Monitoring Programs (PDMP)**: Queries patient's controlled substance history
- **Drug Information Databases**: Accesses drug information, interactions, and dosing guidelines
- **Laboratory Systems**: May receive medication-related lab results for monitoring
- **Billing Systems**: May share prescription information for claims processing (future enhancement)

#### 3.1.6 Data Lifecycle
Prescription data follows a comprehensive data lifecycle:

1. **Creation**: Prescription created by provider with all required information
2. **Validation**: Prescription validated for interactions, allergies, and appropriateness
3. **Transmission**: Prescription transmitted electronically to pharmacy
4. **Processing**: Pharmacy processes prescription and may send status updates
5. **Fulfillment**: Prescription filled by pharmacy (status updated if pharmacy reports)
6. **Refills**: Prescription refilled as authorized (refill count tracked)
7. **Modification**: Prescription modified or replaced if needed
8. **Completion**: Prescription completed when all refills used or expired
9. **Discontinuation**: Prescription discontinued if medication stopped
10. **Retention**: Prescription data retained according to legal and regulatory requirements (typically 6-10 years)
11. **Archival**: Historical prescriptions archived for long-term storage while maintaining accessibility

#### 3.1.7 Security and Privacy Considerations
The Prescription Management feature handles highly sensitive Protected Health Information (PHI) and prescription data and must implement robust security measures:

- **Access Control**: Role-based access ensures only authorized prescribers can create prescriptions
- **Audit Logging**: All prescription activities (creation, modification, transmission, refills) are logged with user identification and timestamp
- **Data Encryption**: All prescription data encrypted at rest and in transit
- **Controlled Substance Security**: Enhanced security measures for controlled substance prescriptions
- **DEA Validation**: DEA numbers validated for controlled substance prescriptions
- **Prescription Integrity**: Prescriptions cannot be modified after transmission without proper authorization
- **Pharmacy Verification**: Pharmacy identity verified before transmission
- **Patient Privacy**: Prescription information protected according to HIPAA requirements

#### 3.1.8 Compliance and Standards
The Prescription Management feature must comply with:

- **HIPAA Privacy and Security Rules**: Protection of PHI and patient privacy rights
- **DEA Regulations**: Drug Enforcement Administration requirements for controlled substances
- **State Prescription Regulations**: State-specific requirements for prescriptions and controlled substances
- **E-Prescribing Standards**: NCPDP SCRIPT standard for electronic prescription transmission.
  - **Version (pinned)**: **NCPDP SCRIPT 2017071** is the minimum required version and the current Surescripts production standard. SCRIPT 10.6 may be used only for legacy pharmacy integrations where the target pharmacy does not support 2017071, and only with explicit approval. Future upgrade to SCRIPT 2023011 shall be evaluated when Surescripts mandates it.
  - Implementors must not use "NCPDP SCRIPT standard" without referencing the specific version in code, configuration, and API documentation.
- **Formulary Standards**: Support for formulary checking and prior authorization
- **PDMP Requirements**: State-specific Prescription Drug Monitoring Program requirements
- **Quality Measures**: Support for e-prescribing quality measures and reporting

#### 3.1.9 Success Metrics
The success of the Prescription Management feature will be measured by:

- **Adoption Rate**: Percentage of prescriptions created electronically vs. paper/fax
- **Transmission Success Rate**: Percentage of prescriptions successfully transmitted electronically (>99% target)
- **Error Reduction**: Reduction in prescription errors (illegible, incorrect dosage, etc.)
- **Interaction Detection**: Percentage of potential drug interactions detected and addressed
- **Refill Processing Time**: Average time to process refill requests
- **User Satisfaction**: Provider and pharmacy satisfaction scores with the system
- **Compliance**: Successful passing of regulatory audits and inspections
- **Medication Safety**: Reduction in medication-related adverse events
- **Efficiency**: Time savings in prescription creation and transmission

### 3.2 Functional Requirements

#### 3.2.0 Easy Prescription Module – Doctor Portal (HMS)

> This subsection refines the general prescription capabilities above into a **fast, doctor-facing OPD/IPD prescribing experience** that can typically complete a prescription in **1–2 minutes**, while re‑using the same safety, integration, and data model requirements already defined in this document.

- **EP-1 Doctor Roles and Access**
  - Primary prescriber is a **Doctor** (as per Doctor Master/Doctor Portal).
  - Optional **Nurse Assist Mode**:
    - Nurse can prepare a draft prescription on behalf of a doctor.
    - Final review and sign‑off must always be by a doctor with `Prescription Status = Yes` and appropriate role.
  - **Admin** may configure masters and defaults (medicines, dose formats, instruction lists, templates, etc.) but **cannot issue prescriptions**.

- **EP-2 Doctor Dashboard – Prescription View**
  - For logged‑in doctors, the system shall provide a **Prescription view** (in Doctor Portal / EHR UI) showing:
    - **Today’s patient list** for OPD and IPD under that doctor, with:
      - Patient Name, Age/Gender, UHID/MRN.
      - Visit type (OPD/IPD), visit/encounter date & time.
      - Queue status (Checked‑in/Waiting/In consultation/Completed), where available.
    - **Search** within the list and globally by:
      - UHID/MRN.
      - Patient name (with partial match).
      - Mobile number (if available).
    - **Recent prescriptions** written by the doctor (e.g., last 20–50), with:
      - Quick open and “Copy & edit” actions.
    - **Favorite templates** (as defined in Prescription Templates below) with one‑click access into prescription creation.
  - This dashboard shall be **responsive and tablet‑friendly** (usable on desktop, tablet, and large‑screen mobile browsers).
  - **OPD / IPD Context Persistence**: When a prescription is created from the Easy Prescription screen, the system shall record the encounter mode (`OPD` or `IPD`) on the prescription record (`ep_encounter_mode` column on `ehr.prescriptions`). This allows:
    - EP-11 adoption metrics to be segmented by care setting (e.g., “X% of OPD prescriptions created via Easy Prescription vs full form”).
    - Future analytics and audit queries to differentiate outpatient prescribing from inpatient prescribing without requiring a join back to the encounter table.
    - The value is set at prescription creation time from the encounter context and is **read-only** after that point. Allowed values: `OPD`, `IPD`. Null is permitted for prescriptions created outside the Easy Prescription workflow (e.g., via API or legacy flows).
    - **Immutability enforcement — two-layer requirement**:
      1. **Database layer (primary)**: A PostgreSQL `BEFORE UPDATE` trigger on `ehr.prescriptions` must raise an exception if an `UPDATE` statement attempts to change `ep_encounter_mode` from a non-null value to a different value. This prevents bypass by any direct SQL client or batch process regardless of application-layer controls. The trigger body (reference implementation, see changeset `048-ep-encounter-mode-immutable.sql`):
         ```sql
         IF OLD.ep_encounter_mode IS NOT NULL AND NEW.ep_encounter_mode IS DISTINCT FROM OLD.ep_encounter_mode THEN
             RAISE EXCEPTION 'ep_encounter_mode is immutable after initial set (old=%, attempted=%)',
                 OLD.ep_encounter_mode, NEW.ep_encounter_mode;
         END IF;
         ```
      2. **Application layer (secondary)**: The `updatePrescription` service method must explicitly guard against updates to `ep_encounter_mode` and throw an `HTTP 422 Unprocessable Entity` with error code `EP_ENCOUNTER_MODE_IMMUTABLE` if a caller attempts to change a non-null value. This application-layer check must not be removed — it is the user-facing enforcement point and the source of the structured error response; the DB trigger is the last-resort backstop.
      - **Rationale**: Without the DB-layer trigger, a refactoring that removes or bypasses the application-layer check would silently allow ep_encounter_mode to change, corrupting OPD/IPD segmentation metrics and violating audit integrity. The two-layer approach is a defence-in-depth requirement, not optional.

- **EP-3 One‑Screen Prescription Creation (Quick Mode)**
  - System shall support a **single‑screen “Easy Prescription” layout** optimized for minimal clicks and typing, built on top of existing functional requirements (`FR-P1.x`, `FR-P1.9`, validation, safety, etc.).
  - **Patient Header (auto‑filled)**:
    - Patient Name.
    - Age/Gender.
    - UHID/MRN.
    - Visit/Encounter Date & Time.
    - Consulting Doctor Name & Department.
  - Header information shall be read‑only (coming from Encounter/EHR) with clear display; corrections must be done in the source modules, not inside the prescription screen.

- **EP-4 Clinical Information (Optional but Fast)**
  - **Complaints (C/C)**:
    - Multi‑select list of common complaints per specialty/department, configurable by Admin.
    - Free‑text box for additional or uncommon complaints.
  - **Clinical Notes**:
    - Short free‑text notes/findings area.
    - May re‑use or link to full EHR progress notes where supported.
  - **Diagnosis**:
    - Searchable **ICD‑10** list with:
      - Auto‑complete search by code and description.
      - Doctor‑specific favorites for quick access.
    - Ability to mark primary vs secondary diagnoses.
  - These sections shall be **optional** in Easy Prescription mode so that a doctor can issue a minimal prescription quickly if clinical documentation is captured elsewhere in the EHR.

- **EP-5 Medicine Prescription (Core Easy Prescription Section)**
  - The Easy Prescription screen shall embed the existing medication search and entry capabilities (`FR-P1.2`, `FR-P1.3`, `FR-P1.9`) in a compact layout.
  - **Medicine Search**:
    - Search by **brand** or **generic** name (both supported).
    - Auto‑suggestions as the doctor types.
    - Doctor favorite medicines list for quick selection.
    - Recently prescribed medicines for that doctor and/or for that diagnosis (where available).
  - **Medicine Fields (Quick Entry)**:
    - Medicine name.
    - Strength (auto‑filled from selection; editable if allowed).
    - Dose pattern using shorthand formats (e.g. `1-0-1`, `0-1-0`, `1-1-1`, PRN) mapped internally to frequency rules.
    - Duration in days (with ability to specify weeks/months if needed).
    - High‑level instructions such as **Before Food / After Food / With Food**, and free‑text special instructions.
  - **Smart Features** (leveraging existing safety engine where applicable):
    - Duplicate medicine warning (same drug / same class).
    - Dose safety/maximum dose alerts (configurable, optional).
    - **Auto‑calculate quantity** from dose + frequency + duration, with ability to override.
    - Highlight allergy and interaction alerts from Patient Health Records when adding or editing medicines.

- **EP-6 Advice, Tests, Follow‑Up & Referral**
  - **Advice & Instructions**:
    - Quick‑select buttons for common advice statements (e.g., rest, hydration, diet advice), configurable per department.
    - Free‑text notes for additional instructions.
  - **Investigation / Test Advice**:
    - Search for lab tests and diagnostic procedures.
    - Preset diagnostic **test groups/panels** (e.g., CBC, LFT, renal profile) for one‑click selection.
    - Ability to mark orders as **Urgent** or **Routine**.
    - Orders created here shall integrate with corresponding Lab/Radiology modules as per their requirements.
  - **Follow‑Up & Referral**:
    - Next visit date (absolute date or time window, e.g. “after 1 week”).
    - Follow‑up notes for the patient and/or internal notes.
    - Optional referral information:
      - Referral doctor (internal).
      - External doctor/hospital/clinic (free‑text and/or master).

- **EP-7 Prescription Templates and Reuse**
  - The Easy Prescription module shall leverage and extend existing template features (`FR-P1.9`):
    - **Doctor Templates**:
      - Disease‑wise templates (e.g., “URTI adult”, “Type 2 DM follow‑up”).
      - Reusable sets of medicines, diagnostic tests, advice, and follow‑up instructions.
    - **Default / System Templates**:
      - OPD standard visit template.
      - Follow‑up visit template.
      - Emergency/STAT template.
  - Doctors shall be able to:
    - Save current prescription as a reusable template (subject to admin rules).
    - Apply a template with one click and then edit before finalization.
    - Manage (view, edit, deactivate) their own templates.

- **EP-8 Save, Print, Share & Previous Prescriptions**
  - **Save & Finalize**:
    - Auto‑save **drafts** while the doctor is editing (with clear indication that the prescription is in draft state).
    - Explicit action to **Finalize / Sign** a prescription; after finalization:
      - Clinical content becomes **read‑only**, except via explicit modification workflow with audit trail.
  - **Print & Digital Output**:
    - Print options:
      - Full A4.
      - Compact/half‑page layout.
    - Generate and store **PDF** version of the prescription.
    - Support sharing prescription to the **Patient Portal** for eligible patients.
  - **Previous Prescription View & Reuse**:
    - Timeline view of a patient’s previous prescriptions for that doctor (and optionally others, as per policy).
    - Ability to **copy a previous prescription** into a new one and edit before finalization.
    - Clear indication of which version is final and which are drafts; maintain **version history**.
  - **Auto-Save Concurrency Model** *(resolves EP-10 "partial saves and merges" requirement)*:
    - Auto-save shall operate on a **server-side DRAFT** record, not only local/browser storage, so that a draft started on one device is recoverable on another.
    - The server shall use **optimistic locking** on the prescription draft: each save request must include the current `version` (integer) of the record; if the version submitted does not match the database version, the server shall reject the save with HTTP 409 (Conflict) and return the current server version of the draft.
    - The UI shall, on receiving a 409:
      1. Pause further auto-saves.
      2. Display a **merge conflict dialog** showing what changed on the server vs. what the user has typed.
      3. Allow the user to (a) keep their version, (b) keep the server version, or (c) manually resolve field-by-field.
      4. Resume auto-save only after conflict resolution.
    - **Nurse Assist + Doctor co-edit scenario** (EP-1): Only one user may hold the draft in EDIT mode at a time. When a second user opens the draft, they receive a read-only view with a notice "Currently being edited by [Name]". The editing lock shall expire automatically after **10 minutes** of inactivity (configurable by Admin); the locked user receives a warning 2 minutes before expiry.
    - **Auto-save trigger strategy** — reconciling UX debounce and server load:
      - The implementation uses a **1.8-second debounce** on every localStorage change, meaning the server is called within ~2 seconds after the user stops typing. This is the **approved approach** because it minimises data loss on browser crash and provides responsive “Saved” feedback.
      - However, an unbounded debounce on every keystroke will generate excessive API load on busy wards. The following constraints make it safe:
        - **Server-side rate limiting**: The workspace PUT endpoint (`/api/hospital/easy-prescription/workspace/{userId}`) must enforce a per-user rate limit of **at most 1 write per 2 seconds** (HTTP 429 with `Retry-After: 2` if exceeded). The client must honour `Retry-After` and back off rather than retry immediately.
        - **Payload diff check**: Before sending a PUT, the client must compare the serialised payload to the last successfully persisted payload. If they are byte-identical, the PUT must be **skipped entirely**. This eliminates saves triggered by re-renders or focus changes that did not change data.
        - **High-risk field immediate flush**: On blur of high-risk fields (medication name, dosage strength, controlled substance flag), the client must bypass the debounce and flush immediately, regardless of the 2-second rate limit window (these events are infrequent and safety-critical).
      - **Minimum server-side save interval**: As an additional backstop, if debounce-triggered saves are suppressed by rate limiting, the client must guarantee a save attempt at least every **30 seconds** even if the user is still actively typing (the 30-second interval cited in earlier EP-8 bullets serves as this ceiling guarantee, not the primary mechanism).
    - A `last_auto_saved_at` timestamp and `version` shall be visible in the UI footer so the user knows when their draft was last persisted.
    - On browser close or navigation away with unsaved changes, the UI shall attempt a final save and display a browser warning if it fails.

- **EP-9 Admin Configuration for Easy Prescription**
  - Admin configuration (building on existing masters in this document and Hospital masters) shall at minimum cover:
    - Medicine master (global, with local brand‑generic mappings).
    - Dose formats and common dose patterns (`1-0-1`, `0-1-0`, etc.).
    - Standard instruction lists and advice snippets.
    - Diagnosis/ICD‑10 favorites per department or specialty.
  - **Doctor Preferences**:
    - Default language for print/digital prescriptions (where multi‑language supported).
    - Font size and layout preferences (within safe, printable bounds).
    - Signature and stamp:
      - Configure digital signature image/stamp (subject to hospital policy and regulations).
      - Configuration of whether digital signature is printed on prescription.

- **EP-10 Technical & Compliance Notes (Easy Prescription Context)**
  - **Frontend / Client UX**:
    - Web UI implemented using the hospital’s standard web stack (e.g., React/Angular), optimized for tablets.
    - Mobile Doctor App (e.g., Flutter) may expose the same Easy Prescription workflow with:
      - One‑screen layout adapted to mobile.
      - Offline‑tolerant draft handling where supported by overall technical architecture.
  - **Backend & Performance**:
    - Use existing fast APIs and data model for prescriptions; Easy Prescription is primarily a **UI/workflow layer** on top of them.
    - Auto‑save drafts at safe intervals and on key field changes, with robust handling of partial saves and merges.
    - Enforce role‑based access as defined in Security & Privacy and Doctor Master (“Prescription Status”).
  - **EP Workspace Payload — Schema Evolution Policy**:
    > This section governs the `ep_doctor_workspace.data_json` blob that stores doctor templates, configuration, recent prescriptions, and drafts. Because this blob is persisted server-side and also cached in browser localStorage, schema changes must be backward- and forward-compatible.
    - **Schema versioning**: The JSON payload must carry a top-level `schemaVersion` integer (currently `1`). Every structural change to the payload shape (new top-level key, renamed key, changed nested structure) must increment `schemaVersion`.
    - **Forward compatibility (old server reads new client data)**: If the server-side reader encounters an unrecognised `schemaVersion`, it must **preserve the blob as-is** and return it unchanged. Servers must never strip unknown top-level keys.
    - **Backward compatibility (new client reads old server data)**: Clients must treat any missing key as if it holds its defined default value. Clients must never crash or throw on an unrecognised or missing field — use optional chaining and fallback values throughout.
    - **Migration at read time**: When a client loads a workspace with `schemaVersion` lower than the client’s current schema, it must silently migrate the in-memory payload to the current shape (e.g., rename a field, add a missing key with its default), then write the migrated payload back to the server, incrementing `schemaVersion`. This is a **read-time migration** pattern — no separate offline migration job is needed.
    - **Breaking changes**: Any change that cannot be handled by read-time migration (e.g., removing a field with semantic meaning for older clients) is a **breaking change** and requires a new API path (`/api/hospital/easy-prescription/workspace/v2`) deployed alongside the old path until all clients have upgraded. Breaking changes require a technical design review before implementation.
    - **localStorage cache invalidation**: On login the client must always pull from the server first and overwrite localStorage (pull-on-login). If the pulled payload carries unrecognised top-level keys (from a newer server schema), the client must preserve those keys in its next push so they are not silently dropped.
  - **Compliance & Audit**:
    - Maintain full **prescription version history**, including drafts and edits.
    - Support **doctor digital signatures** as per regulatory requirements.
    - Enforce **edit lock after finalization**, except through controlled amendment workflows with audit logs.
    - Support attachment of scanned/uploaded documents (e.g., external prescriptions) to the encounter, with:
      - Preview and basic transformations (rotate, crop) where supported by the document subsystem.
      - Readability checks as defined in the document imaging module.
    - **EP Workspace GET Audit (HIPAA Access Log Requirement)**:
      > The EP doctor workspace (`GET /api/hospital/easy-prescription/workspace/{organizationId}`) stores draft prescriptions, doctor templates, and recent prescription references — all of which constitute Protected Health Information (PHI). HIPAA § 164.312(b) requires audit controls that record and examine activity on systems containing PHI, which includes **read access**, not only modifications. The following is therefore an enforceable requirement, not a recommendation.
      - Every `GET` request to the EP workspace endpoint **must** be logged in `ehr.ep_doctor_workspace_audit` (or an equivalent audit log table) with the following fields:
        - `action = 'READ'`
        - `user_id` — the authenticated user UUID from the JWT token
        - `organization_id` — the organization ID from the request path
        - `accessed_at` — server-side UTC timestamp
        - `ip_address` — caller IP (from `X-Forwarded-For` or `RemoteAddr`)
        - `http_status` — the response status code (to detect failed access attempts)
      - Audit log entries for GET operations must be written **regardless of whether the response was a cache hit or a DB read** — the access itself must be logged.
      - Audit log entries must be written even when the response is HTTP 404 (workspace not found) or HTTP 403 (forbidden) — failed access attempts are as important to audit as successful ones.
      - The audit table must **not** store the workspace payload itself (to avoid duplicating PHI in the audit log); it records only the access event metadata.
      - Audit log entries must be **retained for a minimum of 6 years** per HIPAA retention requirements, and must be queryable by `user_id`, `organization_id`, and `accessed_at` range for compliance reporting.

- **EP-11 Success Metrics (Easy Prescription Experience)**
  - **Time to Prescribe**:
    - Median time to create and finalize a standard OPD prescription (from opening Easy Prescription screen with selected patient to final sign‑off) should be **≤ 2 minutes** under normal network and system load.
    - 90th percentile time for the same flow should be **≤ 3 minutes**.
  - **Interaction Efficiency**:
    - Standard OPD prescriptions (1–3 medicines, 1 diagnosis, basic advice) should typically require **≤ 15 user input actions** (clicks/taps/keystroke submissions) excluding free‑text typing.
    - Applying a commonly used doctor template should pre‑fill most fields so only **0–3 edits** are typically required before finalization.
  - **Performance**:
    - Easy Prescription screen initial load (with today’s patient list + recent prescriptions + templates) should complete in **≤ 2 seconds** on a standard hospital network for typical data volumes.
    - Adding or removing a medicine row, or applying a template, should visually update in **≤ 300 ms** on supported client devices.
  - **Adoption & Satisfaction**:
    - Track percentage of prescriptions created via Easy Prescription vs. full detailed flows, queryable by care setting (OPD vs IPD) using the `ep_encounter_mode` field (see EP-2).
    - Track **OPD Easy Prescription adoption rate** separately from **IPD adoption rate**, as OPD is the primary target for the ≤2-minute prescribing goal.
    - Track doctor satisfaction score for the Easy Prescription experience (via periodic survey or in‑app rating), with a target rating of **≥ 4.0 / 5.0**.
  - **Data Requirements for Metrics**:
    - All EP-11 adoption and timing metrics must be derivable from server-side data only (no dependence on client-side analytics or browser telemetry that may be blocked).
    - The `ep_encounter_mode` column on `ehr.prescriptions` is the canonical source for OPD/IPD segmentation. Reporting queries must filter on this column, not on encounter type joins, for performance.
    - Time-to-prescribe is measured as `sent_date - created_at` on the `ehr.prescriptions` record for finalised (SENT status) prescriptions. Only prescriptions with `ep_encounter_mode IS NOT NULL` are counted in EP-11 timing metrics (to exclude non-EP-flow prescriptions from the target).

#### 3.2.0.1 Easy Prescription (EP) → Core Functional Requirements (FR-P) Mapping

> **Purpose**: The EP module (EP-1 through EP-11) is a **UI/workflow layer** built on top of the core functional requirements (FR-P1.x through FR-P9.x). The table below resolves overlaps and defines which EP requirement augments, supersedes, or is simply the UX expression of an underlying FR-P requirement. Implementation teams must satisfy both the EP UX constraint **and** the referenced FR-P functional contract.

| EP Requirement | Relationship | FR-P Requirements Covered |
|---|---|---|
| **EP-1** Doctor Roles and Access | Augments | FR-P9.1 (RBAC), FR-P9.2 (Provider authentication); adds Nurse Assist Mode not in FR-P |
| **EP-2** Doctor Dashboard | New (no FR-P equivalent) | Composed from FR-P1.1 (patient context), FR-P1.9 (templates), FR-P6.x (prescription history) |
| **EP-3** One-Screen Quick Mode | UX constraint on | FR-P1.1, FR-P1.2, FR-P1.3, FR-P1.9; layout requirement only — all data rules from FR-P apply unchanged |
| **EP-4** Clinical Information | Augments | FR-P1.1 (patient context); adds ICD-10 multi-diagnosis and complaints — see **FR-P1.4a** (added below) |
| **EP-5** Medicine Prescription | Augments | FR-P1.2 (drug search), FR-P1.3 (dosage), FR-P1.9 (templates); adds dose shorthand and auto-quantity |
| **EP-6** Advice / Tests / Follow-Up | New (no FR-P equivalent) | Integrates with Lab/Radiology module requirements; referral is out of scope for prescription FR-P |
| **EP-7** Templates | Supersedes UX detail of | FR-P1.9 (templates/copy); EP-7 is the authoritative template spec for the doctor-facing UI |
| **EP-8** Save / Print / Share | Augments | FR-P1.11 (review/signature), FR-P3.1 (print output); auto-save concurrency rules — see **EP-8 Concurrency Model** below |
| **EP-9** Admin Configuration | New (no FR-P equivalent) | Configuration layer; medicine master is referenced by FR-P1.2 but not specified there |
| **EP-10** Technical & Compliance | Augments | FR-P9.x (security/audit), FR-P8.x (reporting); offline draft handling is new |
| **EP-11** Success Metrics | New (no FR-P equivalent) | Complements 3.1.9 success metrics with EP-specific time and interaction targets |

**Rule for conflicts**: Where an EP requirement and an FR-P requirement appear to conflict on the same behaviour (e.g., EP-5 allows quantity override while FR-P1.10 validates quantity), the **clinically safer** rule takes precedence. EP UX optimisations must not bypass FR-P safety checks.

#### 3.2.1 Prescription Creation

##### 3.2.1.1 Prescription Entry and Patient Selection

- **FR-P1.1**: System shall support prescription creation workflow with patient selection and verification:
  - **Patient Selection**:
    - Search and select patient from patient database
    - Verify patient identity (display patient name, DOB, MRN)
    - Display patient summary including:
      - Current medications
      - Known allergies (prominently displayed)
      - Active problems/diagnoses
      - Recent lab results (if relevant to prescribing)
      - Age and weight (for dosage calculations)
      - Insurance information (for formulary checking)
    - Confirm patient selection before proceeding
    - Prevent prescription creation if patient not selected
  
  - **Patient Context Display**:
    - Display patient's current medication list during prescription creation
    - Display patient's allergy list prominently
    - Display relevant clinical information (diagnoses, lab results)
    - Display insurance formulary information (if available)
    - Display patient's preferred pharmacy (if available)
    - Display recent prescription history

- **FR-P1.2**: System shall support medication selection from comprehensive drug database:
  - **Medication Search Methods**:
    - Search by generic name (primary method)
    - Search by brand name
    - Search by drug class/category
    - Search by NDC (National Drug Code)
    - Search by RxNorm code
    - Search by indication/therapeutic use
    - Search by partial name (fuzzy matching)
    - Browse medications by category/class
    - Browse medications by therapeutic class
  
  - **Search Features**:
    - Auto-complete suggestions as user types
    - Recent medications list (frequently prescribed by user)
    - Favorite medications list (user's preferred medications)
    - Common medications list (most commonly prescribed)
    - Search history (recently searched medications)
    - Search filters (by drug class, form, route, etc.)
    - Advanced search with multiple criteria
  
  - **Medication Selection**:
    - Display medication name (generic and brand)
    - Display medication strength options
    - Display available dosage forms
    - Display drug class information
    - Display NDC code
    - Display RxNorm code
    - Allow selection of specific medication and strength
    - Support for combination medications

##### 3.2.1.2 Prescription Details and Dosage Specification

- **FR-P1.3**: System shall support comprehensive prescription detail entry:
  - **Dosage Information** (Required):
    - **Dosage Strength** (required):
      - Select from available strengths for selected medication
      - Enter custom strength (if not in database, with validation)
      - Display strength in standard units (mg, mcg, units, etc.)
      - Validate strength is appropriate for medication
    - **Dosage Form** (required):
      - Select from available forms (Tablet, Capsule, Liquid, Injection, Topical, etc.)
      - Display form options for selected medication
      - Validate form is available for medication
    - **Quantity Per Dose** (required):
      - Specify number of units per dose (e.g., "1 tablet", "2 capsules", "5 ml")
      - Validate quantity is reasonable
    - **Total Quantity** (required):
      - Specify total quantity to dispense
      - Calculate based on duration and frequency (optional auto-calculation)
      - Validate quantity is reasonable
      - Support for standard quantities (30, 60, 90, etc.)
  
  - **Administration Instructions** (Required):
    - **Route of Administration** (required):
      - Select from standard routes (Oral, IV, IM, SubQ, Topical, Sublingual, Rectal, Vaginal, Ophthalmic, Otic, Nasal, Inhalation, Transdermal, Other)
      - Validate route is appropriate for medication and form
      - Display route options based on medication form
    - **Frequency/Schedule** (required):
      - Select from common frequencies (Once daily, Twice daily, Three times daily, Four times daily, Every 8 hours, Every 12 hours, As needed, etc.)
      - Enter custom frequency (with validation)
      - Specify timing if applicable (With meals, Before meals, At bedtime, In the morning, etc.)
      - Support for complex schedules (e.g., "Take 2 tablets in the morning and 1 tablet at bedtime")
      - Support for "As needed" (PRN) prescriptions with indication
    - **Duration of Treatment** (optional but recommended):
      - Specify duration (e.g., "10 days", "2 weeks", "1 month", "3 months", "Ongoing")
      - Enter specific end date (optional)
      - Auto-calculate end date based on start date and duration
      - Support for indefinite duration (ongoing medications)
  
  - **Special Instructions** (Optional):
    - Free-text field for patient instructions
    - Common instruction templates (e.g., "Take with food", "Take on empty stomach", "Do not crush")
    - Support for detailed administration instructions
    - Support for warnings and precautions
    - Character limit with validation (typically 500-1000 characters)

- **FR-P1.4**: System shall support prescription timing and scheduling:
  - **Start Date** (Required):
    - Default to current date
    - Allow future-dating (with validation - typically up to 30 days)
    - Cannot be in the past (except for specific scenarios with authorization)
    - Display start date prominently
  - **End Date** (Optional):
    - Auto-calculated from start date and duration (if duration specified)
    - Can be manually entered
    - Must be after start date
  - **Timing Instructions** (Optional):
    - Specific timing relative to meals
    - Specific time of day
    - Frequency relative to other medications
    - Support for medication schedules

##### 3.2.1.2a Diagnosis Linkage (Multi-Diagnosis)

- **FR-P1.4a**: System shall support linking one or more ICD-10 diagnoses to a prescription, as a first-class data element:

  - **Data Model Requirements**:
    - A prescription shall support **1 to N diagnosis entries** (not a single free-text field), stored in a dedicated `prescription_diagnoses` table with the following attributes per entry:
      - `diagnosis_code` — ICD-10-CM code (required); format validated as alphanumeric per ICD-10-CM conventions.
      - `diagnosis_description` — Human-readable description (auto-populated from ICD-10 database; editable).
      - `diagnosis_type` — `PRIMARY` | `SECONDARY` | `COMORBIDITY` (required per entry; exactly one entry must be `PRIMARY`).
      - `diagnosis_sequence` — Display order (integer, starting at 1 for the primary diagnosis).
    - The header-level `diagnosis_code` field on `ehr.prescriptions` shall be treated as a **derived, read-only summary** of the primary diagnosis code only, maintained for backward compatibility and reporting queries.

  - **Entry Rules**:
    - At least one diagnosis is **required** before a prescription can be transmitted (not required for DRAFT status).
    - Exactly one diagnosis must be marked `PRIMARY`; all others are `SECONDARY` or `COMORBIDITY`.
    - Maximum 12 diagnoses per prescription (consistent with CMS claim limits).
    - Duplicate ICD-10 codes on the same prescription are not permitted (enforced).
    - **Implicit primary (Req-J1)**: When the client supplies a non-empty `diagnoses` array and **no** entry has `isPrimary: true`, the **first** diagnosis in **sequence order** is treated as the primary: entries are ordered by optional per-row `sequenceOrder` (ascending; omitted values sort after explicit numbers), then by request array order when every `sequenceOrder` is omitted. That entry is persisted with `isPrimary=true` and drives the derived header `diagnosis_code`.

  - **API enforcement — diagnosis business rules (Req-J1 / FR-P1.4a)**:
    - These rules apply to `POST`/`PUT` prescription payloads that include a `diagnoses` list. Responses use **HTTP 422** with JSON body `error` (human-readable message), `status: 422`, and `code` (machine-readable), except where noted.
    - **More than 12 diagnoses** in one request → **HTTP 422**, `code`: **`DIAGNOSIS_LIMIT_EXCEEDED`**
    - **Duplicate `diagnosisCode`** (case-insensitive match after trim) within the same request → **HTTP 422**, `code`: **`DUPLICATE_DIAGNOSIS_CODE`**
    - **More than one** entry with `isPrimary: true` → **HTTP 422**, `code`: **`MULTIPLE_PRIMARY_DIAGNOSES`**
    - **Zero** entries with `isPrimary: true` when `diagnoses` is provided → **not an error**; implicit primary behaviour above applies.

  - **UI Requirements** (applies to both Easy Prescription EP-4 and full prescription entry):
    - ICD-10 search by code prefix or description keyword with auto-complete (minimum 2 characters to trigger).
    - Doctor-specific ICD-10 favorites list (per EP-9 admin configuration) surfaced first in search results.
    - Ability to designate any entry as `PRIMARY` with clear visual indicator.
    - Display all linked diagnoses in prescription summary, review, and printed/PDF output.
    - Diagnosis entries are carried through to NCPDP SCRIPT `Diagnosis` segment(s) in the transmission message.

  - **Validation**:
    - ICD-10 code must be a valid, active code in the current ICD-10-CM release year (warn if code is valid but flagged as deprecated).
    - Certain drug classes have **mandatory diagnosis linkage** configured by Admin (e.g., oncology agents, high-cost biologics); block transmission if not linked.

##### 3.2.1.3 Refills and Substitution

- **FR-P1.5**: System shall support refill authorization:
  - **Refill Authorization** (Required):
    - Number of refills authorized (0 to maximum allowed, typically 0-11)
    - Default based on medication type and regulations
    - Zero refills for controlled substances (Schedule II, unless state allows)
    - Maximum refills enforced based on medication type and regulations
    - Display remaining refills after each fill
    - Support for "No refills" option
  
  - **Refill Restrictions**:
    - Controlled substance refill restrictions (Schedule II typically no refills)
    - State-specific refill regulations
    - Medication-specific refill limits
    - Insurance-specific refill limits
    - Time limits for refills (typically 1 year from prescription date)
  
  - **Refill Management**:
    - Track refill count
    - Display refill history
    - Support for refill modifications
    - Support for refill cancellations

- **FR-P1.6**: System shall support generic substitution:
  - **Substitution Preference** (Required):
    - "Substitution Allowed" (Dispense as Written - DAW code 0 or blank)
    - "Do Not Substitute" / "Dispense as Written" (DAW code 1)
    - "Substitute with Generic" (DAW code 2, if applicable)
    - "Brand Medically Necessary" (DAW code 3, if applicable)
    - Default based on medication and insurance formulary
  
  - **Substitution Information**:
    - Display generic availability
    - Display cost difference (if available)
    - Display insurance preference (if available)
    - Alert if "Do Not Substitute" may affect insurance coverage

##### 3.2.1.4 Drug Information and Clinical Support

- **FR-P1.7**: System shall provide comprehensive drug information during prescription creation:
  - **Drug Information Display**:
    - **Drug Interactions**:
      - Display potential drug-drug interactions with current medications
      - Display drug-allergy interactions
      - Display drug-disease contraindications
      - Display severity levels (Critical, Major, Moderate, Minor)
      - Display interaction descriptions
      - Display management recommendations
    - **Contraindications**:
      - Display absolute contraindications
      - Display relative contraindications
      - Display disease-specific contraindications
      - Display patient-specific contraindications (based on patient data)
    - **Side Effects**:
      - Display common side effects
      - Display serious side effects
      - Display frequency of side effects
      - Display warnings and precautions
    - **Dosage Guidelines**:
      - Display standard dosing for indication
      - Display age-appropriate dosing
      - Display weight-based dosing (if applicable)
      - Display renal function adjustments (if applicable)
      - Display hepatic function adjustments (if applicable)
      - Display maximum daily dose
      - Display minimum effective dose
    - **Pregnancy and Lactation**:
      - Display pregnancy category or risk (if applicable)
      - Display lactation safety information
      - Display warnings for women of childbearing age
    - **Administration Instructions**:
      - Display standard administration instructions
      - Display special handling requirements
      - Display storage requirements
    - **Monitoring Requirements**:
      - Display recommended lab monitoring
      - Display recommended clinical monitoring
      - Display monitoring frequency
    - **Drug Class Information**:
      - Display therapeutic class
      - Display mechanism of action
      - Display related medications in same class
  - **Clinical Safety Implementation Phasing**:
    > **Critical note for implementation and QA teams**: The checks described in FR-P1.7 will be satisfied in two phases. Deploying Phase 1 into production does **not** mean FR-P1.7 is fully satisfied for clinical go-live. Phase 2 must be completed and validated before the system is used in a setting where an undetected drug interaction could cause patient harm.
    - **Phase 1 — Local Heuristic Screening (implemented)**:
      - Coverage: Major named drug-drug interactions catalogued in `ClinicalMedicationSafetyService`, major drug classes with class-level rules (e.g., all SSRIs, all NSAIDs), age/weight thresholds, renal/hepatic risk flags, known pregnancy contraindications.
      - Accuracy: Heuristics catch a representative subset of high-severity interactions. They are **not exhaustive** and will miss novel, less-common, or drug-class combinations not explicitly coded.
      - Appropriate for: Development, UAT, pilot with clinical oversight. **Not appropriate as the sole safety layer for unsupervised clinical use.**
    - **Phase 2 — Licensed Drug Database Integration (required before unsupervised clinical go-live)**:
      - Integrate a licensed clinical decision support (CDS) data source (e.g., DrugBank Enterprise, Micromedex, FDB, or equivalent) via API.
      - The licensed database becomes the **primary** source for all drug-drug, drug-food, drug-lab, drug-disease checks listed in FR-P1.7.
      - Local heuristics from Phase 1 may remain as a **fast-path cache** or offline fallback, but must not be the sole decision source once Phase 2 is active.
      - Phase 2 requires: signed data licence agreement, SLA for database update frequency (at minimum monthly, ideally weekly), integration test suite proving coverage of all FR-P1.7 interaction categories, and sign-off from a qualified clinical pharmacist.
    - **Transparency in the UI**: Regardless of phase, the UI must display a visible indicator of which data source is backing the interaction check (e.g., “Powered by DrugBank” badge in Phase 2, or “Local reference data — consult clinical pharmacist for complex interactions” notice in Phase 1).

- **FR-P1.8**: System shall support formulary and insurance information:
  - **Formulary Checking**:
    - Check medication against patient's insurance formulary
    - Display formulary status (Covered, Not Covered, Prior Authorization Required, Step Therapy Required)
    - Display tier information (if applicable)
    - Display copay information (if available)
    - Display alternative medications if not covered
    - Display generic alternatives if available
  - **Insurance Information**:
    - Display patient's primary insurance
    - Display insurance-specific requirements
    - Display prior authorization requirements
    - Display quantity limits
    - Display day supply limits

##### 3.2.1.5 Prescription Templates and Efficiency Features

- **FR-P1.9**: System shall support prescription templates and efficiency features:
  - **Prescription Templates**:
    - Pre-built templates for common medications
    - User-created templates for frequently prescribed medications
    - Specialty-specific templates
    - Condition-specific templates
    - Template library with search
    - Template customization
  
  - **Copy and Modify**:
    - Copy previous prescriptions for same patient
    - Copy previous prescriptions for different patient (with modification)
    - Modify existing prescriptions (before sending)
    - Copy medication from patient's current medication list
    - Copy medication from patient's medication history
  
  - **Batch Prescribing**:
    - Create multiple prescriptions at once
    - Prescribe medication for multiple patients (if authorized)
    - Prescribe multiple medications for same patient
    - Batch review and signing
  
  - **Quick Prescribe**:
    - Quick access to frequently prescribed medications
    - One-click prescription for common medications (with review)
    - Shortcuts for common prescriptions
    - Favorites list for quick access

##### 3.2.1.6 Prescription Validation and Quality Checks

- **FR-P1.10**: System shall perform comprehensive prescription validation with the following specific validation rules:
  - **Required Field Validation**:
    - Patient must be selected (cannot be null or empty)
    - Medication must be selected (cannot be null or empty)
    - Dosage strength must be specified (required, numeric)
    - Dosage form must be specified (required, from valid list)
    - Quantity must be specified (required, numeric, must be > 0)
    - Route must be specified (required, from valid list)
    - Frequency must be specified (required, from valid list or custom with validation)
    - Start date must be specified (required, valid date)
    - Refills must be specified (required, numeric, 0 or positive integer)
    - Substitution preference must be specified (required, Yes/No or Dispense as Written/Substitution Allowed)
    - Provider must be authenticated (required, valid user session)
    - System shall prevent prescription creation if any required field is missing
    - System shall display list of missing required fields
    - System shall highlight missing required fields visually
  
  - **Dosage Strength Validation**:
    - Must be valid numeric value (positive number, can be decimal)
    - Must be greater than 0
    - Must be within medication-specific range (e.g., cannot exceed manufacturer maximum)
    - Must match available strengths for selected medication (warn if custom strength)
    - Format validation: Numeric with up to 3 decimal places (e.g., 10, 10.5, 0.25)
    - Unit validation: Must be valid unit (mg, mcg, g, ml, units, etc.)
    - Unit-dosage form consistency: Unit must match dosage form (e.g., tablets use mg, not ml)
    - Cannot be all zeros
    - Cannot be unreasonably large (warn if > 10,000 mg or medication-specific limit)
    - Age-appropriate validation: Dosage must be within age-appropriate range (see FR-P2.7)
    - Weight-appropriate validation: Dosage must be within weight-appropriate range if weight-based dosing (see FR-P2.7)
  
  - **Quantity Validation**:
    - Must be valid numeric value (positive integer)
    - Must be greater than 0
    - Must be reasonable for medication type:
      - Tablets/Capsules: Typically 1-9999 units (warn if > 1000, require confirmation if > 5000)
      - Liquid: Typically 1-5000 ml (warn if > 2000 ml, require confirmation if > 5000 ml)
      - Injections: Typically 1-100 units (warn if > 50, require confirmation if > 100)
      - Topical: Typically 1-500 units (warn if > 200, require confirmation if > 500)
    - Cannot exceed maximum quantity limits (configurable by organization)
    - Must be reasonable for duration and frequency (warn if quantity doesn't match duration/frequency)
    - Auto-calculation validation: If auto-calculated, must match manual entry (warn if different)
    - Standard quantity validation: Warn if quantity is not a standard quantity (30, 60, 90, etc.) for maintenance medications
  
  - **Frequency/Schedule Validation**:
    - Must be from valid frequency list or custom with validation
    - Common frequencies: Once daily, Twice daily, Three times daily, Four times daily, Every 8 hours, Every 12 hours, Every 6 hours, Every 4 hours, As needed (PRN), Weekly, Monthly, etc.
    - Custom frequency validation:
      - Must be in readable format
      - Must specify number of times per day/week/month
      - Cannot exceed maximum daily frequency for medication (e.g., cannot be "Every hour" for most medications)
      - Must be reasonable (warn if > 6 times per day, require confirmation if > 8 times per day)
    - PRN (As needed) validation:
      - Must include indication/reason if PRN selected
      - Must specify maximum frequency (e.g., "As needed, up to 4 times per day")
    - Frequency-medication consistency: Frequency must be appropriate for medication type
    - Frequency-dosage consistency: Frequency must be appropriate for dosage strength
  
  - **Date Validation**:
    > **Note**: Basic calendar validity (invalid calendar days, date format) is enforced by the UI date picker and ISO-8601 serialisation. The rules below focus on **clinically meaningful** date constraints that the date picker cannot enforce.

    - **Start Date Validation** (clinically meaningful rules):
      - Default to current date if not specified.
      - Warn if start date is more than **30 days in the future** (post-dated prescription); require explicit confirmation if > 90 days — most jurisdictions limit post-dating.
      - Warn if start date is in the past by more than **24 hours** (back-dated prescription); require override reason and audit entry — only permitted with explicit prescriber justification.
      - For controlled substances: start date back-dating is not permitted (enforced, not just warned).
    
    - **End Date Validation** (if specified):
      - Must be after start date (enforced).
      - Warn if end date implies a supply > 90 days for controlled substances Schedule III–V, or > 30 days for Schedule II.
      - Must be consistent with duration if both are specified; if they differ by > 1 day, surface a reconciliation prompt (do not silently choose one over the other).
    
    - **Clinically Meaningful Duration Checks** (if specified):
      - Duration must be > 0 days (enforced).
      - **Antibiotics** (system-tagged drug class): Warn if duration < 3 days or > 21 days; require confirmation if > 14 days.
      - **Controlled substances Schedule II**: Duration must not exceed 30 days (enforced).
      - **Controlled substances Schedule III–V**: Duration must not exceed 90 days (enforced).
      - **Maintenance/chronic medications**: No maximum duration enforced; ongoing (no end date) is valid.
      - If duration conflicts with the refill count (e.g., 5 refills × 30 days = 150-day total supply but duration is 30 days), surface a consistency warning.
  
  - **Refill Validation**:
    - Must be valid integer (0 or positive)
    - Cannot be negative
    - Must be within allowed range:
      - Non-controlled substances: Typically 0-11 refills (warn if > 5, require confirmation if > 11)
      - Schedule III-V controlled substances: Maximum 5 refills (enforced)
      - Schedule II controlled substances: No refills allowed (enforced)
    - Must be appropriate for medication type:
      - Acute medications (antibiotics): Typically 0 refills
      - Maintenance medications: May allow refills
      - Controlled substances: Limited by schedule
    - Refill-duration consistency: Number of refills must be reasonable for total duration
    - Refill-quantity consistency: Total quantity (initial + refills) must be reasonable
  
  - **Route of Administration Validation**:
    - Must be from valid route list (Oral, IV, IM, SubQ, Topical, Sublingual, Rectal, Vaginal, Ophthalmic, Otic, Nasal, Inhalation, Transdermal, Other)
    - Route-dosage form consistency: Route must be appropriate for dosage form:
      - Tablets/Capsules: Oral only
      - Liquid: Oral, Topical, Ophthalmic, Otic, Nasal (depending on type)
      - Injection: IV, IM, SubQ only
      - Topical: Topical, Transdermal only
      - Inhalation: Inhalation only
    - Route-medication consistency: Route must be appropriate for medication (e.g., cannot use Oral route for IV-only medications)
    - Custom route validation: If "Other" selected, must specify custom route
  
  - **Medication Code Validation** (if provided):
    - **NDC Code Validation**:
      - Format validation: Must be 10 or 11 digits
      - Format options: XXXXX-XXXX-XX (11 digits with hyphens) or XXXXXXXXXX (10 digits)
      - Code existence validation: Code must exist in NDC database (if available)
      - Code-medication consistency: NDC code must match selected medication
      - Code format validation: Must follow NDC format standards
      - Cannot be all zeros
      - Cannot be test/invalid codes (e.g., 00000-0000-00)
    
    - **RxNorm Code Validation**:
      - Format validation: Must be valid RxNorm code format (numeric, typically 6-9 digits)
      - Code existence validation: Code must exist in RxNorm database (if available)
      - Code version validation: Code must be from current RxNorm version
      - Code-medication consistency: RxNorm code must match selected medication
      - Code format validation: Must follow RxNorm format standards
  
  - **Age-Based Validation**:
    - Pediatric validation (age < 18):
      - Dosage must be within pediatric dosing guidelines
      - Warn if adult-only medication prescribed to pediatric patient
      - Require weight-based dosing for certain medications in pediatric patients
      - Age-specific contraindications checked
      - Age-specific maximum doses enforced
    
    - Adult validation (age 18-65):
      - Standard adult dosing guidelines applied
      - Age-specific considerations for young adults
    
    - Geriatric validation (age > 65):
      - Dosage may need adjustment for geriatric patients
      - Warn if medication has geriatric-specific dosing requirements
      - Check for geriatric contraindications
      - Consider reduced dosing for certain medications
  
  - **Weight-Based Validation** (if applicable):
    - Patient weight must be available for weight-based dosing
    - Dosage per kg/lb calculation validation
    - Maximum dose per weight validation
    - Warn if weight-based dose exceeds standard maximum dose
    - Weight must be recent (warn if weight > 1 year old for pediatric patients, > 2 years for adults)
    - Weight must be reasonable (warn if weight seems incorrect)
  
  - **Renal Function Validation** (if applicable):
    - Creatinine clearance or eGFR must be available for medications requiring renal adjustment
    - Dosage adjustment recommendations based on renal function
    - Warn if medication contraindicated in renal impairment
    - Warn if medication requires renal function monitoring
    - Renal function must be recent (warn if > 1 year old)
  
  - **Hepatic Function Validation** (if applicable):
    - Liver function tests must be available for medications requiring hepatic adjustment
    - Dosage adjustment recommendations based on hepatic function
    - Warn if medication contraindicated in hepatic impairment
    - Warn if medication requires hepatic function monitoring
    - Hepatic function tests must be recent (warn if > 1 year old)
  
  - **Maximum Daily Dose Validation**:
    - Calculate total daily dose based on dosage strength, quantity per dose, and frequency
    - Compare against medication-specific maximum daily dose
    - Warn if total daily dose exceeds recommended maximum
    - Prevent prescription if total daily dose exceeds absolute maximum (safety limit)
    - Consider patient-specific factors (age, weight, renal/hepatic function) in maximum dose calculation
  
  - **Duration Validation**:
    - Duration must be reasonable for medication type:
      - Antibiotics: Typically 3-14 days (warn if < 3 days or > 21 days)
      - Maintenance medications: Typically ongoing or 30-90 days
      - Acute medications: Typically 3-30 days
    - Duration must match indication (e.g., UTI treatment typically 3-7 days)
    - Warn if duration seems too short or too long for medication type
  
  - **Regulatory Validation**:
    - **DEA Number Validation** (for controlled substances):
      - DEA number required for all controlled substance prescriptions.
      - **Format**: 2 letters followed by exactly 7 digits — regex `^[A-Za-z]{2}[0-9]{7}$` (e.g., `AB1234567`).
        - First letter encodes registrant type: `A`/`B`/`F` = practitioner, `M` = mid-level practitioner, `P`/`R` = manufacturer/distributor, etc. Validate against permitted registrant-type codes for the provider's registration category.
        - Second letter must match the first letter of the registrant's last name (case-insensitive).
      - **Check-digit algorithm** (regulatory requirement — DEA Regulations § 1301.13; must be implemented exactly as specified):
        1. Let D1–D7 be the 7 numeric digits of the DEA number.
        2. Compute **S1** = D1 + D3 + D5 (sum of digits at odd positions 1, 3, 5).
        3. Compute **S2** = D2 + D4 + D6 (sum of digits at even positions 2, 4, 6).
        4. Compute **T** = S1 + 2 × S2.
        5. The **check digit** is `T mod 10`. It must equal D7 (the seventh digit).
        - Example: DEA `AB1234567` → D1–D7 = 1,2,3,4,5,6,7; S1 = 1+3+5 = 9; S2 = 2+4+6 = 12; T = 9 + 24 = 33; check = 3 mod 10 = 3 ≠ 7 → **invalid**. A valid DEA ending in 7 would require T ≡ 7 (mod 10).
        - System must reject prescriptions for controlled substances with a DEA number that fails the check-digit test. Rejection is enforced (not a warning).
      - DEA number must match the prescribing provider's registered DEA number on file.
      - DEA number must be active and not expired (verify against provider record; optionally query DEA validation service if integrated).
      - Prevent prescription creation and transmission if DEA number is missing, malformed, or fails the check-digit test.
    
    - **NPI Validation** (for all prescriptions requiring a National Provider Identifier):
      - NPI required for all e-prescribing transmissions and any prescription where prescriber NPI is on file.
      - **Format**: Exactly 10 digits, all numeric — regex `^[0-9]{10}$`. Leading zeros are significant; the NPI is not a numeric quantity.
      - **Check-digit algorithm** (ISO/IEC 7812 Luhn applied to NPI — CMS NPI Final Rule; must be implemented exactly as specified):
        1. Prepend the constant prefix `80840` (5 digits) to the 10-digit NPI to form a 15-digit working string. E.g., NPI `1234567893` → working string `808401234567893`.
        2. Starting from the **rightmost digit** (position 1) and moving left, **double every digit at an even position** (positions 2, 4, 6, …).
        3. If any doubled value exceeds 9, subtract 9 from it.
        4. Sum all 15 digits (original digits at odd positions + adjusted doubles at even positions).
        5. The check passes if and only if `sum mod 10 = 0`.
        - Example: NPI `1234567893` → working string `808401234567893`  
          Digits left-to-right: 8 0 8 4 0 1 2 3 4 5 6 7 8 9 3  
          Positions right-to-left: pos1=3, pos2=9, pos3=8, pos4=7, pos5=6, pos6=5, pos7=4, pos8=3, pos9=2, pos10=1, pos11=0, pos12=4, pos13=8, pos14=0, pos15=8  
          Even positions doubled (pos2,4,6,8,10,12,14) = 9,7,5,3,1,4,0 → ×2 = 18,14,10,6,2,8,0 → subtract 9 where >9 = 9,5,1,6,2,8,0  
          Sum of all positions: 3+9+8+5+6+1+4+2+2+8+0+8+0+0+8 = **64** — this NPI is **invalid** (64 mod 10 ≠ 0). A valid NPI such as `1234567890` (where the last digit is chosen to make the sum divisible by 10) would pass. Implementers should verify their implementation against the CMS NPI check-digit specification and the NPPES public API, which validates any 10-digit NPI.
        - System must reject any NPI that fails the Luhn check. Rejection is enforced (not a warning) for transmission; display a warning during prescription entry to allow data correction before submission.
      - NPI must match the prescribing provider's NPI on file in the provider registry.
      - NPI must not be all zeros (`0000000000`).

    - **State-Specific Requirements**:
      - Validate against state-specific prescription regulations
      - Validate quantity limits by state
      - Validate duration limits by state
      - Validate refill restrictions by state
      - Validate controlled substance requirements by state
    
    - **Controlled Substance Regulations**:
      - Schedule II: No refills, 30-day supply maximum (enforced)
      - Schedule III-V: Maximum 5 refills, 90-day supply maximum (enforced)
      - **Schedule II refill prohibition — API (Req-J2 / FR-P1.10)**: If any medication on the prescription carries DEA **Schedule II** (stored in the API as `schedule = II` on the prescription or on a medication line — i.e. C-II), the refill-request endpoints must reject the request **before** persisting a refill-request row: **HTTP 422** with JSON body `{ "error": "Schedule II controlled substances may not be refilled. A new prescription is required.", "status": 422 }`. Canonical path: `POST /api/prescriptions/{id}/refill-requests` (equivalent behaviour exists on `POST /api/prescription-refills/requests` with `prescriptionId` in the body). The Schedule II check runs first in the service method, prior to refills-remaining or other validation that writes data.
      - Quantity limits enforced by schedule
      - Duration limits enforced by schedule
      - PDMP query required (if applicable, see FR-P7.4)
    
    - **Prescription Limits**:
      - Maximum number of prescriptions per patient per day (if configured)
      - Maximum total quantity per prescription (if configured)
      - Maximum number of controlled substance prescriptions per provider per day (if configured)
  
  - **Error Handling for Validation**:
    - Display clear, specific error messages for each validation failure
    - Error messages shall indicate which field failed and why
    - Error messages shall suggest corrections when possible (e.g., "Dosage strength must be between 5-20 mg for this medication. Current value: 25 mg")
    - System shall highlight invalid fields visually (red border, error icon, asterisk)
    - System shall prevent prescription submission until all validation errors are resolved
    - System shall maintain entered data when validation fails (don't clear form)
    - System shall group related errors together for better user experience
    - System shall display error count (e.g., "5 validation errors found")
    - System shall allow user to navigate to next error field
    - System shall provide inline validation feedback (real-time or on blur)

- **FR-P1.11**: System shall support prescription review and confirmation:
  - **Prescription Summary**:
    - Display complete prescription summary before sending
    - Display all prescription details
    - Display drug information and warnings
    - Display interaction alerts
    - Display formulary information
    - Display cost information (if available)
  
  - **Review Process**:
    - Require provider review of prescription summary
    - Require acknowledgment of warnings (if any)
    - Require confirmation before sending
    - Allow modification during review
    - Allow cancellation during review
  
  - **Electronic Signature**:
    - Require electronic signature before sending
    - Signature authentication
    - Signature timestamp
    - Signature cannot be backdated
    - Signature audit trail

##### 3.2.1.7 Error Handling and Recovery

- **FR-P1.12**: System shall implement comprehensive error handling for prescription creation:
  - **Validation Error Handling**:
    - Display clear, specific error messages for each validation failure (see FR-P1.10)
    - Error messages shall indicate which field failed and why
    - Error messages shall suggest corrections when possible
    - System shall highlight invalid fields visually (red border, error icon)
    - System shall prevent prescription submission until all validation errors are resolved
    - System shall maintain entered data when validation fails (don't clear form)
    - System shall group related errors together for better user experience
    - System shall display error count (e.g., "5 validation errors found")
    - System shall allow user to navigate to next error field
    - System shall provide inline validation feedback (real-time or on blur)
  
  - **System Error Handling**:
    - **Network Errors**:
      - Handle network connectivity failures during prescription creation
      - Display user-friendly error message: "Unable to connect to server. Please check your internet connection."
      - Provide retry mechanism for failed network operations
      - Auto-save prescription data locally to prevent data loss
      - Queue prescription for submission when connection restored
    
    - **Database Errors**:
      - Handle database connection failures
      - Handle database timeout errors
      - Handle database constraint violations
      - Display user-friendly error messages
      - Log database errors with context (patient ID, prescription data, user)
      - Provide retry mechanism for transient database errors
      - Auto-save prescription data to prevent loss
    
    - **Server Errors**:
      - Handle server errors (500, 503, etc.)
      - Display user-friendly error messages
      - Provide retry mechanism
      - Log server errors with full context
      - Auto-save prescription data
    
    - **Application Errors**:
      - Handle application crashes gracefully
      - Auto-save prescription data periodically
      - Restore prescription data after crash recovery
      - Display recovery message to user
      - Log application errors with stack traces
  
  - **Integration Error Handling**:
    - **Drug Database Integration Errors**:
      - Handle medication lookup failures
      - Handle drug database connection failures
      - Allow manual medication entry when database unavailable
      - Queue medication lookups for retry
      - Display error message: "Drug database temporarily unavailable. You may continue with manual entry."
      - Cache medication data for offline use
    
    - **Formulary Integration Errors**:
      - Handle formulary check failures
      - Handle formulary service unavailability
      - Allow prescription creation without formulary check (with warning)
      - Queue formulary checks for retry
      - Display warning: "Formulary information unavailable. Please verify coverage manually."
    
    - **Patient Record Integration Errors**:
      - Handle patient data retrieval failures
      - Handle allergy list retrieval failures
      - Handle medication list retrieval failures
      - Allow prescription creation with cached patient data
      - Queue patient data refreshes for retry
      - Display warning: "Some patient information may be outdated. Please verify manually."
    
    - **Pharmacy Integration Errors**:
      - Handle pharmacy database lookup failures
      - Handle pharmacy selection errors
      - Allow manual pharmacy entry when database unavailable
      - Queue pharmacy lookups for retry
  
  - **Data Consistency Error Handling**:
    - **Concurrent Edit Conflicts**:
      - Detect concurrent prescription edits
      - Handle prescription modification conflicts
      - Provide conflict resolution interface
      - Display conflicting changes side-by-side
      - Allow user to choose which version to keep
      - Maintain audit trail of conflicts
    
    - **Duplicate Prescription Detection**:
      - Detect duplicate prescriptions (same medication, same patient, same date)
      - Alert user if duplicate detected
      - Provide option to cancel duplicate or proceed
      - Log duplicate detection attempts
    
    - **Data Synchronization Errors**:
      - Handle medication list synchronization failures
      - Handle allergy list synchronization failures
      - Handle patient data synchronization failures
      - Queue synchronization for retry
      - Display warning if data may be out of sync
  
  - **Template and Auto-Save Error Handling**:
    - **Template Loading Errors**:
      - Handle prescription template loading failures
      - Handle template corruption errors
      - Display error: "Template could not be loaded. Please create prescription manually."
      - Allow template recovery or recreation
    
    - **Auto-Save Errors**:
      - Handle auto-save failures gracefully
      - Retry auto-save with exponential backoff
      - Display warning if auto-save fails: "Unable to auto-save. Please save manually."
      - Allow manual save when auto-save fails
      - Log auto-save failures
  
  - **Calculation Error Handling**:
    - **Dosage Calculation Errors**:
      - Handle dosage calculation failures (e.g., weight-based dosing)
      - Handle division by zero errors
      - Handle invalid calculation inputs
      - Display error: "Unable to calculate dosage. Please enter manually."
      - Validate calculation results (warn if unreasonable)
    
    - **Quantity Calculation Errors**:
      - Handle quantity calculation failures
      - Handle duration-based quantity calculation errors
      - Display error: "Unable to calculate quantity. Please enter manually."
      - Validate calculated quantities
  
  - **Security and Authorization Error Handling**:
    - **Authentication Errors**:
      - Handle session expiration during prescription creation
      - Handle authentication failures
      - Auto-save prescription data before session expiration
      - Prompt user to re-authenticate
      - Restore prescription data after re-authentication
    
    - **Authorization Errors**:
      - Handle insufficient permissions for prescription creation
      - Handle DEA number authorization failures
      - Display error: "You do not have permission to create this prescription."
      - Log authorization failures
      - Prevent prescription creation if unauthorized
  
  - **Recovery Mechanisms**:
    - **Auto-Save and Recovery**:
      - Auto-save prescription data every 30 seconds (configurable)
      - Auto-save on field blur
      - Auto-save before navigation away
      - Restore prescription data after error recovery
      - Display recovery notification: "Prescription data has been restored."
      - Allow user to discard recovered data if desired
    
    - **Error Recovery Workflow**:
      - Provide "Retry" button for transient errors
      - Provide "Save Draft" option for all errors
      - Provide "Cancel" option to discard changes
      - Maintain prescription state during error recovery
      - Log all recovery attempts
  
  - **Error Logging and Reporting**:
    - Log all errors with full context:
      - Error type and message
      - Patient ID (if available)
      - Prescription data (if available)
      - User ID and role
      - Timestamp
      - Stack trace (for application errors)
      - System state at time of error
    - Generate error reports for system administrators
    - Alert administrators for critical errors
    - Track error trends and patterns

#### 3.2.2 Drug Interaction and Allergy Checking

##### 3.2.2.1 Drug-Drug Interaction Checking

- **FR-P2.1**: System shall automatically check for drug-drug interactions when creating prescriptions:
  - **Interaction Detection**:
    - Check new medication against all current medications in patient's medication list
    - Check new medication against all medications in prescription being created (if multiple medications)
    - Check for interactions between medications in same prescription
    - Check for interactions with medications recently discontinued (within configurable time period, e.g., 30 days)
    - Check for interactions with medications on hold
    - Real-time checking during medication selection
    - Re-check interactions when dosage or frequency is modified
  
  - **Interaction Types Detected**:
    - **Pharmacokinetic Interactions**:
      - Drug metabolism interactions (CYP450 enzyme interactions)
      - Drug absorption interactions
      - Drug distribution interactions
      - Drug elimination interactions
    - **Pharmacodynamic Interactions**:
      - Additive effects (increased therapeutic or toxic effects)
      - Antagonistic effects (decreased therapeutic effects)
      - Synergistic effects (potentiated effects)
    - **Therapeutic Duplications**:
      - Same medication prescribed multiple times
      - Medications with same active ingredient
      - Medications in same therapeutic class with similar mechanisms
    - **Contraindicated Combinations**:
      - Medications that should never be used together
      - Medications with known serious adverse reactions when combined
  
  - **Interaction Severity Levels**:
    - **Critical/Severe** (Must not prescribe together):
      - Life-threatening interactions
      - Severe adverse reactions
      - Absolute contraindications
      - Requires immediate action
    - **Major** (Requires caution, monitoring, or dose adjustment):
      - Significant clinical significance
      - May require dose adjustment
      - Requires close monitoring
      - May require alternative medication
    - **Moderate** (Monitor closely):
      - Moderate clinical significance
      - May require monitoring
      - May require dose adjustment
      - Consider alternative if possible
    - **Minor** (Informational):
      - Minimal clinical significance
      - Generally safe to use together
      - May have minor effects
      - Informational only
  
  - **Interaction Information Displayed**:
    - Interacting medications (names and dosages)
    - Interaction type and mechanism
    - Severity level (prominently displayed)
    - Clinical significance
    - Potential adverse effects
    - Management recommendations:
      - Dose adjustment suggestions
      - Monitoring recommendations
      - Alternative medication suggestions
      - Timing adjustments (if applicable)
    - Evidence level (if available)
    - References (if available)

- **FR-P2.2**: System shall support interaction checking configuration:
  - **Configurable Settings**:
    - Enable/disable interaction checking
    - Minimum severity level to display (e.g., show only Major and Critical)
    - Time period for checking recently discontinued medications
    - Interaction database selection
    - Custom interaction rules (organization-specific)
  
  - **Interaction Database**:
    - Integration with drug interaction databases (e.g., First Databank, Micromedex, Clinical Pharmacology)
    - Regular database updates
    - Evidence-based interaction information
    - Clinical significance assessment
    - Management recommendations

##### 3.2.2.2 Drug-Allergy Interaction Checking

- **FR-P2.3**: System shall automatically check for drug-allergy interactions:
  - **Allergy Detection**:
    - Check new medication against all allergies in patient's allergy list
    - Check for exact allergen matches
    - Check for drug class matches (e.g., if allergic to Penicillin, check all Penicillins)
    - Check for cross-reactivity (e.g., Penicillin and Cephalosporin)
    - Check for ingredient matches (e.g., if allergic to dye, check medications with that dye)
    - Check for similar chemical structures
    - Check for related substances
    - Real-time checking during medication selection
  
  - **Allergy Information Used**:
    - Allergen name
    - Allergen type (Medication, Food, Environmental, etc.)
    - Allergen category/class (for medications)
    - Reaction type and severity
    - Verification status
    - Cross-reactivity information
  
  - **Allergy Alert Severity**:
    - **Critical** (Must not prescribe):
      - Confirmed allergy to exact medication
      - Confirmed allergy to medication class
      - Life-threatening reaction history
      - Severe reaction history
    - **Major** (Requires caution):
      - Unconfirmed allergy to medication
      - Unconfirmed allergy to medication class
      - Moderate reaction history
      - Potential cross-reactivity
    - **Moderate** (Monitor closely):
      - Possible cross-reactivity
      - Mild reaction history
      - Related substance allergy
    - **Minor** (Informational):
      - Remote possibility of reaction
      - Related class allergy with low cross-reactivity
  
  - **Allergy Alert Information**:
    - Allergen name and type
    - Reaction type and severity
    - Date of allergy occurrence
    - Verification status
    - Cross-reactivity information
    - Alternative medication suggestions (if available)
    - Management recommendations

  - **Drug-Allergy Checking Implementation Phasing**:
    > **Critical note for implementation and QA teams**: The drug-allergy checking described in FR-P2.3 is satisfied in two phases, mirroring the Phase 1 / Phase 2 structure applied to drug-drug interaction checking (see FR-P1.7). Deploying Phase 1 does **not** mean FR-P2.3 is fully satisfied for clinical go-live. Phase 2 must be completed and validated before the system is used in a setting where an undetected drug-allergy interaction could cause patient harm.
    - **Phase 1 — Name-Based Heuristic Matching (implemented; see `AllergyMatchingService`)**:
      - **Coverage**: Direct drug-name match (case-insensitive, word-token level); brand/generic synonym equivalence (~55 pairs); combination-drug component decomposition (e.g., "Amox-Clav" → amoxicillin + clavulanate); drug-class membership matching for ~18 major classes (penicillins, cephalosporins, sulfonamides, fluoroquinolones, NSAIDs, opioids, statins, etc.); 5 documented cross-reactivity rules (penicillin→cephalosporin, opioid→opioid, etc.).
      - **Match types returned**: `DIRECT`, `SYNONYM`, `DRUG_COMPONENT`, `DRUG_CLASS`, `CROSS_REACTIVITY`. Cross-reactivity matches are flagged as warnings, not hard blocks, in the UI.
      - **Limitations**: Heuristics catch a representative but **non-exhaustive** subset. Novel allergen names, less common drug classes, chemical-structure-based allergies (e.g., dye components, preservatives), and allergen code (NDC/RxNorm) cross-references that are not in the coded synonym/class tables will be missed. Phase 1 is **not appropriate** as the sole allergy-checking layer for unsupervised clinical use.
      - Appropriate for: Development, UAT, pilot deployments with active clinical pharmacist oversight.
    - **Phase 2 — Allergen-Code Cross-Reference and Licensed Database (required before unsupervised clinical go-live)**:
      - Integrate an allergen cross-reference database (e.g., First Databank NDDF allergen hierarchy, Multum allergen groupings, or RxNorm/NDF-RT allergen class API) that provides:
        - NDC/RxNorm code → allergen class hierarchy lookups (so a patient allergic to "Amoxicillin" with an RxNorm code is automatically flagged for any NDC-coded amoxicillin product and related penicillins).
        - Complete drug-class membership for all approved NDC/RxNorm codes, not just the classes hardcoded in Phase 1.
        - Ingredient-level cross-reactivity (e.g., dye allergies, preservative allergies such as benzyl alcohol).
      - Phase 2 requires: signed data-licence agreement, SLA for database update frequency (at minimum monthly), integration test suite proving coverage of all FR-P2.3 allergy categories, and sign-off from a qualified clinical pharmacist.
    - **Transparency in the UI**: The allergy-alert dialog must display the `matchType` (DIRECT, DRUG_CLASS, CROSS_REACTIVITY, etc.) and the `clinicalNote` explanation so the prescriber understands why the alert was raised. A visible indicator of the data source must be shown: "Allergy heuristics (Phase 1) — consult clinical pharmacist for complex allergy situations" in Phase 1; the licensed data-source name in Phase 2.

- **FR-P2.4**: System shall support allergy checking configuration:
  - **Configurable Settings**:
    - Enable/disable allergy checking
    - Minimum severity level to display
    - Cross-reactivity checking rules
    - Drug class matching rules
    - Custom allergy rules (organization-specific)
  
  - **Allergy Database**:
    - Integration with allergy/drug databases
    - Cross-reactivity database
    - Drug class identification
    - Ingredient database
    - Alternative medication suggestions

##### 3.2.2.3 Drug-Disease Contraindication Checking

- **FR-P2.5**: System shall automatically check for drug-disease contraindications:
  - **Contraindication Detection**:
    - Check new medication against patient's active problems/diagnoses
    - Check new medication against patient's past medical history (if relevant)
    - Check for absolute contraindications
    - Check for relative contraindications
    - Check for disease-specific warnings
    - Real-time checking during medication selection
  
  - **Disease Information Used**:
    - Active diagnoses/problems
    - Past medical history
    - Chronic conditions
    - Organ system dysfunction (renal, hepatic, cardiac, etc.)
    - Pregnancy status (if applicable)
    - Age-related conditions
  
  - **Contraindication Types**:
    - **Absolute Contraindications**:
      - Medication should never be used with condition
      - Life-threatening risk
      - Severe adverse reaction risk
    - **Relative Contraindications**:
      - Medication use requires caution
      - May require dose adjustment
      - May require monitoring
      - Alternative medication preferred
    - **Warnings**:
      - Increased risk of adverse effects
      - Requires monitoring
      - Dose adjustment may be needed
    - **Precautions**:
      - Use with caution
      - Monitor for adverse effects
      - Informational
  
  - **Contraindication Information Displayed**:
    - Condition/disease name
    - Contraindication type (Absolute, Relative, Warning, Precaution)
    - Clinical significance
    - Potential adverse effects
    - Management recommendations:
      - Alternative medications
      - Dose adjustments
      - Monitoring requirements
      - Special precautions

##### 3.2.2.4 Duplicate Therapy Detection

- **FR-P2.6**: System shall automatically detect duplicate therapy:
  - **Duplicate Detection**:
    - Check for same medication already in patient's medication list
    - Check for medications with same active ingredient
    - Check for medications in same therapeutic class with similar mechanisms
    - Check for medications with overlapping indications
    - Check for medications with similar effects
    - Real-time checking during medication selection
  
  - **Duplicate Types**:
    - **Exact Duplicate**: Same medication, same strength, same form
    - **Same Ingredient**: Different brand, same active ingredient
    - **Therapeutic Duplicate**: Different medications, same therapeutic class and mechanism
    - **Overlapping Therapy**: Medications with similar or overlapping effects
  
  - **Duplicate Alert Information**:
    - Duplicate medications identified
    - Type of duplicate
    - Current medication details
    - New medication details
    - Recommendation (discontinue one, modify one, or proceed with caution)
    - Clinical rationale

##### 3.2.2.5 Dosage Appropriateness Validation

- **FR-P2.7**: System shall validate dosage appropriateness with the following specific validation rules:
  - **Age-Based Validation**:
    - **Pediatric Validation** (age < 18 years):
      - Check dosage against age-appropriate dosing guidelines
      - Age-specific maximum doses enforced (e.g., cannot exceed adult maximum for pediatric patients)
      - Age-specific contraindications checked (e.g., certain medications contraindicated in children < 2 years)
      - Require weight-based dosing for certain medications in pediatric patients
      - Warn if adult-only medication prescribed to pediatric patient
      - Age-specific dosing ranges applied (e.g., infants, children, adolescents have different ranges)
      - Display age-appropriate dosing recommendations
    
    - **Adult Validation** (age 18-65 years):
      - Standard adult dosing guidelines applied
      - Age-specific considerations for young adults (18-25) if applicable
      - Standard maximum doses applied
    
    - **Geriatric Validation** (age > 65 years):
      - Dosage may need reduction for geriatric patients (typically 25-50% reduction)
      - Warn if medication has geriatric-specific dosing requirements
      - Check for geriatric contraindications
      - Consider reduced dosing for medications with increased risk in elderly
      - Warn if standard adult dose may be too high for geriatric patient
      - Display geriatric dosing recommendations
  
  - **Weight-Based Validation**:
    - **Weight Requirement**:
      - Patient weight must be available for weight-based dosing medications
      - Weight must be recent: < 1 year old for pediatric patients, < 2 years for adults
      - Weight must be reasonable: Warn if weight seems incorrect (e.g., < 1 kg or > 500 kg)
      - Weight unit validation: Must be in kg or lbs (convert if needed)
    
    - **Dosage Calculation**:
      - Calculate dose per kg or per lb based on medication requirements
      - Formula validation: dose = (dosage per kg) × (patient weight in kg)
      - Formula validation: dose = (dosage per lb) × (patient weight in lbs)
      - Round dosage appropriately (typically to nearest 0.1 mg or 1 mg depending on medication)
      - Display calculated weight-based dose
    
    - **Weight-Based Dose Validation**:
      - Validate calculated dose against minimum effective dose
      - Validate calculated dose against maximum dose per weight
      - Validate calculated dose against standard maximum dose (warn if exceeds)
      - Alert if weight-based dose exceeds standard maximum (may indicate calculation error)
      - Alert if weight-based dose is below minimum effective dose
      - Display weight-based dosing range for medication
  
  - **Renal Function Validation**:
    - **Renal Function Requirement**:
      - Creatinine clearance (CrCl) or eGFR must be available for medications requiring renal adjustment
      - Renal function must be recent: < 1 year old (warn if > 6 months, require confirmation if > 1 year)
      - Renal function must be reasonable: CrCl typically 0-200 ml/min, eGFR typically 0-150 ml/min/1.73m²
    
    - **Dosage Adjustment**:
      - Recommend dose adjustment based on renal function:
        - CrCl > 50 ml/min: Standard dose
        - CrCl 30-50 ml/min: May require 25-50% dose reduction
        - CrCl 10-30 ml/min: May require 50-75% dose reduction
        - CrCl < 10 ml/min: May require 75%+ dose reduction or contraindicated
      - Display recommended dose adjustment
      - Warn if standard dose may be too high for renal function
      - Prevent prescription if medication contraindicated in severe renal impairment
    
    - **Renal Monitoring Requirements**:
      - Alert if medication requires renal function monitoring
      - Alert if medication contraindicated in renal impairment
      - Recommend monitoring schedule (e.g., "Monitor CrCl every 3 months")
      - Display renal function considerations for medication
  
  - **Hepatic Function Validation**:
    - **Hepatic Function Requirement**:
      - Liver function tests (ALT, AST, bilirubin) must be available for medications requiring hepatic adjustment
      - Hepatic function tests must be recent: < 1 year old (warn if > 6 months, require confirmation if > 1 year)
      - Test values must be reasonable (within typical ranges)
    
    - **Dosage Adjustment**:
      - Recommend dose adjustment based on hepatic function:
        - Normal liver function: Standard dose
        - Mild impairment: May require 25% dose reduction
        - Moderate impairment: May require 50% dose reduction
        - Severe impairment: May require 75%+ dose reduction or contraindicated
      - Display recommended dose adjustment
      - Warn if standard dose may be too high for hepatic function
      - Prevent prescription if medication contraindicated in severe hepatic impairment
    
    - **Hepatic Monitoring Requirements**:
      - Alert if medication requires hepatic function monitoring
      - Alert if medication contraindicated in hepatic impairment
      - Recommend monitoring schedule (e.g., "Monitor LFTs monthly")
      - Display hepatic function considerations for medication
  
  - **Dosage Range Validation**:
    - **Standard Dosing Ranges**:
      - Check dosage against medication-specific standard dosing ranges
      - Minimum effective dose: Warn if below minimum (may be ineffective)
      - Maximum recommended dose: Warn if exceeds maximum (may cause toxicity)
      - Maximum daily dose: Prevent if exceeds absolute maximum (safety limit)
      - Display standard dosing range for medication
    
    - **Dosage Range by Indication**:
      - Different indications may have different dosing ranges
      - Validate dosage against indication-specific range
      - Display indication-specific dosing recommendations
    
    - **Dosage Range Validation Process**:
      - Calculate total daily dose: (dosage strength) × (quantity per dose) × (frequency per day)
      - Compare against minimum and maximum daily doses
      - Warn if below minimum: "Dosage may be below minimum effective dose"
      - Warn if above maximum: "Dosage exceeds maximum recommended dose"
      - Prevent if above absolute maximum: "Dosage exceeds absolute maximum (safety limit)"
      - Display current dosage vs. recommended range
  
  - **Dosage Validation Information Display**:
    - Current prescribed dosage
    - Recommended dosage range (minimum to maximum)
    - Patient-specific factors considered (age, weight, renal function, hepatic function)
    - Dose adjustment recommendations (if applicable)
    - Monitoring recommendations (if applicable)
    - Warnings and alerts (if any)
    - Rationale for dosage validation results

##### 3.2.2.6 Alert Display and Management

- **FR-P2.8**: System shall display alerts prominently and effectively:
  - **Alert Display Methods**:
    - **Visual Indicators**:
      - Color coding (Red for Critical, Orange for Major, Yellow for Moderate, Blue for Minor)
      - Bold text for critical alerts
      - Alert icons
      - Highlighted alert sections
      - Alert banners
    - **Alert Location**:
      - Display alerts during medication selection
      - Display alerts in prescription summary
      - Display alerts prominently in prescription review
      - Persistent alerts until acknowledged
    - **Alert Grouping**:
      - Group alerts by type (Interactions, Allergies, Contraindications, etc.)
      - Group alerts by severity
      - Display most critical alerts first
      - Collapsible alert sections
  
  - **Alert Information Display**:
    - Alert type (Interaction, Allergy, Contraindication, etc.)
    - Severity level (prominently displayed)
    - Alert title/heading
    - Detailed alert description
    - Affected medications/conditions
    - Clinical significance
    - Management recommendations
    - Alternative suggestions (if available)
    - References (if available)
  
  - **Alert Interaction**:
    - Expand/collapse alert details
    - Scroll through multiple alerts
    - Print alert information
    - Access additional information
    - Link to drug information

- **FR-P2.9**: System shall require provider acknowledgment for alerts:
  - **Acknowledgment Requirements**:
    - **Critical Alerts**: Must acknowledge before prescription can be completed
    - **Major Alerts**: Must acknowledge before prescription can be completed
    - **Moderate Alerts**: Must acknowledge (may be configurable)
    - **Minor Alerts**: May be informational only (acknowledgment optional)
  
  - **Acknowledgment Process**:
    - Display acknowledgment checkbox or button
    - Require acknowledgment for each critical/major alert
    - Allow bulk acknowledgment for multiple alerts (with review)
    - Acknowledgment timestamp recorded
    - Acknowledgment user recorded
  
  - **Acknowledgment Documentation**:
    - Track which alerts were acknowledged
    - Track which alerts were not acknowledged
    - Track acknowledgment time
    - Track acknowledgment user
    - Include in audit trail

##### 3.2.2.7 Alert Override and Documentation

- **FR-P2.10**: System shall support alert override with proper documentation:
  - **Override Capability**:
    - Allow provider to override alerts (with proper authorization)
    - Override available for all alert types
    - Override requires acknowledgment and reason
    - Override may require additional authorization for critical alerts
  
  - **Override Process**:
    - Provider acknowledges alert
    - Provider selects override option
    - Provider enters override reason (required, free text or structured)
    - System may require supervisor approval for critical overrides (configurable)
    - Override confirmed
    - Prescription can proceed
  
  - **Override Documentation**:
    - Override reason documented
    - Override timestamp recorded
    - Override user recorded
    - Override supervisor (if applicable) recorded
    - Override included in audit trail
    - Override visible in prescription history
  
  - **Override Restrictions**:
    - Some alerts may not be overridable (configurable)
    - Critical drug-allergy interactions may require additional steps
    - Override may be restricted based on user role
    - Override may require additional documentation for certain alerts

##### 3.2.2.8 Real-Time Checking and Performance

- **FR-P2.11**: System shall perform real-time interaction checking:
  - **Checking Timing**:
    - Check interactions immediately when medication is selected
    - Re-check interactions when dosage is modified
    - Re-check interactions when frequency is modified
    - Re-check interactions when other medications are added
    - Check interactions before prescription finalization
    - Check interactions during prescription review
  
  - **Performance Requirements**:
    - Interaction checking completes within 2 seconds (95% of requests)
    - No noticeable delay in prescription workflow
    - Background checking where possible
    - Caching of interaction results (where appropriate)
    - Efficient database queries
  
  - **Checking Scope**:
    - Check against current medication list
    - Check against medications in current prescription
    - Check against recently discontinued medications (configurable time period)
    - Check against medications on hold
    - Check against patient allergies
    - Check against patient problems/diagnoses
    - Check against patient demographics (age, weight, etc.)
    - Check against patient lab results (renal function, etc.)

##### 3.2.2.9 Integration with Clinical Decision Support

- **FR-P2.12**: System shall integrate with clinical decision support systems:
  - **CDS Integration**:
    - Integration with clinical decision support rules
    - Evidence-based recommendations
    - Guideline-based alerts
    - Best practice suggestions
    - Quality measure support
  
  - **CDS Features**:
    - Medication appropriateness checking
    - Indication-based prescribing
    - Cost-effectiveness recommendations
    - Quality measure compliance
    - Best practice alerts
    - Evidence-based alternatives

##### 3.2.2.10 Reporting and Analytics

- **FR-P2.13**: System shall provide reporting capabilities for interaction checking:
  - **Interaction Reports**:
    - Interactions detected by type
    - Interactions by severity
    - Interactions overridden
    - Override reasons
    - Provider interaction patterns
    - Medication interaction frequency
  
  - **Quality Reports**:
    - Interaction detection rate
    - Alert acknowledgment rate
    - Override rate
    - Override appropriateness
    - Medication safety metrics
  
  - **Analytics**:
    - Trend analysis of interactions
    - Common interaction patterns
    - Provider prescribing patterns
    - Medication safety trends

##### 3.2.2.9 Error Handling and Recovery

- **FR-P2.14**: System shall implement comprehensive error handling for drug interaction and allergy checking:
  - **Interaction Database Error Handling**:
    - **Database Connection Failures**:
      - Handle interaction database connection failures
      - Handle database timeout errors
      - Display warning: "Interaction checking temporarily unavailable. Please verify interactions manually."
      - Allow prescription creation to proceed (with warning and acknowledgment)
      - Queue interaction checks for retry when database available
      - Cache recent interaction results for offline use
      - Log database connection failures
    
    - **Database Query Errors**:
      - Handle query timeout errors
      - Handle query syntax errors
      - Handle invalid medication codes in queries
      - Display error: "Unable to check interactions for this medication. Please verify manually."
      - Allow prescription creation with manual verification
      - Log query errors with medication information
    
    - **Database Response Errors**:
      - Handle invalid response formats
      - Handle missing required data in responses
      - Handle corrupted response data
      - Fall back to cached interaction data if available
      - Display warning if using cached data
      - Log response errors
  
  - **Interaction Calculation Error Handling**:
    - **Calculation Failures**:
      - Handle interaction calculation errors
      - Handle invalid medication combinations in calculations
      - Handle missing medication data for calculations
      - Display error: "Unable to calculate interactions. Please verify manually."
      - Allow prescription creation with manual verification
      - Log calculation errors
    
    - **Severity Level Errors**:
      - Handle missing severity level in interaction results
      - Handle invalid severity level values
      - Default to "Moderate" if severity unknown
      - Display warning: "Interaction severity unknown. Please verify manually."
      - Log severity level errors
  
  - **Alert Display Error Handling**:
    - **Alert Generation Failures**:
      - Handle alert generation errors
      - Handle missing alert data
      - Display generic alert if specific alert unavailable
      - Log alert generation failures
    
    - **Alert Display Failures**:
      - Handle UI rendering errors for alerts
      - Handle alert display timeout errors
      - Fall back to simple text alert if rich display fails
      - Log display errors
    
    - **Alert Acknowledgment Errors**:
      - Handle acknowledgment save failures
      - Retry acknowledgment saves
      - Display warning if acknowledgment not saved
      - Log acknowledgment errors
  
  - **Allergy Checking Error Handling**:
    - **Allergy Database Errors**:
      - Handle allergy database connection failures
      - Handle allergy lookup failures
      - Display warning: "Allergy checking temporarily unavailable. Please verify allergies manually."
      - Allow prescription creation with manual allergy verification
      - Queue allergy checks for retry
      - Cache allergy data for offline use
    
    - **Allergy Matching Errors**:
      - Handle allergy matching algorithm failures
      - Handle missing allergy data
      - Display warning: "Unable to verify allergies. Please check patient's allergy list manually."
      - Log matching errors
    
    - **Cross-Reactivity Checking Errors**:
      - Handle cross-reactivity database failures
      - Handle cross-reactivity calculation errors
      - Display warning if cross-reactivity checking unavailable
      - Log cross-reactivity errors
  
  - **Dosage Validation Error Handling**:
    - **Dosage Calculation Errors**:
      - Handle age-based dosage calculation failures
      - Handle weight-based dosage calculation failures
      - Handle renal function dosage calculation failures
      - Handle hepatic function dosage calculation failures
      - Display error: "Unable to validate dosage appropriateness. Please verify manually."
      - Allow prescription creation with manual verification
      - Log calculation errors
    
    - **Patient Data Errors**:
      - Handle missing patient age for age-based validation
      - Handle missing patient weight for weight-based validation
      - Handle missing renal function for renal validation
      - Handle missing hepatic function for hepatic validation
      - Display warning: "Patient data incomplete for dosage validation. Please verify dosage manually."
      - Allow prescription creation with warning
      - Log missing data warnings
  
  - **System Error Handling**:
    - **Performance Errors**:
      - Handle slow interaction checking (timeout after 10 seconds)
      - Display progress indicator for long-running checks
      - Allow cancellation of slow checks
      - Queue checks for background processing if too slow
      - Log performance issues
    
    - **Memory Errors**:
      - Handle memory exhaustion during large interaction checks
      - Optimize checks for large medication lists
      - Display error if memory insufficient
      - Log memory errors
    
    - **Application Errors**:
      - Handle application crashes during interaction checking
      - Recover interaction check state after crash
      - Display recovery message
      - Log application errors
  
  - **Integration Error Handling**:
    - **CDS Integration Errors**:
      - Handle clinical decision support system connection failures
      - Handle CDS service unavailability
      - Display warning: "Clinical decision support temporarily unavailable."
      - Allow prescription creation without CDS
      - Queue CDS checks for retry
      - Log CDS integration errors
    
    - **External System Integration Errors**:
      - Handle failures from external drug databases
      - Handle failures from external allergy systems
      - Fall back to internal databases if available
      - Display warning if using fallback
      - Log integration errors
  
  - **Data Consistency Error Handling**:
    - **Medication List Synchronization Errors**:
      - Handle medication list retrieval failures
      - Handle medication list update conflicts
      - Use cached medication list if available
      - Display warning if using cached data
      - Queue synchronization for retry
    
    - **Allergy List Synchronization Errors**:
      - Handle allergy list retrieval failures
      - Handle allergy list update conflicts
      - Use cached allergy list if available
      - Display warning if using cached data
      - Queue synchronization for retry
  
  - **Recovery Mechanisms**:
    - **Retry Logic**:
      - Automatic retry for transient errors (up to 3 attempts)
      - Exponential backoff for retries
      - Manual retry option for all errors
      - Display retry status to user
    
    - **Fallback Mechanisms**:
      - Use cached interaction data if database unavailable
      - Use simplified checking if full checking unavailable
      - Allow manual verification if automated checking fails
      - Display fallback status to user
    
    - **Error Recovery**:
      - Restore interaction check state after errors
      - Allow prescription creation after error recovery
      - Maintain error context for troubleshooting
      - Log all recovery attempts
  
  - **Error Logging and Reporting**:
    - Log all errors with full context:
      - Error type and message
      - Medications being checked
      - Patient ID
      - User ID
      - Timestamp
      - System state
      - Interaction database status
    - Generate error reports for system administrators
    - Alert administrators for critical errors (database down, etc.)
    - Track error trends and patterns
    - Monitor interaction checking performance

#### 3.2.3 Prescription Transmission

##### 3.2.3.1 Electronic Prescription Transmission Methods

- **FR-P3.1**: System shall support multiple electronic prescription transmission methods:
  - **E-Prescribing Network Transmission** (Primary Method):
    - Integration with Surescripts network (or similar e-prescribing network)
    - Real-time electronic transmission to pharmacies
    - Support for NCPDP SCRIPT standard messages
    - Support for HL7 FHIR MedicationRequest resources (if applicable)
    - Network connectivity and availability monitoring
    - Automatic retry on transmission failure
    - Transmission confirmation and status updates
  
  - **Direct Pharmacy Integration**:
    - Direct integration with pharmacy systems (if available)
    - Point-to-point transmission
    - Support for pharmacy-specific protocols
    - Faster transmission for integrated pharmacies
    - Enhanced status tracking for integrated pharmacies
  
  - **Fax Transmission** (Fallback Method — **Phase 2; scoped below**):
    > **Scope decision**: Fax transmission is a fallback for pharmacies not reachable via e-prescribing network. It is **deferred to Phase 2** and must not block Phase 1 delivery. The following requirements apply when fax is in scope:
    - **Fax gateway**: System must integrate with a fax gateway service (e.g., Sfax, eFax Developer API, or Twilio Fax). The specific gateway vendor must be selected and contracted before Phase 2 implementation begins; this document does not mandate a vendor.
    - **Fax number source**: Fax number must be sourced from the pharmacy directory record (FR-P3.5); free-text fax number entry by the prescriber is not permitted (prevents misdirected prescriptions).
    - **Prescription format**: Fax output must use the same PDF template as the print output (FR-P3.1 Print Option below), rendered server-side, not client-side.
    - **Delivery confirmation**: Fax gateway must return a delivery confirmation (success/failure + timestamp). Failed fax transmissions must be surfaced to the prescriber with a retry option.
    - **Controlled substances**: Fax transmission is not permitted for Schedule II controlled substances in jurisdictions that require e-prescribing for controlled substances (EPCS). System must block fax for Schedule II unless the organization's jurisdiction explicitly exempts fax.
    - **Audit**: All fax transmission attempts (success, failure, retry) must be logged in `ehr.prescription_transmissions` with `transmission_method = FAX`.
  
  - **Print Option** (Patient Hand-Carry — **Phase 1, server-side PDF only**):
    > **Scope decision**: Printing is supported via **server-generated PDF download** only. Browser print-dialog printing of HTML is not in scope due to layout inconsistency across printers.
    - **PDF generation**: Server must generate a prescription PDF upon request. PDF must be stored in `ehr.prescription_transmissions` (or document store) linked to the prescription, with `transmission_method = PRINT`.
    - **Layouts supported** (Phase 1):
      - **Full A4** — standard hospital letterhead, all prescription fields, prescriber signature block, hospital stamp area, barcode/QR code encoding the prescription number.
      - **Compact half-page** — condensed layout for thermal/label printers, medication name, dosage, instructions, prescriber name and NPI, prescription number barcode.
    - **Security features**:
      - Prescription number barcode (Code 128 or QR code) on all layouts.
      - "VOID" watermark automatically applied if prescription is cancelled or superseded after PDF generation.
      - Printed PDFs are marked as `DRAFT` in the header if the prescription has not yet been finalised/signed.
    - **Controlled substances**: Printed controlled substance prescriptions must include all state-required security elements. System must display a warning if the organisation's state requires e-prescribing for controlled substances and the provider chooses to print instead.
    - **Audit**: Each PDF generation event must be logged (user, timestamp, layout chosen) in `ehr.prescription_transmissions`.

- **FR-P3.2**: System shall support transmission priority and timing:
  - **Transmission Priority**:
    - Stat/Urgent prescriptions transmitted immediately
    - Routine prescriptions transmitted in batch or immediately (configurable)
    - Scheduled transmission for non-urgent prescriptions
    - Priority-based transmission queue
  
  - **Transmission Timing**:
    - Immediate transmission (default)
    - Scheduled transmission (if configured)
    - Batch transmission (if configured)
    - Transmission retry on failure
    - Transmission timeout handling

##### 3.2.3.2 Pharmacy Selection and Management

- **FR-P3.3**: System shall support comprehensive pharmacy selection:
  - **Pharmacy Search Methods**:
    - Search by pharmacy name
    - Search by location (city, state, ZIP code)
    - Search by NPI (National Provider Identifier)
    - Search by phone number
    - Search by distance from patient address (if available)
    - Browse pharmacies by location
    - Browse pharmacies by chain/network
    - Recent pharmacies list
    - Favorite pharmacies list
  
  - **Pharmacy Information Display**:
    - Pharmacy name
    - Pharmacy address
    - Pharmacy phone number
    - Pharmacy fax number
    - Pharmacy NPI
    - Pharmacy hours (if available)
    - Pharmacy services (if available)
    - Distance from patient (if available)
    - Network status (on e-prescribing network, integrated, etc.)
  
  - **Pharmacy Selection**:
    - Select single pharmacy for prescription
    - Select different pharmacy for each prescription (if multiple prescriptions)
    - Save pharmacy selection for future prescriptions
    - Default pharmacy selection (patient's preferred pharmacy)

- **FR-P3.4**: System shall support patient pharmacy preferences:
  - **Patient Pharmacy Management**:
    - Store patient's preferred pharmacy
    - Store multiple pharmacy options per patient
    - Store pharmacy preferences (primary, secondary, etc.)
    - Display patient's preferred pharmacy during prescription creation
    - Allow patient to change preferred pharmacy
    - Support for pharmacy chains (patient may prefer any location in chain)
  
  - **Pharmacy Preference Features**:
    - Auto-select patient's preferred pharmacy
    - Display patient's pharmacy history
    - Display recently used pharmacies
    - Allow pharmacy selection override
    - Support for pharmacy change requests

- **FR-P3.5**: System shall maintain a pharmacy directory as a first-class master data entity:

  - **Pharmacy Data Model**: Each pharmacy entry in the `hospital_pharmacy.pharmacy_directory` table (or equivalent) must store at minimum:
    - `pharmacy_id` — internal UUID primary key.
    - `npi` — 10-digit NPI (unique, required; source of truth for identity).
    - `name` — pharmacy trading name (required).
    - `address_line1`, `address_line2`, `city`, `state`, `zip` — physical address (required).
    - `phone` — primary contact phone (required; E.164 format).
    - `fax` — fax number (required if fax transmission is enabled for this pharmacy; E.164 format).
    - `email` — contact email (optional).
    - `ncpdp_id` — NCPDP Provider ID (required for SCRIPT transmission; 7-digit format).
    - `is_eprescribing_capable` — boolean; true if pharmacy is reachable via e-prescribing network.
    - `is_active` — boolean; inactive pharmacies must not appear in prescriber search results.
    - `chain_name` — pharmacy chain affiliation (optional; used for patient-preferred chain lookup).
    - `hours_json` — operating hours per day (optional; JSON).
    - `last_verified_at` — timestamp of last data verification (for staleness tracking).
    - `data_source` — enum: `MANUAL | SURESCRIPTS_DIRECTORY | NPI_REGISTRY | IMPORT` (audit of how the record was created/updated).

  - **Population and Synchronisation**:
    - **Primary source — Surescripts Directory**: System should support importing and periodically syncing the Surescripts pharmacy directory (Surescripts Directory Download or real-time lookup API). Sync frequency: at minimum weekly full refresh; real-time lookup at point of prescriber search is preferred.
    - **Secondary source — NPPES NPI Registry**: System shall support NPI Registry lookup (via the public CMS NPI API) to validate or enrich pharmacy records by NPI at save time.
    - **Manual entry**: Admins may add pharmacies not in Surescripts (e.g., hospital in-house pharmacy, compounding pharmacy). Manually entered records must be flagged `data_source = MANUAL` and reviewed for accuracy.
    - **Conflict resolution**: If Surescripts sync updates a field that Admin has manually overridden, the system must preserve the manual override and flag the conflict for Admin review rather than silently overwriting.

  - **Search and Lookup Requirements**:
    - Prescriber pharmacy search (FR-P3.3) must query the local directory, not call Surescripts in real time for every keystroke. Local index must be kept fresh via the sync process above.
    - Search must support: name (partial match), NPI (exact), NCPDP ID (exact), city+state, ZIP code radius (if geolocation is configured).
    - Results must indicate e-prescribing capability (`is_eprescribing_capable`) so the prescriber knows which transmission method will be used.

  - **Maintenance (Admin)**:
    - Admin UI must allow: add, edit, deactivate, and trigger manual re-verification of any pharmacy entry.
    - Deactivation must not delete the record (retain for historical prescription audit); deactivated pharmacies must not appear in prescriber-facing search.
    - Admin must be able to bulk-import pharmacies via CSV with NPI as the primary key (upsert semantics).
    - System must surface a staleness alert for any pharmacy record not verified within **90 days**, prompting Admin to re-verify.

  - **Data Quality**:
    - NPI format must be validated (10 digits, passes Luhn check) before any pharmacy record is saved.
    - NCPDP ID format must be validated (7 digits) before any pharmacy record is saved.
    - Phone and fax numbers must be normalised to E.164 format on save.
    - Duplicate detection: system must warn Admin when a new pharmacy entry has the same NPI or NCPDP ID as an existing active record.

##### 3.2.3.3 Prescription Format and Standards

- **FR-P3.6**: System shall generate prescriptions in standard formats:
  - **NCPDP SCRIPT Standard** (Primary Standard):
    - **Required version: NCPDP SCRIPT 2017071** (Surescripts production standard as of 2024).
      - SCRIPT 10.6 is permitted only as a legacy fallback for pharmacies that have not yet migrated; must be logged and flagged for follow-up.
      - System must be architected to allow version upgrade to SCRIPT 2023011 without full message-layer rewrite (version should be a configuration parameter, not hardcoded).
    - Support for all required SCRIPT message types (with version-specific segment names per 2017071 specification):
      - `NewRx` — New Prescription
      - `RxChange` / `ChangeRx` — Prescription Change Request/Response
      - `CancelRx` / `CancelRxResponse` — Prescription Cancellation
      - `RxFill` — Fill status notification (inbound from pharmacy)
      - `RxRenewalRequest` / `RxRenewalResponse` — Refill Request/Response
      - `GetMedicationHistory` / `MedicationHistoryResponse` — Medication history query (if PDMP integration uses SCRIPT)
    - Message XML must conform to the NCPDP SCRIPT 2017071 XSD schema; schema validation must run before transmission and failures must be surfaced as pre-transmission errors (not runtime exceptions).
    - Required data elements per 2017071 must be included; optional elements should be included where data is available.
    - Message validation before transmission (schema + business rules; see FR-P3.7)
    - **Phase 1 limitation — single-medication NewRx only**: The current implementation encodes only the *primary* medication (`Prescription.medicationName`) in the NCPDP `NewRx` message. Prescriptions with multiple `PrescriptionMedication` line items (e.g. Amoxicillin + Ibuprofen + Metronidazole created via Easy Prescription) will silently transmit only the first drug; remaining line items are not included in the NCPDP message. The implementation logs a `WARN`-level entry for every such transmission so the gap is visible in operational logs. **Phase 2 fix required**: generate one `NewRx` per `PrescriptionMedication` line item (each carrying its own `DrugDescription`, `DrugCoded`, `Quantity`, `Sig`, and refill data), grouped under a single batch transaction ID so the pharmacy receives a complete multi-drug order. Until Phase 2 is delivered, the UI must not allow creation of multi-drug prescriptions for patients routed to NCPDP-connected pharmacies, or must warn the prescriber that only the primary medication will be transmitted.
  
  - **HL7 FHIR Support** (If Applicable):
    - Support for HL7 FHIR MedicationRequest resource
    - FHIR resource creation and formatting
    - FHIR resource validation
    - FHIR message transmission
  
  - **Prescription Data Elements**:
    - Patient information (name, DOB, address, phone)
    - Prescriber information (name, NPI, DEA, license, address, phone)
    - Medication information (name, NDC, strength, form, quantity)
    - Dosage instructions (route, frequency, timing, special instructions)
    - Refill information (number of refills, refill authorization)
    - Substitution information (DAW code)
    - Prescription date and expiration date
    - Pharmacy information
    - Prior authorization information (if applicable)
    - Diagnosis codes (if applicable)
    - Prescription number
    - Prescription status

- **FR-P3.7**: System shall validate prescription data before transmission with the following specific validation rules:
  - **Required Fields Validation**:
    - **Patient Information** (all required):
      - Patient name (first and last): Required, 1-100 characters each
      - Patient date of birth: Required, valid date format
      - Patient address: Required (at least street, city, state, ZIP)
      - Patient phone: Required, valid format
    - **Prescriber Information** (all required):
      - Prescriber name: Required
      - Prescriber NPI: Required, 10 digits, valid format
      - Prescriber DEA: Required for controlled substances, valid format (see FR-P7.2)
      - Prescriber license number: Required, state-specific format
      - Prescriber address: Required
      - Prescriber phone: Required, valid format
    - **Medication Information** (all required):
      - Medication name: Required
      - NDC code: Required (if available), valid format
      - Dosage strength: Required, valid numeric
      - Dosage form: Required
      - Quantity: Required, valid numeric
      - Route: Required
      - Frequency: Required
    - **Prescription Information** (all required):
      - Prescription date: Required, valid date
      - Refills: Required, valid integer
      - Substitution preference: Required (DAW code)
    - **Pharmacy Information** (all required):
      - Pharmacy name: Required
      - Pharmacy NCPDP ID: Required, 7 digits, valid format
      - Pharmacy address: Required
      - Pharmacy phone: Required, valid format
    - System shall prevent transmission if any required field is missing
    - System shall display list of missing required fields
  
  - **Code Format Validation**:
    - **NDC Code Validation**:
      - Format: 10 or 11 digits (XXXXX-XXXX-XX or XXXXXXXXXX)
      - Must exist in NDC database (if available)
      - Must match selected medication
      - Cannot be all zeros or invalid codes
    
    - **NPI Validation**:
      - Format: Exactly 10 digits, all numeric — regex `^[0-9]{10}$`.
      - **Check-digit algorithm** (ISO/IEC 7812 Luhn with CMS NPI prefix — same algorithm specified in FR-P1.10 Regulatory Validation):
        1. Prepend `80840` to the 10-digit NPI to produce a 15-digit working string.
        2. Starting from the rightmost digit (position 1), double every digit at an even position (positions 2, 4, 6, …).
        3. If any doubled value exceeds 9, subtract 9 from it.
        4. Sum all 15 digits.
        5. Valid if and only if `sum mod 10 = 0`.
      - Transmission must be blocked if the NPI fails the Luhn check (HTTP pre-transmission validation error, not a runtime exception).
      - Must match the prescribing provider's NPI on file.
      - Cannot be all zeros (`0000000000`).
      - Must exist in NPI registry (NPPES lookup, if integrated).

    - **DEA Number Validation** (for controlled substances):
      - Format: 2 letters + 7 digits (see FR-P7.2 for detailed validation)
      - Checksum validation
      - Must match prescribing provider
      - Must be active and valid
    
    - **NCPDP Pharmacy ID Validation**:
      - Format: Exactly 7 digits (numeric)
      - Must exist in pharmacy database
      - Must be active pharmacy
      - Cannot be all zeros
    
    - **License Number Validation**:
      - Format: State-specific format
      - Must match state of practice
      - Must be valid format for state
      - Must match prescribing provider
  
  - **Date Validation**:
    - **Prescription Date**:
      - Must be valid calendar date
      - Cannot be more than 1 day in the future (warn if future date)
      - Cannot be more than 1 year in the past (warn if > 30 days old)
      - Date format: MM/DD/YYYY or YYYY-MM-DD
      - Leap year validation
    
    - **Start Date**:
      - Must be valid calendar date
      - Cannot be more than 1 year in the future
      - Cannot be more than 1 year in the past
      - Must be on or after prescription date
    
    - **Expiration Date** (if applicable):
      - Must be valid calendar date
      - Must be after prescription date
      - Must be after start date
      - Must comply with schedule limits (Schedule II: 30 days, Schedule III-V: 90 days)
  
  - **Quantity and Dosage Validation**:
    - **Quantity Validation**:
      - Must be valid positive integer
      - Must be > 0
      - Must be reasonable for medication type (see FR-P1.10)
      - Must comply with schedule limits for controlled substances (see FR-P7.6)
    
    - **Dosage Strength Validation**:
      - Must be valid positive numeric value
      - Must be > 0
      - Must be within medication-specific range
      - Must be reasonable (see FR-P1.10)
    
    - **Frequency Validation**:
      - Must be valid frequency
      - Must be reasonable (see FR-P1.10)
      - Must not exceed maximum daily frequency for medication
  
  - **Patient Information Validation**:
    - Patient name: Valid format (1-100 characters, alphanumeric and common characters)
    - Patient DOB: Valid date, cannot be in future, reasonable age (0-150 years)
    - Patient address: Valid format (see FR-1.4 for address validation)
    - Patient phone: Valid format (10 digits for US, see FR-1.4)
  
  - **Prescriber Information Validation**:
    - Prescriber name: Valid format (1-100 characters)
    - Prescriber NPI: Valid format (10 digits, checksum valid)
    - Prescriber DEA: Valid format (if controlled substance, see FR-P7.2)
    - Prescriber license: Valid format for state
    - Prescriber address: Valid format
    - Prescriber phone: Valid format
  
  - **Pharmacy Information Validation**:
    - Pharmacy name: Required, 1-200 characters
    - Pharmacy NCPDP ID: Valid format (7 digits)
    - Pharmacy address: Valid format
    - Pharmacy phone: Valid format
    - Pharmacy must be active and accepting prescriptions
  
  - **Regulatory Requirements Validation**:
    - **Controlled Substances** (if applicable):
      - DEA number valid (see FR-P7.2)
      - Quantity limits met (see FR-P7.6)
      - Duration limits met (see FR-P7.6)
      - Refill limits met (see FR-P7.7)
      - State-specific requirements met (see FR-P7.8)
      - PDMP query completed (if required, see FR-P7.4)
    
    - **State Requirements**:
      - State-specific prescription format requirements met
      - State-specific quantity/duration limits met
      - State-specific documentation requirements met
      - State-specific prescriber requirements met
  
  - **NCPDP SCRIPT Format Validation**:
    - **Message Structure Validation**:
      - Message type valid (NewRx, RxChange, RxCancel, etc.)
      - Message version valid (10.6 or later)
      - Required segments present
      - Required fields within segments present
      - Segment order correct
      - Field delimiters correct
    
    - **Data Element Validation**:
      - All required data elements present
      - Data elements in correct format
      - Data elements within valid value sets
      - Data element lengths within limits
      - Data element relationships valid (e.g., quantity must match days supply)
    
    - **Business Rule Validation**:
      - Prescription business rules met
      - Pharmacy business rules met
      - Regulatory business rules met
      - NCPDP SCRIPT business rules met
  
  - **Validation Error Handling**:
    - **Error Detection**:
      - Detect all validation errors before transmission
      - Categorize errors (Critical, Warning, Informational)
      - Group related errors together
      - Count total errors
    
    - **Error Display**:
      - Display validation errors prominently before transmission
      - Display specific error messages for each validation failure
      - Error messages shall indicate which field failed and why
      - Error messages shall suggest corrections when possible
      - Highlight invalid fields visually
      - Display error count (e.g., "5 validation errors found")
    
    - **Error Prevention**:
      - Prevent transmission if critical errors present
      - Prevent transmission if required fields missing
      - Prevent transmission if codes invalid
      - Prevent transmission if regulatory requirements not met
      - Allow transmission if only warnings (with acknowledgment)
    
    - **Error Correction**:
      - Allow correction of errors before transmission
      - Maintain entered data when validation fails
      - Re-validate after corrections
      - Display updated error list after corrections
      - Allow transmission once all critical errors resolved

##### 3.2.3.4 Transmission Process and Workflow

- **FR-P3.8**: System shall support prescription transmission workflow:
  - **Pre-Transmission Steps**:
    - Prescription review and confirmation
    - Provider electronic signature
    - Final validation
    - Pharmacy selection confirmation
    - Transmission method selection (if multiple options)
  
  - **Transmission Process**:
    - Generate prescription message in standard format
    - Validate prescription message
    - Establish connection with transmission network/pharmacy
    - Transmit prescription message
    - Receive transmission confirmation
    - Update prescription status
    - Log transmission activity
  
  - **Post-Transmission Steps**:
    - Display transmission confirmation
    - Update prescription status to "Sent"
    - Record transmission timestamp
    - Record transmission method
    - Record pharmacy information
    - Generate transmission receipt (if applicable)
    - Notify provider of successful transmission

- **FR-P3.9**: System shall support batch prescription transmission:
  - **Batch Transmission**:
    - Group multiple prescriptions for same patient
    - Group multiple prescriptions for transmission
    - Batch transmission to same pharmacy
    - Batch transmission to different pharmacies (if applicable)
    - Batch transmission confirmation
    - Individual prescription status tracking within batch
  
  - **Batch Management**:
    - Create prescription batches
    - Review batch before transmission
    - Transmit entire batch
    - Track batch transmission status
    - Handle partial batch failures

##### 3.2.3.5 Transmission Confirmation and Status

- **FR-P3.10**: System shall track prescription transmission status:
  - **Transmission Status Types**:
    - **Pending**: Prescription queued for transmission
    - **Transmitting**: Prescription being transmitted
    - **Sent**: Prescription successfully transmitted
    - **Received**: Prescription received by pharmacy (if confirmation available)
    - **Failed**: Transmission failed
    - **Retrying**: Transmission retry in progress
    - **Cancelled**: Transmission cancelled
  
  - **Status Tracking**:
    - Real-time status updates
    - Status history
    - Status change timestamps
    - Status change reasons (if applicable)
    - Status displayed in prescription list
    - Status displayed in prescription detail

- **FR-P3.11**: System shall handle transmission confirmations:
  - **Confirmation Types**:
    - Transmission confirmation (message sent)
    - Delivery confirmation (message received by pharmacy)
    - Read confirmation (pharmacy opened message, if available)
    - Processing confirmation (pharmacy processing prescription, if available)
  
  - **Confirmation Handling**:
    - Receive confirmations from transmission network
    - Receive confirmations from pharmacy (if integrated)
    - Update prescription status based on confirmations
    - Display confirmation information
    - Log confirmation in audit trail
    - Alert provider if confirmation not received (configurable timeout)

##### 3.2.3.5a Pharmacy Fill-Status Callback (Inbound Webhook)

- **FR-P3.11a**: System shall expose and enforce a documented inbound webhook for pharmacies and e-prescribing networks to report fill-status updates:

  - **What this endpoint is**: `POST /api/hospital/prescriptions/transmissions/fill-status` — the existing implementation endpoint — is the canonical fill-status callback. This requirement formalises its contract and security model so it can be shared with pharmacy integration partners.

  - **Caller identity**:
    - The endpoint must be protected by a **shared-secret HMAC-SHA256 signature** on the request body. Callers (pharmacy system or Surescripts relay) must include an `X-Webhook-Signature: sha256=<hex>` header. Requests without a valid signature must be rejected with HTTP 401.
    - Shared secrets are provisioned per pharmacy integration, stored encrypted, and rotatable without downtime.
    - IP allowlisting is a secondary control (optional, configurable per integration); it must not be the sole authentication mechanism.

  - **Request contract**:
    - `networkTransactionId` (required) — the unique transaction ID returned at time of transmission; used to correlate the callback to the correct `ehr.prescription_transmissions` row.
    - `fillStatus` (required) — enum: `PENDING | IN_PROGRESS | ON_HOLD | OUT_OF_STOCK | PARTIALLY_FILLED | FILLED | PICKED_UP | CANCELLED | REJECTED | EXPIRED`. All 10 values are valid inbound fill-status codes. **Note:** `PICKED_UP`, `CANCELLED`, `REJECTED`, and `EXPIRED` are terminal states — the system will reject any subsequent transition away from them with HTTP 409. A pharmacy integration partner should never attempt to transition *out of* a terminal state, but may still report a terminal state as the `fillStatus` of a new callback (e.g. reporting `CANCELLED` from `PENDING` is a valid single-hop transition per the state machine below).
    - `fillStatusDate` (required) — ISO-8601 UTC timestamp of the fill event at the pharmacy. **System must reject requests with a null or absent `fillStatusDate` with HTTP 400. The server must not substitute `now()` for a missing pharmacy-provided date — the timestamp must originate from the pharmacy's system to preserve accurate fill-event timing for audit and billing reconciliation. This prohibition also applies to internal callers (e.g. integration tests) that bypass the HTTP layer; the service layer enforces the same guard directly.**
    - `fillStatusMessage` (optional) — human-readable status note from pharmacy.
    - `filledDate` (conditional) — required if `fillStatus = FILLED | PARTIALLY_FILLED`.
    - `pickedUpDate` (conditional) — required if `fillStatus = PICKED_UP`.
    - `cancellationReason` (conditional) — required if `fillStatus = CANCELLED | REJECTED`.

  - **Idempotency**:
    - The endpoint must be **idempotent**: duplicate callbacks with the same `networkTransactionId` and `fillStatus` must be acknowledged (HTTP 200) without creating duplicate records or duplicate prescription status transitions.
    - If a callback arrives with a `fillStatus` that would be a regression or an invalid transition (see state machine below), the system must reject it with HTTP 409 and log the anomaly.

  - **Fill-Status Valid State Machine**:
    > This table is the authoritative specification for which `fillStatus` transitions are permitted. The implementation (`EPrescribingService.VALID_FILL_TRANSITIONS`) must enforce exactly this table. Any transition **not listed as allowed** is a regression and must be rejected with HTTP 409.

    | Current status | Permitted next statuses |
    |---|---|
    | `PENDING` | `IN_PROGRESS`, `FILLED`, `PARTIALLY_FILLED`, `ON_HOLD`, `OUT_OF_STOCK`, `CANCELLED`, `REJECTED`, `EXPIRED` |
    | `IN_PROGRESS` | `FILLED`, `PARTIALLY_FILLED`, `ON_HOLD`, `OUT_OF_STOCK`, `CANCELLED`, `REJECTED`, `EXPIRED` |
    | `ON_HOLD` | `IN_PROGRESS`, `FILLED`, `PARTIALLY_FILLED`, `OUT_OF_STOCK`, `CANCELLED`, `REJECTED`, `EXPIRED` |
    | `OUT_OF_STOCK` | `IN_PROGRESS`, `FILLED`, `PARTIALLY_FILLED`, `CANCELLED`, `EXPIRED` |
    | `PARTIALLY_FILLED` | `FILLED`, `PICKED_UP`, `CANCELLED`, `REJECTED` |
    | `FILLED` | `PICKED_UP` |
    | `PICKED_UP` | *(terminal — no further transitions)* |
    | `CANCELLED` | *(terminal — no further transitions)* |
    | `REJECTED` | *(terminal — no further transitions)* |
    | `EXPIRED` | *(terminal — no further transitions)* |

    - **Rationale for key rules**:
      - `FILLED → PICKED_UP` only: once a prescription is filled it cannot regress to `PENDING`, `IN_PROGRESS`, etc.
      - `PARTIALLY_FILLED → FILLED` is valid: the pharmacy completes filling after a partial fill.
      - `PARTIALLY_FILLED → CANCELLED` is valid: pharmacy cancels a partial fill.
      - `REJECTED → CANCELLED` is **not valid** (both are terminal); `CANCELLED → REJECTED` is **not valid** (same reason).
      - `IN_PROGRESS → PARTIALLY_FILLED → FILLED` is the normal complete-fill path.
      - `ON_HOLD → OUT_OF_STOCK` is **not valid** — `ON_HOLD` must transition through `IN_PROGRESS` before going `OUT_OF_STOCK`.
    - The `fillStatus` values in this table are the implementation enum; the webhook API accepts them as the `fillStatus` field values in the request body. Any value not in the enum must return HTTP 400.

  - **Response**:
    - HTTP 200 with a JSON body containing the updated `transmissionId` and `prescriptionStatus` on success.
    - HTTP 400 for malformed payloads (with field-level error details).
    - HTTP 401 for invalid/missing signature.
    - HTTP 404 if `networkTransactionId` is not found.
    - HTTP 409 for status regression attempts.
    - No sensitive PHI shall be included in error responses.

  - **Notification**:
    - On receipt of `fillStatus = FILLED`, the system shall:
      1. Update `ehr.prescriptions.prescription_status` to `FILLED` and set `filled_date`.
      2. Publish a `prescription.filled` domain event for downstream consumers (e.g., medication list update, billing).
      3. Optionally notify the prescribing provider via in-app notification (configurable).
    - On receipt of `CANCELLED` or `REJECTED` from pharmacy, notify the prescribing provider with the cancellation reason so they can re-prescribe if needed.

  - **Audit**:
    - Every inbound callback (success or failure) must be logged in `ehr.prescription_transmissions` with `fill_status`, `fill_status_date`, caller IP, and HMAC validation result.

  - **Documentation**:
    - The webhook contract (endpoint URL, authentication, payload schema, response codes) must be published as part of the system's integration API documentation and versioned alongside the API. Breaking changes require a new endpoint version.

##### 3.2.3.6 Transmission Error Handling

- **FR-P3.12**: System shall handle transmission errors and failures:
  - **Error Types**:
    - Network connectivity errors
    - Pharmacy not available errors
    - Invalid pharmacy information errors
    - Message format errors
    - Data validation errors
    - Timeout errors
    - Authentication/authorization errors
    - System errors
  
  - **Error Handling**:
    - Detect transmission errors
    - Display error messages to provider
    - Log error details
    - Automatic retry (configurable number of attempts)
    - Manual retry option
    - Alternative transmission method (e.g., fax if electronic fails)
    - Error notification to provider
    - Error resolution guidance
  
  - **Error Recovery**:
    - Retry failed transmissions
    - Use alternative transmission method
    - Correct errors and retransmit
    - Cancel failed transmission
    - Generate error report

- **FR-P3.13**: System shall support transmission retry logic:
  - **Retry Configuration**:
    - Number of retry attempts (configurable)
    - Retry interval (configurable)
    - Retry conditions (which errors trigger retry)
    - Maximum retry time limit
    - Exponential backoff (if applicable)
  
  - **Retry Process**:
    - Automatic retry on transient errors
    - Manual retry option
    - Retry status tracking
    - Retry success/failure notification
    - Final failure handling after max retries

##### 3.2.3.7 Transmission Security and Compliance

- **FR-P3.14**: System shall ensure secure prescription transmission:
  - **Transmission Security**:
    - Encrypted transmission (TLS 1.2 or higher)
    - Secure authentication with e-prescribing network
    - Secure authentication with pharmacies
    - Message integrity verification
    - Non-repudiation (prescription cannot be denied)
    - Audit trail of all transmissions
  
  - **Data Security**:
    - PHI encrypted in transit
    - Secure storage of transmission logs
    - Access controls for transmission functions
    - Secure handling of prescription data
    - Compliance with HIPAA security requirements

- **FR-P3.15**: System shall comply with e-prescribing regulations:
  - **Regulatory Compliance**:
    - Compliance with DEA e-prescribing regulations (for controlled substances)
    - Compliance with state e-prescribing regulations
    - Compliance with federal e-prescribing requirements
    - Support for e-prescribing mandates (if applicable)
    - Compliance with NCPDP SCRIPT standard requirements
    - Compliance with pharmacy board requirements
  
  - **Controlled Substance E-Prescribing**:
    - Enhanced security for controlled substance prescriptions
    - DEA number validation
    - Two-factor authentication (if required)
    - Additional audit requirements
    - State-specific controlled substance requirements

##### 3.2.3.8 Transmission Reporting and Analytics

- **FR-P3.16**: System shall provide transmission reporting and analytics:
  - **Transmission Reports**:
    - Transmission success rate
    - Transmission failure rate
    - Transmission by method (electronic, fax, print)
    - Transmission by pharmacy
    - Transmission by provider
    - Transmission timing and performance
    - Error reports
  
  - **Analytics**:
    - Transmission trend analysis
    - Pharmacy network utilization
    - Transmission method utilization
    - Error pattern analysis
    - Performance metrics
    - Compliance metrics
  
  - **Quality Metrics**:
    - E-prescribing adoption rate
    - Electronic transmission rate
    - Transmission success rate
    - Average transmission time
    - Error resolution time

#### 3.2.4 Prescription Management

##### 3.2.4.1 Prescription Status Tracking

- **FR-P4.1**: System shall maintain comprehensive prescription status tracking:
  - **Status Types**:
    - **Draft**: Prescription created but not yet signed or sent
      - Prescription can be edited
      - Prescription can be deleted
      - Prescription not visible to pharmacy
    - **Signed**: Prescription signed by provider but not yet sent
      - Prescription cannot be edited (only cancelled or replaced)
      - Prescription ready for transmission
    - **Pending**: Prescription queued for transmission
      - Prescription in transmission queue
      - Awaiting transmission
    - **Transmitting**: Prescription being transmitted
      - Transmission in progress
      - Awaiting confirmation
    - **Sent**: Prescription successfully transmitted to pharmacy
      - Transmission confirmed
      - Prescription received by pharmacy
      - Prescription visible to pharmacy
    - **Received**: Prescription received and acknowledged by pharmacy (if confirmation available)
      - Pharmacy has received prescription
      - Pharmacy processing prescription
    - **Filled**: Prescription filled by pharmacy (if status update received)
      - Medication dispensed
      - Fill date recorded
      - Quantity dispensed recorded
    - **Partially Filled**: Partial quantity dispensed (if status update received)
      - Some quantity dispensed
      - Remaining quantity to be filled
      - Partial fill date recorded
    - **Cancelled**: Prescription cancelled by provider
      - Prescription cancelled before or after transmission
      - Cancellation reason documented
      - Cancellation date recorded
    - **Expired**: Prescription past expiration date
      - Prescription no longer valid
      - Cannot be filled
      - Expiration date recorded
    - **Refilled**: Refill request processed and filled
      - Refill authorized and filled
      - Refill date recorded
      - Refill count updated
    - **Replaced**: Prescription replaced by new prescription
      - Original prescription replaced
      - Replacement prescription linked
      - Replacement date recorded
    - **On Hold**: Prescription temporarily on hold
      - Prescription held by provider or pharmacy
      - Hold reason documented
      - Hold date recorded
  
  - **Status Transitions**:
    - Track all status changes
    - Record status change timestamps
    - Record status change user (if applicable)
    - Record status change reason (if applicable)
    - Maintain status history
    - Prevent invalid status transitions
    - Support status rollback (with authorization)

- **FR-P4.2**: System shall track prescription lifecycle information:
  - **Prescription Dates**:
    - Prescription creation date
    - Prescription signing date
    - Prescription transmission date
    - Prescription received date (if available)
    - Prescription fill date (if available)
    - Prescription expiration date
    - Prescription cancellation date (if applicable)
  
  - **Prescription Details**:
    - Prescription number (unique identifier)
    - Original prescription number (if replaced)
    - Prescription version (if modified)
    - Prescription source (new, copy, template, etc.)
    - Prescription priority (if applicable)
  
  - **Prescription Relationships**:
    - Link to patient
    - Link to prescribing provider
    - Link to pharmacy
    - Link to original prescription (if replacement)
    - Link to related prescriptions (if applicable)
    - Link to diagnosis/problem (if applicable)
    - Link to encounter (if applicable)

##### 3.2.4.2 Prescription Display and Viewing

- **FR-P4.3**: System shall provide comprehensive prescription display:
  - **Prescription List View**:
    - Display all prescriptions in chronological order
    - Display prescriptions by status
    - Display prescriptions by medication
    - Display prescriptions by provider
    - Display prescriptions by pharmacy
    - Filter prescriptions by date range
    - Filter prescriptions by status
    - Filter prescriptions by medication type
    - Search prescriptions by medication name
    - Search prescriptions by prescription number
  
  - **Prescription Information Displayed**:
    - Prescription number
    - Prescription date
    - Medication name (generic and brand)
    - Dosage strength and form
    - Quantity
    - Frequency/instructions
    - Route of administration
    - Prescribing provider name
    - Pharmacy name and location
    - Prescription status
    - Fill status and dates
    - Number of refills authorized
    - Remaining refills
    - Expiration date
    - Special instructions
  
  - **Prescription Detail View**:
    - Complete prescription information
    - All prescription details
    - Prescription history
    - Status history
    - Fill history
    - Refill history
    - Related prescriptions
    - Linked diagnoses/problems
    - Prescription notes/comments

- **FR-P4.4**: System shall support prescription organization and filtering:
  - **Organization Options**:
    - Sort by date (newest first or oldest first)
    - Sort by medication name
    - Sort by status
    - Sort by provider
    - Sort by pharmacy
    - Group by status
    - Group by medication
    - Group by provider
    - Group by pharmacy
  
  - **Filter Options**:
    - Filter by status (Active, Filled, Cancelled, Expired, etc.)
    - Filter by date range
    - Filter by medication
    - Filter by provider
    - Filter by pharmacy
    - Filter by medication type
    - Filter by controlled substance status
    - Multiple filter combinations
    - Save filter configurations

##### 3.2.4.3 Prescription Modification and Cancellation

- **FR-P4.5**: System shall support prescription modification:
  - **Modification Capabilities**:
    - Modify draft prescriptions (full editing)
    - Modify signed but not sent prescriptions (limited - may require cancellation and recreation)
    - Cannot modify sent prescriptions (must cancel and replace)
    - Modification history tracking
    - Modification audit trail
  
  - **Modification Process**:
    - Access prescription for modification
    - Make necessary changes
    - Validate modified prescription
    - Re-check interactions and allergies
    - Re-sign modified prescription (if required)
    - Update prescription version
    - Maintain original prescription information (for audit)
  
  - **Modification Restrictions**:
    - Cannot modify after transmission (must cancel and replace)
    - Cannot modify filled prescriptions
    - Cannot modify expired prescriptions
    - Modification may require authorization (for certain fields)
    - Modification audit trail required

- **FR-P4.6**: System shall support prescription cancellation:
  - **Cancellation Capabilities**:
    - Cancel draft prescriptions
    - Cancel signed but not sent prescriptions
    - Cancel sent prescriptions (transmit cancellation to pharmacy)
    - Cancel filled prescriptions (with restrictions and documentation)
    - Cancel expired prescriptions (administrative)
  
  - **Cancellation Process**:
    - Access prescription for cancellation
    - Select cancellation reason (required):
      - Medication error
      - Patient request
      - Provider decision
      - Duplicate prescription
      - Changed medication
      - Changed dosage
      - Patient adverse reaction
      - Other (with specification)
    - Enter cancellation notes (optional)
    - Confirm cancellation
    - Transmit cancellation to pharmacy (if prescription was sent)
    - Update prescription status
    - Record cancellation date and user
    - Maintain prescription record (soft delete)
  
  - **Cancellation Restrictions**:
    - Cannot cancel prescriptions that are already expired (administrative only)
    - Cancellation of filled prescriptions may require additional documentation
    - Cancellation audit trail required
    - Cancellation reason required

- **FR-P4.7**: System shall support prescription replacement:
  - **Replacement Capabilities**:
    - Replace sent prescriptions with new prescription
    - Replace filled prescriptions (with restrictions)
    - Link replacement prescription to original
    - Maintain original prescription record
    - Replacement reason documentation
  
  - **Replacement Process**:
    - Access original prescription
    - Create new prescription (with modifications)
    - Link new prescription to original
    - Cancel original prescription (if not yet filled)
    - Transmit new prescription
    - Document replacement reason
    - Update prescription statuses

##### 3.2.4.4 Prescription History and Audit Trail

- **FR-P4.8**: System shall maintain comprehensive prescription history:
  - **History Tracking**:
    - Complete prescription lifecycle history
    - All status changes
    - All modifications
    - All cancellations
    - All replacements
    - All fills and refills
    - All transmissions
    - All acknowledgments
  
  - **History Information**:
    - Action type (Created, Modified, Sent, Filled, Cancelled, etc.)
    - Action date and time
    - Action user
    - Action details
    - Previous values (for modifications)
    - New values (for modifications)
    - Reason (for cancellations, modifications)
    - Related prescriptions (for replacements)
  
  - **History Display**:
    - Display prescription history chronologically
    - Display history by action type
    - Display history with details
    - Export prescription history
    - Print prescription history

- **FR-P4.9**: System shall maintain comprehensive audit trail:
  - **Audit Trail Requirements**:
    - All prescription actions logged
    - User identification for all actions
    - Timestamp for all actions
    - IP address or location (if available)
    - Action details
    - Audit trail cannot be modified or deleted
    - Audit trail retained per regulatory requirements
  
  - **Audited Actions**:
    - Prescription creation
    - Prescription modification
    - Prescription signing
    - Prescription transmission
    - Prescription cancellation
    - Prescription replacement
    - Prescription viewing (if required)
    - Prescription printing/exporting
    - Alert overrides
    - Refill approvals/denials

##### 3.2.4.5 Prescription Expiration and Renewal

- **FR-P4.10**: System shall manage prescription expiration:
  - **Expiration Rules**:
    - Prescriptions expire based on state regulations (typically 6-12 months from date)
    - Expiration date calculated automatically
    - Expiration date displayed on prescription
    - Expiration alerts (if configured)
    - Expired prescriptions cannot be filled
    - Expired prescriptions marked as "Expired" status
  
  - **Expiration Management**:
    - Identify expired prescriptions
    - Display expiration status
    - Alert for prescriptions expiring soon (if configured)
    - Support for prescription renewal (new prescription required)
    - Expiration date override (with authorization and reason, if applicable)

- **FR-P4.11**: System shall support prescription renewal:
  - **Renewal Process**:
    - Create new prescription to replace expiring or expired prescription
    - Copy information from original prescription
    - Modify as needed (dosage, frequency, etc.)
    - Link renewal prescription to original
    - Transmit renewal prescription
    - Document renewal reason
  
  - **Renewal Features**:
    - Quick renewal from expiring prescription
    - Renewal templates
    - Renewal reminders (if configured)
    - Renewal history tracking

##### 3.2.4.6 Prescription Reporting and Analytics

- **FR-P4.12**: System shall provide comprehensive prescription reporting:
  - **Patient-Level Reports**:
    - Complete prescription history for patient
    - Active prescriptions
    - Filled prescriptions
    - Cancelled prescriptions
    - Expired prescriptions
    - Prescriptions by medication
    - Prescriptions by provider
    - Prescriptions by pharmacy
  
  - **Provider-Level Reports**:
    - Prescriptions by provider
    - Prescription volume
    - Prescription types
    - Prescription status distribution
    - Cancellation rate
    - Modification rate
  
  - **Pharmacy-Level Reports**:
    - Prescriptions by pharmacy
    - Prescription volume
    - Fill rate
    - Average fill time
    - Prescription status distribution
  
  - **Clinical Reports**:
    - Prescriptions by medication class
    - Prescriptions by indication
    - Prescription patterns
    - Medication utilization
    - Prescription adherence (if fill data available)
  
  - **Quality Reports**:
    - E-prescribing adoption rate
    - Prescription error rate
    - Prescription cancellation rate
    - Prescription modification rate
    - Prescription completion rate
    - Quality metrics
  
  - **Regulatory Reports**:
    - Controlled substance prescriptions
    - Prescription compliance
    - Regulatory requirement compliance
  
  - **Report Features**:
    - Reports exportable in multiple formats (PDF, Excel, CSV)
    - Reports support filtering, sorting, and customization
    - Scheduled reports (if applicable)
    - Report templates

##### 3.2.4.7 Prescription Integration

- **FR-P4.13**: System shall integrate prescription management with other system components:
  - **Patient Record Integration**:
    - Prescriptions appear in patient medication list
    - Prescriptions linked to patient record
    - Prescription information in patient summary
    - Prescription history in patient timeline
  
  - **Medication List Integration**:
    - Active prescriptions update medication list
    - Cancelled prescriptions remove from medication list
    - Prescription changes update medication list
    - Medication list reflects prescription status
  
  - **Encounter Integration**:
    - Prescriptions linked to encounters
    - Prescriptions displayed in encounter view
    - Prescription creation during encounter
    - Encounter-based prescription reporting
  
  - **Problem/Diagnosis Integration**:
    - Prescriptions linked to problems/diagnoses
    - Prescriptions displayed with problems
    - Problem-based prescription reporting
  
  - **Clinical Notes Integration**:
    - Prescriptions mentioned in clinical notes
    - Prescriptions linked to notes
    - Prescription information in note templates

##### 3.2.4.8 Error Handling and Recovery

- **FR-P4.14**: System shall implement comprehensive error handling for prescription management:
  - **Status Update Error Handling**:
    - **Status Update Failures**:
      - Handle failures when updating prescription status
      - Handle concurrent status update conflicts
      - Display error: "Unable to update prescription status. Please try again."
      - Retry status updates automatically (up to 3 attempts)
      - Queue status updates for retry if persistent failure
      - Log status update errors with prescription ID and new status
    
    - **Status Synchronization Errors**:
      - Handle pharmacy status update failures
      - Handle status synchronization conflicts
      - Display warning: "Prescription status may be out of sync. Refreshing..."
      - Auto-refresh status from pharmacy if available
      - Log synchronization errors
    
    - **Invalid Status Transitions**:
      - Detect invalid status transitions (e.g., cannot go from "Filled" to "Pending")
      - Display error: "Invalid status transition. Cannot change from [current] to [new]."
      - Prevent invalid status changes
      - Log invalid transition attempts
  
  - **Prescription Retrieval Error Handling**:
    - **Retrieval Failures**:
      - Handle prescription not found errors
      - Handle prescription access denied errors
      - Handle database query failures
      - Display error: "Unable to retrieve prescription. Please try again."
      - Provide retry mechanism
      - Log retrieval errors
    
    - **Large Result Set Errors**:
      - Handle timeouts when retrieving large prescription lists
      - Handle memory errors with large result sets
      - Implement pagination for large lists
      - Display error: "Too many prescriptions to display. Please use filters."
      - Log performance issues
  
  - **Modification Error Handling**:
    - **Modification Failures**:
      - Handle modification save failures
      - Handle concurrent modification conflicts
      - Display error: "Unable to save modifications. Another user may have modified this prescription."
      - Provide conflict resolution interface
      - Show conflicting changes side-by-side
      - Allow user to choose which version to keep
      - Log modification conflicts
    
    - **Invalid Modification Errors**:
      - Handle attempts to modify filled prescriptions
      - Handle attempts to modify expired prescriptions
      - Handle attempts to modify cancelled prescriptions
      - Display error: "Cannot modify prescription in [status] status."
      - Prevent invalid modifications
      - Log invalid modification attempts
  
  - **Cancellation Error Handling**:
    - **Cancellation Failures**:
      - Handle cancellation save failures
      - Handle cancellation transmission failures (if already sent)
      - Display error: "Unable to cancel prescription. Please try again."
      - Retry cancellation automatically
      - Queue cancellation for retry if persistent failure
      - Log cancellation errors
    
    - **Invalid Cancellation Errors**:
      - Handle attempts to cancel filled prescriptions
      - Handle attempts to cancel expired prescriptions
      - Display error: "Cannot cancel prescription in [status] status."
      - Prevent invalid cancellations
      - Log invalid cancellation attempts
  
  - **History Retrieval Error Handling**:
    - **History Query Failures**:
      - Handle history database query failures
      - Handle history query timeouts
      - Display error: "Unable to retrieve prescription history. Please try again."
      - Provide retry mechanism
      - Cache recent history if available
      - Log history query errors
    
    - **Audit Trail Errors**:
      - Handle audit trail retrieval failures
      - Handle audit trail corruption
      - Display warning: "Some audit trail information may be unavailable."
      - Log audit trail errors
  
  - **Expiration Processing Error Handling**:
    - **Expiration Check Failures**:
      - Handle expiration check process failures
      - Handle expiration status update failures
      - Retry expiration checks automatically
      - Queue expiration checks for retry
      - Log expiration processing errors
    
    - **Expiration Notification Errors**:
      - Handle expiration notification failures
      - Handle notification delivery failures
      - Queue notifications for retry
      - Log notification errors
  
  - **Reporting Error Handling**:
    - **Report Generation Failures**:
      - Handle report query failures
      - Handle report generation timeouts
      - Handle report format errors
      - Display error: "Unable to generate report. Please try again or contact support."
      - Provide retry mechanism
      - Log report generation errors
    
    - **Large Report Errors**:
      - Handle memory errors with large reports
      - Handle timeout errors with large reports
      - Implement report pagination or chunking
      - Display error: "Report too large. Please use date filters to reduce size."
      - Log large report errors
  
  - **Integration Error Handling**:
    - **Patient Record Integration Errors**:
      - Handle medication list update failures
      - Handle medication list synchronization conflicts
      - Display warning: "Medication list may be out of sync."
      - Queue synchronization for retry
      - Log integration errors
    
    - **Pharmacy Integration Errors**:
      - Handle pharmacy status update failures
      - Handle pharmacy communication failures
      - Display warning: "Pharmacy status updates may be delayed."
      - Queue status updates for retry
      - Log pharmacy integration errors
  
  - **System Error Handling**:
    - **Database Errors**:
      - Handle database connection failures
      - Handle database timeout errors
      - Handle database constraint violations
      - Display user-friendly error messages
      - Retry database operations automatically
      - Log database errors with context
    
    - **Network Errors**:
      - Handle network connectivity failures
      - Handle network timeout errors
      - Display error: "Network connection lost. Please check your connection."
      - Retry network operations automatically
      - Queue operations for retry when connection restored
      - Log network errors
    
    - **Application Errors**:
      - Handle application crashes during prescription management
      - Recover prescription management state after crash
      - Display recovery message
      - Log application errors with stack traces
  
  - **Security and Authorization Error Handling**:
    - **Access Denied Errors**:
      - Handle insufficient permissions for prescription access
      - Handle insufficient permissions for prescription modification
      - Display error: "You do not have permission to [action] this prescription."
      - Log authorization failures
      - Prevent unauthorized actions
    
    - **Authentication Errors**:
      - Handle session expiration during prescription management
      - Auto-save state before session expiration
      - Prompt user to re-authenticate
      - Restore state after re-authentication
      - Log authentication errors
  
  - **Recovery Mechanisms**:
    - **Auto-Save and Recovery**:
      - Auto-save prescription modifications every 30 seconds
      - Auto-save before navigation away
      - Restore modifications after error recovery
      - Display recovery notification
      - Allow user to discard recovered changes
    
    - **Retry Logic**:
      - Automatic retry for transient errors (up to 3 attempts)
      - Exponential backoff for retries
      - Manual retry option for all errors
      - Display retry status to user
    
    - **Error Recovery Workflow**:
      - Provide "Retry" button for transient errors
      - Provide "Cancel" option to discard changes
      - Maintain prescription state during error recovery
      - Log all recovery attempts
  
  - **Error Logging and Reporting**:
    - Log all errors with full context:
      - Error type and message
      - Prescription ID
      - User ID and role
      - Action attempted
      - Timestamp
      - System state
      - Stack trace (for application errors)
    - Generate error reports for system administrators
    - Alert administrators for critical errors
    - Track error trends and patterns
    - Monitor prescription management performance

#### 3.2.5 Prescription Refills

##### 3.2.5.1 Refill Request Sources

- **FR-P5.1**: System shall support refill requests from multiple sources:
  - **Pharmacy-Initiated Refill Requests**:
    - Electronic refill requests from pharmacies via e-prescribing network
    - Refill requests via NCPDP SCRIPT RxRenewalRequest messages
    - Refill requests via direct pharmacy integration
    - Refill requests via fax (if applicable)
    - Refill requests via phone (manual entry by staff)
    - Automatic routing of refill requests to prescribing provider
    - Refill request notifications to provider
  
  - **Patient-Initiated Refill Requests**:
    - Refill requests via patient portal (if available)
    - Refill requests via phone (manual entry by staff)
    - Refill requests via secure messaging (if available)
    - Refill requests via mobile app (if available)
    - Patient refill request routing to provider
    - Patient refill request notifications
  
  - **Provider-Initiated Refills**:
    - Provider creates refill without request
    - Provider proactively refills medication
    - Provider refills during patient encounter
    - Provider refills based on medication review
  
  - **Refill Request Information**:
    - Original prescription number
    - Medication name and dosage
    - Patient name and information
    - Pharmacy name and information
    - Request date and time
    - Request source (Pharmacy, Patient, Provider)
    - Number of refills requested
    - Remaining refills on original prescription
    - Last fill date (if available)
    - Days since last fill (if available)

##### 3.2.5.2 Refill Request Management

- **FR-P5.2**: System shall provide refill request workflow and management:
  - **Refill Request Queue**:
    - Display all pending refill requests
    - Organize requests by priority (if applicable)
    - Organize requests by date
    - Organize requests by patient
    - Organize requests by medication
    - Filter requests by status
    - Search requests by patient name or medication
    - Refill request notifications and reminders
  
  - **Refill Request Display**:
    - Original prescription information
    - Patient information
    - Medication information
    - Refill history
    - Remaining refills
    - Last fill date
    - Patient's current status (if available)
    - Relevant clinical information (recent lab results, encounters, etc.)
    - Request source and date
  
  - **Refill Request Status**:
    - **Pending**: Awaiting provider review
    - **Approved**: Refill approved, processing
    - **Denied**: Refill denied
    - **Modified**: Refill modified
    - **Completed**: Refill processed and sent
    - **Cancelled**: Refill request cancelled

##### 3.2.5.3 Refill Approval Process

- **FR-P5.3**: System shall support comprehensive refill approval:
  - **Approval Options**:
    - **Approve as Requested**:
      - Approve refill with same quantity and instructions
      - Approve refill with remaining refills
      - Approve refill and send to pharmacy
      - Update prescription refill count
    - **Approve with Modifications**:
      - Approve refill but modify quantity
      - Approve refill but add additional refills
      - Approve refill but modify instructions
      - Approve refill but change pharmacy
      - Create modified refill prescription
    - **Approve with Conditions**:
      - Approve refill with requirements (e.g., lab test, office visit)
      - Approve refill with time limits
      - Approve refill with monitoring requirements
  
  - **Approval Process**:
    - Review refill request
    - Review patient's current status
    - Review medication history
    - Review relevant clinical information
    - Make approval decision
    - Document approval decision
    - Process approved refill
    - Transmit refill to pharmacy
    - Update prescription status
    - Notify pharmacy of approval
    - Notify patient of approval (if applicable)

- **FR-P5.4**: System shall support refill approval documentation:
  - **Approval Information**:
    - Approval date and time
    - Approving provider
    - Approval decision (Approved, Denied, Modified)
    - Approval modifications (if applicable)
    - Approval conditions (if applicable)
    - Approval notes/comments
    - Clinical rationale (if documented)
  
  - **Approval Audit Trail**:
    - All approval actions logged
    - Approval timestamps
    - Approval user identification
    - Approval details
    - Approval included in prescription history

##### 3.2.5.4 Refill Denial Process

- **FR-P5.5**: System shall support refill denial with proper documentation:
  - **Denial Reasons** (Required):
    - Medication no longer needed
    - Patient needs office visit
    - Patient needs lab test/monitoring
    - Medication change needed
    - Patient non-adherence
    - Safety concerns
    - Insurance/coverage issues
    - Other (with specification)
  
  - **Denial Process**:
    - Review refill request
    - Select denial reason
    - Enter denial notes/comments (optional but recommended)
    - Confirm denial
    - Transmit denial to pharmacy (if request was electronic)
    - Notify pharmacy of denial with reason
    - Notify patient of denial (if applicable)
    - Update refill request status
    - Document denial in prescription history
  
  - **Denial Information**:
    - Denial date and time
    - Denying provider
    - Denial reason
    - Denial notes/comments
    - Denial communicated to pharmacy
    - Denial communicated to patient

##### 3.2.5.5 Refill Modification

- **FR-P5.6**: System shall support refill modification:
  - **Modification Types**:
    - Modify quantity to dispense
    - Modify number of refills authorized
    - Modify special instructions
    - Modify pharmacy (if applicable)
    - Modify timing/frequency (if creating new prescription)
    - Modify dosage (if creating new prescription)
  
  - **Modification Process**:
    - Access refill request
    - Select modification option
    - Make modifications
    - Review modified refill
    - Approve modified refill
    - Transmit modified refill to pharmacy
    - Update prescription with modifications
    - Document modifications
  
  - **Modification Documentation**:
    - Original refill request
    - Modifications made
    - Modification reason
    - Modified refill details
    - Modification date and provider

##### 3.2.5.6 Auto-Approval Rules

- **FR-P5.7**: System shall support auto-approval rules for refills:
  - **Auto-Approval Configuration**:
    - Create auto-approval rules
    - Define rule criteria:
      - Specific medications
      - Medication classes
      - Patient conditions
      - Time since last fill
      - Number of previous refills
      - Provider preferences
    - Define rule conditions:
      - Maximum number of auto-approved refills
      - Time limits for auto-approval
      - Required monitoring (lab tests, etc.)
      - Office visit requirements
    - Enable/disable auto-approval rules
    - Rule priority (if multiple rules apply)
  
  - **Auto-Approval Process**:
    - Evaluate refill request against auto-approval rules
    - Auto-approve if criteria met
    - Auto-approve with conditions if applicable
    - Process auto-approved refill
    - Transmit auto-approved refill
    - Notify provider of auto-approval (optional)
    - Document auto-approval
  
  - **Auto-Approval Management**:
    - Review auto-approval rules
    - Modify auto-approval rules
    - Disable auto-approval rules
    - Review auto-approved refills
    - Override auto-approval (if needed)
    - Auto-approval reporting

##### 3.2.5.7 Refill History and Tracking

- **FR-P5.8**: System shall track comprehensive refill history:
  - **Refill History Information**:
    - All refill requests (approved, denied, modified)
    - Refill request dates
    - Refill request sources
    - Refill approval/denial dates
    - Refill approval/denial providers
    - Refill approval/denial decisions
    - Refill modifications
    - Refill transmission dates
    - Refill fill dates (if available from pharmacy)
    - Refill quantities dispensed (if available)
  
  - **Refill Count Tracking**:
    - Original number of refills authorized
    - Number of refills used
    - Number of refills remaining
    - Refill count updated with each fill
    - Refill count displayed prominently
    - Refill expiration tracking
  
  - **Refill History Display**:
    - Display refill history chronologically
    - Display refill history by status
    - Display refill history with details
    - Display refill count and remaining refills
    - Export refill history
    - Print refill history

- **FR-P5.9**: System shall track refill patterns and adherence:
  - **Refill Pattern Analysis**:
    - Time between refills
    - Refill frequency
    - Refill adherence (if fill data available)
    - Early refills (potential abuse or stockpiling)
    - Late refills (potential non-adherence)
    - Missed refills
    - Refill completion rate
  
  - **Adherence Indicators**:
    - Days supply vs. days between refills
    - Refill timing patterns
    - Refill gaps
    - Adherence alerts (if applicable)
    - Adherence reporting

##### 3.2.5.8 Refill Communication and Notifications

- **FR-P5.10**: System shall support refill communication:
  - **Pharmacy Communication**:
    - Transmit refill approvals to pharmacy
    - Transmit refill denials to pharmacy
    - Transmit refill modifications to pharmacy
    - Receive refill requests from pharmacy
    - Receive refill fill confirmations from pharmacy (if available)
    - Pharmacy notification preferences
  
  - **Patient Communication**:
    - Notify patient of refill approval (if applicable)
    - Notify patient of refill denial (if applicable)
    - Notify patient of refill modifications (if applicable)
    - Patient communication preferences
    - Communication method (email, text, portal, phone)
  
  - **Provider Communication**:
    - Notify provider of refill requests
    - Notify provider of refill approvals (if auto-approved)
    - Notify provider of refill denials (if delegated)
    - Refill request reminders
    - Refill queue notifications

##### 3.2.5.9 Refill Workflow and Efficiency

- **FR-P5.11**: System shall support efficient refill workflows:
  - **Bulk Refill Processing**:
    - Process multiple refill requests at once
    - Bulk approve refills
    - Bulk deny refills
    - Bulk modify refills
    - Bulk review and decision
  
  - **Refill Templates**:
    - Refill approval templates
    - Refill denial templates
    - Refill modification templates
    - Quick refill actions
  
  - **Refill Shortcuts**:
    - Quick approve common refills
    - Quick deny with common reasons
    - One-click refill actions (with confirmation)
    - Refill favorites/preferences

##### 3.2.5.10 Refill Reporting and Analytics

- **FR-P5.12**: System shall provide refill reporting and analytics:
  - **Refill Reports**:
    - Refill requests by source
    - Refill approval rate
    - Refill denial rate
    - Refill modification rate
    - Refill processing time
    - Refill volume by provider
    - Refill volume by medication
    - Refill volume by pharmacy
  
  - **Quality Reports**:
    - Refill request response time
    - Refill approval appropriateness
    - Refill denial appropriateness
    - Auto-approval utilization
    - Refill adherence metrics (if fill data available)
  
  - **Analytics**:
    - Refill pattern analysis
    - Refill trend analysis
    - Medication adherence analysis
    - Provider refill patterns
    - Pharmacy refill patterns
    - Refill efficiency metrics

##### 3.2.5.11 Error Handling and Recovery

- **FR-P5.13**: System shall implement comprehensive error handling for prescription refills:
  - **Refill Request Error Handling**:
    - **Request Processing Failures**:
      - Handle refill request processing failures
      - Handle request validation errors
      - Display error: "Unable to process refill request. Please try again."
      - Retry request processing automatically
      - Queue requests for retry if persistent failure
      - Log request processing errors
    
    - **Request Source Errors**:
      - Handle pharmacy refill request failures
      - Handle patient portal refill request failures
      - Handle phone/voicemail refill request failures
      - Display error: "Unable to receive refill request from [source]."
      - Queue requests for retry
      - Log source errors
    
    - **Invalid Request Errors**:
      - Handle requests for non-refillable prescriptions (Schedule II)
      - Handle requests for expired prescriptions
      - Handle requests for cancelled prescriptions
      - Handle requests exceeding refill limit
      - Display error: "Refill request invalid: [reason]."
      - Prevent invalid requests
      - Log invalid request attempts
  
  - **Refill Approval Error Handling**:
    - **Approval Processing Failures**:
      - Handle approval save failures
      - Handle approval transmission failures
      - Display error: "Unable to approve refill. Please try again."
      - Retry approval automatically
      - Queue approval for retry if persistent failure
      - Log approval errors
    
    - **Concurrent Approval Conflicts**:
      - Handle multiple approval attempts
      - Handle approval/denial conflicts
      - Display error: "Refill request already processed."
      - Prevent duplicate approvals
      - Log conflict errors
    
    - **Authorization Errors**:
      - Handle insufficient permissions for approval
      - Handle supervisor approval requirement failures
      - Display error: "You do not have permission to approve this refill."
      - Prevent unauthorized approvals
      - Log authorization errors
  
  - **Refill Denial Error Handling**:
    - **Denial Processing Failures**:
      - Handle denial save failures
      - Handle denial notification failures
      - Display error: "Unable to deny refill. Please try again."
      - Retry denial automatically
      - Queue denial for retry if persistent failure
      - Log denial errors
    
    - **Documentation Errors**:
      - Handle missing denial reason errors
      - Handle denial documentation save failures
      - Require denial reason before processing
      - Display error: "Denial reason required."
      - Log documentation errors
  
  - **Auto-Approval Error Handling**:
    - **Rule Evaluation Failures**:
      - Handle auto-approval rule evaluation errors
      - Handle rule configuration errors
      - Display warning: "Auto-approval rules unavailable. Manual review required."
      - Fall back to manual approval
      - Log rule evaluation errors
    
    - **Auto-Approval Processing Failures**:
      - Handle auto-approval save failures
      - Handle auto-approval notification failures
      - Retry auto-approval automatically
      - Queue for manual review if persistent failure
      - Log auto-approval errors
    
    - **Rule Conflict Errors**:
      - Handle conflicting auto-approval rules
      - Handle rule priority errors
      - Default to manual review if rules conflict
      - Display warning: "Auto-approval rules conflict. Manual review required."
      - Log rule conflicts
  
  - **Refill Modification Error Handling**:
    - **Modification Processing Failures**:
      - Handle modification save failures
      - Handle modification transmission failures
      - Display error: "Unable to modify refill. Please try again."
      - Retry modification automatically
      - Queue modification for retry if persistent failure
      - Log modification errors
    
    - **Invalid Modification Errors**:
      - Handle attempts to modify approved refills
      - Handle attempts to modify filled refills
      - Display error: "Cannot modify refill in [status] status."
      - Prevent invalid modifications
      - Log invalid modification attempts
  
  - **Refill History Error Handling**:
    - **History Retrieval Failures**:
      - Handle history query failures
      - Handle history query timeouts
      - Display error: "Unable to retrieve refill history. Please try again."
      - Provide retry mechanism
      - Cache recent history if available
      - Log history retrieval errors
    
    - **Adherence Calculation Errors**:
      - Handle adherence calculation failures
      - Handle missing pharmacy data for adherence
      - Display warning: "Adherence data may be incomplete."
      - Log adherence calculation errors
  
  - **Communication Error Handling**:
    - **Notification Failures**:
      - Handle refill approval notification failures
      - Handle refill denial notification failures
      - Handle refill status notification failures
      - Queue notifications for retry
      - Log notification errors
    
    - **Pharmacy Communication Failures**:
      - Handle pharmacy notification failures
      - Handle pharmacy status update failures
      - Display warning: "Pharmacy notifications may be delayed."
      - Queue communications for retry
      - Log pharmacy communication errors
    
    - **Patient Communication Failures**:
      - Handle patient notification failures
      - Handle patient portal update failures
      - Queue patient communications for retry
      - Log patient communication errors
  
  - **System Error Handling**:
    - **Database Errors**:
      - Handle database connection failures
      - Handle database timeout errors
      - Handle database constraint violations
      - Display user-friendly error messages
      - Retry database operations automatically
      - Log database errors with context
    
    - **Network Errors**:
      - Handle network connectivity failures
      - Handle network timeout errors
      - Display error: "Network connection lost. Please check your connection."
      - Retry network operations automatically
      - Queue operations for retry when connection restored
      - Log network errors
    
    - **Application Errors**:
      - Handle application crashes during refill processing
      - Recover refill state after crash
      - Display recovery message
      - Log application errors with stack traces
  
  - **Recovery Mechanisms**:
    - **Retry Logic**:
      - Automatic retry for transient errors (up to 3 attempts)
      - Exponential backoff for retries
      - Manual retry option for all errors
      - Display retry status to user
    
    - **Error Recovery Workflow**:
      - Provide "Retry" button for transient errors
      - Provide "Cancel" option to discard changes
      - Maintain refill state during error recovery
      - Log all recovery attempts
  
  - **Error Logging and Reporting**:
    - Log all errors with full context:
      - Error type and message
      - Refill request ID
      - Prescription ID
      - User ID and role
      - Action attempted
      - Timestamp
      - System state
      - Stack trace (for application errors)
    - Generate error reports for system administrators
    - Alert administrators for critical errors
    - Track error trends and patterns
    - Monitor refill processing performance

#### 3.2.6 Prescription Modifications and Cancellations

##### 3.2.6.1 Prescription Modification

- **FR-P6.1**: System shall support prescription modification with appropriate restrictions:
  - **Modification Capabilities by Status**:
    - **Draft Prescriptions**: Full modification allowed
      - Can modify all prescription fields
      - Can change medication, dosage, frequency, quantity, etc.
      - No restrictions on modifications
    - **Signed but Not Sent Prescriptions**: Limited modification allowed
      - May require cancellation and recreation for major changes
      - Minor modifications may be allowed (with re-signing)
      - Modification restrictions based on organization policy
    - **Sent Prescriptions**: Cannot modify directly
      - Must cancel and replace with new prescription
      - Original prescription must be cancelled first
      - New prescription linked to original
    - **Filled Prescriptions**: Cannot modify
      - Prescription already dispensed
      - Must create new prescription if changes needed
    - **Expired Prescriptions**: Cannot modify
      - Prescription expired, create new prescription instead
  
  - **Modifiable Fields**:
    - Medication (if draft)
    - Dosage strength
    - Dosage form
    - Quantity
    - Frequency/schedule
    - Route of administration
    - Special instructions
    - Duration
    - Number of refills
    - Substitution preference
    - Pharmacy (if not yet sent)
    - Start date (if not yet sent)
  
  - **Modification Process**:
    - Access prescription for modification
    - Make necessary changes
    - System validates modified prescription
    - System re-checks interactions and allergies
    - System re-validates dosage appropriateness
    - Provider reviews modifications
    - Provider re-signs prescription (if required)
    - System updates prescription version
    - System maintains original prescription data (for audit)
    - System logs all modifications

- **FR-P6.2**: System shall validate modifications and re-check safety:
  - **Modification Validation**:
    - Validate all modified fields
    - Validate prescription completeness
    - Validate prescription format
    - Validate regulatory requirements
    - Check for new interactions (with modified medication/dosage)
    - Check for new allergies (if medication changed)
    - Check for new contraindications (if medication changed)
    - Validate dosage appropriateness (if dosage changed)
    - Validate quantity and refills (if changed)
  
  - **Safety Re-Checking**:
    - Re-run drug interaction checking
    - Re-run allergy checking
    - Re-run contraindication checking
    - Re-run dosage validation
    - Display any new alerts
    - Require acknowledgment of new alerts

##### 3.2.6.2 Prescription Cancellation

- **FR-P6.3**: System shall support prescription cancellation at all stages:
  - **Cancellation by Status**:
    - **Draft Prescriptions**: Can cancel freely
      - No restrictions
      - No notification required
      - Immediate cancellation
    - **Signed but Not Sent Prescriptions**: Can cancel freely
      - No restrictions
      - No notification required
      - Immediate cancellation
    - **Sent Prescriptions**: Can cancel with notification
      - Must transmit cancellation to pharmacy
      - Pharmacy notified of cancellation
      - Cancellation reason required
      - Cancellation documented
    - **Received Prescriptions**: Can cancel with notification
      - Must transmit cancellation to pharmacy immediately
      - Pharmacy notified urgently
      - Cancellation reason required
      - May require pharmacy confirmation
    - **Filled Prescriptions**: Cancellation restricted
      - Cannot cancel already filled prescriptions
      - Must create new prescription to stop medication
      - May require additional documentation
    - **Expired Prescriptions**: Administrative cancellation
      - Can mark as cancelled administratively
      - No pharmacy notification needed
      - For record-keeping purposes
  
  - **Cancellation Reasons** (Required):
    - Medication error
    - Dosage error
    - Wrong medication
    - Patient request
    - Provider decision
    - Changed medication
    - Changed dosage
    - Changed frequency
    - Patient adverse reaction
    - Drug interaction identified
    - Allergy identified
    - Duplicate prescription
    - Patient no longer needs medication
    - Insurance/coverage issue
    - Other (with specification)
  
  - **Cancellation Process**:
    - Access prescription for cancellation
    - Select cancellation reason (required)
    - Enter cancellation notes/comments (optional but recommended)
    - Confirm cancellation
    - System validates cancellation
    - System transmits cancellation to pharmacy (if prescription was sent)
    - System updates prescription status to "Cancelled"
    - System records cancellation date and time
    - System records cancelling provider
    - System maintains prescription record (soft delete)
    - System logs cancellation in audit trail

- **FR-P6.4**: System shall handle pharmacy notification for cancellations:
  - **Notification Methods**:
    - Electronic cancellation message via e-prescribing network
    - Electronic cancellation via direct pharmacy integration
    - Fax cancellation (if electronic unavailable)
    - Phone notification (for urgent cancellations, if applicable)
  
  - **Cancellation Message**:
    - Original prescription number
    - Cancellation date and time
    - Cancellation reason
    - Cancelling provider information
    - Patient information
    - Medication information
    - Instructions to pharmacy
  
  - **Cancellation Confirmation**:
    - Receive cancellation confirmation from pharmacy (if available)
    - Track cancellation delivery status
    - Alert if cancellation not confirmed (if required)
    - Document cancellation confirmation

##### 3.2.6.3 Prescription Replacement

- **FR-P6.5**: System shall support prescription replacement:
  - **Replacement Scenarios**:
    - Replace sent prescription with corrected prescription
    - Replace sent prescription with modified prescription
    - Replace filled prescription (with restrictions and documentation)
    - Replace expired prescription with renewal
    - Replace prescription due to error
    - Replace prescription due to change in therapy
  
  - **Replacement Process**:
    - Access original prescription
    - Review original prescription details
    - Create new prescription (with modifications as needed)
    - Link new prescription to original
    - Cancel original prescription (if not yet filled)
    - Transmit new prescription to pharmacy
    - Document replacement reason
    - Update prescription statuses
    - Maintain both prescription records
  
  - **Replacement Information**:
    - Original prescription number
    - Replacement prescription number
    - Replacement date and time
    - Replacement reason
    - Replacing provider
    - Changes made (what was different)
    - Link between original and replacement

- **FR-P6.6**: System shall handle replacement for filled prescriptions:
  - **Filled Prescription Replacement**:
    - Cannot cancel filled prescriptions
    - Must create new prescription to replace
    - New prescription may discontinue medication
    - New prescription may change medication
    - New prescription may change dosage
    - Link new prescription to original
    - Document replacement reason
    - May require additional documentation for controlled substances
  
  - **Replacement Restrictions**:
    - Replacement of filled prescriptions requires documentation
    - Replacement of controlled substances may have additional requirements
    - Replacement reason must be documented
    - Replacement must be clinically justified

##### 3.2.6.4 Modification and Cancellation Documentation

- **FR-P6.7**: System shall maintain comprehensive documentation of all modifications and cancellations:
  - **Modification Documentation**:
    - Original prescription data (preserved)
    - Modified prescription data
    - Fields that were changed
    - Previous values
    - New values
    - Modification date and time
    - Modifying provider
    - Modification reason (if provided)
    - Modification version number
    - Modification audit trail
  
  - **Cancellation Documentation**:
    - Original prescription data (preserved)
    - Cancellation date and time
    - Cancelling provider
    - Cancellation reason (required)
    - Cancellation notes/comments
    - Pharmacy notification status
    - Cancellation confirmation (if received)
    - Cancellation audit trail
  
  - **Replacement Documentation**:
    - Original prescription data (preserved)
    - Replacement prescription data
    - Replacement date and time
    - Replacing provider
    - Replacement reason
    - Changes made
    - Link between prescriptions
    - Replacement audit trail

- **FR-P6.8**: System shall maintain audit trail for all modifications and cancellations:
  - **Audit Trail Requirements**:
    - All modification actions logged
    - All cancellation actions logged
    - All replacement actions logged
    - User identification for all actions
    - Timestamp for all actions
    - IP address or location (if available)
    - Action details
    - Previous and new values (for modifications)
    - Reasons (for cancellations and replacements)
    - Audit trail cannot be modified or deleted
    - Audit trail retained per regulatory requirements
  
  - **Audit Trail Information**:
    - Action type (Modified, Cancelled, Replaced)
    - Action date and time
    - Action user
    - Action details
    - Prescription number
    - Patient information
    - Related prescriptions (for replacements)
    - Pharmacy notification status
    - Confirmation status

##### 3.2.6.5 Modification and Cancellation Workflows

- **FR-P6.9**: System shall support efficient modification and cancellation workflows:
  - **Modification Workflow**:
    - Quick access to prescription for modification
    - Modification wizard or form
    - Modification preview (before saving)
    - Modification confirmation
    - Batch modification (if applicable)
    - Modification templates (if applicable)
  
  - **Cancellation Workflow**:
    - Quick access to prescription for cancellation
    - Cancellation wizard or form
    - Cancellation reason selection
    - Cancellation confirmation
    - Batch cancellation (if applicable)
    - Cancellation templates (if applicable)
  
  - **Replacement Workflow**:
    - Quick access to prescription for replacement
    - Replacement wizard (create new from original)
    - Replacement preview
    - Replacement confirmation
    - Automatic cancellation of original (if applicable)

##### 3.2.6.6 Modification and Cancellation Restrictions and Security

- **FR-P6.10**: System shall enforce appropriate restrictions on modifications and cancellations:
  - **Access Restrictions**:
    - Only prescribing provider can modify/cancel (unless authorized)
    - Covering provider can modify/cancel (if authorized)
    - Supervisor can modify/cancel (with authorization and documentation)
    - Other providers cannot modify/cancel (unless specifically authorized)
    - Role-based restrictions enforced
  
  - **Time Restrictions**:
    - Cannot modify/cancel very old prescriptions (configurable time limit)
    - Cannot modify/cancel after certain time period (configurable)
    - Cannot modify/cancel expired prescriptions (administrative only)
    - Time-based restrictions configurable by organization
  
  - **Status Restrictions**:
    - Cannot modify filled prescriptions
    - Cannot cancel filled prescriptions (must create new prescription)
    - Cannot modify expired prescriptions
    - Cannot modify cancelled prescriptions
    - Status-based restrictions enforced

- **FR-P6.11**: System shall implement security measures for modifications and cancellations:
  - **Authentication**:
    - User authentication required
    - Provider authentication required
    - Electronic signature for modifications (if required)
    - Electronic signature for cancellations (if required)
  
  - **Authorization**:
    - Role-based authorization
    - Permission checks
    - Authorization for sensitive modifications
    - Authorization for cancellations of sent prescriptions
    - Supervisor approval for certain modifications/cancellations (if required)
  
  - **Security Logging**:
    - All modification attempts logged
    - All cancellation attempts logged
    - Failed modification attempts logged
    - Failed cancellation attempts logged
    - Unauthorized access attempts logged
    - Security events monitored

##### 3.2.6.8 Error Handling and Recovery

- **FR-P6.13**: System shall implement comprehensive error handling for prescription modifications and cancellations:
  - **Modification Error Handling**:
    - **Modification Processing Failures**:
      - Handle modification save failures
      - Handle modification validation errors
      - Handle modification transmission failures (if already sent)
      - Display error: "Unable to modify prescription. Please try again."
      - Retry modification automatically
      - Queue modification for retry if persistent failure
      - Log modification errors
    
    - **Concurrent Modification Conflicts**:
      - Handle concurrent modification attempts
      - Handle modification conflicts with other users
      - Display error: "Prescription was modified by another user. Please refresh and try again."
      - Provide conflict resolution interface
      - Show conflicting changes side-by-side
      - Allow user to choose which version to keep
      - Log modification conflicts
    
    - **Invalid Modification Errors**:
      - Handle attempts to modify filled prescriptions
      - Handle attempts to modify expired prescriptions
      - Handle attempts to modify cancelled prescriptions
      - Handle attempts to modify prescriptions beyond time limit
      - Display error: "Cannot modify prescription in [status] status."
      - Prevent invalid modifications
      - Log invalid modification attempts
    
    - **Modification Validation Errors**:
      - Handle validation errors after modification (see FR-P1.10)
      - Handle re-validation failures after modification
      - Display validation errors clearly
      - Prevent saving invalid modifications
      - Log validation errors
  
  - **Cancellation Error Handling**:
    - **Cancellation Processing Failures**:
      - Handle cancellation save failures
      - Handle cancellation transmission failures (if already sent)
      - Handle pharmacy notification failures for cancellations
      - Display error: "Unable to cancel prescription. Please try again."
      - Retry cancellation automatically
      - Queue cancellation for retry if persistent failure
      - Log cancellation errors
    
    - **Invalid Cancellation Errors**:
      - Handle attempts to cancel filled prescriptions
      - Handle attempts to cancel expired prescriptions
      - Handle attempts to cancel already cancelled prescriptions
      - Display error: "Cannot cancel prescription in [status] status."
      - Prevent invalid cancellations
      - Log invalid cancellation attempts
    
    - **Pharmacy Notification Errors**:
      - Handle pharmacy cancellation notification failures
      - Handle pharmacy acknowledgment failures
      - Display warning: "Pharmacy may not have been notified of cancellation."
      - Queue notifications for retry
      - Log notification errors
  
  - **Replacement Error Handling**:
    - **Replacement Processing Failures**:
      - Handle replacement prescription creation failures
      - Handle original prescription cancellation failures during replacement
      - Handle replacement link failures
      - Display error: "Unable to replace prescription. Please try again."
      - Retry replacement automatically
      - Roll back changes if replacement fails
      - Log replacement errors
    
    - **Invalid Replacement Errors**:
      - Handle attempts to replace non-existent prescriptions
      - Handle attempts to replace already replaced prescriptions
      - Display error: "Cannot replace prescription: [reason]."
      - Prevent invalid replacements
      - Log invalid replacement attempts
  
  - **Documentation Error Handling**:
    - **Documentation Save Failures**:
      - Handle modification documentation save failures
      - Handle cancellation documentation save failures
      - Handle replacement documentation save failures
      - Display error: "Unable to save documentation. Please try again."
      - Retry documentation saves automatically
      - Queue documentation for retry if persistent failure
      - Log documentation errors
    
    - **Audit Trail Errors**:
      - Handle audit trail write failures
      - Handle audit trail corruption
      - Display warning: "Some audit trail information may be unavailable."
      - Retry audit trail writes automatically
      - Log audit trail errors
  
  - **System Error Handling**:
    - **Database Errors**:
      - Handle database connection failures
      - Handle database timeout errors
      - Handle database constraint violations
      - Display user-friendly error messages
      - Retry database operations automatically
      - Log database errors with context
    
    - **Network Errors**:
      - Handle network connectivity failures
      - Handle network timeout errors
      - Display error: "Network connection lost. Please check your connection."
      - Retry network operations automatically
      - Queue operations for retry when connection restored
      - Log network errors
    
    - **Application Errors**:
      - Handle application crashes during modification/cancellation
      - Recover modification/cancellation state after crash
      - Display recovery message
      - Log application errors with stack traces
  
  - **Security and Authorization Error Handling**:
    - **Access Denied Errors**:
      - Handle insufficient permissions for modification
      - Handle insufficient permissions for cancellation
      - Handle insufficient permissions for replacement
      - Display error: "You do not have permission to [action] this prescription."
      - Log authorization failures
      - Prevent unauthorized actions
    
    - **Authentication Errors**:
      - Handle session expiration during modification/cancellation
      - Auto-save state before session expiration
      - Prompt user to re-authenticate
      - Restore state after re-authentication
      - Log authentication errors
    
    - **DEA Authorization Errors** (for controlled substances):
      - Handle DEA number validation failures during modification
      - Handle DEA authorization failures
      - Display error: "DEA authorization required for controlled substance modification."
      - Prevent unauthorized modifications
      - Log DEA authorization errors
  
  - **Recovery Mechanisms**:
    - **Auto-Save and Recovery**:
      - Auto-save modification data every 30 seconds
      - Auto-save before navigation away
      - Restore modification data after error recovery
      - Display recovery notification
      - Allow user to discard recovered changes
    
    - **Retry Logic**:
      - Automatic retry for transient errors (up to 3 attempts)
      - Exponential backoff for retries
      - Manual retry option for all errors
      - Display retry status to user
    
    - **Rollback Mechanisms**:
      - Roll back modifications if save fails
      - Roll back cancellations if processing fails
      - Roll back replacements if creation fails
      - Restore original prescription state
      - Log all rollback operations
  
  - **Error Logging and Reporting**:
    - Log all errors with full context:
      - Error type and message
      - Prescription ID
      - Modification/cancellation type
      - User ID and role
      - Action attempted
      - Timestamp
      - System state
      - Stack trace (for application errors)
    - Generate error reports for system administrators
    - Alert administrators for critical errors
    - Track error trends and patterns
    - Monitor modification/cancellation processing performance

##### 3.2.6.7 Modification and Cancellation Reporting

- **FR-P6.12**: System shall provide reporting for modifications and cancellations:
  - **Modification Reports**:
    - Modifications by provider
    - Modifications by prescription type
    - Modifications by reason
    - Modification frequency
    - Fields most frequently modified
    - Modification patterns
  
  - **Cancellation Reports**:
    - Cancellations by provider
    - Cancellations by prescription type
    - Cancellations by reason
    - Cancellation frequency
    - Cancellation patterns
    - Cancellation rate
  
  - **Replacement Reports**:
    - Replacements by provider
    - Replacements by prescription type
    - Replacements by reason
    - Replacement frequency
    - Replacement patterns
  
  - **Quality Reports**:
    - Modification appropriateness
    - Cancellation appropriateness
    - Error-related modifications/cancellations
    - Quality metrics
  
  - **Report Features**:
    - Reports exportable in multiple formats (PDF, Excel, CSV)
    - Reports support filtering, sorting, and customization
    - Scheduled reports (if applicable)

#### 3.2.7 Controlled Substances Management

##### 3.2.7.1 Controlled Substance Classification and Identification

- **FR-P7.1**: System shall identify and classify controlled substances:
  - **DEA Schedule Classification**:
    - **Schedule I**: Drugs with no accepted medical use and high abuse potential (rarely prescribed, if ever)
    - **Schedule II**: Drugs with high abuse potential, accepted medical use, severe psychological or physical dependence
      - Examples: Morphine, Oxycodone, Fentanyl, Adderall, Ritalin
      - No refills allowed (unless state allows)
      - Stricter requirements
    - **Schedule III**: Drugs with moderate abuse potential, accepted medical use, moderate dependence
      - Examples: Codeine combinations, Testosterone, Ketamine
      - Limited refills (up to 5 refills in 6 months)
    - **Schedule IV**: Drugs with low abuse potential, accepted medical use, limited dependence
      - Examples: Xanax, Valium, Ambien, Lorazepam
      - Limited refills (up to 5 refills in 6 months)
    - **Schedule V**: Drugs with lowest abuse potential, accepted medical use, limited dependence
      - Examples: Cough syrups with codeine, Lyrica
      - Limited refills (up to 5 refills in 6 months)
  
  - **Controlled Substance Identification**:
    - Automatic identification of controlled substances in drug database
    - DEA schedule displayed prominently
    - Controlled substance flag on medication
    - Schedule-specific requirements displayed
    - Controlled substance warnings displayed

##### 3.2.7.2 DEA Number Validation

- **FR-P7.2**: System shall validate DEA numbers for controlled substance prescriptions with the following specific validation rules:
  - **DEA Number Requirements**:
    - DEA number required for all controlled substance prescriptions (Schedule II-V)
    - DEA number cannot be null or empty for controlled substances
    - System shall prevent prescription creation if DEA number missing for controlled substance
    - DEA number must be from authenticated prescribing provider
    - DEA number must be active and valid at time of prescription
  
  - **DEA Number Format Validation**:
    - Format: Exactly 2 letters followed by exactly 7 digits (total 9 characters)
    - Format examples: AB1234567, XY9876543
    - First letter: Must be A, B, F, G, M, P, or X (valid DEA registration types)
    - Second letter: Must be first letter of provider's last name (case-insensitive)
    - Seven digits: Must be numeric (0-9)
    - Cannot contain spaces, hyphens, or special characters
    - Case-insensitive for letters (AB1234567 = ab1234567)
    - Format validation: Must match regex pattern: ^[ABFGMXP][A-Za-z]\d{7}$
    - Display clear error if format invalid: "DEA number must be 2 letters followed by 7 digits (e.g., AB1234567)"
  
  - **DEA Number Checksum Validation**:
    - **Checksum Algorithm**:
      - Sum of digits in positions 1, 3, 5: (digit1 + digit3 + digit5)
      - Sum of digits in positions 2, 4, 6, multiplied by 2: (digit2 + digit4 + digit6) × 2
      - Total sum = sum of both calculations
      - Check digit (position 7) must equal last digit of total sum
    - Validate checksum algorithm before accepting DEA number
    - Display error if checksum invalid: "DEA number checksum is invalid. Please verify the number."
    - Prevent prescription if checksum invalid
  
  - **DEA Number Provider Validation**:
    - Second letter must match first letter of prescribing provider's last name
    - Case-insensitive matching (e.g., provider "Smith" can have DEA starting with "S" or "s")
    - Display error if letter doesn't match: "DEA number second letter must match first letter of provider's last name"
    - Warn if letter doesn't match but allow override with documentation (if provider name changed)
    - Verify DEA number belongs to authenticated prescribing provider
    - Prevent prescription if DEA number doesn't match provider
  
  - **DEA Number Status Validation**:
    - Check DEA number status (active, suspended, revoked, expired)
    - Validate against DEA database if available (real-time or cached)
    - Check DEA number expiration date (if available)
    - Prevent prescription if DEA number is suspended or revoked
    - Warn if DEA number is expired (require confirmation)
    - Warn if DEA number expiration is within 30 days
    - Display DEA number status in prescription interface
    - Log DEA number validation attempts and results
  
  - **DEA Number Database Validation** (if available):
    - Query DEA database for number validity
    - Verify number exists in DEA registry
    - Verify number is assigned to correct provider
    - Handle database query failures gracefully (allow with warning if database unavailable)
    - Cache validation results for performance (with expiration)
    - Re-validate if cached result is stale (> 24 hours old)
  
  - **DEA Number Validation Process**:
    - Validate format first (fastest check)
    - Validate checksum second
    - Validate provider match third
    - Validate status fourth (may require database query)
    - Display validation results in real-time
    - Prevent prescription submission if any validation fails
    - Display specific error message for each validation failure
    - Allow correction and re-validation
  
  - **DEA Number Display**:
    - Display provider's DEA number in prescription interface
    - Display DEA number on prescription (required for controlled substances)
    - Display DEA number in prescription details
    - Mask DEA number in certain views (for security, show as AB****567)
    - Mask DEA number in audit logs (show partial number only)
    - Full DEA number visible only to authorized users
    - DEA number encryption at rest and in transit
  
  - **Error Handling for DEA Validation**:
    - Display clear error messages for each validation failure
    - Provide specific guidance for correction (e.g., "DEA number format: 2 letters + 7 digits")
    - Highlight invalid DEA number field visually
    - Prevent prescription creation until DEA number is valid
    - Maintain entered DEA number when validation fails (don't clear field)
    - Allow manual override with documentation (if authorized, with audit trail)

##### 3.2.7.3 Enhanced Security Requirements

- **FR-P7.3**: System shall implement enhanced security for controlled substances:
  - **Authentication Requirements**:
    - Strong authentication required for controlled substance prescriptions
    - Two-factor authentication (if configured)
    - Additional password/PIN for controlled substances (if configured)
    - Biometric authentication (if available and configured)
    - Session timeout for controlled substance functions
  
  - **Authorization Requirements**:
    - Only authorized prescribers can prescribe controlled substances
    - DEA number required and validated
    - State license validation (if required)
    - Special authorization for certain schedules (if required)
    - Role-based access controls
  
  - **Audit Requirements**:
    - Enhanced audit logging for all controlled substance activities
    - All controlled substance prescriptions logged
    - All controlled substance modifications logged
    - All controlled substance cancellations logged
    - All controlled substance refills logged
    - All PDMP queries logged
    - Audit trail cannot be modified
    - Audit trail retained per regulatory requirements (typically longer retention)
  
  - **Access Controls**:
    - Restricted access to controlled substance prescription functions
    - Restricted access to controlled substance prescription data
    - Break-the-glass functionality (with enhanced audit)
    - Access monitoring and alerts

##### 3.2.7.4 Prescription Drug Monitoring Program (PDMP) Integration

- **FR-P7.4**: System shall integrate with Prescription Drug Monitoring Programs:

  > **Known deferred item — PDMP integration is currently simulated (not production-ready).**
  > `PDMPService` (678 lines) contains a real HTTP integration path, but `application.yml` sets `pdmp.simulate: true`, which unconditionally routes every query to `performSimulatedPDMPQuery()`. That method returns synthetic prescription history drawn from local EHR data and always names the source `"Simulated PDMP System"`. Even when `pdmp.simulate: false` is set, the real path silently falls back to simulation on any API error, missing endpoint config, or non-200 response (see `PDMPService` lines 302–373). **No real PDMP authority is ever contacted in the current build.** This means the compliance gate described below (query required before controlled substance prescriptions) is not enforced against live PDMP data. This is an accepted gap for Phase 1; before any production go-live with controlled-substance prescribing the following must be completed:
  > 1. Obtain credentials and endpoint URL from the target state PDMP gateway (e.g. PMP InterConnect, NarxCare, or direct state API).
  > 2. Set `pdmp.simulate: false` and configure `pdmp.endpoint`, `pdmp.api-key` in environment-specific config (not in `application.yml`).
  > 3. Replace the silent simulation fallbacks in the real path with explicit failure — a failed PDMP query must block the prescription, not silently return synthetic data.
  > 4. Obtain legal/compliance sign-off that the integration meets the mandatory-query statute for each state where the system is deployed.

  - **PDMP Query Requirements**:
    - Query PDMP before prescribing controlled substances (if required by state)
    - Query PDMP for Schedule II-V controlled substances
    - Query PDMP for patient's controlled substance history
    - Query PDMP for prescriber's controlled substance prescribing history (if applicable)
    - Real-time or near-real-time PDMP queries
    - PDMP query results displayed to provider
  
  - **PDMP Integration Methods**:
    - Integration with state PDMP systems
    - Integration with national PDMP (if available)
    - Integration via PDMP gateway (if available)
    - Direct API integration
    - Web service integration
    - Manual PDMP query (if automated unavailable)
  
  - **PDMP Data Display**:
    - Patient's controlled substance prescription history
    - Prescriptions by date range
    - Prescriptions by medication
    - Prescriptions by prescriber
    - Prescriptions by pharmacy
    - Prescription quantities
    - Days supply
    - Potential red flags (multiple prescribers, multiple pharmacies, early refills, etc.)
    - PDMP data prominently displayed
    - PDMP data interpretation guidance
  
  - **PDMP Query Workflow**:
    - Automatic PDMP query when controlled substance selected
    - Manual PDMP query option
    - PDMP query before prescription completion
    - PDMP query results review
    - Provider acknowledgment of PDMP review
    - PDMP query logging
    - PDMP query timing and performance

- **FR-P7.5**: System shall handle PDMP query results and alerts:
  - **PDMP Alert Types**:
    - Multiple prescribers (patient receiving controlled substances from multiple providers)
    - Multiple pharmacies (patient filling at multiple pharmacies)
    - Early refills (refills requested/filled before expected)
    - High quantities (unusually high quantities prescribed/dispensed)
    - Drug interactions (multiple controlled substances)
    - Potential abuse patterns
    - Potential diversion patterns
  
  - **PDMP Alert Handling**:
    - Display PDMP alerts prominently
    - Require provider review of PDMP data
    - Require provider acknowledgment of alerts
    - Allow provider to proceed with prescription (with documentation)
    - Allow provider to modify or cancel prescription based on PDMP data
    - Document provider's response to PDMP alerts
    - PDMP alert audit trail

##### 3.2.7.5 Quantity and Duration Limits

- **FR-P7.6**: System shall enforce quantity and duration limits for controlled substances with the following specific validation rules:
  - **Quantity Limits by Schedule**:
    - **Schedule II**:
      - Maximum quantity: 30-day supply (enforced)
      - Maximum daily dose: Based on medication-specific limits
      - Quantity calculation: (daily dose) × (30 days maximum)
      - Prevent prescription if quantity exceeds 30-day supply
      - Warn if quantity approaches 30-day supply limit
      - Display 30-day supply limit prominently
    
    - **Schedule III-V**:
      - Maximum quantity: 90-day supply (enforced)
      - Maximum daily dose: Based on medication-specific limits
      - Quantity calculation: (daily dose) × (90 days maximum)
      - Prevent prescription if quantity exceeds 90-day supply
      - Warn if quantity approaches 90-day supply limit
      - Display 90-day supply limit prominently
    
    - **Quantity Limit Validation**:
      - Calculate days supply: (total quantity) ÷ (daily quantity)
      - Validate days supply against schedule limit
      - Validate quantity against medication-specific maximum
      - Validate quantity against state-specific maximum (if stricter)
      - Display current quantity vs. maximum allowed
      - Prevent prescription if any limit exceeded
  
  - **Duration Limits by Schedule**:
    - **Schedule II**:
      - Maximum duration: 30 days (enforced)
      - Cannot exceed 30 days from start date
      - Prevent prescription if duration > 30 days
      - Warn if duration approaches 30-day limit
      - Display 30-day duration limit prominently
    
    - **Schedule III-V**:
      - Maximum duration: 90 days (enforced)
      - Cannot exceed 90 days from start date
      - Prevent prescription if duration > 90 days
      - Warn if duration approaches 90-day limit
      - Display 90-day duration limit prominently
    
    - **Duration Limit Validation**:
      - Calculate duration: (end date) - (start date) in days
      - Validate duration against schedule limit
      - Validate duration against medication-specific maximum
      - Validate duration against state-specific maximum (if stricter)
      - Display current duration vs. maximum allowed
      - Prevent prescription if any limit exceeded
  
  - **Days Supply Limits**:
    - **Days Supply Calculation**:
      - Formula: Days Supply = (Total Quantity) ÷ (Daily Quantity)
      - Daily Quantity = (Quantity per dose) × (Frequency per day)
      - Round days supply to nearest whole day
      - Validate calculation accuracy
    
    - **Days Supply Limits by Schedule**:
      - Schedule II: Maximum 30 days supply (enforced)
      - Schedule III-V: Maximum 90 days supply (enforced)
      - Prevent prescription if days supply exceeds schedule limit
      - Warn if days supply approaches limit
    
    - **Days Supply Validation**:
      - Validate days supply against schedule limit
      - Validate days supply against medication-specific limit
      - Validate days supply against state-specific limit (if stricter)
      - Display calculated days supply vs. maximum allowed
      - Prevent prescription if any limit exceeded
      - Warn if days supply calculation seems incorrect (e.g., very high or very low)
  
  - **State-Specific Limits**:
    - **State Limit Validation**:
      - Check state-specific quantity limits (if stricter than federal)
      - Check state-specific duration limits (if stricter than federal)
      - Check state-specific days supply limits (if stricter than federal)
      - Apply stricter of federal or state limits
      - Display applicable state limits
      - Prevent prescription if state limit exceeded
    
    - **State Limit Examples** (configurable by state):
      - Some states: Schedule II maximum 7-day supply
      - Some states: Schedule II maximum 14-day supply
      - Some states: Opioid-specific limits (e.g., 7-day supply for acute pain)
      - State limits override federal limits if stricter
  
  - **Medication-Specific Limits**:
    - **Medication Limit Validation**:
      - Check medication-specific quantity limits (if available)
      - Check medication-specific duration limits (if available)
      - Check medication-specific days supply limits (if available)
      - Apply medication-specific limits in addition to schedule limits
      - Display medication-specific limits
      - Prevent prescription if medication limit exceeded
  
  - **Daily Dose Limits**:
    - **Maximum Daily Dose Calculation**:
      - Calculate total daily dose: (dosage strength) × (quantity per dose) × (frequency per day)
      - Validate against medication-specific maximum daily dose
      - Validate against schedule-specific maximum daily dose
      - Prevent prescription if maximum daily dose exceeded
      - Warn if daily dose approaches maximum
  
  - **Limit Enforcement**:
    - **Automatic Limit Checking**:
      - Check all limits in real-time during prescription entry
      - Check limits before prescription completion
      - Check limits before prescription transmission
      - Display limit violations immediately
    
    - **Limit Validation Process**:
      - Validate quantity limit first
      - Validate duration limit second
      - Validate days supply limit third
      - Validate state-specific limits fourth
      - Validate medication-specific limits fifth
      - Display all limit violations
      - Prevent prescription if any limit exceeded
    
    - **Limit Alerts**:
      - Display limit alerts prominently (red for exceeded, yellow for approaching)
      - Display specific limit violated (e.g., "Quantity exceeds 30-day supply limit")
      - Display current value vs. maximum allowed
      - Display calculation details (if applicable)
      - Provide correction suggestions
  
  - **Limit Override Process**:
    - **Override Authorization**:
      - Override requires supervisor or authorized provider approval
      - Override requires documentation of medical necessity
      - Override requires reason for exceeding limit
      - Override requires patient-specific justification
      - Override logged in audit trail
    
    - **Override Restrictions**:
      - Cannot override absolute safety limits (e.g., cannot exceed manufacturer maximum)
      - Cannot override certain state-mandated limits (if non-overridable)
      - Override limits configurable by organization
      - Override requires additional authentication (if configured)
    
    - **Override Documentation**:
      - Reason for override (required, free text)
      - Medical necessity justification (required)
      - Patient-specific factors (if applicable)
      - Alternative options considered (if applicable)
      - Override approval (supervisor signature, if required)
      - Override timestamp and user ID
      - Override audit trail maintained
  
  - **Limit Validation Error Handling**:
    - Display clear error messages for limit violations
    - Error messages shall indicate which limit was violated and why
    - Error messages shall show current value vs. maximum allowed
    - Error messages shall suggest corrections (e.g., "Reduce quantity to 30-day supply or reduce frequency")
    - System shall highlight limit violations visually
    - System shall prevent prescription submission until limits are met or override is authorized
    - System shall maintain entered data when limit validation fails

##### 3.2.7.6 Refill Restrictions

- **FR-P7.7**: System shall enforce refill restrictions for controlled substances with the following specific validation rules:
  - **Schedule-Specific Refill Rules**:
    - **Schedule II**:
      - **Federal Rule**: No refills allowed (enforced)
      - Refills must be set to 0 (zero) for Schedule II prescriptions
      - Prevent prescription if refills > 0 for Schedule II
      - Display message: "Schedule II controlled substances cannot have refills. Must create new prescription for additional supply."
      - State-specific exceptions: Some states allow limited refills with strict limitations (if applicable, configurable)
      - If state exception applies: Maximum 0-2 refills (state-specific, with strict documentation requirements)
    
    - **Schedule III-V**:
      - **Federal Rule**: Maximum 5 refills allowed
      - Maximum refills: 5 (enforced, cannot exceed)
      - Refill count tracked: System tracks number of refills authorized and remaining
      - Refill expiration: 6 months from original prescription date (enforced)
      - Refill restrictions enforced: Cannot authorize refills after expiration
      - Prevent prescription if refills > 5 for Schedule III-V
      - Display message: "Schedule III-V controlled substances: Maximum 5 refills in 6 months"
    
    - **Refill Validation**:
      - **Refill Count Validation**:
        - Must be valid integer (0 or positive)
        - Cannot be negative
        - Schedule II: Must be 0 (enforced)
        - Schedule III-V: Must be between 0-5 (enforced)
        - Display error if refill count invalid for schedule
      
      - **Refill Eligibility Validation**:
        - Check refill eligibility before authorizing refills
        - Check if prescription is still valid (not expired, not cancelled)
        - Check if refill count has been exceeded
        - Check if refill expiration date has passed
        - Check if medication is still appropriate for patient
        - Prevent refill authorization if any eligibility check fails
      
      - **Refill Expiration Validation**:
        - Calculate refill expiration: Original prescription date + 6 months
        - Validate refill expiration date
        - Prevent refills after expiration date
        - Display expiration date prominently
        - Warn if expiration date approaching (within 30 days)
      
      - **Refill Count Tracking**:
        - Track total refills authorized: Initial prescription + refills authorized
        - Track remaining refills: Authorized refills - refills used
        - Display refill count: "X of Y refills remaining"
        - Prevent additional refills if count exceeded
        - Update refill count when refill is authorized
        - Update remaining refills when refill is filled
      
      - **State-Specific Refill Rules** (if applicable):
        - Some states have stricter refill limits
        - Some states have different expiration periods
        - Apply state-specific rules if stricter than federal
        - Display state-specific refill restrictions
        - Prevent refills if state limit exceeded
    - Validate refill count not exceeded
    - Validate refill time limit not exceeded
    - Validate prescription not expired
    - Prevent unauthorized refills
    - Refill restrictions displayed
  
  - **Refill Documentation**:
    - All controlled substance refills documented
    - Refill count tracked
    - Refill dates recorded
    - Refill authorization logged
    - Refill audit trail

##### 3.2.7.7 State-Specific Requirements

- **FR-P7.8**: System shall comply with state-specific controlled substance requirements:
  - **State Requirements**:
    - State-specific prescription format requirements
    - State-specific security paper requirements (if applicable)
    - State-specific quantity limits
    - State-specific duration limits
    - State-specific refill rules
    - State-specific PDMP requirements
    - State-specific reporting requirements
    - State-specific prescriber requirements
  
  - **State Configuration**:
    - Configurable state-specific rules
    - State selection for prescriptions
    - Automatic state rule application
    - State rule validation
    - State rule updates
  
  - **Multi-State Support**:
    - Support for prescribers licensed in multiple states
    - Support for patients in different states
    - State-specific rule application based on prescription location
    - State-specific rule application based on patient location
    - State-specific rule application based on prescriber license

##### 3.2.7.8 Controlled Substance Prescription Format

- **FR-P7.9**: System shall generate controlled substance prescriptions in required formats:
  - **Electronic Prescription Format**:
    - NCPDP SCRIPT standard with controlled substance indicators
    - Controlled substance schedule indicated
    - DEA number included
    - State-specific data elements included
    - Security features included
    - Tamper-resistant features (if applicable)
  
  - **Paper Prescription Format** (if printing required):
    - Security paper requirements (if state requires)
    - Tamper-resistant features
    - Required data elements
    - Prescriber signature required
    - Patient identification required
    - Controlled substance indicators
  
  - **Prescription Data Elements**:
    - Patient name and address
    - Patient date of birth
    - Medication name, strength, quantity
    - Directions for use
    - Prescriber name, address, DEA number
    - Prescription date
    - Number of refills (if allowed)
    - Prescriber signature
    - State-specific requirements

##### 3.2.7.9 Controlled Substance Reporting

- **FR-P7.10**: System shall provide controlled substance reporting:
  - **Prescriber Reports**:
    - Controlled substance prescriptions by prescriber
    - Controlled substance prescribing volume
    - Controlled substance prescribing patterns
    - Prescriber compliance with regulations
    - Prescriber PDMP query compliance
  
  - **Patient Reports**:
    - Controlled substance prescriptions by patient
    - Patient controlled substance history
    - Patient prescription patterns
    - Potential abuse indicators
    - Patient compliance
  
  - **Regulatory Reports**:
    - Controlled substance prescription volume
    - Controlled substance prescription trends
    - Compliance with regulations
    - PDMP query compliance
    - State reporting requirements (if applicable)
  
  - **Quality Reports**:
    - Controlled substance prescribing quality metrics
    - Controlled substance error rates
    - Controlled substance safety metrics
    - Controlled substance adherence metrics

##### 3.2.7.10 Controlled Substance Security and Compliance

- **FR-P7.11**: System shall ensure controlled substance security and compliance:
  - **Data Security**:
    - Enhanced encryption for controlled substance data
    - Secure storage of controlled substance prescriptions
    - Secure transmission of controlled substance prescriptions
    - Access controls for controlled substance data
    - Data backup and recovery for controlled substance data
  
  - **Compliance Monitoring**:
    - Monitor controlled substance prescribing patterns
    - Monitor PDMP query compliance
    - Monitor regulatory compliance
    - Alert on potential compliance issues
    - Compliance reporting
  
  - **Regulatory Compliance**:
    - Compliance with DEA regulations
    - Compliance with state regulations
    - Compliance with federal regulations
    - Compliance with PDMP requirements
    - Compliance documentation
    - Compliance audits

##### 3.2.7.11 Error Handling and Recovery

- **FR-P7.12**: System shall implement comprehensive error handling for controlled substances management:
  - **DEA Validation Error Handling**:
    - **DEA Database Connection Failures**:
      - Handle DEA database connection failures
      - Handle DEA database timeout errors
      - Display warning: "DEA validation temporarily unavailable. Prescription creation may be delayed."
      - Allow prescription creation with manual DEA verification (if authorized)
      - Queue DEA validations for retry
      - Cache recent DEA validation results
      - Log DEA database errors
    
    - **DEA Validation Processing Failures**:
      - Handle DEA checksum validation failures
      - Handle DEA format validation failures
      - Handle DEA provider matching failures
      - Display error: "DEA number validation failed: [reason]."
      - Prevent prescription creation if DEA invalid
      - Log DEA validation errors
    
    - **DEA Status Check Failures**:
      - Handle DEA status check failures
      - Handle DEA expiration check failures
      - Display warning: "Unable to verify DEA status. Please verify manually."
      - Allow prescription creation with warning (if authorized)
      - Log status check errors
  
  - **PDMP Integration Error Handling**:
    - **PDMP Query Failures**:
      - Handle PDMP connection failures
      - Handle PDMP query timeout errors
      - Handle PDMP service unavailability
      - Display warning: "PDMP query unavailable. Prescription creation may be delayed."
      - Allow prescription creation with manual PDMP check (if required by state)
      - Queue PDMP queries for retry
      - Log PDMP query errors
    
    - **PDMP Response Errors**:
      - Handle invalid PDMP response formats
      - Handle missing PDMP data
      - Handle PDMP response parsing errors
      - Display warning: "PDMP data may be incomplete."
      - Log PDMP response errors
    
    - **PDMP Alert Processing Failures**:
      - Handle PDMP alert generation failures
      - Handle PDMP alert display failures
      - Display generic alert if specific alert unavailable
      - Log alert processing errors
  
  - **Quantity and Duration Limit Error Handling**:
    - **Limit Calculation Failures**:
      - Handle days supply calculation failures
      - Handle quantity limit calculation failures
      - Handle duration limit calculation failures
      - Display error: "Unable to calculate limits. Please verify manually."
      - Prevent prescription creation if calculation fails
      - Log calculation errors
    
    - **Limit Validation Failures**:
      - Handle limit validation processing failures
      - Handle state-specific limit lookup failures
      - Display error: "Unable to validate limits. Please verify manually."
      - Prevent prescription creation if validation fails
      - Log validation errors
    
    - **Override Processing Failures**:
      - Handle override authorization failures
      - Handle override documentation save failures
      - Display error: "Unable to process limit override. Please try again."
      - Retry override processing automatically
      - Log override errors
  
  - **Refill Restriction Error Handling**:
    - **Refill Validation Failures**:
      - Handle refill limit validation failures
      - Handle refill expiration calculation failures
      - Handle refill count tracking failures
      - Display error: "Unable to validate refill restrictions. Please verify manually."
      - Prevent prescription creation if validation fails
      - Log refill validation errors
    
    - **Refill Count Tracking Errors**:
      - Handle refill count update failures
      - Handle refill count retrieval failures
      - Display warning: "Refill count may be inaccurate."
      - Log tracking errors
  
  - **State-Specific Requirement Error Handling**:
    - **State Requirement Lookup Failures**:
      - Handle state requirement database failures
      - Handle state requirement lookup timeouts
      - Display warning: "State requirements may not be fully validated."
      - Allow prescription creation with warning (if authorized)
      - Log state requirement lookup errors
    
    - **State Requirement Validation Failures**:
      - Handle state requirement validation processing failures
      - Handle state-specific format validation failures
      - Display error: "Unable to validate state requirements. Please verify manually."
      - Prevent prescription creation if validation fails
      - Log validation errors
  
  - **Controlled Substance Format Error Handling**:
    - **Format Generation Failures**:
      - Handle prescription format generation failures
      - Handle required field population failures
      - Display error: "Unable to generate controlled substance prescription format."
      - Prevent prescription transmission if format invalid
      - Log format generation errors
    
    - **Format Validation Failures**:
      - Handle format validation processing failures
      - Handle format compliance check failures
      - Display error: "Prescription format does not meet controlled substance requirements."
      - Prevent prescription transmission if format invalid
      - Log format validation errors
  
  - **Reporting Error Handling**:
    - **Report Generation Failures**:
      - Handle controlled substance report generation failures
      - Handle report query failures
      - Handle report format errors
      - Display error: "Unable to generate controlled substance report."
      - Retry report generation automatically
      - Log report generation errors
    
    - **Report Transmission Failures**:
      - Handle report transmission failures to regulatory bodies
      - Handle report delivery failures
      - Queue reports for retry
      - Display warning: "Report transmission may be delayed."
      - Log transmission errors
  
  - **Security Error Handling**:
    - **Enhanced Security Requirement Failures**:
      - Handle two-factor authentication failures
      - Handle additional authentication requirement failures
      - Display error: "Enhanced security requirements not met."
      - Prevent prescription creation if security requirements not met
      - Log security requirement failures
    
    - **Access Control Failures**:
      - Handle access control check failures
      - Handle permission validation failures
      - Display error: "Access denied. Insufficient permissions for controlled substance prescriptions."
      - Prevent unauthorized access
      - Log access control failures
  
  - **System Error Handling**:
    - **Database Errors**:
      - Handle database connection failures
      - Handle database timeout errors
      - Handle database constraint violations
      - Display user-friendly error messages
      - Retry database operations automatically
      - Log database errors with context
    
    - **Network Errors**:
      - Handle network connectivity failures
      - Handle network timeout errors
      - Display error: "Network connection lost. Please check your connection."
      - Retry network operations automatically
      - Queue operations for retry when connection restored
      - Log network errors
    
    - **Application Errors**:
      - Handle application crashes during controlled substance processing
      - Recover controlled substance state after crash
      - Display recovery message
      - Log application errors with stack traces
  
  - **Recovery Mechanisms**:
    - **Retry Logic**:
      - Automatic retry for transient errors (up to 3 attempts)
      - Exponential backoff for retries
      - Manual retry option for all errors
      - Display retry status to user
    
    - **Error Recovery Workflow**:
      - Provide "Retry" button for transient errors
      - Provide "Cancel" option to discard changes
      - Maintain controlled substance state during error recovery
      - Log all recovery attempts
    
    - **Fallback Mechanisms**:
      - Use cached DEA validation results if database unavailable
      - Use cached state requirements if database unavailable
      - Allow manual verification if automated checks fail (with proper authorization)
      - Display fallback status to user
  
  - **Error Logging and Reporting**:
    - Log all errors with full context:
      - Error type and message
      - Prescription ID (if available)
      - Controlled substance schedule
      - DEA number (masked in logs)
      - User ID and role
      - Action attempted
      - Timestamp
      - System state
      - Stack trace (for application errors)
    - Generate error reports for system administrators
    - Alert administrators for critical errors (PDMP down, DEA database down, etc.)
    - Track error trends and patterns
    - Monitor controlled substance processing performance
    - Report errors to compliance officers (if required)

#### 3.2.8 Prescription History and Reporting

##### 3.2.8.1 Prescription History Management

- **FR-P8.1**: System shall maintain comprehensive prescription history:
  - **History Scope**:
    - All prescriptions for each patient (current and historical)
    - All prescription statuses (Draft, Sent, Filled, Cancelled, Expired, etc.)
    - All prescription modifications
    - All prescription cancellations
    - All prescription replacements
    - All refill requests and approvals
    - All refill fills (if data available)
    - Complete prescription lifecycle
  
  - **History Information**:
    - Prescription number (unique identifier)
    - Prescription date
    - Medication name (generic and brand)
    - Dosage strength and form
    - Quantity
    - Frequency/instructions
    - Route of administration
    - Prescribing provider
    - Pharmacy information
    - Prescription status
    - Fill status and dates
    - Refill history
    - Modification history
    - Cancellation history
    - Replacement history
    - Related prescriptions
    - Linked diagnoses/problems
    - Special instructions
    - Prescription notes
  
  - **History Organization**:
    - Chronological organization (newest first or oldest first)
    - Organization by status
    - Organization by medication
    - Organization by provider
    - Organization by pharmacy
    - Organization by date range
    - Searchable history
    - Filterable history

- **FR-P8.2**: System shall provide prescription history display:
  - **History Views**:
    - Complete history view (all prescriptions)
    - Active prescriptions view
    - Filled prescriptions view
    - Cancelled prescriptions view
    - Expired prescriptions view
    - Recent prescriptions view
    - Prescriptions by medication
    - Prescriptions by provider
    - Prescriptions by pharmacy
  
  - **History Display Options**:
    - List view
    - Detail view
    - Timeline view
    - Summary view
    - Print view
    - Export view
  
  - **History Navigation**:
    - Scroll through history
    - Jump to specific date
    - Jump to specific prescription
    - Filter by date range
    - Filter by medication
    - Filter by status
    - Search history

##### 3.2.8.2 Medication Adherence Tracking

- **FR-P8.3**: System shall track medication adherence (if pharmacy data available):
  - **Adherence Data Sources**:
    - Pharmacy fill data (if available)
    - Prescription refill data
    - Days supply information
    - Fill dates
    - Refill dates
    - Prescription dates
  
  - **Adherence Calculations**:
    - **Proportion of Days Covered (PDC)**:
      - Calculate days covered by medication
      - Calculate total days in period
      - Calculate PDC percentage
      - PDC thresholds (e.g., <80% = non-adherent)
    - **Medication Possession Ratio (MPR)**:
      - Calculate days supply dispensed
      - Calculate days in period
      - Calculate MPR percentage
    - **Refill Adherence**:
      - Time between refills
      - Early refills (potential abuse or stockpiling)
      - Late refills (potential non-adherence)
      - Missed refills
      - Refill completion rate
  
  - **Adherence Indicators**:
    - Adherence percentage
    - Adherence status (Adherent, Non-adherent, Partially adherent)
    - Adherence trends over time
    - Adherence by medication
    - Adherence by medication class
    - Adherence alerts (if non-adherent)
  
  - **Adherence Display**:
    - Adherence metrics in patient summary
    - Adherence trends over time (graphs)
    - Adherence by medication
    - Adherence reports
    - Adherence alerts

- **FR-P8.4**: System shall support adherence interventions:
  - **Adherence Alerts**:
    - Alert for non-adherent patients
    - Alert for missed refills
    - Alert for late refills
    - Alert for early refills (potential abuse)
    - Adherence alert thresholds (configurable)
  
  - **Adherence Actions**:
    - Document adherence discussions
    - Prescribe adherence aids (if applicable)
    - Adjust medication regimen (if needed)
    - Refer to medication therapy management (if applicable)
    - Adherence counseling documentation

##### 3.2.8.3 Prescription Analytics

- **FR-P8.5**: System shall provide prescription analytics:
  - **Prescription Volume Analytics**:
    - Total prescriptions by time period
    - Prescriptions by provider
    - Prescriptions by medication
    - Prescriptions by medication class
    - Prescriptions by indication
    - Prescriptions by patient
    - Prescription trends over time
    - Prescription volume comparisons
  
  - **Prescription Pattern Analytics**:
    - Most prescribed medications
    - Prescription patterns by provider
    - Prescription patterns by specialty
    - Prescription patterns by patient population
    - Prescription patterns by condition
    - Seasonal prescription patterns
    - Prescription utilization patterns
  
  - **Prescription Quality Analytics**:
    - Prescription error rates
    - Prescription modification rates
    - Prescription cancellation rates
    - Drug interaction detection rates
    - Allergy alert rates
    - Alert override rates
    - E-prescribing adoption rates
    - Prescription completion rates
  
  - **Prescription Cost Analytics** (if data available):
    - Prescription costs
    - Cost by medication
    - Cost by medication class
    - Cost trends
    - Generic vs. brand utilization
    - Formulary compliance
    - Cost savings opportunities

##### 3.2.8.4 Prescription Reporting

- **FR-P8.6**: System shall provide comprehensive prescription reports:
  - **Patient-Level Reports**:
    - Complete prescription history for patient
    - Active prescriptions
    - Historical prescriptions
    - Prescriptions by medication
    - Prescriptions by provider
    - Prescriptions by pharmacy
    - Prescription timeline
    - Medication adherence report
    - Prescription summary report
  
  - **Provider-Level Reports**:
    - Prescriptions by provider
    - Prescription volume by provider
    - Prescription types by provider
    - Prescription patterns by provider
    - Prescription quality metrics by provider
    - Prescription compliance by provider
    - Provider prescribing trends
    - Provider comparison reports
  
  - **Medication-Level Reports**:
    - Prescriptions by medication
    - Medication utilization
    - Medication trends
    - Medication adherence
    - Medication safety (interactions, allergies)
    - Medication cost (if available)
    - Medication effectiveness (if data available)
  
  - **Pharmacy-Level Reports**:
    - Prescriptions by pharmacy
    - Prescription volume by pharmacy
    - Fill rates by pharmacy
    - Refill rates by pharmacy
    - Pharmacy performance metrics
  
  - **Clinical Reports**:
    - Prescriptions by indication/diagnosis
    - Prescriptions by medication class
    - Prescription patterns by condition
    - Medication therapy management reports
    - Clinical quality measures
  
  - **Quality Reports**:
    - Prescription error rates
    - Prescription modification rates
    - Prescription cancellation rates
    - Drug interaction detection
    - Allergy detection
    - E-prescribing adoption
    - Prescription completion
    - Quality measure compliance
  
  - **Regulatory Reports**:
    - Controlled substance prescriptions
    - Controlled substance compliance
    - PDMP query compliance
    - State reporting requirements
    - Federal reporting requirements
    - Regulatory compliance metrics

- **FR-P8.7**: System shall support report customization and export:
  - **Report Customization**:
    - Select report type
    - Select date range
    - Select filters (provider, medication, patient, etc.)
    - Select data elements to include
    - Customize report format
    - Save report configurations
    - Report templates
  
  - **Report Export**:
    - Export to PDF
    - Export to Excel/CSV
    - Export to Word
    - Export to text file
    - Export to XML
    - Export with formatting
    - Export without formatting
    - Scheduled exports (if applicable)
  
  - **Report Distribution**:
    - Email reports
    - Print reports
    - Save reports
    - Share reports (with authorization)
    - Report access controls

##### 3.2.8.5 Audit Trail and Activity Logging

- **FR-P8.8**: System shall maintain comprehensive audit trail:
  - **Audited Activities**:
    - Prescription creation
    - Prescription modification
    - Prescription signing
    - Prescription transmission
    - Prescription cancellation
    - Prescription replacement
    - Prescription refill requests
    - Prescription refill approvals
    - Prescription refill denials
    - Prescription viewing (if required)
    - Prescription printing/exporting
    - Alert overrides
    - PDMP queries
    - Controlled substance activities
  
  - **Audit Trail Information**:
    - Action type
    - Action date and time
    - Action user (provider, staff)
    - User role
    - IP address or location (if available)
    - Action details
    - Previous values (for modifications)
    - New values (for modifications)
    - Reasons (for cancellations, denials)
    - Related prescriptions
    - Patient information
    - Prescription information
  
  - **Audit Trail Features**:
    - Complete audit trail maintained
    - Audit trail cannot be modified
    - Audit trail cannot be deleted
    - Audit trail searchable
    - Audit trail filterable
    - Audit trail exportable
    - Audit trail retained per regulatory requirements
    - Audit trail accessible for compliance audits

- **FR-P8.9**: System shall provide audit trail reporting:
  - **Audit Reports**:
    - Audit trail by user
    - Audit trail by action type
    - Audit trail by date range
    - Audit trail by patient
    - Audit trail by prescription
    - Audit trail by provider
    - Complete audit trail
    - Filtered audit trail
  
  - **Audit Analytics**:
    - User activity patterns
    - Action frequency
    - Audit trail trends
    - Compliance metrics
    - Security event analysis
  
  - **Audit Trail Access**:
    - Role-based access to audit trails
    - Audit trail viewing permissions
    - Audit trail export permissions
    - Audit trail reporting permissions
    - Audit trail access logged

##### 3.2.8.6 Prescription Data Quality and Completeness

- **FR-P8.10**: System shall track prescription data quality:
  - **Data Quality Metrics**:
    - Prescription completeness
    - Required fields populated
    - Data accuracy
    - Data consistency
    - Missing information
    - Incomplete prescriptions
    - Data quality scores
  
  - **Data Quality Reports**:
    - Prescriptions with missing information
    - Prescriptions with incomplete data
    - Prescriptions requiring review
    - Data quality by provider
    - Data quality by medication
    - Data quality trends
    - Data quality improvement recommendations
  
  - **Data Quality Improvement**:
    - Identify data quality issues
    - Data quality alerts
    - Data quality reminders
    - Data quality training (if applicable)
    - Data quality monitoring

##### 3.2.8.7 Prescription Trend Analysis

- **FR-P8.11**: System shall provide prescription trend analysis:
  - **Trend Analysis Types**:
    - Prescription volume trends over time
    - Medication utilization trends
    - Prescription pattern trends
    - Adherence trends
    - Cost trends (if available)
    - Quality metric trends
    - Provider prescribing trends
    - Patient population trends
  
  - **Trend Visualization**:
    - Line graphs for trends
    - Bar charts for comparisons
    - Pie charts for distributions
    - Heat maps for patterns
    - Trend indicators (increasing, decreasing, stable)
    - Trend predictions (if applicable)
  
  - **Trend Reporting**:
    - Trend reports by time period
    - Trend comparisons
    - Trend analysis by category
    - Trend insights and recommendations

##### 3.2.8.8 Error Handling and Recovery

- **FR-P8.12**: System shall implement comprehensive error handling for prescription history and reporting:
  - **History Retrieval Error Handling**:
    - **History Query Failures**:
      - Handle history database query failures
      - Handle history query timeouts
      - Handle large result set errors
      - Display error: "Unable to retrieve prescription history. Please try again."
      - Provide retry mechanism
      - Implement pagination for large result sets
      - Cache recent history if available
      - Log history retrieval errors
    
    - **History Data Corruption**:
      - Handle corrupted history data
      - Handle missing history records
      - Display warning: "Some history data may be incomplete."
      - Log data corruption errors
  
  - **Adherence Tracking Error Handling**:
    - **Adherence Calculation Failures**:
      - Handle adherence calculation errors
      - Handle missing pharmacy data for adherence
      - Handle adherence data synchronization failures
      - Display warning: "Adherence data may be incomplete or inaccurate."
      - Log adherence calculation errors
    
    - **Adherence Data Retrieval Failures**:
      - Handle pharmacy data retrieval failures
      - Handle adherence data query failures
      - Queue adherence calculations for retry
      - Log retrieval errors
  
  - **Analytics Error Handling**:
    - **Analytics Calculation Failures**:
      - Handle analytics query failures
      - Handle analytics calculation timeouts
      - Handle large dataset errors
      - Display error: "Unable to calculate analytics. Please try again or use date filters."
      - Implement analytics pagination or chunking
      - Log analytics errors
    
    - **Analytics Data Quality Errors**:
      - Handle missing data for analytics
      - Handle inconsistent data for analytics
      - Display warning: "Analytics may be incomplete due to missing data."
      - Log data quality errors
  
  - **Reporting Error Handling**:
    - **Report Generation Failures**:
      - Handle report query failures
      - Handle report generation timeouts
      - Handle report format errors
      - Handle large report errors
      - Display error: "Unable to generate report. Please try again or use filters to reduce size."
      - Implement report pagination or chunking
      - Retry report generation automatically
      - Log report generation errors
    
    - **Report Export Failures**:
      - Handle report export format errors
      - Handle report export file size errors
      - Handle report export permission errors
      - Display error: "Unable to export report. Please try again."
      - Log export errors
    
    - **Scheduled Report Failures**:
      - Handle scheduled report generation failures
      - Handle scheduled report delivery failures
      - Queue scheduled reports for retry
      - Log scheduled report errors
  
  - **Audit Trail Error Handling**:
    - **Audit Trail Write Failures**:
      - Handle audit trail write failures
      - Handle audit trail database errors
      - Retry audit trail writes automatically
      - Queue audit trail writes for retry
      - Log audit trail write errors
    
    - **Audit Trail Retrieval Failures**:
      - Handle audit trail query failures
      - Handle audit trail query timeouts
      - Display error: "Unable to retrieve audit trail. Please try again."
      - Provide retry mechanism
      - Log audit trail retrieval errors
    
    - **Audit Trail Corruption**:
      - Handle audit trail data corruption
      - Handle missing audit trail records
      - Display warning: "Some audit trail information may be unavailable."
      - Log corruption errors
  
  - **Data Quality Error Handling**:
    - **Data Quality Check Failures**:
      - Handle data quality check processing failures
      - Handle data quality rule evaluation errors
      - Display warning: "Data quality checks may be incomplete."
      - Log data quality check errors
    
    - **Data Quality Report Failures**:
      - Handle data quality report generation failures
      - Handle data quality report delivery failures
      - Queue data quality reports for retry
      - Log data quality report errors
  
  - **Trend Analysis Error Handling**:
    - **Trend Calculation Failures**:
      - Handle trend calculation errors
      - Handle trend query failures
      - Handle trend calculation timeouts
      - Display error: "Unable to calculate trends. Please try again."
      - Log trend calculation errors
    
    - **Trend Data Quality Errors**:
      - Handle missing data for trend analysis
      - Handle inconsistent data for trend analysis
      - Display warning: "Trend analysis may be incomplete."
      - Log trend data quality errors
  
  - **System Error Handling**:
    - **Database Errors**:
      - Handle database connection failures
      - Handle database timeout errors
      - Handle database constraint violations
      - Display user-friendly error messages
      - Retry database operations automatically
      - Log database errors with context
    
    - **Network Errors**:
      - Handle network connectivity failures
      - Handle network timeout errors
      - Display error: "Network connection lost. Please check your connection."
      - Retry network operations automatically
      - Queue operations for retry when connection restored
      - Log network errors
    
    - **Application Errors**:
      - Handle application crashes during history/reporting operations
      - Recover operation state after crash
      - Display recovery message
      - Log application errors with stack traces
  
  - **Recovery Mechanisms**:
    - **Retry Logic**:
      - Automatic retry for transient errors (up to 3 attempts)
      - Exponential backoff for retries
      - Manual retry option for all errors
      - Display retry status to user
    
    - **Error Recovery Workflow**:
      - Provide "Retry" button for transient errors
      - Provide "Cancel" option to discard operations
      - Maintain operation state during error recovery
      - Log all recovery attempts
  
  - **Error Logging and Reporting**:
    - Log all errors with full context:
      - Error type and message
      - Report type (if applicable)
      - User ID and role
      - Action attempted
      - Timestamp
      - System state
      - Stack trace (for application errors)
    - Generate error reports for system administrators
    - Alert administrators for critical errors
    - Track error trends and patterns
    - Monitor history and reporting performance

#### 3.2.9 Integration with Patient Records

##### 3.2.9.1 Medication List Integration

- **FR-P9.1**: System shall automatically update patient's medication list:
  - **Automatic Medication List Updates**:
    - Add medication to current medication list when prescription is created
    - Update medication information when prescription is modified
    - Remove medication from current list when prescription is discontinued
    - Update medication status when prescription is cancelled
    - Update medication status when prescription expires
    - Update medication information when prescription is replaced
    - Real-time medication list updates
    - Synchronization between prescription system and medication list
  
  - **Medication List Information from Prescriptions**:
    - Medication name (generic and brand)
    - Dosage strength and form
    - Quantity
    - Frequency/schedule
    - Route of administration
    - Special instructions
    - Start date
    - End date (if applicable)
    - Prescribing provider
    - Prescription number
    - Link to prescription
    - Medication status (Active, Discontinued, On Hold, etc.)
    - Number of refills authorized
    - Remaining refills
  
  - **Medication List Status Management**:
    - Active medications from active prescriptions
    - Discontinued medications from cancelled/discontinued prescriptions
    - Medications on hold from prescriptions on hold
    - Completed medications from completed prescriptions
    - Expired medications from expired prescriptions
    - Status synchronization

- **FR-P9.2**: System shall handle medication list conflicts and reconciliation:
  - **Conflict Detection**:
    - Detect duplicate medications
    - Detect conflicting medication information
    - Detect medications with different sources (prescription vs. manual entry)
    - Alert for medication list conflicts
    - Provide conflict resolution options
  
  - **Medication Reconciliation**:
    - Reconcile prescription medications with medication list
    - Reconcile manual medications with prescription medications
    - Resolve medication list discrepancies
    - Medication reconciliation during encounters
    - Medication reconciliation documentation

##### 3.2.9.2 Problem/Diagnosis Integration

- **FR-P9.3**: System shall link prescriptions to diagnoses/problems:
  - **Prescription-Problem Linking**:
    - Link prescription to indication/diagnosis during prescription creation
    - Link prescription to problem from problem list
    - Link prescription to diagnosis from diagnosis list
    - Display linked problems with prescriptions
    - Display linked prescriptions with problems
    - Support for multiple problems per prescription
    - Support for multiple prescriptions per problem
  
  - **Problem-Based Prescribing**:
    - Prescribe medications for specific problems
    - Display problems when prescribing
    - Suggest medications based on problems (if applicable)
    - Track which medications treat which problems
    - Problem-based medication reporting
  
  - **Problem-Prescription Display**:
    - Display prescriptions linked to each problem
    - Display problems linked to each prescription
    - Display problem-prescription relationships
    - Problem-based prescription views
    - Prescription-based problem views

##### 3.2.9.3 Patient Summary Integration

- **FR-P9.4**: System shall include prescriptions in patient summary views:
  - **Patient Summary Display**:
    - Display active prescriptions in patient summary/dashboard
    - Display recent prescriptions in patient summary
    - Display prescription count in patient summary
    - Display prescription status in patient summary
    - Display prescription alerts in patient summary (if applicable)
    - Display medication adherence in patient summary (if available)
  
  - **Summary Information**:
    - Number of active prescriptions
    - Number of recent prescriptions
    - Prescription status summary
    - Medication adherence summary (if available)
    - Prescription alerts summary
    - Quick access to prescription list
    - Quick access to prescription details
  
  - **Summary Customization**:
    - User-configurable prescription display in summary
    - Show/hide prescription information
    - Summary view preferences
    - Summary information organization

##### 3.2.9.4 Allergy Integration

- **FR-P9.5**: System shall integrate prescriptions with allergy information:
  - **Allergy Checking Integration**:
    - Check prescriptions against patient's allergy list
    - Display allergies prominently during prescription creation
    - Alert for drug-allergy interactions
    - Prevent prescription of medications with known allergies (with override)
    - Allergy information in prescription context
  
  - **Allergy-Prescription Display**:
    - Display allergies with prescriptions
    - Display prescriptions with allergy alerts
    - Allergy information in prescription views
    - Allergy history with prescription history
  
  - **Allergy Updates from Prescriptions**:
    - Add medication allergies identified during prescribing
    - Update allergy information based on prescription reactions
    - Link prescription reactions to allergies
    - Allergy documentation from prescription workflow

##### 3.2.9.5 Encounter Integration

- **FR-P9.6**: System shall integrate prescriptions with patient encounters:
  - **Encounter-Prescription Linking**:
    - Link prescriptions to specific encounters/visits
    - Display prescriptions created during encounter
    - Display prescriptions in encounter view
    - Display encounter information with prescriptions
    - Track which prescriptions were created during which encounters
    - Encounter-based prescription reporting
  
  - **Encounter-Based Prescribing**:
    - Create prescriptions during encounter
    - Prescribe medications for encounter diagnoses
    - Link prescriptions to encounter problems
    - Encounter-based prescription workflow
    - Encounter-based prescription documentation
  
  - **Encounter-Prescription Display**:
    - Display prescriptions in encounter timeline
    - Display encounter information with prescriptions
    - Encounter-based prescription views
    - Prescription-based encounter views

##### 3.2.9.6 Clinical Notes Integration

- **FR-P9.7**: System shall integrate prescriptions with clinical notes:
  - **Note-Prescription Linking**:
    - Link prescriptions to clinical notes
    - Insert prescription information into notes
    - Display prescriptions mentioned in notes
    - Display notes related to prescriptions
    - Prescription information in note templates
    - Note-based prescription documentation
  
  - **Prescription Documentation in Notes**:
    - Document prescriptions in clinical notes
    - Document prescription rationale in notes
    - Document prescription changes in notes
    - Document prescription discontinuation in notes
    - Prescription information auto-populated in notes (if applicable)
  
  - **Note-Prescription Display**:
    - Display prescriptions in note context
    - Display notes in prescription context
    - Note-based prescription views
    - Prescription-based note views

##### 3.2.9.7 Lab Results Integration

- **FR-P9.8**: System shall integrate prescriptions with laboratory results:
  - **Lab-Prescription Linking**:
    - Link prescriptions to relevant lab results
    - Display lab results when prescribing medications requiring monitoring
    - Display medications when viewing monitoring lab results
    - Lab-based medication monitoring
    - Medication-based lab monitoring
  
  - **Medication Monitoring Integration**:
    - Alert for medications requiring lab monitoring
    - Display lab results for medication monitoring
    - Track medication monitoring compliance
    - Medication monitoring reminders
    - Lab result interpretation for medications
  
  - **Lab-Prescription Display**:
    - Display lab results with prescriptions
    - Display prescriptions with lab monitoring
    - Lab-based prescription views
    - Prescription-based lab views

##### 3.2.9.8 Vital Signs Integration

- **FR-P9.9**: System shall integrate prescriptions with vital signs:
  - **Vital Signs-Prescription Linking**:
    - Consider vital signs when prescribing medications affecting vital signs
    - Display vital signs when prescribing blood pressure medications
    - Display vital signs when prescribing medications affecting heart rate
    - Vital signs-based medication dosing (if applicable)
    - Medication-based vital signs monitoring
  
  - **Vital Signs-Prescription Display**:
    - Display vital signs with prescriptions
    - Display prescriptions affecting vital signs
    - Vital signs trends with medication changes
    - Medication-based vital signs views

##### 3.2.9.9 Medical History Integration

- **FR-P9.10**: System shall integrate prescriptions with medical history:
  - **History-Prescription Linking**:
    - Consider medical history when prescribing
    - Display relevant medical history during prescribing
    - Link prescriptions to relevant medical history
    - Medical history-based medication selection
    - Medication history integration
  
  - **Medication History Integration**:
    - Display past medications when prescribing
    - Display medication history with current prescriptions
    - Consider medication history for interactions
    - Medication history-based prescribing decisions
    - Historical medication-prescription views

##### 3.2.9.10 Patient Timeline Integration

- **FR-P9.11**: System shall integrate prescriptions into patient timeline:
  - **Timeline Display**:
    - Prescriptions displayed in chronological patient timeline
    - Prescription events in timeline (created, filled, cancelled, etc.)
    - Prescription timeline with other events (encounters, labs, etc.)
    - Timeline filtering by prescription events
    - Timeline navigation to prescriptions
  
  - **Timeline Information**:
    - Prescription creation dates
    - Prescription fill dates
    - Prescription modification dates
    - Prescription cancellation dates
    - Prescription refill dates
    - Prescription-related events
    - Timeline relationships between prescriptions and other events

##### 3.2.9.11 Data Synchronization

- **FR-P9.12**: System shall ensure data synchronization:
  - **Real-Time Synchronization**:
    - Real-time updates between prescription system and patient records
    - Immediate medication list updates
    - Immediate status updates
    - Synchronization of prescription changes
    - Synchronization of medication list changes
  
  - **Data Consistency**:
    - Maintain consistency between prescriptions and medication list
    - Maintain consistency between prescriptions and problems
    - Maintain consistency between prescriptions and encounters
    - Resolve data inconsistencies
    - Data validation and reconciliation
  
  - **Synchronization Monitoring**:
    - Monitor synchronization status
    - Detect synchronization issues
    - Alert for synchronization failures
    - Synchronization error recovery
    - Synchronization reporting

##### 3.2.9.12 Error Handling and Recovery

- **FR-P9.13**: System shall implement comprehensive error handling for prescription integration with patient records:
  - **Medication List Integration Error Handling**:
    - **Medication List Update Failures**:
      - Handle medication list update failures
      - Handle medication list synchronization conflicts
      - Handle concurrent medication list edits
      - Display warning: "Medication list may be out of sync."
      - Queue medication list updates for retry
      - Provide conflict resolution interface
      - Log medication list update errors
    
    - **Medication List Retrieval Failures**:
      - Handle medication list query failures
      - Handle medication list query timeouts
      - Display error: "Unable to retrieve medication list. Please try again."
      - Use cached medication list if available
      - Log retrieval errors
    
    - **Medication List Conflict Errors**:
      - Handle duplicate medication conflicts
      - Handle conflicting medication information
      - Display conflict resolution interface
      - Allow user to resolve conflicts
      - Log conflict errors
  
  - **Problem/Diagnosis Integration Error Handling**:
    - **Link Creation Failures**:
      - Handle prescription-diagnosis link creation failures
      - Handle link save failures
      - Retry link creation automatically
      - Queue links for retry if persistent failure
      - Log link creation errors
    
    - **Link Retrieval Failures**:
      - Handle link query failures
      - Handle link query timeouts
      - Display warning: "Prescription-diagnosis links may be incomplete."
      - Log retrieval errors
  
  - **Patient Summary Integration Error Handling**:
    - **Summary Update Failures**:
      - Handle patient summary update failures
      - Handle summary refresh failures
      - Display warning: "Patient summary may not reflect latest prescriptions."
      - Queue summary updates for retry
      - Log summary update errors
    
    - **Summary Data Retrieval Failures**:
      - Handle summary data query failures
      - Handle summary data query timeouts
      - Use cached summary data if available
      - Log retrieval errors
  
  - **Allergy Integration Error Handling**:
    - **Allergy Check Failures**:
      - Handle allergy list retrieval failures
      - Handle allergy check processing failures
      - Display warning: "Allergy checking may be incomplete."
      - Queue allergy checks for retry
      - Log allergy check errors
    
    - **Allergy Update Failures**:
      - Handle allergy list update failures
      - Handle allergy synchronization failures
      - Queue allergy updates for retry
      - Log allergy update errors
  
  - **Encounter Integration Error Handling**:
    - **Encounter Link Failures**:
      - Handle prescription-encounter link creation failures
      - Handle link save failures
      - Retry link creation automatically
      - Queue links for retry if persistent failure
      - Log link creation errors
    
    - **Encounter Data Retrieval Failures**:
      - Handle encounter data query failures
      - Handle encounter data query timeouts
      - Display warning: "Encounter data may be incomplete."
      - Log retrieval errors
  
  - **Clinical Notes Integration Error Handling**:
    - **Note Link Failures**:
      - Handle prescription-note link creation failures
      - Handle link save failures
      - Retry link creation automatically
      - Queue links for retry if persistent failure
      - Log link creation errors
    
    - **Note Data Retrieval Failures**:
      - Handle note data query failures
      - Handle note data query timeouts
      - Display warning: "Note data may be incomplete."
      - Log retrieval errors
  
  - **Lab Results Integration Error Handling**:
    - **Lab Data Retrieval Failures**:
      - Handle lab data query failures
      - Handle lab data query timeouts
      - Display warning: "Lab results may be incomplete."
      - Log retrieval errors
    
    - **Lab Data Synchronization Failures**:
      - Handle lab data synchronization failures
      - Queue lab data synchronization for retry
      - Log synchronization errors
  
  - **Vital Signs Integration Error Handling**:
    - **Vital Signs Data Retrieval Failures**:
      - Handle vital signs query failures
      - Handle vital signs query timeouts
      - Display warning: "Vital signs data may be incomplete."
      - Log retrieval errors
    
    - **Vital Signs Synchronization Failures**:
      - Handle vital signs synchronization failures
      - Queue vital signs synchronization for retry
      - Log synchronization errors
  
  - **Medical History Integration Error Handling**:
    - **Medical History Data Retrieval Failures**:
      - Handle medical history query failures
      - Handle medical history query timeouts
      - Display warning: "Medical history may be incomplete."
      - Log retrieval errors
    
    - **Medical History Synchronization Failures**:
      - Handle medical history synchronization failures
      - Queue medical history synchronization for retry
      - Log synchronization errors
  
  - **Patient Timeline Integration Error Handling**:
    - **Timeline Update Failures**:
      - Handle timeline update failures
      - Handle timeline refresh failures
      - Display warning: "Patient timeline may not reflect latest prescriptions."
      - Queue timeline updates for retry
      - Log timeline update errors
    
    - **Timeline Data Retrieval Failures**:
      - Handle timeline query failures
      - Handle timeline query timeouts
      - Display error: "Unable to retrieve timeline. Please try again."
      - Provide retry mechanism
      - Log retrieval errors
  
  - **Data Synchronization Error Handling**:
    - **Synchronization Failures**:
      - Handle data synchronization processing failures
      - Handle synchronization conflicts
      - Handle synchronization timeouts
      - Display warning: "Data synchronization may be incomplete."
      - Queue synchronization for retry
      - Log synchronization errors
    
    - **Synchronization Conflict Errors**:
      - Handle concurrent edit conflicts
      - Handle data version conflicts
      - Provide conflict resolution interface
      - Allow user to resolve conflicts
      - Log conflict errors
    
    - **Synchronization Monitoring Failures**:
      - Handle synchronization status check failures
      - Handle synchronization monitoring errors
      - Log monitoring errors
  
  - **System Error Handling**:
    - **Database Errors**:
      - Handle database connection failures
      - Handle database timeout errors
      - Handle database constraint violations
      - Display user-friendly error messages
      - Retry database operations automatically
      - Log database errors with context
    
    - **Network Errors**:
      - Handle network connectivity failures
      - Handle network timeout errors
      - Display error: "Network connection lost. Please check your connection."
      - Retry network operations automatically
      - Queue operations for retry when connection restored
      - Log network errors
    
    - **Application Errors**:
      - Handle application crashes during integration operations
      - Recover integration state after crash
      - Display recovery message
      - Log application errors with stack traces
  
  - **Recovery Mechanisms**:
    - **Retry Logic**:
      - Automatic retry for transient errors (up to 3 attempts)
      - Exponential backoff for retries
      - Manual retry option for all errors
      - Display retry status to user
    
    - **Error Recovery Workflow**:
      - Provide "Retry" button for transient errors
      - Provide "Cancel" option to discard operations
      - Maintain integration state during error recovery
      - Log all recovery attempts
    
    - **Fallback Mechanisms**:
      - Use cached data if available when retrieval fails
      - Use partial data if full data unavailable
      - Display fallback status to user
  
  - **Error Logging and Reporting**:
    - Log all errors with full context:
      - Error type and message
      - Integration type (medication list, diagnosis, etc.)
      - Patient ID
      - Prescription ID (if applicable)
      - User ID and role
      - Action attempted
      - Timestamp
      - System state
      - Stack trace (for application errors)
    - Generate error reports for system administrators
    - Alert administrators for critical errors
    - Track error trends and patterns
    - Monitor integration performance

### 3.3 Data Requirements

#### 3.3.1 Data Model Overview

The Prescription Management feature requires a comprehensive data model to support all prescription-related information, medication data, pharmacy information, refill management, and audit trails. The data model shall follow relational database principles with proper normalization, referential integrity, and data consistency. All entities shall support audit trails, versioning where applicable, and soft deletion for data retention compliance.

##### 3.3.1.1 Data Model Principles
- **Normalization**: Data shall be normalized to third normal form (3NF) or higher to minimize redundancy
- **Referential Integrity**: Foreign key relationships shall be enforced to maintain data consistency
- **Audit Trails**: All entities shall support audit logging of create, update, and delete operations
- **Soft Deletion**: Critical entities shall support soft deletion (mark as deleted, retain data) for compliance
- **Versioning**: Entities requiring version control (prescriptions) shall maintain version history
- **Data Standards**: All coded data shall use standard terminologies (RxNorm, NDC, NCPDP SCRIPT, etc.)
- **Uniqueness**: Primary keys and unique constraints shall ensure data integrity
- **Indexing**: Appropriate indexes shall be created for performance optimization

#### 3.3.2 Core Entity Definitions

##### 3.3.2.1 Prescription Entity

**Purpose**: Stores comprehensive prescription information including medication details, dosing instructions, refill authorization, and prescription lifecycle data.

**Primary Key**: PrescriptionID (Unique, Auto-increment or GUID)

**Attributes**:
- **PrescriptionID** (Primary Key, Unique, Required)
  - Data Type: Integer or GUID
  - Constraints: Unique, Not Null, Auto-increment
  
- **PrescriptionNumber** (Unique, Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Unique, Not Null, Indexed
  - Format: Organization-specific format
  
- **PatientID** (Foreign Key, Required)
  - Data Type: Integer (Foreign Key to Patient table)
  - Constraints: Not Null, Indexed
  
- **ProviderID** (Foreign Key, Required)
  - Data Type: Integer (Foreign Key to Provider table)
  - Constraints: Not Null, Indexed
  
- **EncounterID** (Optional)
  - Data Type: Integer (Foreign Key to Encounter table)
  - Indexed
  
- **MedicationID** (Optional)
  - Data Type: Integer (Foreign Key to Medication table)
  - Indexed
  
- **NDCCode** (Optional)
  - Data Type: String (VARCHAR, 20)
  - Constraints: NDC format validation
  
- **RxNormCode** (Optional)
  - Data Type: String (VARCHAR, 50)
  
- **GenericName** (Required)
  - Data Type: String (VARCHAR, 200)
  - Constraints: Not Null
  - Indexed
  
- **BrandName** (Optional)
  - Data Type: String (VARCHAR, 200)
  
- **DosageStrength** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null
  - Example: "10 mg", "500 mg"
  
- **DosageForm** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null
  - Example: "Tablet", "Capsule", "Liquid"
  
- **Quantity** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null
  - Example: "30", "60", "90"
  
- **QuantityNumeric** (Optional)
  - Data Type: Integer
  - For calculations and validations
  
- **Route** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null
  - Example: "Oral", "IV", "Topical"
  
- **Frequency** (Required)
  - Data Type: String (VARCHAR, 100)
  - Constraints: Not Null
  - Example: "Once daily", "Twice daily"
  
- **TimingInstructions** (Optional)
  - Data Type: String (VARCHAR, 200)
  - Example: "With meals", "Before meals"
  
- **SpecialInstructions** (Optional)
  - Data Type: Text
  
- **Duration** (Optional)
  - Data Type: String (VARCHAR, 100)
  - Example: "10 days", "3 months", "Ongoing"
  
- **DurationDays** (Optional)
  - Data Type: Integer
  - For calculations
  
- **StartDate** (Required)
  - Data Type: Date
  - Constraints: Not Null, Valid date
  - Indexed
  
- **EndDate** (Optional)
  - Data Type: Date
  - Constraints: Valid date, Must be after StartDate if both provided
  
- **RefillsAuthorized** (Required)
  - Data Type: Integer
  - Constraints: Not Null, Default: 0, Range: 0-11
  - Indexed
  
- **RefillsRemaining** (Required)
  - Data Type: Integer
  - Constraints: Not Null, Default: 0
  - Indexed
  
- **SubstitutionAllowed** (Required)
  - Data Type: Boolean
  - Constraints: Not Null, Default: True
  - DAW Code: 0 = Substitution Allowed, 1 = Do Not Substitute
  
- **DAWCode** (Optional)
  - Data Type: String (VARCHAR, 10)
  - Constraints: Enum (0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
  
- **PharmacyID** (Optional)
  - Data Type: Integer (Foreign Key to Pharmacy table)
  - Indexed
  
- **PharmacyName** (Optional)
  - Data Type: String (VARCHAR, 200)
  
- **PharmacyNPI** (Optional)
  - Data Type: String (VARCHAR, 10)
  
- **PrescriptionStatus** (Required)
  - Data Type: String (VARCHAR, 20)
  - Constraints: Not Null, Enum (Draft, Signed, Pending, Transmitting, Sent, Received, Filled, Partially Filled, Cancelled, Expired, Refilled, Replaced, On Hold)
  - Default: Draft
  - Indexed
  
- **IsControlledSubstance** (Required)
  - Data Type: Boolean
  - Constraints: Not Null, Default: False
  - Indexed
  
- **ControlledSubstanceSchedule** (Optional)
  - Data Type: String (VARCHAR, 10)
  - Constraints: Enum (I, II, III, IV, V) if IsControlledSubstance = True
  
- **DEANumber** (Optional)
  - Data Type: String (VARCHAR, 10)
  - Constraints: Required if IsControlledSubstance = True, DEA format validation
  
- **ClinicalIndication** (Optional)
  - Data Type: Text
  
- **ProblemID** (Optional)
  - Data Type: Integer (Foreign Key to Diagnosis table)
  - Link to problem/diagnosis this prescription treats
  
- **IsSigned** (Required)
  - Data Type: Boolean
  - Constraints: Not Null, Default: False
  - Indexed
  
- **SignedDate** (Optional)
  - Data Type: DateTime
  - Constraints: Required if IsSigned = True
  
- **ElectronicSignature** (Optional)
  - Data Type: String (VARCHAR, 500)
  - Constraints: Required if IsSigned = True
  
- **TransmissionMethod** (Optional)
  - Data Type: String (VARCHAR, 50)
  - Example: "Electronic", "Fax", "Print"
  
- **TransmissionStatus** (Optional)
  - Data Type: String (VARCHAR, 20)
  - Example: "Pending", "Sent", "Received", "Failed"
  
- **SentDate** (Optional)
  - Data Type: DateTime
  - Indexed
  
- **ReceivedDate** (Optional)
  - Data Type: DateTime
  
- **FilledDate** (Optional)
  - Data Type: DateTime
  - Indexed
  
- **CancelledDate** (Optional)
  - Data Type: DateTime
  
- **CancellationReason** (Optional)
  - Data Type: Text
  
- **CancelledByUserID** (Optional)
  - Data Type: Integer (Foreign Key to User table)
  
- **VersionNumber** (Required)
  - Data Type: Integer
  - Constraints: Not Null, Default: 1
  
- **ParentPrescriptionID** (Optional, for replacements)
  - Data Type: Integer (Foreign Key to Prescription table)
  
- **ReplacementReason** (Optional)
  - Data Type: Text
  
- **Notes** (Optional)
  - Data Type: Text
  
- **Audit Fields**:
  - **CreatedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
    - Indexed
  - **CreatedByUserID** (Required)
    - Data Type: Integer (Foreign Key to User table)
    - Constraints: Not Null
  - **ModifiedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **ModifiedByUserID** (Required)
    - Data Type: Integer (Foreign Key to User table)
    - Constraints: Not Null
  - **IsDeleted** (Required)
    - Data Type: Boolean
    - Constraints: Not Null, Default: False
    - Indexed
  - **DeletedDate** (Optional)
    - Data Type: DateTime
  - **DeletedByUserID** (Optional)
    - Data Type: Integer (Foreign Key to User table)

**Indexes**:
- Primary Key: PrescriptionID
- Unique: PrescriptionNumber
- Index: PatientID
- Index: ProviderID
- Index: EncounterID
- Index: MedicationID
- Index: GenericName
- Index: PrescriptionStatus
- Index: IsControlledSubstance
- Index: ControlledSubstanceSchedule
- Index: IsSigned
- Index: StartDate
- Index: SentDate
- Index: FilledDate
- Index: RefillsAuthorized
- Index: RefillsRemaining
- Index: IsDeleted
- Composite Index: (PatientID, PrescriptionStatus) for active prescription queries
- Composite Index: (PatientID, StartDate) for chronological queries

**Relationships**:
- Many-to-One: Prescription → Patient
- Many-to-One: Prescription → Provider
- Many-to-One: Prescription → Encounter (optional)
- Many-to-One: Prescription → Medication (optional)
- Many-to-One: Prescription → Pharmacy (optional)
- Many-to-One: Prescription → Problem/Diagnosis (optional)
- Self-Referential: Prescription → Parent Prescription (for replacements)
- One-to-Many: Prescription → Prescription Refills
- One-to-Many: Prescription → Prescription Audit Logs

##### 3.3.2.2 Medication Entity (Drug Database)

**Purpose**: Stores comprehensive medication/drug information including drug identifiers, classifications, safety information, and dosing guidelines.

**Primary Key**: MedicationID (Unique, Auto-increment)

**Attributes**:
- **MedicationID** (Primary Key, Unique, Required)
  - Data Type: Integer
  - Constraints: Unique, Not Null, Auto-increment
  
- **NDCCode** (Optional, but recommended)
  - Data Type: String (VARCHAR, 20)
  - Constraints: Unique if provided, NDC format validation
  - Indexed
  
- **RxNormCode** (Optional, but recommended)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Unique if provided
  - Indexed
  
- **GenericName** (Required)
  - Data Type: String (VARCHAR, 200)
  - Constraints: Not Null
  - Indexed
  
- **BrandNames** (Optional)
  - Data Type: Text
  - Comma-separated or JSON array of brand names
  
- **DrugClass** (Optional)
  - Data Type: String (VARCHAR, 100)
  - Indexed
  
- **TherapeuticClass** (Optional)
  - Data Type: String (VARCHAR, 100)
  
- **DosageFormsAvailable** (Optional)
  - Data Type: Text
  - Comma-separated or JSON array
  
- **StrengthsAvailable** (Optional)
  - Data Type: Text
  - Comma-separated or JSON array
  
- **IsControlledSubstance** (Required)
  - Data Type: Boolean
  - Constraints: Not Null, Default: False
  - Indexed
  
- **ControlledSubstanceSchedule** (Optional)
  - Data Type: String (VARCHAR, 10)
  - Constraints: Enum (I, II, III, IV, V) if IsControlledSubstance = True
  - Indexed
  
- **PregnancyCategory** (Optional)
  - Data Type: String (VARCHAR, 10)
  - Example: "A", "B", "C", "D", "X", "N" (or new format)
  
- **LactationSafety** (Optional)
  - Data Type: String (VARCHAR, 50)
  
- **StandardDosageGuidelines** (Optional)
  - Data Type: Text
  
- **MaximumDailyDose** (Optional)
  - Data Type: Decimal(10,2)
  
- **MinimumEffectiveDose** (Optional)
  - Data Type: Decimal(10,2)
  
- **IsActive** (Required)
  - Data Type: Boolean
  - Constraints: Not Null, Default: True
  - Indexed
  
- **LastUpdated** (Required)
  - Data Type: DateTime
  - Constraints: Not Null, Default: Current timestamp

**Indexes**:
- Primary Key: MedicationID
- Unique: NDCCode (if provided)
- Unique: RxNormCode (if provided)
- Index: GenericName
- Index: DrugClass
- Index: IsControlledSubstance
- Index: ControlledSubstanceSchedule
- Index: IsActive

**Relationships**:
- One-to-Many: Medication → Prescriptions
- Many-to-Many: Medication → Drug Interactions (via Drug Interaction table)

##### 3.3.2.3 Pharmacy Entity

**Purpose**: Stores pharmacy information for prescription transmission and management.

**Primary Key**: PharmacyID (Unique, Auto-increment)

**Attributes**:
- **PharmacyID** (Primary Key, Unique, Required)
  - Data Type: Integer
  - Constraints: Unique, Not Null, Auto-increment
  
- **PharmacyName** (Required)
  - Data Type: String (VARCHAR, 200)
  - Constraints: Not Null
  - Indexed
  
- **NPI** (Optional, but recommended)
  - Data Type: String (VARCHAR, 10)
  - Constraints: Unique if provided, NPI format validation
  - Indexed
  
- **AddressLine1** (Required)
  - Data Type: String (VARCHAR, 200)
  - Constraints: Not Null
  
- **AddressLine2** (Optional)
  - Data Type: String (VARCHAR, 200)
  
- **City** (Required)
  - Data Type: String (VARCHAR, 100)
  - Constraints: Not Null
  
- **State** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null
  - Indexed
  
- **ZipCode** (Required)
  - Data Type: String (VARCHAR, 10)
  - Constraints: Not Null
  - Indexed
  
- **Country** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null, Default: "USA"
  
- **PhoneNumber** (Optional)
  - Data Type: String (VARCHAR, 20)
  
- **FaxNumber** (Optional)
  - Data Type: String (VARCHAR, 20)
  
- **Email** (Optional)
  - Data Type: String (VARCHAR, 255)
  - Constraints: Email format validation
  
- **SurescriptsID** (Optional)
  - Data Type: String (VARCHAR, 50)
  - For e-prescribing network integration
  
- **IsOnEPrescribingNetwork** (Optional)
  - Data Type: Boolean
  - Default: False
  - Indexed
  
- **IsDirectlyIntegrated** (Optional)
  - Data Type: Boolean
  - Default: False
  
- **IsActive** (Required)
  - Data Type: Boolean
  - Constraints: Not Null, Default: True
  - Indexed
  
- **Audit Fields**:
  - **CreatedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **ModifiedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **IsDeleted** (Required)
    - Data Type: Boolean
    - Constraints: Not Null, Default: False

**Indexes**:
- Primary Key: PharmacyID
- Unique: NPI (if provided)
- Index: PharmacyName
- Index: State
- Index: ZipCode
- Index: IsOnEPrescribingNetwork
- Index: IsActive
- Index: IsDeleted

**Relationships**:
- One-to-Many: Pharmacy → Prescriptions

##### 3.3.2.4 Prescription Refill Entity

**Purpose**: Stores refill request and approval information for prescriptions.

**Primary Key**: RefillID (Unique, Auto-increment)

**Attributes**:
- **RefillID** (Primary Key, Unique, Required)
  - Data Type: Integer
  - Constraints: Unique, Not Null, Auto-increment
  
- **PrescriptionID** (Foreign Key, Required)
  - Data Type: Integer (Foreign Key to Prescription table)
  - Constraints: Not Null, Indexed
  
- **RequestDate** (Required)
  - Data Type: DateTime
  - Constraints: Not Null, Default: Current timestamp
  - Indexed
  
- **RequestSource** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null, Enum (Pharmacy, Patient, Provider)
  - Indexed
  
- **RequestedByPharmacyID** (Optional)
  - Data Type: Integer (Foreign Key to Pharmacy table)
  - Required if RequestSource = Pharmacy
  
- **RequestedByPatientID** (Optional)
  - Data Type: Integer (Foreign Key to Patient table)
  - Required if RequestSource = Patient
  
- **RequestedByProviderID** (Optional)
  - Data Type: Integer (Foreign Key to Provider table)
  - Required if RequestSource = Provider
  
- **RequestedByUserID** (Optional)
  - Data Type: Integer (Foreign Key to User table)
  - For staff-initiated requests
  
- **ApprovalStatus** (Required)
  - Data Type: String (VARCHAR, 20)
  - Constraints: Not Null, Enum (Pending, Approved, Denied, Modified, Cancelled)
  - Default: Pending
  - Indexed
  
- **ApprovedDate** (Optional)
  - Data Type: DateTime
  - Required if ApprovalStatus = Approved
  
- **ApprovedByProviderID** (Optional)
  - Data Type: Integer (Foreign Key to Provider table)
  - Required if ApprovalStatus = Approved
  
- **DeniedDate** (Optional)
  - Data Type: DateTime
  - Required if ApprovalStatus = Denied
  
- **DeniedByProviderID** (Optional)
  - Data Type: Integer (Foreign Key to Provider table)
  - Required if ApprovalStatus = Denied
  
- **DenialReason** (Optional)
  - Data Type: Text
  - Required if ApprovalStatus = Denied
  
- **ModificationDetails** (Optional)
  - Data Type: Text
  - If ApprovalStatus = Modified
  
- **FilledDate** (Optional)
  - Data Type: DateTime
  - If refill was filled by pharmacy
  
- **QuantityDispensed** (Optional)
  - Data Type: String (VARCHAR, 50)
  - If refill was filled
  
- **Notes** (Optional)
  - Data Type: Text
  
- **Audit Fields**:
  - **CreatedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **CreatedByUserID** (Optional)
    - Data Type: Integer (Foreign Key to User table)
  - **ModifiedDate** (Required)
    - Data Type: DateTime
    - Constraints: Not Null, Default: Current timestamp
  - **ModifiedByUserID** (Optional)
    - Data Type: Integer (Foreign Key to User table)
  - **IsDeleted** (Required)
    - Data Type: Boolean
    - Constraints: Not Null, Default: False

**Indexes**:
- Primary Key: RefillID
- Index: PrescriptionID
- Index: RequestDate
- Index: RequestSource
- Index: ApprovalStatus
- Index: IsDeleted

**Relationships**:
- Many-to-One: Prescription Refill → Prescription
- Many-to-One: Prescription Refill → Pharmacy (optional)
- Many-to-One: Prescription Refill → Patient (optional)
- Many-to-One: Prescription Refill → Provider (optional)

##### 3.3.2.5 Drug Interaction Entity

**Purpose**: Stores drug-drug interaction information for medication safety checking.

**Primary Key**: InteractionID (Unique, Auto-increment)

**Attributes**:
- **InteractionID** (Primary Key, Unique, Required)
  - Data Type: Integer
  - Constraints: Unique, Not Null, Auto-increment
  
- **MedicationID1** (Required)
  - Data Type: Integer (Foreign Key to Medication table)
  - Constraints: Not Null
  - Indexed
  
- **MedicationID2** (Required)
  - Data Type: Integer (Foreign Key to Medication table)
  - Constraints: Not Null
  - Indexed
  
- **InteractionType** (Optional)
  - Data Type: String (VARCHAR, 100)
  - Example: "Pharmacokinetic", "Pharmacodynamic"
  
- **SeverityLevel** (Required)
  - Data Type: String (VARCHAR, 20)
  - Constraints: Not Null, Enum (Critical, Major, Moderate, Minor, Unknown)
  - Indexed
  
- **Description** (Required)
  - Data Type: Text
  - Constraints: Not Null
  
- **ClinicalSignificance** (Optional)
  - Data Type: Text
  
- **ManagementRecommendations** (Optional)
  - Data Type: Text
  
- **EvidenceLevel** (Optional)
  - Data Type: String (VARCHAR, 50)
  
- **IsActive** (Required)
  - Data Type: Boolean
  - Constraints: Not Null, Default: True
  - Indexed
  
- **LastUpdated** (Required)
  - Data Type: DateTime
  - Constraints: Not Null, Default: Current timestamp

**Indexes**:
- Primary Key: InteractionID
- Index: MedicationID1
- Index: MedicationID2
- Index: SeverityLevel
- Index: IsActive
- Composite Index: (MedicationID1, MedicationID2) for interaction lookups

**Relationships**:
- Many-to-One: Drug Interaction → Medication (Medication 1)
- Many-to-One: Drug Interaction → Medication (Medication 2)

##### 3.3.2.6 Prescription Audit Log Entity

**Purpose**: Stores comprehensive audit trail of all prescription-related activities.

**Primary Key**: AuditLogID (Unique, Auto-increment)

**Attributes**:
- **AuditLogID** (Primary Key, Unique, Required)
  - Data Type: Integer
  - Constraints: Unique, Not Null, Auto-increment
  
- **PrescriptionID** (Foreign Key, Required)
  - Data Type: Integer (Foreign Key to Prescription table)
  - Constraints: Not Null, Indexed
  
- **ActionType** (Required)
  - Data Type: String (VARCHAR, 50)
  - Constraints: Not Null, Enum (Created, Modified, Signed, Sent, Received, Filled, Cancelled, Replaced, Refill Requested, Refill Approved, Refill Denied, Viewed, Printed, Exported, Alert Overridden, PDMP Queried)
  - Indexed
  
- **UserID** (Required)
  - Data Type: Integer (Foreign Key to User table)
  - Constraints: Not Null
  - Indexed
  
- **ProviderID** (Optional)
  - Data Type: Integer (Foreign Key to Provider table)
  - Indexed
  
- **ActionTimestamp** (Required)
  - Data Type: DateTime
  - Constraints: Not Null, Default: Current timestamp
  - Indexed
  
- **IPAddress** (Optional)
  - Data Type: String (VARCHAR, 50)
  
- **Location** (Optional)
  - Data Type: String (VARCHAR, 200)
  
- **PreviousValue** (Optional)
  - Data Type: Text
  - For modifications, stores previous value
  
- **NewValue** (Optional)
  - Data Type: Text
  - For modifications, stores new value
  
- **FieldName** (Optional)
  - Data Type: String (VARCHAR, 100)
  - For modifications, specifies which field changed
  
- **Reason** (Optional)
  - Data Type: Text
  - For cancellations, denials, overrides, etc.
  
- **Notes** (Optional)
  - Data Type: Text
  
- **RelatedPrescriptionID** (Optional)
  - Data Type: Integer (Foreign Key to Prescription table)
  - For replacements, refills, etc.

**Indexes**:
- Primary Key: AuditLogID
- Index: PrescriptionID
- Index: ActionType
- Index: UserID
- Index: ProviderID
- Index: ActionTimestamp
- Composite Index: (PrescriptionID, ActionTimestamp) for prescription history queries
- Composite Index: (UserID, ActionTimestamp) for user activity queries

**Relationships**:
- Many-to-One: Prescription Audit Log → Prescription
- Many-to-One: Prescription Audit Log → User
- Many-to-One: Prescription Audit Log → Provider (optional)
- Many-to-One: Prescription Audit Log → Related Prescription (optional)

#### 3.3.3 Supporting Entity Definitions

##### 3.3.3.1 PDMP Query Entity

**Purpose**: Stores Prescription Drug Monitoring Program query information.

**Primary Key**: PDMPQueryID (Unique, Auto-increment)

**Key Attributes**:
- PDMPQueryID (Primary Key)
- PrescriptionID (Foreign Key, optional - query may be independent)
- PatientID (Foreign Key, required)
- ProviderID (Foreign Key, required)
- QueryDate (DateTime, required)
- QueryResult (Text, optional)
- QueryStatus (String, required)
- IsAlertGenerated (Boolean)

##### 3.3.3.2 Formulary Entity

**Purpose**: Stores insurance formulary information for medications.

**Primary Key**: FormularyID (Unique, Auto-increment)

**Key Attributes**:
- FormularyID (Primary Key)
- InsurancePlanID (Foreign Key)
- MedicationID (Foreign Key)
- FormularyStatus (Covered, Not Covered, Prior Auth Required, Step Therapy)
- Tier (Integer, optional)
- CopayAmount (Decimal, optional)

#### 3.3.4 Data Standards and Coding

##### 3.3.4.1 Coding Standards Requirements

- **RxNorm**: Required for medication identification
- **NDC**: Required for medication identification (for prescriptions)
- **NCPDP SCRIPT**: Required for e-prescribing transmission
- **SNOMED CT**: Recommended for clinical terminology
- **ICD-10**: Required for diagnosis codes (if linking to problems)
- **DAW Codes**: Required for substitution preferences

##### 3.3.4.2 Data Validation Rules

- **Date Validation**: All dates must be valid, cannot be in future (except for scheduled dates)
- **Code Validation**: All codes must be validated against current code sets
- **Required Field Validation**: All required fields must be populated
- **Format Validation**: All data must conform to specified formats
- **Range Validation**: Numeric values must be within reasonable ranges
- **Refill Validation**: Refills must be within allowed limits (0-11, schedule-dependent)
- **DEA Validation**: DEA numbers must be valid format and checksum
- **Referential Integrity**: All foreign keys must reference existing records

#### 3.3.5 Data Retention and Archival

##### 3.3.5.1 Data Retention Requirements

- **Active Prescriptions**: Maintained in primary database
- **Retention Period**: Minimum 6-10 years after prescription date or last activity (varies by jurisdiction)
- **Controlled Substances**: Longer retention may be required (typically 7-10 years)
- **Archival**: Historical prescriptions archived but accessible
- **Disposal**: Secure disposal when retention period expires (if applicable)

##### 3.3.5.2 Data Archival Strategy

- **Archival Criteria**: Prescriptions meeting retention period requirements
- **Archival Process**: Automated archival process
- **Archival Storage**: Separate archival database or storage system
- **Archival Access**: Archived prescriptions accessible but may have performance limitations
- **Data Integrity**: Maintained during archival process

#### 3.3.6 Data Quality Requirements

##### 3.3.6.1 Data Completeness

- **Required Fields**: All required fields must be populated
- **Critical Information**: Critical information (medication, dosage, patient, provider) must be documented
- **Data Completeness Metrics**: Track and report data completeness

##### 3.3.6.2 Data Accuracy

- **Data Validation**: All data validated at entry
- **Data Verification**: Critical data verified by providers
- **Error Detection**: System detects and reports data errors
- **Data Correction**: Process for correcting data errors

##### 3.3.6.3 Data Consistency

- **Referential Integrity**: Maintained through foreign key constraints
- **Data Synchronization**: Data synchronized across related entities
- **Duplicate Detection**: System detects and prevents duplicate prescriptions
- **Data Standardization**: Data standardized using coding systems

#### 3.3.7 Data Security Requirements

##### 3.3.7.1 Data Encryption

- **Encryption at Rest**: All PHI encrypted at rest (AES-256 minimum)
- **Encryption in Transit**: All PHI encrypted in transit (TLS 1.2 minimum)
- **Controlled Substance Data**: Enhanced encryption for controlled substance data
- **Encryption Keys**: Secure key management

##### 3.3.7.2 Access Controls

- **Authentication**: Strong authentication required
- **Authorization**: Role-based access control
- **Audit Logging**: All data access logged
- **Data Masking**: Sensitive data masked based on user role
- **Controlled Substance Access**: Enhanced access controls for controlled substances

##### 3.3.7.3 Data Privacy

- **Minimum Necessary**: Users see only necessary information
- **Patient Privacy Preferences**: Patient privacy preferences enforced
- **Break-the-Glass**: Emergency access with audit trail
- **Controlled Substance Privacy**: Enhanced privacy controls for controlled substances

### 3.4 User Roles and Permissions

#### 3.4.1 Role-Based Access Control (RBAC) Overview

The Prescription Management feature shall implement comprehensive Role-Based Access Control (RBAC) to ensure that users only have access to prescription-related information and functionality appropriate to their role in the healthcare organization. Access control shall be enforced at multiple levels: prescription creation, modification, transmission, refill management, controlled substances handling, and prescription history access.

##### 3.4.1.1 RBAC Principles
- **Principle of Least Privilege**: Users shall have minimum necessary access to perform their job functions related to prescription management
- **Separation of Duties**: Critical prescription functions (e.g., controlled substances, high-risk medications) shall require appropriate authorization and may require additional approvals
- **Need-to-Know Basis**: Access granted based on clinical or administrative need for prescription information
- **Role Hierarchy**: Roles organized hierarchically with inheritance of permissions where appropriate
- **Dynamic Permissions**: Permissions may vary based on context (e.g., assigned patients, prescription status, medication schedule)
- **Audit Trail**: All prescription access and actions logged with user identification, timestamp, and action details
- **Regulatory Compliance**: Access controls shall comply with DEA, state-specific, and federal regulations for controlled substances

##### 3.4.1.2 Permission Categories
- **Read Permissions**: Ability to view prescription information
- **Create Permissions**: Ability to create new prescriptions
- **Modify Permissions**: Ability to modify existing prescriptions (before transmission or after cancellation)
- **Cancel Permissions**: Ability to cancel prescriptions
- **Transmit Permissions**: Ability to transmit prescriptions to pharmacies
- **Refill Permissions**: Ability to approve, deny, or initiate refill requests
- **Override Permissions**: Ability to override drug interaction alerts, allergy warnings, or dosage limits (with documentation)
- **Controlled Substance Permissions**: Special permissions for controlled substance prescriptions (Schedule II-V)
- **PDMP Access**: Ability to query Prescription Drug Monitoring Programs
- **History Access**: Ability to view prescription history and reports
- **Administrative Permissions**: Ability to manage prescription settings, templates, and system configuration

#### 3.4.2 Core User Roles and Permissions

##### 3.4.2.1 Prescribing Provider (MD/DO/NP/PA)

**Role Description**: Licensed healthcare providers with prescribing authority and full clinical responsibility for medication management.

**Access Level**: Full prescription management access

**Permissions**:
- **Prescription Creation**:
  - Create new prescriptions for assigned patients
  - Create prescriptions for any patient (if authorized)
  - Select medications from drug database
  - Enter prescription details (dosage, frequency, quantity, duration, route, instructions)
  - Specify refill authorization
  - Select pharmacy for transmission
  - Use prescription templates
  - Create custom prescription templates
  - Add prescription notes and special instructions
  
- **Prescription Modification**:
  - Modify prescriptions before transmission
  - Modify prescriptions after transmission (if allowed by state regulations)
  - Update prescription details
  - Change pharmacy selection
  - Modify refill authorization
  
- **Prescription Cancellation**:
  - Cancel prescriptions before transmission
  - Cancel prescriptions after transmission (with appropriate notifications)
  - Cancel individual prescriptions or entire prescription sets
  
- **Prescription Transmission**:
  - Transmit prescriptions electronically to pharmacies
  - Transmit prescriptions via fax (if electronic transmission unavailable)
  - Print prescriptions (if required by state regulations)
  - Retransmit failed prescriptions
  - View transmission status and confirmations
  
- **Drug Interaction and Allergy Checking**:
  - View all drug interaction alerts
  - View all allergy warnings
  - Override interaction alerts (with documentation and justification)
  - Override allergy warnings (with documentation and justification)
  - View interaction severity levels
  - Access detailed interaction information
  
- **Prescription Refills**:
  - Approve refill requests from pharmacies
  - Deny refill requests with reason
  - Initiate refill requests
  - Modify refill quantities or frequencies
  - Set auto-approval rules for refills
  - View refill history
  
- **Controlled Substances Management**:
  - Create prescriptions for Schedule II controlled substances
  - Create prescriptions for Schedule III-V controlled substances
  - Query PDMP before prescribing controlled substances (if required)
  - View PDMP results
  - Override controlled substance quantity/duration limits (with documentation)
  - Access controlled substance prescription history
  - View DEA number validation status
  
- **Prescription History and Reporting**:
  - View complete prescription history for all patients
  - View prescription history for assigned patients
  - Generate prescription reports
  - View prescription analytics
  - Export prescription data
  - View prescription status and tracking information
  
- **Formulary and Insurance**:
  - View formulary information
  - View insurance coverage information
  - View medication alternatives
  - Override formulary restrictions (with documentation)
  
- **Administrative Functions**:
  - Print prescriptions
  - Export prescription data
  - Access prescription audit logs (own actions)
  - Manage prescription templates
  - Configure prescription preferences
  
- **Restrictions**:
  - Cannot delete transmitted prescriptions (only cancel)
  - Cannot modify prescriptions after dispensing (unless allowed by regulations)
  - Cannot override certain critical alerts without documentation
  - Cannot access system administration functions (unless also assigned admin role)
  - Must comply with state-specific prescribing restrictions
  - Must have valid DEA number for controlled substances (if applicable)

##### 3.4.2.2 Nurse Practitioner (NP) / Physician Assistant (PA)

**Role Description**: Advanced practice providers with prescribing authority similar to physicians, subject to state regulations and collaborative agreements.

**Access Level**: Full prescription management access (similar to Prescribing Provider)

**Permissions**:
- Same permissions as Prescribing Provider role
- May have state-specific restrictions on certain controlled substances
- May require physician co-signature for certain prescriptions (configurable based on state regulations and collaborative agreements)
- May have restrictions on Schedule II controlled substances (varies by state)
- May require additional authorization for certain high-risk medications

##### 3.4.2.3 Registered Nurse (RN) / Licensed Practical Nurse (LPN)

**Role Description**: Licensed nursing staff providing clinical support and assisting with prescription management workflows.

**Access Level**: Clinical support access with limited prescription management

**Permissions**:
- **Prescription Viewing**:
  - Read access to all prescriptions for assigned patients
  - Read access to prescriptions for patients in assigned unit/facility
  - View prescription details (medication, dosage, frequency, instructions)
  - View prescription status and history
  - View drug interaction alerts (read-only)
  - View allergy warnings (read-only)
  
- **Refill Management**:
  - Initiate refill requests for provider approval
  - View refill request status
  - Cannot approve or deny refills (requires provider)
  - Can communicate refill requests to pharmacies (with provider authorization)
  
- **Prescription Support**:
  - Assist with prescription entry (under provider supervision)
  - Cannot create or transmit prescriptions independently
  - Cannot modify prescription details
  - Cannot cancel prescriptions
  - Can view prescription templates (read-only)
  
- **Drug Information**:
  - View medication information
  - View drug interaction information (read-only)
  - View allergy information (read-only)
  - Cannot override alerts or warnings
  
- **Prescription History**:
  - View prescription history for assigned patients
  - View current medication lists
  - Cannot export prescription data
  - Limited access to prescription reports
  
- **Controlled Substances**:
  - View controlled substance prescriptions (read-only)
  - Cannot create controlled substance prescriptions
  - Cannot query PDMP
  - Cannot override controlled substance restrictions
  
- **Restrictions**:
  - Cannot create prescriptions
  - Cannot transmit prescriptions
  - Cannot modify prescriptions
  - Cannot cancel prescriptions
  - Cannot approve refills
  - Cannot override drug interaction or allergy alerts
  - Cannot access PDMP
  - Cannot create controlled substance prescriptions
  - Cannot access prescription audit logs (except own actions)

##### 3.4.2.4 Medical Assistant (MA)

**Role Description**: Clinical support staff assisting with administrative and clinical tasks related to prescription management.

**Access Level**: Limited prescription access

**Permissions**:
- **Prescription Viewing**:
  - Read-only access to prescriptions for assigned patients
  - View prescription details (medication, dosage, frequency)
  - View prescription status
  - Cannot view detailed drug interaction or allergy information
  
- **Refill Support**:
  - Can receive refill requests from pharmacies (read-only)
  - Can forward refill requests to providers
  - Cannot approve or deny refills
  - Cannot initiate refill requests
  
- **Prescription Support**:
  - Can assist with prescription data entry (under direct provider supervision)
  - Cannot create prescriptions independently
  - Cannot transmit prescriptions
  - Cannot modify prescriptions
  - Cannot cancel prescriptions
  
- **Prescription History**:
  - View current medication lists (read-only)
  - Limited access to prescription history
  - Cannot generate prescription reports
  - Cannot export prescription data
  
- **Controlled Substances**:
  - No access to controlled substance prescription details
  - Cannot view PDMP information
  - Cannot access controlled substance reports
  
- **Restrictions**:
  - Cannot create prescriptions
  - Cannot transmit prescriptions
  - Cannot modify prescriptions
  - Cannot cancel prescriptions
  - Cannot approve refills
  - Cannot override any alerts or warnings
  - Cannot access controlled substance information
  - Cannot access PDMP
  - Cannot access prescription audit logs
  - Cannot export prescription data

##### 3.4.2.5 Pharmacist

**Role Description**: Licensed pharmacists reviewing and processing prescriptions, providing pharmaceutical care, and managing medication therapy.

**Access Level**: Medication-focused clinical access (if integrated with EHR system)

**Permissions**:
- **Prescription Viewing**:
  - Read access to all prescriptions for patients (if integrated)
  - View prescription details (medication, dosage, frequency, instructions)
  - View prescription history
  - View current medication lists
  - View drug interaction information
  - View allergy information
  
- **Prescription Review**:
  - Review prescriptions for appropriateness
  - Identify potential drug interactions
  - Identify potential allergies
  - Suggest medication alternatives
  - Document medication reviews
  
- **Refill Management**:
  - Send refill requests to prescribing providers
  - View refill request status
  - Cannot approve refills (requires provider)
  - Can communicate refill needs to providers
  
- **Prescription Communication**:
  - Send messages to providers regarding prescriptions
  - Request clarifications on prescriptions
  - Report prescription issues or concerns
  - Cannot modify prescriptions
  
- **Drug Information**:
  - Full access to drug information database
  - Access to drug interaction databases
  - Access to formulary information
  - Can provide medication counseling information
  
- **Controlled Substances**:
  - View controlled substance prescriptions (read-only)
  - Cannot create controlled substance prescriptions
  - Cannot query PDMP (unless authorized in jurisdiction)
  - Can verify controlled substance prescriptions
  
- **Prescription History**:
  - View prescription history for patients
  - View medication adherence information
  - Generate medication-related reports
  - Cannot export prescription data (unless authorized)
  
- **Restrictions**:
  - Cannot create prescriptions
  - Cannot transmit prescriptions
  - Cannot modify prescriptions
  - Cannot cancel prescriptions
  - Cannot approve refills (unless authorized in jurisdiction)
  - Cannot override drug interaction or allergy alerts in prescribing system
  - Cannot access PDMP (unless authorized)
  - Cannot create controlled substance prescriptions
  - Limited access to prescription audit logs

##### 3.4.2.6 Administrative Staff

**Role Description**: Non-clinical staff handling administrative tasks, typically with no clinical prescription management responsibilities.

**Access Level**: No prescription access (unless specifically authorized)

**Permissions**:
- **Default Permissions**: No access to prescription management features
- **If Specifically Authorized** (rare, for specific administrative functions):
  - Read-only access to prescription status (for billing or administrative purposes)
  - Cannot view prescription details
  - Cannot view medication information
  - Cannot access prescription history
  - Cannot create, modify, or cancel prescriptions
  - Cannot approve refills
  
- **Restrictions**:
  - No access to prescription creation
  - No access to prescription modification
  - No access to prescription transmission
  - No access to prescription cancellation
  - No access to refill management
  - No access to drug interaction or allergy information
  - No access to controlled substance information
  - No access to PDMP
  - No access to prescription history
  - No access to prescription audit logs

##### 3.4.2.7 System Administrator

**Role Description**: IT staff managing system configuration and technical operations for prescription management system.

**Access Level**: Technical/administrative access

**Permissions**:
- **System Configuration**:
  - Full access to prescription system settings
  - Can configure prescription templates
  - Can manage drug database settings
  - Can configure transmission settings
  - Can manage integration settings (pharmacy networks, PDMP)
  - Can access system logs
  
- **User Management**:
  - Can assign prescription-related roles
  - Can manage user permissions for prescription features
  - Cannot create clinical user accounts (unless also clinical role)
  
- **Data Management**:
  - Can access prescription database for technical purposes
  - Can run system maintenance
  - Can manage backups
  - Cannot access patient prescription data for clinical purposes (unless also clinical role)
  
- **Audit and Security**:
  - Full access to prescription audit logs
  - Can review security events related to prescriptions
  - Can manage security settings
  - Can investigate security incidents
  
- **Restrictions**:
  - Should not access patient prescription data for clinical purposes (unless also clinical role)
  - Cannot create prescriptions
  - Cannot modify prescriptions
  - Cannot transmit prescriptions
  - Cannot approve refills
  - All prescription data access logged and monitored

##### 3.4.2.8 Specialist Physician

**Role Description**: Specialist physicians with prescribing authority for their specialty area, with access to assigned patients and consultation cases.

**Access Level**: Full prescription access for assigned/consultation patients, read-only for others

**Permissions**:
- **Assigned/Consultation Patients**:
  - Full prescription management permissions (same as Prescribing Provider)
  - Can create prescriptions for specialty-related medications
  - Can modify and cancel prescriptions
  - Can approve refills
  - Full access to prescription history
  
- **Other Patients** (if authorized):
  - Read-only access to prescription information
  - Cannot create prescriptions
  - Cannot modify prescriptions
  - Cannot approve refills
  
- **All Other Permissions**: Same as Prescribing Provider role for assigned/consultation patients

#### 3.4.3 Permission Matrix

##### 3.4.3.1 Prescription Management Access Matrix

| Feature | Prescribing Provider | NP/PA | RN/LPN | MA | Admin Staff | Specialist | Pharmacist | System Admin |
|---------|---------------------|-------|--------|-----|-------------|------------|------------|--------------|
| Create Prescription | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ (assigned) | ✗ | ✗ |
| Modify Prescription | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ (assigned) | ✗ | ✗ |
| Cancel Prescription | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ (assigned) | ✗ | ✗ |
| Transmit Prescription | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ (assigned) | ✗ | ✗ |
| View Prescription | ✓ | ✓ | ✓ | ✓ | Limited | ✓ | ✓ | Limited |
| Approve Refill | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ (assigned) | Limited | ✗ |
| Initiate Refill Request | ✓ | ✓ | ✓ | ✗ | ✗ | ✓ (assigned) | ✓ | ✗ |
| Override Interaction Alert | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ (assigned) | ✗ | ✗ |
| Override Allergy Warning | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ (assigned) | ✗ | ✗ |
| Create Controlled Substance | ✓ | Limited | ✗ | ✗ | ✗ | ✓ (assigned) | ✗ | ✗ |
| Query PDMP | ✓ | Limited | ✗ | ✗ | ✗ | ✓ (assigned) | Limited | ✗ |
| View Prescription History | ✓ | ✓ | Limited | Limited | ✗ | ✓ | ✓ | Limited |
| Generate Reports | ✓ | ✓ | Limited | ✗ | ✗ | ✓ | Limited | Limited |
| Export Prescription Data | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ | Limited | Limited |
| Access Audit Logs | Limited | Limited | Limited | ✗ | ✗ | Limited | Limited | ✓ |

##### 3.4.3.2 Controlled Substances Access Matrix

| Feature | Prescribing Provider | NP/PA | RN/LPN | MA | Admin Staff | Specialist | Pharmacist | System Admin |
|---------|---------------------|-------|--------|-----|-------------|------------|------------|--------------|
| Create Schedule II | ✓ | Limited | ✗ | ✗ | ✗ | ✓ (assigned) | ✗ | ✗ |
| Create Schedule III-V | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ (assigned) | ✗ | ✗ |
| Query PDMP | ✓ | Limited | ✗ | ✗ | ✗ | ✓ (assigned) | Limited | ✗ |
| View PDMP Results | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ (assigned) | Limited | ✗ |
| Override Quantity Limits | ✓ | Limited | ✗ | ✗ | ✗ | ✓ (assigned) | ✗ | ✗ |
| Override Duration Limits | ✓ | Limited | ✗ | ✗ | ✗ | ✓ (assigned) | ✗ | ✗ |
| View Controlled Substance History | ✓ | ✓ | Limited | ✗ | ✗ | ✓ | Limited | Limited |
| Generate Controlled Substance Reports | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ | Limited | Limited |

#### 3.4.4 Context-Based Permissions

##### 3.4.4.1 Assigned Patient Permissions

- **Primary Care Provider**: Full prescription management access to assigned patients
- **Specialist**: Full prescription management access to patients referred for consultation
- **Covering Provider**: Temporary full prescription access during coverage period
- **On-Call Provider**: Emergency prescription access with audit trail
- **Care Team Member**: Access based on care team assignment and role

##### 3.4.4.2 Prescription Status-Based Permissions

- **Draft Prescriptions**: Full modify/delete access by creator
- **Transmitted Prescriptions**: Limited modification (cancel only, subject to regulations)
- **Dispensed Prescriptions**: Read-only access (cannot modify)
- **Cancelled Prescriptions**: Read-only access (cannot reactivate)
- **Expired Prescriptions**: Read-only access

##### 3.4.4.3 Medication Schedule-Based Permissions

- **Non-Controlled Substances**: Standard prescription permissions
- **Schedule III-V**: Additional validation and documentation requirements
- **Schedule II**: Maximum restrictions, PDMP query required, limited refills
- **High-Risk Medications**: Additional alerts and may require additional authorization

#### 3.4.5 Special Access Scenarios

##### 3.4.5.1 Emergency Prescription Access

- **Purpose**: Allow emergency prescription creation when normal workflow is insufficient
- **Authorization**: Requires justification and is logged
- **Access Level**: Full prescription creation access for duration of emergency
- **Audit**: All actions logged with emergency access flag
- **Review**: Emergency prescription access reviewed by security officer
- **Available To**: Prescribing providers (MD/DO/NP/PA)
- **Limitations**: May have restrictions on controlled substances in emergency scenarios

##### 3.4.5.2 Delegation of Prescribing Authority

- **Temporary Delegation**: Providers can delegate specific prescription permissions temporarily
- **Delegation Scope**: Limited to specific functions (e.g., refill approval, prescription modification)
- **Delegation Duration**: Time-limited with automatic expiration
- **Audit**: All delegated prescription actions logged with delegator information
- **Regulatory Compliance**: Delegation must comply with state regulations

##### 3.4.5.3 Proxy Prescription Access

- **Purpose**: Allow authorized users to create prescriptions on behalf of providers (e.g., during provider absence)
- **Authorization**: Requires provider authorization and documentation
- **Scope**: Limited to specific functions and time periods
- **Documentation**: Proxy relationship documented with start/end dates
- **Audit**: All proxy prescription actions logged with proxy and delegator information
- **Regulatory Compliance**: Must comply with state regulations on proxy prescribing

##### 3.4.5.4 Break-the-Glass for Prescriptions

- **Purpose**: Allow emergency access to prescription information when normal access is insufficient
- **Authorization**: Requires justification and is logged
- **Access Level**: Read access to prescription information for duration of emergency
- **Audit**: All access logged with break-the-glass flag
- **Review**: Break-the-glass access reviewed by security officer
- **Available To**: Clinical staff (Physicians, NPs, PAs, RNs)
- **Limitations**: Does not grant prescription creation or modification permissions

#### 3.4.6 Permission Management

##### 3.4.6.1 Role Assignment

- **Initial Assignment**: Prescription-related roles assigned during user account creation
- **Role Changes**: Roles can be modified by authorized administrators
- **Multiple Roles**: Users can have multiple roles (e.g., Physician + System Administrator)
- **Role Hierarchy**: Higher roles inherit permissions from lower roles (if applicable)
- **State-Specific Roles**: Roles may be customized based on state regulations

##### 3.4.6.2 Permission Customization

- **Organization-Level**: Prescription permissions can be customized at organization level
- **Department-Level**: Prescription permissions can be customized at department level
- **User-Level**: Individual user prescription permissions can be customized (with authorization)
- **Temporary Permissions**: Time-limited prescription permissions can be granted
- **State-Specific Customization**: Permissions customized to comply with state regulations

##### 3.4.6.3 Permission Review

- **Regular Review**: User prescription permissions reviewed regularly (e.g., annually)
- **Change Management**: Prescription permission changes require approval
- **Audit**: All permission changes logged
- **Compliance**: Permission reviews documented for compliance (DEA, state regulations)
- **License Verification**: Prescribing permissions verified against active licenses and DEA numbers

#### 3.4.7 Security and Compliance

##### 3.4.7.1 Access Logging

- **All Access Logged**: All prescription access logged with user, timestamp, and action
- **Failed Access Attempts**: Failed prescription access attempts logged
- **Audit Trail**: Complete audit trail maintained for all prescription actions
- **Log Retention**: Access logs retained per regulatory requirements (typically 6-10 years)
- **DEA Compliance**: Controlled substance access logged per DEA requirements

##### 3.4.7.2 Access Monitoring

- **Real-Time Monitoring**: Suspicious prescription access patterns monitored in real-time
- **Alerts**: Alerts generated for unusual prescription access patterns (e.g., excessive PDMP queries, unusual controlled substance access)
- **Review**: Prescription access logs reviewed regularly
- **Investigation**: Security incidents related to prescriptions investigated promptly
- **Controlled Substance Monitoring**: Enhanced monitoring for controlled substance prescriptions

##### 3.4.7.3 Compliance Requirements

- **HIPAA Compliance**: Prescription access controls comply with HIPAA requirements
- **DEA Compliance**: Controlled substance access controls comply with DEA requirements
- **State Regulations**: Access controls comply with state-specific prescription regulations
- **Minimum Necessary**: Minimum necessary principle enforced for prescription access
- **Patient Rights**: Patient access rights to prescription information supported
- **Regulatory Compliance**: Access controls comply with applicable federal and state regulations
- **PDMP Compliance**: PDMP access and query logging comply with state requirements

---

