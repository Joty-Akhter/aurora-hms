## Hospital Module – Implementation Plan

This document provides an implementation-oriented view of the **Hospital Module** requirements under `requirements/module-hospital/`. It groups the requirement documents into logical domains, defines submodules and dependencies, and proposes a phased delivery plan.

---

## 1. Scope & Goals

- **Scope**
  - All requirements under `module-hospital` (clinical, operational, billing-facing, and support features).
  - Integrations with cross-cutting platform services (Accounting, Inventory, HR, Portal, etc.).
- **Goals**
  - Deliver an incrementally deployable Hospital Module.
  - Avoid duplication of cross-cutting functionality (security, billing engine, discount engine, corporate management, inventory, accounting).
  - Maintain clear bounded contexts and stable APIs between submodules.

**Card programs (authoritative taxonomy):** “Hospital card” is **not** one undifferentiated requirement. Programs are split across dedicated specs—see **[Hospital card programs – overview](hospital-card-types-overview.md)** (patient identity at registration, staff/employee identity, IPD temporary visitor fee cards, printed corporate/other benefit cards, optional wallet/prepaid). The technical **`hospital-card-management-service`** may host wallet, printed benefit, and identity-only card products; **IPD temporary visitor** cards remain a **separate** fee/refund module per [temporary-card-service-ipd.md](temporary-card-service-ipd.md). Implementation details: [hospital-card-management-service-implementation-plan.md](hospital-card-management-service-implementation-plan.md).

---

## 2. Logical Domains & Submodules

The Hospital Module is organized into the following domains and submodules (mapping to existing requirement documents).

### 2.1 Core Clinical & Patient Data

- **Patient Health Records** (`patient-health-records.md`)
  - Patient demographics, history, vitals, clinical notes, results, medications, and viewing capabilities.
  - Foundation for all clinical and many operational flows.
- **Prescription Management** (`prescription-management.md`)
  - Electronic prescriptions, interaction/allergy checks, pharmacy integration and refill flows.
- **LAB & Diagnostic – Patient-ID Wise Process** (`lab-diagnostic-module.md`)
  - Lab and diagnostics lifecycle, sample handling, microbiology, and imaging basics.
- **Blood Bank** (`blood-bank.md`)
  - Donor management, collection, screening, component management, cross-matching, issue to patient.

### 2.2 Inpatient (IPD) Workflows

- **Admission – IPD Patient Admission** (`admission-ipd.md`)
  - Admission workflow, bed allocation, guardians, corporate and billing rules.
- **Nurse Module – IPD Nursing & Bedside Operations** (`nurse-module.md`)
  - Nursing orders, requisitions/returns, vitals, bedside operations, bed status.
- **OT Procedure Entry – Operation Theatre (IPD)** (`ot-procedure-entry-ipd.md`)
  - OT scheduling, rooms, operation time tracking, multi-doctor roles and payment tagging.
- **Procedure Entry – IPD Procedures & Services** (`procedure-entry-ipd.md`)
  - IPD procedure capture, doctor tagging, link with IPD billing, discounts, and audit trail.
- **Temporary Card Service – IPD Visitor Cards** (`temporary-card-service-ipd.md`)
  - Visitor card issuance, status, payment and refund integration.

### 2.3 Clinical Charge Master & Hierarchy

- **Clinical Chart Management – Chargeable Items Master** (`clinical-chart-management.md`)
  - Central master for chargeable items (clinical, canteen, packages), rates and flags.
- **Department / Category Management – Service Hierarchy** (`department-category-management.md`)
  - Department and service classification hierarchy for mapping, billing, discounts and reporting.

### 2.4 Financial & Billing-Related Modules

- **Billing** (`billing.md`)
  - OPD/IPD billing console, discounts, approvals, credit/corporate billing and integration with Accounts and EHR.
- **Discount Management** (`discount-management.md`)
  - Centralized discount setup, assignment, priority and approvals.
- **Corporate Service & Card Management** (`corporate-service-and-card-management.md`)
  - Corporate clients, service types, card types, card policies, **printed** corporate benefit card issuance (generation/print via Hospital Card Service where integrated), and integration with Billing/Discount Management.
- **Hospital card programs – overview** (`hospital-card-types-overview.md`)
  - Single index of card types; links to patient identity, staff identity, temporary IPD, corporate, and wallet specs.

### 2.5 Pharmacy & Consumables

- **Pharmacy** (`pharmacy.md`)
  - Medicine master, pharmacy stock management, OPD sales, IPD issues, returns and reporting.

### 2.6 Hospital Operations & Admin

- **Doctor Module** (`doctor-module.md`)
  - Doctor registration, department mapping, scheduling and attendance.
- **Hospital Operations** (`hospital-operations.md`)
  - User management and permissions, core hospital settings, dashboards, marketing/B2B, canteen and reporting.
- **Certificate Management** (`certificate-management.md`)
  - Birth, death, discharge and transfer certificates, approvals and printouts.
- **Fixed Assets** (`fixed-assets.md`)
  - Asset register, depreciation, transfers, disposal and Accounting integration.
- **Ambulance** (`ambulance.md`)
  - Trip booking, charge calculation, ledger and due collection, driver/vehicle configuration.

### 2.7 Patient & B2B Portals

- **Portal** (`portal.md`)
  - Patient self-service portal, doctor portal, and B2B/corporate portal.

### 2.8 Non-Functional & Technical

- **Non-Functional Requirements** (`non-functional-requirements.md`)
  - Security, performance, quality, usability.
- **Technical Requirements** (`technical-requirements.md`)
  - Architecture, database, integration specifications, standards.
- **Integration with Accounting, Inventory, and HR Services** (`integration-services.md`)
  - Cross-module data flows and integration patterns.
- **Data Models and Relationships** (`data-models.md`)
  - Shared data definitions and relationships.

### 2.9 Canteen (Operational Submodule)

- **Canteen Module – Food & Meal Services** (`canteen-module-srs.md`)
  - Canteen operations (menu, stock, meal distribution, indoor/outdoor sales).
  - See also `canteen-module-implementation-plan.md` for more detail.

---

## 3. Cross-Cutting Platform Services (External Dependencies)

The Hospital Module relies on shared platform services that are not reimplemented here:

- **Accounts / Finance Service**
  - GL, AR, AP, asset accounting, discount posting and reconciliation.
- **Inventory / Store / Purchase Service**
  - PO, GRN, stock ledger, item valuation and main-store inventory.
- **HR / Staff Service**
  - Employee master, departments, roles and employment status.
- **Authentication / Authorization**
  - User identities, roles, SSO and central RBAC engine.
- **Master Data / Reference Data**
  - Common enumerations, codes, localization and configuration.

Hospital Module submodules should expose well-defined APIs and events for these services instead of embedding financial or HR logic.

---

## 4. Phased Implementation Plan

This plan focuses on building usable increments while respecting dependencies.

### Phase 0 – Foundations & Architecture

Phase 0 establishes the technical, data, integration, and governance groundwork needed for all subsequent phases. It is organized into four subphases: **0.1 Target Architecture & Deployment Model**, **0.2 Shared Data Models & Coding Standards**, **0.3 Integration Contracts with Platform Services**, and **0.4 Security, RBAC & Observability Baseline**.

#### Phase 0.1 – Target Architecture & Deployment Model

- **Objectives**
  - Decide on target runtime architecture for the Hospital Module (modular monolith vs multi-service deployment).
  - Define boundaries between Hospital Module contexts and shared platform services.
- **Scope**
  - Analyze current ERP architecture and constraints (tech stack, deployment, infra, performance).
  - Document candidate options (e.g., single deployable with clear modules vs separate services for Billing, Lab, Pharmacy, etc.).
  - Define:
    - Module boundaries and ownership (Hospital vs platform-level).
    - Communication patterns (synchronous APIs vs events) between Hospital Module and cross-cutting services.
    - Environments and deployment topology (dev, staging, production; multi-tenant vs single-tenant).
  - Align with **Non-Functional Requirements** and **Technical Requirements** docs.
- **Deliverables**
  - Approved architecture decision record (ADR) for Hospital Module.
  - High-level context and component diagrams showing module boundaries and integrations.
  - Deployment model description and initial capacity/performance assumptions.

#### Phase 0.2 – Shared Data Models & Coding Standards

- **Objectives**
  - Establish a consistent data model and code conventions across all Hospital Module submodules.
  - Minimize duplication and drift between Hospital Module and platform services.
