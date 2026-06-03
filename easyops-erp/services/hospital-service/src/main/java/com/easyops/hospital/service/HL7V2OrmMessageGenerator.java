package com.easyops.hospital.service;

import com.easyops.hospital.entity.ImagingOrder;
import com.easyops.hospital.entity.LabOrder;
import com.easyops.hospital.entity.Patient;
import com.easyops.hospital.util.PatientNameInterop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service for generating HL7 V2 ORM (Order Message) messages for laboratory orders.
 * HL7 V2 ORM messages are used to transmit orders to Laboratory Information Systems (LIS).
 */
@Service
@Slf4j
public class HL7V2OrmMessageGenerator {
    
    private static final String FIELD_SEPARATOR = "|";
    private static final String COMPONENT_SEPARATOR = "^";
    private static final String REPETITION_SEPARATOR = "~";
    private static final String ESCAPE_CHARACTER = "\\";
    private static final String SUBCOMPONENT_SEPARATOR = "&";
    
    private static final DateTimeFormatter HL7_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HL7_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    /**
     * Generate HL7 V2 ORM message for a lab order
     * 
     * @param labOrder The lab order to generate message for
     * @return HL7 V2 ORM message string
     */
    public String generateOrmMessage(LabOrder labOrder) {
        Patient patient = labOrder.getPatient();
        
        StringBuilder message = new StringBuilder();
        
        // MSH - Message Header Segment
        message.append(buildMSHSegment(labOrder));
        message.append("\r");
        
        // PID - Patient Identification Segment
        message.append(buildPIDSegment(patient));
        message.append("\r");
        
        // ORC - Common Order Segment
        message.append(buildORCSegment(labOrder));
        message.append("\r");
        
        // OBR - Observation Request Segment
        message.append(buildOBRSegment(labOrder));
        message.append("\r");
        
        return message.toString();
    }
    
    /**
     * Build MSH (Message Header) segment
     * MSH|^~\&|EHR|FACILITY|LIS|LAB|20240101120000||ORM^O01|MSG001|P|2.5
     */
    private String buildMSHSegment(LabOrder labOrder) {
        StringBuilder msh = new StringBuilder("MSH");
        msh.append(FIELD_SEPARATOR).append("^~\\&"); // Encoding characters
        msh.append(FIELD_SEPARATOR).append("EHR"); // Sending Application
        msh.append(FIELD_SEPARATOR).append(labOrder.getOrderingFacilityName() != null ? 
            escapeField(labOrder.getOrderingFacilityName()) : "FACILITY"); // Sending Facility
        msh.append(FIELD_SEPARATOR).append("LIS"); // Receiving Application
        msh.append(FIELD_SEPARATOR).append(labOrder.getLaboratoryName() != null ? 
            escapeField(labOrder.getLaboratoryName()) : "LAB"); // Receiving Facility
        msh.append(FIELD_SEPARATOR).append(formatDateTime(LocalDateTime.now())); // Date/Time of Message
        msh.append(FIELD_SEPARATOR); // Security (empty)
        msh.append(FIELD_SEPARATOR).append("ORM^O01"); // Message Type (ORM^O01 = Order Message)
        msh.append(FIELD_SEPARATOR).append(labOrder.getOrderNumber()); // Message Control ID
        msh.append(FIELD_SEPARATOR).append("P"); // Processing ID (P = Production, T = Test)
        msh.append(FIELD_SEPARATOR).append("2.5"); // Version ID (HL7 Version 2.5)
        msh.append(FIELD_SEPARATOR); // Sequence Number (empty)
        msh.append(FIELD_SEPARATOR); // Continuation Pointer (empty)
        msh.append(FIELD_SEPARATOR); // Accept Acknowledgment Type (empty)
        msh.append(FIELD_SEPARATOR); // Application Acknowledgment Type (empty)
        msh.append(FIELD_SEPARATOR); // Country Code (empty)
        msh.append(FIELD_SEPARATOR); // Character Set (empty)
        msh.append(FIELD_SEPARATOR); // Principal Language of Message (empty)
        
        return msh.toString();
    }
    
