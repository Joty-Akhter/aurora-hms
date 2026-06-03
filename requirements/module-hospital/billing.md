## Billing Module – OPD & IPD

### 1. Overview

**Description**  
The Billing Module is the central console for creating and managing all patient financial transactions for both OPD and IPD. It integrates with existing Accounts, EHR, Pharmacy, and Inventory modules to ensure accurate, auditable billing.

**Objectives**

- Provide a unified, user-friendly billing interface for all hospital services.
- Ensure accurate and timely revenue capture for OPD and IPD.
- Enforce discount, approval, and credit policies as configured in the system.
- Seamlessly post summarized financial data to the Accounts module.

> Detailed pharmacy operational requirements are documented in [Pharmacy Module](pharmacy.md), with corresponding user stories in section 7.3 of [User Stories](user-stories.md).

### 2. General Functional Requirements

- Single landing screen with **OPD / IPD** mode selector (radio button or dropdown).
- Patient search by MRN, Patient ID, Mobile, Name, Visit No, Admission No.
- Ability to:
  - Create invoices, provisional bills, and advance receipts.
  - Collect payments (cash, card, digital wallet, bank transfer, insurance/corporate).
  - Apply configured discounts and “Less By” reasons with approval rules.
  - Print and reprint bills and receipts.
  - View bill history and outstanding dues for a patient.
- Role-based access to:
  - Bill creation, edit, cancellation, refund, and discount application.
  - View-only vs full-access permissions.
- All actions must be fully audited (who, when, what changed).
- **Billable items and pricing** must resolve from the **Clinical Chart / service catalog** (or equivalent charge master): user-entered free-text amounts may be allowed only where policy permits, and must be flagged and audited.
- Where corporate or insurer rules apply, the system shall support **estimates / pro-forma totals** (pre-issue) that reflect applicable discounts and payer splits before an invoice is finalized, without posting to Accounts until issue rules are met.

### 3. OPD Billing

#### 3.1 Scope

When **OPD** mode is selected, the system shall support billing for:

- Patient Registration & Appointment (Doctor Visit)
- Diagnostics (Lab, Radiology, Other tests)
- Pharmacy (OPD/Outdoor)
- Emergency / Casualty
- Gastro / Endoscopy Suite
- Dental
- Dialysis
- Physiotherapy
- Vaccination / Immunization
- Ambulance and other transport services

#### 3.2 Core Features

- **Visit & Appointment Billing**
  - Create bills for consultation/visit fees in line with doctor and department configuration.
  - Link invoices to appointment or walk-in registrations.
  - Support multiple services in a single invoice.

- **Diagnostics Billing**
  - Add tests and packages from the clinical service catalog.
  - Handle STAT/urgent surcharges where configured.
  - Integrate with laboratory and radiology modules for order and result linkage.

- **Pharmacy (OPD) Billing**
  - Add medicines from Pharmacy module with real-time stock validation.
  - Link sales to prescription (from EHR) or walk-in.
  - Reflect pharmacy discounts and schemes as per configuration.

- **Emergency & Specialized Services**
  - Quick billing workflow for emergency services with minimal mandatory data for rapid processing.
  - Billing for specialty services (Gastro, Dental, Dialysis, Physiotherapy, Vaccine) as itemized service lines or packages.

- **Ambulance Billing**
  - Charges based on configured rules (distance, time, type of vehicle, equipment).
  - Integration with Logistics/Transport data where available.

#### 3.3 OPD Ticket & Registration Flow

- **Ticket Creation**
  - On patient arrival, system shall create an **OPD Ticket** (or Registration Ticket) linked to the patient and visit.
  - Ticket captures: Patient ID, Visit Date, Registration Fee (as configured), Department/Doctor (if pre-selected), and Ticket Number.
  - Registration fee may be waived or varied based on patient type (e.g., corporate, staff, referral).

- **Registration Fee Collection**
  - Registration fee is posted to the OPD billing ledger at ticket creation or at a designated collection point.
  - System shall support collection of registration fee separately or as part of a consolidated OPD bill.

