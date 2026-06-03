package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** SS-49, SC-50: Result of bulk import with counts and per-row errors. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkImportResultDto {
    private int createdStructures;
    private int createdGrades;
    private int createdBands;
    /** SC-50: Count of components created by component bulk import. */
    @Builder.Default
    private int createdComponents = 0;
    @Builder.Default
    private List<BulkImportErrorDto> errors = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkImportErrorDto {
        private int row;       // 1-based
        private String type;   // Structure, Grade, Band, Component
        private String message;
    }
}
