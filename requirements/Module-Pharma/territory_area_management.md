# Territory & Area Management

## 📋 Overview

The Territory & Area Management system provides a hierarchical structure for organizing sales territories and assigning employees to specific areas. This is the foundation for all operational activities including product allocation, target setting, deposit tracking, expense management, and incentive calculation.

### Key Objectives
- **Hierarchical Territory Structure**: Division > Region > Territory > Area
- **Area as Operational Unit**: All operations are area-based
- **Employee Assignment**: Assign employees to areas with support for multiple area assignments
- **Role-Based Management**: Support for various sales roles (SR, MPO, AM, TM, Sr.AM, RM, Sr.RM, DSM, ASM, SM)

---

## 🏗️ Territory Hierarchy Structure

### Hierarchy Levels

```
Division (Highest Level)
  └── Region
      └── Territory
          └── Area (Lowest Level - Operational Unit)
```

### Hierarchy Details

#### 1. Division
- **Level**: Highest level in hierarchy
- **Scope**: Large geographical or administrative division
- **Example**: Dhaka, Chittagong, Sylhet
- **Management**: Managed by DSM (Divisional Sales Manager)

#### 2. Region
- **Level**: Second level, within Division
- **Scope**: Sub-division of a Division
- **Example**: Dhaka Metro, Dhaka Rural
- **Management**: Managed by RM (Regional Manager) or Sr.RM (Senior Regional Manager)

#### 3. Territory
- **Level**: Third level, within Region
- **Scope**: Sub-region within a Region
- **Example**: North Territory, South Territory
- **Management**: Managed by TM (Territory Manager) or Sr.AM (Senior Area Manager)

#### 4. Area
- **Level**: Lowest level, within Territory
- **Scope**: Operational unit where Sales Representatives work
- **Example**: Dhaka Metro North, Chittagong South
- **Management**: Managed by AM (Area Manager) or TM (Territory Manager)
- **Operational Use**: All operations (allocation, deposits, expenses, targets, incentives) are area-based

---

## 👥 Employee Roles & Hierarchy

### Sales Official Designations (High to Low)

#### 1. SM (Sales Manager)
- **Level**: All Bangladesh (Head Office)
- **Scope**: National level management
- **Location**: Head Office
- **Reports to**: Top Management

#### 2. ASM (Assistant Sales Manager)
- **Level**: All Bangladesh (Head Office)
- **Scope**: National level management support
- **Location**: Head Office
- **Reports to**: SM
- **Status**: Currently vacant (provision for future)

#### 3. DSM (Divisional Sales Manager)
- **Level**: Division level
- **Scope**: Manages entire division
- **Reports to**: SM/ASM
- **Territory Assignment**: Entire Division

#### 4. Sr.RM (Senior Regional Manager)
- **Level**: Region level
- **Scope**: Manages region within division
- **Reports to**: DSM
- **Territory Assignment**: Region(s) within Division

#### 5. RM (Regional Manager)
- **Level**: Region level
- **Scope**: Manages region within division
- **Reports to**: DSM or Sr.RM
- **Territory Assignment**: Region(s) within Division

#### 6. Sr.AM (Senior Area Manager)
- **Level**: Territory/Area level
- **Scope**: Manages territory or multiple areas
- **Reports to**: RM/Sr.RM
- **Territory Assignment**: Territory or multiple Areas

#### 7. TM (Territory Manager)
- **Level**: Territory/Area level
- **Scope**: Manages territory or area
- **Reports to**: RM/Sr.RM
- **Territory Assignment**: Territory or Area(s)

#### 8. AM (Area Manager)
- **Level**: Area level
- **Scope**: Manages specific area
- **Reports to**: TM/Sr.AM/RM
- **Territory Assignment**: Area(s)

#### 9. MPO (Medical Promotion Officer)
- **Level**: Area level
- **Scope**: Field sales support, works in area
- **Reports to**: AM/TM
- **Territory Assignment**: Area(s)

#### 10. SR (Sales Representative)
- **Level**: Area level
- **Scope**: Field sales, direct customer interaction
- **Reports to**: AM/TM/MPO
- **Territory Assignment**: Area(s)

---

## 📝 Territory & Area Setup

### Territory Entry Process

#### Division Entry
**Required Fields:**
- **Division Name**: Unique division name
- **Division Code**: Optional unique code
- **Status**: Active/Inactive
- **Description**: Optional description

**Business Rules:**
- Division name must be unique
- Cannot delete division if it has regions
- Status change affects all child regions, territories, and areas

#### Region Entry
**Required Fields:**
- **Division**: Select parent division
- **Region Name**: Unique region name within division
- **Region Code**: Optional unique code
- **Status**: Active/Inactive
- **Description**: Optional description

**Business Rules:**
- Region name must be unique within division
- Cannot delete region if it has territories
- Must belong to a valid division