- **OPD Ticket Ledger & Due Collection**
  - All OPD charges (consultation, diagnostics, pharmacy, etc.) for the visit are posted to the **OPD Ticket Ledger** (or equivalent) linked to the ticket/visit.
  - System shall maintain running balance: **Total Charges − Collections − Adjustments = Outstanding Due**.
  - Due collection may occur:
    - At the time of each service (pay-as-you-go), or
    - At end of visit (consolidated payment), or
    - As partial payments with balance carried forward (subject to credit policy).
  - Outstanding dues for a patient across multiple OPD visits may be:
    - Displayed on the billing console when searching the patient.
    - Collected at next visit or via a dedicated due collection screen.
  - All collections must be recorded with payment mode, receipt number, and audit trail.

#### 3.4 OPD Billing Rules

- Multiple OPD visits/services for the same patient on the same day may be:
  - Billed separately, or
  - Consolidated into a single invoice, based on configuration.
- Discounts applied are constrained by:
  - Discount type configuration.
  - Approval matrix thresholds.
- For corporate/insurance OPD patients:
  - Tariffs and eligibility must follow contract configuration from Marketing/B2B.

### 4. IPD Billing

#### 4.1 Scope

When **IPD** mode is selected, the system shall support billing for:

- Admission and registration charges.
- Bed and room charges (ward/ICU/other categories).
- Diagnostics requisitions from wards/units.
- Medicine and consumable requisitions/issues.
- Medical procedures performed in wards/procedure rooms.
- Nursing/station/service charges.
- OT and surgery charges (surgeon, assistants, anaesthesia, OT fees).
- OT medicines and consumables.
- Logistics/transport related to IPD (e.g., intra-hospital transfers).

#### 4.2 Core Features

- **Admission & Deposit Handling**
  - Capture admission deposit/advance and generate receipt.
  - Link admission number with patient IPD account.
  - Maintain running balance of deposit vs charges.

- **Bed Billing**
  - Automated bed charge posting based on:
    - Bed type and tariff.
    - Length of stay (per day/per hour configuration).
    - Admission, transfer, and discharge timestamps.
  - Handle bed upgrades/downgrades with correct tariff application.
  - Auto-link bed charges to the active **IPD billing account** as soon as an admission is finalized and a primary bed is assigned.
  - Support separate billing lines (or clear indicators) for:
    - Primary bed.
    - Extra bed (optional, where hospital configuration permits).
  - Auto-calculate charge duration for each bed segment (from admission/transfer to transfer/discharge) according to:
    - Configured minimum billable period.
    - Per-day vs per-hour charging rules.
  - Enforce that:
    - A bed must be in **Available** (or Reserved, as per policy) status to start billing.
    - Bed charges stop accruing immediately after effective discharge date/time or bed release.
  - Lock bed allocation for the billing period once admission is finalized, with any changes handled through:
    - Bed change / transfer transactions.
    - Discharge / cancellation workflows with full audit trail.

- **Diagnostics Requisitions**
  - Receive diagnostic orders from wards/EHR.
  - Post charges to patient IPD account when tests are performed/accepted.

- **Medicine & Consumable Issues**
  - Receive issues from Pharmacy and central stores.
  - Post corresponding charges automatically to IPD bill.
  - Support patient-level and ward-level consumption where applicable.

- **Medical Procedures & Nursing/Station Charges**
  - Bill for procedures (e.g., bedside procedures, dressings, injections).
  - Add nursing charges, extra nursing care, or specialized care charges per configuration.

- **OT & Surgery Billing**
  - Support:
    - Package-based billing (surgery package including bed, OT time, surgeon fees, anaesthesia, etc.), or
    - Component-wise billing (individual line items).
  - Record and bill:
    - Surgeon, assistant surgeons, anaesthetist.
    - OT time and usage charges.
    - OT medicines and consumables (from Pharmacy/Store).

