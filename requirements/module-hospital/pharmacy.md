## Pharmacy Module

### 1. Overview

**Description**  
The Pharmacy Module manages end-to-end medicine and related consumable operations for both outpatient and inpatient settings, across one **Central Pharmacy/Store** and multiple **Outlet Pharmacies** (OPD/IPD/departmental). It integrates with EHR, Billing, Store/Inventory, and Accounts to ensure safe dispensing, accurate stock control, centralized approval, and accounting‑ready reporting.

**Objectives**

- Ensure safe and accurate dispensing of medicines based on prescriptions, requisitions, and department issues.
- Maintain real-time, location-wise stock information for Central Store and each Outlet Pharmacy.
- Support regulatory compliance for controlled/scheduled medicines.
- Provide clear financial and operational visibility (sales, margins, stock, expiry, dues).
- Enforce approval and audit controls on all inter‑store movements and stock adjustments.

**High-Level Process Flow**

- Supplier → Central Store: Receive goods, create GRN, and update warehouse stock.
- Central Store → Outlet Pharmacies: Approve outlet requisitions, dispatch stock, and update both ledgers.
- Outlet ↔ Outlet: Raise, approve, and post inter‑pharmacy transfers without allowing negative stock.
- Outlet → Patient / Department: Dispense/issue medicines to OPD patients, IPD patients, and departments with appropriate billing or cost-centre tagging.
- Returns & Adjustments: Process patient returns, company returns, and stock adjustments with reasons, approvals, and audit logging.

### 1.1 Service boundaries and identifier alignment

This document describes **hospital-wide pharmacy business processes** (central store, outlets, procurement, dispensing). **Implementations are split across microservices**; the following boundary avoids duplicating ownership of PO/GRN/warehouse logic while keeping identifiers consistent.

| Concern | Owning context (requirements / implementation) | Notes |
|--------|-------------------------------------------------|--------|
| **Drug / medicine master, formulary rules, outlet pharmacy locations** | `hospital-pharmacy-service` (see `hospital-pharmacy-service-implementation-plan.md`) | Authoritative for **retail pharmacy** drug records used at counters and in prescribing UIs that read the same catalog. |
| **Pharmacy location stock, batch-level stock, dispensing orders/lines, patient returns at the counter** | `hospital-pharmacy-service` | Dispense orders reference clinical and billing identifiers by **ID only** where present; **`prescription_id` may be null** per **§1.3**. Pricing and clinical validation rules are not re-embedded here. |
| **Prescriptions, medication lines, e-prescribing transmissions, inbound fill-status from networks** | `hospital-service` | Clinical source for **prescription_id**, **prescription_line_id**, prescriber, DAW/substitution intent, diagnosis links. |
| **Patient, encounter/visit, admission** | `hospital-service` (and scheduling/admission services as applicable) | **patient_id**, **visit_id** / **encounter_id**, and for IPD **admission/account** identifiers must align with billing and EHR. |
| **Charges, invoices, payments, credit, refunds** | `hospital-billing-service` | **charge_id** / invoice line references for pharmacy sales; pharmacy exposes **billable dispense data** to billing, not final prices, unless configured otherwise. |
| **Global PO, GRN, supplier returns, central warehouse stock, hospital main store** | **Hospital Main Store / central inventory** (`hospital-main-store.md` and related inventory services) | Requisitions **to** central store, **GRN**, supplier **PO** lifecycle, and **warehouse-level** valuation live here—not in `hospital-pharmacy-service`. Outlet pharmacies **receive** into `hospital-pharmacy-service` stock via agreed integration contracts (receipt APIs or events). |

**IDs that must align end-to-end**

- **patient_id** — Same UUID (or platform patient key) in EHR, dispensing, and billing.
- **visit_id** / **encounter_id** — Links OPD dispensing to the encounter used for clinical context and, where applicable, charge attribution.
- **prescription_id** / **prescription_line_id** — **Optional** on a dispense order when organizational policy allows dispensing **without** an EHR prescription (e.g. walk-in/OTC, cash sale, or paper-only workflow). When present, created in `hospital-service`; lines reference them for prescription-based sales and clinical sync. See **§1.3**.
- **pharmacy_location_id** (or equivalent outlet id) — Stock ledger and dispense order location; transfers between outlets use two locations’ IDs per movement records.
- **dispense_order_id** / **dispense_line_id** — Owned by `hospital-pharmacy-service`; billing should reference these when creating or reversing charges.
- **charge_id** / **invoice line** — Owned by billing; returns and adjustments reference the originating charge or invoice line.

