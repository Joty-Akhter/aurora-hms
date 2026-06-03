## Hospital Canteen Module – Implementation Plan

This document translates the `canteen-module-srs.md` requirements into an implementation-oriented view. It defines the main canteen submodules, their responsibilities, and a phased delivery plan, including dependencies on other hospital modules.

---

## 1. Scope

- **In scope (within Canteen Module)**:
  - Canteen-specific masters (products, suppliers, meal categories/types, table/waiter setup, etc.).
  - Canteen inventory and raw material management.
  - Meal/package configuration and canteen-side pricing rules.
  - Patient/staff/department meal distribution flows.
  - Table service and POS for indoor and outdoor canteen sales.
  - Canteen-side reporting, controls, RBAC and audit.
- **Out of scope (owned by other modules, integrated via APIs)**:
  - Core Patient/IPD/OPD and Visit management.
  - HR/Staff master data and employment lifecycle.
  - Global Clinical Chart and Department/Category masters.
  - Central Billing, Discount Management, Corporate Service & Card Management.
  - Global Inventory/Purchase, Accounts/Finance, and Security/User Management.

---

## 2. Canteen Submodules (Logical Components)

### 2.1 Canteen Master & Configuration

**Responsibilities**
- Product master:
  - Finished items, raw materials, packaging.
  - Attributes such as category, unit, base selling price, cost price (optional), tax flags, dietary tags, status.
- Supplier master:
  - Supplier identities, contacts, payment terms, activation status.
- Food categories & dietary setup:
  - Configurable food categories and dietary tags (Veg, Non-Veg, Diabetic, etc.).
- Auto stock alert configuration:
  - Minimum stock, reorder levels, expiry alert days per product.
- Meal- and role-based configuration used by other submodules:
  - Meal Category and Meal Type masters.
  - Indoor Category setup (staff/role categories for pricing).

**Key Interfaces**
- Integrates with:
  - Clinical Chart and Department/Category masters for cross-module reporting alignment (read-only references).
  - Inventory/Accounting for item classification and GL mapping (where applicable).

### 2.2 Canteen Inventory & Raw Material Management

**Responsibilities**
- Raw material master and recipe/BOM mapping for finished items.
- Stock-in:
  - Direct purchase, GRN from PO, stock adjustment in.
  - Batch and expiry capture and tracking.
- Raw material receipt & verification with statuses (Pending, Verified, Rejected).
- Raw material issue:
  - Issue to kitchen/production counters and departments.
  - Session-based issues (Breakfast/Lunch/Dinner) tied to dates.
- Stock adjustment & waste:
  - Positive/negative adjustments with mandatory reason codes.
  - Waste/spoilage capture with waste invoice/clearance grouping.
- Inventory controls:
  - Stock and expiry validation before sale or issue.
  - Authorized override flows for exceptions.

**Key Interfaces**
- Integrates with:
  - Central Inventory/Purchase (optional) for PO/GRN ingestion and stock reconciliation.
  - Accounting for valuation of waste and adjustments (via Billing/Finance integration rules).
- Provides:
  - Stock APIs to POS, Meal Distribution, and Reporting submodules.

### 2.3 Meal & Package Management

**Responsibilities**
- Department food entitlement setup:
  - Per-department meal entitlements, meal timings, default meal types, and billing types.
- Meal package setup:
  - Packages by category (Patient/Staff/VIP/Department/Visitor) with included items, dietary profile, prices, validity.
- Meal Category and Meal Type master:
  - Time windows, menu labels, price templates, days-of-week applicability and branch context.
- Optional dietary and nutrition configuration:
  - Diet profiles, nutrition metrics (calories, macros) for advanced features.

**Key Interfaces**
- Integrates with:
  - Patient/IPD/OPD module for ward/department mapping.
  - Discount Management and Corporate Service & Card Management indirectly, via Billing, for package-level discounts.
- Provides:
  - Package catalog and entitlement configuration for Patient/Staff Meal Distribution and POS.

### 2.4 Patient & Staff Meal Distribution

**Responsibilities**
- Patient meal distribution:
  - Patient search (IPD/OPD), ward/bed details.
  - Import/display of diet orders from clinical modules (where available).
  - One-meal-per-patient-per-time-slot enforcement with override workflow.
  - Record of meal date/time, package/items, quantity, billing status, and remarks.
- Staff meal distribution:
  - Staff search (ID, name, department, category).
  - Meal limits per staff/category per day/month.
  - Handling of subsidized vs full-price meals when limits are exceeded.
- Department/bulk distribution:
  - Bulk meal issues against departments with department-level billing/costing.
- Home delivery:
  - Recipient type (Patient/Staff/External), delivery location, timeslot, eligibility rules.

**Key Interfaces**
- Integrates with:
  - Patient/IPD/OPD module for patient and admission context.
  - HR/Staff module for staff master, categories and employment status.
  - Billing module for posting patient/staff/department charges.
- Consumes:
  - Meal & Package Management configuration, inventory availability and alerts.

