# Phase 6: Territory-Specific Incentive Rules — Migration Runbook

## Overview

This runbook guides the migration and rollout of territory-specific incentive rules (Phases 1–5). It covers database migration, verification, and deployment steps.

---

## 1. Pre-Migration Checklist

- [ ] Backup production database
- [ ] Verify Liquibase context includes `pharma` for target environment
- [ ] Ensure all territories with active targets have employee assignments (Manager/MPO)
- [ ] Notify stakeholders of maintenance window (if required)

---

## 2. Database Migration

### 2.1 Migration Order

Liquibase runs changesets in this order:

1. **095-pharma-territory-incentive-migration.sql**
   - Updates existing `territory_incentive_rules`: sets `has_dedicated_sr`, `development_fund_percentage`, `sr_share_percentage`
   - Sets `dual_role_employee_id` when no dedicated SR (picks first MPO)
   - Populates `territory_incentive_allocations` from employee assignments (equal split, sum=100%)

2. **096-pharma-territory-incentive-rules-for-missing-territories.sql**
   - Creates rules for territories that have **targets** but **no active rule**
   - Uses defaults: SR 9%, Dev Fund 1%, equal allocation among Manager/MPO
   - Only creates rules for territories with at least one Manager or MPO assigned

### 2.2 Running Migration

**Option A: Application startup (default)**

Liquibase runs automatically when pharma-service starts. Ensure:

```yaml
spring:
  liquibase:
    contexts: pharma,demo   # or your environment context
```

**Option B: Manual Liquibase update**

```bash
mvn liquibase:update -Dliquibase.contexts=pharma
```

### 2.3 Verify Migration

```sql
-- Territories with targets but no rule (should be 0 after migration)
SELECT t.id, t.name, t.code
FROM pharma.territories t
WHERE EXISTS (
  SELECT 1 FROM pharma.targets tg
  WHERE tg.territory_id = t.id AND tg.status = 'ACTIVE'
)
AND NOT EXISTS (
  SELECT 1 FROM pharma.territory_incentive_rules tir
  WHERE tir.territory_id = t.id AND tir.is_active = true
);

-- Rules with allocations (sample)
SELECT tir.territory_id, tir.sr_share_percentage, tir.development_fund_percentage,
       tir.has_dedicated_sr, tir.dual_role_employee_id,
       (SELECT SUM(allocation_percentage) FROM pharma.territory_incentive_allocations tia
        WHERE tia.territory_incentive_rule_id = tir.id) AS allocation_sum
FROM pharma.territory_incentive_rules tir
WHERE tir.is_active = true
LIMIT 10;
```

---

## 3. Post-Migration Verification

1. **API checks**
   - `GET /api/pharma/incentive-rules/territory/{territoryId}` — returns rule + allocations for territories with targets
   - `GET /api/pharma/incentive-rules/territory/{territoryId}/allocations` — returns allocation list

2. **Incentive calculation**
   - Run `POST /api/pharma/incentives/calculate?territoryId={id}&year=YYYY&month=M` for a test territory
   - Verify distributions include `SR_SHARE`, `DEVELOPMENT_FUND`, `MPO_SHARE`, `MANAGER_SHARE`
   - Verify `total_incentive_distributed` = sum of all distribution amounts

3. **Territories without rules**
   - Incentive calculation must fail with clear error: "Incentive rule is required for territory X"
   - Frontend should show warning for territories without rules

---

## 4. Rollback (If Needed)

If migration causes issues:

1. **Deactivate new rules** (keeps data, stops use):
   ```sql
   UPDATE pharma.territory_incentive_rules
   SET is_active = false, effective_to_date = CURRENT_DATE
   WHERE description = 'Auto-created rule for territory (Phase 6 migration)';
   ```

2. **Full rollback** requires restoring from backup and reverting application to pre-Phase-5 version. The schema changes (Phase 1) are additive; `territory_incentive_allocations` can be truncated if needed.

---

## 5. Known Limitations

- Territories with targets but **no Manager/MPO** assigned will not get auto-created rules. Create rules manually via API/UI.
- Dual-role employee must be updated in the rule when the designated MPO leaves the territory.
- Allocation percentages must sum to 100%; validation enforces this on save.

---

## 6. Support

For issues, refer to:
- Requirements: `incentive_calculation_rules_territory_specific.md`
- Rollout checklist: `PHASE6_ROLLOUT_CHECKLIST.md`
