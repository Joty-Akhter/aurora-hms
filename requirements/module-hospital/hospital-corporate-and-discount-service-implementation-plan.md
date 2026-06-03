# `hospital-corporate-and-discount-service` – Implementation Plan (Granular)

## 1. Overview and Objectives

`hospital-corporate-and-discount-service` is the consolidated service for managing:

- **Corporate/B2B clients and their contracts.**
- **Coverage rules, packages, and tariffs per corporate.**
- **Discount rules and approval/audit flows** used across hospital services (billing, canteen, pharmacy).

**Primary objectives:**

- Provide a single source of truth for corporate relationships, entitlements, and negotiated pricing.
- Centralize discount and coverage logic so that billing, canteen, pharmacy, and other modules do not duplicate rules.
- Offer clear, auditable decisions on “who gets what discount/coverage and why”.

---

## 1.1 Implementation Status

- **Phase 0 – Service Skeleton and Foundations**: **Not started**
- **Phase 1 – Corporate and Contract Basics**: **Not started**
- **Phase 2 – Coverage Rules, Packages, and Tariffs**: **Not started**
- **Phase 3 – Discount Schemes and Approvals**: **Not started**
- **Phase 4 – Optimization, Analytics, and Policy Enhancements**: **Not started**

---

## 1.2 Phase-wise Implementation Checklist (DB · Backend · Frontend)

| Phase | DB | Backend | Frontend |
|-------|----|---------|----------|
| **Phase 0** – Service skeleton | Schema only (020) | 100% checklist below | 0% |
| **Phase 1** – Corporates & contracts | Tables in 020 | 100% checklist below | 100% checklist below |
| **Phase 2** – Coverage, packages, tariffs | 021 | 100% checklist below | 100% checklist below |
| **Phase 3** – Discount schemes & approvals | 022 | 100% checklist below | 100% checklist below |
| **Phase 4** – Optimization & reporting | Optional 023 | 100% checklist below | 100% checklist below |

Use the per-phase sections below (§7) as the single source of truth when asking Cursor to “implement Phase N”; every file, route, and field is listed so nothing is missed.

---

## 2. Scope and Non-Scope

### 2.1 In-Scope

- **Corporate client management:** Corporate entities (companies, TPAs, government schemes, NGOs); contact details, status, validity periods.
- **Corporate contracts:** Contract definitions tied to corporates; validity dates, service locations, coverage scope (cashless, reimbursement, mixed).
- **Coverage rules and packages:** Package definitions (e.g. health check, surgery packages) and inclusions; coverage percentages/limits per service category or service; co-pay, deductible, max benefit limits.
- **Tariffs per corporate:** Corporate-specific tariffs overriding standard price lists; mapping to service codes, departments, packages.
- **Discount rules and workflows:** General discount schemes (senior citizen, employee, promotional); corporate-specific rules; approval workflows (who can approve which level/amount); audit logging for discount decisions and overrides.

### 2.2 Out-of-Scope (Handled by Other Services)

- **Actual billing calculations and invoice generation** – `hospital-billing-service`.
- **Card issuance and balances** – `hospital-card-management-service`.
- **Core patient clinical data** – `hospital-service`.
- **Canteen/pharmacy item master details** – their respective services.

---

## 3. Architecture and Boundaries

- **Service type:** Stateless Spring Boot REST API with its own schema; no event consumers in Phase 0–3 (optional in Phase 4).
- **Database:** PostgreSQL; schema `hospital_corporate_discount` created and versioned via **hospital-service** Liquibase (same pattern as `hospital_billing`, `hospital_pharmacy`).
- **Service module path:** `easyops-erp/services/hospital-corporate-and-discount-service`.
- **API base path:** `/api/hospital-corporate-discount` (API Gateway routes to this service).
- **Integrations:** Synchronous APIs for billing (and later canteen, pharmacy, portals) to query eligibility, pricing, and discounts; optional event publishing in Phase 4.

Bounded contexts within the service:

- **Corporate context:** Corporate clients and contracts.
- **Coverage and package context:** Coverage rules, package definitions, corporate tariffs.
- **Discount context:** Discount schemes, approval levels, discount decisions (audit).

---

## 4. Data Model (Exact Tables for Implementation)

All tables live in schema `hospital_corporate_discount`. External IDs (`patient_id`, `visit_id`, `bill_context_id`, `service_code`, `department_id`, `user_id`) are stored as UUID/VARCHAR references to other services; no physical FKs to other schemas.

### 4.1 corporate_clients

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| name | VARCHAR(255) NOT NULL | |
| code | VARCHAR(50) UNIQUE NOT NULL | |
| type | VARCHAR(30) NOT NULL | EMPLOYER, INSURER, TPA, GOVT_SCHEME, NGO, OTHER |
| status | VARCHAR(20) NOT NULL | ACTIVE, INACTIVE |
| valid_from | DATE | |
| valid_to | DATE | |
| primary_contact_name | VARCHAR(255) | |
| primary_contact_phone | VARCHAR(50) | |
| primary_contact_email | VARCHAR(255) | |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |
| created_by | UUID | |

### 4.2 corporate_contracts

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| corporate_client_id | UUID NOT NULL FK → corporate_clients(id) | |
| contract_code | VARCHAR(50) NOT NULL | unique per corporate or global |
| contract_name | VARCHAR(255) | |
| valid_from | DATE NOT NULL | |
| valid_to | DATE | |
| coverage_type | VARCHAR(20) NOT NULL | CASHLESS, REIMBURSEMENT, MIXED |
| service_locations | VARCHAR(500) | comma-separated or JSON; optional |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |
| created_by | UUID | |

Indexes: `corporate_client_id`, `(corporate_client_id, contract_code)` UNIQUE, `valid_from`, `valid_to`.

### 4.3 coverage_rules (Phase 2)

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| corporate_contract_id | UUID NOT NULL FK → corporate_contracts(id) | |
| scope_type | VARCHAR(20) NOT NULL | SERVICE_CODE, SERVICE_GROUP, DEPARTMENT |
| scope_value | VARCHAR(100) NOT NULL | service_code, group_id, or department_id |
| coverage_percent | NUMERIC(5,2) NOT NULL | 0–100 |
| max_amount | NUMERIC(19,4) | nullable cap |
| co_pay_percent | NUMERIC(5,2) DEFAULT 0 | |
| deductible_amount | NUMERIC(19,4) DEFAULT 0 | |
| applicable_visit_types | VARCHAR(100) | e.g. OP,IP,ED comma-separated |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

