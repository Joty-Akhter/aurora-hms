# `hospital-billing-service` – Implementation Plan (Granular)

## 1. Overview and Objectives

`hospital-billing-service` is the central **financial transaction and charge capture** service for the Hospital Module. It is responsible for:

- Capturing charges for:
  - Clinical services (visits, orders/procedures, lab, radiology, OT, IPD services).
  - Pharmacy and canteen consumption (via integration events).
  - Packages and corporate contracts.
- Managing:
  - Invoices, payments, refunds, and outstanding dues.
  - Adjustments, write-offs, and credits.
  - Links to discount and corporate coverage rules.

**Primary objectives:**

- Provide a consistent, auditable billing engine for all hospital-related services.
- Decouple clinical, pharmacy, and canteen domains from financial logic, while keeping integrations simple and robust.
- Ensure financial integrity and traceability from **charge capture** through to **invoice and payment**.

---

## 1.1 Implementation Status

- **Phase 0 – Service Skeleton and Foundations**: **Not started**
- **Phase 1 – Core Charge and Invoice Foundation**: **Not started**
- **Phase 2 – Payments and Refunds**: **Not started**
- **Phase 3 – Corporate and Discount Integration**: **Not started**
- **Phase 4 – Advanced Features and Hardening**: **Not started**

---

## 1.2 Phase-wise Implementation Checklist (DB · Backend · Frontend)

| Phase | DB | Backend | Frontend |
|-------|----|---------|----------|
| **Phase 0** – Service skeleton | N/A (reuse Postgres) | 100% checklist below | 0% |
| **Phase 1** – Charge & invoice | 100% checklist below | 100% checklist below | 100% checklist below |
| **Phase 2** – Payments & refunds | 100% checklist below | 100% checklist below | 100% checklist below |
| **Phase 3** – Corporate & discount | Optional config table | 100% checklist below | 100% checklist below |
| **Phase 4** – Advanced & hardening | Optional audit tables | 100% checklist below | 100% checklist below |

Use the per-phase sections below as the single source of truth when asking Cursor to “implement Phase N”; every file, route, and field is listed so nothing is missed.

---

## 2. Scope and Non-Scope

### 2.1 In-Scope

- **Charge capture and line item management**
  - Charge lines for visits, orders, procedures, pharmacy items, canteen items, packages, and other services.
  - Status lifecycle for charge lines (pending, posted, cancelled, reversed).
- **Invoice management**
  - OPD/IPD/corporate invoices.
  - Grouping of charge lines into invoices based on patient, visit, corporate contract, or payer.
  - Invoice lifecycle: draft, issued, partially paid, paid, cancelled.
- **Payments, refunds, and balances**
  - Recording payments across payment methods (cash, card, digital, corporate, card-balance, etc.).
  - Handling refunds and reversals.
  - Maintaining outstanding balances at invoice, patient, and corporate account level.
- **Discount application and linkage**
  - Consuming discount rules from `hospital-corporate-and-discount-service`.
  - Applying discounts to charge lines/invoices and recording discount reasons/sources.
- **Integration with accounting**
  - Generating events for accounting/ledger services with mapped GL codes.

### 2.2 Out-of-Scope (Handled by Other Services)

- Detailed **discount and corporate rule configuration** (owned by `hospital-corporate-and-discount-service`).
- **Card lifecycle and balance management** (owned by `hospital-card-management-service`).
- **Inventory and stock** logic for items (pharmacy, canteen, consumables).
- **Clinical behavior** or eligibility logic for when a service can be ordered.
- Full **accounting ledger** and financial reporting (handled by platform-wide Accounting/Finance services).

---

## 3. Architecture and Boundaries

- **Service type**: Stateless Spring Boot service (REST APIs + event listeners/publishers).
- **Database**: PostgreSQL; schema `hospital_billing` created and versioned via **hospital-service** Liquibase (same pattern as `hospital_pharmacy`).
- **Service module path**: `easyops-erp/services/hospital-billing-service`.
- **Integration**: Consumes events from hospital-service, hospital-clinical-orders-service, hospital-pharmacy-service, canteen; publishes billing events to accounting and hospital-portal-bff.

