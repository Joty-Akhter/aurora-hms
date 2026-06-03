package com.easyops.hospitalpharmacy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PharmacyLocationRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    @Size(max = 50)
    private String type; // OPD, IPD, store, ward_store

    @Size(max = 50)
    private String workflowType; // SUPPLIER, CENTRAL_STORE, OUTLET_PHARMACY

    private Boolean is24x7;

    private String operationalHours;

    private Boolean active;
}

