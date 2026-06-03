package com.easyops.hospitalcorporatediscount.api.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UpdateContractRequest {

    @Size(max = 50)
    private String contractCode;

    @Size(max = 255)
    private String contractName;

    private LocalDate validFrom;
    private LocalDate validTo;

    @Size(max = 20)
    private String coverageType;

    @Size(max = 500)
    private String serviceLocations;

    public String getContractCode() { return contractCode; }
    public void setContractCode(String contractCode) { this.contractCode = contractCode; }
    public String getContractName() { return contractName; }
    public void setContractName(String contractName) { this.contractName = contractName; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public String getCoverageType() { return coverageType; }
    public void setCoverageType(String coverageType) { this.coverageType = coverageType; }
    public String getServiceLocations() { return serviceLocations; }
    public void setServiceLocations(String serviceLocations) { this.serviceLocations = serviceLocations; }
}
