# Hospital Clinical Orders Service – Implementation Plan (Granular)

This document is the single source of truth for implementing `hospital-clinical-orders-service`. Each phase has a granular checklist (DB, Backend, Frontend) so that “implement Phase N” can be executed without ambiguity or missing items.

---

## 1. Overview and Objectives

`hospital-clinical-orders-service` is responsible for:

- **Orderable clinical services**: Lab orders, radiology/imaging orders, procedure orders (OPD/IPD, non-OT and OT-adjacent where applicable).
- **Order lifecycle**: requested → verified → scheduled → in-progress → completed → reported → cancelled.
- **Clinical worklists**: Lab, radiology, and procedure execution teams (queues and assignment).
- **Result metadata**: Report availability status, external system IDs (LIS/RIS/PACS), viewer links (not full results—those stay in LIS/RIS/PACS).

**Primary objectives:**

- Single place for order headers, line items, lifecycle, and worklists.
- Publish order/status events for billing, EHR, and portals.
- Integrate with LIS/RIS/PACS for result availability callbacks and optional outbound order push.

**Out of scope (other services):**

- Full clinical results (PDFs, images, panels) – in LIS/RIS/PACS; this service stores links/metadata only.
- Billing/pricing logic – `hospital-billing-service` consumes order events for charge capture.
- Patient/visit master data – references only, owned by `hospital-service`.

---

## 1.1 Implementation Status

- **Phase 0 – Service Skeleton and Foundations**: **Done**
- **Phase 1 – Core Order Management**: **Done**
- **Phase 2 – Result Metadata and LIS/RIS Integration**: **Not started**
- **Phase 3 – Advanced Workflows and Optimization**: **Not started**
- **Phase 4 – Multi-Facility and Scaling**: **Not started**

---

## 1.2 Phase-wise Implementation Checklist (DB · Backend · Frontend)

| Phase | DB | Backend | Frontend |
|-------|----|---------|----------|
| **Phase 0** – Service skeleton | Schema only | 100% checklist below | 0% |
| **Phase 1** – Core orders & worklists | 100% checklist below | 100% checklist below | 100% checklist below |
| **Phase 2** – Results integration | 100% checklist below | 100% checklist below | 100% checklist below |
| **Phase 3** – Advanced workflows | Optional indexes | 100% checklist below | 100% checklist below |
| **Phase 4** – Multi-facility | Optional columns/partitioning | 100% checklist below | Optional |

Use the per-phase sections (§7) as the single source of truth when asking Cursor to “implement Phase N”.

---

## 2. Scope and Non-Scope

### 2.1 In-Scope

- Order set and clinical order (lab, radiology, procedure) creation and lifecycle.
- Worklist items per order (lab section, radiology room, procedure room, mobile collection, etc.) with assignment and status.
- REST APIs for order entry, query, cancel, worklist read/assign/status.
- Result link metadata and callback API for LIS/RIS/PACS.
- Event publishing for order-set created, order created, order status changed, worklist status changed, result available, order cancelled.
- Integration with `hospital-service` for patient/visit reference validation (or stub).
- Integration with `hospital-billing-service` via events for charge capture.

### 2.2 Out-of-Scope

- Full diagnostic results storage (LIS/RIS/PACS own content).
- Billing/pricing logic (billing service consumes events).
- Patient/visit/encounter master data (hospital-service).
- Corporate/discount eligibility (optional pre-check via `hospital-corporate-and-discount-service`; can be stubbed).

---

## 3. Architecture and Boundaries

- **Service type**: Stateless Spring Boot microservice (REST + event publishing).
- **Database**: PostgreSQL; schema `hospital_clinical_orders` versioned via **hospital-service** Liquibase (same pattern as `hospital_billing`, `hospital_pharmacy`).
- **Service module path**: `easyops-erp/services/hospital-clinical-orders-service`.
- **API base path**: `/api/hospital-clinical-orders` (API Gateway routes to this service).
- **Integration**: Synchronous REST for UI and validation; asynchronous events (Kafka or Spring events) for billing, portal BFF, analytics.

---

## 4. Data Model (Exact Tables for Implementation)

### 4.1 order_sets

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| patient_id | UUID NOT NULL | FK reference to hospital-service patient |
| visit_id | UUID | FK reference to hospital-service visit/encounter |
| ordering_doctor_id | UUID | |
| ordering_department_id | UUID | |
| order_context | VARCHAR(20) | OPD, IPD, ED |
| priority | VARCHAR(20) | STAT, ROUTINE, URGENT |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |
| created_by | UUID | |

