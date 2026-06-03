package com.easyops.hospitalscheduling.domain.audit;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scheduling_audit_log", schema = "hospital_scheduling")
public class AuditLog {

    @Id
    private UUID id;

    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_role", length = 100)
    private String actorRole;

    @Column(name = "booking_channel", length = 30)
    private String bookingChannel;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    @Column(name = "before_state", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String beforeState;

    @Column(name = "after_state", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String afterState;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

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
