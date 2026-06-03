--liquibase formatted sql

--changeset hospital-service:143-backfill-doctor-working-hours
--comment: Backfill scheduling_working_hours for all web-bookable doctors that have appointment_slots defined.
--         Runs against hospital_scheduling schema (same PostgreSQL instance).
--         Safe to re-run — inserts only when no working hours exist for the resource.

INSERT INTO hospital_scheduling.scheduling_working_hours (
    id,
    resource_id,
    day_of_week,
    start_time,
    end_time,
    slots_per_interval,
    max_slots_per_segment,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    sr.id,
    CASE lower(day_name.value)
        WHEN 'sunday'    THEN 0
        WHEN 'monday'    THEN 1
        WHEN 'tuesday'   THEN 2
        WHEN 'wednesday' THEN 3
        WHEN 'thursday'  THEN 4
        WHEN 'friday'    THEN 5
        WHEN 'saturday'  THEN 6
    END,
    (slot.value->>'startTime')::time,
    (slot.value->>'endTime')::time,
    1,
    GREATEST(1, COALESCE((slot.value->>'maxPatients')::int, 10)),
    NOW(),
    NOW()
FROM hospital.doctors d
JOIN hospital_scheduling.scheduling_resources sr
    ON sr.external_reference_id = d.doctor_id::text
CROSS JOIN LATERAL jsonb_array_elements(d.appointment_slots::jsonb) AS slot(value)
CROSS JOIN LATERAL jsonb_array_elements_text(slot.value -> 'days') AS day_name(value)
WHERE d.appointments_from_web = true
  AND d.is_active = true
  AND d.appointment_slots IS NOT NULL
  AND d.appointment_slots NOT IN ('', '[]')
  AND (slot.value->>'startTime') IS NOT NULL
  AND (slot.value->>'endTime') IS NOT NULL
  AND (slot.value->>'startTime') != (slot.value->>'endTime')
  AND lower(day_name.value) IN ('sunday','monday','tuesday','wednesday','thursday','friday','saturday')
  AND NOT EXISTS (
      SELECT 1 FROM hospital_scheduling.scheduling_working_hours wh
      WHERE wh.resource_id = sr.id
  );

--rollback DELETE FROM hospital_scheduling.scheduling_working_hours wh WHERE EXISTS (SELECT 1 FROM hospital_scheduling.scheduling_resources sr JOIN hospital.doctors d ON sr.external_reference_id = d.doctor_id::text WHERE sr.id = wh.resource_id AND d.appointments_from_web = true);
