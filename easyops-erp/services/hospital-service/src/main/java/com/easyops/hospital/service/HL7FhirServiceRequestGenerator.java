package com.easyops.hospital.service;

import com.easyops.hospital.entity.ImagingOrder;
import com.easyops.hospital.entity.LabOrder;
import com.easyops.hospital.entity.Patient;
import com.easyops.hospital.util.PatientDisplayName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for generating HL7 FHIR ServiceRequest resources for laboratory orders.
 * FHIR ServiceRequest is the modern standard for representing orders in healthcare systems.
 */
@Service
@Slf4j
public class HL7FhirServiceRequestGenerator {
    
    private static final DateTimeFormatter ISO_DATETIME_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    /**
     * Generate HL7 FHIR ServiceRequest resource as JSON for a lab order
     * 
     * @param labOrder The lab order to generate resource for
     * @return Map representing FHIR ServiceRequest resource (can be serialized to JSON)
     */
    public Map<String, Object> generateServiceRequest(LabOrder labOrder) {
        Patient patient = labOrder.getPatient();
        
        Map<String, Object> serviceRequest = new LinkedHashMap<>();
        
        // Resource Type
        serviceRequest.put("resourceType", "ServiceRequest");
        
        // ID
        serviceRequest.put("id", labOrder.getOrderId().toString());
        
        // Meta
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("versionId", "1");
        meta.put("lastUpdated", formatIsoDateTime(LocalDateTime.now()));
        List<String> profile = new ArrayList<>();
        profile.add("http://hl7.org/fhir/StructureDefinition/ServiceRequest");
        meta.put("profile", profile);
        serviceRequest.put("meta", meta);
        
        // Status
        serviceRequest.put("status", mapStatusToFhir(labOrder.getOrderStatus()));
        
        // Intent
        serviceRequest.put("intent", "order");
        
        // Category
        if (labOrder.getTestCategory() != null) {
            List<Map<String, Object>> categories = new ArrayList<>();
            Map<String, Object> category = new LinkedHashMap<>();
            List<Map<String, Object>> coding = new ArrayList<>();
            Map<String, Object> code = new LinkedHashMap<>();
            code.put("system", "http://terminology.hl7.org/CodeSystem/v2-0074");
            code.put("code", mapCategoryToCode(labOrder.getTestCategory()));
            code.put("display", labOrder.getTestCategory());
            coding.add(code);
            category.put("coding", coding);
            categories.add(category);
            serviceRequest.put("category", categories);
        }
        
        // Priority
        serviceRequest.put("priority", mapPriorityToFhir(labOrder.getPriority()));
        
        // Code (the test being ordered)
        Map<String, Object> code = new LinkedHashMap<>();
        List<Map<String, Object>> coding = new ArrayList<>();
        
        // LOINC code
        if (labOrder.getLoincCode() != null && !labOrder.getLoincCode().isEmpty()) {
            Map<String, Object> loincCode = new LinkedHashMap<>();
            loincCode.put("system", "http://loinc.org");
            loincCode.put("code", labOrder.getLoincCode());
            loincCode.put("display", labOrder.getTestName());
            coding.add(loincCode);
        }
        
        // If no LOINC, use test name
        if (coding.isEmpty() && labOrder.getTestName() != null) {
            Map<String, Object> testCode = new LinkedHashMap<>();
            testCode.put("display", labOrder.getTestName());
            coding.add(testCode);
        }
        
        code.put("coding", coding);
        if (labOrder.getTestName() != null) {
            code.put("text", labOrder.getTestName());
        }
        serviceRequest.put("code", code);
        
        // Subject (Patient)
        Map<String, Object> subject = new LinkedHashMap<>();
        subject.put("reference", "Patient/" + patient.getPatientId().toString());
        String display = PatientDisplayName.of(patient);
        if (!display.isEmpty()) {
            subject.put("display", display);
        }
        serviceRequest.put("subject", subject);
        
        // Encounter
        if (labOrder.getEncounterId() != null) {
            Map<String, Object> encounter = new LinkedHashMap<>();
            encounter.put("reference", "Encounter/" + labOrder.getEncounterId().toString());
            serviceRequest.put("encounter", encounter);
        }
        
        // Occurrence (scheduled date/time)
        if (labOrder.getScheduledDate() != null) {
            serviceRequest.put("occurrenceDateTime", formatIsoDateTime(labOrder.getScheduledDate()));
        } else {
            serviceRequest.put("occurrenceDateTime", formatIsoDateTime(labOrder.getOrderDate()));
        }
        
        // Authored On
        serviceRequest.put("authoredOn", formatIsoDateTime(labOrder.getOrderDate()));
        
        // Requester (Ordering Provider)
        Map<String, Object> requester = new LinkedHashMap<>();
        if (labOrder.getOrderingProviderId() != null) {
            requester.put("reference", "Practitioner/" + labOrder.getOrderingProviderId().toString());
        }
        if (labOrder.getOrderingProviderName() != null) {
            requester.put("display", labOrder.getOrderingProviderName());
        }
        serviceRequest.put("requester", requester);
        
        // Performer (Laboratory)
        if (labOrder.getLaboratoryId() != null || labOrder.getLaboratoryName() != null) {
            List<Map<String, Object>> performers = new ArrayList<>();
            Map<String, Object> performer = new LinkedHashMap<>();
            if (labOrder.getLaboratoryId() != null) {
                performer.put("reference", "Organization/" + labOrder.getLaboratoryId().toString());
            }
            if (labOrder.getLaboratoryName() != null) {
                performer.put("display", labOrder.getLaboratoryName());
            }
            performers.add(performer);
            serviceRequest.put("performer", performers);
        }
        
        // Reason Code (Clinical Indication)
        if (labOrder.getClinicalIndication() != null && !labOrder.getClinicalIndication().isEmpty()) {
            List<Map<String, Object>> reasonCodes = new ArrayList<>();
            Map<String, Object> reasonCode = new LinkedHashMap<>();
            reasonCode.put("text", labOrder.getClinicalIndication());
            reasonCodes.add(reasonCode);
            serviceRequest.put("reasonCode", reasonCodes);
        }
        
        // Note (Special Instructions)
        if (labOrder.getSpecialInstructions() != null && !labOrder.getSpecialInstructions().isEmpty()) {
            List<Map<String, Object>> notes = new ArrayList<>();
            Map<String, Object> note = new LinkedHashMap<>();
            note.put("text", labOrder.getSpecialInstructions());
            notes.add(note);
            serviceRequest.put("note", notes);
        }
        
        // Extension for additional information
        List<Map<String, Object>> extensions = new ArrayList<>();
        
        // Test Panel Extension
        if (labOrder.getIsTestPanel() != null && labOrder.getIsTestPanel()) {
            Map<String, Object> panelExtension = new LinkedHashMap<>();
            panelExtension.put("url", "http://easyops.com/fhir/StructureDefinition/test-panel");
            Map<String, Object> panelValue = new LinkedHashMap<>();
            panelValue.put("valueBoolean", true);
            panelExtension.put("valueBoolean", true);
            if (labOrder.getPanelName() != null) {
                Map<String, Object> panelNameExtension = new LinkedHashMap<>();
                panelNameExtension.put("url", "http://easyops.com/fhir/StructureDefinition/panel-name");
                panelNameExtension.put("valueString", labOrder.getPanelName());
                extensions.add(panelNameExtension);
            }
            extensions.add(panelExtension);
        }
        
        // Fasting Required Extension
        if (labOrder.getFastingRequired() != null && labOrder.getFastingRequired()) {
            Map<String, Object> fastingExtension = new LinkedHashMap<>();
            fastingExtension.put("url", "http://easyops.com/fhir/StructureDefinition/fasting-required");
            fastingExtension.put("valueBoolean", true);
            extensions.add(fastingExtension);
        }
        
        // Patient Preparation Instructions Extension
        if (labOrder.getPatientPreparationInstructions() != null && 
            !labOrder.getPatientPreparationInstructions().isEmpty()) {
            Map<String, Object> prepExtension = new LinkedHashMap<>();
            prepExtension.put("url", "http://easyops.com/fhir/StructureDefinition/patient-preparation");
            prepExtension.put("valueString", labOrder.getPatientPreparationInstructions());
            extensions.add(prepExtension);
        }
        
        // Order Number Extension
        Map<String, Object> orderNumberExtension = new LinkedHashMap<>();
        orderNumberExtension.put("url", "http://easyops.com/fhir/StructureDefinition/order-number");
        orderNumberExtension.put("valueString", labOrder.getOrderNumber());
        extensions.add(orderNumberExtension);
        
        if (!extensions.isEmpty()) {
            serviceRequest.put("extension", extensions);
        }
        
        return serviceRequest;
    }
    
