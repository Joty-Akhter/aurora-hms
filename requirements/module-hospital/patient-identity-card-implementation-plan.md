# Patient identity card – implementation plan (phased)

**Requirements:** [Patient identity card – registration](patient-identity-card-registration.md)  
**Taxonomy:** [Hospital card programs – overview](hospital-card-types-overview.md)  
**Technical reference:** [hospital-card-management-service – implementation plan](hospital-card-management-service-implementation-plan.md)

---

## 1. Goal

Deliver **patient identity card issuance at registration** first, then extend the same platform to other card programs. This document is the **delivery plan**; functional rules remain in the requirements doc above.

**Principle:** Registration **always succeeds** even if card issuance fails; card steps are **retryable** without duplicating patients (requirements R2, R3, NFR availability).

---

## 2. Current baseline (updated as implemented)

| Area | Status |
|------|--------|
| `hospital-service` | **Done (MVP):** After `createPatient`, calls Hospital Card Service to issue or reuse **PATIENT_IDENTITY** card; `PatientResponse` includes `identityCardId`, `identityCardNumber`, `identityCardStatus`, `identityCardMessage`. Registration does **not** roll back if card issuance fails. |
| Liquibase `062-patient-identity-card-product-seed.sql` | **Done:** Seeds `PATIENT_IDENTITY` product id `a0000001-0001-4000-8000-000000000001`. |
| `hospital-card-management-service` | **Unchanged:** Issue path creates zero-balance `card_accounts` row (acceptable for identity). **Card number** set to **MRN** when issuing from registration. |
| Frontend | **Done:** Registration confirmation + optional print; **Patient Overview** and **Edit Patient** expose **print/reprint** and **replace** (audited via `POST …/reprint`). HTML card template (no PDF export yet). |
| **Not done yet** | Dedicated **PDF** export / thermal driver; template operational sign-off on paper; broader **automated test** coverage beyond smoke. |

Phase 1 **core** (auto-issue + API surface + user feedback) is in the repo; polish items remain below.

---

## 3. Phase 1 – Patient identity card (MVP) — **implement first**

### 3.1 Configuration and card product

1. Define a **card product** in `hospital-card-management-service` for **patient identity only** (e.g. code `PATIENT_IDENTITY`): medium **PHYSICAL** or **QR**, **no** wallet behavior (zero balance / skip or neutral `card_accounts` handling per technical design).
2. Document in service README: authorization paths **must not** treat this product as canteen/billing debit unless explicitly linked to a prepaid product later.
3. **Feature flag** (optional): `hospital.patient-identity-card.auto-issue.enabled` (default `true`) and `hospital.patient-identity-card.require-fee-before-print` (default `false` for MVP unless finance requires).

### 3.2 Data and persistence

**Option A (recommended for single registry):** On successful registration, call Hospital Card Service **`POST /api/hospital-card-management/cards`** with `cardProductId` = PATIENT_IDENTITY product, `ownerType=PATIENT`, `ownerReferenceId=patientId` (UUID string), auto-generated `cardNumber` if empty.

**Option B (lighter MVP):** Add table `ehr.patient_identity_card` (or equivalent) in **hospital-service** Liquibase only; sync to Hospital Card Service in a **later** sub-phase. Use when you must ship print **before** Feign to card service is ready.

Pick **one** path for MVP; avoid dual writes long-term.

### 3.3 Backend – `hospital-service`

1. After **`createPatient`** commits successfully, invoke a **`PatientIdentityCardService`** (new):
   - If feature flag on: call Hospital Card Service to **issue** card (or insert local row for Option B).
   - **Idempotent:** same `patient_id` → return existing active issuance (R2).
   - On **failure** (timeout, 5xx): log + expose optional `identityCardStatus=PENDING` on `PatientResponse` or separate GET; **do not** roll back patient.
2. Extend **`PatientResponse`** (or companion DTO) with optional: `identityCardId`, `cardNumber`, `identityCardIssuedAt`, `identityCardError` (message only for staff).
3. Emit event or log **`patient.identity_card.issued`** when issuance succeeds (requirements §6).

### 3.4 Backend – `hospital-card-management-service`

1. Ensure **issue** path supports **PATIENT_IDENTITY** product without requiring top-up.
2. If product implies **no monetary account**, either omit `card_accounts` row or create zero-balance account **read-only** for statements—document chosen rule.
3. **GET** card by owner for reprint: existing **`/cards/search?ownerReferenceId=&ownerType=PATIENT`** or add thin **internal** endpoint if needed for BFF/hospital-service.
4. **Replace flow** (minimal for MVP): **`POST /cards/{id}/replace`** with reason for lost/damaged; link `replaced_by_card_id` (already in model).

