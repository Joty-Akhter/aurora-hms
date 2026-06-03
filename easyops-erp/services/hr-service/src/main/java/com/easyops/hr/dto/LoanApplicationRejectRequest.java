package com.easyops.hr.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoanApplicationRejectRequest {

    @NotBlank
    private String reason;
}
