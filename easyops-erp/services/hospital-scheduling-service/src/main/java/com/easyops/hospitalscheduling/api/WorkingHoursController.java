package com.easyops.hospitalscheduling.api;

import com.easyops.hospitalscheduling.api.dto.WorkingHoursResponse;
import com.easyops.hospitalscheduling.api.dto.SetWorkingHoursRequest;
import com.easyops.hospitalscheduling.domain.resource.WorkingHoursService;
import com.easyops.hospitalscheduling.security.HospitalSchedulingRbacService;
import com.easyops.hospitalscheduling.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-scheduling/resources")
@RequiredArgsConstructor
public class WorkingHoursController {

    private final WorkingHoursService workingHoursService;
    private final HospitalSchedulingRbacService hospitalSchedulingRbac;

    @PostMapping("/{id}/working-hours")
    public ResponseEntity<List<WorkingHoursResponse>> setWorkingHours(
            @PathVariable("id") UUID resourceId,
            @Valid @RequestBody SetWorkingHoursRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireDoctorScheduleManage(actor, organizationId);
        List<WorkingHoursResponse> updated = workingHoursService.setWorkingHours(resourceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }

    @GetMapping("/{id}/working-hours")
    public List<WorkingHoursResponse> getWorkingHours(
            @PathVariable("id") UUID resourceId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalView(actor, organizationId);
        return workingHoursService.getWorkingHours(resourceId);
    }
}
