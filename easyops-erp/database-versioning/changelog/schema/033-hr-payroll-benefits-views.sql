--liquibase formatted sql

--changeset easyops:033-create-hr-payroll-benefits-views splitStatements:true

-- =====================================================
-- HR PAYROLL & BENEFITS ANALYTICS VIEWS
-- Phase 5.3: Payroll & Benefits Administration
-- =====================================================

-- View: Employee Salary Summary
CREATE OR REPLACE VIEW hr.v_employee_salary_summary AS
SELECT 
    e.employee_id,
    e.organization_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
    d.name AS department_name,
    p.title AS position_title,
    ss.structure_name AS salary_structure,
    ss.base_salary,
    ss.currency,
    ss.pay_frequency,
    COUNT(DISTINCT esd.salary_detail_id) AS component_count,
    SUM(CASE WHEN sc.component_type = 'earning' THEN esd.amount ELSE 0 END) AS total_earnings,
    SUM(CASE WHEN sc.component_type = 'deduction' THEN esd.amount ELSE 0 END) AS total_deductions
FROM hr.employees e
LEFT JOIN admin.departments d ON e.department_id = d.id
LEFT JOIN hr.positions p ON e.position_id = p.position_id
LEFT JOIN hr.salary_structures ss ON p.position_id = ss.position_id AND ss.is_active = true
LEFT JOIN hr.employee_salary_details esd ON e.employee_id = esd.employee_id AND esd.is_active = true
LEFT JOIN hr.salary_components sc ON esd.component_id = sc.component_id
WHERE e.employment_status = 'active'
GROUP BY e.employee_id, e.organization_id, e.employee_number, e.first_name, e.last_name,
         d.name, p.title, ss.structure_name, ss.base_salary, ss.currency, ss.pay_frequency;

-- View: Payroll Run Summary
CREATE OR REPLACE VIEW hr.v_payroll_run_summary AS
SELECT 
    pr.payroll_run_id,
    pr.organization_id,
    pr.run_name,
    pr.pay_period_start,
    pr.pay_period_end,
    pr.payment_date,
    pr.status,
    pr.employee_count,
    pr.total_gross_pay,
    pr.total_deductions,
    pr.total_net_pay,
    pr.processed_at,
    processor.first_name || ' ' || processor.last_name AS processed_by_name,
    pr.approved_at,
    approver.first_name || ' ' || approver.last_name AS approved_by_name,
    COUNT(DISTINCT pd.payroll_detail_id) AS payslip_count,
    COUNT(DISTINCT CASE WHEN pd.status = 'paid' THEN pd.payroll_detail_id END) AS paid_count,
    COUNT(DISTINCT CASE WHEN pd.status = 'pending' THEN pd.payroll_detail_id END) AS pending_count
FROM hr.payroll_runs pr
LEFT JOIN hr.employees processor ON pr.processed_by = processor.employee_id
LEFT JOIN hr.employees approver ON pr.approved_by = approver.employee_id
LEFT JOIN hr.payroll_details pd ON pr.payroll_run_id = pd.payroll_run_id
GROUP BY pr.payroll_run_id, pr.organization_id, pr.run_name, pr.pay_period_start,
         pr.pay_period_end, pr.payment_date, pr.status, pr.employee_count,
         pr.total_gross_pay, pr.total_deductions, pr.total_net_pay, pr.processed_at,
         processor.first_name, processor.last_name, pr.approved_at,
         approver.first_name, approver.last_name;

-- View: Employee Payslip Details
CREATE OR REPLACE VIEW hr.v_employee_payslip_details AS
SELECT 
    pd.payroll_detail_id,
    pd.payroll_run_id,
    pd.employee_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
    e.email,
    d.name AS department_name,
    p.title AS position_title,
    pr.run_name AS payroll_run_name,
    pr.pay_period_start,
    pr.pay_period_end,
    pr.payment_date,
    pd.basic_salary,
    pd.gross_salary,
    pd.total_deductions,
    pd.total_reimbursements,
    pd.net_salary,
    pd.working_days,
    pd.present_days,
    pd.leave_days,
    pd.overtime_hours,
    pd.overtime_amount,
    pd.status,
    pd.payment_method,
    pd.paid_at
FROM hr.payroll_details pd
JOIN hr.employees e ON pd.employee_id = e.employee_id
LEFT JOIN admin.departments d ON e.department_id = d.id
LEFT JOIN hr.positions p ON e.position_id = p.position_id
JOIN hr.payroll_runs pr ON pd.payroll_run_id = pr.payroll_run_id;

-- View: Benefits Summary
CREATE OR REPLACE VIEW hr.v_benefits_summary AS
SELECT 
    b.benefit_id,
    b.organization_id,
    b.benefit_name,
    b.benefit_type,
    b.provider_name,
    b.coverage_type,
    b.employer_contribution,
    b.employee_contribution,
    b.is_mandatory,
    b.is_active,
    COUNT(DISTINCT eb.employee_benefit_id) AS enrolled_employees,
    SUM(eb.employer_contribution) AS total_employer_contribution,
    SUM(eb.employee_contribution) AS total_employee_contribution
FROM hr.benefits b
LEFT JOIN hr.employee_benefits eb ON b.benefit_id = eb.benefit_id AND eb.status = 'active'
GROUP BY b.benefit_id, b.organization_id, b.benefit_name, b.benefit_type,
         b.provider_name, b.coverage_type, b.employer_contribution,
         b.employee_contribution, b.is_mandatory, b.is_active;

