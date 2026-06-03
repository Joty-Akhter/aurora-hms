# Pharmacy Module — Gaps & New Requirements Implementation Plan

This document is the **engineering backlog and phased plan** to close gaps between current code (`hospital-pharmacy-service`, `hospital-service`, frontend hospital pharmacy pages) and the **Pharmacy Module** requirements in [`pharmacy.md`](pharmacy.md), including recent additions: **optional prescription linkage**, **regional validation (e.g. Bangladesh)**, **POS/PDF receipts**, **stock override when system shows no stock**, **line-level fulfillment states**, **billing lifecycle**, and **integration/boundary** rules in §1–2.

**Related documents**

- [`pharmacy.md`](pharmacy.md) — business requirements (source of truth for *what*).
- [`hospital-pharmacy-service-implementation-plan.md`](hospital-pharmacy-service-implementation-plan.md) — original service scope, APIs, and phased delivery *already implemented* for core catalog, stock, dispense CRUD, and basic reports.
- [`easyops-erp/rbac/README.md`](../../easyops-erp/rbac/README.md) — RBAC catalog and validation scripts.

**How to use this plan**

- Items are grouped into **workstreams** and **phases** (P1–P4) by dependency and risk.
- Each item lists **current state**, **target**, **primary services**, and **acceptance criteria** suitable for tickets.

---

## 1. Executive summary

| Theme | Gap / requirement | Business impact | Suggested phase |
|-------|--------------------|-----------------|-----------------|
| **Billing** | No `GET …/billable-items`, no charge posting, no `pharmacy.sale.*` events | Revenue not tied to dispense; returns not financially linked | P1–P2 |
| **Clinical sync** | In-house dispense does not update `hospital-service` prescription/fill state | PHR shows “pending” after patient received meds | P2 |
| **Dispense model** | Lines always `DISPENSED`; no `PARTIAL`, `OUT_OF_STOCK`, `FILLED_WITH_STOCK_OVERRIDE`, reason codes | Cannot meet §4.1.1 audit or partial-fill semantics | P2 |
| **Stock override** | Hard block on negative stock in `DispenseOrderService` | Cannot dispense when shelf stock exists but ledger is wrong (§4.1.5) | P2 |
| **Optional Rx / region** | UI/API assume prescription lookup; no “paper ref only” fields | Bangladesh / walk-in workflows incomplete | P3 |
| **Receipts** | No ESC/POS or PDF generation from pharmacy flow | §4.1.4 not implemented | P3 |
| **Safety & policy** | Formulary not enforced at dispense; no near-expiry rule engine; limited controlled-drug workflow | §5 partially unmet | P3–P4 |
| **Platform** | No idempotency keys; Kafka consumer creates orders from “first pharmacy”; minimal outgoing events | Reliability and ops gaps (§7) | P2–P3 |

---

## 2. Current implementation snapshot (baseline)

| Area | Implemented today | Gap vs `pharmacy.md` |
|------|--------------------|-----------------------|
| **Dispense API** | `DispenseOrderService.addDispenseLines` deducts stock; rejects expired & negative | No override path; line status always “dispensed”; no idempotency |
| **Prescription link** | `prescription_id` nullable on order | OK at DB; UI still pushes Rx lookup for many flows |
| **Billing** | None | §4.1.3, §1.1 charge alignment |
| **Fill status** | External webhook → `hospital-service` (`EPrescribingService`) | No internal callback when in-house dispense completes |
| **Reports** | Near-expiry, consumption | No stock-override log, sales summary, variance |
| **Events** | `PrescriptionEventListener` consumes Kafka; no producer for stock/dispense | §6 / implementation plan outgoing events |
| **RBAC** | `HOSPITAL_PHARMACY_DISPENSE`, `HOSPITAL_MANAGE` | Optional dedicated `HOSPITAL_PHARMACY_STOCK_OVERRIDE` |

---

## 3. Workstreams

### WS-A — Billing integration

