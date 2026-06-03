# `hospital-service` – Implementation Plan

## 1. Overview and Objectives

`hospital-service` is the core clinical service for the Hospital Module. It owns:

- Core EHR:
  - Encounters, diagnoses, problems, allergies, clinical notes.
- Prescription:
  - Medication orders created by clinicians.
- Visits/encounters:
  - OP/IP/ED visit entities, linking to billing and scheduling.
- Basic patient registration:
  - Demographics, MRN, contact information, and basic consents.

**Primary objectives:**

- Provide a clean, well-bounded clinical core that other `hospital-*` services can build on.
- Avoid re-splitting this service prematurely while keeping internal modules separable enough to extract later (e.g., `hospital-prescription-service`) if needed.
- Integrate cleanly with platform-level services (auth, master data, accounting, inventory, HR) and other hospital services (scheduling, billing, etc.).

## 2. Scope and Non-Scope

### 2.1 In-Scope

- **Patient core profile** (for hospital context):
  - Patient identifiers (MRN, external IDs).
  - Demographics (name, DOB, gender, contact details).
  - Basic communication preferences (language, contact mode).
  - High-level consent flags relevant to EHR visibility.
- **Visit & encounter management:**
  - OPD visit, IPD admission, ED visit entities.
  - Visit life cycle: create, update, discharge/end.
  - Association with doctors, departments, and corporate/coverage context (via IDs, not embedded corporate logic).
- **Clinical record:**
  - Problems/diagnoses, allergies, clinical notes.
  - Vitals and key clinical measurements (if not split).
  - Links to lab orders, imaging orders, procedures (as references to `hospital-clinical-orders-service`).
- **Prescriptions:**
  - Prescription header (who, when, where).
  - Prescription lines (drug reference, dosage, frequency, duration, route).
  - Status (draft, signed, cancelled, completed).
  - Integration points for fulfillment in `hospital-pharmacy-service`.

### 2.2 Out-of-Scope (Handled by Other Services)

- Detailed **scheduling logic** (handled by `hospital-scheduling-service`).
- **Billing calculations, invoices, payments** (handled by `hospital-billing-service`).
- **Corporate and discount rules** (handled by `hospital-corporate-and-discount-service`).
- **Card lifecycle and balances** (handled by `hospital-card-management-service`).
- **Canteen operations** (handled by `hospital-canteen-service`).
- Detailed **lab/radiology order workflows** (handled by `hospital-clinical-orders-service`).
- **Inventory and stock management** (handled by inventory/pharmacy services).

## 3. Architecture and Boundaries

### 3.1 High-Level Architecture

- **Service type**: Stateless API service with a database per service.
- **Database**: Relational (e.g., PostgreSQL) for strong consistency and relational queries across visits, encounters, and clinical documents.
- **Integration style**:
  - Synchronous REST/gRPC for read/write operations from portals, BFF, and other services.
  - Asynchronous events for key domain events (visit created, prescription issued, diagnosis updated, etc.).

### 3.2 Bounded Context Responsibilities

- **Hospital Patient Context**:
  - Owns MRN and core patient profile in the hospital context (may link to a platform-wide person/patient).
- **Visit Context**:
  - Owns life cycle of visits and encounters and their clinical relationships.
- **Clinical Documentation Context**:
  - Owns notes, problems, diagnoses, allergies.
- **Prescription Context**:
  - Owns prescriptions and their states up to the point of fulfillment.

Each context is implemented as an internal module (package or subdomain) within `hospital-service` with clear boundaries to ease future extraction.

## 4. Data Model (High-Level)

### 4.1 Key Entities

- `Patient`:
  - `id`, `mrn`, external identifiers.
  - `name`, `dob`, `gender`, `contacts`, `primary_contact`.
  - `consent_flags` (e.g., share-with-corporate, research-consent, data-sharing).
- `Visit`:
  - `id`, `patient_id`, `visit_type` (OP, IP, ED).
  - `start_time`, `end_time`, `status` (active, discharged, cancelled).
  - `attending_doctor_id`, `department_id`, `corporate_contract_id` (reference only).
- `Encounter`:
  - `id`, `visit_id`, `encounter_type` (consultation, ward_round, ED_event).
  - `start_time`, `end_time`, `location_id`.
  - `performed_by_doctor_id`, `notes_summary`.
- `Diagnosis` / `Problem`:
  - `id`, `patient_id`, `visit_id` (optional), `encounter_id` (optional).
  - `code`, `code_system`, `description`, `onset_date`, `status` (active/resolved).