    /**
     * Build PID (Patient Identification) segment
     * PID|1||MRN123^^^HOSPITAL^MR||DOE^JOHN^MIDDLE||19800101|M|||123 MAIN ST^^CITY^ST^12345|||(555)123-4567|||S||123456789|987654321
     */
    private String buildPIDSegment(Patient patient) {
        StringBuilder pid = new StringBuilder("PID");
        pid.append(FIELD_SEPARATOR).append("1"); // Set ID
        pid.append(FIELD_SEPARATOR); // Patient ID (External ID) - empty, using internal ID
        pid.append(FIELD_SEPARATOR).append(patient.getMrn() != null ? 
            patient.getMrn() + "^^^HOSPITAL^MR" : ""); // Patient Identifier List
        pid.append(FIELD_SEPARATOR); // Alternate Patient ID - empty
        pid.append(FIELD_SEPARATOR).append(buildName(patient)); // Patient Name
        pid.append(FIELD_SEPARATOR); // Mother's Maiden Name - empty
        pid.append(FIELD_SEPARATOR).append(formatDate(patient.getDateOfBirth())); // Date/Time of Birth
        pid.append(FIELD_SEPARATOR).append(patient.getGender() != null ? 
            patient.getGender().name().substring(0, 1).toUpperCase() : ""); // Administrative Sex
        pid.append(FIELD_SEPARATOR); // Patient Alias - empty
        pid.append(FIELD_SEPARATOR); // Race - empty
        pid.append(FIELD_SEPARATOR).append(buildAddress(patient)); // Patient Address
        pid.append(FIELD_SEPARATOR); // County Code - empty
        pid.append(FIELD_SEPARATOR).append(patient.getPrimaryPhone() != null ? 
            formatPhone(patient.getPrimaryPhone()) : ""); // Phone Number - Home
        pid.append(FIELD_SEPARATOR); // Phone Number - Business - empty
        pid.append(FIELD_SEPARATOR); // Primary Language - empty
        pid.append(FIELD_SEPARATOR); // Marital Status - empty
        pid.append(FIELD_SEPARATOR); // Religion - empty
        pid.append(FIELD_SEPARATOR); // Patient Account Number - empty
        pid.append(FIELD_SEPARATOR); // SSN Number - empty
        pid.append(FIELD_SEPARATOR); // Driver's License Number - empty
        pid.append(FIELD_SEPARATOR); // Mother's Identifier - empty
        pid.append(FIELD_SEPARATOR); // Ethnic Group - empty
        
        return pid.toString();
    }
    
    /**
     * Build ORC (Common Order) segment
     * ORC|NW|ORD123|||^^^20240101120000^^R|12345^PROVIDER^NAME|||20240101120000|PROVIDER^NAME^MD
     */
    private String buildORCSegment(LabOrder labOrder) {
        StringBuilder orc = new StringBuilder("ORC");
        orc.append(FIELD_SEPARATOR).append("NW"); // Order Control (NW = New Order)
        orc.append(FIELD_SEPARATOR).append(labOrder.getOrderNumber()); // Placer Order Number
        orc.append(FIELD_SEPARATOR); // Filler Order Number - empty
        orc.append(FIELD_SEPARATOR); // Placer Group Number - empty
        orc.append(FIELD_SEPARATOR); // Order Status - empty
        orc.append(FIELD_SEPARATOR).append(buildOrderResponseFlag(labOrder)); // Response Flag
        orc.append(FIELD_SEPARATOR); // Quantity/Timing - empty
        orc.append(FIELD_SEPARATOR).append(buildOrderingProvider(labOrder)); // Parent Order
        orc.append(FIELD_SEPARATOR); // Date/Time of Transaction - empty
        orc.append(FIELD_SEPARATOR).append(formatDateTime(labOrder.getOrderDate())); // Entered By
        orc.append(FIELD_SEPARATOR).append(buildOrderingProvider(labOrder)); // Verified By
        orc.append(FIELD_SEPARATOR); // Ordering Provider - empty (in OBR)
        orc.append(FIELD_SEPARATOR); // Enterer's Location - empty
        orc.append(FIELD_SEPARATOR); // Call Back Phone Number - empty
        orc.append(FIELD_SEPARATOR); // Order Effective Date/Time - empty
        orc.append(FIELD_SEPARATOR); // Order Control Code Reason - empty
        orc.append(FIELD_SEPARATOR); // Entering Organization - empty
        orc.append(FIELD_SEPARATOR); // Entering Device - empty
        orc.append(FIELD_SEPARATOR); // Action By - empty
        orc.append(FIELD_SEPARATOR); // Advanced Beneficiary Notice Code - empty
        orc.append(FIELD_SEPARATOR); // Ordering Facility Name - empty
        orc.append(FIELD_SEPARATOR); // Ordering Facility Address - empty
        orc.append(FIELD_SEPARATOR); // Ordering Facility Phone Number - empty
        orc.append(FIELD_SEPARATOR); // Ordering Provider Address - empty
        orc.append(FIELD_SEPARATOR); // Order Status Modifier - empty
        
        return orc.toString();
    }
    
