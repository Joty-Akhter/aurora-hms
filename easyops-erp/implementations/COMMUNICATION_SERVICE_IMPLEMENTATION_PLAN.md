# Communication Service Implementation Plan

## Objective

Design and implement a standalone **Communication Service** for EasyOps ERP to handle SMS, Email, and future channels (WhatsApp, Push, Webhook, In-app) with:

- reusable templates,
- pluggable providers,
- channel routing policies,
- async delivery with retries,
- delivery tracking/audit,
- consent and preference enforcement.

This service should decouple communication concerns from domain services such as `hospital-service`, `hospital-scheduling-service`, and `billing` modules.

---

## Scope

### In Scope (v1)
- SMS + Email channels.
- Event-driven send flow (primary) and API-trigger flow (secondary).
- Template management with versioning and locale.
- Provider abstraction and at least 1 provider per channel.
- Retry, DLQ, idempotency, and delivery status tracking.
- Core consent/preference checks.
- Initial use cases:
  - Appointment confirmation (SMS first, email optional).
  - Appointment cancellation/reschedule notification.
  - Billing invoice created notification (email first).

### Out of Scope (v1)
- Full multi-tenant campaign manager.
- Rich drag-drop template builder UI.
- Omnichannel fallback orchestration with advanced AI routing.
- Full marketing automation journeys.

---

## Current-State Assessment

### Existing Assets in Repo
- `notification-service` exists (email/webhook oriented).
- `hospital-service` has `PonditSmsService` for direct SMS.
- Domain events exist in places via `DomainEventPublisher`.
- Scheduling has appointment events in `hospital-scheduling-service`.

### Key Gaps
- No centralized template/version system across channels.
- SMS/email logic is not unified.
- No single delivery audit timeline across channels/providers.
- Notification logic is embedded in domain services (tight coupling).

---

## Target Architecture

### High-Level Flow
1. Domain service publishes event (e.g., `appointment.created`).
2. Communication service consumes event from message broker.
3. Policy engine decides channel(s), template, and provider.
4. Template renderer builds final message from variables.
5. Provider adapter sends message.
6. Delivery status is persisted and updated (`QUEUED -> SENT -> DELIVERED/FAILED`).
7. Failure paths use retry and DLQ.

### Core Components
- **Event Ingestion**
  - Kafka/Rabbit consumer(s) for domain events.
- **Notification Orchestrator**
  - Maps event -> notification rule -> channel task(s).
- **Template Service**
  - CRUD templates, versioning, compile/validate placeholders.
- **Renderer**
  - Renders SMS/text and Email subject/body.
- **Provider Gateway**
  - SMS providers (Pondit first, Twilio optional).
  - Email providers (SMTP first, SES/SendGrid optional).
- **Delivery Engine**
  - Async queue processing, retry/backoff, DLQ.
- **Consent & Preference Guard**
  - Checks patient/user consent + channel preference + quiet hours.
- **Audit & Tracking**
  - Delivery attempts, provider response, correlation IDs.
- **Admin API**
  - Template preview, test send, provider health, resend failed.

---

## Data Model (Proposed)

### 1) `comm_template`
- `id`
- `template_key` (e.g., `appointment.confirmation`)
- `channel` (`SMS`, `EMAIL`, ...)
- `locale` (`en`, `bn-BD`)
- `version` (int)
- `status` (`DRAFT`, `ACTIVE`, `ARCHIVED`)
- `subject_template` (nullable for SMS)
- `body_template`
- `variables_schema` (JSON)
- `created_by`, `created_at`, `updated_at`

### 2) `comm_notification`
- `id`
- `tenant_id` / `organization_id`
- `event_type`
- `entity_type`, `entity_id`
- `recipient_type`, `recipient_id`
- `correlation_id` (trace across services)
- `idempotency_key`
- `status` (`QUEUED`, `PROCESSING`, `PARTIAL`, `COMPLETED`, `FAILED`)
- `created_at`, `updated_at`

