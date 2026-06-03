# Legacy SQL Review & Requirements Gap Analysis

**Document Purpose:** Use `hms.sql` and `lab.sql` as reference to improve requirements and identify major gaps. We are **not** migrating everything from the old system—only using it to ensure we don't miss important capabilities.

**Date:** March 12, 2026

---

## 1. Executive Summary

Two legacy SQL Server database exports have been added as **reference material**:

| File | Database | Size | Purpose |
|------|----------|------|---------|
| **hms.sql** | WebHospitalMDB | ~5.1 MB | Full hospital management system |
| **lab.sql** | LabDb | ~214 KB | Standalone laboratory information system |

**How we use these:** Compare legacy capabilities against current requirements to find what's missing or under-specified. The goal is **requirements completeness**, not schema migration.

---

## 2. Legacy Schema Overview

### 2.1 hms.sql (WebHospitalMDB)

**Database characteristics:**
- SQL Server 2016+ (compatibility level 120)
- Full export: CREATE DATABASE, users, functions, procedures, triggers, tables
- ~100 CREATE objects (tables, views, functions, procedures)
- Windows-specific users (RAKIBD\Guest, MCSERVER\Guest) – remove for portability

**Domain areas covered:**

| Domain | Key Tables (examples) | Status in Requirements |
|--------|------------------------|------------------------|
| **Patient & Registration** | PatientRegistration, PatientAppointment, PatientAdmission, PatientLedger | Partially covered (patient-health-records, admission-ipd) |
| **Billing** | SalesLedger, PatientLedger, PharSalesLedger, CashReturn, AmbulanceLedger | Partially covered (billing.md) |
| **Pharmacy** | PharProductInfo, PharSalesMaster, PharStockLedger, PharSalesLedger | Partially covered (pharmacy.md) |
| **Lab/Diagnostics** | InvMaster, InvDetails, ClinicalChart, Lab_ChannelDefination, lab_Parameter_Definition | Partially covered (lab-diagnostic-module.md) |
| **Indoor/IPD** | IndoorPrescriptionMaster, IndoorMedicineReqMaster, PatientReleaseMaster | Covered (prescription-management, nurse-module) |
| **Bed Management** | BedInformation | Covered (admission-ipd) |
| **Doctor** | DrInfo | Covered (doctor-module) |
| **Fixed Assets** | FixedAssetRegister, FixedAssetStockLedger | **In scope – add requirements** |
| **HR/Payroll** | HREmployeeMaster, Payregister, tbl_PAYROLL, tbl_Employee | **Integrate with existing HR service** |
| **Main Store/Inventory** | tbl_STOCK_IN_MAIN, tbl_STOCK_LEDGER, tbl_ITEM | **Integrate with existing Inventory service** |
| **Accounts/Finance** | tbl_CHART_OF_ACCOUNTS, tbl_TRANSACT | **Integrate with existing Accounting service** |
| **Blood Bank** | DonorInfo | **In scope – add requirements** |
| **Ambulance** | AmbulanceMaster, AmbulanceLedger | **In scope – add requirements** |
| **Student/Education** | tbl_StudentAdmission, tbl_StudentFeesCollection | **Out of scope** |
| **Discount Cards** | DiscountCardRegistration, DiscountCardRegMain | **In scope – add to billing** |
| **OPD** | OPDTicketMaster, RegistrationFees | Partially covered (billing) |

**Notable functions (business logic to extract):**
- `FN_CalculateDepreciation`, `FN_CalculateDepreciationNew` – Fixed asset depreciation
- `fn_IndoorDueAmount` – Patient due calculation (PatientLedger + SalesLedger + PharSalesLedger)
- `fnc_CrossDefaultValue` – Lab cross-matching defaults
- `fnc_DiagInvName`, `fnc_DiagXrayRpt` – Diagnostic report naming
- `fnc_IndoorPressComplain`, `fnc_IndoorPressMedicine` – Indoor prescription details

### 2.2 lab.sql (LabDb)

**Database characteristics:**
- SQL Server 2008+ (compatibility level 100)
- Separate database from main HMS
- ~50 tables; many views for reporting and workflow

**Domain areas covered:**

