# `hospital-scheduling-service` – Implementation Plan (Granular)

This document translates the `hospital-scheduling-service` scope into an implementation-oriented view with **phase-wise checklists** so that asking Cursor to "implement Phase N" can be executed without ambiguity. Every file, route, and field is listed where applicable.

---

## 1. Overview and Objectives

`hospital-scheduling-service` is the central **scheduling engine** for the Hospital Module. It is responsible for:

- **Time-based reservations** for outpatient (OPD) appointments, inpatient (IPD) planned admissions/transfers, and resource calendars (doctors, rooms, theatres).
- **Slot templates**, capacity rules, working hours, and conflict detection.
- **Waitlists**, rescheduling, cancellation workflows, and basic reporting.

**Primary objectives:**

- Act as the **source of truth for time-based reservations** (appointments, booked slots, reservations), not for clinical or financial records.
- Provide clean APIs for availability search, booking, rescheduling, and cancellation with idempotency and validation.
- Integrate with PHR/Patient, Doctor module, IPD/Bed management, OT, Portal/BFF, and Billing via defined contracts.

---

## 1.1 Implementation Status

- **Phase 0 – Foundations & Service Skeleton**: **Not started**
- **Phase 1 – Core Scheduling Masters & Resource Calendars**: **Not started**
- **Phase 2 – OPD Appointment Booking & Management**: **Not started**
- **Phase 3 – IPD Admission, Transfer & Discharge Scheduling**: **Not started**
- **Phase 4 – Resource Rosters & Cross-Resource Orchestration**: **Not started**
- **Phase 5 – Waitlists, Overbooking & Optimization**: **Not started**
- **Phase 6 – Reporting, Monitoring & Hardening**: **Not started**
- **Phase 7 – Gap-Closure: Security, Real-time, Audit & Integrations**: **Not started**

---

## 1.2 Phase-wise Implementation Checklist (DB · Backend · Frontend)

| Phase | DB | Backend | Frontend |
|-------|----|---------|----------|
| **Phase 0** – Foundations & skeleton | Schema only (empty) | 100% checklist below | 0% |
| **Phase 1** – Core masters & calendars | 100% checklist below | 100% checklist below | 100% checklist below |
| **Phase 2** – OPD appointments | 100% checklist below | 100% checklist below | 100% checklist below |
| **Phase 3** – IPD scheduling | 100% checklist below | 100% checklist below | 100% checklist below |
| **Phase 4** – Rosters & cross-resource | 100% checklist below | 100% checklist below | 100% checklist below |
| **Phase 5** – Waitlists & overbooking | 100% checklist below | 100% checklist below | 100% checklist below |
| **Phase 6** – Reporting & hardening | Optional audit tables | 100% checklist below | 100% checklist below |
| **Phase 7** – Gap-closure (security, real-time, audit) | 100% checklist below | 100% checklist below | 100% checklist below |

Use the per-phase sections below as the single source of truth when asking Cursor to "implement Phase N".

---

## 2. Scope and Non-Scope

### 2.1 In-Scope

- **Scheduling core**: Resources (doctors, rooms, theatres, bed groups), calendars, working hours, slot templates, blackout dates.
- **Reservations**: Generic reservation model and status lifecycle (Tentative, Confirmed, Checked-In, Completed, Cancelled, No-Show); conflict detection.
- **OPD appointments**: Create, reschedule, cancel, check-in, no-show; queue views per doctor/clinic; booking/cancellation rules.
- **IPD scheduling**: Planned admission requests, bed/ward reservations, transfer scheduling, discharge planning views.
- **Resource rosters**: Doctor/room/theatre availability, pattern-based rosters, bulk updates, substitution flows.
- **Waitlists & overbooking**: Waitlist per doctor/clinic, priority rules, overbooking policies, auto-fill on cancellation.
- **APIs & BFF support**: Availability search, booking/reschedule/cancel (idempotent), list appointments by patient/provider.
- **Reporting & audit**: Utilization metrics, no-show/cancellation rates, audit trail for schedule and appointment changes.

### 2.2 Out-of-Scope (Handled by Other Modules)

- **Patient registry, encounters, clinical data** – `hospital-service` / PHR.
- **Detailed IPD admission/bed management** – admission-ipd and IPD bed management modules.
- **OT case management and clinical details** – ot-procedure-entry-ipd; scheduling only does theatre/doctor calendar and conflict detection.
- **Billing, discount rules, corporate contracts** – Billing/Discount/Corporate modules; scheduling only emits events (e.g. no-show fee).
- **HR/Staff and doctor employment lifecycle** – HR/Staff and Doctor modules; scheduling references doctor_id.
- **Authentication and global RBAC** – platform auth; scheduling enforces scheduling-related roles.

---

## 3. Architecture and Boundaries

- **Service type**: Stateless Spring Boot service (REST APIs + optional event publishers).
- **Database**: PostgreSQL; schema `hospital_scheduling` created and versioned via **hospital-service** Liquibase (same pattern as `hospital_billing`, `hospital_card`).
- **Service module path**: `easyops-erp/services/hospital-scheduling-service`.
- **Integration**: Reads doctor/branch/department masters from hospital-service or Doctor module (REST/Feign); publishes appointment/reservation events for Portal/BFF, Billing, analytics.

**Bounded contexts (internal packages):**

- **Resource & config**: Resources, working hours, slot templates, blackouts, booking rules.
- **Reservation**: Generic reservations and conflict detection.
- **Appointment (OPD)**: OPD appointment lifecycle and queue views.
- **IPD scheduling**: Planned admissions, transfers, discharge planning.
- **Roster**: Resource availability and duty rosters (Phase 4).
- **Waitlist** (Phase 5): Waitlist and overbooking.

---

## 4. Dependencies on Other Hospital Modules

- **Patient / PHR / Encounters**: Source of truth for patient identity and visits; scheduling triggers or associates visits but does not own clinical content.
- **Doctor module**: Doctor/provider master and specialties; scheduling references `doctor_id`; rosters may be configured in scheduling or mirrored from Doctor module.
- **IPD Admission & Bed Management**: Actual admission/transfer and bed occupancy owned by IPD; scheduling provides planned reservations and expected movements.
- **OT module**: Scheduling owns theatre/doctor calendar and conflict detection; OT owns case details and procedure workflows.
- **Hospital Operations & Masterdata**: Branch/clinic configuration, working days/hours, department mappings.
- **Portal & BFF**: Consume scheduling APIs for availability and appointment management.
- **Billing**: No billing logic in scheduling; emit events only (e.g. no-show for fee).

---

## 5. Data Model (Exact Schemas for Implementation)

All tables live in schema `hospital_scheduling`. Use these column definitions in Liquibase and JPA entities.

### 5.1 scheduling_resources (Phase 1)

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| resource_type | VARCHAR(30) NOT NULL | DOCTOR, ROOM, THEATRE, BED_GROUP, EQUIPMENT |
| external_reference_id | VARCHAR(255) NOT NULL | e.g. doctor_id from hospital.doctors, room_id |
| name | VARCHAR(255) NOT NULL | Display name |
| branch_id | UUID nullable | Facility/branch |
| department_id | UUID nullable | e.g. hospital.doctor_departments.department_id |
| metadata | JSONB nullable | Extra attributes (capacity, specialty, etc.) |
| status | VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' | ACTIVE, INACTIVE |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |
| created_by | UUID nullable | |

**Unique:** (resource_type, external_reference_id) or (resource_type, external_reference_id, branch_id) per business rule.

### 5.2 scheduling_working_hours (Phase 1)

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| resource_id | UUID NOT NULL FK → scheduling_resources(id) | |
| day_of_week | SMALLINT NOT NULL | 0=Sunday, 1=Monday, …, 6=Saturday |
| start_time | TIME NOT NULL | |
| end_time | TIME NOT NULL | |
| effective_from | DATE nullable | Optional validity start |
| effective_to | DATE nullable | Optional validity end |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

### 5.3 scheduling_slot_templates (Phase 1)

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| name | VARCHAR(255) NOT NULL | e.g. "OPD Morning 10min" |
| resource_type | VARCHAR(30) nullable | Apply to resource type; nullable = generic |
| branch_id | UUID nullable | |
| slot_duration_minutes | INT NOT NULL | e.g. 10, 15 |
| slots_per_interval | INT NOT NULL DEFAULT 1 | Capacity per slot (e.g. 4 patients per slot) |
| start_time | TIME NOT NULL | Template window start |
| end_time | TIME NOT NULL | Template window end |
| lead_time_days | INT DEFAULT 0 | Min days in advance to book |
| max_advance_days | INT nullable | Max days in advance to book |
| status | VARCHAR(20) DEFAULT 'ACTIVE' | ACTIVE, INACTIVE |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |
| created_by | UUID nullable | |

### 5.4 scheduling_blackouts (Phase 1)

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| resource_id | UUID nullable FK → scheduling_resources(id) | Null = branch/global blackout |
| branch_id | UUID nullable | When resource_id null, scope by branch |
| blackout_date | DATE NOT NULL | |
| reason | VARCHAR(255) nullable | Holiday, training, etc. |
| created_at | TIMESTAMPTZ | |
| created_by | UUID nullable | |

### 5.5 scheduling_reservations (Phase 1)

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| resource_id | UUID NOT NULL FK → scheduling_resources(id) | |
| slot_start | TIMESTAMPTZ NOT NULL | |
| slot_end | TIMESTAMPTZ NOT NULL | |
| status | VARCHAR(30) NOT NULL | TENTATIVE, CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED, NO_SHOW |
| reference_type | VARCHAR(30) nullable | APPOINTMENT, IPD_ADMISSION, TRANSFER, OT, GENERIC |
| reference_id | VARCHAR(255) nullable | e.g. appointment_id when reference_type=APPOINTMENT |
| patient_id | UUID nullable | |
| idempotency_key | VARCHAR(255) nullable UNIQUE | |
| version | BIGINT NOT NULL DEFAULT 0 | Optimistic locking — added in changeset 032 (§7.10) |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |
| created_by | UUID nullable | |

### 5.6 scheduling_booking_rules (Phase 1, optional)

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| scope_type | VARCHAR(30) NOT NULL | RESOURCE, BRANCH, GLOBAL |
| scope_id | UUID nullable | resource_id or branch_id when applicable |
| cancellation_cutoff_hours | INT nullable | Hours before slot to allow free cancellation |
| max_per_slot | INT nullable | Max reservations per slot (overbooking cap) |
| channel | VARCHAR(30) nullable | WEB, MOBILE, FRONT_DESK, CALL_CENTER, INTERNAL — null = applies to all channels (§11.6) |
| channel_daily_cap | INT nullable | Max bookings per day for this channel/scope combination; null = unlimited |
| max_advance_days | INT nullable | Override max advance days for this scope (takes precedence over slot template) |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

**Constraint:** `UNIQUE (scope_type, scope_id, channel)` — one rule per scope+channel combination (null channel = all-channel rule).

### 5.7 scheduling_appointments (Phase 2)

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| reservation_id | UUID NOT NULL FK → scheduling_reservations(id) | |
| patient_id | UUID NOT NULL | |
| resource_id | UUID NOT NULL FK → scheduling_resources(id) | Doctor (or primary resource) |
| clinic_or_location_id | UUID nullable | Branch/clinic/location |
| appointment_date | DATE NOT NULL | |
| slot_start | TIMESTAMPTZ NOT NULL | |
| slot_end | TIMESTAMPTZ NOT NULL | |
| appointment_type | VARCHAR(30) NOT NULL | NEW, FOLLOW_UP, EMERGENCY, ROUTINE, REPORT |
| status | VARCHAR(30) NOT NULL | Same as reservation + derived |
| visit_id | UUID nullable | Set when visit created in PHR |
| token_number | INT nullable | Queue order for the day |
| booking_channel | VARCHAR(30) NOT NULL | WEB, MOBILE, FRONT_DESK, CALL_CENTER, INTERNAL (§11.6) |
| booked_by | UUID nullable | Actor who created the booking (staff user id or system) |
| slot_template_id | UUID nullable FK → scheduling_slot_templates(id) | Session template used (§11.12) |
| session_shift | VARCHAR(20) nullable | MORNING, EVENING, NIGHT, FULL_DAY, CUSTOM (§11.12) |
| session_label | VARCHAR(255) nullable | Display label copied from template or shift (§11.12) |
| idempotency_key | VARCHAR(255) nullable UNIQUE | |
| version | BIGINT NOT NULL DEFAULT 0 | Optimistic locking — added in changeset 032 (§7.10) |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |
| created_by | UUID nullable | |

### 5.8 scheduling_planned_admissions (Phase 3)

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| patient_id | UUID NOT NULL | |
| preferred_date | DATE NOT NULL | |
| preferred_ward_or_bed_class | VARCHAR(100) nullable | |
| status | VARCHAR(30) NOT NULL | REQUESTED, RESERVED, CONVERTED, EXPIRED, CANCELLED |
| bed_group_resource_id | UUID nullable FK → scheduling_resources(id) | When reserved |
| reservation_id | UUID nullable FK → scheduling_reservations(id) | Link to reservation |
| expires_at | TIMESTAMPTZ nullable | Reservation expiry |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |
| created_by | UUID nullable | |

