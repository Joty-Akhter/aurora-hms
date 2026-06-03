# `hospital-portal-bff-service` – Implementation Plan

## 1. Overview and Objectives

`hospital-portal-bff-service` is a **Backend-for-Frontend (BFF)** that serves the **patient**, **doctor**, and **corporate** portals for the Hospital Module. It provides:

- Aggregated, portal-specific APIs composed from:
  - `hospital-service`
  - `hospital-scheduling-service`
  - `hospital-billing-service`
  - `hospital-corporate-and-discount-service`
  - `hospital-canteen-service`
  - `hospital-card-management-service`
- UI-friendly view models and payloads tailored to each portal type without leaking UI concerns into core domain services.

**Primary objectives:**

- Provide a single, consistent API layer for all hospital portals.
- Encapsulate orchestration, aggregation, and minor presentation logic.
- Reduce chattiness and latency between browser/mobile clients and backend services.

## 2. Scope and Non-Scope

### 2.1 In-Scope

- **Portal-Oriented Aggregation APIs**
  - Patient dashboard views (visits, orders, results links, bills, payments, cards).
  - Doctor dashboard views (today’s schedule, task lists, patient summaries, pending orders/signatures).
  - Corporate dashboard views (utilization, invoices, discounts, card usage).
- **Orchestration and Composition**
  - Coordinating calls across multiple underlying `hospital-*` services.
  - Simple transformations from domain models to UI view models.
  - Combining domain data with authorization and personalization context.
- **Performance Optimizations for Portals**
  - Caching for read-heavy portal data where safe.
  - Pagination and filtering strategies aligned with UI needs.

### 2.2 Out-of-Scope

- Owning or duplicating **core domain logic**:
  - No billing calculations, discount rule evaluation, or clinical decision logic.
  - No stateful business workflows beyond request/response aggregation.
- Serving **public APIs**; this BFF is only for portal front ends.
- Managing identity, authentication, or authorization policies (delegated to platform auth and gateway).

## 3. Architecture and Boundaries

### 3.1 High-Level Architecture

- **Service type**: Stateless BFF service (Spring Boot or similar) deployed close to the portal front ends.
- **Integration style**:
  - Synchronous REST/gRPC calls to underlying `hospital-*` services.
  - Occasional subscription to events (via a lightweight internal cache or notification gateway) where near real-time updates are needed.
- **Data storage**:
  - Minimal or no local business data persistence.
  - Optional caches:
    - Short-lived in-memory or distributed cache for summaries and reference data (e.g., static lists, basic profile info).

### 3.2 Bounded Contexts (View Domains)

The BFF is logically split into view-centric modules:

- **Patient Portal Context**
  - Patient dashboard, bills, appointments, orders, results, cards, and canteen usage.
- **Doctor Portal Context**
  - Doctor schedule, patient lists, tasks, orders, and clinical summaries.
- **Corporate Portal Context**
  - Corporate contracts, utilization, billing, discounts overview, and card statistics.

Each context is implemented as a separate package/module within the BFF with its own controllers, DTOs, and orchestrators.

## 4. Key Aggregation Flows

### 4.1 Patient Portal – Home Dashboard

1. Front end calls:
   - `GET /portal/patient/me/dashboard`
2. BFF:
   - Resolves authenticated patient identity (via token / gateway context).
   - Calls:
     - `hospital-service` for patient summary, active visits, key diagnoses/allergies.
     - `hospital-scheduling-service` for upcoming appointments/admissions.
     - `hospital-billing-service` for outstanding invoices and recent payments.
     - `hospital-corporate-and-discount-service` for coverage summary (if applicable).
     - `hospital-card-management-service` for current card balance/limits.
     - `hospital-canteen-service` for recent canteen orders (if relevant).
   - Composes a single `PatientDashboardView` with:
     - Profile, visits, appointments, orders/results links, financial summary, card info.

### 4.2 Doctor Portal – Today’s Worklist

1. Front end calls:
   - `GET /portal/doctor/me/dashboard?date=YYYY-MM-DD`
2. BFF:
   - Resolves doctor identity from auth context.
   - Calls:
     - `hospital-scheduling-service` for doctor’s appointments and IP rounds.
     - `hospital-service` for patient summaries for scheduled visits.
     - `hospital-clinical-orders-service` for pending orders linked to doctor’s patients.
     - `hospital-billing-service` for quick view of billing status per visit (optional).
   - Returns a `DoctorDashboardView`:
     - For each scheduled encounter: patient snapshot, reason, location, flags (allergies, outstanding tasks).

### 4.3 Corporate Portal – Utilization and Billing Overview

1. Front end calls:
   - `GET /portal/corporate/me/overview?from=&to=`
2. BFF:
   - Resolves corporate contract/identity.
   - Calls:
     - `hospital-corporate-and-discount-service` for contract metadata and coverage rules.
     - `hospital-billing-service` for invoices, payments, and outstanding balances.
     - Aggregated usage statistics (visits, procedures, canteen, pharmacy) via billing data and, where needed, `hospital-service`.
   - Builds `CorporateOverviewView`:
     - KPI tiles, charts, invoice summaries, and high-level utilization trends.

## 5. API Design (Draft)

> Exact routes will be refined with front-end teams; below is a draft organized by portal context.

