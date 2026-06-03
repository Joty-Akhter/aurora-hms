package com.easyops.hospitalcorporatediscount.domain.contract;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "corporate_contracts", schema = "hospital_corporate_discount")
@EntityListeners(AuditingEntityListener.class)
public class CorporateContract {

    @Id
    private UUID id;

    @Column(name = "corporate_client_id", nullable = false)
    private UUID corporateClientId;

    @Column(name = "contract_code", nullable = false, length = 50)
    private String contractCode;

    @Column(name = "contract_name", length = 255)
    private String contractName;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "coverage_type", nullable = false, length = 20)
    private String coverageType;

    @Column(name = "service_locations", length = 500)
    private String serviceLocations;

    @Column(name = "created_at")
    @CreatedDate
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCorporateClientId() { return corporateClientId; }
    public void setCorporateClientId(UUID corporateClientId) { this.corporateClientId = corporateClientId; }
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
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
