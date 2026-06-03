package com.easyops.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoanAccountingCoaMappingUpsertRequest {

    @NotBlank
    @Size(max = 64)
    private String mappingKey;

    @NotBlank
    @Size(max = 64)
    private String debitAccountCode;

    @NotBlank
    @Size(max = 64)
    private String creditAccountCode;

    @Size(max = 500)
    private String notes;
}
