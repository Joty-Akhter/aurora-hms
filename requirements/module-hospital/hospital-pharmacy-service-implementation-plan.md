# `hospital-pharmacy-service` – Implementation Plan

## 1. Overview and Objectives

`hospital-pharmacy-service` is the dedicated service for managing pharmacy-related operations within the Hospital Module. It owns:

- **Drug catalog and formulary rules.**
- **Pharmacy stock at counters**, including issues, sales/dispensing, and returns.
- **Fulfillment of prescriptions** originating from `hospital-service`.

**Primary objectives:**

- Provide a reliable, auditable backbone for all medication-related operations across OPD, IPD, and ED.
- Centralize drug master and formulary rules to ensure safe and consistent prescribing and dispensing.
- Maintain accurate pharmacy stock and movement records to support patient safety, billing, and inventory reconciliation.
- Integrate cleanly with `hospital-service` (prescriptions), `hospital-billing-service`, inventory services, and platform accounting.

## 1.1 Implementation Status

- **Phase 0 – Service Skeleton and Foundations**: **Implemented**
  - New microservice module `easyops-erp/services/hospital-pharmacy-service` created (Spring Boot 3, Java 21).
  - Main application class `com.easyops.hospitalpharmacy.HospitalPharmacyServiceApplication` with Eureka discovery and JPA auditing enabled.
  - `application.yml` configured with its own `hospital_pharmacy` JPA default schema, Prometheus metrics, and dev profile (database schema managed centrally by `hospital-service` Liquibase).
  - API Gateway route added for `hospital-pharmacy-service` (`/api/hospital-pharmacy/**` → `lb://hospital-pharmacy-service`).
- **Phase 1 – Drug Catalog and Basic Pharmacy Setup**: **Implemented**
  - Hospital pharmacy schema (`hospital_pharmacy`) and tables (`Manufacturer`, `Drug`, `FormularyRule`, `PharmacyLocation`) created and versioned via `hospital-service` Liquibase (`008-hospital-pharmacy-schema.sql`).
  - JPA entities, repositories, services, and REST controllers for:
    - Manufacturers: `POST /api/hospital-pharmacy/manufacturers`, `GET /manufacturers/{id}`, `GET /manufacturers`, `PATCH /manufacturers/{id}`.
    - Drugs: `POST /api/hospital-pharmacy/drugs`, `GET /drugs/{id}`, `GET /drugs`, `PATCH /drugs/{id}`, `POST /drugs/{id}/formulary-rules`, `GET /drugs/{id}/formulary-rules`.
    - Pharmacy locations: `POST /api/hospital-pharmacy/pharmacies`, `GET /pharmacies/{id}`, `GET /pharmacies`, `PATCH /pharmacies/{id}`.
  - These APIs provide the centralized drug master and basic pharmacy location configuration required by prescribing UI and admin tools.
- **Phase 2 – Stock Management at Pharmacy Locations**: **Implemented**
  - Hospital pharmacy stock tables (`PharmacyStock`, `StockMovement`) created and versioned via `hospital-service` Liquibase (`009-hospital-pharmacy-stock-schema.sql`).
  - JPA entities, repositories, services, and REST controller for:
    - On-hand stock and movements: `PharmacyStock`, `StockMovement`, `PharmacyStockRepository`, `StockMovementRepository`, `PharmacyStockService`, `PharmacyStockController`.
    - APIs aligned with the plan:
      - `GET /api/hospital-pharmacy/pharmacies/{pharmacyId}/stock` – view on-hand stock.
      - `POST /api/hospital-pharmacy/pharmacies/{pharmacyId}/stock/receipts` – record receipts into a pharmacy location.
      - `POST /api/hospital-pharmacy/pharmacies/{pharmacyId}/stock/adjustments` – manual stock adjustments with reasons.
      - `POST /api/hospital-pharmacy/pharmacies/{pharmacyId}/stock/transfers` – transfer stock between locations, enforcing non-negative balances and recording movements.
  - These implementations provide a real-time view of on-hand stock per pharmacy and an auditable trail of receipts, transfers, and adjustments.
- **Phase 3 – Prescription Fulfillment and Billing Hooks**: **Implemented (core dispensing domain and APIs)** 
  - Hospital pharmacy dispense tables (`DispenseOrder`, `DispenseLine`) created and versioned via `hospital-service` Liquibase (`010-hospital-pharmacy-dispense-schema.sql`).
  - JPA entities, repositories, services, and REST controller for:
    - Orders and lines: `DispenseOrder`, `DispenseLine`, `DispenseOrderRepository`, `DispenseLineRepository`, `DispenseOrderService`, `DispenseOrderController`.
    - APIs aligned with the plan:
      - `POST /api/hospital-pharmacy/dispense-orders` – create a dispense order (from prescription, walk-in, or department issue).
      - `GET /api/hospital-pharmacy/dispense-orders/{id}` – retrieve a dispense order with lines.
      - `GET /api/hospital-pharmacy/dispense-orders` – search by patient, visit, pharmacy location, and status.
      - `POST /api/hospital-pharmacy/dispense-orders/{id}/lines` – record dispensing for prescription lines (supports partial quantities and batch selection while enforcing non-negative stock).
      - `POST /api/hospital-pharmacy/dispense-orders/{id}/returns` – record returns from patients and update stock/movements.
      - `PATCH /api/hospital-pharmacy/dispense-orders/{id}` – update order status (e.g., cancel, complete).
  - These implementations provide end-to-end dispensing and returns workflows, with stock updates and movement audit; event publication and deep integration with `hospital-service` and `hospital-billing-service` can now be layered on top following the described contracts.
- **Phase 4 – Optimization, Safety Rules, and Reporting**: **Implemented (initial safety controls and reports)**
  - **Safety rules**:
    - Block receipt of expired stock and dispensing of expired batches by validating `expiry_date` in `PharmacyStockService.receiveStock` and `DispenseOrderService.addDispenseLines`.
    - Continue to enforce non-negative stock on all mutations (receipts, adjustments, transfers, dispensing, and returns).
  - **Enhanced reporting** (read-only APIs):
    - Near-expiry stock report:
      - `GET /api/hospital-pharmacy/reports/near-expiry?pharmacyId=&days=` – lists stock items per drug/batch whose expiry falls within the next _N_ days (default 30), based on `PharmacyStock`.
    - Consumption report:
      - `GET /api/hospital-pharmacy/reports/consumption?pharmacyId=&from=&to=` – aggregates issued quantities per drug (using `StockMovement` of type `issue_to_patient`) for the given pharmacy and date range.
  - These additions provide practical safety controls and operational visibility for near-expiry items and drug consumption, and can be iteratively extended with more advanced formulary rules and performance optimizations as load grows.

### 1.1.1 Pharmacy gaps — Phase P1 foundation ([`pharmacy-gaps-implementation-plan.md`](pharmacy-gaps-implementation-plan.md))

**Implemented** (Liquibase `053-pharmacy-dispense-phase1-gaps.sql` and `054-dispense-line-override-reason-length.sql` in `hospital-service`; logic in `hospital-pharmacy-service`):