| Domain | Key Tables | Status in Requirements |
|--------|------------|------------------------|
| **Lab Invoice/Order** | tb_InvMaster, tb_InvDetails | Covered (lab-diagnostic-module) |
| **Parameter Master** | tb_Parameter_Definition, Channeldefination | Covered |
| **Machine/Analyzer** | tb_MachineDataMaster, tb_MachineDataDtls, tb_MachineSetup | Covered (analyzer integration) |
| **Sample Tracking** | tb_LabSampleStatusInfo, tb_SampleStatusInfo | Covered (sample lifecycle) |
| **Vacutainer** | tb_VaqGroup, tb_VacutainerSetup, tb_Group | Covered (vacutainer setup) |
| **Microbiology** | tb_MachineDataDtls_MicroMaster, tb_MachineDataDtls_MicroDetail | Covered |
| **ICU/NICU** | ICUDischargeCertificateHistory, NICUDischargeCertificateHistory, NICUTransferNote | **Not covered** (certificate-management) |
| **OPD Ticket** | OPDTicketLedger, OPDTicketDueCollection | Partially covered |
| **Imaging** | A_Imaging_Report, A_HeaderView | Covered (imaging basics) |
| **Vitros Integration** | tb_VitrosOrder, V_VitrosChannelMapping | Covered (analyzer) |

**Overlap with hms.sql:**
- LabDb appears to be a separate LIS implementation; some tables (InvMaster, InvDetails) have similar naming but different structure in hms.sql (InvMaster, InvDetails) vs lab.sql (tb_InvMaster, tb_InvDetails).
- LabDb may have been used standalone or integrated with HMS via PatientId/BranchId.

---

## 3. Major Gaps – What Might We Be Missing?

### 3.1 Domains in legacy but not in requirements

| Domain | In Legacy | In Requirements? | Scope decision |
|--------|-----------|------------------|----------------|
| **Blood Bank** | DonorInfo | No | **In scope** – add requirements |
| **Fixed Assets** | FixedAssetRegister, depreciation logic | No | **In scope** – add requirements |
| **Ambulance** | AmbulanceMaster, AmbulanceLedger | No | **In scope** – add requirements |
| **Discount Cards** | DiscountCardRegistration | billing exists; discount cards not explicit | **In scope** – add to billing |
| **ICU/NICU Certificates** | ICUDischargeCertificateHistory, NICUTransferNote | certificate-management exists but no ICU/NICU specifics | **In scope** – add to certificate-management |
| **OPD Ticket Ledger** | OPDTicketMaster, OPDTicketLedger | billing partially covers OPD | **In scope** – ensure OPD flow specified |
| **HR/Payroll** | HREmployeeMaster, Payregister | No | **In scope** – integrate with existing HR service |
| **Accounts/Finance (GL)** | tbl_CHART_OF_ACCOUNTS, tbl_TRANSACT | No | **In scope** – integrate with existing Accounting service |
| **Main Store/Inventory** | tbl_STOCK_IN_MAIN, tbl_STOCK_LEDGER | hospital-main-store | **In scope** – integrate with existing Inventory service |
| **Student/Education** | tbl_StudentAdmission | No | **Out of scope** – not required |

> **Note:** Accounting, Inventory, and HR services already exist in the platform. Hospital module will integrate with these rather than duplicate functionality.

### 3.2 Business logic that may be missing from requirements

| Logic | Legacy source | Question for requirements |
|-------|---------------|---------------------------|
| **Indoor patient due amount** | `fn_IndoorDueAmount` – sums PatientLedger + SalesLedger + PharSalesLedger | Does billing.md specify how we calculate total patient due across clinical + pharmacy? |
| **Lab cross-matching defaults** | `fnc_CrossDefaultValue` | Is blood bank / cross-matching in scope? If yes, add. |
| **Diagnostic report naming** | `fnc_DiagInvName`, `fnc_DiagXrayRpt` | Does lab-diagnostic-module specify how report headers/names are built? |
| **Pharmacy include flag** | `PharmacyInclude` in tbl_ComInfo used in due calc | Is pharmacy bill included in patient due by default? Configurable? |

### 3.3 Workflows to verify

