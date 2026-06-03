--liquibase formatted sql

--changeset easyops:034-create-hr-performance-schema splitStatements:false

-- =====================================================
-- HR PERFORMANCE MANAGEMENT SCHEMA
-- Phase 5.4: Performance Management & Analytics
-- =====================================================

-- Performance Review Cycles Table
CREATE TABLE hr.performance_cycles (
    cycle_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    cycle_name VARCHAR(200) NOT NULL,
    cycle_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    review_start_date DATE,
    review_end_date DATE,
    status VARCHAR(50) DEFAULT 'planned',
    description TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, cycle_name)
);

CREATE INDEX idx_performance_cycle_organization ON hr.performance_cycles(organization_id);
CREATE INDEX idx_performance_cycle_status ON hr.performance_cycles(status);
CREATE INDEX idx_performance_cycle_dates ON hr.performance_cycles(start_date, end_date);

-- Goals Table
CREATE TABLE hr.goals (
    goal_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    cycle_id UUID REFERENCES hr.performance_cycles(cycle_id),
    goal_title VARCHAR(200) NOT NULL,
    goal_description TEXT,
    goal_category VARCHAR(100),
    target_value DECIMAL(10,2),
    current_value DECIMAL(10,2) DEFAULT 0,
    unit_of_measure VARCHAR(50),
    priority VARCHAR(50) DEFAULT 'medium',
    weight DECIMAL(5,2) DEFAULT 0,
    start_date DATE NOT NULL,
    target_date DATE NOT NULL,
    completion_date DATE,
    status VARCHAR(50) DEFAULT 'in_progress',
    progress_percentage INTEGER DEFAULT 0,
    manager_id UUID REFERENCES hr.employees(employee_id),
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_goal_employee ON hr.goals(employee_id);
CREATE INDEX idx_goal_organization ON hr.goals(organization_id);
CREATE INDEX idx_goal_cycle ON hr.goals(cycle_id);
CREATE INDEX idx_goal_status ON hr.goals(status);

-- Goal Updates Table
CREATE TABLE hr.goal_updates (
    update_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL REFERENCES hr.goals(goal_id) ON DELETE CASCADE,
    update_date DATE NOT NULL,
    current_value DECIMAL(10,2),
    progress_percentage INTEGER,
    update_notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_goal_update_goal ON hr.goal_updates(goal_id);
CREATE INDEX idx_goal_update_date ON hr.goal_updates(update_date);

-- Performance Reviews Table
CREATE TABLE hr.performance_reviews (
    review_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    cycle_id UUID REFERENCES hr.performance_cycles(cycle_id),
    reviewer_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    review_type VARCHAR(50) NOT NULL,
    review_date DATE NOT NULL,
    review_period_start DATE NOT NULL,
    review_period_end DATE NOT NULL,
    overall_rating DECIMAL(3,2),
    status VARCHAR(50) DEFAULT 'draft',
    self_review_completed BOOLEAN DEFAULT false,
    manager_review_completed BOOLEAN DEFAULT false,
    hr_review_completed BOOLEAN DEFAULT false,
    strengths TEXT,
    areas_for_improvement TEXT,
    achievements TEXT,
    comments TEXT,
    recommended_action VARCHAR(100),
    submitted_at TIMESTAMP,
    approved_by UUID REFERENCES hr.employees(employee_id),
    approved_at TIMESTAMP,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_performance_review_employee ON hr.performance_reviews(employee_id);
CREATE INDEX idx_performance_review_organization ON hr.performance_reviews(organization_id);
CREATE INDEX idx_performance_review_cycle ON hr.performance_reviews(cycle_id);
CREATE INDEX idx_performance_review_reviewer ON hr.performance_reviews(reviewer_id);
CREATE INDEX idx_performance_review_status ON hr.performance_reviews(status);

-- Review Competencies Table
CREATE TABLE hr.competencies (
    competency_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    competency_name VARCHAR(200) NOT NULL,
    competency_category VARCHAR(100),
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, competency_name)
);

CREATE INDEX idx_competency_organization ON hr.competencies(organization_id);
CREATE INDEX idx_competency_category ON hr.competencies(competency_category);

-- Review Ratings Table
CREATE TABLE hr.review_ratings (
    rating_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id UUID NOT NULL REFERENCES hr.performance_reviews(review_id) ON DELETE CASCADE,
    competency_id UUID REFERENCES hr.competencies(competency_id),
    rating_area VARCHAR(200) NOT NULL,
    rating_value DECIMAL(3,2) NOT NULL,
    weight DECIMAL(5,2) DEFAULT 0,
    comments TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_review_rating_review ON hr.review_ratings(review_id);
CREATE INDEX idx_review_rating_competency ON hr.review_ratings(competency_id);

-- 360 Feedback Table
CREATE TABLE hr.feedback_360 (
    feedback_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    cycle_id UUID REFERENCES hr.performance_cycles(cycle_id),
    reviewer_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    reviewer_relationship VARCHAR(50) NOT NULL,
    is_anonymous BOOLEAN DEFAULT false,
    overall_rating DECIMAL(3,2),
    strengths TEXT,
    areas_for_improvement TEXT,
    specific_feedback TEXT,
    would_recommend_collaboration BOOLEAN,
    status VARCHAR(50) DEFAULT 'draft',
    submitted_at TIMESTAMP,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_feedback_360_employee ON hr.feedback_360(employee_id);
CREATE INDEX idx_feedback_360_organization ON hr.feedback_360(organization_id);
CREATE INDEX idx_feedback_360_cycle ON hr.feedback_360(cycle_id);
CREATE INDEX idx_feedback_360_reviewer ON hr.feedback_360(reviewer_id);

-- Development Plans Table
CREATE TABLE hr.development_plans (
    plan_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    plan_name VARCHAR(200) NOT NULL,
    plan_type VARCHAR(50),
    start_date DATE NOT NULL,
    target_completion_date DATE,
    actual_completion_date DATE,
    status VARCHAR(50) DEFAULT 'active',
    objectives TEXT,
    action_items TEXT,
    required_resources TEXT,
    success_criteria TEXT,
    progress_notes TEXT,
    manager_id UUID REFERENCES hr.employees(employee_id),
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_development_plan_employee ON hr.development_plans(employee_id);
CREATE INDEX idx_development_plan_organization ON hr.development_plans(organization_id);
CREATE INDEX idx_development_plan_status ON hr.development_plans(status);

-- Training & Certifications Table
CREATE TABLE hr.training_certifications (
    training_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    training_name VARCHAR(200) NOT NULL,
    training_type VARCHAR(50),
    provider VARCHAR(200),
    start_date DATE,
    completion_date DATE,
    expiry_date DATE,
    certification_number VARCHAR(100),
    status VARCHAR(50) DEFAULT 'planned',
    cost DECIMAL(10,2),
    currency VARCHAR(3) DEFAULT 'USD',
    hours_spent DECIMAL(6,2),
    score DECIMAL(5,2),
    certificate_url VARCHAR(500),
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_training_employee ON hr.training_certifications(employee_id);
CREATE INDEX idx_training_organization ON hr.training_certifications(organization_id);
CREATE INDEX idx_training_status ON hr.training_certifications(status);

-- One-on-One Meetings Table
CREATE TABLE hr.one_on_one_meetings (
    meeting_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    manager_id UUID NOT NULL REFERENCES hr.employees(employee_id),
    organization_id UUID NOT NULL REFERENCES admin.organizations(id),
    meeting_date TIMESTAMP NOT NULL,
    duration_minutes INTEGER,
    meeting_type VARCHAR(50) DEFAULT 'regular',
    agenda TEXT,
    discussion_points TEXT,
    action_items TEXT,
    employee_notes TEXT,
    manager_notes TEXT,
    status VARCHAR(50) DEFAULT 'scheduled',
    next_meeting_date TIMESTAMP,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_one_on_one_employee ON hr.one_on_one_meetings(employee_id);
CREATE INDEX idx_one_on_one_manager ON hr.one_on_one_meetings(manager_id);
CREATE INDEX idx_one_on_one_organization ON hr.one_on_one_meetings(organization_id);
CREATE INDEX idx_one_on_one_date ON hr.one_on_one_meetings(meeting_date);

-- =====================================================
-- TRIGGERS FOR UPDATED_AT
-- =====================================================

CREATE OR REPLACE FUNCTION hr.update_performance_cycles_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_performance_cycles_updated_at
    BEFORE UPDATE ON hr.performance_cycles
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_performance_cycles_updated_at();

CREATE OR REPLACE FUNCTION hr.update_goals_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_goals_updated_at
    BEFORE UPDATE ON hr.goals
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_goals_updated_at();

CREATE OR REPLACE FUNCTION hr.update_performance_reviews_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_performance_reviews_updated_at
    BEFORE UPDATE ON hr.performance_reviews
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_performance_reviews_updated_at();

CREATE OR REPLACE FUNCTION hr.update_competencies_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_competencies_updated_at
    BEFORE UPDATE ON hr.competencies
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_competencies_updated_at();

CREATE OR REPLACE FUNCTION hr.update_feedback_360_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_feedback_360_updated_at
    BEFORE UPDATE ON hr.feedback_360
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_feedback_360_updated_at();

CREATE OR REPLACE FUNCTION hr.update_development_plans_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_development_plans_updated_at
    BEFORE UPDATE ON hr.development_plans
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_development_plans_updated_at();

CREATE OR REPLACE FUNCTION hr.update_training_certifications_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_training_certifications_updated_at
    BEFORE UPDATE ON hr.training_certifications
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_training_certifications_updated_at();

CREATE OR REPLACE FUNCTION hr.update_one_on_one_meetings_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_one_on_one_meetings_updated_at
    BEFORE UPDATE ON hr.one_on_one_meetings
    FOR EACH ROW
    EXECUTE FUNCTION hr.update_one_on_one_meetings_updated_at();

-- HR Performance Management schema complete