    /**
     * Build OBR (Observation Request) segment
     * OBR|1|ORD123||12345-6^COMPLETE BLOOD COUNT^LN|||20240101120000|||||||||PROVIDER^NAME^MD|||||||||F||||||
     */
    private String buildOBRSegment(LabOrder labOrder) {
        StringBuilder obr = new StringBuilder("OBR");
        obr.append(FIELD_SEPARATOR).append("1"); // Set ID
        obr.append(FIELD_SEPARATOR).append(labOrder.getOrderNumber()); // Placer Order Number
        obr.append(FIELD_SEPARATOR); // Filler Order Number - empty
        obr.append(FIELD_SEPARATOR).append(buildUniversalServiceIdentifier(labOrder)); // Universal Service Identifier
        obr.append(FIELD_SEPARATOR); // Priority - empty (in ORC)
        obr.append(FIELD_SEPARATOR).append(formatDateTime(labOrder.getOrderDate())); // Requested Date/Time
        obr.append(FIELD_SEPARATOR); // Observation Date/Time - empty
        obr.append(FIELD_SEPARATOR); // Observation End Date/Time - empty
        obr.append(FIELD_SEPARATOR); // Collection Volume - empty
        obr.append(FIELD_SEPARATOR); // Collector Identifier - empty
        obr.append(FIELD_SEPARATOR); // Specimen Action Code - empty
        obr.append(FIELD_SEPARATOR); // Danger Code - empty
        obr.append(FIELD_SEPARATOR); // Relevant Clinical Information - empty
        obr.append(FIELD_SEPARATOR); // Specimen Received Date/Time - empty
        obr.append(FIELD_SEPARATOR); // Specimen Source - empty
        obr.append(FIELD_SEPARATOR).append(buildOrderingProvider(labOrder)); // Ordering Provider
        obr.append(FIELD_SEPARATOR); // Order Callback Phone Number - empty
        obr.append(FIELD_SEPARATOR); // Placer Field 1 - empty
        obr.append(FIELD_SEPARATOR); // Placer Field 2 - empty
        obr.append(FIELD_SEPARATOR); // Filler Field 1 - empty
        obr.append(FIELD_SEPARATOR); // Filler Field 2 - empty
        obr.append(FIELD_SEPARATOR).append(formatDateTime(labOrder.getScheduledDate() != null ? 
            labOrder.getScheduledDate() : labOrder.getOrderDate())); // Results Rpt/Status Chng - Date/Time
        obr.append(FIELD_SEPARATOR); // Charge to Practice - empty
        obr.append(FIELD_SEPARATOR); // Diagnostic Serv Sect ID - empty
        obr.append(FIELD_SEPARATOR); // Result Status (F = Final, P = Preliminary, etc.)
        obr.append(FIELD_SEPARATOR); // Parent Result - empty
        obr.append(FIELD_SEPARATOR); // Quantity/Timing - empty
        obr.append(FIELD_SEPARATOR); // Result Copies To - empty
        obr.append(FIELD_SEPARATOR); // Parent - empty
        obr.append(FIELD_SEPARATOR); // Transportation Mode - empty
        obr.append(FIELD_SEPARATOR); // Reason for Study - empty
        obr.append(FIELD_SEPARATOR); // Principal Result Interpreter - empty
        obr.append(FIELD_SEPARATOR); // Assistant Result Interpreter - empty
        obr.append(FIELD_SEPARATOR); // Technician - empty
        obr.append(FIELD_SEPARATOR); // Transcriptionist - empty
        obr.append(FIELD_SEPARATOR); // Scheduled Date/Time - empty
        obr.append(FIELD_SEPARATOR); // Number of Sample Containers - empty
        obr.append(FIELD_SEPARATOR); // Transport Logistics of Collected Sample - empty
        obr.append(FIELD_SEPARATOR); // Collector's Comment - empty
        obr.append(FIELD_SEPARATOR); // Transport Arrangement Responsibility - empty
        obr.append(FIELD_SEPARATOR); // Transport Arranged - empty
        obr.append(FIELD_SEPARATOR); // Escort Required - empty
        obr.append(FIELD_SEPARATOR); // Planned Patient Transport Comment - empty
        
        return obr.toString();
    }
    