    // Helper methods
    
    private String mapStatusToFhir(LabOrder.OrderStatus status) {
        if (status == null) return "draft";
        switch (status) {
            case PENDING: return "draft";
            case SENT: return "active";
            case COLLECTED: return "active";
            case IN_PROCESS: return "active";
            case COMPLETED: return "completed";
            case CANCELLED: return "revoked";
            default: return "draft";
        }
    }
    
    private String mapPriorityToFhir(LabOrder.OrderPriority priority) {
        if (priority == null) return "routine";
        switch (priority) {
            case STAT: return "stat";
            case ASAP: return "urgent";
            case TIMED: return "asap";
            case ROUTINE: default: return "routine";
        }
    }
    
    private String mapCategoryToCode(String category) {
        if (category == null) return "LAB";
        // Map common categories to HL7 v2-0074 codes
        String upperCategory = category.toUpperCase();
        if (upperCategory.contains("CHEMISTRY") || upperCategory.contains("CHEM")) {
            return "CH";
        } else if (upperCategory.contains("HEMATOLOGY") || upperCategory.contains("HEME")) {
            return "HE";
        } else if (upperCategory.contains("MICROBIOLOGY") || upperCategory.contains("MICRO")) {
            return "MB";
        } else if (upperCategory.contains("IMMUNOLOGY") || upperCategory.contains("IMMUNO")) {
            return "IMM";
        } else if (upperCategory.contains("SEROLOGY")) {
            return "SER";
        } else if (upperCategory.contains("BLOOD") || upperCategory.contains("BANK")) {
            return "BL";
        } else {
            return "LAB";
        }
    }
    
