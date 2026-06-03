package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.PharmacyDirectory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * FR-P3.5: Pharmacy directory entry response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyDirectoryResponse {

    private UUID id;
    private String name;
    private String npi;
    private String ncpdpId;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zip;
    private String country;
    private String phone;
    private String fax;
    private String email;
    private Boolean isEprescribingCapable;
    private String eprescribingNetwork;
    private PharmacyDirectory.DataSource dataSource;
    private OffsetDateTime lastVerifiedAt;
    private String verificationNotes;
    private Boolean isActive;
    private String notes;

    /** TRUE when lastVerifiedAt is null or older than 90 days (staleness flag). */
    private Boolean isStale;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}
