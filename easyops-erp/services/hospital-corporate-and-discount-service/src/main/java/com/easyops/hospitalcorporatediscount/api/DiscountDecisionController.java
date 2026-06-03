package com.easyops.hospitalcorporatediscount.api;

import com.easyops.hospitalcorporatediscount.api.dto.CreateDiscountDecisionRequest;
import com.easyops.hospitalcorporatediscount.api.dto.DiscountDecisionResponse;
import com.easyops.hospitalcorporatediscount.api.dto.PagedResponse;
import com.easyops.hospitalcorporatediscount.domain.discount.DiscountDecisionService;
import com.easyops.hospitalcorporatediscount.security.HospitalCorporateDiscountRbacService;
import com.easyops.hospitalcorporatediscount.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-corporate-discount")
@RequiredArgsConstructor
public class DiscountDecisionController {

    private final DiscountDecisionService discountDecisionService;
    private final HospitalCorporateDiscountRbacService hospitalCorporateDiscountRbac;

    @GetMapping("/discount-decisions")
    public PagedResponse<DiscountDecisionResponse> list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return discountDecisionService.list(page, size);
    }

    @PostMapping("/discount-decisions")
    public ResponseEntity<DiscountDecisionResponse> create(
            @Valid @RequestBody CreateDiscountDecisionRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalManage(actor, organizationId);
        DiscountDecisionResponse created = discountDecisionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/discount-decisions/{id}")
    public DiscountDecisionResponse getById(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return discountDecisionService.getById(id);
    }
}
