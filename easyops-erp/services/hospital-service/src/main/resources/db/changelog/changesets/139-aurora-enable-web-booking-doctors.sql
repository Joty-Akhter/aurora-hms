--liquibase formatted sql

--changeset hospital-service:139-aurora-enable-web-booking-doctors
--comment: Enable public web appointment booking for active doctors that already have appointment slot schedules

UPDATE hospital.doctors
SET appointments_from_web = true,
    number_of_days_can_appointment = CASE
        WHEN COALESCE(number_of_days_can_appointment, 0) <= 0 THEN 30
        ELSE number_of_days_can_appointment
    END,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = 'system'
WHERE is_active = true
  AND availability_status = 'AVAILABLE'
  AND appointment_slots IS NOT NULL
  AND TRIM(appointment_slots) <> ''
  AND appointment_slots <> '[]';

--rollback UPDATE hospital.doctors SET appointments_from_web = false, updated_at = CURRENT_TIMESTAMP, updated_by = 'system' WHERE updated_by = 'system' AND appointments_from_web = true;
