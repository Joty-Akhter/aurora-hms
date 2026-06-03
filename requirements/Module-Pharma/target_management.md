# Target Management & Setting

## 📋 Overview

The Target Management system handles area-wise target setting for pharmaceutical sales operations. Targets are set for areas (not individual employees) and assigned to managers responsible for those areas. Targets are set twice a year for 6-month periods, with the same monthly target applying to each month in the period.

### Key Objectives
- **Area-Based Targets**: Targets always set area-wise
- **Manager Assignment**: Targets assigned to Managers (AM, TM, Sr.AM, etc.) for their areas
- **6-Month Periods**: Targets set twice a year for 6-month periods
- **Fixed Monthly Target**: Same monthly target applies to each month in the period
- **Target Coverage Tracking**: Track target achievement (Covered Amount vs Target Amount)

---

## 🎯 Target Setting Process

### Target Entry Process

#### Step 1: Area Selection
- **Area**: User selects area name
  - Area selected from territory hierarchy (Division > Region > Territory > Area)
  - System displays full hierarchy path
  - Area must be active

#### Step 2: Employee (Manager) Selection
- **Employee Name**: User selects/enters employee name
  - Employee must be assigned to the selected area
  - Target is assigned to the employee (manager) for the area
  - Employee should typically be a Manager role (AM, TM, Sr.AM, RM, Sr.RM, DSM, etc.)
  - Multiple managers can be assigned to same area

#### Step 3: Period Information
- **Year**: User enters/selects year
- **Month Range**: User enters/selects month range
  - **Start Month**: User selects start month
  - **End Month**: User selects end month
  - Example: January 2026 to June 2026 (6-month period)
  - Typical period: 6 months (twice a year)

#### Step 4: Target Amount
- **Target Amount**: User enters/inserts target amount (in Taka)
  - Monthly target amount for the area
  - Target is area-based but assigned to the employee (manager)
  - Same target amount applies to each month in the period

### Target Entry Example

```
Target Entry:
Area: Dhaka Metro North
Employee Name: AM-001 (Manager Name)
Year: 2026
Month Range: January 2026 to June 2026
Target Amount: 100,000 Taka per month

Total Target for Period: 100,000 × 6 = 600,000 Taka
Monthly Target: 100,000 Taka (same for each month)
```

---

## 📊 Target Structure & Rules

### Area-Based Targets

**Key Principle**: Targets are always set area-wise.

**Business Rules:**
1. **Area as Unit**: All targets are for areas (not individual employees)
2. **All Employees Work Towards Same Target**: All employees in the area work towards the same area target
3. **Manager Assignment**: Target assigned to Manager for management purposes
4. **Multiple Managers**: Multiple managers can be in same area; all work towards same area target
5. **Manager with Multiple Areas**: If manager manages multiple areas, each area has its own target

### Target Period

**Period Structure:**
- **Frequency**: Targets set twice a year
- **Period Length**: Typically 6 months
- **Month Range**: User selects start month and end month
- **Fixed Monthly Target**: Same monthly target applies to all months in the period

**Example Periods:**
- Period 1: January to June (6 months)
- Period 2: July to December (6 months)

### Target Assignment

**Assignment Rules:**
1. **Manager Assignment**: Targets assigned to Managers (AM, TM, Sr.AM, RM, Sr.RM, DSM, etc.)
2. **Employee Must Belong to Area**: Employee must be assigned to the selected area
3. **Target is for Area**: Target is for the area, not individual employee
4. **Multiple Managers**: Multiple managers can be assigned to same area target
5. **Head Office Sets Targets**: Targets set by Head Office employees (roles TBD)

### Target Independence

**Important**: Targets are independent of allocations.

**Business Rules:**
1. **No Relationship with Allocations**: There is **no relationship** between target and disbursed/allocated amount to an area
2. **Target is Management-Set**: Targets are set by management (Head Office)
3. **Allocation Can Exceed Target**: Allocations/disbursements can be larger than the target; there is **no issue**
4. **Target is Performance Metric**: Target is used for performance measurement and incentive calculation

---

## 📈 Target Coverage Calculation

### Coverage Calculation

**Formula:**
```
Target Coverage = (Covered Amount / Target Amount) × 100%
```

**Where:**
- **Target Amount**: Monthly target amount for the area
- **Covered Amount**: Total money deposited/recorded for the area in the month

### Coverage Status

**Status Types:**
1. **Target Achieved**: Covered Amount ≥ Target Amount
2. **Target Not Achieved**: Covered Amount < Target Amount
3. **Target Exceeded**: Covered Amount > Target Amount

### Monthly Coverage Tracking

**Tracking Process:**
1. **Monthly Target**: Same target amount applies to each month in the period
2. **Monthly Coverage**: Calculate coverage for each month separately
3. **Period Coverage**: Calculate overall coverage for the entire period
4. **Coverage Reporting**: Report coverage by month and by period

---

## 🔄 Target Management Workflows

### Target Setting Workflow
```
1. Head Office User Initiates Target Entry
   ↓
2. Select Area
   ↓
3. Select Manager (Employee assigned to area)
   ↓
4. Select Year
   ↓
5. Select Month Range (Start Month to End Month)
   ↓
6. Enter Target Amount (Monthly)
   ↓
7. System Validates:
   - Area is active
   - Employee assigned to area
   - Month range is valid
   - Target amount > 0
   ↓
8. Save Target
   ↓
9. Target Active for All Months in Period
```

