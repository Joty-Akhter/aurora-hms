## Software Requirements Specification (SRS)
## Hospital Canteen Management Module

### 1. Introduction

#### 1.1 Purpose

This document defines the functional and non-functional requirements for the **Hospital Canteen Management Module** within the hospital ERP. It is intended for product owners, business analysts, architects, developers, QA, and implementation teams.

#### 1.2 Scope

- **System Name**: Hospital Canteen Management Module
- **Scope**:
  - Manage end-to-end canteen operations:
    - Product and supplier management
    - Stock and raw material management
    - Meal package and distribution management
    - Table and waiter management
    - Product sales (indoor and outdoor) and home delivery
    - Reporting and analytics
  - Integrate with:
    - Patient/IPD/OPD systems
    - Staff/HR systems
    - Department/cost center master
    - Billing/Finance modules

#### 1.3 Definitions, Acronyms, and Abbreviations

- **IPD**: In-Patient Department  
- **OPD**: Out-Patient Department  
- **GRN**: Goods Receipt Note  
- **ERP**: Enterprise Resource Planning  
- **Indoor Sale**: Canteen sale linked to hospital entities (patient/staff/department) with postpaid billing  
- **Outdoor Sale**: Canteen sale to walk-in/public with prepaid cash/card/UPI payment  

#### 1.4 References

- `requirements/module-hospital/introduction.md`
- `requirements/module-hospital/billing.md`
- `requirements/Module-Inventory/README.md`
- `requirements/Module-Accounting/README.md`

#### 1.5 Overview

This SRS describes the overall product perspective and user roles, then details specific functional requirements, system controls, interfaces, and non-functional requirements for the canteen module.

---

### 2. Overall Description

#### 2.1 Product Perspective

- The Canteen Module is a **subsystem** of the hospital ERP.
- It relies on:
  - Central master data (patients, staff, departments)
  - Billing/Accounts for financial postings
- It provides:
  - Internal APIs/services for meal orders, stock, and billing events
  - UI screens for canteen staff, store keepers, dieticians, and managers

#### 2.2 Product Functions (High-Level)

- Maintain product, raw material, and supplier masters.
- Record stock in, stock out, raw material issues, and waste.
- Define and manage meal packages and diet-based offerings.
- Capture indoor and outdoor sales with correct pricing and billing logic.
- Manage table reservations, waiters, and table service.
- Support home delivery of meals with approval and delivery confirmation.
- Generate operational and analytical reports.

#### 2.3 User Classes and Characteristics

- **Canteen Admin**: Configures masters, prices, and rules.
- **Store Keeper**: Manages stock, purchases, and adjustments.
- **Cashier/Counter Staff**: Performs daily sales (indoor and outdoor).
- **Waiter/Service Staff**: Handles table orders and service updates.
- **Dietician** (optional): Manages diet profiles and nutrition data.
- **Manager/Supervisor**: Reviews reports, approves overrides, monitors KPIs.
- **Finance/Billing Users**: Consumes billing entries for accounting.

#### 2.4 Operating Environment

- Web-based module running within the hospital ERP environment.
- Browser-based front-end, application server, and relational database.
- Operates on hospital intranet with secure access controls.

#### 2.5 Design and Implementation Constraints

- Must follow hospital ERP architecture and technology stack.
- Must comply with hospital security, audit, and regulatory policies.
- Must support integration APIs/formats defined by existing modules.

#### 2.6 Assumptions and Dependencies

- Patient, staff, department, and billing masters are maintained and accurate.
- Network and servers provide sufficient performance for counter operations.
- Scanners/devices (e.g., QR/barcode readers) are available where advanced features are implemented.

---

### 3. Specific Requirements

#### 3.1 Master Data Management

##### 3.1.1 Product Master

