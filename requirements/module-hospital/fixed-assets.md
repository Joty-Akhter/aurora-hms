# Fixed Assets Module

## 1. Overview

**Description**  
The Fixed Assets Module manages the hospital's fixed asset register, including acquisition, depreciation, transfer, disposal, and reporting. It integrates with the existing Accounting service for financial posting and with Inventory/Store for asset receipt and tracking.

**Objectives**

- Maintain a complete register of fixed assets with unique identification.
- Calculate and post depreciation (Straight Line and Diminishing Balance methods).
- Track asset location, department, and custody.
- Support asset transfer, sale, and disposal with proper accounting.
- Provide depreciation reports and asset valuation for financial statements.

**Scope**

- Asset registration and master data.
- Depreciation calculation and posting.
- Asset transfer (location/department).
- Asset disposal/sale.
- Integration with Accounting service (GL posting).
- Integration with Inventory/Store (for assets received via purchase).

---

## 2. Master Data & Configuration

### 2.1 Asset Type Master

- **Asset Type Code \***, **Asset Type Name \***
- **Category** – Equipment, Furniture, Vehicle, Building, IT Hardware, etc.
- **Default Depreciation Method** – Straight Line, Diminishing Balance.
- **Default Useful Life** – Years or months.
- **Default Depreciation Rate** – Percentage per annum.

### 2.2 Asset Register (Master Record per Asset)

- **Asset Code \*** – Unique identifier (system-generated or manual).
- **Asset Name / Description \***
- **Asset Type** – From Asset Type Master.
- **Quantity** – Typically 1 per asset; batch assets (e.g., furniture set) may have Qty > 1.
- **Unit** – Nos, Set, etc.
- **Purchase Date \***
- **Purchase Price / Total Cost \***
- **Supplier** – Optional; from Supplier Master.
- **Manufacturer** – Name, Model, Serial Number.
- **Location** – Building, Floor, Room (or free text).
- **Department** – Custody department.
- **Tracking Number** – Barcode or asset tag number.
- **Depreciation Method** – Straight Line, Diminishing Balance (override at asset level).
- **Depreciation Rate** – Percentage (override at asset level).
- **Useful Life** – Years (override at asset level).
- **Opening Depreciation** – Accumulated depreciation brought forward (for migrated assets).
- **Residual / Salvage Value** – Optional; value at end of useful life.
- **Status** – Active, Transferred, Sold, Disposed, Written Off.
- **GL Account Codes** – Asset account, Depreciation account, Accumulated depreciation account (for Accounting integration).

### 2.3 Depreciation Configuration

- **Financial Year** – Align with Accounting period.
- **Depreciation Frequency** – Monthly, Quarterly, Annually.
- **Cut-off Date** – Assets purchased before/after cut-off included in period depreciation.
- **Rounding Rules** – Round depreciation amount (e.g., to nearest integer).

---

## 3. Depreciation Methods

### 3.1 Straight Line

- **Formula:** Annual Depreciation = (Cost − Residual Value − Opening Depreciation) × (Rate / 100) × (Months in Period / 12)
- Or: Annual Depreciation = (Cost − Residual Value) / Useful Life (years)
- Depreciation is constant each period until fully depreciated or disposed.

### 3.2 Diminishing Balance (Reducing Balance)

- **Formula:** Depreciation = Carrying Value × (Rate / 100) × (Days in Period / 365)
- Carrying Value = Cost − Accumulated Depreciation − Sales/Disposal adjustments
- Depreciation decreases each period as carrying value reduces.
- Rate is applied to the declining balance.

### 3.3 Depreciation Calculation Rules

- Depreciation starts from the month following purchase (or as per configuration).
- For partial year: prorate by days or months in the period.
- On disposal/sale: stop depreciation from effective date; calculate depreciation for partial period up to disposal date.
- Opening depreciation (for migrated assets) reduces the depreciable base.

---

## 4. Core Workflows

### 4.1 Asset Acquisition

- **From Purchase** – Asset received via Store/Inventory; GRN or purchase invoice triggers asset creation.
- **Manual Entry** – Direct registration for assets not from purchase (e.g., donated, transferred from another entity).
- **Fields Captured** – Asset Code, Name, Type, Purchase Date, Cost, Supplier, Location, Department, Depreciation method and rate.
- **Status** – Active.
- **Accounting** – Post to Asset GL account (debit) and Creditor/Cash (credit) as per Accounting integration rules.

### 4.2 Depreciation Posting

- **Periodic Run** – System calculates depreciation for all active assets for the period (month/quarter/year).
- **Depreciation Ledger** – Each posting creates: Asset Code, Period, Depreciation Amount, Accumulated Depreciation, Carrying Value.
- **Accounting** – Post to Depreciation Expense (debit) and Accumulated Depreciation (credit).
- **Approval** – Depreciation batch may require approval before posting to GL (configurable).
- **Reversal** – Support reversal of incorrect depreciation with audit trail.

### 4.3 Asset Transfer

- **Transfer** – Change of location or department.
- **Fields** – From Location/Department, To Location/Department, Transfer Date, Reason.
- **Audit** – Log transfer with user and date.
- **Depreciation** – Continues; no impact on calculation unless disposal.

### 4.4 Asset Disposal / Sale

- **Disposal Record**
  - Asset Code, Disposal Date, Disposal Type (Sale, Scrap, Donation, Write-off).
  - Sale Value (if sold), Buyer/Recipient.
  - Reason, Approved By.
- **Depreciation** – Calculate and post depreciation for partial period up to disposal date.
- **Accounting** – Remove asset from books: Credit Asset, Debit Accumulated Depreciation, Debit/Credit P&L for gain/loss; if sold, Debit Cash/Receivable.
- **Status** – Update to Sold, Disposed, or Written Off.

### 4.5 Asset Stock Ledger (Optional)

- For hospitals tracking asset movements (issue/return) similar to inventory:
  - In (purchase/receive), Out (issue to department), Return, Transfer.
- Balance quantity and location per asset.
- Integration with Store for assets held in store before issue to department.

---

## 5. Integration

### 5.1 Accounting Service

- **Asset Acquisition** – Post to Asset GL and Creditor/Cash.
- **Depreciation** – Post to Depreciation Expense and Accumulated Depreciation.
- **Disposal** – Reverse asset, post gain/loss.
- **Chart of Accounts** – Asset codes and depreciation accounts configured in Accounting; Fixed Assets module references them.

### 5.2 Inventory / Store

- **Purchase Receipt** – GRN or purchase invoice may trigger asset creation for capital items.
- **Item Master** – Link asset type to store item where applicable (e.g., equipment received as store item, then capitalized).

---

## 6. Reporting

- **Asset Register** – List of all assets with cost, accumulated depreciation, carrying value, location, department.
- **Depreciation Schedule** – Period-wise depreciation by asset, type, department.
- **Asset Movement Report** – Transfers and disposals.
- **Aging Report** – Assets by age (purchase year).
- **Department-wise Asset Summary** – Assets under each department/cost center.
- Export to Excel/PDF for audit and management.

---

## 7. Non-Functional & Audit

- **Audit Trail** – All asset create, update, transfer, disposal, and depreciation postings logged.
- **Role-Based Access** – Asset registration, depreciation run, disposal approval – separate permissions.
- **Data Integrity** – Asset code unique; no duplicate disposal; depreciation cannot exceed cost minus residual.
- **Period Lock** – Depreciation and disposal for closed accounting periods require elevated permission.
