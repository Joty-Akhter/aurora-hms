--liquibase formatted sql

--changeset easyops:031-create-hr-time-attendance-views splitStatements:true

-- =====================================================
-- HR TIME & ATTENDANCE ANALYTICS VIEWS
-- Phase 5.2: Time & Attendance Management
-- =====================================================

-- View: Attendance Summary
CREATE OR REPLACE VIEW hr.v_attendance_summary AS
SELECT 
    ar.attendance_id,
    ar.employee_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
    d.name AS department_name,
    ar.attendance_date,
    ar.clock_in_time,
    ar.clock_out_time,
    ar.total_hours,
    ar.regular_hours,
    ar.overtime_hours,
    ar.status,
    ar.work_location,
    CASE 
        WHEN ar.clock_in_time IS NOT NULL AND ar.clock_out_time IS NOT NULL THEN 'complete'
        WHEN ar.clock_in_time IS NOT NULL THEN 'in_progress'
        ELSE 'not_started'
    END AS attendance_status
FROM hr.attendance_records ar
JOIN hr.employees e ON ar.employee_id = e.employee_id
LEFT JOIN admin.departments d ON e.department_id = d.id;

-- View: Timesheet Summary
CREATE OR REPLACE VIEW hr.v_timesheet_summary AS
SELECT 
    t.timesheet_id,
    t.employee_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
    d.name AS department_name,
    t.week_start_date,
    t.week_end_date,
    t.total_hours,
    t.regular_hours,
    t.overtime_hours,
    t.status,
    t.submitted_at,
    t.approved_at,
    approver.first_name || ' ' || approver.last_name AS approved_by_name,
    COUNT(tl.line_id) AS line_count
FROM hr.timesheets t
JOIN hr.employees e ON t.employee_id = e.employee_id
LEFT JOIN admin.departments d ON e.department_id = d.id
LEFT JOIN hr.employees approver ON t.approved_by = approver.employee_id
LEFT JOIN hr.timesheet_lines tl ON t.timesheet_id = tl.timesheet_id
GROUP BY t.timesheet_id, t.employee_id, e.employee_number, e.first_name, e.last_name,
         d.name, t.week_start_date, t.week_end_date, t.total_hours, t.regular_hours,
         t.overtime_hours, t.status, t.submitted_at, t.approved_at, 
         approver.first_name, approver.last_name;

-- View: Leave Request Summary
CREATE OR REPLACE VIEW hr.v_leave_request_summary AS
SELECT 
    lr.leave_request_id,
    lr.employee_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
    d.name AS department_name,
    lt.type_name AS leave_type,
    lr.start_date,
    lr.end_date,
    lr.total_days,
    lr.reason,
    lr.status,
    lr.approved_at,
    approver.first_name || ' ' || approver.last_name AS approved_by_name,
    lr.rejection_reason
FROM hr.leave_requests lr
JOIN hr.employees e ON lr.employee_id = e.employee_id
LEFT JOIN admin.departments d ON e.department_id = d.id
JOIN hr.leave_types lt ON lr.leave_type_id = lt.leave_type_id
LEFT JOIN hr.employees approver ON lr.approved_by = approver.employee_id;

-- View: Leave Balance Summary
CREATE OR REPLACE VIEW hr.v_leave_balance_summary AS
SELECT 
    lb.balance_id,
    lb.employee_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
    lt.type_name AS leave_type,
    lb.year,
    lb.allocated_days,
    lb.used_days,
    lb.carried_forward_days,
    lb.balance_days,
    CASE 
        WHEN lb.balance_days < 0 THEN 'deficit'
        WHEN lb.balance_days = 0 THEN 'exhausted'
        WHEN lb.balance_days < 5 THEN 'low'
        ELSE 'good'
    END AS balance_status
FROM hr.leave_balances lb
JOIN hr.employees e ON lb.employee_id = e.employee_id
JOIN hr.leave_types lt ON lb.leave_type_id = lt.leave_type_id;

-- View: Attendance Dashboard Stats
CREATE OR REPLACE VIEW hr.v_attendance_dashboard_stats AS
SELECT 
    ar.organization_id,
    ar.attendance_date,
    COUNT(DISTINCT ar.employee_id) AS total_employees_marked,
    COUNT(DISTINCT CASE WHEN ar.status = 'present' THEN ar.employee_id END) AS present_count,
    COUNT(DISTINCT CASE WHEN ar.status = 'absent' THEN ar.employee_id END) AS absent_count,
    COUNT(DISTINCT CASE WHEN ar.status = 'late' THEN ar.employee_id END) AS late_count,
    COUNT(DISTINCT CASE WHEN ar.status = 'half_day' THEN ar.employee_id END) AS half_day_count,
    ROUND(AVG(ar.total_hours), 2) AS avg_hours,
    ROUND(SUM(ar.overtime_hours), 2) AS total_overtime
FROM hr.attendance_records ar
WHERE ar.attendance_date >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY ar.organization_id, ar.attendance_date;