**Goal:** Dispense → charge → payment → return/reversal with stable IDs and idempotency (`pharmacy.md` §4.1.3, §1.1).

| # | Task | Owner | Deliverables |
|---|------|--------|--------------|
| A1 | **Billable view API** — `GET /api/hospital-pharmacy/dispense-orders/{id}/billable-items` (or POST preview) returning line-level items with `dispense_line_id`, drug, qty, unit price refs, tax hints | `hospital-pharmacy-service` | OpenAPI + integration test |
| A2 | **Charge posting** — On order complete (or per line policy), call `hospital-billing-service` `POST /charges` (exact path per billing API) with idempotency key = `dispense_order_id` + phase | `hospital-pharmacy-service` + `hospital-billing-service` (contract) | E2E test with test billing |
| A3 | **Return/reversal** — When pharmacy records return, notify billing to credit/reverse using original charge reference | Same | Idempotent return handling |
| A4 | **Domain events** — Emit `pharmacy.sale.completed`, `pharmacy.sale.cancelled`, `dispense-line.returned` (payload includes org, order/line IDs) | `hospital-pharmacy-service` | Kafka or existing broker; consumer in billing optional |

**Acceptance:** Completing a dispense order creates exactly one charge set per configuration; retries do not duplicate charges; returns adjust financials once.

---

### WS-B — In-house fulfillment → prescription / PHR sync

**Goal:** When `prescription_id` is present, internal dispensing updates the same conceptual state as external fill-status (`pharmacy.md` §1.2).

| # | Task | Owner | Deliverables |
|---|------|--------|--------------|
| B1 | **API or event** — On dispense line post / order complete, call `hospital-service` to update medication line quantity filled / transmission fill status (reuse or extend existing fill-status model for `IN_HOUSE` channel) | `hospital-pharmacy-service` → `hospital-service` | Contract doc + auth (service account or gateway) |
| B2 | **Channel discriminant** — Persist `fulfillment_channel` / source so external webhook and in-house updates do not double-apply | `hospital-service` | Migration + rules in `PrescriptionService` / `EPrescribingService` |
| B3 | **Skip when no Rx** — If `prescription_id` null, no call to B1 (`pharmacy.md` §1.3) | `hospital-pharmacy-service` | Unit tests |

**Acceptance:** Prescription UI shows filled/partial consistent with pharmacy dispense when linked; no update when walk-in.

---

### WS-C — Line-level fulfillment model

**Goal:** Align DB and API with §4.1.1 states: `NOT_STARTED`, `PARTIAL`, `FILLED`, `REFUSED`, `OUT_OF_STOCK`, `CANCELLED`, `FILLED_WITH_STOCK_OVERRIDE`.

| # | Task | Owner | Deliverables |
|---|------|--------|--------------|
| C1 | **Schema** — Extend `dispense_lines`: `line_status`, `reason_code`, `documenting_user_id`, `stock_snapshot_ref`, `substituted_drug_id`, `override_reason_code`, `override_approver_id`, `remaining_qty` | `hospital-service` Liquibase (`hospital_pharmacy`) + entities | Migration |
| C2 | **State machine** — Valid transitions; partial dispense updates `PARTIAL` + remaining | `hospital-pharmacy-service` | Tests |
| C3 | **Unfilled / refused** — Endpoints or PATCH to set line to `OUT_OF_STOCK` / `REFUSED` with mandatory reason without issuing stock | Same | API + UI |
| C4 | **Frontend** — `PharmacyDispense.tsx` shows line state, remaining qty, refusal/out-of-stock flows | `easyops-erp/frontend` | UX review |

**Acceptance:** Auditor can see why a line was not filled; partial fills retain remaining quantity until closed.

---

### WS-D — Stock override (§4.1.5)

**Goal:** Allow issue when recorded qty insufficient; full audit; configurable ledger behavior.

