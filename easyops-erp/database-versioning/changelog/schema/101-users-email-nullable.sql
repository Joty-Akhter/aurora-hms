--liquibase formatted sql

--changeset easyops:101-users-email-nullable context:schema
--comment: Allow users without an email (optional at creation; multiple NULLs allowed under UNIQUE)
ALTER TABLE users.users ALTER COLUMN email DROP NOT NULL;