#### Territory Entry
**Required Fields:**
- **Division**: Select parent division (auto-filled from region)
- **Region**: Select parent region
- **Territory Name**: Unique territory name within region
- **Territory Code**: Optional unique code
- **Status**: Active/Inactive
- **Description**: Optional description

**Business Rules:**
- Territory name must be unique within region
- Cannot delete territory if it has areas
- Must belong to a valid region

#### Area Entry
**Required Fields:**
- **Division**: Select parent division (auto-filled from territory)
- **Region**: Select parent region (auto-filled from territory)
- **Territory**: Select parent territory
- **Area Name**: Unique area name within territory
- **Area Code**: Optional unique code
- **Status**: Active/Inactive
- **Description**: Optional description

**Business Rules:**
- Area name must be unique within territory
- Cannot delete area if it has active employees assigned
- Cannot delete area if it has active allocations, deposits, or targets
- Must belong to a valid territory
- Area is the operational unit for all business operations

---

## 👤 Employee Assignment to Areas

### Employee Assignment Process

#### Assignment Entry
**Required Fields:**
- **Employee Name**: Select employee from master data
- **Area**: Select area(s) for assignment
- **Assignment Date**: Date when assignment becomes effective
- **Role in Area**: Primary role in this area (SR, MPO, AM, TM, etc.)
- **Status**: Active/Inactive

**Optional Fields:**
- **End Date**: Date when assignment ends (for historical tracking)
- **Notes**: Optional notes about assignment

#### Multiple Area Assignment
**Key Feature**: Employees can be assigned to multiple areas simultaneously.

**Business Rules:**
- Employee can be assigned to multiple areas
- Employee can have different roles in different areas
- Employee gets incentives from all assigned areas (if eligible)
- Assignment must have effective date
- Cannot assign employee to area if employee is inactive
- Cannot assign employee to area if area is inactive

#### Assignment Validation
- Employee must be active
- Area must be active
- Assignment date cannot be future date (or within reasonable limit)
- Employee must have valid designation/role
- Cannot assign same employee to same area twice with overlapping dates

### Employee Role in Area

#### Role Types
- **Manager Roles**: AM, TM, Sr.AM, RM, Sr.RM, DSM (responsible for area/territory)
- **Field Roles**: SR, MPO (field sales employees)
- **Dual Roles**: Employee can have multiple roles (e.g., MPO and SR in same area)

#### Role Assignment Rules
- Manager roles (AM, TM, etc.) can be assigned to areas/territories
- Field roles (SR, MPO) are assigned to areas
- Employee can have different roles in different areas
- Employee with dual roles gets incentives for all roles

---

## 🔍 Area Selection & Navigation

### Area Selection in Operations

#### Selection Method
- **Single Selection**: Area selected as single unit (not Division > Region > Territory separately)
- **Hierarchy Display**: System displays full hierarchy path when area is selected
- **Search & Filter**: Users can search/filter areas by:
  - Area name
  - Territory name
  - Region name
  - Division name
  - Employee assigned to area
  - Status (Active/Inactive)

#### Hierarchy Display Format
When area is selected, system displays:
```
Division: Dhaka
Region: Dhaka Metro
Territory: North Territory
Area: Dhaka Metro North
```

### Area-Based Operations

All following operations use area as the operational unit:
1. **Product Disbursement**: Products allocated to areas
2. **Deposit Entry**: Deposits recorded for areas
3. **Expense Entry**: Expenses tagged to areas
4. **Target Setting**: Targets set for areas
5. **Incentive Calculation**: Incentives calculated area-wise
6. **Due Management**: Dues tracked area-wise
7. **Reporting**: All reports filterable by area

---

## 📊 Data Model

### Territory Hierarchy Tables

#### Division Table
```sql
- division_id (PK)
- division_name (Unique)
- division_code (Optional, Unique)
- status (Active/Inactive)
- description
- created_date
- created_by
- updated_date
- updated_by
```

#### Region Table
```sql
- region_id (PK)
- division_id (FK)
- region_name (Unique within division)
- region_code (Optional)
- status (Active/Inactive)
- description
- created_date
- created_by
- updated_date
- updated_by
```

#### Territory Table
```sql
- territory_id (PK)
- region_id (FK)
- division_id (FK, denormalized for performance)
- territory_name (Unique within region)
- territory_code (Optional)
- status (Active/Inactive)
- description
- created_date
- created_by
- updated_date
- updated_by
```

#### Area Table
```sql
- area_id (PK)
- territory_id (FK)
- region_id (FK, denormalized)
- division_id (FK, denormalized)
- area_name (Unique within territory)
- area_code (Optional)
- status (Active/Inactive)
- description
- created_date
- created_by
- updated_date
- updated_by
```

### Employee Assignment Table

