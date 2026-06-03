package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanAccountingCoaMappingDto {

    private UUID mappingId;
    private UUID organizationId;
    private String mappingKey;
    private String debitAccountCode;
    private String creditAccountCode;
    private String notes;
}