### 1.2 Two fulfillment channels (in-house vs external / e-prescribing)

Medication may be fulfilled either **inside the organization** (hospital pharmacy dispenses from stock) or **outside** (prescription transmitted to a retail chain or mail-order pharmacy that reports status back). The patient and clinicians should see **one coherent medication timeline** without duplicate “fills” or conflicting statuses.

| Channel | Source of truth for “fill” state | How billing/clinical views update |
|--------|-----------------------------------|-------------------------------------|
| **In-house dispensing** | **`hospital-pharmacy-service`** dispense lines and order completion | When a **`prescription_id`** is linked: on post or completion, **`hospital-service`** prescription/transmission records (or medication administration summaries) should be updated via an **internal integration** (API or domain event)—equivalent in effect to a fill-status update—so PHR and prescribing UIs match stock reality. **Skip** this sync when there is no linked prescription (**§1.3**). |
| **External / network pharmacy** | **Inbound fill-status** (e.g. webhook or network message) handled by **`hospital-service`** e-prescribing / transmission entities | See **Prescription Management** (`prescription-management.md`) for **FR-P3.11a**-style callbacks; no duplicate manual fill in `hospital-pharmacy-service` for the same transmission. |

**Rules**

1. **Single active fulfillment path per prescription line** — For a given `prescription_line_id`, either an in-house dispense line **or** an external fill-status stream applies; the system must **not** double-count quantity filled when both channels exist (e.g. after route change, cancel the superseded fulfillment path in audit).
2. **Unified timeline** — Patient chart and billing show **one** progression (pending → in progress → filled/partially filled/picked up/cancelled) with a **discriminant** (`fulfillment_channel`: `IN_HOUSE` | `EXTERNAL_NETWORK`).
3. **Source of truth** — **Clinical/legal quantity dispensed** for in-house: pharmacy stock movement + dispense lines. **Financial charge** for the patient: **`hospital-billing-service`** once billable items are posted from the dispense completion event or API.
4. **Reconciliation** — If external pharmacy reports **PARTIALLY_FILLED** and the patient later completes in-house, requirements for **residual quantity** and **charge adjustments** are owned by billing + prescription policy (documented in prescription and billing specs).

5. **No linked prescription** — If the dispense order has **no** `prescription_id`, **§1.2** rules that depend on updating `hospital-service` prescription/transmission state **do not apply**; billing and stock movements remain authoritative for that sale.

### 1.3 Prescription linkage and regional validation (configuration)

- **Dispensing without a matching prescription ID** — The platform shall support **cash / walk-in / counter sales** and **department issues** where **`prescription_id` is null** (or omitted), subject to **organization policy** and local law. Pharmacists record **drug, batch, quantity, and patient or customer context** as configured; this is **not** an error state when policy allows it.

- **Validation is not universal** — In some jurisdictions, **paper prescriptions cannot be validated** against a central registry or the EHR at point of sale (e.g. **Bangladesh**: national electronic prescription validation is not assumed). Configuration shall allow:
  - **Optional** prescription capture (reference number, image scan, free text) for audit only, **without** blocking dispense when validation is unavailable or not required by law.
  - Clear separation between **“soft” checks** (warnings, counselling prompts) and **hard blocks** (only where regulation and hospital policy mandate).

- **When EHR prescribing is used** — If the hospital uses **`hospital-service` prescriptions**, linking **`prescription_id`** enables substitution rules, line-level states vs ordered meds, and fill-status coherence (**§1.2**). This path is **recommended** where available but **not** mandatory for every sale in every deployment.

---

### 2. Master Data & Configuration

- **Medicine / Product Master (Shared Drug Database)**
  - Purpose:
    - Acts as the single, authoritative **Drug / Medicine database** for the entire Hospital Module.
    - Is used for:
      - Clinical prescribing in **Prescription Management / EHR**.
      - Stocked products, purchasing, and dispensing in the **Pharmacy Module**.
  - Fields: Product ID (system-generated, read-only), Product Code (optional human-readable code), Product Name (unique, required), Generic Name (required), Strength & Dosage Form (e.g., 500mg Tablet), Pack Size, Unit, HSN/Tax Code, MRP, Default Purchase Price, Default Selling Price, Min/Max stock level, Reorder level, Controlled Drug flag, Active/Inactive.
  - Batch/expiry configuration:
    - Batch Required (Yes/No – default Yes).
    - Expiry Required (Yes/No – default Yes).
  - Manufacturer / company linkage:
    - Mandatory link to an active Company/Manufacturer.
  - Business rules:
    - Product Name must be unique.
    - Inactive products cannot be selected in transactions (purchase, receive, sale, transfer).
  - Link to:
    - Therapeutic group, manufacturer, supplier(s).
    - Cost center and GL accounts (via configuration).