### 3) `comm_delivery`
- `id`
- `notification_id`
- `channel`
- `provider`
- `recipient_address` (masked for logs)
- `template_key`, `template_version`
- `rendered_subject` (nullable)
- `rendered_body` (optional retention config)
- `status` (`QUEUED`, `SENT`, `DELIVERED`, `FAILED`, `SKIPPED`)
- `failure_reason`
- `attempt_count`
- `provider_message_id`
- `provider_response`
- `next_retry_at`
- `sent_at`, `delivered_at`

### 4) `comm_preference` (optional in v1, can proxy existing patient/user)
- `recipient_id`
- `channel`
- `enabled`
- `quiet_hours_from`, `quiet_hours_to`
- `locale`

---

## API Design (Service)

### Internal API
- `POST /api/communications/dispatch`
  - For direct-trigger notifications (fallback/manual).
- `GET /api/communications/{notificationId}`
  - Aggregate status and delivery timeline.
- `POST /api/communications/{notificationId}/retry`
  - Manual retry for failed notifications.

### Template API
- `GET /api/communication-templates`
- `POST /api/communication-templates`
- `PUT /api/communication-templates/{id}`
- `POST /api/communication-templates/{id}/activate`
- `POST /api/communication-templates/preview`

### Provider API (Admin)
- `GET /api/communication-providers/health`
- `PUT /api/communication-providers/config` (secured admin only)

---

## Event Contract Strategy

### Recommended Event Payload Standards
- `eventId`
- `eventType`
- `eventVersion`
- `occurredAt`
- `organizationId`
- `entityId`
- `actorId`
- `correlationId`
- `payload` (event-specific fields)

### First Event Contracts
1. `appointment.created.v1`
   - patientId, doctorId, appointmentDate, slotStart, slotEnd, tokenNumber, chamber, location
2. `appointment.updated.v1`
   - include status and slot changes for reschedule/cancel scenarios
3. `invoice.created.v1`
   - patientId/customerId, invoiceNumber, amount, dueDate, link

---

## Phased Implementation Plan

## Phase 0 - Discovery and Foundation (Implemented)

### Decisions Finalized
- **Broker strategy:** Kafka is the primary event transport for communication events in v1.
  - Rationale: existing producer patterns already use `KafkaTemplate` in multiple services, reducing rollout risk and platform variance.
  - Retry/DLQ model: consumer-level retry with exponential backoff + DLQ topic per event domain.
- **Service boundary:** Introduce a new `communication-service` module; keep existing `notification-service` running for legacy flows during migration.
  - Ownership: Communication Service Team owns orchestration, templates, providers, and delivery audit model.
  - Migration: domain services move to event publishing + communication-service consumption; legacy direct notification paths remain feature-flagged until Phase 4 cutover.
- **Template rendering engine:** Start with Mustache-compatible rendering (logic-light, deterministic placeholders) and strict variable validation against `variables_schema`.

### Canonical Event Schema Convention (v1)
All communication-triggering events must include the following envelope:
- `eventId` (UUID)
- `eventType` (versioned name, e.g., `appointment.created.v1`)
- `eventVersion` (integer)
- `occurredAt` (ISO-8601 UTC instant)
- `organizationId` (UUID)
- `entityId` (UUID/string by domain)
- `actorId` (nullable UUID/system identifier)
- `correlationId` (request/workflow trace id)
- `idempotencyKey` (producer-supplied; fallback generated by consumer policy)
- `payload` (event-specific data only)

Conventions:
- Event type naming: `<domain>.<action>.v<major>` (e.g., `invoice.created.v1`).
- Backward compatibility: additive payload changes only within same major version.
- Required metadata headers on publish: `eventType`, `eventVersion`, `correlationId`, `organizationId`.

