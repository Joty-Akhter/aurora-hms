# Sales Force Collection & Deposit Management

## 📋 Overview

The Sales Force Collection & Deposit Management system handles the complete collection and deposit workflow for pharmaceutical sales operations. Sales Representatives collect money from customers (pharmacies) and deposit it to Head Office. The system tracks these deposits area-wise, manages outstanding quantities, calculates dues, and provides comprehensive reporting.

### Key Objectives
- **Area-Based Deposit Tracking**: All deposits recorded for areas (not employee-specific)
- **Product-Wise Collection**: Track collections product-wise with quantity and amount
- **Due Management**: Calculate and track dues (Supply Amount - Covered Amount)
- **Outstanding Quantity Management**: Track outstanding inventory quantities per product per area
- **Multiple Deposits Support**: Allow multiple deposits per month per area

---

## 💰 Deposit Entry Process

### Process Overview

**Definition**: Deposit entry records amounts paid or given to Head Office by Sales Representatives of an area. Deposits are tracked **area-wise** (not employee-specific). The employee tag is **optional** and used for reference/tracking purposes only.

**Key Concepts:**
- Deposits are **for an area** (not tied to a specific employee)
- Employee tag is **optional** - can be left blank or selected for reference
- Multiple deposits can be recorded per month per area
- All deposits for an area contribute to the area's covered amount
- Product-wise tracking with quantities and amounts

### Detailed Workflow

#### Step 1: Area Selection
- **Area Selection**: User selects an area
  - Deposits are recorded for the selected area
  - All deposits for an area are cumulative
  - System displays area hierarchy path (Division > Region > Territory > Area)

#### Step 2: Employee Tag (Optional)
- **Employee Search (Optional)**: User can optionally search for Employee name based on the selected area
  - **Important**: Employee tag is **OPTIONAL**
  - If provided, system displays list of employees assigned to the selected area
  - User can select an employee for reference/tracking purposes
  - If not provided, deposit is recorded for the area only (no employee tag)
  - Employee tag does not affect deposit amount or area calculations

#### Step 3: Deposit Header Information

**Required Fields:**
- **Amount**: User enters total deposit amount (amount paid/given to Head Office)
- **Bank Name**: User selects bank name from dropdown/list
- **Bank Account Number**: User enters/inserts bank account number
- **Date**: User enters/inserts deposit date
  - **Year**: User enters/selects year
  - **Month**: User enters/selects month
  - **Date**: User enters/selects specific date

**Note**: Deposit represents money paid/given to Head Office by Sales Representatives of the selected area.

#### Step 4: Product Selection & Entry

**Option A: Product-Wise Entry (Manual Selection)**

- **Product Selection**: User selects product names (multiple products can be selected)
- **For Each Product:**
  - **Product Name**: User selects from product list
  - **TP (Trade Price)**: System displays TP with VAT (read-only, auto-filled from product master)
  - **Quantity**: User enters/inserts quantity of product sold
  - **Current Outstanding Quantity**: System displays automatically
    - Shows remaining quantity that SR has for this product
    - Calculated from: Previous opening + Allocated - Previously sold
  - **Product Amount**: System calculates and displays automatically
    - Formula: Product Amount = Quantity × TP with VAT
    - Displayed immediately after quantity is entered

**Product Entry Process:**
- User can add multiple products in a single deposit entry
- Each product shows:
  - Product name (user selected)
  - TP with VAT (auto-filled, displayed)
  - Quantity (user entered)
  - Current outstanding quantity (auto-displayed)
  - Product amount (auto-calculated)
- System displays running total as products are added

**Option B: Full Product List (Area-Wise)**

- **Alternative Approach**: System displays full product list for the selected area
- **Product List Display:**
  - Shows all products that have been allocated/disbursed to the area
  - For each product:
    - Product Name
    - TP with VAT (displayed)
    - Current Outstanding Quantity (displayed automatically)
    - Amount Input Field: User enters amount product-wise
- **Amount Entry:**
  - User enters amount for each product (instead of quantity)
  - System can optionally calculate quantity from amount: Quantity = Amount / TP with VAT
  - User can enter amounts for multiple products
- **Total Calculation:**
  - System calculates total amount from all product amounts entered

#### Step 5: Amount Calculation & Validation

**Total Amount Calculation:**
- **For Option A (Product-Wise Entry):**
  - Total Amount = Σ (Product Amount) for all products
  - Formula: Total Amount = Σ (Quantity × TP with VAT) for all products
- **For Option B (Full Product List):**
  - Total Amount = Σ (Amount entered) for all products

**Validation:**
- Total Amount from products should match Deposit Amount entered in header
- System validates: Σ (Product Amounts) = Deposit Amount
- If mismatch, system alerts user to reconcile