- **Location & Store Mapping**
  - One Central Store (main warehouse) and multiple Outlet Pharmacies (e.g., OPD Pharmacy, IPD Pharmacy, OT Pharmacy, departmental pharmacies).
  - Each pharmacy/location maintains its own stock ledger and closing balance.
  - Mapping of users/roles to one or more pharmacies with appropriate permissions (Central Store Admin, Outlet Pharmacist, Approver, Auditor).

- **Supplier / Company Master**
  - Shared with Store/Inventory; visible here for purchase and returns.
  - Core fields:
    - Company ID (system-generated, unique, read-only).
    - Company Name (required, unique).
    - Contact Info (phone, email, address with basic format validation).
    - License No (required, alphanumeric, unique).
    - Status (Active/Inactive).
  - Business rules:
    - Inactive companies cannot be used for purchase orders, GRNs, or returns.

- **Unit & Conversion Master**
  - Base units (tablet, vial, ml, etc.) and pack-to-unit conversion.

### 3. Procurement & Stock Management

#### 3.1 Medicine Receive (Purchase)

- Receive stock against approved Purchase Orders from Store/Inventory or via direct entry (as per configuration).
- Capture batch details:
  - Batch No, Expiry Date, MRP, Purchase Rate, Tax %, Free quantity, Total quantity.
- Validate:
  - Expiry not in the past.
  - Duplicate batches handled as per policy (allow or merge).
- Generate Goods Receipt Note (GRN) and update stock and valuation.

#### 3.2 Transfers

- **Medicine Transfer Between Pharmacies/Stores**
  - Initiate transfer from source location with list of items and quantities.
  - Approve and receive at destination; stock adjusted at both ends.
  - Maintain transfer history for audit and reporting.
  - Support both:
    - Central Store → Outlet Pharmacy (distribution based on approved requisitions).
    - Outlet ↔ Outlet (inter‑pharmacy transfers) with issuing‑pharmacy approval.

#### 3.3 Returns to Supplier

- Create return notes for:
  - Expired items.
  - Damaged items.
  - Excess stock.
- Support partial or full batch returns.
- Adjust stock and trigger corresponding financial entries via Accounts.
 - Require supervisor/manager approval for returns above configurable value/quantity thresholds, with mandatory reason capture.

#### 3.4 Stock Adjustment

- Handle discrepancies from physical stock count or breakage.
- Adjustment reasons configured in master data.
- Role-based permission and approval workflow for adjustments above threshold.
- Full audit trail of before/after quantities and user actions.

#### 3.5 Central Store & Outlet Stock Responsibilities

- **Central Store**
  - Primary receiving point from suppliers (GRN creation).
  - Maintains batch- and expiry-wise stock at warehouse level.
  - Distributes medicines to Outlet Pharmacies based on requisitions and approved transfer notes.
  - Handles company returns (expired/damaged/excess stock) back to suppliers.
  - Can record emergency purchase entries (direct purchase without standard PO flow) with appropriate flags and approvals.
  - For broader, non-pharmacy main store processes and responsibilities, see **Hospital Main Store – Functional Modules & Processes** (`hospital-main-store.md`).

- **Outlet Pharmacies**
  - Maintain independent, location-specific stock and ledger.
  - Receive stock from Central Store or other Outlet Pharmacies.
  - Issue/dispense medicines to patients (OPD/IPD) or departments.
  - Initiate requisitions to Central Store or other outlets when stock is low.
  - Perform local stock adjustments (with central oversight where required).

#### 3.6 Requisition & Transfer Workflow

- **3.6.1 Product Requisition to Central Store**
  - Initiator: Outlet Pharmacy.
  - Steps:
    - Create requisition selecting pharmacy/location, products, and requested quantities.
    - Save as Draft or Submit for approval.
    - Central Store reviews requisition and can:
      - Approve in full.
      - Approve partially (with visible approved vs requested quantity).
      - Reject with mandatory reason.
    - Upon approval:
      - Stock is deducted from Central Store.
      - Stock is added to requesting Outlet Pharmacy.
      - A linked transfer/issue document is created for audit and reporting.

