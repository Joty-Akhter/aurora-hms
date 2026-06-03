--liquibase formatted sql

--changeset easyops:061-hr-views-use-unified-name splitStatements:true
--comment: Update HR analytics views to use unified employees.name instead of first_name/last_name

SET search_path TO hr, admin, public;

-- First drop existing views that still depend on employees.first_name/last_name
DROP VIEW IF EXISTS hr.v_attendance_summary CASCADE;
DROP VIEW IF EXISTS hr.v_timesheet_summary CASCADE;
DROP VIEW IF EXISTS hr.v_leave_request_summary CASCADE;
DROP VIEW IF EXISTS hr.v_leave_balance_summary CASCADE;
DROP VIEW IF EXISTS hr.v_pending_leave_requests CASCADE;
DROP VIEW IF EXISTS hr.v_employee_attendance_report CASCADE;
DROP VIEW IF EXISTS hr.v_shift_schedule_summary CASCADE;
DROP VIEW IF EXISTS hr.v_overtime_report CASCADE;

DROP VIEW IF EXISTS hr.v_employee_salary_summary CASCADE;
DROP VIEW IF EXISTS hr.v_payroll_run_summary CASCADE;
DROP VIEW IF EXISTS hr.v_employee_payslip_details CASCADE;
DROP VIEW IF EXISTS hr.v_employee_benefits_enrollment CASCADE;
DROP VIEW IF EXISTS hr.v_reimbursement_summary CASCADE;
DROP VIEW IF EXISTS hr.v_bonus_summary CASCADE;
DROP VIEW IF EXISTS hr.v_payroll_pending_approvals CASCADE;

DROP VIEW IF EXISTS hr.v_employee_goals_summary CASCADE;
DROP VIEW IF EXISTS hr.v_performance_review_summary CASCADE;
DROP VIEW IF EXISTS hr.v_360_feedback_summary CASCADE;
DROP VIEW IF EXISTS hr.v_development_plan_summary CASCADE;
DROP VIEW IF EXISTS hr.v_training_certification_summary CASCADE;
DROP VIEW IF EXISTS hr.v_one_on_one_summary CASCADE;
DROP VIEW IF EXISTS hr.v_goal_progress_tracking CASCADE;

-- ============================================
-- Time & Attendance views
-- ============================================

-- v_attendance_summary
CREATE OR REPLACE VIEW hr.v_attendance_summary AS
SELECT 
    ar.attendance_id,
    ar.employee_id,
    e.employee_number,
    e.name AS employee_name,
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

-- v_timesheet_summary
CREATE OR REPLACE VIEW hr.v_timesheet_summary AS
SELECT 
    t.timesheet_id,
    t.employee_id,
    e.employee_number,
    e.name AS employee_name,
    d.name AS department_name,
    t.week_start_date,
    t.week_end_date,
    t.total_hours,
    t.regular_hours,
    t.overtime_hours,
    t.status,
    t.submitted_at,
    t.approved_at,
    approver.name AS approved_by_name,
    COUNT(tl.line_id) AS line_count
FROM hr.timesheets t
JOIN hr.employees e ON t.employee_id = e.employee_id
LEFT JOIN admin.departments d ON e.department_id = d.id
LEFT JOIN hr.employees approver ON t.approved_by = approver.employee_id
LEFT JOIN hr.timesheet_lines tl ON t.timesheet_id = tl.timesheet_id
GROUP BY t.timesheet_id, t.employee_id, e.employee_number, e.name,
         d.name, t.week_start_date, t.week_end_date, t.total_hours, t.regular_hours,
         t.overtime_hours, t.status, t.submitted_at, t.approved_at, approver.name;

-- v_leave_request_summary
CREATE OR REPLACE VIEW hr.v_leave_request_summary AS
SELECT 
    lr.leave_request_id,
    lr.employee_id,
    e.employee_number,
    e.name AS employee_name,
    d.name AS department_name,
    lt.type_name AS leave_type,
    lr.start_date,
    lr.end_date,
    lr.total_days,
    lr.reason,
    lr.status,
    lr.approved_at,
    approver.name AS approved_by_name,
    lr.rejection_reason