### 2.5 Table, Waiter & Service Management

**Responsibilities**
- Table master:
  - Table identity, capacity, floor/zone, category/type, table-level charges and service attributes.
  - Real-time table status: Free/Occupied/Reserved/Cleaning.
- Table booking:
  - Walk-in, pre-booked, and scheduled bookings with time slots and basic KYC details.
- Waiter setup and assignment:
  - Waiter master, availability, default zones/tables, shift-based assignment.
- Service tracking:
  - Per-table orders with status transitions (Ordered → In Preparation → Served → Billed → Closed).
  - Monitoring of pending orders and service times per waiter/table.

**Key Interfaces**
- Integrates with:
  - Sales & POS module for billing of table orders.
- Provides:
  - Data for operational dashboards (table utilization, waiter efficiency).

### 2.6 Sales & POS (Indoor & Outdoor)

**Responsibilities**
- Sale header and line management:
  - Sale ID, timestamps, sale type (Indoor/Outdoor), order type (Counter/Table/Patient/Staff/Department/Home Delivery), and status.
  - Line items with products/packages, quantity, pricing, discounts and totals.
- Outdoor sale behavior:
  - Prepaid-only flows (cash/card/UPI) and mandatory payment validation.
  - Outdoor prices and receipt printing.
- Indoor sale behavior:
  - Patient/staff/department selection and indoor categories for pricing.
  - Postpaid billing behavior via Billing module with due creation.
- Refunds, cancellations and due collection:
  - Partial/full refunds, optional stock return, mandatory reasons, time-based limits.
  - Due ledger with multi-mode payments and collection audit.

**Key Interfaces**
- Integrates with:
  - Patient/IPD/OPD, HR/Staff, and Department masters for account selection.
  - Billing and Finance modules for revenue, due, and payment postings.
  - Discount Management and Corporate Service & Card Management through Billing flows.
- Consumes:
  - Inventory validation APIs, Meal & Package configuration, Table/Waiter data.

### 2.7 Reporting & Analytics

**Responsibilities**
- Operational and analytical reports including:
  - Daily/weekly/monthly sales reports by sale type, product, package, and role/category.
  - Consumption reports by patient, staff, department, ward, and time slot.
  - Waste/spoilage and stock status, near-expiry reports.
  - Staff package utilization, supplier purchase breakdowns.
  - Home delivery performance (volume, on-time vs delayed).

**Key Interfaces**
- Integrates with:
  - Hospital-wide reporting/BI layer where present.
  - Accounting and Inventory modules for reconciliations.

### 2.8 Security, Controls & Audit

**Responsibilities**
- Role-based access control:
  - Canteen Admin, Store Keeper, Cashier, Waiter, Manager/Supervisor, Finance/Billing roles.
- Business controls:
  - Stock validation before issue/sale, expiry checks, entitlement and limit enforcement.
  - Authorization for overrides (multiple meals, expired stock, price overrides, cancellations, stock adjustments).
- Audit logging:
  - Master changes, stock transactions, sales/refunds, overrides and rule exceptions.
- Non-functional:
  - Performance SLAs for POS operations.
  - Availability, reliability and usability requirements specific to high-volume canteen operations.

---

## 3. Dependencies on Other Hospital Modules

The canteen module **must not** duplicate core master or billing logic. Instead, it should integrate with the following modules:

- **Patient / IPD / OPD**
  - Patient registry, visits, ward/bed information and discharge status.
  - Endpoint contracts for patient search and patient account charging.
- **HR / Staff**
  - Staff master, departments, designations and categories.
  - Source of staff IDs and meal entitlement categories.
- **Department / Category Management**
  - Department codes and cost centers for department-level billing and reporting.
- **Clinical Chart Management**
  - Common classification for canteen items where cross-module discounting or reporting requires alignment.
- **Billing**
  - Common billing engine for OPD/IPD/Canteen.
  - Posting of indoor canteen charges to patient/staff/corporate accounts.
  - Handling of dues, receipts, and AR.
- **Discount Management**
  - Central configuration, assignment and application logic for discounts.
  - Priority rules and approval workflows.
- **Corporate Service & Card Management**
  - Corporate clients, card types, and policies.
  - Card issuance and card-level entitlement/discount constraints.
- **Inventory / Purchase**
  - Optional integration for shared stock items, PO/GRN flows and valuation.
- **Accounts / Finance**
  - GL mappings for canteen revenue, discount expense/contra revenue, waste, and stock adjustments.
- **Security & User Management**
  - Authentication and cross-module RBAC framework.

---

## 4. Phased Implementation Plan

The following phases are ordered to deliver usable increments while managing dependencies.

### Phase 0 – Foundations & Architecture

- Finalize canteen module boundaries and submodules as per this document.
- Define data models for core canteen entities and align with system-wide data standards.
- Agree and document APIs with Patient, Staff, Department, Billing, Inventory, Discount and Corporate Card modules.
- Establish non-functional baselines:
  - Performance SLAs for POS screens and inventory operations.
  - Audit, security, and logging standards.