- **`GET /api/hospital-pharmacy/dispense-orders/{id}/billable-items`** — read-only billable line DTOs for future billing integration (quantities &gt; 0 only).
- **`Idempotency-Key`** optional header on **`POST …/dispense-orders/{id}/lines`** and **`POST …/returns`** — persisted replay (`hospital_pharmacy.dispense_idempotency`); PostgreSQL advisory lock + duplicate-key fallback.
- **Dispense line schema** — audit/override columns on `dispense_lines`; extended `status` values (e.g. `FILLED_WITH_STOCK_OVERRIDE`, `PARTIALLY_DISPENSED` with `remaining_quantity`).
- **Policy flags** (`hospital.pharmacy.dispense` in `application.yml`): `allow-negative-stock`, `allow-issue-without-batch`, `require-supervisor-for-override`, optional **idempotency retention** + daily cleanup. With **`allow-negative-stock: true`** and **`stockOverrideReason`** on the line request, dispensing may proceed when no stock row exists or ledger would go negative; optional **blank batch** when **`allow-issue-without-batch: true`** (still requires override). Free-text reasons persist to `override_reason_code` (**`054`** widens the column to 2000 chars to match validation). Defaults preserve previous strict batch + non-negative behavior.
- **RBAC on override** — If any line carries a non-blank **`stockOverrideReason`**, the controller calls **`requireStockOverrideOrHospitalManage`** (resource `hospital.pharmacy` / action `stock_override`, or `hospital` / `manage`) after **`requirePharmacyDispenseMutate`**. Covered by **`DispenseOrderControllerRbacTest`** (403 vs success).

### 1.1.2 Pharmacy gaps — Phase P2 ([`pharmacy-gaps-implementation-plan.md`](pharmacy-gaps-implementation-plan.md) §4)

**Implemented** (see gaps doc for Liquibase ids `055`–`058`, config keys, and event names):

- **Money (WS-A):** Optional charge posting to **`hospital-billing-service`** and return credits; **`HOSPITAL_PHARMACY_CHARGE_POST`** on billing; Kafka sale/cancel/return line events.
- **Clinical sync (WS-B):** **`POST /api/integrations/pharmacy/in-house-dispense-fill`** on **`hospital-service`**; **`fill_status_source`** on transmissions; in-house fill skipped when the dispense order has no **`prescription_id`**.
- **Lines (WS-C):** **`POST …/dispense-orders/{id}/lines/unfulfilled`**; **`DispenseLineStatusRules`** + **`DispenseLineStatusRulesTest`**.
- **Override RBAC/UI (WS-D):** Seeds **`HOSPITAL_PHARMACY_STOCK_OVERRIDE`** / **`HOSPITAL_PHARMACY_CHARGE_POST`**; **`PharmacyDispense`** gates override field and exposes “not filled” flow.
- **Events (WS-K):** **`pharmacy.stock.changed`**, **`dispense-order.completed`**, extended **`pharmacy.sale.completed`**; prescription-created dedupe; optional **`default-pharmacy-location-id`**; **`spring.kafka.bootstrap-servers`** in **`application.yml`**.

### 1.1.3 Pharmacy gaps — Phase P3 ([`pharmacy-gaps-implementation-plan.md`](pharmacy-gaps-implementation-plan.md) §4)

**Implemented** (Liquibase `059`–`060` in `hospital-service`; `hospital-pharmacy-service`):

- **Regional (WS-E):** `paper_prescription_ref`, `prescription_image_attachment_id`, `external_validation_status` on `dispense_orders`; **`hospital.pharmacy.regional`** (`require-ehr-prescription-for-rx-skus`, `block-on-soft-validation-failure`); **`PATCH /dispense-orders/{id}/regional`** (optional **`clearPrescriptionImageAttachment`**).
- **Receipts (WS-F):** **`GET /dispense-orders/{id}/receipt.pdf`** (OpenPDF); header lines include paper ref, image attachment id, validation status; **`pharmacy_receipt_reprint_audit`** with duplicate detection.
- **Formulary (WS-G):** **`formulary_override_reason`** on lines; **`substitutedDrugId`** / **`formularyOverrideReason`** on requests; **`GET /drugs/{id}/formulary-alternatives`**.
- **Near-expiry (WS-J):** **`pharmacy_near_expiry_rules`** + **`NearExpiryEvaluationService`** on dispense from stock rows.
- **Stock override report (WS-L1):** **`GET /reports/stock-overrides`** (alias **`/stock-override-lines`**); response includes **pharmacy** id/name per line.

## 1.2 Phase-wise Implementation Comparison (DB, Backend, Frontend)

The bullets below summarize the implementation status per phase across database schema, backend services, and frontend UIs. Percentages are approximate but give a clear sense of completeness.

- **Phase 0 – Service Skeleton and Foundations** – **DB 100% · Backend 100% · Frontend 0%**
  - **DB**:
    - `hospital-service` Liquibase configured; shared Postgres setup ready for hospital modules.
  - **Backend**:
    - `hospital-pharmacy-service` Spring Boot microservice created with Eureka, JPA auditing, metrics, and API Gateway route.
  - **Frontend**:
    - No pharmacy-specific UI in this phase; only global shell (auth, dashboard, layout).

- **Phase 1 – Drug Catalog and Basic Pharmacy Setup** – **DB 100% · Backend 100% · Frontend 100%**
  - **DB**:
    - `hospital_pharmacy` schema and core tables:
      - `manufacturers`, `drugs`, `formulary_rules`, `pharmacy_locations`.
    - Managed via `hospital-service` Liquibase change set `008-hospital-pharmacy-schema.sql`.
  - **Backend**:
    - Entities, repositories, services, controllers:
      - `Manufacturer`, `Drug`, `FormularyRule`, `PharmacyLocation`.
      - REST APIs under `/api/hospital-pharmacy/manufacturers`, `/drugs`, `/pharmacies`.
  - **Frontend**:
    - `PharmacyCatalogPage` (`/hospital/pharmacy/catalog`) – manage hospital drug master.
    - `PharmacyLocationsPage` (`/hospital/pharmacy/locations`) – manage OPD/IPD/store/ward pharmacy locations.

- **Phase 2 – Stock Management at Pharmacy Locations** – **DB 100% · Backend 100% · Frontend 100%**
  - **DB**:
    - Stock and movement tables:
      - `pharmacy_stock`, `stock_movements`.
    - Managed via `hospital-service` Liquibase change set `009-hospital-pharmacy-stock-schema.sql`.
  - **Backend**:
    - Entities and services:
      - `PharmacyStock`, `StockMovement`, `PharmacyStockService`, `PharmacyStockController`.
      - APIs for receipts, adjustments, transfers, and stock listing per pharmacy.
  - **Frontend**:
    - `PharmacyStockPage` (`/hospital/pharmacy/stock`) – view on-hand stock per pharmacy and trigger:
      - Receipts (`/stock/receipts`).
      - Adjustments (`/stock/adjustments`).
      - Transfers (`/stock/transfers`).

- **Phase 3 – Prescription Fulfillment and Billing Hooks** – **DB 100% · Backend 100% · Frontend 100%**
  - **DB**:
    - Dispensing tables:
      - `dispense_orders`, `dispense_lines`.
    - Managed via `hospital-service` Liquibase change set `010-hospital-pharmacy-dispense-schema.sql`.
  - **Backend**:
    - Entities and services:
      - `DispenseOrder`, `DispenseLine`, `DispenseOrderService`, `DispenseOrderController`.
      - APIs for creating/searching orders, recording dispensing and returns, and updating order status.
  - **Frontend**:
    - `PharmacyDispensePage` (`/hospital/pharmacy/dispense`) – UI for:
      - Creating dispense orders (prescription, walk-in, department issue).
      - Dispensing drugs and recording patient returns.
      - Managing order statuses for each order.

