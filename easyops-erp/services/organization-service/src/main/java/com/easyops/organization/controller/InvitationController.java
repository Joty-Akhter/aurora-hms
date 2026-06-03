package com.easyops.organization.controller;

import com.easyops.organization.dto.InvitationRequest;
import com.easyops.organization.dto.InvitationResponse;
import com.easyops.organization.security.OrganizationRbacService;
import com.easyops.organization.security.RbacRequestHeaders;
import com.easyops.organization.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Invitation Controller
 */
@RestController
@RequestMapping("/api/organizations/{organizationId}/invitations")
@RequiredArgsConstructor
@Slf4j
public class InvitationController {

    private final InvitationService invitationService;
    private final OrganizationRbacService organizationRbac;

    @PostMapping
    public ResponseEntity<InvitationResponse> sendInvitation(
            @PathVariable UUID organizationId,
            @Valid @RequestBody InvitationRequest request,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgWrite(actor, organizationId);
        InvitationResponse response = invitationService.sendInvitation(organizationId, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<InvitationResponse>> getInvitations(
            @PathVariable UUID organizationId,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgRead(actor, organizationId);
        List<InvitationResponse> response = invitationService.getOrganizationInvitations(organizationId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{invitationId}")
    public ResponseEntity<Void> cancelInvitation(
            @PathVariable UUID organizationId,
            @PathVariable UUID invitationId,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgWrite(actor, organizationId);
        invitationService.cancelInvitation(invitationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/accept/{token}")
    public ResponseEntity<InvitationResponse> acceptInvitation(@PathVariable String token) {
        InvitationResponse response = invitationService.acceptInvitation(token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/token/{token}")
    public ResponseEntity<InvitationResponse> getInvitationByToken(@PathVariable String token) {
        InvitationResponse response = invitationService.getInvitationByToken(token);
        return ResponseEntity.ok(response);
    }
}
