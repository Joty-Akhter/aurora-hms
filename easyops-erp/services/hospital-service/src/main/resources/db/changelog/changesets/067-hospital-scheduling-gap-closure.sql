--liquibase formatted sql

--changeset easyops:hosp-sched-013-doctor-resource-mappings
--comment: Phase 7 – scheduling_doctor_resource_mappings
CREATE TABLE IF NOT EXISTS hospital_scheduling.scheduling_doctor_resource_mappings (
    id UUID PRIMARY KEY,
    doctor_user_id UUID NOT NULL,
    resource_id UUID NOT NULL,
    branch_id UUID,
    is_primary BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from DATE,
    effective_to DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    CONSTRAINT fk_hs_drm_resource FOREIGN KEY (resource_id) REFERENCES hospital_scheduling.scheduling_resources(id),
    CONSTRAINT uq_hs_drm_doctor_resource_branch UNIQUE (doctor_user_id, resource_id, branch_id)
);
CREATE INDEX IF NOT EXISTS idx_hs_drm_doctor_status ON hospital_scheduling.scheduling_doctor_resource_mappings(doctor_user_id, status);
CREATE INDEX IF NOT EXISTS idx_hs_drm_resource ON hospital_scheduling.scheduling_doctor_resource_mappings(resource_id);

--changeset easyops:hosp-sched-014-audit-log
--comment: Phase 7 – scheduling_audit_log (immutable insert-only)
CREATE TABLE IF NOT EXISTS hospital_scheduling.scheduling_audit_log (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(30) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    actor_id UUID,
    actor_role VARCHAR(100),
    booking_channel VARCHAR(30),
    reason TEXT,
    correlation_id VARCHAR(255),
    before_state JSONB,
    after_state JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_hs_audit_entity ON hospital_scheduling.scheduling_audit_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_hs_audit_actor ON hospital_scheduling.scheduling_audit_log(actor_id);
CREATE INDEX IF NOT EXISTS idx_hs_audit_created ON hospital_scheduling.scheduling_audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_hs_audit_correlation ON hospital_scheduling.scheduling_audit_log(correlation_id);

--changeset easyops:hosp-sched-015-appointments-gap-closure-columns
--comment: Phase 7 – add booking_channel, booked_by, slot_template_id, session_shift, session_label, version to scheduling_appointments
ALTER TABLE hospital_scheduling.scheduling_appointments
    ADD COLUMN IF NOT EXISTS booking_channel VARCHAR(30) NOT NULL DEFAULT 'INTERNAL',
    ADD COLUMN IF NOT EXISTS booked_by UUID,
    ADD COLUMN IF NOT EXISTS slot_template_id UUID,
    ADD COLUMN IF NOT EXISTS session_shift VARCHAR(20),
    ADD COLUMN IF NOT EXISTS session_label VARCHAR(255),
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_hs_appt_booking_channel ON hospital_scheduling.scheduling_appointments(booking_channel, appointment_date);
CREATE INDEX IF NOT EXISTS idx_hs_appt_slot_template ON hospital_scheduling.scheduling_appointments(slot_template_id);

--changeset easyops:hosp-sched-016-reservations-version
--comment: Phase 7 – add optimistic locking version to scheduling_reservations
ALTER TABLE hospital_scheduling.scheduling_reservations
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

--changeset easyops:hosp-sched-017-booking-rules-channel-quotas
--comment: Phase 7 – add channel quota columns to scheduling_booking_rules
ALTER TABLE hospital_scheduling.scheduling_booking_rules
    ADD COLUMN IF NOT EXISTS channel VARCHAR(30),
    ADD COLUMN IF NOT EXISTS channel_daily_cap INT,
    ADD COLUMN IF NOT EXISTS max_advance_days INT;
