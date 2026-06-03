--liquibase formatted sql

--changeset easyops:030-create-hr-time-attendance-schema splitStatements:false

-- =====================================================
-- HR TIME & ATTENDANCE SCHEMA
-- Phase 5.2: Time & Attendance Management
-- =====================================================

-- Attendance Records Table
CREATE TABLE hr.attendance_records (
    attendance_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    attendance_date DATE NOT NULL,
    clock_in_time TIMESTAMP,
    clock_out_time TIMESTAMP,
    break_start_time TIMESTAMP,
    break_end_time TIMESTAMP,
    total_hours DECIMAL(5,2),
    regular_hours DECIMAL(5,2),
    overtime_hours DECIMAL(5,2),
    status VARCHAR(50) DEFAULT 'present',
    work_location VARCHAR(100),
    notes TEXT,
    approved_by UUID REFERENCES hr.employees(employee_id),
    approved_at TIMESTAMP,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(employee_id, attendance_date)
);

CREATE INDEX idx_attendance_employee ON hr.attendance_records(employee_id);
CREATE INDEX idx_attendance_organization ON hr.attendance_records(organization_id);
CREATE INDEX idx_attendance_date ON hr.attendance_records(attendance_date);
CREATE INDEX idx_attendance_status ON hr.attendance_records(status);

-- Timesheets Table
CREATE TABLE hr.timesheets (
    timesheet_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    week_start_date DATE NOT NULL,
    week_end_date DATE NOT NULL,
    total_hours DECIMAL(6,2),
    regular_hours DECIMAL(6,2),
    overtime_hours DECIMAL(6,2),
    status VARCHAR(50) DEFAULT 'draft',
    submitted_at TIMESTAMP,
    approved_by UUID REFERENCES hr.employees(employee_id),
    approved_at TIMESTAMP,
    rejection_reason TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(employee_id, week_start_date)
);

CREATE INDEX idx_timesheet_employee ON hr.timesheets(employee_id);
CREATE INDEX idx_timesheet_organization ON hr.timesheets(organization_id);
CREATE INDEX idx_timesheet_week ON hr.timesheets(week_start_date);
CREATE INDEX idx_timesheet_status ON hr.timesheets(status);

-- Timesheet Lines Table
CREATE TABLE hr.timesheet_lines (
    line_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timesheet_id UUID NOT NULL REFERENCES hr.timesheets(timesheet_id) ON DELETE CASCADE,
    work_date DATE NOT NULL,
    project_code VARCHAR(50),
    task_description TEXT,
    hours_worked DECIMAL(5,2) NOT NULL,
    overtime_hours DECIMAL(5,2) DEFAULT 0,
    billable BOOLEAN DEFAULT false,
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_timesheet_line_timesheet ON hr.timesheet_lines(timesheet_id);
CREATE INDEX idx_timesheet_line_date ON hr.timesheet_lines(work_date);

-- Leave Types Table
CREATE TABLE hr.leave_types (
    leave_type_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    type_name VARCHAR(100) NOT NULL,
    description TEXT,
    is_paid BOOLEAN DEFAULT true,
    max_days_per_year INTEGER,
    requires_approval BOOLEAN DEFAULT true,
    carry_forward BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, type_name)
);

CREATE INDEX idx_leave_type_organization ON hr.leave_types(organization_id);
CREATE INDEX idx_leave_type_active ON hr.leave_types(is_active);