### 4.2 clinical_orders

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| order_set_id | UUID NOT NULL FK → order_sets(id) | |
| order_type | VARCHAR(20) NOT NULL | LAB, RADIOLOGY, PROCEDURE |
| item_code | VARCHAR(100) NOT NULL | Chargeable item / clinical chart code |
| status | VARCHAR(30) NOT NULL | REQUESTED, VERIFIED, SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, REJECTED |
| priority | VARCHAR(20) | STAT, ROUTINE, URGENT |
| ordering_notes | TEXT | Clinical indications, reason for test/procedure |
| performed_at | TIMESTAMPTZ | |
| performed_by | UUID | |
| cancel_reason | TEXT | |
| cancelled_at | TIMESTAMPTZ | |
| cancelled_by | UUID | |
| external_system_id | VARCHAR(255) | LIS/RIS/PACS order or accession ID |
| result_status | VARCHAR(20) | PENDING, PARTIAL, FINAL |
| result_available_at | TIMESTAMPTZ | |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |
| created_by | UUID | |

### 4.3 order_worklist_items

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| order_id | UUID NOT NULL FK → clinical_orders(id) | |
| worklist_type | VARCHAR(50) NOT NULL | LAB_SECTION, RADIOLOGY_ROOM, PROCEDURE_ROOM, MOBILE_COLLECTION, etc. |
| assigned_to_user_id | UUID | |
| assigned_to_role | VARCHAR(100) | |
| scheduled_time | TIMESTAMPTZ | |
| status | VARCHAR(30) NOT NULL | QUEUED, ASSIGNED, IN_PROGRESS, COMPLETED, ON_HOLD |
| remarks | TEXT | |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

### 4.4 result_links (Phase 2)

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| order_id | UUID NOT NULL FK → clinical_orders(id) | |
| system_type | VARCHAR(30) NOT NULL | LIS, RIS, PACS, INTERNAL |
| external_system_id | VARCHAR(255) | Accession number, study UID, etc. |
| viewer_url | VARCHAR(1000) | URL for portal/hospital viewer |
| version | INTEGER DEFAULT 1 | Revised reports increment |
| revised_at | TIMESTAMPTZ | |
| created_at | TIMESTAMPTZ | |

---

## 5. APIs (Exact Contracts for Implementation)

Base path: **`/api/hospital-clinical-orders`**.

### 5.1 Order Set APIs

| Method | Path | Request body / params | Response | Purpose |
|--------|------|------------------------|----------|--------|
| POST | `/order-sets` | CreateOrderSetRequest | OrderSetResponse | Create order set with one or more clinical orders |
| GET | `/order-sets/{orderSetId}` | — | OrderSetDetailResponse | Order set with all orders and status metadata |
| GET | `/order-sets` | patientId?, visitId?, status?, from?, to?, page?, size? | PagedResponse&lt;OrderSetResponse&gt; | Search order sets |

**CreateOrderSetRequest**:  
`patientId`, `visitId?`, `orderingDoctorId`, `orderingDepartmentId?`, `orderContext` (OPD\|IPD\|ED), `priority?` (STAT\|ROUTINE\|URGENT), `orders` (array of CreateOrderLineRequest)

**CreateOrderLineRequest** (each line):  
`orderType` (LAB\|RADIOLOGY\|PROCEDURE), `itemCode`, `orderingNotes?`, `priority?`

**OrderSetResponse**:  
`id`, `patientId`, `visitId`, `orderingDoctorId`, `orderingDepartmentId`, `orderContext`, `priority`, `createdAt`, `createdBy`

**OrderSetDetailResponse**: extends OrderSetResponse with `orders: ClinicalOrderResponse[]`

### 5.2 Order (Clinical Order) APIs

| Method | Path | Request body / params | Response | Purpose |
|--------|------|------------------------|----------|--------|
| GET | `/orders/{orderId}` | — | ClinicalOrderDetailResponse | Full order details (worklist and result metadata) |
| GET | `/orders` | patientId?, visitId?, orderSetId?, type?, status?, from?, to?, page?, size? | PagedResponse&lt;ClinicalOrderResponse&gt; | List orders |
| POST | `/orders/{orderId}/cancel` | CancelOrderRequest | ClinicalOrderResponse | Cancel order with reason |
| PATCH | `/orders/{orderId}` | UpdateOrderRequest | ClinicalOrderResponse | Limited updates (priority, orderingNotes) |

**CancelOrderRequest**:  
`reason` (required), `cancelledBy?`

**UpdateOrderRequest**:  
`priority?`, `orderingNotes?`

