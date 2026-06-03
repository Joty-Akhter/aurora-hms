package com.easyops.hospitalpharmacy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class UnitResponse {

    private UUID id;
    private String name;
    private String abbreviation;
    private UUID baseUnitId;
    private String baseUnitName;
    private BigDecimal conversionFactor;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
