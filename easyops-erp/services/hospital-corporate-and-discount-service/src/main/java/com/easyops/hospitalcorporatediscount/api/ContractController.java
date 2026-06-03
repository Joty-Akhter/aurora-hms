package com.easyops.hospitalcorporatediscount.api;

import com.easyops.hospitalcorporatediscount.api.dto.*;
import com.easyops.hospitalcorporatediscount.domain.contract.CorporateContractService;
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
public class ContractController {

    private final CorporateContractService contractService;
    private final HospitalCorporateDiscountRbacService hospitalCorporateDiscountRbac;

    @PostMapping("/corporates/{corporateId}/contracts")
    public ResponseEntity<ContractResponse> create(
            @PathVariable("corporateId") UUID corporateId,
            @Valid @RequestBody CreateContractRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalManage(actor, organizationId);
        ContractResponse created = contractService.create(corporateId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/contracts/{id}")
    public ContractResponse getById(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return contractService.getById(id);
    }

    @GetMapping("/corporates/{corporateId}/contracts")
    public PagedResponse<ContractResponse> listByCorporate(
            @PathVariable("corporateId") UUID corporateId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return contractService.listByCorporate(corporateId, status, page, size);
    }

    @PatchMapping("/contracts/{id}")
    public ContractResponse update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateContractRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalManage(actor, organizationId);
        return contractService.update(id, request);
    }
}