- **Interim and Final Billing**
  - Generate interim bills or statements for patient/attendant or corporate approval.
  - Final bill generation on discharge with:
    - Deposit adjustment.
    - Insurance/corporate share vs patient share.
    - Outstanding or refund calculation.

- **Indoor Patient Due Calculation**
  - The **total amount due** for an IPD patient shall be the sum of all charges minus all collections and adjustments.
  - System shall aggregate charges from:
    - **Clinical/Service Ledger** – bed, procedures, OT, doctor visits, diagnostics, nursing charges, and other clinical services.
    - **Pharmacy Ledger** – medicine issues and consumables (minus returns).
    - **Ambulance Ledger** – ambulance trip charges (when configured to include ambulance in patient due; see [Ambulance](ambulance.md)).
  - Whether pharmacy charges are included in the patient due display and collection flow shall be **configurable** (e.g., "Include Pharmacy in Patient Due" flag) to support hospitals that bill pharmacy separately or together.
  - Formula: **Total Due = (Clinical Charges + Pharmacy Charges) − Collections − Discounts − Adjustments**.
  - The due amount must be:
    - Displayed on the billing console for the active admission.
    - Available for interim bill generation and final settlement.
    - Correctly reduced by advance/deposit application and payments.

#### 4.3 IPD Billing Rules

- Each admission maintains a single IPD billing account; multiple interim bills are all part of the same account.
- Bed charges must not continue after discharge date/time.
- All reversals (cancelled services, returns, corrections) must keep an audit trail and, where needed, require approval.
- Closed/finalized bills:
  - Are not editable.
  - Any corrections must be done via adjustment entries (credit/debit notes) with full audit and approval.

#### 4.4 Final Release Bill – IPD Discharge Bill Paper

##### 4.4.1 Purpose & Scope

- The system shall generate a **Final Release Bill Paper** for IPD patients at the time of discharge, which serves as the **negotiation and settlement bill** between hospital and patient/guardian (and, where applicable, corporate/insurer).
- This document is a **snapshot of the finalized IPD financials** for a specific admission and must:
  - Consolidate all approved charges, discounts, advances, and adjustments.
  - Clearly show net payable / refund amounts in both figures and words.
  - Be printable and exportable to PDF for patient handover and internal audit.

##### 4.4.2 Data Sources & Dependencies

- **Patient Admission Module**
  - Patient ID / MRN, Registration No.
  - Admission Date & Time.
  - Patient Type (e.g., General, Corporate).
  - Under Doctor / Consultant, Ref. Doctor.
  - Bed / Cabin No at admission and at discharge.

- **Bed Management Module**
  - Bed / Cabin type (e.g., General Ward, Semi‑Private, Private, ICU).
  - Bed/base charges and length of stay calculation (Total Stay Duration / Total Days).
  - Final bed status (must be set to `Vacant` or equivalent after discharge).

- **Nurse Module (IPD Nursing & Bedside Operations)**
  - Lab requisition charges pushed to Lab billing.
  - Medicine requisition issues and **Medicine Returns** (adjustments).
  - Nursing / service charges raised from nurse activities, where modeled.
  - Indirect support data from daily care logs (for audit, not printed in detail).

- **Lab & Radiology Modules**
  - Consolidated **Lab Test bill** lines for the admission.
  - Consolidated **Radiology / Imaging bill** lines for the admission.
  - Optional **investigation‑wise aggregation** for internal reference vs. printed summary.

- **Pharmacy / Medicine Module**
  - IPD **Medicine Bill** for issues during admission.
  - **Returned medicine** entries and corresponding negative/adjustment postings.
  - Net medicine charge (issues minus valid returns).

- **Doctor / Consultant Visit Module**
  - All **Consultant / Visit fees** for the admission.
  - Visit entry confirmation / billing status for each visit.

- **Accounts / Billing (this module – final authority)**
  - Performs final calculation of:
    - Subtotals by charge category (bed, consultant, lab, radiology, pharmacy, services, admission).
    - Discounts / “Less By” with reasons and approvals.
    - Advance collections and previous dues.
    - Refund / return amount (if hospital owes patient).
    - Grand Total and Net Payable.
  - Generates amount in words and final settlement status.