### 5.9 scheduling_waitlist_entries (Phase 5)

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| patient_id | UUID NOT NULL | |
| resource_id | UUID NOT NULL FK → scheduling_resources(id) | Doctor/clinic |
| preferred_from_date | DATE nullable | |
| preferred_to_date | DATE nullable | |
| priority | INT DEFAULT 0 | Higher = higher priority |
| priority_reason | VARCHAR(50) nullable | EMERGENCY, VIP, FOLLOW_UP, ROUTINE |
| status | VARCHAR(30) NOT NULL | PENDING, PROMOTED, CANCELLED, EXPIRED |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |
| created_by | UUID nullable | |

### 5.10 scheduling_doctor_resource_mappings (Phase 7 – §11.2)

Deterministic, server-side mapping from authenticated doctor user to scheduling resource. Required for scoped API access and must not be configurable client-side.

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| doctor_user_id | UUID NOT NULL | Authenticated user identity from auth-service |
| resource_id | UUID NOT NULL FK → scheduling_resources(id) | Must be DOCTOR type |
| branch_id | UUID nullable | Scope mapping to a specific branch; null = all branches |
| is_primary | BOOLEAN NOT NULL DEFAULT TRUE | Primary mapping for the doctor |
| effective_from | DATE nullable | |
| effective_to | DATE nullable | |
| status | VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' | ACTIVE, INACTIVE |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |
| created_by | UUID nullable | |

**Unique:** `(doctor_user_id, resource_id, branch_id)` — one active mapping per doctor/resource/branch.

**Fallback resolution order** (§11.2): (1) exact branch match → (2) null-branch (all-branch) match → (3) fail-safe error if none found.

---

### 5.11 scheduling_audit_log (Phase 7 – §11.7, mandatory)

Immutable audit trail for all appointment and reservation mutations.

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| entity_type | VARCHAR(30) NOT NULL | APPOINTMENT, RESERVATION, PLANNED_ADMISSION, WAITLIST_ENTRY |
| entity_id | UUID NOT NULL | |
| action | VARCHAR(50) NOT NULL | CREATED, RESCHEDULED, CANCELLED, CHECKED_IN, NO_SHOW, STATUS_CHANGED, etc. |
| actor_id | UUID nullable | User / system actor |
| actor_role | VARCHAR(100) nullable | Role context at time of action |
| booking_channel | VARCHAR(30) nullable | Channel used (mirrors appointment.booking_channel) |
| reason | TEXT nullable | Cancellation reason, override note, etc. |
| correlation_id | VARCHAR(255) nullable | Request correlation id for distributed tracing |
| before_state | JSONB nullable | Snapshot of entity before change |
| after_state | JSONB nullable | Snapshot of entity after change |
| created_at | TIMESTAMPTZ NOT NULL | Event timestamp |

**Immutable**: no UPDATE or DELETE permitted; insert-only. Retention minimum: 7 years (§11.17).

---

### 5.12 scheduling_roster_blocks (Phase 4 – optional)

Explicit time blocks for resource availability overrides (unavailability, substitution). Complements working hours and blackouts with intra-day or short-window precision.

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| resource_id | UUID NOT NULL FK → scheduling_resources(id) | |
| start_time | TIMESTAMPTZ NOT NULL | Block start (exact datetime) |
| end_time | TIMESTAMPTZ NOT NULL | Block end |
| block_type | VARCHAR(20) NOT NULL | AVAILABLE, UNAVAILABLE, SUBSTITUTE |
| substitute_resource_id | UUID nullable FK → scheduling_resources(id) | When block_type = SUBSTITUTE |
| reason | VARCHAR(255) nullable | |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |
| created_by | UUID nullable | |

---

**Indexes (implement in Liquibase):**

- `scheduling_resources(resource_type, external_reference_id)`, `scheduling_resources(branch_id)`, `scheduling_resources(status)`
- `scheduling_working_hours(resource_id, day_of_week)`
- `scheduling_slot_templates(resource_type, branch_id)`, `scheduling_slot_templates(status)`
- `scheduling_blackouts(resource_id, blackout_date)`, `scheduling_blackouts(branch_id, blackout_date)`
- `scheduling_reservations(resource_id, slot_start, slot_end)`, `scheduling_reservations(status)`, `scheduling_reservations(patient_id)`, `scheduling_reservations(idempotency_key)`
- `scheduling_appointments(patient_id, appointment_date)`, `scheduling_appointments(resource_id, appointment_date)`, `scheduling_appointments(idempotency_key)`, `scheduling_appointments(reservation_id)`
- `scheduling_planned_admissions(patient_id)`, `scheduling_planned_admissions(preferred_date)`, `scheduling_planned_admissions(status)`
- `scheduling_waitlist_entries(resource_id, status)`, `scheduling_waitlist_entries(patient_id)`
- `scheduling_doctor_resource_mappings(doctor_user_id, status)`, `scheduling_doctor_resource_mappings(resource_id)`, `scheduling_doctor_resource_mappings(branch_id)`
- `scheduling_audit_log(entity_type, entity_id)`, `scheduling_audit_log(actor_id)`, `scheduling_audit_log(created_at)`, `scheduling_audit_log(correlation_id)`
- `scheduling_roster_blocks(resource_id, start_time, end_time)`, `scheduling_roster_blocks(substitute_resource_id)`
- `scheduling_appointments(slot_template_id)`, `scheduling_appointments(booking_channel, appointment_date)`

---

## 6. APIs (Exact Contracts for Implementation)

Base path: **`/api/hospital-scheduling`** (API Gateway routes to `hospital-scheduling-service`).

### 6.1 Resource & Config APIs (Phase 1)

| Method | Path | Request body / params | Response | Purpose |
|--------|------|------------------------|----------|--------|
| POST | `/resources` | CreateResourceRequest | ResourceResponse | Create scheduling resource |
| GET | `/resources/{id}` | — | ResourceResponse | Get by id |
| GET | `/resources` | resourceType?, branchId?, departmentId?, status?, page?, size? | PagedResponse&lt;ResourceResponse&gt; | List/filter resources |
| PATCH | `/resources/{id}` | UpdateResourceRequest (partial) | ResourceResponse | Update resource |
| POST | `/resources/{id}/working-hours` | CreateWorkingHoursRequest (or batch) | WorkingHoursResponse / List | Set working hours |
| GET | `/resources/{id}/working-hours` | — | List&lt;WorkingHoursResponse&gt; | Get working hours |
| POST | `/slot-templates` | CreateSlotTemplateRequest | SlotTemplateResponse | Create slot template |
| GET | `/slot-templates/{id}` | — | SlotTemplateResponse | Get by id |
| GET | `/slot-templates` | resourceType?, branchId?, status?, page?, size? | PagedResponse&lt;SlotTemplateResponse&gt; | List templates |
| POST | `/blackouts` | CreateBlackoutRequest | BlackoutResponse | Create blackout |
| GET | `/blackouts` | resourceId?, branchId?, fromDate?, toDate?, page?, size? | PagedResponse&lt;BlackoutResponse&gt; | List blackouts |
| DELETE | `/blackouts/{id}` | — | 204 | Remove blackout |

**CreateResourceRequest:**  
`resourceType`, `externalReferenceId`, `name`, `branchId?`, `departmentId?`, `metadata?`, `status?` (default ACTIVE)

**ResourceResponse:**  
`id`, `resourceType`, `externalReferenceId`, `name`, `branchId`, `departmentId`, `metadata`, `status`, `createdAt`, `updatedAt`, `createdBy`

**CreateWorkingHoursRequest:**  
`resourceId`, `dayOfWeek`, `startTime`, `endTime`, `effectiveFrom?`, `effectiveTo?` (or batch: list of dayOfWeek/startTime/endTime)

**WorkingHoursResponse:**  
`id`, `resourceId`, `dayOfWeek`, `startTime`, `endTime`, `effectiveFrom`, `effectiveTo`, `createdAt`, `updatedAt`

**CreateSlotTemplateRequest:**  
`name`, `resourceType?`, `branchId?`, `slotDurationMinutes`, `slotsPerInterval`, `startTime`, `endTime`, `leadTimeDays?`, `maxAdvanceDays?`, `status?`

**SlotTemplateResponse:**  
`id`, `name`, `resourceType`, `branchId`, `slotDurationMinutes`, `slotsPerInterval`, `startTime`, `endTime`, `leadTimeDays`, `maxAdvanceDays`, `status`, `createdAt`, `updatedAt`, `createdBy`

**CreateBlackoutRequest:**  
`resourceId?`, `branchId?`, `blackoutDate`, `reason?`

**BlackoutResponse:**  
`id`, `resourceId`, `branchId`, `blackoutDate`, `reason`, `createdAt`, `createdBy`

### 6.2 Reservation & Availability APIs (Phase 1)

| Method | Path | Request body / params | Response | Purpose |
|--------|------|------------------------|----------|--------|
| GET | `/availability` | resourceId, fromDate, toDate, slotTemplateId? | AvailabilityResponse | Get available slots |
| POST | `/reservations` | CreateReservationRequest | ReservationResponse | Create generic reservation |
| GET | `/reservations/{id}` | — | ReservationResponse | Get reservation |
| GET | `/reservations` | resourceId?, patientId?, status?, from?, to?, page?, size? | PagedResponse&lt;ReservationResponse&gt; | List reservations |
| PATCH | `/reservations/{id}/status` | UpdateReservationStatusRequest | ReservationResponse | Update status (e.g. CONFIRMED, CANCELLED) |
| POST | `/reservations/check-conflicts` | CheckConflictsRequest | ConflictCheckResponse | Check for overlapping reservations |

**AvailabilityResponse:**  
`resourceId`, `date`, `slots: [{ start, end, availableCount }]`, `blackouts?`

**CreateReservationRequest:**  
`resourceId`, `slotStart`, `slotEnd`, `status?` (default TENTATIVE), `referenceType?`, `referenceId?`, `patientId?`, `idempotencyKey?`

**ReservationResponse:**  
`id`, `resourceId`, `slotStart`, `slotEnd`, `status`, `referenceType`, `referenceId`, `patientId`, `createdAt`, `updatedAt`, `createdBy`

**UpdateReservationStatusRequest:**  
`status`, `reason?`

**CheckConflictsRequest:**  
`resourceId`, `slotStart`, `slotEnd`, `excludeReservationId?`

**ConflictCheckResponse:**  
`hasConflict` (boolean), `conflictingReservations` (list of ReservationResponse or ids)

### 6.3 Appointment APIs (Phase 2)

| Method | Path | Request body / params | Response | Purpose |
|--------|------|------------------------|----------|--------|
| POST | `/appointments` | CreateAppointmentRequest | AppointmentResponse | Book OPD appointment |
| GET | `/appointments/{id}` | — | AppointmentDetailResponse | Get appointment (with reservation) |
| GET | `/appointments` | patientId?, resourceId?, clinicId?, fromDate?, toDate?, status?, slotTemplateId?, sessionShift?, sessionLabel?, bookingChannel?, scope?, page?, size? | PagedResponse&lt;AppointmentResponse&gt; | List appointments |
| POST | `/appointments/{id}/reschedule` | RescheduleAppointmentRequest | AppointmentResponse | Reschedule to new slot |
| POST | `/appointments/{id}/cancel` | CancelAppointmentRequest (reason?, idempotencyKey?) | AppointmentResponse | Cancel appointment |
| POST | `/appointments/{id}/check-in` | — | AppointmentResponse | Mark checked-in |
| POST | `/appointments/{id}/no-show` | — | AppointmentResponse | Mark no-show |
| GET | `/appointments/queue` | resourceId, date, clinicId?, slotTemplateId?, sessionShift?, sessionLabel? | QueueResponse | Get queue for doctor/clinic for date |

**CreateAppointmentRequest:**  
`patientId`, `resourceId` (doctor), `clinicOrLocationId?`, `appointmentDate`, `slotStart`, `slotEnd`, `appointmentType` (NEW, FOLLOW_UP, EMERGENCY, ROUTINE, REPORT), `bookingChannel` (**required** — WEB, MOBILE, FRONT_DESK, CALL_CENTER, INTERNAL), `bookedBy?` (actor user id), `slotTemplateId?`, `sessionShift?` (MORNING, EVENING, NIGHT, FULL_DAY, CUSTOM), `sessionLabel?`, `idempotencyKey?`

**AppointmentResponse:**  
`id`, `reservationId`, `patientId`, `resourceId`, `clinicOrLocationId`, `appointmentDate`, `slotStart`, `slotEnd`, `appointmentType`, `status`, `visitId`, `tokenNumber`, `bookingChannel`, `bookedBy`, `slotTemplateId`, `sessionShift`, `sessionLabel`, `createdAt`, `updatedAt`, `createdBy`

