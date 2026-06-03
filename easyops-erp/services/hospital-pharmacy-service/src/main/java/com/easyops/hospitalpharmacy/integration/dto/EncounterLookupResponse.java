package com.easyops.hospitalpharmacy.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

/** Subset of hospital-service encounter JSON for internal RestTemplate lookups. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EncounterLookupResponse {
    private UUID encounterId;
}
