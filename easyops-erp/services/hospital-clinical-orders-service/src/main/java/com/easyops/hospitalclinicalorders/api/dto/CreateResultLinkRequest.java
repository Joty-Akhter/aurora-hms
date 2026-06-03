package com.easyops.hospitalclinicalorders.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;

public class CreateResultLinkRequest {

    @NotBlank(message = "systemType is required (LIS, RIS, PACS, INTERNAL)")
    private String systemType;

    private String externalSystemId;
    private String viewerUrl;
    private Integer version;
    private OffsetDateTime revisedAt;

    /** Optional: when FINAL, order result_status and result_available_at are updated. */
    private String resultStatus;

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
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
}
