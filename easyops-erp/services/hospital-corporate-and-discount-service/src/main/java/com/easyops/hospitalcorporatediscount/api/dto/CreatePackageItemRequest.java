package com.easyops.hospitalcorporatediscount.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreatePackageItemRequest {

    @NotBlank
    @Size(max = 20)
    private String itemType;

    @NotBlank
    @Size(max = 100)
    private String itemCode;

    private BigDecimal quantityIncluded;

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public BigDecimal getQuantityIncluded() { return quantityIncluded; }
    public void setQuantityIncluded(BigDecimal quantityIncluded) { this.quantityIncluded = quantityIncluded; }
}
