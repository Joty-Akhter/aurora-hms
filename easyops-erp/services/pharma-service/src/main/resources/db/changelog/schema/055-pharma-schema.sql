--liquibase formatted sql

--changeset easyops:055-create-pharma-schema context:pharma
--comment: Create pharma module database schema
CREATE SCHEMA IF NOT EXISTS pharma;
GRANT ALL PRIVILEGES ON SCHEMA pharma TO easyops;

--changeset easyops:056-create-pharma-divisions context:pharma
CREATE TABLE pharma.divisions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50) UNIQUE,
    description TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

--changeset easyops:057-create-pharma-regions context:pharma
CREATE TABLE pharma.regions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    division_id UUID NOT NULL REFERENCES pharma.divisions(id),
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50),
    description TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

--changeset easyops:058-create-pharma-areas context:pharma
--comment: Areas are middle tier in Division -> Region -> Area -> Territory
CREATE TABLE pharma.areas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    division_id UUID NOT NULL REFERENCES pharma.divisions(id),
    region_id UUID NOT NULL REFERENCES pharma.regions(id),
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50),
    description TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

--changeset easyops:059-create-pharma-territories context:pharma
--comment: Territories are leaf tier; each has warehouse. Division -> Region -> Area -> Territory
CREATE TABLE pharma.territories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    division_id UUID NOT NULL REFERENCES pharma.divisions(id),
    region_id UUID NOT NULL REFERENCES pharma.regions(id),
    area_id UUID NOT NULL REFERENCES pharma.areas(id),
    warehouse_id UUID REFERENCES inventory.warehouses(id),
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50),
    description TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

--changeset easyops:060-create-pharma-product-receipts context:pharma
CREATE TABLE pharma.product_receipts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    receipt_date DATE NOT NULL,
    receipt_number VARCHAR(100) UNIQUE,
    total_value DECIMAL(19, 2) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'DRAFT',
    user_name VARCHAR(200),
    user_designation VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