- **3.6.2 Inter‑Pharmacy Requisition (Outlet ↔ Outlet)**
  - Initiator: Receiving Outlet (Outlet A) raises requisition to Issuing Outlet (Outlet B).
  - Business rules:
    - Issuing Outlet must have sufficient surplus stock; negative stock is not allowed.
    - Approval by Issuing Outlet (or its designated Approver role) is mandatory.
    - All transfers are logged with:
      - Requesting outlet, issuing outlet.
      - Products, requested vs issued quantities.
      - Approver, date, and time.
  - On approval:
    - Stock deducted from Issuing Outlet.
    - Stock added to Receiving Outlet.

- **3.6.3 Central Pharmacy to Supplier Requisition (Purchase Order – PO)**
  - Purpose:
    - Automate procurement based on past sales, current stock, and forecasted demand.
    - Maintain optimal inventory levels and prevent stock-out situations.
  - Inputs for PO suggestion:
    - From Date / To Date (period for sales analysis).
    - No of Days (forecast period – how many days of stock to plan for).
    - Company (supplier/manufacturer filter).
  - Auto-generated product table (per selected company):
    - Company Name.
    - Product Name.
    - Average Sale (based on historical period).
    - Current Stock.
    - Request Quantity (system-calculated, read-only).
    - Order Quantity (initialized from Request Quantity but editable).
  - Calculation logic:
    - Determine day count = number of days in the selected From–To date range.
    - Average Daily Sale = Total Sale Quantity in period ÷ day count.
    - Forecast Required Quantity = Average Daily Sale × No of Days (forecast period).
    - If Forecast Required Quantity > Current Stock:
      - Request Quantity = Forecast Required Quantity − Current Stock (view-only).
      - Default Order Quantity = Request Quantity (can be manually overridden).
    - Products where Current Stock is already sufficient for the forecast period may be excluded from the suggested list.
  - User controls and audit:
    - User can change Order Quantity per line item before finalizing.
    - System logs all manual overrides (previous value, new value, user, timestamp) for audit.
  - Workflow:
    - Requisition status lifecycle: Draft → Submitted → Approved/Rejected.
    - On approval:
      - Generate Purchase Order (PO) document within Pharmacy/Store module.
      - Store PO for subsequent GRN matching.
      - Provide a configurable **Send to Supplier** action (e.g., email) with audit logging (sent time, recipient).

### 4. Dispensing & Sales

#### 4.1 OPD / Outdoor Sales

- **Prescription-based Sales**
  - Load prescription from EHR; display prescribed items, doses, and durations.
  - Allow full or partial issue; record unfilled items with reason (e.g., stock out, patient refused).
  - Suggest generic or equivalent medicines if substitution is allowed by policy (see **4.1.2 Substitution**).

- **Walk-in Sales**
  - Allow cash sales not linked to a prescription where legally permitted.
  - Basic patient details captured as per configuration (optional or mandatory).

- **Dispense orders without `prescription_id`**
  - Normal mode for OTC, walk-in, or jurisdictions where prescriptions are **not** electronically validated at the counter (**§1.3**).
  - Stock deduction, pricing, and receipt printing follow the same flow as prescription-linked sales; only clinical reconciliation to **`hospital-service`** is skipped when no Rx is linked.

- **Billing Integration**
  - Create corresponding Pharmacy invoices/receipts in Billing module.
  - Apply configured pharmacy discounts/schemes and approvals.
  - Support cash and credit sales modes with linkage to patient/customer accounts.
  - Ensure sales returns are always tied back to original invoice/receipt for quantity and amount validation.

##### 4.1.1 Line-level fulfillment states (partial vs unfilled)

Each **prescription line** (or OTC line) under a dispense order has a **fulfillment line state** independent of the order header. The system shall support at least the following **line states** (exact enums may match `hospital-pharmacy-service` and EHR):

