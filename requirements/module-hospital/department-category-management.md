# Department / Category Management – Hierarchical Service Structure

## 1. Overview

**Description**  
The Department / Category Management module defines the **hierarchical medical and non‑medical service structure** used throughout the hospital system. It provides a consistent, configurable taxonomy of:
- Departments  
- Sub‑Departments  
- Sub‑Sub‑Departments  
- Head Groups (HGroup) / Category groups  

This hierarchy is a **pre‑requisite** for:
- `clinical-chart-management.md` (Clinical Chart – Chargeable Items Master)  
- `procedure-entry-ipd.md` (IPD Procedure Entry)  
- `doctor-visit-entry-ipd.md` (Doctor Visit charges grouping)  
- `ot-procedure-entry-ipd.md` (OT services classification)  
- `billing.md` (OPD/IPD Billing, discount groups, and reporting)  
- Canteen and Corporate Discount configuration

**Purpose**  
- Ensure every **service/test/procedure/item** is correctly classified.  
- Enable **accurate billing, corporate discount application, and package rules**.  
- Provide **clean reporting dimensions** for clinical, operational, and financial analytics.

---

## 2. Hierarchy Structure

The system follows a configurable **4‑level hierarchy**:

1. **Department**  
2. **Sub‑Department**  
3. **Sub‑Sub‑Department**  
4. **Head Group (HGroup)** – logical “head of group” or category-level grouping.

**Example**:
- **Department**: Diagnostic  
- **Sub‑Department**: Pathology  
- **Sub‑Sub‑Department**: Hematology  
- **Head Group (HGroup)**: CBC  

This same pattern applies for non‑clinical areas such as Canteen or Administrative charges, e.g.:
- **Department**: Canteen  
- **Sub‑Department**: Canteen  
- **Sub‑Sub‑Department**: Inpatient Meals  
- **Head Group**: Staff Lunch / Regular Lunch / Package  

---

## 3. Department Master

### 3.1 Fields

- **Department Code**  
  - Type: Auto-generated (string/integer)  
  - Required: Yes  
  - Description: Unique identifier for each Department.  
  - Behavior: System-generated; not editable in UI.  

- **Department Name \***  
  - Type: Text  
  - Required: Yes  
  - Description: Human-readable name (e.g., Diagnostic, OT, IPD, Canteen).  
  - Validation:
    - Must be unique (case-insensitive).  
    - Non-empty.  

- **Department Type**  
  - Type: Enum (`Medical`, `Non‑Medical`)  
  - Required: Yes  
  - Description: High-level classification for clinical vs ancillary/administrative departments.  

- **Applicable Area**  
  - Type: Enum (`OPD`, `IPD`, `Both`)  
  - Required: Yes  
  - Description: Indicates where this Department is applicable.  
  - Usage:
    - Helps filter items for OPD vs IPD billing and reporting.  

- **Status**  
  - Type: Enum (`Active`, `Inactive`)  
  - Required: Yes  
  - Description: Controls availability for mapping in Clinical Chart and transactional modules.  

### 3.2 Business Rules

- **Deletion Constraints**  
  - Department **cannot be deleted** if:
    - It has one or more Sub‑Departments linked.  
    - It is referenced by any Clinical Chart item, Billing entry, or configuration.  
  - In such cases, only **status changes** (Active → Inactive) are allowed.  

- **Inactive Behavior**  
  - Inactive Departments:
    - Are **hidden from Clinical Chart** when assigning Department.  
    - Cannot be selected for new chargeable items or new configuration mappings.  
    - Remain available in historical data and reports.  

---

## 4. Sub‑Department Master

### 4.1 Fields

- **Sub‑Department Code**  
  - Type: Auto-generated  
  - Required: Yes  
  - Description: Unique identifier for each Sub‑Department.  

- **Department \*** (Parent)  
  - Type: Lookup (Department Master)  
  - Required: Yes  
  - Description: Parent Department this Sub‑Department belongs to.  

- **Sub‑Department Name \***  
  - Type: Text  
  - Required: Yes  
  - Description: Name of the Sub‑Department (e.g., Pathology, Radiology, Canteen).  
  - Validation:
    - Non-empty; typically unique per Department.  