Indexes: `corporate_contract_id`, `(corporate_contract_id, scope_type, scope_value)`.

### 4.4 packages (Phase 2)

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| code | VARCHAR(50) UNIQUE NOT NULL | |
| name | VARCHAR(255) NOT NULL | |
| description | TEXT | |
| default_price | NUMERIC(19,4) | |
| is_corporate_only | BOOLEAN DEFAULT FALSE | |
| is_public | BOOLEAN DEFAULT TRUE | |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

### 4.5 package_items (Phase 2)

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| package_id | UUID NOT NULL FK → packages(id) | |
| item_type | VARCHAR(20) NOT NULL | SERVICE_CODE, SERVICE_GROUP |
| item_code | VARCHAR(100) NOT NULL | |
| quantity_included | NUMERIC(19,4) DEFAULT 1 | |
| created_at | TIMESTAMPTZ | |

Indexes: `package_id`.

### 4.6 corporate_tariffs (Phase 2)

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| corporate_contract_id | UUID NOT NULL FK → corporate_contracts(id) | |
| scope_type | VARCHAR(20) NOT NULL | SERVICE_CODE, SERVICE_GROUP |
| scope_value | VARCHAR(100) NOT NULL | |
| tariff_type | VARCHAR(20) NOT NULL | FIXED, PERCENT_OF_BASE |
| tariff_amount | NUMERIC(19,4) | when FIXED |
| tariff_percent | NUMERIC(5,2) | when PERCENT_OF_BASE |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

Indexes: `corporate_contract_id`, `(corporate_contract_id, scope_type, scope_value)`.

### 4.7 discount_schemes (Phase 3)

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| code | VARCHAR(50) UNIQUE NOT NULL | |
| name | VARCHAR(255) NOT NULL | |
| corporate_client_id | UUID | optional; null = general scheme |
| visit_type | VARCHAR(20) | OP, IP, ED, or null = all |
| department_id | UUID | optional filter |
| service_code | VARCHAR(100) | optional filter |
| patient_category | VARCHAR(50) | optional e.g. SENIOR_CITIZEN, EMPLOYEE |
| discount_type | VARCHAR(20) NOT NULL | PERCENT, AMOUNT |
| discount_value | NUMERIC(19,4) NOT NULL | |
| max_discount_amount | NUMERIC(19,4) | optional cap |
| max_discount_percent | NUMERIC(5,2) | optional cap |
| requires_approval | BOOLEAN DEFAULT FALSE | |
| status | VARCHAR(20) DEFAULT 'ACTIVE' | ACTIVE, INACTIVE |
| valid_from | DATE | |
| valid_to | DATE | |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |
| created_by | UUID | |

Indexes: `code`, `corporate_client_id`, `status`, `valid_from`, `valid_to`.

### 4.8 discount_approval_levels (Phase 3)

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| discount_scheme_id | UUID NOT NULL FK → discount_schemes(id) | |
| role_or_group_id | VARCHAR(100) NOT NULL | role name or user_group_id |
| max_discount_percent | NUMERIC(5,2) | e.g. 10 = up to 10% |
| max_discount_amount | NUMERIC(19,4) | optional amount cap |
| sort_order | INT DEFAULT 0 | for ordering approval chain |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

Indexes: `discount_scheme_id`.

### 4.9 discount_decisions (Phase 3)

| Column | Type | Notes |
|--------|------|--------|
| id | UUID PK | |
| bill_context_id | VARCHAR(255) | reference from billing service |
| patient_id | UUID | |
| corporate_client_id | UUID | optional |
| discount_scheme_id | UUID FK → discount_schemes(id) | optional |
| discount_amount | NUMERIC(19,4) NOT NULL | |
| discount_percent | NUMERIC(5,2) | |
| decided_by_user_id | UUID | |
| approved_by_user_id | UUID | if required |
| created_at | TIMESTAMPTZ | |
| approved_at | TIMESTAMPTZ | |

Indexes: `bill_context_id`, `patient_id`, `created_at`.

---

## 5. APIs (Exact Contracts for Implementation)

Base path: **`/api/hospital-corporate-discount`**. All IDs in paths are UUIDs unless stated otherwise.

### 5.1 Corporate and Contract APIs (Phase 1)

| Method | Path | Request / params | Response | Purpose |
|--------|------|-------------------|----------|--------|
| POST | `/corporates` | CreateCorporateRequest | CorporateResponse | Create corporate client |
| GET | `/corporates/{id}` | — | CorporateResponse | Get corporate by id |
| GET | `/corporates` | code?, type?, status?, page?, size? | PagedResponse&lt;CorporateResponse&gt; | List/search corporates |
| PATCH | `/corporates/{id}` | UpdateCorporateRequest | CorporateResponse | Update corporate |
| POST | `/corporates/{corporateId}/contracts` | CreateContractRequest | ContractResponse | Create contract |
| GET | `/contracts/{id}` | — | ContractResponse | Get contract by id |
| GET | `/corporates/{corporateId}/contracts` | status?, page?, size? | PagedResponse&lt;ContractResponse&gt; | List contracts for corporate |
| PATCH | `/contracts/{id}` | UpdateContractRequest | ContractResponse | Update contract |

**CreateCorporateRequest:**  
`name`, `code`, `type`, `status?`, `validFrom?`, `validTo?`, `primaryContactName?`, `primaryContactPhone?`, `primaryContactEmail?`

**CorporateResponse:**  
`id`, `name`, `code`, `type`, `status`, `validFrom`, `validTo`, `primaryContactName`, `primaryContactPhone`, `primaryContactEmail`, `createdAt`, `updatedAt`

**UpdateCorporateRequest:** same fields as create, all optional (partial update).

**CreateContractRequest:**  
`contractCode`, `contractName`, `validFrom`, `validTo?`, `coverageType`, `serviceLocations?`

**ContractResponse:**  
`id`, `corporateClientId`, `contractCode`, `contractName`, `validFrom`, `validTo`, `coverageType`, `serviceLocations`, `createdAt`, `updatedAt`

**UpdateContractRequest:** same as create, all optional.

**PagedResponse&lt;T&gt;:**  
`content: T[]`, `totalElements`, `totalPages`, `number`, `size`, `first`, `last`

### 5.2 Coverage, Packages, and Tariffs APIs (Phase 2)