-- View: Employee Benefits Enrollment
CREATE OR REPLACE VIEW hr.v_employee_benefits_enrollment AS
SELECT 
    eb.employee_benefit_id,
    eb.employee_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
    b.benefit_name,
    b.benefit_type,
    b.provider_name,
    eb.enrollment_date,
    eb.start_date,
    eb.end_date,
    eb.status,
    eb.employee_contribution,
    eb.employer_contribution,
    eb.coverage_amount,
    eb.beneficiary_name,
    eb.beneficiary_relationship
FROM hr.employee_benefits eb
JOIN hr.employees e ON eb.employee_id = e.employee_id
JOIN hr.benefits b ON eb.benefit_id = b.benefit_id;

-- View: Reimbursement Summary
CREATE OR REPLACE VIEW hr.v_reimbursement_summary AS
SELECT 
    r.reimbursement_id,
    r.employee_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
    d.name AS department_name,
    r.reimbursement_type,
    r.amount,
    r.currency,
    r.expense_date,
    r.claim_date,
    r.description,
    r.status,
    r.approved_at,
    approver.first_name || ' ' || approver.last_name AS approved_by_name,
    r.payment_date,
    CURRENT_DATE - r.claim_date AS days_pending
FROM hr.reimbursements r
JOIN hr.employees e ON r.employee_id = e.employee_id
LEFT JOIN admin.departments d ON e.department_id = d.id
LEFT JOIN hr.employees approver ON r.approved_by = approver.employee_id;

-- View: Bonus Summary
CREATE OR REPLACE VIEW hr.v_bonus_summary AS
SELECT 
    b.bonus_id,
    b.employee_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
    d.name AS department_name,
    b.bonus_type,
    b.amount,
    b.currency,
    b.bonus_period,
    b.description,
    b.status,
    b.is_taxable,
    b.approved_at,
    approver.first_name || ' ' || approver.last_name AS approved_by_name,
    b.payment_date
FROM hr.bonuses b
JOIN hr.employees e ON b.employee_id = e.employee_id
LEFT JOIN admin.departments d ON e.department_id = d.id
LEFT JOIN hr.employees approver ON b.approved_by = approver.employee_id;

-- View: Payroll Dashboard Stats
CREATE OR REPLACE VIEW hr.v_payroll_dashboard_stats AS
SELECT 
    pr.organization_id,
    COUNT(DISTINCT pr.payroll_run_id) AS total_payroll_runs,
    COUNT(DISTINCT CASE WHEN pr.status = 'processed' THEN pr.payroll_run_id END) AS processed_runs,
    COUNT(DISTINCT CASE WHEN pr.status = 'approved' THEN pr.payroll_run_id END) AS approved_runs,
    SUM(pr.total_gross_pay) AS total_gross_paid,
    SUM(pr.total_net_pay) AS total_net_paid,
    SUM(pr.employee_count) AS total_employees_paid,
    COUNT(DISTINCT pd.employee_id) AS unique_employees,
    AVG(pd.net_salary) AS average_net_salary,
    SUM(CASE WHEN r.status = 'pending' THEN r.amount ELSE 0 END) AS pending_reimbursements,
    SUM(CASE WHEN bo.status = 'pending' THEN bo.amount ELSE 0 END) AS pending_bonuses
FROM hr.payroll_runs pr
LEFT JOIN hr.payroll_details pd ON pr.payroll_run_id = pd.payroll_run_id
LEFT JOIN hr.reimbursements r ON pd.employee_id = r.employee_id
LEFT JOIN hr.bonuses bo ON pd.employee_id = bo.employee_id
WHERE pr.pay_period_start >= DATE_TRUNC('year', CURRENT_DATE)
GROUP BY pr.organization_id;

-- View: Salary Component Breakdown
CREATE OR REPLACE VIEW hr.v_salary_component_breakdown AS
SELECT 
    sc.component_id,
    sc.organization_id,
    sc.component_name,
    sc.component_type,
    sc.calculation_type,
    sc.is_taxable,
    sc.is_statutory,
    COUNT(DISTINCT esd.employee_id) AS employee_count,
    SUM(esd.amount) AS total_amount,
    AVG(esd.amount) AS average_amount
FROM hr.salary_components sc
LEFT JOIN hr.employee_salary_details esd ON sc.component_id = esd.component_id 
    AND esd.is_active = true
WHERE sc.is_active = true
GROUP BY sc.component_id, sc.organization_id, sc.component_name, sc.component_type,
         sc.calculation_type, sc.is_taxable, sc.is_statutory;

-- View: Pending Approvals
CREATE OR REPLACE VIEW hr.v_payroll_pending_approvals AS
SELECT 
    'reimbursement' AS approval_type,
    r.reimbursement_id AS item_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
    r.reimbursement_type AS item_description,
    r.amount,
    r.claim_date AS request_date,
    CURRENT_DATE - r.claim_date AS days_pending
FROM hr.reimbursements r
JOIN hr.employees e ON r.employee_id = e.employee_id
WHERE r.status = 'pending'

UNION ALL

SELECT 
    'bonus' AS approval_type,
    b.bonus_id AS item_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
    b.bonus_type AS item_description,
    b.amount,
    b.created_at::DATE AS request_date,
    CURRENT_DATE - b.created_at::DATE AS days_pending
FROM hr.bonuses b
JOIN hr.employees e ON b.employee_id = e.employee_id
WHERE b.status = 'pending';

-- Success message
SELECT 'HR Payroll & Benefits views created successfully' AS status;

