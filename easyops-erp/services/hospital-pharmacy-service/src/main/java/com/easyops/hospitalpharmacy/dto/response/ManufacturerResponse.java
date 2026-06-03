package com.easyops.hospitalpharmacy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ManufacturerResponse {

    private UUID id;
    private String name;
    private String shortCode;
    private String country;
    private String contactInfo;
    private boolean active;
    private String licenseNo;
    private String vat;
    private BigDecimal commission;
    private String type;
    private String email;
    private String phone;
    private String address;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