- **Phase 4 – Optimization, Safety Rules, and Reporting** – **DB 100% · Backend 100% · Frontend 100% (initial scope)**
  - **DB**:
    - Reuses existing `pharmacy_stock`, `stock_movements`, `dispense_orders`, and `dispense_lines` tables; no extra schema required for basic safety/reporting.
  - **Backend**:
    - Safety logic:
      - Expiry checks on stock receipt and dispensing.
      - Continued enforcement of non-negative stock.
    - Reporting services and controller:
      - `PharmacyReportService`, `PharmacyReportController` providing:
        - `GET /api/hospital-pharmacy/reports/near-expiry`.
        - `GET /api/hospital-pharmacy/reports/consumption`.
  - **Frontend**:
    - `PharmacyReportsPage` (`/hospital/pharmacy/reports`) – hospital UI for:
      - **Near-expiry stock**: parameterized by pharmacy and days to expiry window.
      - **Consumption report**: parameterized by pharmacy and date range, showing total issued quantity per drug.
    - These complete the initial reporting and safety visibility defined for Phase 4; further refinements (e.g., charts, exports) can be added incrementally.

## 2. Scope and Non-Scope

### 2.1 In-Scope

- **Drug catalog and formulary:**
  - Drug master (generic and branded), dosage forms, strengths, routes, and pack sizes.
  - Mappings to therapeutic classes and other classification systems.
  - Formulary rules (allowed/restricted, substitution rules, corporate-specific or ward-specific limitations where necessary).
- **Pharmacy locations and counters:**
  - OPD pharmacy counters, IPD stores, and ward-level sub-stores (if required).
  - Configuration of counters and operational attributes (e.g., 24x7 vs scheduled).
- **Stock management at pharmacy level:**
  - Receipts from central inventory/warehouse (integrating with inventory service if present).
  - Stock adjustments, expiries, damage, and transfers between counters.
  - Real-time available stock per pharmacy location.
- **Prescription fulfillment:**
  - Receiving prescriptions from `hospital-service`.
  - Dispensing against prescription lines, partial vs full issues.
  - Returns and cancellation of dispensed items (with full audit).
- **Billing integration:**
  - Generate charge lines for dispensed items and returns.
  - Provide data required by `hospital-billing-service` for invoicing.

### 2.2 Out-of-Scope (Handled by Other Services)

- **Core clinical prescribing logic** (diagnoses, allergies, clinical context): owned by `hospital-service`.
- **Global inventory and purchase management** (POs, GRNs, supplier returns, non-retail warehouse flows): `hospital-main-store` / central inventory service (see `hospital-main-store.md`).
- **Billing calculations and invoices**: `hospital-billing-service`.
- **Corporate-specific discount and eligibility rules**: `hospital-corporate-and-discount-service`.
- **Card lifecycle and balances**: `hospital-card-management-service`.

## 3. Architecture and Boundaries

### 3.1 High-Level Architecture

- **Service type**: Stateless API service with a dedicated relational database.
- **Database**: Relational (e.g., PostgreSQL) for strong transactional behavior and integrity across stock movements and dispensing.
- **Integrations**:
  - REST/gRPC APIs for synchronous operations from BFFs, hospital UI, and other services.
  - Event-driven messaging for stock updates and dispensing events to billing, inventory, and analytics.

### 3.2 Bounded Context Responsibilities

- **Drug Catalog Context:**
  - Owns complete definition of drugs and formulary rules.
- **Pharmacy Location & Stock Context:**
  - Owns pharmacy locations/counters and their on-hand stock.
- **Dispensing Context:**
  - Owns the lifecycle of dispensing operations tied to prescriptions and visits.

## 4. Data Model (High-Level)

### 4.1 Key Entities

- `Manufacturer`:
  - `id`, `name`.
  - Optional fields: `short_code`, `country`, `contact_info`, `is_active`.
- `Drug`:
  - `id`, `generic_name`, `brand_name`, `strength`, `form`, `route`.
  - `pack_size`, `unit_of_measure`.
  - `therapeutic_class_id`, `is_active`.
  - `controlled_drug_flag` (for scheduled/controlled medicine handling as per Pharmacy Module requirements).
  - `batch_required`, `expiry_required` (configuration flags; default `true` to match Pharmacy Module defaults).
  - `manufacturer_id` (foreign key to `Manufacturer`; typically mandatory and validated against an active manufacturer, aligning with `pharmacy.md`).
- `FormularyRule`:
  - `id`, `drug_id`.
  - `restricted` flag, `restriction_reason`.
  - Optional `ward_id`, `department_id`, `corporate_contract_id` scope.
  - Substitution preferences (preferred alternative drug IDs).
- `PharmacyLocation`:
  - `id`, `name`, `type` (OPD, IPD, store, ward_store).
  - `is_24x7`, operational hours.
- `PharmacyStock`:
  - `id`, `pharmacy_location_id`, `drug_id`.
  - `batch_number`, `expiry_date`.
  - `quantity_on_hand`.
- `StockMovement`:
  - `id`, `pharmacy_location_id`, `drug_id`.
  - `movement_type` (receipt, issue_to_patient, return_from_patient, transfer_in, transfer_out, adjustment, expiry, damage).
  - `quantity`, `batch_number`, `movement_time`.
  - References to originating entities (purchase order, prescription line, inventory transfer, etc.).
- `DispenseOrder`:
  - `id`, `prescription_id` (from `hospital-service`, nullable for walk-in or department issues), `visit_id`, `patient_id`.
  - `pharmacy_location_id`, `status` (pending, in_progress, completed, cancelled).
  - `context_type` (patient_prescription, walk_in, department_issue).
  - Optional `department_id` for department/ward stock issues.
  - `created_at`, `completed_at`.
- `DispenseLine`:
  - `id`, `dispense_order_id`, `prescription_line_id`.
  - `drug_id`, `batch_number`.
  - `quantity_prescribed`, `quantity_dispensed`, `quantity_returned`.
  - `status` (pending, dispensed, partially_dispensed, returned, cancelled).

### 4.2 Referential Strategy

- Reference external entities by ID only:
  - `patient_id`, `visit_id`, `prescription_id`, `prescription_line_id`, `doctor_id`, `corporate_contract_id`, `manufacturer_id`.
- Do not embed external business logic (e.g., discount calculation, eligibility); expose dispense and stock data to the relevant services that own those rules.

## 5. APIs

### 5.1 Manufacturer and Drug Catalog APIs

- **Manufacturer APIs** (admin-only):
  - `POST /manufacturers` – create a manufacturer master record.
  - `GET /manufacturers/{id}` – retrieve manufacturer details.
  - `GET /manufacturers` – search/list manufacturers with filters (name, active).
  - `PATCH /manufacturers/{id}` – update manufacturer metadata (name, status, optional fields).
- **Drug APIs**:
  - `POST /drugs` – create a drug master record (with required `manufacturer_id` unless configuration explicitly allows exceptions).
  - `GET /drugs/{id}` – retrieve drug details (including manufacturer reference and, where appropriate, manufacturer name).
  - `GET /drugs` – search/list drugs with filters (name, class, active, manufacturer, productGroupId, department).
  - `GET /drugs/search` – lightweight autocomplete endpoint optimized for counter-speed typeahead (returns id, name, strength, form, unit, rack_no, mrp, sale_price); backed by the same query as `GET /drugs` but with a smaller response projection and `q=` parameter.
  - `PATCH /drugs/{id}` – update selected fields (e.g., activation, descriptions, manufacturer reference, rack_no, pricing), respecting mandatory manufacturer constraints.
  - `POST /drugs/{id}/formulary-rules` – create/update formulary rules for a drug.
  - `GET /drugs/{id}/formulary-rules` – get formulary rules for a drug.
  - `GET /drugs/{id}/formulary-alternatives` – list formulary-approved alternative drugs for substitution at dispense.

### 5.2 Pharmacy Location and Stock APIs

