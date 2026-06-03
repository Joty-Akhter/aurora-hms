# Data Models and Relationships

### 6.1 Core Entity Relationships

This section provides a comprehensive overview of the core entity relationships within the EHR system. All relationships shall enforce referential integrity through foreign key constraints, and appropriate indexes shall be created on foreign key columns for optimal query performance.

#### 6.1.1 Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CORE ENTITY RELATIONSHIPS                          │
└─────────────────────────────────────────────────────────────────────────────┘

PATIENT (Central Entity)
│
├─── (1) ──< (Many) MEDICAL HISTORY
│    │       └─── Foreign Key: PatientID (Required, Indexed)
│    │       └─── Cascade: Restrict Delete (preserve history)
│    │
├─── (1) ──< (Many) VITAL SIGNS
│    │       └─── Foreign Key: PatientID (Required, Indexed)
│    │       └─── Foreign Key: EncounterID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (1) ──< (Many) CLINICAL NOTES
│    │       └─── Foreign Key: PatientID (Required, Indexed)
│    │       └─── Foreign Key: ProviderID (Required, Indexed)
│    │       └─── Foreign Key: EncounterID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (1) ──< (Many) DIAGNOSES/PROBLEMS
│    │       └─── Foreign Key: PatientID (Required, Indexed)
│    │       └─── Foreign Key: ProviderID (Optional, Indexed)
│    │       └─── Foreign Key: EncounterID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (1) ──< (Many) LAB RESULTS
│    │       └─── Foreign Key: PatientID (Required, Indexed)
│    │       └─── Foreign Key: OrderingProviderID (Optional, Indexed)
│    │       └─── Foreign Key: EncounterID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (1) ──< (Many) IMAGING STUDIES
│    │       └─── Foreign Key: PatientID (Required, Indexed)
│    │       └─── Foreign Key: OrderingProviderID (Optional, Indexed)
│    │       └─── Foreign Key: EncounterID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (1) ──< (Many) ALLERGIES
│    │       └─── Foreign Key: PatientID (Required, Indexed)
│    │       └─── Foreign Key: ReportedByProviderID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete (critical safety data)
│    │
├─── (1) ──< (Many) MEDICATIONS (Medication History)
│    │       └─── Foreign Key: PatientID (Required, Indexed)
│    │       └─── Foreign Key: PrescribedByProviderID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (1) ──< (Many) PRESCRIPTIONS
│    │       └─── Foreign Key: PatientID (Required, Indexed)
│    │       └─── Foreign Key: ProviderID (Required, Indexed)
│    │       └─── Foreign Key: EncounterID (Optional, Indexed)
│    │       └─── Foreign Key: ProblemID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (1) ──< (Many) ENCOUNTERS
│    │       └─── Foreign Key: PatientID (Required, Indexed)
│    │       └─── Foreign Key: ProviderID (Optional, Indexed)
│    │       └─── Foreign Key: LocationID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (Many) >── (1) PRIMARY CARE PROVIDER
│    │       └─── Foreign Key: PrimaryCareProviderID (Optional, Indexed)
│    │       └─── References: Provider.ProviderID
│    │
└─── (Many) >── (1) REGISTRATION LOCATION
         └─── Foreign Key: RegistrationLocationID (Required, Indexed)
         └─── References: Location.LocationID

