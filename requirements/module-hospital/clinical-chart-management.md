# Clinical Chart Management – Chargeable Items Master

## 1. Overview

**Description**  
The Clinical Chart Management module is the **master configuration** for all **chargeable clinical and non-clinical items** used across the hospital’s IPD workflows. It standardizes **codes, rates, department mappings, and billing behavior** for:
- Indoor procedures and services  
- Diagnostic investigations  
- OT services  
- Doctor visit charges  
- Canteen & dietary items  
- Packages / bundles

It is a **pre‑requisite** for:
- `procedure-entry-ipd.md` (IPD Procedure Entry)  
- `doctor-visit-entry-ipd.md` (Doctor Visit Entry – IPD)  
- `ot-procedure-entry-ipd.md` (OT Procedure Entry – IPD)  
- `billing.md` (IPD/OPD Billing and discounts)

**Purpose**  
- Ensure **single source of truth** for all billable items.  
- Enforce **consistent pricing and discount behavior** across modules.  
- Provide robust **departmental mapping** for reporting and financial analysis.  
- Control which items can be used in transaction screens (active vs inactive).

---

## 2. Module Scope

Clinical Chart manages master records for the following categories:
- **Indoor Procedures** (medical/surgical/bedside procedures)  
- **Diagnostic Investigations** (lab, imaging, other diagnostics)  
- **OT Services** (OT time, anesthesia, consumables as modeled)  
- **Doctor Visit Charges** (visit fee schedules, special consults)  
- **Canteen & Dietary Charges** (patient meals, staff meals, packages)  
- **Packages / Bundles** (fixed-price combinations of services/items)

> The exact category taxonomy (e.g., via `RepoName`, `Department`, `HoGroup`) is configurable but must remain consistent across modules.

---

## 3. Master Fields

### 3.1 Basic Information

- **SL**  
  - Type: Auto (integer sequence)  
  - Required: Yes  
  - Description: Internal serial identifier for display/sorting.  
  - Behavior: Auto-generated; not used as business key.  

- **Code \***  
  - Type: String  
  - Required: Yes; globally unique within Clinical Chart.  
  - Description: Primary **item code** used across transaction modules.  
  - Validation:
    - Cannot be blank.  
    - Must be unique (no duplicates, case-insensitive).  

- **Investigation / Procedure Name \***  
  - Type: String  
  - Required: Yes  
  - Description: Human-readable name of the investigation/procedure/item.  
  - Validation:
    - Non-empty; length and character set as per system rules.  

### 3.2 Rate Configuration

- **Rate (Base Rate)**  
  - Type: Decimal  
  - Required: Yes  
  - Description: Base or reference rate of the item before discounts and overrides.  
  - Validation:
    - Numeric, `≥ 0`.  

- **Fix (Fixed / Editable Flag)**  
  - Type: Boolean / Enum (`Fixed`, `Editable`)  
  - Required: Yes  
  - Description: Controls whether the rate can be edited at transaction time (Procedure Entry, Billing).  
  - Behavior:
    - `Fixed` (`Yes`): Rate locked in transaction screens; no user overrides allowed.  
    - `Editable` (`No`): Rate may be overridden based on role/permission and discount rules.  

- **AfRate (After Fix / Billing Rate)**  
  - Type: Decimal  
  - Required: Yes  
  - Description: Effective **billing rate** used when posting to invoices.  
  - Behavior:
    - Typically equal to `Rate` unless configuration sets different logic (e.g., loaded from external tariffs).  
    - Used as **Charge** in Procedure Entry and other transaction modules.  
  - Validation:
    - Numeric, `≥ 0`.  

- **Ref (Reference Flag)**  
  - Type: Enum (`Doctor`, `Department`, `None`, extensible)  
  - Required: No  
  - Description: Indicates if rate is **doctor-referenced**, **department-referenced**, or **generic**.  
  - Usage:
    - `Doctor`: Rate may vary by doctor; downstream modules should consider doctor-specific tariffs.  
    - `Department`: Rate may vary by department.  
    - `None`: Global rate.  