| # | Task | Owner | Deliverables |
|---|------|--------|--------------|
| D1 | **Policy config** — Org (or pharmacy location) flags: `allow_negative_on_dispense`, `require_supervisor_for_override`, `allow_issue_without_batch` | Config table or `application.yml` + UI later | Documented defaults |
| D2 | **Service logic** — Branch in `addDispenseLines`: if override, validate permission, persist `FILLED_WITH_STOCK_OVERRIDE`, apply negative `PharmacyStock` OR create movement with `batch_number` null + reconciliation flag | `hospital-pharmacy-service` | Tests for both modes |
| D3 | **RBAC** — Optional new permission `HOSPITAL_PHARMACY_STOCK_OVERRIDE` in `002-hospital-permissions.sql` (or use `HOSPITAL_MANAGE` until split) | `hospital-service` Liquibase + `HospitalPharmacyRbacService` | Seeded role example |
| D4 | **UI** — Warning dialog, reason dropdown, optional second-user approval | Frontend | Matches §4.1.5 |

**Acceptance:** Pharmacist cannot override without policy + permission; every override appears on a report (WS-F).

---

### WS-E — Optional prescription & regional validation

**Goal:** Walk-in and jurisdictions without e-validation (`pharmacy.md` §1.3).

| # | Task | Owner | Deliverables |
|---|------|--------|--------------|
| E1 | **Order fields** — Optional `paper_prescription_ref`, `prescription_image_attachment_id`, `external_validation_status` (enum: `NOT_REQUIRED`, `PENDING`, `VERIFIED`, `FAILED_SOFT`) | Schema + API | Nullable columns |
| E2 | **Validation policy** — Org setting: `require_ehr_prescription_for_rx_skus` (boolean), `block_on_soft_validation_failure` | Config | Bangladesh-friendly defaults documented |
| E3 | **UI** — Clear path: create order without Rx UUID; capture optional paper ref | `PharmacyDispense.tsx` | |

**Acceptance:** Dispense completes with `prescription_id` null and optional paper ref only; no hard block when policy says soft.

---

### WS-F — Receipt printing (POS + PDF) (§4.1.4)

**Goal:** Thermal ESC/POS and PDF after payment or on demand.

| # | Task | Owner | Deliverables |
|---|------|--------|--------------|
| F1 | **Receipt payload** — Server builds canonical receipt DTO from dispense + billing line (amounts from billing when integrated) | `hospital-pharmacy-service` or BFF | |
| F2 | **PDF** — Endpoint `GET …/dispense-orders/{id}/receipt.pdf` or billing-owned PDF with pharmacy template | TBD (prefer billing if invoice is system of record) | |
| F3 | **Browser print** — Frontend uses PDF print dialog; optional **QZ Tray** / local agent for raw ESC/POS if browser cannot | Frontend + ops doc | |
| F4 | **Reprint audit** — Log user, time, duplicate flag | Service | §4.1.4 |

**Acceptance:** User can print thermal (where driver exists) and PDF; reprints audited.

---

### WS-G — Formulary & substitution at dispense (§4.1.2)

**Goal:** Enforce `FormularyRule` and DAW when prescription present; suggest alternatives when allowed.

| # | Task | Owner | Deliverables |
|---|------|--------|--------------|
| G1 | **Validation service** — Before line commit, load rules by drug + scope; block or require override | `hospital-pharmacy-service` | |
| G2 | **Substitution suggestions** — Query equivalent drugs per rule set version | Same + catalog | API for UI autocomplete |
| G3 | **Audit** — Persist substitution fields per §4.1.2 | `dispense_lines` | |

**Acceptance:** Restricted drug cannot dispense without override path; DAW respected when Rx linked.

---

### WS-H — Controlled drug profiles (§5)

**Goal:** Jurisdiction-agnostic profiles (DEA, NHS CD, etc.) — phased.

| # | Task | Owner | Deliverables |
|---|------|--------|--------------|
| H1 | **Profile config** — CD profile id on drug or org mapping: limits, PDMP step required, witness required | Schema + admin | |
| H2 | **Dispense checks** — Enforce profile at line save: extra fields, register entry | `hospital-pharmacy-service` | |
| H3 | **Reporting** — Controlled drug register export | Reports API | |

