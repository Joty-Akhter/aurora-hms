package com.easyops.hospitalcorporatediscount.api;

import com.easyops.hospitalcorporatediscount.api.dto.CorporateTariffResponse;
import com.easyops.hospitalcorporatediscount.api.dto.CreateCorporateTariffRequest;
import com.easyops.hospitalcorporatediscount.domain.tariff.CorporateTariffService;
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
public class CorporateTariffController {

    private final CorporateTariffService corporateTariffService;
    private final HospitalCorporateDiscountRbacService hospitalCorporateDiscountRbac;

    @PostMapping("/contracts/{contractId}/tariffs")
    public ResponseEntity<CorporateTariffResponse> create(
            @PathVariable("contractId") UUID contractId,
            @Valid @RequestBody CreateCorporateTariffRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalManage(actor, organizationId);
        CorporateTariffResponse created = corporateTariffService.create(contractId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/contracts/{contractId}/tariffs")
    public List<CorporateTariffResponse> listByContractId(
            @PathVariable("contractId") UUID contractId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return corporateTariffService.listByContractId(contractId);
    }

    @DeleteMapping("/tariffs/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalManage(actor, organizationId);
        corporateTariffService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
