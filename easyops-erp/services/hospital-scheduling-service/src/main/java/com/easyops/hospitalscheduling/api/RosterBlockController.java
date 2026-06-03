package com.easyops.hospitalscheduling.api;

import com.easyops.hospitalscheduling.api.dto.CreateRosterBlockRequest;
import com.easyops.hospitalscheduling.api.dto.RosterBlockResponse;
import com.easyops.hospitalscheduling.domain.roster.RosterBlockService;
import com.easyops.hospitalscheduling.security.HospitalSchedulingRbacService;
import com.easyops.hospitalscheduling.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-scheduling")
@RequiredArgsConstructor
public class RosterBlockController {

    private final RosterBlockService rosterBlockService;
    private final HospitalSchedulingRbacService hospitalSchedulingRbac;

    @PostMapping("/resources/{resourceId}/roster-blocks")
    public ResponseEntity<RosterBlockResponse> create(
            @PathVariable("resourceId") UUID resourceId,
            @Valid @RequestBody CreateRosterBlockRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireDoctorScheduleManage(actor, organizationId);
        RosterBlockResponse created = rosterBlockService.create(resourceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/resources/{resourceId}/roster-blocks")
    public List<RosterBlockResponse> listByResource(
            @PathVariable("resourceId") UUID resourceId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalView(actor, organizationId);
        return rosterBlockService.listByResourceId(resourceId, from, to);
    }

    @DeleteMapping("/roster-blocks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireDoctorScheduleManage(actor, organizationId);
        rosterBlockService.deleteById(id);
    }
}
