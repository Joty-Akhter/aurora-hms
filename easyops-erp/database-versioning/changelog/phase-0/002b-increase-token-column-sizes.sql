--liquibase formatted sql

--changeset easyops:017b-increase-token-column-sizes context:auth
--comment: Increase token and refresh_token column sizes to accommodate larger JWTs with extensive claims
ALTER TABLE auth.user_sessions ALTER COLUMN token TYPE VARCHAR(8192);
ALTER TABLE auth.user_sessions ALTER COLUMN refresh_token TYPE VARCHAR(8192);