- **Status**  
  - Type: Enum (`Active`, `Inactive`)  
  - Required: Yes  

### 4.2 Business Rules

- **Parent Link**  
  - Every Sub‑Department **must** be linked to exactly one Department.  
  - If the parent Department is Inactive:
    - Sub‑Department becomes unavailable for new mappings (implicitly or explicitly).  

- **Deletion Constraints**  
  - Sub‑Department cannot be deleted if:
    - It is used in any Clinical Chart item.  
    - It has any Sub‑Sub‑Departments linked.  
  - In such cases, only **Inactive** status is allowed.  

- **Inactive Behavior**  
  - Inactive Sub‑Departments:
    - Are excluded from selection in Clinical Chart for new items.  
    - Do not appear as filters in new transaction context, but remain in reports.  

---

## 5. Sub‑Sub‑Department Master

### 5.1 Fields

- **Sub‑Sub‑Department Code**  
  - Type: Auto-generated  
  - Required: Yes  

- **Department**  
  - Type: Auto-filled from parent Sub‑Department’s Department.  
  - Read-only.  

- **Sub‑Department \***  
  - Type: Lookup (Sub‑Department Master)  
  - Required: Yes  

- **Sub‑Sub‑Department Name \***  
  - Type: Text  
  - Required: Yes  
  - Description: More granular category (e.g., Hematology, Biochemistry, Inpatient Meals).  

- **Status**  
  - Type: Enum (`Active`, `Inactive`)  
  - Required: Yes  

### 5.2 Business Rules

- **Parent Integrity**  
  - Must always be linked to a valid, Active Sub‑Department at creation.  
  - If Sub‑Department becomes Inactive:
    - New Clinical Chart mappings to its Sub‑Sub‑Departments should be blocked.  

- **Deletion Constraints**  
  - Cannot be deleted if:
    - Used in any Clinical Chart or configuration.  
  - Use soft inactivation instead.  

---

## 6. Head Group (HGroup) Management

> Note: HGroup is sometimes modelled directly in `clinical-chart-management.md` via the `HoGroup` field. This section defines its underlying master behavior when implemented as a separate, reusable structure.

### 6.1 Fields

- **Head Group Code / Name (HGroup)**  
  - Type: String / Enum  
  - Required: Yes  
  - Description: Logical group identifier (e.g., `CBC`, `DoctorVisit`, `OTCharges`, `Canteen-Staff`, `Package-Summer`).  

- **Department / Sub‑Department / Sub‑Sub‑Department** (Optional mappings)  
  - Type: Lookups  
  - Description: Optional explicit link of a Head Group to portions of the hierarchy if needed.  

- **Status**  
  - Type: Enum (`Active`, `Inactive`)  
  - Required: Yes  

### 6.2 Usage & Rules

- HGroup values are primarily used for:
  - **Discount rules** (e.g., corporate discount not applicable to certain groups).  
  - **Package / bundle mapping**.  
  - **Reporting aggregation** (grouping similar services across departments).  
- HGroup referenced by Clinical Chart (`HoGroup` field) must be Active to be used in new items.  

---

## 7. CRUD & Maintenance Operations

### 7.1 Create

- **Department**:
  - Requires unique Department Name, Department Type, Applicable Area, Status.  
- **Sub‑Department**:
  - Requires linked Department, unique Name (within Department), Status.  
- **Sub‑Sub‑Department**:
  - Requires linked Sub‑Department, Sub‑Sub‑Department Name, Status.  
- **HGroup**:
  - Requires HGroup Code/Name and Status.  

Validation:
- All required parent relationships must exist and be Active.  
- Duplicate naming/code patterns should be prevented where configured.  

### 7.2 Read

- Support search and filtering by:
  - Name, Code  
  - Type (Medical/Non‑Medical)  
  - Applicable Area (OPD/IPD/Both)  
  - Status  
- Views:
  - Hierarchical tree view (Department → SubDept → SubSubDept).  
  - Flat lists per level with filters.  

### 7.3 Update

