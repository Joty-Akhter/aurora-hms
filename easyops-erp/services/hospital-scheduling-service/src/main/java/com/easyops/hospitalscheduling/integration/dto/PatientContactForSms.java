package com.easyops.hospitalscheduling.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Subset of hospital-service {@code PatientResponse} JSON for waitlist → appointment SMS enrichment.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatientContactForSms {

    private UUID patientId;
    private String fullName;
    private String primaryPhone;
    private String secondaryPhone;
    /** When false, do not copy registry phones into appointment SMS fields (hospital-service consent). */
    private Boolean consentTextMessaging;

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPrimaryPhone() {
        return primaryPhone;
    }

    public void setPrimaryPhone(String primaryPhone) {
        this.primaryPhone = primaryPhone;
    }

    public String getSecondaryPhone() {
        return secondaryPhone;
    }

    public void setSecondaryPhone(String secondaryPhone) {
        this.secondaryPhone = secondaryPhone;
    }

    public Boolean getConsentTextMessaging() {
        return consentTextMessaging;
    }

    public void setConsentTextMessaging(Boolean consentTextMessaging) {
        this.consentTextMessaging = consentTextMessaging;
    }
}
