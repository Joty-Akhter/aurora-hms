## Corporate Service & Card Management

### 1. Overview

**Description**  
This document defines the requirements for managing **corporate services, corporate clients, corporate card types, card policies, and card issuance**. These capabilities are used by Billing and Discount Management to:

- Control which services are eligible for corporate billing and discounts.
- Configure benefit levels per **corporate client** and **card type**.
- Automatically apply corporate/card-based discounts at invoice time.

**Scope**

- Corporate Service Type configuration (which modules and services are eligible).
- Corporate Card Type configuration (benefit tiers, validity, and discount limits).
- Corporate Client registration (master data & agreements).
- Corporate Card Policy configuration (service mapping & discount rules).
- Card Issuance and lifecycle (issue, block, reissue).
- **Physical card generation and printing** for **corporate benefit cards** (offices and named employees/beneficiaries), orchestrated via the **Hospital Card Service module** (`hospital-card-management-service`); optional extension to **other printed benefit card** programs using the same printing pipeline.
- Integration points with **Billing**, **Discount Management**, and **Hospital Card Service** (card record, number, print template).

---

### 2. Module 1 – Corporate Service Type Configuration

#### 2.1 Purpose

Provide a centralized master of **Corporate Service Types** that define which categories of healthcare services are eligible for corporate clients and cards, and in which modules (OPD, IPD, Pharmacy, Diagnostic, etc.) they apply.

#### 2.2 Functional Requirements

1. The system shall allow users with appropriate roles to **create, edit, and deactivate** Corporate Service Types.  
2. For each Corporate Service Type, the system shall capture at least:
   - Service Type ID (UUID / system key).
   - Service Type Code (human-readable, unique code).
   - Service Type Name.
   - Description.
   - Applicable Modules (one or many; e.g., OPD, IPD, Pharmacy, Diagnostics, Canteen).
   - Status (Active/Inactive).
3. The system shall ensure that **Service Type Name** and **Service Type Code** are unique among all active service types.  
4. Inactive service types:
   - Shall not be available for selection in corporate card policies or new mapping activities.
   - Shall not be removable from existing historical data, but may remain referenced in past invoices/policies.
5. The system shall prevent deletion (hard delete) of any Corporate Service Type that is already:
   - Referenced in an existing corporate card policy, or
   - Used in historical billing or discount rules.
   In such cases, only status change to **Inactive** is allowed.

#### 2.3 User Interface – Form View

Field-level requirements for the Corporate Service Type form:

- **Service Type ID**
  - Type: UUID / System-generated key.
  - Required: Yes (system-assigned).
  - Behavior: Auto-generated; not editable by user.

- **Service Type Code**
  - Type: String.
  - Required: Yes.
  - Description: Unique service type identifier.
  - Validation:
    - Must be unique.
    - Read-only after initial creation (or editable only by Admin roles with audit).

- **Service Type Name**
  - Type: String.
  - Required: Yes.
  - Validation:
    - Must be unique (case-insensitive comparison).
    - Max length as per system standard (e.g., 150 chars).

- **Description**
  - Type: Text / Textarea.
  - Required: No.
  - Validation: Max 1000 characters.

- **Applicable Module(s)**
  - Type: Enum / Multi-select (e.g., OPD, IPD, Pharmacy, Diagnostic, Canteen).
  - Required: Yes.
  - Validation:
    - At least one module must be selected.

- **Status**
  - Type: Boolean / Enum.
  - Values: Active / Inactive.
  - Default: Active on creation.

Actions:

- **Save** – Create new Corporate Service Type.
- **Update** – Modify allowed fields of existing type (respecting validation and reference rules).
- **Cancel** – Discard unsaved changes and return to list.

#### 2.4 User Interface – List View

The list view for Corporate Service Types shall display at minimum:

- Service Type Code (read-only).
- Service Type Name (read-only).
- Applicable Module(s) (read-only, display as concatenated list or badges).
- Status (Active / Inactive).
- Action buttons:
  - View (always available).
  - Edit (role-based; disabled for users without configuration rights).