**ClinicalOrderResponse**:  
`id`, `orderSetId`, `orderType`, `itemCode`, `status`, `priority`, `orderingNotes`, `performedAt`, `performedBy`, `cancelReason`, `cancelledAt`, `cancelledBy`, `externalSystemId`, `resultStatus`, `resultAvailableAt`, `createdAt`, `createdBy`

**ClinicalOrderDetailResponse**: extends ClinicalOrderResponse with `worklistItems: WorklistItemResponse[]`, `resultLinks: ResultLinkResponse[]` (Phase 2)

### 5.3 Worklist APIs

| Method | Path | Request body / params | Response | Purpose |
|--------|------|------------------------|----------|--------|
| GET | `/worklists` | type?, status?, assignedTo?, from?, to?, page?, size? | PagedResponse&lt;WorklistItemDetailResponse&gt; | Get worklist entries |
| POST | `/worklists/{worklistItemId}/assign` | AssignWorklistRequest | WorklistItemResponse | Assign to user/role |
| POST | `/worklists/{worklistItemId}/status` | UpdateWorklistStatusRequest | WorklistItemResponse | Transition status (QUEUED → ASSIGNED → IN_PROGRESS → COMPLETED, ON_HOLD) |

**AssignWorklistRequest**:  
`assignedToUserId?`, `assignedToRole?`

**UpdateWorklistStatusRequest**:  
`status` (required), `remarks?`

**WorklistItemResponse**:  
`id`, `orderId`, `worklistType`, `assignedToUserId`, `assignedToRole`, `scheduledTime`, `status`, `remarks`, `createdAt`, `updatedAt`

**WorklistItemDetailResponse**: extends WorklistItemResponse with `order: ClinicalOrderResponse` (minimal), `orderSetId`, `patientId`, `visitId` for list context

### 5.4 Results Integration APIs (Phase 2)

| Method | Path | Request body / params | Response | Purpose |
|--------|------|------------------------|----------|--------|
| POST | `/orders/{orderId}/results` | CreateResultLinkRequest | ResultLinkResponse | LIS/RIS/PACS callback – result availability |
| GET | `/orders/{orderId}/results` | — | List&lt;ResultLinkResponse&gt; | List result links for order |

**CreateResultLinkRequest**:  
`resultStatus` (PARTIAL\|FINAL), `systemType` (LIS\|RIS\|PACS\|INTERNAL), `externalSystemId?`, `viewerUrl?`, `version?`

**ResultLinkResponse**:  
`id`, `orderId`, `systemType`, `externalSystemId`, `viewerUrl`, `version`, `revisedAt`, `createdAt`

---

## 6. Events and Messaging

**Topics / event names (indicative; align with platform conventions):**

- `hospital.clinical-orders.order-set.created`
- `hospital.clinical-orders.order.created`
- `hospital.clinical-orders.order.status-changed`
- `hospital.clinical-orders.worklist.status-changed`
- `hospital.clinical-orders.result.available`
- `hospital.clinical-orders.order.cancelled`

**Payload (minimal):** orderSetId, orderId, patientId, visitId, status, timestamp, source. Include worklistItemId for worklist events.

**Consumers:**

- `hospital-billing-service`: order.created, order.status-changed (e.g. COMPLETED for charge capture), order.cancelled.
- `hospital-service` / EHR: result.available for timelines/notifications.
- `hospital-portal-bff-service`: all relevant events for patient/doctor portal views.
- Analytics: all events for TAT, volume, cancellation rates.

---

## 7. Phased Implementation – Granular Checklists

Implement in order. Each phase is self-contained so that “implement Phase N” can be executed without ambiguity.

---

### Phase 0 – Service Skeleton and Foundations

**Goal**: Deployable `hospital-clinical-orders-service` with health, discovery, and API Gateway route; no domain logic.

#### 0.1 Maven module

- [ ] Create `easyops-erp/services/hospital-clinical-orders-service/pom.xml`.
  - Parent: `com.easyops:easyops-erp:1.0.0`.
  - Artifact: `hospital-clinical-orders-service`, packaging `jar`.
  - Dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `spring-cloud-starter-netflix-eureka-client`, `spring-cloud-starter-loadbalancer`, `postgresql` (runtime), `liquibase-core`, `lombok` (optional), `micrometer-registry-prometheus`, `springdoc-openapi-starter-webmvc-ui`. Same pattern as `hospital-billing-service/pom.xml`. Add `spring-kafka` or `spring-boot-starter-amqp` if events will use Kafka/RabbitMQ from Phase 1.

#### 0.2 Application and config

