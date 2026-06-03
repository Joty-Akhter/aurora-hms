# Hospital Corporate and Discount Service – Event Contracts

When Kafka or RabbitMQ is in use, this service publishes domain events (async, non-blocking) so that **hospital-billing-service**, **portals**, and other consumers can react to changes in corporates, contracts, coverage rules, discount schemes, and discount decisions.

Without a messaging broker, the default implementation logs events only (no external publish).

## Envelope

Each event is published with:

- **type**: Event type string (see below).
- **timestamp**: ISO-8601 instant (e.g. `2025-03-15T12:00:00Z`).
- **payload**: JSON object with the fields described per event type.

Consumers can subscribe by topic (e.g. `hospital-corporate-discount-events`) and filter or route by `type`.

---

## Event Types and Payloads

### Corporate

| Type | When | Payload |
|------|------|--------|
| `corporate.created` | New corporate client created | `id`, `code`, `name`, `status` |
| `corporate.updated` | Corporate client updated | `id`, `code`, `name`, `status` |
| `corporate.deactivated` | Corporate status set to INACTIVE | `id`, `code`, `name`, `status` |

**Consumer use:** hospital-billing-service can invalidate or refresh corporate/contract caches; portals can refresh corporate lists.

---

### Contract

| Type | When | Payload |
|------|------|--------|
| `contract.created` | New corporate contract created | `id`, `corporateClientId`, `contractCode`, `validFrom`, `validTo` |
| `contract.updated` | Contract updated | `id`, `corporateClientId`, `contractCode`, `validFrom`, `validTo` |

**Consumer use:** billing and portals can refresh contract and coverage-rule data for the given `corporateClientId` or `id`.

---

### Coverage rule

| Type | When | Payload |
|------|------|--------|
| `coverage-rule.created` | New coverage rule added to a contract | `id`, `corporateContractId`, `scopeType`, `scopeValue` |
| `coverage-rule.deleted` | Coverage rule removed | `id`, `corporateContractId`, `scopeType`, `scopeValue` |

**Consumer use:** hospital-billing-service should refresh or invalidate coverage rules for `corporateContractId` so that `/coverage/evaluate` uses up-to-date rules.

---

### Discount scheme

| Type | When | Payload |
|------|------|--------|
| `discount-scheme.created` | New discount scheme created | `id`, `code`, `name`, `corporateClientId`, `status` |
| `discount-scheme.updated` | Discount scheme updated | `id`, `code`, `name`, `corporateClientId`, `status` |
| `discount-scheme.deactivated` | Scheme status set to INACTIVE | `id`, `code`, `name`, `corporateClientId`, `status` |

**Consumer use:** billing and portals can refresh discount scheme and approval-level data; disable or adjust discount evaluation when a scheme is deactivated.

---

### Discount decision

| Type | When | Payload |
|------|------|--------|
| `discount-decision.created` | A discount decision (audit record) created | `id`, `billContextId`, `patientId`, `discountSchemeId`, `discountAmount` |

**Consumer use:** hospital-billing-service can link the decision to the bill; portals and reporting can show or aggregate decisions.

---

## IDs

All `id`, `corporateClientId`, `corporateContractId`, `patientId`, `discountSchemeId` in payloads are UUID strings. Consumers can call the service REST API to fetch full details when needed.

## Optional: Kafka / RabbitMQ

To enable real publishing:

1. Add `spring-kafka` (or `spring-amqp`) to the service and configure the broker.
2. Provide a bean that implements `CorporateDiscountEventPublisher` and publishes to your topic (e.g. JSON envelope as above). The default `LoggingCorporateDiscountEventPublisher` is then not registered (`@ConditionalOnMissingBean`).
