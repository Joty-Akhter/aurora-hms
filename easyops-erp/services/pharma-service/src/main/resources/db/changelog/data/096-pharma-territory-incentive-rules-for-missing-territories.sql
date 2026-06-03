--liquibase formatted sql

--changeset easyops:100-create-rules-for-territories-missing-them context:pharma
--comment: Phase 6 - Create incentive rules for territories with targets but no rule (defaults + equal allocation)

-- Create rules for territories that have targets but no active incentive rule
INSERT INTO pharma.territory_incentive_rules (
  id, organization_id, territory_id, incentive_percentage, sr_share_percentage,
  development_fund_percentage, has_dedicated_sr, dual_role_employee_id,
  mpo_share_percentage, manager_share_percentage, expense_limit_percentage,
  rule_version, effective_from_date, is_active, description, created_at, updated_at
)
SELECT
  uuid_generate_v4(),
  t.organization_id,
  t.id AS territory_id,
  0.0400,
  0.0900,
  0.0100,
  COALESCE((
    SELECT EXISTS (
      SELECT 1 FROM pharma.employee_territory_assignments eta
      WHERE eta.territory_id = t.id
      AND eta.status = 'ACTIVE'
      AND (eta.end_date IS NULL OR eta.end_date >= CURRENT_DATE)
      AND eta.role_in_territory = 'SR'
    )
  ), true),
  CASE WHEN NOT EXISTS (
    SELECT 1 FROM pharma.employee_territory_assignments eta
    WHERE eta.territory_id = t.id
    AND eta.status = 'ACTIVE'
    AND (eta.end_date IS NULL OR eta.end_date >= CURRENT_DATE)
    AND eta.role_in_territory = 'SR'
  ) THEN (
    SELECT eta.employee_id
    FROM pharma.employee_territory_assignments eta
    WHERE eta.territory_id = t.id
    AND eta.status = 'ACTIVE'
    AND (eta.end_date IS NULL OR eta.end_date >= CURRENT_DATE)
    AND eta.role_in_territory = 'MPO'
    ORDER BY eta.employee_id
    LIMIT 1
  ) ELSE NULL END,
  0.7200,
  0.1800,
  0.3000,
  1,
  CURRENT_DATE,
  true,
  'Auto-created rule for territory (Phase 6 migration)',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM pharma.territories t
WHERE EXISTS (
  SELECT 1 FROM pharma.targets tg
  WHERE tg.territory_id = t.id
  AND tg.status = 'ACTIVE'
)
AND NOT EXISTS (
  SELECT 1 FROM pharma.territory_incentive_rules tir
  WHERE tir.territory_id = t.id
  AND tir.is_active = true
)
-- Only create if territory has Manager/MPO for allocations (sum must equal 100%)
AND EXISTS (
  SELECT 1 FROM pharma.employee_territory_assignments eta
  WHERE eta.territory_id = t.id
  AND eta.status = 'ACTIVE'
  AND (eta.end_date IS NULL OR eta.end_date >= CURRENT_DATE)
  AND eta.role_in_territory != 'SR'
  AND (
    eta.role_in_territory = 'MPO'
    OR eta.role_in_territory LIKE '%AM%'
    OR eta.role_in_territory LIKE '%TM%'
    OR eta.role_in_territory LIKE '%RM%'
    OR eta.role_in_territory LIKE '%DSM%'
    OR eta.role_in_territory LIKE '%ASM%'
    OR eta.role_in_territory LIKE '%SM%'
  )
);

-- Populate allocations for newly created rules (equal split among Manager/MPO)
INSERT INTO pharma.territory_incentive_allocations (
  territory_incentive_rule_id, employee_id, role_in_territory, allocation_percentage
)
WITH new_rules AS (
  SELECT tir.id AS rule_id, tir.territory_id
  FROM pharma.territory_incentive_rules tir
  WHERE tir.description = 'Auto-created rule for territory (Phase 6 migration)'
  AND tir.is_active = true
),
manager_mpo AS (
  SELECT
    nr.rule_id,
    eta.employee_id,
    eta.role_in_territory,
    ROW_NUMBER() OVER (PARTITION BY nr.rule_id ORDER BY eta.employee_id) AS rn,
    COUNT(*) OVER (PARTITION BY nr.rule_id) AS cnt
  FROM new_rules nr
  JOIN pharma.employee_territory_assignments eta
    ON eta.territory_id = nr.territory_id
    AND eta.status = 'ACTIVE'
    AND (eta.end_date IS NULL OR eta.end_date >= CURRENT_DATE)
    AND eta.role_in_territory != 'SR'
    AND (
      eta.role_in_territory = 'MPO'
      OR eta.role_in_territory LIKE '%AM%'
      OR eta.role_in_territory LIKE '%TM%'
      OR eta.role_in_territory LIKE '%RM%'
      OR eta.role_in_territory LIKE '%DSM%'
      OR eta.role_in_territory LIKE '%ASM%'
      OR eta.role_in_territory LIKE '%SM%'
    )
)
SELECT
  rule_id,
  employee_id,
  role_in_territory,
  ROUND(
    (100.0 / cnt)::numeric + CASE WHEN rn = 1 THEN (100.0 - (ROUND((100.0 / cnt)::numeric, 2) * cnt)) ELSE 0 END,
    2
  )::decimal(5,2)
FROM manager_mpo
WHERE cnt > 0
ON CONFLICT (territory_incentive_rule_id, employee_id) DO NOTHING;
