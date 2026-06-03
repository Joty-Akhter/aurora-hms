## Discount Management – Configuration, Assignment, and Application

### 1. Overview

**Description**  
This document defines the requirements for **Discount Management** across hospital billing (OPD, IPD, Pharmacy, Diagnostics, Canteen, Packages, and other chargeable services). It covers:

- Discount **configuration** (type-wise and rule-wise master setup)
- Discount **assignment** to people/entities (patients, corporates, cards, staff, etc.)
- Discount **application** at **invoice time** with prioritization and approvals
- Accounting impact, reporting, and audit requirements

**Objectives**

- Provide a unified, consistent discount framework that can be reused by all billing modules.
- Ensure discounts are controlled by configuration and assignment, not ad‑hoc free text.
- Enforce priority, approval, and validity rules so that discounts are predictable and auditable.
- Support both **automatic** discounts (e.g., health card, corporate) and **manual**/direct discounts within tight governance.

---

### 2. Phase 1 – Discount Configuration (Type-wise Setup)

**Actor**: Admin / Authorized Configuration User  
**Module**: Discount Configuration (Master Setup)  
**Purpose**: Define discount types, default behavior, limits, and service/package applicability.

#### 2.1 Discount Types (Master)

1. The system shall maintain a **Discount Type Master** where each discount record belongs to **exactly one** discount type.  
2. The following baseline discount types shall be supported (configurable / extendable):
   - Management
   - Share Holder
   - Employee
   - Doctor
   - Consultant
   - Marketing
   - Corporate
   - Health Card
   - Promotional
   - Direct Discount / Less
   - Package Discount
3. The system shall enforce that **no discount can be used** in billing unless:
   - A valid Discount Type is selected, and
   - Both the Discount Type and the specific discount record are **Active**.

> Note: The Discount Type Master may be seeded with codes such as `D01`–`D11` for reference; actual codes are configurable but must remain unique.

#### 2.2 Discount Configuration (Core Setup)

Each discount definition in the system shall include at least the following **core configuration fields**:

- **Discount Name**
  - Display name shown on UI and reports.
- **Discount Code**
  - System-unique identifier.
  - Used in integration and audit logs.
- **Discount Type**
  - Reference to Discount Type Master (e.g., Management, Corporate, Health Card).
- **Discount Mode**
  - Allowed values: `Percentage`, `FixedAmount`.
  - Determines how `Discount Value` is interpreted.
- **Discount Value**
  - If mode = Percentage → discount percentage value (e.g., 10 = 10%).  
  - If mode = FixedAmount → absolute currency amount.
- **Maximum Discount Limit (Cap)**
  - Maximum discount value allowed per invoice or per service line (configuration must specify scope).
  - System shall never exceed this cap when applying the discount.
- **Valid From / Valid To**
  - Effective date range during which this discount can be applied.
  - If `Valid To` is null/blank, discount is open-ended until inactivated.
- **Approval Required (Yes/No)**
  - Indicates whether application of this discount needs a separate approval workflow.
- **Approval Level**
  - Applicable when Approval Required = Yes.
  - Allowed values (configurable): `User`, `Department Head`, `Management`, `CustomLevel`.
- **Status**
  - Values: `Active`, `Inactive`.
  - Inactive discounts must not be available for assignment or application.
- **Priority**
  - Numeric priority where **lower number = higher priority** during selection.
  - Used when multiple discounts are eligible at the same time.

Configuration Rules:

4. The system shall validate that `Discount Code` is unique among all discount definitions.  
5. The system shall prevent saving a discount with:
   - Missing or invalid Discount Type.
   - Invalid date range (Valid From > Valid To).
   - Negative or zero Discount Value (unless explicitly allowed by configuration).  
6. The system shall allow configuration of whether `Maximum Discount Limit` is:
   - Applied **per service line**, or
   - Applied to the **total invoice**, or
   - Both (line and invoice caps), as per hospital policy.

