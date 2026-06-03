--liquibase formatted sql

--changeset easyops:hosp-sched-001-create-schema
--comment: Create hospital_scheduling schema (Phase 0)
CREATE SCHEMA IF NOT EXISTS hospital_scheduling;
GRANT ALL PRIVILEGES ON SCHEMA hospital_scheduling TO easyops;

--changeset easyops:hosp-sched-002-scheduling-resources
--comment: Phase 1 – scheduling_resources
CREATE TABLE IF NOT EXISTS hospital_scheduling.scheduling_resources (
    id UUID PRIMARY KEY,
    resource_type VARCHAR(30) NOT NULL,
    external_reference_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    branch_id UUID,
    department_id UUID,
    metadata JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID
);

CREATE INDEX IF NOT EXISTS idx_hs_resources_type_ext_ref
    ON hospital_scheduling.scheduling_resources (resource_type, external_reference_id);
CREATE INDEX IF NOT EXISTS idx_hs_resources_branch
    ON hospital_scheduling.scheduling_resources (branch_id);
CREATE INDEX IF NOT EXISTS idx_hs_resources_status
    ON hospital_scheduling.scheduling_resources (status);

--changeset easyops:hosp-sched-003-scheduling-working-hours
--comment: Phase 1 – scheduling_working_hours
CREATE TABLE IF NOT EXISTS hospital_scheduling.scheduling_working_hours (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    day_of_week SMALLINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    effective_from DATE,
    effective_to DATE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hs_working_hours_resource
        FOREIGN KEY (resource_id) REFERENCES hospital_scheduling.scheduling_resources(id)
);

CREATE INDEX IF NOT EXISTS idx_hs_working_hours_resource_day
    ON hospital_scheduling.scheduling_working_hours (resource_id, day_of_week);

--changeset easyops:hosp-sched-004-scheduling-slot-templates
--comment: Phase 1 – scheduling_slot_templates
CREATE TABLE IF NOT EXISTS hospital_scheduling.scheduling_slot_templates (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    resource_type VARCHAR(30),
    branch_id UUID,
    slot_duration_minutes INT NOT NULL,
    slots_per_interval INT NOT NULL DEFAULT 1,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    lead_time_days INT DEFAULT 0,
    max_advance_days INT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID
);

CREATE INDEX IF NOT EXISTS idx_hs_slot_templates_resource_type_branch
    ON hospital_scheduling.scheduling_slot_templates (resource_type, branch_id);
CREATE INDEX IF NOT EXISTS idx_hs_slot_templates_status
    ON hospital_scheduling.scheduling_slot_templates (status);

--changeset easyops:hosp-sched-005-scheduling-blackouts
--comment: Phase 1 – scheduling_blackouts
CREATE TABLE IF NOT EXISTS hospital_scheduling.scheduling_blackouts (
    id UUID PRIMARY KEY,
    resource_id UUID,
    branch_id UUID,
    blackout_date DATE NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    CONSTRAINT fk_hs_blackouts_resource
        FOREIGN KEY (resource_id) REFERENCES hospital_scheduling.scheduling_resources(id)
);

CREATE INDEX IF NOT EXISTS idx_hs_blackouts_resource_date
    ON hospital_scheduling.scheduling_blackouts (resource_id, blackout_date);
CREATE INDEX IF NOT EXISTS idx_hs_blackouts_branch_date
    ON hospital_scheduling.scheduling_blackouts (branch_id, blackout_date);

--changeset easyops:hosp-sched-006-scheduling-reservations
--comment: Phase 1 – scheduling_reservations
CREATE TABLE IF NOT EXISTS hospital_scheduling.scheduling_reservations (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    slot_start TIMESTAMPTZ NOT NULL,
    slot_end TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL,
    reference_type VARCHAR(30),
    reference_id VARCHAR(255),
    patient_id UUID,
    idempotency_key VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    CONSTRAINT fk_hs_reservations_resource
        FOREIGN KEY (resource_id) REFERENCES hospital_scheduling.scheduling_resources(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_hs_reservations_idempotency
    ON hospital_scheduling.scheduling_reservations (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_hs_reservations_resource_slot
    ON hospital_scheduling.scheduling_reservations (resource_id, slot_start, slot_end);
CREATE INDEX IF NOT EXISTS idx_hs_reservations_status
    ON hospital_scheduling.scheduling_reservations (status);
CREATE INDEX IF NOT EXISTS idx_hs_reservations_patient
    ON hospital_scheduling.scheduling_reservations (patient_id);

--changeset easyops:hosp-sched-007-scheduling-booking-rules
--comment: Phase 1 optional – scheduling_booking_rules
CREATE TABLE IF NOT EXISTS hospital_scheduling.scheduling_booking_rules (
    id UUID PRIMARY KEY,
    scope_type VARCHAR(30) NOT NULL,
    scope_id UUID,
    cancellation_cutoff_hours INT,
    max_per_slot INT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