### 5.1 Patient Portal APIs

- `GET /portal/patient/me/dashboard`
  - Main dashboard aggregation.
- `GET /portal/patient/me/visits`
  - Paginated list of visits with summary info.
- `GET /portal/patient/me/visits/{visitId}`
  - Visit detail view: diagnoses, notes summary, orders, results links.
- `GET /portal/patient/me/appointments`
  - Upcoming and past appointments (via `hospital-scheduling-service`).
- `GET /portal/patient/me/billing`
  - Invoice list, balances, and payments view.
- `GET /portal/patient/me/cards`
  - Card balances and recent transactions (with `hospital-card-management-service` and canteen/pharmacy).
- `GET /portal/patient/me/orders`
  - List of lab/radiology/procedure orders and result availability (via `hospital-clinical-orders-service`).

### 5.2 Doctor Portal APIs

- `GET /portal/doctor/me/dashboard`
  - Today’s schedule and tasks.
- `GET /portal/doctor/me/appointments?from=&to=`
  - Appointment list with patient snapshots.
- `GET /portal/doctor/me/patients/{patientId}/summary`
  - Composed clinical and administrative summary:
    - From `hospital-service`, `hospital-clinical-orders-service`, billing summary, etc.
- `GET /portal/doctor/me/orders/pending`
  - Pending orders requiring doctor’s review/authorisation (if applicable).

For prescribing workflows:

- `GET /portal/doctor/me/prescriptions/{patientId}`
  - List prescriptions for a patient (proxy to `hospital-service`).
- `GET /portal/doctor/me/drugs/search?query=&page=&size=`
  - BFF proxy to `hospital-pharmacy-service` `GET /api/hospital-pharmacy/drugs/search` for doctor-facing autocomplete:
    - Used by prescription UI to suggest drugs as the doctor types.
    - Returns generic/brand name, strength, form, route, and optional default dose/frequency/duration so the UI can pre-fill the prescription form while still allowing overrides.

### 5.3 Corporate Portal APIs

- `GET /portal/corporate/me/overview`
  - High-level KPIs, balances, and alerts.
- `GET /portal/corporate/me/invoices`
  - Corporate invoices list.
- `GET /portal/corporate/me/invoices/{invoiceId}`
  - Invoice details and payment history.
- `GET /portal/corporate/me/utilization?from=&to=`
  - Aggregated usage by department/service type.

## 6. Security, Access Control, and Privacy

- **Authentication**
  - Delegated to centralized identity platform; BFF trusts tokens and user context forwarded by API Gateway.
- **Authorization**
  - Enforces portal-specific access:
    - Patient: can only access own data.
    - Doctor: limited to patients and visits they are associated with (and allowed by policy).
    - Corporate: limited to their contract(s), members, and invoices.
- **Privacy and Data Minimization**
  - BFF limits returned fields to what is needed for UI.
  - Avoids exposing internal identifiers or sensitive internal-only fields where not required.

## 7. Non-Functional Requirements

- **Performance**
  - Target p95 latency for main dashboard endpoints \(< 500 ms\) under normal load.
  - Use parallel calls and caching where safe to reduce end-to-end time.
- **Availability**
  - Target 99.9%+ availability during portal operating hours.
- **Resilience**
  - Graceful degradation:
    - If one downstream service is temporarily unavailable, return partial data with clear flags instead of full failure where possible.
- **Observability**
  - Per-portal and per-endpoint metrics (latency, error rates).
  - Distributed tracing across BFF and downstream services via correlation IDs.

## 8. Implementation Approach

### 8.1 Technology and Patterns

- Spring Boot (or equivalent) service with:
  - Web layer for REST controllers.
  - Orchestrator/services layer for aggregation logic.
  - Client layer (REST/gRPC clients) for each downstream `hospital-*` service.
- Use **circuit breakers**, **bulkheads**, and **timeouts** for downstream calls.
- Introduce **DTOs/view models** that are decoupled from downstream service DTOs.

### 8.2 Phased Delivery Plan

- **Phase 1 – Patient Portal MVP**
  - Implement patient dashboard, visits list, appointments list, and basic billing summary.
  - Integrate with `hospital-service`, `hospital-scheduling-service`, and `hospital-billing-service`.
- **Phase 2 – Doctor Portal MVP**
  - Implement doctor dashboard and schedule.
  - Add patient summary aggregation for doctor UI.
- **Phase 3 – Corporate Portal MVP**
  - Implement corporate overview, invoices list, and utilization summary.
  - Integrate deeply with `hospital-corporate-and-discount-service` and `hospital-billing-service`.
- **Phase 4 – Optimization and Hardening**
  - Add caching and performance optimizations.
  - Implement graceful degradation patterns and enhanced observability.

## 9. Risks and Mitigations

- **Risk**: Overgrowth of logic in BFF (becoming a “god service”).
  - **Mitigation**: Keep strict rule: no core business rules; push domain logic back into underlying services.
- **Risk**: Tight coupling to downstream service contracts.
  - **Mitigation**: Use dedicated client interfaces and mapping layers; manage version compatibility explicitly.
- **Risk**: Latency due to multiple downstream calls.
  - **Mitigation**: Use parallelism, caching, and pre-aggregated endpoints where necessary.