### v1 Use Cases and Acceptance Criteria (Final)
1. **Appointment confirmation (SMS primary, email optional)**
   - Given `appointment.created.v1`, when recipient has SMS consent and valid mobile, then SMS delivery is created and attempted asynchronously.
   - If SMS is blocked/invalid and email exists + consented, email delivery is created.
2. **Appointment reschedule/cancellation notification**
   - Given `appointment.updated.v1` with status/slot changes, communication-service resolves correct template variant and delivers through policy-selected channel(s).
3. **Invoice created notification (Email primary)**
   - Given `invoice.created.v1`, communication-service sends invoice notification email with invoice number, amount, due date, and link.

Cross-cutting acceptance criteria:
- Idempotent processing prevents duplicate notifications for repeated event delivery.
- Delivery status lifecycle is queryable (`QUEUED`, `SENT`, `DELIVERED`, `FAILED`, `SKIPPED`).
- All failures capture normalized reason + provider response reference.
- Audit data includes `eventId` and `correlationId`.

### ADRs Produced (Approved)
- **ADR-001: Service boundary and ownership**
  - Decision: standalone `communication-service` module.
  - Consequence: clear ownership and faster channel expansion; requires migration bridge from `notification-service`.
- **ADR-002: Event transport and retry model**
  - Decision: Kafka topics with consumer retry and DLQ.
  - Consequence: higher operational consistency with existing event-driven services.
- **ADR-003: Template rendering engine choice**
  - Decision: Mustache-compatible rendering + strict schema validation.
  - Consequence: predictable templates, lower risk of logic-heavy template bugs.

### Deliverables Completed
- Architecture baseline approved (service boundary, Kafka transport, async delivery flow).
- ADR set approved (`ADR-001`, `ADR-002`, `ADR-003`).
- Finalized v1 backlog and timeline aligned to Phases 1-6.

### Final v1 Backlog Snapshot
- **P1:** service skeleton, Liquibase schema, template CRUD, RBAC/admin auth.
- **P2:** template rendering/versioning, provider abstraction, Pondit + SMTP adapters.
- **P3:** event consumers, orchestrator, retry/DLQ worker, idempotency controls.
- **P4:** hospital/scheduling/billing integration and staged migration from direct sends.
- **P5:** consent/preferences/localization rules.
- **P6:** observability, admin UX, hardening, SLO/runbook.

---

## Phase 1 - Service Skeleton + Data Layer (1-2 weeks)

### Scope checklist
- [x] Confirm service boundary:
  - [x] Use standalone `communication-service` (preferred per ADR-001), or
  - [ ] formally document why `notification-service` extension is required.
- [x] Bootstrap module with baseline service conventions:
  - [x] Spring Boot 3.3.x, Java 21
  - [x] OpenAPI config and exposed docs endpoint
  - [x] Actuator health/info/metrics endpoints
  - [x] Eureka registration and discovery metadata
  - [x] Standard logging and audit/correlation scaffolding

### Implementation checklist
- [x] Database foundation
  - [x] Create communication schema + template tables
  - [x] Add Liquibase changelog(s) with explicit rollback steps
  - [x] Add indexes/constraints for uniqueness and lookup performance
- [x] Core data layer
  - [x] Implement base template entities (including status/version markers for next phases)
  - [x] Implement repositories and query methods needed for admin CRUD
  - [x] Add DTO/mapper layer where needed to avoid leaking entity internals
- [x] Secure admin API foundation
  - [x] Define RBAC permissions for read/manage template operations
  - [x] Implement RBAC guard/service checks on every admin endpoint
  - [x] Add initial admin CRUD endpoints for templates (create/read/update/delete/list)
  - [x] Enforce request validation and consistent API error responses

### Definition of Done (must all pass)
- [ ] Service starts locally and registers in Eureka successfully.
- [ ] Liquibase migrations apply cleanly on fresh DB and rollback works for Phase 1 changes.
- [x] Template CRUD endpoints are functional and RBAC-protected.
- [x] OpenAPI and health endpoints are reachable and correct.
- [x] Basic unit/integration tests cover repository + admin CRUD happy/guard paths.