**AppointmentDetailResponse:**  
Extends AppointmentResponse with `reservation: ReservationResponse`

**RescheduleAppointmentRequest:**  
`newSlotStart`, `newSlotEnd`, `idempotencyKey?`

**CancelAppointmentRequest:**  
`reason?`, `idempotencyKey?`

**QueueResponse:**  
`resourceId`, `date`, `appointments: AppointmentResponse[]` (ordered by slot then token_number)

> **Note on `scope` filter**: When a doctor-scoped session is active, the server enforces resource scope automatically (§11.1). The `scope` query param is for admin/coverage overrides only and requires explicit RBAC permission plus audit.

### 6.4 Planned Admission / IPD Scheduling APIs (Phase 3)

| Method | Path | Request body / params | Response | Purpose |
|--------|------|------------------------|----------|--------|
| POST | `/planned-admissions` | CreatePlannedAdmissionRequest | PlannedAdmissionResponse | Request planned admission |
| GET | `/planned-admissions/{id}` | — | PlannedAdmissionResponse | Get by id |
| GET | `/planned-admissions` | patientId?, preferredDateFrom?, preferredDateTo?, status?, page?, size? | PagedResponse&lt;PlannedAdmissionResponse&gt; | List |
| PATCH | `/planned-admissions/{id}/status` | UpdatePlannedAdmissionStatusRequest | PlannedAdmissionResponse | Reserve, convert, expire, cancel |
| GET | `/planned-admissions/expected` | fromDate, toDate, wardOrBedClass? | ExpectedAdmissionsResponse | Expected admissions by date (for dashboards) |

**CreatePlannedAdmissionRequest:**  
`patientId`, `preferredDate`, `preferredWardOrBedClass?`

**PlannedAdmissionResponse:**  
`id`, `patientId`, `preferredDate`, `preferredWardOrBedClass`, `status`, `bedGroupResourceId`, `reservationId`, `expiresAt`, `createdAt`, `updatedAt`, `createdBy`

**ExpectedAdmissionsResponse:**  
`items: [{ date, count, byWardOrClass? }]`

### 6.5 Waitlist APIs (Phase 5)

| Method | Path | Request body / params | Response | Purpose |
|--------|------|------------------------|----------|--------|
| POST | `/waitlist` | CreateWaitlistEntryRequest | WaitlistEntryResponse | Add to waitlist |
| GET | `/waitlist` | resourceId?, patientId?, status?, page?, size? | PagedResponse&lt;WaitlistEntryResponse&gt; | List waitlist |
| PATCH | `/waitlist/{id}/status` | UpdateWaitlistStatusRequest | WaitlistEntryResponse | PROMOTED, CANCELLED, EXPIRED |
| POST | `/waitlist/promote` | PromoteWaitlistRequest (resourceId, slotStart, slotEnd, maxCandidates?) | PromoteWaitlistResponse | Auto-fill slot from waitlist |

**CreateWaitlistEntryRequest:**  
`patientId`, `resourceId`, `preferredFromDate?`, `preferredToDate?`, `priority?`, `priorityReason?`

**WaitlistEntryResponse:**  
`id`, `patientId`, `resourceId`, `preferredFromDate`, `preferredToDate`, `priority`, `priorityReason`, `status`, `createdAt`, `updatedAt`, `createdBy`

**PromoteWaitlistResponse:**  
`appointment?: AppointmentResponse`, `candidatesContacted?: number`

### 6.6 Reporting APIs (Phase 6)

| Method | Path | Request body / params | Response | Purpose |
|--------|------|------------------------|----------|--------|
| GET | `/reports/utilization` | resourceId?, fromDate, toDate, groupBy? (DAY, WEEK) | UtilizationReportResponse | Slots used vs available |
| GET | `/reports/no-show` | resourceId?, fromDate, toDate | NoShowReportResponse | No-show count/rate |
| GET | `/reports/cancellations` | resourceId?, fromDate, toDate | CancellationReportResponse | Cancellation count/rate |

### 6.7 Real-time Queue APIs (Phase 7 – §11.3, mandatory)

| Method | Path | Request body / params | Response | Purpose |
|--------|------|------------------------|----------|--------|
| GET | `/appointments/queue/stream` | resourceId, date (query params) | SSE stream of QueueUpdateEvent | Push queue status updates to subscribed clients |

**Transport**: Server-Sent Events (SSE) primary; fallback polling via `GET /appointments/queue` with configurable `pollIntervalSeconds` hint in response headers.

**QueueUpdateEvent** (SSE event payload):  
`eventType` (CHECKED_IN, CANCELLED, NO_SHOW, RESCHEDULED, TOKEN_CHANGED), `appointmentId`, `patientId`, `tokenNumber`, `status`, `slotStart`, `timestamp`

**Non-functional**: propagation target ≤ 5 seconds; reconnection must re-sync missed events via `Last-Event-ID` header + re-fetch queue snapshot; if SSE unavailable, server returns `503` with `X-Fallback-Poll-Interval` header.

### 6.8 Doctor-Resource Mapping APIs (Phase 7 – §11.2, mandatory)

| Method | Path | Request body / params | Response | Purpose |
|--------|------|------------------------|----------|--------|
| POST | `/doctor-resource-mappings` | CreateDoctorResourceMappingRequest | DoctorResourceMappingResponse | Create mapping |
| GET | `/doctor-resource-mappings` | doctorUserId?, resourceId?, branchId?, status?, page?, size? | PagedResponse&lt;DoctorResourceMappingResponse&gt; | List mappings |
| GET | `/doctor-resource-mappings/resolve` | doctorUserId, branchId? | DoctorResourceMappingResponse | Resolve mapping for current session (uses fallback order) |
| PATCH | `/doctor-resource-mappings/{id}` | UpdateDoctorResourceMappingRequest | DoctorResourceMappingResponse | Update or deactivate mapping |

**CreateDoctorResourceMappingRequest:**  
`doctorUserId`, `resourceId`, `branchId?`, `isPrimary?`, `effectiveFrom?`, `effectiveTo?`

**DoctorResourceMappingResponse:**  
`id`, `doctorUserId`, `resourceId`, `branchId`, `isPrimary`, `effectiveFrom`, `effectiveTo`, `status`, `createdAt`, `updatedAt`

**Error contract**: if `/resolve` cannot find a mapping, return `404` with code `DOCTOR_RESOURCE_MAPPING_NOT_FOUND` and actionable message (do not fall back silently).

### 6.9 Audit Log APIs (Phase 7 – §11.7, mandatory)

| Method | Path | Request body / params | Response | Purpose |
|--------|------|------------------------|----------|--------|
| GET | `/audit-log` | entityType?, entityId?, actorId?, action?, fromDate?, toDate?, correlationId?, page?, size? | PagedResponse&lt;AuditLogResponse&gt; | Search audit log |
| GET | `/audit-log/{id}` | — | AuditLogResponse | Get single audit entry |

**AuditLogResponse:**  
`id`, `entityType`, `entityId`, `action`, `actorId`, `actorRole`, `bookingChannel`, `reason`, `correlationId`, `beforeState`, `afterState`, `createdAt`

**Access**: read-only, requires `schedule.audit.read` RBAC permission.

---

## 7. Events and Integrations (Summary)

- **Incoming**: Doctor/branch/department masters (read from hospital-service or Doctor module REST); optional bed capacity from IPD module.
- **Outgoing**: `appointment.created`, `appointment.rescheduled`, `appointment.cancelled`, `appointment.checked_in`, `appointment.no_show`; `planned_admission.reserved`, `planned_admission.converted`. Consumers: Portal BFF, Billing (no-show fee), analytics.

---

## 8. Phased Implementation – Granular Checklists

Implement in order. Each phase is self-contained so that "implement Phase N" can be executed without omission.

---

### Phase 0 – Foundations & Service Skeleton

**Goal**: Deployable `hospital-scheduling-service` with health, discovery, and API Gateway route; no domain logic yet.

#### 0.1 Maven module

- [ ] Create `easyops-erp/services/hospital-scheduling-service/pom.xml`.
  - Parent: `com.easyops:easyops-erp:1.0.0`.
  - Artifact: `hospital-scheduling-service`, packaging `jar`.
  - Dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `spring-cloud-starter-netflix-eureka-client`, `spring-cloud-starter-loadbalancer`, `postgresql` (runtime), `liquibase-core`, `lombok` (optional), `micrometer-registry-prometheus`, `springdoc-openapi-starter-webmvc-ui` (or equivalent). Same pattern as `hospital-billing-service/pom.xml`.

#### 0.2 Application and config

- [ ] Create `easyops-erp/services/hospital-scheduling-service/src/main/java/com/easyops/hospitalscheduling/HospitalSchedulingServiceApplication.java`.
  - `@SpringBootApplication`, `@EnableJpaAuditing` if used later.
- [ ] Create `easyops-erp/services/hospital-scheduling-service/src/main/resources/application.yml`.
  - `spring.application.name: hospital-scheduling-service`
  - Server port: **8092**.
  - JPA: default schema `hospital_scheduling`, ddl-auto `none`, Hibernate dialect for PostgreSQL.
  - Datasource URL, username, password (placeholders; same DB as hospital-service).
  - Eureka client config (defaultZone, etc.).
  - Actuator: expose health, info, metrics, prometheus.
- [ ] Create `application-dev.yml` / `application-local.yml` if needed (e.g. local DB URL).

#### 0.3 API Gateway

- [ ] In `easyops-erp/services/api-gateway/src/main/resources/application.yml`, add route:
  - id: `hospital-scheduling-service`
  - uri: `lb://hospital-scheduling-service`
  - predicates: `Path=/api/hospital-scheduling/**`

#### 0.4 Database schema (Liquibase in hospital-service)

- [ ] Create `easyops-erp/services/hospital-service/src/main/resources/db/changelog/changesets/023-hospital-scheduling-schema.sql`.
  - Liquibase formatted SQL.
  - Create schema `hospital_scheduling` if not exists; grant to app user.
  - Phase 0: empty schema only; Phase 1 will add tables in same file or new changeset 024.
- [ ] In `easyops-erp/services/hospital-service/src/main/resources/db/changelog/db.changelog-master.xml`, add:
  - `<include file="changesets/023-hospital-scheduling-schema.sql" relativeToChangelogFile="true"/>`

#### 0.5 Parent POM

- [ ] In `easyops-erp/pom.xml`, add `<module>services/hospital-scheduling-service</module>`.

#### 0.6 Health and info

- [ ] Ensure actuator health and info endpoints respond when service runs and DB is available.

**Deliverables**: Service starts, registers with Eureka, reachable via API Gateway at `/api/hospital-scheduling/**`. No domain tables or controllers required in Phase 0.

**Frontend**: None.

---

### Phase 1 – Core Scheduling Masters & Resource Calendars

**Goal**: Resources, working hours, slot templates, blackouts, generic reservations, availability API, and conflict detection. Teams can configure and visualize working hours and slots for selected doctors and rooms.

#### 1.1 Database (Liquibase)

- [ ] In `easyops-erp/services/hospital-service/src/main/resources/db/changelog/changesets/023-hospital-scheduling-schema.sql` (or new `024-hospital-scheduling-core-tables.sql`), add:
  - Table `hospital_scheduling.scheduling_resources` (all columns from §5.1).
  - Table `hospital_scheduling.scheduling_working_hours` (all columns from §5.2).
  - Table `hospital_scheduling.scheduling_slot_templates` (all columns from §5.3).
  - Table `hospital_scheduling.scheduling_blackouts` (all columns from §5.4).
  - Table `hospital_scheduling.scheduling_reservations` (all columns from §5.5).
  - Optional: table `hospital_scheduling.scheduling_booking_rules` (§5.6).
  - Indexes as listed in §5 (for resources, working_hours, slot_templates, blackouts, reservations).
  - FKs: working_hours.resource_id → scheduling_resources(id); blackouts.resource_id → scheduling_resources(id); reservations.resource_id → scheduling_resources(id).
- [ ] If using new file 024, include it in `db.changelog-master.xml`.

#### 1.2 Backend – Resource context

- [ ] Package: `com.easyops.hospitalscheduling.domain.resource`.
- [ ] Entity: `SchedulingResource` (map to `hospital_scheduling.scheduling_resources`).
- [ ] Repository: `SchedulingResourceRepository` (JpaRepository); methods: `findByResourceTypeAndExternalReferenceId`, `findByBranchId`, `findByResourceTypeAndStatus`.
- [ ] DTOs (under `com.easyops.hospitalscheduling.api.dto`):
  - Request: `CreateResourceRequest`, `UpdateResourceRequest` (partial).
  - Response: `ResourceResponse` (all fields from §6.1).
- [ ] Service: `SchedulingResourceService` – create, get by id, list with filters (paginated), update.
- [ ] Controller: `ResourceController` – base path so Gateway forwards to `/api/hospital-scheduling`: `POST /resources`, `GET /resources/{id}`, `GET /resources`, `PATCH /resources/{id}`.