### Target Coverage Calculation Workflow
```
1. Month End
   ↓
2. System Retrieves:
   - Monthly Target Amount for Area
   - Covered Amount for Area (from deposits)
   ↓
3. Calculate Coverage:
   - Coverage = (Covered / Target) × 100%
   - Status = Achieved/Not Achieved/Exceeded
   ↓
4. Update Target Coverage Record
   ↓
5. Available for Reporting and Incentive Calculation
```

### Target Update Workflow
```
1. Head Office User Initiates Target Update
   ↓
2. Select Existing Target
   ↓
3. Modify Target Amount or Period
   ↓
4. System Validates Changes
   ↓
5. System Maintains History:
   - Previous target values
   - Change date and user
   ↓
6. Update Target
   ↓
7. Recalculate Coverage (if needed)
```

---

## 📊 Data Model

### Target Table
```sql
- target_id (PK)
- area_id (FK)
- employee_id (FK) -- Manager assigned to target
- year
- start_month
- end_month
- target_amount (Monthly target amount)
- status (Active/Inactive/Completed)
- created_date
- created_by
- updated_date
- updated_by
```

### Target Coverage Table (Monthly)
```sql
- coverage_id (PK)
- target_id (FK)
- area_id (FK)
- year
- month
- target_amount (Monthly target)
- covered_amount (Total deposits in month)
- coverage_percentage (Covered / Target × 100)
- status (Achieved/Not Achieved/Exceeded)
- last_updated
```

### Target History Table (Audit)
```sql
- history_id (PK)
- target_id (FK)
- change_type (Created/Updated/Deleted)
- old_value
- new_value
- changed_date
- changed_by
- notes
```

**Indexes:**
- Index on (area_id, year, month) for area-based queries
- Index on (employee_id, year) for manager-based queries
- Index on (year, month) for period-based queries

---

## ✅ Business Rules & Validation

### Target Setting Rules
1. **Area-Based**: Targets always set area-wise
2. **Manager Assignment**: Target assigned to Manager (employee) for the area
3. **Period Validation**: Month range must be valid (start ≤ end, within same year or consecutive)
4. **Target Amount**: Target amount must be > 0
5. **Active Area**: Area must be active
6. **Employee Assignment**: Employee must be assigned to selected area
7. **Period Overlap**: System should warn if target period overlaps with existing target (business decision)

### Target Coverage Rules
1. **Monthly Calculation**: Coverage calculated monthly
2. **Covered Amount Source**: Covered amount from deposits (area-wise)
3. **Target Amount Source**: Target amount from target setting
4. **Coverage Percentage**: Coverage = (Covered / Target) × 100%
5. **Status Determination**: Status based on coverage percentage

### Target Update Rules
1. **Head Office Only**: Only Head Office users can set/update targets
2. **History Maintenance**: All target changes must be logged
3. **Impact on Incentives**: Target changes may affect incentive calculations (recalculate if needed)
4. **Period Changes**: Changing period may require recalculation of coverage

---

## 📈 Reporting Requirements

### Target Reports
1. **Target Setting Report**: All targets by area, period, manager
2. **Target Summary Report**: Target summary by area, month, year
3. **Target vs Allocation Report**: Target vs actual allocations (for reference)
4. **Target History Report**: Target change history

### Coverage Reports
1. **Monthly Coverage Report**: Coverage by area, month
2. **Period Coverage Report**: Overall coverage for 6-month periods
3. **Coverage Trend Report**: Coverage trends over time
4. **Target Achievement Report**: Areas that achieved/exceeded targets

### Performance Reports
1. **Area Performance Report**: Target achievement by area
2. **Manager Performance Report**: Target achievement by manager
3. **Territory Performance Report**: Target achievement by territory level
4. **Target Achievement Ranking**: Ranking of areas by achievement percentage

---

## 🔐 Security & Access Control

### Access Permissions
- **Target Setting**: Head Office users only (roles TBD)
- **Target Update**: Head Office users only (roles TBD)
- **Target View**: Head Office users, managers, area managers
- **Coverage Reports**: Head Office users, managers

### Data Entry Responsibility
- **All Target Setting**: Head Office employees only
- **All Target Updates**: Head Office employees only
- **Coverage Calculation**: System performs automatic calculation

---

## 🎯 Success Criteria

### Functional Success
- ✅ Accurate target setting and management
- ✅ Correct target coverage calculation
- ✅ Proper period management (6-month periods)
- ✅ Multiple manager support
- ✅ Target history tracking

### Technical Success
- ✅ Fast target entry (< 2 seconds)
- ✅ Accurate coverage calculations
- ✅ Data integrity maintained
- ✅ Audit trail for all changes

### Business Success
- ✅ All areas have targets set
- ✅ Targets properly assigned to managers
- ✅ Accurate coverage tracking
- ✅ Comprehensive reporting

---

## 🔗 Integration Points

### Integration with Other Modules
1. **Deposit Module**: Covered amount from deposits used for coverage calculation
2. **Incentive Module**: Target coverage used for incentive eligibility
3. **Expense Module**: Target amount used for expense limit calculation (30% of target)
4. **Reporting Module**: Target data used in performance reports

---

**Document Status**: Complete Requirements  
**Related Documents**: 
- [Territory & Area Management](territory_area_management.md)
- [Sales Force Collection & Deposit](sales_force_collection_deposit.md)
- [Incentive System](incentive_system.md)

