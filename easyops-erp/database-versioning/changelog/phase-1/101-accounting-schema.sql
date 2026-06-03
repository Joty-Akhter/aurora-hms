--liquibase formatted sql

--changeset easyops:043-create-accounting-schema context:accounting
--comment: Create accounting schema
CREATE SCHEMA IF NOT EXISTS accounting;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA accounting TO easyops;
GRANT ALL PRIVILEGES ON SCHEMA accounting TO easyops;

--changeset easyops:044-create-coa-templates-table context:accounting
--comment: Create Chart of Accounts template table
CREATE TABLE accounting.coa_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    industry VARCHAR(100),
    locale VARCHAR(10) DEFAULT 'bn-BD',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users.users(id)
);

--changeset easyops:045-create-chart-of-accounts-table context:accounting
--comment: Create Chart of Accounts (Hierarchical) table
CREATE TABLE accounting.chart_of_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    account_code VARCHAR(50) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    parent_account_id UUID REFERENCES accounting.chart_of_accounts(id),
    account_type VARCHAR(50) NOT NULL,
    account_category VARCHAR(100),
    account_subcategory VARCHAR(100),
    level INTEGER DEFAULT 1,
    is_group BOOLEAN DEFAULT false,
    is_system_account BOOLEAN DEFAULT false,
    currency VARCHAR(3) DEFAULT 'USD',
    opening_balance DECIMAL(19, 4) DEFAULT 0,
    opening_balance_date DATE,
    current_balance DECIMAL(19, 4) DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    allow_manual_entry BOOLEAN DEFAULT true,
    description TEXT,
    tax_type VARCHAR(50),
    tags TEXT[],
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users.users(id),
    updated_by UUID REFERENCES users.users(id),
    UNIQUE(organization_id, account_code)
);

CREATE INDEX idx_coa_organization ON accounting.chart_of_accounts(organization_id);
CREATE INDEX idx_coa_code ON accounting.chart_of_accounts(account_code);
CREATE INDEX idx_coa_type ON accounting.chart_of_accounts(account_type);
CREATE INDEX idx_coa_parent ON accounting.chart_of_accounts(parent_account_id);
CREATE INDEX idx_coa_active ON accounting.chart_of_accounts(is_active);

--changeset easyops:046-create-fiscal-years-table context:accounting
--comment: Create fiscal year configuration table
CREATE TABLE accounting.fiscal_years (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    year_code VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_closed BOOLEAN DEFAULT false,
    closed_at TIMESTAMP WITH TIME ZONE,
    closed_by UUID REFERENCES users.users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, year_code)
);

--changeset easyops:047-create-periods-table context:accounting
--comment: Create accounting periods (monthly/quarterly) table
CREATE TABLE accounting.periods (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    fiscal_year_id UUID NOT NULL REFERENCES accounting.fiscal_years(id) ON DELETE CASCADE,
    period_number INTEGER NOT NULL,
    period_name VARCHAR(50) NOT NULL,
    period_type VARCHAR(20) DEFAULT 'MONTHLY',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'OPEN',
    closed_at TIMESTAMP WITH TIME ZONE,
    closed_by UUID REFERENCES users.users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, fiscal_year_id, period_number)
);

CREATE INDEX idx_periods_org ON accounting.periods(organization_id);
CREATE INDEX idx_periods_fiscal_year ON accounting.periods(fiscal_year_id);
CREATE INDEX idx_periods_status ON accounting.periods(status);
CREATE INDEX idx_periods_dates ON accounting.periods(start_date, end_date);

