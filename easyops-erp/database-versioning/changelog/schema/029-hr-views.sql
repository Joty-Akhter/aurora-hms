--liquibase formatted sql

--changeset easyops:029-create-hr-views
--comment: Create HR module views for reporting and analytics

SET search_path TO hr, admin, public;

-- =============================================
-- 1. EMPLOYEE SUMMARY VIEW
-- =============================================
CREATE OR REPLACE VIEW hr.v_employee_summary AS
SELECT 
    e.employee_id,
    e.organization_id,
    e.employee_number,
    e.first_name,
    e.last_name,
    e.first_name || ' ' || e.last_name AS full_name,
    e.email,
    e.phone,
    e.hire_date,
    e.termination_date,
    e.employment_type,
    e.employment_status,
    
    -- Department info
    d.id AS department_id,
    d.name AS department_name,
    
    -- Position info
    p.position_id,
    p.title AS position_title,
    p.level AS position_level,
    
    -- Manager info
    m.employee_id AS manager_id,
    m.first_name || ' ' || m.last_name AS manager_name,
    
    -- Tenure calculation
    CASE 
        WHEN e.termination_date IS NOT NULL THEN 
            DATE_PART('year', AGE(e.termination_date, e.hire_date))
        ELSE 
            DATE_PART('year', AGE(CURRENT_DATE, e.hire_date))
    END AS years_of_service,
    
    e.is_active,
    e.created_at,
    e.updated_at
FROM hr.employees e
LEFT JOIN admin.departments d ON e.department_id = d.id
LEFT JOIN hr.positions p ON e.position_id = p.position_id
LEFT JOIN hr.employees m ON e.manager_id = m.employee_id;

COMMENT ON VIEW hr.v_employee_summary IS 'Comprehensive employee information with department, position, and manager details';

-- =============================================
-- 2. DEPARTMENT HIERARCHY VIEW
-- =============================================
CREATE OR REPLACE VIEW hr.v_department_hierarchy AS
WITH RECURSIVE dept_tree AS (
    -- Root departments
    SELECT 
        d.id AS department_id,
        d.organization_id,
        d.name,
        d.parent_department_id,
        d.manager_id,
        0 AS level,
        d.name::TEXT AS path
    FROM admin.departments d
    WHERE d.parent_department_id IS NULL AND d.status = 'ACTIVE'
    
    UNION ALL
    
    -- Child departments
    SELECT 
        d.id,
        d.organization_id,
        d.name,
        d.parent_department_id,
        d.manager_id,
        dt.level + 1,
        dt.path || ' > ' || d.name AS path
    FROM admin.departments d
    INNER JOIN dept_tree dt ON d.parent_department_id = dt.department_id
    WHERE d.status = 'ACTIVE'
)
SELECT 
    dt.*,
    e.first_name || ' ' || e.last_name AS manager_name,
    (SELECT COUNT(*) FROM hr.employees WHERE department_id = dt.department_id AND is_active = TRUE) AS employee_count
FROM dept_tree dt
LEFT JOIN hr.employees e ON dt.manager_id = e.employee_id;

COMMENT ON VIEW hr.v_department_hierarchy IS 'Hierarchical view of department structure with employee counts';

-- =============================================
-- 3. HR DASHBOARD STATISTICS VIEW
-- =============================================
CREATE OR REPLACE VIEW hr.v_hr_dashboard_stats AS
SELECT 
    e.organization_id,
    
    -- Total counts
    COUNT(*) AS total_employees,
    COUNT(*) FILTER (WHERE e.employment_status = 'ACTIVE') AS active_employees,
    COUNT(*) FILTER (WHERE e.employment_status = 'ON_LEAVE') AS on_leave_employees,
    COUNT(*) FILTER (WHERE e.employment_status = 'TERMINATED') AS terminated_employees,
    
    -- Employment type breakdown
    COUNT(*) FILTER (WHERE e.employment_type = 'FULL_TIME') AS full_time_employees,
    COUNT(*) FILTER (WHERE e.employment_type = 'PART_TIME') AS part_time_employees,
    COUNT(*) FILTER (WHERE e.employment_type = 'CONTRACT') AS contract_employees,
    COUNT(*) FILTER (WHERE e.employment_type = 'INTERN') AS intern_employees,
    
    -- Recent hires (last 30 days)
    COUNT(*) FILTER (WHERE e.hire_date >= CURRENT_DATE - INTERVAL '30 days') AS recent_hires_30d,
    
    -- Recent terminations (last 30 days)
    COUNT(*) FILTER (WHERE e.termination_date >= CURRENT_DATE - INTERVAL '30 days') AS recent_terminations_30d,
    
    -- Average tenure
    AVG(DATE_PART('year', AGE(COALESCE(e.termination_date, CURRENT_DATE), e.hire_date))) AS avg_tenure_years,
    
    -- Department count
    COUNT(DISTINCT e.department_id) AS department_count,
    
    -- Position count
    COUNT(DISTINCT e.position_id) AS position_count
    
FROM hr.employees e
WHERE e.is_active = TRUE
GROUP BY e.organization_id;

COMMENT ON VIEW hr.v_hr_dashboard_stats IS 'Key HR metrics and statistics for dashboard';

