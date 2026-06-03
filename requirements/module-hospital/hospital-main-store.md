## Hospital Main Store – Functional Modules & Processes

### 1. Overview

**Description**  
The Hospital Main Store (Central Store) is the primary non-pharmacy warehouse that manages procurement, storage, and distribution of medicines (bulk), medical consumables, equipment, and general products to all hospital departments, including Pharmacy. It ensures that stock is available at the right place and time, with proper approvals, documentation, and accounting-ready records.

**Objectives**

- Centralize purchasing and stock control for all non-retail inventory.
- Provide transparent, auditable flows for requisitions, approvals, receiving, issues, returns, and adjustments.
- Integrate seamlessly with Pharmacy stores and department-level inventories.
- Support accurate valuation and stock visibility across the hospital.

---

### 2. Master Data / Registration

#### 2.1 Item Registration

- Register all items managed by the Main Store:
  - Medicines (bulk/ward stock).
  - Medical consumables (syringes, gloves, dressings, etc.).
  - Equipment and instruments (where tracked as stock items or capital items).
  - General products (administrative, housekeeping, etc. as per scope).
- Key fields (aligned with Pharmacy Medicine / Product Master where applicable):
  - Item ID (system-generated, unique, read-only).
  - Item Code (optional human-readable code/barcode).
  - Item Name (required, unique per organization).
  - Category / Subcategory (e.g., Medicine, Consumable, Equipment).
  - Unit of Measure (e.g., piece, box, pack, kg).
  - Pack Size and conversion to base unit (where applicable).
  - Reorder Level, Minimum and Maximum Stock.
  - Rate / Valuation method (FIFO/Weighted Average, as per accounting rules).
  - Batch/Expiry Required (Yes/No).
  - Active / Inactive status.

#### 2.2 Supplier Registration

- Register suppliers/vendors who provide items to the Main Store.
- Fields:
  - Supplier ID (system-generated, unique).
  - Supplier Name (required, unique).
  - Contact Details (address, phone, email, contact person).
  - Tax/Registration Numbers (e.g., VAT/GST, License where applicable).
  - Payment Terms (credit days, allowed modes).
  - Status (Active / Inactive).
- Business rules:
  - Only Active suppliers can be used in Purchase Orders and Goods Receipts.
  - Supplier master may be shared with Pharmacy and other purchasing modules.

#### 2.3 Department Information

- Maintain master data for all hospital departments that can:
  - Raise requisitions to the Main Store.
  - Receive items from the Main Store.
  - Return items back to the Main Store.
- Examples: OPD, IPD wards, OT, ICU, Labs, Pharmacy, Admin, Housekeeping, etc.
- Each department is mapped to:
  - Department ID and Name.
  - Cost Center / GL mapping (for valuation and reporting).
  - Default delivery location.

---

### 3. Requisition & Purchase

#### 3.1 Purchase Requisition Creation

- Initiators: Departments and Pharmacy.
- Functionality:
  - Create item-wise requisitions specifying:
    - Requesting department/store.
    - Required items, quantities, required-by date, and remarks.
  - Save as Draft or Submit for approval.
- Business rules:
  - Items must be selected from the Item Registration master.
  - Validation for duplicate open requisitions (as per configuration).

#### 3.2 Purchase Requisition Approval

- Approver roles (e.g., Store Manager, Procurement Officer, Management).
- Actions:
  - Approve fully or partially per line item.
  - Reject line items with mandatory reason.
  - Consolidate multiple requisitions into a single Purchase Order where applicable.
- Audit:
  - Capture approver, date/time, and decisions (approved/rejected quantities, reasons).

#### 3.3 Purchase Order (PO) Creation

- Convert approved requisitions (fully or partially) into Purchase Orders to suppliers.
- Features:
  - Select supplier for each PO (based on rate contracts or manual selection).
  - Include item details, quantities, agreed rates, taxes, and delivery schedule.
  - Support multiple POs from a single requisition when multiple suppliers are involved.
- Business rules:
  - Only approved requisition quantities can be converted to POs (unless exceptional override with justification).
  - PO must reference originating requisition(s) for traceability.

#### 3.4 Goods Receive / Product Receive

- Receive items from suppliers:
  - Against existing POs (standard case).
  - Without PO (exceptional case – see section 6.2).
- Capture details:
  - Supplier, PO reference (if applicable), invoice details, delivery challan.
  - Per-item quantities received, free quantities (if any), batch/expiry (for batch-controlled items), and pricing.
- Business rules:
  - Quantities received should not exceed PO quantities beyond configured tolerance.
  - Differences between ordered and received quantities must be flagged for verification.

---

### 4. Stock Management

#### 4.1 Store Verification

- Verify incoming stock against POs and delivery documents.
- Steps:
  - Match quantities, units, item codes, and prices with the PO.
  - Inspect for damage, expiry, or quality issues.
  - Approve or hold line items pending clarification.
- On verification approval:
  - Confirm Goods Receipt.
  - Update stock levels and valuation in Main Store.

