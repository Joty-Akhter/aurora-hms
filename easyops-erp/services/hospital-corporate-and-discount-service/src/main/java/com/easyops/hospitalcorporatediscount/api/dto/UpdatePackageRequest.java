package com.easyops.hospitalcorporatediscount.api.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class UpdatePackageRequest {

    @Size(max = 50)
    private String code;

    @Size(max = 255)
    private String name;

    private String description;

    private BigDecimal defaultPrice;

    private Boolean isCorporateOnly;

    private Boolean isPublic;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getDefaultPrice() { return defaultPrice; }
    public void setDefaultPrice(BigDecimal defaultPrice) { this.defaultPrice = defaultPrice; }
    public Boolean getIsCorporateOnly() { return isCorporateOnly; }
    public void setIsCorporateOnly(Boolean isCorporateOnly) { this.isCorporateOnly = isCorporateOnly; }
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
}
