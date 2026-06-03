# Territory-Specific Incentive Calculation Rules — Detailed Requirements

## Document Information

| Attribute | Value |
|-----------|-------|
| **Document Title** | Territory-Specific Incentive Calculation Rules |
| **Version** | 1.0 |
| **Status** | Draft Requirements |
| **Related Documents** | [Incentive System](incentive_system.md), [Territory & Area Management](territory_area_management.md) |

---

## 1. Executive Summary

This document defines the requirements for **territory-specific incentive calculation rules**. There is **no general rule** — rules are different for all territories. Each territory has its own rule with explicit percentage allocations. Each territory has exactly **one SR**; if no employee has SR designation, one MPO additionally acts as SR (dual role). Same role, multiple employees (e.g. two MPOs) may have different configured shares. The number of employees per territory varies.

---

## 2. Business Context

### 2.1 Current State (To Be Replaced)

- **General rule**: Single default rule applies across all territories when no territory-specific rule exists
- **Distribution**: SR 10%, remaining 90% split (MPO 80%, Manager 20%) based on headcount
- **Equal split**: Within each role group, employees share equally

### 2.2 Target State

- **No general rule**: Rules are different for all territories; each territory has its own rule
- **One SR per territory**: Each territory has exactly one SR for incentive purposes
- **If no SR designation**: One MPO-designation person additionally acts as SR (dual role: SR + MPO)
- **Per-employee shares**: Same role, multiple employees (e.g. two MPOs) may have different configured % shares
- **Variable headcount**: Number of employees per territory can differ
- **SR share**: Default 9% (configurable per territory); **Development fund**: Default 1% (configurable)
- **Manager/MPO shares**: Defined on **remaining as 100%** — each employee gets their configured percentage

---

## 3. Core Principles

### 3.1 No General Rule

- There **shall be no general rule**; rules are different for all territories
- The system **shall not** maintain any default/fallback rule
- Every territory **must** have its own explicit incentive rule defined
- If a territory has no rule, incentive calculation **shall fail** with a clear error (e.g., "No incentive rule defined for territory X")

### 3.2 Territory as Rule Boundary

- Incentive rules are defined **per territory**
- One territory = one rule (with optional versioning for effective dates)
- Rules do not cascade from parent (Region/Division)

### 3.3 One SR Per Territory

- Each territory has **exactly one SR** for incentive purposes
- If no employee has SR designation in the territory, **one MPO-designation person** additionally acts as SR (dual role: SR + MPO)
- The SR share is always allocated to that one person (dedicated SR or dual-role MPO)

---

## 4. Distribution Framework

### 4.1 Allocation Structure

| Recipient | Default % | Configurable | Notes |
|-----------|-----------|--------------|-------|
| **SR** | 9% | Yes | One SR per territory; or one MPO acts as SR when no SR designation |
| **Development Fund** | 1% | Yes | Per territory; Product Development / SDM Fund — not distributed to individuals |
| **Managers & MPOs** | Remainder | — | Shares defined on **remaining as 100%** (see Section 5) |

**Formula:**
```
Incentive Base Amount = Covered Amount × Incentive Percentage (e.g., 4%)

SR Share           = Incentive Base Amount × sr_share_percentage   (default 9%)
Development Fund   = Incentive Base Amount × development_fund_percentage (default 1%)
Remaining Pool     = Incentive Base Amount - SR Share - Development Fund
```

**Manager/MPO distribution:** The remaining pool is treated as **100%** for allocation. Number of employees varies by territory. **Same role, multiple employees** (e.g. two MPOs) may have **different** configured % shares — allocation is per-employee, not per-role. Total of all Manager + MPO shares **must equal 100%** of the remaining pool.

### 4.2 Development Fund

- **Purpose**: Default 1% of total incentive pool goes to Development (Product Development / SDM) Fund
- **Recipient**: Organizational fund, not an individual employee
- **Configurable**: Default 1%; can be changed per territory rule
- **Tracking**: Must be recorded for accounting and reporting
- **Distribution**: No further distribution; fund is managed separately

---

## 5. Manager and MPO Percentage Allocation

### 5.1 Remaining Amount as 100%

- After SR share and Development Fund are deducted, the **remaining amount** is treated as **100%** for distribution
- Number of employees **varies by territory**; each territory can have different headcount
- **Same role, multiple employees may have different shares** — e.g. MPO-001 gets 40%, MPO-002 gets 35%; allocation is per-employee, not equal split by role
- Percentages are defined in the territory’s incentive rule
- Each employee (Manager or MPO) has a configured % in the territory rule; total **must equal 100%** of the remaining pool