---

## Phase 2 - Template Engine + Provider Abstraction (1-2 weeks)

### Scope checklist
- [ ] Finalize template rendering contract:
  - [ ] Define supported placeholder syntax and escaping rules
  - [ ] Define strict behavior for missing/extra variables
  - [ ] Define channel-specific constraints (SMS length/encoding, email subject/body requirements)
- [ ] Confirm provider abstraction boundary:
  - [ ] `SmsProvider` and `EmailProvider` are transport-focused only
  - [ ] provider adapters do not own orchestration/business policy logic

### Implementation checklist
- [x] Template engine and version lifecycle
  - [x] Implement template versioning model (draft/active/deprecated or equivalent)
  - [x] Implement activation flow ensuring exactly one active version per template/channel scope
  - [x] Add safe migration/defaulting for existing template records introduced in Phase 1
- [x] Template validation and rendering
  - [x] Validate `variables_schema` at create/update time
  - [x] Implement strict placeholder parser (fail fast on missing required vars)
  - [x] Add deterministic `preview` rendering endpoint for admin usage
- [x] Provider abstraction and initial adapters
  - [x] Implement provider strategy/registry for channel -> adapter resolution
  - [x] Implement SMS adapter: Pondit
  - [x] Implement Email adapter: SMTP
  - [x] Normalize provider responses into internal status/result model
- [x] Operational surfaces
  - [x] Add test-send endpoints for SMS and email
  - [x] Add provider health/check endpoint(s) with redacted diagnostics
  - [x] Add structured logs/metrics for provider latency, success, and failure categories

### Definition of Done (must all pass)
- [x] Template version activation and rollback scenarios behave correctly.
- [x] `variables_schema` validation blocks invalid templates before persistence.
- [x] `preview` endpoint renders correctly for valid payloads and returns clear errors for invalid payloads.
- [ ] SMS (Pondit) and Email (SMTP) test-send flows work end-to-end in non-prod.
- [x] Provider health endpoint reports actionable status without leaking secrets.
- [x] Unit/integration tests cover renderer, validator, strategy resolution, and adapter normalization paths.

---

## Phase 3 - Event-Driven Orchestration + Delivery Engine (2 weeks)

### Scope checklist
- [x] Finalize v1 inbound event contract set:
  - [x] Appointment lifecycle events
  - [x] Invoice lifecycle events
  - [x] Required envelope fields and schema version handling
- [x] Confirm orchestration ownership boundaries:
  - [x] domain services publish events only
  - [x] communication service owns routing, retries, and delivery state machine

### Implementation checklist
- [x] Event ingestion and validation
  - [x] Implement consumers for appointment/invoice topics
  - [x] Validate event envelope/schema and reject malformed payloads with clear reason
  - [x] Propagate `correlationId` and `eventId` through all processing stages
- [x] Orchestration and routing
  - [x] Implement orchestrator for event -> template/channel/provider route resolution
  - [x] Persist orchestration decisions for traceability/auditability
  - [x] Handle no-route/no-template cases with explicit terminal status
- [x] Delivery worker, retry, and DLQ
  - [x] Implement queue worker for provider dispatch
  - [x] Implement transient retry policy (e.g., 1m, 5m, 15m, 1h)
  - [x] Route permanent or exhausted failures to DLQ with failure classification
- [x] Idempotency and state model
  - [x] Compute idempotency key from (`eventId + channel + recipient + templateVersion`)
  - [x] Add de-dup persistence check to prevent duplicate sends
  - [x] Persist full delivery lifecycle states (`QUEUED`, `SENT`, `FAILED`, `SKIPPED`, etc.)