FROM hr.leave_requests lr
JOIN hr.employees e ON lr.employee_id = e.employee_id
LEFT JOIN admin.departments d ON e.department_id = d.id
JOIN hr.leave_types lt ON lr.leave_type_id = lt.leave_type_id
LEFT JOIN hr.employees approver ON lr.approved_by = approver.employee_id;

-- v_leave_balance_summary
CREATE OR REPLACE VIEW hr.v_leave_balance_summary AS
SELECT 
    lb.balance_id,
    lb.employee_id,
    e.employee_number,
    e.name AS employee_name,
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

-- v_pending_leave_requests
CREATE OR REPLACE VIEW hr.v_pending_leave_requests AS
SELECT 
    lr.leave_request_id,
    lr.employee_id,
    e.employee_number,
    e.name AS employee_name,
    e.email,
    d.name AS department_name,
    lt.type_name AS leave_type,
    lr.start_date,
    lr.end_date,
    lr.total_days,
    lr.reason,
    lr.created_at,
    CURRENT_DATE - lr.start_date AS days_until_leave,
    manager.name AS manager_name
FROM hr.leave_requests lr
JOIN hr.employees e ON lr.employee_id = e.employee_id
LEFT JOIN admin.departments d ON e.department_id = d.id
JOIN hr.leave_types lt ON lr.leave_type_id = lt.leave_type_id
LEFT JOIN hr.employees manager ON e.manager_id = manager.employee_id
WHERE lr.status = 'pending'
ORDER BY lr.start_date;

-- v_employee_attendance_report
CREATE OR REPLACE VIEW hr.v_employee_attendance_report AS
SELECT 
    e.employee_id,
    e.organization_id,
    e.employee_number,
    e.name AS employee_name,
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
GROUP BY e.employee_id, e.organization_id, e.employee_number, e.name, d.name;

-- v_shift_schedule_summary
CREATE OR REPLACE VIEW hr.v_shift_schedule_summary AS
SELECT 
    ss.schedule_id,
    ss.employee_id,
    e.employee_number,
    e.name AS employee_name,
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

-- v_overtime_report
CREATE OR REPLACE VIEW hr.v_overtime_report AS
SELECT 
    e.employee_id,
    e.organization_id,
    e.employee_number,
    e.name AS employee_name,
    d.name AS department_name,
    DATE_TRUNC('month', ar.attendance_date) AS month,
    COUNT(DISTINCT ar.attendance_date) AS days_with_overtime,
    ROUND(SUM(ar.overtime_hours), 2) AS total_overtime_hours,
    ROUND(AVG(ar.overtime_hours), 2) AS avg_overtime_per_day
FROM hr.employees e
LEFT JOIN admin.departments d ON e.department_id = d.id
JOIN hr.attendance_records ar ON e.employee_id = ar.employee_id
WHERE ar.overtime_hours > 0
GROUP BY e.employee_id, e.organization_id, e.employee_number, e.name, d.name, DATE_TRUNC('month', ar.attendance_date);

-- ============================================
-- Payroll & Benefits views
-- ============================================

-- v_employee_salary_summary
CREATE OR REPLACE VIEW hr.v_employee_salary_summary AS
SELECT 
    e.employee_id,
    e.organization_id,
    e.employee_number,
    e.name AS employee_name,
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
GROUP BY e.employee_id, e.organization_id, e.employee_number, e.name,
         d.name, p.title, ss.structure_name, ss.base_salary, ss.currency, ss.pay_frequency;

-- v_payroll_run_summary
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
    processor.name AS processed_by_name,
    pr.approved_at,
    approver.name AS approved_by_name,
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
         processor.name, pr.approved_at, approver.name;

-- v_employee_payslip_details
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

-- v_employee_benefits_enrollment
CREATE OR REPLACE VIEW hr.v_employee_benefits_enrollment AS
SELECT 
    eb.employee_benefit_id,
    eb.employee_id,
    e.employee_number,
    e.name AS employee_name,
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