**Example (same role, different shares):**
```
Territory: North Territory
SR Share: 9%, Development Fund: 1% → Remaining = 90% of Incentive Base

Per-employee configured % shares of remaining (must total 100%):
  - AM-001 (Manager): 25%
  - MPO-001: 40%   ← same role as MPO-002, different share
  - MPO-002: 35%
  Total: 100% ✓
```

### 5.2 Calculation for Managers and MPOs

```
Remaining Pool = Incentive Base Amount - SR Share - Development Fund

Employee Incentive (Manager/MPO) = Remaining Pool × (Employee's Assigned Percentage / 100)
```

Where:
- **Remaining Pool** = Incentive Base Amount − SR Share − Development Fund
- **Employee's Assigned Percentage** = Value from territory incentive rule (percent of remaining, sums to 100%)

### 5.3 Validation Rules

- Sum of all Manager and MPO percentages for a territory **must equal 100%**
- Each percentage **must be** ≥ 0 and ≤ 100
- At least one Manager or MPO **must** be assigned to the territory

---

## 6. SR Role and Dual-Role MPO

### 6.1 Territory Has SR Designation

- One employee has SR designation in the territory
- That SR receives the SR share (default 9%) of incentive base amount
- One SR per territory

### 6.2 Territory Has No SR Designation

- No employee has SR designation in the territory
- **One MPO-designation person** additionally acts as SR (dual role: SR + MPO)
- This MPO receives:
  - **SR share**: SR share % (default 9%) of incentive base amount
  - **MPO share**: Their assigned percentage of the remaining amount (remaining = 100%)

**Example:**
```
Territory: Small Territory (no dedicated SR)
- MPO-003: Dual role (SR + MPO), MPO percentage = 100% of 90% pool

Incentive Base: 10,000 Taka
SR Share: 10,000 × 9% = 900 Taka → MPO-003
Development Fund: 10,000 × 1% = 100 Taka
Remaining: 10,000 × 90% = 9,000 Taka → MPO-003 (100%)

Total for MPO-003: 900 + 9,000 = 9,900 Taka
```

### 6.3 Designation of Dual-Role MPO

- When a territory has no SR designation, the **territory incentive rule** must specify which MPO acts as SR
- Only **one** MPO per territory can be designated as dual-role (SR + MPO)
- The dual-role MPO receives both SR share and their configured MPO % of the remaining pool

---

## 7. Territory Incentive Rule Structure

### 7.1 Rule Definition (Per Territory)

Each territory incentive rule **shall** include:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `territory_id` | UUID | Yes | Territory this rule applies to |
| `incentive_percentage` | Decimal | Yes | % of covered amount for incentive base (e.g., 4%) |
| `expense_limit_percentage` | Decimal | Yes | % of target for expense limit (e.g., 30%) |
| `has_dedicated_sr` | Boolean | Yes | True if territory has dedicated SR; False if MPO in dual role |
| `dual_role_employee_id` | UUID | Conditional | Required when `has_dedicated_sr` = false |
| `sr_share_percentage` | Decimal | Yes | Default 9%; configurable per territory |
| `development_fund_percentage` | Decimal | Yes | Default 1%; configurable per territory |
| `effective_from_date` | Date | Yes | Rule start date |
| `effective_to_date` | Date | No | Rule end date (null = active) |
| `is_active` | Boolean | Yes | Active flag |

### 7.2 Employee Percentage Allocation (Per Territory)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `territory_incentive_rule_id` | UUID | Yes | Parent rule |
| `employee_id` | UUID | Yes | Manager or MPO |
| `role_in_territory` | String | Yes | e.g., AM, TM, MPO |

| `allocation_percentage` | Decimal | Yes | % of the 90% pool (0–100) |

**Constraint:**
- Sum of `allocation_percentage` for all Manager/MPO entries in a territory rule = 100

---

## 8. Eligibility (Unchanged)

Eligibility rules remain as per existing incentive system:

1. **Target Achievement**: Covered Amount ≥ Target Amount
2. **Expense Limit**: Territory Expenses ≤ Expense Limit (% of target, e.g., 30%)
3. **All-or-Nothing**: If territory is eligible, all designated recipients get incentives; if not, no one does

---

## 9. Calculation Workflow

