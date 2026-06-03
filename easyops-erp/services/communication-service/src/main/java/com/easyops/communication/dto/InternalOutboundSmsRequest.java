package com.easyops.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InternalOutboundSmsRequest(
        @NotBlank @Size(max = 64) String recipient,
        @NotBlank @Size(max = 640) String message
) {
}
