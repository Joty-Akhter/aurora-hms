--liquibase formatted sql

--changeset easyops:058-hr-employee-name-migration
--comment: Migrate employee firstName/lastName to single name field and make email optional

SET search_path TO hr, admin, public;

-- Step 1: Add name column
ALTER TABLE hr.employees ADD COLUMN IF NOT EXISTS name VARCHAR(200);

-- Step 2: Migrate existing data from firstName + lastName to name
UPDATE hr.employees 
SET name = TRIM(CONCAT(COALESCE(first_name, ''), ' ', COALESCE(last_name, '')))
WHERE name IS NULL OR name = '';

-- Step 3: Make name NOT NULL after migration
ALTER TABLE hr.employees ALTER COLUMN name SET NOT NULL;

-- Step 4: Make email nullable
ALTER TABLE hr.employees ALTER COLUMN email DROP NOT NULL;

-- Step 5: Update unique constraint on email to allow NULL (PostgreSQL allows multiple NULLs by default)
-- The existing constraint should work fine with nullable email

-- Step 6: Drop and recreate views to use name instead of first_name/last_name
-- Drop views first to avoid column reference issues
DROP VIEW IF EXISTS hr.v_employee_summary CASCADE;
DROP VIEW IF EXISTS hr.v_department_hierarchy CASCADE;
DROP VIEW IF EXISTS hr.v_onboarding_progress CASCADE;
DROP VIEW IF EXISTS hr.v_employee_documents_summary CASCADE;
DROP VIEW IF EXISTS hr.v_headcount_by_department CASCADE;

-- Recreate v_employee_summary view
CREATE VIEW hr.v_employee_summary AS
SELECT 
    e.employee_id,
    e.organization_id,
    e.employee_number,
    e.name,
    e.name AS full_name,
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
    m.name AS manager_name,
    
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

-- Recreate v_department_hierarchy view
CREATE VIEW hr.v_department_hierarchy AS
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
    e.name AS manager_name,
    (SELECT COUNT(*) FROM hr.employees WHERE department_id = dt.department_id AND is_active = TRUE) AS employee_count
FROM dept_tree dt
LEFT JOIN hr.employees e ON dt.manager_id = e.employee_id;

-- Recreate v_onboarding_progress view
CREATE VIEW hr.v_onboarding_progress AS
SELECT 
    oc.employee_id,
    oc.organization_id,
    e.name AS employee_name,
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
    e.name, 
    e.hire_date;

-- Recreate v_employee_documents_summary view
CREATE VIEW hr.v_employee_documents_summary AS
SELECT 
    ed.employee_id,
    ed.organization_id,
    e.name AS employee_name,
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
    e.name,
    e.employee_number;

-- Recreate v_headcount_by_department view
CREATE VIEW hr.v_headcount_by_department AS
SELECT 
    d.organization_id,
    d.id AS department_id,
    d.name AS department_name,
    d.manager_id,
    m.name AS manager_name,
    
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
    m.name;

-- Step 7: Drop old columns (commented out for safety - uncomment after verifying migration)
-- ALTER TABLE hr.employees DROP COLUMN IF EXISTS first_name;
-- ALTER TABLE hr.employees DROP COLUMN IF EXISTS last_name;

--rollback ALTER TABLE hr.employees DROP COLUMN IF EXISTS name;
--rollback ALTER TABLE hr.employees ALTER COLUMN email SET NOT NULL;
--rollback -- Note: Rollback of views and column drops would need to be done manually
