package com.easyops.communication.controller;

import com.easyops.communication.dto.Phase0FoundationResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/communications/foundation")
public class CommunicationFoundationController {

    @GetMapping("/phase-0")
    public Phase0FoundationResponse getPhase0Foundation() {
        return new Phase0FoundationResponse(
                "phase-0",
                "implemented",
                "Kafka (consumer retry + DLQ)",
                "New standalone communication-service with legacy notification-service bridge",
                "Mustache-compatible templates with strict variable schema validation",
                List.of("ADR-001", "ADR-002", "ADR-003"),
                List.of(
                        "eventId",
                        "eventType",
                        "eventVersion",
                        "occurredAt",
                        "organizationId",
                        "entityId",
                        "actorId",
                        "correlationId",
                        "idempotencyKey",
                        "payload"
                ),
                Map.of(
                        "eventTypePattern", "<domain>.<action>.v<major>",
                        "compatibility", "additive payload changes within major version",
                        "requiredHeaders", "eventType,eventVersion,correlationId,organizationId"
                ),
                List.of(
                        "Appointment confirmation (SMS primary, email optional)",
                        "Appointment reschedule/cancellation notification",
                        "Invoice created notification (email primary)"
                ),
                List.of(
                        "Idempotent processing prevents duplicate deliveries",
                        "Delivery status lifecycle is queryable",
                        "Failures are normalized with provider response references",
                        "Audit entries include eventId and correlationId"
                ),
                List.of(
                        "P1: service skeleton, schema baseline, template CRUD, RBAC admin guard",
                        "P2: template rendering/versioning, provider abstraction, Pondit + SMTP adapters",
                        "P3: event consumers, orchestration, retry/DLQ, idempotency controls",
                        "P4: hospital/scheduling/billing integration with staged migration",
                        "P5: consent, preferences, localization",
                        "P6: observability, admin UX, hardening and runbooks"
                ),
                "Indicative v1 timeline: 8-10 weeks"
        );
    }
}
