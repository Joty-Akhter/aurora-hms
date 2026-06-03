package com.easyops.hospitalcard.api;

import com.easyops.hospitalcard.api.dto.CardResponse;
import com.easyops.hospitalcard.api.dto.IssueStaffCardRequest;
import com.easyops.hospitalcard.api.dto.ReplaceStaffCardRequest;
import com.easyops.hospitalcard.api.dto.StaffCardActionRequest;
import com.easyops.hospitalcard.api.dto.StaffCardVerificationResponse;
import com.easyops.hospitalcard.api.dto.StaffEmploymentStatusSyncRequest;
import com.easyops.hospitalcard.domain.card.CardService;
import com.easyops.hospitalcard.security.HospitalCardRbacService;
import com.easyops.hospitalcard.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-card-management/staff-cards")
@RequiredArgsConstructor
public class StaffCardController {

    private final CardService cardService;
    private final HospitalCardRbacService hospitalCardRbac;

    @PostMapping
    public ResponseEntity<CardResponse> issue(
            @Valid @RequestBody IssueStaffCardRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalManage(actor, organizationId);
        CardResponse created = cardService.issueStaffIdentity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/replace")
    public CardResponse replace(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ReplaceStaffCardRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalManage(actor, organizationId);
        return cardService.replaceStaffIdentity(id, request);
    }

    @PostMapping("/{id}/reprint")
    public CardResponse reprint(
            @PathVariable("id") UUID id,
            @Valid @RequestBody StaffCardActionRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalManage(actor, organizationId);
        return cardService.reprintStaffIdentity(id, request.getReason());
    }

    @PostMapping("/{id}/suspend")
    public CardResponse suspend(
            @PathVariable("id") UUID id,
            @Valid @RequestBody StaffCardActionRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalManage(actor, organizationId);
        return cardService.suspendStaffIdentity(id, request.getReason());
    }

    @PostMapping("/{id}/revoke")
    public CardResponse revoke(
            @PathVariable("id") UUID id,
            @Valid @RequestBody StaffCardActionRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalManage(actor, organizationId);
        return cardService.revokeStaffIdentity(id, request.getReason());
    }

    @GetMapping("/verify")
    public StaffCardVerificationResponse verify(
            @RequestParam(value = "cardNumber", required = false) String cardNumber,
            @RequestParam(value = "employeeId", required = false) String employeeId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalView(actor, organizationId);
        return cardService.verifyStaffCard(cardNumber, employeeId);
    }

    @PostMapping("/sync-employment-status")
    public List<CardResponse> syncEmploymentStatus(
            @Valid @RequestBody StaffEmploymentStatusSyncRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalManage(actor, organizationId);
        return cardService.syncStaffEmploymentStatus(request);
    }

    @PostMapping("/events/employment-status")
    public List<CardResponse> onEmploymentStatusEvent(
            @Valid @RequestBody StaffEmploymentStatusSyncRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalManage(actor, organizationId);
        return cardService.syncStaffEmploymentStatus(request);
    }
}