- **Scope**
  - Review and refine shared entities from `data-models.md` (patients, encounters, departments, services, charges, discounts, corporates, etc.).
  - Define:
    - Common identifiers and key relationships (patient ID, visit/encounter ID, order IDs, billing IDs).
    - Versioning and evolution strategy for core entities.
  - Establish technical standards from `technical-requirements.md`:
    - API style (REST conventions, pagination, error format).
    - Coding standards (language/framework-specific guidelines, naming, linting, testing minimums).
    - Database conventions (schema naming, migration strategy, soft delete/audit fields).
  - Create shared libraries/modules for cross-cutting models and utilities where appropriate.
- **Deliverables**
  - Consolidated Hospital Module data model diagrams and glossary.
  - Coding standards and API guidelines document referenced by all sub-teams.
  - Initial shared libraries (e.g., core domain models, API error envelope, base repository/util components).

#### Phase 0.3 – Integration Contracts with Platform Services

- **Objectives**
  - Define stable integration contracts between Hospital Module and Accounting, Inventory, HR, Auth, and other ERP core modules.
  - Ensure Hospital Module can evolve independently without breaking platform services.
- **Scope**
  - Identify cross-boundary flows (e.g., billing postings to Accounts, stock consumption to Inventory, staff details from HR).
  - For each integration, specify:
    - Owning system and source of truth.
    - API endpoints/events, payload structures, and error semantics.
    - Latency, throughput, and availability expectations.
  - Decide authentication/authorization approach for service-to-service calls.
  - Plan backward-compatibility and migration strategy if existing integrations already exist.
- **Deliverables**
  - Integration specification/contract document per major platform service:
    - **Accounting/Finance**, **Inventory/Store**, **HR/Staff**, **Authentication/Authorization**, **Master Data**.
  - Example payloads and API definitions (OpenAPI/AsyncAPI or equivalent).
  - List of integration test cases and mock/stub services to be used in later phases.

#### Phase 0.4 – Security, RBAC & Observability Baseline

- **Objectives**
  - Provide a reusable baseline for security, RBAC, audit logging, and observability so that later phases can plug into a consistent framework.
- **Scope**
  - Security & RBAC:
    - Integrate with central Auth/RBAC engine (single sign-on, token/credential handling).
    - Define role and permission primitives used by Hospital Module (e.g., doctor, nurse, billing clerk, admin) without over-detailing module-specific permissions yet.
    - Implement common authorization middleware/filters and enforcement hooks.
  - Audit & Logging:
    - Define audit events and minimal audit fields (who, what, when, where, previous vs new values).
    - Implement shared audit logging utilities and persistence strategy (or integration with existing platform audit store).
    - Standardize application logging format, correlation IDs, and error categories.
  - Observability:
    - Establish metrics to be tracked across modules (e.g., request latency, error rates, key business counters).
    - Set up tracing/monitoring integration (APM, dashboards, alert templates).
- **Deliverables**
  - Security/RBAC integration blueprint and base implementation (auth middleware, role/permission checks, common error responses).
  - Audit logging and standardized logging libraries ready for reuse by all submodules.
  - Initial observability setup (dashboards, key metrics definitions, tracing configuration templates).

### Phase 1 – Core Masters & Clinical Charge Backbone

This phase establishes the shared classification and charge master backbone that all clinical, operational, and billing modules depend upon.

#### Phase 1.1 – Department / Category Hierarchy Foundations

- **Objectives**
  - Model the full department and service hierarchy (department → sub-department → sub-sub-department → head-group, etc.).
  - Provide consistent classification codes to be reused by Clinical Chart, Billing, Discounts, Lab, Pharmacy, Canteen, and Reporting.
- **Scope**
  - Implement **Department / Category Management** (`department-category-management.md`):
    - CRUD for departments, sub-departments, groups/head-groups.
    - Status (Active/Inactive) and effective date ranges.
    - Mapping to cost centers and reporting dimensions (where applicable).
  - Seed initial hierarchy for pilot hospitals, with configuration support for local customization.
- **Activities**
  - Run joint workshops with finance, clinical, operations and reporting stakeholders to define the target department/service structure.
  - Model the hierarchy in a sandbox environment and validate key reports (revenue by department, utilization, discount analysis) and billing flows.
  - Define migration/mapping rules from any existing legacy department/service codes to the new structure.
  - Implement import/export utilities to manage bulk updates of the hierarchy (e.g., Excel/CSV based where appropriate).
- **Deliverables**
  - Department/category master data model and APIs.
  - Admin UI for creating and maintaining hierarchy.
  - Reference documentation for how other modules should use department/category codes.
  - Mapping guide for legacy → new codes (if applicable).
  - Exit criteria: at least one pilot hospital’s hierarchy configured and validated against reporting/billing scenarios.

#### Phase 1.2 – Clinical Chart Master (Chargeable Items)

- **Objectives**
  - Establish a single, configurable master for all chargeable clinical items, canteen items, and packages.
  - Ensure every billable item is tagged with the correct department/category and other attributes needed for billing and reporting.
- **Scope**
  - Implement **Clinical Chart Management** (`clinical-chart-management.md`):
    - Service/item master with codes, names, units, rate types (fixed/variable), and tax flags.
    - Mapping to department/category hierarchy from Phase 1.1.
    - Flags for discount behavior, package inclusion, and activation status.
  - Support configurable rate plans and branches/sites where needed (implementation detail).
- **Activities**
  - Define service coding standards (code patterns, naming conventions) in collaboration with billing and clinical leads.
  - Identify service groups and attributes required by Discount Management, Corporate Card, Reporting and Accounting.
  - Design and implement UI workflows for adding, updating, inactivating services, including validation rules to prevent duplicates or inconsistent mappings.
  - Plan and execute initial chart seeding:
    - From greenfield templates, or
    - Via migration scripts from legacy HMS catalogues.
  - Align rate plan strategy with finance (base rates vs contract/corporate overrides handled in other modules).
- **Deliverables**
  - Clinical Chart data model and APIs for:
    - Service lookup and selection.
    - Rate and attribute retrieval.
  - Admin UI for clinical chart maintenance (with RBAC and audit).
  - Initial seeding/migration strategy (if moving from legacy HMS).
  - Exit criteria: all services needed for Phases 2–4 present in the chart, with correct department/category and discount flags.

#### Phase 1.3 – Cross-Module Alignment & Governance

- **Objectives**
  - Align masters from Phases 1.1 and 1.2 with downstream modules and external services.
  - Define governance rules so new services/departments can be added without breaking integrations.
- **Scope**
  - Align:
    - Codelists and hierarchies with Accounting (GL mappings, revenue groups), Lab, Pharmacy, Canteen, and Billing.
    - Discount groups and eligibility tags used by **Discount Management** and **Corporate Service & Card Management**.
  - Define:
    - Naming conventions, code patterns, and versioning rules for chart and department entries.
    - Approval workflow for structural changes (e.g., adding/removing departments, merging services).
- **Activities**
  - Conduct integration design sessions with each consumer module (Billing, Lab, Pharmacy, Canteen, Ambulance, etc.) to agree how they reference departments and services.
  - Document GL mapping rules between Clinical Chart/Departments and Accounting service (revenue and discount accounts).
  - Define governance forums/owners for approving new departments/services and major structural changes.
  - Implement simple automated checks (linting scripts or CI jobs) that validate new/changed codes against conventions and uniqueness constraints.
- **Deliverables**
  - Integration specification describing how each consumer (Billing, Lab, Pharmacy, Canteen, etc.) uses department and clinical chart codes.
  - Governance document/checklist for adding or modifying services and departments.
  - Validation rules and automated checks to prevent inconsistent or duplicate coding.

#### Phase 1 Outcomes

- **Single source of truth** for chargeable items and service hierarchy.
- **Stable references** and APIs that all billing, discount, and reporting modules use for lookups and pricing.
- **Governed change process** so future additions to services/departments remain compatible across the ecosystem.

### Phase 2 – Patient Health Records & Prescription

- Implement:
  - **Patient Health Records**.
  - **Prescription Management**.
- Integrate with:
  - Lab/Diagnostic and Pharmacy for orders and result/dispense flows.
- Outcomes:
  - Clinically usable core EHR (viewing and documenting patient data).
  - Foundation for IPD, OPD and specialty workflows.