### 3.5 Frontend – registration UX

1. After successful **create patient**, show confirmation with **MRN** and:
   - **Print patient card** (primary),
   - **Print preview** (optional MVP+),
   - **Skip / Print later** if policy allows (requirements §3.3.1).
2. Print action:
   - Call backend to fetch **print payload** (HTML/PDF URL or base template + fields) **or** open browser print on client-rendered template with data from API.
3. Non-blocking message if user skips print.

### 3.6 Patient profile – reprint / replace (minimal)

1. **Reprint:** button loads current active card info and repeats print (audit `last_print_at` or log).
2. **Replace card:** wizard with reason → calls replace API → new card number, old invalidated (R1).

### 3.7 Security and RBAC

1. Issue/reprint: **`HOSPITAL_MANAGE`** or dedicated permission (align with existing hospital RBAC).
2. Scan/lookup by card number at reception: **`HOSPITAL_VIEW`** + patient search rules.

### 3.8 Testing

1. Unit: idempotency on duplicate createPatient calls (mock).
2. Integration: registration + card issue + search by `ownerReferenceId`.
3. E2E (optional): registration flow and print dialog.

### 3.9 Phase 1 exit criteria

- [x] New patient registration creates **one** active patient identity card record when auto-issue is on (idempotent via search by `ownerReferenceId` + `cardProductId` before POST).
- [x] **Card number** issued as **MRN** from registration (aligns patient master with printed identifier until a separate sequence is required).
- [ ] Physical **card template** layout verified on paper/PDF (operational sign-off).
- [x] Registration works if card service is down (**FAILED** status + message on `PatientResponse`).
- [x] Reprint and replace available from patient context (overview + edit) with **PRINT/REPRINT** audit via `POST …/reprint`.
- [~] Matches acceptance criteria in [patient-identity-card-registration.md](patient-identity-card-registration.md) §10 — **partially** (items 1–2, 4–6 addressed in MVP; item 3 “MRN on card” pending print template).

---

## 4. Phase 2 – Hardening and billing (optional)

1. **Issuance fee:** integrate `hospital-billing-service` charge before print when configured (requirements §5.4, R4).
2. **Kafka:** consumer on `patient.created` for async issuance (requirements §5.2) if sync path is insufficient at scale.
3. **Reporting:** cards issued / pending print / reprints (requirements §9).
4. **Template management:** admin UI for logo, colors, barcode format.

---

## 5. Later phases (order suggested)

| Phase | Scope | Spec |
|-------|--------|------|
| 3 | **Staff / employee identity card** — HR linkage, STAFF product, print | [staff-identity-card.md](staff-identity-card.md) |
| 4 | **Corporate benefit card** print + sync — corporate module + Hospital Card Service | [corporate-service-and-card-management.md](corporate-service-and-card-management.md) |
| 5 | **IPD temporary visitor** module — dedicated schema/fees/refunds | [temporary-card-service-ipd.md](temporary-card-service-ipd.md) |
| 6 | Wallet/prepaid enhancements, canteen authorization | [hospital-card-management-service-implementation-plan.md](hospital-card-management-service-implementation-plan.md) |

Do **not** start Phase 3–6 until Phase 1 exit criteria are met unless parallel teams are agreed.

---

## 6. File / service touchpoints (checklist)

| Layer | Artifacts |
|-------|-----------|
| Requirements | [patient-identity-card-registration.md](patient-identity-card-registration.md) (unchanged; this plan implements it) |
| DB | Liquibase in `hospital-service` if Option B; else `hospital_card` via existing migrations + seed `card_products` |
| `hospital-service` | `PatientService`, `PatientIdentityCardService`, DTOs, optional Feign client to card service |
| `hospital-card-management-service` | `PATIENT_IDENTITY` product seed, issue/validate rules for non-wallet |
| `frontend` | `PatientForm.tsx`, patient detail reprint/replace, optional `patientIdentityCardService.ts` |
| Gateway | Existing route `/api/hospital-card-management/**` — ensure token propagation for server-to-server calls |

---

## 7. Document history

- Initial plan: **Phase 1 = patient identity card first**; later phases sequenced for staff, corporate, IPD temporary, wallet.
