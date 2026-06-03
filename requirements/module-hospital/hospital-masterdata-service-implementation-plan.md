## `hospital-masterdata-service` – Implementation Plan

This document translates the optional `hospital-masterdata-service` scope defined in `README.md` into an implementation-oriented view. It defines the main submodules, their responsibilities, dependencies on platform-wide master data, and a phased delivery plan.

> **Note:** This service is only required if there is **no adequate platform-wide master data service** for departments, wards/rooms/beds, and generic service/procedure catalogs. If a platform master data service exists, Hospital Module services should integrate with it instead of using `hospital-masterdata-service`.

---

## 1. Overview and Objectives

`hospital-masterdata-service` provides hospital-specific reference data that is:

- Shared across hospital clinical and operational modules (PHR, IPD, OT, Lab, Pharmacy, Canteen, Billing, etc.).
- Stable and governed (versioned, audited, centrally configured).
- Optimized for lookup and configuration rather than high-frequency transactional updates.

**Primary objectives**:

- Provide a **single source of truth** for:
  - Hospital departments and service hierarchy (where not provided by platform).
  - Wards, rooms, beds and related attributes.
  - Generic procedure/service catalogs where they are not already modeled in Clinical Chart or platform masters.
- Avoid duplication and drift of master data across `hospital-*` services.
- Offer clear APIs and events so other services can rely on consistent codes and references.

---

## 2. Scope and Non-Scope

### 2.1 In-Scope

- **Department & Service Hierarchy (Hospital-Scoped)**
  - Departments, sub-departments, units and specialty groupings used across hospital modules.
  - Mapping to reporting dimensions and cost centers (where Accounting platform does not already cover this).

- **Ward, Room & Bed Masters**
  - Ward definitions (e.g., General Ward 1, ICU 2, NICU, HDU).
  - Rooms within wards (where applicable) and bed positions.
  - Bed attributes: type/class, gender rules, isolation flags, specialty, billing class tags.
  - Bed status configuration (allowed statuses, visibility in downstream IPD modules).

- **Generic Service/Procedure Catalogs (if not handled elsewhere)**
  - Catalog of generic clinical procedures or services not covered by:
    - Clinical Chart Management, or
    - Platform-wide service catalogs.
  - Grouping and tagging for later mapping to Billing or Clinical Chart masters.

- **Reference Lists & Enumerations (Hospital-Scoped)**
  - Enumerations used consistently across hospital modules (e.g., admission types, discharge disposition codes, visit types, bed types).
  - Localization/labeling for hospital-specific dictionaries if not already provided by a global reference data service.

- **Governance & Configuration Workflows**
  - Administrative UIs/APIs for creating, updating, deactivating masters.
  - Change governance support (approval flows where required).

### 2.2 Out-of-Scope (Handled by Other Services)

- **Accounting chart of accounts, global cost centers** – Accounting/Finance service.
- **Global item/service masters for cross-ERP use** – platform master data or Inventory/Clinical Chart services.
- **Detailed Clinical Chart Management** – `clinical-chart-management.md` and its owning module.
- **HR/Staff structure and roles** – HR/Staff service.
- **User and security configuration** – Auth/RBAC and Hospital Operations services.

`hospital-masterdata-service` focuses on **hospital-specific structural masters** required by multiple hospital modules, without duplicating platform-wide masters.

---

## 3. Architecture and Boundaries

### 3.1 High-Level Architecture

- **Service type**: Relatively low-throughput configuration and lookup service.
- **Database**: Relational (e.g., PostgreSQL) for relational integrity and auditing.
- **Integration style**:
  - Synchronous REST/gRPC for lookups and configuration changes.
  - Events for master data changes to notify consuming services.

### 3.2 Bounded Context Responsibilities

- **Department & Service Hierarchy Context**
  - Owns hospital-specific department and service group definitions.
- **Location & Bed Context**
  - Owns wards, rooms and beds and their attributes.
- **Reference Data Context**
  - Owns hospital-specific enumerations and codelists not provided by global reference data services.

---

## 4. Data Model (High-Level)

### 4.1 Key Entities

- `Department`
  - `id`, `code`, `name`, `description`.
  - Hierarchy references: `parent_department_id`, `level`.
  - Optional mapping to platform `cost_center_id`.
  - `status` (active/inactive), effective date range.

- `ServiceGroup` (optional if Clinical Chart is not yet in place)
  - `id`, `code`, `name`, `description`.
  - `department_id` link.
  - Category/grouping attributes for reporting.

- `Ward`
  - `id`, `code`, `name`.
  - `department_id`, `location_id` (site/branch).
  - Type (General, ICU, NICU, HDU, etc.).
  - Default bed class and other config flags.

- `Room`
  - `id`, `ward_id`, `code`, `name`.
  - Capacity (number of beds).
  - Optional attributes (room type, class).

- `Bed`
  - `id`, `room_id` (or directly `ward_id` for ward-level beds).
  - `code`, `name` or label.
  - `bed_class` (e.g., General, Semi-Private, Private, Deluxe).
  - Attributes: gender-restricted, isolation, specialty tags.
  - `status` (config master for valid states, not real-time occupancy).

- `ReferenceCode`
  - `id`, `domain` (e.g., `ADMISSION_TYPE`, `DISCHARGE_DISPOSITION`, `BED_TYPE`).
  - `code`, `display_value`, `description`.
  - Localization fields where needed.
  - `status` and effective date range.