This phase delivers the core electronic health record (EHR) and prescription capabilities that all clinical workflows build upon.

#### Phase 2.1 – PHR Data Model & Infrastructure

- **Objectives**
  - Define and implement the core PHR data model and APIs.
  - Ensure alignment with shared `data-models.md` and Phase 0 standards.
- **Scope**
  - Model key PHR entities:
    - Patient profile (linking to central patient registry where applicable).
    - Encounters/visits (OPD, IPD, emergency).
    - Clinical documents/notes, vitals, diagnoses/problem list, orders, and results references.
  - Implement:
    - Core PHR storage layer (database schemas, repositories).
    - Service APIs for retrieving and storing PHR content (read/write).
  - Apply:
    - RBAC, audit logging, and observability baselines from Phase 0.
- **Activities**
  - Finalize the logical and physical data model for PHR entities, reusing shared entity definitions where possible.
  - Design and document versioning and correction patterns for clinical data (amend vs append-only notes, audit trails).
  - Implement PHR service endpoints (REST/GraphQL, etc.) and wire them into the shared auth/audit frameworks.
  - Create automated tests and seed datasets for core PHR flows (typical OPD/IPD encounters).
- **Deliverables**
  - PHR domain model diagrams and API specifications.
  - Initial PHR service (or module) with basic CRUD for core entities.
  - Test data and migration strategy (if legacy EHR exists).

#### Phase 2.2 – Clinical Documentation & Viewing Flows

- **Objectives**
  - Provide clinicians with usable workflows to document and view patient information.
- **Scope**
  - Implement:
    - Clinical notes and documentation UI (progress notes, summary notes, templates where applicable).
    - Vitals capture flows and trending views.
    - Problem list/diagnoses management screens.
    - Consolidated patient overview (timeline or summary view).
  - Ensure:
    - Fast patient/visit lookup and context switching.
    - Read-only views are optimized for performance (frequent access).
- **Activities**
  - Conduct UX design sessions with clinicians to define minimal but effective documentation and viewing flows.
  - Build reusable UI components for notes, vitals charts, and problem lists that can be embedded in IPD/OPD screens.
  - Optimize queries and caching strategies for high-frequency read paths (e.g., ward rounds, OPD queues).
  - Define printing/export requirements (e.g., clinical summaries, discharge summaries) that will leverage PHR data.
- **Deliverables**
  - End-to-end user flows for viewing and documenting key PHR data.
  - UX patterns and UI components to be reused by IPD/OPD and specialty modules.

#### Phase 2.3 – Prescription Management Core

- **Objectives**
  - Implement the prescription engine and integration points for medication ordering.
- **Scope**
  - Implement **Prescription Management** (`prescription-management.md`):
    - Prescription creation, editing, and cancellation.
    - Drug search/selection (backed by medicine master from Pharmacy or shared catalog).
    - Dosage, frequency, duration, route and instructions capture.
  - Integrate with:
    - PHR encounter/visit model for context.
    - Pharmacy module for dispensing workflows (at minimum, order handoff).
  - Prepare hooks for:
    - Drug interaction and allergy checking (can be advanced in later phases if needed).
- **Activities**
  - Design prescription templates and common order sets with clinicians.
  - Implement medication search leveraging Pharmacy’s master data and formulary restrictions.
  - Implement status tracking for prescriptions (draft, signed, cancelled, dispensed/partially dispensed).
  - Define extension points for clinical decision support (alerts for allergies, interactions, duplicate therapy).
- **Deliverables**
  - Prescription domain model and APIs.
  - UI for clinicians to create and manage prescriptions.
  - Basic integration contract with Pharmacy (order messages/requests).

#### Phase 2.4 – Orders, Results & External Module Integrations

- **Objectives**
  - Connect PHR and prescriptions with Lab/Diagnostic and Pharmacy modules.
- **Scope**
  - Integrate with:
    - **LAB & Diagnostic**:
      - Order placement interface from PHR (lab/imaging requests).
      - Result links or embedding into PHR views (e.g., latest lab results section).
    - **Pharmacy**:
      - Prescription-to-dispense flow (status updates from Pharmacy back to PHR).
  - Implement:
    - Order tracking in PHR (requested, in progress, completed, cancelled).
    - Minimal result viewing within PHR (link out or embedded summary).
- **Activities**
  - Define canonical order and result payloads in collaboration with Lab/Diagnostics and Pharmacy teams.
  - Implement order placement UIs and API clients inside PHR/Prescription modules.
  - Implement result ingestion or linking logic to surface key results on PHR views.
  - Create integration tests and sandbox environments for end-to-end order/result scenarios.
- **Deliverables**
  - End-to-end test scenarios for orders and results across PHR, Lab and Pharmacy.
  - Documentation of integration patterns for future extensions (e.g., additional diagnostic services).

### Phase 3 – IPD Admission & Nursing Workflows

This phase operationalizes inpatient (IPD) care by implementing structured admission, bed/ward management, and nursing workflows on top of the PHR and masters built in earlier phases.

#### Phase 3.1 – IPD Admission & Bed/Ward Management

- **Objectives**
  - Provide end-to-end IPD admission capability from pre-admission through discharge initiation.
  - Ensure every IPD stay is consistently linked to PHR, Billing, and department/clinical chart masters.
  - Establish a reliable bed/ward/room occupancy model for operational visibility and charge capture.
- **Scope**
  - Implement **Admission – IPD Patient Admission** (`admission-ipd.md`):
    - Patient registration/selection (leveraging central patient registry/PHR where applicable).
    - Admission request and approval, including admission type (cash, credit/corporate, insurance, etc.).
    - Bed/ward/room selection based on configured bed hierarchy and availability.
    - Capture of primary consultant/doctor, admitting department, and admission reason.
    - Handling of guardians/attendants, contact details, and consent forms (where specified).
  - Bed & Ward Management (aligned with **Hospital Operations** where centralized):
    - Bed master configuration (ward → room → bed, with type/class, gender rules, isolation flags, etc.).
    - Bed status lifecycle (available, blocked, occupied, cleaning, under maintenance).
    - Transfers within hospital (bed-to-bed, ward-to-ward, ICU transfers) with appropriate audit.
    - Support for temporary holds/reservations for pre-admissions where required.
  - Financial & linkage aspects:
    - Link admissions to billing accounts, payer type, and corporate/contract details (where available).
    - Ensure every admission and transfer is traceable for charge calculation and room rent logic in later phases.
  - Integration touchpoints:
    - PHR: creation/linking of IPD encounter record and visit identifiers.
    - Department/Clinical Chart: department tagging for admission and bed/ward.
    - Billing: admission-level financial account/episode that Billing module will later consume.
- **Activities**
  - Design admission and bed management flows in collaboration with nursing, admission desk, and medical administration teams.
  - Configure and validate bed masters for pilot sites, including special care areas (ICU, HDU, isolation).
  - Implement admission UI screens:
    - Patient search/selection.
    - Admission details capture and confirmation.
    - Bed/ward selection with real-time occupancy view.
  - Implement transfer and discharge-initiation flows (final discharge billing remains in Phase 5, but discharge initiation must be available here).
  - Implement necessary APIs/events:
    - Admission created/updated/cancelled events.
    - Bed status change events for use by dashboards and housekeeping systems (if any).
  - Define basic reporting views for bed occupancy, admissions by department, and current census.
- **Deliverables**
  - IPD Admission domain model and APIs aligned with PHR and Billing identifiers.
  - Bed/ward/room master and occupancy model with UI for configuration and daily operation.
  - Admission and intra-hospital transfer workflows available and validated in at least one pilot ward.
  - Documentation of integration events and contracts for downstream consumers (Billing, dashboards, housekeeping).

#### Phase 3.2 – Nursing & Bedside Care Workflows

- **Objectives**
  - Enable nursing staff to manage day-to-day bedside activities for IPD patients efficiently.
  - Ensure nursing actions (vitals, requisitions, returns, bedside services) are captured in a structured, auditable way.
  - Provide a real-time operational view of ward/bed status and patient care tasks.