#### 2.3 Service-wise Discount Mapping (Mandatory)

**Purpose**: Control which services/items are eligible for each discount.

7. The system shall provide a **Service-wise Discount Mapping** screen or API where each discount can be linked to:
   - All services (global scope), or
   - Selected services / groups (e.g., by Clinical Chart, Department, HoGroup, Package Code).
8. For each (Discount, Service) combination, the system shall allow configuration of:
   - Maximum discount percentage for that service (optional override of master Discount Value).
   - Maximum discount cap amount specific to that service.
9. If a discount is **not** mapped to any service (or service group), it shall **not apply** to those services even if assigned to the patient/entity.
10. During invoice calculation, the system shall ensure:
    - A discount is only considered for a service line if:
      - The service is mapped to that discount, and
      - The discount is active and valid on the invoice date.

> Example: For X-Ray, a particular discount may allow 10% up to 500; for Blood Tests, 15% up to 300, etc.

#### 2.4 Package Discount Configuration

11. The system shall support **Package-level discounts** that apply to **packages as a whole**, not to individual component services.
12. For each package discount configuration, the system shall capture at least:
    - Package Code / Package Name (or group of packages).
    - Discount Mode (Percentage / FixedAmount).
    - Discount Value (percentage or fixed amount).
    - Maximum discount cap per package (optional).
    - Validity period and status.
13. The system shall enforce that **Package Discounts override any service-level discounts** for the services included within that package:
    - When a package discount is active and applicable, service-level discounts for items within that package shall be ignored or suppressed for that invoice line.
14. On invoices, the system shall display **package discount amounts separately** from:
    - Service-level discounts, and
    - Generic invoice-level discounts,
    to maintain transparency.

---

### 3. Phase 2 – Discount Assignment (Person / Entity-wise)

**Actor**: Admin / Billing Supervisor / Authorized Marketing/HR User  
**Module**: Discount Assignment  
**Purpose**: Assign configured discounts to eligible persons or entities so they become available at billing time.

#### 3.1 Assignment Categories

15. The system shall support assigning discounts to multiple **assignment categories**, including but not limited to:

- Management → specific patients, invoices, or internal authorization contexts.
- Share Holder → shareholder profile or registration ID.
- Employee → Employee ID / Staff ID.
- Doctor → Doctor ID.
- Consultant → Consultant ID.
- Marketing → Marketing person ID or campaign owner.
- Corporate → Corporate client / contract / corporate ID.
- Health Card / Corporate Card → Card ID or card number.
- Promotional → Campaign ID / promo code.
- Direct Discount / Less → User permission at invoice level (no pre-assignment to person; bound to user role and approval).
- Package Discount → Package Code (configurable mapping).

16. For each assignment, the system shall capture at least:
   - Discount Code / Discount Type.
   - Assigned To category (e.g., Employee, Corporate, Health Card).
   - Assigned Entity Identifier (e.g., Employee ID, Corporate ID, Card ID, Campaign ID).
   - Valid From / Valid To for the assignment.
   - Status (Enabled / Disabled).
   - Optional service scope override (All services vs Selected services).
   - Optional usage limit (e.g., maximum number of invoices or visits).

#### 3.2 Assignment Rules (System Enforced)

17. The system shall allow **one person/entity to have multiple discounts assigned** simultaneously (e.g., a staff member may also have a health card).  
18. The system shall ensure that **only one discount per service line** is actually applied at billing time:
   - When multiple discounts are eligible, the system shall select the discount according to configured **priority rules** (see section 4.1).  
19. Expired or inactive discount assignments shall be automatically ignored by discount selection logic.  
20. An assignment shall be considered **eligible** only if:
   - Assignment status is Enabled.
   - Current date is within assignment Valid From / Valid To range.
   - The underlying Discount configuration is also active and valid on that date.

#### 3.3 Assignment Validity Controls