- [ ] Create `easyops-erp/services/hospital-clinical-orders-service/src/main/java/com/easyops/hospitalclinicalorders/HospitalClinicalOrdersServiceApplication.java`.
  - `@SpringBootApplication`, `@EnableJpaAuditing` if used later.
- [ ] Create `easyops-erp/services/hospital-clinical-orders-service/src/main/resources/application.yml`.
  - `spring.application.name: hospital-clinical-orders-service`
  - Server port (e.g. 8090, or random).
  - JPA: default schema `hospital_clinical_orders`, ddl-auto `none`, Hibernate dialect for PostgreSQL.
  - Datasource URL, username, password (placeholders; same DB as hospital-service).
  - Eureka client config.
  - Actuator: expose health, info, metrics, prometheus.
- [ ] Create `application-dev.yml` / `application-local.yml` if needed.

#### 0.3 API Gateway

- [ ] In `easyops-erp/services/api-gateway/src/main/resources/application.yml`, add route:
  - id: `hospital-clinical-orders-service`
  - uri: `lb://hospital-clinical-orders-service`
  - predicates: `Path=/api/hospital-clinical-orders/**`

#### 0.4 Database schema (Liquibase in hospital-service)

- [ ] Create `easyops-erp/services/hospital-service/src/main/resources/db/changelog/changesets/021-hospital-clinical-orders-schema.sql`.
  - Liquibase formatted SQL.
  - Create schema `hospital_clinical_orders` if not exists; grant to app user.
  - Phase 0: empty schema only (Phase 1 adds tables in same file or new changeset 022).
- [ ] In `easyops-erp/services/hospital-service/src/main/resources/db/changelog/db.changelog-master.xml`, add:
  - `<include file="changesets/021-hospital-clinical-orders-schema.sql" relativeToChangelogFile="true"/>`

#### 0.5 Parent POM

- [ ] In `easyops-erp/pom.xml`, add `<module>services/hospital-clinical-orders-service</module>`.

#### 0.6 Health and info

- [ ] Ensure actuator health and info endpoints respond when service runs and DB is available.

**Deliverables**: Service starts, registers with Eureka, reachable via API Gateway at `/api/hospital-clinical-orders/**`. No domain tables or controllers in Phase 0.

**Frontend**: None.

---

### Phase 1 – Core Order Management and Worklists

**Goal**: Order sets and clinical orders (create, read, list, cancel); worklist generation and worklist APIs; event publishing; optional hospital-service validation stub; simple clinician order entry and worklist UI.

#### 1.1 Database (Liquibase)

- [ ] In `021-hospital-clinical-orders-schema.sql` (or new `022-hospital-clinical-orders-core-tables.sql`), add:
  - Table `hospital_clinical_orders.order_sets` (all columns from §4.1).
  - Table `hospital_clinical_orders.clinical_orders` (all columns from §4.2).
  - Table `hospital_clinical_orders.order_worklist_items` (all columns from §4.3).
  - FK: `clinical_orders.order_set_id` → `order_sets(id)`.
  - FK: `order_worklist_items.order_id` → `clinical_orders(id)`.
  - Indexes: `order_sets(patient_id, created_at)`, `order_sets(visit_id, created_at)`, `clinical_orders(order_set_id)`, `clinical_orders(status, order_type)`, `clinical_orders(created_at)`, `order_worklist_items(order_id)`, `order_worklist_items(worklist_type, status)`, `order_worklist_items(assigned_to_user_id, status)`.
- [ ] If using a new file 022, include it in `db.changelog-master.xml`.

#### 1.2 Backend – Order Set context

- [ ] Package: `com.easyops.hospitalclinicalorders.domain.orderset`.
- [ ] Entity: `OrderSet` (map to `hospital_clinical_orders.order_sets`).
- [ ] Repository: `OrderSetRepository` (JpaRepository); methods: `findById`, `findByPatientIdOrderByCreatedAtDesc`, `findByVisitIdOrderByCreatedAtDesc`, and a method for filtered list with pagination (patientId, visitId, from, to, Pageable).
- [ ] DTOs: `CreateOrderSetRequest`, `CreateOrderLineRequest`, `OrderSetResponse`, `OrderSetDetailResponse` (with list of ClinicalOrderResponse).
- [ ] Service: `OrderSetService` – create order set (create OrderSet + N ClinicalOrder + N OrderWorklistItem per order, e.g. one worklist item per order with worklist_type derived from order_type); get by id with orders; list with filters (paginated). On create, publish `order-set.created` and `order.created` per order.
- [ ] Controller: `OrderSetController` – `POST /order-sets`, `GET /order-sets/{id}`, `GET /order-sets` with request params. Base path or context so Gateway forwards to `/api/hospital-clinical-orders`.

