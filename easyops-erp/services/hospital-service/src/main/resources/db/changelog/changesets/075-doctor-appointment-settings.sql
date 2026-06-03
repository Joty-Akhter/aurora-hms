--liquibase formatted sql

--changeset easyops:075-doctor-appointment-settings
--comment: Add appointment settings columns to hospital.doctors: appointments_from_web, appointments_from_mobile, slots_per_day, weekly_schedule (JSON), off_days (JSON)

ALTER TABLE hospital.doctors
    ADD COLUMN IF NOT EXISTS appointments_from_web    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS appointments_from_mobile BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS slots_per_day            INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS weekly_schedule          TEXT,
    ADD COLUMN IF NOT EXISTS off_days                 TEXT;