- `POST /pharmacies` – create pharmacy location.
- `GET /pharmacies` – list pharmacy locations.
- `GET /pharmacies/{id}` – get details of a pharmacy location.
- `PATCH /pharmacies/{id}` – update pharmacy location metadata (name, type, hours, active status).
- `GET /pharmacies/{id}/stock` – list on-hand stock by drug/batch (includes rack_no from drug master).
- `POST /pharmacies/{id}/stock/receipts` – receive stock into a pharmacy location (from inventory).
- `POST /pharmacies/{id}/stock/adjustments` – manual stock adjustment with reason (approval-pending if above threshold per GAP-4).
- `POST /pharmacies/{id}/stock/transfers` – direct manager-initiated transfer between locations (see GAP-2 for requisition-based transfers).
- `GET /pharmacies/{id}/stock/movements` – query stock movement history with filters: `drugId`, `batchNumber`, `type`, `from`, `to`; paginated (see GAP-6, ENH-3).

### 5.3 Dispensing and Returns APIs

- `POST /dispense-orders` – create a dispense order from a prescription (input: `prescription_id`, `pharmacy_location_id`).
- `GET /dispense-orders/{id}` – retrieve a dispense order and lines.
- `GET /dispense-orders` – search/filter dispense orders (by patient, visit, status, pharmacy).
- `POST /dispense-orders/{id}/lines` – record dispensing for prescription lines (supports partial quantities, batch selection).
- `POST /dispense-orders/{id}/returns` – record returns (patient returning unused medicine).
- `PATCH /dispense-orders/{id}` – update status (e.g., cancel).

### 5.4 Billing Integration APIs

- `GET /dispense-orders/{id}/billable-items` – returns a list of billable items with pricing references for `hospital-billing-service`.
- Optional push model:
  - Webhook/event-based integration where billable lines are pushed as events to billing.
  - In practice, `hospital-pharmacy-service` will either:
    - Call `hospital-billing-service`'s `POST /charges` API with billable line details derived from `DispenseOrder` / `DispenseLine`, or
    - Publish them via the `pharmacy.sale.completed` / `pharmacy.sale.cancelled` events, which `hospital-billing-service` consumes to create or reverse `ChargeLine` entries.

### 5.5 Frontend / BFF Integration

- **Consumer UIs**:
  - Pharmacy workstations (OPD/IPD counters) for dispensing, returns, stock lookup, and adjustments.
  - Central pharmacy/manager UI for catalog, formulary, and location configuration.
  - Ward/department UIs for viewing ward stock and requesting issues where applicable.
- **Integration pattern**:
  - Frontend apps should primarily call `hospital-pharmacy-service` via a `hospital-portal-bff-service` layer, which:
    - Orchestrates calls to `hospital-service` (for prescriptions, patient/visit context) and `hospital-billing-service` (for bill previews, payment status).
    - Adapts API contracts to specific UI flows (e.g., combined “Dispense & Collect Payment” screen).
- **UX and performance considerations**:
  - Provide search-optimized APIs (`GET /drugs`, `GET /drugs/search`, `GET /pharmacies/{id}/stock`, `GET /dispense-orders`) tuned for autocomplete, batch selection, and counter workflows.
  - Support pagination and server-side filtering/sorting to keep UI responsive under high-volume dispensing.
  - Ensure all write operations surface clear, user-friendly error messages (e.g., stock-out, formulary restriction, controlled-drug rules) suitable for direct display in pharmacy UIs.

## 6. Events and Integrations

### 6.1 Outgoing Events

Emit domain events for:

- `pharmacy.stock.changed` – after receipts, transfers, adjustments, dispensing, returns.
- `dispense-order.created`, `dispense-order.completed`, `dispense-order.cancelled`.
- `dispense-line.dispensed`, `dispense-line.returned`.

For billing integration (see `hospital-billing-service`):

- Emit higher-level financial events aligned with billing expectations:
  - `pharmacy.sale.completed` – emitted when a dispense order (or equivalent sale) is fully posted and corresponding billable items are ready.
  - `pharmacy.sale.cancelled` – emitted when a previously completed sale is cancelled or fully reversed.
  - These events carry references to `DispenseOrder` / `DispenseLine` IDs so that `hospital-billing-service` can create or adjust `ChargeLine` entries (via `/charges` or event consumers).

Consumers:

- Inventory/warehouse service – for reconciliation between central stock and pharmacy stock.
- `hospital-billing-service` – to generate or update charge entries.
- Analytics/reporting – for utilization, wastage, and expiry reporting.

### 6.2 Incoming Events and Calls

- From inventory/warehouse:
  - Purchase orders and GRNs that drive `pharmacy.stock.received`.
- From `hospital-service`:
  - Prescriptions via API or event to instantiate `DispenseOrder`.
- From `hospital-corporate-and-discount-service`:
  - Optional consultation for coverage/discount eligibility during billing \(via billing service\).

## 7. Non-Functional Requirements

- **Performance**:
  - 95th percentile response time for dispensing APIs \(< 500 ms\) under normal load.
  - Stock queries optimized with proper indexing and caching where appropriate.
- **Reliability & Consistency**:
  - Strong consistency on stock mutations per pharmacy location.
  - Idempotency for stock movement and dispensing APIs to avoid double-issuing.
- **Security**:
  - Fine-grained roles (pharmacist, pharmacy manager, auditor) for catalog changes, dispensing, and adjustments.
  - Full audit logging for stock movements and dispensing actions.
- **Compliance**:
  - Retain logs and records according to regulatory requirements.

## 8. Phased Implementation Plan

### Phase 0 – Service Skeleton and Foundations

- Implement:
  - Initial `hospital-pharmacy-service` Spring Boot project/microservice skeleton (aligned with common hospital-module templates).
  - Baseline modules for web, persistence, and messaging (REST controllers, JPA/repository layer, event publishing/consumption stubs).
  - Empty schemas for core tables (`Manufacturer`, `Drug`, `PharmacyLocation`, `PharmacyStock`, `StockMovement`, `DispenseOrder`, `DispenseLine`) with **Liquibase** migration tooling set up, consistent with `hospital-service`.
  - Basic error handling, validation, and standard API response envelope consistent with other `hospital-*` services.
- Integrate:
  - Service registration and routing via API Gateway, including auth/RBAC hooks and correlation IDs.
  - Observability foundations (logging, metrics, tracing) wired into platform standards.
  - CI/CD pipeline for build, unit tests, static analysis, and deployment to lower environments.
- Deliverables:
  - A deployable, observable, and testable `hospital-pharmacy-service` skeleton with empty or stubbed endpoints.
  - Ready baseline to incrementally add Domain logic in Phases 1–4 without reworking infrastructure.

### Phase 1 – Drug Catalog and Basic Pharmacy Setup

- Implement:
  - `Drug`, `FormularyRule`, `PharmacyLocation`.
  - CRUD APIs for drug catalog and pharmacy locations.
- Deliverables:
  - Centralized drug master in use by prescribing UI (via read APIs).
  - Configured pharmacy locations but without live stock management.

### Phase 2 – Stock Management at Pharmacy Locations

- Implement:
  - `PharmacyStock` and `StockMovement` entities and APIs.
  - Receipts, transfers, and adjustments.
- Integrate:
  - Initial integration with inventory/warehouse for incoming stock.
- Deliverables:
  - Real-time view of on-hand stock at each pharmacy.
  - Audit trail for stock movements.

### Phase 3 – Prescription Fulfillment and Billing Hooks

- Implement:
  - `DispenseOrder` and `DispenseLine` entities and APIs.
  - End-to-end dispensing and returns workflow.
  - Outgoing events for dispensing and stock changes.
- Integrate:
  - With `hospital-service` for prescription creation and updates.
  - With `hospital-billing-service` for billable line generation.
- Deliverables:
  - Live OPD/IPD dispensing linked to prescriptions and billing.

### Phase 4 – Optimization, Safety Rules, and Reporting

