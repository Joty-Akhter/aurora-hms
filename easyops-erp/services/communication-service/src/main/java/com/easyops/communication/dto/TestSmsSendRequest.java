package com.easyops.communication.dto;

import jakarta.validation.constraints.NotBlank;

public record TestSmsSendRequest(
        @NotBlank String recipient
) {
}