-- Leave Requests Table
CREATE TABLE hr.leave_requests (
    leave_request_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    leave_type_id UUID NOT NULL REFERENCES hr.leave_types(leave_type_id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days DECIMAL(4,1) NOT NULL,
    reason TEXT,
    status VARCHAR(50) DEFAULT 'pending',
    approved_by UUID REFERENCES hr.employees(employee_id),
    approved_at TIMESTAMP,
    rejection_reason TEXT,
    emergency_contact VARCHAR(100),
    emergency_phone VARCHAR(20),
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_leave_request_employee ON hr.leave_requests(employee_id);
CREATE INDEX idx_leave_request_organization ON hr.leave_requests(organization_id);
CREATE INDEX idx_leave_request_status ON hr.leave_requests(status);
CREATE INDEX idx_leave_request_dates ON hr.leave_requests(start_date, end_date);

-- Leave Balance Table
CREATE TABLE hr.leave_balances (
    balance_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    leave_type_id UUID NOT NULL REFERENCES hr.leave_types(leave_type_id),
    year INTEGER NOT NULL,
    allocated_days DECIMAL(5,1) DEFAULT 0,
    used_days DECIMAL(5,1) DEFAULT 0,
    carried_forward_days DECIMAL(5,1) DEFAULT 0,
    balance_days DECIMAL(5,1) GENERATED ALWAYS AS (allocated_days + carried_forward_days - used_days) STORED,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(employee_id, leave_type_id, year)
);

CREATE INDEX idx_leave_balance_employee ON hr.leave_balances(employee_id);
CREATE INDEX idx_leave_balance_organization ON hr.leave_balances(organization_id);
CREATE INDEX idx_leave_balance_year ON hr.leave_balances(year);

-- Holidays Table
CREATE TABLE hr.holidays (
    holiday_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    holiday_name VARCHAR(200) NOT NULL,
    holiday_date DATE NOT NULL,
    holiday_type VARCHAR(50) DEFAULT 'public',
    description TEXT,
    is_recurring BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_holiday_organization ON hr.holidays(organization_id);
CREATE INDEX idx_holiday_date ON hr.holidays(holiday_date);
CREATE INDEX idx_holiday_active ON hr.holidays(is_active);

-- Shift Schedules Table
CREATE TABLE hr.shift_schedules (
    schedule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    shift_date DATE NOT NULL,
    shift_name VARCHAR(100),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_duration INTEGER DEFAULT 0,
    is_overtime BOOLEAN DEFAULT false,
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_shift_schedule_employee ON hr.shift_schedules(employee_id);
CREATE INDEX idx_shift_schedule_organization ON hr.shift_schedules(organization_id);
CREATE INDEX idx_shift_schedule_date ON hr.shift_schedules(shift_date);

-- =====================================================
-- TRIGGERS FOR UPDATED_AT
-- =====================================================

CREATE OR REPLACE FUNCTION hr.update_attendance_records_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_attendance_records_updated_at
    BEFORE UPDATE ON hr.attendance_records
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_attendance_records_updated_at();

CREATE OR REPLACE FUNCTION hr.update_timesheets_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_timesheets_updated_at
    BEFORE UPDATE ON hr.timesheets
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_timesheets_updated_at();

CREATE OR REPLACE FUNCTION hr.update_timesheet_lines_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_timesheet_lines_updated_at
    BEFORE UPDATE ON hr.timesheet_lines
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_timesheet_lines_updated_at();


CREATE OR REPLACE FUNCTION hr.update_leave_types_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_leave_types_updated_at
    BEFORE UPDATE ON hr.leave_types
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_leave_types_updated_at();


CREATE OR REPLACE FUNCTION hr.update_leave_requests_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_leave_requests_updated_at
    BEFORE UPDATE ON hr.leave_requests
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_leave_requests_updated_at();


CREATE OR REPLACE FUNCTION hr.update_leave_balances_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_leave_balances_updated_at
    BEFORE UPDATE ON hr.leave_balances
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_leave_balances_updated_at();


CREATE OR REPLACE FUNCTION hr.update_holidays_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_holidays_updated_at
    BEFORE UPDATE ON hr.holidays
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_holidays_updated_at();


CREATE OR REPLACE FUNCTION hr.update_shift_schedules_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_shift_schedules_updated_at
    BEFORE UPDATE ON hr.shift_schedules
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_shift_schedules_updated_at();


-- HR Time & Attendance schema complete


