package com.easyops.hospitalcorporatediscount.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class PackageItemResponse {

    private UUID id;
    private UUID packageId;
    private String itemType;
    private String itemCode;
    private BigDecimal quantityIncluded;
    private OffsetDateTime createdAt;

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
