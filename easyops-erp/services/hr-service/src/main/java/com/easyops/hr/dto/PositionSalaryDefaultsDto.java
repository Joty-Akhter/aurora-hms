package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * ES-06: Default structure/grade/band from a position for suggesting or auto-filling employee salary assignment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionSalaryDefaultsDto {
    private UUID positionId;
    private UUID organizationId;
    private UUID defaultSalaryStructureId;
    private UUID defaultSalaryGradeId;
    private UUID defaultSalaryBandId;
}