- **Scope**
  - Implement **Nurse Module – IPD Nursing & Bedside Operations** (`nurse-module.md`):
    - Nursing dashboards for ward/bed-wise patient lists and quick access to key actions.
    - Vitals capture (leveraging PHR vitals infrastructure from Phase 2.2) with ward-friendly UI.
    - Bedside orders/requisitions and returns (e.g., consumables, investigations, procedures) referencing Clinical Chart services.
    - Nursing notes and shift handover summaries where defined in requirements.
    - Bed status updates (occupied, in transit, under procedure) as driven by nursing workflows.
  - Task & workflow support:
    - To-do/task lists per nurse/ward (medication rounds, vitals due, investigations pending etc., scoped to MVP).
    - Basic support for nursing care plans if part of the initial scope, otherwise hooks for later enhancement.
  - Integration touchpoints:
    - PHR: all vitals, notes and care events recorded against the correct encounter/visit.
    - Lab/Diagnostics & Pharmacy: requisitions/orders created from nursing screens should reuse PHR/Prescription ordering where possible.
    - Billing & Clinical Chart: bedside chargeable services (e.g., nursing procedures, consumables) tagged with correct department/service codes for later billing.
    - Bed Management: live reflection of bed occupancy and patient location.
- **Activities**
  - Conduct workflow mapping sessions with nursing staff for typical wards (general, ICU, maternity etc.) and define an MVP nursing console.
  - Design and implement ward/bed boards and patient cards with key information and quick actions.
  - Implement nursing requisition flows:
    - From bed/patient context, select service/consumable (using Clinical Chart).
    - Capture quantities, timings, and ordering doctor where relevant.
    - Record returns/cancellations with reason codes.
  - Implement vitals and nursing notes capture optimized for speed and low-friction data entry (shortcuts, templates).
  - Define and implement basic nurse-facing reports (e.g., vitals trends per bed, pending tasks).
  - Ensure all actions follow RBAC and audit rules from Phase 0 (who did what, when, where).
- **Deliverables**
  - Nurse console UI (ward/bed board) and underlying APIs.
  - Integrated flows for vitals, bedside orders/requisitions, returns, and nursing notes linked to PHR and masters.
  - Initial set of nursing-side reports or dashboards for operational oversight.
  - Exit criteria: at least one pilot ward using the nurse module for daily bedside operations for a defined pilot period.

#### Phase 3.3 – IPD Operational Governance & Pre-Billing Hooks

- **Objectives**
  - Ensure that IPD operational data captured in Phases 3.1 and 3.2 is clean, consistent, and ready for billing and analytics in later phases.
  - Define governance and controls over admission, transfer, and bedside service capture.
- **Scope**
  - Data quality & governance:
    - Validation rules for mandatory data at admission (doctor, department, payer type, etc.).
    - Constraints to prevent inconsistent bed states (double-booking, conflicting statuses).
    - Rules for when bedside services can be captured (e.g., only when patient is admitted and bed-assigned).
  - Pre-billing hooks:
    - Capture of room/bed class and effective timespans for stay segments (to allow accurate room rent calculation later).
    - Event hooks and summary views that Billing can later consume to compute charges (without yet implementing full billing engine).
  - Operational controls:
    - Maker-checker or approval flows where required (e.g., admission cancellation, backdated transfers).
    - Simple audit and reconciliation views for admissions, transfers, and nursing service capture.
- **Activities**
  - Define IPD operational policies with hospital administrators (e.g., who can admit, transfer, cancel, or backdate actions).
  - Implement validation and guardrails in UI and APIs for admission and nursing flows.
  - Design and implement pre-billing summary screens (e.g., stay summary per patient, services rendered per stay) that will later plug into Billing.
  - Configure dashboards for operations leads to monitor census, bed occupancy, and key IPD KPIs.
- **Deliverables**
  - Governance and validation rules implemented across admission and nursing workflows.
  - Pre-billing summary and event hooks ready for consumption by the Billing module in Phase 5.
  - Operational dashboards/views for census and occupancy monitoring.
  - Exit criteria: IPD admission and nursing workflows running in production/pilot with data quality sufficient for later billing and analytics phases.

### Phase 4 – OT, Procedures & IPD Services

This phase builds on IPD admission and nursing to cover surgical/OT workflows, IPD procedure and service capture, and IPD visitor/temporary card management, ensuring accurate charge capture and provider tagging.

#### Phase 4.1 – OT Procedure Entry (Operation Theatre – IPD)

- **Objectives**
  - Provide end-to-end management of OT procedures for IPD patients, from scheduling to completion.
  - Ensure OT activities are consistently linked to PHR, IPD admissions, and downstream billing.
  - Capture all key clinical and operational data points related to surgeries/procedures.
- **Scope**
  - Implement **OT Procedure Entry – Operation Theatre (IPD)** (`ot-procedure-entry-ipd.md`):
    - OT case creation from IPD admission/encounter context (patient, bed, ward).
    - OT scheduling (date/time, theatre/room, estimated duration).
    - Allocation of surgeons, assistant surgeons, anesthetists, nurses, and support staff.
    - Pre-op checks and status (fitness, consents, investigations).
    - Intra-op status updates (start/stop times, complications, notes) where defined.
    - Post-op tracking (recovery location, transfer back to ward/ICU).
  - OT resource & calendar management:
    - Theatre/room master with attributes (type, equipment, constraints).
    - OT schedule/roster views (per theatre, per surgeon, per date).
    - Handling of delays, cancellations, and rescheduling with reasons.
  - Integration touchpoints:
    - PHR: OT notes, procedure details, and key timestamps recorded in the patient record.
    - IPD Admission/Bed Management: updates to bed/ward status (e.g., in OT, in recovery).
    - Doctor Module: mapping of surgeons/anesthetists to provider master for later payment tagging.
    - Billing & Clinical Chart: hooks to link OT procedures to appropriate billable services (without yet implementing full billing engine).
- **Activities**
  - Map typical OT workflows with surgeons, anesthetists, and OT nursing teams to define MVP flows.
  - Design and implement OT scheduling UI and calendars.
  - Implement OT case lifecycle management (created → scheduled → in-progress → completed/cancelled).
  - Implement APIs/events for OT case creation and status changes consumable by PHR, Bed Management, and future Billing.
  - Define basic OT reports (theatre utilization, case counts by department/procedure type, cancellations).
- **Deliverables**
  - OT procedure entry domain model and APIs.
  - OT scheduling and case management UI.
  - Integration events/contracts with PHR, IPD Admission/Bed Management, and Doctor Module.
  - Exit criteria: at least one OT running pilot workflows for scheduled IPD surgeries with complete basic data capture.

#### Phase 4.2 – IPD Procedure Entry & Chargeable Services

- **Objectives**
  - Provide structured capture of IPD procedures and services (beyond OT) for both clinical documentation and later billing.
  - Ensure each procedure/service is linked to Clinical Chart items, departments, and responsible providers.
- **Scope**
  - Implement **Procedure Entry – IPD Procedures & Services** (`procedure-entry-ipd.md`):
    - Catalog of procedure types/services (linked to Clinical Chart master).
    - Procedure entry from IPD context (ward, bed, specialty units).
    - Ability to capture responsible doctor(s), assisting staff, and timestamps.
    - Status tracking (planned, in-progress, completed, cancelled).
  - Linkage to chargeable services:
    - Mapping between clinical procedures and one or more billable items in Clinical Chart.
    - Support for multiple occurrences (e.g., repeated procedures during stay).
    - Capture of quantity, modifiers, and notes affecting billing (e.g., emergency, after-hours flags where relevant).
  - Integration touchpoints:
    - PHR: procedure documentation and summary available in patient timeline.
    - Billing & Discount Management: exposure of structured procedure events and mapped service codes for later charge calculation and discount rules.
    - Doctor Module: doctor tagging for revenue/payment attribution.
- **Activities**
  - Work with clinical leaders to define the initial scope of IPD procedures and services to be captured.
  - Design and implement IPD procedure entry screens accessible from nurse, doctor, and IPD consoles where appropriate.
  - Implement mapping configuration between procedure definitions and Clinical Chart items.
  - Implement APIs/events for procedure completion that downstream Billing can consume in Phase 5.
  - Define reports/views for procedures per patient, per ward, per doctor, and per department.
- **Deliverables**
  - IPD procedure entry module with UI and APIs.
  - Configured mappings between procedures and Clinical Chart services for pilot specialties.
  - Procedure datasets and views ready for consumption by Billing and analytics.
  - Exit criteria: structured procedure capture in at least one pilot service line (e.g., general surgery or orthopedics).

#### Phase 4.3 – Temporary Card Service for IPD Visitor Management

- **Objectives**
  - Implement controlled IPD visitor access using temporary/visitor cards, integrated with IPD admissions and Billing.
  - Ensure visitor card issuance, usage, and refunds (where applicable) are traceable.
