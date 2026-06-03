package com.easyops.communication.controller;

import com.easyops.communication.dto.CommunicationDeliveryResponse;
import com.easyops.communication.dto.DeliveryResendRequest;
import com.easyops.communication.dto.InboundCommunicationEvent;
import com.easyops.communication.security.CommunicationTemplateRbacService;
import com.easyops.communication.security.RbacRequestHeaders;
import com.easyops.communication.service.CommunicationDeliveryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@RestController
@RequestMapping("/api/communications")
@Tag(name = "Communication Delivery")
public class CommunicationDeliveryController {

    private final CommunicationDeliveryService deliveryService;
    private final CommunicationTemplateRbacService rbacService;
    private final boolean bypassRbac;

    public CommunicationDeliveryController(
            CommunicationDeliveryService deliveryService,
            CommunicationTemplateRbacService rbacService,
            @Value("${communication.ops.bypass-rbac:false}") boolean bypassRbac
    ) {
        this.deliveryService = deliveryService;
        this.rbacService = rbacService;
        this.bypassRbac = bypassRbac;
    }

    @PostMapping("/events")
    public ResponseEntity<CommunicationDeliveryResponse> ingest(
            @Valid @RequestBody InboundCommunicationEvent request,
            @org.springframework.web.bind.annotation.RequestHeader("X-User-Id") String userIdHeader,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId
    ) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        if (!bypassRbac) {
            rbacService.requireManage(actor, organizationId);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(deliveryService.ingest(request));
    }

    @GetMapping("/deliveries")
    public Page<CommunicationDeliveryResponse> query(
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) String eventId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel,
            Pageable pageable,
            @org.springframework.web.bind.annotation.RequestHeader("X-User-Id") String userIdHeader,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId
    ) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        if (!bypassRbac) {
            rbacService.requireOperationsRead(actor, organizationId);
        }
        return deliveryService.query(correlationId, eventId, status, channel, pageable);
    }

    @PostMapping("/deliveries/{id}/resend")
    public CommunicationDeliveryResponse resend(
            @PathVariable UUID id,
            @RequestBody(required = false) DeliveryResendRequest request,
            @org.springframework.web.bind.annotation.RequestHeader("X-User-Id") String userIdHeader,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId
    ) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        if (!bypassRbac) {
            rbacService.requireOperationsManage(actor, organizationId);
        }
        return deliveryService.resendFailed(id, actor.toString(), request == null ? null : request.reason());
    }
}
