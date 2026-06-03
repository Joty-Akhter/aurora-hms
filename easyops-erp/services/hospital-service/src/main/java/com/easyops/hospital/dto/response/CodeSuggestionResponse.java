package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeSuggestionResponse {
    private String code;
    private String description;
    private String codeType; // ICD10, ICD11, SNOMED
}
