# Temporary (Attendee / Visitor) Card Service – IPD

**Status:** Final requirements (supersedes generic “hospital card” descriptions for this use case)  
**See also:** [Hospital card programs – overview](hospital-card-types-overview.md)

---

## 1. Module purpose

### 1.1 Purpose

The **Temporary Card Service** manages **visitor / attendee cards** for **inpatients admitted to IPD**. For each admission, the patient’s attendants or guardians may receive **one or more** temporary cards. Cards are **fee-based**: a configurable per-card fee (typically **100–200 BDT**; hospital-configurable range). The fee is **refundable when the card is returned** according to policy.

The module ensures **traceability** of cards, **collection of fees**, **returns**, and **refunds**, with appropriate **role-based access** and integration with **IPD admission** and **Accounts / Billing**.

### 1.2 Objectives

- Issue and track temporary cards tied to **patient** and **IPD admission**.
- Collect and account for **card fees** at issuance.
- Process **returns** and **refunds** (automatic or manual per configuration).
- Support **operational and financial reporting** (issued, active, returned, refunds, lost).
- Enforce **admission validity** and **auditability** of all money movements.

---

## 2. Scope and preconditions

### 2.1 In scope

- IPD **visitor/attendee** cards only (not corporate benefit cards, not general wallet/prepaid cards; see [overview](hospital-card-types-overview.md)).
- Issuance, return, lost handling, fees, refunds, and reports for this program.

### 2.2 Preconditions

| Prerequisite | Requirement |
|----------------|-------------|
| Patient master | Patient exists in the system (searchable by name / ID). |
| IPD admission | **Active IPD admission** for the patient; issuance is blocked or strongly warned if no valid admission. |
| Admission identifier | Linkage to **admission number** (or equivalent IPD identifier) for each issuance. |
| Accounts / Billing | Post **receipts** for card fees and **refunds** with traceability to the card transaction. |
| Configuration | Valid **fee range** (e.g. min/max per card), optional **max cards per admission**, **numbering rules** for card numbers. |

### 2.3 Out of scope

- OPD-only visits without IPD admission (unless the hospital extends policy explicitly).
- Corporate contract cards (see [Corporate Service & Card Management](corporate-service-and-card-management.md)).
- Patient identity card at first registration (separate program).

### 2.4 Technical boundary vs `hospital-card-management-service`

Fee-based **IPD temporary visitor** cards use **payment and refund** semantics distinct from **prepaid wallet** cards. Implementations should use a **dedicated** temporary-IPD card store or schema (or module) aligned with Accounts, rather than overloading **wallet** `card_accounts` in `hospital-card-management-service` unless the hospital explicitly decides to unify them. See [Hospital card programs – overview](hospital-card-types-overview.md) and [hospital-card-management-service implementation plan](hospital-card-management-service-implementation-plan.md).

---

## 3. Functional requirements

### 3.1 Core features

1. **Generate** visitor/temporary cards for **admitted IPD patients** (attendants/guardians).
2. Assign **unique card number(s)** per physical card and link **patient**, **admission**, fee, and status.
3. Collect **configurable card fee per unit** within admin-defined bounds (e.g. 100–200 BDT).
4. Track **card status**: **Issued**, **Returned**, **Lost** (see §6).
5. On return: update status and initiate **refund** (full per policy; link to original payment).
6. **Role-based access**: Admin, Front Desk, Nurse/Ward, Accounts (see §8).
7. **Integration** with IPD admission master and Accounts module for payments and refunds.
8. **Reports**: issuance by period, active cards, returned cards, refund summary, lost cards as needed.

### 3.2 Workflow – issuance

1. Patient is **admitted**; **IPD admission record** exists.
2. Authorized staff opens **Temporary Card Service**.
3. **Generate / Create card(s)**:
   - Select **patient** (search by name or ID); system validates **active admission**.
   - Optionally select or confirm **Admission No** (must match a valid admission for that patient when provided).
   - Enter **number of cards** (≥ 1; optional system max).
   - Enter **card fee per unit** within configured range (e.g. 100–200 BDT).
   - System calculates **Total fee** = quantity × fee per unit (read-only).
   - Select **payment method**: Cash, Card, Other (configurable list).
4. On confirmation:
   - System generates **unique card number(s)** (one per card).
   - Creates issuance record(s); initial status = **Issued**.
   - Records **payment** in Accounts (total fee, method, user, timestamp).
5. Staff **prints** card(s) and hands to patient or guardian.

### 3.3 Workflow – return

1. Staff opens **Card return**.
2. Identify card by **scanning or entering card number** (future: QR on card; see §11).
3. System displays **patient name**, **admission no**, issue details, fee collected, current status.
4. Staff marks **Returned** (only if status is **Issued**).
5. **Refund**: per configuration, either **automatic posting** to Accounts or **manual confirmation** by Accounts/Cashier.
6. Refund methods may include **cash**, **card reversal**, or **account credit**, subject to hospital policy and integration capabilities.

### 3.4 Workflow – lost

1. Authorized role marks card as **Lost** (from Issued).
2. **Refund rules** for lost cards are **configurable** (default: no refund); overrides require Admin with audit.

### 3.5 Reporting