- Add:
  - Advanced formulary and safety checks (e.g., max daily dose per drug, sensible defaults).
  - Enhanced reporting for consumption, near-expiry stock, and wastage.
- Optimize:
  - Indexes and caching strategies for high-load dispensing counters.
- Deliverables:
  - Mature pharmacy operations ready for higher volumes and audit scrutiny.

## 9. Deployment and Migration Strategy

- **Initial deployment**:
  - Start with a single pilot pharmacy location.
  - Parallel-run with legacy pharmacy workflows if needed, gradually migrating high-volume items first.
- **Data migration**:
  - Migrate existing drug masters (if present) via mapping scripts.
  - Optionally migrate opening stock per pharmacy location before go-live.

## 10. Risks and Mitigations

- **Risk**: Stock inconsistencies between central inventory and pharmacy.
  - **Mitigation**: Clear ownership rules and reconciliations; idempotent movement APIs and periodic audits.
- **Risk**: Over-complex formulary rules delaying go-live.
  - **Mitigation**: Start with a simple, global formulary and incrementally add complexity.
- **Risk**: Performance issues at peak dispensing times.
  - **Mitigation**: Pre-emptive indexing, load testing, and horizontal scaling strategy.

---

## 11. Gap & Inconsistency Review (vs `pharmacy.md`)

This section documents inconsistencies found between this implementation plan and the business requirements in [`pharmacy.md`](pharmacy.md), plus gaps and enhancements identified during review. Each item is classified as **Inconsistency** (contradicts or conflicts), **Gap** (in scope per requirements but absent from the plan), or **Enhancement** (improvement beyond current spec).

---

### 11.1 Inconsistencies

#### INC-1 — `PATCH /pharmacies/{id}` missing from API spec (§5.2)

- **Where**: §1.1 Phase 1 status records `PATCH /pharmacies/{id}` as implemented; §5.2 omits it entirely.
- **Fix**: Add `PATCH /pharmacies/{id}` to §5.2 Pharmacy Location and Stock APIs.

#### INC-2 — `GET /drugs/search` referenced in §5.5 but not defined in §5.1

- **Where**: §5.5 (Frontend/BFF integration) says "Provide search-optimized APIs (`GET /drugs`, `GET /drugs/search`, ...)". §5.1 only lists `GET /drugs`.
- **Fix**: Either add `GET /drugs/search` as a distinct endpoint to §5.1, or remove the reference from §5.5 and confirm `GET /drugs` with filter params covers autocomplete needs.

#### INC-3 — Drug entity missing mandatory pricing, classification, and physical-location fields

- **Where**: §4.1 `Drug` data model and the actual `Drug.java` entity both omit fields required by `pharmacy.md §2` and the first-hand SRS (`pharmacy.txt §3.2`).
- **Missing fields**:
  - **Pricing**: `mrp` (unit price / MRP), `default_purchase_price`, `default_selling_price` / `sale_price`.
  - **Stock control**: `min_stock_level` / `reminder_stock` (reorder alert threshold), `max_stock_level`, `reorder_level`.
  - **Classification**: `product_group` (e.g. Antibiotics, Analgesics — required; needed for group-wise stock reports and PO suggestion filtering), `department` (optional department linkage, e.g. OT, ICU, General).
  - **Physical location**: `rack_no` (shelf/rack identifier within a pharmacy — shown in the drug list view per `pharmacy.txt §3.2`).
  - **Tax**: `hsn_code` / `tax_code` (for invoice compliance).
  - **Human-readable code**: `product_code` (optional, must be unique when provided).
- **Impact**: Without these fields, the following are blocked: gross profit report, group-wise/supplier-wise stock reports, PO demand-forecast calculation (uses current stock + average sale vs reminder stock), billing price display at counter, and physical stock-count lookup by rack.
- **Fix**: Add all fields to the `Drug` entity and a new Liquibase change set. Expose on drug create/update APIs. `product_group` should reference a `product_groups` lookup table (see ENH-11). Mark pricing fields nullable for migration; `reminder_stock` defaults to 0.

#### INC-4 — `Manufacturer` entity does not satisfy "Supplier/Company Master" requirements

- **Where**: §4.1 models `Manufacturer` with `name`, `short_code`, `country`, `contact_info`, `is_active`. Both `pharmacy.md §2` and the first-hand SRS (`pharmacy.txt §3.1`) specify a richer "Supplier/Company Master."
- **Missing fields** (from `pharmacy.txt §3.1`):
  - `license_no` — required, alphanumeric, unique. Blocks use in purchases when absent.
  - `vat` — VAT registration number or percentage (tax compliance).
  - `commission` — supplier commission rate or amount (financial terms).
  - `type` — company type enum: `COMPANY` (direct manufacturer) vs `LOCAL_MARKET` (local distributor/importer).
- **Missing business rule**: Inactive companies cannot be used in purchase orders, GRNs, stock receipts, or supplier returns. Currently no enforcement on `POST /pharmacies/{id}/stock/receipts` when the referenced manufacturer is inactive.
- **List view gap**: `pharmacy.txt §3.1` specifies the company list view includes `Type`, `Address`, `Phone` columns — the `GET /manufacturers` response DTO should expose these.
- **Fix**: Add the four missing fields to `Manufacturer` entity and Liquibase migration. Enforce active status check on stock receipt creation. Update `ManufacturerResponse` DTO to include all list-view fields.

#### INC-5 — Near-expiry rule model incomplete against `pharmacy.md §5`

- **Where**: WS-J / §1.1.3 describes a `pharmacy_near_expiry_rules` engine, but `pharmacy.md §5` specifies three action types (`ALERT_ONLY`, `BLOCK`, `ALLOW_WITH_APPROVAL`) and effective dates. The implemented rule table and `NearExpiryEvaluationService` do not document:
  - `ALLOW_WITH_APPROVAL` action with a configurable `approver_role_code`.
  - `discount_required` flag (when a near-expiry sale is allowed, mandate a discount or patient acknowledgement).
  - `effective_from` / `effective_to` for rule versioning.
- **Fix**: Add these columns to `pharmacy_near_expiry_rules` in a new Liquibase change set; extend `NearExpiryEvaluationService` to handle the approval branch.

#### INC-6 — Formulary substitution is modelled as single drug on `dispense_lines` but requirements imply one-to-many alternatives

- **Where**: §4.1 `FormularyRule` says "Substitution preferences (preferred alternative drug IDs)" (plural). `pharmacy.md §4.1.2` requires documenting `substituted_drug_id` and `original_prescribed_drug_id` per line. The `GET /drugs/{id}/formulary-alternatives` API returns a list, but the schema only allows one `substituted_drug_id` per dispense line.
- **This is consistent at the dispense level** (one drug is actually issued), but the `FormularyRule` entity should store a list of alternatives, not a single preference. Clarify that `FormularyRule` holds n alternatives; the dispense line records the single chosen substitute.
- **Fix**: Ensure `FormularyRule` stores a separate `formulary_alternatives` table (drug_id → alternative_drug_id, priority). Document this in §4.1.

---

### 11.2 Gaps (in `pharmacy.md` scope but absent from the plan)

#### GAP-1 — Unit & Conversion Master completely missing (`pharmacy.md §2`)

