package com.easyops.hospitalclinicalorders.api;

import com.easyops.hospitalclinicalorders.api.dto.*;
import com.easyops.hospitalclinicalorders.domain.order.ClinicalOrderService;
import com.easyops.hospitalclinicalorders.domain.result.ResultLinkService;
import com.easyops.hospitalclinicalorders.security.HospitalClinicalOrdersRbacService;
import com.easyops.hospitalclinicalorders.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-clinical-orders/orders")
@RequiredArgsConstructor
public class OrderController {

    private final ClinicalOrderService clinicalOrderService;
    private final ResultLinkService resultLinkService;
    private final HospitalClinicalOrdersRbacService hospitalClinicalOrdersRbac;

    @GetMapping("/{id}")
    public ClinicalOrderDetailResponse getById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalClinicalOrdersRbac.requireHospitalView(actor, organizationId);
        return clinicalOrderService.getById(id);
    }

    @GetMapping
    public PagedResponse<ClinicalOrderResponse> list(
            @RequestParam(required = false) UUID facilityId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID visitId,
            @RequestParam(required = false) UUID orderSetId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalClinicalOrdersRbac.requireHospitalView(actor, organizationId);
        return clinicalOrderService.list(facilityId, patientId, visitId, orderSetId, type, status, from, to, page, size);
    }

    @PostMapping("/{id}/cancel")
    public ClinicalOrderResponse cancel(
            @PathVariable UUID id,
            @Valid @RequestBody CancelOrderRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalClinicalOrdersRbac.requireHospitalManage(actor, organizationId);
        return clinicalOrderService.cancel(id, request);
    }

    @PatchMapping("/{id}")
    public ClinicalOrderResponse update(
            @PathVariable UUID id,
            @RequestBody UpdateOrderRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalClinicalOrdersRbac.requireHospitalManage(actor, organizationId);
        return clinicalOrderService.update(id, request);
    }

    @PostMapping("/{orderId}/results")
    public ResponseEntity<ResultLinkResponse> createResultLink(
            @PathVariable UUID orderId,
            @Valid @RequestBody CreateResultLinkRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalClinicalOrdersRbac.requireHospitalManage(actor, organizationId);
        ResultLinkResponse created = resultLinkService.create(orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{orderId}/results")
    public List<ResultLinkResponse> getResultLinks(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalClinicalOrdersRbac.requireHospitalView(actor, organizationId);
        return resultLinkService.findByOrderId(orderId);
    }
}
