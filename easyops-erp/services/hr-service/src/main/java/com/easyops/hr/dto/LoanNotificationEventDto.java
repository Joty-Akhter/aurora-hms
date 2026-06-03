package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanNotificationEventDto {

    private UUID eventId;
    private UUID organizationId;
    private String eventType;
    private String title;
    private String body;
    private UUID loanApplicationId;
    private UUID loanId;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