- `pharmacy.md §2` explicitly requires a "Unit & Conversion Master — Base units (tablet, vial, ml, etc.) and pack-to-unit conversion." This affects dispensing accuracy (issue 3 tablets from a pack of 10) and procurement quantities.
- **No entity, API, or Liquibase migration is planned.** The `Drug` entity has `unit_of_measure` as a free-text string, which is insufficient for conversion logic.
- **Proposed plan**:
  - Add `units` table: `id`, `name`, `abbreviation`, `base_unit_id` (FK, null = base), `conversion_factor`.
  - Replace `Drug.unitOfMeasure` (string) with `Drug.dispensing_unit_id` FK + `Drug.pack_unit_id` FK (pack unit, e.g. strip/bottle) + `Drug.pack_size` (numeric, how many dispensing units per pack).
  - Add admin CRUD APIs; expose unit conversion on stock receipt and dispense line APIs.

#### GAP-2 — Transfer/Requisition approval workflow missing (`pharmacy.md §3.2`, §3.6.1, §3.6.2`)

- `pharmacy.md §3.2` and §3.6 require a **requisition → approval → fulfillment** lifecycle, not an atomic transfer. The current `POST /pharmacies/{id}/stock/transfers` executes immediately with no approval states.
- **Required states**: `DRAFT → SUBMITTED → APPROVED / PARTIALLY_APPROVED / REJECTED → IN_TRANSIT → RECEIVED`.
- **Required data**: requested qty vs approved qty per line; approver id + timestamp; rejection reason (mandatory); linked "transfer document" id for audit.
- **Proposed plan**:
  - Add `stock_requisitions` table and `stock_requisition_lines` table.
  - Add APIs: `POST /stock-requisitions`, `GET /stock-requisitions/{id}`, `POST /stock-requisitions/{id}/submit`, `POST /stock-requisitions/{id}/approve`, `POST /stock-requisitions/{id}/reject`, `POST /stock-requisitions/{id}/receive`.
  - The existing `POST /pharmacies/{id}/stock/transfers` can remain for internal or manager-initiated direct moves; tag requisition-driven transfers with `requisition_id` on `StockMovement`.

#### GAP-3 — Supplier returns from outlet pharmacies not addressed (`pharmacy.md §3.3`)

- §2.2 of this plan classifies "Global inventory and purchase management … supplier returns" as out of scope. However, `pharmacy.md §3.3` explicitly requires outlet pharmacies to create return notes for expired, damaged, or excess stock to suppliers — this is an outlet-level operation, distinct from central-warehouse PO/GRN flows.
- **Proposed plan**: Either:
  - (a) Scope outlet-level supplier return notes into `hospital-pharmacy-service` with a `supplier_return_orders` table (status: `DRAFT → SUBMITTED → APPROVED → DISPATCHED`; items: drug, batch, qty, reason, supplier/manufacturer id); or
  - (b) Explicitly document that outlet supplier returns are handled by the `hospital-main-store` service and define the API/event contract for pharmacy to initiate a return.
  - Require supervisor/manager approval for returns above configurable quantity/value thresholds (as specified in `pharmacy.md §3.3`).

#### GAP-4 — Stock adjustment approval workflow missing (`pharmacy.md §3.4`)

- `pharmacy.md §3.4` says adjustments above a threshold require role-based approval. The current `POST /pharmacies/{id}/stock/adjustments` API executes synchronously without any approval state.
- **Proposed plan**: Add a `min_adjustment_require_approval_qty` / `min_adjustment_require_approval_value` config per pharmacy location. When exceeded, the adjustment records a `PENDING_APPROVAL` state and requires a separate `POST /stock/adjustments/{id}/approve` or `reject` step by a manager role.

#### GAP-5 — Procurement suggestion / PO engine: ownership unclear and detailed logic not captured

- `pharmacy.md §3.6.3` describes a demand-forecasting PO suggestion engine. The first-hand SRS (`pharmacy.txt §5.4`) provides significantly more detail that is not reflected anywhere in the current plan:

  **Detailed calculation (from `pharmacy.txt §5.4`)**:
  - **Input**: `From Date`, `To Date`, `No of Days` (forecast period), `Company` (supplier filter).
  - **Step A — Average Sale**: `Average Sale = Total Sale Quantity ÷ day_count` where `day_count = days in From–To range`.
  - **Step B — Forecast Required Qty**: `Forecast Qty = Average Daily Sale × No of Days`.
  - **Step C — Request Qty**: If `Forecast Qty > Current Stock` → `Request Qty = Forecast Qty − Current Stock` (read-only). Otherwise **exclude the product from the list entirely** (not shown with 0; Seclo example in §5.4.6 shows it is hidden).
  - **Order Qty**: Defaults to Request Qty; **user-editable**. All manual changes must be logged (previous value, new value, user, timestamp) for audit.
  - **"Send to Supplier" action**: On PO approval, a "Send to Supplier" button emails the PO directly to the supplier; sending time and recipient are audit-logged.

  **Workflow**: `Draft → Submitted → (Store/Management Approval: Full / Partial / Reject with reason) → PO Generated → Save PO in Pharmacy Module → Send to Supplier`.

- **Ownership decision required**: If this module lives in `hospital-main-store`, explicitly state so in §2.2 and define the integration contract (API or events) for the pharmacy UI to initiate and track a requisition. If it lives in `hospital-pharmacy-service`, a dedicated phase (P6+) is needed with the entities: `purchase_requisitions`, `purchase_requisition_lines`, `purchase_orders`, `purchase_order_lines`.
- **Proposed plan for §2.2**: Add a note clarifying GRN creation, PO lifecycle, and central warehouse valuation live in `hospital-main-store`; outline the boundary where pharmacy-initiated requisitions hand off to central store procurement.

#### GAP-6 — Stock movement history query API missing

- `StockMovement` records are created but there is no `GET /pharmacies/{id}/stock/movements` or equivalent API. This is needed for:
  - Transfer history report (`pharmacy.md §6`).
  - Auditor "before and after" stock level queries.
  - Supplier-wise and reason-wise movement analysis.
- **Proposed plan**: Add `GET /pharmacies/{id}/stock/movements?drugId=&from=&to=&type=` with pagination to §5.2.

#### GAP-7 — Missing reports from `pharmacy.md §6`

The following reports are required by `pharmacy.md §6` but are not planned in any phase:

| Report | `pharmacy.md §6` reference | Status |
|--------|---------------------------|--------|
| Date-wise / group-wise / supplier-wise stock | §6 Stock Reports | Not planned |
| Transfer history (between stores/pharmacies) | §6 Profitability & Control | Not planned (needs GAP-6 API) |
| Department issue report (ward/department consumption) | §6 Profitability & Control | Partial — consumption report exists but not department-wise |
| Gross profit report (by item, group, supplier, period) | §6 Profitability & Control | Not planned (requires pricing on `Drug` — see INC-3) |
| Discount report (by date, user, type, medicine) | §6 Discount & Return | Not planned |
| Sales return reports with reasons and financial impact | §6 Discount & Return | Not planned |
| Due/credit report with aging buckets (0–30, 31–60, 61–90, >90 days) | §6 Sales & Collection | Not planned (requires billing integration) |
| Stock adjustment report (reason-wise) | §6 Profitability & Control | Not planned |

**Proposed plan**: Add a Phase P5 reporting iteration to `pharmacy-gaps-implementation-plan.md` covering these reports once billing integration (WS-A) and the movement history API (GAP-6) are stable.

#### GAP-8 — Return references to original charge not enforced (`pharmacy.md §4.1.3`)

- `pharmacy.md §4.1.3` says: "Sales returns are always tied back to original invoice/receipt for quantity and amount validation." The `POST /dispense-orders/{id}/returns` API does not accept or enforce a `charge_id` or `invoice_line_id` reference.
- **Proposed plan**: After WS-A (billing integration) lands, extend `DispenseReturnRequest` and `DispenseLine.quantity_returned` tracking to require a billing reference for credited lines. Gate enforcement behind a feature flag initially.

#### GAP-9 — Walk-in/OTC patient detail capture policy not defined (`pharmacy.md §4.1`)