### 9.1 Pre-Conditions

1. Territory has an active incentive rule
2. Rule has valid employee assignments with percentages summing to 100%
3. If no dedicated SR, dual-role employee is specified

### 9.2 Calculation Steps

```
1. Retrieve territory incentive rule (FAIL if no rule)
2. Check eligibility (target achieved, expenses within limit)
3. If not eligible:
   - Set incentive base amount
   - Set all distributions to 0
   - Exit
4. Calculate Incentive Base Amount = Covered Amount × incentive_percentage
5. Calculate SR Share = Incentive Base × sr_share_percentage (default 9%)
6. Calculate Development Fund = Incentive Base × development_fund_percentage (default 1%)
7. Calculate Remaining Pool = Incentive Base − SR Share − Development Fund
8. If has_dedicated_sr:
   - Allocate SR Share to the one SR
9. Else (dual role):
   - Allocate SR Share to dual_role_employee_id
   - Allocate MPO share to dual_role_employee_id per their allocation_percentage
10. For each Manager/MPO (excluding dual-role if already processed):
    - Allocate Remaining Pool × (allocation_percentage / 100)
11. Create distribution records (SR_SHARE, DEVELOPMENT_FUND, MPO_SHARE, MANAGER_SHARE)
12. Persist calculation and distributions
```

### 9.3 Distribution Types

| Type | Description |
|------|-------------|
| `SR_SHARE` | 9% of incentive base |
| `DEVELOPMENT_FUND` | development_fund_percentage of incentive base (default 1%; organizational fund) |
| `MPO_SHARE` | From 90% pool per allocation percentage |
| `MANAGER_SHARE` | From 90% pool per allocation percentage |

---

## 10. Data Model Changes

### 10.1 Territory Incentive Rule (Extended)

**New/Modified Fields:**
- `has_dedicated_sr` (Boolean)
- `dual_role_employee_id` (UUID, nullable)
- `development_fund_percentage` (Decimal, default 1%)
- `sr_share_percentage` (Decimal, default 9%)

**Removed/Deprecated:**
- General default rule fallback
- `mpo_share_percentage`, `manager_share_percentage` at rule level (replaced by per-employee allocation)

### 10.2 Territory Incentive Allocation (New)

**New Table: `territory_incentive_allocations`**

| Column | Type | Description |
|--------|------|--------------|
| id | UUID | PK |
| territory_incentive_rule_id | UUID | FK to rule |
| employee_id | UUID | FK to employee |
| role_in_territory | VARCHAR | AM, TM, MPO, etc. |
| allocation_percentage | DECIMAL(5,2) | % of 90% pool |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

**Constraint:** `SUM(allocation_percentage) = 100` per territory_incentive_rule_id

### 10.3 Incentive Distribution (Extended)

**New Distribution Type:**
- `DEVELOPMENT_FUND` — for the development fund allocation (may be stored as a special distribution record)

---

## 11. Business Rules Summary

| Rule | Description |
|------|-------------|
| BR-1 | No general rule; rules are different for all territories |
| BR-2 | One SR per territory; if no SR designation, one MPO additionally acts as SR |
| BR-3 | SR share default 9%, Development Fund default 1% (both configurable); Manager+MPO get remainder as 100% |
| BR-4 | Per-employee allocation_percentage; same role, multiple employees may have different shares |
| BR-5 | Sum of allocation_percentage for Managers and MPOs = 100% of remaining pool |
| BR-6 | Territory without SR designation: one MPO designated dual-role; receives SR + MPO incentives |
| BR-7 | Dual-role MPO must have allocation_percentage for MPO share |
| BR-8 | Eligibility unchanged: target achieved AND expenses within limit |

---

## 12. Edge Cases and Validation

### 12.1 Territory Without Rule

- **Action**: Reject incentive calculation
- **Message**: "Incentive rule is required for territory [name]. Please define a territory-specific rule."

### 12.2 Allocation Sum ≠ 100%

- **Action**: Reject rule save/update
- **Message**: "Sum of Manager and MPO allocation percentages must equal 100%. Current sum: X%"

### 12.3 No SR and No Dual-Role Designated

- **Action**: Reject rule save/update
- **Message**: "Territory has no SR designation. Please designate one MPO to additionally act as SR (dual role)."

### 12.4 Dual-Role Employee Not in Territory

- **Action**: Reject rule save/update
- **Message**: "Dual-role employee must be assigned to this territory."

