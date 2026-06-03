--liquibase formatted sql

--changeset easyops:hosp-sched-011-scheduling-waitlist-entries
--comment: Phase 5 – scheduling_waitlist_entries (waitlist per doctor/clinic with priority)
CREATE TABLE IF NOT EXISTS hospital_scheduling.scheduling_waitlist_entries (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    resource_id UUID NOT NULL,
    preferred_from_date DATE,
    preferred_to_date DATE,
    priority INT NOT NULL DEFAULT 0,
    priority_reason VARCHAR(50),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    CONSTRAINT fk_hs_waitlist_resource
        FOREIGN KEY (resource_id) REFERENCES hospital_scheduling.scheduling_resources(id),
    CONSTRAINT chk_hs_waitlist_status
        CHECK (status IN ('PENDING', 'PROMOTED', 'CANCELLED', 'EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_hs_waitlist_resource_status
    ON hospital_scheduling.scheduling_waitlist_entries (resource_id, status);
CREATE INDEX IF NOT EXISTS idx_hs_waitlist_patient
    ON hospital_scheduling.scheduling_waitlist_entries (patient_id);
