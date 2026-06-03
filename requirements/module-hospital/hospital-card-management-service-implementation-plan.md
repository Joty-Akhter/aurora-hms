# `hospital-card-management-service` – Unified Implementation Plan

This is the **current implementation roadmap** for the card platform based on the updated card taxonomy:
- Patient identity card
- Staff/employee identity card
- Corporate benefit card (printed)

It replaces the old granular checklist style in this file. Legacy phase checkboxes and stale “not started / 100%” sections have been removed to avoid confusion.

---

## 1) Service role and boundaries

`hospital-card-management-service` is the **technical card platform** for:
- Card registry (card number, owner, status, lifecycle)
- Shared print/template pipeline
- Wallet/prepaid balances and transactions (for products that require money/limits)

It is **not** the business owner for:
- Corporate contracts/policies (owned by `corporate-service-and-card-management.md`)
- Patient registration rules (owned by `patient-identity-card-registration.md`)
- Staff HR lifecycle rules (owned by `staff-identity-card.md`)
- IPD temporary visitor fee/refund business process (owned by `temporary-card-service-ipd.md`)

Reference taxonomy: `hospital-card-types-overview.md`.

---

## 2) Product model strategy

Use `card_products` to separate behavior:

- **Identity-only products** (no wallet semantics):
  - `PATIENT_IDENTITY`
  - `STAFF_IDENTITY`
  - optional `CORPORATE_BENEFIT_*` (if no stored value)
- **Wallet/prepaid products**:
  - e.g. `PATIENT_PREPAID`, `STAFF_SUBSIDIZED`, `CORPORATE_CREDIT`

### Required behavior split

- Identity products:
  - issue/search/replace/print supported
  - no canteen debit/authorization unless explicitly linked to a wallet product
- Wallet products:
  - full balance/authorization/ledger behavior

---

## 3) Phased delivery plan (single source of truth)

## Phase 0 – Foundation and governance

### Objective
Lock ownership boundaries and make the service ready for multi-program card usage.

### Backend/Platform
- Keep existing service module and gateway route `/api/hospital-card-management/**`.
- Keep existing core schema (`hospital_card`) and card tables.
- Keep RBAC guard pattern for manage/view actions.

### Requirements/Docs
- Maintain links between:
  - `hospital-card-types-overview.md`
  - `card-management-phase-0-foundation-governance.md`
  - `patient-identity-card-registration.md`
  - `staff-identity-card.md`
  - `corporate-service-and-card-management.md`
  - `temporary-card-service-ipd.md`

### Governance closure checklist
- Baseline governance document exists and is linked.
- Taxonomy, registry boundary, and product-code conventions are frozen.
- RBAC matrix is documented for issue/reprint/replace/revoke/refund.
- Formal sign-off is tracked in `card-management-phase-0-foundation-governance.md` approval table.

### Exit criteria
- Clear ownership and no contradictory “single hospital card” statements in active docs.
- Approval tracker rows (Product, Architecture, Security, Operations) are all marked approved.

---

## Phase 1 – Patient identity card first (priority)

### Objective
Issue one patient identity card at registration with safe fallback.

### Implemented baseline (already in code)
- `hospital-service` triggers patient card issuance after successful registration.
- Seeded `PATIENT_IDENTITY` product.
- Response includes identity card status/details for UI feedback.
- Registration does not roll back when card issuance fails.

### Remaining work in this phase
- Add **PDF** (or dedicated thermal) export for patient card; HTML print template is implemented server-side with embedded QR.
- ~~Add patient profile actions: reprint and replace.~~ **Done** (Patient Overview + Edit Patient; audited print via `POST …/reprint`).
- ~~Add explicit audit surface (`printed_by`, `printed_at`, reason for replacement).~~ **Done** for print/reprint and replace (see `patient_identity_card_audit_log`).
- Expand **automated tests** beyond smoke (unit + integration coverage).

### Exit criteria
- New registration produces one active patient identity card (or deterministic failed status).
- Front desk can print/reprint from supported screens.
- Replace flow invalidates old card and links history.

---

## Phase 2 – Staff/employee identity card

### Objective
Support HR/security-driven staff card lifecycle.

### Scope
- Issue/reprint/replace/suspend/revoke staff cards.
- Link owner as `STAFF` and `owner_reference_id = employee_id` (or mapped user id).
- Verification endpoint/view for security desk scans.
- Auto status updates from HR events (terminated/suspended -> revoke/suspend).

### Exit criteria
- Staff onboarding and exit can manage card lifecycle end-to-end.

---

## Phase 3 – Corporate benefit cards (printed) via shared platform

### Objective
Use this service as the card registry + print engine for corporate benefit cards.

### Scope
- Corporate module remains business owner for client/type/policy.
- On issue/reissue, corporate workflow syncs card record here.
- Card number/status here must match billing validation path.
- Reprint uses same number; reissue generates new number and blocks old.

### Exit criteria
- Corporate beneficiary cards are printable and verifiable with policy-consistent status.

---

> Phase 4 and Phase 5 are intentionally out of scope for this implementation cycle.

---

## 4) Explicit non-goals for this service

- IPD temporary visitor fee/refund module ownership.
- Corporate contract policy authoring.
- HR employment master ownership.
- Patient registration ownership.

This service may provide technical primitives for those domains, but business rules stay in their source modules.

---

## 5) API direction by card family

### Identity and printed benefit cards
- Issue/search/get/update status/replace
- Print preview/render/reprint APIs (to be standardized)
- No wallet debit by default

### Wallet/prepaid cards
- Full identity APIs +
- Balance/topup/adjustment
- Authorization/capture/refund
- Transaction and reconciliation/report endpoints

---

## 6) Data and migration guidelines

- Keep using `hospital_card` schema for platform card records.
- Add new product seeds via Liquibase changesets (idempotent `ON CONFLICT DO NOTHING`).
- Do not repurpose wallet accounting tables for IPD temporary visitor deposit/refund business semantics unless a formal architecture decision is approved.

---

## 7) Cleanup policy for old implementation

Do **not** delete old code immediately.

Use staged cleanup:
1. Mark legacy paths as deprecated.
2. Add migration/use logs and verify zero active usage.
3. Replace callers.
4. Remove deprecated code in a dedicated cleanup release.

This avoids regressions in existing billing/canteen/card flows.

---

## 8) Release gates checklist

- [ ] Phase 1 patient card print + replace completed
- [ ] Staff card lifecycle completed
- [ ] Corporate print sync completed
- [ ] Deprecated paths removed only after usage audit

---

## 9) Related documents

- `hospital-card-types-overview.md`
- `card-management-phase-0-foundation-governance.md`
- `patient-identity-card-registration.md`
- `patient-identity-card-implementation-plan.md`
- `staff-identity-card.md`
- `corporate-service-and-card-management.md`
- `temporary-card-service-ipd.md`