### Definition of Done (must all pass)
- [x] End-to-end async pipeline processes supported events from ingestion to persisted delivery outcome.
- [x] Retry and DLQ behavior matches policy for transient vs permanent failures.
- [x] Idempotency prevents duplicate sends on replay/redelivery.
- [x] Delivery states are queryable with correlation/event identifiers for troubleshooting.
- [x] Integration tests cover success, retry-then-success, permanent failure, and DLQ paths.

---

## Phase 4 - Integration with Hospital/Scheduling/Billing (2 weeks)

### Scope checklist
- [x] Align producer/consumer contracts across target domains:
  - [x] `hospital-scheduling-service` appointment events
  - [x] billing/invoice events
- [x] Define phased migration strategy from direct-send paths to communication-service-driven flow.
- [x] Define rollback-safe feature flag controls per event/channel flow.

### Implementation checklist
- [x] Domain event publishing integration
  - [x] Publish or align required domain events at source services
  - [x] Ensure payloads include required communication envelope fields
  - [x] Add contract validation/tests between producers and communication consumers
- [x] Direct-send decommissioning and compatibility
  - [x] Remove/disable direct SMS/email calls in migrated domain paths
  - [x] Maintain temporary fallback direct path during rollout window
  - [x] Add explicit deprecation notes and removal target date
- [ ] Controlled rollout mechanics
  - [x] Implement/verify feature flags:
    - [x] `comm.appointment.sms.enabled`
    - [x] `comm.invoice.email.enabled`
  - [ ] Run dual-mode validation in staging before production cutover
  - [x] Add monitoring checkpoints for each expansion step (org/event)
    - [x] Scheduling producer metrics: `comm.phase4.producer.appointment.published|failed|skipped`
    - [x] Billing producer metrics: `comm.phase4.producer.invoice.published|failed|skipped`
    - [x] Use per-org/event rollout gates with publish/fail ratio checks before expanding scope

### Definition of Done (must all pass)
- [ ] Appointment confirmation SMS is delivered through communication service in target environments.
- [ ] Invoice email is delivered through communication service in target environments.
- [x] Producer-consumer contract tests pass for all integrated domain events.
- [x] Feature-flag rollback restores safe fallback behavior without data loss.
- [x] Direct-send logic is removed or hard-disabled in migrated production paths.

---

## Phase 5 - Consent, Preferences, and Localization (1-2 weeks)

### Scope checklist
- [x] Define communication policy decision model for:
  - [x] consent eligibility (`services/communication-service/src/main/java/com/easyops/communication/service/CommunicationDeliveryService.java` -> `evaluatePolicy`)
  - [x] recipient channel preferences (`services/communication-service/src/main/java/com/easyops/communication/service/CommunicationDeliveryService.java` -> `evaluatePolicy` / `parseChannelSet`)
  - [x] quiet-hour restrictions (`services/communication-service/src/main/java/com/easyops/communication/service/CommunicationDeliveryService.java` -> `quietHoursEnd`)
  - [x] locale selection (`services/communication-service/src/main/java/com/easyops/communication/service/CommunicationDeliveryService.java` -> `resolveLocale` / `pickTemplate`)
- [x] Confirm minimum localization scope for v1 (Bangla + English templates and fallbacks) (`services/communication-service/src/main/java/com/easyops/communication/service/CommunicationDeliveryService.java` -> locale fallback order and `services/communication-service/src/test/java/com/easyops/communication/service/CommunicationDeliveryServiceIT.java` -> `localeResolution_prefersRecipientLocaleThenFallback`).

### Implementation checklist
- [x] Consent and preference policy engine
  - [x] Enforce consent checks before provider dispatch (`services/communication-service/src/main/java/com/easyops/communication/service/CommunicationDeliveryService.java`)
  - [x] Enforce recipient-level channel preferences (`services/communication-service/src/main/java/com/easyops/communication/service/CommunicationDeliveryService.java`)
  - [x] Enforce quiet-hour rules with defined defer/skip behavior (`services/communication-service/src/main/java/com/easyops/communication/service/CommunicationDeliveryService.java`)