- `Allergy`:
  - `id`, `patient_id`.
  - `substance`, `reaction`, `severity`, `status`.
- `ClinicalNote`:
  - `id`, `patient_id`, `visit_id`, `encounter_id`.
  - `note_type`, `content`, `author_id`, `created_at`, `amended_at`.
- `Prescription` (header):
  - `id`, `patient_id`, `visit_id`, `encounter_id`.
  - `prescribing_doctor_id`, `location_id`, `created_at`, `status`.
- `PrescriptionLine`:
  - `id`, `prescription_id`.
  - `drug_id` (reference to drug master in `hospital-pharmacy-service` or global catalog).
  - `dose`, `route`, `frequency`, `duration`, `instructions`.
  - `status` (active, stopped, changed, discontinued).

### 4.2 Referential Strategy

- Use **IDs only** for entities owned by other services (e.g., corporate contracts, rooms, beds, doctor identities, drugs).
- Avoid duplicating business logic from other bounded contexts; consume their APIs / events instead.

## 5. APIs

### 5.1 Patient and Visit APIs

- `POST /patients` – create patient profile.
- `GET /patients/{id}` – get patient details.
- `PATCH /patients/{id}` – update patient demographics and consents.
- `POST /visits` – create a visit (OP/IP/ED).
- `GET /visits/{id}` – retrieve visit details.
- `PATCH /visits/{id}` – update visit (e.g., discharge time, status).
- `GET /patients/{id}/visits` – list visits for a patient.

### 5.2 Encounter and Clinical Data APIs

- `POST /visits/{visitId}/encounters` – create encounter under a visit.
- `GET /encounters/{id}` – get encounter.
- `POST /patients/{id}/diagnoses` – add diagnosis/problem.
- `GET /patients/{id}/diagnoses` – list diagnoses/problems.
- `POST /patients/{id}/allergies` – add allergy.
- `GET /patients/{id}/allergies` – list allergies.
- `POST /encounters/{encounterId}/notes` – add clinical note.
- `GET /encounters/{encounterId}/notes` – list notes for encounter.

### 5.3 Prescription APIs

- `POST /encounters/{encounterId}/prescriptions` – create prescription.
- `GET /prescriptions/{id}` – get prescription (header + lines).
- `POST /prescriptions/{id}/lines` – add prescription lines.
- `PATCH /prescriptions/{id}` – update header (e.g., status).
- `PATCH /prescriptions/{id}/lines/{lineId}` – update line (dose change, discontinue).
- `GET /patients/{id}/prescriptions` – list prescriptions for a patient.

### 5.4 Search and Summary APIs

- `GET /patients/search` – search by name, MRN, phone, etc.
- `GET /patients/{id}/summary` – high-level clinical summary for portals and BFFs.

## 6. Events and Integrations

### 6.1 Outgoing Domain Events

Emit events via the platform event bus (Kafka, RabbitMQ, etc.) for:

- `patient.created`, `patient.updated`.
- `visit.created`, `visit.updated`, `visit.discharged`.
- `encounter.created`.
- `diagnosis.added`, `diagnosis.updated`.
- `allergy.added`, `allergy.updated`.
- `prescription.created`, `prescription.updated`.

Downstream consumers:

- `hospital-billing-service` – to pick up visit start/discharge and related context.
- `hospital-scheduling-service` – for alignment of actual vs planned encounters.
- `hospital-pharmacy-service` – for prescription fulfillment.
- `hospital-portal-bff-service` – to push real-time updates to UI, if subscribed.

### 6.2 Incoming Integrations

- From `hospital-scheduling-service`:
  - Appointment/bed allocation references (IDs) passed when creating visits/encounters.
- From `hospital-pharmacy-service`:
  - Optionally receive status updates for prescription line fulfillment (e.g., `prescriptionline.fulfilled`) to reflect in EHR.
- From platform services:
  - User/doctor identity, departments, locations via master data APIs.

## 7. Non-Functional Requirements

- **Performance**:
  - 95th percentile response time for primary read APIs \(< 300 ms\) under typical load.
  - 99th percentile for write APIs \(< 500 ms\) for visit/patient/prescription operations.
- **Availability**:
  - Target 99.9%+ for core EHR operations.
- **Security & Privacy**:
  - Enforce auth & RBAC at API gateway and within service (scoped checks).
  - Field-level masking and filtering for PII and sensitive clinical data.
