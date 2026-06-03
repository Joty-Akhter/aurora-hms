--liquibase formatted sql

--changeset easyops:075-pharma-performance-indexes context:pharma
--comment: Create additional indexes for performance optimization (Phase 5.4)

-- Indexes for frequent queries
CREATE INDEX IF NOT EXISTS idx_deposits_org_area_date ON pharma.deposits(organization_id, area_id, deposit_date);
CREATE INDEX IF NOT EXISTS idx_deposits_status_date ON pharma.deposits(status, deposit_date);
CREATE INDEX IF NOT EXISTS idx_incentive_calc_area_year_month ON pharma.incentive_calculations(area_id, year, month);
CREATE INDEX IF NOT EXISTS idx_incentive_dist_employee_status ON pharma.incentive_distributions(employee_id, status);
CREATE INDEX IF NOT EXISTS idx_targets_area_year ON pharma.targets(area_id, year);
CREATE INDEX IF NOT EXISTS idx_expenses_area_year_month ON pharma.expenses(area_id, year, month);
CREATE INDEX IF NOT EXISTS idx_assignments_employee_status ON pharma.employee_area_assignments(employee_id, status);

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_deposit_lines_product ON pharma.deposit_lines(product_id, deposit_id);
CREATE INDEX IF NOT EXISTS idx_disbursement_lines_product ON pharma.product_disbursement_lines(product_id, product_disbursement_id);
CREATE INDEX IF NOT EXISTS idx_area_territory_active ON pharma.areas(territory_id, is_active);

--changeset easyops:076-pharma-archive-tables context:pharma
--comment: Create archive tables for historical data (Phase 5.4)

-- Archive table for old deposits (keep last 2 years in main table)
CREATE TABLE IF NOT EXISTS pharma.deposits_archive (
    LIKE pharma.deposits INCLUDING ALL
);

-- Archive table for old incentive calculations
CREATE TABLE IF NOT EXISTS pharma.incentive_calculations_archive (
    LIKE pharma.incentive_calculations INCLUDING ALL
);

-- Archive table for old incentive distributions
CREATE TABLE IF NOT EXISTS pharma.incentive_distributions_archive (
    LIKE pharma.incentive_distributions INCLUDING ALL
);

-- Archive table for old adjustments
CREATE TABLE IF NOT EXISTS pharma.adjustments_archive (
    LIKE pharma.adjustments INCLUDING ALL
);

-- Archive table for old expenses
CREATE TABLE IF NOT EXISTS pharma.expenses_archive (
    LIKE pharma.expenses INCLUDING ALL
);

COMMENT ON TABLE pharma.deposits_archive IS 'Archived deposits older than 2 years - Phase 5.4';
COMMENT ON TABLE pharma.incentive_calculations_archive IS 'Archived incentive calculations older than 2 years - Phase 5.4';
COMMENT ON TABLE pharma.incentive_distributions_archive IS 'Archived incentive distributions older than 2 years - Phase 5.4';