- [x] Localization logic
  - [x] Implement locale resolution priority:
    - [x] recipient preference -> organization default -> template fallback (`services/communication-service/src/main/java/com/easyops/communication/service/CommunicationDeliveryService.java` -> `resolveLocale` + `pickTemplate`)
  - [x] Support localized template variants for Bangla/English (`services/communication-service/src/main/java/com/easyops/communication/service/CommunicationDeliveryService.java` and test coverage in `services/communication-service/src/test/java/com/easyops/communication/service/CommunicationDeliveryServiceIT.java`)
  - [x] Record selected locale/template version in delivery metadata (`services/communication-service/src/main/java/com/easyops/communication/entity/CommunicationDelivery.java`, `services/communication-service/src/main/resources/db/changelog/changesets/003-phase5-policy-preferences-localization.xml`)
- [x] Decision traceability
  - [x] Persist policy evaluation outcomes and blocking reasons (`services/communication-service/src/main/java/com/easyops/communication/entity/CommunicationDelivery.java`, `services/communication-service/src/main/resources/db/changelog/changesets/003-phase5-policy-preferences-localization.xml`)
  - [x] Return explicit `SKIPPED`/deferred reasons in internal status model (`services/communication-service/src/main/java/com/easyops/communication/service/CommunicationDeliveryService.java`)
  - [x] Expose admin-queryable rationale for support/audit use (`services/communication-service/src/main/java/com/easyops/communication/controller/CommunicationDeliveryController.java`, `services/communication-service/src/main/java/com/easyops/communication/dto/CommunicationDeliveryResponse.java`)

### Definition of Done (must all pass)
- [x] Consent violations are hard-blocked before send and persisted with explicit reason (`services/communication-service/src/test/java/com/easyops/communication/service/CommunicationDeliveryServiceIT.java` -> `consentDenied_isBlockedBeforeDispatch`).
- [x] Channel preference and quiet-hour rules are consistently enforced (`services/communication-service/src/test/java/com/easyops/communication/service/CommunicationDeliveryServiceIT.java` -> `channelPreferenceBlocked_whenPreferredDoesNotContainRoutedChannel`, `quietHours_deferredWithFutureNextAttempt`).
- [x] Locale-aware template selection works with documented fallback behavior (`services/communication-service/src/test/java/com/easyops/communication/service/CommunicationDeliveryServiceIT.java` -> `localeResolution_prefersRecipientLocaleThenFallback`).
- [x] Bangla/English flows are verified through integration tests (`services/communication-service/src/test/java/com/easyops/communication/service/CommunicationDeliveryServiceIT.java`).
- [x] Policy decision outcomes are queryable for audit/support investigation (`services/communication-service/src/main/java/com/easyops/communication/controller/CommunicationDeliveryController.java`, `services/communication-service/src/main/java/com/easyops/communication/dto/CommunicationDeliveryResponse.java`).

### Remaining items (if any)
- [ ] No open backend Phase 5 blocker identified in current implementation slice.
- [ ] Optional enhancement: replace payload-driven preference/consent inputs with authoritative recipient/org preference stores once those sources are finalized.

---

## Phase 6 - Observability, Admin UX, and Hardening (1-2 weeks)

### Scope checklist
- [ ] Finalize production observability baseline:
  - [x] service/channel/provider metrics
  - [ ] actionable alert thresholds
  - [x] operational dashboards
- [x] Finalize admin operations UX scope:
  - [x] template management
  - [x] delivery search/filter
  - [x] resend failed
- [ ] Define hardening goals for security, reliability, and performance.

### Implementation checklist
- [ ] Metrics, dashboards, and alerting
  - [x] Expose key metrics via Micrometer/Prometheus
  - [x] Build dashboards for success rate, failure rate, latency, retry queue depth
  - [ ] Configure alerts for error spikes, backlog growth, and provider outage indicators