**Acceptance:** At least one profile works end-to-end; others stubbed with config flags.

---

### WS-I — Dispensing-time clinical safety net (§5)

**Goal:** Re-check interactions/allergies when high-risk or data changed since prescribe.

| # | Task | Owner | Deliverables |
|---|------|--------|--------------|
| I1 | **Call existing safety APIs** — `hospital-service` clinical medication safety endpoints (or cache) at dispense | `hospital-pharmacy-service` | |
| I2 | **Severity tiers** — Alert vs hard stop per org policy | Config | |
| I3 | **Acknowledgement** — Store user + reason on override | Line extension | |

**Acceptance:** Hard-stop scenario blocks until documented override; soft alert logged.

---

### WS-J — Near-expiry rule engine (§5)

**Goal:** Replace single global check with parameterized rules (days, class, action).

| # | Task | Owner | Deliverables |
|---|------|--------|--------------|
| J1 | **Rule table** — `pharmacy_near_expiry_rules` (effective_from, drug_class, days, action) | Liquibase | |
| J2 | **Evaluate at dispense** — Replace fixed expiry check in `addDispenseLines` | Service | |
| J3 | **Admin UI** — CRUD rules (optional, can be seed-only v1) | Frontend or Admin | |

**Acceptance:** Auditor can explain why a near-expiry sale was blocked or allowed.

---

### WS-K — Events, Kafka, idempotency (§7)

**Goal:** Reliable async integration and no duplicate dispense orders from events.

| # | Task | Owner | Deliverables |
|---|------|--------|--------------|
| K1 | **Outgoing events** — `pharmacy.stock.changed`, `dispense-order.completed`, etc., with keys | `hospital-pharmacy-service` | |
| K2 | **Idempotency** — `Idempotency-Key` header on POST lines/returns; store processed keys TTL | Same | |
| K3 | **Prescription consumer** — Idempotent `prescription.created`: dedupe by `prescription_id`; choose pharmacy by rule (not always first location) | `PrescriptionEventListener` + config | |
| K4 | **Kafka readiness** — Document `spring.kafka` bootstrap for dev/stage | `application.yml` | |

**Acceptance:** Duplicate POST with same idempotency key is no-op; duplicate Kafka delivery does not create second order.

---

### WS-L — Reporting gaps (§6)

| # | Task | Owner | Deliverables |
|---|------|--------|--------------|
| L1 | **Stock override report** — Filter movements/lines with `FILLED_WITH_STOCK_OVERRIDE` | `PharmacyReportService` | |
| L2 | **Sales summary** — Tie to billing when WS-A done | Reports or billing | |
| L3 | **Export** — CSV/PDF export for audits | Frontend | |

---

## 4. Phased rollout

### Phase P1 — Foundation (2–4 weeks, team-dependent)

- K2 Idempotency keys on dispense POSTs (quick win, low risk).
- C1 schema foundations for line status (additive migration).
- D1 policy flags + D2 minimal override path (negative stock OR placeholder batch) behind config default **off** to preserve current behavior until enabled.
- A1 billable-items read API (read-only; no billing call yet).

**Exit criteria:** Feature flags control override; billable structure defined; no regression in default strict stock mode.

**Implementation status (codebase)** — Delivered in `hospital-pharmacy-service` + Liquibase `053-pharmacy-dispense-phase1-gaps.sql` and `054-dispense-line-override-reason-length.sql`:

- **K2:** Optional header `Idempotency-Key` on `POST .../lines` and `POST .../returns`; table `hospital_pharmacy.dispense_idempotency`; PostgreSQL `pg_advisory_xact_lock` per scope+key; JSON replay of `DispenseOrderResponse`.
- **C1:** New nullable columns on `dispense_lines` (`reason_code`, `documenting_user_id`, `stock_snapshot_ref`, `substituted_drug_id`, `override_*`, `remaining_quantity`); `DispenseLine.Status` extended (`FILLED_WITH_STOCK_OVERRIDE`, etc.).
- **D1/D2:** `hospital.pharmacy.dispense.*` in `application.yml`; when `allow-negative-stock: true` and request includes `stockOverrideReason`, issue proceeds with negative ledger or new stock row; line status `FILLED_WITH_STOCK_OVERRIDE`. Defaults **false** (unchanged strict behavior).
- **A1:** `GET /api/hospital-pharmacy/dispense-orders/{id}/billable-items` → `BillableDispenseItemResponse[]` (includes `overrideReasonCode` for stock-override audit; pricing still null until billing integration).
- **Frontend:** `hospitalPharmacyService.ts` — `stockOverrideReason`, optional idempotency on add lines / returns, `getBillableItems`. **`PharmacyDispense.tsx`** — billable-items dialog, `Idempotency-Key` on add-line/return submits, remaining-qty column, status chip colors, stock-override reason field (max 2000 chars), display of persisted override text on lines; **`getApiErrorMessage`** surfaces Bean Validation `errors` map from 400 responses.

**Gap review (post-audit)** — Addressed:

- **Clearer 400 messages** when override is allowed by config but `stockOverrideReason` is missing; distinct messages for missing stock row vs negative balance.
- **`allow-issue-without-batch`** — Enforced with batch normalization (blank → null); unbatched issue only when this flag and **`allow-negative-stock`** + **`stockOverrideReason`** are set (matches plan D2 “placeholder” path).
- **Billable items** — Only lines with `quantityDispensed > 0` (avoids empty billable rows).
- **Idempotency** — `DataIntegrityViolationException` on unique conflict → replay stored JSON (cross-node race).
- **RBAC test** — `DispenseOrderControllerRbacTest`: read path (`search`, `GET …/{id}`, `getBillableItems`); mutate path (`POST` create, `POST …/lines`, `POST …/returns`, `PATCH …/{id}` status); create denied when RBAC always-deny; lines with/without `stockOverrideReason` and stock-override permission; pass-through idempotency stub for POST bodies that call `DispenseIdempotencyService`.
- **Docs** — `hospital-pharmacy-service-implementation-plan.md` §1.1.1 references P1 delivery.
- **K2 TTL** — Scheduled cleanup of `dispense_idempotency` rows older than `hospital.pharmacy.dispense.idempotency-retention-days` (default 90; 0 disables).
- **OpenAPI** — `DispenseOrderController` annotated for Swagger (`@Tag`, `@Operation`, idempotency header docs).
- **Override reason length** — Liquibase `054-dispense-line-override-reason-length.sql`: `override_reason_code` widened to `VARCHAR(2000)` so free-text audit reasons match `@Size(max=2000)` on `stockOverrideReason`.
- **Bean Validation on collections** — `POST …/lines` uses `@Valid @RequestBody List<@Valid DispenseLineRequest>` with `@Validated` on `DispenseOrderController` so each line’s `@NotNull`, `@Size`, etc. are applied; `DispenseReturnRequest.lines` uses `List<@Valid Line>` for nested return rows. Unit test: `DispenseLineRequestValidationTest`.
- **A1 service test** — `DispenseOrderServiceGetBillableItemsTest` covers qty &gt; 0 filter, `overrideReasonCode` mapping, and missing order.
- **Non-empty line batches** — `POST …/lines` requires `@NotEmpty` on the list so `[]` cannot advance an order to `IN_PROGRESS` without creating lines.
- **Validation HTTP mapping** — `GlobalExceptionHandler` handles `ConstraintViolationException` and `HandlerMethodValidationException` as 400; `MethodArgumentNotValidException` merges duplicate field keys with `"; "` instead of silently dropping messages.

### Phase P2 — Money & clinical truth (4–8 weeks)

- A2–A4 billing posting and events (with billing team).
- B1–B3 in-house fill sync to `hospital-service`.
- C2–C3 line state machine + partial/refusal APIs.
- D3–D4 RBAC + UI for override.
- K1–K3 events and Kafka idempotency for prescription-created.