#### 1.3 Backend – Working hours

- [ ] Package: `com.easyops.hospitalscheduling.domain.resource` (or `domain.calendar`).
- [ ] Entity: `WorkingHours` (map to `scheduling_working_hours`).
- [ ] Repository: `WorkingHoursRepository`; methods: `findByResourceId`, `findByResourceIdAndDayOfWeek`.
- [ ] DTOs: `CreateWorkingHoursRequest`, `WorkingHoursResponse`; support batch create (list of dayOfWeek/startTime/endTime).
- [ ] Service: `WorkingHoursService` – set working hours for resource (replace or merge per business rule), get by resource.
- [ ] Controller: `ResourceController` or `WorkingHoursController` – `POST /resources/{id}/working-hours`, `GET /resources/{id}/working-hours`.

#### 1.4 Backend – Slot templates

- [ ] Entity: `SlotTemplate` (map to `scheduling_slot_templates`).
- [ ] Repository: `SlotTemplateRepository`; methods: `findByResourceType`, `findByBranchId`, `findByStatus`.
- [ ] DTOs: `CreateSlotTemplateRequest`, `SlotTemplateResponse`, `UpdateSlotTemplateRequest` (optional).
- [ ] Service: `SlotTemplateService` – create, get by id, list with filters (paginated).
- [ ] Controller: `SlotTemplateController` – `POST /slot-templates`, `GET /slot-templates/{id}`, `GET /slot-templates`.

#### 1.5 Backend – Blackouts

- [ ] Entity: `Blackout` (map to `scheduling_blackouts`).
- [ ] Repository: `BlackoutRepository`; methods: `findByResourceIdAndBlackoutDateBetween`, `findByBranchIdAndBlackoutDateBetween`.
- [ ] DTOs: `CreateBlackoutRequest`, `BlackoutResponse`.
- [ ] Service: `BlackoutService` – create, list with filters (resourceId, branchId, fromDate, toDate), delete by id.
- [ ] Controller: `BlackoutController` – `POST /blackouts`, `GET /blackouts`, `DELETE /blackouts/{id}`.

#### 1.6 Backend – Reservations and conflict detection

- [ ] Package: `com.easyops.hospitalscheduling.domain.reservation`.
- [ ] Entity: `Reservation` (map to `scheduling_reservations`).
- [ ] Repository: `ReservationRepository`; methods: `findByResourceIdAndSlotStartBetweenAndSlotEndBetween` (overlap query), `findByPatientId`, `findByIdempotencyKey`, list with filters (resourceId, patientId, status, from, to) with pagination.
- [ ] DTOs: `CreateReservationRequest`, `ReservationResponse`, `UpdateReservationStatusRequest`, `CheckConflictsRequest`, `ConflictCheckResponse`.
- [ ] Service: `ReservationService` – create (validate no conflict unless overbooking allowed; set idempotency); get by id; list with filters; update status; check conflicts (overlapping reservations for resource in time range).
- [ ] Controller: `ReservationController` – `POST /reservations`, `GET /reservations/{id}`, `GET /reservations`, `PATCH /reservations/{id}/status`, `POST /reservations/check-conflicts`.

#### 1.7 Backend – Availability

- [ ] Service: `AvailabilityService` – given resourceId, fromDate, toDate, optional slotTemplateId: load working hours and blackouts for resource; generate slots (using template or default duration); for each slot, count existing non-cancelled reservations and compare to capacity (from template or 1); return list of slots with availableCount.
- [ ] Controller: `AvailabilityController` or under `ReservationController`: `GET /availability` (query params: resourceId, fromDate, toDate, slotTemplateId?).
- [ ] Response DTO: `AvailabilityResponse` (resourceId, date, slots array with start, end, availableCount, blackouts optional).

#### 1.8 Backend – Shared and global

- [ ] Add OpenAPI dependency and config so `/v3/api-docs` and Swagger UI work.
- [ ] Global exception handler: 404, 400 validation, 409 conflict (e.g. duplicate idempotency key, conflict on create reservation) with consistent JSON body.
- [ ] DTO: `PagedResponse<T>` (content, totalElements, totalPages, size, number) for all list endpoints.

#### 1.9 Frontend – Service and types

- [ ] Create `easyops-erp/frontend/src/services/hospitalSchedulingService.ts`.
  - Base URL: `/api/hospital-scheduling`.
  - Types: Resource, ResourceResponse, CreateResourceRequest; WorkingHoursResponse, CreateWorkingHoursRequest; SlotTemplate, SlotTemplateResponse, CreateSlotTemplateRequest; Blackout, BlackoutResponse, CreateBlackoutRequest; Reservation, ReservationResponse, CreateReservationRequest, UpdateReservationStatusRequest, AvailabilityResponse, ConflictCheckResponse; PagedResponse&lt;T&gt;.
  - Methods: createResource, getResource, getResources(params); setWorkingHours(resourceId, body), getWorkingHours(resourceId); createSlotTemplate, getSlotTemplate, getSlotTemplates(params); createBlackout, getBlackouts(params), deleteBlackout(id); getAvailability(params), createReservation(body), getReservation(id), getReservations(params), updateReservationStatus(id, body), checkConflicts(body).

#### 1.10 Frontend – Resources page

- [ ] Create `easyops-erp/frontend/src/pages/hospital/SchedulingResources.tsx`.
  - List resources: type, external ref, name, branch, department, status, actions (Edit, View working hours).
  - Filters: resourceType, branchId, status.
  - "Create resource" form: resourceType, externalReferenceId, name, branchId, departmentId, status. Submit → createResource.
  - Route: `/hospital/scheduling/resources`.

#### 1.11 Frontend – Working hours and slot templates

- [ ] In SchedulingResources or separate page: "Working hours" for a resource – list day_of_week, start_time, end_time; "Set working hours" form (batch: multiple rows for Mon–Sun). Submit → setWorkingHours.
  - Optional separate page: `SchedulingSlotTemplates.tsx` – list slot templates; create form (name, resourceType, branchId, slotDurationMinutes, slotsPerInterval, startTime, endTime, leadTimeDays, maxAdvanceDays). Route: `/hospital/scheduling/slot-templates`.

#### 1.12 Frontend – Blackouts and availability

- [ ] Create `easyops-erp/frontend/src/pages/hospital/SchedulingBlackouts.tsx` (or section under a "Calendar" page): list blackouts (resource/branch, date, reason); create blackout (resourceId/branchId, date, reason). Route: `/hospital/scheduling/blackouts`.
- [ ] Create simple "Availability" view: select resource, fromDate, toDate, optional template → call getAvailability → show grid or list of slots with available count.

#### 1.13 Frontend – Reservations (generic)

- [ ] List reservations: resourceId, patientId, slot start/end, status, actions (View, Update status, Cancel). Create reservation form: resourceId, slotStart, slotEnd, status, patientId, idempotencyKey. Route: `/hospital/scheduling/reservations` or under same Scheduling page.

#### 1.14 Frontend – Routes and navigation

- [ ] In `easyops-erp/frontend/src/App.tsx`: under `isModuleEnabled('hospital')`, add routes:
  - `hospital/scheduling/resources` → SchedulingResources
  - `hospital/scheduling/slot-templates` → SchedulingSlotTemplates (if separate)
  - `hospital/scheduling/blackouts` → SchedulingBlackouts
  - `hospital/scheduling/reservations` → SchedulingReservations (or combined Scheduling calendar page)
  - `hospital/scheduling/availability` → Availability view (or embedded in reservations)
- [ ] In `easyops-erp/frontend/src/components/Layout/MainLayout.tsx`: under Hospital menu, add:
  - "Scheduling – Resources", path `/hospital/scheduling/resources`
  - "Scheduling – Slot templates", path `/hospital/scheduling/slot-templates`
  - "Scheduling – Blackouts", path `/hospital/scheduling/blackouts`
  - "Scheduling – Reservations", path `/hospital/scheduling/reservations`
  - "Scheduling – Availability", path `/hospital/scheduling/availability`
  - Use icon e.g. CalendarMonth, Schedule (MUI).

**Deliverables**: Resource calendars configured for at least one pilot department; working hours and slots configurable; availability API and generic reservation create/update/conflict check working; frontend can manage resources, working hours, templates, blackouts, and reservations.

---

### Phase 2 – OPD Appointment Booking & Management

**Goal**: Full OPD appointment lifecycle (create, reschedule, cancel, check-in, no-show); queue view per doctor/clinic; booking and cancellation rules enforced. Front desk can book, reschedule, cancel and view appointments.

#### 2.1 Database (Liquibase)

- [x] Create `easyops-erp/services/hospital-service/src/main/resources/db/changelog/changesets/028-hospital-scheduling-appointments.sql` (plan §022; repo uses 028).
  - Table `hospital_scheduling.scheduling_appointments` (all columns from §5.7).
  - Indexes: `scheduling_appointments(patient_id, appointment_date)`, `scheduling_appointments(resource_id, appointment_date)`, `scheduling_appointments(idempotency_key)`, `scheduling_appointments(reservation_id)`.
  - FK: `scheduling_appointments(reservation_id)` → scheduling_reservations(id), `scheduling_appointments(resource_id)` → scheduling_resources(id).
  - Include in `db.changelog-master.xml`.

#### 2.2 Backend – Appointment entity and repository

- [x] Package: `com.easyops.hospitalscheduling.domain.appointment`.
- [x] Entity: `Appointment` (map to `scheduling_appointments`).
- [x] Repository: `AppointmentRepository`; methods: `findByPatientIdAndAppointmentDateBetween`, `findByResourceIdAndAppointmentDate`, `findByReservationId`, `findByIdempotencyKey`, list with specification (patientId, resourceId, clinicId, fromDate, toDate, status) with pagination.

#### 2.3 Backend – Appointment service

- [x] DTOs: `CreateAppointmentRequest`, `AppointmentResponse`, `AppointmentDetailResponse`, `RescheduleAppointmentRequest`, `CancelAppointmentRequest`, `QueueResponse`.
- [x] Service: `AppointmentService`:
  - create: validate slot available (via ReservationService conflict check or availability); create Reservation (CONFIRMED) then Appointment linked to it; assign token_number for same resource+date (max+1); idempotency by idempotencyKey.
  - get by id (with reservation); list with filters (paginated).
  - reschedule: load appointment and reservation; check new slot available; update reservation slot_start/slot_end; update appointment slot_start/slot_end and appointment_date.
  - cancel: update reservation and appointment status to CANCELLED; optional reason and idempotency.
  - checkIn: set reservation and appointment status to CHECKED_IN.
  - noShow: set reservation and appointment status to NO_SHOW (emit event for Billing if applicable).
- [x] Controller: `AppointmentController` – `POST /appointments`, `GET /appointments/{id}`, `GET /appointments`, `POST /appointments/{id}/reschedule`, `POST /appointments/{id}/cancel`, `POST /appointments/{id}/check-in`, `POST /appointments/{id}/no-show`, `GET /appointments/queue` (resourceId, date).

#### 2.4 Backend – Booking rules (optional)

- [x] If scheduling_booking_rules exists: validate cancellation against cancellation_cutoff_hours; validate max_per_slot when creating appointment. Otherwise document "booking rules Phase 2 optional".
  - Implemented: `BookingRule` entity, `BookingRuleRepository` (findByScopeTypeAndScopeId, findByScopeTypeAndScopeIdIsNull). In `AppointmentService`: create checks effective `max_per_slot` (RESOURCE > BRANCH > GLOBAL); cancel checks effective `cancellation_cutoff_hours` and rejects if within cutoff of slot start.

#### 2.5 Backend – Events

- [x] After create/reschedule/cancel/check-in/no-show: publish domain events or outbound events (appointment.created, appointment.rescheduled, appointment.cancelled, appointment.checked_in, appointment.no_show). Use ApplicationEventPublisher or Kafka/Rabbit; document payload schema.
  - Implemented: `AppointmentEvent` (type, appointmentId, reservationId, patientId, resourceId, status, slotStart, slotEnd, appointmentDate, timestamp), `AppointmentEventPublisher` (publishCreated/Rescheduled/Cancelled/CheckedIn/NoShow). `AppointmentService` publishes after each mutation; no event on idempotent create. Payload schema documented in `services/hospital-scheduling-service/EVENTS.md`.

#### 2.6 Frontend – Service

- [x] In `hospitalSchedulingService.ts`: add createAppointment, getAppointment, getAppointments(params), rescheduleAppointment(id, body), cancelAppointment(id, body), checkInAppointment(id), noShowAppointment(id), getAppointmentQueue(resourceId, date); types CreateAppointmentRequest, AppointmentResponse, AppointmentDetailResponse, RescheduleAppointmentRequest, CancelAppointmentRequest, QueueResponse.

#### 2.7 Frontend – Appointments page