- **Scope**
  - Implement **Temporary Card Service – IPD Visitor Cards** (`temporary-card-service-ipd.md`):
    - Visitor/temporary card types and policies (e.g., refundable deposit, daily/visit-based access).
    - Card issuance against specific IPD admission/bed/patient context.
    - Tracking of card status (issued, active, lost, returned).
    - Handling of refunds, forfeitures, and replacements as per policy.
  - Integration touchpoints:
    - IPD Admission: link visitor cards to the relevant admission/bed and store basic visitor info if required.
    - Billing & Discount Management:
      - Posting of deposits and fees as billable items.
      - Rules for refunds and write-offs to be consumed in Phase 5 Billing implementation.
    - Doctor/Hospital Operations modules: visibility into visitor access policies and exceptions.
- **Activities**
  - Define visitor policies and operational flows with hospital administration and security.
  - Design and implement visitor card issuance/return UI, likely used by reception or ward clerks.
  - Implement card inventory and numbering logic (if physical cards/badges are tracked).
  - Implement APIs/events for card issuance, status changes, and refunds to be consumed by Billing and operational dashboards.
  - Define basic reports (cards issued per ward, active cards, deposit/refund summaries).
- **Deliverables**
  - Temporary Card Service module with issuance, tracking, and return flows.
  - Integration events/contracts with IPD Admission and Billing.
  - Operational views/reports for visitor card monitoring.
  - Exit criteria: visitor card management in use for at least one pilot ward with deposit/fee information ready for later Billing integration.

### Phase 5 – Billing, Discounts & Corporate Management

This phase introduces the central financial control layer for the Hospital Module: unified billing, discount management, and corporate service/card management, tightly integrated with Accounting and all charge-producing modules.

#### Phase 5.1 – Core Billing Console & Charge Aggregation

- **Objectives**
  - Provide a unified billing console for OPD and IPD (and basic canteen linkage) that consolidates charges from all upstream clinical and operational modules.
  - Ensure accurate, auditable patient and corporate billing, with clear separation between clinical data and financial postings.
- **Scope**
  - Implement **Billing** (`billing.md`):
    - Patient account and visit/episode billing structures (OPD visit, IPD stay, package, corporate episode).
    - Charge aggregation from:
      - IPD services/procedures (Phase 4).
      - Lab/Diagnostics orders and results (later phases).
      - Pharmacy issues/returns.
      - Canteen and Ambulance (where applicable).
      - Visitor/temporary cards and deposits from Phase 4.3.
    - Support for multiple payer types per account (self, corporate, insurer, government schemes).
    - Posting of room/bed charges based on pre-billing hooks from Phase 3.3.
  - Billing workflows:
    - Provisional bills and interim statements.
    - Final bill generation with summaries by service category/department.
    - Handling of advances, deposits, and adjustments.
  - Integration touchpoints:
    - Accounts/Finance: GL postings, AR, and reconciliation via defined integration contracts.
    - Discount Management engine and Corporate Service & Card Management (for applying discounts/entitlements).
    - All charge-producing modules (event-driven or API-based charge feeds).
- **Activities**
  - Design billing data model aligned with Accounting’s chart of accounts and Hospital Clinical Chart/Department masters.
  - Implement billing UIs for:
    - OPD billing (quick visit-level billing).
    - IPD billing (stay-level, interim and final bills).
  - Implement charge ingestion layer (APIs/events) consuming upstream events from IPD, OT, Lab, Pharmacy, Canteen, Ambulance, Temporary Card, etc.
  - Implement billing calculations (totals, taxes where applicable, adjustments, write-offs) according to finance rules.
  - Define and implement core billing reports (bill registers, collection reports, ageing snapshots at a minimum).
- **Deliverables**
  - Billing domain model, APIs, and console UIs for OPD/IPD.
  - Charge aggregation pipeline and event handlers wired to key upstream modules.
  - Integration with Accounting/Finance via agreed posting interfaces.
  - Exit criteria: pilot hospital able to run full billing for selected OPD and IPD cases end-to-end, including postings into Accounting test environment.

#### Phase 5.2 – Central Discount Management Engine

- **Objectives**
  - Implement a centralized, rules-driven discount management engine used consistently across all modules.
  - Provide governance, approvals, and audit for discounts at patient, service, department, and corporate levels.
- **Scope**
  - Implement **Discount Management** (`discount-management.md`):
    - Discount definitions:
      - Percentage/amount discounts.
      - Item/department/category-based eligibility using Clinical Chart and Department masters.
      - Scheme/campaign-based discounts (if in initial scope).
    - Priority and stacking rules (which discounts can combine, and in what order).
    - Eligibility rules by payer type, corporate/contract, and patient category.
  - Operational flows:
    - Request and approval workflows for exceptional discounts (doctor, billing clerk, admin).
    - Limit controls (max discount percentages by role, service category, or bill amount).
    - Audit logs for who granted which discount and why.
  - Integration touchpoints:
    - Billing engine: discount calculation hooks when computing bill totals.
    - Corporate Service & Card Management: mapping of corporate/contract rules into discount definitions.
    - Accounts/Finance: GL mapping for discount postings.
- **Activities**
  - Define discount policy matrix with finance and hospital administration (what’s allowed by default vs by exception).
  - Design discount configuration UI for finance/admin teams.
  - Implement discount evaluation engine invoked from Billing calculations:
    - Input: billable items, payer context, corporate/contract, patient attributes.
    - Output: applied discounts with breakdown and rationale.
  - Implement approval workflows and audit trail for manual/exceptional discounts.
  - Validate discount scenarios with real-world test cases (e.g., corporate patients, charity discounts, promotional schemes).
- **Deliverables**
  - Discount engine services and configuration UI.
  - Policies, rules, and approval workflows wired into Billing.
  - Reporting on discounts (by department, doctor, payer, scheme).
  - Exit criteria: discounts applied consistently for pilot hospitals with finance sign-off on correctness and controls.

#### Phase 5.3 – Corporate Service & Card Management

- **Objectives**
  - Implement robust management of corporate clients, contracts, and cards, enabling cashless or semi-cashless billing and negotiated discount structures.
  - Integrate corporate rules directly with Billing and Discount Management.
- **Scope**
  - Implement **Corporate Service & Card Management** (`corporate-service-and-card-management.md`):
    - Corporate master data (companies, TPAs, government schemes).
    - Contract management:
      - Contracted services and packages.
      - Tariff/price lists per corporate where applicable.
      - Contracted discounts and co-pay rules.
    - Corporate card policies:
      - Card types (employee, dependent, VIP, etc.).
      - Limits (amount, visits, services).
      - Validity periods and blocking/blacklisting.
  - Integration touchpoints:
    - Billing:
      - Corporate eligibility checks at registration/admission/billing.
      - Application of contract tariffs and co-pays.
      - Corporate billing and invoices (bulk, periodic).
    - Discount Management:
      - Translation of contract discount rules into discount engine configurations.
    - **Corporate benefit cards** (printed, B2B beneficiaries) vs **IPD temporary visitor cards** (Phase 4.3): different programs; only the former is “corporate card” in the contract sense. See [Hospital card programs – overview](hospital-card-types-overview.md).
- **Activities**
  - Define corporate contract templates and rule sets with the business/finance teams.
  - Design and implement corporate and contract configuration UIs.
  - Implement corporate eligibility and contract lookup logic in registration/admission and Billing flows.
  - Implement corporate invoice generation (summaries by corporate, period, and contract).
  - Validate with at least one real or pilot corporate contract end-to-end (from registration/admission through billing and invoicing).
- **Deliverables**
  - Corporate master and contract management module.
  - Corporate card management capabilities integrated with Billing and Discount Management.
  - Corporate invoicing and reconciliation views.
  - Exit criteria: at least one pilot corporate client fully supported with end-to-end contract billing and discount handling.

#### Phase 5.4 – Reconciliation, Governance & Rollout Readiness

- **Objectives**
  - Ensure financial integrity between Billing, Discount Management, Corporate Management, and Accounting.
  - Prepare governance, controls, and reporting needed for broader rollout beyond pilots.