- `pharmacy.md §4.1` says "Basic patient details captured as per configuration (optional or mandatory)" for walk-in sales. The plan and API do not specify which patient fields are required vs optional when `prescription_id` is null and `context_type = walk_in`.
- **Proposed plan**: Add a config flag `hospital.pharmacy.walk-in.require-patient-id` (default false). Document the accepted patient context fields for walk-in orders in the `CreateDispenseOrderRequest` DTO docs.

#### GAP-10 — Formulary rule versioning / effective dates missing (`pharmacy.md §4.1.2`)

- `pharmacy.md §4.1.2` requires auditors to trace "rule set version" used at dispense time. `FormularyRule` has no `effective_from`, `effective_to`, or `rule_set_version` fields.
- **Proposed plan**: Add `effective_from DATE`, `effective_to DATE` (nullable), and a `rule_set_version VARCHAR(50)` to `formulary_rules`. When `FormularyDispenseValidator` evaluates rules, record the `rule_set_version` on `dispense_lines.formulary_override_reason` context or a separate audit column.

#### GAP-11 — Controlled drug register and running balance (`pharmacy.md §5`)

- `pharmacy.md §5` requires a **running balance per patient and per batch** for controlled drugs, and an **append-only register export** for inspections. The Phase P4 implementation adds a controlled substance report (`GET /reports/controlled-substance-register`) but does not describe how the running balance is maintained (e.g., a dedicated `controlled_drug_register` table with sequential entries and a balance column, vs. aggregated from `StockMovement`).
- A simple aggregation from movements is sufficient for low-volume drugs, but append-only registers for inspection export typically require immutable, sequentially numbered entries (like a logbook page).
- **Proposed plan**: Define whether the controlled drug register is a view over `StockMovement` or a separate append-only table. If regulatory inspections require sequential numbering, add a `controlled_drug_register` table with `entry_no` sequence per location.

#### GAP-12 — Emergency Purchase Entry not in the plan (`pharmacy.txt §4.1`)

- **Source**: `pharmacy.txt §4.1 Central Store Stock` explicitly lists **"Emergency Purchase Entry"** as a function of the central store. Neither `pharmacy.md` nor this implementation plan mentions it.
- **Definition**: A direct stock-receipt entry where the central pharmacy can bypass the standard PO → GRN flow (e.g. urgent procurement from a local market supplier with no pre-approved PO). Must be flagged as an emergency purchase for separate reporting and typically requires elevated approval.
- **Fields required**: `emergency_purchase_ref`, `reason` (mandatory), `approver_user_id`, `supplier_name` (free text if unlisted supplier), `drug_id`, `batch_number`, `quantity`, `unit_purchase_price`, `invoice_ref`. Status: `DRAFT → APPROVED → RECEIVED`.
- **Business rules**:
  - Requires manager-level approval (`HOSPITAL_MANAGE` or dedicated `HOSPITAL_PHARMACY_EMERGENCY_PURCHASE` permission).
  - Flagged separately in stock movement records (`movement_type = emergency_purchase`) so they are distinguishable from standard GRN receipts.
  - Audit log must capture who initiated, who approved, and the stated reason.
- **Proposed plan**: Add `emergency_purchase_entries` table and APIs (`POST /emergency-purchases`, `GET /emergency-purchases/{id}`, `POST /emergency-purchases/{id}/approve`, `POST /emergency-purchases/{id}/receive`). Tag resulting `StockMovement` rows with `movement_type = EMERGENCY_PURCHASE`. Expose in the stock movement report and a dedicated emergency-purchase log report.

#### GAP-13 — Due Collection management missing (`pharmacy.txt §10`)

- **Source**: `pharmacy.txt §10` defines "Due Collection" as a first-class feature: track customer dues, partial payments, and aging reports.
- **Current state**: The implementation plan has no concept of credit sales, outstanding balances, or customer due management. All financial tracking is deferred to `hospital-billing-service`, but pharmacy-level due collection (particularly for walk-in credit/OTC sales not tied to a hospital admission) requires pharmacy-side data.
- **Required capabilities**:
  - **Credit sale flag** on dispense order: `payment_mode` enum (`CASH`, `CREDIT`, `CORPORATE`); `credit_account_id` for walk-in credit customers.
  - **Customer credit account**: `pharmacy_credit_accounts` table (`customer_id`, `customer_name`, `credit_limit`, `outstanding_balance`).
  - **Partial payment recording**: `pharmacy_payments` table (`credit_account_id`, `amount_paid`, `payment_date`, `payment_mode`, `reference`).
  - **Aging calculation**: Outstanding invoices bucketed by days overdue (0–30, 31–60, 61–90, >90 days) per customer.
- **Scope boundary**: For patients on IPD admission, dues are managed by `hospital-billing-service`. This gap covers **walk-in / OTC credit customers** where pharmacy is the point of collection.
- **Proposed plan**: Add credit account and payment entities to `hospital-pharmacy-service`. Add APIs: `GET /credit-accounts`, `POST /credit-accounts/{id}/payments`, `GET /reports/due-collection?agingBuckets=true`. This is a P6 item dependent on WS-A billing integration being stable.

---

### 11.3 Enhancements

#### ENH-1 — Near-expiry rule: add `ALLOW_WITH_APPROVAL` action and `discount_required` flag

Already captured under INC-5; restated here for implementation tracking. Extend `pharmacy_near_expiry_rules` with `action` enum (`ALERT_ONLY`, `BLOCK`, `ALLOW_WITH_APPROVAL`), `approver_role_code`, and `discount_required` boolean.

#### ENH-2 — Department-wise consumption report

The existing consumption report aggregates issued quantities per drug across the pharmacy. `pharmacy.md §6` and §4.2 require a **department-wise** breakdown with cost center tagging. Extend `GET /reports/consumption` with an optional `departmentId` parameter, and ensure `DispenseOrder.department_id` is populated and indexed for this query.

#### ENH-3 — Stock movement `GET` API with rich filtering

`GET /pharmacies/{id}/stock/movements?drugId=&batchNumber=&type=&from=&to=&page=&size=` is a low-effort addition that unlocks transfer history, adjustment audit, and supplier-wise stock movement reports. Add to §5.2 and implement in `PharmacyStockController`.

#### ENH-4 — `FormularyRule` one-to-many alternatives table

Replace or supplement the current formulary rule's single-alternative reference with a `formulary_alternatives` join table (`formulary_rule_id`, `alternative_drug_id`, `priority`, `equivalence_class`). The `GET /drugs/{id}/formulary-alternatives` response already returns a list; this aligns the DB schema with the API contract.

#### ENH-5 — Return flow: link to original billing charge after WS-A

After billing integration is live (WS-A), the `POST /dispense-orders/{id}/returns` endpoint should optionally accept `originalChargeId` per return line, and pharmacy should verify with billing that the charge is reversible before updating stock. This closes the gap in `pharmacy.md §4.1.3` ("returns tied back to original invoice").

#### ENH-6 — Barcode/QR scanning roadmap entry

`pharmacy.md §8` Future Enhancements lists barcode/QR scanning for batch selection, fast dispensing, and stock counting. Add this to a future phase or the roadmap section so it is not forgotten during counter workflow design. Key surfaces: stock receipt (batch barcode), dispense line (drug barcode → auto-fill), and physical count.

#### ENH-7 — WS-I Clinical safety net: full P4 design

The current stub (a config flag `check-at-dispense-enabled`) defers all logic. `pharmacy.md §5` defines specific severity tiers (alert-only vs hard-stop) and an acknowledgement workflow. Plan the full WS-I implementation in Phase P4: define the `hospital-service` endpoint to call for interaction/allergy checks, the response schema, how acknowledgements are stored on `dispense_lines`, and what hard-stop means for the API (400 with structured error vs a separate "acknowledge and proceed" endpoint).

