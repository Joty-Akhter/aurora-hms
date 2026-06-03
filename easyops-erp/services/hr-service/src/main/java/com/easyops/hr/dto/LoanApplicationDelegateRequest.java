package com.easyops.hr.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class LoanApplicationDelegateRequest {

    @NotNull
    private UUID delegateToUserId;
}