### Phase 1 – Canteen Masters & Inventory Core

- Implement **Canteen Master & Configuration**:
  - Product and supplier masters, categories, dietary tags, auto stock alerts.
- Implement **Canteen Inventory & Raw Material Management**:
  - Stock-in, raw material receipt/verification, issues, adjustments, and waste flows.
  - Batch- and expiry-level tracking and validations.
- Provide inventory APIs for:
  - Stock availability checks.
  - Batch selection and auto-allocation rules.

**Exit Criteria**
- All canteen masters required by SRS implemented and secured.
- Inventory operations usable end-to-end for internal testing with basic UIs and APIs.

### Phase 2 – Meal & Package Configuration

- Implement **Meal & Package Management**:
  - Department food entitlements and billing types.
  - Meal category/type masters and package definitions with validity and pricing.
- Integrate with:
  - Department/Category, Patient/IPD/OPD (for department mapping only at this stage).

**Exit Criteria**
- Canteen can maintain a complete package catalog and meal entitlements independent of distribution or billing flows.

### Phase 3 – POS for Outdoor Sales

- Implement **Sales & POS** for **Outdoor Sale Type**:
  - Counter-based sale creation with:
    - Outdoor pricing, tax, totals.
    - Payment capture (cash/card/UPI) with mandatory payment validation.
  - Basic receipt printing.
- Integrate with:
  - Inventory for stock validation and deduction.
  - Finance/Accounting for optional summarized revenue entries.

**Exit Criteria**
- Outdoor canteen operations can be run in production with full stock control and receipts, independent of patient/staff flows.

### Phase 4 – Indoor Sales & Due Collection

- Extend **Sales & POS** for **Indoor Sale Type**:
  - Patient/staff/department selection and indoor category pricing.
  - Posting of charges to Billing as postpaid dues.
  - Due ledger UI and due collection workflow with multi-mode payments.
- Tighten:
  - Stock and expiry validation for all indoor transactions.
  - RBAC and audit for refunds, cancellations, and overrides.

**Exit Criteria**
- Indoor canteen sales fully integrated with Billing and AR.
- Due collection flows verified end-to-end with Accounting.

### Phase 5 – Patient & Staff Meal Distribution

- Implement **Patient & Staff Meal Distribution**:
  - Patient meal ordering with IPD/OPD integration and diet order imports.
  - One-meal-per-patient-per-slot enforcement with override approvals.
  - Staff meal distribution with per-category limits and auto-switch to full-price when limits exceeded.
  - Department/bulk issues and costing.
  - Home delivery flows and eligibility controls.
- Ensure:
  - All distribution flows consume inventory and create proper Indoor sales/billing events.

**Exit Criteria**
- Hospitals can operate diet and meal distribution as per SRS, with full integration to stock and billing.

### Phase 6 – Table, Waiter & Service Management

- Implement **Table, Waiter & Service Management**:
  - Table master, booking flows, and real-time table status dashboard.
  - Waiter master and shift-based table assignment.
  - Table order lifecycle integrated with POS indoor sales.

**Exit Criteria**
- Table service operations run through canteen POS, with measured service times and utilization reporting.

### Phase 7 – Discounts, Corporate Cards & Advanced Pricing

- Integrate canteen POS and meal distribution with:
  - **Discount Management** for discount selection, caps and approvals.
  - **Corporate Service & Card Management** for corporate/card eligibility and policy-driven benefits.
- Implementation details:
  - Pass corporate/card context to Billing on invoice creation.
  - Ensure canteen items are properly mapped to Clinical Chart/Service hierarchy for discount eligibility.
  - Enforce card and corporate status/validity at billing time with clear error messages.

**Exit Criteria**
- Corporate and card-based discounts are available and auditable in canteen billing in the same way as other modules.

### Phase 8 – Reporting, Analytics & Hardening

- Implement **Reporting & Analytics**:
  - Operational and management reports as per SRS.
  - Export/APIs to central BI platform if applicable.
- Complete **Security, Controls & Audit**:
  - Fine-tuned roles and permissions.
  - Full audit coverage and monitoring dashboards for errors, performance and usage.
- Hardening:
  - Load and performance testing for POS and inventory.
  - Final refinements based on pilot/early adopter feedback.

**Exit Criteria**
- Canteen module is production-ready for high-volume use, with complete reporting, controls, and monitoring in place.

---

## 5. Open Design Questions / Assumptions

The following points should be validated during detailed design:

- Whether canteen inventory is maintained as an independent store or tightly coupled to central Inventory, and how stock transfers between main store and canteen will be modeled.
- Exact granularity of canteen items in Clinical Chart (one-to-one vs grouped mappings for reporting and discounts).
- Rules for combining package discounts, corporate/card discounts and manual discounts in canteen billing (governed primarily by Discount Management).
- Preferred deployment style (independent service vs part of a hospital-operations service) and how this affects transaction boundaries and failure handling across modules.