#### 1.3 Backend – Clinical Order context

- [ ] Package: `com.easyops.hospitalclinicalorders.domain.order`.
- [ ] Entity: `ClinicalOrder` (map to `hospital_clinical_orders.clinical_orders`).
- [ ] Repository: `ClinicalOrderRepository`; methods: `findById`, `findByOrderSetId`; filtered list with pagination (patientId, visitId via join to OrderSet; orderSetId, type, status, from, to, Pageable). Use JPA Specification or @Query joining `clinical_orders` and `order_sets` for patient/visit filters.
- [ ] DTOs: `ClinicalOrderResponse`, `ClinicalOrderDetailResponse` (order + worklistItems + resultLinks empty list for Phase 1), `CancelOrderRequest`, `UpdateOrderRequest`.
- [ ] Service: `ClinicalOrderService` – get by id (with worklist items); list with filters; cancel (validate not COMPLETED/finalized; set status CANCELLED, cancelled_at, cancelled_by, cancel_reason; update related worklist items; publish `order.cancelled`); update (priority, orderingNotes only).
- [ ] Controller: `OrderController` – `GET /orders/{id}`, `GET /orders`, `POST /orders/{id}/cancel`, `PATCH /orders/{id}`.

#### 1.4 Backend – Worklist context

- [ ] Package: `com.easyops.hospitalclinicalorders.domain.worklist`.
- [ ] Entity: `OrderWorklistItem` (map to `hospital_clinical_orders.order_worklist_items`).
- [ ] Repository: `OrderWorklistItemRepository`; methods: `findById`, `findByOrderId`, `findByWorklistTypeAndStatus`, `findByAssignedToUserIdAndStatus`, and filtered list (type, status, assignedTo, from, to, Pageable).
- [ ] DTOs: `WorklistItemResponse`, `WorklistItemDetailResponse` (item + minimal order + orderSetId, patientId, visitId for list view), `AssignWorklistRequest`, `UpdateWorklistStatusRequest`.
- [ ] Service: `WorklistService` – list worklist items with optional order/patient context; assign (set assigned_to_user_id, assigned_to_role; publish `worklist.status-changed`); update status (validate transitions; update item; optionally update ClinicalOrder.status when IN_PROGRESS or COMPLETED; publish `worklist.status-changed` and possibly `order.status-changed`).
- [ ] Controller: `WorklistController` – `GET /worklists`, `POST /worklists/{worklistItemId}/assign`, `POST /worklists/{worklistItemId}/status`.

#### 1.5 Backend – Event publishing

- [ ] Create event DTOs (e.g. `OrderSetCreatedEvent`, `OrderCreatedEvent`, `OrderStatusChangedEvent`, `WorklistStatusChangedEvent`, `OrderCancelledEvent`) with fields: orderSetId, orderId, patientId, visitId, status, worklistItemId (where applicable), timestamp.
- [ ] Create `ClinicalOrdersEventPublisher` (or use `ApplicationEventPublisher` + Kafka producer): publish events on order set create, order create, order status change, worklist status change, order cancel.
- [ ] If Kafka: configure `spring.kafka.*` in application.yml; create producer config and send to topic names above. If in-process first: use `ApplicationEventPublisher` and document that billing/BFF will consume via same app or adapter later.

#### 1.6 Backend – Optional hospital-service integration

- [ ] If `hospital-service` exposes patient/visit validation API: create a Feign client or RestTemplate client in `com.easyops.hospitalclinicalorders.integration` (e.g. `HospitalServiceClient`) and call it from `OrderSetService` before creating order set. If not available: stub the validation (accept all) and add a TODO to wire when hospital-service API is defined.

#### 1.7 Backend – OpenAPI and error handling

- [ ] Add OpenAPI (springdoc) so `/v3/api-docs` and Swagger UI work.
- [ ] Global exception handler: 404, 400 validation, 409 conflict (e.g. cancel already completed order) with consistent JSON body.

#### 1.8 Frontend – Service and types

- [ ] Create `easyops-erp/frontend/src/services/hospitalClinicalOrdersService.ts`.
  - Base URL: `/api/hospital-clinical-orders`.
  - Types: `OrderSet`, `OrderSetDetail`, `CreateOrderSetRequest`, `CreateOrderLineRequest`, `ClinicalOrder`, `ClinicalOrderDetail`, `WorklistItem`, `WorklistItemDetail`, `AssignWorklistRequest`, `UpdateWorklistStatusRequest`, `CancelOrderRequest`, `UpdateOrderRequest`, `PagedResponse<T>`.
  - Methods: `createOrderSet`, `getOrderSet`, `getOrderSets` (params), `getOrder`, `getOrders` (params), `cancelOrder`, `updateOrder`, `getWorklists` (params), `assignWorklistItem`, `updateWorklistStatus`.

