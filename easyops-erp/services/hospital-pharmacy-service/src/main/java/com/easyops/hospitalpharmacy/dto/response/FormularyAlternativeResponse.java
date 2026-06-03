package com.easyops.hospitalpharmacy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class FormularyAlternativeResponse {

    private UUID id;
    private UUID alternativeDrugId;
    private String alternativeDrugGenericName;
    private String alternativeDrugBrandName;
    private int priority;
    private String equivalenceClass;
}