    // Helper methods
    
    private String buildName(Patient patient) {
        String[] n = PatientNameInterop.splitFullName(patient.getFullName());
        String lastName = escapeField(n[2]);
        String firstName = escapeField(n[0]);
        String middleName = escapeField(n[1]);
        return lastName + COMPONENT_SEPARATOR + firstName + COMPONENT_SEPARATOR + middleName;
    }
    
    private String buildAddress(Patient patient) {
        // Build address from address components
        StringBuilder address = new StringBuilder();
        if (patient.getPrimaryAddressLine1() != null && !patient.getPrimaryAddressLine1().isEmpty()) {
            address.append(escapeField(patient.getPrimaryAddressLine1()));
        }
        if (patient.getPrimaryAddressLine2() != null && !patient.getPrimaryAddressLine2().isEmpty()) {
            if (address.length() > 0) address.append(" ");
            address.append(escapeField(patient.getPrimaryAddressLine2()));
        }
        address.append(COMPONENT_SEPARATOR).append(COMPONENT_SEPARATOR); // Address line 2, city
        if (patient.getPrimaryCity() != null && !patient.getPrimaryCity().isEmpty()) {
            address.append(escapeField(patient.getPrimaryCity()));
        }
        address.append(COMPONENT_SEPARATOR); // State
        if (patient.getPrimaryState() != null && !patient.getPrimaryState().isEmpty()) {
            address.append(escapeField(patient.getPrimaryState()));
        }
        address.append(COMPONENT_SEPARATOR); // Zip
        if (patient.getPrimaryZip() != null && !patient.getPrimaryZip().isEmpty()) {
            address.append(escapeField(patient.getPrimaryZip()));
        }
        address.append(COMPONENT_SEPARATOR); // Country
        if (patient.getPrimaryCountry() != null && !patient.getPrimaryCountry().isEmpty()) {
            address.append(escapeField(patient.getPrimaryCountry()));
        }
        return address.toString();
    }
    
