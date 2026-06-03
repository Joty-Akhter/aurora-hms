package com.easyops.hr.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanSettlementCloseRequest {

    /**
     * If true and outstanding balance is positive, close the loan by writing off the remainder
     * (sets settlement shortfall and zeros balance; ST-04).
     */
    @NotNull
    private Boolean writeOffRemaining;

    private String reason;

    /** ST-04: e.g. LEGAL_REVIEW, BOARD_WRITE_OFF, WRITTEN_OFF. */
    private String settlementWriteOffPath;

    private String legalCaseReference;

    private String writeOffNotes;
}
