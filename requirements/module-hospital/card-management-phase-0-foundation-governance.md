# Card management Phase 0 - Foundation and governance (1-2 weeks)

**Status:** Finalized governance baseline  
**Duration:** 1-2 weeks  
**Applies to:** Patient identity, staff identity, temporary IPD visitor, corporate benefit, and optional other benefit card programs

---

## 1. Objective

Freeze cross-program governance decisions before expanding implementation, so all modules use one taxonomy, one technical registry strategy, and a consistent security baseline.

---

## 2. Frozen taxonomy and ownership

| Card program | Business owner | Trigger | Money flow owner | Technical card registry owner |
|--------------|----------------|---------|------------------|-------------------------------|
| **Patient identity (registration)** | Registration / Patient Administration | New patient registration | Billing only if issuance fee is configured | `hospital-card-management-service` |
| **Staff identity (employee facility card)** | HR + Security / Facility | Active employee onboarding or replacement | Billing only if replacement fee is configured | `hospital-card-management-service` |
| **Temporary IPD visitor (fee/refund)** | IPD Operations + Accounts | Active IPD admission and attendant issuance | Accounts / Billing (fee + refund) | **Separate temporary-card flow** (not wallet registry by default) |
| **Corporate benefit (policy/discount + printed card)** | Corporate Team + Billing | Corporate beneficiary enrollment under active policy | Corporate billing/discount policies | `hospital-card-management-service` (registry + print) |
| **Other benefit cards (optional)** | Program owner (Marketing / Partnerships / Management) | Program-specific enrollment | Program-specific policy | `hospital-card-management-service` (distinct product) |

**Freeze decision:** The five programs above are distinct and must not be merged into one business workflow.

---

## 3. Single technical registry decision

### 3.1 Approved decision

1. Use **`hospital-card-management-service`** as the single technical registry for:
   - Card records (`cards`) and lifecycle for patient/staff/corporate/other benefit products.
   - Print template references and print/reprint actions.
2. Keep **temporary IPD visitor fee/refund logic** as a separate business flow and data model, integrated to Accounts/Billing and admission context.

### 3.2 Boundary rule

- `hospital-card-management-service` is the shared **registry + print infrastructure**.
- IPD temporary visitor cards remain an **admission-scoped operational card flow** with fee/refund semantics and do not automatically become wallet/registry products unless an explicit later bridging phase is approved.

---

## 4. Product code and naming conventions

### 4.1 Canonical product codes

Use uppercase snake-case product codes only:

- `PATIENT_IDENTITY`
- `STAFF_IDENTITY`
- `TEMP_IPD_VISITOR` (reserved for future bridge only; not active in registry by default)
- `CORPORATE_BENEFIT_<TIER_OR_POLICY>` (examples: `CORPORATE_BENEFIT_GOLD`, `CORPORATE_BENEFIT_STANDARD`)
- `OTHER_BENEFIT_<PROGRAM_CODE>` (examples: `OTHER_BENEFIT_LOYALTY`, `OTHER_BENEFIT_PARTNER_X`)

### 4.2 Convention rules

1. Prefix indicates business family (`PATIENT`, `STAFF`, `TEMP_IPD`, `CORPORATE_BENEFIT`, `OTHER_BENEFIT`).
2. Suffix identifies tier/policy/program where applicable.
3. Product code is immutable after creation.
4. Product code uniqueness is global in `hospital_card.card_products`.
5. `TEMP_IPD_VISITOR` remains reserved until bridge approval and must not be used for wallet authorization in this phase.

---

## 5. Security baseline (RBAC matrix)

### 5.1 Operation definitions

- **Issue**: New card issuance.
- **Reprint**: Print same active card again (no new card number).
- **Replace**: Lost/damaged reissue with old card superseded.
- **Revoke**: Suspend/block/close card so it is no longer valid.
- **Refund**: Financial refund operation (primarily IPD temporary card flow; optional configured replacement-fee refunds).