- **Audit**:
  - Maintain immutable audit trails for clinical changes (who changed what and when).
- **Compliance**:
  - Align with applicable healthcare privacy regulations in target regions (e.g., HIPAA-like constraints, local data residency if required).

## 8. Phased Implementation Plan

Implement in order. Each phase below is written so that “implement Phase N of `hospital-service`” can be executed by Cursor without ambiguity.

**Implementation status overview (as of 2026‑03‑14):**

- **Phase 1 – Baseline Patient, Visit, and Notes**: ~**85% implemented** in existing `hospital-service` (patient registration, encounters/visits, clinical notes, and patient summary APIs are present under `/api/patients`, `/api/encounters`, `/api/clinical-notes`, `/api/patients/{patientId}/summary`). Remaining work is mainly aligning naming/URLs with this plan (if desired) and standardizing event emission and RBAC scopes.
- **Phase 2 – Problems, Diagnoses, Allergies**: ~**80% implemented** (problem list and allergy entities, services, and controllers exist via `ProblemListController`, `AllergyController`, and related services/DTOs). Remaining work is primarily standardized domain events and ensuring the patient summary response fully matches the requirements in §5.2.
- **Phase 3 – Prescriptions**: ~**75% implemented** (rich prescription model and `PrescriptionController`/`PrescriptionService` already handle electronic prescriptions, validation, allergy and interaction checks, and e‑prescribing flows). Remaining work is aligning the simpler prescription header/line model and events in this plan with the richer existing implementation, and wiring consistent outbound events for `hospital-pharmacy-service`.
- **Phase 4 – Hardening, Refactoring, and Extraction Readiness**: ~**30% implemented** (service is deployed with many features and some modularization, but explicit package‑level separation, internal interfaces for future extraction, and systematic performance/observability work are still pending as per this plan.

### Phase 1 – Baseline Patient, Visit, and Notes

- Implement:
  - `Patient`, `Visit`, `Encounter`, `ClinicalNote` entities and basic APIs.
  - `patient.created` and `visit.created` events.
- Integrate:
  - Basic auth and RBAC integration.
  - Minimal linkage to `hospital-billing-service` (visit references only).
- Deliverables:
  - Can register patients, create visits, attach encounters, and record notes.
  - Portals can show basic patient and visit information.

#### Phase 1.1 – Database (Liquibase)

- [x] Review existing hospital schema changesets in `easyops-erp/services/hospital-service/src/main/resources/db/changelog/changesets`:
  - `001-initial-schema.xml` (which in turn applies `hospital.sql`), `002-hospital-permissions.xml`, `003-medical-codes.xml`, `004-add-prescription-fields.xml`, and related files.
- [x] Confirm that core EHR tables already exist:
  - `ehr.patients`, `ehr.patient_emergency_contacts`, `ehr.patient_insurance`, `ehr.patient_consents` and other patient‑related tables.
  - `ehr.encounters` (and related encounter/visit tracking) – used by `Encounter`/`EncounterService`.
  - `ehr.clinical_notes` and related attachments/templates – used by `ClinicalNoteService` and `ClinicalNoteController`.
- [x] If any fields are missing or need extension versus the data model in §4.1 (`Patient`, `Visit`, `Encounter`, `ClinicalNote`), add a new Liquibase SQL changeset file (next number after the latest, currently `015-hospital-billing-invoice-discounts-and-audit.sql`, so use `016-hospital-clinical-core.sql` or higher) that:
  - [x] Alters existing `ehr.*` tables to add only the additional required columns and indexes (no table renames or schema moves).
  - [x] Is idempotent and safe for existing deployments.
  - _Current assessment_: the existing `ehr.patients`, `ehr.encounters`, and `ehr.clinical_notes` schemas already cover the fields required for Phase 1, so **no `016-hospital-clinical-core.sql` changeset is needed at this time**. If future gaps are discovered, they will be handled in a later phase as additive schema changes.
- **Note**: In this codebase the core clinical tables already exist in the `ehr` schema via `001-initial-schema.xml`/`hospital.sql`. **Do not create parallel `hospital.*` tables.** The “If tables do not exist yet” bootstrap instructions from the original plan are not applicable here and are intentionally omitted to avoid duplicate schemas.

#### Phase 1.2 – Backend: Patient & Visit Context

- [x] Use existing package `com.easyops.hospital.entity` / `com.easyops.hospital.repository` / `com.easyops.hospital.service` for patient-related domain code (no new `domain.patient` package needed right now).
  - [x] Entity `Patient` mapped to `ehr.patients` with all required demographic and contact fields.
  - [x] Repository `PatientRepository extends JpaRepository<Patient, UUID>` with methods:
    - [x] `Optional<Patient> findByMrn(String mrn)`.
    - [x] Search helpers as needed for `/api/patients/search` (by name, MRN, phone) via `searchPatients`.
  - [x] DTOs:
    - [x] `PatientRequest` for create/update (`name`, DOB, gender, contacts, consents, etc.).
    - [x] `PatientResponse` for read operations (including IDs and timestamps).
    - [x] `PatientSummaryResponse` used by `/api/patients/{id}/summary` via `PatientSummaryController`.
  - [x] Service `PatientService`:
    - [x] `PatientResponse createPatient(PatientRequest request, UUID userId)`.
    - [x] `PatientResponse getPatientById(UUID id)` / `getPatientByMrn(String mrn)`.
    - [x] `PatientResponse updatePatient(UUID id, PatientRequest request, UUID userId)`.
    - [x] `List<PatientResponse> searchPatients(String searchTerm)` backing `/api/patients/search`.
  - _Note_: The existing implementation already exceeds the minimal plan with duplicate checking, MRN generation, and rich contact/consent handling.
- [x] Use existing `Encounter` model as the visit/encounter aggregate (no separate `Visit` entity in this codebase).
  - [x] Entity `Encounter` mapped to `ehr.encounters` with `visit_type`, status, dates, location, and provider fields.
  - [x] Repository `EncounterRepository` with methods:
    - [x] `findByPatientPatientIdOrderByStartDateDescStartTimeDesc(UUID patientId)` and other filters (status, type, organization, date range).
  - [x] DTOs:
    - [x] `EncounterRequest`, `EncounterResponse` (already used by `EncounterController`).
  - [x] Service `EncounterService`:
    - [x] `EncounterResponse createEncounter(UUID organizationId, EncounterRequest request, UUID userId)` exposed via `POST /api/encounters`.
    - [x] `EncounterResponse getEncounterById(UUID id)` and `getEncounterByNumber(String number)`.
    - [x] Query helpers such as `getEncountersByPatient`, `getActiveEncountersByPatient`, and organization/date‑range queries.
  - _Note_: The APIs currently use `/api/encounters` rather than `/visits`; in this implementation plan, **“visit” is represented by `Encounter`**, and no additional `Visit` aggregate is required unless we later decide to split them.

#### Phase 1.3 – Backend: Clinical Notes Context

- [x] Use existing `com.easyops.hospital.entity`, `repository`, `service`, and `controller` structure for clinical notes (no new `domain.clinicalnote` package needed).
  - [x] Entity `ClinicalNote` mapped to `ehr.clinical_notes` with SOAP sections, additional fields, status, authoring, signing, amendment, and versioning.
  - [x] Repository `ClinicalNoteRepository` with methods including:
    - [x] `List<ClinicalNote> findByEncounterId(UUID encounterId)`.
    - [x] `List<ClinicalNote> findByPatientPatientIdOrderByNoteDateDescNoteTimeDesc(UUID patientId)` and other helpers (current versions, signed/draft, search by content).
  - [x] DTOs:
    - [x] `ClinicalNoteRequest` (note type, content/sections, metadata).
    - [x] `ClinicalNoteResponse` (used throughout UI and APIs).
  - [x] Service `ClinicalNoteService`:
    - [x] `ClinicalNoteResponse createNote(ClinicalNoteRequest request, UUID userId)` (adds a note, optionally linked to an encounter).
    - [x] `ClinicalNoteResponse getNoteById(UUID noteId)`.
    - [x] `List<ClinicalNoteResponse> getNotesByPatient(UUID patientId)`, `getCurrentVersionNotesByPatient`, `getNotesByPatientAndType`, `getSignedNotesByPatient`, `getDraftNotesByPatient`, `searchNotesByContent`.
    - [x] Update/sign/amend/void/attachment/template operations as implemented.
  - _Note_: The existing implementation already provides a **richer clinical notes model** than the minimal “addNoteToEncounter/getNotesForEncounter” pair described originally; for this codebase, those requirements are satisfied (and exceeded) by the current `ClinicalNote*` stack.

#### Phase 1.4 – REST Controllers and API Wiring

- [x] Use existing controller package `com.easyops.hospital.controller` (no new `api` package needed).
- [x] Controllers:
  - [x] `PatientController` (already implemented):
    - [x] `POST /api/patients` → `createPatient`.
    - [x] `GET /api/patients/{patientId}` → `getPatientById`.
    - [x] `PUT /api/patients/{patientId}` → `updatePatient`.
    - [x] `GET /api/patients/search` → `searchPatients`.
    - [x] Additional helpers like `/api/patients/mrn/{mrn}`, `/api/patients/generate-mrn`, and duplicate‑check endpoints.
  - [x] `EncounterController` (serves as visit/encounter API):
    - [x] `POST /api/encounters` → `createEncounter`.
    - [x] `GET /api/encounters/{encounterId}` → `getEncounterById`.
    - [x] `GET /api/encounters/patient/{patientId}` and other filters for active/by org/date range.
  - [x] `ClinicalNoteController`:
    - [x] `POST /api/clinical-notes` → create clinical note (optionally with `encounterId`).
    - [x] `GET /api/clinical-notes/{noteId}` and `/api/clinical-notes/patients/{patientId}` plus type/signed/draft/search endpoints.
  - [x] `PatientSummaryController`:
    - [x] `GET /api/patients/{patientId}/summary` → `getPatientSummary`.
    - [x] Additional timeline/export/report endpoints under `/api/patients/{patientId}/summary/**`.
- [x] Ensure controllers use DTOs (`PatientRequest`/`PatientResponse`, `EncounterRequest`/`EncounterResponse`, `ClinicalNoteRequest`/`ClinicalNoteResponse`, etc.) rather than exposing entities directly.
- [x] Validation: controllers already use `@Valid` on request bodies and Bean Validation annotations on DTOs.
- _Note_: The original plan’s `/api/hospital/**` URL shape and separate `VisitController` are not used in this codebase; instead, we standardize on the existing `/api/patients`, `/api/encounters`, `/api/clinical-notes`, and `/api/patients/{patientId}/summary` endpoints, which fully satisfy (and extend) the Phase 1 requirements.

#### Phase 1.5 – Events, Security, and Gateway

This phase must **reuse platform services** and infrastructure that already exist:

- Event transport should use the shared event bus (Kafka/RabbitMQ) used by other EasyOps services.
- Authentication and RBAC should rely on `auth-service`, `rbac-service`, and `user-management-service` – `hospital-service` must **not** re‑implement auth or role logic, only consume identities/permissions they provide.

- **Event publishing (platform event bus – TODO):**
  - [x] Replace the internal logging-only `DomainEventPublisher` in `hospital-service` with an implementation that can publish to the **shared event bus** (Kafka/RabbitMQ) used elsewhere in the platform.
  - [x] Standardize event contracts for:
    - `patient.created`, `patient.updated`.
    - `encounter.created`, `encounter.updated`, `encounter.discharged` (representing visit lifecycle).
  - [x] Ensure `PatientService` and `EncounterService` emit these events **on successful transaction commit** only.
  - _Current state_: `DomainEventPublisher` is implemented by a Kafka-backed publisher when Kafka is configured for the service; a logging-only fallback is kept for local/dev usage.
- **Security and RBAC (using auth/RBAC/user services – partial):**
  - [x] Rely on `auth-service` and API Gateway to perform primary authentication and to attach the authenticated user (and roles/claims) to the request (e.g., via JWT) – `hospital-service` now assumes a real `X-User-Id` header and no longer fabricates random user IDs.
  - [x] In `hospital-service`, read the authenticated user/doctor ID from the propagated header instead of generating random UUIDs or relying on optional IDs for core Phase 1 flows (patients, encounters, clinical notes).
  - [ ] For operations that need explicit permission checks (e.g., creating/updating encounters, prescriptions, and clinical notes), either:
    - Use roles/permissions embedded in the JWT from `rbac-service`, or
    - Call `rbac-service` explicitly where fine-grained checks are required.
  - [ ] Align permission names with those defined in `002-hospital-permissions.sql` and the central RBAC model (do not introduce local ad‑hoc permission strings).
  - _Current state_: controllers accept optional `X-User-Id` and default to a random UUID; no method‑level RBAC checks are enforced inside `hospital-service` yet.
- **API Gateway and URL shape (DONE / documented):**
  - [x] `api-gateway` already routes `/api/hospital/**` to `hospital-service` and rewrites to the internal `/api/**` endpoints:
    - Route `id: hospital-service`, `uri: lb://hospital-service`, `predicates: Path=/api/hospital/**`.
    - Filter: `RewritePath=/api/hospital/(?<segment>.*), /api/${segment},/api/doctor-departments/**,/api/doctors/**`.
  - [x] In this implementation, **“visit” is represented by `Encounter`**:
    - External callers use `/api/hospital/encounters/**` (gateway) → `/api/encounters/**` (service).
    - Patient and notes APIs are similarly exposed as `/api/hospital/patients/**` and `/api/hospital/clinical-notes/**` at the gateway.
  - _Note_: No new gateway config is required for Phase 1; the key requirement is to keep documentation and client expectations aligned with this URL and naming scheme.

**Phase 1 Deliverables Checklist**

- [x] All APIs in §5.1 and relevant parts of §5.2/§5.4 for patient/visit/notes are implemented and tested (using existing `/api/patients`, `/api/encounters`, `/api/clinical-notes`, and `/api/patients/{id}/summary` endpoints where “visit” is modeled as `Encounter`).
- [x] Events for `patient.*` and `visit.*` (modeled as `encounter.*`) are emitted via `DomainEventPublisher` (Kafka in normal environments, logging fallback in local/dev).
- [ ] Basic RBAC enforced; unauthenticated or unauthorized calls are rejected.

### Phase 2 – Problems, Diagnoses, Allergies

- Implement:
  - `Diagnosis`, `Problem`, and `Allergy` entities and APIs.
  - Events: `diagnosis.added`, `allergy.added`.
- Integrate:
  - Visualization in patient summary APIs for portals and BFFs.
- Deliverables:
  - Structured clinical conditions and allergy tracking visible in clinician and patient views.

#### Phase 2.1 – Database (Liquibase)

- [ ] If not already present, in new changeset file `017-hospital-diagnoses-allergies.sql` (or next in sequence):
  - [ ] Create `hospital.diagnoses` (or `problems`) table with fields from §4.1:
    - `id`, `patient_id`, optional `visit_id`, optional `encounter_id`, `code`, `code_system`, `description`, `onset_date`, `status` (ACTIVE/RESOLVED), audit columns.
  - [ ] Create `hospital.allergies` table:
    - `id`, `patient_id`, `substance`, `reaction`, `severity`, `status` (ACTIVE/INACTIVE), audit columns.
  - [ ] Indexes:
    - `diagnoses (patient_id, status)`, `diagnoses (visit_id)`.
    - `allergies (patient_id, status)`.
- [ ] Include the new changeset in `db.changelog-master.xml`.

#### Phase 2.2 – Backend: Diagnosis & Problem Context

- [ ] Package `com.easyops.hospital.domain.diagnosis`.
  - [ ] Entity `Diagnosis` mapped to `hospital.diagnoses`.
  - [ ] Repository `DiagnosisRepository`.
  - [ ] DTOs:
    - `CreateDiagnosisRequest`, `DiagnosisResponse`.
  - [ ] Service `DiagnosisService`:
    - `DiagnosisResponse addDiagnosis(UUID patientId, CreateDiagnosisRequest request)`.
    - `List<DiagnosisResponse> getDiagnosesForPatient(UUID patientId)`.
    - Optionally, methods filtered by visit/encounter.

#### Phase 2.3 – Backend: Allergy Context

- [ ] Package `com.easyops.hospital.domain.allergy`.
  - [ ] Entity `Allergy`.
  - [ ] Repository `AllergyRepository`.
  - [ ] DTOs:
    - `CreateAllergyRequest`, `AllergyResponse`.
  - [ ] Service `AllergyService`:
    - `AllergyResponse addAllergy(UUID patientId, CreateAllergyRequest request)`.
    - `List<AllergyResponse> getAllergiesForPatient(UUID patientId)`.

#### Phase 2.4 – REST Controllers and Events

- [ ] Extend `DiagnosisController`/`AllergyController` in `com.easyops.hospital.api` or add endpoints to a `ClinicalDataController`:
  - [ ] `POST /api/hospital/patients/{patientId}/diagnoses`.
  - [ ] `GET /api/hospital/patients/{patientId}/diagnoses`.
  - [ ] `POST /api/hospital/patients/{patientId}/allergies`.
  - [ ] `GET /api/hospital/patients/{patientId}/allergies`.
- [ ] Event publishing:
  - [ ] Emit `diagnosis.added`, `diagnosis.updated` when diagnoses are created/updated.
  - [ ] Emit `allergy.added`, `allergy.updated` for allergy changes.
- [ ] Patient summary:
  - [ ] Update `PatientService`/`PatientSummaryResponse` so `/patients/{id}/summary` includes:
    - Active problems/diagnoses.
    - Active allergies.

**Phase 2 Deliverables Checklist**

- [ ] All APIs in §5.2 relevant to diagnosis and allergy are implemented.
- [ ] Patient summary includes diagnoses and allergies in a clinician-friendly format.
- [ ] Domain events for diagnoses and allergies are emitted and documented.

### Phase 3 – Prescriptions

- Implement:
  - `Prescription` and `PrescriptionLine` entities and APIs.
  - Status transitions and simple validation (allergy checks using in-service data).
  - Events: `prescription.created`, `prescription.updated`.
- Integrate:
  - Outgoing events for `hospital-pharmacy-service` to fulfill prescriptions.
  - Hooks for billing to recognize chargeable prescription lines (via clinical orders or pharmacy).
- Deliverables:
  - Fully electronic prescription capture, modifiable over time, with clear audit trail.
  - **UX**: Prescription entry form must use drug search/autocomplete backed by `hospital-pharmacy-service` (via BFF/gateway) instead of free-text medication entry; selecting a drug should pre-fill strength, form, route, and optional default frequency/duration, while still allowing overrides.

#### Phase 3.1 – Database (Liquibase)

- [ ] Review existing prescription-related changes in `004-add-prescription-fields.sql` and any other relevant changesets.
- [ ] If `prescriptions` and `prescription_lines` tables do not fully cover the model from §4.1:
  - [ ] Create new changeset file `018-hospital-prescriptions.sql`:
    - [ ] Create or alter `hospital.prescriptions`:
      - `id`, `patient_id`, `visit_id`, `encounter_id`, `prescribing_doctor_id`, `location_id`, `created_at`, `status` (DRAFT/SIGNED/CANCELLED/COMPLETED), audit columns.
    - [ ] Create or alter `hospital.prescription_lines`:
      - `id`, `prescription_id`, `drug_id`, `dose`, `route`, `frequency`, `duration`, `instructions`, `status` (ACTIVE/STOPPED/CHANGED/DISCONTINUED), audit columns.
    - [ ] Indexes:
      - `prescriptions (patient_id, created_at DESC)`, `prescriptions (visit_id)`.
      - `prescription_lines (prescription_id)`.

#### Phase 3.2 – Backend: Prescription Context

- [ ] Package `com.easyops.hospital.domain.prescription`.
  - [ ] Entities `Prescription`, `PrescriptionLine`.
  - [ ] Repositories `PrescriptionRepository`, `PrescriptionLineRepository`.
  - [ ] DTOs:
    - `CreatePrescriptionRequest`, `PrescriptionResponse`.
    - `AddPrescriptionLineRequest`, `PrescriptionLineResponse`.
  - [ ] Service `PrescriptionService`:
    - `PrescriptionResponse createPrescription(UUID encounterId, CreatePrescriptionRequest request)` – ensures encounter and patient exist; copies allergy info for validation.
    - `PrescriptionResponse getPrescription(UUID id)` – includes lines.
    - `PrescriptionResponse addLines(UUID prescriptionId, List<AddPrescriptionLineRequest> lines)`.
    - `PrescriptionResponse updatePrescription(UUID id, PrescriptionUpdateRequest request)` – status changes, metadata.
    - `PrescriptionLineResponse updatePrescriptionLine(UUID prescriptionId, UUID lineId, UpdatePrescriptionLineRequest request)` – dose changes, discontinue, etc.
    - `List<PrescriptionResponse> getPrescriptionsForPatient(UUID patientId)`.
- [ ] Implement simple in-service allergy checks:
  - [ ] Before creating prescription lines, read patient allergies and, if a matching `substance` or mapped drug-allergy exists, mark the line with a warning flag or reject per config.

#### Phase 3.3 – REST Controllers and Events

- [ ] `PrescriptionController` in `com.easyops.hospital.api`:
  - [ ] `POST /api/hospital/encounters/{encounterId}/prescriptions`.
  - [ ] `GET /api/hospital/prescriptions/{id}`.
  - [ ] `POST /api/hospital/prescriptions/{id}/lines`.
  - [ ] `PATCH /api/hospital/prescriptions/{id}`.
  - [ ] `PATCH /api/hospital/prescriptions/{id}/lines/{lineId}`.
  - [ ] `GET /api/hospital/patients/{patientId}/prescriptions`.
- [ ] Events:
  - [ ] Emit `prescription.created` when a prescription is first signed/created.
  - [ ] Emit `prescription.updated` when lines or status change.
  - [ ] Define event payloads including patient/visit/encounter IDs, prescribing doctor, and line details (for pharmacy).

#### Phase 3.4 – Integration with Pharmacy and Billing

- [ ] Outgoing integration:
  - [ ] For each `prescription.created` event, ensure `hospital-pharmacy-service` can consume and convert to its own order model (see pharmacy implementation plan).
  - [ ] For partial fulfillment updates from pharmacy, plan for future incoming events like `prescriptionline.fulfilled` (even if implemented later).
- [ ] Billing hooks:
  - [ ] Document that billable items for drugs will primarily come from `hospital-pharmacy-service` (sales/dispense), not directly from `hospital-service`, but ensure IDs and references are consistent so billing can relate charges to prescriptions if needed.

**Phase 3 Deliverables Checklist**

- [ ] All APIs in §5.3 are implemented and tested.
- [ ] Events for prescriptions are published and documented.
- [ ] Basic allergy conflict warnings/checks are in place.

### Phase 4 – Hardening, Refactoring, and Extraction Readiness

- Optimize:
  - Indexes and query performance for high-volume visits and prescriptions.
  - Caching for frequently accessed patient summaries.
- Introduce:
  - Internal modularization (packages) to separate:
    - Patient & visit.
    - Clinical documentation.
    - Prescription.
- Prep for possible future extraction:
  - Clearly defined internal interfaces so `Prescription` can be split into `hospital-prescription-service` without major consumer changes.

#### Phase 4.1 – Performance and Observability

- [ ] Add or refine indexes based on real query patterns (from logs / APM).
- [ ] Ensure pagination on all list endpoints to avoid unbounded result sets.
- [ ] Add metrics (via Micrometer) for:
  - [ ] Request counts and latencies per API.
  - [ ] Event publish failures/retries.
- [ ] Add structured logging around key domain operations (create visit, discharge, create prescription).

#### Phase 4.2 – Internal Modularization

- [ ] Refactor package structure under `com.easyops.hospital` to have clear modules:
  - `domain.patient`, `domain.visit`, `domain.encounter`, `domain.clinicalnote`, `domain.diagnosis`, `domain.allergy`, `domain.prescription`.
  - `api` package with controllers grouped logically.
  - `integration`/`events` package for event publishers and consumers.
- [ ] Introduce internal interfaces between modules where necessary (e.g., `PatientReadModel`, `VisitReadModel`) so future extractions can depend on interfaces rather than concrete JPA entities.

#### Phase 4.3 – API and Schema Review

- [ ] Review all APIs in §5 to ensure:
  - Stable URLs and request/response contracts.
  - Proper use of HTTP status codes and error bodies.
- [ ] Ensure OpenAPI documentation is complete and accurate.
- [ ] Add backward-compatible changes only (avoid breaking changes) from this point onwards.

**Phase 4 Deliverables Checklist**

- [ ] Service meets non-functional requirements in §7 for performance, availability, security, and audit.
- [ ] Package/module boundaries are clean enough to extract `Prescription` into a separate service if needed.
- [ ] Observability and documentation are sufficient for operations and future development.

## 9. Deployment and Migration Strategy

- **Initial deployment**:
  - Deploy alongside existing `hospital-service` behavior if partially implemented already, placing new modules behind versioned APIs.
- **Legacy data**:
  - If legacy HMS data exists, implement:
    - One-time import scripts for patients, visits, and key EHR data.
    - Mapping rules documented in legacy review plans.
- **Rollout**:
  - Start with a single pilot hospital/department.
  - Enable only Phase 1/2 features; add prescriptions (Phase 3) after validation.

## 10. Risks and Mitigations

- **Risk**: Overloading `hospital-service` with too many responsibilities.
  - **Mitigation**: Keep internal modules cleanly separated with clear boundaries and factor out non-core logic to other `hospital-*` services.
- **Risk**: Tight coupling with downstream services (billing, pharmacy, scheduling).
  - **Mitigation**: Use events and ID-based references instead of embedding downstream logic.
- **Risk**: Data model churn due to evolving clinical requirements.
  - **Mitigation**: Maintain versioned APIs and apply additive schema changes where possible.

