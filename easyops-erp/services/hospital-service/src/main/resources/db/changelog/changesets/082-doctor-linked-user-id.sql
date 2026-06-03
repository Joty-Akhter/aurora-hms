-- Link hospital.doctors to users.users for portal login created from doctor registration
ALTER TABLE hospital.doctors
    ADD COLUMN IF NOT EXISTS linked_user_id UUID NULL;

COMMENT ON COLUMN hospital.doctors.linked_user_id IS 'users.users.id when a portal user was created for this doctor';