| State | Meaning |
|-------|--------|
| **NOT_STARTED** | No quantity issued yet; optional planned batch reserved. |
| **PARTIAL** | `0 < quantity_dispensed < quantity_prescribed` (or remaining authorized quantity). |
| **FILLED** | Line fully satisfied for this episode (`quantity_dispensed` meets prescribed or clinically adjusted authorized quantity). |
| **REFUSED** | Patient declined after counsel; **mandatory reason code** (e.g. cost, side-effect concern). |
| **OUT_OF_STOCK** | Refused or not supplied because nothing could be issued **without** an approved override (**§4.1.5**). |
| **CANCELLED** | Line voided before or after partial issue per policy (supervisor approval if required). |
| **FILLED_WITH_STOCK_OVERRIDE** | Quantity issued **even though** the system showed **no or insufficient** recorded stock (physical stock elsewhere, delayed receipt entry, emergency). Requires **§4.1.5** fields. |

**Required fields (audit and safety)**

- **reason_code** — Controlled vocabulary for unfilled, partial stop, refusal, or stock-out (required when state is not NOT_STARTED/FILLED as applicable).
- **documenting_pharmacist_user_id** — User who confirmed the state (especially REFUSED, OUT_OF_STOCK, substitution approval).
- **stock_snapshot_ref** (optional but recommended for disputes) — Reference to batch-level availability or movement id at decision time (supports “we had no batch” audits).
- **partial_issue_notes** — Free text when **PARTIAL** is intentional (e.g. split fill, days supply limit).

Partial issue must persist **remaining quantity** on the line until **FILLED**, **FILLED_WITH_STOCK_OVERRIDE**, **REFUSED**, **OUT_OF_STOCK**, or **CANCELLED** closes the line.

##### 4.1.2 Substitution (generic / therapeutic alternative)

When **no prescription is linked**, there is **no DAW** constraint from a prescriber; product choice follows **counter policy**, **formulary**, and **pharmacist judgement** within local law. When a prescription exists and the prescriber allows substitution (see **DAW / substitution flags** on the prescription in `prescription-management.md`):

- **Eligibility** — Only offer substitutes if **prescription DAW and formulary rules** permit; blocked substitutions must be **hard-stopped** or **override** per next bullet.
- **Therapeutic equivalence** — Suggested alternatives must match **configured equivalence class** (e.g. same active ingredient + strength + route, or ATC class + strength band per hospital policy). Document the **rule set version** in audit.
- **Who may override** — **Pharmacist** with dispensing authority may select an allowed alternative; **off-formulary or restricted override** requires **`HOSPITAL_MANAGE`** or a dedicated **pharmacy supervisor** permission as configured in RBAC.
- **DAW alignment** — If DAW = dispense as written, **no** therapeutic substitution without **new prescription** or **prescriber contact workflow** (policy-specific).
- **Audit** — Store **substituted_drug_id**, **original_prescribed_drug_id**, **approver_user_id** (if different from dispenser), **timestamp**, and **reason** (e.g. stock, patient preference with consent).

##### 4.1.3 Billing, charges, returns, and idempotency (minimal lifecycle)

**Happy path**

1. **Dispense posted** — Stock deducted; dispense lines reach FILLED/PARTIAL per policy.
2. **Charge created** — `hospital-billing-service` creates **charge lines** from **billable dispense** payload (references `dispense_order_id` / `dispense_line_id`).
3. **Payment** — Cash/credit/corporate per billing rules.
4. **Return** — Patient return **references original charge or invoice line**; stock and financial **reversal** is idempotent (same return request must not double-credit).

**Reversal rules**

- **Idempotency key** — Client-supplied key (or natural key: `dispense_line_id` + `return_attempt`) on **POST dispense lines** and **POST returns** to prevent double issue under retries.
- **Reversal vs adjustment** — Financial credit follows **billing** service rules; pharmacy records **quantity_returned** and movement reversal only after billing accepts the reversal (or in parallel per contract—document integration order in `hospital-pharmacy-service-implementation-plan.md`).

See also **§8 Integration** for cross-module links.

##### 4.1.4 Customer receipt (POS thermal printer and PDF)

After payment or on-demand at the counter, the system shall support:

- **POS / thermal receipt** — Print to **ESC/POS** (or equivalent) drivers for common **80mm/58mm** counter printers: line items, quantities, unit price, taxes/discounts, total, payment method, pharmacy name/address, invoice or receipt number, date/time, and optional batch/expiry lines where policy requires.
- **PDF receipt** — Generate a **printable PDF** (same or summary content) for email, reprint, or A4 filing; template should match branding and statutory fields for the deployment region.
- **Configuration** — Default printer profile per workstation; **reprint** and **duplicate** controls with audit (user, time) to deter fraud.

##### 4.1.5 Dispensing when the system shows no / insufficient stock (override)

