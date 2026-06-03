package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.PharmacyDirectory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FR-P3.5: Create or update a pharmacy directory entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyDirectoryRequest {

    @NotBlank(message = "Pharmacy name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;

    /** 10-digit CMS Organisation NPI. Must be numeric and exactly 10 digits when present. */
    @Pattern(regexp = "^\\d{10}$", message = "NPI must be exactly 10 digits")
    private String npi;

    /** NCPDP/Surescripts pharmacy ID (up to 7 alphanumeric characters). */
    @Size(max = 15, message = "NCPDP ID must be at most 15 characters")
    private String ncpdpId;

    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @Size(max = 100)
    private String city;

    @Size(max = 50)
    private String state;

    @Size(max = 20)
    private String zip;

    @Size(max = 100)
    private String country;

    @Size(max = 30)
    private String phone;

    @Size(max = 30)
    private String fax;

    @Size(max = 255)
    private String email;

    private Boolean isEprescribingCapable;

    @Size(max = 100)
    private String eprescribingNetwork;

    private PharmacyDirectory.DataSource dataSource;

    private String verificationNotes;

    private String notes;
}
