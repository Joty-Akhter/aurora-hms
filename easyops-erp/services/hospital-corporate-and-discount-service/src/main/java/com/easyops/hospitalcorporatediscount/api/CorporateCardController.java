package com.easyops.hospitalcorporatediscount.api;

import com.easyops.hospitalcorporatediscount.api.dto.CorporateCardResponse;
import com.easyops.hospitalcorporatediscount.api.dto.CorporateCardValidationResponse;
import com.easyops.hospitalcorporatediscount.api.dto.BlockCorporateCardRequest;
import com.easyops.hospitalcorporatediscount.api.dto.CreateCorporateCardRequest;
import com.easyops.hospitalcorporatediscount.api.dto.PagedResponse;
import com.easyops.hospitalcorporatediscount.api.dto.ReissueCorporateCardRequest;
import com.easyops.hospitalcorporatediscount.domain.corporatecard.CorporateCardService;
import com.easyops.hospitalcorporatediscount.security.HospitalCorporateDiscountRbacService;
import com.easyops.hospitalcorporatediscount.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-corporate-discount/corporate-cards")
@RequiredArgsConstructor
public class CorporateCardController {

    private final CorporateCardService corporateCardService;
    private final HospitalCorporateDiscountRbacService hospitalCorporateDiscountRbac;

    @PostMapping
    public ResponseEntity<CorporateCardResponse> issue(
            @Valid @RequestBody CreateCorporateCardRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalManage(actor, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(corporateCardService.issue(request, actor, organizationId));
    }

    @PostMapping("/{corporateCardId}/reissue")
    public CorporateCardResponse reissue(
            @PathVariable UUID corporateCardId,
            @Valid @RequestBody ReissueCorporateCardRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalManage(actor, organizationId);
        return corporateCardService.reissue(corporateCardId, request.getReason(), actor, organizationId);
    }

    @PostMapping("/{corporateCardId}/reprint")
    public CorporateCardResponse reprint(
            @PathVariable UUID corporateCardId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return corporateCardService.reprint(corporateCardId);
    }

    @PostMapping("/{corporateCardId}/block")
    public CorporateCardResponse block(
            @PathVariable UUID corporateCardId,
            @Valid @RequestBody BlockCorporateCardRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalManage(actor, organizationId);
        return corporateCardService.block(corporateCardId, request.getReason(), actor, organizationId);
    }

    @GetMapping("/{corporateCardId}")
    public CorporateCardResponse getById(
            @PathVariable UUID corporateCardId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return corporateCardService.getById(corporateCardId);
    }

    @GetMapping
    public PagedResponse<CorporateCardResponse> list(
            @RequestParam(value = "corporateClientId", required = false) UUID corporateClientId,
            @RequestParam(value = "holderIdentifier", required = false) String holderIdentifier,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return corporateCardService.list(corporateClientId, holderIdentifier, status, page, size);
    }

    @GetMapping("/validate")
    public CorporateCardValidationResponse validateForBilling(
            @RequestParam("cardNumber") String cardNumber,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return corporateCardService.validateForBilling(cardNumber, actor, organizationId);
    }
}
