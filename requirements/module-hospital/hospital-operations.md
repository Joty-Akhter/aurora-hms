## Hospital Operations Module

### 1. Overview

**Description**  
The Hospital Operations Module defines non-clinical but operational features that support end-to-end hospital management: user management, system settings, dashboards, marketing/B2B, canteen, reporting, and EHR integration into hospital workflows.

**Objectives**

- Provide a consistent foundation for access control and configuration across all hospital modules.
- Enable operational visibility through dashboards and reports.
- Support revenue growth and control via marketing/B2B and canteen integrations.
- Ensure that clinical (EHR) and operational data are properly linked.

---

### 2. User Management

#### 2.1 Description

Centralized user management to control who can access which parts of the hospital system and what actions they can perform.

These requirements are implemented using the existing enterprise `auth-service`, `rback-service`, and `user-management-service`. The hospital module will configure roles, permissions, and policies in those shared services rather than implementing new ones.

#### 2.2 Core Features

- **User Master**
  - Create, edit, deactivate users.
  - Fields:
    - Employee ID (linked to HR where available).
    - Full Name.
    - Mobile, Email.
    - Department, Designation.
    - Default Role.
    - Login ID and Password (securely stored).
    - Status (Active/Inactive).
    - Default Location/Unit.
  - Soft delete or inactivation only; historical references are never lost.

- **Role Management**
  - Maintain a library of roles (e.g., Hospital Admin, Doctor, Nurse, Pharmacist, Store Keeper, Billing Officer, Front Desk, HR, Accountant, Marketing, Canteen Operator, IT Support).
  - Copy-from-existing-role function to speed configuration.

- **Permission Management**
  - Role-wise menu and action-level permissions:
    - View, Create, Edit, Delete, Export, Approve, etc.
  - Department-wise restrictions:
    - Example: Pharmacy user limited to selected pharmacies.
    - Example: Store keeper limited to specific stores/warehouses.
  - Ability to preview effective permissions for a user or role.

- **Security & Compliance**
  - Configurable password policy (length, complexity, expiry).
  - Account lockout on repeated failed login attempts.
  - Session timeout and automatic logout on inactivity.
  - Login history and audit trail of key operations.

---

### 3. System Settings & Master Configuration

#### 3.1 General Master Settings

- **Doctor Registration Configuration**
  - Default patterns for doctor codes.
  - Visit fee defaults (overridable at doctor level).
  - Department linkage rules.

- **Doctor Department Configuration**
  - See `doctor-module.md` for detailed requirements.

- **Staff Discounts Configuration**
  - Define discount types and eligibility (staff, dependents, special categories).
  - Maximum allowed limits and required approval levels.

- **Patient Source Information**
  - Master list of patient sources (self, referral, corporate, insurance, agent, online campaign, etc.).
  - Mandatory/optional rule per visit type and mode.

- **“Less By” Reasons**
  - Standard reasons for manual reductions, waivers, adjustments.
  - Enabled modules: Billing, Pharmacy, Canteen, etc.

- **Patient Management Masters**
  - **Patient Category Master**
    - Configure high-level patient categories:
      - General, VIP, Corporate, Staff, Staff Dependent, Charity/Free, Others (configurable).
    - For each category define:
      - Eligibility rules (e.g., requires corporate link, employee ID, approval).
      - Default discount or tariff group, if any (linked to Billing & Pricing rules).
      - Visibility in registration and reporting.
  - **Patient Class Master**
    - Define care class labels:
      - OPD, IPD, Emergency, Day Care, Teleconsultation (optional).
    - Use class to:
      - Drive allowed workflows (e.g., bed assignment only for IPD/Day Care).
      - Control which billing rules and packages apply.
  - **Nationality Master**
    - Maintain list of nationalities with:
      - Standard names and codes.
      - Default residency/visa category (optional).
    - Use for:
      - Reporting (local vs foreign patients).
      - Special tariffs or documentation rules where applicable.
  - **Blood Group Master**
    - Standard ABO and Rh blood groups:
      - A+, A−, B+, B−, AB+, AB−, O+, O−.
    - Configurable to add rare groups (e.g., Bombay blood group) if needed.
  - **Religion Master (Optional)**
    - Maintain list of religions/faiths:
      - Controlled vocabulary, configurable per country context.
    - Mark field as:
      - Mandatory or optional at registration (configurable).
  - **Patient Insurance Type Master**
    - Configure insurance types:
      - Self-pay (cash), Corporate/Employer, Insurance/TPA, Government Scheme, NGO/Charity.
    - Map each insurance type to:
      - Required identifiers (policy no, corporate ID, scheme ID).
      - Pre-authorization requirement flag.
      - Credit vs cash handling rules (linked to Credit Policy and Insurance Claim Rules).
  - **Consent Form Template Master**
    - Library of consent templates:
      - Admission consent, Surgery consent, Anesthesia consent, Blood transfusion consent, Data sharing consent, etc.
    - For each template configure:
      - Language versions.
      - Whether digital signature is allowed.
      - Whether printing is mandatory before certain workflows (e.g., OT start).
  - **Patient Alert Tag Master**
    - Master list of alert tags that can be attached to patient record:
      - Clinical: High-risk fall, Isolation, DNR, Pregnancy, Chronic disease tags.
      - Administrative: Legal case/MLC, VIP, High outstanding due, Blacklisted.
    - Configuration:
      - Color code and icon.
      - Whether alert is clinical vs administrative (used for filtering).
      - Which roles can assign/remove each tag; approval rules for sensitive tags.

#### 3.2 Clinical & Medical Services Configuration

- **Clinical Service Catalog**
  - Master list of billable services:
    - Consultations.
    - Procedures.
    - Diagnostics.
    - OT services.
    - Nursing procedures.
  - Fields: Code, Name, Category, Department, Default Price, Tax %, Cost Center, Active/Inactive.

- **Advice & Service Packages**
  - Advice package configuration: group of services with a single package price.
  - Medicine package configuration: medicine bundles/protocols.
  - Health checkup packages: tests, consultations, and procedures grouped into one offering.

