# Incentive System

## 📋 Overview

The Incentive System calculates and distributes incentives to employees based on area-wise target achievement. Incentives are calculated as a percentage of sales (covered amount) and distributed among employees (SRs, MPOs, and Managers) based on predefined rules. The system supports multi-area employee assignments, dual-role employees, and configurable distribution rules.

### Key Objectives
- **Area-Based Incentive Calculation**: Incentives calculated area-wise based on target achievement
- **Eligibility Rules**: All-or-nothing eligibility per area (target achieved AND expenses within limit)
- **Distribution Rules**: Configurable distribution among SRs, MPOs, and Managers
- **Multi-Area Support**: Employees assigned to multiple areas get incentives from all areas
- **Dual-Role Support**: Employees with multiple roles get incentives for all roles

---

## 💰 Incentive Calculation Overview

### Incentive Percentage

**Standard Rule:**
- **Incentive Percentage**: 4% of sales (covered amount) for all areas
- **Configuration**: Stored in system configuration (Head Office managed)
- **Uniform Application**: Same percentage applies to all areas (no area-specific customization currently)

### Incentive Base Amount

**Formula:**
```
Incentive Base Amount = Covered Amount × Incentive Percentage
```

**Where:**
- **Covered Amount**: Total money deposited/recorded for the area in the month
- **Incentive Percentage**: 4% (configurable)

**Example:**
```
Covered Amount: 100,000 Taka
Incentive Percentage: 4%
Incentive Base Amount: 100,000 × 4% = 4,000 Taka
```

---

## ✅ Incentive Eligibility

### Eligibility Criteria

**All-or-Nothing Rule**: Incentive eligibility is determined per area. If the area is eligible, all employees in the area get incentives. If not eligible, no one gets incentives.

**Eligibility Requirements:**
1. **Target Achievement**: Covered Amount ≥ Target Amount
2. **Expense Limit**: Area Expenses ≤ 30% of Target Amount

**Both conditions must be met for the area to be eligible.**

### Eligibility Calculation

**Step 1: Check Target Achievement**
```
If Covered Amount ≥ Target Amount:
    Target Achievement = TRUE
Else:
    Target Achievement = FALSE
```

**Step 2: Check Expense Limit**
```
Expense Limit = Target Amount × 30%
If Area Expenses ≤ Expense Limit:
    Expense Check = TRUE
Else:
    Expense Check = FALSE
```

**Step 3: Determine Eligibility**
```
If Target Achievement = TRUE AND Expense Check = TRUE:
    Area Eligible = TRUE
    Incentive Calculation Proceeds
Else:
    Area Eligible = FALSE
    No Incentive for Area
```

### Eligibility Example

**Example 1: Eligible Area**
```
Target Amount: 100,000 Taka
Covered Amount: 110,000 Taka (≥ Target) ✓
Area Expenses: 25,000 Taka
Expense Limit: 30,000 Taka (30% of Target)
Expense Check: 25,000 ≤ 30,000 ✓

Result: Area Eligible = TRUE
```

**Example 2: Not Eligible (Target Not Achieved)**
```
Target Amount: 100,000 Taka
Covered Amount: 90,000 Taka (< Target) ✗
Area Expenses: 20,000 Taka
Expense Limit: 30,000 Taka

Result: Area Eligible = FALSE (Target not achieved)
```

**Example 3: Not Eligible (Expenses Exceeded)**
```
Target Amount: 100,000 Taka
Covered Amount: 110,000 Taka (≥ Target) ✓
Area Expenses: 35,000 Taka
Expense Limit: 30,000 Taka (30% of Target)
Expense Check: 35,000 > 30,000 ✗

Result: Area Eligible = FALSE (Expenses exceeded limit)
```

---

## 📊 Incentive Distribution Rules

### Standard Distribution Framework

**Distribution Structure:**
1. **SR Share**: 10% of incentive base amount (distributed equally among all SRs)
2. **Remaining 90%**: Split between MPOs and Managers
   - **MPO Share**: 80% of remaining 90%
   - **Manager Share**: 20% of remaining 90%

**Exception Rule:**
- When there is only ONE employee (manager) in an area:
  - SRs get 9% (if any SRs exist)
  - Product Development Department gets 1%
  - Remaining 90% split: MPO 80%, Manager 20%

### Distribution Calculation

#### Step 1: Calculate SR Share
```
SR Share = Incentive Base Amount × 10%
SR Individual Share = SR Share / Number of SRs in Area
```

**Rules:**
- Distributed equally among all SRs in the area
- If no SRs, SR share = 0

#### Step 2: Calculate Remaining Amount
```
Remaining Amount = Incentive Base Amount - SR Share
```

#### Step 3: Calculate MPO Share
```
MPO Share = Remaining Amount × 80%
MPO Individual Share = MPO Share / Number of Eligible MPOs
```

