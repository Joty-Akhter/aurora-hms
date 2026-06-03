## Clinical Tariff Coverage – Derived from `clinical.txt`

This document links the real clinical tariff file `clinical.txt` to the abstract model defined in `clinical-chart-management.md` and `department-category-management.md`. It describes the **families of services** and **mapping expectations** that the Hospital Module must support.

> Source: `requirements/module-hospital/clinical.txt` – hospital Clinical Chart export (several thousand rows).

---

### 1. Column Mapping Expectations

The following columns from `clinical.txt` must map into the Clinical Chart master:

- **SL** → Internal serial (display/sorting only).
- **Code** → Clinical Chart `Code` (primary item code, unique).
- **Investigation Name** → Clinical Chart `Investigation / Procedure Name`.
- **Rate** → Clinical Chart `Rate` (base rate).
- **Ref / Fee / Fix / Les** → Used to drive reference/fee behavior and the `Fix` flag as defined in `clinical-chart-management.md`.
- **RepoName** → High-level logical grouping (e.g., `Canteen`, `DENTAL`, `LabTest`, `Ambulance`, `Health Checkup`, `DIAGNOSTIC`).
- **Department / SubDeptName / SubSubDeptName** → Hierarchy defined in `department-category-management.md`.
- **HoGroup** → Clinical Chart `HoGroup` / HGroup master.
- **AfRate** → Clinical Chart `AfRate` (effective billing rate).

The system must provide an import/synchronization process that:

- Validates mandatory fields (`Code`, `Investigation / Procedure Name`, `Rate`, `Department`).
- Ensures `Code` uniqueness (no duplicates, including inactive items).
- Logs and reports rejected/invalid rows for manual correction.

---

### 2. Service Families Present in `clinical.txt`

The current `clinical.txt` includes at least the following **service families**. The Clinical Chart, Department hierarchy, Billing, and Reporting must all handle these correctly.

- **Canteen / Dietary**
  - Examples (from `clinical.txt`): `Canteen Bill`, `Summer Package`, `Lunch-1-(staff)`, `Lunch-2-(staff)`, `Lunch-1-(Regular)`, `Dinner-1-(Regular)` etc.
  - Mapping:
    - `RepoName = Canteen`
    - `Department = Canteen`
    - `SubDeptName = Canteen`
    - `HoGroup` to distinguish **Staff vs Regular vs Package**.

- **Covid / Special Lab Tests**
  - Examples: `COVID-19 Antigen Test (Govt. Rate)`, `Covid-19 Antigen` (hospital rate).
  - Mapping:
    - `RepoName = LabTest`
    - `Department` such as `Covid` / `Diagnostic` as per configuration.
    - `HoGroup` separates **Govt tariff vs Hospital tariff**.

- **Dental / Oral & Maxillofacial**
  - Examples include:
    - Periodontal & surgeries: `Deep Cleaning / Deep curettage`, `Periodontal / Gum Surgery / Gingivectomy`, `Ds Ginsivectomy`, `DD - Gingivectomy`.
    - Restorative: `Amalgum filling`, `GI (Autocure)`, `GI Restoration (Fuji II)`, `Light Cure Filling`, `Composite Filling`, `Pit & Fissure Sealant`.
    - Endodontics: `Pulpectomy`, `Root Planning`, multiple `Ds Root Filling` variants, `Single visit RCT`, `Revised RCT`.
    - Prosthodontics: `Porcelain Bridge`, `Porcelain Cap`, `All Ceramic Crown`, `Noble Bio-Care`, `Zirconia Prosthesis`, `Complete Denture (Heat Cure/Flexible)`, `Flexible denture (per unit)`.
    - Orthodontics: `Fixed Braces (Regular)`, `Removable Appliance`, `Myobrace`, `Orthodontic Followup`, `Re-Treatment (Orthodontic)`.
    - Minor oral surgery & trauma: `Surgical Extraction` variants, `Fracture Of Mandible`, `Fracture Of Maxilla`, `Abscess Drainage`, `Cyst Enucleation`.
  - Mapping:
    - `RepoName = DENTAL`
    - `Department = DENTAL` / `Dental`
    - `SubDeptName` such as `Dental`, `OPG` (for dental X‑ray items).