#### Employee Area Assignment Table
```sql
- assignment_id (PK)
- employee_id (FK)
- area_id (FK)
- assignment_date (Effective date)
- end_date (Optional, for historical tracking)
- role_in_area (SR, MPO, AM, TM, etc.)
- status (Active/Inactive)
- notes
- created_date
- created_by
- updated_date
- updated_by
```

**Indexes:**
- Index on (employee_id, area_id, assignment_date) for quick lookup
- Index on area_id for area-based queries
- Index on employee_id for employee-based queries

---

## ✅ Business Rules & Validation

### Territory Hierarchy Rules
1. **Hierarchy Integrity**: Must maintain valid hierarchy (Division > Region > Territory > Area)
2. **Uniqueness**: Names must be unique within parent level
3. **Status Cascade**: Inactive parent makes all children inactive (or warning)
4. **Deletion Rules**: Cannot delete if has children or active assignments
5. **Code Uniqueness**: Codes (if provided) must be unique

### Employee Assignment Rules
1. **Active Status**: Both employee and area must be active for assignment
2. **Multiple Areas**: Employee can be assigned to multiple areas
3. **Multiple Roles**: Employee can have different roles in different areas
4. **Dual Roles**: Employee can have multiple roles in same area (e.g., MPO and SR)
5. **Date Validation**: Assignment date cannot be future date (or within reasonable limit)
6. **Overlap Prevention**: Cannot have overlapping active assignments for same employee-area combination

### Area Operational Rules
1. **Area as Unit**: All operations (allocation, deposit, expense, target, incentive) are area-based
2. **Employee Tag**: Employee tag in operations is optional and for reference only
3. **Receiving Employee**: For disbursements, receiving employee must belong to selected area
4. **Manager Assignment**: Areas should have at least one manager assigned (business rule)
5. **SR Assignment**: Areas should have at least one SR assigned (business rule)

---

## 🔄 Workflows

### Territory Setup Workflow
```
1. Create Division
   ↓
2. Create Region (under Division)
   ↓
3. Create Territory (under Region)
   ↓
4. Create Area (under Territory)
   ↓
5. Assign Employees to Area
   ↓
6. Area Ready for Operations
```

### Employee Assignment Workflow
```
1. Select Employee
   ↓
2. Select Area(s) for Assignment
   ↓
3. Set Assignment Date
   ↓
4. Set Role in Area
   ↓
5. Save Assignment
   ↓
6. Employee Available for Area Operations
```

### Area Selection Workflow (in Operations)
```
1. User Initiates Operation (e.g., Disbursement, Deposit)
   ↓
2. System Prompts for Area Selection
   ↓
3. User Searches/Selects Area
   ↓
4. System Displays Full Hierarchy Path
   ↓
5. System Validates Area is Active
   ↓
6. System Shows Employees Assigned to Area (if needed)
   ↓
7. Operation Proceeds with Selected Area
```

---

## 📈 Reporting Requirements

### Territory Reports
1. **Territory Hierarchy Report**: Complete hierarchy structure
2. **Area List Report**: All areas with hierarchy path
3. **Employee Assignment Report**: Employees by area/territory
4. **Territory Performance Report**: Performance metrics by territory level

### Area Reports
1. **Area Master List**: All areas with status and employee count
2. **Area Employee List**: Employees assigned to each area
3. **Area Status Report**: Active/Inactive areas summary

### Employee Reports
1. **Employee Territory Assignment**: Areas assigned to each employee
2. **Multi-Area Employees**: Employees assigned to multiple areas
3. **Role Distribution**: Employee count by role and area

---

## 🔐 Security & Access Control

### Access Permissions
- **Territory Setup**: Head Office users only (roles TBD)
- **Employee Assignment**: Head Office users only (roles TBD)
- **Area Selection**: All operational users (for operations)
- **View Access**: Users can view territories/areas based on their assignments

### Data Entry Responsibility
- **All Territory Setup**: Head Office employees only
- **All Employee Assignment**: Head Office employees only
- **All Operations**: Head Office employees enter all operational data

---

## 🎯 Success Criteria

### Functional Success
- ✅ Complete territory hierarchy setup and management
- ✅ Employee assignment to areas with multiple area support
- ✅ Area selection in all operational screens
- ✅ Hierarchy navigation and validation
- ✅ Multi-area employee support

### Technical Success
- ✅ Fast area selection (< 1 second)
- ✅ Efficient hierarchy queries
- ✅ Data integrity maintained
- ✅ Audit trail for all changes

### Business Success
- ✅ All areas properly set up and assigned
- ✅ All employees assigned to appropriate areas
- ✅ Area-based operations functioning correctly
- ✅ Reporting provides accurate territory/area data

---

**Document Status**: Complete Requirements  
**Related Documents**: 
- [Employee Management](employee_management.md)
- [Target Management](target_management.md)
- [Sales Force Collection & Deposit](sales_force_collection_deposit.md)

