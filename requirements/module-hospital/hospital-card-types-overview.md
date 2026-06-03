# Hospital card programs – overview

## 1. Purpose

The hospital uses **several distinct card programs**. They must not be conflated: each has different triggers, money flows, lifecycles, and integrations. This document classifies them and points to the authoritative requirements for each.
Phase-0 governance baseline (taxonomy freeze, registry boundary, product-code convention, and RBAC matrix) is defined in **[Card management Phase 0 - Foundation and governance](card-management-phase-0-foundation-governance.md)**.

**Supersedes:** Any undifferentiated “single hospital card” requirement that mixed IPD visitor fees, patient identity, corporate benefits, and wallet/prepaid balances into one model.

---

## 2. Card program taxonomy

| Program | Primary purpose | Typical trigger | Payment / refund | Authoritative spec |
|--------|------------------|-----------------|------------------|----------------------|
| **Patient identity card** | Permanent patient identification (MRN-linked); used for visits, lab, pharmacy queues | Patient registration (one card per registered patient, subject to policy) | Usually issuance fee only if hospital charges for plastic; not IPD visitor deposit | **[Patient identity card – registration](patient-identity-card-registration.md)** |
| **Staff / employee identity card** | Facility credential for **hospital personnel** (name, employee id, department, photo); security and on-site identification | Active **employment** in staff/HR master; onboarding milestone | Optional replacement fee; not visitor deposit | **[Staff / employee identity card](staff-identity-card.md)** |
| **Temporary (visitor / attendee) card – IPD** | Physical visitor passes for attendants/guardians of admitted inpatients; fee-based and refundable on return | Active **IPD admission**; staff issues from Temporary Card module | Per-card fee (e.g. 100–200 BDT range, configurable); **refund on return** | **[Temporary Card Service – IPD](temporary-card-service-ipd.md)** |
| **Corporate benefit card** | Eligibility and discount/benefit tier for **corporate offices and named beneficiaries**; **physical card printed** for identification at billing | Corporate enrollment, policy, issuance | Per corporate agreement; not IPD visitor deposit | **[Corporate Service & Card Management](corporate-service-and-card-management.md)** — generation/print via **Hospital Card Service** |
| **Other printed benefit cards** (optional) | Promotional, loyalty, or partner benefit schemes | Program-specific enrollment | Per program | Same **Hospital Card Service** print pipeline; **distinct** products/config from corporate (see corporate doc §6.2a) |
| **Wallet / prepaid / canteen-style card** (optional) | Stored value or entitlements for hospital services, canteen, etc. | Product issuance, top-up | Balance, limits, authorizations | Implementation-oriented: `hospital-card-management-service` (see [implementation plan](hospital-card-management-service-implementation-plan.md)); **not** a substitute for IPD temporary visitor cards |

**Hospital Card Service (`hospital-card-management-service`):** One **technical** service may host (a) **wallet/prepaid** products with balances and authorizations, (b) **printed benefit** records (corporate / other) tied to billing discounts, and (c) optional **identity-only** card products (patient/staff) with **no wallet**—each distinguished by **card product** configuration. **IPD temporary visitor** cards remain a **separate business module** (fee/refund); they are **not** modeled as prepaid wallet cards unless the hospital explicitly decides otherwise.

---

## 3. Scope boundaries

1. **Temporary IPD visitor cards** are **admission-scoped operational passes** with **deposit-like fees** and **return/refund** workflows. They are **not** the same as the **patient identity card** issued at registration.
2. **Corporate benefit cards** follow **contract and discount** rules and are **printed** for beneficiaries; **generation and print** go through the **Hospital Card Service module**; they do not replace IPD visitor card fees unless explicitly designed (future integration).
3. **Wallet/prepaid** services (if used) are **financial instrument** cards; they do not satisfy IPD visitor access control unless the hospital explicitly maps one program to the other (out of scope unless stated).
4. **Staff identity cards** identify **employees**; they are **not** patient cards and **not** IPD visitor passes; wallet features for staff (canteen) are optional and separate.

---

## 4. Implementation notes (non-binding)

- **Temporary Card Service** may be implemented as its own module or service, or as part of IPD/admission workflows, but its **requirements** are defined in [temporary-card-service-ipd.md](temporary-card-service-ipd.md).
- **Patient identity card at registration** is specified in [patient-identity-card-registration.md](patient-identity-card-registration.md); integration points (MRN, demographics) belong with **patient master**, not with IPD visitor fee logic.
- **Staff identity cards** are specified in [staff-identity-card.md](staff-identity-card.md); master data and employment status come from **HR / staff** services.
- **Corporate and other printed benefit cards** are specified in [corporate-service-and-card-management.md](corporate-service-and-card-management.md) (Module 5–6 and **§8 Integration Summary**, item **56** — Hospital Card Service); **Hospital Card Service** centralizes card record + print for those programs when that integration is used.
- **Patient** and **staff** identity cards are specified in [patient-identity-card-registration.md](patient-identity-card-registration.md) and [staff-identity-card.md](staff-identity-card.md); they may use the same service for **registry + print** (identity products without balance).

---

## 5. Related documents

- [Card management Phase 0 - Foundation and governance](card-management-phase-0-foundation-governance.md) — finalized taxonomy, ownership, product code convention, registry boundary, RBAC baseline, and done criteria  
- [Patient identity card – registration](patient-identity-card-registration.md) — **final** requirements for card issuance when registering a patient  
- [Staff / employee identity card](staff-identity-card.md) — **final** requirements for hospital employee/staff facility cards  
- [Temporary Card Service – IPD Visitor Cards](temporary-card-service-ipd.md) — **final** functional requirements for IPD temporary visitor cards  
- [Corporate Service & Card Management](corporate-service-and-card-management.md) — corporate card types and policies  
- [Patient Health Records](patient-health-records.md) — registration and demographics  
- [Admission – IPD](admission-ipd.md) — admission records that temporary cards must link to  
- [Hospital Card Service – implementation plan](hospital-card-management-service-implementation-plan.md) — technical APIs, schema `hospital_card`, wallet vs identity product patterns  
- [Patient identity card – implementation plan](patient-identity-card-implementation-plan.md) — phased delivery; Phase 1 patient card wired in `hospital-service` + registration UI alert  

---

## 6. Which document is authoritative?

| Topic | Authoritative spec |
|--------|---------------------|
| Patient identity at registration | [patient-identity-card-registration.md](patient-identity-card-registration.md) — delivery: [patient-identity-card-implementation-plan.md](patient-identity-card-implementation-plan.md) |
| Staff / employee facility badge | [staff-identity-card.md](staff-identity-card.md) |
| IPD visitor fee cards | [temporary-card-service-ipd.md](temporary-card-service-ipd.md) |
| Corporate benefit cards, policies, discounts | [corporate-service-and-card-management.md](corporate-service-and-card-management.md) |
| Service APIs, DB, phases for `hospital-card-management-service` | [hospital-card-management-service-implementation-plan.md](hospital-card-management-service-implementation-plan.md) |
| Taxonomy and cross-program rules | This overview |
