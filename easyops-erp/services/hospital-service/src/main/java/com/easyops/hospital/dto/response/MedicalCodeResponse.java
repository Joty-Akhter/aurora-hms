package com.easyops.hospital.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MedicalCodeResponse {
    private String code;
    private String description;
    private String category;
    private String chapter;
    private Boolean isValid;
    private String codeType;
}
