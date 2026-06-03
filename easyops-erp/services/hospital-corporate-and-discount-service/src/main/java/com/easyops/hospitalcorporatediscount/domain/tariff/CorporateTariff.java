package com.easyops.hospitalcorporatediscount.domain.tariff;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "corporate_tariffs", schema = "hospital_corporate_discount")
@EntityListeners(AuditingEntityListener.class)
public class CorporateTariff {

    @Id
    private UUID id;

    @Column(name = "corporate_contract_id", nullable = false)
    private UUID corporateContractId;

    @Column(name = "scope_type", nullable = false, length = 20)
    private String scopeType;

    @Column(name = "scope_value", nullable = false, length = 100)
    private String scopeValue;

    @Column(name = "tariff_type", nullable = false, length = 20)
    private String tariffType;

    @Column(name = "tariff_amount", precision = 19, scale = 4)
    private BigDecimal tariffAmount;

    @Column(name = "tariff_percent", precision = 5, scale = 2)
    private BigDecimal tariffPercent;

    @CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCorporateContractId() { return corporateContractId; }
    public void setCorporateContractId(UUID corporateContractId) { this.corporateContractId = corporateContractId; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public String getScopeValue() { return scopeValue; }
    public void setScopeValue(String scopeValue) { this.scopeValue = scopeValue; }
    public String getTariffType() { return tariffType; }
    public void setTariffType(String tariffType) { this.tariffType = tariffType; }
    public BigDecimal getTariffAmount() { return tariffAmount; }
    public void setTariffAmount(BigDecimal tariffAmount) { this.tariffAmount = tariffAmount; }
    public BigDecimal getTariffPercent() { return tariffPercent; }
    public void setTariffPercent(BigDecimal tariffPercent) { this.tariffPercent = tariffPercent; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