- **Diagnostic Equipment & Consumables**
  - Vacutainer and diagnostic equipment configuration:
    - Map which tubes/equipment are required per test.

- **Clinical Master Setup (Outpatient & Inpatient)**
  - **Medical Specialty Master**
    - Maintain list of clinical specialties (e.g., Internal Medicine, Cardiology, Pediatrics, Orthopedics, Neurology, Obstetrics & Gynecology, Anesthesiology, Surgery, ICU, NICU).
    - Link each specialty to:
      - One or more hospital departments and service categories.
      - Default consultation room/OPD location (where applicable).
    - Allow configuration of:
      - Active/Inactive status.
      - Display order in UI for appointment booking and reporting.
  - **Doctor Service Charge Setup**
    - Configure doctor-wise charging rules for:
      - First (new) consultation.
      - Follow-up consultation.
      - Emergency consultation.
      - Procedure/OT-assistant/surgeon roles (high level – OT details in OT module).
    - Support multiple rate layers:
      - Default hospital-wide rate by doctor grade and consultation type.
      - Doctor-specific override rate.
      - Corporate/Insurance-specific override using B2B tariff charts.
    - Effective date range and versioning of rates; maintain full history for audit.
  - **Consultation Type Master**
    - Master list of visit types: New, Follow-up, Emergency, Teleconsultation (optional), Review-only, Second Opinion (configurable).
    - For each consultation type configure:
      - Default duration/slot length.
      - Pricing rule (full price, discounted price, free/no charge).
      - Eligible time-window for “follow-up” pricing (e.g., within 7/14/30 days of last visit).
      - Whether allowed for OPD, IPD, Emergency, or all.
  - **Clinical Template Setup (SOAP Notes & Others)**
    - Template library for:
      - SOAP notes.
      - Department-wise clinical notes (e.g., antenatal visit, diabetic clinic, cardiac follow-up).
      - Admission notes, progress notes, discharge summaries (where not already covered in EHR templates).
    - Template components:
      - Structured sections (Subjective, Objective, Assessment, Plan).
      - Drop-downs, checklists, and free-text fields.
      - Default phrases/snippets for common findings.
    - Version control:
      - Maintain template versions with effective date.
      - Restrict editing of approved templates to authorized users (e.g., Clinical Admin).
  - **Diagnosis & Procedure Coding Masters**
    - **Diagnosis Code (ICD-10/ICD-11) Master**
      - Central master of diagnosis codes with:
        - Code, official description, local description, grouping/category.
        - Status (Active/Inactive) and effective dates.
      - Ability to:
        - Map to local diagnosis groups for reporting and package rules.
        - Mark diagnosis as chronic vs acute (where defined).
    - **Procedure Code Master**
      - Central procedure/operation/procedure-group master (e.g., CPT-like or local).
      - Fields:
        - Code, description, department, specialty, anesthesia type (where relevant), default duration, default OT/Procedure room.
      - Link to:
        - Billing/service items in Clinical Service Catalog.
        - Packages and revenue centers.
  - **Treatment Protocol & Order Set Setup**
    - Create standard treatment protocols/order sets for:
      - Common conditions (e.g., dengue, pneumonia, diabetes, hypertension).
      - Clinical pathways (e.g., pre-op, post-op, antenatal, NICU care).
    - Protocol components may include:
      - Medicines with default dose, route, frequency, duration.
      - Investigations (lab, imaging).
      - Nursing tasks and vital sign schedules.
      - Diet and activity orders.
    - Configuration:
      - Department/specialty ownership.
      - Applicability (OPD/IPD/Emergency).
      - Approval workflow for creating/updating protocols.
  - **Prescription Template Master**
    - Doctor-wise and department-wise prescription templates:
      - Favorite medicine lists for common conditions.
      - Disease-specific quick prescriptions (e.g., URTI, gastritis).
    - Allow:
      - Template creation from existing prescriptions.
      - Sharing of templates among doctors in same department (configurable).
  - **Nurse Task & Care Plan Setup**
    - Master list of nursing tasks:
      - Medication administration, wound dressing, IV infusion checks, turning schedule, catheter care, etc.
    - Map tasks to:
      - Ward/bed category (e.g., ICU vs general ward).
      - Clinical protocols (e.g., post-op care bundles).
    - Configure:
      - Default frequency and timing rules (e.g., every 4 hours, once per shift).
      - Whether billable or non-billable, and linked charge code if billable.
  - **Vital Sign Template Master**
    - Define vitals templates per:
      - Location (OPD, IPD ward, ICU, NICU).
      - Specialty/department (e.g., anesthesia pre-op, pediatric clinic).
    - Configure:
      - Which vitals appear (e.g., BP, HR, RR, Temp, SpO2, Pain score, GCS, Weight, Height).
      - Default measurement intervals and responsible role (nurse, technician).
      - Alert thresholds that integrate with alert/notification rules (see Smart Automation).
  - **Allergy Master**
    - Central master of common allergens:
      - Drug, food, environmental, contrast media, latex, others.
    - For each allergen, maintain:
      - Standardized name, category, severity scale guidance.
      - Cross-links to drug database (for interaction checking).
    - Allow configuration of:
      - Whether allergen is shown with high-visibility flag in patient header (e.g., RED banner).
  - **Chronic Disease Registry / Problem List Master**
    - Master list of chronic diseases to standardize problem list entries and registries:
      - Diabetes, Hypertension, CKD, COPD, IHD, Cancer, etc.
    - Link each chronic disease to:
      - ICD code(s).
      - Follow-up protocol templates and recall rules (future enhancement).
    - Use this master to drive:
      - Patient alerts/tags (e.g., chronic disease indicator).
      - Population health and chronic disease management reporting.

#### 3.3 IPD & Bed Configuration

**Purpose**  
Provide a centralized **Bed Management** configuration to support real-time availability, allocation, and billing of beds during patient admission.