- **Ambulance Services**
  - Examples: `Ambulance Rent - 4(Big)`, multiple `Ambulance Charge` rows with different rates (e.g., 500, 700, 1,000, 1,700, 2,000).
  - Mapping:
    - `RepoName = Ambulance`
    - `Department` / `SubDeptName` under the configured Ambulance or Diagnostic service area.
    - `HoGroup` for ambulance revenue/discount grouping.

- **Collection / Service Charges**
  - Examples: `Covid Sample Collection Charge`, `FNAC Collection Charge`, `Home Collection Charge`, `Online Report Delivery Charge`, `Corporate B2B Sample Collection`, `Hospital Fee`, `Dr. Reprt Fee`.
  - Mapping:
    - `RepoName = Collection`
    - `Department = Diagnostic` (or equivalent).
    - `SubDeptName = Collection Charge`.

- **Diagnostic – Lab Tests**
  - Biochemistry examples: `Peritoneal fluid ADA`, `S. Creatinine`, `Liver Function Test (LFT)`, `DHEAS`, `24 hrs. Phosphate`, multiple fluid/sugar/protein tests.
  - Immunology examples: `Anti Hsv-IgG, IgM`, `IL-6 (Interleukin-6)`, `Factor IX Assay`, `Fecal Calprotectin`, `Lupus Anticoagulant`.
  - Hematology examples: `Cir. Eosinophil`, `Serum Protein Electrophoresis (SPEP)` and other blood tests.
  - Microbiology examples: `Sputum For C/S`, `Sputum For Gram Stain`, `Peritoneal fluid for M/E`, `HVS For Gram Stain`.
  - Mapping:
    - `RepoName = DIAGNOSTIC` or `LabTest`
    - `Department = Diagnostic`
    - `SubDeptName` values: `Biochemistry`, `Immunology`, `Hematology`, `Microbiology`.

- **Diagnostic – Imaging / X‑Ray / Dental OPG**
  - Examples: `XD DENTAL OPG DIGITAL`, `XD OCCLUSAL DIGITAL`, `XD SINGLE TOOTH`, `XD CEPHALOMETRY DIGITAL`, `XD PA VIEW`, `Cervical Spine RT.Lat View (Digital)`.
  - Mapping:
    - `Department = Diagnostic`
    - `SubDeptName = X-ray` / `OPG` / other imaging areas.
    - `RepoName` as imaging/diagnostic.

- **Health Checkup / Packages**
  - Examples: `Executive Check-Up (EC)`, `Dengue Package-01`, `Circumcision (LA)`, `Circumcision (GA)`, consultation fees under Health Checkup.
  - Mapping:
    - `RepoName = Health Checkup`
    - `Department` configured as `Health Checkup` / `Diagnostic`.
    - `HoGroup` for executive packages, disease-specific packages, and standalone services.

> The above list is illustrative: the system must support **all** rows in `clinical.txt` through the generic field model in `clinical-chart-management.md`, not only the examples listed here.

---

### 3. Volume & Performance Requirements

- `clinical.txt` currently contains several thousand rows.
- Clinical Chart UI and APIs must:
  - Load and search items by **Code**, **Name (partial)**, **Department/SubDept**, and **HoGroup** with acceptable response times at this scale.
  - Support pagination and server-side filtering for admin/master-data screens.

---

### 4. Import, Update & Governance

- There must be an **admin-only** process to:
  - Perform initial bulk import from `clinical.txt` (or equivalent CSV/Excel).
  - Periodically re-import revised tariff files and:
    - Update existing items matched by `Code`.
    - Create new items for new codes.
    - Optionally flag items missing from the latest file for review/inactivation.
- All imports/updates derived from `clinical.txt` must be:
  - **Audited** (who, when, file reference, summary of changes).
  - Validated against the Department hierarchy (`department-category-management.md`).