Operational reality: **recorded** stock may be wrong (goods on shelf not yet received into the system, batch not logged, emergency issue from floor stock) while the patient still must receive medication. The system shall **allow dispensing to complete** in these cases when **organization policy** permits—not only when on-screen quantity is positive.

- **Default UX** — Warn when available quantity is zero or below requested; show **“insufficient stock”** but offer **Continue with override** (or equivalent) for authorized users.
- **Authorization** — **Configurable**: e.g. any pharmacist on duty, or **supervisor PIN / second user** (`HOSPITAL_MANAGE` or a dedicated **stock override** permission when added to RBAC).
- **Mandatory capture on override**
  - **reason_code** (e.g. `PHYSICAL_STOCK_NOT_RECORDED`, `EMERGENCY_ISSUE`, `PENDING_GRN`, `DATA_ENTRY_LAG`).
  - **documenting user(s)** — Dispenser and, if required, **approver** id.
  - **Optional** free text (e.g. location of physical stock).
- **Ledger behavior (configurable per deployment)**
  - **Allow negative on-hand** at the location until receipt/adjustment corrects it, **or**
  - **Issue without selecting a batch** (placeholder) with a follow-up task to **allocate batch and true up** stock movements.
- **Audit & reporting** — All overrides appear in **dispense and stock-variance reports**; they must not be silent.
- **What stays strict** — **Transfers between locations** and **supplier returns** may still **forbid** negative balance where policy requires; only **patient/department dispensing** is in scope for this override unless explicitly configured otherwise.

#### 4.2 IPD / Indoor Issues

- **Department-wise & Patient-wise Issues**
  - Issue medicines and consumables against:
    - Specific inpatients (Admission No / IPD Account).
    - Ward/Department stock (ward stock model).
  - For patient-specific issues:
    - Automatically post charges to patient IPD bill.

- **Returns from Ward/Patient**
  - Accept returns (unused, intact packs) according to policy.
  - Re-add to stock (with appropriate flags) and adjust IPD bill.
  - Distinguish between:
    - Patient-level returns (linked to specific admission/bill).
    - Department/ward stock returns.
  - For patient and OPD sales returns:
    - Enforce linkage to original invoice/issue note.
    - Require reason for return (e.g., excess, change of therapy, patient not used).
    - Apply supervisor approval for high-value or exception scenarios.

- **Department-wise Issue & Cost Centers**
  - Allow direct issue of medicines to hospital departments (e.g., OT, ICU, Emergency).
  - Mandatory selection of department/cost center for such issues.
  - Support tagging of issues for:
    - Internal consumption.
    - Package/clinical bundle usage.
  - Feed department consumption data into:
    - Cost center reports.
    - Department-wise utilization and budgeting.

### 5. Controls, Safety, and Compliance

- **Stock & Batch Controls**
  - Prevent sale/issue of expired items.
  - **Near-expiry policy (parameterized)** — Behavior shall not be a single global “alert vs block”; it must be **rule-driven** so auditors can trace behavior. Example dimensions (configurable per deployment):
    - **Days to expiry** threshold (e.g. block if < N days; warn if < M days).
    - **Drug class** (e.g. OTC vs Rx, controlled schedule, high-cost biologics).
    - **Action** — `ALERT_ONLY`, `BLOCK`, or `ALLOW_WITH_APPROVAL` (with approver role).
    - **Discount requirement** — If near-expiry sale is allowed, optional mandatory discount or patient acknowledgement flag.
  - Rules are versioned in configuration or master data with **effective dates** where possible.

- **Controlled/Scheduled Drugs**

  - **Baseline (all deployments)** — Additional tracking: patient ID, prescriber, quantity, running balance per patient and per batch; restricted access; separate reporting.
  - **Jurisdiction-agnostic profiles** — Implementations map **hospital policy profiles** to local regulation (e.g. US DEA schedules, NHS Controlled Drugs (CD), national schedules). Each profile may enable subsets of:
    - **Extra identifiers** — Second patient ID check, witness ID for destruction/waste.
    - **Daily / rolling limits** — Per patient, per drug class, with hard stop vs supervisor override.
    - **PDMP / registry step** — Mandatory query or documentation of “checked / not applicable” before first fill (region-specific).
    - **Witness** — Second signature for select schedules or high quantities.
    - **Separate register / audit log** — Append-only register export for inspections.
    - **Lockable storage** — Flag on location; dispensing only from approved vaults (operational tracking).