- **Ward/Room/Bed Master**
  - **Building / Block Setup**
    - Master list of hospital buildings/blocks (e.g., Main Block, OT Block, Cardiac Block, NICU Block).
    - Attributes:
      - Code, Name, Address/Location, Branch.
      - Active/Inactive, display order.
  - **Floor / Ward Setup**
    - Master list of floors and wards per building:
      - Floor No/Name, Ward Name/Code (e.g., 3rd Floor ICU, Ward 5, NICU).
    - Attributes:
      - Type (General Ward, ICU, NICU, HDU, Private, Day Care, Emergency Observation).
      - Gender restriction (if any).
      - Clinical Specialty focus (optional).
  - **Room & Room Type Setup**
    - Room Type master:
      - General Ward Bed, Semi-private Cabin, Private Cabin, Deluxe Room, Suite, Isolation Room, ICU Bed, NICU Cot, etc.
    - For each room type configure:
      - Default bed category/class.
      - Maximum occupancy (beds per room).
      - Amenities flags (AC, TV, attendant sofa, washroom, etc.) – mainly for information and pricing.
  - Building / Unit and Ward structure:
    - Hospital Building / Unit (e.g., Building A, Main Block, NICU Block).
    - Ward / Floor / Unit (e.g., Ward 1, 3rd Floor ICU, CCU).
    - Room / Cabin hierarchy where applicable.
  - Bed master:
    - Unique **Bed No** / Bed identifier.
    - Bed Categories / Types (configurable, minimum set):
      - General.
      - Cabin.
      - Shared Cabin.
      - NICU, PICU.
      - ICU, CCU.
      - Ward.
      - Day Care / Observation (where applicable).
    - Attributes:
      - Bed Type / Class (e.g., General, Cabin, ICU).
      - AC / Non-AC (where applicable).
      - Gender restriction (if any).
      - Bed Charge / tariff configuration (per day or per hour, currency, effective dates).
      - Status:
        - Available.
        - Occupied.
        - Reserved.
        - Maintenance / Blocked.
      - Extra attributes for shared and critical care beds (where applicable):
        - Maximum sharing capacity (e.g., Shared Cabin beds per cabin).
        - NICU/PICU/ICU/CCU clinical category / acuity level (for reporting and billing).
        - Isolation / special precautions flags.
  - Bed information fields (as part of master configuration):
    - Building / Unit (Hospital Building No / name).
    - Bed No (unique bed identifier).
    - Ward / Floor name.
    - Room / Cabin number (where applicable).
    - Bed Type (e.g., General / Cabin / ICU).
    - Bed Charge (default per-day or per-hour charge).
    - Status (Available / Occupied / Reserved / Maintenance/Blocked).

- **Bed Allocation & Status Rules**
  - Allocation constraints:
    - A bed must be in **Available** status to be assigned to an admission.
    - Prevent double allocation of the **same bed** to more than one active patient at the same time.
    - Allow category-level sharing where configured (e.g., Shared Cabins, NICU, PICU, ICU, CCU, Ward) while still tracking each **bed position** uniquely.
    - One **primary bed** per patient per admission.
    - Optional **extra bed** per admission (where configuration permits), with its own charge and status tracking.
  - Status behavior and system actions:
    - On **Admission save**:
      - Selected primary bed status changes from **Available** (or Reserved) to **Occupied**.
      - Optional extra bed (if selected) also changes to **Occupied**.
      - Bed(s) are locked against conflicting allocations for the active admission period.
    - On **Bed change / transfer**:
      - Previous bed’s status updates (e.g., to Available, Cleaning, or Maintenance/Blocked as per configuration).
      - New bed’s status becomes **Occupied** and linked to the same admission.
    - On **Patient release / discharge**:
      - Bed status changes from **Occupied** to **Available** (or another configured post-discharge status such as Cleaning).
      - No further bed charges accrue after configured discharge date/time.
  - Additional status rules:
    - Support temporary statuses:
      - Cleaning, Inspection, Blocked/Admin Hold.
    - Rules for:
      - Auto-switch from Cleaning to Available after configured duration or manual confirmation.
      - Blocking a bed for maintenance with reason and expected date of release.

- **Bed Billing Rules**
  - Charge calculation:
    - Per day / per hour (configurable at hospital level and/or by bed category).
    - Minimum billable period (e.g., minimum 1 day / minimum N hours).
    - Auto-calculation of bed charge duration based on:
      - Admission date/time.
      - Bed change / transfer timestamps.
      - Discharge date/time.
  - Upgrade/downgrade rules across bed classes and packages:
    - Clear handling of tariff differences when moving between:
      - Lower to higher class (upgrade).
      - Higher to lower class (downgrade).
    - Rules for partial-day upgrades/downgrades (e.g., whether higher or lower class rate applies for overlapping periods).
  - Billing linkage:
    - Bed charge configuration is auto-linked to the corresponding **IPD billing account** for the admission (see `billing.md`).
    - Support separate billing for:
      - Primary bed.
      - Extra bed (if enabled and assigned).

- **Indoor Package Configuration**
  - Inpatient packages (e.g., maternity, surgery packages).
  - Included and excluded services/medicines.
  - Overstay rules and overage charges.

- **Bed Pricing Matrix**
  - Configure tariff matrix by:
    - Bed Category / Class.
    - Room Type.
    - Payer Type (Self, Corporate, Insurance, Government Scheme).
    - Season or special period (optional; e.g., festive, pandemic).
  - Support:
    - Effective date ranges and versioning.
    - Separate pricing for:
      - Inclusions in package vs non-package admissions.
      - Extra bed/attendant bed where applicable.

