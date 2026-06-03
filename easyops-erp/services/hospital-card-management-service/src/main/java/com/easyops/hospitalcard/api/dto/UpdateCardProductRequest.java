package com.easyops.hospitalcard.api.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Partial update: only non-null fields are applied.
 */
public class UpdateCardProductRequest {

    private String name;
    private String description;
    private String mediumType;
    private String usageDomains;
    private UUID defaultLimitProfileId;
    private LocalDate validityStartDate;
    private LocalDate validityEndDate;
    private String status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMediumType() {
        return mediumType;
    }

    public void setMediumType(String mediumType) {
        this.mediumType = mediumType;
    }

    public String getUsageDomains() {
        return usageDomains;
    }

    public void setUsageDomains(String usageDomains) {
        this.usageDomains = usageDomains;
    }

    public UUID getDefaultLimitProfileId() {
        return defaultLimitProfileId;
    }

    public void setDefaultLimitProfileId(UUID defaultLimitProfileId) {
        this.defaultLimitProfileId = defaultLimitProfileId;
    }

    public LocalDate getValidityStartDate() {
        return validityStartDate;
    }

    public void setValidityStartDate(LocalDate validityStartDate) {
        this.validityStartDate = validityStartDate;
    }

    public LocalDate getValidityEndDate() {
        return validityEndDate;
    }

    public void setValidityEndDate(LocalDate validityEndDate) {
        this.validityEndDate = validityEndDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
