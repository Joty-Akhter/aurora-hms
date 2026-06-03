-- Organization-level time & attendance rules for payroll (OT rate, missing-day LOP, standard hours for OT divisor).
--changeset easyops:099-hr-payroll-time-attendance-policy splitStatements:false

CREATE TABLE IF NOT EXISTS hr.payroll_time_attendance_policy (
    organization_id UUID PRIMARY KEY REFERENCES admin.organizations(id) ON DELETE CASCADE,
    overtime_rate_multiplier DECIMAL(5,2) NOT NULL DEFAULT 1.50,
    infer_missing_weekday_lop BOOLEAN NOT NULL DEFAULT TRUE,
    standard_hours_per_day DECIMAL(4,2) NOT NULL DEFAULT 8.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE hr.payroll_time_attendance_policy IS 'Per-organization payroll rules for attendance roll-up (OT multiplier, infer LOP for weekdays without rows, hours/day for OT rate).';
COMMENT ON COLUMN hr.payroll_time_attendance_policy.overtime_rate_multiplier IS 'Multiplier on hourly rate for OT pay (e.g. 1.5 for time-and-a-half).';
COMMENT ON COLUMN hr.payroll_time_attendance_policy.infer_missing_weekday_lop IS 'When true, weekdays in the pay period with no attendance row count as 1 LOP day each (in addition to explicit LOP statuses).';
COMMENT ON COLUMN hr.payroll_time_attendance_policy.standard_hours_per_day IS 'Denominator hours per day when deriving hourly rate from basic ÷ (working days × hours).';

-- Support fractional present days (e.g. half-day = 0.5).
-- Drop the dependent view first; PostgreSQL forbids altering a column type
-- that is referenced by a view. We recreate the identical view afterwards.
DROP VIEW IF EXISTS hr.v_employee_payslip_details;

ALTER TABLE hr.payroll_details
    ALTER COLUMN present_days TYPE DECIMAL(5,2) USING (
        CASE WHEN present_days IS NULL THEN NULL ELSE present_days::DECIMAL(5,2) END
    );

CREATE OR REPLACE VIEW hr.v_employee_payslip_details AS
SELECT
    pd.payroll_detail_id,
    pd.payroll_run_id,
    pd.employee_id,
    e.employee_number,
    e.name AS employee_name,
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