| Method | Path | Request / params | Response | Purpose |
|--------|------|-------------------|----------|--------|
| POST | `/contracts/{contractId}/coverage-rules` | CreateCoverageRuleRequest | CoverageRuleResponse | Create coverage rule |
| GET | `/contracts/{contractId}/coverage-rules` | — | List&lt;CoverageRuleResponse&gt; | List coverage rules |
| DELETE | `/coverage-rules/{id}` | — | 204 | Delete coverage rule |
| POST | `/packages` | CreatePackageRequest | PackageResponse | Create package |
| GET | `/packages/{id}` | — | PackageDetailResponse | Get package with items |
| GET | `/packages` | code?, isPublic?, page?, size? | PagedResponse&lt;PackageResponse&gt; | List packages |
| PATCH | `/packages/{id}` | UpdatePackageRequest | PackageResponse | Update package |
| POST | `/packages/{id}/items` | CreatePackageItemRequest | PackageItemResponse | Add package item |
| GET | `/packages/{id}/items` | — | List&lt;PackageItemResponse&gt; | List package items |
| DELETE | `/packages/{packageId}/items/{itemId}` | — | 204 | Remove package item |
| POST | `/contracts/{contractId}/tariffs` | CreateCorporateTariffRequest | CorporateTariffResponse | Create tariff |
| GET | `/contracts/{contractId}/tariffs` | — | List&lt;CorporateTariffResponse&gt; | List tariffs |
| DELETE | `/tariffs/{id}` | — | 204 | Delete tariff |
| POST | `/coverage/evaluate` | EvaluateCoverageRequest | EvaluateCoverageResponse | Evaluate coverage for bill items |

**CreateCoverageRuleRequest:**  
`scopeType`, `scopeValue`, `coveragePercent`, `maxAmount?`, `coPayPercent?`, `deductibleAmount?`, `applicableVisitTypes?` (comma-separated)

**CoverageRuleResponse:**  
`id`, `corporateContractId`, `scopeType`, `scopeValue`, `coveragePercent`, `maxAmount`, `coPayPercent`, `deductibleAmount`, `applicableVisitTypes`, `createdAt`

**CreatePackageRequest:**  
`code`, `name`, `description?`, `defaultPrice?`, `isCorporateOnly?`, `isPublic?`

**PackageResponse:**  
`id`, `code`, `name`, `description`, `defaultPrice`, `isCorporateOnly`, `isPublic`, `createdAt`, `updatedAt`

**PackageDetailResponse:** extends PackageResponse with `items: PackageItemResponse[]`

**CreatePackageItemRequest:**  
`itemType`, `itemCode`, `quantityIncluded?`

**PackageItemResponse:**  
`id`, `packageId`, `itemType`, `itemCode`, `quantityIncluded`, `createdAt`

**CreateCorporateTariffRequest:**  
`scopeType`, `scopeValue`, `tariffType`, `tariffAmount?`, `tariffPercent?`

**CorporateTariffResponse:**  
`id`, `corporateContractId`, `scopeType`, `scopeValue`, `tariffType`, `tariffAmount`, `tariffPercent`, `createdAt`

**EvaluateCoverageRequest:**  
`patientId?`, `visitId?`, `corporateContractId`, `visitType?` (OP/IP/ED), `items: [{ serviceCode, serviceGroupId?, departmentId?, quantity, basePrice }]`

**EvaluateCoverageResponse:**  
`items: [{ lineIndex, serviceCode, coveredPercent, coveredAmount, patientShare, corporateShare, maxApplicable, ruleId? }]`, `totalCovered`, `totalPatientShare`, `totalCorporateShare`

### 5.3 Discount APIs (Phase 3)

| Method | Path | Request / params | Response | Purpose |
|--------|------|-------------------|----------|--------|
| POST | `/discount-schemes` | CreateDiscountSchemeRequest | DiscountSchemeResponse | Create discount scheme |
| GET | `/discount-schemes/{id}` | — | DiscountSchemeDetailResponse | Get scheme with approval levels |
| GET | `/discount-schemes` | code?, corporateClientId?, status?, page?, size? | PagedResponse&lt;DiscountSchemeResponse&gt; | List schemes |
| PATCH | `/discount-schemes/{id}` | UpdateDiscountSchemeRequest | DiscountSchemeResponse | Update scheme |
| POST | `/discount-schemes/{id}/approval-levels` | CreateApprovalLevelRequest | DiscountApprovalLevelResponse | Add approval level |
| GET | `/discount-schemes/{id}/approval-levels` | — | List&lt;DiscountApprovalLevelResponse&gt; | List approval levels |
| DELETE | `/discount-schemes/{schemeId}/approval-levels/{levelId}` | — | 204 | Remove approval level |
| POST | `/discounts/evaluate` | EvaluateDiscountsRequest | EvaluateDiscountsResponse | Get applicable discounts and approval need |
| POST | `/discount-decisions` | CreateDiscountDecisionRequest | DiscountDecisionResponse | Persist discount decision |
| GET | `/discount-decisions/{id}` | — | DiscountDecisionResponse | Get decision and audit |

**CreateDiscountSchemeRequest:**  
`code`, `name`, `corporateClientId?`, `visitType?`, `departmentId?`, `serviceCode?`, `patientCategory?`, `discountType`, `discountValue`, `maxDiscountAmount?`, `maxDiscountPercent?`, `requiresApproval?`, `validFrom?`, `validTo?`

**DiscountSchemeResponse:**  
`id`, `code`, `name`, `corporateClientId`, `visitType`, `departmentId`, `serviceCode`, `patientCategory`, `discountType`, `discountValue`, `maxDiscountAmount`, `maxDiscountPercent`, `requiresApproval`, `status`, `validFrom`, `validTo`, `createdAt`, `updatedAt`

**DiscountSchemeDetailResponse:** extends DiscountSchemeResponse with `approvalLevels: DiscountApprovalLevelResponse[]`

**CreateApprovalLevelRequest:**  
`roleOrGroupId`, `maxDiscountPercent?`, `maxDiscountAmount?`, `sortOrder?`

**DiscountApprovalLevelResponse:**  
`id`, `discountSchemeId`, `roleOrGroupId`, `maxDiscountPercent`, `maxDiscountAmount`, `sortOrder`, `createdAt`