- **Drug Interaction & Allergy Checks (prescribing vs dispensing)**

  - **Prescribing-time** — Primary reconciliation of new orders against allergies, major interactions, and duplicates is **prescriber workflow** in `hospital-service` / Prescription Management (see `prescription-management.md`).
  - **Dispensing-time** — Pharmacy performs a **safety net** re-evaluation when:
    - **New clinical data** arrived since prescribing (e.g. new allergy documented, new Rx from another provider in the same episode).
    - **High-risk medication classes** (configurable list: anticoagulants, insulin, opioids, etc.).
    - **Substitution or partial fill** changes exposure (e.g. quantity jump).
  - **Severity behavior**
    - **Alert-only** — Non-blocking warning; requires **acknowledgement** with user id + reason for override.
    - **Hard stop** — Blocking issue until prescriber contact, override by **pharmacy supervisor** permission, or policy-defined exception—document which applies per interaction severity tier.

- **User Permissions and RBAC mapping**

  Logical **business roles** below are **not** hard-coded in code; they map to **`rbac.permissions`** rows (see `easyops-erp/services/hospital-service/src/main/resources/db/changelog/changesets/002-hospital-permissions.sql` and `easyops-erp/rbac/README.md`). Typical mapping:

  | Business role (conceptual) | Typical permission codes / resource actions |
  |----------------------------|---------------------------------------------|
  | **Outlet / staff pharmacist (dispense)** | `HOSPITAL_PHARMACY_DISPENSE` (`hospital.pharmacy` / `dispense`); often `HOSPITAL_PRESCRIPTION_VIEW`; **not** prescribe/transmit unless also prescriber. |
  | **Pharmacy cashier / billing touchpoint** | Primarily **`hospital-billing-service`** permissions; may need `HOSPITAL_VIEW` for patient context. |
  | **Central store / main store admin** | Inventory / main-store **`manage`** permissions (see platform inventory seeds); **`HOSPITAL_MANAGE`** if the deployment ties store admin to hospital module. |
  | **Pharmacy store manager / catalog** | `HOSPITAL_MANAGE` for drug master and locations in `hospital-pharmacy-service`, or delegated permission set per `HospitalPharmacyRbacService` (pharmacy catalog: `hospital` / `view` or `manage`). |
  | **Approver** (transfers, high-value returns, discounts) | `HOSPITAL_MANAGE` or dedicated approval permissions in billing/discount modules. |
  | **Stock override at dispense** (issue when system shows no stock) | Often `HOSPITAL_MANAGE` or supervisor approval; may be bundled into **`HOSPITAL_PHARMACY_DISPENSE`** per org policy (**§4.1.5**). |
  | **Auditor** | Read-only: `HOSPITAL_VIEW` + `HOSPITAL_PRESCRIPTION_VIEW` without dispense/mutate, or explicit audit role per org. |
  | **Prescriber** | `HOSPITAL_PRESCRIPTION_PRESCRIBE` (and `HOSPITAL_PRESCRIPTION_TRANSMIT` if e-prescribing). |

  **`hospital-pharmacy-service`** enforcement summary: dispensing **read/mutate** requires **`HOSPITAL_PHARMACY_DISPENSE`** or coarse **`HOSPITAL_MANAGE`**; catalog writes require **`HOSPITAL_MANAGE`**. Clinical roles such as **`PHARMACIST_DISPENSER`** (seeded in `036-phase4-clinical-roles.sql`) bundle these for convenience.

  Fine-grained control over dispense, override, stock adjustment, and supplier return remains aligned with **resource/action** pairs in the RBAC catalog, not ad-hoc strings in application code.

- **Business Rules Summary**
  - Central Store is the primary receiving point for all supplier deliveries.
  - Each Outlet Pharmacy maintains its own stock ledger; inter‑pharmacy transfers are always documented and approved.
  - All stock movements (receive, issue, transfer, return, adjustment) require appropriate user permissions and, where configured, supervisory approval.
  - **Recorded quantity vs dispensing** — By default the system **warns or blocks** issues that would drive **on-hand below zero**; **patient/department dispensing** may **override** when policy allows (**§4.1.5**), with audit. **Inter-location transfers** and similar flows typically remain **non-negative** unless explicitly configured.
  - Expired medicines are strictly blocked from sale/issue; near‑expiry handling follows configured policy.
  - System must prevent deletion of posted stock transactions; only reversal/adjustment with audit trail is allowed.