1. The system shall allow creation, update, inactivation, and viewing of product records.
2. Each product shall have at minimum:
   - Product ID (system-generated)
   - Product Code (unique)
   - Product Name
   - Product Type (Raw Material / Finished Item / Packaging)
   - Category (Breakfast / Lunch / Dinner / Snacks / Beverages / Other)
   - Unit of Measure
   - Base Selling Price
   - Cost Price (optional)
   - Tax applicable flag and tax percentage
   - Dietary tags (multi-select: Veg/Non-Veg/Egg, Diabetic, Low Salt, etc.)
   - Active/Inactive status
3. The system shall prevent using inactive products in new transactions while preserving historical data.

##### 3.1.2 Supplier Master

4. The system shall maintain supplier records with:
   - Supplier ID
   - Supplier Name
   - Contact Person
   - Phone, Email
   - Address
   - Payment Terms
   - Tax/GST Number (optional)
   - Status (Active/Inactive)
5. The system shall disallow deletion of suppliers referenced in any transaction and allow inactivation instead.

##### 3.1.3 Food Category & Dietary Setup

6. The system shall maintain configurable food categories and dietary tags.
7. The system shall allow assigning multiple dietary tags to each product.

##### 3.1.4 Auto Stock Alert Configuration

8. For each product, the system shall allow setting:
   - Minimum Stock Level
   - Reorder Level
   - Expiry Alert Days
9. The system shall generate alerts when:
   - Stock falls below Minimum Stock Level.
   - The earliest batch of a product is within Expiry Alert Days of expiry.

---

#### 3.2 Stock & Raw Material Management

##### 3.2.1 Raw Material Master

10. The system shall support raw material items flagged separately from finished products.
11. The system should optionally allow mapping recipes/BOM for finished items to raw materials.

##### 3.2.2 Stock In / Purchase

12. The system shall allow recording stock in transactions (Direct Purchase, GRN from PO, Stock Adjustment In).
13. Each stock in transaction shall capture:
    - Supplier (where applicable)
    - Invoice Number & Date
    - Item, Quantity, Unit Price, Tax, Total
    - Batch Number (optional), Manufacturing Date, Expiry Date (if applicable)
14. Stock in transactions shall increase on-hand stock and maintain batch-level details.

##### 3.2.3 Raw Material Receive & Verification

15. The system shall support raw material receipt with a verification status (Pending, Verified, Rejected).
16. For rejected receipts, the system shall require a rejection reason.

##### 3.2.4 Raw Material Issue to Kitchen

17. The system shall allow issuing raw materials to kitchen or production areas.
18. Each issue shall capture:
    - Issue Date
    - Destination (Kitchen/Counter/Department)
    - Item, Quantity, Unit
    - Associated meal session (Breakfast/Lunch/Dinner) and date
19. Raw material issue shall reduce raw material stock balances.

##### 3.2.5 Stock Adjustment & Waste

20. The system shall support positive and negative stock adjustments with mandatory reason codes.
21. The system shall record waste/spoilage transactions with:
    - Item
    - Quantity
    - Reason (Expired, Overproduction, Returned Food, etc.)
    - Optional batch reference.
22. The system shall support a structured waste food clearance process where waste is grouped under a waste invoice or clearance number, with separate master (summary) and detail records capturing total items, total quantity, total amount, and per-item quantities and amounts.

##### 3.2.6 Inventory Controls

22. Before any sale or issue, the system shall validate that available stock is greater than or equal to the requested quantity.
23. If stock is insufficient, the system shall block the transaction or permit it only with authorized override and reason.
24. The system shall prevent using expired batches unless overridden by authorized users with a mandatory reason.

---

#### 3.3 Meal & Distribution Management

##### 3.3.1 Department Food Setup

25. The system shall allow defining meal entitlements per department (e.g., wards, ICU, OPD, staff, VIP).
26. For each department, the system shall specify:
    - Meal timings (Breakfast, Lunch, Dinner, Snacks)
    - Default meal type (Normal/Diet/Special)
    - Billing type (Chargeable/Complimentary/Package-Included).