--changeset easyops:048-create-journal-entries-table context:accounting
--comment: Create journal entry header table
CREATE TABLE accounting.journal_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    journal_number VARCHAR(50) UNIQUE NOT NULL,
    journal_date DATE NOT NULL,
    period_id UUID NOT NULL REFERENCES accounting.periods(id),
    journal_type VARCHAR(50) DEFAULT 'MANUAL',
    source_module VARCHAR(50),
    source_document_id UUID,
    reference_number VARCHAR(100),
    description TEXT,
    status VARCHAR(20) DEFAULT 'DRAFT',
    total_debit DECIMAL(19, 4) DEFAULT 0,
    total_credit DECIMAL(19, 4) DEFAULT 0,
    posted_at TIMESTAMP WITH TIME ZONE,
    posted_by UUID REFERENCES users.users(id),
    reversed_at TIMESTAMP WITH TIME ZONE,
    reversed_by UUID REFERENCES users.users(id),
    reversal_journal_id UUID REFERENCES accounting.journal_entries(id),
    approval_status VARCHAR(20) DEFAULT 'PENDING',
    approved_by UUID REFERENCES users.users(id),
    approved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users.users(id),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES users.users(id)
);

CREATE INDEX idx_journal_org ON accounting.journal_entries(organization_id);
CREATE INDEX idx_journal_number ON accounting.journal_entries(journal_number);
CREATE INDEX idx_journal_date ON accounting.journal_entries(journal_date);
CREATE INDEX idx_journal_period ON accounting.journal_entries(period_id);
CREATE INDEX idx_journal_status ON accounting.journal_entries(status);
CREATE INDEX idx_journal_type ON accounting.journal_entries(journal_type);
CREATE INDEX idx_journal_source ON accounting.journal_entries(source_module);

--changeset easyops:049-create-journal-lines-table context:accounting
--comment: Create journal entry lines (detail) table
CREATE TABLE accounting.journal_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    journal_entry_id UUID NOT NULL REFERENCES accounting.journal_entries(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    account_id UUID NOT NULL REFERENCES accounting.chart_of_accounts(id),
    debit_amount DECIMAL(19, 4) DEFAULT 0,
    credit_amount DECIMAL(19, 4) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'USD',
    exchange_rate DECIMAL(19, 6) DEFAULT 1,
    debit_base DECIMAL(19, 4) DEFAULT 0,
    credit_base DECIMAL(19, 4) DEFAULT 0,
    description TEXT,
    cost_center_id UUID,
    department_id UUID REFERENCES admin.departments(id),
    project_id UUID,
    tags JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_journal_lines_entry ON accounting.journal_lines(journal_entry_id);
CREATE INDEX idx_journal_lines_account ON accounting.journal_lines(account_id);
CREATE INDEX idx_journal_lines_department ON accounting.journal_lines(department_id);

--changeset easyops:050-create-account-balances-table context:accounting
--comment: Create account balance tracking by period table
CREATE TABLE accounting.account_balances (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounting.chart_of_accounts(id) ON DELETE CASCADE,
    period_id UUID NOT NULL REFERENCES accounting.periods(id) ON DELETE CASCADE,
    opening_balance DECIMAL(19, 4) DEFAULT 0,
    debit_total DECIMAL(19, 4) DEFAULT 0,
    credit_total DECIMAL(19, 4) DEFAULT 0,
    closing_balance DECIMAL(19, 4) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'USD',
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(account_id, period_id)
);

CREATE INDEX idx_balances_account ON accounting.account_balances(account_id);
CREATE INDEX idx_balances_period ON accounting.account_balances(period_id);
CREATE INDEX idx_balances_org ON accounting.account_balances(organization_id);

--changeset easyops:051-create-account-balance-summary-table context:accounting
--comment: Create account balance summary (for quick reporting) table
CREATE TABLE accounting.account_balance_summary (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounting.chart_of_accounts(id) ON DELETE CASCADE,
    fiscal_year_id UUID NOT NULL REFERENCES accounting.fiscal_years(id) ON DELETE CASCADE,
    ytd_debit DECIMAL(19, 4) DEFAULT 0,
    ytd_credit DECIMAL(19, 4) DEFAULT 0,
    ytd_balance DECIMAL(19, 4) DEFAULT 0,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(account_id, fiscal_year_id)
);

--changeset easyops:052-create-journal-templates-table context:accounting
--comment: Create recurring journal templates table
CREATE TABLE accounting.journal_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id) ON DELETE CASCADE,
    template_name VARCHAR(100) NOT NULL,
    template_code VARCHAR(50) NOT NULL,
    description TEXT,
    journal_type VARCHAR(50) DEFAULT 'MANUAL',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users.users(id),
    UNIQUE(organization_id, template_code)
);

