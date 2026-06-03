package com.easyops.hospitalcorporatediscount.api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class CorporateResponse {

    private UUID id;
    private String name;
    private String code;
    private String type;
    private String status;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String primaryContactName;
    private String primaryContactPhone;
    private String primaryContactEmail;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public String getPrimaryContactName() { return primaryContactName; }
    public void setPrimaryContactName(String primaryContactName) { this.primaryContactName = primaryContactName; }
    public String getPrimaryContactPhone() { return primaryContactPhone; }
    public void setPrimaryContactPhone(String primaryContactPhone) { this.primaryContactPhone = primaryContactPhone; }
    public String getPrimaryContactEmail() { return primaryContactEmail; }
    public void setPrimaryContactEmail(String primaryContactEmail) { this.primaryContactEmail = primaryContactEmail; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