- [x] Create `easyops-erp/frontend/src/pages/hospital/SchedulingAppointments.tsx`.
  - List appointments: patient Id, doctor (resource), clinic, date, slot, type, status, token, actions (View, Reschedule, Cancel, Check-in, No-show).
  - Filters: patientId, resourceId, clinicId, fromDate, toDate, status.
  - "Book appointment" form: patientId, resourceId, clinicOrLocationId, appointmentDate, slot (from availability or manual), appointmentType, idempotencyKey. Submit → createAppointment.
  - Detail view: show AppointmentDetailResponse (appointment + reservation).
  - Reschedule: pick new slot → rescheduleAppointment. Cancel: reason → cancelAppointment. Check-in / No-show buttons.
  - Route: `/hospital/scheduling/appointments`.

#### 2.8 Frontend – Queue view

- [x] Create `easyops-erp/frontend/src/pages/hospital/SchedulingQueue.tsx` or section in SchedulingAppointments: select resource (doctor), date → getAppointmentQueue → show ordered list (token, patient, slot, status). Route: `/hospital/scheduling/queue`.

#### 2.9 Frontend – Routes and nav

- [x] In `App.tsx`: add `hospital/scheduling/appointments` → SchedulingAppointments, `hospital/scheduling/queue` → SchedulingQueue.
- [x] In MainLayout: add "Scheduling – Appointments", "Scheduling – Queue" with paths above.

**Deliverables**: At least one OPD clinic can operate via electronic appointment booking; front desk can book, reschedule, cancel, check-in, no-show and view queue with minimal workarounds.

---

### Phase 3 – IPD Admission, Transfer & Discharge Scheduling

**Goal**: Planned admission requests with preferred date and ward/bed class; bed/ward-level reservation logic; planned transfer and discharge dates surfaced to operations; conversion flow to actual admission (integration point with IPD module).

#### 3.1 Database (Liquibase)

- [x] Create `easyops-erp/services/hospital-service/src/main/resources/db/changelog/changesets/029-hospital-scheduling-planned-admissions.sql` (plan §023; repo uses 029).
  - Table `hospital_scheduling.scheduling_planned_admissions` (all columns from §5.8).
  - Indexes: `scheduling_planned_admissions(patient_id)`, `scheduling_planned_admissions(preferred_date)`, `scheduling_planned_admissions(status)`.
  - FKs: bed_group_resource_id → scheduling_resources(id), reservation_id → scheduling_reservations(id).
  - Include in `db.changelog-master.xml`.

#### 3.2 Backend – Planned admission context

- [x] Package: `com.easyops.hospitalscheduling.domain.plannedadmission`.
- [x] Entity: `PlannedAdmission` (map to `scheduling_planned_admissions`).
- [x] Repository: `PlannedAdmissionRepository`; methods: findByPatientId, findByPreferredDateBetween, findByStatus, list with filters (patientId, preferredDateFrom, preferredDateTo, status) with pagination.
- [x] DTOs: `CreatePlannedAdmissionRequest`, `PlannedAdmissionResponse`, `UpdatePlannedAdmissionStatusRequest`, `ExpectedAdmissionsResponse` (items: date, count, byWardOrClass optional).
- [x] Service: `PlannedAdmissionService` – create (status REQUESTED); update status (RESERVED when bed reserved and reservation created, CONVERTED when converted to actual admission, EXPIRED, CANCELLED); list; get expected admissions by date range (aggregate by date, optional by ward/bed class).
- [x] Controller: `PlannedAdmissionController` – `POST /planned-admissions`, `GET /planned-admissions/{id}`, `GET /planned-admissions`, `PATCH /planned-admissions/{id}/status`, `GET /planned-admissions/expected` (fromDate, toDate, wardOrBedClass?).

#### 3.3 Backend – Reservation link

- [x] When status set to RESERVED: create or link Reservation (resource = bed group or ward), set expires_at from booking rules (e.g. 24 hours). When CONVERTED, optional: update or close reservation. Document integration with IPD module for "convert to admission" (IPD module may call scheduling to mark CONVERTED and pass admission_id).
  - RESERVED: create reservation (bed group, preferred date full-day slot), set reservation_id, bed_group_resource_id, expires_at (request or default 24h; booking rules could be extended for expiry). Comment in code.
  - CONVERTED: set linked reservation status to COMPLETED, then set planned admission status CONVERTED.
  - Documented in `services/hospital-scheduling-service/IPD_INTEGRATION.md` (reservation link behaviour, convert-to-admission flow, optional future admission_id).

#### 3.4 Frontend – Service

- [x] In `hospitalSchedulingService.ts`: add createPlannedAdmission, getPlannedAdmission, getPlannedAdmissions(params), updatePlannedAdmissionStatus(id, body), getExpectedAdmissions(fromDate, toDate, wardOrBedClass); types as in §6.4.

#### 3.5 Frontend – Planned admissions and expected view

- [x] Create `easyops-erp/frontend/src/pages/hospital/SchedulingPlannedAdmissions.tsx`: list planned admissions (patient, preferred date, ward/bed class, status, expires at, actions); create form (patientId, preferredDate, preferredWardOrBedClass); update status (Reserve, Convert, Expire, Cancel).
  - Create `SchedulingExpectedAdmissions.tsx` or dashboard widget: fromDate, toDate, optional ward → getExpectedAdmissions → table or chart (date, count, by ward). Routes: `/hospital/scheduling/planned-admissions`, `/hospital/scheduling/expected-admissions`.

#### 3.6 Frontend – Routes and nav

- [x] Add routes and menu items for planned admissions and expected admissions.

**Deliverables**: Bed management can see upcoming admissions and discharges for pilot wards; conversion from planned admission to actual admission tested end-to-end with IPD modules (or stubbed).

---

### Phase 4 – Resource Rosters & Cross-Resource Orchestration

**Goal**: Doctor/room/theatre roster management (pattern-based rosters, bulk updates, substitution); multi-resource booking (e.g. doctor + room) with validation that all required resources are available.

#### 4.1 Database (Liquibase)

- [x] Create `easyops-erp/services/hospital-service/src/main/resources/db/changelog/changesets/030-hospital-scheduling-rosters.sql`.
  - Table `hospital_scheduling.scheduling_roster_blocks` (all columns from §5.12).
  - Indexes as listed in §5. Include in `db.changelog-master.xml`.
  - Note: If roster_blocks is deferred, Phase 4 falls back to working_hours + blackouts only; document decision in ADR.

#### 4.2 Backend – Roster and multi-resource

- [x] Package: `com.easyops.hospitalscheduling.domain.roster` (optional).
- [x] If roster blocks: Entity `RosterBlock`, Repository, Service for create/list/delete blocks; API `POST /resources/{id}/roster-blocks`, `GET /resources/{id}/roster-blocks`, `DELETE /roster-blocks/{id}`. AvailabilityService considers roster blocks (e.g. UNAVAILABLE reduces availability, SUBSTITUTE may swap resource in response).
- [x] Multi-resource reservation: extend CreateReservationRequest or add CreateMultiResourceReservationRequest (list of resourceIds, slotStart, slotEnd). Service: for each resource check conflict; if any conflict fail; else create one Reservation per resource with same reference_type/reference_id (e.g. APPOINTMENT and same appointment_id). Document that Phase 2 appointments use single resource (doctor); Phase 4 can add "room required" and create two reservations (doctor + room) with same reference_id.
- [x] Controller: optional `POST /reservations/multi` (body: resourceIds[], slotStart, slotEnd, referenceType, referenceId, patientId, idempotencyKey) returning list of ReservationResponse.

#### 4.3 Frontend – Roster and multi-resource

- [x] If roster blocks: UI to add/remove roster blocks for a resource (date range, type, substitute). Availability view shows effect.
  - Multi-resource: in booking flow, optionally select multiple resources (e.g. doctor + room) and call multi-resource endpoint; show success only if all reserved.

**Deliverables**: Pilot departments manage provider/room rosters in system; multi-resource booking supported for at least one scenario (e.g. doctor + room).

---

### Phase 5 – Waitlists, Overbooking & Optimization

**Goal**: Waitlist per doctor/clinic with priority; overbooking rules (max per slot); auto-fill from waitlist when slot freed; notification hooks.

#### 5.1 Database (Liquibase)

- [x] Create `easyops-erp/services/hospital-service/src/main/resources/db/changelog/changesets/031-hospital-scheduling-waitlist.sql`.
  - Table `hospital_scheduling.scheduling_waitlist_entries` (all columns from §5.9).
  - Indexes as in §5. Include in `db.changelog-master.xml`.

#### 5.2 Backend – Waitlist context

- [x] Package: `com.easyops.hospitalscheduling.domain.waitlist`.
- [x] Entity: `WaitlistEntry` (map to `scheduling_waitlist_entries`).
- [x] Repository: `WaitlistEntryRepository`; methods: findByResourceIdAndStatusOrderByPriorityDesc, findByPatientId, list with filters (resourceId, patientId, status) with pagination.
- [x] DTOs: `CreateWaitlistEntryRequest`, `WaitlistEntryResponse`, `UpdateWaitlistStatusRequest`, `PromoteWaitlistRequest`, `PromoteWaitlistResponse`.
- [x] Service: `WaitlistService` – add entry; list; update status (PROMOTED, CANCELLED, EXPIRED); promote: given resourceId, slotStart, slotEnd, find pending entries by priority, create appointment for first candidate(s) (up to maxCandidates or 1), create reservation+appointment, mark entry PROMOTED, return created appointment and count contacted (notification hook to be implemented elsewhere).
- [x] Controller: `WaitlistController` – `POST /waitlist`, `GET /waitlist`, `PATCH /waitlist/{id}/status`, `POST /waitlist/promote`.

#### 5.3 Backend – Overbooking

- [x] Use scheduling_booking_rules.max_per_slot or slot template slots_per_interval: when creating appointment, allow up to max_per_slot reservations per slot; beyond that reject or require override reason. Document overbooking policy (e.g. configurable per resource).

#### 5.4 Frontend – Waitlist and promote

- [x] In `hospitalSchedulingService.ts`: add createWaitlistEntry, getWaitlist(params), updateWaitlistStatus(id, body), promoteWaitlist(body); types as in §6.5.
- [x] Create `SchedulingWaitlist.tsx`: list waitlist (patient, resource, preferred dates, priority, status); add entry form; "Promote" for a slot (resource + slot) → promoteWaitlist → show new appointment. Route: `/hospital/scheduling/waitlist`.

**Deliverables**: Waitlist in use for at least one busy OPD clinic; overbooking rules and auto-fill policies validated with operations.

---

### Phase 6 – Reporting, Monitoring & Hardening

**Goal**: Utilization and no-show/cancellation reports; occupancy dashboards; fine-grained permissions and full audit; load and concurrency testing; rollout-ready.

#### 6.1 Backend – Reporting

- [x] Package: `com.easyops.hospitalscheduling.domain.reporting` or `.api.report`.
- [x] DTOs: `UtilizationReportResponse` (resourceId, fromDate, toDate, groupBy, data points: date/slotUsed/slotAvailable or counts), `NoShowReportResponse`, `CancellationReportResponse`.
- [x] Service: aggregate from reservations/appointments: utilization = count reserved vs available slots by resource and date; no-show = count status NO_SHOW in range; cancellations = count CANCELLED in range. Optionally rates (no-show rate, cancellation rate).
- [x] Controller: `SchedulingReportController` – `GET /reports/utilization`, `GET /reports/no-show`, `GET /reports/cancellations` (params as in §6.6).

#### 6.2 Backend – Audit and security

- [x] Ensure all appointment and reservation mutations set created_by/updated_at; optional audit log table for appointment/reservation change events (who, what, when). Document "audit trail for schedule and appointment changes".
- [x] Fine-grained permissions: document or implement checks (e.g. schedule.edit, schedule.book, schedule.view_only) in controller or service; integrate with platform RBAC when available.

#### 6.3 Backend – Observability and performance

- [x] Metrics: e.g. scheduling_appointments_created_total, scheduling_reservations_conflicts_total, scheduling_availability_requests_total. Correlation IDs in logs and traces.
- [x] Index and query review for list endpoints (reservations, appointments, planned admissions, waitlist) to keep p95 under 300 ms for typical page sizes.

#### 6.4 Frontend – Reports and dashboards

- [x] Create `easyops-erp/frontend/src/pages/hospital/SchedulingReports.tsx`: Utilization (resource, from–to, groupBy → table or chart); No-show report (resource, from–to); Cancellation report (resource, from–to). Route: `/hospital/scheduling/reports`. Add nav "Scheduling – Reports".

**Deliverables**: Scheduling service stable under expected peak loads; operations and clinical leads can use scheduling views and reports; audit and permissions documented or implemented.

---

### Phase 7 – Gap-Closure: Security, Real-time, Audit & Integrations

**Goal**: Implement the mandatory requirements from §11 that were captured during implementation review. This phase is a prerequisite for production readiness of doctor-facing scheduling.

#### 7.1 Database (Liquibase)

