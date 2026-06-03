# Patient identity card – issuance at registration

**Status:** Final requirements  
**See also:** [Hospital card programs – overview](hospital-card-types-overview.md), [Patient Health Records](patient-health-records.md)

---

## 1. Purpose

### 1.1 Objective

When a **new patient is successfully registered** in the hospital system, the facility shall support issuing a **patient identity card** (physical and/or printable digital artifact) that **binds the person to their Medical Record Number (MRN)** and core demographics. The card is used for **identification** at subsequent visits, queues (OPD, lab, pharmacy), and operational workflows—not for IPD visitor deposits or wallet balances.

### 1.2 Distinction from other card programs

| Program | Relationship |
|--------|----------------|
| **IPD temporary visitor card** | Separate product: attendee passes, fees, return/refund. **Not** issued automatically at registration. See [Temporary Card Service – IPD](temporary-card-service-ipd.md). |
| **Corporate benefit card** | Contract-based eligibility; optional linkage to a registered patient, but **not** a substitute for the standard patient identity card unless hospital policy explicitly merges them. See [Corporate Service & Card Management](corporate-service-and-card-management.md). |
| **Wallet / prepaid card** | Stored value; optional. See [hospital-card-management-service implementation plan](hospital-card-management-service-implementation-plan.md). |

---

## 2. Scope

### 2.1 In scope

- **Trigger:** Successful completion of **new patient registration** (patient master record created; MRN assigned).
- **Default policy:** **One patient identity card issuance per successful registration**, unless configuration disables automatic issuance or staff explicitly defers printing.
- **Card content:** At minimum: **MRN**, **patient full name** (as stored in master), **date of birth**; optional fields per §4.
- **Identifiers:** A **card identifier** (number or token) unique within the hospital; may equal MRN or follow a separate numbering scheme (configurable).
- **Printing:** Immediate print after registration and/or **reprint** under policy (lost/damaged/updated card).
- **Audit:** Who issued/reprinted, when, and linkage to `patient_id` / MRN.

### 2.2 Out of scope

- Clinical content (diagnoses, allergies) on the card surface—optional barcode/QR may **point** to system lookups only, not full PHI on the card.
- IPD visitor card fees and refunds.
- Corporate contract negotiation.

### 2.3 Preconditions

- Patient registration workflow completes with a **persisted patient record** and **assigned MRN**.
- User has permission to **register patients** and (where applicable) **print identity cards** (may be the same or a subset role).

---

## 3. Functional requirements

### 3.1 Registration workflow integration

1. **Patient arrives**; staff captures demographics and required registration data (existing PHR process).
2. System **assigns MRN** and **creates** the patient record.
3. **Upon successful save** (transaction committed):
   - System shall **record an identity-card issuance event** for that patient (see §6).
   - If **automatic issuance** is enabled (default **on** unless configured otherwise), the system shall:
     - Generate or resolve **card identifier(s)** per §5.
     - Set initial **card status** to **ISSUED** (or **ACTIVE** if nomenclature is unified with card engine).
     - **Offer** or **automatically open** the **print** step (configurable: auto-print dialog vs. “Print card” button).
4. If **automatic issuance** is **disabled** org-wide or per site, staff may still **manually** trigger “Issue / print patient card” from the patient record after registration.

### 3.2 Business rules

| Rule ID | Rule |
|--------|------|
| R1 | **One active patient identity card per patient** for the standard program: issuing a new card for the same patient (replacement) shall **invalidate** or **supersede** the prior card in system records (historical rows retained for audit). |
| R2 | **Idempotency:** Duplicate registration submissions must **not** create duplicate active cards; the system shall detect same-session or duplicate API calls and return the existing issuance or a single new card per policy. |
| R3 | **Deferred printing:** If print fails (printer offline), issuance remains **recorded**; user can **reprint** from patient profile without re-registering. |
| R4 | **Optional issuance fee:** If the hospital charges for the physical card, **fee amount** and **Billing/Accounts posting** shall be configurable; registration may **block** printing until payment **or** allow print with deferred billing per policy. |
| R5 | **Duplicate patient workflow:** If registration proceeds under “acknowledge duplicate” (existing similar patients), **card issuance still follows** the **new** patient record that was created (the card links to that record’s MRN). |

### 3.3 User interface

#### 3.3.1 During / after registration

- After successful save, display **clear confirmation** including **MRN** and an action:
  - **Print patient card** (primary), and optionally **Skip** / **Print later** if policy allows.
- Show **non-blocking warning** if print is skipped (e.g. “Card not printed; reprint from patient profile”).
- Optional: **preview** of card layout (PDF/print preview) before physical print.

#### 3.3.2 Patient profile (existing patient)

- **Reprint card** action: available to authorized roles when patient is active.
- **Replace card** (lost/damaged): creates a **new** card identifier (or reuses policy), marks previous as **REPLACED** / **INVALID**, with **reason** and audit.

#### 3.3.3 Card layout (minimum content)

Configurable template, but must support:

| Element | Required | Notes |
|---------|----------|--------|
| Hospital name / logo | Yes | Branding |
| **MRN** | Yes | Human-readable, large enough for manual verification |
| **Patient full name** | Yes | As in master |
| **Date of birth** | Yes | Format configurable (locale) |
| **Card identifier** | Yes* | *If distinct from MRN, both may appear |
| Issue date | Recommended | |
| **Barcode or QR** | Recommended | Encodes MRN and/or card id + checksum for scanning at queues |

Optional: photo (from capture at registration if available), primary phone last four digits (policy-dependent; privacy review).

### 3.4 Encoding (barcode / QR)

- **Payload** shall be **non-sensitive** or **minimal**: e.g. MRN + card version + checksum, or internal card UUID—**not** full clinical data.
- Scanning at reception/lab/pharmacy shall **resolve to patient** via controlled service APIs (authorize by role).

---

## 4. Data model (logical)

Minimum persisted entities or attributes (implementation may map to `hospital-service` tables, a dedicated `patient_identity_card` table, or integration with `hospital-card-management-service` with a dedicated **card product** type `PATIENT_IDENTITY`):

| Field | Description |
|-------|-------------|
| `patient_id` | FK to patient master |
| `mrn` | Denormalized or joined for reporting |
| `card_identifier` | Unique key for print/scan (may match MRN or separate sequence) |
| `status` | e.g. ISSUED, REPLACED, INVALID |
| `issued_at` | Timestamp |
| `issued_by` | User id |
| `replaced_by_card_id` | Nullable; chain for replacements |
| `print_count` or last_print_at | Optional; supports reprint audit |

---

## 5. Integration and orchestration

### 5.1 Synchronous path (recommended for MVP)

- **Registration service** (e.g. `hospital-service` patient API), after commit, calls internal **PatientIdentityCard** component or **card issuance API** with `patientId`, `mrn`, demographics snapshot.
- Response includes **card_identifier** and **print payload** (or URL to rendered PDF).

### 5.2 Asynchronous path (optional)

- Publish **`patient.created`** (or `patient.registered`) event; **consumer** creates card record and queues print job.
- **Idempotency key:** `patient_id` to avoid duplicate cards on redelivery.

### 5.3 Use of `hospital-card-management-service` (recommended for platform consistency)

- When the hospital uses a **single card registry and print pipeline**, **patient identity** issuance should create a **card** row with **owner_type = PATIENT**, **owner_reference_id = patient_id**, and a **card product** of type **PATIENT_IDENTITY** (or equivalent) with **no balance / no wallet** semantics and **no** canteen authorization unless explicitly linked to a prepaid product.
- **Print templates** for patient, staff, and corporate benefit cards can then share one technical **print** path while keeping **business rules** in patient registration, HR, and corporate modules respectively.
- **Must** remain distinct from prepaid products in authorization and billing flows.

### 5.4 Billing

- If issuance fee applies: create **charge line** or **receipt** via Billing/Accounts; link `external_reference` to patient and card issuance id.

---

## 6. Events and audit

- Emit **`patient.identity_card.issued`** (or equivalent) with `patientId`, `mrn`, `cardIdentifier`, `issuedBy`, `issuedAt` for downstream analytics and portal display.
- Log **reprints** and **replacements** with user, timestamp, reason.

---

## 7. Role-based access

| Role | Permissions |
|------|-------------|
| Registration / Front desk | Register patient; print/reprint standard identity card |
| Supervisor / Admin | Replace card; void; configure templates; waive fees if applicable |
| Clinical staff | Typically **read** MRN from card scan; **no** issuance unless policy extends |

---

## 8. Non-functional requirements

- **Availability:** Card issuance must not block registration if card subsystem is down: registration **succeeds**; card step **queues** or shows **retry** (no data loss on patient).
- **Performance:** Issuance record + print trigger within **2 seconds** typical after registration commit (excluding physical printer speed).
- **Security:** Card payloads and scan APIs **authenticated**; audit access to **reprint** functions.
- **Privacy:** Lost-card workflow should allow **invalidating** scannable identifier while keeping patient record active.

---

## 9. Reporting

- **Cards issued by day/week/month** (registration volume vs. print volume).
- **Reprints and replacements** by user (fraud/abuse monitoring).
- **Pending print** (registered but not printed), if tracked.

---

## 10. Acceptance criteria (summary)

1. New patient registration **creates** patient record and **records** identity card issuance per policy **R1–R5**.
2. User can **print** immediately after registration and **reprint** from patient profile with audit.
3. **MRN** appears on card and matches master.
4. **Duplicate** registration does not yield **duplicate active** identity cards without explicit replacement flow.
5. Patient identity card **requirements** do not conflate with **IPD temporary visitor** or **wallet** cards.
6. Where the platform uses **`hospital-card-management-service`**, patient identity is represented as a **non-wallet** card product and shares the **print pipeline** with other identity or benefit cards per configuration.

---

## 11. Document history

- Introduces detailed requirements for **patient identity card at registration**, aligned with [Hospital card programs – overview](hospital-card-types-overview.md).