**MPO Eligibility Rules:**
- **MPOs with ≥ 1 year service**: Get full share (equal split among eligible MPOs)
- **MPOs with < 1 year service**: Get reduced amount (smaller than ≥1 year MPOs)
- **Reduced Amount Ratio**: Configurable (Head Office managed)

#### Step 4: Calculate Manager Share
```
Manager Share = Remaining Amount × 20%
Manager Individual Share = Manager Share / Number of Managers in Area
```

**Manager Distribution Rules:**
- Distributed equally among all managers in the area
- All manager types (AM, TM, Sr.AM, RM, Sr.RM, DSM) get equal share
- **Provision for Weightages**: System supports future weightage-based distribution (by designation/seniority)

### Distribution Example

**Example: Standard Distribution**
```
Incentive Base Amount: 4,000 Taka

SRs in Area: 3
MPOs in Area: 2 (both ≥ 1 year)
Managers in Area: 1

Calculation:
1. SR Share = 4,000 × 10% = 400 Taka
   SR Individual = 400 / 3 = 133.33 Taka per SR

2. Remaining = 4,000 - 400 = 3,600 Taka

3. MPO Share = 3,600 × 80% = 2,880 Taka
   MPO Individual = 2,880 / 2 = 1,440 Taka per MPO

4. Manager Share = 3,600 × 20% = 720 Taka
   Manager Individual = 720 / 1 = 720 Taka

Total Distribution: 400 + 2,880 + 720 = 4,000 Taka ✓
```

---

## 👥 Multi-Area Employee Incentives

### Multiple Area Assignment

**Key Feature**: Employees assigned to multiple areas get incentives from all areas (if eligible).

**Business Rules:**
1. **Separate Calculation**: Incentive calculated separately for each area
2. **Cumulative Incentive**: Employee's total incentive = Sum of incentives from all assigned areas
3. **Role-Based**: Employee gets incentive based on their role in each area
4. **Eligibility Per Area**: Each area's eligibility determined independently

### Multi-Area Example

**Employee: AM-001 (Area Manager)**
- Assigned to Area A (as Manager)
- Assigned to Area B (as Manager)
- Assigned to Area C (as Manager)

**Incentive Calculation:**
```
Area A: Eligible, Incentive = 500 Taka (Manager share)
Area B: Eligible, Incentive = 300 Taka (Manager share)
Area C: Not Eligible (target not achieved)

Total Incentive for AM-001 = 500 + 300 + 0 = 800 Taka
```

---

## 🔄 Dual-Role Employee Incentives

### Dual-Role Support

**Key Feature**: Employees with multiple roles get incentives for all roles.

**Business Rules:**
1. **Multiple Roles**: Employee can have multiple roles in same area (e.g., MPO and SR)
2. **Separate Incentive**: Employee gets incentive for each role separately
3. **Cumulative Incentive**: Total incentive = Sum of incentives for all roles

### Dual-Role Example

**Employee: SR-001**
- Role in Area A: MPO (≥ 1 year service)
- Role in Area A: SR (also acts as SR)

**Incentive Calculation:**
```
Area A: Eligible, Incentive Base = 4,000 Taka

As MPO:
- MPO Share = 2,880 Taka (from 2 MPOs, so 1,440 Taka)

As SR:
- SR Share = 400 Taka (from 3 SRs, so 133.33 Taka)

Total Incentive for SR-001 = 1,440 + 133.33 = 1,573.33 Taka
```

---

## 📊 Data Model

### Incentive Calculation Table
```sql
- incentive_id (PK)
- area_id (FK)
- year
- month
- target_amount
- covered_amount
- incentive_base_amount (Covered × 4%)
- target_achieved (Boolean)
- expense_within_limit (Boolean)
- area_eligible (Boolean)
- total_sr_share
- total_mpo_share
- total_manager_share
- total_incentive_distributed
- calculation_date
- calculated_by
- status (Calculated/Paid)
```

### Incentive Distribution Table (Employee-Wise)
```sql
- distribution_id (PK)
- incentive_id (FK)
- employee_id (FK)
- area_id (FK)
- role_in_area (SR/MPO/Manager)
- incentive_amount
- distribution_type (SR Share/MPO Share/Manager Share)
- years_of_service (for MPO eligibility)
- calculation_date
- paid_date (Optional)
- status (Calculated/Paid)
```

### Incentive Configuration Table
```sql
- config_id (PK)
- config_key (e.g., 'incentive_percentage', 'mpo_reduced_ratio')
- config_value
- description
- updated_date
- updated_by
```

**Indexes:**
- Index on (area_id, year, month) for area-based queries
- Index on (employee_id, year, month) for employee-based queries
- Index on calculation_date for date-based queries

---

## 🔄 Incentive Calculation Workflow