> All contributing modules must push **financially consistent, closed data** to the IPD billing account before Final Release Bill generation is allowed (see 4.4.4).

##### 4.4.3 Layout & Sections – Final Release Bill Paper

The Final Release Bill Paper must, at minimum, present the following sections in a clear, patient‑readable format (labels may be localized/configurable, but semantics must remain):

- **A. Patient Identity**
  - Patient ID / MRN.
  - Registration No.
  - Patient Name.
  - Age.
  - Gender.
  - Address (at least city / locality with address line).
  - Mobile / Cell No.

- **B. Admission & Stay Information**
  - Bed / Cabin No (at discharge).
  - Bed / Cabin Type.
  - Admission Date.
  - Release (Discharge) Date & Time.
  - Total Stay Duration / Total Days (derived from timestamps and tariff rules).
  - Patient Type (Indoor General / Corporate / Package where relevant).
  - Under Doctor / Consultant Name.

- **C. Charge Breakdown (Category‑wise)**
  - Tabular area summarizing charges per source/category. Indicative columns:
    - **Particulars / Charge Head** (e.g., Admission Charge, Bed Charge, Consultant Fee, Lab Test, Radiology, Medicine Bill, Nursing / Service Charges, OT/Procedure if applicable).
    - **Amount** (numeric).
  - System must aggregate underlying line items from contributing modules into these categories as per Clinical Chart / service master mapping.
  - A separate **Adjustments** row (or group) for:
    - Medicine Returns.
    - Reversed / voided services that impact net totals.
    - Any “Less By” or special adjustments not modeled as standard discounts.

- **D. Financial Summary**
  - **Grand Total** (sum of all charge categories before advances).
  - **Advance Collection** (all deposits/advances collected against this admission).
  - **Previous Dues / Old Balance** (if hospital policy includes outstanding from earlier episodes).
  - **Net Payable** or **Refund / Return Amount**:
    - If Grand Total + Previous Dues − Advances > 0 → Net Payable (Due from patient).
    - If Grand Total + Previous Dues − Advances < 0 → Return Amount (Refund to patient).
  - **Amount in Words** for the Net Payable or Refund amount.

- **E. Audit & Control Information**
  - Posted By (user who finalized the bill).
  - Served By / Counter / Location (optional).
  - Printed Date & Time.
  - Release / Settlement Status (e.g., `Provisional`, `Final – Pending Payment`, `Final – Settled`).
  - Page Number (Page X of Y) where multi‑page output exists.

##### 4.4.4 System Logic & Preconditions

- **Blocking Preconditions for Final Release Bill Generation**
  - System must **not allow Final Release Bill** generation (or marking as “Final”) unless all of the following are true for the corresponding admission:
    - All **Nurse Module activities** that can generate charges (lab/medicine requisitions, returns, service entries) are in a **closed** or **finalized** state; no open/pending requisitions remain that can change financials.
    - All **Lab** and **Radiology** bills for this admission are **finalized** (no pending orders or unbilled completed tests).
    - All **Pharmacy / Medicine** issues and returns linked to the admission are **posted** and reflected in the IPD bill (no pending draft issues/returns).
    - Current **Bed allocation segment** is closed and bed is **released** or marked for discharge in Bed Management (no further stay accrual).
    - All relevant **Procedure / OT entries** and **Doctor Visit entries** for the admission are in a **locked / billed** state (as per respective module rules) or explicitly marked as excluded.
    - **Clinical Chart** or central charge master mapping is consistent (no orphaned chargeable items without revenue mapping).