### 6. Reporting

- **Stock Reports**
  - Product Stock:
    - Date-wise.
    - Group-wise.
    - Medicine-wise.
    - Supplier-wise.
  - Medicine list with real-time expiry view (near-expiry and expired).

- **Sales & Collection Reports**
  - Detail and summary views:
    - Date-wise.
    - Medicine-wise.
    - User/counter-wise.
  - Due/credit and settlement tracking where applicable.
  - Due collection and aging:
    - Track outstanding dues at customer/patient level.
    - Support partial payments and show remaining balance.
    - Provide aging buckets (e.g., 0–30, 31–60, 61–90, >90 days).

- **Discount & Return Reports**
  - Discount report (by date, user, type, medicine).
  - Due report.
  - Sales return reports with reasons and financial impact.

- **Profitability & Control Reports**
  - Gross profit report by item, group, supplier, period.
  - Stock adjustment report (reason-wise).
  - Transfer history between stores/pharmacies.
  - Department issue report (ward/department consumption).

- **Audit & Logs**
  - Every stock movement (receive, issue, transfer, return, adjustment) is logged with:
    - User, date, time.
    - Source and destination locations (where applicable).
    - Before and after stock levels (where applicable).
  - **Stock override log** — Issues completed while the system showed **insufficient stock** (**§4.1.5**) must be listable for audit and inventory reconciliation.
  - Maintain immutable transaction history; corrections are done via new transactions (not hard edits).
  - Capture key workflow events:
    - Requisition and PO approvals/rejections.
    - Email/notification sending for POs or alerts.

### 7. Non-Functional Requirements

- Real-time stock updates with **appropriate locking** to prevent overselling.
- **Concurrency** — Multiple counters (or concurrent API requests) may select the same **drug + batch** at the same location; stock mutations must be **transactional** with **optimistic or pessimistic locking** so concurrent issues are not double-applied. Under **strict stock** mode, issued quantity must not exceed recorded on-hand; under **override** mode (**§4.1.5**), the system records the issue and may post **negative on-hand** or a **pending true-up** per configuration (no silent inconsistency).
- **Idempotency** — **POST** operations that record dispensing or returns (`POST .../dispense-orders/{id}/lines`, returns endpoints) must accept an **idempotency key** (header or body) or equivalent **natural idempotency** so retries after network failure do not duplicate issues or credits.
- Performance (dispensing counters at peak):
  - **Drug search / autocomplete** — Target **p95 < 500 ms** for catalog search under normal load (aligns with `hospital-pharmacy-service` implementation plan).
  - **Batch pick list** for a drug at a location — p95 within the same order of magnitude under normal load.
  - **Dispense line post** — Strong consistency on stock; latency target set per deployment (planning guideline: **p95 < 500 ms** for the dispense API when not blocked by downstream billing).
- High availability and integrity:
  - No stock operation should be left in an inconsistent half-completed state.
- All financial amounts and quantities must be precise and auditable for internal and external audits.

### 8. Integration & Future Enhancements

- **Integration Notes**
  - Integrated with:
    - Billing & Invoice Module (for OPD invoices, IPD postings, due/collection updates).
    - IPD Module (for admission-wise charging and ward stock operations).
    - Accounts & GL Posting (for purchase, sale, adjustment, and return entries).
  - **Fulfillment and fill-status coherence** — See **§1.2** (in-house vs external) and **Prescription Management** (`prescription-management.md`, e.g. fill-status / FR-P3.11a) so billing and EHR stay aligned.
  - **Microservice contracts** — `hospital-pharmacy-service-implementation-plan.md` describes APIs, billable hooks, and events; **§1.1** lists identifier alignment with `hospital-service` and `hospital-billing-service`.
  - Ensure that pharmacy postings can be reconciled with Accounts and Billing summaries.

- **Future Enhancements (Pharmacy-Specific)**
  - **Receipt delivery** — SMS/WhatsApp links to PDF receipt (where integrated); fiscal **e-invoice** hooks per country.
  - Barcode/QR code scanning for:
    - Batch selection and verification.
    - Fast dispensing and stock counting.
  - Mobile/remote requisition approval for:
    - Central Store requisitions.
    - Inter‑pharmacy transfer requests.
  - Auto‑reorder level and suggestion engine:
    - Rule-based auto-generation of purchase suggestions based on:
      - Minimum stock levels.
      - Lead times and supplier preferences.
      - Historical consumption and seasonality.
