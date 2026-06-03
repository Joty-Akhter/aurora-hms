# Hospital staff / employee identity card

**Status:** Final requirements  
**See also:** [Hospital card programs – overview](hospital-card-types-overview.md), [Integration with Accounting, Inventory, and HR Services](integration-services.md)

---

## 1. Purpose

### 1.1 Objective

The hospital shall provide an **employee / staff identity card** program: a **physical or printable credential** that identifies a person as **authorized hospital personnel** for use inside the facility (corridors, staff-only areas, canteen identification, security checkpoints, and operational workflows). The card is **linked to the staff/employee master record** (employee identifier, name, department, role) and is **separate** from patient cards, IPD visitor passes, corporate benefit cards, and optional wallet/prepaid cards.

### 1.2 Distinction from other programs

| Program | Relationship |
|--------|----------------|
| **Patient identity card** | For **patients** at registration (MRN). Staff card **must not** reuse patient card templates or numbering spaces without clear separation. See [Patient identity card – registration](patient-identity-card-registration.md). |
| **IPD temporary visitor card** | For **attendants/guardians** of inpatients; fee and return/refund rules. Not for employees. See [Temporary Card Service – IPD](temporary-card-service-ipd.md). |
| **Corporate benefit card** | For **corporate beneficiaries** under B2B contracts; not the same as **internal employment** identification. See [Corporate Service & Card Management](corporate-service-and-card-management.md). |
| **Wallet / prepaid (staff)** | Optional **stored-value or meal subsidy** on a **card product** may **reference** the same person, but the **staff identity card** requirement is **identification and facility access**, not balances. See [hospital-card-management-service implementation plan](hospital-card-management-service-implementation-plan.md). |

---

## 2. Scope

### 2.1 In scope

- **Issuance** of a staff identity card when an employee is **eligible** per hospital policy (typically: active employment record exists).
- **Card content** and **layout** (branding, photo, employee id, department, role, validity).
- **Lifecycle:** issue, **active**, **suspended** (e.g. long leave, investigation), **revoked** (termination, resignation), **replaced** (lost/damaged/defective), **expired** (if validity is time-bound).
- **Reprint** and **replacement** with full **audit** (who, when, reason).
- **Role-based access** for HR, Admin, Security, and designated issuance desks.
- **Reporting:** issued, active, suspended, revoked, replaced, expiring-soon.

### 2.2 Out of scope (unless extended by separate spec)

- **Physical access control** (doors, turnstiles): integration with ACS/biometrics is **optional**; this document defines **identity presentation** and **system records**; vendor-specific door hardware is an integration layer.
- **Payroll and HR policy** beyond what is needed to resolve **employee id**, **status**, and **department** for the card.
- **Clinical** system privileges (those remain in RBAC / user management; the card may **display** role but does not **grant** clinical permissions by itself).

### 2.3 Preconditions

| Prerequisite | Requirement |
|--------------|-------------|
| **Staff / employee master** | Person exists with a **unique employee identifier** (or staff ID) in HR or hospital staff master. |
| **Employment status** | Issuance rules shall respect **active** employment; **suspended/revoked** employment drives card status per §5. |
| **User account (optional)** | Link to platform **user id** where the employee has system access; supports audit and future SSO. Not always mandatory for non-system users (e.g. outsourced housekeeping) if policy allows. |
| **Photo** | Captured or uploaded per hospital policy; **required** unless policy explicitly allows provisional card without photo for a limited period. |

---

## 3. Functional requirements

### 3.1 Eligibility and triggers

1. **Default trigger:** **New employee onboarding** completed to the point where **employee record is active** in staff master (configurable milestone: e.g. after HR approval or first day of work).
2. **Reissue triggers:**
   - **Replacement:** lost, stolen, damaged, name change, department transfer, role change, **photo update**.
   - **Renewal:** **expiry date** reached or upcoming (configurable reminder window).
3. **Block issuance:** If employment status is **not active** (terminated, resigned, blacklisted), **no new** active card shall be issued unless **Admin override** with audit (rare exceptions).

### 3.2 Card content (minimum)

Configurable template; minimum elements:

| Element | Required | Notes |
|---------|----------|--------|
| Hospital name / logo | Yes | Branding |
| **Employee full name** | Yes | As in staff master |
| **Employee ID** | Yes | Human-readable; may match HR employee number |
| **Department / unit** | Yes | Primary assignment; secondary line optional |
| **Job title / role** | Recommended | Display label; may differ from RBAC role names |
| **Card number** | Yes | Unique identifier for the **credential** (may differ from employee id) |
| **Issue date** | Yes | |
| **Valid until** | Optional | If policy uses fixed validity (e.g. 1–3 years) |
| **Photo** | Yes* | *Unless provisional policy §2.3 |
| **Barcode or QR** | Recommended | Encodes employee id or card id + checksum for scanning |

Optional: blood group (for emergency staff), emergency contact (policy-dependent; privacy review).

### 3.3 Status model

| Status | Meaning |
|--------|---------|
| **ISSUED** | Printed; not yet collected (optional) |
| **ACTIVE** | Valid for identification |
| **SUSPENDED** | Temporarily invalid (leave, card reported suspicious, HR hold) |
| **REVOKED** | Employment ended or card invalidated; **not** valid |
| **REPLACED** | Superseded by a newer card record |
| **EXPIRED** | Past valid-until date if used |