#### ENH-8 — IPD return distinction (patient vs ward/department stock)

`pharmacy.md §4.2` distinguishes patient-level returns (linked to admission/bill) from department/ward stock returns. The current `POST /dispense-orders/{id}/returns` API does not capture this distinction. Add a `return_type` enum (`PATIENT_RETURN`, `WARD_STOCK_RETURN`) on return lines so reports and bill adjustments handle them correctly.

#### ENH-9 — Transfer document / voucher reference

`pharmacy.md §3.2` says a "linked transfer/issue document is created for audit and reporting." When a transfer is completed (or when a requisition is approved and received — see GAP-2), generate a transfer document reference number and attach it to all `StockMovement` records created by that transfer. This enables the transfer history report (GAP-7) to show one row per document rather than per movement line.

#### ENH-10 — Manufacturer `license_no` and inactive enforcement

Already captured under INC-4. In addition, enforce that `PATCH /manufacturers/{id}` cannot set a manufacturer inactive if there are active (non-cancelled) dispense orders or open stock receipts referencing it.

#### ENH-11 — Product Group master table

`pharmacy.txt §3.2` marks `Product Group` as a required field on the drug master (shown with an asterisk) and it appears in the product list view. Group-wise stock reports (GAP-7) and PO demand forecasting (GAP-5) both need it. Currently there is no `product_groups` lookup table.

- **Proposed plan**: Add `product_groups` table (`id`, `name`, `description`, `is_active`). Add CRUD APIs: `POST /product-groups`, `GET /product-groups`, `PATCH /product-groups/{id}`. Add `product_group_id` FK on `Drug`. Include `productGroupId` in `DrugRequest` (required) and `DrugResponse`. Use in `GET /drugs?productGroupId=` filter. This is the foundation for group-wise reports and PO filtering by product category.

#### ENH-12 — Rack No on Drug for physical stock-count workflows

`pharmacy.txt §3.2` lists `Rack No` in the product list view. During physical stock counts and receiving, pharmacists need to know which shelf/rack the drug is stored on to reconcile counts quickly.

- **Proposed plan**: Add `rack_no VARCHAR(50)` (nullable) to `Drug`. Expose in `DrugResponse` and the stock list view API (`GET /pharmacies/{id}/stock`). This is a low-effort addition that significantly improves counter workflow UX.

#### ENH-13 — "Send to Supplier" email on PO generation

`pharmacy.txt §5.4.9` requires a "Send to Supplier" button during PO generation that emails the PO directly to the supplier, with audit logging (sent time, recipient email). When the PO engine is implemented (GAP-5), this integration must be planned:

- Store the supplier email on the `Manufacturer` record.
- On PO approval, an email with a PDF attachment of the PO is sent via the platform email service.
- Log `po_supplier_email_audit` (po_id, recipient, sent_at, triggered_by_user_id, success flag).

#### ENH-14 — PO Order Quantity modification history

`pharmacy.txt §5.4.7` requires the system to log modification history when a user manually changes the auto-calculated Order Quantity on a PO requisition line. Add a `purchase_requisition_line_audit` table capturing: `line_id`, `changed_by_user_id`, `changed_at`, `previous_quantity`, `new_quantity`, `reason` (optional). Expose via `GET /purchase-requisitions/{id}/lines/{lineId}/history`.

---

### 11.4 Summary Priority Table

**Sources**: `(A)` = derived from `pharmacy.md`; `(B)` = derived from first-hand SRS `pharmacy.txt`; `(AB)` = both.

| # | Item | Source | Type | Priority | Suggested Phase |
|---|------|--------|------|----------|-----------------|
| INC-1 | `PATCH /pharmacies/{id}` missing from API spec §5.2 | A | Inconsistency | Low (docs only) | Immediate |
| INC-2 | `GET /drugs/search` spec alignment | A | Inconsistency | Low (docs only) | Immediate |
| INC-3 | Drug missing: pricing, product_group, department, rack_no, reminder_stock, tax | AB | Inconsistency | **High** | P5 |
| INC-4 | Manufacturer missing: license_no, vat, commission, type + inactive enforcement | AB | Inconsistency | **High** | P5 |
| INC-5 | Near-expiry `ALLOW_WITH_APPROVAL` + discount_required + effective dates | A | Inconsistency | Medium | P5 |
| INC-6 | Formulary alternatives one-to-many vs single substituted_drug_id | A | Inconsistency | Medium | P5 |
| GAP-1 | Unit & Conversion Master entirely missing | A | Gap | **High** | P5 |
| GAP-2 | Transfer/Requisition approval workflow (Draft→Submitted→Approved→Received) | AB | Gap | **High** | P5–P6 |
| GAP-3 | Supplier returns from outlet pharmacies (with supervisor approval) | AB | Gap | **High** | P5–P6 |
| GAP-4 | Stock adjustment approval workflow above threshold | AB | Gap | Medium | P5 |
| GAP-5 | Procurement/PO suggestion engine: ownership + full calculation detail | AB | Gap | **High** | Docs + P6 |
| GAP-6 | Stock movement history query API | A | Gap | Medium | P5 |
| GAP-7 | Missing reports: group-wise, transfer history, gross profit, due/aging, discount, returns, adjustment | AB | Gap | Medium–High | P5–P6 |
| GAP-8 | Return → billing charge linkage | A | Gap | Medium | After WS-A |
| GAP-9 | Walk-in patient detail capture policy (config: mandatory vs optional) | A | Gap | Low | P5 |
| GAP-10 | Formulary rule effective dates + rule_set_version | A | Gap | Medium | P5 |
| GAP-11 | Controlled drug register: aggregate view vs append-only sequential table | A | Gap | Medium | P4 ext |
| GAP-12 | Emergency Purchase Entry (bypass PO flow, flag + elevated approval) | **B** | Gap | **High** | P5–P6 |
| GAP-13 | Due Collection management (credit accounts, partial payments, aging) | **B** | Gap | **High** | P6 |
| ENH-1 | Near-expiry ALLOW_WITH_APPROVAL (same as INC-5) | A | Enhancement | Medium | P5 |
| ENH-2 | Department-wise consumption report | A | Enhancement | Medium | P5 |
| ENH-3 | Stock movement GET API with rich filtering | A | Enhancement | Medium | P5 |
| ENH-4 | Formulary alternatives one-to-many table | A | Enhancement | Medium | P5 |
| ENH-5 | Return → billing charge link after WS-A | A | Enhancement | Medium | Post WS-A |
| ENH-6 | Barcode/QR scanning roadmap entry | AB | Enhancement | Low | Future |
| ENH-7 | WS-I clinical safety net: full P4 design (severity tiers + acknowledgement) | A | Enhancement | Medium | P4 ext |
| ENH-8 | IPD return type distinction (PATIENT_RETURN vs WARD_STOCK_RETURN) | A | Enhancement | Medium | P5 |
| ENH-9 | Transfer document / voucher reference number on StockMovement | A | Enhancement | Low–Medium | P5 |
| ENH-10 | Manufacturer inactive enforcement on PATCH | A | Enhancement | Low | P5 |
| ENH-11 | Product Group master table + Drug FK + API + report filter | **B** | Enhancement | **High** | P5 |
| ENH-12 | Rack No on Drug for physical stock-count UX | **B** | Enhancement | Low | P5 |
| ENH-13 | "Send to Supplier" email on PO generation with audit log | **B** | Enhancement | Medium | P6 |
| ENH-14 | PO Order Quantity modification history on requisition lines | **B** | Enhancement | Medium | P6 |

