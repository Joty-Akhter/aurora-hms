package com.easyops.hospitalpharmacy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ManufacturerRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 50)
    private String shortCode;

    @Size(max = 100)
    private String country;

    private String contactInfo;

    private Boolean active;

    @Size(max = 100)
    private String licenseNo;

    @Size(max = 50)
    private String vat;

    private BigDecimal commission;

    @Size(max = 50)
    private String type;

    @Size(max = 255)
    private String email;

    @Size(max = 50)
    private String phone;

    private String address;
}