### 12.5 One SR Per Territory

- **Rule**: Each territory has exactly one SR for incentive purposes
- **Validation**: Territory rule must designate one SR (or one dual-role MPO when no SR designation)

### 12.6 Employee in Multiple Territories

- **Behavior**: Employee gets incentives from each territory per that territory’s rule
- **Cumulative**: Total incentive = sum across all assigned territories

---

## 13. Reporting Requirements

### 13.1 Territory Incentive Rule Report

- List all territories with active rules
- Show: Territory, SR type (dedicated/dual-role), allocation breakdown
- Highlight territories without rules

### 13.2 Incentive Distribution Report

- Per territory, per month: SR share, Development Fund, each Manager/MPO amount
- Show dual-role employees with both SR and MPO amounts

### 13.3 Development Fund Report

- Monthly total Development Fund by territory
- Year-to-date Development Fund

---

## 14. Migration Considerations

### 14.1 From Current to New Model

1. **Rule creation**: For each territory with incentive calculations, create a territory-specific rule
2. **Allocation derivation**: From current headcount-based logic, derive initial allocation percentages (e.g., equal split)
3. **Dual-role identification**: Identify territories without SR; designate MPO for dual role
4. **Default removal**: Remove general/default rule fallback after migration

### 14.2 Backward Compatibility

- Existing `TerritoryIncentiveRule` records may need migration script to add new fields
- `territory_incentive_allocations` populated from current employee assignments + equal split as initial values

---

## 15. Glossary

| Term | Definition |
|------|------------|
| **SR** | Sales Representative — field sales role |
| **MPO** | Medical Promotion Officer |
| **SDM Fund** | Sales Development / Product Development fund (1% of incentive pool) |
| **Dual Role** | MPO acting as both SR and MPO when no dedicated SR in territory |
| **Allocation Percentage** | Percentage of the 90% pool assigned to a Manager or MPO |
| **Territory** | Operational unit (Division > Region > Territory > Area) |

---

## 16. Appendix: Example Scenarios

### A. Territory with Dedicated SR

```
Territory: Metro North
Rule: has_dedicated_sr = true
Allocations: AM-001: 30%, MPO-001: 45%, MPO-002: 25%

Incentive Base: 20,000 Taka
SR Share: 1,800 Taka → SR-001 (only SR)
Development Fund: 200 Taka
Remaining: 18,000 Taka
  - AM-001: 18,000 × 30% = 5,400 Taka
  - MPO-001: 18,000 × 45% = 8,100 Taka
  - MPO-002: 18,000 × 25% = 4,500 Taka
```

### B. Territory with Dual-Role MPO

```
Territory: Rural South
Rule: has_dedicated_sr = false, dual_role_employee_id = MPO-003
Allocations: MPO-003: 100%

Incentive Base: 15,000 Taka
SR Share: 1,350 Taka → MPO-003 (as SR)
SDM Fund: 150 Taka
Remaining: 13,500 Taka → MPO-003 (100% as MPO)

Total MPO-003: 1,350 + 13,500 = 14,850 Taka
```

### C. Territory with Manager and Dual-Role MPO

```
Territory: Coastal East
Rule: has_dedicated_sr = false, dual_role_employee_id = MPO-004
Allocations: AM-002: 40%, MPO-004: 60%

Incentive Base: 25,000 Taka
SR Share: 2,250 Taka → MPO-004 (as SR)
Development Fund: 250 Taka
Remaining: 22,500 Taka
  - AM-002: 22,500 × 40% = 9,000 Taka
  - MPO-004: 22,500 × 60% = 13,500 Taka (as MPO)

Total MPO-004: 2,250 + 13,500 = 15,750 Taka
```

---

## 17. Implementation Plan

### 17.1 Overview

| Phase | Scope | Estimated Effort |
|-------|-------|------------------|
| Phase 1 | Database schema & migration | 2–3 hours |
| Phase 2 | Backend: entities, repositories, services | 4–5 hours |
| Phase 3 | Backend: API & validation | 2 hours |
| Phase 4 | Frontend: rule management UI | 4–5 hours |
| Phase 5 | Incentive calculation logic update | 3–4 hours |
| Phase 6 | Migration & testing | 2–3 hours |
| **Total** | | **17–22 hours** |

---

### 17.2 Phase 1: Database Schema & Migration

**Objective**: Add new tables and columns; migrate existing data.

**Tasks**:

1. **Create Liquibase changelog** (e.g. `056-pharma-territory-incentive-allocations.sql`):
   - Add columns to `territory_incentive_rules`:
     - `has_dedicated_sr` BOOLEAN DEFAULT true
     - `dual_role_employee_id` UUID (nullable, FK to employees)
     - `development_fund_percentage` DECIMAL(5,4) DEFAULT 0.0100
     - Rename/repurpose: `sr_share_percentage` DEFAULT 0.0900 (keep for SR share)
   - Deprecate (keep for backward compatibility, set via migration): `mpo_share_percentage`, `manager_share_percentage`
   - Create table `territory_incentive_allocations`:
     - id, territory_incentive_rule_id, employee_id, role_in_territory, allocation_percentage
     - created_at, updated_at, created_by, updated_by
     - Unique (territory_incentive_rule_id, employee_id)
     - Check: allocation_percentage >= 0 AND allocation_percentage <= 100

2. **Migration script** (data):
   - For each existing `territory_incentive_rules` row:
     - Set `has_dedicated_sr` = true if any SR in territory; else false
     - If no SR: set `dual_role_employee_id` to first MPO
     - Set `development_fund_percentage` = 0.01, `sr_share_percentage` = 0.09
   - For each territory with active rule: create `territory_incentive_allocations` from `employee_territory_assignments` (Managers + MPOs), with equal split so total = 100%

3. **Add `DEVELOPMENT_FUND` distribution type** to `incentive_distributions` (if type is enum/constrained).

**Deliverables**: Changelog file, migration script, rollback script.

---

### 17.3 Phase 2: Backend — Entities, Repositories, Services

**Objective**: Implement data layer and business logic.

**Tasks**:

1. **Entity: `TerritoryIncentiveRule`** (extend):
   - Add: `hasDedicatedSr`, `dualRoleEmployeeId`, `developmentFundPercentage`
   - Update defaults: `srSharePercentage` = 0.09
   - Keep `mpoSharePercentage`, `managerSharePercentage` for migration; mark deprecated

2. **Entity: `TerritoryIncentiveAllocation`** (new):
   - id, territoryIncentiveRuleId, employeeId, roleInTerritory, allocationPercentage
   - ManyToOne to TerritoryIncentiveRule, employee reference

3. **Repository: `TerritoryIncentiveAllocationRepository`**:
   - `findByTerritoryIncentiveRuleId(UUID ruleId)`
   - `deleteByTerritoryIncentiveRuleId(UUID ruleId)` (for rule updates)
   - `existsByTerritoryIncentiveRuleIdAndEmployeeId(UUID ruleId, UUID employeeId)`

4. **Service: `IncentiveRuleService`** (extend):
   - Remove default rule fallback: throw if no rule for territory
   - Add `validateAndSaveRule(TerritoryIncentiveRule rule, List<TerritoryIncentiveAllocation> allocations)`:
     - Validate allocation sum = 100%
     - Validate `has_dedicated_sr` or `dual_role_employee_id` set
     - Validate dual-role employee in territory
   - Add `getAllocationsForRule(UUID ruleId)`
   - Update `TerritoryIncentiveRuleDTO` to include allocations or separate endpoint

5. **Service: `TerritoryIncentiveAllocationService`** (new, optional):
   - CRUD for allocations
   - Validation: sum = 100%, employee in territory

**Deliverables**: New/updated Java classes, unit tests for validation.

---

### 17.4 Phase 3: Backend — API & Validation

**Objective**: Expose APIs for rule and allocation management.

**Tasks**:

1. **`IncentiveRuleController`** (extend):
   - `GET /api/pharma/incentive-rules/territory/{id}` — return rule + allocations (fail if no rule)
   - `POST /api/pharma/incentive-rules` — accept rule + allocations; validate sum = 100%
   - `PUT /api/pharma/incentive-rules/{id}` — same
   - `GET /api/pharma/incentive-rules/territory/{id}/allocations` — list allocations
   - Remove or change behavior when no rule exists (return 404, not defaults)

2. **Request/Response DTOs**:
   - `TerritoryIncentiveRuleRequest`: rule fields + `allocations: [{ employeeId, roleInTerritory, allocationPercentage }]`
   - `TerritoryIncentiveRuleResponse`: rule + allocations + validation status

3. **Validation**:
   - Allocation sum = 100%
   - If `hasDedicatedSr` = false, `dualRoleEmployeeId` required
   - Dual-role employee must have allocation record
   - All allocation employees must be in territory (cross-check `employee_territory_assignments`)