##### 3.3.2 Meal Package Setup

27. The system shall allow creating meal packages with:
    - Package ID and Name
    - Applicable category (Patient/Staff/VIP/Department/Visitor)
    - List of included items with quantities
    - Dietary profile
    - Standard and category-specific prices (including Outdoor price)
28. The system shall allow validity periods for packages (start and end dates).
29. The system shall maintain a Meal Category master (for example, Breakfast, Lunch, Dinner, Snacks) and allow linking each package and product to one or more meal categories for reporting and pricing.
30. The system shall maintain a Meal Type master that defines, for each meal type, at least: category, default serving time window, default food menu text, sale amount, cost price, days of week applicability, and validity/branch context.

##### 3.3.3 Patient Meal Distribution

31. The system shall integrate with IPD/OPD to search and select patients (ID, Name, Ward, Bed).
32. The system shall display or import diet orders from clinical modules (Normal, Diabetic, Low Salt, etc.) where available.
33. For each patient and meal time, the system shall enforce one meal order per patient per time slot, blocking duplicates or requiring authorized override.
34. The system shall provide an in-patient food description master (for example, standard diet or meal descriptions) that can be reused when recording meals or diet plans.
35. The system shall record:
    - Meal date/time
    - Assigned package/items
    - Quantity
    - Billing status (Chargeable/Complimentary)
    - Remarks (e.g., patient refused, special meal).

##### 3.3.4 Staff Meal Distribution

37. The system shall allow staff search by Staff ID, Name, Department, Category.
38. The system shall maintain configurable staff meal limits (per day/month, by staff or category).
39. When staff meal limits are exceeded, the system shall either:
    - Switch to full-price billing, or
    - Block subsidized meals, as per configuration.

##### 3.3.5 Department/Bulk Distribution

36. The system shall support department-level bulk meal issues with:
    - Department code
    - Number of meals per package
37. Department meals shall be billed or costed to the department rather than individuals.

##### 3.3.6 Home Delivery (Meal Distribution Perspective)

38. The system shall allow capturing delivery details:
    - Recipient type (Patient/Staff/External)
    - Delivery location (Ward/Room/External address)
    - Preferred delivery time slot.
39. The system shall allow restricting eligible delivery locations/areas by configuration.

---

#### 3.4 Table & Waiter Management

##### 3.4.1 Table Master

40. The system shall maintain a table master with:
    - Table ID, Number
    - Seating Capacity
    - Floor or zone identifier
    - Area/Zone (VIP/General/Staff)
    - Table category and type (for example, VIP/General, inside/outside)
    - Billing attributes such as charge per day or per booking and any table-level service charges
    - Status (Active/Inactive)
41. The system shall display real-time table status (Free/Occupied/Reserved/Cleaning).

##### 3.4.2 Table Booking

42. The system shall support Walk-in, Pre-booked, and Scheduled meal bookings.
43. The system shall capture at least booking date, time, table, table category, booked-by name, mobile number, and an identifier such as national ID or employee ID for traceability.
44. The system shall prevent double booking for the same table and time slot or allow override with reason.

##### 3.4.3 Waiter Setup and Assignment

45. The system shall maintain waiter records with at least: ID/code, name, contact details, availability status, and default department/area and tables.
46. The system shall allow assigning waiters to tables by shift.

##### 3.4.4 Service Tracking

46. For each table order, the system shall track:
    - Order Time
    - Ordered items
    - Assigned waiter
    - Status (Ordered/In Preparation/Served/Billed/Closed)
47. The system shall support monitoring pending orders and service times per waiter/table.

---

#### 3.5 Sales & Order Management

##### 3.5.1 Sale Type Selector (Top-Level Control)

48. At the start of each sale, the system shall require selection of Sale Type as Indoor Sale or Outdoor Sale.
49. If the user changes Sale Type after adding items, the system shall prompt for confirmation and optionally clear/reset the transaction as per configuration.

