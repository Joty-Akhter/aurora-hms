package com.easyops.hr.dto;

import com.easyops.hr.entity.LoanApplicationActionType;
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
public class LoanApplicationActionDto {

    private UUID actionId;
    private UUID applicationId;
    private LoanApplicationActionType actionType;
    private UUID actorUserId;
    private String commentText;
    private LocalDateTime createdAt;
}
