--liquibase formatted sql

--changeset easyops:098-migrate-territory-incentive-rules context:pharma
--comment: Migrate existing territory_incentive_rules to new model (SR 9%, Dev Fund 1%, has_dedicated_sr, dual_role)

-- Update existing rules: set new columns
UPDATE pharma.territory_incentive_rules tir
SET
  has_dedicated_sr = COALESCE((
    SELECT EXISTS (
      SELECT 1 FROM pharma.employee_territory_assignments eta
      WHERE eta.territory_id = tir.territory_id
      AND eta.status = 'ACTIVE'
      AND (eta.end_date IS NULL OR eta.end_date >= CURRENT_DATE)
      AND eta.role_in_territory = 'SR'
    )
  ), true),
  development_fund_percentage = 0.0100,
  sr_share_percentage = 0.0900
WHERE tir.is_active = true;

-- Set dual_role_employee_id when no dedicated SR - pick first MPO in territory
UPDATE pharma.territory_incentive_rules tir
SET dual_role_employee_id = (
  SELECT eta.employee_id
  FROM pharma.employee_territory_assignments eta
  WHERE eta.territory_id = tir.territory_id
  AND eta.status = 'ACTIVE'
  AND (eta.end_date IS NULL OR eta.end_date >= CURRENT_DATE)
  AND eta.role_in_territory = 'MPO'
  ORDER BY eta.employee_id
  LIMIT 1
)
WHERE tir.is_active = true
AND tir.has_dedicated_sr = false
AND tir.dual_role_employee_id IS NULL;

--changeset easyops:099-migrate-territory-incentive-allocations context:pharma
--comment: Populate territory_incentive_allocations from employee assignments (equal split, sum=100%)
INSERT INTO pharma.territory_incentive_allocations (
  territory_incentive_rule_id, employee_id, role_in_territory, allocation_percentage
)
WITH manager_mpo AS (
  SELECT
    tir.id AS rule_id,
    eta.employee_id,
    eta.role_in_territory,
    ROW_NUMBER() OVER (PARTITION BY tir.id ORDER BY eta.employee_id) AS rn,
    COUNT(*) OVER (PARTITION BY tir.id) AS cnt
  FROM pharma.territory_incentive_rules tir
  JOIN pharma.employee_territory_assignments eta
    ON eta.territory_id = tir.territory_id
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
  WHERE tir.is_active = true
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
