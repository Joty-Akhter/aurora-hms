package com.easyops.hospitalclinicalorders.domain.result;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "result_links", schema = "hospital_clinical_orders")
@EntityListeners(AuditingEntityListener.class)
public class ResultLink {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "system_type", nullable = false, length = 30)
    private String systemType;

    @Column(name = "external_system_id", length = 255)
    private String externalSystemId;

    @Column(name = "viewer_url", length = 1000)
    private String viewerUrl;

    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "revised_at")
    private OffsetDateTime revisedAt;

    @CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public String getSystemType() { return systemType; }
    public void setSystemType(String systemType) { this.systemType = systemType; }
    public String getExternalSystemId() { return externalSystemId; }
    public void setExternalSystemId(String externalSystemId) { this.externalSystemId = externalSystemId; }
    public String getViewerUrl() { return viewerUrl; }
    public void setViewerUrl(String viewerUrl) { this.viewerUrl = viewerUrl; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public OffsetDateTime getRevisedAt() { return revisedAt; }
    public void setRevisedAt(OffsetDateTime revisedAt) { this.revisedAt = revisedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