#### 2.5 Business Rules & Edge Cases

6. **Uniqueness**: Service Type Name and Code must be unique; duplicated entries shall be blocked with a clear validation message.  
7. **Inactive usage**: If a service type is marked Inactive while active corporate card policies still reference it:
   - New policies using this service type shall be blocked.
   - Existing policies shall remain but may be auto-marked for review (implementation detail), and no new assignments beyond existing scope should rely on it.  
8. Attempting to delete a service type that is mapped to any policy shall:
   - Fail with an error message stating that deletion is restricted due to existing mappings.

#### 2.6 Error Handling & System Actions

9. On form submission, the system shall:
   - Validate mandatory fields (Name, Code, Applicable Module, Status).
   - Reject invalid module selections or missing required fields.  
10. The system shall automatically:
    - Generate Service Type ID and (optionally) Service Type Code according to configured rules.
    - Log creation and modification history (user, timestamp, old vs new values).
11. Changes to Corporate Service Types shall affect **future policy setup and billing only** and shall not retroactively change historical invoices.

---

### 3. Module 2 – Corporate Card Type Configuration

#### 3.1 Purpose

Define **Corporate Card Types** that represent different benefit/privilege levels (e.g., Silver, Gold, Platinum) with associated validity and maximum discount/benefit limits.

#### 3.2 Functional Requirements

12. The system shall allow creation, update, activation, and deactivation of **Card Types**.  
13. For each Card Type, the system shall capture at least:
    - Card Type ID (system key).
    - Card Type Name.
    - Validity (as duration or rules used to derive expiry dates).
    - Max Discount Percentage (or equivalent benefit cap).
    - Status (Active / Inactive).
14. Inactive Card Types:
    - Cannot be used for **issuing new cards**.
    - Shall not be selectable in new Corporate Card Policies.
    - Continue to appear in historical records and reports.

#### 3.3 User Interface – Form View

- **Card Type ID**
  - Type: UUID / System-generated.
  - Required: Yes.
  - Behavior: Auto-assigned; not editable.

- **Card Type Name**
  - Type: String.
  - Required: Yes.
  - Validation:
    - Must be unique.

- **Validity / Expiry Rule**
  - Representation:
    - Either as **duration** (e.g., “months from issue date”) or explicit rules (e.g., `ExpiryMonths` integer).
  - Required: Yes.
  - Validation:
    - Must be greater than 0 (for duration-based).

- **Max Discount Percentage**
  - Type: Decimal.
  - Required: Yes.
  - Validation:
    - Value must be ≥ 0 and ≤ system-wide maximum allowed discount percentage.
    - Actual effective discount per card/policy is further constrained by **Discount Management** rules.

- **Status**
  - Type: Boolean / Enum (Active / Inactive).
  - Default: Active.

#### 3.4 User Interface – List View

The list shall display:

- Card Type Name (read-only).
- Validity period / rule (read-only).
- Max Discount Percentage / Discount Limit (read-only).
- Status (Active / Inactive).
- Action (View; Edit based on role/permission).

#### 3.5 Business Rules & Error Handling

15. Discount percentage configured at Card Type level must not exceed:
    - A configurable **system max discount limit**, and
    - Any legal or hospital policy constraints.  
16. Card Types that are Inactive shall not be available for:
    - Issuing new cards (Card Issue module).
    - New card policy configuration.  
17. The system shall:
    - Validate missing mandatory fields and invalid discount values.
    - Reject invalid submissions with clear error messages.
18. During card issue, the system shall use Card Type validity rules to **auto-calculate card expiry dates**.

---

### 4. Module 3 – Corporate Client Registration

#### 4.1 Purpose

Register and maintain master records for **Corporate Clients** (companies), including agreement details, status, and high-level credit/usage controls.

#### 4.2 Functional Requirements