- [x] Admin UX and operational controls
  - [x] Deliver template management page/workflow
  - [x] Deliver delivery log search and drill-down
  - [x] Deliver resend-failed action with RBAC + audit trail
- [ ] Security and compliance hardening
  - [x] Mask PII (phone/email) in logs and observability outputs
  - [ ] Verify secret handling for provider credentials (vault/secrets manager)
  - [x] Tighten RBAC coverage for all operational/admin surfaces
- [ ] Performance and resilience hardening
  - [ ] Run load tests for expected peak burst scenarios
  - [ ] Tune worker concurrency, retry/backoff, and DB indexes as needed
  - [x] Document failure playbooks and on-call runbook actions

### Definition of Done (must all pass)
- [ ] Operations dashboard provides real-time health of send/retry/failure signals.
- [ ] Alerting is active with validated thresholds and response procedures.
- [ ] Admin operations workflows are functional and RBAC-protected.
- [ ] PII masking and secret management controls pass security review.
- [ ] Load/performance targets are met (or documented with approved mitigation plan).
- [ ] SLO baseline and runbooks are published and reviewed.

---

## Cutover and Rollout Plan

### Environments
1. Local/dev: provider sandbox + mocked transport.
2. UAT/staging: real provider test credentials.
3. Production: gradual percentage-based rollout.

### Rollout Steps
1. Enable template and provider admin only.
2. Enable event ingestion with dry-run (no actual send).
3. Enable SMS for one event (`appointment.created`) in one org.
4. Expand by org, then by event type.
5. Enable email flows.

### Rollback
- Feature flags off -> revert to legacy direct path temporarily.
- Preserve events for replay post-fix.

---

## Testing Strategy

### Unit Tests
- template renderer variable resolution.
- provider adapter mapping.
- policy and routing rules.
- retry/idempotency logic.

### Integration Tests
- consumer -> orchestrator -> delivery persistence.
- provider success/failure simulation.
- DLQ + replay.

### Contract Tests
- event payload compatibility between producer and consumer.

### E2E Tests
- appointment created -> SMS delivered path.
- invoice created -> email sent path.
- consent denied -> message skipped path.

---

## Security and Compliance Checklist

- RBAC on template/provider admin APIs.
- PII masking in logs and monitoring.
- Encryption for provider credentials (vault/secrets manager).
- Audit trail for template changes and manual retries.
- Configurable retention policy for rendered content.

---

## Risks and Mitigations

1. **Event schema drift**
   - Mitigation: versioned events + contract tests.

2. **Provider downtime**
   - Mitigation: retries, failover provider support, DLQ.

3. **Duplicate notifications**
   - Mitigation: strong idempotency key + de-dup storage.

4. **Template errors in production**
   - Mitigation: pre-activation validation + preview + staged activation.

5. **Consent violation**
   - Mitigation: hard-block rules before provider dispatch.

---

## Team and Ownership

- **Communication Service Team**: service core, templates, providers, delivery engine.
- **Domain Teams**: event publishing and payload ownership.
- **Platform Team**: broker infra, secrets, monitoring.
- **QA Team**: event and channel E2E validation.

---

## Suggested Timeline (Indicative)

- Phase 0-1: Weeks 1-2
- Phase 2: Weeks 3-4
- Phase 3: Weeks 5-6
- Phase 4: Weeks 7-8
- Phase 5-6: Weeks 9-10

Total: **8-10 weeks** for robust v1 (can be compressed with parallel workstreams).

---

## Immediate Next Actions (This Week)

1. Approve service boundary decision:
   - new `communication-service` vs extending `notification-service`.
2. Finalize first 3 event contracts.
3. Create schema changesets for template + delivery tables.
4. Start Phase 1 skeleton and RBAC-secured admin APIs.