**EvaluateDiscountsRequest:**  
`patientId`, `visitId?`, `corporateClientId?`, `visitType?`, `departmentId?`, `items: [{ serviceCode, quantity, unitPrice, departmentId? }]`, `requestedSchemeId?`, `requestedDiscountPercent?`, `requestedDiscountAmount?`, `reason?`

**EvaluateDiscountsResponse:**  
`applicableSchemes: [{ schemeId, schemeCode, recommendedPercent, recommendedAmount, cappedAmount, requiresApproval, requiredApprovalLevel? }]`, `recommendedTotalDiscount`, `requiresApproval`, `message?`

**CreateDiscountDecisionRequest:**  
`billContextId`, `patientId`, `corporateClientId?`, `discountSchemeId?`, `discountAmount`, `discountPercent?`, `decidedByUserId`, `approvedByUserId?`

**DiscountDecisionResponse:**  
`id`, `billContextId`, `patientId`, `corporateClientId`, `discountSchemeId`, `discountAmount`, `discountPercent`, `decidedByUserId`, `approvedByUserId`, `createdAt`, `approvedAt`

---

## 6. Events and Integrations (Summary)

- **Outgoing (Phase 4 optional):**  
  `corporate.created`, `corporate.updated`, `corporate.deactivated`, `contract.created`, `contract.updated`, `contract.expired`, `coverage-rule.created`, `coverage-rule.updated`, `discount-scheme.created`, `discount-scheme.updated`, `discount-scheme.deactivated`, `discount-decision.created`. Consumers: hospital-billing-service, portals, analytics.
- **Incoming:**  
  Synchronous calls from `hospital-billing-service` for `/coverage/evaluate` and `/discounts/evaluate`; bill context ID used in `POST /discount-decisions`. No event consumers required in Phase 0–3.

---

## 7. Phased Implementation – Granular Checklists

Implement in order. Each phase is self-contained so that “implement Phase N” can be executed without ambiguity.

---

### Phase 0 – Service Skeleton and Foundations

**Goal:** Deployable `hospital-corporate-and-discount-service` with health, discovery, and API Gateway route; no domain logic yet.

#### 0.1 Maven module

- [x] Create `easyops-erp/services/hospital-corporate-and-discount-service/pom.xml`.
  - Parent: `com.easyops:easyops-erp:1.0.0`.
  - Artifact: `hospital-corporate-and-discount-service`, packaging `jar`.
  - Dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `spring-cloud-starter-netflix-eureka-client`, `spring-cloud-starter-loadbalancer`, `postgresql` (runtime), `liquibase-core`, `lombok` (optional), `micrometer-registry-prometheus`, `springdoc-openapi-starter-webmvc-ui` (or equivalent OpenAPI). Same pattern as `hospital-pharmacy-service` / `hospital-billing-service`.

#### 0.2 Application and config

- [x] Create `easyops-erp/services/hospital-corporate-and-discount-service/src/main/java/com/easyops/hospitalcorporatediscount/HospitalCorporateAndDiscountServiceApplication.java`.
  - `@SpringBootApplication`; add `@EnableJpaAuditing` if you use auditing.
- [x] Create `easyops-erp/services/hospital-corporate-and-discount-service/src/main/resources/application.yml`.
  - `spring.application.name: hospital-corporate-and-discount-service`
  - Server port (e.g. 8090 or random).
  - JPA: default schema `hospital_corporate_discount`, ddl-auto `none`, Hibernate dialect for PostgreSQL.
  - Datasource URL, username, password (placeholders; same DB as hospital-service).
  - Eureka client config (defaultZone, etc.).
  - Actuator: expose health, info, metrics, prometheus.
- [x] Create `application-dev.yml` / `application-local.yml` if needed (e.g. local DB URL).

#### 0.3 API Gateway

- [x] In `easyops-erp/services/api-gateway/src/main/resources/application.yml`, add route:
  - id: `hospital-corporate-and-discount-service`
  - uri: `lb://hospital-corporate-and-discount-service`
  - predicates: `Path=/api/hospital-corporate-discount/**`

#### 0.4 Database schema (Liquibase in hospital-service)

- [x] Create `easyops-erp/services/hospital-service/src/main/resources/db/changelog/changesets/020-hospital-corporate-discount-schema.sql`.
  - Liquibase formatted SQL.
  - Create schema `hospital_corporate_discount` if not exists; grant to app user.
  - Phase 0 can create empty schema only; Phase 1 will add tables in same file or in this changeset.
- [x] In `easyops-erp/services/hospital-service/src/main/resources/db/changelog/db.changelog-master.xml`, add:
  - `<include file="changesets/020-hospital-corporate-discount-schema.sql" relativeToChangelogFile="true"/>`

#### 0.5 Parent POM

- [x] In `easyops-erp/pom.xml`, add `<module>services/hospital-corporate-and-discount-service</module>`.

#### 0.6 Health and info

- [x] Ensure actuator health and info endpoints respond when service runs and DB is available.

**Deliverables:** Service starts, registers with Eureka, and is reachable via API Gateway at `/api/hospital-corporate-discount/**`. No domain tables or controllers required in Phase 0 (add in Phase 1).

**Frontend:** None for Phase 0.

#### Phase 0 review (gaps addressed)

- **Checklist (0.1–0.6):** All items verified present (pom, main class, application.yml, gateway route, Liquibase 020 + master include, parent POM, actuator health/info).
- **Port:** Service was on port 8090, conflicting with `hospital-card-management-service`. Changed to **8095** in `application.yml`.
- **Start/stop scripts:** Service was missing from hospital component scripts, so it was never started with `start-hospital-components.sh`/`.ps1`. Added to:
  - `scripts/start-hospital-components.sh` (HOSPITAL_SERVICES, port 8095, wait-for-service, URL list)
  - `scripts/stop-hospital-components.sh` (HOSPITAL_PORTS, HOSPITAL_SERVICES)
  - `scripts/start-hospital-components.ps1` (service list, port switch, URL list)
  - `scripts/stop-hospital-components.ps1` (ports, service list)
- **Docker Compose:** Not required by Phase 0; other hospital services (billing, pharmacy, card-management, clinical-orders) are also not in docker-compose, only in scripts.

---

### Phase 1 – Corporate and Contract Basics

**Goal:** Corporate clients and contracts CRUD; ability to register corporates and set up active contracts with validity; visits can reference `corporate_contract_id`.

#### 1.1 Database (Liquibase)

