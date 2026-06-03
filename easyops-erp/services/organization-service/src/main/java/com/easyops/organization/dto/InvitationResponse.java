package com.easyops.organization.dto;

import com.easyops.organization.entity.Invitation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Invitation Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponse {

    private UUID id;
    private UUID organizationId;
    private String email;
    private String role;
    private UUID invitedBy;
    private String status;
    private ZonedDateTime expiresAt;
    private ZonedDateTime acceptedAt;
    private ZonedDateTime createdAt;

    public static InvitationResponse fromEntity(Invitation invitation) {
        return InvitationResponse.builder()
                .id(invitation.getId())
                .organizationId(invitation.getOrganizationId())
                .email(invitation.getEmail())
                .role(invitation.getRole())
                .invitedBy(invitation.getInvitedBy())
                .status(invitation.getStatus())
                .expiresAt(invitation.getExpiresAt())
                .acceptedAt(invitation.getAcceptedAt())
                .createdAt(invitation.getCreatedAt())
                .build();
    }
}