PRESCRIPTION (Prescription Management)
│
├─── (1) ──< (Many) PRESCRIPTION REFILLS
│    │       └─── Foreign Key: PrescriptionID (Required, Indexed)
│    │       └─── Foreign Key: RequestedByPharmacyID (Optional, Indexed)
│    │       └─── Foreign Key: ApprovedByProviderID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (Many) >──< (Many) DRUG INTERACTIONS
│    │       └─── Junction Table: PrescriptionDrugInteraction
│    │       └─── Foreign Key: PrescriptionID (Required, Indexed)
│    │       └─── Foreign Key: InteractionID (Required, Indexed)
│    │       └─── Cascade: Cascade Delete
│    │
├─── (Many) >── (1) MEDICATION / PRODUCT (Shared Drug Master)
│    │       └─── Foreign Key: MedicationID (Required, Indexed)
│    │       └─── References: MedicineProduct.ProductID (Pharmacy Medicine / Product Master)
│    │       └─── Alternative: NDC Code lookup (where available)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (Many) >── (1) PHARMACY
│    │       └─── Foreign Key: PharmacyID (Optional, Indexed)
│    │       └─── Alternative: PharmacyNPI lookup
│    │       └─── Cascade: Restrict Delete
│    │
├─── (Many) >── (1) PROVIDER (Prescribing Provider)
│    │       └─── Foreign Key: ProviderID (Required, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
└─── (Many) >── (1) PATIENT
         └─── Foreign Key: PatientID (Required, Indexed)
         └─── Cascade: Restrict Delete

PROVIDER (Healthcare Provider)
│
├─── (1) ──< (Many) PRESCRIPTIONS
│    │       └─── Foreign Key: ProviderID (Required, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (1) ──< (Many) CLINICAL NOTES
│    │       └─── Foreign Key: ProviderID (Required, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (1) ──< (Many) DIAGNOSES/PROBLEMS
│    │       └─── Foreign Key: ProviderID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (1) ──< (Many) ENCOUNTERS
│    │       └─── Foreign Key: ProviderID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
└─── (1) ──< (Many) PATIENTS (Primary Care Provider)
         └─── Foreign Key: PrimaryCareProviderID (Optional, Indexed)
         └─── Cascade: Restrict Delete

ENCOUNTER (Patient Visit/Encounter)
│
├─── (1) ──< (Many) VITAL SIGNS
│    │       └─── Foreign Key: EncounterID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (1) ──< (Many) CLINICAL NOTES
│    │       └─── Foreign Key: EncounterID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (1) ──< (Many) PRESCRIPTIONS
│    │       └─── Foreign Key: EncounterID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (1) ──< (Many) DIAGNOSES/PROBLEMS
│    │       └─── Foreign Key: EncounterID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (1) ──< (Many) LAB RESULTS
│    │       └─── Foreign Key: EncounterID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (1) ──< (Many) IMAGING STUDIES
│    │       └─── Foreign Key: EncounterID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (Many) >── (1) PATIENT
│    │       └─── Foreign Key: PatientID (Required, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
├─── (Many) >── (1) PROVIDER
│    │       └─── Foreign Key: ProviderID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
└─── (Many) >── (1) LOCATION
         └─── Foreign Key: LocationID (Optional, Indexed)
         └─── Cascade: Restrict Delete

MEDICATION (Drug Database/Formulary)
│
└─── (1) ──< (Many) PRESCRIPTIONS
         └─── Foreign Key: MedicationID (Optional, Indexed)
         └─── Alternative: NDC Code or RxNorm Code lookup
         └─── Cascade: Restrict Delete

PHARMACY
│
├─── (1) ──< (Many) PRESCRIPTIONS
│    │       └─── Foreign Key: PharmacyID (Optional, Indexed)
│    │       └─── Cascade: Restrict Delete
│    │
└─── (1) ──< (Many) PRESCRIPTION REFILLS
         └─── Foreign Key: RequestedByPharmacyID (Optional, Indexed)
         └─── Cascade: Restrict Delete

DIAGNOSIS/PROBLEM
│
└─── (1) ──< (Many) PRESCRIPTIONS
         └─── Foreign Key: ProblemID (Optional, Indexed)
         └─── Links prescription to treating diagnosis
         └─── Cascade: Restrict Delete
```

#### 6.1.2 Detailed Relationship Specifications

##### 6.1.2.1 Patient-Centric Relationships

**Patient → Medical History (1:Many)**
- **Cardinality**: One Patient can have many Medical History records
- **Foreign Key**: `MedicalHistory.PatientID` → `Patient.PatientID`
- **Required**: Yes (PatientID is required in Medical History)
- **Cascade Rules**: 
  - On Delete: RESTRICT (cannot delete patient with medical history)
  - On Update: CASCADE (update PatientID if patient record is updated)
- **Index**: Required on `MedicalHistory.PatientID`
- **Business Rule**: Medical history records are permanent and should not be deleted when patient is deactivated

**Patient → Vital Signs (1:Many)**
- **Cardinality**: One Patient can have many Vital Signs records
- **Foreign Key**: `VitalSigns.PatientID` → `Patient.PatientID`
- **Required**: Yes (PatientID is required)
- **Optional Foreign Key**: `VitalSigns.EncounterID` → `Encounter.EncounterID` (vital signs may be recorded during encounter or standalone)
- **Cascade Rules**: 
  - On Delete: RESTRICT
  - On Update: CASCADE
- **Indexes**: Required on `VitalSigns.PatientID` and `VitalSigns.EncounterID`
- **Business Rule**: Vital signs are time-stamped and linked to encounters when applicable

**Patient → Clinical Notes (1:Many)**
- **Cardinality**: One Patient can have many Clinical Notes
- **Foreign Key**: `ClinicalNotes.PatientID` → `Patient.PatientID`
- **Required**: Yes (PatientID is required)
- **Required Foreign Key**: `ClinicalNotes.ProviderID` → `Provider.ProviderID` (notes must have author)
- **Optional Foreign Key**: `ClinicalNotes.EncounterID` → `Encounter.EncounterID`
- **Cascade Rules**: 
  - On Delete: RESTRICT
  - On Update: CASCADE
- **Indexes**: Required on `ClinicalNotes.PatientID`, `ClinicalNotes.ProviderID`, and `ClinicalNotes.EncounterID`
- **Business Rule**: Clinical notes require electronic signature and cannot be deleted, only amended

**Patient → Diagnoses/Problems (1:Many)**
- **Cardinality**: One Patient can have many Diagnoses/Problems
- **Foreign Key**: `Diagnosis.PatientID` → `Patient.PatientID`
- **Required**: Yes (PatientID is required)
- **Optional Foreign Key**: `Diagnosis.ProviderID` → `Provider.ProviderID` (diagnosing provider)
- **Optional Foreign Key**: `Diagnosis.EncounterID` → `Encounter.EncounterID` (diagnosis made during encounter)
- **Cascade Rules**: 
  - On Delete: RESTRICT
  - On Update: CASCADE
- **Indexes**: Required on `Diagnosis.PatientID`, `Diagnosis.ProviderID`, and `Diagnosis.EncounterID`
- **Business Rule**: Active diagnoses should be prominently displayed; resolved diagnoses are retained for history

**Patient → Lab Results (1:Many)**
- **Cardinality**: One Patient can have many Lab Results
- **Foreign Key**: `LabResults.PatientID` → `Patient.PatientID`
- **Required**: Yes (PatientID is required)
- **Optional Foreign Key**: `LabResults.OrderingProviderID` → `Provider.ProviderID`
- **Optional Foreign Key**: `LabResults.EncounterID` → `Encounter.EncounterID`
- **Cascade Rules**: 
  - On Delete: RESTRICT
  - On Update: CASCADE
- **Indexes**: Required on `LabResults.PatientID`, `LabResults.OrderingProviderID`, and `LabResults.EncounterID`
- **Business Rule**: Lab results are immutable once finalized; corrections require new result entry

**Patient → Imaging Studies (1:Many)**
- **Cardinality**: One Patient can have many Imaging Studies
- **Foreign Key**: `ImagingStudies.PatientID` → `Patient.PatientID`
- **Required**: Yes (PatientID is required)
- **Optional Foreign Key**: `ImagingStudies.OrderingProviderID` → `Provider.ProviderID`
- **Optional Foreign Key**: `ImagingStudies.EncounterID` → `Encounter.EncounterID`
- **Cascade Rules**: 
  - On Delete: RESTRICT
  - On Update: CASCADE
- **Indexes**: Required on `ImagingStudies.PatientID`, `ImagingStudies.OrderingProviderID`, and `ImagingStudies.EncounterID`
- **Business Rule**: Imaging studies link to external PACS systems; metadata is stored in EHR

**Patient → Allergies (1:Many)**
- **Cardinality**: One Patient can have many Allergies
- **Foreign Key**: `Allergies.PatientID` → `Patient.PatientID`
- **Required**: Yes (PatientID is required)
- **Optional Foreign Key**: `Allergies.ReportedByProviderID` → `Provider.ProviderID`
- **Cascade Rules**: 
  - On Delete: RESTRICT (critical safety data)
  - On Update: CASCADE
- **Indexes**: Required on `Allergies.PatientID` and `Allergies.ReportedByProviderID`
- **Business Rule**: Allergies are critical safety data and must be prominently displayed; active allergies cannot be deleted

**Patient → Medications (Medication History) (1:Many)**
- **Cardinality**: One Patient can have many Medication History records
- **Foreign Key**: `Medications.PatientID` → `Patient.PatientID`
- **Required**: Yes (PatientID is required)
- **Optional Foreign Key**: `Medications.PrescribedByProviderID` → `Provider.ProviderID`
- **Cascade Rules**: 
  - On Delete: RESTRICT
  - On Update: CASCADE
- **Indexes**: Required on `Medications.PatientID` and `Medications.PrescribedByProviderID`
- **Business Rule**: Medication history tracks current and past medications; separate from active prescriptions

**Patient → Prescriptions (1:Many)**
- **Cardinality**: One Patient can have many Prescriptions
- **Foreign Key**: `Prescription.PatientID` → `Patient.PatientID`
- **Required**: Yes (PatientID is required)
- **Cascade Rules**: 
  - On Delete: RESTRICT
  - On Update: CASCADE
- **Indexes**: Required on `Prescription.PatientID`
- **Business Rule**: Prescriptions are linked to patients and cannot be orphaned

**Patient → Encounters (1:Many)**
- **Cardinality**: One Patient can have many Encounters
- **Foreign Key**: `Encounter.PatientID` → `Patient.PatientID`
- **Required**: Yes (PatientID is required)
- **Cascade Rules**: 
  - On Delete: RESTRICT
  - On Update: CASCADE
- **Indexes**: Required on `Encounter.PatientID`
- **Business Rule**: Encounters represent patient visits; all clinical data can be linked to encounters

**Patient → Primary Care Provider (Many:1)**
- **Cardinality**: Many Patients can have one Primary Care Provider
- **Foreign Key**: `Patient.PrimaryCareProviderID` → `Provider.ProviderID`
- **Required**: No (PrimaryCareProviderID is optional)
- **Cascade Rules**: 
  - On Delete: SET NULL (if provider is deleted, set to NULL)
  - On Update: CASCADE
- **Indexes**: Required on `Patient.PrimaryCareProviderID`
- **Business Rule**: Patients may not have an assigned primary care provider

**Patient → Registration Location (Many:1)**
- **Cardinality**: Many Patients can be registered at one Location
- **Foreign Key**: `Patient.RegistrationLocationID` → `Location.LocationID`
- **Required**: Yes (RegistrationLocationID is required)
- **Cascade Rules**: 
  - On Delete: RESTRICT
  - On Update: CASCADE
- **Indexes**: Required on `Patient.RegistrationLocationID`
- **Business Rule**: All patients must have a registration location

##### 6.1.2.2 Prescription-Centric Relationships

**Prescription → Prescription Refills (1:Many)**
- **Cardinality**: One Prescription can have many Refills
- **Foreign Key**: `PrescriptionRefill.PrescriptionID` → `Prescription.PrescriptionID`
- **Required**: Yes (PrescriptionID is required)
- **Optional Foreign Key**: `PrescriptionRefill.RequestedByPharmacyID` → `Pharmacy.PharmacyID`
- **Optional Foreign Key**: `PrescriptionRefill.ApprovedByProviderID` → `Provider.ProviderID`
- **Cascade Rules**: 
  - On Delete: RESTRICT (preserve refill history)
  - On Update: CASCADE
- **Indexes**: Required on `PrescriptionRefill.PrescriptionID`, `PrescriptionRefill.RequestedByPharmacyID`, and `PrescriptionRefill.ApprovedByProviderID`
- **Business Rule**: Refills track the complete lifecycle of prescription renewals

**Prescription ↔ Drug Interactions (Many:Many)**
- **Cardinality**: Many Prescriptions can have many Drug Interactions
- **Implementation**: Junction table `PrescriptionDrugInteraction`
- **Foreign Keys**: 
  - `PrescriptionDrugInteraction.PrescriptionID` → `Prescription.PrescriptionID` (Required)
  - `PrescriptionDrugInteraction.InteractionID` → `DrugInteraction.InteractionID` (Required)
- **Cascade Rules**: 
  - On Delete: CASCADE (if prescription deleted, remove interaction links)
  - On Update: CASCADE
- **Indexes**: Required on both foreign keys; composite unique index on (PrescriptionID, InteractionID)
- **Business Rule**: Drug interactions are checked at prescription creation and stored for audit

**Prescription → Medication (Drug Database) (Many:1)**
- **Cardinality**: Many Prescriptions can reference one Medication
- **Foreign Key**: `Prescription.MedicationID` → `Medication.MedicationID`
- **Required**: No (MedicationID is optional; can use NDC or RxNorm codes)
- **Alternative Lookup**: NDC Code or RxNorm Code (if MedicationID not available)
- **Cascade Rules**: 
  - On Delete: RESTRICT
  - On Update: CASCADE
- **Indexes**: Required on `Prescription.MedicationID`, `Prescription.NDCCode`, and `Prescription.RxNormCode`
- **Business Rule**: Medication reference can be by ID, NDC, or RxNorm code for flexibility

**Prescription → Pharmacy (Many:1)**
- **Cardinality**: Many Prescriptions can be sent to one Pharmacy
- **Foreign Key**: `Prescription.PharmacyID` → `Pharmacy.PharmacyID`
- **Required**: No (PharmacyID is optional; can use PharmacyNPI)
- **Alternative Lookup**: PharmacyNPI (if PharmacyID not available)
- **Cascade Rules**: 
  - On Delete: SET NULL (if pharmacy deleted, set to NULL)
  - On Update: CASCADE
- **Indexes**: Required on `Prescription.PharmacyID` and `Prescription.PharmacyNPI`
- **Business Rule**: Pharmacy can be identified by ID or NPI for interoperability

**Prescription → Provider (Prescribing Provider) (Many:1)**
- **Cardinality**: Many Prescriptions can be written by one Provider
- **Foreign Key**: `Prescription.ProviderID` → `Provider.ProviderID`
- **Required**: Yes (ProviderID is required)
- **Cascade Rules**: 
  - On Delete: RESTRICT
  - On Update: CASCADE
- **Indexes**: Required on `Prescription.ProviderID`
- **Business Rule**: All prescriptions must have a prescribing provider

**Prescription → Patient (Many:1)**
- **Cardinality**: Many Prescriptions can belong to one Patient
- **Foreign Key**: `Prescription.PatientID` → `Patient.PatientID`
- **Required**: Yes (PatientID is required)
- **Cascade Rules**: 
  - On Delete: RESTRICT
  - On Update: CASCADE
- **Indexes**: Required on `Prescription.PatientID`
- **Business Rule**: All prescriptions must be linked to a patient

**Prescription → Encounter (Many:1)**
- **Cardinality**: Many Prescriptions can be created during one Encounter
- **Foreign Key**: `Prescription.EncounterID` → `Encounter.EncounterID`
- **Required**: No (EncounterID is optional; prescriptions can be created outside encounters)
- **Cascade Rules**: 
  - On Delete: SET NULL (if encounter deleted, set to NULL)
  - On Update: CASCADE
- **Indexes**: Required on `Prescription.EncounterID`
- **Business Rule**: Prescriptions can be created during encounters or as standalone orders

**Prescription → Diagnosis/Problem (Many:1)**
- **Cardinality**: Many Prescriptions can treat one Diagnosis/Problem
- **Foreign Key**: `Prescription.ProblemID` → `Diagnosis.ProblemID`
- **Required**: No (ProblemID is optional; clinical indication can be free text)
- **Cascade Rules**: 
  - On Delete: SET NULL (if diagnosis deleted, set to NULL)
  - On Update: CASCADE
- **Indexes**: Required on `Prescription.ProblemID`
- **Business Rule**: Prescriptions can be linked to diagnoses for clinical documentation

##### 6.1.2.3 Provider-Centric Relationships

**Provider → Prescriptions (1:Many)**
- **Cardinality**: One Provider can write many Prescriptions
- **Foreign Key**: `Prescription.ProviderID` → `Provider.ProviderID`
- **Required**: Yes (ProviderID is required in Prescription)
- **Cascade Rules**: 
  - On Delete: RESTRICT (cannot delete provider with active prescriptions)
  - On Update: CASCADE
- **Indexes**: Required on `Prescription.ProviderID`
- **Business Rule**: Provider prescriptions are retained for audit and legal requirements

**Provider → Clinical Notes (1:Many)**
- **Cardinality**: One Provider can author many Clinical Notes
- **Foreign Key**: `ClinicalNotes.ProviderID` → `Provider.ProviderID`
- **Required**: Yes (ProviderID is required in Clinical Notes)
- **Cascade Rules**: 
  - On Delete: RESTRICT
  - On Update: CASCADE
- **Indexes**: Required on `ClinicalNotes.ProviderID`
- **Business Rule**: Clinical notes require provider attribution for legal and clinical purposes

**Provider → Diagnoses/Problems (1:Many)**
- **Cardinality**: One Provider can diagnose many Problems
- **Foreign Key**: `Diagnosis.ProviderID` → `Provider.ProviderID`
- **Required**: No (ProviderID is optional in Diagnosis)
- **Cascade Rules**: 
  - On Delete: SET NULL (if provider deleted, set to NULL)
  - On Update: CASCADE
- **Indexes**: Required on `Diagnosis.ProviderID`
- **Business Rule**: Diagnoses may not always have a specific diagnosing provider

**Provider → Encounters (1:Many)**
- **Cardinality**: One Provider can have many Encounters
- **Foreign Key**: `Encounter.ProviderID` → `Provider.ProviderID`
- **Required**: No (ProviderID is optional in Encounter)
- **Cascade Rules**: 
  - On Delete: SET NULL (if provider deleted, set to NULL)
  - On Update: CASCADE
- **Indexes**: Required on `Encounter.ProviderID`
- **Business Rule**: Encounters may involve multiple providers; primary provider is optional

**Provider → Patients (Primary Care Provider) (1:Many)**
- **Cardinality**: One Provider can be primary care provider for many Patients
- **Foreign Key**: `Patient.PrimaryCareProviderID` → `Provider.ProviderID`
- **Required**: No (PrimaryCareProviderID is optional in Patient)
- **Cascade Rules**: 
  - On Delete: SET NULL (if provider deleted, set to NULL)
  - On Update: CASCADE
- **Indexes**: Required on `Patient.PrimaryCareProviderID`
- **Business Rule**: Patients may not have an assigned primary care provider

##### 6.1.2.4 Encounter-Centric Relationships

**Encounter → Patient (Many:1)**
- **Cardinality**: Many Encounters can belong to one Patient
- **Foreign Key**: `Encounter.PatientID` → `Patient.PatientID`
- **Required**: Yes (PatientID is required)
- **Cascade Rules**: 
  - On Delete: RESTRICT
  - On Update: CASCADE
- **Indexes**: Required on `Encounter.PatientID`
- **Business Rule**: All encounters must be linked to a patient

**Encounter → Provider (Many:1)**
- **Cardinality**: Many Encounters can have one Provider
- **Foreign Key**: `Encounter.ProviderID` → `Provider.ProviderID`
- **Required**: No (ProviderID is optional)
- **Cascade Rules**: 
  - On Delete: SET NULL
  - On Update: CASCADE
- **Indexes**: Required on `Encounter.ProviderID`
- **Business Rule**: Encounters may have multiple providers; primary provider is optional

**Encounter → Location (Many:1)**
- **Cardinality**: Many Encounters can occur at one Location
- **Foreign Key**: `Encounter.LocationID` → `Location.LocationID`
- **Required**: No (LocationID is optional)
- **Cascade Rules**: 
  - On Delete: SET NULL
  - On Update: CASCADE
- **Indexes**: Required on `Encounter.LocationID`
- **Business Rule**: Encounters can occur at various locations (clinic, hospital, telehealth)

**Encounter → Vital Signs (1:Many)**
- **Cardinality**: One Encounter can have many Vital Signs records
- **Foreign Key**: `VitalSigns.EncounterID` → `Encounter.EncounterID`
- **Required**: No (EncounterID is optional; vital signs can be recorded standalone)
- **Cascade Rules**: 
  - On Delete: SET NULL (if encounter deleted, set to NULL)
  - On Update: CASCADE
- **Indexes**: Required on `VitalSigns.EncounterID`
- **Business Rule**: Vital signs can be recorded during encounters or as standalone measurements

**Encounter → Clinical Notes (1:Many)**
- **Cardinality**: One Encounter can have many Clinical Notes
- **Foreign Key**: `ClinicalNotes.EncounterID` → `Encounter.EncounterID`
- **Required**: No (EncounterID is optional; notes can be created outside encounters)
- **Cascade Rules**: 
  - On Delete: SET NULL
  - On Update: CASCADE
- **Indexes**: Required on `ClinicalNotes.EncounterID`
- **Business Rule**: Clinical notes can be encounter-specific or general patient notes

**Encounter → Prescriptions (1:Many)**
- **Cardinality**: One Encounter can generate many Prescriptions
- **Foreign Key**: `Prescription.EncounterID` → `Encounter.EncounterID`
- **Required**: No (EncounterID is optional)
- **Cascade Rules**: 
  - On Delete: SET NULL
  - On Update: CASCADE
- **Indexes**: Required on `Prescription.EncounterID`
- **Business Rule**: Prescriptions can be created during encounters or as standalone orders

**Encounter → Diagnoses/Problems (1:Many)**
- **Cardinality**: One Encounter can result in many Diagnoses
- **Foreign Key**: `Diagnosis.EncounterID` → `Encounter.EncounterID`
- **Required**: No (EncounterID is optional)
- **Cascade Rules**: 
  - On Delete: SET NULL
  - On Update: CASCADE
- **Indexes**: Required on `Diagnosis.EncounterID`
- **Business Rule**: Diagnoses can be made during encounters or added to problem list independently

**Encounter → Lab Results (1:Many)**
- **Cardinality**: One Encounter can order many Lab Results
- **Foreign Key**: `LabResults.EncounterID` → `Encounter.EncounterID`
- **Required**: No (EncounterID is optional)
- **Cascade Rules**: 
  - On Delete: SET NULL
  - On Update: CASCADE
- **Indexes**: Required on `LabResults.EncounterID`
- **Business Rule**: Lab results can be ordered during encounters or as standalone orders

**Encounter → Imaging Studies (1:Many)**
- **Cardinality**: One Encounter can order many Imaging Studies
- **Foreign Key**: `ImagingStudies.EncounterID` → `Encounter.EncounterID`
- **Required**: No (EncounterID is optional)
- **Cascade Rules**: 
  - On Delete: SET NULL
  - On Update: CASCADE
- **Indexes**: Required on `ImagingStudies.EncounterID`
- **Business Rule**: Imaging studies can be ordered during encounters or as standalone orders

##### 6.1.2.5 Medication and Pharmacy Relationships

**Medication → Prescriptions (1:Many)**
- **Cardinality**: One Medication can be prescribed in many Prescriptions
- **Foreign Key**: `Prescription.MedicationID` → `Medication.MedicationID`
- **Required**: No (MedicationID is optional; can use NDC or RxNorm codes)
- **Cascade Rules**: 
  - On Delete: RESTRICT
  - On Update: CASCADE
- **Indexes**: Required on `Prescription.MedicationID`
- **Business Rule**: Medication database serves as formulary reference; prescriptions can reference by ID, NDC, or RxNorm

**Pharmacy → Prescriptions (1:Many)**
- **Cardinality**: One Pharmacy can receive many Prescriptions
- **Foreign Key**: `Prescription.PharmacyID` → `Pharmacy.PharmacyID`
- **Required**: No (PharmacyID is optional; can use PharmacyNPI)
- **Cascade Rules**: 
  - On Delete: SET NULL
  - On Update: CASCADE
- **Indexes**: Required on `Prescription.PharmacyID`
- **Business Rule**: Pharmacy can be identified by ID or NPI for interoperability with external systems

**Pharmacy → Prescription Refills (1:Many)**
- **Cardinality**: One Pharmacy can request many Prescription Refills
- **Foreign Key**: `PrescriptionRefill.RequestedByPharmacyID` → `Pharmacy.PharmacyID`
- **Required**: No (RequestedByPharmacyID is optional)
- **Cascade Rules**: 
  - On Delete: SET NULL
  - On Update: CASCADE
- **Indexes**: Required on `PrescriptionRefill.RequestedByPharmacyID`
- **Business Rule**: Refill requests originate from pharmacies and are approved by providers

##### 6.1.2.6 Diagnosis/Problem Relationships

**Diagnosis/Problem → Prescriptions (1:Many)**
- **Cardinality**: One Diagnosis/Problem can be treated by many Prescriptions
- **Foreign Key**: `Prescription.ProblemID` → `Diagnosis.ProblemID`
- **Required**: No (ProblemID is optional)
- **Cascade Rules**: 
  - On Delete: SET NULL
  - On Update: CASCADE
- **Indexes**: Required on `Prescription.ProblemID`
- **Business Rule**: Prescriptions can be linked to diagnoses for clinical documentation and treatment tracking

#### 6.1.3 Relationship Integrity Rules

##### 6.1.3.1 Referential Integrity
- All foreign key relationships shall enforce referential integrity at the database level
- Foreign key constraints shall prevent orphaned records
- Cascade rules shall be defined for each relationship (RESTRICT, CASCADE, SET NULL)
- All foreign key columns shall be indexed for query performance

##### 6.1.3.2 Cascade Delete Rules
- **RESTRICT**: Prevents deletion of parent record if child records exist
  - Applied to: Patient, Provider, Prescription, Medication, Pharmacy (critical entities)
- **CASCADE**: Deletes child records when parent is deleted
  - Applied to: Junction tables (PrescriptionDrugInteraction)
- **SET NULL**: Sets foreign key to NULL when parent is deleted
  - Applied to: Optional relationships (PrimaryCareProvider, Encounter links)

##### 6.1.3.3 Soft Delete Considerations
- Critical entities (Patient, Prescription, Clinical Notes) support soft deletion
- Soft deletion preserves relationships for audit and compliance
- Foreign key constraints work with soft deletion (IsDeleted flag)
- Queries shall filter soft-deleted records by default

##### 6.1.3.4 Relationship Validation
- System shall validate relationship constraints before allowing data modifications
- System shall prevent circular dependencies
- System shall validate optional vs required relationships based on business rules
- System shall enforce relationship cardinality constraints

#### 6.1.4 Index Requirements for Relationships

All foreign key columns shall have indexes created for optimal query performance:

**Patient-Related Indexes:**
- `MedicalHistory.PatientID`
- `VitalSigns.PatientID`
- `ClinicalNotes.PatientID`
- `Diagnosis.PatientID`
- `LabResults.PatientID`
- `ImagingStudies.PatientID`
- `Allergies.PatientID`
- `Medications.PatientID`
- `Prescription.PatientID`
- `Encounter.PatientID`

**Prescription-Related Indexes:**
- `PrescriptionRefill.PrescriptionID`
- `Prescription.ProviderID`
- `Prescription.MedicationID`
- `Prescription.PharmacyID`
- `Prescription.EncounterID`
- `Prescription.ProblemID`

**Provider-Related Indexes:**
- `Prescription.ProviderID`
- `ClinicalNotes.ProviderID`
- `Diagnosis.ProviderID`
- `Encounter.ProviderID`
- `Patient.PrimaryCareProviderID`

**Encounter-Related Indexes:**
- `VitalSigns.EncounterID`
- `ClinicalNotes.EncounterID`
- `Prescription.EncounterID`
- `Diagnosis.EncounterID`
- `LabResults.EncounterID`
- `ImagingStudies.EncounterID`

**Composite Indexes:**
- `(PatientID, EncounterID)` on clinical data tables for encounter-based queries
- `(PatientID, Date)` on time-series data (Vital Signs, Lab Results) for chronological queries
- `(PrescriptionID, InteractionID)` on PrescriptionDrugInteraction junction table

#### 6.1.5 Relationship Query Patterns

The following common query patterns shall be optimized through proper indexing:

1. **Patient Summary Queries**: Retrieve all clinical data for a patient
   - Requires indexes on all PatientID foreign keys
   - Composite indexes on (PatientID, Date) for chronological sorting

2. **Encounter-Based Queries**: Retrieve all data for a specific encounter
   - Requires indexes on all EncounterID foreign keys
   - Composite indexes on (EncounterID, PatientID) for encounter context

3. **Provider Activity Queries**: Retrieve all prescriptions/notes by provider
   - Requires indexes on ProviderID foreign keys
   - Date-based indexes for time-range queries

4. **Prescription Tracking Queries**: Track prescription lifecycle and refills
   - Requires indexes on PrescriptionID in related tables
   - Status and date indexes for filtering

5. **Drug Interaction Queries**: Check interactions for patient medications
   - Requires indexes on MedicationID and junction table indexes
   - Patient medication aggregation queries

### 6.2 Key Indexes Required
- Patient ID on all patient-related tables
- Prescription ID on prescription-related tables
- Date/Time fields for chronological queries
- Provider ID for access control queries
- Medication NDC for drug lookups

### 6.3 Common Fields and Patterns

The following business concepts recur across transactional and master entities. They are described at a conceptual level; the legacy SQL files (`hms.sql`, `lab.sql`) serve as the detailed field reference.

#### 6.3.1 Valid (Soft Delete)

- **Concept**: A boolean flag indicating whether a record is active or logically deleted.
- **Usage**: Records are retained for audit and history; queries filter by `Valid = true` by default.
- **Scope**: Master data (items, services, tariffs), transactional records where retention is required.
- **Business Rule**: Soft-deleted records cannot be modified; reactivation may be supported under defined policies.

#### 6.3.2 BranchId (Multi-Branch)

- **Concept**: Identifies the branch or location where a transaction or record originates.
- **Usage**: Supports multi-branch operations; reporting, stock, and billing are scoped by branch.
- **Scope**: Transactions (bills, collections, receipts), stock movements, cost center allocation.
- **Business Rule**: All transactional records must carry BranchId; master data may be shared or branch-specific as configured.

#### 6.3.3 InputFrom (Transaction Source)

- **Concept**: Indicates the source or context from which a transaction was created.
- **Usage**: Distinguishes collection types (e.g., Collection, Patient Release, Indoor Due Collection, OPD Due Collection).
- **Scope**: Payment/collection records, receipts, and related financial postings.
- **Business Rule**: Required for correct ledger posting and reconciliation; determines which workflow and approval rules apply.

#### 6.3.4 PtStatus (Patient Status)

- **Concept**: Distinguishes patient type or encounter context—typically **OPD** (Outdoor/Outpatient) vs **IPD** (Indoor/Inpatient).
- **Usage**: Drives billing rules, tariff application, and workflow (e.g., OPD ticket vs admission-based billing).
- **Scope**: Bills, collections, encounters, and service entries.
- **Business Rule**: OPD and IPD flows may use different ledgers, approval paths, and due-collection rules.

#### 6.3.5 Corporate Flags

- **Concept**: **CorporateId** links a patient or transaction to a corporate/contract entity; **IsCorporate** indicates corporate billing.
- **Usage**: Corporate patients follow contract tariffs, credit limits, and settlement rules; billing and reporting are segmented.
- **Scope**: Patient master, bills, collections, and service entries.
- **Business Rule**: Corporate billing requires valid CorporateId; discounts and tariffs are governed by contract configuration.

#### 6.3.6 Return Types

- **Concept**: Different return flows are tracked separately for audit and reconciliation:
  - **ReturnAmount** – Total value of returned items/services.
  - **ReturnCash** – Cash refunded to the patient.
  - **ReturnDue** – Amount credited against outstanding dues.
  - **ReturnLess** – Amount written off or adjusted (e.g., partial refund, less-by reason).
- **Usage**: Pharmacy returns, bill cancellations, and refund workflows must distinguish these flows for correct GL posting.
- **Scope**: Return transactions, refund records, and related financial postings.
- **Business Rule**: Each return type maps to specific ledger accounts; ReturnLess may require approval based on configured thresholds.

---