| Report | Content (minimum) |
|--------|-------------------|
| **Cards issued** | Filters: date range, ward, user, payment method. Columns: card no., patient, admission no., issue date, qty, fees, issued by. |
| **Active cards** | Status = Issued (not returned/lost); supports security and discharge checks. |
| **Returned cards** | Return date, refund amount, refund method, processed by. |
| **Refund summary** | Totals and breakdowns by period, department, user. |
| **Lost / outstanding** (optional) | Lost cards; long-outstanding Issued past configurable threshold. |

---

## 4. User interface

### 4.1 Card generation form

| Field | Description | Type | Required | Validation / behavior |
|-------|-------------|------|----------|------------------------|
| Patient name / ID | Select patient | Searchable dropdown / search | Yes | Must exist in Patient Master; **must have active IPD admission** for issuance. |
| Admission No | IPD admission identifier | Text or dropdown from patient’s admissions | Optional* | If shown, must match valid admission; *strongly required in practice for audit. |
| Number of cards | Quantity to issue | Integer | Yes | ≥ 1; optional max from config. |
| Card fee per unit | Fee per card | Decimal | Yes | Within configured min–max (e.g. 100–200 BDT). |
| Total fee | Auto total | Decimal, read-only | Yes | Number of cards × fee per unit. |
| Payment method | How fee is collected | Dropdown | Yes | e.g. Cash, Card, Other. |
| Generate / Create | Submit issuance | Button | — | Enabled only when form valid; creates records, posts payment, enables print. |
| Print temporary card(s) | Print labels/cards | Button / flow | — | After successful creation; includes card number and required patient/admission identifiers. |

### 4.2 Card return form

| Field | Description |
|-------|-------------|
| Card number | Scan or type; primary lookup key. |
| Patient name / Admission no | Read-only after lookup. |
| Current status | Display Issued / Returned / Lost. |
| Mark returned | Action: Issued → Returned. |
| Refund | Triggers refund workflow per §3.3; prevent duplicate refunds. |

### 4.3 Access by role (summary)

| Role | Typical permissions |
|------|---------------------|
| **Admin** | Full access; configuration; overrides with audit. |
| **Front Desk** | Issue cards; initiate return; mark returned. |
| **Nurse / Ward** | Issue/return per hospital policy (may mirror Front Desk). |
| **Accounts** | Payment/refund execution, reconciliation, reports. |

---

## 5. Business rules

1. **Admission**: Issuance requires an **active IPD admission** for the selected patient; otherwise block with clear message.
2. **Unique card numbers**: Each card number is **globally unique** unless hospital explicitly defines reuse policy (not recommended by default).
3. **Fee range**: Enforced by configuration (e.g. 100–200 BDT); hospital may adjust bounds.
4. **Status transitions**:
   - **Issued** → **Returned** or **Lost**.
   - **Returned** is terminal for that card; new access requires **new issuance**.
5. **Refunds**: Tied to original collection; **no double refunds**; refund amount per policy (typically full fee on valid return).
6. **Audit**: Issuance, status changes, payments, and refunds are logged (user, time, amounts).

---

## 6. Data and status model

### 6.1 Card status (enum)

- **Issued** – Active; fee collected; not returned.
- **Returned** – Card returned; refund processed per policy.
- **Lost** – Card reported lost; refund rules per configuration.

### 6.2 Minimum persisted data per card instance

- Unique **card number**
- **Patient** reference
- **Admission** reference
- **Issue** timestamp and **issued by**
- **Fee** per unit, **quantity** (or one row per card), **total**, **payment method**, **payment reference** to Accounts
- **Status**, **return** timestamp (if applicable), **refund** reference(s)
- **Lost** flag/timestamp if applicable

---

## 7. Integrations

| System | Integration need |
|--------|------------------|
| **IPD admission** | Validate admission; resolve Admission No; optional ward/bed display. |
| **Accounts / Billing** | Post card fee revenue; post refunds; reconciliation keys. |
| **Patient master** | Resolve patient identity for search and display. |

---

## 8. Non-functional requirements

- **Performance**: Issuance and return flows usable under front-desk peak load; lookups by card number &lt; agreed SLA (e.g. 2 s typical).
- **Security**: Role-based access; sensitive operations audited.
- **Consistency**: Payment and refund states consistent with Accounts; idempotent refund processing.
- **Availability**: Module degrades gracefully if Accounts is slow (queue or clear error; no silent money loss).

---

## 9. Error handling

- Invalid or missing patient / admission: **block** issuance; show actionable message.
- Unknown or duplicate card number on return: **no** duplicate refund; clear error.
- Invalid fee or quantity: **block** submit with field-level errors.
- Already returned/lost card: **block** repeat return/refund.

---

## 10. Future enhancements

- **QR code** on card for fast return and audit (encode card number ± admission id).
- **Bulk issuance** UX for multiple cards under one payment transaction with per-card IDs.
- **Notifications** (SMS/app) when cards are overdue for return after discharge.
- **RFID / smart card** integration for access control and automated return detection.

---

## 11. Document history

- Consolidates draft sections 1.1–1.8 (purpose, features, workflows, forms, technical notes, future items) into a single **final** specification for the Temporary (IPD visitor) Card Service module.
- Aligned with [Hospital card programs – overview](hospital-card-types-overview.md) so this module is not confused with patient identity cards or corporate cards.
