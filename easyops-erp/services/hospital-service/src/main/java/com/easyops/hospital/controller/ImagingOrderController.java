package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.ImagingOrderRequest;
import com.easyops.hospital.dto.response.HL7MessageResponse;
import com.easyops.hospital.dto.response.ImagingOrderResponse;
import com.easyops.hospital.service.DICOMWorklistService;
import com.easyops.hospital.service.HL7FhirServiceRequestGenerator;
import com.easyops.hospital.service.HL7V2OrmMessageGenerator;
import com.easyops.hospital.service.ImagingOrderService;
import com.easyops.hospital.service.ImagingSchedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/imaging-orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Imaging Order Management", description = "APIs for imaging study ordering and management")
public class ImagingOrderController {
    
    private final ImagingOrderService imagingOrderService;
    private final HL7V2OrmMessageGenerator hl7V2OrmMessageGenerator;
    private final HL7FhirServiceRequestGenerator hl7FhirServiceRequestGenerator;
    private final DICOMWorklistService dicomWorklistService;
    private final ImagingSchedulingService imagingSchedulingService;
    
    // ========== Imaging Order CRUD Operations ==========
    
    @PostMapping
    @Operation(summary = "Create a new imaging order", description = "Create a new imaging study order")
    public ResponseEntity<ImagingOrderResponse> createImagingOrder(
            @Valid @RequestBody ImagingOrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Creating imaging order for patient: {}", request.getPatientId());
        if (userId == null) userId = UUID.randomUUID();
        ImagingOrderResponse response = imagingOrderService.createImagingOrder(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{orderId}")
    @Operation(summary = "Get imaging order by ID", description = "Retrieve an imaging order by its ID")
    public ResponseEntity<ImagingOrderResponse> getImagingOrderById(@PathVariable UUID orderId) {
        ImagingOrderResponse response = imagingOrderService.getImagingOrderById(orderId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get imaging order by number", description = "Retrieve an imaging order by its order number")
    public ResponseEntity<ImagingOrderResponse> getImagingOrderByNumber(@PathVariable String orderNumber) {
        ImagingOrderResponse response = imagingOrderService.getImagingOrderByNumber(orderNumber);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all imaging orders", description = "Retrieve all imaging orders")
    public ResponseEntity<List<ImagingOrderResponse>> getAllImagingOrders() {
        List<ImagingOrderResponse> responses = imagingOrderService.getAllImagingOrders();
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}")
    @Operation(summary = "Get all imaging orders for a patient", description = "Retrieve all imaging orders for a patient, ordered by date")
    public ResponseEntity<List<ImagingOrderResponse>> getImagingOrdersByPatient(@PathVariable UUID patientId) {
        List<ImagingOrderResponse> responses = imagingOrderService.getImagingOrdersByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/pending")
    @Operation(summary = "Get pending imaging orders", description = "Retrieve pending imaging orders for a patient")
    public ResponseEntity<List<ImagingOrderResponse>> getPendingImagingOrders(@PathVariable UUID patientId) {
        List<ImagingOrderResponse> responses = imagingOrderService.getPendingImagingOrdersByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/completed")
    @Operation(summary = "Get completed imaging orders", description = "Retrieve completed imaging orders for a patient")
    public ResponseEntity<List<ImagingOrderResponse>> getCompletedImagingOrders(@PathVariable UUID patientId) {
        List<ImagingOrderResponse> responses = imagingOrderService.getCompletedImagingOrdersByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{orderId}")
    @Operation(summary = "Update imaging order", description = "Update an imaging order (only allowed for PENDING status)")
    public ResponseEntity<ImagingOrderResponse> updateImagingOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody ImagingOrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Updating imaging order: {}", orderId);
        if (userId == null) userId = UUID.randomUUID();
        ImagingOrderResponse response = imagingOrderService.updateImagingOrder(orderId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{orderId}/send")
    @Operation(summary = "Send imaging order", description = "Send imaging order to radiology (change status to SENT)")
    public ResponseEntity<ImagingOrderResponse> sendImagingOrder(
            @PathVariable UUID orderId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Sending imaging order: {}", orderId);
        if (userId == null) userId = UUID.randomUUID();
        ImagingOrderResponse response = imagingOrderService.sendImagingOrder(orderId, userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel imaging order", description = "Cancel an imaging order with reason")
    public ResponseEntity<ImagingOrderResponse> cancelImagingOrder(
            @PathVariable UUID orderId,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Cancelling imaging order: {}", orderId);
        if (userId == null) userId = UUID.randomUUID();
        if (reason == null) reason = "Cancelled by provider";
        ImagingOrderResponse response = imagingOrderService.cancelImagingOrder(orderId, reason, userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{orderId}/reschedule")
    @Operation(summary = "Reschedule imaging order", description = "Reschedule an imaging order to a new date/time")
    public ResponseEntity<ImagingOrderResponse> rescheduleImagingOrder(
            @PathVariable UUID orderId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledTime,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Rescheduling imaging order: {} to {}", orderId, scheduledDate);
        if (userId == null) userId = UUID.randomUUID();
        ImagingOrderResponse response = imagingOrderService.rescheduleImagingOrder(orderId, scheduledDate, scheduledTime, userId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{orderId}")
    @Operation(summary = "Delete imaging order", description = "Delete an imaging order (only allowed for PENDING status)")
    public ResponseEntity<Void> deleteImagingOrder(@PathVariable UUID orderId) {
        log.info("Deleting imaging order: {}", orderId);
        imagingOrderService.deleteImagingOrder(orderId);
        return ResponseEntity.noContent().build();
    }
    
    // ========== RIS/PACS Transmission ==========
    
    @PostMapping("/{orderId}/transmit-ris-pacs")
    @Operation(summary = "Manually transmit imaging order to RIS/PACS", 
               description = "Manually trigger transmission of an imaging order to RIS/PACS (can be used to retry failed transmissions)")
    public ResponseEntity<Map<String, Object>> transmitToRISPACS(@PathVariable UUID orderId) {
        log.info("Manually transmitting imaging order {} to RIS/PACS", orderId);
        
        com.easyops.hospital.service.RISPACSTransmissionService.TransmissionResult result = 
            imagingOrderService.transmitToRISPACS(orderId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("status", result.getStatus());
        response.put("message", result.getMessage());
        response.put("transmittedAt", result.getTransmittedAt());
        response.put("transmissionMethod", result.getTransmissionMethod());
        response.put("responseCode", result.getResponseCode());
        response.put("responseMessage", result.getResponseMessage());
        response.put("attempt", result.getAttempt());
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // ========== HL7 Message Generation ==========
    
    @GetMapping("/{orderId}/hl7-v2-orm")
    @Operation(summary = "Generate HL7 V2 ORM message", description = "Generate HL7 V2 ORM (Order Message) for transmission to RIS/PACS")
    public ResponseEntity<HL7MessageResponse> generateHL7V2OrmMessage(@PathVariable UUID orderId) {
        log.info("Generating HL7 V2 ORM message for order: {}", orderId);
        ImagingOrderResponse orderResponse = imagingOrderService.getImagingOrderById(orderId);
        
        // Get the full order entity for message generation
        com.easyops.hospital.entity.ImagingOrder imagingOrder = imagingOrderService.getImagingOrderEntityById(orderId);
        
        String ormMessage = hl7V2OrmMessageGenerator.generateOrmMessageForImaging(imagingOrder);
        
        HL7MessageResponse response = HL7MessageResponse.builder()
            .messageType("HL7_V2_ORM")
            .messageFormat("TEXT")
            .messageContent(ormMessage)
            .orderId(orderId.toString())
            .orderNumber(orderResponse.getOrderNumber())
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{orderId}/hl7-fhir-servicerequest")
    @Operation(summary = "Generate HL7 FHIR ServiceRequest", description = "Generate HL7 FHIR ServiceRequest resource for the imaging order")
    public ResponseEntity<HL7MessageResponse> generateHL7FhirServiceRequest(@PathVariable UUID orderId) {
        log.info("Generating HL7 FHIR ServiceRequest for order: {}", orderId);
        ImagingOrderResponse orderResponse = imagingOrderService.getImagingOrderById(orderId);
        
        // Get the full order entity for message generation
        com.easyops.hospital.entity.ImagingOrder imagingOrder = imagingOrderService.getImagingOrderEntityById(orderId);
        
        java.util.Map<String, Object> serviceRequest = hl7FhirServiceRequestGenerator.generateServiceRequestForImaging(imagingOrder);
        
        HL7MessageResponse response = HL7MessageResponse.builder()
            .messageType("HL7_FHIR_SERVICEREQUEST")
            .messageFormat("JSON")
            .messageResource(serviceRequest)
            .orderId(orderId.toString())
            .orderNumber(orderResponse.getOrderNumber())
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    // ========== DICOM Worklist Integration ==========
    
    @PostMapping("/{orderId}/dicom-worklist/submit")
    @Operation(summary = "Submit imaging order to DICOM worklist", 
               description = "Submit an imaging order to DICOM Modality Worklist (MWL) for query by imaging modalities")
    public ResponseEntity<Map<String, Object>> submitToDICOMWorklist(@PathVariable UUID orderId) {
        log.info("Submitting imaging order {} to DICOM worklist", orderId);
        
        com.easyops.hospital.entity.ImagingOrder imagingOrder = 
            imagingOrderService.getImagingOrderEntityById(orderId);
        
        DICOMWorklistService.WorklistSubmissionResult result = 
            dicomWorklistService.submitWorklistEntry(imagingOrder);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("status", result.getStatus());
        response.put("message", result.getMessage());
        response.put("submittedAt", result.getSubmittedAt());
        response.put("worklistEntry", result.getWorklistEntry());
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/{orderId}/dicom-worklist")
    @Operation(summary = "Generate DICOM worklist entry", 
               description = "Generate DICOM Modality Worklist entry for an imaging order (without submitting)")
    public ResponseEntity<DICOMWorklistService.WorklistEntry> generateDICOMWorklistEntry(@PathVariable UUID orderId) {
        log.info("Generating DICOM worklist entry for imaging order {}", orderId);
        
        com.easyops.hospital.entity.ImagingOrder imagingOrder = 
            imagingOrderService.getImagingOrderEntityById(orderId);
        
        DICOMWorklistService.WorklistEntry entry = 
            dicomWorklistService.generateWorklistEntry(imagingOrder);
        
        return ResponseEntity.ok(entry);
    }
    
    @GetMapping("/dicom-worklist/query")
    @Operation(summary = "Query DICOM worklist", 
               description = "Query DICOM worklist for scheduled studies by modality and date")
    public ResponseEntity<List<DICOMWorklistService.WorklistEntry>> queryDICOMWorklist(
            @RequestParam String modality,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledDate) {
        log.info("Querying DICOM worklist for modality: {}, date: {}", modality, scheduledDate);
        
        List<DICOMWorklistService.WorklistEntry> entries = 
            dicomWorklistService.queryWorklist(modality, scheduledDate);
        
        return ResponseEntity.ok(entries);
    }
    
    // ========== Scheduling Integration ==========
    
    @PostMapping("/{orderId}/schedule")
    @Operation(summary = "Schedule imaging order", 
               description = "Schedule an imaging order for a specific date/time using the scheduling service")
    public ResponseEntity<Map<String, Object>> scheduleImagingOrder(
            @PathVariable UUID orderId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledDate,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Scheduling imaging order {} for date: {}", orderId, scheduledDate);
        
        com.easyops.hospital.entity.ImagingOrder imagingOrder = 
            imagingOrderService.getImagingOrderEntityById(orderId);
        
            ImagingSchedulingService.SchedulingResult result = 
            imagingSchedulingService.scheduleOrder(imagingOrder, scheduledDate);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("status", result.getStatus());
        response.put("message", result.getMessage());
        response.put("scheduledDate", result.getScheduledDate());
        response.put("appointmentId", result.getAppointmentId());
        response.put("scheduledAt", result.getScheduledAt());
        
        if (result.isSuccess()) {
            // Update order with scheduled date
            if (result.getScheduledDate() != null) {
                imagingOrder.setScheduledDate(result.getScheduledDate());
                imagingOrder.setScheduledTime(result.getScheduledDate().toLocalTime());
            } else {
                imagingOrder.setScheduledDate(scheduledDate);
                imagingOrder.setScheduledTime(scheduledDate.toLocalTime());
            }
            imagingOrderService.updateImagingOrder(imagingOrder.getOrderId(), 
                com.easyops.hospital.dto.request.ImagingOrderRequest.builder()
                    .scheduledDate(result.getScheduledDate() != null ? 
                        result.getScheduledDate() : scheduledDate)
                    .scheduledTime(result.getScheduledDate() != null ?
                        result.getScheduledDate().toLocalTime() : scheduledDate.toLocalTime())
                    .build(), userId);
            
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/{orderId}/cancel-appointment")
    @Operation(summary = "Cancel scheduled appointment", 
               description = "Cancel a scheduled appointment for an imaging order")
    public ResponseEntity<Map<String, Object>> cancelAppointment(@PathVariable UUID orderId) {
        log.info("Cancelling appointment for imaging order {}", orderId);
        
        com.easyops.hospital.entity.ImagingOrder imagingOrder = 
            imagingOrderService.getImagingOrderEntityById(orderId);
        
        ImagingSchedulingService.SchedulingResult result = 
            imagingSchedulingService.cancelAppointment(imagingOrder);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("status", result.getStatus());
        response.put("message", result.getMessage());
        response.put("scheduledAt", result.getScheduledAt());
        
        return ResponseEntity.ok(response);
    }
}