21. The system shall enforce the following validity controls for assignments:
   - **Start / End Date**: Time-bound assignment; discount must not apply before start or after end.
   - **Status**: Enabled/Disabled flag to quickly pause or revoke an assignment without deleting history.
   - **Service Scope**:
     - `All Services` → discount applies to all services it is mapped to in configuration.
     - `Selected Services` → only specific services (from service mapping) for this assignee.
   - **Usage Limit**:
     - Maximum number of invoices or visits on which the discount may be used.
     - Once the limit is reached, further discount attempts must be blocked or require override as per configuration.

22. The system shall provide a way to **view current utilization** of assignment usage limits (e.g., number of times discount has been used against available quota).

---

### 4. Phase 3 – Discount Application (Invoice Time)

**Actor**: Billing User / Cashier / Operator  
**Module**: Billing (OPD, IPD, Pharmacy, Canteen, etc.)  
**Purpose**: Automatically or manually apply discounts on services during invoice creation, according to configuration and assignments.

#### 4.1 Discount Priority (System-controlled)

23. The system shall implement a **global discount priority order** so that, when multiple discounts are applicable to the same service line, the system can deterministically select the correct one.
24. A default priority sequence shall be configured as follows (lowest number = highest priority), and kept configurable:
   1. Management  
   2. Corporate  
   3. Health Card  
   4. Share Holder  
   5. Employee  
   6. Doctor  
   7. Consultant  
   8. Promotional  
   9. Direct Discount / Less  
   10. Package Discount (package discount may override service discounts per section 2.4)
25. When multiple discounts of the **same type** are applicable to a line, the system shall:
   - Choose the one with higher benefit to the patient **within configured caps and approval rules**, or
   - Use additional per-discount `Priority` numbers if configured.

#### 4.2 Invoice Discount Application Flow

At invoice creation time, the system shall follow this sequence for discount processing:

26. **Capture Context**
   - User selects or confirms the billing context (OPD, IPD, Pharmacy, Canteen, etc.).
   - User enters or selects Patient ID / Corporate / Health Card / Card ID / Campaign Code, as applicable.

27. **Fetch Eligible Discounts**
   - System fetches all **active and valid** discount assignments for the given patient/entity/card/corporate.
   - System includes automatically eligible discounts based on admission type, corporate contract, or card type where applicable.

28. **Per-Service Evaluation**
   - For each service line on the invoice:
     - Determine the **eligible discounts** (from assignments and service mappings).
     - Filter out any discounts that violate configuration or validity rules.
     - Sort remaining discounts by **priority** (section 4.1).
     - Select the **highest priority** discount (or combination if business rules allow invoice-level + line-level discounts, as configured).

29. **Apply Discount**
   - Compute discount amount based on:
     - Discount Mode (Percentage or FixedAmount).
     - Discount Value.
     - Service-wise or global maximum caps.
   - Apply discount amount to line:
     - Ensure that resulting amount is **not negative**.
   - Summarize discount amounts:
     - Per line.
     - Per invoice.

30. **Net Amount Calculation**
   - For each line: `Net Line Amount = Gross Line Amount − Line Discount`.
   - For invoice: `Net Invoice Amount = Sum(Net Line Amounts) − Any Invoice-level Discount (if configured)`.

#### 4.3 Invoice Validation Rules (All Must Pass)

31. Before a discount is actually applied to a service line, the system shall validate **all** of the following:
   - Discount configuration exists and is `Active`.
   - Discount is correctly assigned to the person/entity (where required).
   - Current date falls within **both** configuration validity and assignment validity windows.
   - The service is mapped to the discount (service-wise mapping).
   - User has permission to apply this discount type (especially for Direct Discount / Less).
   - Approval is completed or not required (see section 4.4).
   - Any usage limit (per assignment or per discount) is not exceeded.
32. If any of the above validation checks fail, the system shall:
   - **Skip** applying that discount to the line.
   - Optionally log the reason in an internal audit/diagnostic log.
   - Allow other eligible discounts (if any) to be considered according to priority.

#### 4.4 Discount Approval Workflow (If Required)

