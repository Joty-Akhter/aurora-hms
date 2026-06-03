package com.easyops.hospitalcorporatediscount.api.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UpdateCorporateRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 50)
    private String code;

    @Size(max = 30)
    private String type;

    @Size(max = 20)
    private String status;

    private LocalDate validFrom;
    private LocalDate validTo;

    @Size(max = 255)
    private String primaryContactName;

    @Size(max = 50)
    private String primaryContactPhone;

    @Size(max = 255)
    private String primaryContactEmail;

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
}