##### 3.5.2 Core Sale Data

50. Each sale shall have:
    - Sale ID
    - Date & Time
    - Sale Type (Indoor/Outdoor)
    - Order Type (Counter/Table/Patient/Staff/Department/Home Delivery)
    - Item list (Product/Package, Quantity, Unit Price, Discount, Line Total)
    - Subtotal, Tax, Grand Total
    - Payment or billing information
    - Status (Pending/Confirmed/Cancelled/Refunded/Due/Paid).

##### 3.5.3 Refunds and Cancellations

51. The system shall allow referencing an original sale to process partial or full refunds.
52. The system shall optionally increase stock for returned items according to configuration.
53. Refunds shall require a reason and may be limited by time window (configurable).

##### 3.5.4 Due Collection (Indoor Sales)

54. The system shall maintain a list of outstanding dues arising from Indoor Sales.
55. The system shall allow recording partial and full payments and update due status accordingly.
56. The system shall support multi-mode payments for dues (for example, cash, card, cheque, advance adjustments) and track, for each payment, the paid amounts per mode, given amount, change amount, and any return amount.
57. The system shall support capturing card and cheque details (card number, card bank, cheque number, cheque bank, cheque date) and associate them with the due collection record.

---

#### 3.6 Indoor vs Outdoor Sale Behavior

##### 3.6.1 Indoor Role Categories & Pricing

56. The system shall maintain an Indoor Category Setup defining categories such as Staff, Consultant, Office, OT, F&B, Driver, Doctor, Management, and others as needed.
57. The system shall allow defining product/package-specific prices by category (for example, different prices for Chicken Meal or Tea for each category).
58. When Sale Type is Indoor and a staff/consultant/department is selected, the system shall apply the corresponding category-specific price.

##### 3.6.2 Outdoor Pricing

59. The system shall maintain Outdoor prices for products/packages and use them when Sale Type is Outdoor.

##### 3.6.3 Outdoor Sale Behavior

60. When Sale Type equals Outdoor, the system shall:
    - Enable prepaid payment inputs (cash/card/UPI etc.).
    - Disable billing integration with patient/staff/department accounts.
    - Hide or disable fields: Patient ID, Staff ID, Department Selection, Due Payment, credit options.
61. For Outdoor Sales, the UI shall display:
    - Product list with Outdoor prices
    - Quantity and totals
    - Payment mode and amount
    - Print receipt/bill.
62. Validation for Outdoor Sales:
    - Require PaymentAmount greater than or equal to TotalAmount.
    - Set PaymentModeType to Prepaid.
    - Prevent saving the transaction if payment is missing or insufficient, unless overridden by authorized users.
    - Mark sale as Paid on successful save.

##### 3.6.4 Indoor Sale Behavior

63. When Sale Type equals Indoor, the system shall:
    - Enable billing integration with patient/staff/department accounts.
    - Disable or hide prepaid cash inputs by default (unless partial payment is allowed by configuration).
64. For Indoor Sales, the UI shall display:
    - Patient search (IPD/OPD)
    - Staff search
    - Department selection
    - Indoor Category selector
    - Product list with category-based pricing
    - Due amount display and billing status.
65. Validation for Indoor Sales:
    - Require at least one of: PatientID or StaffID or Department.
    - Set PaymentModeType to Postpaid.
    - Set BillingStatus to Due (or equivalent status in the billing system).
    - Block save if no account (patient/staff/department) is selected.

---

#### 3.7 Integration Requirements

##### 3.7.1 Patient/IPD/OPD Integration

66. The system shall integrate with the Patient module for:
    - Patient search by ID, Name, Ward, Bed, Visit.
    - Fetching current admission/visit details.
    - Posting charges for Indoor Sales to patient accounts.

##### 3.7.2 Staff/HR Integration

