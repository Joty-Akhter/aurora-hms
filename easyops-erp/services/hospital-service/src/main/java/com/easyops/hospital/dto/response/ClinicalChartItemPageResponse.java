package com.easyops.hospital.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClinicalChartItemPageResponse {
    private List<ClinicalChartItemResponse> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
