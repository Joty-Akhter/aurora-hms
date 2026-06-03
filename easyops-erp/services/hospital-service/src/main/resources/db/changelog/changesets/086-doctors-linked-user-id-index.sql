--liquibase formatted sql

--changeset easyops:086-doctors-linked-user-id-index
--comment: Index hospital.doctors.linked_user_id for portal user → doctor resolution (IPD filter, encounter create/update).

CREATE INDEX IF NOT EXISTS idx_doctors_linked_user_id
    ON hospital.doctors (linked_user_id)
    WHERE linked_user_id IS NOT NULL;
