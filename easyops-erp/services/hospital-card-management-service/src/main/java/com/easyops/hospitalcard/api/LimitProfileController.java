package com.easyops.hospitalcard.api;

import com.easyops.hospitalcard.api.dto.CardWithLimitUsageResponse;
import com.easyops.hospitalcard.api.dto.CreateLimitProfileRequest;
import com.easyops.hospitalcard.api.dto.LimitProfileResponse;
import com.easyops.hospitalcard.api.dto.PagedResponse;
import com.easyops.hospitalcard.domain.product.LimitProfileService;
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
@RequestMapping("/api/hospital-card-management/limit-profiles")
@RequiredArgsConstructor
public class LimitProfileController {

    private final LimitProfileService limitProfileService;
    private final HospitalCardRbacService hospitalCardRbac;

    @PostMapping
    public ResponseEntity<LimitProfileResponse> create(
            @Valid @RequestBody CreateLimitProfileRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalManage(actor, organizationId);
        LimitProfileResponse created = limitProfileService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public LimitProfileResponse getById(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalView(actor, organizationId);
        return limitProfileService.getById(id);
    }

    @GetMapping("/{id}/cards-with-usage")
    public List<CardWithLimitUsageResponse> getCardsWithUsage(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalView(actor, organizationId);
        return limitProfileService.getCardsWithUsage(id);
    }

    @GetMapping
    public PagedResponse<LimitProfileResponse> list(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalView(actor, organizationId);
        return limitProfileService.list(name, page, size);
    }
}
