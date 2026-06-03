--liquibase formatted sql

-- Phase B (HR-LV-03, HR-LV-02): leave-payroll bridge flags + optional holiday scope for payroll LOP/denominator.
--changeset easyops:108-hr-phase-b-leave-bridge-holiday-scope splitStatements:false

SET search_path TO hr, admin, public;

ALTER TABLE hr.holidays
    ADD COLUMN IF NOT EXISTS department_id UUID REFERENCES admin.departments (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS employee_id UUID REFERENCES hr.employees (employee_id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_holiday_department ON hr.holidays (department_id);
CREATE INDEX IF NOT EXISTS idx_holiday_employee ON hr.holidays (employee_id);

COMMENT ON COLUMN hr.holidays.department_id IS 'Optional payroll scope: applies to employees in this organization department (admin.departments). Null with null employee_id = organization-wide.';
COMMENT ON COLUMN hr.holidays.employee_id IS 'Optional payroll scope: applies only to this employee. Null = department-wide or org-wide per department_id.';

ALTER TABLE hr.payroll_time_attendance_policy
    ADD COLUMN IF NOT EXISTS leave_payroll_bridge_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS unpaid_approved_leave_counts_as_lop BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS exclude_active_holidays_from_working_days BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS exclude_active_holidays_from_lop_inference BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN hr.payroll_time_attendance_policy.leave_payroll_bridge_enabled IS 'HR-LV-03: approved paid leave suppresses inferred LOP on weekdays without attendance; unpaid handling follows unpaid_approved_leave_counts_as_lop.';
COMMENT ON COLUMN hr.payroll_time_attendance_policy.unpaid_approved_leave_counts_as_lop IS 'When leave bridge is on and infer_missing_weekday_lop is on: weekdays covered only by approved unpaid leave count as LOP.';
COMMENT ON COLUMN hr.payroll_time_attendance_policy.exclude_active_holidays_from_working_days IS 'Subtract applicable active holidays (weekdays) from payroll working-day denominator.';
COMMENT ON COLUMN hr.payroll_time_attendance_policy.exclude_active_holidays_from_lop_inference IS 'Do not infer LOP on weekdays that are applicable organization/department/employee holidays.';
