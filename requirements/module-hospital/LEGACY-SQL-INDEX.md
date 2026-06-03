# Legacy SQL – Object Index

Quick reference to find tables, views, functions, and procedures in the cleaned legacy SQL files. Use your editor's "Go to line" to jump to definitions.

---

## hms.sql (WebHospitalMDB)

**File:** `hms.sql` (cleaned – database/user setup removed)

### Key Tables by Domain

| Domain | Tables |
|--------|--------|
| **Patient** | PatientRegistration, PatientAppointment, PatientAdmission, PatientLedger, PatientReleaseMaster |
| **Billing** | SalesLedger, PatientLedger, PharSalesLedger, CashReturn, AmbulanceLedger, OPDTicketMaster, RegistrationFees |
| **Pharmacy** | PharProductInfo, PharSalesMaster, PharStockLedger, PharSalesLedger, PharStockInMain |
| **Lab** | InvMaster, InvDetails, ClinicalChart, Lab_ChannelDefination, lab_Parameter_Definition |
| **Indoor** | IndoorPrescriptionMaster, IndoorMedicineReqMaster, CommonMedicine, CommonInvestigations |
| **Bed** | BedInformation |
| **Doctor** | DrInfo |
| **Fixed Assets** | FixedAssetRegister, FixedAssetStockLedger |
| **Blood Bank** | DonorInfo |
| **Ambulance** | AmbulanceMaster, AmbulanceLedger |
| **Main Store** | tbl_STOCK_IN_MAIN, tbl_STOCK_LEDGER, tbl_ITEM | 
| **Discount** | DiscountCardRegistration, DiscountCardRegMain |

### Key Functions (Business Logic)

| Function | Purpose |
|----------|---------|
| `fn_IndoorDueAmount` | Patient due = PatientLedger + SalesLedger + PharSalesLedger |
| `fnc_CrossDefaultValue` | Lab cross-matching default values |
| `FN_CalculateDepreciation`, `FN_CalculateDepreciationNew` | Fixed asset depreciation |
| `fnc_DiagInvName`, `fnc_DiagXrayRpt` | Diagnostic report naming |
| `fnc_IndoorPressComplain`, `fnc_IndoorPressMedicine` | Indoor prescription details |

**Search:** Use `CREATE TABLE` or `CREATE FUNCTION` in `hms.sql` to find definitions.

---

## lab.sql (LabDb)

**File:** `lab.sql` (cleaned – database/user setup removed)

### Key Tables by Domain

| Domain | Tables |
|--------|--------|
| **Lab Order/Invoice** | tb_InvMaster, tb_InvDetails |
| **Parameter Master** | tb_Parameter_Definition, Channeldefination |
| **Machine/Result** | tb_MachineDataMaster, tb_MachineDataDtls, tb_LabSampleStatusInfo |
| **Vacutainer** | tb_VaqGroup, tb_VacutainerSetup, tb_Group |
| **Microbiology** | tb_MachineDataDtls_MicroMaster, tb_MachineDataDtls_MicroDetail |
| **ICU/NICU** | ICUDischargeCertificateHistory, NICUDischargeCertificateHistory, NICUTransferNote |
| **OPD** | OPDTicketLedger, OPDTicketDueCollection |

### Key Views

| View | Purpose |
|------|---------|
| VW_LABTESTMAPPING | Lab test mapping |
| VW_GET_LAB_REPORT_VIEW | Lab report view |
| VW_Sample_Process_Tracking | Sample status tracking |
| V_VitrosChannelMapping | Vitros analyzer mapping |

**Search:** Use `CREATE TABLE` or `CREATE VIEW` in `lab.sql` to find definitions.

---

## Cleanup Script

Run `cleanup-legacy-sql.ps1` to regenerate cleaned files from originals (if you restore from backup):

```powershell
cd requirements/module-hospital
.\cleanup-legacy-sql.ps1
```

To replace originals with cleaned versions:

```powershell
Move-Item hms-clean.sql hms.sql -Force
Move-Item lab-clean.sql lab.sql -Force
```
