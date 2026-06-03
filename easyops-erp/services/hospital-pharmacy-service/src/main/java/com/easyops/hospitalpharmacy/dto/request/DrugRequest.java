package com.easyops.hospitalpharmacy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class DrugRequest {

    @NotBlank
    @Size(max = 255)
    private String genericName;

    @Size(max = 255)
    private String brandName;

    @Size(max = 100)
    private String strength;

    @Size(max = 100)
    private String form;

    @Size(max = 100)
    private String route;

    @Size(max = 50)
    private String packSize;

    @Size(max = 50)
    private String unitOfMeasure;

    private UUID therapeuticClassId;

    private Boolean active;

    private Boolean controlledDrugFlag;

    /** Phase P4 — optional profile code (e.g. US_DEA_II); null leaves column unset. */
    @Size(max = 64)
    private String controlledProfileCode;

    private Boolean batchRequired;

    private Boolean expiryRequired;

    @NotNull
    private UUID manufacturerId;

    private UUID productGroupId;

    private UUID dispensingUnitId;

    private BigDecimal mrp;

    private BigDecimal salePrice;

    private BigDecimal purchasePrice;

    @Size(max = 50)
    private String rackNo;

    private BigDecimal reminderStock;

    @Size(max = 50)
    private String hsnCode;

    @Size(max = 100)
    private String productCode;

    private UUID departmentId;
}