-- v_reimbursement_summary
CREATE OR REPLACE VIEW hr.v_reimbursement_summary AS
SELECT 
    r.reimbursement_id,
    r.employee_id,
    e.employee_number,
    e.name AS employee_name,
    d.name AS department_name,
    r.reimbursement_type,
    r.amount,
    r.currency,
    r.expense_date,
    r.claim_date,
    r.description,
    r.status,
    r.approved_at,
    approver.name AS approved_by_name,
    r.payment_date,
    CURRENT_DATE - r.claim_date AS days_pending
FROM hr.reimbursements r
JOIN hr.employees e ON r.employee_id = e.employee_id
LEFT JOIN admin.departments d ON e.department_id = d.id
LEFT JOIN hr.employees approver ON r.approved_by = approver.employee_id;

-- v_bonus_summary
CREATE OR REPLACE VIEW hr.v_bonus_summary AS
SELECT 
    b.bonus_id,
    b.employee_id,
    e.employee_number,
    e.name AS employee_name,
    d.name AS department_name,
    b.bonus_type,
    b.amount,
    b.currency,
    b.bonus_period,
    b.description,
    b.status,
    b.is_taxable,
    b.approved_at,
    approver.name AS approved_by_name,
    b.payment_date
FROM hr.bonuses b
JOIN hr.employees e ON b.employee_id = e.employee_id
LEFT JOIN admin.departments d ON e.department_id = d.id
LEFT JOIN hr.employees approver ON b.approved_by = approver.employee_id;

-- v_payroll_pending_approvals
CREATE OR REPLACE VIEW hr.v_payroll_pending_approvals AS
SELECT 
    'reimbursement' AS approval_type,
    r.reimbursement_id AS item_id,
    e.employee_number,
    e.name AS employee_name,
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
    e.name AS employee_name,
    b.bonus_type AS item_description,
    b.amount,
    b.created_at::DATE AS request_date,
    CURRENT_DATE - b.created_at::DATE AS days_pending
FROM hr.bonuses b
JOIN hr.employees e ON b.employee_id = e.employee_id
WHERE b.status = 'pending';

-- ============================================
-- Performance views
-- ============================================

-- v_employee_goals_summary
CREATE OR REPLACE VIEW hr.v_employee_goals_summary AS
SELECT 
    g.employee_id,
    e.employee_number,
    e.name AS employee_name,
    d.name AS department_name,
    pc.cycle_name,
    COUNT(DISTINCT g.goal_id) AS total_goals,
    COUNT(DISTINCT CASE WHEN g.status = 'completed' THEN g.goal_id END) AS completed_goals,
    COUNT(DISTINCT CASE WHEN g.status = 'in_progress' THEN g.goal_id END) AS in_progress_goals,
    COUNT(DISTINCT CASE WHEN g.status = 'not_started' THEN g.goal_id END) AS not_started_goals,
    COUNT(DISTINCT CASE WHEN g.status = 'delayed' THEN g.goal_id END) AS delayed_goals,
    ROUND(AVG(g.progress_percentage), 2) AS average_progress,
    SUM(g.weight) AS total_weight
FROM hr.goals g
JOIN hr.employees e ON g.employee_id = e.employee_id
LEFT JOIN admin.departments d ON e.department_id = d.id
LEFT JOIN hr.performance_cycles pc ON g.cycle_id = pc.cycle_id
GROUP BY g.employee_id, e.employee_number, e.name, d.name, pc.cycle_name;

-- v_performance_review_summary
CREATE OR REPLACE VIEW hr.v_performance_review_summary AS
SELECT 
    pr.review_id,
    pr.employee_id,
    e.employee_number,
    e.name AS employee_name,
    d.name AS department_name,
    p.title AS position_title,
    reviewer.name AS reviewer_name,
    pc.cycle_name,
    pr.review_type,
    pr.review_date,
    pr.review_period_start,
    pr.review_period_end,
    pr.overall_rating,
    pr.status,
    pr.self_review_completed,
    pr.manager_review_completed,
    pr.hr_review_completed,
    pr.recommended_action,
    pr.submitted_at,
    pr.approved_at,
    COUNT(DISTINCT rr.rating_id) AS rating_count,
    ROUND(AVG(rr.rating_value), 2) AS average_competency_rating
