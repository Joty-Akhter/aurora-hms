package com.easyops.communication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InternalOutboundEmailRequest(
        @NotBlank @Email @Size(max = 255) String recipient,
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 4000) String body
) {
}