33. When a discount requires approval (per configuration or due to thresholds), the system shall operate as follows:
   - Invoice is saved in a state such as **“Discount Approval Pending”**.
   - Discount details (type, amount, reason, user, patient, invoice context) are queued for approver review.
34. Approver Workflow:
   - Approver (User / Dept Head / Management as configured) sees pending discount requests.
   - Approver may:
     - **Approve** → Invoice can be finalized/posted with the discount.
     - **Reject** → Discount is removed automatically; invoice recalculates without it.
35. The system shall prevent final posting/printing of confirmed invoices with pending discount approvals, except where configuration explicitly allows provisional billing with pending approval.

---

### 5. Phase 4 – Accounting Impact

**Actor**: System (auto-posting), Finance / Accounts (review)  
**Module**: Accounts / Finance Integration

36. The system shall support accounting entries that correctly reflect discount impact:
   - **Debit**: Discount Expense / Discount Allowed account.
   - **Credit**: Service Revenue account (or contra revenue).
37. For each invoice with discounts applied, the system shall:
   - Post revenue at **gross amount** (before discount).
   - Post a separate **discount amount** to the appropriate Discount Expense or Contra-Revenue account, as per chart of accounts design.
38. The accounting integration shall ensure:
   - Discounts are visible as a separate column/field in financial reports.
   - Discount entries are tied to the originating invoice and patient/corporate account.
39. The system shall allow configuration of:
   - Which GL accounts are used for discount expense/contra revenue, by department or service group (e.g., separate discount accounts for Pharmacy vs Clinical vs Canteen).

---

### 6. Phase 5 – Reporting & Audit

#### 6.1 Reporting

40. The system shall support reports that analyze discounts by:
   - Discount Type (Management, Corporate, Employee, Promotional, etc.).
   - Service group or HoGroup (Lab, Radiology, Canteen, etc.).
   - Department / Sub‑department.
   - Patient type (IPD/OPD, Corporate vs General).
   - Approver and applying user.
   - Date range.
41. Standard reports shall include at least:
   - **Discount Summary Report** (by day, by department, by discount type).
   - **Discount by Doctor/Consultant** (to see discounts linked to provider-level decisions).
   - **Corporate / Health Card Discount Utilization** (per contract, per card program).
   - **Promotional Campaign Performance** (discount vs incremental revenue).

#### 6.2 Audit & Compliance

42. The system shall maintain an **audit log** for all discount activities that captures at minimum:
   - Who applied the discount (user ID, role).
   - Who approved the discount (if applicable).
   - Date and time of application and approval/rejection.
   - Discount Type and Code.
   - Original gross amount, discount amount, net amount.
   - Reason for discount (mandatory for Management and Direct Discount / Less; configurable for others).
43. The system shall ensure **approval is non-bypassable** when required by configuration:
   - Users without necessary approval rights shall not be able to finalize invoices with unapproved discounts.
44. The system shall preserve historical discount records even if:
   - The discount configuration is later inactivated or changed.
   - The assignment is revoked or disabled.

---

### 7. Key Business Notes & Constraints

45. **No assignment = No discount**  
   - Unless the discount type is explicitly designed as a **global** discount (e.g., public festival promotional campaign), discounts require a valid assignment to a person/entity/card/campaign.

46. **No service mapping = No discount**  
   - If a service is not mapped to the discount in Service-wise Discount Mapping, it is not eligible, regardless of assignment.

47. **Priority decides which discount applies**  
   - When multiple discounts compete for a service line, the system uses the configured priority rules and per-discount priority to determine the single applied discount (unless combined discounts are explicitly supported by design in certain use cases).

48. **Package discount overrides service discounts**  
   - When a package discount is configured and applicable, it takes precedence over underlying service-level discounts for that package’s components.

49. **Approval is mandatory when configured**  
   - For discounts marked as “Approval Required”, the system must not allow finalization of the invoice without an approved status on all such discounts.