FROM hr.performance_reviews pr
JOIN hr.employees e ON pr.employee_id = e.employee_id
LEFT JOIN admin.departments d ON e.department_id = d.id
LEFT JOIN hr.positions p ON e.position_id = p.position_id
JOIN hr.employees reviewer ON pr.reviewer_id = reviewer.employee_id
LEFT JOIN hr.performance_cycles pc ON pr.cycle_id = pc.cycle_id
LEFT JOIN hr.review_ratings rr ON pr.review_id = rr.review_id
GROUP BY pr.review_id, pr.employee_id, e.employee_number, e.name,
         d.name, p.title, reviewer.name, pc.cycle_name,
         pr.review_type, pr.review_date, pr.review_period_start, pr.review_period_end,
         pr.overall_rating, pr.status, pr.self_review_completed, pr.manager_review_completed,
         pr.hr_review_completed, pr.recommended_action, pr.submitted_at, pr.approved_at;

-- v_360_feedback_summary
CREATE OR REPLACE VIEW hr.v_360_feedback_summary AS
SELECT 
    f.employee_id,
    e.employee_number,
    e.name AS employee_name,
    d.name AS department_name,
    pc.cycle_name,
    COUNT(DISTINCT f.feedback_id) AS total_feedback_received,
    COUNT(DISTINCT CASE WHEN f.reviewer_relationship = 'peer' THEN f.feedback_id END) AS peer_feedback_count,
    COUNT(DISTINCT CASE WHEN f.reviewer_relationship = 'manager' THEN f.feedback_id END) AS manager_feedback_count,
    COUNT(DISTINCT CASE WHEN f.reviewer_relationship = 'direct_report' THEN f.feedback_id END) AS direct_report_feedback_count,
    COUNT(DISTINCT CASE WHEN f.reviewer_relationship = 'other' THEN f.feedback_id END) AS other_feedback_count,
    ROUND(AVG(f.overall_rating), 2) AS average_rating,
    COUNT(DISTINCT CASE WHEN f.would_recommend_collaboration = true THEN f.feedback_id END) AS positive_collaboration_count
FROM hr.feedback_360 f
JOIN hr.employees e ON f.employee_id = e.employee_id
LEFT JOIN admin.departments d ON e.department_id = d.id
LEFT JOIN hr.performance_cycles pc ON f.cycle_id = pc.cycle_id
WHERE f.status = 'submitted'
GROUP BY f.employee_id, e.employee_number, e.name, d.name, pc.cycle_name;

-- v_development_plan_summary
CREATE OR REPLACE VIEW hr.v_development_plan_summary AS
SELECT 
    dp.plan_id,
    dp.employee_id,
    e.employee_number,
    e.name AS employee_name,
    d.name AS department_name,
    dp.plan_name,
    dp.plan_type,
    dp.start_date,
    dp.target_completion_date,
    dp.actual_completion_date,
    dp.status,
    manager.name AS manager_name,
    CASE 
        WHEN dp.actual_completion_date IS NOT NULL THEN 100
        WHEN dp.target_completion_date < CURRENT_DATE THEN 0
        ELSE ROUND((CURRENT_DATE - dp.start_date)::NUMERIC / 
             NULLIF((dp.target_completion_date - dp.start_date)::NUMERIC, 0) * 100, 0)
    END AS estimated_progress_percentage
FROM hr.development_plans dp
JOIN hr.employees e ON dp.employee_id = e.employee_id
LEFT JOIN admin.departments d ON e.department_id = d.id
LEFT JOIN hr.employees manager ON dp.manager_id = manager.employee_id;

