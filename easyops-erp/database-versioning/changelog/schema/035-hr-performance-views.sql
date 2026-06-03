--liquibase formatted sql

--changeset easyops:035-create-hr-performance-views splitStatements:true

-- =====================================================
-- HR PERFORMANCE MANAGEMENT ANALYTICS VIEWS
-- Phase 5.4: Performance Management & Analytics
-- =====================================================

-- View: Employee Goals Summary
CREATE OR REPLACE VIEW hr.v_employee_goals_summary AS
SELECT 
    g.employee_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
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
GROUP BY g.employee_id, e.employee_number, e.first_name, e.last_name, 
         d.name, pc.cycle_name;

-- View: Performance Review Summary
CREATE OR REPLACE VIEW hr.v_performance_review_summary AS
SELECT 
    pr.review_id,
    pr.employee_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
    d.name AS department_name,
    p.title AS position_title,
    reviewer.first_name || ' ' || reviewer.last_name AS reviewer_name,
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
GROUP BY pr.review_id, pr.employee_id, e.employee_number, e.first_name, e.last_name,
         d.name, p.title, reviewer.first_name, reviewer.last_name, pc.cycle_name,
         pr.review_type, pr.review_date, pr.review_period_start, pr.review_period_end,
         pr.overall_rating, pr.status, pr.self_review_completed, pr.manager_review_completed,
         pr.hr_review_completed, pr.recommended_action, pr.submitted_at, pr.approved_at;

-- View: 360 Feedback Summary
CREATE OR REPLACE VIEW hr.v_360_feedback_summary AS
SELECT 
    f.employee_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
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
GROUP BY f.employee_id, e.employee_number, e.first_name, e.last_name, d.name, pc.cycle_name;

-- View: Development Plan Summary
CREATE OR REPLACE VIEW hr.v_development_plan_summary AS
SELECT 
    dp.plan_id,
    dp.employee_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
    d.name AS department_name,
    dp.plan_name,
    dp.plan_type,
    dp.start_date,
    dp.target_completion_date,
    dp.actual_completion_date,
    dp.status,
    manager.first_name || ' ' || manager.last_name AS manager_name,
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

-- View: Training & Certification Summary
CREATE OR REPLACE VIEW hr.v_training_certification_summary AS
SELECT 
    tc.employee_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
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
GROUP BY tc.employee_id, e.employee_number, e.first_name, e.last_name, d.name;

-- View: One-on-One Meeting Summary
CREATE OR REPLACE VIEW hr.v_one_on_one_summary AS
SELECT 
    oom.employee_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
    manager.first_name || ' ' || manager.last_name AS manager_name,
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
GROUP BY oom.employee_id, e.employee_number, e.first_name, e.last_name,
         manager.first_name, manager.last_name, d.name;

-- View: Performance Dashboard Stats
CREATE OR REPLACE VIEW hr.v_performance_dashboard_stats AS
SELECT 
    e.organization_id,
    COUNT(DISTINCT e.employee_id) AS total_employees,
    COUNT(DISTINCT g.goal_id) AS total_goals,
    COUNT(DISTINCT CASE WHEN g.status = 'completed' THEN g.goal_id END) AS completed_goals,
    ROUND(AVG(g.progress_percentage), 2) AS average_goal_progress,
    COUNT(DISTINCT pr.review_id) AS total_reviews,
    COUNT(DISTINCT CASE WHEN pr.status = 'completed' THEN pr.review_id END) AS completed_reviews,
    ROUND(AVG(pr.overall_rating), 2) AS average_review_rating,
    COUNT(DISTINCT f.feedback_id) AS total_360_feedback,
    COUNT(DISTINCT dp.plan_id) AS total_development_plans,
    COUNT(DISTINCT CASE WHEN dp.status = 'active' THEN dp.plan_id END) AS active_development_plans,
    COUNT(DISTINCT tc.training_id) AS total_trainings,
    COUNT(DISTINCT CASE WHEN tc.status = 'completed' THEN tc.training_id END) AS completed_trainings,
    SUM(tc.hours_spent) AS total_training_hours,
    COUNT(DISTINCT oom.meeting_id) AS total_one_on_ones
FROM hr.employees e
LEFT JOIN hr.goals g ON e.employee_id = g.employee_id
LEFT JOIN hr.performance_reviews pr ON e.employee_id = pr.employee_id
LEFT JOIN hr.feedback_360 f ON e.employee_id = f.employee_id
LEFT JOIN hr.development_plans dp ON e.employee_id = dp.employee_id
LEFT JOIN hr.training_certifications tc ON e.employee_id = tc.employee_id
LEFT JOIN hr.one_on_one_meetings oom ON e.employee_id = oom.employee_id
WHERE e.employment_status = 'active'
GROUP BY e.organization_id;

-- View: Goal Progress Tracking
CREATE OR REPLACE VIEW hr.v_goal_progress_tracking AS
SELECT 
    g.goal_id,
    g.employee_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name AS employee_name,
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
GROUP BY g.goal_id, g.employee_id, e.employee_number, e.first_name, e.last_name,
         g.goal_title, g.goal_category, g.priority, g.weight, g.start_date,
         g.target_date, g.completion_date, g.status, g.target_value,
         g.current_value, g.unit_of_measure, g.progress_percentage;

-- View: Competency Ratings Analysis
CREATE OR REPLACE VIEW hr.v_competency_ratings_analysis AS
SELECT 
    c.competency_id,
    c.competency_name,
    c.competency_category,
    COUNT(DISTINCT rr.review_id) AS review_count,
    ROUND(AVG(rr.rating_value), 2) AS average_rating,
    MIN(rr.rating_value) AS min_rating,
    MAX(rr.rating_value) AS max_rating,
    COUNT(DISTINCT pr.employee_id) AS employee_count
FROM hr.competencies c
LEFT JOIN hr.review_ratings rr ON c.competency_id = rr.competency_id
LEFT JOIN hr.performance_reviews pr ON rr.review_id = pr.review_id
WHERE c.is_active = true
GROUP BY c.competency_id, c.competency_name, c.competency_category;

-- View: Performance Cycle Progress
CREATE OR REPLACE VIEW hr.v_performance_cycle_progress AS
SELECT 
    pc.cycle_id,
    pc.organization_id,
    pc.cycle_name,
    pc.cycle_type,
    pc.start_date,
    pc.end_date,
    pc.status,
    COUNT(DISTINCT g.employee_id) AS employees_with_goals,
    COUNT(DISTINCT g.goal_id) AS total_goals,
    COUNT(DISTINCT pr.review_id) AS total_reviews,
    COUNT(DISTINCT CASE WHEN pr.status = 'completed' THEN pr.review_id END) AS completed_reviews,
    COUNT(DISTINCT f.feedback_id) AS total_360_feedback,
    ROUND(AVG(pr.overall_rating), 2) AS average_cycle_rating,
    CASE 
        WHEN CURRENT_DATE < pc.start_date THEN 'not_started'
        WHEN CURRENT_DATE > pc.end_date THEN 'completed'
        ELSE 'in_progress'
    END AS cycle_timeline_status
FROM hr.performance_cycles pc
LEFT JOIN hr.goals g ON pc.cycle_id = g.cycle_id
LEFT JOIN hr.performance_reviews pr ON pc.cycle_id = pr.cycle_id
LEFT JOIN hr.feedback_360 f ON pc.cycle_id = f.cycle_id
GROUP BY pc.cycle_id, pc.organization_id, pc.cycle_name, pc.cycle_type,
         pc.start_date, pc.end_date, pc.status;

-- Success message
SELECT 'HR Performance Management views created successfully' AS status;