**Rules:**

- At most **one ACTIVE** staff identity card per employee for the standard program (unless hospital explicitly supports **multiple concurrent roles** with separate card types—out of scope unless configured).
- **Replacement** sets previous card to **REPLACED** (or **REVOKED**) with link to new card.

### 3.4 User interface

#### 3.4.1 Issuance / search

- Search by **employee name**, **employee id**, **department**, **card number**.
- **Issue new card** wizard: select employee → validate eligibility → confirm photo and fields → **print** / **send to queue**.
- **Bulk renewal** (optional): filter expiring cards, batch print (admin).

#### 3.4.2 Replacement

- **Reason** required: Lost / Stolen / Damaged / Name change / Department transfer / Photo update / Expiry renewal.
- **Stolen:** optional **invalidate** old card number immediately in access systems; audit log.
- **Fee:** optional **replacement fee** via Billing/Accounts (configurable).

#### 3.4.3 Suspension and revocation

- **Suspended:** HR or Security action; card scans show **invalid** at verification points.
- **Revoked:** on **termination** or **exit clearance**; integrate with HR exit workflow when available.

### 3.5 Verification

- At facility checkpoints or reception, authorized staff may **scan** card or enter **card number** / **employee id** to confirm **ACTIVE** status and **photo match** (read-only view).
- **No** patient PHI on staff verification screen.

---

## 4. Integration

### 4.1 HR / staff master

- **Source of truth** for **name**, **employee id**, **department**, **title**, **employment status**, **photo**.
- **Inbound events (optional):** `employee.activated`, `employee.terminated`, `employee.updated` → update card eligibility or trigger revocation/suspension.

### 4.2 User management / RBAC

- Link **staff identity card** record to **user id** when present for **audit** and **portal** (“my profile” reprint request—optional).

### 4.3 `hospital-card-management-service` (recommended for registry and print)

- Store a **card** row with **owner_type = STAFF**, **owner_reference_id = employee id** or **user id**, and a **card product** such as `STAFF_IDENTITY` with **no wallet** semantics (or a separate **wallet** product if the hospital issues a combined credential—explicit configuration only).
- **Authorization** APIs for canteen debits must **not** treat staff identity card as wallet unless explicitly linked to a **wallet product**.
- **Printing** should use the same **Hospital Card Service** print/template mechanism as [corporate printed benefit cards](corporate-service-and-card-management.md) and [patient identity cards](patient-identity-card-registration.md) when the platform standardizes on one service.

### 4.4 Printing

- Support **standard card printer** formats (PDF, label, CR80 layout) and **reprint** from audit-approved UI; align with §4.3 when templates are centralized.

---

## 5. Business rules

| ID | Rule |
|----|------|
| S1 | **Unique card number** per issued credential (global uniqueness within hospital). |
| S2 | **Terminated** employees: **ACTIVE** cards must move to **REVOKED** (batch or event-driven). |
| S3 | **Suspended** employment → **SUSPENDED** card status (or **REVOKED** per policy). |
| S4 | **Idempotency:** Issuance API shall not create duplicate **ACTIVE** cards for the same employee without **replacement** flow. |
| S5 | **Provisional card** (no photo): allowed only if **expiry** is short and **flag** visible on card; full card required afterward. |

---

## 6. Role-based access

| Role | Permissions |
|------|-------------|
| **HR** | Issue, replace, renew, suspend; view all reports |
| **Admin** | Full configuration; overrides; revoke |
| **Security / Facility** | Suspend/verify; view active status; optional limited issuance |
| **Department head** | Request replacement for direct reports (approval workflow optional) |
| **Employee (self)** | Optional: request reprint via portal (approval + fee if applicable) |

---

## 7. Non-functional requirements

- **Security:** Staff photos and PII **protected**; access to issuance/revocation **audited**.
- **Performance:** Lookup and verification **under agreed SLA** (e.g. &lt; 2 s) at peak shift change.
- **Availability:** HR outage **must not** delete card records; **offline verification** (cached last-known status) optional for security desks.
- **Privacy:** Lost/stolen workflow **invalidates** scannable identifier promptly.

---

## 8. Reporting

- Staff cards **issued / active / suspended / revoked** by period and department.
- **Replacements** by reason (loss vs damage vs renewal).
- **Employees without active card** while employment active (compliance gap).
- **Expiring** cards (if validity used).

---

## 9. Acceptance criteria (summary)

1. Eligible **active** employees can receive a **staff identity card** with required fields and unique card number.
2. **Termination** and **suspension** drive **revocation** or **suspension** of the card in system records.
3. **Replacement** retires prior card with audit and reason.
4. Staff card program is **clearly separated** from patient, visitor, corporate benefit, and wallet programs in data and UX.
5. Verification UI confirms **ACTIVE** status **without** exposing unrelated systems.

---

## 10. Future enhancements

- Integration with **door access** (ACS), **turnstiles**, **parking**.
- **Mobile soft badge** (digital staff card) with rotating QR.
- **Visitor management** tie-in (distinguish staff vs visitor at kiosk).

---

## 11. Document history

- Adds **hospital staff / employee identity card** requirements under [Hospital card programs – overview](hospital-card-types-overview.md).
