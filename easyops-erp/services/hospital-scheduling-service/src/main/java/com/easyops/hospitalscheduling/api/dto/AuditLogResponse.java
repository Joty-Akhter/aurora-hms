package com.easyops.hospitalscheduling.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AuditLogResponse {
    private UUID id;
    private String entityType;
    private UUID entityId;
    private String action;
    private UUID actorId;
    private String actorRole;
    private String bookingChannel;
    private String reason;
    private String correlationId;
    private String beforeState;
    private String afterState;
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }
    public String getBookingChannel() { return bookingChannel; }
    public void setBookingChannel(String bookingChannel) { this.bookingChannel = bookingChannel; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getBeforeState() { return beforeState; }
    public void setBeforeState(String beforeState) { this.beforeState = beforeState; }
    public String getAfterState() { return afterState; }
    public void setAfterState(String afterState) { this.afterState = afterState; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