19. The system shall allow creation and maintenance of Corporate Client records, including:
    - Client name and contact details.
    - Agreement summary and period.
    - Billing cycle.
    - Status (Active, Inactive, Expired, Suspended).  
20. Corporate Client records shall be used as billable entities in:
    - OPD/IPD Billing.
    - Accounts Receivable.
    - Discount and card policies.

#### 4.3 User Interface – Form View

Fields:

- **Client Name**
  - Type: String.
  - Required: Yes.
  - Validation:
    - Must be unique or clearly distinguishable (configurable).

- **Address**
  - Type: Text / Textarea.
  - Required: Yes.

- **Contact Person**
  - Type: String.
  - Required: No (recommended).

- **Phone**
  - Type: String.
  - Required: No.
  - Validation:
    - Valid phone format as per hospital policy.

- **Email**
  - Type: String.
  - Required: No.
  - Validation:
    - Valid email format.

- **Agreement Details**
  - Type: Text / Textarea.
  - Required: No.
  - Description: Summary of contract terms or a reference to scanned contracts.

- **Agreement Period (Start / End)**
  - Type: Date / DateTime.
  - Required: No (but recommended).
  - Validation:
    - If provided, End Date must be ≥ Start Date.

- **Billing Cycle**
  - Type: Enum / Text (e.g., Monthly, Quarterly, Half‑yearly, Yearly, Custom).
  - Required: Yes.

- **Status**
  - Type: Enum.
  - Values: Active / Inactive / Expired / Suspended.
  - Required: Yes.

Additional (optional) fields:

- **Credit Limit**
  - Type: Decimal.
  - Used when corporate is allowed credit billing.

#### 4.4 User Interface – List View

Minimum columns:

- Client Name.
- Agreement Period (start–end or current status).
- Status (Active / Inactive / Expired / Suspended).
- Key contract indicators (e.g., credit limit, billing cycle).
- Action: View / Edit / Deactivate (based on role).

#### 4.5 Business Rules & System Actions

21. When **agreement period ends**, system shall:
    - Automatically mark status as **Expired**.
    - Prevent new corporate billing or new card issuance under that client, unless extended/renewed.  
22. If credit limits are configured, the system shall:
    - Enforce credit limit checks during billing or periodic AR posting (see Billing SRS).  
23. Manual suspension:
    - An admin may manually set client status to **Suspended**, which shall be treated similar to Expired for new invoices and new card issuance (until reinstated).  
24. The system shall log creation, updates, and status changes with user and timestamp.

---

### 5. Module 4 – Corporate Card Policy Configuration

#### 5.1 Purpose

Define **service eligibility and discount rules** that link:

- Corporate clients,
- Card types, and
- Corporate service types / services.

These policy records drive automatic corporate/card-based discount decisions at invoice time (in combination with the central **Discount Management** module).

#### 5.2 Functional Requirements

25. The system shall allow configuration of **Card Policies** with the following minimum fields:
    - Corporate Client ID.
    - Card Type.
    - Service Type(s) and/or specific services.
    - Discount rules and limits.
    - Validity period for the policy.
    - Status (Active / Inactive).  
26. Policy configuration shall be constrained by:
    - Corporate Client status (must not be Expired or Suspended).
    - Card Type status (must be Active).
    - Corporate Service Type status (must be Active).

#### 5.3 User Interface – Form View

Fields:

- **Corporate Client ID**
  - Type: Dropdown / lookup.
  - Required: Yes.
  - Validation:
    - Must select an existing, active or valid corporate client.

- **Corporate Client Name**
  - Type: Read-only text.
  - Required: Yes.
  - Behavior:
    - Auto-filled based on selected Corporate Client ID.

- **Card Type**
  - Type: Dropdown.
  - Required: Yes.
  - Validation:
    - Must select from available, Active Card Types.

