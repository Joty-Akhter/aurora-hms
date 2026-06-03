package com.easyops.hospitalbilling.api;

import com.easyops.hospitalbilling.api.dto.ChargeResponse;
import com.easyops.hospitalbilling.api.dto.CreateChargeBatchRequest;
import com.easyops.hospitalbilling.api.dto.PagedResponse;
import com.easyops.hospitalbilling.domain.charge.ChargeService;
import com.easyops.hospitalbilling.security.HospitalBillingRbacService;
import com.easyops.hospitalbilling.security.RbacRequestHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-billing/charges")
@RequiredArgsConstructor
public class ChargeController {

    private final ChargeService chargeService;
    private final HospitalBillingRbacService hospitalBillingRbac;

    @PostMapping
    public ResponseEntity<List<ChargeResponse>> createCharges(
            @Valid @RequestBody CreateChargeBatchRequest batchRequest,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireChargePostOrHospitalManage(actor, organizationId);
        if (batchRequest == null || batchRequest.getCharges() == null || batchRequest.getCharges().isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
        List<ChargeResponse> created = chargeService.createCharges(batchRequest.getCharges());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ChargeResponse getCharge(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireHospitalView(actor, organizationId);
        return chargeService.getCharge(id);
    }

    @GetMapping
    public PagedResponse<ChargeResponse> listCharges(
            @RequestParam(value = "patientId", required = false) UUID patientId,
            @RequestParam(value = "visitId", required = false) UUID visitId,
            @RequestParam(value = "status", required = false) List<String> statuses,
            @RequestParam(value = "sourceService", required = false) String sourceService,
            @RequestParam(value = "createdFrom", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.OffsetDateTime createdFrom,
            @RequestParam(value = "createdTo", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.OffsetDateTime createdTo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalBillingRbac.requireHospitalView(actor, organizationId);
        return chargeService.listCharges(patientId, visitId, statuses, sourceService, createdFrom, createdTo, page, size);
    }
}