#### 1.9 Frontend – Order entry page

- [ ] Create `easyops-erp/frontend/src/pages/hospital/ClinicalOrderEntry.tsx`.
  - Form: patient Id, visit Id (optional), ordering doctor Id, ordering department Id, order context (OPD/IPD/ED), priority (STAT/ROUTINE/URGENT), and a list of order lines (order type LAB/RADIOLOGY/PROCEDURE, item code, ordering notes, priority).
  - Add/remove line rows; submit calls `createOrderSet`. Show success with order set id and link to view.
  - Use MUI (TextField, Select, Button, Table for lines). Route: `/hospital/clinical-orders/entry`.

#### 1.10 Frontend – Order set and order list/detail

- [ ] Create `easyops-erp/frontend/src/pages/hospital/ClinicalOrderSets.tsx`.
  - List order sets: id, patient Id, visit Id, order context, priority, created at. Filters: patientId, visitId, date range. Click row to view detail.
  - Detail: order set header + table of orders (order type, item code, status, result status, performed at, cancel reason). Actions: Cancel order (per order) with reason. Route: `/hospital/clinical-orders/sets` and `/hospital/clinical-orders/sets/:id` (or modal).
- [ ] Optional: `ClinicalOrdersList.tsx` at `/hospital/clinical-orders/orders` – list orders with filters (patient, visit, type, status) and link to order detail.

#### 1.11 Frontend – Worklist views

- [ ] Create `easyops-erp/frontend/src/pages/hospital/ClinicalWorklists.tsx`.
  - Tabs or filters: worklist type (LAB_SECTION, RADIOLOGY_ROOM, PROCEDURE_ROOM, etc.), status (QUEUED, ASSIGNED, IN_PROGRESS, COMPLETED).
  - Table: worklist item id, order id, patient id, visit id, order type, item code, status, assigned to, scheduled time, actions (Assign, Update status). Assign opens dialog (user/role); Update status opens dialog (status, remarks). Route: `/hospital/clinical-orders/worklists`.

#### 1.12 Frontend – Routes and navigation

- [ ] In `easyops-erp/frontend/src/App.tsx`: under hospital module, add routes:
  - `hospital/clinical-orders/entry` → ClinicalOrderEntry
  - `hospital/clinical-orders/sets` → ClinicalOrderSets
  - `hospital/clinical-orders/sets/:id` → Order set detail (or same page with id)
  - `hospital/clinical-orders/orders` → ClinicalOrdersList (if implemented)
  - `hospital/clinical-orders/worklists` → ClinicalWorklists
- [ ] In `easyops-erp/frontend/src/components/Layout/MainLayout.tsx`: under Hospital menu, add:
  - “Clinical Orders – Entry” → `/hospital/clinical-orders/entry`
  - “Clinical Orders – Order Sets” → `/hospital/clinical-orders/sets`
  - “Clinical Orders – Worklists” → `/hospital/clinical-orders/worklists`

**Deliverables**: User can create order sets with multiple orders; view order sets and orders; cancel orders; view and manage worklists (assign, status transitions). Events published for billing and downstream consumers.

---

### Phase 2 – Result Metadata and LIS/RIS Integration

**Goal**: ResultLink entity and APIs; POST/GET results; update order result_status and result_available_at; publish result.available; UI to show result availability and viewer links.

#### 2.1 Database (Liquibase)

- [ ] New file `easyops-erp/services/hospital-service/src/main/resources/db/changelog/changesets/025-hospital-clinical-orders-result-links.sql` (actual repo filename; plan previously said 023).
  - Table `hospital_clinical_orders.result_links` (all columns from §4.4).
  - FK: `result_links.order_id` → `clinical_orders(id)`.
  - Indexes: `result_links(order_id)`, `result_links(external_system_id)`.
  - Include in `db.changelog-master.xml`.

#### 2.2 Backend – Result link context

- [ ] Package: `com.easyops.hospitalclinicalorders.domain.result`.
- [ ] Entity: `ResultLink` (map to `hospital_clinical_orders.result_links`).
- [ ] Repository: `ResultLinkRepository`; methods: `findByOrderId`, `findById`.
- [ ] DTOs: `CreateResultLinkRequest`, `ResultLinkResponse`.
- [ ] Service: `ResultLinkService` – create result link (validate order exists; set order.result_status, order.result_available_at when resultStatus is FINAL; persist ResultLink; publish `result.available`); list by order id.
- [ ] Controller: `ResultController` or extend `OrderController`: `POST /orders/{orderId}/results`, `GET /orders/{orderId}/results`. Ensure orderId path variable is validated (order must exist).

