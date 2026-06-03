# Hospital Card Management Service

Spring Boot service for card and wallet operations: card products, limit profiles, card lifecycle, balances, and (from Phase 2) authorization and transaction ledger.

## Phase 0 – Service skeleton

- Runs on port **8090** (configurable).
- Registers with Eureka; reachable via API Gateway at `/api/hospital-card-management/**`.
- Actuator: health, info, metrics, prometheus.
- Database schema `hospital_card` is created by **hospital-service** Liquibase (run hospital-service first so the schema exists).

## Running

1. Ensure PostgreSQL and Eureka are up; run hospital-service at least once so Liquibase creates `hospital_card`.
2. `./mvnw -pl services/hospital-card-management-service spring-boot:run`
3. Health: `http://localhost:8090/actuator/health` or via gateway.

See `requirements/module-hospital/hospital-card-management-service-implementation-plan.md` for phased implementation details.

## Events (Phase 2.5)

The service uses Spring’s `ApplicationEventPublisher` to emit domain events. Consumers can register `@EventListener` beans in the same application or (in the future) bridge to Kafka/Rabbit.

### card.transaction.committed

Published after a transaction is committed (e.g. capture). Payload: `CardTransactionCommittedEvent`.

| Field | Type | Description |
|-------|------|-------------|
| cardId | UUID | Card id |
| transactionId | UUID | Committed transaction id |
| amount | BigDecimal | Amount (positive) |
| sourceSystem | String | e.g. CANTEEN, HOSPITAL_BILLING |
| externalReferenceId | String | External reference (bill id, sale id, etc.) |

### card.balance.changed (optional)

Published when an account balance changes (e.g. after capture). Payload: `CardBalanceChangedEvent`.

| Field | Type | Description |
|-------|------|-------------|
| cardId | UUID | Card id |
| accountId | UUID | Card account id |
| currentBalance | BigDecimal | New balance |
| currency | String | Account currency |

## Corporate integration (Phase 3.5 – optional)

When **hospital-corporate-and-discount-service** is available:

- Add a Feign client (or RestTemplate) to fetch contract entitlements by `corporate_id` (e.g. `GET /api/hospital-corporate-discount/contracts/{corporateId}/entitlements` or equivalent from the corporate service API).
- Map the response to a `limit_profile_id` or inline limits (daily/monthly amount, meal/visit) for the card.
- When issuing a card with `ownerType=CORPORATE_BENEFICIARY` and a non-null `corporateId`, optionally call the corporate service to resolve the contract and set `limit_profile_id` on the card (or apply inline limits) so that authorization and balance respect contract-driven limits.

Until the corporate service is deployed, leave this integration unwired. Cards for corporate beneficiaries can still be issued with a manually chosen limit profile (or product default). No Feign client or dependency is added in the card-management service until the corporate service contract and URL are defined.

## Reports (Phase 5.1)

- **GET /api/hospital-card-management/reports/liabilities?asOf=&cardProductId=&ownerType=**  
  Prepaid liabilities (cards/accounts with balance &gt; 0). When **asOf** is provided (ISO-8601), balance is computed point-in-time from committed transactions with `posted_at` (or `created_at`) &le; asOf. When asOf is omitted, current balance is used. Optional filters: cardProductId, ownerType.

- **GET /api/hospital-card-management/reports/usage-by-domain?from=&to=&sourceSystem=**  
  Usage aggregated by source_system in date range.

- **GET /api/hospital-card-management/reports/corporate-exposure?corporateId=&asOf=**  
  Cards linked to corporate_id with balance and credit_limit; summary total.

## Lifecycle (Phase 4.1)

- **Issue:** Newly issued cards are created with status **ISSUED** only. A separate activate action (PATCH status to ACTIVE) is required before the card can be used for authorizations. A future “per rule” option (e.g. product-level auto-activate on issue) may set ACTIVE at issue time.
- **Block:** New authorizations are rejected for non-ACTIVE cards (including BLOCKED). Capture of already-authorized (PENDING) transactions is allowed even if the card was blocked after the auth.
- **Close:** Closing requires balance zero, or a reason to force-close. On close, `card.closed` is emitted with final balance and currency.
- **Replace:** If the old card has a positive balance, it is transferred to the new card via TRANSFER transactions; then the old card is linked to the new one and closed. `card.replaced` is emitted with old card id, new card id, and transferred amount.

All lifecycle changes (status, replace, close) set **updated_at** on the card. The **reason** field in status/replace requests is used for validation (e.g. force-close) but is not persisted on the card; an optional audit log table for status changes can be added later.

## Portal / self-service APIs (Phase 4.2)