### 4.2 Referential Strategy

- Use hospital master IDs (`department_id`, `ward_id`, `bed_id`, etc.) across Hospital Module services.
- Where platform-wide masters exist:
  - Store mapping references (`platform_department_code`, `platform_location_id`) instead of duplicating business logic.

---

## 5. APIs

### 5.1 Department & Service Hierarchy APIs

- `POST /departments` – create department.
- `GET /departments/{id}` – get department.
- `GET /departments` – search/list departments (with filtering by level, status, site).
- `PATCH /departments/{id}` – update department.

- Optional (if used):
  - `POST /service-groups`, `GET /service-groups`, `PATCH /service-groups/{id}`.

### 5.2 Ward, Room & Bed APIs

- `POST /wards` – create ward.
- `GET /wards/{id}` – get ward.
- `GET /wards` – list/search wards.
- `PATCH /wards/{id}` – update ward.

- `POST /wards/{wardId}/rooms` – create room.
- `GET /rooms/{id}` – get room.
- `GET /wards/{wardId}/rooms` – list rooms in ward.
- `PATCH /rooms/{id}` – update room.

- `POST /rooms/{roomId}/beds` (or `/wards/{wardId}/beds`) – create bed.
- `GET /beds/{id}` – get bed details.
- `GET /beds` – search beds (by ward, room, class, attributes).
- `PATCH /beds/{id}` – update bed attributes or activation status.

### 5.3 Reference Data APIs

- `GET /reference-codes/{domain}` – list reference codes for a domain.
- `POST /reference-codes/{domain}` – create new code.
- `PATCH /reference-codes/{domain}/{code}` – update code (e.g., deactivate).

---

## 6. Events and Integrations

### 6.1 Outgoing Events

Emit events for:

- `department.created`, `department.updated`, `department.deactivated`.
- `ward.created`, `ward.updated`.
- `room.created`, `room.updated`.
- `bed.created`, `bed.updated`.
- `referencecode.created`, `referencecode.updated`.

Downstream consumers:

- IPD Admission & Bed Management – to maintain an up-to-date bed hierarchy and attributes.
- Clinical Chart & Billing – for department and service group references.
- Canteen, Lab, Pharmacy, Ambulance – for department codes and locations where required.
- Reporting/BI – for consistent reference dimensions.

### 6.2 Incoming Integrations

- From platform-wide master data (if present):
  - Mappings to global cost centers, locations, and other cross-module codes.
- From Accounting:
  - Optional mappings to GL or reporting structures for revenue and cost reporting.

---

## 7. Non-Functional Requirements

- **Performance**
  - Optimized for read-heavy workloads (lookups in IPD, OT, Billing, etc.).
  - Typical lookup queries should return within \(< 150 ms\) at 95th percentile.
- **Availability**
  - Target 99.9%+ for read operations.
  - Configuration writes may be limited to admin windows but should remain safe and auditable.
- **Data Quality & Governance**
  - Validation to prevent duplicate codes and inconsistent hierarchies.
  - Effective dating and deactivation patterns to avoid breaking downstream references.
- **Security & Audit**
  - RBAC controls to restrict who can create/modify masters.
  - Full audit of structural changes (who, what, when).

---

## 8. Phased Implementation Plan

### Phase 1 – Department Hierarchy Foundations

- Implement:
  - `Department` entity and APIs.
  - Basic hierarchy modeling (parent/child, levels).
- Integrate:
  - With at least one downstream module (e.g., Billing or Clinical Chart) as reference.
- Deliverables:
  - Pilot hospital department hierarchy configured and used consistently in at least one downstream module.

### Phase 2 – Ward, Room & Bed Masters

- Implement:
  - `Ward`, `Room`, `Bed` entities and APIs.
  - Bed attribute modeling (class, isolation, gender rules).
- Integrate:
  - With IPD Admission & Bed Management for real-time occupancy and admission flows.
- Deliverables:
  - Pilot ward/bed structure configured and consumed by IPD modules.

### Phase 3 – Reference Data & Enumerations

- Implement:
  - `ReferenceCode` model and APIs.
  - Initial domains for admission types, discharge dispositions, bed types, etc.
- Integrate:
  - With IPD, Billing, Lab, and other services that need standardized codes.
- Deliverables:
  - Shared reference code catalog adopted by at least two hospital modules.

### Phase 4 – Governance, Mapping & Hardening

- Implement:
  - Governance features (approval workflows for structural changes where required).
  - Mapping fields to platform-level masters (cost centers, locations) where appropriate.
- Hardening:
  - Validation rules, migration scripts for legacy department/bed codes, and reconciliation reports.
- Deliverables:
  - Stable master data management process with clear ownership and controls; service ready for broader rollout.

---

## 9. Open Questions / Assumptions

- Existence and capability of **platform-wide master data service**:
  - If available and sufficiently rich, this service may be unnecessary or significantly reduced.
- Granularity and ownership boundaries between this service and **Clinical Chart Management**:
  - Which service owns detailed chargeable item and procedure catalogs vs simple department/service group structures.
- Degree of **site/branch** variation:
  - How much department and bed structure varies by branch and whether multi-tenant or multi-site features are required in MVP.
- Legacy data migration:
  - Complexity of mapping existing HMS department and bed codes into the new hierarchy and identifiers.