- [ ] Create `easyops-erp/services/hospital-service/src/main/resources/db/changelog/changesets/032-hospital-scheduling-gap-closure.sql` using Liquibase formatted SQL. Include in `db.changelog-master.xml`.

  **New tables:**
  - Table `hospital_scheduling.scheduling_doctor_resource_mappings` — all columns from §5.10; UNIQUE constraint `(doctor_user_id, resource_id, branch_id)`; FKs: `resource_id` → scheduling_resources(id).
  - Table `hospital_scheduling.scheduling_audit_log` — all columns from §5.11; immutable (add `COMMENT 'insert-only'` or equivalent documentation; no cascade delete).

  **Column additions — scheduling_appointments:**
  ```sql
  ALTER TABLE hospital_scheduling.scheduling_appointments
    ADD COLUMN booking_channel VARCHAR(30) NOT NULL DEFAULT 'INTERNAL',
    ADD COLUMN booked_by UUID,
    ADD COLUMN slot_template_id UUID REFERENCES hospital_scheduling.scheduling_slot_templates(id),
    ADD COLUMN session_shift VARCHAR(20),
    ADD COLUMN session_label VARCHAR(255),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
  ```
  Backfill note: `booking_channel DEFAULT 'INTERNAL'` covers all existing rows automatically.

  **Column additions — scheduling_reservations:**
  ```sql
  ALTER TABLE hospital_scheduling.scheduling_reservations
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
  ```

  **Column additions — scheduling_booking_rules:**
  ```sql
  ALTER TABLE hospital_scheduling.scheduling_booking_rules
    ADD COLUMN channel VARCHAR(30),
    ADD COLUMN channel_daily_cap INT,
    ADD COLUMN max_advance_days INT;
  ALTER TABLE hospital_scheduling.scheduling_booking_rules
    ADD CONSTRAINT uq_booking_rules_scope_channel UNIQUE (scope_type, scope_id, channel);
  ```

  **Indexes:**
  - `CREATE INDEX IF NOT EXISTS idx_sched_appt_booking_channel ON hospital_scheduling.scheduling_appointments(booking_channel, appointment_date);`
  - `CREATE INDEX IF NOT EXISTS idx_sched_appt_slot_template ON hospital_scheduling.scheduling_appointments(slot_template_id);`
  - `CREATE INDEX IF NOT EXISTS idx_sched_drm_doctor_status ON hospital_scheduling.scheduling_doctor_resource_mappings(doctor_user_id, status);`
  - `CREATE INDEX IF NOT EXISTS idx_sched_drm_resource ON hospital_scheduling.scheduling_doctor_resource_mappings(resource_id);`
  - `CREATE INDEX IF NOT EXISTS idx_sched_audit_entity ON hospital_scheduling.scheduling_audit_log(entity_type, entity_id);`
  - `CREATE INDEX IF NOT EXISTS idx_sched_audit_actor ON hospital_scheduling.scheduling_audit_log(actor_id);`
  - `CREATE INDEX IF NOT EXISTS idx_sched_audit_created ON hospital_scheduling.scheduling_audit_log(created_at);`

#### 7.2 Backend – Doctor-resource mapping service

- [ ] Package: `com.easyops.hospitalscheduling.domain.doctormapping`.
- [ ] Entity: `DoctorResourceMapping` (map to `scheduling_doctor_resource_mappings`).
- [ ] Repository: `DoctorResourceMappingRepository`; methods: `findByDoctorUserIdAndStatus`, `findByDoctorUserIdAndBranchIdAndStatus`, `findByResourceId`.
- [ ] Service: `DoctorResourceMappingService` – create, list, resolve (fallback order: exact branch → null-branch → throw `DOCTOR_RESOURCE_MAPPING_NOT_FOUND`), update status.
- [ ] Controller: `DoctorResourceMappingController` – all routes from §6.8; `GET /doctor-resource-mappings/resolve` must be callable by authenticated doctor sessions.

#### 7.3 Backend – Doctor-scoped access control

- [ ] Implement `DoctorScopeResolver`: reads JWT claims, calls `DoctorResourceMappingService.resolve`, caches result per request. Throw `403` with `UNAUTHORIZED_SCOPE` if mapping missing and caller lacks admin override permission.
- [ ] Apply to `AppointmentController.list` and `AppointmentController.queue`: if caller has `DOCTOR` role, auto-apply resource scope filter using resolved mapping; `scope` query param requires `schedule.admin` permission and creates audit entry.
- [ ] Audit all cross-doctor access attempts (allowed or denied) via `AuditLogService`.

#### 7.4 Backend – Audit log service

- [ ] Package: `com.easyops.hospitalscheduling.domain.audit`.
- [ ] Entity: `AuditLog` (map to `scheduling_audit_log`; `@Immutable` or equivalent — no JPA update/delete).
- [ ] Repository: `AuditLogRepository`; dynamic query with filters (entityType, entityId, actorId, action, fromDate, toDate, correlationId) with pagination.
- [ ] Service: `AuditLogService` – `record(entityType, entityId, action, actorId, actorRole, channel, reason, correlationId, beforeState, afterState)` — insert only; `search(filters, pageable)`.
- [ ] Integrate into `AppointmentService`, `ReservationService`, `PlannedAdmissionService`, `WaitlistService`: call `AuditLogService.record` after every mutation.
- [ ] Controller: `AuditLogController` – `GET /audit-log`, `GET /audit-log/{id}` from §6.9; requires `schedule.audit.read` permission.

#### 7.5 Backend – Doctor module Feign client (§11.5, §11.10)

Before checking slot availability, `AppointmentService` must validate doctor master constraints from the Doctor module. This requires a Feign client to the `hospital-service` (or a dedicated doctor service if extracted).

