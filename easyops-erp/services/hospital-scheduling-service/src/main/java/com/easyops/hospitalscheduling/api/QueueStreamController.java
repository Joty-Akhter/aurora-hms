package com.easyops.hospitalscheduling.api;

import com.easyops.hospitalscheduling.api.dto.QueueResponse;
import com.easyops.hospitalscheduling.domain.appointment.AppointmentService;
import com.easyops.hospitalscheduling.domain.appointment.QueueEventBroadcaster;
import com.easyops.hospitalscheduling.security.HospitalSchedulingRbacService;
import com.easyops.hospitalscheduling.security.RbacRequestHeaders;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-scheduling/appointments/queue")
public class QueueStreamController {

    private static final Logger log = LoggerFactory.getLogger(QueueStreamController.class);
    private final QueueEventBroadcaster broadcaster;
    private final AppointmentService appointmentService;
    private final HospitalSchedulingRbacService rbac;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public QueueStreamController(QueueEventBroadcaster broadcaster, AppointmentService appointmentService,
                                  HospitalSchedulingRbacService rbac) {
        this.broadcaster = broadcaster;
        this.appointmentService = appointmentService;
        this.rbac = rbac;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamQueue(
            @RequestParam UUID resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID orgId) {

        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        rbac.requireAppointmentStaffRead(actor, orgId);

        SseEmitter emitter = broadcaster.subscribe(resourceId, date);

        // Send current snapshot as first event
        try {
            QueueResponse snapshot = appointmentService.getQueue(resourceId, date);
            String json = mapper.writeValueAsString(snapshot);
            emitter.send(SseEmitter.event().name("SNAPSHOT").data(json));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize queue snapshot", e);
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }
}