#### 2.3 Backend – Order detail with result links

- [ ] In `ClinicalOrderService.getOrderDetail` (or equivalent), include `ResultLinkService.findByOrderId(orderId)` in `ClinicalOrderDetailResponse.resultLinks`.

#### 2.4 Frontend – Service

- [ ] In `hospitalClinicalOrdersService.ts`: add `createResultLink(orderId, body)`, `getResultLinks(orderId)`; types `CreateResultLinkRequest`, `ResultLinkResponse`.

#### 2.5 Frontend – Result availability in UI

- [ ] In order set detail and order detail views: show `resultStatus`, `resultAvailableAt`, and list of result links (viewer URL as link, system type, external ID, revised at). If role allows, optional “Add result” form for testing (POST /orders/{id}/results) or document that this is typically called by LIS/RIS/PACS only.
- [ ] Optional: “Result available” badge or column in worklist and order list tables.

**Deliverables**: LIS/RIS/PACS (or integration bridge) can POST result metadata; order result_status and result_available_at updated; result.available event published; UI shows result links and viewer URLs.

---

### Phase 3 – Advanced Workflows and Optimization

**Goal**: Priority handling (STAT/URGENT) in worklist ordering; department/section-specific worklist filters; re-order/repeat order with pre-filled context; cancellation/amendment with audit; basic TAT/analytics endpoints.

#### 3.1 Backend – Worklist ordering and filters

- [ ] In `WorklistService` (or repository): support sort by priority (STAT first, then URGENT, then ROUTINE) and by scheduled_time, created_at. Add query params: `departmentId`, `section` (e.g. lab section name) if needed; map to worklist_type or order attributes.
- [ ] Document or implement department-level filtering (e.g. by ordering_department_id on order set or order).

#### 3.2 Backend – Re-order / repeat order

- [ ] New endpoint: `POST /order-sets/from-order-set` or `POST /order-sets?copyFrom={orderSetId}`. Request body or param: copyFrom order set id; create new order set with same patient, visit, ordering doctor/department, and same order lines (new ids); status REQUESTED. Use case: repeat panel after a period.
- [ ] DTO: optional `CopyOrderSetRequest` with `sourceOrderSetId`, `orderContext?`, `priority?`.

#### 3.3 Backend – Cancellation and audit

- [ ] Ensure cancel flow records `cancelled_by`, `cancelled_at`, `cancel_reason` and publishes `order.cancelled`. Add optional rule: if order is COMPLETED or result_status is FINAL, require admin override (e.g. role check) and log override reason in audit (new column or audit log table if needed).
- [ ] Optional: `AuditLog` or similar for order status transitions (order_id, from_status, to_status, changed_by, changed_at, reason). Add Liquibase table and persist in OrderSetService/ClinicalOrderService/WorklistService on state changes.

#### 3.4 Backend – Analytics / reporting

- [ ] New controller or endpoints: `GET /reports/tat?from=&to=&orderType=` (aggregate turnaround time: order created to result_available_at or completed); `GET /reports/volumes?from=&to=&groupBy=orderType|department` (count orders by type/department). Return simple DTOs (e.g. list of { period, orderType, count, avgTatHours }).
- [ ] Controller: `ClinicalOrdersReportController` or under existing.

#### 3.5 Frontend – Worklist filters and repeat order

- [ ] In ClinicalWorklists: add priority sort option; add department/section filter if backend supports.
- [ ] In Order set detail or list: “Repeat order set” button that calls copy-from API and navigates to new order set.

#### 3.6 Frontend – Reports (optional)

- [ ] New page `ClinicalOrdersReports.tsx` at `/hospital/clinical-orders/reports`: TAT and volume charts/tables using new report APIs. Add route and nav “Clinical Orders – Reports”.

**Deliverables**: Priority-aware worklists; department filters; repeat order; cancellation audit; TAT and volume report endpoints and optional UI.

---

### Phase 4 – Multi-Facility and Scaling

**Goal**: Optional facility_id on order_sets (and orders if denormalized); configurable routing of orders to different LIS/RIS per facility; documentation for partitioning/sharding if needed.

#### 4.1 Database (Liquibase)

- [ ] New changeset: add `facility_id UUID` to `order_sets` (and optionally to `clinical_orders` for query). Index `order_sets(facility_id, created_at)`.
- [ ] If multi-tenant by facility: add facility_id to worklist queries and order set creation.