67. The system shall fetch staff details and categories from HR/Staff master.
68. The system shall use staff category for pricing, benefits, and meal limits.

##### 3.7.3 Department/Cost Center Integration

69. The system shall support department selection and map it to cost centers for internal billing.

##### 3.7.4 Billing/Finance Integration

70. For Indoor Sales, the system shall:
    - Post charges as postpaid entries linked to patient/staff/department accounts.
    - Support tax and revenue GL mappings.
71. For Outdoor Sales, the system shall:
    - Not create billing entries in patient/staff accounts.
    - Optionally create summarized revenue entries for finance as required.

---

#### 3.8 Reporting Requirements

72. The system shall provide Daily/Weekly/Monthly Sales Reports (overall and by Sale Type).
73. The system shall provide Product-wise consumption reports (by patient/staff/department).
74. The system shall provide Waste/Spoilage reports.
75. The system shall provide Staff package consumption reports (by staff, department, category).
76. The system shall provide Indoor delivery/meal distribution summaries by ward/department and time slot.
77. The system shall provide Supplier purchase reports by supplier, product, and date range.
78. The system shall provide stock status and near-expiry reports.
79. The system shall provide home delivery performance reports (volume, on-time vs delayed).

---

#### 3.9 Optional / Advanced Features

##### 3.9.1 Dietary Management

80. The system should maintain patient diet profiles and check meal selections against restrictions, warning or blocking incompatible meals.

##### 3.9.2 Nutrition Dashboard

81. The system should store nutritional values (calories, protein, fats, carbs, sodium) per meal/package.
82. The system should summarize daily nutritional intake per patient versus prescribed targets.

##### 3.9.3 Loyalty / Staff Benefits

83. The system should support loyalty or benefit points for staff purchases with configurable earn and redeem rules.

##### 3.9.4 QR-based Meal Tracking

84. The system should support scanning patient or staff QR codes to confirm meal served or delivery completed, recording timestamp and serving staff.

---

### 4. System Controls & Security Requirements

#### 4.1 Business Controls

85. The system shall validate stock before sale or issue.
86. The system shall track expiry and spoilage with mandatory reasons.
87. The system shall enforce one meal per patient per time slot, with controlled overrides.
88. The system shall enforce staff meal/package limits.
89. The system shall require authorization for:
    - Price overrides
    - Stock adjustments
    - Sale cancellations and refunds
    - Rule overrides (multiple meals, expired stock, etc.).

#### 4.2 Security and Access Control

90. The system shall implement role-based access control (RBAC) for all canteen functions.
91. Access to configuration, pricing, and integration endpoints shall be restricted to authorized roles.

#### 4.3 Audit and Logging

92. The system shall maintain an audit trail for:
    - Creation and modification of masters
    - Stock transactions (in, out, adjustment, waste)
    - Sales, refunds, and cancellations
    - Overrides of validation rules.
93. Audit logs shall include user, timestamp, action, and before/after values where applicable.

---

### 5. Non-Functional Requirements

#### 5.1 Performance Requirements

94. Counter sale screens shall respond (load/search/submit) within defined SLA limits under normal load.
95. Reports for typical daily usage shall generate within defined SLA limits.

#### 5.2 Reliability & Availability

96. The module shall be available during defined hospital operating hours with uptime targets defined at ERP level.
97. The system shall ensure no loss of committed transactions in case of application restarts.

#### 5.3 Usability

98. The UI shall be optimized for high-volume operations (minimal clicks, keyboard shortcuts, efficient search).
99. The system shall provide clear error and validation messages.

#### 5.4 Maintainability & Extensibility

100. The system shall support configuration of categories, pricing, and limits without code changes.
101. The architecture shall allow adding new sale types, categories, and integration endpoints with minimal impact.

#### 5.5 Security

102. All sensitive communications shall use secure protocols as per hospital IT policy.
103. User authentication shall reuse the ERP’s central authentication and authorization mechanisms.

