package com.easyops.hospitalpharmacy.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class FormularyAlternativeRequest {

    private UUID alternativeDrugId;

    private int priority;

    private String equivalenceClass;
}