- **Computation & Locking**
  - When Final Release Bill is generated:
    - System calculates all subtotals, discounts, advances, and net payable in a single, atomic operation.
    - The resulting bill snapshot is **immutable** for financial values once marked as “Final – Settled”.
    - Any post‑settlement correction must occur via:
      - Credit/Debit note or adjustment entry, and
      - With appropriate approval as per Discount/Approval matrix.
  - For negotiation scenarios:
    - Users with appropriate roles may:
      - Apply additional approved discounts or adjustments **before** marking the bill as “Final – Settled”.
      - Regenerate/update the **draft Final Release** printout to reflect updated negotiation values.

- **Provisional vs Final Prints**
  - System must clearly watermark or label:
    - `Provisional / Draft` bills used for negotiation, vs.
    - `Final` bills after payment/settlement.
  - Only **Final** bills should be considered legally binding for Accounts posting (subject to local regulation).

##### 4.4.5 Printing, Format & Access Control

- The Final Release Bill Paper must be:
  - Printable in A4 (and optionally receipt roll) format with hospital logo, header, and footer.
  - Configurable to include/exclude certain fields (e.g., address lines, guardian name) as per hospital policy.
  - Exportable as PDF from the Billing console.
- Role-based controls:
  - Only authorized billing roles can:
    - Generate or regenerate Final Release Bills.
    - Mark bills as "Final – Settled".
  - View‑only roles (e.g., doctors, nurses, auditors) may:
    - View or print **read‑only** copies, clearly marked as duplicates/reprints, with audit logging.
- Every print/reprint of the Final Release Bill must:
  - Be logged with user, date/time, and reason (optional but configurable as mandatory).
  - Support **"Duplicate Copy"** or similar notation for non‑original prints where policy requires.

### 5. Collection Ledger & Transaction Recording

#### 5.1 Collection Ledger – Per-Transaction Recording

The system shall maintain a **Collection Ledger** (or equivalent transactional record) for every payment, advance, collection, return, and adjustment. Each ledger entry must support both OPD and IPD flows and enable correct aggregation of patient dues and reconciliation.

- **Per-Transaction Recording**
  - Every collection, advance, refund, or adjustment shall create a **ledger entry** with:
    - **Received Amount** – Cash, card, cheque, digital wallet, or other payment received.
    - **Return Amount** – Cash or payment refunded to the patient.
    - **Due Amount** – Amount credited against outstanding dues (reducing patient balance).
    - **Less Amount** – Discount, write-off, or adjustment applied (with reason and approval where required).
  - Each entry shall carry:
    - Patient ID, Visit/Admission ID (where applicable).
    - **InputFrom** – Source of the transaction (e.g., Collection, Patient Release, Indoor Due Collection, OPD Due Collection, Sales).
    - **PtStatus** – OPD or IPD (Indoor).
    - Branch ID, Cost Center, User, Date/Time.
    - Receipt/Reference number, Payment mode breakdown (Cash, Card, Cheque, etc.).
    - Valid flag for soft delete and audit.

- **Running Balance**
  - System shall maintain a running balance per patient (and per admission for IPD) such that:
    - **Total Due = (Charges − Collections − Returns − Less) + Previous Dues**.
  - Advances and deposits shall reduce the net payable; refunds shall be tracked separately for correct GL posting.

- **Aggregation Rules**
  - Total patient due shall aggregate charges from Clinical Ledger, Pharmacy Ledger (when configured to include), and Ambulance Ledger (when configured).
  - Collections and returns shall be correctly attributed to the appropriate ledger (OPD ticket, IPD admission, or standalone) for reporting and reconciliation.

#### 5.2 Cash Closure & Day-End

- **Cash Closure**
  - System shall support **cash closure** (or cash handover) at designated intervals (e.g., end of shift, end of day).
  - Cash closure shall:
    - Record the closing cash balance for the user/counter.
    - Link to the collection ledger entries since the last closure.
    - Generate a closure summary (total received, total refunded, net cash, payment mode breakdown).
    - Require acknowledgment (e.g., supervisor sign-off) where configured.
  - Once closed, further edits to closed-period collections shall be restricted or require approval.