- **Admission & Discharge Type Masters**
  - **Admission Type Master**
    - Examples:
      - Planned / Elective.
      - Emergency.
      - Transfer-in (from another facility).
      - Day Care.
    - For each type configure:
      - Allowed sources (OPD, Emergency, Direct Admission).
      - Default deposit rule (if any).
      - Whether consent and pre-authorization are mandatory.
  - **Discharge Type Master**
    - Examples:
      - Planned/Recovered.
      - LAMA (Leave Against Medical Advice).
      - Referred/Transferred Out.
      - Death.
    - For each type configure:
      - Required documentation (e.g., death summary, medico-legal forms).
      - Billing rules (e.g., waiver conditions, partial waivers – if allowed by policy).

#### 3.4 Billing & Financial Configuration

- **Discount Configuration**
  - All discount types across modules.
  - Applicable scenarios and maximum limits.

- **Financial Rules**
  - Rounding rules (per line/per bill).
  - Tax inclusion/exclusion behavior.
  - Posting strategy (summarized vs detailed) to Accounts module.

- **Bank Information**
  - Bank accounts used for collections, refunds, and vendor payments.
  - Visibility in billing and receipts.

- **Billing & Pricing Setup**
  - **Service Category Master**
    - Hierarchical service categories aligned with Department/Category Management:
      - Consultation, Procedure, Surgery, Diagnostic (Lab/Imaging), Nursing, Room/Bed, Package, Others.
    - Used to:
      - Group services for pricing, discounting, and reporting.
      - Drive department-wise and revenue center-wise analytics.
  - **Service Price List**
    - Base price list for:
      - All clinical services (from Clinical Service Catalog).
      - Non-clinical services (e.g., canteen, miscellaneous).
    - Features:
      - Effective date range and version history.
      - Separate price lists per:
        - Branch/Location.
        - Payer group (Self, Corporate, Insurance, Government Scheme).
      - Bulk update tools (e.g., % increase by department or category).
  - **Package Discount & Rule Configuration**
    - Rules for packages:
      - Package price vs sum of individual items.
      - Included vs excluded items (e.g., ICU stay limited to N days).
      - Overstay/overage charge rules.
    - Discount rules:
      - Package-level discounts vs line-level discounts.
      - Eligibility by:
        - Patient category (e.g., Staff, VIP).
        - Campaign/corporate contract.
  - **Dynamic & Time-based Pricing**
    - Dynamic pricing parameters:
      - Rate by time of day (day vs night).
      - Emergency surcharge flag and percentage.
      - Weekend/holiday pricing (optional).
    - Configuration:
      - Define “night hours” and “emergency” windows.
      - Map which services and bed categories are eligible for time-based pricing.
  - **Credit & Limit Configuration**
    - **Credit Limit Setup**
      - Define credit limits by:
        - Corporate/Insurance account.
        - Patient category (e.g., Staff, VIP).
      - Rules when limit is exceeded:
        - Hard stop vs soft stop with approval.
        - Notification and escalation to finance/admin.
    - **Credit Policy**
      - Payment terms (e.g., 30/60/90 days).
      - Interest or penalty rules for overdue invoices (if applicable).
      - Credit eligibility criteria and approval workflow.
    - **Refund & Write-off Rules**
      - Master for refund reasons and allowed scenarios.
      - Write-off types (e.g., small balance, charity, management decision).
      - Approval levels and required documentation.
  - **Tax & Invoice Setup**
    - **Tax Setup (VAT/GST or Local Tax)**
      - Define tax types, rates, and applicability:
        - By service category.
        - By item/medicine category.
      - Inclusive vs exclusive tax behavior per service/medicine.
    - **Invoice Number Series**
      - Configure invoice series per:
        - Location/branch.
        - Billing type (OPD, IPD, Pharmacy, Canteen, Corporate).
      - Reset rules (yearly, monthly) and format (prefix, running number, suffix).
    - **Payment Method Master**
      - Cash, Card, bKash, Nagad, Bank Transfer, Cheque, Others.
      - Configure:
        - Settlement mapping to bank/cash accounts.
        - Additional required fields (e.g., transaction ID, cheque no, bank name).

#### 3.5 HR & Accounts Configuration (Integration)

- **HR Configurations**
  - Designations, Pay Scales, Duty Schedules, Rosters, Leave Types, National Holidays, Bonuses, Departments.
  - Managed in HR module; Hospital Operations must be able to reference:
    - Department.
    - Roster and duty data for dashboards and approvals.
  - **HR & Payroll Master Setup**
    - **Employee Grade**
      - Grade bands (e.g., Grade I–V, Junior/Senior Consultant, Nursing Grades).
      - Map grades to:
        - Eligibility for benefits, allowances, and overtime.
        - Approval levels in workflows (e.g., who can approve whom).
    - **Salary Structure**
      - Define salary structures/templates per:
        - Employee grade and designation.
      - Components:
        - Basic, House Rent, Medical, Conveyance, Other Allowances, Deductions.
      - Versioning for structure changes with effective dates.
    - **Payroll Components**
      - Master list of earning and deduction components:
        - e.g., Basic, HRA, Overtime, Incentive, Commission, Bonus, Provident Fund, Tax.
      - Attributes:
        - Type (Earning/Deduction).
        - Taxable vs non-taxable.
        - Fixed vs variable.
    - **Attendance & Leave Policy**
      - Attendance Policy:
        - Shift timings, grace periods, late/early rules.
        - Half-day and full-day absence thresholds.
      - Leave Type Setup:
        - Casual Leave, Sick Leave, Earned Leave, Maternity Leave, Compensatory Off, etc.
        - Rules per leave type:
          - Annual entitlement, carry-forward, encashment, approval levels.
      - Overtime Policy:
        - Eligibility by grade or department.
        - Calculation method (e.g., hourly rate multiple).
    - **Approval Matrix**
      - Multi-level approval chains for:
        - Leave applications.
        - Overtime approvals.
        - Expense claims (if in scope).
      - Mapping of approvers by:
        - Department.
        - Grade.