    private String formatIsoDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.atZone(ZoneId.systemDefault())
                      .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
    
    /**
     * Generate HL7 FHIR ServiceRequest resource as JSON for an imaging order
     * 
     * @param imagingOrder The imaging order to generate resource for
     * @return Map representing FHIR ServiceRequest resource (can be serialized to JSON)
     */
    public Map<String, Object> generateServiceRequestForImaging(ImagingOrder imagingOrder) {
        Patient patient = imagingOrder.getPatient();
        
        Map<String, Object> serviceRequest = new LinkedHashMap<>();
        
        // Resource Type
        serviceRequest.put("resourceType", "ServiceRequest");
        
        // ID
        serviceRequest.put("id", imagingOrder.getOrderId().toString());
        
        // Meta
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("versionId", "1");
        meta.put("lastUpdated", formatIsoDateTime(LocalDateTime.now()));
        List<String> profile = new ArrayList<>();
        profile.add("http://hl7.org/fhir/StructureDefinition/ServiceRequest");
        meta.put("profile", profile);
        serviceRequest.put("meta", meta);
        
        // Status
        serviceRequest.put("status", mapStatusToFhirForImaging(imagingOrder.getOrderStatus()));
        
        // Intent
        serviceRequest.put("intent", "order");
        
        // Category - Imaging
        List<Map<String, Object>> categories = new ArrayList<>();
        Map<String, Object> category = new LinkedHashMap<>();
        List<Map<String, Object>> coding = new ArrayList<>();
        Map<String, Object> code = new LinkedHashMap<>();
        code.put("system", "http://terminology.hl7.org/CodeSystem/v2-0074");
        code.put("code", "RAD");
        code.put("display", "Radiology");
        coding.add(code);
        category.put("coding", coding);
        categories.add(category);
        serviceRequest.put("category", categories);
        
        // Priority
        serviceRequest.put("priority", mapPriorityToFhirForImaging(imagingOrder.getPriority()));
        
        // Code (the imaging study being ordered)
        Map<String, Object> codeMap = new LinkedHashMap<>();
        List<Map<String, Object>> codeCoding = new ArrayList<>();
        
        // CPT code
        if (imagingOrder.getCptCode() != null && !imagingOrder.getCptCode().isEmpty()) {
            Map<String, Object> cptCode = new LinkedHashMap<>();
            cptCode.put("system", "http://www.ama-assn.org/go/cpt");
            cptCode.put("code", imagingOrder.getCptCode());
            cptCode.put("display", imagingOrder.getStudyDescription());
            codeCoding.add(cptCode);
        }
        
        // If no CPT, use study description
        if (codeCoding.isEmpty() && imagingOrder.getStudyDescription() != null) {
            Map<String, Object> studyCode = new LinkedHashMap<>();
            studyCode.put("display", imagingOrder.getStudyDescription());
            codeCoding.add(studyCode);
        }
        
        codeMap.put("coding", codeCoding);
        if (imagingOrder.getStudyDescription() != null) {
            codeMap.put("text", imagingOrder.getStudyDescription());
        }
        serviceRequest.put("code", codeMap);
        
        // Subject (Patient)
        Map<String, Object> subject = new LinkedHashMap<>();
        subject.put("reference", "Patient/" + patient.getPatientId().toString());
        String display = PatientDisplayName.of(patient);
        if (!display.isEmpty()) {
            subject.put("display", display);
        }
        serviceRequest.put("subject", subject);
        
        // Encounter
        if (imagingOrder.getEncounterId() != null) {
            Map<String, Object> encounter = new LinkedHashMap<>();
            encounter.put("reference", "Encounter/" + imagingOrder.getEncounterId().toString());
            serviceRequest.put("encounter", encounter);
        }
        
        // Occurrence (scheduled date/time)
        if (imagingOrder.getScheduledDate() != null) {
            serviceRequest.put("occurrenceDateTime", formatIsoDateTime(imagingOrder.getScheduledDate()));
        } else {
            serviceRequest.put("occurrenceDateTime", formatIsoDateTime(imagingOrder.getOrderDate()));
        }
        
        // Authored On
        serviceRequest.put("authoredOn", formatIsoDateTime(imagingOrder.getOrderDate()));
        
        // Requester (Ordering Provider)
        Map<String, Object> requester = new LinkedHashMap<>();
        if (imagingOrder.getOrderingProviderId() != null) {
            requester.put("reference", "Practitioner/" + imagingOrder.getOrderingProviderId().toString());
        }
        if (imagingOrder.getOrderingProviderName() != null) {
            requester.put("display", imagingOrder.getOrderingProviderName());
        }
        serviceRequest.put("requester", requester);
        
        // Performer (Radiology Facility)
        if (imagingOrder.getRadiologyFacilityId() != null || imagingOrder.getRadiologyFacilityName() != null) {
            List<Map<String, Object>> performers = new ArrayList<>();
            Map<String, Object> performer = new LinkedHashMap<>();
            if (imagingOrder.getRadiologyFacilityId() != null) {
                performer.put("reference", "Organization/" + imagingOrder.getRadiologyFacilityId().toString());
            }
            if (imagingOrder.getRadiologyFacilityName() != null) {
                performer.put("display", imagingOrder.getRadiologyFacilityName());
            }
            performers.add(performer);
            serviceRequest.put("performer", performers);
        }
        
        // Reason Code (Clinical Indication)
        if (imagingOrder.getClinicalIndication() != null && !imagingOrder.getClinicalIndication().isEmpty()) {
            List<Map<String, Object>> reasonCodes = new ArrayList<>();
            Map<String, Object> reasonCode = new LinkedHashMap<>();
            reasonCode.put("text", imagingOrder.getClinicalIndication());
            reasonCodes.add(reasonCode);
            serviceRequest.put("reasonCode", reasonCodes);
        }
        
        // Note (Special Instructions)
        if (imagingOrder.getSpecialInstructions() != null && !imagingOrder.getSpecialInstructions().isEmpty()) {
            List<Map<String, Object>> notes = new ArrayList<>();
            Map<String, Object> note = new LinkedHashMap<>();
            note.put("text", imagingOrder.getSpecialInstructions());
            notes.add(note);
            serviceRequest.put("note", notes);
        }
        
        // Extension for additional information
        List<Map<String, Object>> extensions = new ArrayList<>();
        
        // Study Modality Extension
        if (imagingOrder.getStudyModality() != null) {
            Map<String, Object> modalityExtension = new LinkedHashMap<>();
            modalityExtension.put("url", "http://easyops.com/fhir/StructureDefinition/study-modality");
            modalityExtension.put("valueString", imagingOrder.getStudyModality().name());
            extensions.add(modalityExtension);
        }
        
        // Body Part Extension
        if (imagingOrder.getBodyPart() != null) {
            Map<String, Object> bodyPartExtension = new LinkedHashMap<>();
            bodyPartExtension.put("url", "http://easyops.com/fhir/StructureDefinition/body-part");
            bodyPartExtension.put("valueString", imagingOrder.getBodyPart());
            extensions.add(bodyPartExtension);
        }
        
        // Laterality Extension
        if (imagingOrder.getLaterality() != null) {
            Map<String, Object> lateralityExtension = new LinkedHashMap<>();
            lateralityExtension.put("url", "http://easyops.com/fhir/StructureDefinition/laterality");
            lateralityExtension.put("valueString", imagingOrder.getLaterality());
            extensions.add(lateralityExtension);
        }
        
        // Contrast Required Extension
        if (imagingOrder.getContrastRequired() != null && imagingOrder.getContrastRequired()) {
            Map<String, Object> contrastExtension = new LinkedHashMap<>();
            contrastExtension.put("url", "http://easyops.com/fhir/StructureDefinition/contrast-required");
            contrastExtension.put("valueBoolean", true);
            extensions.add(contrastExtension);
            if (imagingOrder.getContrastType() != null) {
                Map<String, Object> contrastTypeExtension = new LinkedHashMap<>();
                contrastTypeExtension.put("url", "http://easyops.com/fhir/StructureDefinition/contrast-type");
                contrastTypeExtension.put("valueString", imagingOrder.getContrastType());
                extensions.add(contrastTypeExtension);
            }
        }
        
        // Patient Preparation Required Extension
        if (imagingOrder.getPatientPreparationRequired() != null && imagingOrder.getPatientPreparationRequired()) {
            Map<String, Object> prepExtension = new LinkedHashMap<>();
            prepExtension.put("url", "http://easyops.com/fhir/StructureDefinition/patient-preparation");
            if (imagingOrder.getPatientPreparationInstructions() != null) {
                prepExtension.put("valueString", imagingOrder.getPatientPreparationInstructions());
            } else {
                prepExtension.put("valueBoolean", true);
            }
            extensions.add(prepExtension);
        }
        
        // Order Number Extension
        Map<String, Object> orderNumberExtension = new LinkedHashMap<>();
        orderNumberExtension.put("url", "http://easyops.com/fhir/StructureDefinition/order-number");
        orderNumberExtension.put("valueString", imagingOrder.getOrderNumber());
        extensions.add(orderNumberExtension);
        
        if (!extensions.isEmpty()) {
            serviceRequest.put("extension", extensions);
        }
        
        return serviceRequest;
    }
    
    private String mapStatusToFhirForImaging(ImagingOrder.OrderStatus status) {
        if (status == null) return "draft";
        switch (status) {
            case PENDING: return "draft";
            case SENT: return "active";
            case SCHEDULED: return "active";
            case IN_PROGRESS: return "active";
            case COMPLETED: return "completed";
            case CANCELLED: return "revoked";
            case NO_SHOW: return "revoked";
            default: return "draft";
        }
    }
    
    private String mapPriorityToFhirForImaging(ImagingOrder.OrderPriority priority) {
        if (priority == null) return "routine";
        switch (priority) {
            case STAT: return "stat";
            case URGENT: return "urgent";
            case ROUTINE: default: return "routine";
        }
    }
}
