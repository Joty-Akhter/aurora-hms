package com.easyops.hr.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoanApplicationClarificationRequest {

    @NotBlank
    private String message;
}