**Deliverables**: Updated controller, DTOs, integration tests.

---

### 17.5 Phase 4: Frontend — Rule Management UI

**Objective**: UI to configure territory-specific rules and per-employee allocations.

**Tasks**:

1. **`IncentiveRulesManagement.tsx`** (major update):
   - Territory selector (existing)
   - **Rule form**:
     - Incentive %, SR share %, Development fund %, Expense limit %
     - `has_dedicated_sr` toggle
     - If false: `dual_role_employee_id` selector (MPOs in territory)
   - **Allocations table**:
     - List Managers and MPOs in territory
     - Editable allocation % per employee
     - Running total display; validation when sum ≠ 100%
     - Add/remove rows when employees change
   - Save: submit rule + allocations in one request
   - Show territories without rules (warning/alert)

2. **`TerritoryIncentiveAllocation` component** (new or inline):
   - Table: Employee name, Role, Allocation %
   - Sum validation
   - Load employees from `employee_territory_assignments` for selected territory

3. **`pharmaService.ts`** (extend):
   - `getIncentiveRuleWithAllocations(territoryId)`
   - `saveIncentiveRuleWithAllocations(rule, allocations)`
   - Types: `TerritoryIncentiveAllocation`, `TerritoryIncentiveRuleWithAllocations`

**Deliverables**: Updated React components, service methods, UI tests.

---

### 17.6 Phase 5: Incentive Calculation Logic Update

**Objective**: Update `IncentiveService.calculateDistribution` to use new model.

**Tasks**:

1. **`IncentiveService`**:
   - Remove default rule fallback; require rule for territory
   - Get rule + allocations from `IncentiveRuleService`
   - **SR share**: If `hasDedicatedSr`, find SR in assignments; else use `dualRoleEmployeeId`
   - **Development fund**: Create `DEVELOPMENT_FUND` distribution record (or fund ledger entry)
   - **Manager/MPO**: Use `TerritoryIncentiveAllocation` percentages; `RemainingPool × (allocationPercentage / 100)` per employee
   - Dual-role MPO: add both SR_SHARE and MPO_SHARE distributions

2. **`IncentiveRuleService.TerritoryIncentiveRuleDTO`**:
   - Extend to include `hasDedicatedSr`, `dualRoleEmployeeId`, `developmentFundPercentage`
   - Include allocations list or fetch separately

3. **`IncentiveDistribution`**:
   - Support `DEVELOPMENT_FUND` type (employeeId may be null for fund)
   - Ensure dual-role employee gets two distribution records

**Deliverables**: Updated IncentiveService, DTO, unit/integration tests for calculation.

---

### 17.7 Phase 6: Migration & Testing

**Objective**: Migrate existing territories and validate end-to-end.

**Tasks**:

1. **Data migration**:
   - Run migration on dev/staging
   - Verify all territories with targets have rules
   - Create rules for territories missing them (using defaults + equal allocation)

2. **Testing**:
   - Unit: IncentiveRuleService validation, IncentiveService calculation
   - Integration: API for rule CRUD, incentive calculation with new rules
   - E2E: Create rule → run calculation → verify distributions
   - Regression: Compare old vs new calculation for migrated territories (where possible)

3. **Rollout**:
   - Feature flag (optional): `territory_specific_incentive_rules`
   - Deploy backend first; frontend can work with new API
   - Communicate to users: rules must be defined per territory

**Deliverables**: Migration runbook, test report, rollout checklist.

---

### 17.8 Implementation Order

```
Phase 1 (DB) → Phase 2 (Backend entities/services) → Phase 3 (API) → Phase 5 (Incentive calc) → Phase 4 (Frontend) → Phase 6 (Migration)
```

**Rationale**: Schema and backend first; incentive calculation can be tested via API before UI; frontend last.

---

### 17.9 Dependencies & Risks

| Dependency | Mitigation |
|------------|------------|
| Employee-territory assignments must be current | Validate allocations reference only assigned employees |
| No rule for territory | Fail calculation with clear error; UI shows territories without rules |
| Migration of existing rules | Script to derive allocations from current headcount (equal split) |
| Dual-role employee changes | Rule must be updated when MPO leaves or SR joins |

---

**Document Status**: Complete Requirements + Implementation Plan  
**Next Steps**: Execute Phase 1 (Database schema & migration)