- **Scope**
  - Reconciliation & controls:
    - Daily reconciliation between Billing and Accounting (bill totals vs GL postings).
    - Reconciliation between Billing and upstream modules (e.g., charges vs clinical events).
    - Exception handling processes for mismatches and corrections.
  - Governance & operations:
    - Segregation of duties across billing, discount approval, and corporate contract maintenance.
    - Checklists and SOPs for go-live and daily operations.
  - Reporting & analytics:
    - Core financial KPIs (revenue by department/service, discount rates, AR ageing at basic level).
    - Dashboards for CFO/finance/controller views.
- **Activities**
  - Work with finance and audit teams to define reconciliation and control requirements.
  - Implement reconciliation jobs/reports between Billing and Accounting.
  - Implement exception queues and remediation workflows (e.g., failed postings, rejected corporate invoices).
  - Prepare training materials and operational runbooks for billing/finance teams.
- **Deliverables**
  - Reconciliation tools/reports and exception handling flows.
  - Governance documentation and SOPs for billing, discounts, and corporate processes.
  - Exit criteria: finance sign-off that pilot hospitals can be used as the blueprint for scaled rollout.

### Phase 6 – Pharmacy & Consumables

- Implement:
  - **Pharmacy** (master, stock, OPD/IPD issues, returns).
- Integrate with:
  - Inventory/Store for PO/GRN and stock movement.
  - Billing & Discount Management for charges and pharmacy-specific discount rules.
- Outcomes:
  - End-to-end pharmacy operations within the Hospital Module scope.

This phase delivers end-to-end pharmacy operations, tightly integrated with Inventory/Store, Billing, Discount Management, and clinical modules (PHR, IPD, OPD).

#### Phase 6.1 – Pharmacy Master & Stock Foundations

- **Objectives**
  - Establish a robust pharmacy master and stock management foundation that aligns with Inventory/Store and Clinical Chart masters.
  - Ensure traceable and compliant tracking of medicines and consumables across stores and counters.
- **Scope**
  - Implement core elements from **Pharmacy** (`pharmacy.md`):
    - Medicine master (drug codes, names, strengths, forms, pack sizes, ATC/grouping where applicable).
    - Consumables and non-drug items relevant for pharmacy.
    - Mappings to Clinical Chart services (for billable items) and Inventory item masters (for stock).
  - Stock structure & integration:
    - Definitions of pharmacy locations (main store, sub-stores, OPD/IPD counters).
    - Alignment with Inventory/Store for PO, GRN, and primary stock ledger.
    - Basic stock visibility at pharmacy level (on-hand, reserved, reorder thresholds).
  - Regulatory & safety data (as per initial scope):
    - Schedule/category flags (e.g., controlled substances).
    - Expiry tracking, batch/lot details, and manufacturer where relevant.
- **Activities**
  - Harmonize existing item masters between Inventory and Pharmacy; define authoritative sources and mapping rules.
  - Implement medicine/consumable master maintenance UI and APIs.
  - Configure pharmacy location hierarchy and integration with Inventory/Store flows (PO, GRN, stock transfers).
  - Implement initial stock import/migration process for pilot sites.
- **Deliverables**
  - Pharmacy master data model and APIs, aligned with Inventory and Clinical Chart.
  - Configured pharmacy locations and integrated stock structures.
  - Exit criteria: pilot pharmacy environments with clean masters and baseline stock data in place.

#### Phase 6.2 – OPD Pharmacy Sales & Returns

- **Objectives**
  - Enable efficient OPD pharmacy operations for prescriptions, over-the-counter sales, and returns, with proper billing integration.
- **Scope**
  - Sales flows:
    - Dispensing against electronic prescriptions from Phase 2.3 (including partial fills).
    - Over-the-counter sales (walk-in customers) with/without prescriptions, as allowed by policy.
    - Handling of substitutions where permitted (generic/brand).
  - Returns & adjustments:
    - Returns for OPD invoices with quantity, reason codes, and stock adjustments.
    - Handling of cancelled prescriptions/order lines.
  - Integration touchpoints:
    - PHR & Prescription: consumption of prescription data, update of dispense status.
    - Billing & Discount Management:
      - Posting of OPD pharmacy charges as billable items.
      - Application of pharmacy-specific discounts and schemes.
    - Inventory/Store: decrements and adjustments at pharmacy location level.
- **Activities**
  - Design OPD pharmacy counter UI for quick search, prescription selection, and sale processing.
  - Implement prescription-reading and dispense status update APIs.
  - Implement stock decrement logic and return flows with appropriate audit.
  - Define and validate typical scenarios (full/partial dispenses, substitutions, returns, discounts).
- **Deliverables**
  - OPD pharmacy sales and returns module with UI and APIs.
  - Integrated billing events for OPD pharmacy charges and discounts.
  - Exit criteria: OPD pharmacy in pilot hospital able to run day-to-day operations fully through the system.

#### Phase 6.3 – IPD Issues, Returns & Ward Supply

- **Objectives**
  - Support IPD medication and consumable supply workflows from pharmacy to wards/ICUs, with accurate patient-level and ward-level tracking.
- **Scope**
  - IPD issue flows:
    - Issues against patient-level prescriptions/orders (linked to IPD stay).
    - Ward/ward-stock issues for commonly used items (where modelled).
  - Returns:
    - Patient-level returns for unused medicines/consumables with appropriate stock and financial adjustments.
    - Ward-stock returns to pharmacy.
  - Integration touchpoints:
    - IPD Admission & Nursing:
      - Issues and returns initiated from nursing or IPD consoles where appropriate.
      - Status updates visible in patient medication views.
    - Billing:
      - Charges for IPD issues and credits for returns surfaced to Billing (Phase 5) with correct mappings.
    - Inventory/Store:
      - Accurate stock ledger entries at pharmacy and ward-stock levels.
- **Activities**
  - Map IPD medication and supply workflows with pharmacy and nursing/ward staff.
  - Implement issue/return UIs and APIs (patient-level and ward-level).
  - Implement integration events/handlers connecting pharmacy issues/returns to Billing and Inventory.
  - Define reconciliations between ward stocks and pharmacy stocks (at least basic periodic checks).
- **Deliverables**
  - IPD pharmacy issue/return workflows implemented and integrated.
  - Data flows delivering accurate patient-level and ward-level consumption information.
  - Exit criteria: at least one pilot ward using electronic pharmacy issues/returns with acceptable stock and billing accuracy.

#### Phase 6.4 – Pharmacy Governance, Controls & Reporting

- **Objectives**
  - Ensure pharmacy operations meet governance, audit, and reporting needs, particularly around stock, expiry, and high-risk items.
- **Scope**
  - Controls:
    - Role-based permissions for dispensing, returns, and stock adjustments.
    - Approval workflows for sensitive operations (e.g., high-value adjustments, controlled substances).
    - Expiry management alerts and blocking rules (no dispensing after expiry, near-expiry handling).
  - Reporting:
    - Stock and consumption reports by item, department, ward, and time period.
    - Expiry and near-expiry reports.
    - Basic profitability/usage views in conjunction with Billing.
  - Integration with cross-cutting:
    - Logging/audit (from Phase 0) applied to key pharmacy events (dispenses, returns, adjustments).
    - Monitoring metrics (e.g., stock-outs, near-expiry counts) for ops dashboards.
- **Activities**
  - Define pharmacy-specific SOPs and control requirements with pharmacy leads and finance.
  - Implement configuration and workflows for approvals and restricted operations.
  - Build core pharmacy reports and dashboards, using shared reporting/analytics patterns.
- **Deliverables**
  - Pharmacy control framework (permissions, approvals, alerts) implemented.
  - Core pharmacy reports and dashboards available for operations and finance.
  - Exit criteria: pharmacy operations in pilot hospitals meet agreed control and reporting requirements.

### Phase 7 – LAB, Diagnostics & Blood Bank

- Implement:
  - **LAB & Diagnostic – Patient-ID Wise Process**.
  - **Blood Bank**.
- Integrate with:
  - PHR (results, history).
  - Billing, Discount Management and Corporate frameworks.
  - Inventory (consumables, reagents, blood components).
- Outcomes:
  - Complete diagnostic flows and blood bank operations with traceability and billing.

This phase delivers end-to-end diagnostics and blood bank capabilities, integrated with PHR, Billing, Discount Management, Corporate frameworks, and Inventory.

#### Phase 7.1 – Lab & Diagnostic Orders, Workbench & Results

- **Objectives**
  - Implement the full lifecycle for lab and diagnostic services: ordering, sample collection, processing, validation, and result reporting.
  - Ensure traceable, patient-ID–wise processes with tight integration to PHR and Billing.