#### 4.2 Stock Adjustment

- Handle changes in stock not linked directly to purchases or issues:
  - Physical stock count discrepancies.
  - Damaged items discovered in store.
  - Expired stock requiring write-off.
- Features:
  - Adjustment reasons configured in master data.
  - Role-based thresholds with approval workflow for high-impact adjustments.
- Audit:
  - Record before and after quantities, reason, user, and approver.

#### 4.3 Stock Transfer Between Stores / Departments

- Transfer stock:
  - Between Main Store and sub-stores (e.g., Secondary Stores, OT stores).
  - Between peer stores (e.g., General store to CSSD store) as per configuration.
- Process:
  - Source store initiates transfer note with items and quantities.
  - Destination store/department receives and confirms quantities.
  - System adjusts stock at both source and destination.
- Business rules:
  - Negative stock is not allowed at any store.
  - All transfers must be traceable with unique document numbers.

#### 4.4 Item-to-Item Product Transfer

- Convert/transfer stock from one item or unit to another, for example:
  - Bulk packs into smaller dispensing packs.
  - Generic “kit” item assembled from individual components (if in scope).
- Requirements:
  - Define conversion ratios (e.g., 1 box = 10 packs, 1 pack = 10 units).
  - Ensure value conservation (total stock valuation remains consistent).
  - Log component deduction and new item addition with full audit trail.

---

### 5. Issue / Distribution

#### 5.1 Department Requisition & Issue Against Requisition

- Departments raise requisitions to Main Store for required items.
- Main Store actions:
  - Review and approve/reject requested quantities.
  - Issue stock against approved requisitions.
- System behavior:
  - Reduce Main Store stock.
  - Optionally increase department/sub-store stock (for departments that maintain stock).
  - Link issues to originating requisition for reporting and audit.

#### 5.2 Issue Against Requisition Without Batch

- For items where batch/expiry tracking is not required (e.g., certain non-medical supplies):
  - Allow simplified issue process without batch selection.
  - Still ensure quantity and valuation tracking at item level.

#### 5.3 Issue Without Requisition (Emergency / Unplanned)

- Support urgent/emergency issues when prior requisition is not possible.
- Features:
  - Quick issue entry with department, item, quantity, and mandatory reason.
  - Post-facto requisition or justification (as per hospital policy).
- Business rules:
  - May require higher-level approval or separate reporting for monitoring.

#### 5.4 Department Product Return

- Departments can return unused or excess items to Main Store.
- Requirements:
  - Validate condition (e.g., unopened, within expiry) as per policy.
  - Increase Main Store stock and optionally reduce department stock.
  - Record return reason (e.g., excess, cancelled procedure, change in plan).

---

### 6. Supplier Interaction & Exceptional Cases

#### 6.1 Product Return to Supplier

- Return damaged, expired, or excess items to suppliers.
- Features:
  - Create Supplier Return Notes referencing original GRN/PO where applicable.
  - Support partial batch returns and full batch returns.
  - Capture reason (e.g., damage on receipt, quality failure, near-expiry return as per agreement).
- Effects:
  - Reduce Main Store stock.
  - Generate accounting impact (credit note, write-off) via integration with Accounts.

#### 6.2 Goods Receive Without PO

- Handle exceptional receipts without a pre-existing PO, for example:
  - Urgent/emergency delivery.
  - Promotional/free-of-charge items.
- Requirements:
  - Mark such receipts clearly as **“Without PO”** with mandatory justification.
  - Enforce separate approval workflow (e.g., Store Manager + Finance).
  - Allow linking to a later-created PO or adjustment document if required.

---

### 7. Integration with Pharmacy

- Pharmacy can function as a department/store that:
  - Raises requisitions to the Main Store for medicines and consumables.
  - Receives and confirms issues from the Main Store.
- Process:
  - Pharmacy creates requisition → Main Store reviews and approves.
  - Main Store issues stock → Main Store stock decreases; Pharmacy stock increases.
- Business rules:
  - Item master should be harmonized with Pharmacy Medicine / Product Master to avoid duplication.
  - All Main Store → Pharmacy transfers must be logged and reportable.

---

### 8. High-Level Process Flow (Textual)

1. **Departments / Pharmacy → Requisition Creation**  
   - Departments and Pharmacy raise requisitions to the Main Store for required items.

2. **Requisition Approval**  
   - Store/Management reviews and approves or rejects requisition lines.

3. **Purchase & Issue / Stock Transfer**  
   - For purchase needs: Approved requisitions generate POs and Goods Receipts.  
   - For internal supply: Approved requisitions generate Issues / Stock Transfers from Main Store.

4. **Product Receive / Return / Adjust**  
   - Main Store receives products from suppliers, departments return products, and stock is adjusted for discrepancies, damage, or expiry.

5. **Supplier Interaction (PO / Return)**  
   - Main Store interacts with suppliers for Purchase Orders, deliveries, and supplier returns, closing the loop between requisitions, receipts, and adjustments.