- **Service Type(s)**
  - Type: Multi-select dropdown.
  - Required: Yes.
  - Validation:
    - At least one Corporate Service Type must be selected.
    - Only Active service types shall be selectable.

- **Discount Rules**
  - Type: Structured (e.g., percentage, max cap per service type).
  - Required: Yes.
  - Validation:
    - Numeric values ≥ 0.
    - Must not exceed:
      - Card Type’s Max Discount Percentage.
      - Any corporate-specific max discount limits.
      - System-wide maximum discount (from Discount Management).

- **Validity Period (Start / End)**
  - Type: Date Range.
  - Required: Yes.
  - Validation:
    - Start Date ≤ End Date.
    - Policy validity must not exceed the corporate client’s agreement period by default (warning or block per config).

- **Status**
  - Type: Enum (Active / Inactive).
  - Required: Yes.

#### 5.4 User Interface – List View

Columns:

- Corporate Client (ID / Name).
- Card Type.
- Service Type(s) (or summary).
- Validity period.
- Status.
- Action: View / Edit (role-based).

#### 5.5 Business Rules, Edge Cases & System Actions

27. **Policy validity vs card validity**:
    - Policy validity may be **more restrictive** than card validity (shorter period), but systems must ensure that:
      - If a policy expires while cards remain valid, discounts stop applying according to policy expiry.  
28. At most **one active policy** per combination of (Corporate Client, Card Type, Service Type) may exist for a given date range:
    - Overlapping policy periods for the same combination shall be blocked or versioned explicitly.  
29. The system shall support **version control or audit logging** for policy updates, capturing:
    - Old vs new values.
    - Effective date of change.
    - User and timestamp.  
30. The system shall prevent:
    - Duplicate policy mappings for same Corporate Client / Card Type / Service Type and overlapping validity.  
31. Edge cases such as overlapping policy dates or conflicting discounts must result in:
    - Clear user error messages at configuration time.
    - Well-defined precedence rules at invoice time (in conjunction with Discount Management priority).

---

### 6. Module 5 – Card Issue

#### 6.1 Purpose

Issue, activate, block, and track **corporate cards** assigned to individuals (e.g., employees or beneficiaries) under a corporate client.

#### 6.2 Functional Requirements

32. The system shall support card lifecycle operations:
    - Card creation/issuance.
    - Status changes (Active, Blocked, Expired).
    - Reissue (in case of lost/damaged cards) with linkage to prior card history.  
33. Each card shall be tied to:
    - A Corporate Client.
    - A Card Type.
    - A named person (e.g., Employee or dependent).
34. The system shall support **physical printing** of **corporate benefit cards** for use by **corporate offices** and **named beneficiaries** (employees/dependents). Printed cards complement the electronic card record used at billing time for discount eligibility.
35. **Card generation and printing** shall be integrated with the **Hospital Card Service module** (implementation: `hospital-card-management-service` or successor):
    - On issuance (or reissue), the corporate card workflow shall **create or synchronize** a card instance in the Hospital Card Service with a **unique card number** (or token), linkage to **Corporate Client** and **Card Type**, **holder identity**, and **validity**, consistent with corporate master data.
    - The Hospital Card Service shall expose **print-ready output** (template-based: PDF, label, or card-stock layout) and/or **Print / Reprint** actions bound to that card record.
    - Billing and discount validation shall continue to use the **same card number** and status as stored in Hospital Card Service (single source of truth for the credential).
36. **Other benefit card** programs (non-corporate or supplementary), such as promotional health cards, loyalty tiers, or partner schemes, may **also** be **generated and printed** through the **same** Hospital Card Service module using **distinct card products** and configuration; corporate cards remain a **separate** business program with their own masters (Corporate Client, Card Type, Policy) but **share** technical issuance and print capabilities.

#### 6.2a Physical card layout (minimum)

Printed corporate benefit cards shall support a **configurable template**; minimum elements:

| Element | Required | Notes |
|---------|----------|--------|
| Hospital name / logo | Yes | Branding |
| **Corporate client name** | Yes | As registered |
| **Card Type** / tier name (e.g. Silver, Gold) | Yes | Matches configured Card Type |
| **Card number** | Yes | Unique; matches Hospital Card Service record |
| **Holder name** | Yes | Employee or beneficiary |
| **Valid from / Expiry** | Yes | Per Card Type rules |
| **Barcode or QR** | Recommended | Encodes card number or secure token for validation at billing |

Optional: employee id, corporate reference code, photo (policy-dependent).

#### 6.3 User Interface – Form View

Fields:

- **Card ID**
  - Type: UUID / system key.
  - Required: Yes.
  - Behavior: Auto-generated.

- **Corporate Client ID**
  - Type: Dropdown / lookup.
  - Required: Yes.
  - Validation:
    - Must refer to an Active (non-suspended, non-expired) corporate client.

- **Corporate Name**
  - Type: Read-only text.
  - Required: Yes.
  - Behavior: Auto-filled from selected Corporate Client.

- **Card Type**
  - Type: Dropdown.
  - Required: Yes.
  - Validation:
    - Must select from Active Card Types.

- **Card Number**
  - Type: String or auto-generated sequence.
  - Required: Yes.
  - Validation:
    - Unique per card.
    - May be generated by the system to avoid duplication.

- **Employee / Holder Name**
  - Type: String.
  - Required: Yes.

- **Employee / Holder Identifier**
  - Type: String (e.g., Employee ID, National ID).
  - Required: Yes or as per policy.
  - Purpose: Prevent duplicates and support reissue tracking.

- **Card Expiry Date**
  - Type: Date.
  - Required: Yes.
  - Behavior:
    - Auto-calculated based on Card Type validity rules and/or corporate policy.

- **Status**
  - Type: Enum (Active, Blocked, Expired).
  - Required: Yes.
  - Default: Active on issue.

#### 6.3.1 Print and Hospital Card Service actions

- **Generate / sync card** (implicit on successful issue or reissue): creates or updates the card in **Hospital Card Service**; surfaces errors if sync fails (user may retry; corporate record state per configuration).
- **Print card** / **Print preview**: opens template preview or sends to configured printer; enabled after successful generation.
- **Reprint**: allowed for authorized roles when card is **Active** (lost print, damaged print); **reissue** for lost/stolen card follows §6.5 and creates a **new** card number with updated Hospital Card Service record.
- **Reissue** flow: old card **Blocked** in both corporate module and Hospital Card Service; new card **issued** and **printed** as for new issuance (see §6.5 rules 40 and 42–44).

#### 6.4 User Interface – List View

Columns:

- Card Number.
- Employee / Holder Name.
- Corporate Client Name.
- Card Type.
- Expiry Date.
- Status (Active / Blocked / Expired).
- Actions: View, Block, Reissue (role-based).

#### 6.5 Business Rules, Edge Cases & System Actions

37. Only **active policies and active card types** allow card issuance:
    - If no valid policy exists for a given Corporate Client / Card Type, the system should warn or block issuance (configurable).  
38. Expired cards shall:
    - Automatically move to **Expired** status on or after expiry date.
    - No longer grant discounts at invoice time.  
39. Duplicate employee/holder card detection:
    - System should warn when a holder already has an active card for the same Corporate Client and Card Type.
    - Reissue flows should link new card to old card history.  
40. Lost card reissue:
    - Old card may be set to **Blocked**.
    - New card created with new Card Number and appropriate expiry.
41. The system shall track **usage history** per card (invoices, discounts applied) for audit and analytics.
42. **Printing** shall not create a second logical card: **reprint** uses the same card number and Hospital Card Service id unless a **reissue** is performed (new number).
43. If Hospital Card Service is **unavailable**, corporate issuance may **queue** sync and print, or **block** issuance with a clear message (configurable); financial/discount rules must not diverge from stored card state once sync completes.
44. **Other benefit cards** using the shared print pipeline shall use **separate** product/configuration keys so corporate discounts apply only to **corporate-issued** cards linked to Corporate Client and Policy.