#### Step 6: Outstanding Quantity Update

**Current Outstanding Quantity Display:**
- System displays current outstanding quantity for each product
- Shows remaining inventory at SR level for each product
- Updated after deposit entry is saved

**Outstanding Quantity Calculation:**
- Formula: Current Outstanding = Previous Outstanding + Allocated - Sold (including this deposit)
- For each product, system tracks:
  - Opening balance
  - Allocated quantities
  - Sold quantities (from deposits)
  - Current outstanding = Opening + Allocated - Sold

#### Step 7: Submit & Update

**User Review:**
- Area (required)
- Employee name (optional - if provided)
- Deposit amount, bank details, date
- Product-wise quantities and amounts
- Current outstanding quantities
- Total amount

**System Validation:**
- Area must be selected
- All required fields completed (Amount, Bank Name, Bank Account Number, Date)
- Product amounts match deposit amount
- Quantities do not exceed outstanding quantities
- Date is valid
- Employee tag is optional (validation skipped if not provided)

**Upon Successful Submission:**
- Deposit record created with all details
- **Covered amount updated for the AREA** (not employee-specific)
- Outstanding quantities updated for each product (area-wise)
- Due amount recalculated for the area
- Entry saved with product-wise breakdown
- Employee tag saved if provided (for reference only)

---

## 📊 Due Management

### Due Amount Calculation

**Formula:**
```
Due Amount = Supply Amount - Covered Amount
```