--changeset easyops:053-create-journal-template-lines-table context:accounting
--comment: Create template lines table
CREATE TABLE accounting.journal_template_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID NOT NULL REFERENCES accounting.journal_templates(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    account_id UUID NOT NULL REFERENCES accounting.chart_of_accounts(id),
    debit_amount DECIMAL(19, 4) DEFAULT 0,
    credit_amount DECIMAL(19, 4) DEFAULT 0,
    description TEXT,
    is_variable_amount BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--changeset easyops:054-create-accounting-triggers context:accounting splitStatements:false
--comment: Create triggers for accounting tables
CREATE OR REPLACE FUNCTION update_journal_totals()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE accounting.journal_entries
    SET total_debit = (
        SELECT COALESCE(SUM(debit_amount), 0)
        FROM accounting.journal_lines
        WHERE journal_entry_id = NEW.journal_entry_id
    ),
    total_credit = (
        SELECT COALESCE(SUM(credit_amount), 0)
        FROM accounting.journal_lines
        WHERE journal_entry_id = NEW.journal_entry_id
    )
    WHERE id = NEW.journal_entry_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_journal_totals
AFTER INSERT OR UPDATE OR DELETE ON accounting.journal_lines
FOR EACH ROW EXECUTE FUNCTION update_journal_totals();

CREATE OR REPLACE FUNCTION update_account_balances()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'POSTED' AND OLD.status != 'POSTED' THEN
        UPDATE accounting.account_balances ab
        SET 
            debit_total = ab.debit_total + jl.debit_amount,
            credit_total = ab.credit_total + jl.credit_amount,
            closing_balance = ab.opening_balance + ab.debit_total - ab.credit_total
        FROM accounting.journal_lines jl
        WHERE ab.account_id = jl.account_id
        AND ab.period_id = NEW.period_id
        AND jl.journal_entry_id = NEW.id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_balances_on_post
AFTER UPDATE ON accounting.journal_entries
FOR EACH ROW EXECUTE FUNCTION update_account_balances();

CREATE TRIGGER update_coa_updated_at BEFORE UPDATE ON accounting.chart_of_accounts FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_journal_updated_at BEFORE UPDATE ON accounting.journal_entries FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

--changeset easyops:055-create-accounting-views context:accounting
--comment: Create views for reporting
CREATE OR REPLACE VIEW accounting.v_trial_balance AS
SELECT 
    ab.organization_id,
    ab.period_id,
    p.period_name,
    coa.account_code,
    coa.account_name,
    coa.account_type,
    ab.opening_balance,
    ab.debit_total,
    ab.credit_total,
    ab.closing_balance,
    ab.currency
FROM accounting.account_balances ab
JOIN accounting.chart_of_accounts coa ON ab.account_id = coa.id
JOIN accounting.periods p ON ab.period_id = p.id
WHERE coa.is_group = false
ORDER BY coa.account_code;

CREATE OR REPLACE VIEW accounting.v_account_ledger AS
SELECT 
    je.organization_id,
    je.journal_number,
    je.journal_date,
    je.journal_type,
    je.description as journal_description,
    je.status,
    jl.account_id,
    coa.account_code,
    coa.account_name,
    jl.debit_amount,
    jl.credit_amount,
    jl.description as line_description,
    jl.department_id,
    je.reference_number,
    je.posted_at
FROM accounting.journal_entries je
JOIN accounting.journal_lines jl ON je.id = jl.journal_entry_id
JOIN accounting.chart_of_accounts coa ON jl.account_id = coa.id
WHERE je.status = 'POSTED'
ORDER BY je.journal_date, je.journal_number, jl.line_number;

--changeset easyops:056-grant-accounting-permissions context:accounting
--comment: Grant permissions on accounting tables to easyops user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA accounting TO easyops;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA accounting TO easyops;
GRANT SELECT ON accounting.v_trial_balance TO easyops;
GRANT SELECT ON accounting.v_account_ledger TO easyops;