- **Day-End**
  - System shall support **day-end** processing to:
    - Finalize the day’s transactions for the billing period.
    - Lock or flag the period for posting to Accounts.
    - Generate day-end reports (collection summary by counter, payment mode, department).
  - Day-end may be configurable per branch or cost center.
  - Post–day-end corrections shall be handled via adjustment entries with full audit trail.

#### 5.3 Refunds, payment reversals, and voids

- **Full and partial refunds** must be supported against prior collections, with:
  - Reference to the original receipt/payment (and invoice, if applicable).
  - Refund method (cash, card reversal, transfer, adjustment to deposit/credit note) per policy.
  - Reason code and, where configured, secondary approval.
- **Payment reversals** for card/UPI/bank channels must record gateway or bank reference identifiers when available, for reconciliation with settlement reports.
- **Void vs refund**: distinguish voiding an erroneous same-day entry (where policy allows) from refunding settled amounts; both paths must be auditable.
- Refunds and reversals must update patient/admission balance and feed **Accounts** with the correct GL impact (cash, receivable, revenue adjustment) per integration contracts.

### 6. Discounts, Approvals, and Credit

#### 6.1 Discounts & "Less By"

- Discounts are defined in configuration with:
  - Types (staff, management, corporate, promotional, etc.).
  - Max percentage/amount.
  - Applicable modules (OPD, IPD, Pharmacy, Canteen).
- On applying a discount or "Less By":
  - User must select a reason from master data.
  - System must log user, amount, reason, and approval status.

#### 6.2 Approval Matrix Enforcement

- Approval is required when:
  - Discount exceeds role threshold.
  - Writing off dues or cancelling finalized services.
  - Granting credit beyond configured limit.
- Approval flow:
  - Request raised from Billing screen.
  - Approver(s) notified and able to approve/reject with comments.
  - Audit trail retained for all decisions.

#### 6.3 Credit, Insurance, and Corporate Billing

- Corporate/B2B and insurance patients:
  - Must be mapped to a contract with defined tariffs and credit limits.
  - Billing must separate:
    - Payer share (corporate/insurance).
    - Patient share (co-pay, non-covered items).
- Aging and outstanding for corporate accounts tracked in Accounts/Reports.

##### 6.3.1 Multi-payer allocation and settlement order

- Where a single episode has **multiple payers** (e.g. insurer + patient co-pay, corporate + employee share), the system shall:
  - Support **allocation rules** per contract (percentage, cap, excluded categories, co-pay floors).
  - Show **payer-wise balance** (who owes what) in addition to total patient account balance where configuration requires it.
  - Define **settlement priority** when collecting (e.g. apply payment to co-pay first vs corporate invoice) as configurable or contract-driven.
- Requirements for **insurance claim submission, denial management, EOB ingestion, and payer portal workflows** are out of scope for this billing console unless separately specified; this module must still **retain billed amounts, adjustments, and payer attribution** for handoff to a claims or RCM system.

#### 6.4 Discount Cards & Membership

- **Discount Card Registration**
  - System shall support registration of **Discount Cards** (or membership cards) that entitle the holder to predefined discounts on billing.
  - Each card type shall have:
    - Card code/ID, name, and validity period.
    - Applicable discount (percentage or fixed amount) and scope (OPD, IPD, Pharmacy, specific services).
    - Active/Inactive status.
  - Cards may be issued to:
    - Individual patients (linked to Patient ID).
    - Corporate/group (linked to contract or organization).
    - Staff/family (linked to employee or beneficiary).

- **Application at Billing**
  - At billing time, user may associate a valid discount card with the patient or visit.
  - System shall automatically apply the configured discount to eligible charge lines, subject to:
    - Card validity and scope.
    - Any approval rules if discount exceeds threshold.
  - Discount card application must be logged (card used, discount amount, user).
  - Multiple discount sources (card + management approval) may be combined where policy allows; system shall enforce configurable rules to prevent over-discounting.

- **Card Master Maintenance**
  - Admin shall maintain discount card types and their rules in a central master.
  - Card issuance and linking to patients shall be auditable.