--changeset easyops:061-create-pharma-product-receipt-lines context:pharma
CREATE TABLE pharma.product_receipt_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_receipt_id UUID NOT NULL REFERENCES pharma.product_receipts(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(500),
    pack_size DECIMAL(10, 2),
    tp_with_vat DECIMAL(19, 4),
    mrp DECIMAL(19, 4),
    quantity DECIMAL(19, 4) NOT NULL,
    amount DECIMAL(19, 2),
    expiry_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--changeset easyops:062-create-pharma-product-disbursements context:pharma
CREATE TABLE pharma.product_disbursements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    territory_id UUID NOT NULL REFERENCES pharma.territories(id),
    employee_id UUID NOT NULL,
    disbursement_date DATE NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    previous_month_opening_total_due DECIMAL(19, 2) DEFAULT 0,
    total_supply_amount DECIMAL(19, 2) DEFAULT 0,
    total_balance_amount DECIMAL(19, 2) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'DRAFT',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

--changeset easyops:063-create-pharma-product-disbursement-lines context:pharma
CREATE TABLE pharma.product_disbursement_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_disbursement_id UUID NOT NULL REFERENCES pharma.product_disbursements(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(500),
    pack_size DECIMAL(10, 2),
    tp_with_vat DECIMAL(19, 4),
    mrp DECIMAL(19, 4),
    previous_month_opening_quantity DECIMAL(19, 4) DEFAULT 0,
    current_month_quantity DECIMAL(19, 4) NOT NULL,
    total_quantity DECIMAL(19, 4),
    product_amount DECIMAL(19, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--changeset easyops:064-create-pharma-adjustments context:pharma
CREATE TABLE pharma.adjustments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    territory_id UUID NOT NULL REFERENCES pharma.territories(id),
    adjustment_date DATE NOT NULL,
    adjustment_type VARCHAR(50) NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    total_adjustment_amount DECIMAL(19, 2) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'DRAFT',
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

--changeset easyops:065-create-pharma-adjustment-lines context:pharma
CREATE TABLE pharma.adjustment_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    adjustment_id UUID NOT NULL REFERENCES pharma.adjustments(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(500),
    pack_size DECIMAL(10, 2),
    tp_with_vat DECIMAL(19, 4),
    mrp DECIMAL(19, 4),
    current_outstanding_quantity DECIMAL(19, 4),
    adjustment_quantity DECIMAL(19, 4) NOT NULL,
    adjustment_amount DECIMAL(19, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--changeset easyops:066-create-pharma-deposits context:pharma
CREATE TABLE pharma.deposits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    territory_id UUID NOT NULL REFERENCES pharma.territories(id),
    employee_id UUID,
    deposit_date DATE NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    deposit_amount DECIMAL(19, 2) NOT NULL,
    bank_name VARCHAR(200) NOT NULL,
    bank_account_number VARCHAR(100) NOT NULL,
    total_product_amount DECIMAL(19, 2),
    status VARCHAR(50) DEFAULT 'DRAFT',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

--changeset easyops:067-create-pharma-deposit-lines context:pharma
CREATE TABLE pharma.deposit_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    deposit_id UUID NOT NULL REFERENCES pharma.deposits(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(500),
    tp_with_vat DECIMAL(19, 4),
    quantity_sold DECIMAL(19, 4) NOT NULL,
    current_outstanding_quantity DECIMAL(19, 4),
    product_amount DECIMAL(19, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--changeset easyops:068-create-pharma-targets context:pharma
CREATE TABLE pharma.targets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    territory_id UUID NOT NULL REFERENCES pharma.territories(id),
    employee_id UUID NOT NULL,
    year INTEGER NOT NULL,
    start_month INTEGER NOT NULL,
    end_month INTEGER NOT NULL,
    target_amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

--changeset easyops:069-create-pharma-expense-categories context:pharma
CREATE TABLE pharma.expense_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50) UNIQUE,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

--changeset easyops:070-create-pharma-expenses context:pharma
CREATE TABLE pharma.expenses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    territory_id UUID NOT NULL REFERENCES pharma.territories(id),
    expense_category_id UUID NOT NULL REFERENCES pharma.expense_categories(id),
    source_employee_id UUID,
    expense_amount DECIMAL(19, 2) NOT NULL,
    description TEXT NOT NULL,
    expense_date DATE NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    receipt_url VARCHAR(500),
    status VARCHAR(50) DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

--changeset easyops:071-create-pharma-territory-incentive-rules context:pharma
CREATE TABLE pharma.territory_incentive_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    territory_id UUID NOT NULL REFERENCES pharma.territories(id) ON DELETE CASCADE,
    incentive_percentage DECIMAL(5, 4) DEFAULT 0.0400,
    sr_share_percentage DECIMAL(5, 4) DEFAULT 0.0900,
    development_fund_percentage DECIMAL(5, 4) DEFAULT 0.0100,
    has_dedicated_sr BOOLEAN DEFAULT true,
    dual_role_employee_id UUID,
    mpo_share_percentage DECIMAL(5, 4) DEFAULT 0.7200,
    manager_share_percentage DECIMAL(5, 4) DEFAULT 0.1800,
    expense_limit_percentage DECIMAL(5, 4) DEFAULT 0.3000,
    rule_version INTEGER DEFAULT 1,
    effective_from_date DATE,
    effective_to_date DATE,
    is_active BOOLEAN DEFAULT true,
    description TEXT,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_territory_incentive_rules_unique_active ON pharma.territory_incentive_rules(territory_id) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_territory_incentive_rules_org ON pharma.territory_incentive_rules(organization_id);
CREATE INDEX IF NOT EXISTS idx_territory_incentive_rules_territory ON pharma.territory_incentive_rules(territory_id);

--changeset easyops:072-create-pharma-sold-product-entries context:pharma
CREATE TABLE pharma.sold_product_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    territory_id UUID NOT NULL REFERENCES pharma.territories(id),
    employee_id UUID,
    entry_date DATE NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    total_product_amount DECIMAL(19, 2),
    status VARCHAR(50) DEFAULT 'DRAFT',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);
CREATE TABLE pharma.sold_product_entry_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sold_product_entry_id UUID NOT NULL REFERENCES pharma.sold_product_entries(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(500),
    tp_with_vat DECIMAL(19, 4),
    quantity_sold DECIMAL(19, 4) NOT NULL,
    current_outstanding_quantity DECIMAL(19, 4),
    product_amount DECIMAL(19, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sold_product_entries_territory_date ON pharma.sold_product_entries(territory_id, entry_date);
CREATE INDEX IF NOT EXISTS idx_sold_product_entries_org_status ON pharma.sold_product_entries(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_sold_product_entry_lines_entry ON pharma.sold_product_entry_lines(sold_product_entry_id);
CREATE INDEX IF NOT EXISTS idx_sold_product_entry_lines_product ON pharma.sold_product_entry_lines(product_id, sold_product_entry_id);

--changeset easyops:073-create-pharma-employee-territory-assignments context:pharma
CREATE TABLE pharma.employee_territory_assignments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL,
    territory_id UUID NOT NULL REFERENCES pharma.territories(id),
    role_in_territory VARCHAR(50),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    assignment_date DATE NOT NULL,
    end_date DATE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    UNIQUE(employee_id, territory_id)
);
CREATE INDEX IF NOT EXISTS idx_territory_assignments_employee_status ON pharma.employee_territory_assignments(employee_id, status);

--changeset easyops:074-create-pharma-incentive-calculations context:pharma
CREATE TABLE pharma.incentive_calculations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    territory_id UUID NOT NULL REFERENCES pharma.territories(id),
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    target_amount DECIMAL(19, 2),
    covered_amount DECIMAL(19, 2),
    incentive_base_amount DECIMAL(19, 2),
    target_achieved BOOLEAN,
    expense_within_limit BOOLEAN,
    territory_eligible BOOLEAN,
    total_sr_share DECIMAL(19, 2),
    total_mpo_share DECIMAL(19, 2),
    total_manager_share DECIMAL(19, 2),
    total_incentive_distributed DECIMAL(19, 2),
    calculation_date TIMESTAMP WITH TIME ZONE,
    calculated_by UUID,
    status VARCHAR(50) DEFAULT 'CALCULATED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--changeset easyops:075-create-pharma-incentive-distributions context:pharma
CREATE TABLE pharma.incentive_distributions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    incentive_calculation_id UUID NOT NULL REFERENCES pharma.incentive_calculations(id) ON DELETE CASCADE,
    employee_id UUID,
    territory_id UUID NOT NULL REFERENCES pharma.territories(id),
    role_in_area VARCHAR(50),
    incentive_amount DECIMAL(19, 2) NOT NULL,
    distribution_type VARCHAR(50),
    years_of_service INTEGER,
    calculation_date TIMESTAMP WITH TIME ZONE,
    paid_date TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) DEFAULT 'CALCULATED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--changeset easyops:076-create-pharma-target-coverage context:pharma
CREATE TABLE pharma.target_coverage (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    target_id UUID NOT NULL REFERENCES pharma.targets(id) ON DELETE CASCADE,
    territory_id UUID NOT NULL REFERENCES pharma.territories(id),
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    target_amount DECIMAL(19, 2) NOT NULL,
    covered_amount DECIMAL(19, 2) DEFAULT 0,
    coverage_percentage DECIMAL(5, 2),
    status VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE
);

--changeset easyops:077-create-pharma-performance-indexes context:pharma
CREATE INDEX IF NOT EXISTS idx_deposits_org_territory_date ON pharma.deposits(organization_id, territory_id, deposit_date);
CREATE INDEX IF NOT EXISTS idx_deposits_status_date ON pharma.deposits(status, deposit_date);
CREATE INDEX IF NOT EXISTS idx_targets_territory_year ON pharma.targets(territory_id, year);
CREATE INDEX IF NOT EXISTS idx_expenses_territory_year_month ON pharma.expenses(territory_id, year, month);
CREATE INDEX IF NOT EXISTS idx_deposit_lines_product ON pharma.deposit_lines(product_id, deposit_id);
CREATE INDEX IF NOT EXISTS idx_disbursement_lines_product ON pharma.product_disbursement_lines(product_id, product_disbursement_id);
CREATE INDEX IF NOT EXISTS idx_area_region_active ON pharma.areas(region_id, is_active);
CREATE INDEX IF NOT EXISTS idx_territories_warehouse_id ON pharma.territories(warehouse_id);

--changeset easyops:078-create-pharma-archive-tables context:pharma
CREATE TABLE IF NOT EXISTS pharma.deposits_archive (LIKE pharma.deposits INCLUDING ALL);
CREATE TABLE IF NOT EXISTS pharma.adjustments_archive (LIKE pharma.adjustments INCLUDING ALL);
CREATE TABLE IF NOT EXISTS pharma.expenses_archive (LIKE pharma.expenses INCLUDING ALL);
COMMENT ON COLUMN pharma.territories.warehouse_id IS 'Reference to inventory.warehouses.id - Auto-created when territory is created';