Bounded contexts within the service:

- **Charge context**: Charge lines and rules for grouping into invoices.
- **Invoice context**: Invoices, lifecycle, payer association, totals, discounts, taxes.
- **Payment context**: Payments, refunds, partial payments, multiple instruments.

---

## 4. Data Model (Reference for Implementation)

### 4.1 ChargeLine

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| source_service | VARCHAR(50) | HOSPITAL, PHARMACY, CANTEEN, AMBULANCE, etc. |
| source_reference_id | VARCHAR(255) | visit ID, order ID, pharmacy sale ID, etc. |
| patient_id | UUID | FK reference to external patient |
| visit_id | UUID nullable | FK reference to external visit |
| corporate_contract_id | UUID nullable | |
| item_code | VARCHAR(100) | chargeable item / clinical chart code |
| item_description | VARCHAR(500) nullable | |
| quantity | DECIMAL(19,4) | |
| unit_price | DECIMAL(19,4) | |
| gross_amount | DECIMAL(19,4) | |
| discount_amount | DECIMAL(19,4) DEFAULT 0 | |
| discount_source | VARCHAR(50) nullable | corporate, manual, scheme |
| tax_amount | DECIMAL(19,4) DEFAULT 0 | |
| net_amount | DECIMAL(19,4) | |
| status | VARCHAR(20) | PENDING, POSTED, CANCELLED, REVERSED |
| invoice_id | UUID nullable FK → invoices(id) | |
| idempotency_key | VARCHAR(255) nullable UNIQUE | for event dedup |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |
| created_by | UUID nullable | |

### 4.2 Invoice

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| invoice_number | VARCHAR(50) UNIQUE NOT NULL | facility-specific, human-readable |
| patient_id | UUID NOT NULL | |
| visit_id | UUID nullable | |
| payer_type | VARCHAR(20) | SELF, CORPORATE, MIXED |
| payer_id | UUID nullable | e.g. corporate_contract_id |
| status | VARCHAR(20) | DRAFT, ISSUED, PARTIALLY_PAID, PAID, CANCELLED |
| gross_amount | DECIMAL(19,4) | |
| total_discount | DECIMAL(19,4) DEFAULT 0 | |
| tax_amount | DECIMAL(19,4) DEFAULT 0 | |
| net_amount | DECIMAL(19,4) | |
| balance_due | DECIMAL(19,4) | |
| issued_at | TIMESTAMPTZ nullable | |
| due_date | DATE nullable | |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |
| created_by | UUID nullable | |

### 4.3 Payment

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| invoice_id | UUID NOT NULL FK → invoices(id) | |
| payment_reference | VARCHAR(100) | receipt number, gateway ID |
| payment_method | VARCHAR(50) | CASH, CARD, UPI, BANK_TRANSFER, CARD_BALANCE, CORPORATE_ADJUSTMENT |
| amount | DECIMAL(19,4) | |
| payment_date | TIMESTAMPTZ | |
| status | VARCHAR(20) | COMPLETED, PENDING, FAILED, REVERSED |
| received_by_user_id | UUID nullable | |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

### 4.4 Refund

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| original_payment_id | UUID NOT NULL FK → payments(id) | |
| invoice_id | UUID NOT NULL FK → invoices(id) | |
| amount | DECIMAL(19,4) | |
| reason | TEXT nullable | |
| processed_at | TIMESTAMPTZ | |
| processed_by_user_id | UUID nullable | |
| created_at | TIMESTAMPTZ | |

---

## 5. APIs (Exact Contracts for Implementation)

Base path: **`/api/hospital-billing`** (API Gateway routes to `hospital-billing-service`).

### 5.1 Charge APIs