- **Accounts Configurations**
  - Chart of Accounts, Voucher Types, Cost Centers, Contra Codes, VAT/GST, Service-wise Tax.
  - Maintained in Accounts module; referenced for:
    - Mapping services and medicines to GL and cost centers.
    - Voucher creation from Billing, Pharmacy, and Canteen.
  - **Financial & Accounting Setup**
    - **Chart of Accounts (COA)**
      - Standard COA structure:
        - Assets, Liabilities, Equity, Revenue, Expenses.
      - Hospital-specific segments:
        - Department, Service Line, Branch.
      - Mapping rules:
        - Each service/medicine/bed category mapped to default revenue GL.
        - Each store/pharmacy mapped to stock and COGS GLs.
    - **Cost Center & Revenue Center Setup**
      - Cost Centers:
        - Departments or units incurring cost (e.g., ICU, OT, Laboratory, Radiology, Pharmacy).
      - Revenue Centers:
        - Groups used for revenue reporting (e.g., OPD Consultation, IPD Bed, Diagnostics, OT).
      - Rules:
        - All billing lines must carry both cost center and revenue center where applicable.
    - **Tax & Compliance Accounts**
      - Tax GLs for VAT/GST or local taxes.
      - Withholding tax configuration (if applicable).
    - **Credit, Refund, and Write-off Policies**
      - Link to Billing & Pricing Setup for:
        - Credit Policy rules.
        - Refund workflows.
        - Write-off approvals.
    - **Insurance Claim Rules**
      - High-level rules for claim handling:
        - Mapping payer contracts to revenue/cost centers.
        - Claim status lifecycle (e.g., Submitted, Query, Approved, Rejected).
        - Basic validation checks before claim submission (e.g., authorization attached, diagnosis present).
    - **Shareholder & Profit Distribution Setup**
      - Master data for shareholders/owners:
        - Name, percentage share, active/inactive.
      - Profit Distribution Rule:
        - High-level configuration for how profits are allocated:
          - By shareholding, by doctor pool, by department, or hybrid.
        - Detailed accounting logic to be defined in Accounts module implementation.

#### 3.6 Approval Matrix Configuration

- **Department-wise Approval Rules**
  - Define who can approve:
    - Discounts beyond threshold.
    - Credit or limit exceptions.
    - Refunds and write-offs.
    - Cancellation or reversal of key transactions (e.g., final bill cancellation).
  - Multi-level approvals:
    - E.g., Supervisor → Manager → Director.
  - Time-bound approvals and escalation options.

#### 3.7 Marketing & B2B Configuration

- **Marketing Person Master**
  - Fields: Code, Name, Contact details, Zone/Area, Commission type (fixed/percentage), Active/Inactive.

- **B2B Client Registration**
  - Corporate clients, TPAs, insurance entities, referral partners.
  - Contract details: agreed tariffs, packages, credit limit, payment terms, validity.

- **B2B Chart/Tariff Info**
  - Contract-specific price lists (for services, bed categories, packages).
  - Effective date ranges and versioning.
  - **Marketing & Corporate Setup Extensions**
    - **Referral Doctor Master**
      - External/referral doctor registry with:
        - Name, Specialty, Contact details, Affiliated organization, Registration numbers.
      - Map to:
        - Commission/fee sharing rules (if applicable).
        - Preferred hospitals/locations.
    - **Corporate Pricing Rules**
      - Configure corporate-specific tariffs:
        - Discount percentages or special package prices.
        - Service bundles for corporate clients (e.g., annual health check plans).
      - Link to:
        - B2B tariff charts.
        - Credit limits and payment terms.
    - **Marketing Campaign Master**
      - Campaign details:
        - Name, Code, Period, Target segment, Owner (Marketing Person).
      - Configuration:
        - Linked offers (e.g., discount packages, free tests).
        - Tracking parameters for referrals (online/offline).
    - **Commission Structure Setup**
      - Commission rules for:
        - Marketing persons.
        - Referral doctors.
      - Rule dimensions:
        - Percentage or fixed amount.
        - By service category, department, or corporate.
      - Cap and clawback rules where required.
    - **Territory / Zone Setup**
      - Define marketing territories/zones:
        - Region, City, Area, Corporate clusters.
      - Link:
        - Marketing persons and referral doctors to territories.
        - Corporate clients for territory-wise performance tracking.
    - **Client Contract Terms**
      - Structured configuration of:
        - Credit limit and payment terms.
        - Exclusions/inclusions (services covered/not covered).
        - Co-pay rules (patient vs corporate share).
      - Versioning:
        - Contract start/end dates.
        - Renewal history and amendments.

#### 3.8 Pharmacy & Store Configuration

