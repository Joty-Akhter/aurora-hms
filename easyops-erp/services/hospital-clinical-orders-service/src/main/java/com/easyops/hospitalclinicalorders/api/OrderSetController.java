package com.easyops.hospitalclinicalorders.api;

import com.easyops.hospitalclinicalorders.api.dto.*;
import com.easyops.hospitalclinicalorders.domain.orderset.OrderSetService;
import com.easyops.hospitalclinicalorders.security.HospitalClinicalOrdersRbacService;
import com.easyops.hospitalclinicalorders.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-clinical-orders/order-sets")
@RequiredArgsConstructor
public class OrderSetController {

    private final OrderSetService orderSetService;
    private final HospitalClinicalOrdersRbacService hospitalClinicalOrdersRbac;

    @PostMapping
    public ResponseEntity<OrderSetResponse> create(
            @Valid @RequestBody CreateOrderSetRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalClinicalOrdersRbac.requireHospitalManage(actor, organizationId);
        OrderSetResponse created = orderSetService.create(request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Create a new order set by copying an existing one (repeat order). Same patient, visit, doctor/department and order lines; all orders REQUESTED. */
    @PostMapping("/from-order-set")
    public ResponseEntity<OrderSetResponse> copyFromOrderSet(
            @Valid @RequestBody CopyOrderSetRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalClinicalOrdersRbac.requireHospitalManage(actor, organizationId);
        OrderSetResponse created = orderSetService.copyFromOrderSet(request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public OrderSetDetailResponse getById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalClinicalOrdersRbac.requireHospitalView(actor, organizationId);
        return orderSetService.getById(id);
    }

    @GetMapping
    public PagedResponse<OrderSetResponse> list(
            @RequestParam(required = false) UUID facilityId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID visitId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalClinicalOrdersRbac.requireHospitalView(actor, organizationId);
        return orderSetService.list(facilityId, patientId, visitId, from, to, page, size);
    }
}