### 3.3 Department Mapping

- **RepoName (Report / Category Name)**  
  - Type: String  
  - Required: No  
  - Description: Logical category/grouping name used in reports (e.g., “Lab – Biochemistry”, “Canteen”).  

- **Department \***  
  - Type: Enum / Lookup (e.g., `Canteen`, `OT`, `IPD`, `OPD`, `Diagnostics`)  
  - Required: Yes  
  - Description: High-level department under which the item falls.  
  - Validation:
    - Must be selected from a predefined list.  

- **SubDeptName**  
  - Type: String / Enum  
  - Required: Recommended for complex organizations  
  - Description: Sub-department (e.g., “Canteen”, “Ward Procedures”, “Lab – Hematology”).  

- **SubSubDeptName**  
  - Type: String / Enum  
  - Required: Optional  
  - Description: Further sub-categorization if required.  

- **HoGroup (Head of Group)**  
  - Type: String / Enum  
  - Required: Recommended  
  - Description: Logical **grouping code** used for:
    - Discount eligibility rules  
    - Package/bundle mapping  
    - Reporting aggregation (e.g., “DoctorVisit”, “OTCharges”, “Canteen”, “Package”)  

---

## 4. CRUD Operations

### 4.1 Create

- Ability to create new Clinical Chart items with all mandatory fields.  
- **Code must be unique** across active and inactive items.  
- **Department** (and relevant SubDept) are mandatory for correct routing and reporting.  
- Business rules:
  - System should block creation if:
    - Code already exists (including soft-deleted records).  
    - Required mappings (Department, Name, Rate) are missing.  

### 4.2 Read

- **Search** capabilities:
  - By Code  
  - By Name (partial match)  
  - By Department / SubDept / HoGroup  
- **Filtering**:
  - Filter by Department (e.g., `Canteen` only).  
  - Filter by active/inactive items.  

### 4.3 Update

- Fields such as Name, RepoName, SubDept, HoGroup can be updated subject to permissions.  
- **Rate updates**:
  - Rates (Rate/AfRate) are editable based on **Fix flag** and user role:
    - If `Fix = Yes`:  
      - Only high-privilege Admin roles may update master rate.  
      - Rate is non-editable at transaction level.  
    - If `Fix = No`:  
      - Rate may be updated in master by Admin roles; transaction-level overrides may be allowed.  
- **Department change restrictions**:
  - If the item has already been used in invoices or procedure entries:
    - Changing Department/SubDept should be restricted or require an explicit migration/override process.  
    - At minimum, show a warning and audit the change.  

### 4.4 Delete (Soft Delete)

- Only **soft delete** is allowed (e.g., `Active = No` flag).  
- When an item has been used in any invoice or procedure entry:
  - It **cannot be hard deleted**.  
  - Soft delete hides it from transaction screens but retains it for historical records.  
- On soft delete:
  - Item is excluded from searches and dropdowns in Procedure Entry, OT, Doctor Visit, Billing, etc.  

---

## 5. Canteen Configuration Rules

For **Canteen** items, the following conventions apply:

### 5.1 Department & Sub‑Department

- **Department = Canteen**  
- **SubDeptName = Canteen** (or equivalent standard naming)  

Common items:
- Canteen Bill  
- Staff Lunch (multiple slabs / plans)  
- Regular Lunch / Dinner  
- Special menus (e.g., festival/summer packages)  

### 5.2 Business Rules

- **Staff vs Regular Pricing**  
  - Staff-specific items (e.g., “Staff Lunch – A”, “Staff Lunch – B”) must be clearly separated by Code/Name and/or HoGroup.  
  - Regular patient/visitor packages must use distinct codes from staff packages.  

- **Package Support**  
  - Canteen packages (e.g., “Summer Package”) support **fixed pricing**:
    - Typically **Fix = Yes** to prevent transaction-time rate edits.  
    - Mapped via HoGroup/category for bundle rules, if any.  

- **Procedure Entry Visibility**  
  - Canteen items must be selectable in **Indoor Procedure Entry** (and/or Billing) when allowed by policy.  
  - This is typically controlled through Department/SubDept filters and active status.  

