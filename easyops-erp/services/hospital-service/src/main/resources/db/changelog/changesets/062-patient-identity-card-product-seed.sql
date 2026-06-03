--liquibase formatted sql

--changeset easyops:hosp-card-patient-identity-product context:hospital-card
--comment: Seed PATIENT_IDENTITY card product for patient registration (Phase 1 patient identity card). Fixed UUID for configuration.
INSERT INTO hospital_card.card_products (
    id,
    code,
    name,
    description,
    medium_type,
    usage_domains,
    default_limit_profile_id,
    validity_start_date,
    validity_end_date,
    status,
    created_at,
    updated_at
)
VALUES (
    'a0000001-0001-4000-8000-000000000001',
    'PATIENT_IDENTITY',
    'Patient identity card',
    'MRN-linked identification card at registration; no wallet / stored value',
    'PHYSICAL',
    'HOSPITAL',
    NULL,
    NULL,
    NULL,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (code) DO NOTHING;
