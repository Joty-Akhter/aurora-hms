--liquibase formatted sql

--changeset easyops:111-hr-device-attendance splitStatements:false

-- HR-AT-03: Raw punch log from biometric / access-control devices.
-- Punches are ingested here first, then processed into hr.attendance_records.
CREATE TABLE IF NOT EXISTS hr.attendance_raw_logs (
    raw_log_id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id      UUID        NOT NULL REFERENCES admin.organizations(id),
    employee_id          UUID        REFERENCES hr.employees(employee_id) ON DELETE SET NULL,
    device_id            VARCHAR(100),
    punch_time           TIMESTAMP   NOT NULL,
    punch_type           VARCHAR(10) DEFAULT 'UNKNOWN', -- IN | OUT | UNKNOWN
    source               VARCHAR(50) DEFAULT 'DEVICE',  -- DEVICE | API | IMPORT
    raw_employee_code    VARCHAR(100),                  -- device's own employee identifier
    processed            BOOLEAN     NOT NULL DEFAULT FALSE,
    processed_at         TIMESTAMP,
    attendance_record_id UUID        REFERENCES hr.attendance_records(attendance_id) ON DELETE SET NULL,
    notes                TEXT,
    created_at           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_raw_logs_org_employee     ON hr.attendance_raw_logs(organization_id, employee_id);
CREATE INDEX IF NOT EXISTS idx_raw_logs_punch_time       ON hr.attendance_raw_logs(organization_id, punch_time);
CREATE INDEX IF NOT EXISTS idx_raw_logs_processed        ON hr.attendance_raw_logs(processed) WHERE NOT processed;
CREATE INDEX IF NOT EXISTS idx_raw_logs_employee_day     ON hr.attendance_raw_logs(employee_id, (punch_time::date));

COMMENT ON TABLE  hr.attendance_raw_logs                        IS 'HR-AT-03: Raw device/biometric punch log. Processed into attendance_records.';
COMMENT ON COLUMN hr.attendance_raw_logs.punch_type             IS 'IN = entry punch, OUT = exit punch, UNKNOWN = device did not specify direction.';
COMMENT ON COLUMN hr.attendance_raw_logs.raw_employee_code      IS 'Device-side employee code; used when device does not send UUID.';
COMMENT ON COLUMN hr.attendance_raw_logs.processed              IS 'TRUE after this punch has been folded into an attendance_record row.';
COMMENT ON COLUMN hr.attendance_raw_logs.attendance_record_id   IS 'FK to the resulting attendance_record after processing.';
