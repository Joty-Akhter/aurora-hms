--liquibase formatted sql

--changeset easyops:017a-extend-token-columns context:auth
--comment: Extend token columns to support long JWTs (roles/permissions in claims)
ALTER TABLE auth.user_sessions ALTER COLUMN token TYPE VARCHAR(4096);
ALTER TABLE auth.user_sessions ALTER COLUMN refresh_token TYPE VARCHAR(4096);
ALTER TABLE auth.user_sessions ALTER COLUMN user_agent TYPE VARCHAR(1024);
