package com.easyops.hospital.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for extracting and managing DICOM metadata.
 * Handles DICOM tag extraction, metadata storage, and retrieval.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DICOMMetadataService {
    
    /**
     * Extract all DICOM metadata from a file
     */
    public DICOMMetadata extractMetadata(File dicomFile) throws IOException {
        if (dicomFile == null || !dicomFile.isFile() || !dicomFile.canRead()) {
            throw new IOException("DICOM file is missing or not readable");
        }
        try (DicomInputStream dis = new DicomInputStream(dicomFile)) {
            return extractMetadata(readDatasetAfterFileMetaInformation(dis));
        }
    }
    
    /**
     * Extract DICOM metadata from input stream
     */
    public DICOMMetadata extractMetadata(InputStream inputStream) throws IOException {
        try (DicomInputStream dis = new DicomInputStream(inputStream)) {
            return extractMetadata(readDatasetAfterFileMetaInformation(dis));
        }
    }

    /**
     * Part 10 / DICOM File Format: consume file meta information group (0002,xxxx), then read the full dataset.
     */
    private static Attributes readDatasetAfterFileMetaInformation(DicomInputStream dis) throws IOException {
        dis.readFileMetaInformation();
        return dis.readDataset();
    }
    
    /**
     * Extract metadata from DICOM attributes
     */
    public DICOMMetadata extractMetadata(Attributes attributes) {
        DICOMMetadata metadata = new DICOMMetadata();
        
        // Patient Information
        metadata.setPatientId(attributes.getString(Tag.PatientID));
        metadata.setPatientName(attributes.getString(Tag.PatientName));
        metadata.setPatientBirthDate(parseDate(attributes.getString(Tag.PatientBirthDate)));
        metadata.setPatientSex(attributes.getString(Tag.PatientSex));
        
        // Study Information
        metadata.setStudyInstanceUID(attributes.getString(Tag.StudyInstanceUID));
        metadata.setStudyDate(parseDateTime(attributes.getString(Tag.StudyDate), attributes.getString(Tag.StudyTime)));
        metadata.setStudyTime(attributes.getString(Tag.StudyTime));
        metadata.setStudyDescription(attributes.getString(Tag.StudyDescription));
        metadata.setAccessionNumber(attributes.getString(Tag.AccessionNumber));
        metadata.setStudyID(attributes.getString(Tag.StudyID));
        
        // Series Information
        metadata.setSeriesInstanceUID(attributes.getString(Tag.SeriesInstanceUID));
        metadata.setSeriesNumber(attributes.getInt(Tag.SeriesNumber, -1));
        metadata.setSeriesDescription(attributes.getString(Tag.SeriesDescription));
        metadata.setModality(attributes.getString(Tag.Modality));
        metadata.setSeriesDate(parseDateTime(attributes.getString(Tag.SeriesDate), attributes.getString(Tag.SeriesTime)));
        
        // Instance Information
        metadata.setSopInstanceUID(attributes.getString(Tag.SOPInstanceUID));
        metadata.setSopClassUID(attributes.getString(Tag.SOPClassUID));
        metadata.setInstanceNumber(attributes.getInt(Tag.InstanceNumber, -1));
        
        // Image Information
        metadata.setRows(attributes.getInt(Tag.Rows, -1));
        metadata.setColumns(attributes.getInt(Tag.Columns, -1));
        metadata.setBitsAllocated(attributes.getInt(Tag.BitsAllocated, -1));
        metadata.setBitsStored(attributes.getInt(Tag.BitsStored, -1));
        metadata.setHighBit(attributes.getInt(Tag.HighBit, -1));
        metadata.setSamplesPerPixel(attributes.getInt(Tag.SamplesPerPixel, -1));
        metadata.setPhotometricInterpretation(attributes.getString(Tag.PhotometricInterpretation));
        metadata.setPixelSpacing(attributes.getDoubles(Tag.PixelSpacing));
        metadata.setSliceThickness(attributes.getDouble(Tag.SliceThickness, -1));
        metadata.setWindowCenter(attributes.getDouble(Tag.WindowCenter, -1));
        metadata.setWindowWidth(attributes.getDouble(Tag.WindowWidth, -1));
        metadata.setNumberOfFrames(attributes.getInt(Tag.NumberOfFrames, 1));
        
        // Equipment Information
        metadata.setManufacturer(attributes.getString(Tag.Manufacturer));
        metadata.setManufacturerModelName(attributes.getString(Tag.ManufacturerModelName));
        metadata.setDeviceSerialNumber(attributes.getString(Tag.DeviceSerialNumber));
        metadata.setSoftwareVersions(attributes.getString(Tag.SoftwareVersions));
        
        // Acquisition Information
        metadata.setAcquisitionDate(parseDateTime(attributes.getString(Tag.AcquisitionDate), attributes.getString(Tag.AcquisitionTime)));
        metadata.setKvp(attributes.getDouble(Tag.KVP, -1));
        metadata.setExposureTime(attributes.getDouble(Tag.ExposureTime, -1));
        metadata.setXRayTubeCurrent(attributes.getInt(Tag.XRayTubeCurrent, -1));
        
        // All attributes as map
        metadata.setAllAttributes(attributesToMap(attributes));
        
        return metadata;
    }
    
    /**
     * Get specific DICOM tag value
     */
    public String getTagValue(File dicomFile, int tag) throws IOException {
        if (dicomFile == null || !dicomFile.isFile() || !dicomFile.canRead()) {
            throw new IOException("DICOM file is missing or not readable");
        }
        try (DicomInputStream dis = new DicomInputStream(dicomFile)) {
            Attributes attributes = readDatasetAfterFileMetaInformation(dis);
            return attributes.getString(tag);
        }
    }
    
    /**
     * Update DICOM metadata (creates new file with updated attributes)
     */
    public void updateMetadata(File sourceFile, File targetFile, Map<Integer, String> tagUpdates) throws IOException {
        try (DicomInputStream dis = new DicomInputStream(sourceFile);
             org.dcm4che3.io.DicomOutputStream dos = new org.dcm4che3.io.DicomOutputStream(targetFile)) {

            Attributes fmi = dis.readFileMetaInformation();
            Attributes attributes = dis.readDataset();

            // Update specified tags (assume string VR for simplicity)
            for (Map.Entry<Integer, String> entry : tagUpdates.entrySet()) {
                attributes.setString(entry.getKey(), VR.LO, entry.getValue());
            }

            Attributes metaOut =
                fmi != null && !fmi.isEmpty() ? fmi : attributes.createFileMetaInformation(UID.ExplicitVRLittleEndian);
            dos.writeDataset(metaOut, attributes);
        }
    }
    
    /**
     * Convert attributes to map
     */
    private Map<String, Object> attributesToMap(Attributes attributes) {
        Map<String, Object> map = new HashMap<>();

        for (int tag : attributes.tags()) {
            VR vr = attributes.getVR(tag);
            Object value;

            switch (vr) {
                case AE:
                case AS:
                case AT:
                case CS:
                case DA:
                case DS:
                case DT:
                case IS:
                case LO:
                case LT:
                case SH:
                case ST:
                case TM:
                case UI:
                case UT:
                    value = attributes.getString(tag);
                    break;
                case SL:
                case SS:
                case UL:
                case US:
                    value = attributes.getInt(tag, -1);
                    break;
                case FD:
                case FL:
                    value = attributes.getDouble(tag, -1);
                    break;
                default:
                    value = attributes.getString(tag, null);
            }

            String tagName = String.format("0x%08X", tag);
            map.put(tagName, value);
        }

        return map;
    }
    
    /**
     * Parse DICOM date (YYYYMMDD)
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.length() != 8) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateStr);
            return null;
        }
    }
    
    /**
     * Parse DICOM date and time
     */
    private LocalDateTime parseDateTime(String dateStr, String timeStr) {
        if (dateStr == null) {
            return null;
        }
        try {
            String dateTimeStr = dateStr;
            if (timeStr != null && timeStr.length() >= 6) {
                dateTimeStr += "T" + timeStr.substring(0, Math.min(6, timeStr.length()));
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (Exception e) {
            log.warn("Failed to parse date/time: {} / {}", dateStr, timeStr);
            return null;
        }
    }
    
    /**
     * DICOM Metadata DTO
     */
    @Data
    public static class DICOMMetadata {
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
        
        // All attributes
        private Map<String, Object> allAttributes;
    }
}
