# Blood Bank Module

## 1. Overview

**Description**  
The Blood Bank Module manages donor registration, blood collection, storage, cross-matching, and issue of blood and blood components to patients. It ensures traceability, safety, and compliance with blood bank regulations.

**Objectives**

- Maintain a register of blood donors with eligibility and deferral tracking.
- Track blood collection, grouping, screening, and component preparation.
- Support cross-matching and compatibility checking before issue.
- Ensure full traceability from donor to recipient.
- Integrate with Lab module for grouping/screening tests and with Billing for charges.

**Scope**

- Donor registration and donor master.
- Blood donation (whole blood, apheresis) and collection records.
- Blood grouping (ABO, Rh) and screening (HIV, HBV, HCV, etc.).
- Blood component management (RBC, FFP, Platelets, Cryoprecipitate).
- Cross-matching and compatibility.
- Blood issue to patient (OPD/IPD) with requisition linkage.
- Stock and expiry management.
- Reporting and audit trail.

---

## 2. Master Data & Configuration

### 2.1 Donor Master

- **Donor ID \*** – System-generated unique identifier.
- **Donor Name \***, **Donor Age \***, **Donor Gender \***
- **Blood Group** – ABO and Rh (A+, B+, O+, AB+, A-, B-, O-, AB-).
- **Contact** – Mobile, Address.
- **Eligibility Status** – Active, Deferred (with reason and deferral end date), Permanent deferral.
- **Last Donation Date** – For minimum interval enforcement (e.g., 56 days for whole blood).
- **Donation Count** – Total donations (for recognition/limits).

### 2.2 Blood Group & Component Master

- **Blood Group Master** – ABO, Rh, and compatibility matrix (who can receive from whom).
- **Component Type Master** – Whole Blood, Packed RBC, FFP, Platelets, Cryoprecipitate, etc.
- **Unit Volume** – Standard volume per unit (e.g., 350 ml whole blood).
- **Expiry Rules** – Shelf life by component (e.g., RBC 35–42 days, FFP 1 year frozen).

### 2.3 Cross-Matching Default Values

- **Default Value Master** – Configurable default results for cross-matching tests (e.g., by test name).
- Used when pre-populating or validating cross-match results.
- Integration with Lab module for test definitions.

### 2.4 Screening Test Master

- Tests required before issue: HIV, HBV, HCV, Syphilis, Malaria (as per local regulations).
- Link to Lab test codes for result integration.
- Positive result → unit quarantined; negative → eligible for issue.

---

## 3. Core Workflows

### 3.1 Donor Registration

- Capture donor demographics, contact, and initial blood group (if known).
- Perform eligibility check (age, weight, hemoglobin, interval since last donation).
- Record deferral reasons if ineligible.
- Generate Donor ID and donor card/number.

### 3.2 Blood Collection

- **Collection Record**
  - Donor ID, Collection Date/Time, Component Type, Unit Number (unique barcode).
  - Volume collected, Anticoagulant used.
  - Phlebotomist, Collection site.
- **Unit Number** – Unique identifier per unit; used for traceability throughout lifecycle.
- Status: Collected → Sent for Grouping/Screening → Grouped/Screened → Available / Quarantined / Discarded.

### 3.3 Grouping & Screening

- **Blood Grouping** – ABO, Rh determination; result linked to unit.
- **Screening** – HIV, HBV, HCV, Syphilis, etc. (as per configuration).
- Results may be entered manually or received from Lab module integration.
- **Positive screening** → Unit status: Quarantined; notify donor per policy.
- **Negative screening** → Unit status: Available (after grouping confirmed).

### 3.4 Component Preparation (Optional)

- Split whole blood into components (RBC, FFP, Platelets, Cryoprecipitate).
- Each component gets child unit number(s) linked to parent unit.
- Expiry and storage conditions per component type.

### 3.5 Cross-Matching

- **Requisition** – Doctor requests blood/component for patient (linked to Patient ID, Admission/Visit).
- **Compatibility Check**
  - Patient blood group vs. available units.
  - Cross-match test (e.g., saline, Coombs) – result: Compatible / Incompatible.
  - Default values for cross-match parameters (from Cross-Matching Default Value master).
- **Reservation** – Reserve unit(s) for patient; status: Reserved.
- **Issue** – On confirmation, issue unit to patient; status: Issued.

### 3.6 Blood Issue to Patient

- **Issue Record**
  - Unit Number, Patient ID, Admission/Visit ID, Requisition reference.
  - Issue Date/Time, Issued By, Receiving location (Ward/OT/Emergency).
  - Component type, Volume.
- **Billing Integration** – Blood bank charges posted to Patient Ledger (OPD/IPD) as configured in Clinical Chart.
- **Traceability** – Full chain: Donor → Collection → Grouping/Screening → Issue → Patient.

### 3.7 Stock & Expiry Management

- **Stock by** – Blood Group, Component Type, Location (if multiple storage points).
- **Expiry Alerts** – Units nearing expiry (e.g., 7 days) highlighted; overdue units blocked from issue.
- **Discard** – Quarantined, expired, or damaged units discarded with reason and audit.

---

## 4. Integration

### 4.1 Lab Module

- Blood grouping and screening tests may be performed in Lab; results flow to Blood Bank.
- Cross-matching test definitions and default values.
- Shared Patient ID for requisition and result linkage.

### 4.2 Billing

- Blood and component charges defined in Clinical Chart.
- Issue to patient triggers billing entry (OPD/IPD as applicable).
- Discount and approval rules as per Billing module.

### 4.3 Patient / Admission

- Requisition linked to Patient ID and Visit/Admission ID.
- Patient blood group from registration or previous records.

---

## 5. Reporting & Audit

- Donor-wise donation history.
- Unit-wise traceability (donor → collection → issue → patient).
- Stock report by group, component, expiry.
- Discard and quarantine report.
- Cross-match and issue audit trail.
- All create, update, and status-change actions logged with user, date/time, and reason where applicable.

---

## 6. Non-Functional & Compliance

- **Data Integrity** – No unit can be issued without valid grouping and screening (negative).
- **Traceability** – Mandatory for regulatory and recall scenarios.
- **Role-Based Access** – Donor registration, collection, grouping, cross-match, issue, discard – separate permissions.
- **Backup & Recovery** – Critical data; regular backups and recovery procedures.
