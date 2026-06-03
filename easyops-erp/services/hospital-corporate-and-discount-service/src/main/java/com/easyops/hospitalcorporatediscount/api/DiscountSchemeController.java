package com.easyops.hospitalcorporatediscount.api;

import com.easyops.hospitalcorporatediscount.api.dto.*;
import com.easyops.hospitalcorporatediscount.domain.discount.DiscountSchemeService;
import com.easyops.hospitalcorporatediscount.security.HospitalCorporateDiscountRbacService;
import com.easyops.hospitalcorporatediscount.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-corporate-discount")
@RequiredArgsConstructor
public class DiscountSchemeController {

    private final DiscountSchemeService discountSchemeService;
    private final HospitalCorporateDiscountRbacService hospitalCorporateDiscountRbac;

    @PostMapping("/discount-schemes")
    public ResponseEntity<DiscountSchemeResponse> create(
            @Valid @RequestBody CreateDiscountSchemeRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalManage(actor, organizationId);
        DiscountSchemeResponse created = discountSchemeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/discount-schemes/{id}")
    public DiscountSchemeDetailResponse getById(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return discountSchemeService.getById(id);
    }

    @GetMapping("/discount-schemes")
    public PagedResponse<DiscountSchemeResponse> list(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "corporateClientId", required = false) UUID corporateClientId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return discountSchemeService.list(code, corporateClientId, status, page, size);
    }

    @PatchMapping("/discount-schemes/{id}")
    public DiscountSchemeResponse update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateDiscountSchemeRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalManage(actor, organizationId);
        return discountSchemeService.update(id, request);
    }

    @PostMapping("/discount-schemes/{id}/approval-levels")
    public ResponseEntity<DiscountApprovalLevelResponse> addApprovalLevel(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateApprovalLevelRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalManage(actor, organizationId);
        DiscountApprovalLevelResponse created = discountSchemeService.addApprovalLevel(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/discount-schemes/{id}/approval-levels")
    public List<DiscountApprovalLevelResponse> getApprovalLevels(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return discountSchemeService.getApprovalLevels(id);
    }

    @DeleteMapping("/discount-schemes/{schemeId}/approval-levels/{levelId}")
    public ResponseEntity<Void> deleteApprovalLevel(
            @PathVariable("schemeId") UUID schemeId,
            @PathVariable("levelId") UUID levelId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalManage(actor, organizationId);
        discountSchemeService.deleteApprovalLevel(schemeId, levelId);
        return ResponseEntity.noContent().build();
    }
}