| Method | Path | Request body / params | Response | Purpose |
|--------|------|------------------------|----------|--------|
| POST | `/charges` | CreateChargeRequest (single or batch) | ChargeResponse or List&lt;ChargeResponse&gt; | Create one or more charge lines |
| GET | `/charges/{id}` | — | ChargeResponse | Get charge by id |
| GET | `/charges` | patientId?, visitId?, status?, sourceService?, from?, to?, page?, size? | PagedResponse&lt;ChargeResponse&gt; | List/search charges |

**CreateChargeRequest** (single line):  
`sourceService`, `sourceReferenceId`, `patientId`, `visitId?`, `corporateContractId?`, `itemCode`, `itemDescription?`, `quantity`, `unitPrice`, `discountAmount?`, `discountSource?`, `taxAmount?`, `idempotencyKey?`

**ChargeResponse**:  
`id`, `sourceService`, `sourceReferenceId`, `patientId`, `visitId`, `corporateContractId`, `itemCode`, `itemDescription`, `quantity`, `unitPrice`, `grossAmount`, `discountAmount`, `discountSource`, `taxAmount`, `netAmount`, `status`, `invoiceId`, `createdAt`, `createdBy`

### 5.2 Invoice APIs

| Method | Path | Request body / params | Response | Purpose |
|--------|------|------------------------|----------|--------|
| POST | `/invoices` | CreateInvoiceRequest | InvoiceResponse | Create invoice from charge line IDs or criteria |
| GET | `/invoices/{id}` | — | InvoiceDetailResponse (invoice + charge lines + payments summary) | Get full invoice |
| GET | `/invoices` | patientId?, visitId?, payerType?, status?, from?, to?, page?, size? | PagedResponse&lt;InvoiceResponse&gt; | Search invoices |
| POST | `/invoices/{id}/issue` | — | InvoiceResponse | DRAFT → ISSUED |
| POST | `/invoices/{id}/cancel` | CancelInvoiceRequest? (reason) | InvoiceResponse | Cancel invoice |

**CreateInvoiceRequest**:  
`patientId`, `visitId?`, `payerType`, `payerId?`, `chargeLineIds` (list of UUID) or `groupBy` (VISIT \| PATIENT \| CORPORATE) + filters, `dueDate?`

**InvoiceResponse**:  
`id`, `invoiceNumber`, `patientId`, `visitId`, `payerType`, `payerId`, `status`, `grossAmount`, `totalDiscount`, `taxAmount`, `netAmount`, `balanceDue`, `issuedAt`, `dueDate`, `createdAt`, `createdBy`

**InvoiceDetailResponse**: extends InvoiceResponse with `chargeLines: ChargeResponse[]`, `paymentsSummary: { totalPaid, lastPaymentAt }`

### 5.3 Payment and Refund APIs (Phase 2)

| Method | Path | Request body / params | Response | Purpose |
|--------|------|------------------------|----------|--------|
| POST | `/invoices/{id}/payments` | CreatePaymentRequest | PaymentResponse | Record payment |
| GET | `/invoices/{id}/payments` | — | List&lt;PaymentResponse&gt; | List payments for invoice |
| GET | `/payments/{id}` | — | PaymentDetailResponse (payment + refunds) | Payment details |
| POST | `/payments/{id}/refunds` | CreateRefundRequest | RefundResponse | Create refund (full or partial) |

**CreatePaymentRequest**:  
`amount`, `paymentMethod`, `paymentReference?`, `paymentDate?` (default now)

**PaymentResponse**:  
`id`, `invoiceId`, `paymentReference`, `paymentMethod`, `amount`, `paymentDate`, `status`, `receivedByUserId`, `createdAt`

**CreateRefundRequest**:  
`amount`, `reason?`

**RefundResponse**:  
`id`, `originalPaymentId`, `invoiceId`, `amount`, `reason`, `processedAt`, `processedByUserId`

### 5.4 Estimate and Discount APIs (Phase 3)

| Method | Path | Request body / params | Response | Purpose |
|--------|------|------------------------|----------|--------|
| POST | `/invoices/estimate` | EstimateRequest (list of charge-like lines) | EstimateResponse | Get discounted/net estimate |
| GET | `/invoices/{id}/discounts` | — | List&lt;DiscountLineResponse&gt; | View applied discounts |