    private String formatPhone(String phone) {
        if (phone == null) return "";
        // Remove non-numeric characters
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.length() == 10) {
            return "(" + cleaned.substring(0, 3) + ")" + cleaned.substring(3, 6) + "-" + cleaned.substring(6);
        }
        return phone;
    }
    
    private String buildUniversalServiceIdentifier(LabOrder labOrder) {
        String loincCode = labOrder.getLoincCode() != null ? labOrder.getLoincCode() : "";
        String testName = labOrder.getTestName() != null ? escapeField(labOrder.getTestName()) : "";
        return loincCode + COMPONENT_SEPARATOR + testName + COMPONENT_SEPARATOR + "LN";
    }
    
    private String buildOrderingProvider(LabOrder labOrder) {
        String providerId = labOrder.getOrderingProviderId() != null ? 
            labOrder.getOrderingProviderId().toString() : "";
        String providerName = labOrder.getOrderingProviderName() != null ? 
            escapeField(labOrder.getOrderingProviderName()) : "";
        return providerId + COMPONENT_SEPARATOR + providerName + COMPONENT_SEPARATOR + "MD";
    }
    
    private String buildOrderResponseFlag(LabOrder labOrder) {
        // Format: ^^^DateTime^^Priority
        String dateTime = formatDateTime(labOrder.getScheduledDate() != null ? 
            labOrder.getScheduledDate() : labOrder.getOrderDate());
        String priority = mapPriorityToHL7(labOrder.getPriority());
        return COMPONENT_SEPARATOR + COMPONENT_SEPARATOR + COMPONENT_SEPARATOR + 
            dateTime + COMPONENT_SEPARATOR + COMPONENT_SEPARATOR + priority;
    }
    
    private String mapPriorityToHL7(LabOrder.OrderPriority priority) {
        if (priority == null) return "R"; // Routine
        switch (priority) {
            case STAT: return "S";
            case ASAP: return "A";
            case TIMED: return "T";
            case ROUTINE: default: return "R";
        }
    }
    
    private String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(HL7_DATE_FORMAT);
    }
    
    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(HL7_DATE_FORMAT);
    }
    
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(HL7_DATETIME_FORMAT);
    }
    
    private String escapeField(String field) {
        if (field == null) return "";
        // Escape special HL7 characters - note: actual escaping depends on encoding characters defined in MSH
        // For simplicity, we'll just return the field as-is and let the LIS handle it
        // In production, proper escaping should be implemented based on MSH encoding characters
        return field;
    }
    
    /**
     * Generate HL7 V2 ORM message for an imaging order
     * 
     * @param imagingOrder The imaging order to generate message for
     * @return HL7 V2 ORM message string
     */
    public String generateOrmMessageForImaging(ImagingOrder imagingOrder) {
        Patient patient = imagingOrder.getPatient();
        
        StringBuilder message = new StringBuilder();
        
        // MSH - Message Header Segment
        message.append(buildMSHSegmentForImaging(imagingOrder));
        message.append("\r");
        
        // PID - Patient Identification Segment
        message.append(buildPIDSegment(patient));
        message.append("\r");
        
        // ORC - Common Order Segment
        message.append(buildORCSegmentForImaging(imagingOrder));
        message.append("\r");
        
        // OBR - Observation Request Segment
        message.append(buildOBRSegmentForImaging(imagingOrder));
        message.append("\r");
        
        return message.toString();
    }
    
    /**
     * Build MSH (Message Header) segment for imaging order
     */
    private String buildMSHSegmentForImaging(ImagingOrder imagingOrder) {
        StringBuilder msh = new StringBuilder("MSH");
        msh.append(FIELD_SEPARATOR).append("^~\\&"); // Encoding characters
        msh.append(FIELD_SEPARATOR).append("EHR"); // Sending Application
        msh.append(FIELD_SEPARATOR).append(imagingOrder.getOrderingFacilityName() != null ? 
            escapeField(imagingOrder.getOrderingFacilityName()) : "FACILITY"); // Sending Facility
        msh.append(FIELD_SEPARATOR).append("RIS"); // Receiving Application (RIS = Radiology Information System)
        msh.append(FIELD_SEPARATOR).append(imagingOrder.getRadiologyFacilityName() != null ? 
            escapeField(imagingOrder.getRadiologyFacilityName()) : "RADIOLOGY"); // Receiving Facility
        msh.append(FIELD_SEPARATOR).append(formatDateTime(LocalDateTime.now())); // Date/Time of Message
        msh.append(FIELD_SEPARATOR); // Security (empty)
        msh.append(FIELD_SEPARATOR).append("ORM^O01"); // Message Type (ORM^O01 = Order Message)
        msh.append(FIELD_SEPARATOR).append(imagingOrder.getOrderNumber()); // Message Control ID
        msh.append(FIELD_SEPARATOR).append("P"); // Processing ID (P = Production, T = Test)
        msh.append(FIELD_SEPARATOR).append("2.5"); // Version ID (HL7 Version 2.5)
        msh.append(FIELD_SEPARATOR); // Sequence Number (empty)
        msh.append(FIELD_SEPARATOR); // Continuation Pointer (empty)
        msh.append(FIELD_SEPARATOR); // Accept Acknowledgment Type (empty)
        msh.append(FIELD_SEPARATOR); // Application Acknowledgment Type (empty)
        msh.append(FIELD_SEPARATOR); // Country Code (empty)
        msh.append(FIELD_SEPARATOR); // Character Set (empty)
        msh.append(FIELD_SEPARATOR); // Principal Language of Message (empty)
        
        return msh.toString();
    }
    
    /**
     * Build ORC (Common Order) segment for imaging order
     */
    private String buildORCSegmentForImaging(ImagingOrder imagingOrder) {
        StringBuilder orc = new StringBuilder("ORC");
        orc.append(FIELD_SEPARATOR).append("NW"); // Order Control (NW = New Order)
        orc.append(FIELD_SEPARATOR).append(imagingOrder.getOrderNumber()); // Placer Order Number
        orc.append(FIELD_SEPARATOR); // Filler Order Number - empty
        orc.append(FIELD_SEPARATOR); // Placer Group Number - empty
        orc.append(FIELD_SEPARATOR); // Order Status - empty
        orc.append(FIELD_SEPARATOR).append(buildOrderResponseFlagForImaging(imagingOrder)); // Response Flag
        orc.append(FIELD_SEPARATOR); // Quantity/Timing - empty
        orc.append(FIELD_SEPARATOR).append(buildOrderingProviderForImaging(imagingOrder)); // Parent Order
        orc.append(FIELD_SEPARATOR); // Date/Time of Transaction - empty
        orc.append(FIELD_SEPARATOR).append(formatDateTime(imagingOrder.getOrderDate())); // Entered By
        orc.append(FIELD_SEPARATOR).append(buildOrderingProviderForImaging(imagingOrder)); // Verified By
        orc.append(FIELD_SEPARATOR); // Ordering Provider - empty (in OBR)
        orc.append(FIELD_SEPARATOR); // Enterer's Location - empty
        orc.append(FIELD_SEPARATOR); // Call Back Phone Number - empty
        orc.append(FIELD_SEPARATOR); // Order Effective Date/Time - empty
        orc.append(FIELD_SEPARATOR); // Order Control Code Reason - empty
        orc.append(FIELD_SEPARATOR); // Entering Organization - empty
        orc.append(FIELD_SEPARATOR); // Entering Device - empty
        orc.append(FIELD_SEPARATOR); // Action By - empty
        orc.append(FIELD_SEPARATOR); // Advanced Beneficiary Notice Code - empty
        orc.append(FIELD_SEPARATOR); // Ordering Facility Name - empty
        orc.append(FIELD_SEPARATOR); // Ordering Facility Address - empty
        orc.append(FIELD_SEPARATOR); // Ordering Facility Phone Number - empty
        orc.append(FIELD_SEPARATOR); // Ordering Provider Address - empty
        orc.append(FIELD_SEPARATOR); // Order Status Modifier - empty
        
        return orc.toString();
    }
    
    /**
     * Build OBR (Observation Request) segment for imaging order
     */
    private String buildOBRSegmentForImaging(ImagingOrder imagingOrder) {
        StringBuilder obr = new StringBuilder("OBR");
        obr.append(FIELD_SEPARATOR).append("1"); // Set ID
        obr.append(FIELD_SEPARATOR).append(imagingOrder.getOrderNumber()); // Placer Order Number
        obr.append(FIELD_SEPARATOR); // Filler Order Number - empty
        obr.append(FIELD_SEPARATOR).append(buildUniversalServiceIdentifierForImaging(imagingOrder)); // Universal Service Identifier
        obr.append(FIELD_SEPARATOR); // Priority - empty (in ORC)
        obr.append(FIELD_SEPARATOR).append(formatDateTime(imagingOrder.getOrderDate())); // Requested Date/Time
        obr.append(FIELD_SEPARATOR); // Observation Date/Time - empty
        obr.append(FIELD_SEPARATOR); // Observation End Date/Time - empty
        obr.append(FIELD_SEPARATOR); // Collection Volume - empty
        obr.append(FIELD_SEPARATOR); // Collector Identifier - empty
        obr.append(FIELD_SEPARATOR); // Specimen Action Code - empty
        obr.append(FIELD_SEPARATOR); // Danger Code - empty
        obr.append(FIELD_SEPARATOR).append(escapeField(imagingOrder.getClinicalIndication())); // Relevant Clinical Information
        obr.append(FIELD_SEPARATOR); // Specimen Received Date/Time - empty
        obr.append(FIELD_SEPARATOR); // Specimen Source - empty
        obr.append(FIELD_SEPARATOR).append(buildOrderingProviderForImaging(imagingOrder)); // Ordering Provider
        obr.append(FIELD_SEPARATOR); // Order Callback Phone Number - empty
        obr.append(FIELD_SEPARATOR); // Placer Field 1 - empty
        obr.append(FIELD_SEPARATOR); // Placer Field 2 - empty
        obr.append(FIELD_SEPARATOR); // Filler Field 1 - empty
        obr.append(FIELD_SEPARATOR); // Filler Field 2 - empty
        obr.append(FIELD_SEPARATOR).append(formatDateTime(imagingOrder.getScheduledDate() != null ? 
            imagingOrder.getScheduledDate() : imagingOrder.getOrderDate())); // Results Rpt/Status Chng - Date/Time
        obr.append(FIELD_SEPARATOR); // Charge to Practice - empty
        obr.append(FIELD_SEPARATOR); // Diagnostic Serv Sect ID - empty
        obr.append(FIELD_SEPARATOR); // Result Status (F = Final, P = Preliminary, etc.)
        obr.append(FIELD_SEPARATOR); // Parent Result - empty
        obr.append(FIELD_SEPARATOR); // Quantity/Timing - empty
        obr.append(FIELD_SEPARATOR); // Result Copies To - empty
        obr.append(FIELD_SEPARATOR); // Parent - empty
        obr.append(FIELD_SEPARATOR); // Transportation Mode - empty
        obr.append(FIELD_SEPARATOR); // Reason for Study - empty
        obr.append(FIELD_SEPARATOR); // Principal Result Interpreter - empty
        obr.append(FIELD_SEPARATOR); // Assistant Result Interpreter - empty
        obr.append(FIELD_SEPARATOR); // Technician - empty
        obr.append(FIELD_SEPARATOR); // Transcriptionist - empty
        obr.append(FIELD_SEPARATOR); // Scheduled Date/Time - empty
        obr.append(FIELD_SEPARATOR); // Number of Sample Containers - empty
        obr.append(FIELD_SEPARATOR); // Transport Logistics of Collected Sample - empty
        obr.append(FIELD_SEPARATOR); // Collector's Comment - empty
        obr.append(FIELD_SEPARATOR); // Transport Arrangement Responsibility - empty
        obr.append(FIELD_SEPARATOR); // Transport Arranged - empty
        obr.append(FIELD_SEPARATOR); // Escort Required - empty
        obr.append(FIELD_SEPARATOR); // Planned Patient Transport Comment - empty
        
        return obr.toString();
    }
    
    private String buildUniversalServiceIdentifierForImaging(ImagingOrder imagingOrder) {
        String cptCode = imagingOrder.getCptCode() != null ? imagingOrder.getCptCode() : "";
        String studyDescription = imagingOrder.getStudyDescription() != null ? escapeField(imagingOrder.getStudyDescription()) : "";
        return cptCode + COMPONENT_SEPARATOR + studyDescription + COMPONENT_SEPARATOR + "CPT";
    }
    
    private String buildOrderingProviderForImaging(ImagingOrder imagingOrder) {
        String providerId = imagingOrder.getOrderingProviderId() != null ? 
            imagingOrder.getOrderingProviderId().toString() : "";
        String providerName = imagingOrder.getOrderingProviderName() != null ? 
            escapeField(imagingOrder.getOrderingProviderName()) : "";
        return providerId + COMPONENT_SEPARATOR + providerName + COMPONENT_SEPARATOR + "MD";
    }
    
    private String buildOrderResponseFlagForImaging(ImagingOrder imagingOrder) {
        // Format: ^^^DateTime^^Priority
        String dateTime = formatDateTime(imagingOrder.getScheduledDate() != null ? 
            imagingOrder.getScheduledDate() : imagingOrder.getOrderDate());
        String priority = mapPriorityToHL7ForImaging(imagingOrder.getPriority());
        return COMPONENT_SEPARATOR + COMPONENT_SEPARATOR + COMPONENT_SEPARATOR + 
            dateTime + COMPONENT_SEPARATOR + COMPONENT_SEPARATOR + priority;
    }
    
    private String mapPriorityToHL7ForImaging(ImagingOrder.OrderPriority priority) {
        if (priority == null) return "R"; // Routine
        switch (priority) {
            case STAT: return "S";
            case URGENT: return "U";
            case ROUTINE: default: return "R";
        }
    }
}
