package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.dto.response.ConsumptionReportItemResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PharmacyReportServiceCsvTest {

    @Test
    void formatConsumptionCsv_includesHeaderAndEscapesQuotes() {
        UUID id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        List<ConsumptionReportItemResponse> rows = List.of(
                ConsumptionReportItemResponse.builder()
                        .drugId(id)
                        .genericName("Aspirin, \"low dose\"")
                        .brandName("Brand")
                        .strength("81mg")
                        .form("tablet")
                        .route("oral")
                        .totalQuantityIssued(new BigDecimal("10.5"))
                        .build());

        String csv = PharmacyReportService.formatConsumptionCsv(rows);

        assertThat(csv).startsWith("drugId,genericName,brandName,strength,form,route,totalQuantityIssued\n");
        assertThat(csv).contains("\"Aspirin, \"\"low dose\"\"\"");
        assertThat(csv).contains("10.5");
    }
}
