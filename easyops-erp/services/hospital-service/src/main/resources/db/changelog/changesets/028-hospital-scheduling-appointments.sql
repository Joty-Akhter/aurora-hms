--liquibase formatted sql

--changeset easyops:hosp-sched-008-scheduling-appointments
--comment: Phase 2 – scheduling_appointments (OPD appointments)
CREATE TABLE IF NOT EXISTS hospital_scheduling.scheduling_appointments (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    resource_id UUID NOT NULL,
    clinic_or_location_id UUID,
    appointment_date DATE NOT NULL,
    slot_start TIMESTAMPTZ NOT NULL,
    slot_end TIMESTAMPTZ NOT NULL,
    appointment_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    visit_id UUID,
    token_number INT,
    idempotency_key VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    CONSTRAINT fk_hs_appointments_reservation
        FOREIGN KEY (reservation_id) REFERENCES hospital_scheduling.scheduling_reservations(id),
    CONSTRAINT fk_hs_appointments_resource
        FOREIGN KEY (resource_id) REFERENCES hospital_scheduling.scheduling_resources(id)
);

CREATE INDEX IF NOT EXISTS idx_hs_appointments_patient_date
    ON hospital_scheduling.scheduling_appointments (patient_id, appointment_date);
CREATE INDEX IF NOT EXISTS idx_hs_appointments_resource_date
    ON hospital_scheduling.scheduling_appointments (resource_id, appointment_date);
CREATE UNIQUE INDEX IF NOT EXISTS idx_hs_appointments_idempotency
    ON hospital_scheduling.scheduling_appointments (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_hs_appointments_reservation
    ON hospital_scheduling.scheduling_appointments (reservation_id);