**Exit criteria:** End-to-end: prescribe → dispense → charge → PHR shows filled (when Rx linked).

**Implementation status (codebase)** — Delivered in `hospital-pharmacy-service`, `hospital-service`, `hospital-billing-service`, and `PharmacyDispense.tsx` (Liquibase `055`–`058` in hospital-service for `fill_status_source`, in-house idempotency, `billing_posted_at`, RBAC seeds):

- **WS-A (A2–A4):** Optional `hospital.pharmacy.integration.billing.post-charges-enabled` → `POST` charges to `hospital-billing-service`; `post-return-credits-enabled` for return credits; idempotent keys per line. Kafka: `pharmacy.sale.completed` (includes `prescriptionId` when present), `pharmacy.sale.cancelled`, `dispense-line.returned`. `ChargeController` allows `HOSPITAL_PHARMACY_CHARGE_POST` or `HOSPITAL_MANAGE`.
- **WS-B (B1–B3):** `POST /api/integrations/pharmacy/in-house-dispense-fill` (gateway: `/api/hospital/integrations/pharmacy/...`) with idempotency table; `fill_status_source` on transmissions; skip in-house sync when `prescription_id` is null on the order.
- **WS-C (C2–C3):** `POST …/lines/unfulfilled`, `DispenseLineStatusRules`, UI “Not filled (OOS / refused)”.
- **WS-D (D3–D4):** Permissions `HOSPITAL_PHARMACY_STOCK_OVERRIDE`, `HOSPITAL_PHARMACY_CHARGE_POST`; stock override field gated by RBAC on the frontend.
- **WS-K (K1,K3,K4):** Outgoing `pharmacy.stock.changed`, `dispense-order.completed`, plus sale events; `prescription.created` dedupe via `existsByPrescriptionId`; optional `hospital.pharmacy.integration.events.default-pharmacy-location-id` for queue routing; `spring.kafka.bootstrap-servers` documented in `application.yml`.

### Phase P3 — Regional UX, receipts, formulary, near-expiry v1

- E1–E3 optional prescription / paper ref.
- F1–F4 receipts PDF + print path.
- G1 formulary enforcement v1.
- J1–J2 near-expiry rules v1.
- L1 override report.

**Exit criteria:** Bangladesh-style walk-through documented; receipt printable; formulary blocks restricted drugs.

**Regional walk-through (operator checklist)** — For walk-in / paper-led flows: (1) create or patch order with **paper Rx ref** and/or **prescription image attachment id** when no EHR `prescription_id` is linked; (2) set **external validation** as appropriate; (3) if **controlled (Rx) SKU** policy is enabled (`hospital.pharmacy.regional.require-ehr-prescription-for-rx-skus`), ensure evidence exists before adding lines; (4) optional **FAILED_SOFT** blocking via `block-on-soft-validation-failure`; (5) print **Receipt PDF** and rely on **reprint audit** for duplicates; (6) use **formulary alternatives** API or override fields when a restricted drug is involved; (7) run **Stock overrides** report for reconciliation.

**Implementation status (codebase)** — Delivered in `hospital-pharmacy-service`, Liquibase `059`–`060` (hospital-service changelog for `hospital_pharmacy` schema), and `PharmacyDispense.tsx` / `hospitalPharmacyService.ts`:

- **WS-E (E1–E3):** `CreateDispenseOrderRequest` / `DispenseOrder` — `paperPrescriptionRef`, `prescriptionImageAttachmentId`, `externalValidationStatus`; `hospital.pharmacy.regional` in `application.yml` (`require-ehr-prescription-for-rx-skus`, `block-on-soft-validation-failure`); `PATCH /dispense-orders/{id}/regional` (optional `clearPrescriptionImageAttachment`); **Pharmacy Dispense** UI: **Paper / validation** dialog (patch) + create-order fields.
- **WS-F (F1–F4):** OpenPDF receipt — `GET /dispense-orders/{id}/receipt.pdf`; `pharmacy_receipt_reprint_audit` with duplicate detection (same user within 1 minute).
- **WS-G (G1):** `FormularyDispenseValidator` + `dispense_lines.formulary_override_reason`; line fields `substitutedDrugId` / `formularyOverrideReason`; `GET /drugs/{id}/formulary-alternatives`; **Add Line** dialog: **Load formulary alternatives** + chips to pick issued drug.
- **WS-J (J1–J2):** `pharmacy_near_expiry_rules` + `NearExpiryEvaluationService` (replaces fixed near-expiry check on stock rows).
- **WS-L1:** `GET /reports/stock-overrides` (alias `/stock-override-lines`); UI tab on pharmacy dispense.

