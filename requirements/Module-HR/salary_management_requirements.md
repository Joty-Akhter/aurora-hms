# HR Salary Management – Salary Structure, Salary Components, and Employee Salaries

## 📋 Overview

This document specifies detailed requirements for HR salary features: **salary structure**, **salary components**, and **employee salaries**. These form the foundation for payroll processing, compensation planning, and payslip generation.

### Scope
- **Salary structure**: Grades, bands, pay ranges, and their linkage to organization/position.
- **Salary components**: Configurable earnings and deductions (basic, HRA, allowances, tax, PF, loans, etc.) with types and calculation rules.
- **Employee salaries**: Assignment of structure and component values to employees, effective dates, revision history, and derivation of payslips.

### Key Objectives
- **Structured compensation**: Consistent, auditable salary structures and component definitions across the organization.
- **Flexible components**: Configurable earnings and deductions with calculation rules (fixed, percentage of basic, formula-based).
- **Accurate employee pay**: Clear assignment of structure and components per employee with effective dating and full history for payroll and reporting.

### Out of Scope (covered elsewhere)
- Payroll run execution, tax calculations, and statutory reporting → [payroll_benefits_administration.md](payroll_benefits_administration.md)
- Provident Fund rules and contributions → [payroll_benefits_administration.md](payroll_benefits_administration.md) (Retirement Benefits / Provident Fund)
- Incentive plans and sales incentives → [payroll_benefits_administration.md](payroll_benefits_administration.md) (Incentives Management)

---