- [x] In `020-hospital-corporate-discount-schema.sql` (or same file), add:
  - Table `hospital_corporate_discount.corporate_clients` (all columns from §4.1).
  - Table `hospital_corporate_discount.corporate_contracts` (all columns from §4.2).
  - Indexes: `corporate_clients(code)`, `corporate_clients(status)`, `corporate_clients(type)`; `corporate_contracts(corporate_client_id)`, UNIQUE `corporate_contracts(corporate_client_id, contract_code)`, `corporate_contracts(valid_from)`, `corporate_contracts(valid_to)`.
  - FK from `corporate_contracts.corporate_client_id` to `corporate_clients.id`.

#### 1.2 Backend – Corporate context

- [x] Package: `com.easyops.hospitalcorporatediscount.domain.corporate`.
- [x] Entity: `CorporateClient` (map to `hospital_corporate_discount.corporate_clients`).
- [x] Repository: `CorporateClientRepository` (JpaRepository); add methods: `findByCode`, `findByStatus`, `findByType`, and a custom query or Specification for list with filters (code, type, status) and pagination.
- [x] DTOs (in `api/dto` or equivalent):
  - Request: `CreateCorporateRequest`, `UpdateCorporateRequest` (all fields optional).
  - Response: `CorporateResponse` (all fields from §5.1).
- [x] Service: `CorporateClientService` – create, get by id, list with filters (code, type, status, page, size), update (partial).
- [x] Controller: `CorporateController` – base path `/api/hospital-corporate-discount` (or context path so Gateway forwards correctly):
  - `POST /corporates`, `GET /corporates/{id}`, `GET /corporates` (params: code, type, status, page, size), `PATCH /corporates/{id}`.

#### 1.3 Backend – Contract context

- [x] Package: `com.easyops.hospitalcorporatediscount.domain.contract`.
- [x] Entity: `CorporateContract` (map to `hospital_corporate_discount.corporate_contracts`).
- [x] Repository: `CorporateContractRepository`; methods: `findByCorporateClientId`, `findById`, and list by corporate with optional status filter and pagination.
- [x] DTOs: `CreateContractRequest`, `UpdateContractRequest`, `ContractResponse` (all fields from §5.1).
- [x] Service: `CorporateContractService` – create (validate corporate exists), get by id, list by corporateClientId (status, page, size), update (partial).
- [x] Controller: `ContractController` (or under CorporateController):
  - `POST /corporates/{corporateId}/contracts`, `GET /contracts/{id}`, `GET /corporates/{corporateId}/contracts` (params: status, page, size), `PATCH /contracts/{id}`.

#### 1.4 Shared – PagedResponse and global error handling

- [x] Add shared DTO `PagedResponse<T>`: `content`, `totalElements`, `totalPages`, `number`, `size`, `first`, `last`.
- [x] Global exception handler: 404, 400 validation, 409 conflict (e.g. duplicate code) with consistent JSON body.
- [x] OpenAPI dependency and config so `/v3/api-docs` and Swagger UI work.

#### 1.5 Frontend – Service and types

- [x] Create `easyops-erp/frontend/src/services/hospitalCorporateDiscountService.ts`.
  - Use `api` from `./api` (same axios instance as other services).
  - Base URL: `/api/hospital-corporate-discount`.
  - Types: `CorporateClient`, `CorporateResponse`, `CreateCorporateRequest`, `UpdateCorporateRequest`, `CorporateContract`, `ContractResponse`, `CreateContractRequest`, `UpdateContractRequest`, `PagedResponse<T>`.
  - Methods: `createCorporate`, `getCorporate`, `getCorporates` (params), `updateCorporate`, `createContract`, `getContract`, `getContractsByCorporate`, `updateContract`.

#### 1.6 Frontend – Corporates page

- [x] Create `easyops-erp/frontend/src/pages/hospital/CorporateClients.tsx`.
  - List corporates in a table: code, name, type, status, valid from/to, primary contact, actions (View, Edit, Manage contracts).
  - Filters: code, type, status; pagination.
  - “Create corporate” button → form (all fields from CreateCorporateRequest) → call `createCorporate`.
  - Edit: open form with existing data → `updateCorporate`.
  - “Manage contracts” navigates to contracts list for that corporate (or inline expand).
  - Route: `/hospital/corporate-discount/corporates`.

#### 1.7 Frontend – Contracts page

- [x] Create `easyops-erp/frontend/src/pages/hospital/CorporateContracts.tsx`.
  - When opened from “Manage contracts”, show contracts for selected corporate; or standalone list with corporate name column.
  - Table: contract code, name, corporate (name/code), valid from/to, coverage type, actions (View, Edit).
  - “Create contract” (requires corporate Id or selection) → form → `createContract`.
  - Edit → `updateContract`.
  - Route: `/hospital/corporate-discount/contracts` or `/hospital/corporate-discount/corporates/:corporateId/contracts`.

#### 1.8 Frontend – Routes and navigation

- [x] In `easyops-erp/frontend/src/App.tsx`:
  - Import `CorporateClientsPage` (or default from CorporateClients), `CorporateContractsPage` from CorporateContracts.
  - Inside the `isModuleEnabled('hospital')` block, add:
    - `<Route path="hospital/corporate-discount/corporates" element={<CorporateClientsPage />} />`
    - `<Route path="hospital/corporate-discount/contracts" element={<CorporateContractsPage />} />`
    - If using nested route: `<Route path="hospital/corporate-discount/corporates/:corporateId/contracts" element={<CorporateContractsPage />} />`
- [x] In `easyops-erp/frontend/src/components/Layout/MainLayout.tsx`:
  - Under Hospital menu, add:
    - `{ text: 'Corporate & Discount – Corporates', icon: <BusinessIcon or similar>, path: '/hospital/corporate-discount/corporates', permission: { resource: 'hospital', action: 'view' } }`
    - `{ text: 'Corporate & Discount – Contracts', icon: <DescriptionIcon>, path: '/hospital/corporate-discount/contracts', permission: { resource: 'hospital', action: 'view' } }`
  - Add icon imports from MUI if not present.

**Deliverables:** User can create/read/update corporates and contracts; list and filter both; frontend uses `hospitalCorporateDiscountService` and new routes/nav.

#### Phase 1 review (gaps addressed)

