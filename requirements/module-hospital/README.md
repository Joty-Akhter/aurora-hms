# Hospital Module - Requirements Documentation

This directory contains the comprehensive requirements documentation for the Hospital Module. The documentation has been organized into separate files for better maintainability and easier navigation.

## Documentation Structure

### Core Requirements

1. **[Introduction](introduction.md)**
   - Purpose, scope, and objectives
   - Stakeholders and document structure
   - Standards, regulations, and definitions
   - Document conventions

2. **[Patient Health Records](patient-health-records.md)**
   - Patient registration and demographics
   - **[Patient identity card – registration](patient-identity-card-registration.md)** — card issuance and print when registering a new patient
   - Medical history management
   - Vital signs and clinical measurements
   - Clinical notes and documentation
   - Diagnoses and problem lists
   - Laboratory results
   - Imaging and diagnostic studies
   - Allergies and adverse reactions
   - Medications (current and historical)
   - Viewing and access capabilities

3. **[Prescription Management](prescription-management.md)**
   - Electronic prescription creation and transmission
   - Drug interaction and allergy checking
   - Prescription refill management
   - Controlled substances handling
   - Pharmacy integration
   - Prescription history and tracking

4. **[Non-Functional Requirements](non-functional-requirements.md)**
   - Security requirements
   - Performance requirements
   - Compliance and quality requirements
   - Usability requirements

5. **[Technical Requirements](technical-requirements.md)**
   - System architecture
   - Database requirements
   - Integration specifications
   - Standards and protocols

6. **[Integration with Accounting, Inventory, and HR Services](integration-services.md)**
   - Data flows between Hospital module and existing platform services
   - Configuration requirements and integration patterns
   - Summary matrix and non-functional considerations

7. **[Data Models and Relationships](data-models.md)**
   - Entity relationships
   - Data structure definitions
   - Data standards and coding

### Supporting Documentation

8. **[User Stories](user-stories.md)**
   - User-centric requirements in story format
   - Organized by feature area

9. **[Acceptance Criteria](acceptance-criteria.md)**
   - Measurable criteria for feature completion
   - Testing and validation requirements

### Hospital-Specific Features

10. **[Doctor Module](doctor-module.md)**
   - Department management
   - Doctor registration and management
   - Doctor scheduling and attendance management

11. **[Admission – IPD Patient Admission](admission-ipd.md)**
    - IPD patient admission workflow
    - Bed assignment and guardian capture
    - Doctor, corporate, and integration rules with billing and clinical modules

12. **[Nurse Module – IPD Nursing & Bedside Operations](nurse-module.md)**
    - Lab and medicine requisitions and returns for admitted patients
    - Nursing follow-up, vitals tracking, and bed status monitoring
    - Doctor visit entry and integration with IPD billing and EHR

13. **[OT Procedure Entry – Operation Theatre (IPD)](ot-procedure-entry-ipd.md)**
    - OT scheduling, room allocation, and operation time tracking
    - Multi-doctor roles (surgeon/assistants) with doctor-wise payment tagging
    - OT charge calculation, IPD billing linkage, and OT utilization reporting

14. **[Procedure Entry – IPD Procedures & Services](procedure-entry-ipd.md)**
    - Admission-wise capture of medical, surgical, and service procedures
    - Per-procedure doctor tagging and per-unit charging
    - Direct integration with IPD billing, discounts, and audit trail

15. **[Clinical Chart Management – Chargeable Items Master](clinical-chart-management.md)**
    - Central master for all chargeable clinical items, canteen items, and packages
    - Controls rates, fixed/variable behavior, department mapping, and discount groups
    - Shared prerequisite for Procedure Entry, Doctor Visit, OT Entry, Billing, and reporting

16. **[Department / Category Management – Service Hierarchy](department-category-management.md)**
    - Defines department, sub-department, sub-sub-department, and head-group hierarchy
    - Provides classification backbone for Clinical Chart, billing, discounts, and reporting
    - Ensures every service/test/procedure is correctly mapped and reportable

17. **[Hospital card programs – overview](hospital-card-types-overview.md)**
    - Taxonomy: patient identity card, staff/employee identity card, IPD temporary visitor card, printed corporate/other benefit cards (via Hospital Card Service), optional wallet/prepaid cards
    - Scope boundaries and links to detailed specifications (replaces a single undifferentiated “hospital card” model)

18. **[Patient identity card – registration](patient-identity-card-registration.md)**
    - Issuance when registering a new patient: MRN-linked card, print/reprint, replacement, optional fee
    - Integration with registration workflow; distinct from IPD visitor cards and wallet/prepaid cards
    - **[Patient identity card – implementation plan](patient-identity-card-implementation-plan.md)** — phased implementation; **Phase 1 = patient card first** (hospital-service + Hospital Card Service + frontend)