- Supplier, product unit, product masters (Medicine and Medical Store), Store configuration, and Pharmacy user configuration are defined in detail in `pharmacy.md` and Inventory requirements. Hospital Operations references them as base configuration.
  - **Medicine & Drug Masters**
    - **Generic Name Master**
      - Central list of generic drug names with:
        - Generic Name, Therapeutic Class, ATC/Standard code (where applicable).
      - Used as base for prescribing and stock management.
    - **Brand Name Master**
      - Brand/commercial names mapped to:
        - Generic name.
        - Manufacturer.
        - Strength and dosage form.
      - Support:
        - Active/Inactive status (for discontinued brands).
    - **Drug Category Master**
      - Categories such as:
        - Antibiotics, Analgesics, Antipyretics, Antihypertensives, Insulins, Vaccines, etc.
      - Map to:
        - Stock valuation and reporting.
        - Alerts and restrictions (e.g., high-risk drugs).
    - **Manufacturer Master**
      - Master for pharmaceutical manufacturers with:
        - Name, Address, Contact details, License info.
      - Used for purchase, recalls, and reporting.
    - **Dosage Form & Strength Setup**
      - Master for dosage forms:
        - Tablet, Capsule, Syrup, Injection, Cream, Ointment, Drops, Inhaler, etc.
      - Strength definitions:
        - Numerical strength and unit (e.g., 500 mg, 5 mg/mL).
      - Combined with generic & brand for precise item identification.
    - **Medicine Schedule / Controlled Drug Flags**
      - Schedule master (based on local law):
        - e.g., Schedule H, X, Narcotics, OTC, etc.
      - Configuration:
        - Additional documentation requirements.
        - Restrictions (e.g., only specific prescriber levels).
  - **Batch, Expiry & Stock Rules**
    - **Batch & Expiry Rule**
      - Enforce batch-wise stock management with:
        - Batch No, Manufacturing Date, Expiry Date.
      - Configurable rules:
        - Minimum remaining shelf-life on receipt.
        - Block/hold stock within N days of expiry.
        - Prevent sale/issue of expired items.
    - **Pharmacy Pricing Rule**
      - Define pricing formula:
        - Cost + Markup %, MRP-based, Contract-based (for corporate/insurance).
      - Separate rules per:
        - Outlet/branch.
        - Item category or manufacturer.
      - Support maximum discount caps and approval needs.
    - **Auto Reorder Level**
      - Item-level configuration for:
        - Reorder Level, Reorder Quantity, Minimum/Maximum levels.
      - Reorder triggers:
        - Based on current stock vs reorder level.
        - Optionally based on consumption averages over past N days.
      - Reorder notifications to:
        - Store/Pharmacy in-charge and Purchase department.

  - **Inventory & Store Setup (Enterprise)**
    - **Item Category Master**
      - Hierarchical categories for:
        - Medicines, Surgical items, Consumables, Implants, Non-medical items, Assets (if tracked).
      - Used for:
        - Stock valuation, consumption analysis, and reorder rules.
    - **Unit of Measure (UOM) Master**
      - Base and alternate units (e.g., Box, Strip, Tablet; Vial, mL; Piece, Pack).
      - Conversion factors between purchase UOM and issue UOM.
    - **Supplier/Vendor Master**
      - Supplier details:
        - Name, Address, Contact, Tax registration, Payment terms.
      - Flags:
        - Preferred vs alternate supplier.
        - Blacklist/inactive status with reasons.
    - **Warehouse & Store Location Setup**
      - Central warehouse(s) and sub-stores:
        - Main Store, Pharmacy Store(s), OT Store, CSSD, Ward Stores, Lab Store.
      - Attributes:
        - Location, responsible user/owner, type (issuing vs storage).
      - Store Location/Bin:
        - Optional bin/rack-level configuration for large warehouses.
    - **Stock Valuation Method**
      - Configurable at organization or store level:
        - FIFO, Weighted Average (recommended), or others as allowed by policy.
      - Once chosen, changes should be tightly controlled with finance approval.
    - **Reorder & Issue Approval Rules**
      - Reorder Rules:
        - Global vs item-specific parameters for:
          - Safety stock, lead time, minimum order quantity.
      - Issue Approval Rules:
        - Thresholds for:
          - High-value items.
          - Controlled or restricted items (e.g., implants, high-cost drugs).
        - Approval workflow:
          - Nurse/Doctor request → Store Keeper → Store/Pharmacy In-charge → Management (for exceptional cases).

#### 3.9 Logistics & Transport

- **Driver & Vehicle Master**
  - Vehicles (ambulances, transport) with type, registration number, capacity, equipment.
  - Drivers: license details, contact, schedule/shift info.
  - Used for ambulance billing, patient transport, and logistics planning.
  - **Ambulance Type Master**
    - Configure ambulance types:
      - BLS (Basic Life Support), ALS (Advanced Life Support), ICU Ambulance, Neonatal Ambulance, General Transport.
    - For each type define:
      - Equipment and staffing expectations (for information).
      - Default tariff and minimum charge.
  - **Trip Type Master**
    - Examples:
      - Patient Transfer (internal between wards/buildings).
      - Inter-facility Transfer.
      - Emergency Call.
      - Supply/Logistics Trip.
    - For each type configure:
      - Billing rules (billable vs non-billable).
      - Payer defaults (patient, corporate, internal/cost center).
  - **Fuel & Maintenance Configuration**
    - **Fuel Expense Setup**
      - Capture fuel purchase entries with:
        - Vehicle, Date, Quantity, Amount, Vendor, Odometer reading.
      - Rules:
        - Approval levels for large fuel expenses.
        - Fuel efficiency monitoring (KM/Litre) via reports.
    - **Vehicle Maintenance Schedule**
      - Maintenance master by vehicle:
        - Routine service intervals (e.g., every 5,000 km or 6 months).
        - Safety checks and statutory inspections.
      - Alerts:
        - Upcoming maintenance due based on date/odometer.
        - Overdue tasks highlighted on dashboard.
  - **Driver Shift Scheduling**
    - Define shift patterns for drivers (e.g., Morning, Evening, Night).
    - Link drivers to:
      - Shifts.
      - Assigned primary vehicles.
    - Integration:
      - With HR attendance where applicable.
      - With trip allocation to avoid overbooking or rule violations (e.g., maximum consecutive hours).

#### 3.11 Laboratory Configuration

- **Test Category & Sample Masters**
  - **Test Category Master**
    - Group tests into logical categories:
      - Biochemistry, Hematology, Microbiology, Serology, Pathology, Radiology (if shared), Others.
    - Used for:
      - Menu organization, pricing, and reporting.
  - **Sample Type Master**
    - Sample types:
      - Blood, Serum, Plasma, Urine, Stool, CSF, Swab, Tissue, Aspirate, etc.
    - Map:
      - Which sample types each test can use.
  - **Machine/Analyzer Mapping**
    - Link tests to:
      - One or more lab machines/analyzers.
    - Attributes:
      - Default machine priority.
      - Code mapping between LIS and analyzer (if applicable).

- **Result & Reference Configuration**
  - **Result Template Master**
    - Result entry layouts per test/panel:
      - Single result, multi-parameter, tabular, narrative.
    - Supports:
      - Normal/Abnormal flags.
      - Units and decimal precision.
  - **Reference Range Setup**
    - Reference ranges by:
      - Age group, Sex, Special population (e.g., pregnancy).
    - Effective date/version control.
  - **Critical Value Alert Rules**
    - Master list of critical values per test:
      - Lower/upper critical thresholds.
    - Alert configuration:
      - Notification targets (responsible doctor/nurse).
      - Escalation rules and acknowledgement logging.