### 5.2 RBAC permission matrix (baseline)

| Role | Issue | Reprint | Replace | Revoke | Refund |
|------|-------|---------|---------|--------|--------|
| **CARD_ADMIN** | Yes (all programs) | Yes (all programs) | Yes (all programs) | Yes (all programs) | Yes (with finance rules) |
| **REGISTRATION_FRONTDESK** | Patient only | Patient only | No | No | No |
| **HR_STAFF_CARD_OFFICER** | Staff only | Staff only | Staff only | Staff only | No |
| **CORPORATE_CARD_OFFICER** | Corporate/other benefit only | Corporate/other benefit only | Corporate/other benefit only | Corporate/other benefit only | No |
| **IPD_TEMP_CARD_DESK** | IPD temporary only | IPD temporary only | IPD temporary only | IPD temporary only | Request only (no final approval) |
| **ACCOUNTS_CASHIER** | No | No | No | No | Execute refund (IPD temporary + approved fee refunds) |
| **AUDITOR_READONLY** | No | No | No | No | No (view logs/reports only) |

### 5.3 Permission keys (for implementation mapping)

- `CARD_ISSUE`
- `CARD_REPRINT`
- `CARD_REPLACE`
- `CARD_REVOKE`
- `CARD_REFUND`
- Optional scope qualifiers:
  - `.PATIENT_IDENTITY`
  - `.STAFF_IDENTITY`
  - `.CORPORATE_BENEFIT`
  - `.OTHER_BENEFIT`
  - `.TEMP_IPD_VISITOR`

Example scoped permission: `CARD_ISSUE.PATIENT_IDENTITY`.

---

## 6. Approved sequence (Phase 0 governance)

1. Confirm taxonomy and ownership table with module owners.
2. Confirm registry boundary: shared registry + print, separate IPD temporary fee/refund flow.
3. Freeze product-code conventions and reserve `TEMP_IPD_VISITOR`.
4. Freeze RBAC matrix and permission-key naming.
5. Capture approvals from Product, Architecture, Security, and Operations leads.

---

## 7. Done criteria (Phase 0 exit)

Phase 0 is complete only when all are true:

1. **Finalized matrix document exists** (this document) and is linked from card requirements.
2. **Approved sequence is signed off** by Product + Architecture + Security + Operations.
3. **Permission matrix and permission keys are approved** for backend and frontend integration planning.
4. **Registry boundary decision is approved**: shared registry/print in `hospital-card-management-service`; IPD temporary fee/refund remains separate.
5. **Canonical product codes are frozen** and referenced by implementation plans.

---

## 8. Approval tracker (required for Phase 0 sign-off)

| Area | Required approver group | Approver | Date | Status | Notes |
|------|--------------------------|----------|------|--------|-------|
| Taxonomy and ownership freeze | Product | TBD | TBD | Pending | |
| Registry boundary decision | Architecture | TBD | TBD | Pending | |
| RBAC matrix and permission keys | Security | TBD | TBD | Pending | |
| Operational rollout readiness | Operations | TBD | TBD | Pending | |

**Rule:** Phase 0 is not fully closed until all rows are marked **Approved**.

---

## 9. Implementation status (current)

- Governance baseline document: **Done**
- Taxonomy and boundaries documented: **Done**
- Product-code conventions documented: **Done**
- RBAC matrix documented: **Done**
- Cross-document linkage from overview and implementation plan: **Done**
- Formal approval capture (table above): **Pending**

---

## 10. References

- [Hospital card programs - overview](hospital-card-types-overview.md)
- [Hospital Card Service - implementation plan](hospital-card-management-service-implementation-plan.md)
- [Patient identity card - registration](patient-identity-card-registration.md)
- [Staff / employee identity card](staff-identity-card.md)
- [Temporary Card Service - IPD](temporary-card-service-ipd.md)
- [Corporate Service & Card Management](corporate-service-and-card-management.md)