---

## 6. Events and Integrations (Summary)

- **Incoming**: visit.created, visit.discharged, order.*, pharmacy.sale.completed/cancelled, canteen.order.* (consume to create/update/cancel charge lines; use idempotency keys).
- **Outgoing**: billing.charge.created/updated/cancelled, billing.invoice.created/issued/cancelled, billing.payment.received, billing.payment.refunded (for accounting and BFF).

---

## 7. Phased Implementation – Granular Checklists

Implement in order. Each phase is self-contained so that “implement Phase N” can be executed without ambiguity.

---

### Phase 0 – Service Skeleton and Foundations

**Goal**: Deployable `hospital-billing-service` with health, discovery, and API Gateway route; no domain logic yet.

#### 0.1 Maven module

- [ ] Create `easyops-erp/services/hospital-billing-service/pom.xml`.
  - Parent: `com.easyops:easyops-erp:1.0.0`.
  - Artifact: `hospital-billing-service`, packaging `jar`.
  - Dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `spring-cloud-starter-netflix-eureka-client`, `spring-cloud-starter-loadbalancer`, `postgresql` (runtime), `liquibase-core`, `lombok` (optional), `micrometer-registry-prometheus`, `springdoc-openapi-starter-webmvc-ui` (or equivalent OpenAPI). Same pattern as `hospital-pharmacy-service/pom.xml`.

#### 0.2 Application and config

- [ ] Create `easyops-erp/services/hospital-billing-service/src/main/java/com/easyops/hospitalbilling/HospitalBillingServiceApplication.java`.
  - `@SpringBootApplication`, `@EnableJpaAuditing` (if used later).
- [ ] Create `easyops-erp/services/hospital-billing-service/src/main/resources/application.yml`.
  - `spring.application.name: hospital-billing-service`
  - Server port (e.g. 8088, or random).
  - JPA: default schema `hospital_billing`, ddl-auto `none`, hibernate dialect for PostgreSQL.
  - Datasource URL, username, password (placeholders; same DB as hospital-service).
  - Eureka client config (defaultZone, etc.).
  - Actuator: expose health, info, metrics, prometheus.
- [ ] Create `application-dev.yml` / `application-local.yml` if needed (e.g. local DB URL).

#### 0.3 API Gateway

- [ ] In `easyops-erp/services/api-gateway/src/main/resources/application.yml`, add route:
  - id: `hospital-billing-service`
  - uri: `lb://hospital-billing-service`
  - predicates: `Path=/api/hospital-billing/**`

#### 0.4 Database schema (Liquibase in hospital-service)

- [ ] Create `easyops-erp/services/hospital-service/src/main/resources/db/changelog/changesets/012-hospital-billing-schema.sql`.
  - Liquibase formatted SQL.
  - Create schema `hospital_billing` if not exists; grant to app user.
  - (Phase 0 can create empty schema only; Phase 1 will add tables in same file or a new changeset 013.)
- [ ] In `easyops-erp/services/hospital-service/src/main/resources/db/changelog/db.changelog-master.xml`, add:
  - `<include file="changesets/012-hospital-billing-schema.sql" relativeToChangelogFile="true"/>`

#### 0.5 Parent POM

- [ ] In `easyops-erp/pom.xml`, add `<module>services/hospital-billing-service</module>`.

#### 0.6 Health and info

- [ ] Ensure actuator health and info endpoints respond when service runs and DB is available.

**Deliverables**: Service starts, registers with Eureka, and is reachable via API Gateway at `/api/hospital-billing/**`. No domain tables or controllers required in Phase 0 if you prefer to add them in Phase 1; otherwise add a single stub `GET /api/hospital-billing/health` that returns 200.

**Frontend**: None for Phase 0.

---

### Phase 1 – Core Charge and Invoice Foundation

**Goal**: Charge lines and invoices (create, read, list, issue, cancel); draft → issued flow. No payments yet.

#### 1.1 Database (Liquibase)

