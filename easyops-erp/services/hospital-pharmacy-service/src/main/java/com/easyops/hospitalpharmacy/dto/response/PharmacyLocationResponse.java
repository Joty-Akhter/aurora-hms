package com.easyops.hospitalpharmacy.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class PharmacyLocationResponse {

    private UUID id;
    private String name;
    private String type;
    private String workflowType;

    @JsonProperty("is24x7")
    private boolean is24x7;
    private String operationalHours;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

