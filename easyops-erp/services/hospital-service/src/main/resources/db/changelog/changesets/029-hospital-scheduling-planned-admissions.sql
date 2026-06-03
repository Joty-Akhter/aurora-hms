--liquibase formatted sql

--changeset easyops:hosp-sched-009-scheduling-planned-admissions
--comment: Phase 3 – scheduling_planned_admissions (IPD planned admissions)
CREATE TABLE IF NOT EXISTS hospital_scheduling.scheduling_planned_admissions (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    preferred_date DATE NOT NULL,
    preferred_ward_or_bed_class VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    bed_group_resource_id UUID,
    reservation_id UUID,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    CONSTRAINT fk_hs_planned_admissions_bed_group
        FOREIGN KEY (bed_group_resource_id) REFERENCES hospital_scheduling.scheduling_resources(id),
    CONSTRAINT fk_hs_planned_admissions_reservation
        FOREIGN KEY (reservation_id) REFERENCES hospital_scheduling.scheduling_reservations(id)
);

CREATE INDEX IF NOT EXISTS idx_hs_planned_admissions_patient
    ON hospital_scheduling.scheduling_planned_admissions (patient_id);
CREATE INDEX IF NOT EXISTS idx_hs_planned_admissions_preferred_date
    ON hospital_scheduling.scheduling_planned_admissions (preferred_date);
CREATE INDEX IF NOT EXISTS idx_hs_planned_admissions_status
    ON hospital_scheduling.scheduling_planned_admissions (status);