---

## 6. Integration with Procedure Entry

Clinical Chart items are consumed by `procedure-entry-ipd.md` as follows:

- **Code** → Clinical Chart **Code**.  
- **Description** → `Investigation / Procedure Name`.  
- **Charge** → `AfRate` (Billing Rate).  
- **Unit** → Default unit (implicit from master; often treated as 1 unless explicit units are modeled elsewhere).  
- **NoU (Number of Units)** → Entered by user at transaction time.  
- **Total** in Procedure Entry → `Charge × NoU` (and any additional unit logic if modeled).  

Only **active** Clinical Chart items:
- Appear in Procedure Entry search/dropdowns.  
- Are available for selection in relevant departments (based on Department/SubDept filters).  

---

## 7. Billing & Discount Rules

- **HoGroup-driven Behavior**  
  - `HoGroup` influences:
    - **Corporate discount eligibility** (e.g., some groups may be exempt).  
    - Whether an item is part of **Packages/Bundles**.  
    - Inclusion in specific billing groups (e.g., Professional Fees vs Hospital Charges).  

- **Fixed vs Editable Rate (Fix flag)**  
  - `Fix = Yes`:
    - Rate is **locked** in Procedure Entry and other clinical transaction screens.  
    - User cannot change the rate; any discount must be applied via Billing discount logic (if allowed).  
  - `Fix = No`:
    - Rate is **potentially editable** at transaction level, subject to:
      - Role-based permissions.  
      - Maximum allowed discount or override thresholds.  

- **Corporate Billing**  
  - When patient is tagged as **Corporate** (see Admission & Billing rules):
    - Clinical Chart fields (Department, HoGroup, Ref) help determine whether:
      - Item is covered by corporate contract.  
      - Which tariff (corporate vs general) should apply.  

---

## 8. Validation Rules

- **Unique Code**  
  - Duplicate `Code` is **not allowed**, including across inactive/soft-deleted items.  

- **Rate Validation**  
  - `Rate` and `AfRate` must be numeric and `≥ 0`.  

- **Department Mapping**  
  - `Department` is mandatory; at least one of SubDept/SubSubDept should be provided for configurable organizations.  

- **Inactive Item Handling**  
  - Inactive (soft-deleted) items:
    - Are hidden from all transactional search/dropdowns (Procedure Entry, OT, Doctor Visit, Billing).  
    - Remain visible in master lists and reports with appropriate filters.  

---

## 9. Audit & Security

- **Audit Fields**
  - **Created By / Created Date** – captured on creation.  
  - **Updated By / Updated Date** – updated on each modification.  
  - **Deleted By / Deleted Date / Reason** – for soft delete (if modeled).  

- **Change Tracking**
  - Key fields (Code, Name, Rate, AfRate, Department, HoGroup, Fix) must have:
    - Before/after values stored in audit logs.  
    - Easy retrieval for compliance and financial reconciliation.  

- **Role-wise Access**
  - **Admin / Finance / Configuration roles**:
    - Full CRUD access (subject to deletion restrictions).  
  - **Operational/Clinical roles (Doctors, Nurses, OT Staff)**:
    - Read-only access to active items as needed (e.g., for configuration awareness).  
  - No general user should have permission to arbitrarily change Code or key pricing without strong controls.  

---

## 10. Dependency Matrix

| Module          | Depends On       | Usage in Module                                               |
|-----------------|------------------|---------------------------------------------------------------|
| Procedure Entry | Clinical Chart   | Uses Code, Name, AfRate, Dept/HoGroup for procedure billing. |
| Doctor Visit    | Clinical Chart   | Uses doctor visit charge items for visit billing.            |
| OT Entry        | Clinical Chart   | Uses OT service/procedure items for OT billing.              |
| Billing         | Clinical Chart   | Uses all chart items for invoice lines & discount rules.     |
| Reports / MIS   | Clinical Chart   | Uses Dept/HoGroup/RepoName for analytics and KPIs.           |