#### 4.2 Backend – Facility context

- [ ] In `OrderSet`, `CreateOrderSetRequest`: add `facilityId` (optional). Validate or resolve facility in `OrderSetService`.
- [ ] In list APIs: add filter `facilityId?`. Scope worklist and order lists by facility when provided.
- [ ] Document integration pattern: “routing to LIS/RIS per facility” (e.g. config map facility_id → external system endpoint); actual adapter can be implemented in integration layer.

#### 4.3 Backend – Performance and observability

- [ ] Review indexes for list endpoints (order-sets, orders, worklists) for large datasets; add pagination limits and default page size.
- [ ] Metrics: orders_created_total, order_sets_created_total, worklist_status_changes_total, result_available_total; correlation IDs in logs.

**Deliverables**: Facility-aware order sets and filtering; documentation for external system routing; performance and metrics in place.

---

## 8. Security, Access Control, and Audit

- **Authentication**: All APIs behind platform identity (JWT/OAuth2/OpenID Connect); same pattern as hospital-billing-service and hospital-pharmacy-service.
- **Authorization**: Role-based – clinicians create/view orders for their patients; lab/radiology staff manage worklists for their department; admins can override/cancel with justification. Department/unit scoping for worklists when RBAC is available.
- **Audit**: Log state transitions (order and worklist) with changed_by, changed_at, previous/new status, reason; optional audit table for compliance. Result link add/update logged; correlation IDs for external callbacks.

---

## 9. Non-Functional Requirements

- **Performance**: High write throughput at peak OPD/IPD; worklist queries &lt; 300 ms p95 with pagination and indexes.
- **Reliability**: Idempotent create/update where applicable (e.g. idempotency key on order set create if needed); retry and DLQ for external callbacks.
- **Observability**: Structured logs with correlation ID; metrics (orders per hour, TAT, cancellation rate, worklist backlog by type).

---

## 10. Dependencies and Integration Touchpoints

- **hospital-service**: Patient/visit validation (sync); optional. Stub if not available.
- **hospital-billing-service**: Consumes order.created, order.status-changed, order.cancelled (async).
- **hospital-portal-bff-service**: Consumes events and/or calls read APIs for portal views.
- **hospital-corporate-and-discount-service**: Optional pre-check for eligibility; stub in Phase 1.
- **LIS/RIS/PACS**: Callback `POST /orders/{id}/results`; outbound order push pattern TBD per site (pull from this service or push via queue/API).

---

## 11. File and Path Quick Reference

| Layer | Path / artifact |
|-------|------------------|
| Service root | `easyops-erp/services/hospital-clinical-orders-service/` |
| App class | `.../hospitalclinicalorders/HospitalClinicalOrdersServiceApplication.java` |
| Config | `.../resources/application.yml` |
| Liquibase (schema) | `easyops-erp/services/hospital-service/.../changelog/changesets/021-hospital-clinical-orders-schema.sql`, `022-...-core-tables.sql`, `025-...-result-links.sql` |
| Changelog master | `easyops-erp/services/hospital-service/.../db.changelog-master.xml` |
| Order set | `...hospitalclinicalorders/domain/orderset/` (entity, repo, service, controller, DTOs) |
| Order | `...hospitalclinicalorders/domain/order/` (entity, repo, service, controller, DTOs) |
| Worklist | `...hospitalclinicalorders/domain/worklist/` (entity, repo, service, controller, DTOs) |
| Result | `...hospitalclinicalorders/domain/result/` (entity, repo, service, controller, DTOs) – Phase 2 |
| Events | `...hospitalclinicalorders/events/` or `integration/` (event DTOs, publisher) |
| API Gateway | `easyops-erp/services/api-gateway/src/main/resources/application.yml` |
| Frontend service | `easyops-erp/frontend/src/services/hospitalClinicalOrdersService.ts` |
| Frontend pages | `easyops-erp/frontend/src/pages/hospital/ClinicalOrderEntry.tsx`, `ClinicalOrderSets.tsx`, `ClinicalOrderDetail.tsx`, `ClinicalWorklists.tsx`, optional `ClinicalOrdersList.tsx`, `ClinicalOrdersReports.tsx` |
| Routes | `easyops-erp/frontend/src/App.tsx` (hospital/clinical-orders/*) |
| Nav | `easyops-erp/frontend/src/components/Layout/MainLayout.tsx` (Hospital submenu) |

When asking Cursor to “implement Phase N”, reference this document and the corresponding phase section (§7) so that every checkbox (DB, backend, frontend, routes, nav) is implemented without omission.