- [ ] In `easyops-erp/services/hospital-service/src/main/resources/db/changelog/changesets/012-hospital-billing-schema.sql` (or new `013-hospital-billing-charge-invoice.sql`), add:
  - Table `hospital_billing.charge_lines` (all columns from §4.1).
  - Table `hospital_billing.invoices` (all columns from §4.2).
  - Indexes: `charge_lines(patient_id, status, created_at)`, `charge_lines(invoice_id)`, `charge_lines(idempotency_key)`, `invoices(patient_id, status)`, `invoices(invoice_number)`.
  - FK from `charge_lines.invoice_id` to `invoices.id`.

#### 1.2 Backend – Charge context

- [ ] Package: `com.easyops.hospitalbilling.domain.charge`.
- [ ] Entity: `ChargeLine` (map to `hospital_billing.charge_lines`).
- [ ] Repository: `ChargeLineRepository` (JpaRepository); add methods: `findByInvoiceId`, `findByPatientIdAndStatusIn`, `findByIdempotencyKey`.
- [ ] DTOs:
  - Request: `CreateChargeRequest`, `CreateChargeBatchRequest` (list of CreateChargeRequest).
  - Response: `ChargeResponse` (all fields from §5.1).
- [ ] Service: `ChargeService` – create single/batch, get by id, list with filters (paginated); compute gross/discount/tax/net; set status POSTED when attached to invoice.
- [ ] Controller: `ChargeController` – `POST /charges`, `GET /charges/{id}`, `GET /charges` with request params; base path `/api/hospital-billing` (or context path so Gateway forwards correctly).

#### 1.3 Backend – Invoice context

- [ ] Package: `com.easyops.hospitalbilling.domain.invoice`.
- [ ] Entity: `Invoice` (map to `hospital_billing.invoices`).
- [ ] Repository: `InvoiceRepository`; methods: `findByPatientId`, `findByVisitId`, `findByInvoiceNumber`, plus filtered list with pagination.
- [ ] DTOs:
  - Request: `CreateInvoiceRequest` (patientId, visitId, payerType, payerId, chargeLineIds or groupBy + filters, dueDate).
  - Response: `InvoiceResponse`, `InvoiceDetailResponse` (with charge lines and optional payments summary).
- [ ] Service: `InvoiceService` – create from charge line IDs (validate all in PENDING/POSTED, same patient); compute totals; generate `invoice_number` (facility sequence); issue (DRAFT→ISSUED, set issuedAt); cancel (status→CANCELLED, unlink charge lines or leave linked per business rule).
- [ ] Controller: `InvoiceController` – `POST /invoices`, `GET /invoices/{id}`, `GET /invoices`, `POST /invoices/{id}/issue`, `POST /invoices/{id}/cancel`.

#### 1.4 OpenAPI / global error handling

- [ ] Add OpenAPI dependency and config (e.g. springdoc) so `/v3/api-docs` and Swagger UI work.
- [ ] Global exception handler: 404, 400 validation, 409 conflict (e.g. duplicate idempotency key) with consistent JSON body.

#### 1.5 Frontend – Service and types

- [ ] Create `easyops-erp/frontend/src/services/hospitalBillingService.ts`.
  - Use `api` from `./api` (same axios instance as other services).
  - Base URL: `/api/hospital-billing`.
  - Types (mirror backend DTOs): `ChargeLine`, `ChargeResponse`, `CreateChargeRequest`, `Invoice`, `InvoiceResponse`, `InvoiceDetailResponse`, `CreateInvoiceRequest`, `PagedResponse<T>`.
  - Methods: `createCharges`, `getCharge`, `getCharges` (params), `createInvoice`, `getInvoice`, `getInvoices` (params), `issueInvoice`, `cancelInvoice`.

#### 1.6 Frontend – Charges page

- [ ] Create `easyops-erp/frontend/src/pages/hospital/BillingCharges.tsx`.
  - List charges in a table: patient Id, visit Id, source, item code, quantity, unit price, gross, discount, tax, net, status, invoice Id, created at.
  - Filters: patientId, visitId, status, sourceService, date range; optional “Create charge” button that opens a form (single line) calling `createCharges`.
  - Use MUI (Table, TextField, Select, Button, etc.) and existing `Hospital.css` if present.
  - Route: `/hospital/billing/charges`.

