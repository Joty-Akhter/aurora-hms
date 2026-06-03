package com.easyops.hospitalcard.api;

import com.easyops.hospitalcard.api.dto.CardProductResponse;
import com.easyops.hospitalcard.api.dto.CreateCardProductRequest;
import com.easyops.hospitalcard.api.dto.PagedResponse;
import com.easyops.hospitalcard.api.dto.UpdateCardProductRequest;
import com.easyops.hospitalcard.domain.product.CardProductService;
import com.easyops.hospitalcard.security.HospitalCardRbacService;
import com.easyops.hospitalcard.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-card-management/card-products")
@RequiredArgsConstructor
public class CardProductController {

    private final CardProductService cardProductService;
    private final HospitalCardRbacService hospitalCardRbac;

    @PostMapping
    public ResponseEntity<CardProductResponse> create(
            @Valid @RequestBody CreateCardProductRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalManage(actor, organizationId);
        CardProductResponse created = cardProductService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public CardProductResponse getById(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalView(actor, organizationId);
        return cardProductService.getById(id);
    }

    @GetMapping
    public PagedResponse<CardProductResponse> list(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalView(actor, organizationId);
        return cardProductService.list(code, status, page, size);
    }

    @PatchMapping("/{id}")
    public CardProductResponse update(
            @PathVariable("id") UUID id,
            @RequestBody UpdateCardProductRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalManage(actor, organizationId);
        return cardProductService.update(id, request);
    }
}
