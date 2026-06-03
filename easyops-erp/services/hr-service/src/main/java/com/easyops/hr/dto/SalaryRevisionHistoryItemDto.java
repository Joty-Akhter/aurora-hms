package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * ES-18: Single item in queryable revision history (assignment or component detail).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryRevisionHistoryItemDto {
    public static final String KIND_ASSIGNMENT = "ASSIGNMENT";
    public static final String KIND_COMPONENT = "COMPONENT";

    private String kind;
    private UUID id;
    private UUID employeeId;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String revisionType;
    private String revisionReason;
    private String summary;
    private String createdBy;
}