### Phase P4 — Advanced safety & compliance

- H1–H3 controlled profiles.
- I1–I3 dispensing-time clinical net.
- L2–L3 sales reporting and exports.
- Performance tuning (indexes, p95 targets §7).

**Implementation status (partial — hospital-pharmacy-service + Liquibase `060-pharmacy-p4-controlled-safety-reporting.sql`):**

- **H1:** `drugs.controlled_profile_code` (nullable); `Drug.isControlledSubstance()` treats legacy `controlled_drug_flag` or non-`NONE` profile as controlled; drug API exposes the field.
- **H2:** `hospital.pharmacy.dispense.require-witness-for-controlled` (default false); `DispenseLineRequest.witnessUserId` + `dispense_lines.witness_user_id`; enforced in `DispenseOrderService.addDispenseLines` when flag is true.
- **H3 / L1:** `GET /api/hospital-pharmacy/reports/controlled-substance-register` and `GET .../stock-override-lines` (date range + pharmacy), RBAC read.
- **L2:** `GET .../reports/sales-summary` — aggregates consumption quantities (financial amounts when billing integration exists).
- **L3:** `GET .../reports/consumption/export` — CSV of consumption report.
- **I1 (stub):** `hospital.pharmacy.integration.clinical-safety.check-at-dispense-enabled` (default false); no hospital-service call yet — flag reserved for WS-I wiring.
- **Performance:** indexes on `dispense_lines(created_at)` and `stock_movements(pharmacy_location_id, movement_time, movement_type)`.

---

## 5. Cross-cutting concerns

- **Testing:** Integration tests for `hospital-pharmacy-service` + Testcontainers; contract tests with billing; frontend E2E for dispense happy path and override.
- **Documentation:** Update [`hospital-pharmacy-service-implementation-plan.md`](hospital-pharmacy-service-implementation-plan.md) §1.1 status table after each phase; keep [`pharmacy.md`](pharmacy.md) as requirements only (no duplicate prose).
- **Security:** Service-to-service auth for `hospital-pharmacy-service` → `hospital-service` / billing; no secrets in logs for receipt reprint audit.

---

## 6. Risks and mitigations

| Risk | Mitigation |
|------|------------|
| Billing API not stable | Agree charge DTO early; use feature flag for posting |
| Double fill-status (webhook + in-house) | B2 channel discriminant + idempotent updates in `hospital-service` |
| Negative stock data corruption | Reconciliation job + reports (WS-L); transfers stay strict |
| Scope creep on CD profiles | Ship H1–H2 for one profile first |

---

## 7. Traceability matrix (requirements → workstream)

| `pharmacy.md` section | Workstreams |
|----------------------|-------------|
| §1.1 IDs / billing | A, B |
| §1.2 Fulfillment channels | B |
| §1.3 Optional Rx / region | E, B |
| §4.1.1 Line states | C |
| §4.1.2 Substitution | G |
| §4.1.3 Billing lifecycle | A |
| §4.1.4 Receipts | F |
| §4.1.5 Stock override | D, L |
| §5 Near-expiry | J |
| §5 Controlled drugs | H |
| §5 Clinical checks | I |
| §5 RBAC | D, G |
| §6 / §7 Reporting & NFR | K, L |

---

*Document version: 1.0 — aligns with `pharmacy.md` as of implementation planning for EasyOps Hospital Module.*