- **GET /api/hospital-card-management/me/cards** – Returns cards for the current user. Requires header **X-Owner-Reference-Id** (patient_id, staff_id, etc.); optional **X-Owner-Type** (PATIENT, STAFF, etc.). BFF or API Gateway should set these after resolving the authenticated user. Missing header → 401.
- **GET /api/hospital-card-management/me/cards/{id}/statement** – Transaction history for a card. Same headers required; returns 403 if the card does not belong to the owner. Query params: `from`, `to` (ISO-8601), `page`, `size`.

## Admin search (Phase 4.3)

**GET /api/hospital-card-management/cards/search** supports: `cardNumber`, `ownerReferenceId`, `ownerType`, `corporateId`, `cardProductId`, `status`, `issuedAtFrom`, `issuedAtTo` (ISO-8601), `page`, `size`. Response is paged for export-friendly listing.

## Reconciliation (Phase 5.2)

Reconciliation matches **card_transactions** (by `source_system` and `external_reference_id`) with Billing or Canteen records.

### Format

- **Match key**: `source_system` + `external_reference_id` (e.g. HOSPITAL_BILLING + invoice id, CANTEEN + sale id).
- **Card side**: Committed transactions only; `amount` and `posted_at` are the values to compare.

### APIs

- **GET /api/hospital-card-management/reconciliation/card-vs-billing?from=&to=&sourceSystem=**  
  Returns the card-side export: list of committed transactions in the date range (optional `from`, `to` in ISO-8601; optional `sourceSystem` e.g. `HOSPITAL_BILLING`, `CANTEEN`). Each item: `transactionId`, `sourceSystem`, `externalReferenceId`, `amount`, `currency`, `postedAt`. Use this to compare with Billing/Canteen export (e.g. CSV or their API) to find mismatches. If `from`/`to` are omitted, all committed transactions are returned (use with care).

- **POST /api/hospital-card-management/reconciliation/compare**  
  Request body: JSON array of `{ "sourceSystem": "...", "externalReferenceId": "...", "amount": <number> }`. Response: for each entry, `{ sourceSystem, externalReferenceId, status: "MATCHED" | "NOT_FOUND" | "AMOUNT_MISMATCH", expectedAmount, actualAmount }`. Billing/Canteen can send their list and get match results without exporting our full list.

- **POST /api/hospital-card-management/reconciliation/mismatches**  
  Same request body as compare. Response: only entries with status `NOT_FOUND` or `AMOUNT_MISMATCH` (mismatches only). Single-call API when you only need the list of mismatches.

### CSV / external format

For file-based reconciliation, export from this service via GET card-vs-billing (e.g. as JSON or map to CSV with columns: `transactionId`, `sourceSystem`, `externalReferenceId`, `amount`, `currency`, `postedAt`). The other system should provide a CSV or API with at least `source_system`, `external_reference_id`, `amount`. Compare by key (`source_system` + `external_reference_id`) and then by `amount`; report NOT_FOUND (in their list but not in ours, or vice versa) and AMOUNT_MISMATCH.

## Hardening (Phase 5.3)

### Idempotency

Auth, capture, top-up, adjustment, and refund accept an optional **idempotency_key**. When provided:

- **Replay**: If a transaction with the same key already exists for that operation type, the service returns the existing result (same response as the first call).
- **Reject duplicate**: If the key was already used for a *different* operation type, the service returns **409 Conflict** with code `IDEMPOTENCY_CONFLICT`. The unique index on `card_transactions(idempotency_key)` also prevents duplicate keys at insert time (409 on DB violation).

Clients should send a unique key per logical request (e.g. UUID or client-ref) and retry with the same key on timeout/network failure.

### Load and indexes

Indexes in place for authorization and balance hot path:

- `cards(card_number)` – auth lookup by card number.
- `card_accounts(card_id)` UNIQUE – balance and account by card.
- `card_transactions(idempotency_key)` UNIQUE (where not null) – idempotency and duplicate rejection.
- `card_transactions(card_account_id, created_at)` – list transactions per card.
- `card_transactions(status, posted_at)` – reconciliation and usage-by-domain queries.

Target: authorization and balance APIs &lt; 200 ms p95 under typical POS load. Concurrency: balance updates are per account with row-level locking inside a transaction; avoid long-running transactions. For high throughput, scale the service horizontally; DB connection pool and PostgreSQL are the main limits.

### Metrics (Prometheus)

Custom counters exposed via `/actuator/prometheus` (and `/actuator/metrics`):

| Metric | Description |
|--------|-------------|
| `card_authorizations_total` | Count of authorization attempts. Tag `outcome`: `approved` \| `declined`. |
| `card_transactions_total` | Count of transactions. Tag `type`: `AUTH`, `CAPTURE`, `TOPUP`, `ADJUSTMENT`, `REFUND`. |
| `card_balance_checks_total` | Count of balance check requests (GET balance). |