- **Scope**
  - Implement **LAB & Diagnostic – Patient-ID Wise Process** (`lab-diagnostic-module.md`):
    - Order entry from clinical contexts (PHR, OPD, IPD, OT, Nurse module).
    - Test panels/profiles and individual tests with mappings to Clinical Chart and Inventory items where required.
    - Sample collection workflows (label printing, barcoding where applicable, collection points).
    - Worklist management for laboratory sections (hematology, biochemistry, microbiology, etc.).
    - Result entry/import, validation/authorization flows, and final result reporting.
  - Integration touchpoints:
    - PHR:
      - Orders raised from clinical screens reflected as PHR orders.
      - Final results and key values visible in PHR views and summaries.
    - Billing & Discount Management:
      - Mapping of ordered tests/panels to Clinical Chart items and charge events.
      - Discount rules based on test groups, panels, and packages.
    - Inventory:
      - Hooks for consumption of reagents and consumables (high-level; detailed integration may be phased).
- **Activities**
  - Map end-to-end lab and diagnostic workflows with pathology, radiology, and clinical teams.
  - Design and implement order entry UIs for different entry points (OPD, IPD, OT, etc.).
  - Implement lab workbench/worklist screens and role-specific views (technicians, pathologists, radiologists).
  - Implement result entry/import mechanisms and validation chains (technician → specialist → consultant).
  - Implement APIs/events to push final results to PHR and charge events to Billing.
- **Deliverables**
  - Lab/diagnostic ordering, workbench, and results module with UIs and APIs.
  - Integration with PHR for orders and results visibility.
  - Charge event hooks into Billing and Discount Management.
  - Exit criteria: at least one lab section and one radiology modality running on the system for defined pilot cases.

#### Phase 7.2 – Blood Bank Operations & Traceability

- **Objectives**
  - Implement full blood bank operations from donor management through component issue, ensuring strong traceability and compliance.
- **Scope**
  - Implement **Blood Bank** (`blood-bank.md`):
    - Donor registration and eligibility screening.
    - Donation sessions and collection events (whole blood and apheresis where applicable).
    - Testing and screening workflows (TTIs and other mandated tests).
    - Component preparation and inventory (PRBC, platelets, plasma, cryo, etc.).
    - Cross-matching and reservation of components for patients.
    - Issue, return, and discard workflows with reasons (expiry, test failures, breakage, etc.).
  - Integration touchpoints:
    - PHR & Lab:
      - Link patient requests for blood components from clinical/lab modules.
      - Record issued components in the patient record.
    - Inventory:
      - Optional integration for storage equipment and consumables, and high-level stock tracking by component.
    - Billing & Corporate/Discount:
      - Charge capture for components and services (cross-match, screening, etc.).
      - Discounts/coverage based on payer or scheme where applicable.
- **Activities**
  - Map blood bank workflows with transfusion medicine specialists and blood bank staff.
  - Design donor, component inventory, and patient request/issue UIs.
  - Implement component lifecycle tracking (collection → testing → componentization → issue/return/discard).
  - Implement APIs/events to reflect component issue in PHR and to trigger billing events.
  - Define key traceability and compliance reports (donor/component trace, adverse event logs).
- **Deliverables**
  - Blood bank operations module (donor, components, issue/return/discard) with UI and APIs.
  - Integration with PHR/Lab for patient-side visibility.
  - Billing event hooks for blood bank services.
  - Exit criteria: pilot blood bank able to manage donations and issues end-to-end for selected units with traceability validated.

#### Phase 7.3 – Quality, Governance & Reporting for Diagnostics & Blood Bank

- **Objectives**
  - Ensure diagnostic and blood bank modules meet quality, regulatory, and reporting standards.
- **Scope**
  - Quality & governance:
    - Result validation and amendment rules (who can change what, with audit).
    - Delta checks and critical value handling workflows.
    - SOP alignment for sample handling, storage, and retention (at least at metadata level).
  - Reporting & analytics:
    - Workload and turnaround-time (TAT) reports for lab and radiology.
    - Blood bank utilization, wastage/expiry, and donor statistics.
    - Basic clinical quality metrics (e.g., repeat tests, critical results).
  - Integration with cross-cutting:
    - RBAC and audit trails for result entry, validation, and release.
    - Monitoring metrics (TAT, queue lengths, critical value counts) for dashboards.
- **Activities**
  - Define quality and compliance requirements with pathology, radiology, transfusion medicine, and quality teams.
  - Implement validation and amendment workflows in lab and blood bank modules.
  - Build standard reports and dashboards using shared analytics infrastructure.
- **Deliverables**
  - Quality and governance features implemented for lab, diagnostics, and blood bank.
  - Standardized TAT, utilization, and compliance reports.
  - Exit criteria: quality/compliance teams sign off that diagnostics and blood bank modules can be used as reference implementations for further rollout.

### Phase 8 – Canteen, Ambulance, Fixed Assets & Operational Modules

- Implement:
  - **Canteen Module – Food & Meal Services** (per dedicated implementation plan).
  - **Ambulance** booking and billing.
  - **Fixed Assets** for hospital assets.
  - **Certificate Management**.
- Integrate with:
  - Billing and Discount Management.
  - Accounting, Inventory and HR where applicable.
- Outcomes:
  - Non-clinical but revenue-impacting hospital operations modules go live.

This phase focuses on operational and support modules that are not clinical but have material operational and financial impact: canteen, ambulance, fixed assets, and certificates.

#### Phase 8.1 – Canteen Module – Food & Meal Services

- **Objectives**
  - Implement canteen operations for staff, patients, and visitors, with proper linkage to Inventory and Billing.
  - Support indoor (IPD meal distribution) and outdoor (counter sales) flows as per the dedicated SRS.
- **Scope**
  - Implement **Canteen Module – Food & Meal Services** (`canteen-module-srs.md` and `canteen-module-implementation-plan.md`):
    - Menu and item master aligned with Clinical Chart/department where relevant for billing.
    - Meal plan definitions for IPD patients (diet types, schedules, restrictions).
    - Indoor meal distribution (ward/bed-wise meal orders, delivery confirmation).
    - Outdoor sales (counter POS-style flows for staff/visitors).
  - Integration touchpoints:
    - Inventory/Store: consumption of raw materials and packaged items with appropriate stock decrements.
    - Billing & Discount Management:
      - Charge capture for paid meals and items (patient, staff, visitors).
      - Discounts/schemes for staff or corporate contracts where applicable.
    - Corporate/Card Management: corporate or staff card-based payment/entitlement where supported.
- **Activities**
  - Refine canteen-specific implementation details per the dedicated canteen implementation plan.
  - Design canteen UIs for menu management, indoor meal scheduling, and counter sales.
  - Implement meal distribution workflows integrated with IPD ward/bed information.
  - Implement charge/event hooks from canteen to Billing and Inventory.
- **Deliverables**
  - Canteen operations module and UIs (menu, meal plans, indoor/outdoor flows).
  - Integration with Inventory, Billing, and Corporate/card frameworks.
  - Exit criteria: pilot hospital using canteen module for at least one canteen location and one IPD ward.

#### Phase 8.2 – Ambulance Booking & Billing

- **Objectives**
  - Provide full ambulance service management from booking to trip completion and billing.
- **Scope**
  - Implement **Ambulance** (`ambulance.md`):
    - Ambulance vehicle and driver masters (with basic scheduling/availability).
    - Trip booking for patients and non-patients (e.g., referrals, external transfers).
    - Fare and charge calculation based on distance, time, service type, and waiting where applicable.
    - Trip completion and closure workflows.
  - Integration touchpoints:
    - Billing & Discount Management:
      - Charge events for ambulance trips, linked to patient account or corporate as appropriate.
      - Discounts or coverage rules by contract or scheme.
    - Accounting:
      - Mapping of ambulance revenues and costs to relevant GL accounts.
    - IPD/OPD & PHR:
      - Optional linkage of ambulance trips to encounters (e.g., emergency admissions).
- **Activities**
  - Map ambulance workflows with operations and finance (booking channels, approvals, pricing).
  - Design booking, dispatch, and trip closure UIs.
  - Implement trip lifecycle tracking and billing event generation.
- **Deliverables**
  - Ambulance module with booking, tracking, and billing integration.
  - Reports for trip volumes, utilization, revenue, and outstanding amounts.
  - Exit criteria: ambulance operations for selected scenarios running through the system end-to-end.