## 📚 Table of Contents
1. [Salary Structure](#1-salary-structure)
2. [Salary Components](#2-salary-components)
3. [Employee Salaries](#3-employee-salaries)
4. [Integration with Payroll and Other Modules](#4-integration-with-payroll-and-other-modules)
5. [Reporting and Compliance](#5-reporting-and-compliance)
6. [Non-Functional and Technical](#6-non-functional-and-technical)

---

## 1. Salary Structure

Salary structure defines the organizational framework for compensation levels (grades/bands) and pay ranges. It is used for placement of employees, salary reviews, and compensation benchmarking.

### 1.1 Salary Structure Entity (Top-Level)

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| SS-01 | The system shall support a **salary structure** as a top-level entity with: **code** (unique per organization), **name**, **description**, and **organization identifier**. | Must |
| SS-02 | Each salary structure shall have **effective from** and **effective to** dates so that historical and future structures can be maintained; only one active structure with the same code may be effective for a given date within the same scope. | Must |
| SS-03 | Each structure shall have a **pay frequency** (e.g., Monthly, Bi-Weekly, Weekly). All amounts in that structure (grades/bands) are interpreted in that frequency. | Must |
| SS-04 | Each structure shall have a **default currency** (e.g., INR, USD). All grade/band amounts in that structure shall use this currency unless overridden at band level where supported. | Must |
| SS-05 | The system shall allow defining **multiple named structures** per organization (e.g., "India Monthly", "US Bi-Weekly") for different payroll or regional needs. | Should |
| SS-06 | The system shall support marking one structure as **default** per organization (or per org + pay frequency) for use when no structure is explicitly assigned. | Should |
| SS-07 | Structure **code** shall be unique within an organization and immutable after creation. **Name** and **description** may be updated. | Must |

### 1.2 Salary Grades

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| SS-08 | The system shall support **salary grades** (e.g., Grade A, B, C, or Level 1–10) as named levels belonging to a **salary structure**. | Must |
| SS-09 | Each grade shall have: **code** (unique within the structure), **name**, **display order** or **level number** (integer for sorting and comparison), and optional **description**. | Must |
| SS-10 | Grades within a structure shall be ordered by **display order**; the system shall use this order for lists, reports, and progression (e.g., next grade). | Must |
| SS-11 | The system shall support **effective from** and **effective to** dates at grade level so that grades can be added or retired within a structure without invalidating the whole structure. | Should |
| SS-12 | **Grade code** within a structure shall be unique and immutable after creation. | Must |

### 1.3 Salary Bands and Pay Ranges

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| SS-13 | The system shall support **salary bands** within a grade. Each band represents a pay range: **minimum amount**, **maximum amount**, and optionally **mid-point**. | Must |
| SS-14 | For each band the system shall store: **currency** (default from structure if not overridden), **minimum amount**, **maximum amount**, **mid-point** (optional; may be derived as (min+max)/2 if not entered). | Must |
| SS-15 | The system shall enforce **minimum ≤ mid-point ≤ maximum** (when mid is present) and **minimum &lt; maximum** for every band. | Must |
| SS-16 | A grade may have **one band** (single min–max range) or **multiple bands** (e.g., "Entry", "Mid", "Senior" or "Min", "Mid", "Max" with distinct ranges). Each band may have an optional **name** or **code**. | Must |
| SS-17 | When a grade has multiple bands, each band shall have a **display order** or **sequence** for consistent listing and for progression (e.g., move from Band 1 to Band 2 within same grade). | Should |
| SS-18 | Band amounts shall be **numeric**, with configurable **decimal precision** (e.g., 2 decimal places) and **rounding rule** (e.g., round to nearest integer) at structure or system level. | Should |
| SS-19 | The system shall support **effective from** and **effective to** dates at band level so that band ranges can be revised (e.g., annual increase) with full history. | Should |

### 1.4 Validation and Business Rules

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| SS-20 | The system shall **prevent overlapping** pay ranges between bands within the same grade (e.g., Band A max must not exceed Band B min if they are ordered). Overlap across grades may be allowed (e.g., high end of Grade 2 overlaps low end of Grade 3). | Should |
| SS-21 | When **versioning** or **revision** of a structure is supported, creating a new effective-dated version shall **copy** grades and bands from the previous version by default so that only changed values need to be updated. | Should |
| SS-22 | The system shall **warn** if a grade or band has **no employees** assigned (for cleanup) and shall **prevent deletion** of a grade or band that has active employee assignments. | Should |
| SS-23 | **Effective to** date of a structure (or grade/band) shall be **greater than or equal to effective from**; **effective to** may be null to indicate "open-ended". | Must |

### 1.5 Linkage to Organization and Position

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| SS-24 | A salary structure shall be **scoped by organization** (tenant/organization identifier). Grades and bands inherit this scope. | Must |
| SS-25 | The system may support **structure assignment** at organization level with optional overrides by **department** or **location** (e.g., "Structure A for Dept X", "Structure B for Location Y"). | Should |
| SS-26 | **Positions** or **job roles** may be linked to a **default salary grade** and optionally **default band** so that new hires or transfers inherit the correct placement. | Should |
| SS-27 | When position is linked to a grade/band, the system shall validate that the referenced grade/band belongs to a structure that is applicable to the organization (and department/location if used). | Should |

### 1.6 Structure Lifecycle and Audit

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| SS-28 | All changes to salary structure, grades, and bands (create, update, effective date change, deactivate) shall be **audit logged** with: **user**, **timestamp**, **entity** (structure/grade/band), **action** (create/update/delete), and **old/new values** (or reference to version). | Must |
| SS-29 | The system shall support **versioning** or **revision history** of structure definitions (by effective date) so that past payroll can be reproduced using the structure that was effective in that period. | Should |
| SS-30 | Structure and grade/band definitions shall be **read-only** for **effective periods** that have already been **payroll-processed** (closed). No back-dated edits that affect closed periods shall be allowed. | Must |
| SS-31 | **Deactivation** of a structure (effective to set to past date or status = inactive) shall not delete it; historical assignments and payroll data shall remain valid. | Must |

### 1.7 User Interface (UI) Requirements

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| SS-32 | HR shall be able to **create** a new salary structure via UI by entering code, name, pay frequency, currency, and effective from date. | Must |
| SS-33 | HR shall be able to **add, edit, and reorder** salary grades within a structure. Mandatory fields: code, name, display order. | Must |
| SS-34 | HR shall be able to **add, edit, and reorder** salary bands within a grade. Mandatory fields: minimum, maximum; optional: mid-point, band name/code, currency. | Must |
| SS-35 | The UI shall display **validation errors** (e.g., min ≥ max, overlapping bands, duplicate code) inline and prevent save until resolved. | Must |
| SS-36 | The UI shall provide a **structure summary** view listing all grades and bands in a single screen (e.g., table or matrix: Grade → Bands with min/mid/max). | Must |
| SS-37 | The UI shall support **effective date** selection to view historical or future structure versions. | Should |
| SS-38 | The system shall support **copy structure** (or "Create from existing") to clone a structure with a new code and effective from date, then allow editing. | Should |

### 1.8 API Capabilities

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| SS-39 | **Create structure**: POST with body (code, name, description, payFrequency, currency, effectiveFrom, organizationId). Response: full structure with id. | Must |
| SS-40 | **Update structure**: PATCH/PUT for name, description, effectiveTo; code and organizationId shall be immutable. | Must |
| SS-41 | **List structures**: GET by organizationId, optional effectiveDate, optional includeInactive. Response: list of structures with id, code, name, payFrequency, currency, effectiveFrom, effectiveTo. | Must |
| SS-42 | **Get structure by id**: GET returning structure with nested grades and bands (or separate endpoints for grades/bands by structureId). | Must |
| SS-43 | **Create/Update/List grades**: APIs for grade CRUD within a structure (structureId, code, name, displayOrder, effectiveFrom, effectiveTo). | Must |
| SS-44 | **Create/Update/List bands**: APIs for band CRUD within a grade (gradeId, min, max, mid, currency, name/code, displayOrder, effectiveFrom, effectiveTo). | Must |
| SS-45 | **List structures/grades by organization and effective date**: GET used by payroll and other modules to resolve which structure and grade apply for an employee. | Must |
| SS-46 | API responses shall use **consistent field names** (e.g., camelCase or snake_case per project standard) and **HTTP status codes** (200, 201, 400, 404, 409). | Must |

### 1.9 Reports and Export

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| SS-47 | **Structure summary report**: List salary structure(s) by organization and effective date, with all grades and bands (code, name, min, mid, max, currency). Export to PDF or Excel. | Should |
| SS-48 | **Grade-wise headcount** (optional in this section): Count of employees per grade/band for a given date; may be in Employee Salaries or Reporting section. | Could |
| SS-49 | **Bulk import** (optional): CSV/Excel import for creating or updating structures, grades, and bands with validation and error report. | Could |

---

## 2. Salary Components

Salary components are the building blocks of pay: **earnings** (basic, HRA, allowances, etc.) and **deductions** (tax, PF, loans, etc.). They are configurable so that different organizations or payrolls can define their own component set and rules.

### 2.1 Component Master Data (Entity and Attributes)

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| SC-01 | The system shall maintain a **salary component master** with: **code** (unique per organization), **name**, **type** (Earning or Deduction), **description**, and **active** flag. | Must |
| SC-02 | Each component shall have a **category** (sub-type). **Earnings** categories: Basic, HRA, Special Allowance, Conveyance, Medical, Leave Travel, Other Allowance. **Deductions** categories: Statutory Deduction (PF, ESI, Tax), Voluntary Deduction, Loan Repayment, Recovery, Other Deduction. | Must |
| SC-03 | Components shall be **organization-scoped** (organization identifier); each organization has its own component set. | Must |
| SC-04 | Each component shall have **effective from** and **effective to** dates; **effective to** may be null for open-ended. Only components effective on a given date shall be available for assignment and payroll. | Must |
| SC-05 | **Display order** or **sequence** (integer) shall be configurable per component for consistent ordering on payslips and reports (earnings first, then deductions, or as defined). | Must |
| SC-06 | **Component code** shall be unique within an organization and **immutable** after creation. **Name** and **description** may be updated subject to lifecycle rules. | Must |
| SC-07 | The system shall support an optional **short name** or **payslip label** for display on payslips (e.g., "Basic Sal", "HRA"). If not provided, **name** is used. | Should |
| SC-08 | Each component shall have a **currency** (default from organization or structure when used in payroll). Multi-currency organizations may allow currency per component. | Should |

### 2.2 Component Type and Calculation Basis

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| SC-09 | Each component shall have a **calculation basis**: **Fixed**, **PercentageOfBasic**, **PercentageOfGross**, **Formula**, **Statutory**, **Manual**. | Must |
| SC-10 | **Fixed**: The system shall store a **default amount** (numeric). When assigned to an employee, this may be overridden by employee-specific amount. | Must |
| SC-11 | **PercentageOfBasic** (or **PercentageOfGross**): The system shall store **percentage** (0–100 or higher if needed) and **base component code** (e.g., BASIC). Base component must be an earning component in the same organization. | Must |
| SC-12 | **Formula**: The system shall store a **formula expression** (e.g., `BASIC * 0.4` or `BASIC + HRA * 0.1`). Allowed operands: component codes, constants, operators (+, -, *, /). Formula shall be validated for syntax and for reference only to existing component codes. | Must |
| SC-13 | **Statutory**: The component amount is computed by a statutory engine (PF, ESI, tax). The system shall store a **statutory type** or **reference** (e.g., PF_EMPLOYEE, INCOME_TAX) so payroll can invoke the correct calculator. | Must |
| SC-14 | **Manual**: No automatic calculation; amount shall be entered manually per pay period (e.g., one-time bonus, ad-hoc deduction). The system may allow a **default amount** that can be overridden. | Must |
| SC-15 | When **percentage** or **formula** is used, the system shall **validate** that the base component(s) exist and are earnings (for percentage-of-basic/gross). **Circular reference** in formulas shall be detected and rejected. | Must |
| SC-16 | **Ceiling** (max amount) and **floor** (min amount) may be defined per component. Calculated or entered amount shall be constrained within floor–ceiling when both are set. | Should |
| SC-17 | **Rounding rule** per component (e.g., round to nearest integer, round up, two decimals) shall be configurable for calculated amounts. | Should |
| SC-18 | **Conditional applicability**: The system may support rules such as "applicable only if employee type = Permanent" or "only for grade in [G1, G2]" or "only for location = X". When not met, the component is excluded from that employee's payroll. | Should |
| SC-19 | At least one **earning** component with category **Basic** shall exist per organization (for percentage-based and formula components that reference Basic). | Must |

### 2.3 Statutory and Compliance

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| SC-20 | Components used for **statutory** purposes shall have a **statutory tag**: e.g., **PF_WAGE** (included in PF wage base), **PF_EMPLOYEE** (employee PF contribution), **PF_EMPLOYER** (employer PF contribution), **TAXABLE** (included in gross for tax), **TAX_EXEMPT** (exempt from income tax), **ESI_WAGE**. | Must |
| SC-21 | **Taxability** shall be defined per component: **Taxable**, **Exempt**, or **PartiallyTaxable** (with optional rule or cap). Tax calculation module shall use this for income tax computation. | Must |
| SC-22 | **PF wage**: The system shall allow marking one or more **earning** components as **part of PF wage** (e.g., Basic + DA). PF contribution shall be calculated on the sum of those components only (subject to statutory ceiling). | Must |
| SC-23 | Only **one** component per statutory type (e.g., one PF_EMPLOYEE, one PF_EMPLOYER) shall be recommended per organization to avoid double application; the system may **warn** if multiple components share the same statutory tag. | Should |
| SC-24 | **Statutory component** (calculation basis = Statutory) shall reference the correct **statutory type** and shall not allow manual override of amount for normal payroll (adjustments may be via separate mechanism). | Should |

### 2.4 Validation and Business Rules

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| SC-25 | **Code** format: alphanumeric and optionally underscore; length limit (e.g., 20 chars). Invalid characters shall be rejected. | Must |
| SC-26 | **Percentage** shall be non-negative; upper limit may be configured (e.g., 100 for HRA, or higher for special cases). | Must |
| SC-27 | **Ceiling** and **floor**: when both present, ceiling shall be **greater than or equal to** floor. | Must |
| SC-28 | **Formula** shall not reference a component that itself depends (directly or indirectly) on the current component; the system shall **reject** save if circular dependency is detected. | Must |
| SC-29 | **Deactivation** (effective to set or active = false) shall not be allowed if the component is **the only Basic** earning and other components depend on it; either define another Basic first or block deactivation with a clear message. | Should |

### 2.5 Component Lifecycle and Audit

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| SC-30 | All changes to salary component master (create, update, effective date change, deactivate) shall be **audit logged** with: **user**, **timestamp**, **component code**, **action**, **old and new values** (or snapshot reference). | Must |
| SC-31 | **Deactivation** of a component shall not delete it; set **effective to** date or **active = false** so that historical payslips and payroll runs remain valid. | Must |
| SC-32 | The system shall **prevent deletion** (physical delete) of a component that is referenced in any **employee salary** record or in **past payroll** results. Soft delete (deactivate) only. | Must |
| SC-33 | **Prevent edit** of component **code** and **type** (Earning/Deduction) after creation, to avoid breaking existing employee assignments and formulas. Other attributes may be edited with audit. | Must |
| SC-34 | When a component is **deactivated**, the system shall **warn** if it is still assigned to active employees; user shall confirm. Future payroll shall exclude it for new periods unless re-activated. | Should |

### 2.6 User Interface (UI) Requirements

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| SC-35 | HR shall be able to **create** a salary component via UI: code, name, type (Earning/Deduction), category, description, calculation basis, and basis-specific fields (amount, percentage, base component, formula, statutory type). | Must |
| SC-36 | **Mandatory fields** shall be clearly marked; **validation errors** (duplicate code, invalid formula, circular ref, ceiling &lt; floor) shall be shown inline and block save until resolved. | Must |
| SC-37 | HR shall be able to **list** components with filters: organization, type (Earning/Deduction), category, active only / include inactive, effective date. Columns: code, name, type, category, calculation basis, effective from/to, display order. | Must |
| SC-38 | HR shall be able to **edit** component (name, description, display order, default amount, percentage, formula, ceiling, floor, statutory tags, taxability, effective to) subject to lifecycle rules. | Must |
| SC-39 | **Reorder** components (change display order) via drag-and-drop or up/down buttons; order shall apply to payslip and reports. | Should |
| SC-40 | **Formula** field shall support syntax help or a simple expression builder; invalid formula shall be highlighted with error message. | Should |
| SC-41 | The UI shall show **usage** (e.g., "Used by N employees", "Referenced in M formulas") so that HR understands impact before deactivating. | Should |

### 2.7 API Capabilities

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| SC-42 | **Create component**: POST with body (code, name, type, category, description, calculationBasis, defaultAmount | percentage, baseComponentCode | formulaExpression | statutoryType, ceiling, floor, displayOrder, effectiveFrom, organizationId, statutoryTags, taxability). Response: full component with id. | Must |
| SC-43 | **Update component**: PATCH/PUT for allowed attributes; code, type, organizationId shall be immutable. Return 400 if validation fails, 409 if conflict (e.g., already used in closed payroll). | Must |
| SC-44 | **Get component by id or code**: GET returning full component including calculation parameters and statutory tags. | Must |
| SC-45 | **List components**: GET by organizationId, optional type (Earning/Deduction), category, effectiveDate, includeInactive. Response: list with id, code, name, type, category, calculationBasis, displayOrder, effectiveFrom, effectiveTo. | Must |
| SC-46 | **List components for payroll**: GET by organizationId and effectiveDate, returning only components effective on that date, ordered by displayOrder, for use by payroll and employee salary screens. | Must |
| SC-47 | API responses shall use **consistent field names** and **HTTP status codes** (200, 201, 400, 404, 409). **Conflict** (409) when component is in use and change not allowed. | Must |

### 2.8 Reports and Export

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| SC-48 | **Component master report**: List all salary components by organization with code, name, type, category, calculation basis, base/percentage/formula, statutory tags, taxability, effective from/to, display order. Filter by type and category. Export to PDF or Excel. | Should |
| SC-49 | **Component dependency report** (optional): List components that reference other components (e.g., "HRA references Basic") for impact analysis. | Could |
| SC-50 | **Bulk import** (optional): CSV/Excel import for creating/updating components with validation and error report. | Could |

---

## 3. Employee Salaries

Employee salaries define **what each employee is paid**: which structure (or grade/band) they belong to and what **amount or rule** applies for each salary component. Effective dating and history are required for correct payroll and audits.

### 3.1 Assignment of Structure and Grade (Entity and Attributes)

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| ES-01 | Each employee shall have an **employee salary assignment** record with: **employee identifier**, **salary structure identifier** (or structure code), **salary grade identifier** (or grade code), optional **salary band identifier**, **effective from** date, **effective to** date (null = open-ended). | Must |
| ES-02 | Only **one** active structure/grade assignment shall apply per employee at any given date (effective from ≤ date &lt; effective to or effective to is null). | Must |
| ES-03 | The system shall store **effective from** and **effective to** for each assignment so that full history is maintained; overlapping periods for the same employee shall be **rejected**. | Must |
| ES-04 | Assignment may record **source**: **Position** (inherited from position’s default grade/band) or **Override** (explicitly set at employee level). When position changes, HR may choose to inherit new grade or keep override. | Should |
| ES-05 | **Structure** and **grade** (and band if used) shall belong to the same organization as the employee. The system shall validate reference integrity. | Must |
| ES-06 | When **position** is linked to a default grade/band, the system may **suggest** or **auto-fill** structure and grade for new employee salary assignment; user may override. | Should |

### 3.2 Employee Salary Component Values (Per-Component Overrides)

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| ES-07 | For each employee, the system shall store **employee salary component** records: **employee id**, **component code**, **value type** (Amount, Percentage, UseMasterDefault), **amount** (when value type = Amount), **percentage** (when value type = Percentage), **effective from**, **effective to**. | Must |
| ES-08 | **Value type = UseMasterDefault**: Payroll shall use the component master’s default amount, percentage, or formula. No override stored. | Must |
| ES-09 | **Value type = Amount**: Fixed amount per pay period for this employee; payroll uses this instead of master default or formula. | Must |
| ES-10 | **Value type = Percentage**: Override the percentage (e.g., HRA 40% of Basic); base component remains from master. Percentage shall be validated against component ceiling/floor if defined. | Must |
| ES-11 | Employee salary component records shall have **effective from** and **effective to**; only one active value per employee per component at any date. **Duplicate effective periods** for same employee + component shall be rejected. | Must |
| ES-12 | The system shall support **adding** a component to an employee (new row with component code and value) and **removing** (set effective to or deactivate) with effective dating. | Must |
| ES-13 | **Default component set**: When assigning a structure/grade to an employee, the system may **copy** default components from a **grade template** or **structure template** (e.g., all components linked to that grade), so that HR only overrides where needed. | Should |
| ES-14 | Components not in the employee’s component set shall **not** appear in that employee’s payroll for that period (unless added by payroll run, e.g., one-time bonus). Statutory components (e.g., PF) may be **auto-included** based on eligibility. | Must |
| ES-15 | **Basic** component (or equivalent) shall be **required** for every employee with salary assignment; at least one earning component with amount or default must be present. | Must |

### 3.3 Revisions and History

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| ES-16 | **Salary revision** shall be done by creating **new** effective-dated row(s) for structure/grade assignment and/or component values; **previous** rows remain for history. No in-place edit of past rows. | Must |
| ES-17 | The system shall support **revision reason** or **revision type**: e.g., Annual Increment, Promotion, Grade Change, Allowance Revision, Correction. Stored with the new row or in a separate revision log. | Must |
| ES-18 | **Revision history** shall be queryable: for an employee, list of changes with effective date, previous/new structure, grade, component values, reason, and user. | Must |
| ES-19 | **No back-dated changes** that affect **closed payroll periods**. The system shall enforce: effective from of new assignment/component ≥ **payroll lock date** (or first day of next open period). Edits to past effective-dated rows that fall in closed period shall be **rejected**. | Must |
| ES-20 | **Bulk revision**: Apply a change (e.g., X% increment to Basic, or same fixed amount to an allowance) to **multiple employees** (e.g., by grade, department, or selection). One effective date for all; optional **approval workflow** before commit. | Should |
| ES-21 | **Approval workflow** (optional): Salary revision (single or bulk) may require approval by designated role; after approval, effective-dated rows are created. Rejected revisions shall not create rows. | Could |

### 3.4 Gross, Deductions, and Net (Payslip Derivation)

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| ES-22 | The system shall **calculate** per-component amount for each earning and deduction using: employee component value (amount/percentage/use master), component master rules (formula, statutory), and dependency order (e.g., Basic first, then HRA from Basic). | Must |
| ES-23 | **Total earnings** = sum of all earning component amounts for the period. **Total deductions** = sum of all deduction component amounts. **Gross pay** may equal total earnings or be defined as total earnings minus certain exempt deductions (organization policy). | Must |
| ES-24 | **Net pay** = Total earnings − Total deductions. Rounding shall be applied per component (if configured) and to net pay (e.g., round to nearest currency unit). | Must |
| ES-25 | Calculated values (per component and totals) shall be **stored** in **payroll result** (or payslip) for each payroll run and period, so that payslips and reports are **consistent** and auditable. | Must |
| ES-26 | **Payslip** shall list: employee, period, all earning components with amount, total earnings, all deduction components with amount, total deductions, net pay. Order by component display order. Currency and pay frequency shall be shown. | Must |
| ES-27 | **Recalculation**: For an open period, payroll may **recalculate** from current employee salary and component master; stored result updated. For closed period, recalculation shall be **blocked** or restricted to specific correction process. | Must |

### 3.5 Proration and Pro-rata

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| ES-28 | **Proration** for **mid-period join** or **mid-period leave**: Salary for partial month shall be calculable (e.g., gross × (days worked / days in month)). Configurable at organization or component level. | Should |
| ES-29 | **Proration rule** options: **By days** (prorate by working days in period), **No proration** (full month amount if employed any day in period), **By hours** (if time data available). Default per component (e.g., Basic prorated, fixed allowance not). | Should |
| ES-30 | **Join date** and **relieving date** (or period end) shall be used to determine **days worked** for proration. System shall use employee master join/relieving date or period-specific override. | Should |

### 3.6 Eligibility and Validation

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| ES-31 | Salary assignment shall be allowed only for employees who are **active** (e.g., join date ≤ effective date and no relieving date, or relieving date &gt; effective date). Assignment for inactive employees shall be **rejected** or **warned**. | Must |
| ES-32 | **Component value** (amount or percentage) shall be validated against component master **ceiling** and **floor** when defined; out-of-range shall be rejected or warned. | Should |
| ES-33 | **Duplicate effective periods**: For same employee, (a) only one structure/grade assignment per date range, (b) only one component value per component per date range. Overlaps shall be **rejected**. | Must |
| ES-34 | **Component code** in employee salary component shall reference an **existing, active** component in the same organization effective on the assignment’s effective date. | Must |
| ES-35 | When **structure or grade** is changed for an employee, the system may **warn** if new grade’s default component set differs from current (e.g., suggest adding/removing components). | Could |

### 3.7 Lifecycle and Audit

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| ES-36 | All changes to employee salary (assignment, component values, revision) shall be **audit logged**: user, timestamp, employee, action (create/update/effective end), entity (assignment/component), old/new values or reference. | Must |
| ES-37 | **Deactivation** of an employee (relieving) shall not delete salary history; past assignment and component rows remain. Future payroll shall exclude the employee from the period after relieving. | Must |

### 3.8 User Interface (UI) Requirements

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| ES-38 | HR shall be able to **view** employee salary: current structure, grade, band, and list of components with values (amount/percentage/use default), effective dates, and revision history. | Must |
| ES-39 | HR shall be able to **create** salary assignment for an employee: select structure, grade, optional band, effective from; optional copy from grade template; then add/edit component values. | Must |
| ES-40 | HR shall be able to **edit** assignment: change effective to (end current), add new row for new structure/grade/component with new effective from; or add new component row with effective from. | Must |
| ES-41 | **Mandatory fields** and **validation**: structure, grade, effective from; at least one earning (e.g., Basic) with value. Errors shown inline; save blocked until valid. | Must |
| ES-42 | **Effective date** picker and **as-of date** view: view employee salary as of a past or future date (for audit and planning). | Should |
| ES-43 | **Bulk revision** screen: select employees (by grade, department, or list), choose revision type (e.g., % increment on Basic), enter percentage/amount and effective from, optional approval submit. | Should |
| ES-44 | **Payslip** view: select employee and pay period; display payslip with all components and totals. Export PDF. | Must |
| ES-45 | **List employees by salary**: Filter employees by structure, grade, band, or component (e.g., "all with Transport Allowance"); useful for payroll batch and reports. | Should |

### 3.9 API Capabilities

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| ES-46 | **Get employee salary**: GET by employeeId, optional asOfDate. Response: structure, grade, band, list of components with value type, amount/percentage, effective from/to. | Must |
| ES-47 | **Create/update assignment**: POST or PUT employee salary assignment (employeeId, structureId, gradeId, bandId?, effectiveFrom, effectiveTo?, source?). Validate and return 400 if invalid, 409 if overlap. | Must |
| ES-48 | **Create/update component value**: POST or PUT employee salary component (employeeId, componentCode, valueType, amount?, percentage?, effectiveFrom, effectiveTo?). Validate and return 400/409. | Must |
| ES-49 | **List employees by salary**: GET by organizationId, optional structureId, gradeId, asOfDate. Response: list of employee ids or summary for payroll batch. | Must |
| ES-50 | **Get revision history**: GET by employeeId, optional date range. Response: list of revisions with effective date, previous/new values, reason, user. | Should |
| ES-51 | **Calculate payslip** (preview): POST employeeId, period start/end; response: component-wise amounts, gross, deductions, net (without storing). For open period only. | Should |
| ES-52 | API responses shall use **consistent field names** and **HTTP status codes** (200, 201, 400, 404, 409). 409 when effective period overlaps or payroll period closed. | Must |

### 3.10 Employee Self-Service

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| ES-53 | **Employee self-service** may allow an employee to **view** their own current salary: structure name, grade name, and list of component names. **Amounts** may be **masked** (e.g., "***") or shown per organization policy. | Should |
| ES-54 | Employee may **view** and **download** their **payslips** for past periods (read-only). Access controlled by role and data scope. | Should |

### 3.11 Reports and Export

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| ES-55 | **Employee salary report**: List employees with structure, grade, band, component-wise amount (current or as-of date). Filter by org, department, structure, grade. Export Excel/PDF. | Must |
| ES-56 | **Salary revision history report**: List revisions (employee, effective date, previous/new structure, grade, component changes, reason, user). Filter by employee, date range. | Should |
| ES-57 | **Grade-wise headcount and cost**: Count of employees and total cost (e.g., sum of gross or sum of Basic) by grade and optionally by structure. As-of date. | Should |
| ES-58 | **Component-wise cost report**: Total amount per component across employees (e.g., total Basic, total HRA) for a given period or as-of snapshot. For budgeting and GL posting. | Should |
| ES-59 | **Bulk export** (optional): Export employee salary data (assignment + components) for selected employees or grade for migration or external payroll. | Could |

---

## 4. Integration with Payroll and Other Modules

This section specifies how the salary management features (structure, components, employee salaries) integrate with payroll processing and other HR or enterprise modules. Data flows, triggers, and contracts are defined at a level sufficient for implementation.

### 4.1 Payroll Processing Integration

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| INT-01 | **Payroll processing** shall consume **employee salary** (structure, grade, band, component values) and **salary component master** (calculation basis, formula, statutory type) to compute per-component amounts, total earnings, total deductions, and net pay for each pay period. | Must |
| INT-02 | Payroll shall resolve **effective** structure, grade, and component values using the **pay period end date** (or period start) as the "as-of" date; only assignment and component rows with effective from ≤ period date and (effective to is null or effective to ≥ period date) shall be used. | Must |
| INT-03 | Payroll shall **fetch** employee list (by organization, active as of period), then for each employee fetch assignment and component values from salary management; **batch API** (e.g., list employee salaries by org + period date) shall be supported for performance. | Must |
| INT-04 | **Payroll period lock**: Once a pay period is **closed** or **finalized**, salary management shall **reject** creation or update of employee salary (assignment or component) with effective date falling within that period. Lock status shall be queryable (e.g., API or shared reference). | Must |
| INT-05 | **Recalculation**: For an **open** period, payroll may trigger **recalculation**; salary management shall not change. Payroll stores results; if employee salary is updated, payroll may re-run for affected employees and update stored results. | Must |
| INT-06 | **Payslip storage**: Payroll (or salary module) shall **store** payslip/payroll result with component-wise amounts and totals per employee per period; salary management remains source of **inputs**, not the stored payslip amounts. | Must |
| INT-07 | **Error handling**: If an employee has no salary assignment or missing required component (e.g., Basic) for the period, payroll shall **flag** the employee (e.g., validation error) and exclude or hold until corrected; no silent default. | Must |
| INT-08 | **Currency and pay frequency**: Payroll shall use structure’s **pay frequency** and **currency** from the assigned structure; multi-currency payroll may convert using defined exchange rate for the period. | Should |

### 4.2 Provident Fund (PF) Integration

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| INT-09 | **PF wage base**: Provident Fund module shall receive from salary/payroll the **PF wage** for each employee for the period = sum of amounts of earning components tagged **PF_WAGE** (e.g., Basic + DA), subject to statutory ceiling if applicable. | Must |
| INT-10 | **PF contribution components**: Salary component master shall define components with statutory tag **PF_EMPLOYEE** and **PF_EMPLOYER**. Payroll (or PF module) shall compute contribution using PF wage and statutory rules; amount shall be **written back** as deduction (PF_EMPLOYEE) and expense/liability (PF_EMPLOYER) in payroll result. | Must |
| INT-11 | **Data flow**: Option A—Payroll computes PF using PF wage from salary components and writes PF_EMPLOYEE/PF_EMPLOYER amounts into payslip. Option B—PF module receives PF wage per employee per period and returns contribution amounts; payroll posts them. Contract (API or event) shall be defined. | Must |
| INT-12 | **PF eligibility**: PF module may determine eligibility (e.g., by wage threshold, employee type). Salary management shall provide **gross/PF wage**; eligibility logic may reside in PF or payroll. | Should |
| INT-13 | **PF statements and compliance**: PF module shall use **employee salary identity** (employee id, organization) and **period** to link contribution to PF account; salary management does not store PF account balance. | Should |

### 4.3 Tax Calculation Integration

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| INT-14 | **Taxable gross**: Tax calculation shall receive **gross taxable earnings** = sum of earning components marked **Taxable** (and optionally part of **PartiallyTaxable** per rule). Components marked **Tax Exempt** shall be excluded. | Must |
| INT-15 | **Component-level taxability**: Salary component master **taxability** (Taxable, Exempt, PartiallyTaxable) shall be passed to tax engine or payroll so that correct gross is used for income tax and withholding. | Must |
| INT-16 | **Tax deduction component**: One or more deduction components (e.g., INCOME_TAX) with statutory tag shall hold the **calculated tax** amount per period; tax module (or payroll) computes and writes amount, salary management stores component definition only. | Must |
| INT-17 | **Data flow**: Payroll computes taxable gross from salary components, calls tax service (or internal tax logic) with taxable gross and other inputs (e.g., regime, deductions); tax amount is stored in payroll result and as deduction component amount in payslip. | Must |
| INT-18 | **Statutory reporting**: Tax reporting (e.g., TDS, annual return) shall use **employee salary identity** and **period-wise component amounts** from payroll result; salary management is not the system of record for tax filings. | Should |

### 4.4 Accounting (GL) Integration

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| INT-19 | **Accounting** shall receive **payroll results** (by component or summary) for journal posting: e.g., Dr Salary Expense (by component or by account mapping), Cr Salary Payable; Dr Deductions (e.g., PF, Tax) to liability accounts. | Must |
| INT-20 | **Account mapping**: Organization shall define **GL account** per salary component (or per component category) for expense and liability; payroll or accounting module shall use this mapping to generate journal lines. | Should |
| INT-21 | **Payroll journal**: Posting may be **summary** (one line per expense account, one per liability) or **detail** (per component); audit trail shall support reconciliation between payslip and GL. | Should |
| INT-22 | **Period and cut-off**: Accounting period close shall align with payroll period lock; posting for a pay period shall use **final** payroll result (no posting for open period until finalized, or post with hold/reversal). | Should |
| INT-23 | **API or event**: Payroll (or HR) shall expose **payroll result for accounting** (e.g., API or event payload) with organization, period, employee, component code, amount, account code; accounting module consumes and posts. | Should |

### 4.5 Incentives Integration

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| INT-24 | **Incentive as earning**: Incentive amount (e.g., sales incentive, performance bonus) may be **added as an earning component** in the payslip for the period. Component may be **manual** (amount entered per period) or **fed from incentive module**. | Must |
| INT-25 | **Feed from incentive module**: Incentive module shall provide **amount per employee per period** (e.g., via API or batch file). Payroll shall **merge** this amount into payslip under the designated incentive component(s); component code and period shall match. | Should |
| INT-26 | **Tax and PF**: Incentive component shall have **taxability** and **PF wage** tagging so that tax and PF are calculated correctly (e.g., incentive taxable, may or may not be part of PF wage per policy). | Must |
| INT-27 | **Timing**: Incentive amount for a period shall be **finalized** before or during payroll run; if incentive is updated after payroll run, organization may define process (e.g., adjustment in next period, or payroll re-run for open period). | Should |

### 4.6 Position / Job Role Integration

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| INT-28 | **Position default grade**: Position (or job role) shall store optional **default salary structure** and **default salary grade** (and band). When creating employee salary assignment, HR (or onboarding) may **inherit** structure and grade from position. | Should |
| INT-29 | **Validation**: Assigned structure and grade in employee salary shall belong to the same organization as the employee; if position has default grade, the grade’s structure shall be valid for that organization. | Must |
| INT-30 | **Position change**: When employee’s **position** changes, salary management may **suggest** new grade (from new position) or **retain** current assignment; no automatic overwrite of employee salary unless business process explicitly updates it. | Should |
| INT-31 | **API**: Position module (or HR core) shall expose **default structure/grade by position**; salary management (or payroll) may call this for new-hire setup or reporting. | Could |

### 4.7 Employee Master Data Integration

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| INT-32 | **Join and relieving date**: Salary management and payroll shall use **join date** and **relieving date** from **employee master** (or HR core) to determine eligibility and proration. No duplicate storage of join/relieving in salary module; single source of truth. | Must |
| INT-33 | **Active flag**: Only employees **active** as of the pay period (join date ≤ period end, relieving date null or > period end) shall be included in payroll; salary assignment for inactive employees shall be rejected or warned. | Must |
| INT-34 | **Employee attributes**: Grade/structure assignment may depend on **department**, **location**, **employee type** (e.g., Permanent, Contract); these attributes shall be read from employee master (or org structure) for validation and filtering. | Should |

### 4.8 Loans and Recoveries Integration

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| INT-35 | **Loan recovery component**: Deduction components with category **Loan Repayment** or **Recovery** may have amount **fed from loan module** (e.g., EMI or custom recovery amount per period). Payroll shall merge this into payslip. | Should |
| INT-36 | **Loan module contract**: Loan module shall provide **recovery amount per employee per period** (and optionally loan id); payroll or salary module shall not compute loan EMI—only post the amount. | Should |
| INT-37 | **Stop recovery**: When loan is fully recovered or stopped, loan module shall return zero (or stop feed); payroll shall not deduct beyond that period. | Should |

### 4.9 Time and Attendance (Optional)

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| INT-38 | **Unpaid leave / LOP**: If time/attendance module provides **days of unpaid leave** or **loss of pay** days per employee per period, payroll may **reduce** salary (e.g., prorate or deduct LOP amount); salary management provides base amounts, payroll applies attendance factor. | Could |
| INT-39 | **Overtime**: Overtime pay may be a **separate earning component** (amount or rate × hours from time module); time module shall provide **approved overtime hours** per employee per period; payroll multiplies by rate and posts. | Could |

### 4.10 Integration Patterns and Non-Functional

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| INT-40 | **APIs**: Integration between salary management, payroll, PF, tax, and accounting may be **synchronous API** (e.g., REST) or **asynchronous** (e.g., event/message); payload format and error handling shall be documented. | Must |
| INT-41 | **Idempotency**: Payroll result post and accounting post shall support **idempotent** calls (e.g., same request id or period + run id) to avoid duplicate posting on retry. | Should |
| INT-42 | **Traceability**: Payroll result and journal shall reference **source** (e.g., payroll run id, period); salary structure/component/employee salary version (effective dates) shall be traceable for audit. | Should |
| INT-43 | **Failure handling**: If PF or tax service is unavailable during payroll run, payroll shall **fail** or **hold** for affected employees and retry or manual intervention; no partial inconsistent post. | Should |

---

## 5. Reporting and Compliance

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| RPT-01 | **Salary structure report**: list grades/bands with min–max and effective dates. | Should |
| RPT-02 | **Salary component master report**: list components by type (earning/deduction), category, and calculation basis. | Should |
| RPT-03 | **Employee salary report**: list employees with assigned structure, grade, and component-wise amount (current or as-of date). | Must |
| RPT-04 | **Salary revision history report**: effective date, old/new values, reason. | Should |
| RPT-05 | **Grade-wise headcount and cost**: number of employees and total cost by grade/structure. | Should |
| RPT-06 | **Audit trail**: changes to structure, components, and employee salary with user and timestamp. | Must |

---

## 6. Non-Functional and Technical

| Requirement ID | Requirement | Priority |
|----------------|-------------|----------|
| NF-01 | **Multi-tenant / multi-organization**: salary structure, components, and employee salaries shall be isolated by organization (or tenant). | Must |
| NF-02 | **Currency**: structure and component amounts shall support a defined currency per organization or per structure. | Must |
| NF-03 | **Performance**: retrieval of employee salary (with components) for a batch of employees (e.g., 5,000) for payroll shall complete within acceptable time (e.g., &lt; 30 seconds). | Should |
| NF-04 | **Security**: only authorized roles (e.g., HR, Payroll Admin) shall create/update salary structure, components, and employee salary; read access may be restricted by role and data scope. | Must |
| NF-05 | **Data integrity**: effective-date overlaps and duplicate component assignment for same period shall be prevented by validation or database constraints. | Must |

---

## Summary Table: Coverage

| Feature area | Key entities | Main capabilities |
|-------------|--------------|-------------------|
| **Salary structure** | Grades, bands, pay ranges | Create/update with effective dates; link to org/position; versioning and audit; no edit of closed periods. |
| **Salary components** | Earnings, deductions | Master with code, type, category, calculation basis (fixed, %, formula, statutory); taxability and PF wage tagging; org-scoped; effective dating; no delete if used. |
| **Employee salaries** | Assignment, component values | Assign structure/grade per employee with effective dates; per-component values/overrides with effective dates; revision history; gross/deductions/net for payslip; APIs and reports. |

This document should be used alongside [payroll_benefits_administration.md](payroll_benefits_administration.md) for end-to-end payroll and compensation requirements.
