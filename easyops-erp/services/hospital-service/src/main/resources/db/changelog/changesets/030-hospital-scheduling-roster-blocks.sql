--liquibase formatted sql

--changeset easyops:hosp-sched-010-scheduling-roster-blocks
--comment: Phase 4 – scheduling_roster_blocks (explicit roster blocks: AVAILABLE, UNAVAILABLE, SUBSTITUTE)
CREATE TABLE IF NOT EXISTS hospital_scheduling.scheduling_roster_blocks (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    type VARCHAR(20) NOT NULL,
    substitute_resource_id UUID,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hs_roster_blocks_resource
        FOREIGN KEY (resource_id) REFERENCES hospital_scheduling.scheduling_resources(id),
    CONSTRAINT fk_hs_roster_blocks_substitute
        FOREIGN KEY (substitute_resource_id) REFERENCES hospital_scheduling.scheduling_resources(id),
    CONSTRAINT chk_hs_roster_blocks_type
        CHECK (type IN ('AVAILABLE', 'UNAVAILABLE', 'SUBSTITUTE')),
    CONSTRAINT chk_hs_roster_blocks_end_after_start
        CHECK (end_time > start_time)
);

CREATE INDEX IF NOT EXISTS idx_hs_roster_blocks_resource
    ON hospital_scheduling.scheduling_roster_blocks (resource_id);
CREATE INDEX IF NOT EXISTS idx_hs_roster_blocks_resource_time
    ON hospital_scheduling.scheduling_roster_blocks (resource_id, start_time, end_time);