- [ ] Add dependency `spring-cloud-starter-openfeign` to `hospital-scheduling-service/pom.xml` if not already present.
- [ ] Create `easyops-erp/services/hospital-scheduling-service/src/main/java/com/easyops/hospitalscheduling/client/DoctorClient.java`:
  - `@FeignClient(name = "hospital-service", path = "/api/hospital")` (adjust path to match hospital-service's doctor endpoint prefix).
  - Method: `GET /doctors/{doctorId}` → `DoctorSummaryResponse` (fields: `id`, `status` (ACTIVE/INACTIVE), `availabilityStatus` (AVAILABLE/UNAVAILABLE/LEAVE), `maxAdvanceDays`, `dailySerialLimit`, `channelCaps: Map<String, Integer>`).
  - Configure `fallbackFactory` returning a stub that throws `ServiceUnavailableException` (do not silently book against stale data per §11.10).
  - Add `FeignConfig` class with `@EnableFeignClients` scan if not already global.
- [ ] Create `DoctorSummaryResponse` DTO in `com.easyops.hospitalscheduling.client.dto`.
- [ ] In `AppointmentService.create` and `.reschedule`, call `DoctorClient.getDoctor(resourceId → externalReferenceId)` **before** availability check:
  - If `status != ACTIVE` → reject `DOCTOR_NOT_ACTIVE`.
  - If `availabilityStatus == UNAVAILABLE or LEAVE` → reject `DOCTOR_NOT_AVAILABLE`.
  - If `maxAdvanceDays` set and `appointmentDate > today + maxAdvanceDays` → reject `ADVANCE_WINDOW_EXCEEDED`.
  - If `dailySerialLimit` set: count existing CONFIRMED/CHECKED_IN appointments for that doctor on that date; reject with `DAILY_LIMIT_REACHED` if at or over limit.
- [ ] Log a WARN (not error) when Feign call fails and service is unavailable; surface as `503` to caller with `DOCTOR_SERVICE_UNAVAILABLE` code.

#### 7.6 Backend – Booking channel and quota enforcement (§11.6)

- [ ] Update `AppointmentService.create`: require `bookingChannel` (validate is non-null and a known enum value: WEB, MOBILE, FRONT_DESK, CALL_CENTER, INTERNAL); populate `booking_channel`, `booked_by`, `slot_template_id`, `session_shift`, `session_label` on entity.
- [ ] Add `BookingRuleService.checkChannelQuota(resourceId, branchId, bookingChannel, appointmentDate)`:
  - Load rules with effective precedence: RESOURCE+channel > BRANCH+channel > GLOBAL+channel > RESOURCE+null > BRANCH+null > GLOBAL+null.
  - Count today's appointments for the resource/channel/date combination.
  - Reject with `CHANNEL_LIMIT_REACHED` if `channel_daily_cap` is set and count ≥ cap.
- [ ] Call `checkChannelQuota` from `AppointmentService.create` after `DoctorClient` checks and before reservation creation.
- [ ] Define and register all rejection error codes as constants in `SchedulingErrorCodes.java`: `DOCTOR_NOT_ACTIVE`, `DOCTOR_NOT_AVAILABLE`, `DOCTOR_SERVICE_UNAVAILABLE`, `ADVANCE_WINDOW_EXCEEDED`, `DAILY_LIMIT_REACHED`, `CHANNEL_LIMIT_REACHED`, `SLOT_UNAVAILABLE`, `MAX_PER_SLOT_EXCEEDED`, `CANCELLATION_CUTOFF_EXCEEDED`, `DOCTOR_RESOURCE_MAPPING_NOT_FOUND`, `UNAUTHORIZED_SCOPE`. Each code must appear in API error response body as `{ "errorCode": "...", "message": "..." }`.

#### 7.7 Backend – Real-time queue (SSE)

- [ ] Add dependency: Spring WebFlux or Spring MVC SSE support (Spring `SseEmitter` if servlet stack, `ServerSentEvent` if reactive).
- [ ] Implement `QueueEventBroadcaster`: in-memory `ConcurrentHashMap<String, List<SseEmitter>>` keyed by `"resourceId:date"`. Register emitter on SSE connect; remove on complete/timeout/error.
- [ ] After `AppointmentService` mutates status: call `QueueEventBroadcaster.broadcast(resourceId, date, QueueUpdateEvent)`. Include `id` (event id = UUID) for `Last-Event-ID`.
- [ ] Controller: `GET /appointments/queue/stream` — create `SseEmitter`, register with broadcaster, send current queue snapshot as first event on connect, then push subsequent events. Timeout: 30 minutes.
- [ ] Reconnect recovery: on connect with `Last-Event-ID`, query audit log for events after that id for the given resourceId+date and replay them.
- [ ] Document fallback: clients with no SSE support should poll `GET /appointments/queue` with interval from `X-Fallback-Poll-Interval` response header.

#### 7.8 Backend – Timezone enforcement

- [ ] Config: `hospital.scheduling.default-timezone` (e.g. `Asia/Dhaka`); override per branch in `scheduling_resources.metadata` (`{ "timezone": "..." }`).
- [ ] Utility: `SchedulingTimeZoneResolver.resolveForBranch(branchId)` — returns ZoneId.
- [ ] `AppointmentService.create`: derive `appointment_date` from `slotStart` converted to branch timezone; reject if `appointmentDate` in request does not match derived date (prevent off-by-one on DST boundary).
- [ ] `GET /appointments/queue` and filtered list: interpret `date` param as a local calendar day in branch timezone; query using timezone-aware `[start_of_day_UTC, end_of_day_UTC]` range.
- [ ] Integration test: cover DST boundary day for at least one pilot timezone.

#### 7.9 Backend – Session model enforcement

- [ ] Update `AppointmentRepository` query filters to accept `slotTemplateId` and `sessionShift` filter conditions.
- [ ] `AppointmentService.create`: if `slotTemplateId` provided, verify FK exists and copy `name` → `session_label`; if `sessionShift` provided but no `slotTemplateId`, set shift only.
- [ ] Ensure `GET /appointments` and `GET /appointments/queue` filter by `slotTemplateId`, `sessionShift`, `sessionLabel` consistently.

#### 7.10 Backend – Concurrency, locking, and rate limiting (§9, §11.14)

**Optimistic locking (double-booking prevention):**

- [ ] Add `@Version` field (`version BIGINT DEFAULT 0`) to `scheduling_reservations` and `scheduling_appointments` JPA entities.
- [ ] Add `version BIGINT NOT NULL DEFAULT 0` column to both tables in changeset `032-hospital-scheduling-gap-closure.sql`.
- [ ] In `ReservationService.create`: wrap conflict check + insert in a single transaction with `@Transactional(isolation = READ_COMMITTED)`; rely on version/unique index to catch race conditions and surface `409 CONFLICT` with `SLOT_CONFLICT` error code on `ObjectOptimisticLockingFailureException`.
- [ ] Document the chosen strategy (optimistic locking) in an ADR file `easyops-erp/services/hospital-scheduling-service/ADR-001-concurrency.md`.

**Rate limiting (§11.14):**

- [ ] In `easyops-erp/services/api-gateway/src/main/resources/application.yml`, add rate limiter filter for the `hospital-scheduling-service` route:
  - Use `RequestRateLimiter` filter (`spring-cloud-gateway-mvc` or `spring-cloud-starter-gateway`) backed by Redis (`RedisRateLimiter`).
  - Config: `redis-rate-limiter.replenishRate = 100`, `redis-rate-limiter.burstCapacity = 200` (adjust per load test; document as configurable).
  - Key resolver: `PrincipalNameKeyResolver` (per authenticated user) for booking endpoints; `RemoteAddrKeyResolver` for unauthenticated availability queries.
  - Return `429 Too Many Requests` with `Retry-After` header when limit exceeded (§11.14).
- [ ] Add `spring-boot-starter-data-redis` dependency check in `api-gateway/pom.xml`; Redis is already in the stack.
- [ ] Add SLO configuration comments to `application.yml` for the scheduling route:
  ```yaml
  # SLO targets (§11.14): read p95 ≤ 300ms, write p95 ≤ 500ms, page size max 100
  ```
- [ ] In `AppointmentController` and `ReservationController`, add `@Validated` on list endpoints and enforce `size` param ≤ 100 (default 20) with `@Max(100)` validation; throw `400` with `PAGE_SIZE_EXCEEDED` if exceeded.

**Report channel filter (§11.6 + §6.6):**

- [ ] Update `SchedulingReportController.utilization`, `.noShow`, `.cancellations`: add optional `bookingChannel?` filter param; pass to service aggregate queries to filter/group by channel where provided.

#### 7.11 Frontend – Service file updates (`hospitalSchedulingService.ts`)

All new Phase 7 APIs need corresponding TypeScript methods and types. Update `easyops-erp/frontend/src/services/hospitalSchedulingService.ts`:

**New types:**
- [ ] `BookingChannel` enum: `'WEB' | 'MOBILE' | 'FRONT_DESK' | 'CALL_CENTER' | 'INTERNAL'`
- [ ] `SessionShift` enum: `'MORNING' | 'EVENING' | 'NIGHT' | 'FULL_DAY' | 'CUSTOM'`
- [ ] Update `CreateAppointmentRequest`: add `bookingChannel: BookingChannel` (required), `bookedBy?: string`, `slotTemplateId?: string`, `sessionShift?: SessionShift`, `sessionLabel?: string`.
- [ ] Update `AppointmentResponse`: add `bookingChannel`, `bookedBy`, `slotTemplateId`, `sessionShift`, `sessionLabel`.
- [ ] `DoctorResourceMappingResponse`: `id`, `doctorUserId`, `resourceId`, `branchId`, `isPrimary`, `effectiveFrom`, `effectiveTo`, `status`, `createdAt`, `updatedAt`.
- [ ] `CreateDoctorResourceMappingRequest`: `doctorUserId`, `resourceId`, `branchId?`, `isPrimary?`, `effectiveFrom?`, `effectiveTo?`.
- [ ] `AuditLogResponse`: `id`, `entityType`, `entityId`, `action`, `actorId`, `actorRole`, `bookingChannel`, `reason`, `correlationId`, `beforeState`, `afterState`, `createdAt`.
- [ ] `QueueUpdateEvent`: `eventType`, `appointmentId`, `patientId`, `tokenNumber`, `status`, `slotStart`, `timestamp`.

**New methods:**
- [ ] `getDoctorResourceMappings(params)` → `PagedResponse<DoctorResourceMappingResponse>`
- [ ] `createDoctorResourceMapping(body: CreateDoctorResourceMappingRequest)` → `DoctorResourceMappingResponse`
- [ ] `updateDoctorResourceMapping(id: string, body: Partial<CreateDoctorResourceMappingRequest>)` → `DoctorResourceMappingResponse`
- [ ] `resolveDoctorResourceMapping(doctorUserId: string, branchId?: string)` → `DoctorResourceMappingResponse`
- [ ] `getAuditLog(params)` → `PagedResponse<AuditLogResponse>`
- [ ] `getAuditLogEntry(id: string)` → `AuditLogResponse`
- [ ] `subscribeQueueStream(resourceId: string, date: string, onEvent: (e: QueueUpdateEvent) => void, onError?: (e: Event) => void)` → `() => void` (returns unsubscribe function using `EventSource`; handles reconnect with `lastEventId`).

#### 7.12 Frontend – Doctor-scoped appointment views

- [ ] Update `SchedulingAppointments.tsx`:
  - For doctor-role sessions, default to "My appointments" view (omit resourceId filter; server applies scope).
  - Add filter fields: `clinicOrLocationId`, `slotTemplateId`, `sessionShift` (dropdown with SessionShift enum values), `bookingChannel`.
  - In "Book appointment" form: add `bookingChannel` required dropdown; add `sessionShift` selector (shown when no slotTemplateId chosen); pre-populate `bookedBy` from current auth context.
  - Show user-facing error messages for all error codes from §7.6 (e.g. "Doctor is currently unavailable", "Daily booking limit reached for this channel").
- [ ] Update `SchedulingQueue.tsx`:
  - For doctor-role sessions, default to resolved resource queue (server scope).
  - Subscribe to SSE using `subscribeQueueStream`; display real-time status badges on each row (colored dot: green=CHECKED_IN, amber=CONFIRMED, red=NO_SHOW/CANCELLED).
  - On SSE connection error, fall back to polling with 10-second interval; show "Live" / "Polling" indicator in UI.
  - Add optional filter fields: `clinicId`, `slotTemplateId`, `sessionShift`.

#### 7.13 Frontend – Doctor-resource mapping page

- [ ] Create `easyops-erp/frontend/src/pages/hospital/SchedulingDoctorMappings.tsx`:
  - List view: columns doctorUserId, resourceId (with resource name), branchId, isPrimary, effectiveFrom, effectiveTo, status; actions (Edit, Deactivate).
  - Create/edit form: doctorUserId (text or lookup), resourceId (searchable dropdown of DOCTOR-type resources), branchId (optional branch dropdown), isPrimary toggle, effectiveFrom/To date pickers.
  - "Resolve" section: input doctorUserId + optional branchId → `resolveDoctorResourceMapping` → show resolved resource or error.
  - Route: `/hospital/scheduling/doctor-mappings`.
- [ ] Add nav item "Scheduling – Doctor Mappings" (requires `schedule.admin` role).

#### 7.14 Frontend – Audit log page

- [ ] Create `easyops-erp/frontend/src/pages/hospital/SchedulingAuditLog.tsx`:
  - Filter bar: entityType (dropdown), entityId (UUID input), actorId (UUID input), action (dropdown of known actions), fromDate, toDate, correlationId.
  - Results table: createdAt, entityType, entityId, action, actorId, actorRole, bookingChannel, reason, correlationId.
  - Row expand: show `beforeState` and `afterState` as a side-by-side JSON diff (use a lightweight diff lib or pre/code block).
  - Route: `/hospital/scheduling/audit-log`.
- [ ] Add nav item "Scheduling – Audit Log" (visible only to users with `schedule.audit.read` permission; hide otherwise).

#### 7.15 Frontend – Routes and nav

- [ ] In `easyops-erp/frontend/src/App.tsx`, add routes under `isModuleEnabled('hospital')`:
  - `hospital/scheduling/doctor-mappings` → `SchedulingDoctorMappings`
  - `hospital/scheduling/audit-log` → `SchedulingAuditLog`
- [ ] In `easyops-erp/frontend/src/components/Layout/MainLayout.tsx`, under Hospital → Scheduling submenu, add:
  - "Doctor Mappings", path `/hospital/scheduling/doctor-mappings` (icon: AccountBox or Person)
  - "Audit Log", path `/hospital/scheduling/audit-log` (icon: History or ManageSearch)
  - Guard both items with role/permission check before rendering.

**Deliverables**:
- Doctor module Feign client validates doctor active/availability status before every create/reschedule.
- Booking channel required on all new appointments; per-channel daily quotas enforced server-side.
- Doctor-scoped access enforced server-side via `scheduling_doctor_resource_mappings`; no client-side configuration accepted as security scope.
- Optimistic locking on reservations and appointments prevents double-booking under concurrent requests.
- Real-time queue operational with SSE (`/appointments/queue/stream`); polling fallback for unsupported clients.
- Full audit trail (`scheduling_audit_log`) for every appointment/reservation/planned-admission/waitlist mutation with actor, role, channel, reason, and correlation id.
- Rate limiting enforced at API Gateway with 429 + `Retry-After`; pagination capped at 100.
- Session model (slotTemplateId / sessionShift) stored and filterable consistently across list, queue, and reports.
- Doctor mappings and audit log manageable via dedicated frontend pages.

---

## 9. Open Design Questions / Assumptions

- **Doctor roster ownership**: Doctor rosters may be owned in Doctor module and mirrored into scheduling, or configured in scheduling with Doctor module as authoritative for identities only. Phase 1 uses scheduling_resources with external_reference_id = doctor_id; roster details (working hours) in scheduling.
- **OT vs scheduling**: Scheduling owns theatre/doctor calendar and conflict detection; OT module owns case details and procedure workflows. Clear handoff for OT booking (scheduling creates reservation, OT creates case).
- **IPD reservation window**: How far in advance planned admissions can be reserved and expiry rules (e.g. 24h before preferred date) to be confirmed with operations.
- **Planned transfer and discharge dates**: Phase 3 goal mentions “planned transfer and discharge dates surfaced to operations”. These are **owned by the IPD module** (admission, bed management, discharge workflows). Scheduling does not model or expose transfer/discharge dates; it provides planned admissions and expected admissions by date only. Integration is via convert-to-admission (IPD calls scheduling to mark CONVERTED). See `hospital-scheduling-service/IPD_INTEGRATION.md`.
- **No-show fee**: Scheduling emits appointment.no_show event; Billing subscribes and applies fee. No billing logic in scheduling.
- **Concurrency**: Use optimistic locking (version on reservation/appointment) or pessimistic lock when creating reservation for a slot to avoid double-booking; document choice in ADR.
- **Pattern-based rosters and bulk updates**: Phase 4 goal mentions "pattern-based rosters, bulk updates". Currently only single roster-block add/delete and SUBSTITUTE type are implemented. Pattern-based definitions and bulk create/delete are **deferred**; see `hospital-scheduling-service/ROSTER.md`.

---

## 10. File and Path Quick Reference

| Layer | Path / artifact |
|-------|------------------|
| Service root | `easyops-erp/services/hospital-scheduling-service/` |
| App class | `.../hospitalscheduling/HospitalSchedulingServiceApplication.java` |
| Config | `.../resources/application.yml` (port **8092**) |
| Liquibase (schema) | `easyops-erp/services/hospital-service/.../changelog/changesets/023-hospital-scheduling-schema.sql` (Phase 0 schema); `024` core tables; `028` appointments; `029` planned admissions; `030` roster blocks; `031` waitlist; `032` gap-closure (doctor mappings, audit log, column additions) |
| Changelog master | `easyops-erp/services/hospital-service/.../db.changelog-master.xml` |
| Resource | `...hospitalscheduling/domain/resource/` (SchedulingResource, WorkingHours, SlotTemplate, Blackout, BookingRule – entities, repos, services, DTOs) |
| Reservation | `...hospitalscheduling/domain/reservation/` (Reservation, Availability – service, controller, DTOs) |
| Appointment | `...hospitalscheduling/domain/appointment/` (Appointment – entity, repo, service, controller, DTOs, AppointmentEventPublisher) |
| Planned admission | `...hospitalscheduling/domain/plannedadmission/` |
| Roster | `...hospitalscheduling/domain/roster/` (RosterBlock – entity, repo, service, controller) |
| Waitlist | `...hospitalscheduling/domain/waitlist/` |
| Doctor mapping | `...hospitalscheduling/domain/doctormapping/` (DoctorResourceMapping, DoctorScopeResolver) |
| Audit | `...hospitalscheduling/domain/audit/` (AuditLog – entity, repo, service, controller) |
| Reporting | `...hospitalscheduling/api/report/` or `domain/reporting/` |
| Error codes | `...hospitalscheduling/api/SchedulingErrorCodes.java` |
| Feign client | `...hospitalscheduling/client/DoctorClient.java`, `client/dto/DoctorSummaryResponse.java` |
| Concurrency ADR | `easyops-erp/services/hospital-scheduling-service/ADR-001-concurrency.md` |
| API Gateway | `easyops-erp/services/api-gateway/src/main/resources/application.yml` (route + rate limiter) |
| Frontend service | `easyops-erp/frontend/src/services/hospitalSchedulingService.ts` |
| Frontend pages | `.../pages/hospital/SchedulingResources.tsx`, `SchedulingSlotTemplates.tsx`, `SchedulingBlackouts.tsx`, `SchedulingReservations.tsx`, `SchedulingAppointments.tsx`, `SchedulingQueue.tsx`, `SchedulingPlannedAdmissions.tsx`, `SchedulingExpectedAdmissions.tsx`, `SchedulingWaitlist.tsx`, `SchedulingReports.tsx`, `SchedulingDoctorMappings.tsx`, `SchedulingAuditLog.tsx` |
| Routes | `easyops-erp/frontend/src/App.tsx` (hospital/scheduling/*) |
| Nav | `easyops-erp/frontend/src/components/Layout/MainLayout.tsx` (Hospital submenu – Scheduling) |
| Events schema | `easyops-erp/services/hospital-scheduling-service/EVENTS.md` |
| IPD integration | `easyops-erp/services/hospital-scheduling-service/IPD_INTEGRATION.md` |
| Roster ADR | `easyops-erp/services/hospital-scheduling-service/ROSTER.md` |

When asking Cursor to "implement Phase N", point to this document and the corresponding phase section (§8) so that every checkbox (DB, backend, frontend, routes, nav) is implemented without omission.

---

## 11. Gap-Closure Requirements (Doctor Appointment and Scheduling)

This section captures mandatory requirement updates discovered during implementation-vs-requirement review. These requirements apply to scheduling flows that power doctor appointment operations.

### 11.1 Doctor-scoped access control (mandatory)

- Appointment and queue read APIs must enforce doctor-patient scope for doctor-facing users, not only generic hospital view permission.
- For doctor-facing sessions, results must be limited to:
  - appointments where `resource_id` maps to the authenticated doctor's scheduling resource, and/or
  - patients currently or recently assigned to that doctor per policy.
- Any cross-doctor access (department coverage, emergency coverage, admin override) must be policy-driven and fully audited.
- Unauthorized scope access must return a clear authorization error and emit an audit event.

### 11.2 Deterministic doctor identity mapping (mandatory)

- The system must define and enforce a deterministic mapping between authenticated doctor user and scheduling `resource_id`.
- Client-side configuration alone (for example local settings) is insufficient as a source of truth for security scope.
- Mapping resolution order must be documented and implemented server-side (example: explicit doctor-resource mapping table, then approved fallback).
- If mapping is missing or ambiguous, the API must fail safely with actionable error details.

### 11.3 Doctor queue and appointment real-time behavior (mandatory)

- Doctor-facing queue and appointment views must support near real-time updates for:
  - check-in,
  - cancellation,
  - no-show,
  - reschedule,
  - queue token changes.
- Transport can be WebSocket or SSE. If unavailable, fallback polling is required and configurable.
- Non-functional requirement: queue status propagation target <= 5 seconds for normal operations.
- Reconnection behavior and missed-event recovery must be defined (for example, re-sync on reconnect).

### 11.4 Appointment filtering contract (mandatory)

- Doctor appointment views must support filters for:
  - clinic/location,
  - session/shift,
  - date range,
  - status.
- "Session" is defined by the locked model in **§11.12** (`slot_template_id` and/or `session_shift`, optional `session_label`).
- Filter behavior must be consistent across:
  - list API (`GET /appointments`),
  - queue API (`GET /appointments/queue`),
  - doctor dashboard views.

### 11.5 Doctor master constraints in scheduling create/reschedule (mandatory)

- Appointment create/reschedule must enforce doctor master constraints before confirmation:
  - doctor active status,
  - doctor availability status,
  - maximum days-in-advance,
  - per-day patient limit where configured.
- If source-channel quotas are configured (web/mobile/front-desk), create flow must enforce them by booking channel.
- Rejection reason must be explicit and user-facing (for example `DOCTOR_NOT_AVAILABLE`, `ADVANCE_WINDOW_EXCEEDED`, `CHANNEL_LIMIT_REACHED`).

### 11.6 Booking channel and quota model (mandatory)

- Scheduling requirements must define booking channel values at API level (minimum: `WEB`, `MOBILE`, `FRONT_DESK`, `CALL_CENTER`, `INTERNAL`).
- `CreateAppointmentRequest` must include `bookingChannel` (required) and optional `bookedBy`.
- Reporting APIs should support grouping/filtering by booking channel.
- Policy must define quota precedence and fallback behavior when:
  - channel quota is reached,
  - total daily cap is reached,
  - overbooking override is requested.

### 11.7 Audit and traceability additions (mandatory)

- Appointment mutations must capture and persist:
  - actor id,
  - actor role context,
  - channel,
  - reason (where applicable: cancel, override, reassignment),
  - request correlation id.
- Audit retrieval requirements must include searchable filters for resource, patient, actor, action type, and date range.

### 11.8 API contract additions (required updates to section 6.3)

Update appointment API contracts to include:

- `GET /appointments`:
  - add `slotTemplateId?`, `sessionShift?`, optional `sessionLabel?` (see §11.12),
  - add `scope?` (for controlled server-side scoping modes where permitted).
- `GET /appointments/queue`:
  - add optional `clinicId?`, `slotTemplateId?`, `sessionShift?`, optional `sessionLabel?`.
- `CreateAppointmentRequest`:
  - add `bookingChannel` (required),
  - add `slotTemplateId?` and/or `sessionShift?` and optional `sessionLabel?` (see §11.12),
  - add `bookedBy?`.
- `AppointmentResponse`:
  - add `bookingChannel`,
  - add `slotTemplateId?`, `sessionShift?`, `sessionLabel?` (see §11.12).

### 11.9 Acceptance criteria additions (required)

For doctor-facing scheduling acceptance, add testable criteria:

- Doctor user cannot view another doctor's appointments unless explicit policy override exists.
- Doctor dashboard defaults to scoped "my appointments/my queue" without manual local configuration.
- Queue status changes are visible to subscribed clients within target latency.
- Session and clinic filters produce consistent counts across queue and appointment list.
- Booking channel caps are enforced with correct user-visible error messages and audit entries.

### 11.10 Single source of truth: Doctor master vs scheduling calendars (mandatory)

- **Authoritative identity and business rules** (active/inactive, short-term availability, serial limits, web/mobile daily caps, max advance days, fees metadata) live in the **Doctor module** (`doctor-module.md`, doctor master and related configuration).
- **Authoritative time blocks for booking** (working hours, slot templates, blackouts, roster blocks, reservations) live in **`hospital-scheduling-service`**.
- **Precedence on conflict** (when both apply to the same decision):
  1. Doctor master: doctor must be eligible for new appointments (active + available per policy).
  2. Scheduling: slot must exist in availability for the resource and must not violate blackout/roster/conflict rules.
  3. Booking rules and channel quotas: applied after (1) and (2) pass.
- If Doctor module marks a doctor unavailable but scheduling still shows open slots, **create/reschedule must reject** with a clear code (for example `DOCTOR_NOT_AVAILABLE`) until calendars are reconciled or an admin override is used (audited).
- **Synchronization**: Any mirror of doctor identity into `scheduling_resources` must be documented (frequency, failure handling). Scheduling must not silently book against stale doctor state when the platform exposes a stronger rule.

### 11.11 Time zone and calendar-day boundary (mandatory)

- All API `slot_start` / `slot_end` values must be **unambiguous** (ISO-8601 with offset or UTC with documented org default).
- Each organization (or branch) must have a configured **primary timezone** for display and for “what day is this appointment?”
- **Appointment date** (`appointment_date`) must be derivable from the canonical slot times in that org timezone (same local calendar day as the slot start unless explicitly overridden by policy).
- **Queue and list “for date”** queries (`GET /appointments/queue`, filtered lists) must use the same org timezone when interpreting `date` parameters.
- Requirements for implementers: document in service config and API docs; integration tests must cover DST boundary days for at least one pilot timezone.

### 11.12 Session model (locked definition) (mandatory)

The following **single model** applies across create, list, queue, and reports (no mixed meanings of “session”):

- **Session** is identified by **`slot_template_id`** (UUID FK to `scheduling_slot_templates`) when the appointment is tied to a template-driven slot; **or** by **`session_shift`** enum when templates are not used: `MORNING`, `EVENING`, `NIGHT`, `FULL_DAY`, `CUSTOM`.
- Optional human-readable **`session_label`** (string) may be copied from template name or shift for UI only; filters must prefer `slot_template_id` or `session_shift` for consistency.
- Persist on appointment (or resolve deterministically from slot + resource): `slot_template_id?`, `session_shift?`, `session_label?`.
- APIs use the same fields as in §11.8: list/queue filters accept `slotTemplateId?` and/or `sessionShift?` in addition to optional `sessionLabel?` for display-only search where needed.

### 11.13 Outbound integration contracts (mandatory)

Scheduling remains the **source of truth for reservations and appointment lifecycle**; it **does not** own billing line items, notification templates, or clinical note content.

| Consumer | Responsibility | Contract expectation |
|----------|----------------|----------------------|
| **Billing** | May apply fees (e.g. no-show) | Subscribe to `appointment.no_show` (and policy-defined events). Payload includes `appointmentId`, `patientId`, `resourceId`, `organizationId`/`branchId`, `slotStart`, `slotEnd`, `timestamp`. Idempotent by `appointmentId` + event type. |
| **Notification service** | Reminders, confirmations | Consume scheduling events or call scheduling read APIs; scheduling publishes at minimum `appointment.created`, `appointment.rescheduled`, `appointment.cancelled`, `appointment.checked_in` with stable identifiers for deduplication. |
| **EHR / hospital-service** | Visit linkage | When a visit is created, EHR may set `visit_id` on the appointment via agreed API or event; scheduling stores `visit_id` only as reference. |
| **Analytics / BFF** | Dashboards | Use documented event schema or REST; no duplicate lifecycle state outside scheduling. |

- **Idempotency**: Event consumers must treat `(eventType, appointmentId, logicalTimestamp)` as dedupe keys where applicable.
- **Schema**: Event field list must stay in sync with `hospital-scheduling-service/EVENTS.md` (or successor); breaking changes require versioned event type or schema version field.

### 11.14 Non-functional SLOs and API limits (mandatory)

- **Read latency**: p95 for `GET /appointments`, `GET /appointments/queue`, and `GET /availability` under nominal load ≤ **300 ms** (excluding network); document exclusions for cold start.
- **Write latency**: p95 for create/reschedule/cancel/check-in/no-show ≤ **500 ms** under nominal load.
- **Pagination**: Default page size 20; maximum page size **100** unless overridden for internal batch jobs (documented).
- **Rate limiting**: Per API key / per user / per channel for public booking endpoints (exact numbers configurable); scheduling must return **429** with `Retry-After` when applicable.
- **Availability**: Target **99.9%** monthly for scheduling API during published maintenance windows (adjust per org SLA).

### 11.15 Operational workflows (explicit scope) (mandatory labeling)

The following are **recognized requirements** but may be delivered in a **later phase**; until implemented they must be explicitly **out of scope** for “complete” doctor scheduling in release notes:

- **Bulk cancel / bulk reschedule** when a doctor is on leave (affects many appointments in one operation).
- **Reassignment** of an appointment to another doctor/resource with audit and optional patient notification trigger.
- **Pattern-based roster bulk apply** (see §9 and `ROSTER.md`).

When implemented, each workflow requires: RBAC, audit, idempotency key, dry-run/preview option where bulk, and integration hooks to Notification per policy.

### 11.16 Cross-reference: Doctor module (mandatory)

- Doctor-level appointment limits, serial configuration, web/mobile caps, advance booking window, and availability flags are specified in **`requirements/module-hospital/doctor-module.md`** (Doctor master and Doctor Scheduling Management subsections).
- This document (**§11** and scheduling APIs) must remain **consistent** with those fields; any change to limits in Doctor module triggers a review of §11.5, §11.6, and §11.10.
- Implementation checklist: when adding a new doctor master constraint, update **both** Doctor module requirements and this §11 in the same change control.

### 11.17 Data retention and staff UX (recommended)

- **Retention**: Minimum retention period for appointment and audit rows (for example **7 years** for operational/legal needs or org policy); anonymization rules for deleted patients must cascade or be documented if scheduling holds `patient_id`.
- **Staff-facing UI**: Scheduling admin and front-desk screens should meet baseline usability (keyboard navigation, visible focus) for operational use; detailed WCAG level is org policy.

### 11.18 Summary table (quick reference)

| Topic | Requirement section | Implementation phase |
|-------|---------------------|----------------------|
| Access scope | §11.1, §11.2 | Phase 7 (§7.2, §7.3) |
| Doctor-resource mapping table | §11.2 | Phase 7 (§7.1 DB, §7.2 backend, §7.10 frontend) |
| Real-time queue | §11.3 | Phase 7 (§7.6) |
| Filters / session | §11.4, §11.12 | Phase 7 (§7.8) |
| Doctor master vs scheduling | §11.5, §11.10, §11.16 | Phase 7 (§7.3, §7.5) |
| Channel / quotas | §11.6 | Phase 7 (§7.5) |
| Audit | §11.7 | Phase 7 (§7.4, §7.11) |
| API contract deltas | §11.8 | Captured in §6.3 (updated), §6.7, §6.8, §6.9 |
| Acceptance criteria | §11.9 | Verify in Phase 7 QA |
| Timezone | §11.11 | Phase 7 (§7.7) |
| Integrations | §11.13 | Phase 2 events + Phase 7 refinements |
| SLOs / limits | §11.14 | Phase 6 observability + Phase 7 rate limiting |
| Bulk / reassignment (deferred) | §11.15 | Post Phase 7 |
| Retention / UX | §11.17 | Phase 7 audit table (7-year) + UX review |