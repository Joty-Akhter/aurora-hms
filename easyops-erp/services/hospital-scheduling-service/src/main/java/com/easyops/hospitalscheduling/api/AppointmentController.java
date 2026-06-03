package com.easyops.hospitalscheduling.api;

import com.easyops.hospitalscheduling.api.dto.*;
import com.easyops.hospitalscheduling.domain.appointment.AppointmentService;
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
@RequestMapping("/api/hospital-scheduling/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final HospitalSchedulingRbacService hospitalSchedulingRbac;

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(
            @Valid @RequestBody CreateAppointmentRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireAppointmentBook(actor, organizationId);
        AppointmentResponse created = appointmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/queue")
    public QueueResponse getQueue(
            @RequestParam("resourceId") UUID resourceId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireAppointmentStaffRead(actor, organizationId);
        return appointmentService.getQueue(resourceId, date);
    }

    @GetMapping("/{id}")
    public AppointmentDetailResponse getById(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireAppointmentStaffRead(actor, organizationId);
        return appointmentService.getById(id);
    }

    @GetMapping
    public PagedResponse<AppointmentResponse> list(
            @RequestParam(value = "patientId", required = false) UUID patientId,
            @RequestParam(value = "resourceId", required = false) UUID resourceId,
            @RequestParam(value = "clinicId", required = false) UUID clinicId,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "appointmentType", required = false) String appointmentType,
            @RequestParam(value = "slotTemplateId", required = false) UUID slotTemplateId,
            @RequestParam(value = "sessionShift", required = false) String sessionShift,
            @RequestParam(value = "bookingChannel", required = false) String bookingChannel,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireAppointmentStaffRead(actor, organizationId);
        return appointmentService.list(patientId, resourceId, clinicId, fromDate, toDate, status, appointmentType,
                slotTemplateId, sessionShift, bookingChannel, page, size);
    }

    @PostMapping("/{id}/reschedule")
    public AppointmentResponse reschedule(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RescheduleAppointmentRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireAppointmentReschedule(actor, organizationId);
        return appointmentService.reschedule(id, request);
    }

    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) CancelAppointmentRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireAppointmentCancel(actor, organizationId);
        CancelAppointmentRequest body = request != null ? request : new CancelAppointmentRequest();
        return appointmentService.cancel(id, body);
    }

    @PostMapping("/{id}/check-in")
    public AppointmentResponse checkIn(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireAppointmentStatusUpdate(actor, organizationId);
        return appointmentService.checkIn(id);
    }

    @PostMapping("/{id}/no-show")
    public AppointmentResponse noShow(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireAppointmentStatusUpdate(actor, organizationId);
        return appointmentService.noShow(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalManage(actor, organizationId);
        appointmentService.delete(id);
    }

    @PostMapping("/{id}/complete")
    public AppointmentResponse complete(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireAppointmentStatusUpdate(actor, organizationId);
        return appointmentService.complete(id);
    }
}