- **Checklist (1.1–1.8):** All items verified present (DB schema, corporate/contract backend, PagedResponse, error handler, OpenAPI, frontend service, Corporates page, Contracts page, routes and nav).
- **API alignment:** Backend and §5.1 match (paths, request/response fields, PagedResponse shape). Frontend uses same axios instance via `api` from `./api`, base `/api/hospital-corporate-discount`.
- **Gap fixed:** Contracts page did not pass optional `status` param to `getContractsByCorporate`. Added Status filter (All / Active / Expired / Future) and pass `status` when set; backend already supported it via `ContractSpecifications.hasEffectiveStatus`.
- **Optional (not required for Phase 1):** `created_by` is not set from auth context on create/update; backend does not validate type/status/coverageType enums (frontend constrains via dropdowns).

---

### Phase 2 – Coverage Rules, Packages, and Tariffs

**Goal:** Coverage rules and corporate tariffs; package definitions and package items; `POST /coverage/evaluate` for billing to consult at bill creation; cashless/partially cashless flows.

#### 2.1 Database (Liquibase)

- [x] Create `easyops-erp/services/hospital-service/src/main/resources/db/changelog/changesets/021-hospital-corporate-discount-coverage-tariffs.sql`.
  - Tables: `coverage_rules` (§4.3), `packages` (§4.4), `package_items` (§4.5), `corporate_tariffs` (§4.6).
  - All indexes from §4.3–4.6.
  - FKs: coverage_rules → corporate_contracts; package_items → packages; corporate_tariffs → corporate_contracts.
- [x] In `db.changelog-master.xml`, add include for `021-hospital-corporate-discount-coverage-tariffs.sql`.

#### 2.2 Backend – Coverage context

- [x] Package: `com.easyops.hospitalcorporatediscount.domain.coverage`.
- [x] Entities: `CoverageRule` (map to `coverage_rules`).
- [x] Repository: `CoverageRuleRepository` – `findByCorporateContractId`, `deleteById`.
- [x] DTOs: `CreateCoverageRuleRequest`, `CoverageRuleResponse`.
- [x] Service: `CoverageRuleService` – create (validate contract exists), list by contractId, delete.
- [x] Controller: `CoverageRuleController` (or under ContractController): `POST /contracts/{contractId}/coverage-rules`, `GET /contracts/{contractId}/coverage-rules`, `DELETE /coverage-rules/{id}`.

#### 2.3 Backend – Package context

- [x] Package: `com.easyops.hospitalcorporatediscount.domain.packagedefinition` (do not use `package` – it is a Java reserved word).
- [x] Entities: `Package` (map to `packages`), `PackageItem` (map to `package_items`).
- [x] Repositories: `PackageRepository` (findByCode, list with filters), `PackageItemRepository` (findByPackageId).
- [x] DTOs: `CreatePackageRequest`, `UpdatePackageRequest`, `PackageResponse`, `PackageDetailResponse` (with items), `CreatePackageItemRequest`, `PackageItemResponse`.
- [x] Service: `PackageService` – create package, get by id (with items), list (code, isPublic, page, size), update package; add item, list items, delete item.
- [x] Controller: `PackageController`: `POST /packages`, `GET /packages/{id}`, `GET /packages`, `PATCH /packages/{id}`, `POST /packages/{id}/items`, `GET /packages/{id}/items`, `DELETE /packages/{packageId}/items/{itemId}`.

#### 2.4 Backend – Tariff context

- [x] Package: `com.easyops.hospitalcorporatediscount.domain.tariff`.
- [x] Entity: `CorporateTariff` (map to `corporate_tariffs`).
- [x] Repository: `CorporateTariffRepository` – `findByCorporateContractId`, `deleteById`.
- [x] DTOs: `CreateCorporateTariffRequest`, `CorporateTariffResponse`.
- [x] Service: `CorporateTariffService` – create (validate contract exists), list by contractId, delete.
- [x] Controller: `CorporateTariffController` (or under ContractController): `POST /contracts/{contractId}/tariffs`, `GET /contracts/{contractId}/tariffs`, `DELETE /tariffs/{id}`.

#### 2.5 Backend – Coverage evaluation

- [x] DTOs: `EvaluateCoverageRequest` (patientId?, visitId?, corporateContractId, visitType?, items[] with serviceCode, serviceGroupId?, departmentId?, quantity, basePrice), `EvaluateCoverageResponse` (items[] with lineIndex, serviceCode, coveredPercent, coveredAmount, patientShare, corporateShare, maxApplicable, ruleId?; totals totalCovered, totalPatientShare, totalCorporateShare).
- [x] Service: `CoverageEvaluationService` – load contract and its coverage rules; for each request item, resolve matching rule (by service_code, then service_group, then department; consider applicable_visit_types); compute covered amount/percent, co-pay, deductible; aggregate totals.
- [x] Controller: `CoverageEvaluationController` or add to existing: `POST /coverage/evaluate` → `EvaluateCoverageResponse`.

#### 2.6 Frontend – Service

- [x] In `hospitalCorporateDiscountService.ts`: add types and methods for coverage rules, packages, package items, tariffs, and evaluate coverage:
  - `createCoverageRule(contractId, body)`, `getCoverageRules(contractId)`, `deleteCoverageRule(id)`
  - `createPackage(body)`, `getPackage(id)`, `getPackages(params)`, `updatePackage(id, body)`, `addPackageItem(packageId, body)`, `getPackageItems(packageId)`, `deletePackageItem(packageId, itemId)`
  - `createTariff(contractId, body)`, `getTariffs(contractId)`, `deleteTariff(id)`
  - `evaluateCoverage(body)` → `EvaluateCoverageResponse`

#### 2.7 Frontend – Coverage and tariffs UI

- [x] On contract detail or a dedicated “Coverage & tariffs” view for a contract:
  - List coverage rules: scope type/value, coverage %, max amount, co-pay, deductible, visit types; Add rule form; Delete.
  - List tariffs: scope type/value, tariff type, amount or percent; Add tariff form; Delete.
- [x] Optional: `ContractCoverage.tsx` at `/hospital/corporate-discount/contracts/:contractId/coverage`; route in App.tsx; "Coverage & tariffs" in contracts table and view dialog.

#### 2.8 Frontend – Packages UI

- [x] Create `easyops-erp/frontend/src/pages/hospital/Packages.tsx`.
  - List packages: code, name, default price, is public, is corporate only; actions (View, Edit, Manage items).
  - Filters: code, isPublic; pagination.
  - Create package form; Edit package.
  - Package detail: `PackageDetail.tsx` at `/hospital/corporate-discount/packages/:packageId` – list package items (item type, item code, quantity); Add item; Remove item.
  - Route: `/hospital/corporate-discount/packages`.