---

### 7. Corporate/Card Discount Application in Invoice

> Detailed cross-module discount behavior and priority are governed by **[Discount Management](discount-management.md)**. This section describes how corporate/card context is supplied to Billing and how it influences discount selection.

#### 7.1 Purpose

Automatically apply pre-configured **corporate or card-based discounts** when a valid Corporate Client or Card Number is provided during invoice creation. Discounts are applied service-wise for eligible clinical and non-clinical services according to configured policies.

#### 7.2 Functional Requirements

45. During invoice creation (OPD, IPD, Pharmacy, Canteen, etc.), the billing screen shall accept:
    - Corporate Client reference, and/or
    - Card Number (Corporate Card).  
46. Upon entry of a valid Corporate Client / Card Number, the system shall:
    - Fetch applicable corporate client record.
    - Fetch valid card record and Card Type (if card is used).
    - Fetch active Card Policies matching:
      - Corporate Client, Card Type, Service Type(s), and current date.  
47. For each service line added to the invoice, the system shall:
    - Determine whether the service belongs to a Corporate Service Type included in any active policy for that client/card.
    - If eligible, determine the applicable discount rule (percentage, cap) in combination with Discount Management:
      - Card Type-level discount limits.
      - Policy-level discount rules.
      - Global discount configuration and priority.

#### 7.3 Business Rules & Priority

48. Discount application shall obey the following high-level rules:
    - Discount applies only after a valid corporate or card context is captured.
    - Discount hierarchy (example, configurable):
      - Card Type-specific rules → Corporate Service Type policy → Corporate default discount → Other discounts.  
49. The system shall:
    - Calculate discount **per service item**, based on corporate/card policy and Discount Management configuration.
    - Restrict manual override of discount amounts to users with special permissions; all overrides must be audited.  
50. If:
    - Card is invalid or expired, or
    - Corporate status is suspended or agreement has expired, or
    - Service is not eligible per policy,
    then no corporate/card discount shall be applied, and the user shall see a meaningful reason.

#### 7.4 Error Handling & Audit

51. The system shall handle the following error conditions gracefully:
    - **Invalid or expired card** → show clear error, do not apply card-based discounts.
    - **Corporate account suspended/expired** → show error, prevent corporate billing or discount usage.
    - **Service not eligible for discount** → show informational message if user attempts to apply corporate discount manually.  
52. All corporate/card discount applications shall be:
    - Logged in discount audit trails (who, which card/corporate, what discount, on which invoice/line).
    - Traceable back to Corporate Client, Card, and Policy configuration versions.

---

### 8. Integration Summary

53. **With Billing Module** (`billing.md`):
    - Corporate Clients and Cards serve as billable entities and discount triggers.
    - Credit limit, agreement period, and status influence billing flows and AR posting.
54. **With Discount Management** (`discount-management.md`):
    - Corporate and Card discounts are configured as discount types (`Corporate`, `Health Card`, `Promotional`, etc.).
    - Corporate Card Policies and Card Types constrain maximum discount and service eligibility.
    - Global discount priority and approval rules decide which discount is ultimately applied per invoice line.
55. **With Clinical Chart / Service Catalog** (`clinical-chart-management.md`, `department-category-management.md`):
    - Corporate Service Types and policies bind to defined services (e.g., via Department, HoGroup, or codes).
56. **With Hospital Card Service module** (`hospital-card-management-service`):
    - **Corporate benefit cards**: issuance and reissue create/update card records with owner/corporate linkage, card number, and lifecycle aligned with Module 5.
    - **Printing**: templates and print/reprint actions are provided or invoked via this service (or a dedicated print façade) so all **printed benefit cards** share one technical path.
    - **Other benefit card** programs may register additional card products in the same service without merging business rules with corporate masters.

