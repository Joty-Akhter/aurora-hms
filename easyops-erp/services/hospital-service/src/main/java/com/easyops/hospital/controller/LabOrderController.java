package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.LabOrderRequest;
import com.easyops.hospital.dto.response.HL7MessageResponse;
import com.easyops.hospital.dto.response.LabOrderResponse;
import com.easyops.hospital.service.HL7FhirServiceRequestGenerator;
import com.easyops.hospital.service.HL7V2OrmMessageGenerator;
import com.easyops.hospital.service.LabOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lab-orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Laboratory Order Management", description = "APIs for laboratory test ordering and management")
public class LabOrderController {
    
    private final LabOrderService labOrderService;
    private final HL7V2OrmMessageGenerator hl7V2OrmMessageGenerator;
    private final HL7FhirServiceRequestGenerator hl7FhirServiceRequestGenerator;
    
    // ========== Lab Order CRUD Operations ==========
    
    @PostMapping
    @Operation(summary = "Create a new lab order", description = "Create a new laboratory test order")
    public ResponseEntity<LabOrderResponse> createLabOrder(
            @Valid @RequestBody LabOrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Creating lab order for patient: {}", request.getPatientId());
        if (userId == null) userId = UUID.randomUUID();
        LabOrderResponse response = labOrderService.createLabOrder(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{orderId}")
    @Operation(summary = "Get lab order by ID", description = "Retrieve a lab order by its ID")
    public ResponseEntity<LabOrderResponse> getLabOrderById(@PathVariable UUID orderId) {
        LabOrderResponse response = labOrderService.getLabOrderById(orderId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get lab order by number", description = "Retrieve a lab order by its order number")
    public ResponseEntity<LabOrderResponse> getLabOrderByNumber(@PathVariable String orderNumber) {
        LabOrderResponse response = labOrderService.getLabOrderByNumber(orderNumber);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all lab orders", description = "Retrieve all lab orders")
    public ResponseEntity<List<LabOrderResponse>> getAllLabOrders() {
        List<LabOrderResponse> responses = labOrderService.getAllLabOrders();
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}")
    @Operation(summary = "Get all lab orders for a patient", description = "Retrieve all lab orders for a patient, ordered by date")
    public ResponseEntity<List<LabOrderResponse>> getLabOrdersByPatient(@PathVariable UUID patientId) {
        List<LabOrderResponse> responses = labOrderService.getLabOrdersByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/pending")
    @Operation(summary = "Get pending lab orders", description = "Retrieve pending lab orders for a patient")
    public ResponseEntity<List<LabOrderResponse>> getPendingLabOrders(@PathVariable UUID patientId) {
        List<LabOrderResponse> responses = labOrderService.getPendingLabOrdersByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/completed")
    @Operation(summary = "Get completed lab orders", description = "Retrieve completed lab orders for a patient")
    public ResponseEntity<List<LabOrderResponse>> getCompletedLabOrders(@PathVariable UUID patientId) {
        List<LabOrderResponse> responses = labOrderService.getCompletedLabOrdersByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{orderId}")
    @Operation(summary = "Update lab order", description = "Update a lab order (only allowed for PENDING status)")
    public ResponseEntity<LabOrderResponse> updateLabOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody LabOrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Updating lab order: {}", orderId);
        if (userId == null) userId = UUID.randomUUID();
        LabOrderResponse response = labOrderService.updateLabOrder(orderId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{orderId}/send")
    @Operation(summary = "Send lab order", description = "Send lab order to laboratory (change status to SENT)")
    public ResponseEntity<LabOrderResponse> sendLabOrder(
            @PathVariable UUID orderId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Sending lab order: {}", orderId);
        if (userId == null) userId = UUID.randomUUID();
        LabOrderResponse response = labOrderService.sendLabOrder(orderId, userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel lab order", description = "Cancel a lab order with reason")
    public ResponseEntity<LabOrderResponse> cancelLabOrder(
            @PathVariable UUID orderId,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Cancelling lab order: {}", orderId);
        if (userId == null) userId = UUID.randomUUID();
        if (reason == null) reason = "Cancelled by provider";
        LabOrderResponse response = labOrderService.cancelLabOrder(orderId, reason, userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{orderId}/reschedule")
    @Operation(summary = "Reschedule lab order", description = "Reschedule a lab order to a new date/time")
    public ResponseEntity<LabOrderResponse> rescheduleLabOrder(
            @PathVariable UUID orderId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime scheduledDate,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Rescheduling lab order: {} to {}", orderId, scheduledDate);
        if (userId == null) userId = UUID.randomUUID();
        LabOrderResponse response = labOrderService.rescheduleLabOrder(orderId, scheduledDate, userId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{orderId}")
    @Operation(summary = "Delete lab order", description = "Delete a lab order (only allowed for PENDING status)")
    public ResponseEntity<Void> deleteLabOrder(@PathVariable UUID orderId) {
        log.info("Deleting lab order: {}", orderId);
        labOrderService.deleteLabOrder(orderId);
        return ResponseEntity.noContent().build();
    }
    
    // ========== HL7 Message Generation ==========
    
    @GetMapping("/{orderId}/hl7-v2-orm")
    @Operation(summary = "Generate HL7 V2 ORM message", description = "Generate HL7 V2 ORM (Order Message) for transmission to LIS")
    public ResponseEntity<HL7MessageResponse> generateHL7V2OrmMessage(@PathVariable UUID orderId) {
        log.info("Generating HL7 V2 ORM message for order: {}", orderId);
        LabOrderResponse orderResponse = labOrderService.getLabOrderById(orderId);
        
        // Get the full order entity for message generation
        com.easyops.hospital.entity.LabOrder labOrder = labOrderService.getLabOrderEntityById(orderId);
        
        String ormMessage = hl7V2OrmMessageGenerator.generateOrmMessage(labOrder);
        
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
    @Operation(summary = "Generate HL7 FHIR ServiceRequest", description = "Generate HL7 FHIR ServiceRequest resource for the lab order")
    public ResponseEntity<HL7MessageResponse> generateHL7FhirServiceRequest(@PathVariable UUID orderId) {
        log.info("Generating HL7 FHIR ServiceRequest for order: {}", orderId);
        LabOrderResponse orderResponse = labOrderService.getLabOrderById(orderId);
        
        // Get the full order entity for message generation
        com.easyops.hospital.entity.LabOrder labOrder = labOrderService.getLabOrderEntityById(orderId);
        
        java.util.Map<String, Object> serviceRequest = hl7FhirServiceRequestGenerator.generateServiceRequest(labOrder);
        
        HL7MessageResponse response = HL7MessageResponse.builder()
            .messageType("HL7_FHIR_SERVICEREQUEST")
            .messageFormat("JSON")
            .messageResource(serviceRequest)
            .orderId(orderId.toString())
            .orderNumber(orderResponse.getOrderNumber())
            .build();
        
        return ResponseEntity.ok(response);
    }
}