- [x] Add route in `App.tsx` and nav item “Corporate & Discount – Packages” in `MainLayout.tsx`.

**Deliverables:** Coverage rules and tariffs CRUD per contract; packages and package items CRUD; `POST /coverage/evaluate` implemented and callable by billing; frontend can manage rules, tariffs, and packages.

---

### Phase 3 – Discount Schemes and Approvals

**Goal:** Discount schemes with optional approval levels; `POST /discounts/evaluate` and `POST /discount-decisions`; billing UI can request and approve discounts; role-based approval chains; unified discount engine for OPD/IPD billing (and later canteen, pharmacy).

#### 3.1 Database (Liquibase)

- [x] Create `easyops-erp/services/hospital-service/src/main/resources/db/changelog/changesets/022-hospital-corporate-discount-discount-schemes.sql`.
  - Tables: `discount_schemes` (§4.7), `discount_approval_levels` (§4.8), `discount_decisions` (§4.9).
  - Indexes as in §4.7–4.9.
  - FK: discount_approval_levels → discount_schemes; discount_decisions.discount_scheme_id → discount_schemes (optional); discount_schemes.corporate_client_id → corporate_clients (optional).
- [x] In `db.changelog-master.xml`, add include for `022-hospital-corporate-discount-discount-schemes.sql`.

#### 3.2 Backend – Discount scheme context

- [x] Package: `com.easyops.hospitalcorporatediscount.domain.discount`.
- [x] Entities: `DiscountScheme`, `DiscountApprovalLevel`, `DiscountDecision` (map to tables above).
- [x] Repositories: `DiscountSchemeRepository` (findByCode, existsByCode, JpaSpecificationExecutor for code/corporateClientId/status), `DiscountApprovalLevelRepository` (findByDiscountSchemeIdOrderBySortOrderAsc, existsByDiscountSchemeIdAndId), `DiscountDecisionRepository` (findByBillContextId, findByPatientIdOrderByCreatedAtDesc, findById).
- [x] DTOs: `CreateDiscountSchemeRequest`, `UpdateDiscountSchemeRequest`, `DiscountSchemeResponse`, `DiscountSchemeDetailResponse` (with approvalLevels); `CreateApprovalLevelRequest`, `DiscountApprovalLevelResponse`; `CreateDiscountDecisionRequest`, `DiscountDecisionResponse`.
- [x] Service: `DiscountSchemeService` – create scheme, get by id (with levels), list with filters, update scheme; add approval level, list levels, delete level.
- [x] Service: `DiscountDecisionService` – create decision (persist for audit), get by id.
- [x] Controller: `DiscountSchemeController`: `POST /discount-schemes`, `GET /discount-schemes/{id}`, `GET /discount-schemes`, `PATCH /discount-schemes/{id}`, `POST /discount-schemes/{id}/approval-levels`, `GET /discount-schemes/{id}/approval-levels`, `DELETE /discount-schemes/{schemeId}/approval-levels/{levelId}`.
- [x] Controller: `DiscountDecisionController`: `POST /discount-decisions`, `GET /discount-decisions/{id}`.

#### 3.3 Backend – Discount evaluation

- [x] DTOs: `EvaluateDiscountsRequest` (patientId, visitId?, corporateClientId?, visitType?, departmentId?, items[], requestedSchemeId?, requestedDiscountPercent?, requestedDiscountAmount?, reason?), `EvaluateDiscountsItemRequest` (serviceCode, quantity, unitPrice, departmentId?), `EvaluateDiscountsResponse` (applicableSchemes[], recommendedTotalDiscount, requiresApproval, message?), `ApplicableSchemeDto` (schemeId, schemeCode, recommendedPercent, recommendedAmount, cappedAmount, requiresApproval, requiredApprovalLevel?).
- [x] Service: `DiscountEvaluationService` – load active schemes (specs: activeAndValidNow, corporateOrGeneral, visitTypeMatches, departmentMatches); filter by serviceCode and patientCategory in memory; for each scheme compute recommended discount (PERCENT/AMOUNT) with max_discount_amount and max_discount_percent caps; set requiredApprovalLevel from first approval level when requiresApproval; recommendedTotalDiscount = max(cappedAmount).
- [x] Controller: `DiscountEvaluationController` – `POST /discounts/evaluate` → `EvaluateDiscountsResponse`.

#### 3.4 Frontend – Service

- [x] In `hospitalCorporateDiscountService.ts`: add types and methods for discount schemes, approval levels, evaluate, and decisions:
  - `createDiscountScheme(body)`, `getDiscountScheme(id)`, `getDiscountSchemes(params)`, `updateDiscountScheme(id, body)`, `addApprovalLevel(schemeId, body)`, `getApprovalLevels(schemeId)`, `deleteApprovalLevel(schemeId, levelId)`
  - `evaluateDiscounts(body)`, `createDiscountDecision(body)`, `getDiscountDecision(id)`

#### 3.5 Frontend – Discount schemes page

- [x] Create `easyops-erp/frontend/src/pages/hospital/DiscountSchemes.tsx`.
  - List schemes: code, name, corporate, discount type/value, requires approval, status, valid from/to; actions (View, Edit, Manage approval levels).
  - Filters: code, corporateClientId, status; Create scheme form; Edit scheme.
  - Scheme detail: `SchemeApprovalLevels.tsx` at `/hospital/corporate-discount/discount-schemes/:schemeId` – list approval levels (role/group, max percent/amount, sort order); Add level; Remove level.
  - Route: `/hospital/corporate-discount/discount-schemes`.
- [x] Add route and nav item “Corporate & Discount – Discount schemes” in `App.tsx` and `MainLayout.tsx`.

#### 3.6 Frontend – Discount decisions (audit view)

- [x] Optional: `DiscountDecisions.tsx` at `/hospital/corporate-discount/decisions` – list recent discount decisions (bill context, patient, scheme, amount, decided by, approved by, dates). Backend: `GET /discount-decisions` (page, size) added; frontend `getDiscountDecisions(params)`; route and nav "Corporate & Discount – Decisions" added.

**Deliverables:** Discount schemes and approval levels CRUD; `POST /discounts/evaluate` and `POST /discount-decisions` implemented; frontend can manage schemes and view decisions; billing service can call evaluate and persist decisions.

---

### Phase 4 – Optimization, Analytics, and Policy Enhancements

