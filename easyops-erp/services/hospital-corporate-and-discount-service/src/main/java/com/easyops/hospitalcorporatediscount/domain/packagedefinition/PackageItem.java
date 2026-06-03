package com.easyops.hospitalcorporatediscount.domain.packagedefinition;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "package_items", schema = "hospital_corporate_discount")
@EntityListeners(AuditingEntityListener.class)
public class PackageItem {

    @Id
    private UUID id;

    @Column(name = "package_id", nullable = false)
    private UUID packageId;

    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType;

    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    @Column(name = "quantity_included", precision = 19, scale = 4)
    private BigDecimal quantityIncluded = BigDecimal.ONE;

    @CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPackageId() { return packageId; }
    public void setPackageId(UUID packageId) { this.packageId = packageId; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public BigDecimal getQuantityIncluded() { return quantityIncluded; }
    public void setQuantityIncluded(BigDecimal quantityIncluded) { this.quantityIncluded = quantityIncluded; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