- **Panels, Outsourcing, and Assignment**
  - **Lab Panel Setup**
    - Define panels/profiles:
      - e.g., LFT, RFT, Cardiac Panel, Fever Panel.
    - Configure:
      - Included tests.
      - Panel pricing vs sum of individual tests.
  - **Outsource Lab Setup**
    - Master for external labs:
      - Name, Address, Contact, Turnaround times, Transport rules.
    - For each test:
      - Flag as in-house vs outsourced.
      - Configure outsource lab and pricing.
  - **Pathologist/Consultant Assignment**
    - Rules for assigning:
      - Pathologist or radiologist (for imaging) to specific test types or departments.
    - Optional workload distribution rules:
      - Round-robin, load-based, or manual assignment only.

#### 3.10 Organization / Project Setup

- **Hospital/Organization Profile**
  - Hospital name, logo, branches, addresses, time zone, currency, financial year, regulatory registrations.
  - Configuration flags:
    - Local language support.
    - Date/time formats.
  - Used across reports, documents, and integrations.

#### 3.12 Security, Audit & System Configuration

- **Security & Login Policy**
  - Password policies:
    - Length, complexity, expiry, history.
  - Account lockout rules:
    - Failed login attempt thresholds and lockout duration.
  - Session Timeout:
    - Inactivity-based auto logout, configurable per role or globally.
  - Two-Factor Authentication (2FA):
    - Optional 2FA for:
      - High-privilege roles (e.g., Admin, Finance).
      - Remote access.
    - Support for:
      - OTP via SMS/Email or authenticator app (implementation detail).
- **Access Level Hierarchy**
  - Role hierarchy definition:
    - Hospital Admin, Department Head, Unit In-Charge, Frontline Staff, Read-only roles, etc.
  - Mapping:
    - Which hierarchy levels can:
      - Approve configuration changes.
      - View sensitive financial or clinical data.
      - Perform bulk operations (e.g., price updates).
- **Audit Log Setup**
  - Audit events to be logged for:
    - Logins/logouts and failed login attempts.
    - Configuration changes (masters, pricing, approval rules).
    - Financial actions (bill creation, discounts, refunds, write-offs).
    - Clinical data access (view/edit of patient records at a summary level – detailed EHR log in EHR module).
  - Configuration:
    - Retention period for audit logs.
    - Access rights for viewing audit trails.
- **Data Backup & Recovery Configuration**
  - Backup policies:
    - Frequency (daily/hourly), type (full/incremental).
    - On-site vs off-site backup (implementation detail).
  - Recovery objectives (high-level):
    - Target RPO/RTO to be agreed with infrastructure team.
  - Test and validation:
    - Periodic backup restore tests logged and reported.

#### 3.13 Printing, Labels & Barcode Configuration

- **Printer Mapping**
  - Map physical/virtual printers to:
    - Departments, Counters, Wards, Labs, Pharmacies.
  - Default printer per:
    - Document type (bill, receipt, report, label).
    - Location/role.
- **Label & Report Templates**
  - Label Template:
    - Barcode stickers for:
      - Patient ID, Sample labels, Medicine labels, Bed labels.
    - Configurable fields and layout per label type.
  - Report Template:
    - Header/footer configuration (logo, address, disclaimers).
    - Department-wise report templates (Lab, Radiology, Discharge Summary).
  - **Auto Print Rules**
    - Rules for auto-print on key events:
      - Registration (patient ID label).
      - Sample collection (sample labels).
      - Billing (invoice/receipt).
      - Pharmacy dispense (medicine label, if used).
    - Ability to:
      - Enable/disable auto printing per location.
      - Override with manual print commands.

#### 3.14 Smart Automation & Rules Engine (Enterprise)

- **Automation Rule Framework**
  - Central rules engine for:
    - Auto Bed Allocation.
    - Auto Billing and Package Suggestion.
    - Alerts & Notifications.
    - Escalation Workflows.
  - Rule configuration:
    - Condition builder (e.g., “if ward = ICU and bed type = X and patient class = IPD”).
    - Action definitions (e.g., “assign bed”, “add charge”, “send alert”).
- **Auto Bed Allocation Rules**
  - Default allocation logic:
    - Match by bed category, ward, and gender rules.
    - Try nearest available bed to requested ward/room type.
  - Priority rules:
    - Clinical priority (e.g., ICU > HDU > Ward) where applicable.
    - VIP/Corporate preferences where policy allows.
- **Auto Billing & Package Suggestion**
  - Auto Billing Rules:
    - Automatically add recurring charges (e.g., daily bed charge, nursing charge).
    - Auto-stop certain charges on discharge or transfer.
  - Package Suggestion:
    - When ordered services match configured patterns:
      - Suggest moving to an appropriate package (e.g., Health Check Package, Procedure Package).
    - Display:
      - Financial comparison: package vs individual items.
- **Clinical Decision Support Hooks (High-Level)**
  - Hooks for:
    - Allergy and drug interaction alerts (detailed logic in EHR/Prescription modules).
    - Vital sign thresholds triggering alerts (e.g., high fever, low SpO2).
  - Configuration:
    - Ability to turn specific alert types on/off or set sensitivity.
- **Alerts, Notifications & Escalation**
  - Alert & Notification Rules:
    - Events:
      - Critical lab values, bed overstay, credit limit breach, stock-out, maintenance due.
    - Channels:
      - In-app, SMS/Email (implementation detail), dashboard widgets.
  - Escalation Workflow:
    - For unresolved alerts within defined time:
      - Escalate to higher-level roles (e.g., from Nurse to Duty Doctor to Medical Superintendent).
    - Log:
      - Escalation history and acknowledgement timestamps.

---

### 4. Dashboards

#### 4.1 General Requirements

- Role-based dashboards:
  - Menu visibility and widget visibility driven by permissions and department.
