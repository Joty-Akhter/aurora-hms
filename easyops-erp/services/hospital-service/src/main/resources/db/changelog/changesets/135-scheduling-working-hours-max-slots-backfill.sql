--liquibase formatted sql
--changeset easyops:scheduling-working-hours-max-slots-backfill-135
--comment: Backfill max_slots_per_segment and corrected slot_duration from hospital.doctors appointment_slots

UPDATE hospital_scheduling.scheduling_working_hours wh
SET
    max_slots_per_segment = matched.max_patients,
    slot_duration_minutes = matched.computed_duration
FROM (
    SELECT
        wh_inner.id AS wh_id,
        (slot->>'maxPatients')::integer AS max_patients,
        GREATEST(
            1,
            (
                (
                    CASE
                        WHEN (slot->>'endTime')::time <= (slot->>'startTime')::time THEN
                            EXTRACT(EPOCH FROM ((slot->>'endTime')::time - (slot->>'startTime')::time)) / 60 + 1440
                        ELSE
                            EXTRACT(EPOCH FROM ((slot->>'endTime')::time - (slot->>'startTime')::time)) / 60
                    END
                )::bigint + (slot->>'maxPatients')::integer - 1
            ) / (slot->>'maxPatients')::integer
        )::integer AS computed_duration
    FROM hospital.doctors d
    JOIN hospital_scheduling.scheduling_resources sr
        ON sr.resource_type = 'DOCTOR'
        AND sr.external_reference_id = d.doctor_id::text
    CROSS JOIN LATERAL jsonb_array_elements(
        COALESCE(NULLIF(trim(d.appointment_slots), '')::jsonb, '[]'::jsonb)
    ) AS slot
    CROSS JOIN LATERAL jsonb_array_elements_text(slot->'days') AS day_name
    JOIN hospital_scheduling.scheduling_working_hours wh_inner
        ON wh_inner.resource_id = sr.id
        AND wh_inner.start_time = (slot->>'startTime')::time
        AND wh_inner.end_time = (slot->>'endTime')::time
        AND wh_inner.day_of_week = CASE lower(trim(both '"' from day_name::text))
            WHEN 'sunday' THEN 0
            WHEN 'monday' THEN 1
            WHEN 'tuesday' THEN 2
            WHEN 'wednesday' THEN 3
            WHEN 'thursday' THEN 4
            WHEN 'friday' THEN 5
            WHEN 'saturday' THEN 6
            ELSE NULL
        END
    WHERE (slot->>'maxPatients') ~ '^[0-9]+$'
      AND (slot->>'maxPatients')::integer > 0
      AND wh_inner.max_slots_per_segment IS NULL
) matched
WHERE wh.id = matched.wh_id;