- **Sample lifecycle** – Lab has detailed status tracking (CollStatus, SendStatus, ReceiveInLabStatus, ReportPrintStatus, etc.). Does lab-diagnostic-module cover all these states?
- **Microbiology** – Organism, ColonyCount, Incubation, Sensitivity. Does lab-diagnostic-module cover culture + sensitivity fully?
- **Vacutainer mapping** – Test → Container mapping. Is this explicit in requirements?

---

## 4. Recommended Actions (Requirements Improvement Only)

### High priority – Add missing requirements

1. **New requirement docs** – Blood Bank, Fixed Assets, Ambulance need dedicated requirements (or sections in existing docs).
2. **Integration with existing services** – HR, Accounts, Inventory already exist; document how hospital module integrates with them.

### Medium priority – Fill gaps in existing docs

2. **billing.md** – Add or clarify:
   - How is total patient due calculated? (clinical + pharmacy aggregation)
   - OPD ticket flow and due collection (if in scope)
   - Discount cards (if in scope)

3. **certificate-management.md** – Add ICU/NICU discharge and transfer certificates if in scope.

4. **lab-diagnostic-module.md** – Verify:
   - Sample status flow matches legacy (collection → send → receive → process → print → deliver)
   - Microbiology organism + sensitivity structure
   - Vacutainer/test mapping

### Low priority – Reference only

5. **README.md** – Add short note: "Legacy reference: hms.sql and lab.sql are kept for gap analysis; we are not migrating the legacy schema."

---

## 5. SQL File Handling

| Action | Recommendation |
|--------|----------------|
| **Keep hms.sql, lab.sql** | Yes – as reference for gap analysis. |
| **Use for migration?** | No – we are not migrating everything; use only to improve requirements. |

---

## 6. Implementation Checklist

- [x] Make scope decisions for Blood Bank, Fixed Assets, Ambulance, HR, Accounts, Student, Discount Cards *(Student: out of scope; others: in scope; HR, Accounts, Inventory: integrate with existing services)*
- [x] Add requirements for Blood Bank, Fixed Assets, Ambulance (new or update existing docs)
- [x] Update billing.md with indoor due calculation, OPD ticket flow, discount cards
- [x] Update certificate-management.md with ICU/NICU certificates
- [x] Document integration points with existing Accounting, Inventory, HR services
- [x] Verify lab-diagnostic-module.md covers sample status flow, microbiology, vacutainer mapping
- [x] Add one-line note to README.md about legacy reference files

---

## 7. Appendix: Legacy Table List (for reference)

### hms.sql – Key tables by domain

**Patient & Registration:** PatientRegistration, PatientAppointment, PatientAdmission, PatientLedger, PatientReleaseMaster  
**Billing:** SalesLedger, PatientLedger, PharSalesLedger, CashReturn, AmbulanceLedger, OPDTicketMaster, RegistrationFees  
**Pharmacy:** PharProductInfo, PharSalesMaster, PharStockLedger, PharSalesLedger  
**Lab:** InvMaster, InvDetails, ClinicalChart, Lab_ChannelDefination, lab_Parameter_Definition  
**Indoor:** IndoorPrescriptionMaster, IndoorMedicineReqMaster, CommonMedicine, CommonInvestigations  
**Bed:** BedInformation  
**Doctor:** DrInfo  
**To add:** FixedAssetRegister (Fixed Assets), DonorInfo (Blood Bank), AmbulanceMaster (Ambulance), DiscountCardRegistration (Discount Cards). **Integrate with existing:** HR, Accounts, Inventory. **Out of scope:** tbl_StudentAdmission (Student)  

### lab.sql – Key tables by domain

**Lab:** tb_InvMaster, tb_InvDetails, tb_Parameter_Definition, tb_MachineDataMaster, tb_MachineDataDtls, tb_LabSampleStatusInfo, tb_VacutainerSetup  
**Microbiology:** tb_MachineDataDtls_MicroMaster, tb_MachineDataDtls_MicroDetail  
**ICU/NICU:** ICUDischargeCertificateHistory, NICUDischargeCertificateHistory, NICUTransferNote  
**OPD:** OPDTicketLedger, OPDTicketDueCollection  

---

*End of plan.*