### 7. Integration with Other Modules

- **Accounts**
  - Automatic creation of financial vouchers (summarized or detailed as configured).
  - Mapping of billing items to chart of accounts and cost centers.
- **EHR**
  - Linking of visits, diagnoses, procedures, and orders to corresponding bill items.
- **Pharmacy & Inventory**
  - Real-time synchronization of issues, returns, and stock adjustments with billing.
- **HR**
  - Mapping of doctor and staff charges to departments/cost centers for productivity analysis.
- **Blood Bank**
  - Issues (and any returns/adjustments) that carry a charge must post to patient billing with correct service codes and audit linkage, consistent with [Integration with Accounting, Inventory, and HR Services](integration-services.md).
- **Canteen / auxiliary services** (where deployed)
  - Consumption charges attributable to a patient or visit must be feedable to billing with source reference for reconciliation.
- **Scheduling**
  - Visit/appointment identifiers must be referenceable on OPD charges and invoices where the hospital links billing to scheduled encounters.
- **Corporate, discount, and card programs**
  - Eligibility, tariffs, and discount evaluation are governed by **Discount / Corporate** and **Card** policy services; billing consumes evaluated rules at estimate and invoice time and persists applied discounts with references for audit (see [Hospital card programs – overview](hospital-card-types-overview.md) where relevant).

#### 7.1 Charge capture, events, and service boundaries

- Upstream clinical and operational modules (EHR orders, pharmacy dispense, bed events, lab/radiology completion, OT, etc.) shall feed charges via **documented APIs and/or domain events**. Feeds must be **idempotent** where the same clinical event could be retried: duplicate submissions must not double-bill (e.g. idempotency keys or natural keys on source id + charge type).
- The billing domain **owns** invoice, payment, refund, and adjustment lifecycle; it does **not** own discount rule authoring, full general ledger, or inventory stock logic—those remain in platform or sibling services per [Technical Requirements](technical-requirements.md) and the **`hospital-billing-service`** implementation plan.
- For implementation-level APIs, data contracts, and phase checklist, see [hospital-billing-service-implementation-plan.md](hospital-billing-service-implementation-plan.md).

### 8. Non-Functional & UX Requirements

- Billing screens must load and respond within acceptable performance thresholds under normal load (target: interactive steps such as patient load and invoice save complete within a few seconds under nominal conditions; heavy reports may run asynchronously).
- UI must follow the standard hospital design system for consistent look and feel.
- **Currency, rounding, and tax**
  - All monetary values must respect hospital **currency**, **decimal precision**, and **rounding rules** (per line, per invoice, or statutory method as configured).
  - **Tax / statutory levies** (e.g. GST, VAT, service tax—jurisdiction-specific): support configurable tax categories, exempt vs taxable lines, tax-inclusive vs tax-exclusive display, and totals that reconcile for Accounts handoff; tax registration numbers on invoices where legally required.
- **Privacy and documents**
  - Receipts and bills displayed or printed in public areas must avoid unnecessary exposure of sensitive clinical detail; configuration may suppress diagnosis text while retaining financial line items.
  - Duplicate/reprint copies must be clearly marked when policy requires.
- System must support concurrent billing operations safely with appropriate locking or conflict resolution (e.g. two users on the same patient account: consistent balance reads; controlled writes).
- **Availability**: billing should degrade gracefully when a non-critical integration (e.g. discount evaluation) is unavailable—core capture and audit remain possible per fallback policy (e.g. manual discount with approval, or block issue—**configurable**).

### 9. Traceability and amendment documents

- Every issued invoice and material financial correction must be traceable through:
  - Human-readable **invoice/receipt numbers** with facility- or branch-scoped uniqueness rules where applicable.
  - **Credit notes / debit notes** or formal adjustment documents that reference the original invoice or charge line, reason, and approver when required.
- Amendment of **finalized** bills remains via adjustment entries only (aligned with §4.3); the system must not silently overwrite historical totals.
