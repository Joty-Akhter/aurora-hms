package com.easyops.pharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Territory with warehouse_id (Phase 3: Warehouse per territory).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TerritoryDTO {

    private UUID id;
    private UUID organizationId;
    private UUID divisionId;
    private UUID regionId;
    private UUID areaId;
    private UUID warehouseId;
    private String name;
    private String code;
    private String description;
    private String status;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