-- View: Pending Leave Requests
CREATE OR REPLACE VIEW hr.v_pending_leave_requests AS
SELECT 
    lr.leave_request_id,
    lr.employee_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
    e.email,
    d.name AS department_name,
    lt.type_name AS leave_type,
    lr.start_date,
    lr.end_date,
    lr.total_days,
    lr.reason,
    lr.created_at,
    CURRENT_DATE - lr.start_date AS days_until_leave,
    manager.first_name || ' ' || manager.last_name AS manager_name
FROM hr.leave_requests lr
JOIN hr.employees e ON lr.employee_id = e.employee_id
LEFT JOIN admin.departments d ON e.department_id = d.id
JOIN hr.leave_types lt ON lr.leave_type_id = lt.leave_type_id
LEFT JOIN hr.employees manager ON e.manager_id = manager.employee_id
WHERE lr.status = 'pending'
ORDER BY lr.start_date;

-- View: Employee Attendance Report
CREATE OR REPLACE VIEW hr.v_employee_attendance_report AS
SELECT 
    e.employee_id,
    e.organization_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
    d.name AS department_name,
    COUNT(DISTINCT ar.attendance_date) AS days_worked,
    COUNT(DISTINCT CASE WHEN ar.status = 'present' THEN ar.attendance_date END) AS present_days,
    COUNT(DISTINCT CASE WHEN ar.status = 'absent' THEN ar.attendance_date END) AS absent_days,
    COUNT(DISTINCT CASE WHEN ar.status = 'late' THEN ar.attendance_date END) AS late_days,
    ROUND(SUM(ar.total_hours), 2) AS total_hours_worked,
    ROUND(SUM(ar.overtime_hours), 2) AS total_overtime_hours,
    ROUND(AVG(ar.total_hours), 2) AS avg_daily_hours
FROM hr.employees e
LEFT JOIN admin.departments d ON e.department_id = d.id
LEFT JOIN hr.attendance_records ar ON e.employee_id = ar.employee_id 
    AND ar.attendance_date >= DATE_TRUNC('month', CURRENT_DATE)
WHERE e.employment_status = 'active'
GROUP BY e.employee_id, e.organization_id, e.employee_number, e.first_name, 
         e.last_name, d.name;

-- View: Holiday Calendar
CREATE OR REPLACE VIEW hr.v_holiday_calendar AS
SELECT 
    h.holiday_id,
    h.organization_id,
    h.holiday_name,
    h.holiday_date,
    h.holiday_type,
    h.description,
    EXTRACT(DOW FROM h.holiday_date) AS day_of_week,
    TO_CHAR(h.holiday_date, 'Day') AS day_name,
    TO_CHAR(h.holiday_date, 'Month') AS month_name,
    CASE 
        WHEN h.holiday_date < CURRENT_DATE THEN 'past'
        WHEN h.holiday_date = CURRENT_DATE THEN 'today'
        WHEN h.holiday_date > CURRENT_DATE THEN 'upcoming'
    END AS holiday_status
FROM hr.holidays h
WHERE h.is_active = true
ORDER BY h.holiday_date;

-- View: Shift Schedule Summary
CREATE OR REPLACE VIEW hr.v_shift_schedule_summary AS
SELECT 
    ss.schedule_id,
    ss.employee_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
    d.name AS department_name,
    ss.shift_date,
    ss.shift_name,
    ss.start_time,
    ss.end_time,
    ss.break_duration,
    EXTRACT(EPOCH FROM (ss.end_time - ss.start_time))/3600 - ss.break_duration/60.0 AS scheduled_hours,
    ss.is_overtime,
    CASE 
        WHEN ss.shift_date < CURRENT_DATE THEN 'past'
        WHEN ss.shift_date = CURRENT_DATE THEN 'today'
        WHEN ss.shift_date > CURRENT_DATE THEN 'upcoming'
    END AS shift_status
FROM hr.shift_schedules ss
JOIN hr.employees e ON ss.employee_id = e.employee_id
LEFT JOIN admin.departments d ON e.department_id = d.id;

-- View: Overtime Report
CREATE OR REPLACE VIEW hr.v_overtime_report AS
SELECT 
    e.employee_id,
    e.organization_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
    d.name AS department_name,
    DATE_TRUNC('month', ar.attendance_date) AS month,
    COUNT(DISTINCT ar.attendance_date) AS days_with_overtime,
    ROUND(SUM(ar.overtime_hours), 2) AS total_overtime_hours,
    ROUND(AVG(ar.overtime_hours), 2) AS avg_overtime_per_day
FROM hr.employees e
LEFT JOIN admin.departments d ON e.department_id = d.id
JOIN hr.attendance_records ar ON e.employee_id = ar.employee_id
WHERE ar.overtime_hours > 0
GROUP BY e.employee_id, e.organization_id, e.employee_number, e.first_name, 
         e.last_name, d.name, DATE_TRUNC('month', ar.attendance_date);

-- Success message
DO $$
BEGIN
    RAISE NOTICE '✅ HR Time & Attendance views created successfully';
END $$;

