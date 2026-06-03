package com.easyops.hospitalscheduling.api;

import com.easyops.hospitalscheduling.api.dto.BlackoutResponse;
import com.easyops.hospitalscheduling.api.dto.CreateBlackoutRequest;
import com.easyops.hospitalscheduling.api.dto.PagedResponse;
import com.easyops.hospitalscheduling.domain.resource.BlackoutService;
import com.easyops.hospitalscheduling.security.HospitalSchedulingRbacService;
import com.easyops.hospitalscheduling.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-scheduling/blackouts")
@RequiredArgsConstructor
public class BlackoutController {

    private final BlackoutService blackoutService;
    private final HospitalSchedulingRbacService hospitalSchedulingRbac;

    @PostMapping
    public ResponseEntity<BlackoutResponse> create(
            @Valid @RequestBody CreateBlackoutRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireDoctorScheduleManage(actor, organizationId);
        BlackoutResponse created = blackoutService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public PagedResponse<BlackoutResponse> list(
            @RequestParam(value = "resourceId", required = false) UUID resourceId,
            @RequestParam(value = "branchId", required = false) UUID branchId,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalView(actor, organizationId);
        return blackoutService.list(resourceId, branchId, fromDate, toDate, page, size);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireDoctorScheduleManage(actor, organizationId);
        blackoutService.deleteById(id);
    }
}
