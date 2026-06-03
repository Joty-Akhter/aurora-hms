package com.easyops.hospitalclinicalorders.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ResultLinkResponse {
    private UUID id;
    private UUID orderId;
    private String systemType;
    private String externalSystemId;
    private String viewerUrl;
    private Integer version;
    private OffsetDateTime revisedAt;
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
