package com.easyops.hospitalcard.api;

import com.easyops.hospitalcard.api.dto.*;
import com.easyops.hospitalcard.domain.authorization.AuthorizationService;
import com.easyops.hospitalcard.security.HospitalCardRbacService;
import com.easyops.hospitalcard.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-card-management/authorizations")
@RequiredArgsConstructor
public class AuthorizationController {

    private final AuthorizationService authorizationService;
    private final HospitalCardRbacService hospitalCardRbac;

    @PostMapping
    public ResponseEntity<AuthorizationResponse> authorize(
            @Valid @RequestBody AuthorizationRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalManage(actor, organizationId);
        AuthorizationResponse response = authorizationService.authorize(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{authId}/capture")
    public ResponseEntity<CardTransactionResponse> capture(
            @PathVariable("authId") UUID authId,
            @Valid @RequestBody CaptureRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalManage(actor, organizationId);
        CardTransactionResponse result = authorizationService.capture(authId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