**Goal:** Optional event publishing; reporting (corporate utilization, discount leakage); caching for low-latency evaluate calls; optional rule enhancements (time-bound promotions, tiered discounts).

#### 4.1 Backend – Event publishing (optional)

- [x] Event payloads and types: `EventTypes` constants and `CorporateDiscountEventPublisher.publish(type, payload)`. Events: corporate.created/updated/deactivated, contract.created/updated, coverage-rule.created/deleted, discount-scheme.created/updated/deactivated, discount-decision.created.
- [x] After each create/update/delete in CorporateClientService, ContractService, CoverageRuleService, DiscountSchemeService, DiscountDecisionService: publish corresponding event. Default `LoggingCorporateDiscountEventPublisher` (no-op except logging); when Kafka/RabbitMQ is in use, a custom bean can implement the interface.
- [x] Consumer contracts documented in `services/hospital-corporate-and-discount-service/docs/EVENTS.md` for hospital-billing-service and portals.

#### 4.2 Backend – Reporting

- [x] Endpoints (simple DTOs): `CorporateUtilizationResponse`, `DiscountSummaryResponse` (with single / byCorporate / byScheme).
  - `GET /api/hospital-corporate-discount/reports/corporate-utilization?corporateId=&from=&to=` – count of discount decisions per corporate in period; corporateId optional (single vs list by corporate).
  - `GET /api/hospital-corporate-discount/reports/discount-summary?from=&to=&schemeId=` – total discount amount and decision count by scheme; schemeId optional.
- [x] Controller: `CorporateDiscountReportController`; service `CorporateDiscountReportService`; repository methods on `DiscountDecisionRepository` for counts and sums by date range.

#### 4.3 Backend – Caching and performance

- [x] Cache active coverage rules and discount schemes (e.g. Caffeine or Redis) keyed by contractId/schemeId; TTL short (e.g. 60s) or invalidate on update.
- [x] Ensure `/coverage/evaluate` and `/discounts/evaluate` p95 < 300–400 ms for typical bill sizes; add indexes if needed and document.

#### 4.4 Backend – Rule enhancements (optional)

- [x] If required: extend discount_schemes or coverage_rules with time-bound validity (already have valid_from/valid_to); tiered discounts (e.g. second tier table or JSON column). Document in API and DTOs.

#### 4.5 Frontend – Reports

- [x] New page `CorporateDiscountReports.tsx` at `/hospital/corporate-discount/reports`: Corporate utilization (select corporate, date range; show table or chart); Discount summary (date range, optional scheme; show totals). Add route and nav “Corporate & Discount – Reports”.

**Deliverables:** Optional events; utilization and discount summary reports; caching and performance tuning; frontend reports page; optional rule enhancements documented and implemented.

---

## 8. Security, Access Control, and Audit

- All APIs protected by platform auth (JWT/OAuth2); same pattern as hospital-pharmacy-service and hospital-billing-service.
- RBAC: Strict control for creating/modifying corporates, contracts, coverage rules, tariffs, discount schemes; read access separation between operational users and financial admins.
- Audit: Every discount decision and coverage/discount evaluation result traceable to rules and approvers; immutable audit trail for discount decisions.

---

## 9. Non-Functional Requirements

- **Performance:** Coverage/discount evaluation p95 < 300–400 ms for typical bill sizes.
- **Availability:** 99.9%+; downtime directly impacts billing and check-out flows.
- **Auditability:** Every discount and coverage decision traceable to rules and approvers.
- **Security:** RBAC for contracts, coverage, and discount scheme changes.

---

## 10. Risks and Mitigations

- **Overly complex rule engine:** Start with simple rule types; add complexity gradually based on real needs.
- **Misconfigured rules leading to financial loss:** Approval workflows for rule changes; simulation tools and monitoring in Phase 4.
- **Tight coupling with billing:** This service only returns **decisions**; billing owns line items and calculations; communicate via well-defined evaluate APIs.

---

## 11. Deployment and Migration (Summary)

- Initial deployment: Start with a small subset of key corporates; mirror existing contracts and validate side-by-side.
- Migration: Import corporate master and contract data from legacy systems; clean up duplicate or inconsistent codes.

---

## 12. File and Path Quick Reference

| Layer | Path / artifact |
|-------|------------------|
| Service root | `easyops-erp/services/hospital-corporate-and-discount-service/` |
| App class | `.../hospitalcorporatediscount/HospitalCorporateAndDiscountServiceApplication.java` |
| Config | `.../resources/application.yml` |
| Liquibase Phase 0+1 | `easyops-erp/services/hospital-service/.../changelog/changesets/020-hospital-corporate-discount-schema.sql` |
| Liquibase Phase 2 | `.../changesets/021-hospital-corporate-discount-coverage-tariffs.sql` |
| Liquibase Phase 3 | `.../changesets/022-hospital-corporate-discount-discount-schemes.sql` |
| Changelog master | `easyops-erp/services/hospital-service/.../db.changelog-master.xml` |
| Corporate | `...hospitalcorporatediscount/domain/corporate/` (entity, repo, service, controller, DTOs) |
| Contract | `...hospitalcorporatediscount/domain/contract/` |
| Coverage | `...hospitalcorporatediscount/domain/coverage/` |
| Package | `...hospitalcorporatediscount/domain/packagedefinition/` |
| Tariff | `...hospitalcorporatediscount/domain/tariff/` |
| Discount | `...hospitalcorporatediscount/domain/discount/` |
| Evaluation | `...hospitalcorporatediscount/domain/evaluation/` or under coverage/discount (CoverageEvaluationService, DiscountEvaluationService) |
| API Gateway | `easyops-erp/services/api-gateway/src/main/resources/application.yml` |
| Frontend service | `easyops-erp/frontend/src/services/hospitalCorporateDiscountService.ts` |
| Frontend pages | `CorporateClients.tsx`, `CorporateContracts.tsx`, `Packages.tsx`, `DiscountSchemes.tsx`, optional `CoverageRulesPage`, `DiscountDecisions.tsx`, `CorporateDiscountReports.tsx` |
| Routes | `easyops-erp/frontend/src/App.tsx` (hospital/corporate-discount/*) |
| Nav | `easyops-erp/frontend/src/components/Layout/MainLayout.tsx` (Hospital submenu) |

When asking Cursor to “implement Phase N”, point to this document and the corresponding phase section (§7) so that every checkbox (DB, backend, frontend, routes, nav) is implemented without omission.
