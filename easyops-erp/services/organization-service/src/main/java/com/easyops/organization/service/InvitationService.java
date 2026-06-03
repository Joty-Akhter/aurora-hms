package com.easyops.organization.service;

import com.easyops.organization.dto.InvitationRequest;
import com.easyops.organization.dto.InvitationResponse;
import com.easyops.organization.entity.Invitation;
import com.easyops.organization.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Invitation Service
 * Handles business logic for organization invitations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private final InvitationRepository invitationRepository;

    /**
     * Send invitation
     */
    @Transactional
    public InvitationResponse sendInvitation(UUID organizationId, InvitationRequest request, UUID invitedBy) {
        log.info("Sending invitation to {} for organization: {}", request.getEmail(), organizationId);

        // Check if there's already a pending invitation
        if (invitationRepository.existsByEmailAndOrganizationIdAndStatus(
                request.getEmail(), organizationId, "PENDING")) {
            throw new IllegalStateException("Pending invitation already exists for this email");
        }

        // Generate secure token
        String token = UUID.randomUUID().toString();

        Invitation invitation = Invitation.builder()
                .organizationId(organizationId)
                .email(request.getEmail())
                .role(request.getRole())
                .invitedBy(invitedBy)
                .token(token)
                .status("PENDING")
                .expiresAt(ZonedDateTime.now().plusDays(7)) // Invitation valid for 7 days
                .build();

        Invitation saved = invitationRepository.save(invitation);
        log.info("Invitation sent successfully: {}", saved.getId());

        // In a real application, send email notification here
        // emailService.sendInvitationEmail(request.getEmail(), token);

        return InvitationResponse.fromEntity(saved);
    }

    /**
     * Get invitation by token
     */
    public InvitationResponse getInvitationByToken(String token) {
        log.debug("Fetching invitation by token");
        
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invitation token"));

        // Check if expired
        if (invitation.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new IllegalStateException("Invitation has expired");
        }

        if (!"PENDING".equals(invitation.getStatus())) {
            throw new IllegalStateException("Invitation is not pending");
        }

        return InvitationResponse.fromEntity(invitation);
    }

    /**
     * Accept invitation
     */
    @Transactional
    public InvitationResponse acceptInvitation(String token) {
        log.info("Accepting invitation with token");

        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invitation token"));

        if (invitation.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new IllegalStateException("Invitation has expired");
        }

        if (!"PENDING".equals(invitation.getStatus())) {
            throw new IllegalStateException("Invitation is not pending");
        }

        invitation.setStatus("ACCEPTED");
        invitation.setAcceptedAt(ZonedDateTime.now());

        Invitation updated = invitationRepository.save(invitation);
        log.info("Invitation accepted successfully");

        return InvitationResponse.fromEntity(updated);
    }

    /**
     * Cancel invitation
     */
    @Transactional
    public void cancelInvitation(UUID invitationId) {
        log.info("Cancelling invitation: {}", invitationId);

        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        if (!"PENDING".equals(invitation.getStatus())) {
            throw new IllegalStateException("Only pending invitations can be cancelled");
        }

        invitation.setStatus("CANCELLED");
        invitationRepository.save(invitation);

        log.info("Invitation cancelled successfully: {}", invitationId);
    }

    /**
     * Get invitations for organization
     */
    public List<InvitationResponse> getOrganizationInvitations(UUID organizationId) {
        log.debug("Fetching invitations for organization: {}", organizationId);
        return invitationRepository.findByOrganizationId(organizationId).stream()
                .map(InvitationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get pending invitations for email
     */
    public List<InvitationResponse> getPendingInvitations(String email) {
        log.debug("Fetching pending invitations for email: {}", email);
        return invitationRepository.findByEmailAndStatus(email, "PENDING").stream()
                .map(InvitationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Clean up expired invitations
     */
    @Transactional
    public void cleanupExpiredInvitations() {
        log.info("Cleaning up expired invitations");
        invitationRepository.deleteByExpiresAtBefore(ZonedDateTime.now());
    }
}