- Near real-time data with refresh options (manual and auto-refresh).
- Drill-down from summary widgets to detailed views.

#### 4.2 Admin Dashboard

- Hospital-wide KPIs:
  - OPD visits (today, MTD, trend).
  - IPD admissions, discharges, transfers.
  - Bed occupancy and ALOS (average length of stay).
  - Revenue by department and service type.
  - Pending approvals (discounts, refunds, credit, purchase, etc.).
  - Critical stock alerts (near-expiry, low stock).

#### 4.3 General/User Dashboard

- Personalized view based on role:
  - Doctors: today’s appointments, scheduled procedures, pending reports or approvals.
  - Billing staff: bills created, collections, pending dues.
  - Store/Pharmacy users: pending requisitions, stock alerts.
  - HR/Managers: attendance and roster adherence indicators.

#### 4.4 IPD Dashboard

- IPD-specific metrics:
  - Total admitted, newly admitted, discharged, and transfers.
  - Bed occupancy by ward/unit.
  - Patients with long stays or high-risk flags.
  - Pending procedures, lab tests, and discharge summaries.

#### 4.5 OPD Dashboard

- OPD-specific metrics:
  - Registration and visit counts.
  - Waiting time by doctor/department.
  - No-show rate and cancellations.
  - Queue length per doctor.

#### 4.6 Inventory Dashboard

- Inventory/Store and Pharmacy view:
  - Stock value and stock movement.
  - Fast-moving and slow-moving items.
  - Near-expiry and expired items.
  - Pending purchase requisitions, POs, and goods receipts.

#### 4.7 HR Dashboard

- HR view:
  - On-duty vs off-duty staff.
  - Absences, late arrivals, early departures.
  - Overtime and shift compliance.
  - Pending leave applications and approvals.

---

### 5. Marketing & B2B Operations

#### 5.1 Marketing Operations

- Capture and manage:
  - Campaign codes or references.
  - Association of patients with marketing persons or campaigns (where applicable).
- Provide analytics:
  - Patient volume and revenue by marketing person, campaign, and source.

#### 5.2 B2B / Corporate Operations

- Manage:
  - Corporate patient registrations linked to specific B2B accounts.
  - Contract-specific benefits (tariffs, packages, covered vs non-covered).
- Operational support:
  - Pre-approval and guarantee letter management (where required).
  - Invoice submission tracking and payment follow-up.

---

### 6. Canteen Operations

#### 6.1 Description

Manage hospital canteen sales for patients, attendants, staff, and public, integrated with Inventory and Accounts.

#### 6.2 Core Features

- **Menu Management**
  - Menu items with categories:
    - Patient diet items.
    - Staff items.
    - General public items.
  - Attributes:
    - Item Name, Category, Portion Size, Price, Diet tags (e.g., diabetic, low salt).
    - Active/Inactive status.

- **Order Management**
  - Counter orders (walk-in).
  - Patient-room orders:
    - Link to patient and bed/room.
    - Option to post charges to patient IPD bill (as per configuration).

- **Stock & Consumption**
  - Raw material stock and consumption calculation per sale.
  - Requisition to central store for raw materials.

- **Billing & Payment**
  - Cash, card, and non-cash payments.
  - Optional integration with Billing module for patient charges.

#### 6.3 Reporting

- Daily sales summaries and item-wise sales.
- Cost and margin analysis (using purchase cost and selling price).
- Wastage and variance reports (optional based on configuration).

---

### 7. Reporting & Analytics

#### 7.1 HR & Payroll Reports

- Payroll and payslip summaries.
- Roster and shift compliance.
- Attendance, overtime, and leave usage.
- Employee list by department, role, and status.

#### 7.2 Financial & Accounts Reports

- Daily collection report (by mode, by counter, by user).
- Department-wise and doctor-wise revenue.
- OPD/IPD revenue, insurance vs cash revenue.
- Outstanding and aging reports by payer (patient, corporate, insurance).
- Profit & Loss, Balance Sheet, Cash Flow (from Accounts module).

#### 7.3 Pharmacy & Store Reports

- As detailed in `pharmacy.md` and Inventory requirements:
  - Stock, expiry, sales, discount, due, sales return, gross profit, stock adjust, transfer history, department issue.

#### 7.4 Discount & Approval Reports

- Discount type-wise summary.
- User-wise discount usage.
- Patient/invoice-wise discount history.
- Pending vs approved discounts and adjustments.
- Revenue impact analysis of discounts and approvals.

#### 7.5 General Reporting Requirements

- Role-based access and export permissions.
- Common filters:
  - Date range, department, doctor, payer, service, location.
- Exports:
  - Excel/CSV/PDF.
- For large reports:
  - Server-side pagination and optional background generation with notifications.

---

### 8. EHR Integration into Hospital Operations

#### 8.1 Description

Clinical EHR data (patient registration, encounters, diagnoses, procedures, orders, and results) must be consistently linked into hospital operational flows (billing, pharmacy, reporting).

#### 8.2 Integration Points

- **Billing**
  - OPD/IPD bills reference:
    - Visits and encounter IDs.
    - Diagnoses and procedures.
    - Service orders (lab, imaging, procedures).

- **Pharmacy**
  - Medicine orders and prescriptions from EHR feed directly into Pharmacy module.
  - Dispensing activity updates medication administration and medication history in EHR where applicable.

- **Diagnostics**
  - Orders placed from EHR must be traceable to:
    - Scheduling/performing modules.
    - Billing lines.
    - Result reports.

- **Reporting & Analytics**
  - Combine clinical data (diagnoses, procedures) with operational and financial data to produce:
    - Case-mix statistics.
    - Revenue by diagnosis/procedure.
    - Quality and outcome measures (where defined).

#### 8.3 Security & Access Control

- User Management and role/permission configuration must enforce:
  - Which users can view, create, or edit clinical vs financial vs operational data.
  - Separation of duties where required (e.g., clinical staff vs billing staff vs auditors).