- Renaming and status changes:
  - Allowed with audit logging.  
  - Must consider downstream impact on Clinical Chart, Billing, and reports.  
- Moving items across hierarchy:
  - Re‑parenting (e.g., moving Sub‑Department to a different Department) should be:
    - Strongly controlled or disallowed if items are already used.  
    - If allowed, must trigger reclassification logic and thorough audit.  

### 7.4 Delete / Inactivate

- **Hard Delete**:
  - Generally **disallowed** once any downstream references exist.  
- **Soft Inactivation**:
  - Primary strategy:
    - Status set to Inactive.  
    - Item excluded from new mappings and transactional selections.  
    - Historical references remain valid for reporting.  

---

## 8. Integration with Clinical Chart & Other Modules

### 8.1 Clinical Chart Integration

- Clinical Chart (`clinical-chart-management.md`) uses:
  - **Department** → mapped to Clinical Chart `Department`.  
  - **Sub‑Department** → mapped to `SubDeptName`.  
  - **Sub‑Sub‑Department** → mapped to `SubSubDeptName`.  
  - **HGroup** → mapped to `HoGroup`.  

- Rules:
  - Only **Active** Departments/Sub‑Departments/Sub‑Sub‑Departments are selectable when creating/editing Clinical Chart items.  
  - When a Department/SubDept is inactivated:
    - Existing Clinical Chart items retain the linkage for history.  
    - No new Clinical Chart items can be mapped to them.  

### 8.2 Transaction Modules (Procedure, OT, Billing, Canteen)

- **Procedure Entry (IPD)**:
  - Inherits Department/Category via Clinical Chart for each procedure line.  
- **Doctor Visit & OT Entry**:
  - Uses the mapped Department/HoGroup for visit and OT charge classification.  
- **Billing**:
  - Uses Department/HoGroup for:
    - Service grouping on invoices.  
    - Applying corporate and other discounts.  
    - Financial reporting by department and category.  
- **Canteen & Corporate**:
  - Canteen items are grouped under Canteen Department/SubDept.  
  - Corporate discount rules can key off Department/HoGroup combinations.  

---

## 9. Validation & Data Quality Rules

- **Hierarchy Consistency**  
  - No Sub‑Department without a valid Department.  
  - No Sub‑Sub‑Department without a valid Sub‑Department.  

- **Status Propagation (Informational)**  
  - If a Department is set to Inactive:
    - System may suggest or automatically inactivate its Sub‑Departments and Sub‑Sub‑Departments (depending on configuration).  

- **Uniqueness**  
  - Department Name must be unique.  
  - Sub‑Department Name should be unique within a Department.  
  - Sub‑Sub‑Department Name should be unique within a Sub‑Department (as configured).  

- **Usage Protection**  
  - Prevent destructive changes (delete / re-parent) when entities are already referenced by:
    - Clinical Chart items  
    - Billing/Invoice lines  
    - Historical reports  

---

## 10. Audit & Security

- **Audit Logging**
  - All create, update, status change, and delete/inactivate operations for:
    - Department  
    - Sub‑Department  
    - Sub‑Sub‑Department  
    - HGroup  
  - Audit record includes:
    - Who performed the change  
    - When it occurred  
    - What changed (before/after values)  

- **Role-Based Access**
  - Only **Admin / Configuration / Finance** roles can:
    - Create or modify Department and category structures.  
    - Change Department Type, Applicable Area, or Status.  
  - Clinical/operational users (Doctors, Nurses, OT staff) have **read-only** visibility where needed (e.g., filters, reports) and cannot alter master hierarchy.  

--- 

## 11. Reporting & Analytics

- Departments and categories serve as primary dimensions in:
  - Revenue by Department / SubDept / HGroup.  
  - Utilization of Diagnostics, OT, Canteen, etc.  
  - Corporate vs non‑corporate revenue breakdowns.  
  - Clinical performance dashboards (e.g., high-volume tests/procedures).  

- Any restructuring of the hierarchy must be:
  - Planned carefully.  
  - Fully audited.  
  - Communicated to reporting teams to adjust historical vs current mappings.  