-- =============================================
-- 4. ONBOARDING PROGRESS VIEW
-- =============================================
CREATE OR REPLACE VIEW hr.v_onboarding_progress AS
SELECT 
    oc.employee_id,
    oc.organization_id,
    e.first_name || ' ' || e.last_name AS employee_name,
    e.hire_date,
    
    -- Task statistics
    COUNT(*) AS total_tasks,
    COUNT(*) FILTER (WHERE oc.status = 'COMPLETED') AS completed_tasks,
    COUNT(*) FILTER (WHERE oc.status = 'PENDING') AS pending_tasks,
    COUNT(*) FILTER (WHERE oc.status = 'IN_PROGRESS') AS in_progress_tasks,
    COUNT(*) FILTER (WHERE oc.due_date < CURRENT_DATE AND oc.status != 'COMPLETED') AS overdue_tasks,
    
    -- Progress percentage
    CASE 
        WHEN COUNT(*) > 0 THEN 
            ROUND((COUNT(*) FILTER (WHERE oc.status = 'COMPLETED')::DECIMAL / COUNT(*)) * 100, 2)
        ELSE 0 
    END AS completion_percentage,
    
    -- Days since hire
    DATE_PART('day', AGE(CURRENT_DATE, e.hire_date))::INTEGER AS days_since_hire
    
FROM hr.onboarding_checklists oc
INNER JOIN hr.employees e ON oc.employee_id = e.employee_id
GROUP BY 
    oc.employee_id, 
    oc.organization_id, 
    e.first_name, 
    e.last_name, 
    e.hire_date;

COMMENT ON VIEW hr.v_onboarding_progress IS 'Onboarding progress tracking for new employees';

-- =============================================
-- 5. EMPLOYEE DOCUMENTS SUMMARY VIEW
-- =============================================
CREATE OR REPLACE VIEW hr.v_employee_documents_summary AS
SELECT 
    ed.employee_id,
    ed.organization_id,
    e.first_name || ' ' || e.last_name AS employee_name,
    e.employee_number,
    
    -- Document counts by type
    COUNT(*) AS total_documents,
    COUNT(*) FILTER (WHERE ed.status = 'ACTIVE') AS active_documents,
    COUNT(*) FILTER (WHERE ed.status = 'EXPIRED') AS expired_documents,
    COUNT(*) FILTER (WHERE ed.expiry_date IS NOT NULL AND ed.expiry_date < CURRENT_DATE) AS expiring_soon,
    
    -- Document types
    COUNT(*) FILTER (WHERE ed.document_type = 'IDENTIFICATION') AS id_documents,
    COUNT(*) FILTER (WHERE ed.document_type = 'CONTRACT') AS contract_documents,
    COUNT(*) FILTER (WHERE ed.document_type = 'CERTIFICATE') AS certificate_documents,
    COUNT(*) FILTER (WHERE ed.document_type = 'MEDICAL') AS medical_documents,
    
    -- Latest upload date
    MAX(ed.upload_date) AS latest_upload_date
    
FROM hr.employee_documents ed
INNER JOIN hr.employees e ON ed.employee_id = e.employee_id
GROUP BY 
    ed.employee_id, 
    ed.organization_id, 
    e.first_name, 
    e.last_name,
    e.employee_number;

COMMENT ON VIEW hr.v_employee_documents_summary IS 'Summary of employee documents and expiry tracking';

-- =============================================
-- 6. HEADCOUNT BY DEPARTMENT VIEW
-- =============================================
CREATE OR REPLACE VIEW hr.v_headcount_by_department AS
SELECT 
    d.organization_id,
    d.id AS department_id,
    d.name AS department_name,
    d.manager_id,
    m.first_name || ' ' || m.last_name AS manager_name,
    
    -- Headcount statistics
    COUNT(e.employee_id) AS total_employees,
    COUNT(e.employee_id) FILTER (WHERE e.employment_status = 'ACTIVE') AS active_employees,
    COUNT(e.employee_id) FILTER (WHERE e.employment_type = 'FULL_TIME') AS full_time_count,
    COUNT(e.employee_id) FILTER (WHERE e.employment_type = 'PART_TIME') AS part_time_count,
    COUNT(e.employee_id) FILTER (WHERE e.employment_type = 'CONTRACT') AS contract_count,
    
    -- Recent changes
    COUNT(e.employee_id) FILTER (WHERE e.hire_date >= CURRENT_DATE - INTERVAL '30 days') AS new_hires_30d,
    COUNT(e.employee_id) FILTER (WHERE e.termination_date >= CURRENT_DATE - INTERVAL '30 days') AS terminations_30d
    
FROM admin.departments d
LEFT JOIN hr.employees e ON d.id = e.department_id AND e.is_active = TRUE
LEFT JOIN hr.employees m ON d.manager_id = m.employee_id
WHERE d.status = 'ACTIVE'
GROUP BY 
    d.organization_id,
    d.id,
    d.name,
    d.manager_id,
    m.first_name,
    m.last_name;

COMMENT ON VIEW hr.v_headcount_by_department IS 'Employee headcount breakdown by department';

--rollback DROP VIEW IF EXISTS hr.v_employee_summary CASCADE;
--rollback DROP VIEW IF EXISTS hr.v_department_hierarchy CASCADE;
--rollback DROP VIEW IF EXISTS hr.v_hr_dashboard_stats CASCADE;
--rollback DROP VIEW IF EXISTS hr.v_onboarding_progress CASCADE;
--rollback DROP VIEW IF EXISTS hr.v_employee_documents_summary CASCADE;
--rollback DROP VIEW IF EXISTS hr.v_headcount_by_department CASCADE;