### Monthly Incentive Calculation Workflow
```
1. Month End
   ↓
2. For Each Area:
   a. Retrieve Target Amount
   b. Retrieve Covered Amount (from deposits)
   c. Retrieve Area Expenses
   ↓
3. Calculate Eligibility:
   a. Check: Covered ≥ Target?
   b. Check: Expenses ≤ 30% of Target?
   c. Determine: Area Eligible?
   ↓
4. If Area Eligible:
   a. Calculate Incentive Base = Covered × 4%
   b. Identify Employees in Area:
      - Count SRs
      - Count MPOs (with service years)
      - Count Managers
   c. Calculate Distribution:
      - SR Share = Base × 10% / Number of SRs
      - MPO Share = Remaining × 80% / Eligible MPOs
      - Manager Share = Remaining × 20% / Number of Managers
   d. Create Distribution Records
   ↓
5. For Multi-Area Employees:
   a. Sum incentives from all assigned areas
   ↓
6. For Dual-Role Employees:
   a. Sum incentives for all roles
   ↓
7. Generate Incentive Report
   ↓
8. Mark Incentives as Calculated
```

### Incentive Payment Workflow
```
1. Head Office User Reviews Incentive Report
   ↓
2. Approve Incentive Payment
   ↓
3. System Marks Incentives as Paid
   ↓
4. Update Payment Date
   ↓
5. Generate Payment Report
```

---

## ✅ Business Rules & Validation

### Eligibility Rules
1. **All-or-Nothing**: Eligibility determined per area (all employees or none)
2. **Both Conditions**: Target achievement AND expense limit must be met
3. **Monthly Calculation**: Eligibility calculated monthly
4. **Independent Areas**: Each area's eligibility determined independently

### Distribution Rules
1. **Equal Distribution**: SRs and Managers get equal shares (within their group)
2. **MPO Service-Based**: MPOs with < 1 year get reduced amount
3. **Manager Equal Share**: All managers get equal share (provision for weightages)
4. **Exception Handling**: Single employee exception (9% SR, 1% Product Development)

### Multi-Area Rules
1. **Separate Calculation**: Incentive calculated separately for each area
2. **Cumulative Total**: Employee's total = Sum from all areas
3. **Role-Based**: Incentive based on role in each area
4. **Independent Eligibility**: Each area's eligibility independent

### Dual-Role Rules
1. **Multiple Roles**: Employee can have multiple roles in same area
2. **Separate Incentive**: Incentive calculated for each role
3. **Cumulative Total**: Total incentive = Sum for all roles

---

## 📈 Reporting Requirements

### Incentive Reports
1. **Monthly Incentive Report**: Incentives by area, month
2. **Employee Incentive Report**: Incentives by employee
3. **Area Incentive Summary**: Total incentives by area
4. **Incentive Eligibility Report**: Eligibility status by area

### Distribution Reports
1. **Distribution Breakdown Report**: SR/MPO/Manager share breakdown
2. **Multi-Area Employee Report**: Employees with incentives from multiple areas
3. **Dual-Role Employee Report**: Employees with incentives for multiple roles

### Performance Reports
1. **Incentive Achievement Report**: Areas that received incentives
2. **Incentive Trend Report**: Incentive trends over time
3. **Incentive vs Target Report**: Incentive amount vs target achievement

---

## 🔐 Security & Access Control

### Access Permissions
- **Incentive Calculation**: Head Office users only (roles TBD)
- **Incentive Configuration**: Head Office users only (roles TBD)
- **Incentive View**: Head Office users, managers
- **Incentive Reports**: Head Office users, managers

### Data Entry Responsibility
- **All Incentive Calculations**: System performs automatic calculation
- **Incentive Configuration**: Head Office employees only
- **Incentive Payment**: Head Office employees only

---

## 🎯 Success Criteria

### Functional Success
- ✅ Accurate incentive calculation
- ✅ Correct eligibility determination
- ✅ Proper distribution among employees
- ✅ Multi-area employee support
- ✅ Dual-role employee support

### Technical Success
- ✅ Fast calculation (< 5 seconds per area)
- ✅ Accurate calculations
- ✅ Data integrity maintained
- ✅ Audit trail for all calculations

### Business Success
- ✅ 100% accurate incentive calculations
- ✅ Fair distribution among employees
- ✅ Comprehensive reporting
- ✅ Configurable rules

---

## 🔗 Integration Points

### Integration with Other Modules
1. **Target Module**: Target amount and coverage used for eligibility
2. **Deposit Module**: Covered amount used for incentive base calculation
3. **Expense Module**: Area expenses used for eligibility check
4. **Employee Module**: Employee assignments and roles used for distribution
5. **HR Module**: Incentive amounts calculated by Pharma module are passed to HR module for payroll processing and payment

---

**Document Status**: Complete Requirements  
**Related Documents**: 
- [Territory-Specific Incentive Calculation Rules](incentive_calculation_rules_territory_specific.md) — New territory-specific rules (SR 9%, SDM Fund 1%, per-employee allocation)
- [Target Management](target_management.md)
- [Sales Force Collection & Deposit](sales_force_collection_deposit.md)
- [Territory & Area Management](territory_area_management.md)
- [Expense Management](expense_management.md)

