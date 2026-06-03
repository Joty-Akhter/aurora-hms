package com.easyops.hospitalscheduling.api;

import com.easyops.hospitalscheduling.api.dto.AvailabilityResponse;
import com.easyops.hospitalscheduling.config.SchedulingMetrics;
import com.easyops.hospitalscheduling.domain.resource.AvailabilityService;
import com.easyops.hospitalscheduling.security.HospitalSchedulingRbacService;
import com.easyops.hospitalscheduling.security.RbacRequestHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-scheduling")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final SchedulingMetrics schedulingMetrics;
    private final HospitalSchedulingRbacService hospitalSchedulingRbac;

    @GetMapping("/availability")
    public List<AvailabilityResponse> getAvailability(
            @RequestParam("resourceId") UUID resourceId,
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "slotTemplateId", required = false) UUID slotTemplateId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireAppointmentStaffRead(actor, organizationId);
        schedulingMetrics.incrementAvailabilityRequests();
        return availabilityService.getAvailability(resourceId, fromDate, toDate, slotTemplateId);
    }
}
