package com.easyops.hospitalcorporatediscount.api;

import com.easyops.hospitalcorporatediscount.api.dto.CoverageRuleResponse;
import com.easyops.hospitalcorporatediscount.api.dto.CreateCoverageRuleRequest;
import com.easyops.hospitalcorporatediscount.domain.coverage.CoverageRuleService;
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
public class CoverageRuleController {

    private final CoverageRuleService coverageRuleService;
    private final HospitalCorporateDiscountRbacService hospitalCorporateDiscountRbac;

    @PostMapping("/contracts/{contractId}/coverage-rules")
    public ResponseEntity<CoverageRuleResponse> create(
            @PathVariable("contractId") UUID contractId,
            @Valid @RequestBody CreateCoverageRuleRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalManage(actor, organizationId);
        CoverageRuleResponse created = coverageRuleService.create(contractId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/contracts/{contractId}/coverage-rules")
    public List<CoverageRuleResponse> listByContractId(
            @PathVariable("contractId") UUID contractId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return coverageRuleService.listByContractId(contractId);
    }

    @DeleteMapping("/coverage-rules/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalManage(actor, organizationId);
        coverageRuleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
