--liquibase formatted sql

-- EP-2: ep_encounter_mode is set at prescription creation time from the encounter context
-- and is read-only after that point. This trigger enforces immutability at the DB layer
-- so the constraint holds regardless of how the row is updated (service, direct SQL, migrations).

--changeset hospital-service:048-ep-encounter-mode-immutable-trigger splitStatements:false
--comment: EP-2 — trigger that prevents ep_encounter_mode from being changed once set (splitStatements:false for $$ function body)
CREATE OR REPLACE FUNCTION ehr.fn_prescriptions_ep_encounter_mode_immutable()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    -- Allow: NULL → NULL (never set), NULL → value (initial set), value → same value (idempotent).
    -- Deny:  value → different value  OR  value → NULL (clearing an already-set mode).
    IF OLD.ep_encounter_mode IS NOT NULL
       AND (NEW.ep_encounter_mode IS NULL
            OR NEW.ep_encounter_mode <> OLD.ep_encounter_mode) THEN
        RAISE EXCEPTION
            'ep_encounter_mode is immutable after initial set (EP-2): '
            'prescription % already has ep_encounter_mode=''%'', '
            'cannot change to ''%''',
            OLD.prescription_id, OLD.ep_encounter_mode, NEW.ep_encounter_mode
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_prescriptions_ep_encounter_mode_immutable
    BEFORE UPDATE OF ep_encounter_mode ON ehr.prescriptions
    FOR EACH ROW
    EXECUTE FUNCTION ehr.fn_prescriptions_ep_encounter_mode_immutable();

--rollback DROP TRIGGER IF EXISTS trg_prescriptions_ep_encounter_mode_immutable ON ehr.prescriptions;
--rollback DROP FUNCTION IF EXISTS ehr.fn_prescriptions_ep_encounter_mode_immutable();