-- v_training_certification_summary
CREATE OR REPLACE VIEW hr.v_training_certification_summary AS
SELECT 
    tc.employee_id,
    e.employee_number,
    e.name AS employee_name,
    d.name AS department_name,
    COUNT(DISTINCT tc.training_id) AS total_trainings,
    COUNT(DISTINCT CASE WHEN tc.status = 'completed' THEN tc.training_id END) AS completed_trainings,
    COUNT(DISTINCT CASE WHEN tc.status = 'in_progress' THEN tc.training_id END) AS in_progress_trainings,
    COUNT(DISTINCT CASE WHEN tc.expiry_date >= CURRENT_DATE THEN tc.training_id END) AS active_certifications,
    COUNT(DISTINCT CASE WHEN tc.expiry_date < CURRENT_DATE THEN tc.training_id END) AS expired_certifications,
    SUM(tc.hours_spent) AS total_training_hours,
    SUM(tc.cost) AS total_training_cost,
    ROUND(AVG(tc.score), 2) AS average_score
FROM hr.training_certifications tc
JOIN hr.employees e ON tc.employee_id = e.employee_id
LEFT JOIN admin.departments d ON e.department_id = d.id
GROUP BY tc.employee_id, e.employee_number, e.name, d.name;

-- v_one_on_one_summary
CREATE OR REPLACE VIEW hr.v_one_on_one_summary AS
SELECT 
    oom.employee_id,
    e.employee_number,
    e.name AS employee_name,
    manager.name AS manager_name,
    d.name AS department_name,
    COUNT(DISTINCT oom.meeting_id) AS total_meetings,
    COUNT(DISTINCT CASE WHEN oom.status = 'completed' THEN oom.meeting_id END) AS completed_meetings,
    COUNT(DISTINCT CASE WHEN oom.status = 'scheduled' THEN oom.meeting_id END) AS scheduled_meetings,
    COUNT(DISTINCT CASE WHEN oom.status = 'cancelled' THEN oom.meeting_id END) AS cancelled_meetings,
    SUM(oom.duration_minutes) AS total_meeting_minutes,
    MAX(oom.meeting_date) AS last_meeting_date,
    MAX(oom.next_meeting_date) AS next_meeting_date
FROM hr.one_on_one_meetings oom
JOIN hr.employees e ON oom.employee_id = e.employee_id
JOIN hr.employees manager ON oom.manager_id = manager.employee_id
LEFT JOIN admin.departments d ON e.department_id = d.id
GROUP BY oom.employee_id, e.employee_number, e.name, manager.name, d.name;

-- v_goal_progress_tracking
CREATE OR REPLACE VIEW hr.v_goal_progress_tracking AS
SELECT 
    g.goal_id,
    g.employee_id,
    e.employee_number,
    e.name AS employee_name,
    g.goal_title,
    g.goal_category,
    g.priority,
    g.weight,
    g.start_date,
    g.target_date,
    g.completion_date,
    g.status,
    g.target_value,
    g.current_value,
    g.unit_of_measure,
    g.progress_percentage,
    CASE 
        WHEN g.completion_date IS NOT NULL THEN 'completed'
        WHEN g.target_date < CURRENT_DATE AND g.status != 'completed' THEN 'overdue'
        WHEN g.target_date - CURRENT_DATE <= 7 THEN 'due_soon'
        ELSE 'on_track'
    END AS timeline_status,
    COUNT(DISTINCT gu.update_id) AS update_count,
    MAX(gu.update_date) AS last_update_date
FROM hr.goals g
JOIN hr.employees e ON g.employee_id = e.employee_id
LEFT JOIN hr.goal_updates gu ON g.goal_id = gu.goal_id
GROUP BY g.goal_id, g.employee_id, e.employee_number, e.name,
         g.goal_title, g.goal_category, g.priority, g.weight, g.start_date,
         g.target_date, g.completion_date, g.status, g.target_value,
         g.current_value, g.unit_of_measure, g.progress_percentage;

-- Success notice
DO $$
BEGIN
    RAISE NOTICE '✅ HR views updated to use employees.name instead of first_name/last_name';
END $$;