#### Phase 8.3 – Fixed Assets for Hospital Assets

- **Objectives**
  - Manage hospital fixed assets with proper registration, tracking, and accounting integration.
- **Scope**
  - Implement **Fixed Assets** (`fixed-assets.md`):
    - Asset master (type, location, department, cost, acquisition details).
    - Asset lifecycle: acquisition, transfer, maintenance events, disposal.
    - Basic depreciation data and schedules, in coordination with Accounting.
  - Integration touchpoints:
    - Accounting:
      - GL postings for capitalization, depreciation, and disposal, respecting Finance service contracts.
    - Inventory/Store:
      - Optional linkage for capital items procured via Inventory.
    - Hospital Operations:
      - Asset location and responsibility information for operations/biomed/engineering teams.
- **Activities**
  - Define asset categories and attributes with finance and operations.
  - Implement asset master maintenance and lifecycle workflows.
  - Integrate with Accounting service for depreciation and other postings (using existing accounting integration patterns).
- **Deliverables**
  - Fixed Assets module with asset lifecycle management.
  - Integration with Accounting for depreciation and other required postings.
  - Exit criteria: all major assets for a pilot facility registered and being tracked, with depreciation validated against finance expectations.

#### Phase 8.4 – Certificate Management

- **Objectives**
  - Implement standardized, auditable certificate issuance (birth, death, discharge, transfer) aligned with clinical and legal requirements.
- **Scope**
  - Implement **Certificate Management** (`certificate-management.md`):
    - Certificate templates and numbering for each certificate type.
    - Issuance, re-issuance, and cancellation flows with appropriate approvals.
    - Linkage to clinical and admission/discharge data for accurate content.
  - Integration touchpoints:
    - PHR & IPD/OPD:
      - Source data for clinical and demographic details printed on certificates.
    - Hospital Operations:
      - Configuration of templates, signatories, and approval workflows.
    - Audit/Compliance:
      - Audit records for all issuance events and changes.
- **Activities**
  - Confirm legal and regulatory requirements for certificates in target jurisdictions.
  - Design certificate management UIs and template configuration interfaces.
  - Implement workflows for creation, approval, printing, and re-issuance.
- **Deliverables**
  - Certificate Management module with templates, workflows, and integrations.
  - Audit and reporting views (e.g., issued certificates by type, date, and facility).
  - Exit criteria: pilot facility using electronic certificate management for at least one certificate type end-to-end.

### Phase 9 – Doctor Module, Hospital Operations & Portal

- Implement:
  - **Doctor Module** (registration, scheduling, attendance).
  - **Hospital Operations** (user management, settings, dashboards).
  - **Portal** (patient, doctor, and corporate portals).
- Focus areas:
  - Role and permission modeling for all previously delivered modules.
  - Dashboards and KPIs across clinical, operational and financial domains.
  - Self-service and B2B capabilities for engagement and transparency.
- Outcomes:
  - Operational maturity: configuration, dashboards and portals available.

This phase focuses on provider-facing and admin-facing capabilities (Doctor Module and Hospital Operations), plus external portals that expose self-service to patients, doctors, and corporate partners.

#### Phase 9.1 – Doctor Module (Registration, Scheduling, Attendance)

- **Objectives**
  - Provide a consistent, centralized view of doctors/providers, their schedules, and attendance, integrated with clinical and financial modules.
- **Scope**
  - Implement **Doctor Module** (`doctor-module.md`):
    - Doctor/provider master data (demographics, specialties, departments, privileges).
    - Scheduling and availability management (OPD sessions, OT slots, on-call rosters).
    - Attendance and duty rosters where in scope (e.g., in/out times, shift assignments).
  - Integration touchpoints:
    - PHR, IPD, OT, Lab, etc.: provider tagging for encounters, orders, procedures.
    - Billing & Corporate/Discount:
      - Mapping of provider codes for revenue attribution and payment/settlement logic.
    - Hospital Operations:
      - Role and permission assignment in coordination with RBAC from Phase 0.
- **Activities**
  - Define provider master attributes and governance with medical administration and HR.
  - Implement doctor registration and maintenance UIs and APIs.
  - Implement schedule/roster management screens and basic attendance workflows (where required).
  - Wire provider references into major clinical and financial flows (encounters, procedures, billing).
- **Deliverables**
  - Doctor master and scheduling module with UIs and APIs.
  - Provider tagging integrated into key upstream modules (PHR, IPD, OT, Billing).
  - Exit criteria: doctor data and schedules for pilot departments fully maintained in system and used as reference in clinical/billing workflows.

#### Phase 9.2 – Hospital Operations (User Management, Settings, Dashboards)

- **Objectives**
  - Provide central operational controls for user management, configuration, and high-level dashboards across the Hospital Module.
- **Scope**
  - Implement **Hospital Operations** (`hospital-operations.md`):
    - User and role mapping (leveraging central Auth/RBAC but providing hospital-specific views and assignments).
    - Core hospital settings (branches, working hours, departmental configurations that cut across modules).
    - Operational dashboards for administrative stakeholders (admissions, census, revenue snapshots, key KPIs).
  - Integration touchpoints:
    - Auth/RBAC service:
      - Synchronization of users, roles, and permissions with hospital-specific mappings.
    - All modules:
      - Consumption of central configuration (branch/site, defaults, feature toggles where applicable).
    - Reporting/analytics:
      - Use shared data and metrics to build consolidated dashboards.
- **Activities**
  - Define operational configuration model with hospital admin and IT (what is set where).
  - Implement UIs for user-to-role mapping, branch/site settings, and module configurations.
  - Design and implement base dashboards aggregating metrics from Billing, IPD, OPD, Lab, Pharmacy, etc.
- **Deliverables**
  - Hospital Operations console for configuration and user/role views.
  - Initial set of operational dashboards using shared analytics infrastructure.
  - Exit criteria: hospital admin team can manage key configs and view basic KPIs via the Hospital Operations console.

#### Phase 9.3 – Portals (Patient, Doctor, Corporate)

- **Objectives**
  - Expose selected Hospital Module capabilities via self-service web portals for patients, doctors, and corporate clients.
- **Scope**
  - Implement **Portal** (`portal.md`):
    - Patient portal:
      - Appointment booking and history.
      - Access to reports/summaries permitted by policy (lab results, visit summaries, bills).
      - Basic profile and communication preferences.
    - Doctor portal:
      - Schedule and appointment views.
      - Access to selected PHR information and task lists (subject to RBAC).
      - Access to performance dashboards where applicable.
    - Corporate/B2B portal:
      - Contract/coverage views.
      - Access to billing and utilization reports for their beneficiaries.
  - Integration touchpoints:
    - Auth/RBAC:
      - Secure login, SSO and role-based access for external and internal users.
    - PHR, Billing, Corporate, and Scheduling modules:
      - Read/limited write access to underlying data according to portal use cases.
- **Activities**
  - Define portal MVP scope with stakeholders (what external users can see/do initially).
  - Design portal UX flows and UI components, reusing internal UI patterns where sensible.
  - Implement portal APIs and adaptors against internal services (PHR, Billing, Doctor Module, Corporate).
  - Ensure security, privacy, and consent requirements are met for exposed data.
- **Deliverables**
  - Patient, doctor, and corporate portal experiences for the defined MVP scope.
  - Integration with core Hospital Module and platform services.
  - Exit criteria: at least one pilot group (e.g., selected patients/doctors/corporates) using portals in production with positive validation of usability and security.

---

## 5. Cross-Cutting Concerns per Phase

Each phase must also:

- Respect **non-functional requirements** (performance, security, compliance).
- Extend central **RBAC** and audit coverage to new features.
- Add **monitoring and logging** for new modules (metrics, alerts, traces).
- Follow shared patterns for:
  - API design and versioning.
  - Eventing and asynchronous integrations.
  - Data migration and backward compatibility.

---

## 6. Open Questions / Assumptions

- Final split between Hospital Module responsibilities and platform-level services (e.g., how much of user/role management lives in Hospital vs global).
- Deployment model (single deployable hospital service vs multiple context-based services).
- Adoption strategy:
  - Greenfield vs migration from existing HMS.
  - Need for coexistence with legacy modules in interim phases.
- Regulatory and local compliance requirements influencing rollout sequence (e.g., modules that must be present together for legal reasons in some regions).