#### 1.7 Frontend – Invoices page

- [ ] Create `easyops-erp/frontend/src/pages/hospital/BillingInvoices.tsx`.
  - List invoices: invoice number, patient Id, visit Id, payer type, status, net amount, balance due, issued at, actions (View, Issue, Cancel).
  - Filters: patientId, visitId, status, payerType, date range.
  - “Create invoice” flow: select patient (and optionally visit), select charge line IDs (from pending charges list) or “group by visit/patient”, due date, then POST create; show created invoice.
  - Detail view: show invoice header + charge lines (read-only) + later payments summary (Phase 2).
  - Route: `/hospital/billing/invoices`.
  - Optional: `/hospital/billing/invoices/:id` for detail view (or modal).

#### 1.8 Frontend – Routes and navigation

- [ ] In `easyops-erp/frontend/src/App.tsx`:
  - Import: `BillingChargesPage` (or default export from BillingCharges), `BillingInvoicesPage` from BillingInvoices.
  - Inside the `isModuleEnabled('hospital')` block, add:
    - `<Route path="hospital/billing/charges" element={<BillingChargesPage />} />`
    - `<Route path="hospital/billing/invoices" element={<BillingInvoicesPage />} />`
    - `<Route path="hospital/billing/invoices/:id" element={<BillingInvoiceDetailPage />} />` (if implemented as separate page).
- [ ] In `easyops-erp/frontend/src/components/Layout/MainLayout.tsx`:
  - Under Hospital menu (where Pharmacy items are), add:
    - `{ text: 'Billing – Charges', icon: <ReceiptIcon or similar>, path: '/hospital/billing/charges', permission: { resource: 'hospital', action: 'view' } }`
    - `{ text: 'Billing – Invoices', icon: <ReceiptIcon>, path: '/hospital/billing/invoices', permission: { resource: 'hospital', action: 'view' } }`
  - Add `Receipt` or `Payment` icon import from MUI if not present.

**Deliverables**: User can create charges (single/batch), create invoices from selected charges, issue and cancel invoices; list and filter charges and invoices. Frontend uses `hospitalBillingService` and new routes/nav.

---

### Phase 2 – Payments and Refunds

**Goal**: Record payments against invoices; full and partial refunds; invoice status PARTIALLY_PAID / PAID; balance_due and receipts.

#### 2.1 Database (Liquibase)

- [ ] New file `easyops-erp/services/hospital-service/src/main/resources/db/changelog/changesets/013-hospital-billing-payments.sql` (or append to 012).
  - Table `hospital_billing.payments` (§4.3).
  - Table `hospital_billing.refunds` (§4.4).
  - Indexes: `payments(invoice_id)`, `refunds(original_payment_id)`, `refunds(invoice_id)`.
  - Include in `db.changelog-master.xml`.

#### 2.2 Backend – Payment context

- [ ] Package: `com.easyops.hospitalbilling.domain.payment`.
- [ ] Entities: `Payment`, `Refund`.
- [ ] Repositories: `PaymentRepository`, `RefundRepository`.
- [ ] DTOs: `CreatePaymentRequest`, `PaymentResponse`, `CreateRefundRequest`, `RefundResponse`, `PaymentDetailResponse` (payment + list of refunds).
- [ ] Service: `PaymentService` – create payment (validate invoice exists and is ISSUED; update invoice balance_due and status PARTIALLY_PAID/PAID); list payments by invoice; create refund (validate amount ≤ remaining refundable amount); get payment by id with refunds.
- [ ] Controller: `PaymentController` or extend InvoiceController – `POST /invoices/{id}/payments`, `GET /invoices/{id}/payments`, `GET /payments/{id}`, `POST /payments/{id}/refunds`.

#### 2.3 Invoice balance and status

