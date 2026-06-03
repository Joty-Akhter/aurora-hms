package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for DICOM metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DICOMMetadataResponse {
    
    // Patient Information
    private String patientId;
    private String patientName;
    private LocalDate patientBirthDate;
    private String patientSex;
    
    // Study Information
    private String studyInstanceUID;
    private LocalDateTime studyDate;
    private String studyDescription;
    private String accessionNumber;
    private String studyID;
    private String studyTime;
    
    // Series Information
    private String seriesInstanceUID;
    private Integer seriesNumber;
    private String seriesDescription;
    private String modality;
    private LocalDateTime seriesDate;
    
    // Instance Information
    private String sopInstanceUID;
    private String sopClassUID;
    private Integer instanceNumber;
    
    // Image Information
    private Integer rows;
    private Integer columns;
    private Integer imageWidth;
    private Integer imageHeight;
    private Integer bitsAllocated;
    private Integer bitsStored;
    private Integer highBit;
    private Integer samplesPerPixel;
    private String photometricInterpretation;
    private double[] pixelSpacing;
    private Double sliceThickness;
    private Double windowCenter;
    private Double windowWidth;
    private Integer numberOfFrames;
    
    // Equipment Information
    private String manufacturer;
    private String manufacturerModelName;
    private String deviceSerialNumber;
    private String softwareVersions;
    
    // Acquisition Information
    private LocalDateTime acquisitionDate;
    private Double kvp;
    private Double exposureTime;
    private Integer xRayTubeCurrent;
}
