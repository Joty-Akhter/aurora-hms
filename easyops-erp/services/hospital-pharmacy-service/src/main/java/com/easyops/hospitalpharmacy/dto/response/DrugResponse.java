package com.easyops.hospitalpharmacy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class DrugResponse {

    private UUID id;
    private String genericName;
    private String brandName;
    private String strength;
    private String form;
    private String route;
    private String packSize;
    private String unitOfMeasure;
    private UUID therapeuticClassId;
    private boolean active;
    private boolean controlledDrugFlag;
    private String controlledProfileCode;
    private boolean batchRequired;
    private boolean expiryRequired;
    private UUID manufacturerId;
    private String manufacturerName;
    private UUID productGroupId;
    private String productGroupName;
    private UUID dispensingUnitId;
    private String dispensingUnitAbbreviation;
    private BigDecimal mrp;
    private BigDecimal salePrice;
    private BigDecimal purchasePrice;
    private String rackNo;
    private BigDecimal reminderStock;
    private String hsnCode;
    private String productCode;
    private UUID departmentId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