- [ ] In `InvoiceService`: on payment created, subtract amount from `balance_due`; set status to PAID when balance_due <= 0, else PARTIALLY_PAID. On refund, increase balance_due and possibly revert status.

#### 2.4 Frontend – Service

- [ ] In `hospitalBillingService.ts`: add `createPayment(invoiceId, body)`, `getPayments(invoiceId)`, `getPayment(paymentId)`, `createRefund(paymentId, body)`; types `PaymentResponse`, `RefundResponse`, `CreatePaymentRequest`, `CreateRefundRequest`.

#### 2.5 Frontend – Invoice detail and payments

- [ ] In `BillingInvoices.tsx` (or `BillingInvoiceDetail.tsx`): on invoice detail view, add section “Payments” – table of payments (amount, method, date, status); button “Record payment” opening form (amount, payment method, reference, date); button “Refund” per payment opening form (amount, reason). After payment/refund, refresh invoice and payments list.

#### 2.6 Frontend – Optional payments/receipts page

- [ ] Optional: `BillingPayments.tsx` at `/hospital/billing/payments` – list recent payments (all or by date range) with link to invoice. Add route and nav item if implemented.

**Deliverables**: User can record payments against issued invoices; see balance update and status change; record full/partial refunds; see payments and refunds on invoice detail.

---

### Phase 3 – Corporate and Discount Integration

**Goal**: Estimate API with discount application; optional integration with `hospital-corporate-and-discount-service`; view applied discounts on invoice.

#### 3.1 Backend – Estimate and discounts

- [ ] DTOs: `EstimateRequest` (list of line items: itemCode, quantity, unitPrice, patientId?, corporateContractId?), `EstimateResponse` (per-line and total with discount applied, net payable); `DiscountLineResponse` (description, source, amount).
- [ ] Service: `EstimateService` or extend `InvoiceService` – `computeEstimate(EstimateRequest)`: if `hospital-corporate-and-discount-service` is available, call it for rules and apply discounts; else return estimate with zero discount. `getAppliedDiscounts(invoiceId)`: return list of discount lines for an invoice (from charge lines or invoice-level discounts).
- [ ] Controller: `POST /invoices/estimate`, `GET /invoices/{id}/discounts`.

#### 3.2 Optional: Corporate contract and config

- [ ] If corporate/discount service exists: define REST client or Feign to call it; map response to discount amount and source. If not, stub with zero discount and document “to be wired when corporate service is available”.

#### 3.3 Frontend – Estimate and discounts

- [ ] In `hospitalBillingService.ts`: add `getEstimate(body)`, `getInvoiceDiscounts(invoiceId)`; types `EstimateRequest`, `EstimateResponse`, `DiscountLineResponse`.
- [ ] In Create Invoice or a separate “Estimate” flow: allow user to enter candidate lines (or select pending charges) and call `getEstimate`; show net amount and discount breakdown.
- [ ] On invoice detail: add “Discounts” section calling `getInvoiceDiscounts(id)` and display table.

**Deliverables**: Estimate endpoint returns discounted totals; invoice detail shows applied discounts; frontend can show estimate before creating invoice and show discount breakdown on invoice.

---

### Phase 4 – Advanced Features and Hardening

**Goal**: Write-offs/adjustments with audit; reconciliation and reporting hooks; performance and observability.

#### 4.1 Backend – Write-off and adjustments

- [ ] Optional table: `hospital_billing.adjustments` (id, invoice_id, type: WRITE_OFF|CREDIT|ADJUSTMENT, amount, reason, approved_by, created_at). Liquibase changeset in hospital-service.
- [ ] Service: `AdjustmentService` – create write-off or credit (with optional approval workflow); update invoice balance and status.
- [ ] Controller: `POST /invoices/{id}/adjustments` (body: type, amount, reason).

#### 4.2 Backend – Reporting and reconciliation

- [ ] Endpoints: `GET /reports/outstanding?patientId=&corporateId=&asOf=` (list of invoices with balance_due > 0); `GET /reports/collected?from=&to=` (payments in range). Return simple DTOs for now.
- [ ] Controller: `BillingReportController` or under existing controller.

