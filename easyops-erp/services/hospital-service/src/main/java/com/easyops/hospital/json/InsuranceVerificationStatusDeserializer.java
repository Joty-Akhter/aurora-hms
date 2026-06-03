package com.easyops.hospital.json;

import com.easyops.hospital.entity.PatientInsurance;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.Locale;

/**
 * Accepts common UI / legacy strings for insurance verification so invalid enum JSON
 * does not fall through to persistence errors (CHECK constraint / JDBC type mismatch).
 */
public class InsuranceVerificationStatusDeserializer extends JsonDeserializer<PatientInsurance.VerificationStatus> {

    @Override
    public PatientInsurance.VerificationStatus deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String raw = p.getValueAsString();
        if (raw == null || raw.isBlank()) {
            return PatientInsurance.VerificationStatus.Not_Verified;
        }
        String s = raw.trim();
        try {
            return PatientInsurance.VerificationStatus.valueOf(s);
        } catch (IllegalArgumentException ignored) {
            // continue normalisation
        }
        String compact = s.replace(" ", "_").replace("-", "_");
        for (PatientInsurance.VerificationStatus v : PatientInsurance.VerificationStatus.values()) {
            if (v.name().equalsIgnoreCase(compact)) {
                return v;
            }
        }
        String u = compact.toUpperCase(Locale.ROOT);
        if (u.contains("NOT") && (u.contains("VERIF") || u.contains("VERIFY"))) {
            return PatientInsurance.VerificationStatus.Not_Verified;
        }
        if (u.contains("NOT") && u.contains("APPLIC")) {
            return PatientInsurance.VerificationStatus.Not_Applicable;
        }
        if (u.contains("PEND")) {
            return PatientInsurance.VerificationStatus.Pending;
        }
        if (u.contains("VERIF") && !u.contains("NOT")) {
            return PatientInsurance.VerificationStatus.Verified;
        }
        return PatientInsurance.VerificationStatus.Not_Verified;
    }
}