**Where:**
- **Supply Amount**: Total value of products allocated to the area in the month (sum of all disbursement entries' Total Supply Amount)
- **Covered Amount**: Total money deposited/recorded for the area in the month

### Due Scenarios

#### Scenario 1: Full Coverage
```
Supply Amount: 50,000 Taka
Covered Amount: 50,000 Taka
Due Amount: 0 Taka
Status: Fully Covered
```

#### Scenario 2: Partial Coverage
```
Supply Amount: 50,000 Taka
Covered Amount: 40,000 Taka
Due Amount: 10,000 Taka
Status: Partially Covered (Due exists)
```

#### Scenario 3: Over Coverage
```
Supply Amount: 50,000 Taka
Covered Amount: 55,000 Taka
Due Amount: -5,000 Taka (Advance/Overpayment)
Status: Over Covered
```

### Due Handling Rules

1. **Due Carry Forward**: Due amounts carry forward to subsequent months
2. **Due Tracking**: Due tracking maintained per area (not per employee)
3. **Due Payment**: SRs expected to clear dues in subsequent months
4. **Credit Limit Impact**: Due affects credit limit for future allocations/disbursements (business decision)
5. **Due Reporting**: System provides due reports and aging analysis

---

## 📦 Outstanding Quantity Management

### Outstanding Quantity Calculation

**Formula:**
```
Current Outstanding Quantity = Previous Month Opening + Current Month Allocated - Current Month Sold
```

**Per Product Per Area:**
- System tracks outstanding quantity for each product in each area
- Updated after each deposit entry
- Used to validate deposit quantities (cannot exceed outstanding)

### Outstanding Quantity Tracking

**Tracking Components:**
1. **Previous Month Opening**: Closing balance from previous month
2. **Current Month Allocated**: Sum of all allocations/disbursements in current month
3. **Current Month Sold**: Sum of all sold quantities from deposits in current month
4. **Adjustments**: Damage/expiry adjustments reduce outstanding quantities

**Update Triggers:**
- After product disbursement: Outstanding increases
- After deposit entry: Outstanding decreases
- After damage/expiry adjustment: Outstanding decreases

---

## ✅ Business Rules & Validation

### Deposit Entry Rules
1. **Area-Based Deposits**: Deposits are **for an area** (not employee-specific)
2. **Employee Tag (Optional)**: Employee tag is **not required**; if provided, employee must be assigned to selected area
3. **Multiple Deposits**: Multiple deposits allowed per month **per area**
4. **Product Entry**: User can select multiple products; quantities or amounts can be entered
5. **Amount Validation**: Total amount from products must match deposit amount
6. **Quantity Validation**: Quantities cannot exceed outstanding quantities

### Due Management Rules
1. **Due Calculation**: Due = Supply Amount - Covered Amount (area-wise)
2. **Due Carry Forward**: Dues carry forward to subsequent months
3. **Due Tracking**: Dues tracked per area (not per employee)
4. **Overpayment Handling**: Negative dues indicate advance/overpayment

### Outstanding Quantity Rules
1. **Area-Based Tracking**: Outstanding quantities tracked per product per area
2. **Real-Time Updates**: Outstanding quantities updated after each deposit
3. **Validation**: Deposit quantities cannot exceed outstanding quantities
4. **Adjustment Impact**: Damage/expiry adjustments reduce outstanding quantities

---

## 📈 Data Model

### Deposit Entry Table
```sql
- deposit_id (PK)
- area_id (FK)
- employee_id (FK, Optional - for reference only)
- deposit_date
- year
- month
- deposit_amount
- bank_name
- bank_account_number
- total_product_amount (calculated)
- status (Draft/Submitted)
- notes
- created_date
- created_by
- updated_date
- updated_by
```

### Deposit Product Details Table
```sql
- deposit_product_id (PK)
- deposit_id (FK)
- product_id (FK)
- quantity_sold
- product_amount (Quantity × TP with VAT)
- tp_with_vat (denormalized for audit)
- created_date
- created_by
```

### Area Covered Amount Table (Summary)
```sql
- area_id (FK)
- year
- month
- total_covered_amount (Sum of all deposits)
- total_supply_amount (Sum of all allocations)
- due_amount (Supply - Covered)
- last_updated
```

### Area Outstanding Quantity Table (Per Product Per Area)
```sql
- area_product_id (PK)
- area_id (FK)
- product_id (FK)
- year
- month
- previous_month_opening
- current_month_allocated
- current_month_sold
- current_outstanding (Opening + Allocated - Sold)
- last_updated
```

**Indexes:**
- Index on (area_id, year, month) for area-based queries
- Index on (product_id, area_id) for product-wise queries
- Index on deposit_date for date-based queries

---

## 📊 Reporting Requirements

### Deposit Reports
1. **Deposit Summary Report**: Total deposits by area, month, year
2. **Deposit Detail Report**: Detailed deposit entries with product breakdown
3. **Deposit by Employee Report**: Deposits with employee tags (if provided)
4. **Bank-Wise Deposit Report**: Deposits grouped by bank

### Due Reports
1. **Area Due Report**: Due amounts by area
2. **Due Aging Report**: Dues by age (30 days, 60 days, 90+ days)
3. **Due Summary Report**: Total dues across all areas
4. **Due Trend Report**: Due trends over time

### Outstanding Quantity Reports
1. **Product Outstanding Report**: Outstanding quantities by product and area
2. **Area Outstanding Summary**: Total outstanding value by area
3. **Outstanding Aging Report**: Outstanding quantities by age

### Collection Reports
1. **Collection Efficiency Report**: Collection percentage by area
2. **Collection Trend Report**: Collection trends over time
3. **Monthly Collection Report**: Monthly collection summary

---

## 🔄 Workflows

### Deposit Entry Workflow
```
1. User Initiates Deposit Entry
   ↓
2. Select Area
   ↓
3. (Optional) Select Employee for Reference
   ↓
4. Enter Deposit Header (Amount, Bank, Date)
   ↓
5. Add Products (Quantity/Amount)
   ↓
6. System Validates Amount Match
   ↓
7. System Validates Quantities ≤ Outstanding
   ↓
8. Submit Deposit
   ↓
9. System Updates:
   - Covered Amount (Area)
   - Outstanding Quantities (Per Product)
   - Due Amount (Area)
   ↓
10. Deposit Recorded
```

### Due Calculation Workflow
```
1. Month End
   ↓
2. System Calculates:
   - Total Supply Amount (All allocations in month)
   - Total Covered Amount (All deposits in month)
   ↓
3. Calculate Due Amount = Supply - Covered
   ↓
4. Update Area Due Summary
   ↓
5. Carry Forward to Next Month (if due exists)
```

---

## 🔐 Security & Access Control

### Access Permissions
- **Deposit Entry**: Head Office users only (roles TBD)
- **Deposit View**: Head Office users and area managers
- **Due Reports**: Head Office users and management
- **Outstanding Reports**: Head Office users and area managers

### Data Entry Responsibility
- **All Deposit Entries**: Head Office employees only
- **All Validations**: System performs automatic validations
- **All Updates**: System performs automatic updates

---

## 🎯 Success Criteria

### Functional Success
- ✅ Accurate deposit entry and tracking
- ✅ Correct covered amount calculation
- ✅ Accurate due amount calculation
- ✅ Real-time outstanding quantity updates
- ✅ Multiple deposits per month support
- ✅ Product-wise tracking

### Technical Success
- ✅ Fast deposit entry (< 2 seconds)
- ✅ Accurate calculations
- ✅ Data integrity maintained
- ✅ Audit trail for all deposits

### Business Success
- ✅ 100% deposit tracking accuracy
- ✅ Real-time due visibility
- ✅ Accurate outstanding quantity tracking
- ✅ Comprehensive reporting

---

**Document Status**: Complete Requirements  
**Related Documents**: 
- [Territory & Area Management](territory_area_management.md)
- [Target Management](target_management.md)
- [Incentive System](incentive_system.md)