#### 4.3 Backend – Audit and idempotency

- [ ] Ensure all financial mutations log audit fields (created_by, updated_at). Optional: separate audit log table for invoice/payment/refund changes.
- [ ] Document and enforce idempotency keys on charge creation from events; add reconciliation query by idempotency_key and source_reference_id.

#### 4.4 Frontend – Adjustments and reports

- [ ] Invoice detail: “Adjustments” section and “Add write-off / credit” button if backend supports it.
- [ ] New page `BillingReports.tsx` at `/hospital/billing/reports`: Outstanding (filter by patient/corporate, as-of date), Collected (from–to). Tables with export or print-friendly layout. Add route and nav “Billing – Reports”.

#### 4.5 Observability and performance

- [ ] Add metrics (e.g. billing_charges_created_total, billing_invoices_issued_total, billing_payments_total); ensure correlation IDs are propagated in logs and traces.
- [ ] Indexes and query review for list endpoints (charges, invoices) to keep p95 under 300 ms for typical page sizes.

**Deliverables**: Optional write-offs/adjustments; outstanding and collected reports; audit trail and idempotency documented; frontend reports page; basic metrics and performance checks.

---

## 8. Security, Access Control, and Audit

- All APIs protected by platform auth (JWT/OAuth2); use same pattern as hospital-pharmacy-service.
- Roles: Billing staff (create invoices, record payments, refunds within limits); clinical (view bills/estimates); corporate (view own contracts’ invoices via portal). Implement role checks in controller or service layer when RBAC is available.
- Audit: Immutable audit trail for financial transactions (who, what, when); compliance with hospital policies.

---

## 9. Non-Functional Requirements

- Financial integrity: no double-counting; idempotent event handling; reconciliation reports.
- Performance: write operations < 500 ms p99; read operations < 300 ms for typical lists.
- Availability: 99.9%+; fail-safe and queued capture where applicable.
- Observability: metrics for billed/collected/outstanding; tracing with correlation IDs.

---

## 10. Risks and Mitigations

- **Double charging**: Idempotency keys and reconciliation by source_reference_id and idempotency_key.
- **Tight coupling**: Use neutral charge events and references; keep discount rules in corporate service.
- **Audit non-compliance**: Involve finance early; implement read-only financial history and audit log.

---

## 11. File and Path Quick Reference

| Layer | Path / artifact |
|-------|------------------|
| Service root | `easyops-erp/services/hospital-billing-service/` |
| App class | `.../hospitalbilling/HospitalBillingServiceApplication.java` |
| Config | `.../resources/application.yml` |
| Liquibase (schema) | `easyops-erp/services/hospital-service/.../changelog/changesets/012-hospital-billing-schema.sql`, `013-...-payments.sql` |
| Changelog master | `easyops-erp/services/hospital-service/.../db.changelog-master.xml` |
| Charge | `...hospitalbilling/domain/charge/` (entity, repo, service, controller, DTOs) |
| Invoice | `...hospitalbilling/domain/invoice/` (entity, repo, service, controller, DTOs) |
| Payment | `...hospitalbilling/domain/payment/` (entity, repo, service, controller, DTOs) |
| API Gateway | `easyops-erp/services/api-gateway/src/main/resources/application.yml` |
| Frontend service | `easyops-erp/frontend/src/services/hospitalBillingService.ts` |
| Frontend pages | `easyops-erp/frontend/src/pages/hospital/BillingCharges.tsx`, `BillingInvoices.tsx`, optional `BillingInvoiceDetail.tsx`, `BillingPayments.tsx`, `BillingReports.tsx` |
| Routes | `easyops-erp/frontend/src/App.tsx` (hospital/billing/*) |
| Nav | `easyops-erp/frontend/src/components/Layout/MainLayout.tsx` (Hospital submenu) |

When asking Cursor to “implement Phase N”, point to this document and the corresponding phase section (§7) so that every checkbox (DB, backend, frontend, routes, nav) is implemented without omission.