19. **[Staff / employee identity card](staff-identity-card.md)**
    - Facility credential for hospital personnel: employee id, department, photo, lifecycle (issue, suspend, revoke, replace)
    - HR/staff master integration; distinct from patient cards and IPD visitor cards

20. **[Temporary Card Service – IPD Visitor Cards](temporary-card-service-ipd.md)**
    - Fee-based visitor/attendee card issuance and return tracking for IPD patients
    - Card number generation, status (issued/returned/lost), and payment/refund integration
    - Role-based access for front desk, nursing, admin, and accounts

21. **[Billing](billing.md)**
    - OPD and IPD billing console
    - Discounts, approvals, credit and corporate billing
    - Integration with Accounts, Pharmacy, and EHR

22. **[Pharmacy](pharmacy.md)**
    - Medicine master and stock management
    - Service boundaries vs `hospital-pharmacy-service` / main store / billing (IDs, fulfillment channels)
    - Optional prescription linkage (e.g. walk-in, regional validation limits); POS thermal + PDF receipts; stock override when system shows no stock (audited)
    - OPD sales and IPD issues; line-level fulfillment states, substitution, billing lifecycle
    - Returns, adjustments, and pharmacy reporting
    - **[Pharmacy gaps & new requirements — implementation plan](pharmacy-gaps-implementation-plan.md)** — backlog to close requirements vs code (billing, fill sync, line states, override, receipts, safety, events)

23. **[Hospital Operations](hospital-operations.md)**
    - User management and permissions
    - System settings and master configurations
    - Dashboards, marketing/B2B, canteen, and reporting
    - EHR integration into hospital workflows

24. **[Portal](portal.md)**
    - Patient self-service portal
    - Doctor portal for schedules and tasks
    - B2B/corporate portal for utilization and financials

25. **[Certificate Management](certificate-management.md)**
    - Birth, death, discharge, and transfer certificate workflows
    - Standardized content, approvals, and printing
    - Audit trails and legal/statutory support

26. **[LAB & Diagnostic – Patient-ID Wise Process](lab-diagnostic-module.md)**
    - Patient-ID centric model for all lab and diagnostic activities
    - End-to-end sample lifecycle: order, barcode, collection, dispatch, receive, processing, and reporting
    - Microbiology (growth/no-growth, organism and sensitivity) and basic imaging (USG & X-Ray) workflows
    - Mandatory system controls, audit trails, and patient-wise lab history

27. **[Blood Bank](blood-bank.md)**
    - Donor registration, blood collection, grouping, screening, and component management
    - Cross-matching and blood issue to patient
    - Traceability, stock, expiry, and Lab/Billing integration

28. **[Fixed Assets](fixed-assets.md)**
    - Asset register, depreciation (Straight Line, Diminishing Balance)
    - Asset transfer, disposal, and Accounting service integration

29. **[Ambulance](ambulance.md)**
    - Ambulance trip booking, charge calculation, ledger, and due collection
    - Integration with Billing, Patient, and Hospital Operations (Driver/Vehicle config)

30. **[Canteen Module – Food & Meal Services](canteen-module-srs.md)**
    - Hospital canteen operations, including menu, stock, and raw materials
    - Indoor vs outdoor sales, meal distribution, and staff/patient packages
    - Integration with Billing, Patient, Staff/HR, and Inventory modules
    - See also: **[Canteen Module – Implementation Plan](canteen-module-implementation-plan.md)** for architecture, submodules, and phased delivery plan

31. **[Discount Management](discount-management.md)**
    - Central discount configuration, assignment, and application rules for OPD, IPD, Pharmacy, Canteen, and other services
    - Priority-based discount selection, approval workflows, and accounting impact
    - Reporting and audit requirements for all discount activity

32. **[Corporate Service & Card Management](corporate-service-and-card-management.md)**
    - Corporate client, service type, card type, policy, and card issuance configuration
    - **Printed corporate benefit cards** for offices/beneficiaries; generation and print via **Hospital Card Service**; other printed benefit cards may share the same pipeline
    - Corporate/card-based eligibility and discount rules integrated with Billing and Discount Management
    - Governance for card lifecycle, policy validity, and corporate billing controls

### Future Planning

10. **[Future Enhancements](future-enhancements.md)**
    - Features planned for future releases
    - Patient-facing features
    - Clinical features
    - Administrative features
    - Integration features
    - Population health features

### Reference

11. **[Glossary](glossary.md)**
    - Definitions of key terms and acronyms
    - Healthcare terminology
    - Regulatory and compliance terms
    - Technical and standards terms
    - Clinical terms

## Service Modularization (Hospital Module)

The Hospital Module is implemented as a set of focused microservices, all following a `hospital-*` naming convention. The target modularization (kept under 10 services, with one optional) is:

1. **`hospital-service` (existing, expanded slightly)**
   - **Scope**:
     - Core EHR: encounters, diagnoses, problems, allergies, notes.
     - Prescription: medication orders from clinicians.
     - Visits/encounters: OP/IP/ED visit entities, linking to billing and scheduling.
     - Patient registration (basic): patient demographics, MRN, contact, basic consents.
   - **Why**: This is the core clinical record and is already live; we avoid splitting it immediately.

2. **`hospital-scheduling-service`**
   - **Scope**:
     - Outpatient appointments, inpatient admissions/discharges/transfers.
     - Doctor/room/bed schedules.
     - Waitlists, rescheduling, cancellations.
   - **Why**: Scheduling is a high-change, high-traffic domain worth its own service.

3. **`hospital-clinical-orders-service`**
   - **Scope**:
     - Lab, radiology, and procedure orders.
     - Result/status lifecycle for lab and radiology (metadata, links to PACS/LIS/RIS).
     - Clinical worklists for test/procedure execution.
   - **Why**: Instead of separate lab/radiology services, all orderable clinical services are grouped in one place.

4. **`hospital-pharmacy-service`**
   - **Scope**:
     - Drug catalog and formulary rules.
     - Pharmacy stock at counters, dispensing, returns.
     - Fulfillment of prescriptions from `hospital-service`.
   - **Why**: Medication safety and stock handling benefit from a focused service, without further splitting.

5. **`hospital-billing-service`**
   - **Scope**:
     - Charge capture for visits, orders, canteen, packages.
     - Invoices, payments, refunds, outstanding dues.
     - Application of discounts (from rules in its own module or shared with corporate).
   - **Why**: Financial integrity is critical; billing is kept as a standalone service.

6. **`hospital-corporate-and-discount-service`**
   - **Scope**:
     - Corporate/B2B clients and contracts.
     - Coverage rules, packages, tariffs per corporate.
     - Discount rules and approval/audit.
   - **Why**: Corporate logic and discounts are tightly related; combining them avoids two small, highly coupled services.

7. **`hospital-card-management-service`**
   - **Scope**:
     - Cards for patients/corporates (RFID/QR/etc.).
     - Card lifecycle: issue, block, replace.
     - Balances/limits, and transaction views across hospital and canteen.
   - **Why**: Card logic is shared infrastructure for billing and canteen, so it is implemented as a reusable service.

8. **`hospital-canteen-service`**
   - **Scope**:
     - Menus, pricing, canteen orders.
     - Integration with `hospital-card-management-service` and `hospital-billing-service`.
     - Optional inpatient meal plans (linked to visits from `hospital-service`).
   - **Why**: A clear domain attached to, but not identical with, clinical flows.

9. **`hospital-portal-bff-service`**
   - **Scope**:
     - Backend-for-frontend (BFF) façade for patient, doctor, and corporate portals.
     - Aggregates from: `hospital-service`, `hospital-scheduling-service`, `hospital-billing-service`, `hospital-corporate-and-discount-service`, `hospital-canteen-service`, `hospital-card-management-service`.
   - **Why**: Keeps UI-specific composition and orchestration out of core domain services.

10. **(Optional) `hospital-masterdata-service` (only if needed)**
    - **Scope**:
      - Departments, wards, rooms, generic procedure/service catalogs, etc., if they are not managed by a platform-wide master data service.
    - **Why**: If there is already a platform-wide master data service, this can be skipped, keeping the Hospital Module at 9 services total.

## Quick Navigation

- **Getting Started**: Start with [Introduction](introduction.md) for an overview of the system
- **Implementation Plan**: See [Hospital Module – Implementation Plan](module-hospital-implementation-plan.md) for domains, submodules, and phased delivery
- **Core Features**: Review [Patient Health Records](patient-health-records.md) and [Prescription Management](prescription-management.md)
- **Technical Details**: See [Technical Requirements](technical-requirements.md) and [Data Models](data-models.md)
- **Hospital Operations**: Check [Doctor Module](doctor-module.md), [Billing](billing.md), [Pharmacy](pharmacy.md), [Hospital Operations](hospital-operations.md), and [Portal](portal.md) for hospital-specific features
- **Reference**: Use [Glossary](glossary.md) for terminology definitions

## Document History

This documentation was originally maintained as a single large file (`ehr.md`). It has been split into multiple focused files to improve maintainability and organization as the Hospital Module expanded beyond EHR functionality to include additional hospital management features.

## Related Documentation

- For implementation plans and technical specifications, refer to other documentation in the `requirements/module-hospital/` directory
- For system architecture and design documents, refer to the main project documentation
- **Legacy reference:** `hms.sql` and `lab.sql` are cleaned schema-only exports for gap analysis; we are not migrating the legacy schema. See [LEGACY-SQL-INDEX.md](LEGACY-SQL-INDEX.md) for object index and [LEGACY-SQL-REVIEW-AND-REQUIREMENTS-PLAN.md](LEGACY-SQL-REVIEW-AND-REQUIREMENTS-PLAN.md) for the review plan.
