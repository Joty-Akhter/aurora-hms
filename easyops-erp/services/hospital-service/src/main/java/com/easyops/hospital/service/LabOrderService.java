package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.LabOrderRequest;
import com.easyops.hospital.dto.response.LabOrderResponse;
import com.easyops.hospital.entity.LabOrder;
import com.easyops.hospital.entity.Patient;
import com.easyops.hospital.repository.LabOrderRepository;
import com.easyops.hospital.repository.PatientRepository;
import com.easyops.hospital.util.PatientDisplayName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabOrderService {
    
    private final LabOrderRepository labOrderRepository;
    private final PatientRepository patientRepository;
    private final LISTransmissionService lisTransmissionService;
    
    // ========== Lab Order CRUD Operations ==========
    
    /**
     * Create a new lab order
     */
    @Transactional
    public LabOrderResponse createLabOrder(LabOrderRequest request, UUID userId) {
        log.info("Creating lab order for patient: {}", request.getPatientId());
        
        // Verify patient exists
        Patient patient = patientRepository.findById(request.getPatientId())
            .orElseThrow(() -> new RuntimeException("Patient not found: " + request.getPatientId()));
        
        // Generate order number if not provided
        String orderNumber = request.getOrderNumber();
        if (orderNumber == null || orderNumber.isEmpty()) {
            orderNumber = generateOrderNumber();
        }
        
        // Build lab order entity
        LabOrder labOrder = LabOrder.builder()
            .patient(patient)
            .encounterId(request.getEncounterId())
            .orderNumber(orderNumber)
            .orderDate(LocalDateTime.now())
            .testName(request.getTestName())
            .loincCode(request.getLoincCode())
            .testCategory(request.getTestCategory())
            .testType(request.getTestType())
            .isTestPanel(request.getIsTestPanel() != null ? request.getIsTestPanel() : false)
            .panelName(request.getPanelName())
            .clinicalIndication(request.getClinicalIndication())
            .priority(request.getPriority() != null ? request.getPriority() : LabOrder.OrderPriority.ROUTINE)
            .specialInstructions(request.getSpecialInstructions())
            .fastingRequired(request.getFastingRequired() != null ? request.getFastingRequired() : false)
            .patientPreparationInstructions(request.getPatientPreparationInstructions())
            .orderingProviderId(request.getOrderingProviderId() != null ? request.getOrderingProviderId() : userId)
            .orderingProviderName(request.getOrderingProviderName())
            .orderingFacilityId(request.getOrderingFacilityId())
            .orderingFacilityName(request.getOrderingFacilityName())
            .laboratoryId(request.getLaboratoryId())
            .laboratoryName(request.getLaboratoryName())
            .orderStatus(LabOrder.OrderStatus.PENDING)
            .createdBy(userId)
            .build();
        
        LabOrder savedOrder = labOrderRepository.save(labOrder);
        log.info("Created lab order: {} for patient: {}", savedOrder.getOrderId(), patient.getPatientId());
        
        return mapToResponse(savedOrder);
    }
    
    /**
     * Get lab order by ID
     */
    public LabOrderResponse getLabOrderById(UUID orderId) {
        LabOrder labOrder = labOrderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Lab order not found: " + orderId));
        return mapToResponse(labOrder);
    }
    
    /**
     * Get lab order entity by ID (for internal use, e.g., HL7 message generation)
     */
    public LabOrder getLabOrderEntityById(UUID orderId) {
        return labOrderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Lab order not found: " + orderId));
    }
    
    /**
     * Get lab order by order number
     */
    public LabOrderResponse getLabOrderByNumber(String orderNumber) {
        LabOrder labOrder = labOrderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new RuntimeException("Lab order not found: " + orderNumber));
        return mapToResponse(labOrder);
    }
    
    /**
     * Get all lab orders for a patient
     */
    public List<LabOrderResponse> getLabOrdersByPatient(UUID patientId) {
        List<LabOrder> orders = labOrderRepository.findByPatientPatientIdOrderByOrderDateDesc(patientId);
        return orders.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get pending lab orders for a patient
     */
    public List<LabOrderResponse> getPendingLabOrdersByPatient(UUID patientId) {
        List<LabOrder> orders = labOrderRepository.findPendingOrdersByPatient(patientId);
        return orders.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get completed lab orders for a patient
     */
    public List<LabOrderResponse> getCompletedLabOrdersByPatient(UUID patientId) {
        List<LabOrder> orders = labOrderRepository.findCompletedOrdersByPatient(patientId);
        return orders.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all lab orders
     */
    public List<LabOrderResponse> getAllLabOrders() {
        List<LabOrder> orders = labOrderRepository.findAll();
        return orders.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Update a lab order (only if status is PENDING)
     */
    @Transactional
    public LabOrderResponse updateLabOrder(UUID orderId, LabOrderRequest request, UUID userId) {
        log.info("Updating lab order: {}", orderId);
        
        LabOrder labOrder = labOrderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Lab order not found: " + orderId));
        
        // Only allow updates to PENDING orders
        if (labOrder.getOrderStatus() != LabOrder.OrderStatus.PENDING) {
            throw new RuntimeException("Cannot update lab order that is not in PENDING status");
        }
        
        // Update fields
        if (request.getTestName() != null) labOrder.setTestName(request.getTestName());
        if (request.getLoincCode() != null) labOrder.setLoincCode(request.getLoincCode());
        if (request.getTestCategory() != null) labOrder.setTestCategory(request.getTestCategory());
        if (request.getTestType() != null) labOrder.setTestType(request.getTestType());
        if (request.getIsTestPanel() != null) labOrder.setIsTestPanel(request.getIsTestPanel());
        if (request.getPanelName() != null) labOrder.setPanelName(request.getPanelName());
        if (request.getClinicalIndication() != null) labOrder.setClinicalIndication(request.getClinicalIndication());
        if (request.getPriority() != null) labOrder.setPriority(request.getPriority());
        if (request.getSpecialInstructions() != null) labOrder.setSpecialInstructions(request.getSpecialInstructions());
        if (request.getFastingRequired() != null) labOrder.setFastingRequired(request.getFastingRequired());
        if (request.getPatientPreparationInstructions() != null) 
            labOrder.setPatientPreparationInstructions(request.getPatientPreparationInstructions());
        if (request.getOrderingProviderId() != null) labOrder.setOrderingProviderId(request.getOrderingProviderId());
        if (request.getOrderingProviderName() != null) labOrder.setOrderingProviderName(request.getOrderingProviderName());
        if (request.getLaboratoryId() != null) labOrder.setLaboratoryId(request.getLaboratoryId());
        if (request.getLaboratoryName() != null) labOrder.setLaboratoryName(request.getLaboratoryName());
        
        labOrder.setUpdatedBy(userId);
        
        LabOrder updatedOrder = labOrderRepository.save(labOrder);
        log.info("Updated lab order: {}", updatedOrder.getOrderId());
        
        return mapToResponse(updatedOrder);
    }
    
    /**
     * Send lab order to laboratory
     */
    @Transactional
    public LabOrderResponse sendLabOrder(UUID orderId, UUID userId) {
        log.info("Sending lab order: {}", orderId);
        
        LabOrder labOrder = labOrderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Lab order not found: " + orderId));
        
        if (labOrder.getOrderStatus() != LabOrder.OrderStatus.PENDING) {
            throw new RuntimeException("Cannot send lab order that is not in PENDING status");
        }
        
        // Attempt to transmit to LIS
        LISTransmissionService.TransmissionResult transmissionResult = 
            lisTransmissionService.transmitOrder(labOrder);
        
        // Update order status based on transmission result
        if (transmissionResult.isSuccess()) {
            labOrder.setOrderStatus(LabOrder.OrderStatus.SENT);
            labOrder.setTransmissionStatus("SENT");
            labOrder.setTransmissionMethod(transmissionResult.getTransmissionMethod());
            log.info("Lab order {} successfully transmitted to LIS", orderId);
        } else {
            // If transmission fails but is disabled or misconfigured, still mark as SENT
            // (allows workflow to continue when LIS integration is not set up)
            if ("DISABLED".equals(transmissionResult.getStatus()) || 
                "CONFIGURATION_ERROR".equals(transmissionResult.getStatus())) {
                labOrder.setOrderStatus(LabOrder.OrderStatus.SENT);
                labOrder.setTransmissionStatus("SENT_MANUAL");
                labOrder.setTransmissionMethod(transmissionResult.getTransmissionMethod());
                log.warn("Lab order {} marked as SENT but LIS transmission failed: {}", 
                    orderId, transmissionResult.getMessage());
            } else {
                // Transmission attempted but failed - mark as transmission error
                labOrder.setOrderStatus(LabOrder.OrderStatus.SENT);
                labOrder.setTransmissionStatus("TRANSMISSION_ERROR");
                labOrder.setTransmissionMethod(transmissionResult.getTransmissionMethod());
                log.error("Lab order {} transmission to LIS failed: {}", 
                    orderId, transmissionResult.getMessage());
            }
        }
        
        labOrder.setSentDate(LocalDateTime.now());
        labOrder.setTransmissionDate(transmissionResult.getTransmittedAt());
        labOrder.setUpdatedBy(userId);
        
        LabOrder sentOrder = labOrderRepository.save(labOrder);
        log.info("Sent lab order: {} with transmission status: {}", 
            sentOrder.getOrderId(), sentOrder.getTransmissionStatus());
        
        return mapToResponse(sentOrder);
    }
    
    /**
     * Cancel a lab order
     */
    @Transactional
    public LabOrderResponse cancelLabOrder(UUID orderId, String reason, UUID userId) {
        log.info("Cancelling lab order: {}", orderId);
        
        LabOrder labOrder = labOrderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Lab order not found: " + orderId));
        
        if (labOrder.getOrderStatus() == LabOrder.OrderStatus.CANCELLED) {
            throw new RuntimeException("Lab order is already cancelled");
        }
        
        if (labOrder.getOrderStatus() == LabOrder.OrderStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel a completed lab order");
        }
        
        labOrder.setOrderStatus(LabOrder.OrderStatus.CANCELLED);
        labOrder.setCancelledDate(LocalDateTime.now());
        labOrder.setCancellationReason(reason != null ? reason : "Cancelled by provider");
        labOrder.setUpdatedBy(userId);
        
        LabOrder cancelledOrder = labOrderRepository.save(labOrder);
        log.info("Cancelled lab order: {}", cancelledOrder.getOrderId());
        
        return mapToResponse(cancelledOrder);
    }
    
    /**
     * Reschedule a lab order
     */
    @Transactional
    public LabOrderResponse rescheduleLabOrder(UUID orderId, LocalDateTime newScheduledDate, UUID userId) {
        log.info("Rescheduling lab order: {} to {}", orderId, newScheduledDate);
        
        LabOrder labOrder = labOrderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Lab order not found: " + orderId));
        
        // Only allow rescheduling of PENDING or SENT orders
        if (labOrder.getOrderStatus() != LabOrder.OrderStatus.PENDING && 
            labOrder.getOrderStatus() != LabOrder.OrderStatus.SENT) {
            throw new RuntimeException("Cannot reschedule lab order that is not in PENDING or SENT status");
        }
        
        if (newScheduledDate == null) {
            throw new RuntimeException("Scheduled date is required for rescheduling");
        }
        
        if (newScheduledDate.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Scheduled date cannot be in the past");
        }
        
        labOrder.setScheduledDate(newScheduledDate);
        labOrder.setUpdatedBy(userId);
        
        // If order was SENT, reset status to PENDING
        if (labOrder.getOrderStatus() == LabOrder.OrderStatus.SENT) {
            labOrder.setOrderStatus(LabOrder.OrderStatus.PENDING);
            labOrder.setSentDate(null);
            labOrder.setTransmissionStatus(null);
            labOrder.setTransmissionDate(null);
        }
        
        LabOrder rescheduledOrder = labOrderRepository.save(labOrder);
        log.info("Rescheduled lab order: {} to {}", rescheduledOrder.getOrderId(), newScheduledDate);
        
        return mapToResponse(rescheduledOrder);
    }
    
    /**
     * Delete a lab order (only if status is PENDING)
     */
    @Transactional
    public void deleteLabOrder(UUID orderId) {
        log.info("Deleting lab order: {}", orderId);
        
        LabOrder labOrder = labOrderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Lab order not found: " + orderId));
        
        // Only allow deletion of PENDING orders
        if (labOrder.getOrderStatus() != LabOrder.OrderStatus.PENDING) {
            throw new RuntimeException("Cannot delete lab order that is not in PENDING status");
        }
        
        labOrderRepository.delete(labOrder);
        log.info("Deleted lab order: {}", orderId);
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Generate a unique order number
     */
    private String generateOrderNumber() {
        String prefix = "LAB";
        long timestamp = System.currentTimeMillis();
        String suffix = String.valueOf(timestamp).substring(7); // Last 6 digits
        return prefix + suffix;
    }
    
    /**
     * Map LabOrder entity to LabOrderResponse DTO
     */
    private LabOrderResponse mapToResponse(LabOrder labOrder) {
        Patient patient = labOrder.getPatient();
        return LabOrderResponse.builder()
            .orderId(labOrder.getOrderId())
            .patientId(patient.getPatientId())
            .patientName(PatientDisplayName.of(patient))
            .mrn(patient.getMrn())
            .encounterId(labOrder.getEncounterId())
            .organizationId(labOrder.getOrganizationId())
            .orderNumber(labOrder.getOrderNumber())
            .orderDate(labOrder.getOrderDate())
            .scheduledDate(labOrder.getScheduledDate())
            .orderingProviderId(labOrder.getOrderingProviderId())
            .orderingProviderName(labOrder.getOrderingProviderName())
            .orderingFacilityId(labOrder.getOrderingFacilityId())
            .orderingFacilityName(labOrder.getOrderingFacilityName())
            .testName(labOrder.getTestName())
            .loincCode(labOrder.getLoincCode())
            .testCategory(labOrder.getTestCategory())
            .testType(labOrder.getTestType())
            .isTestPanel(labOrder.getIsTestPanel())
            .panelName(labOrder.getPanelName())
            .clinicalIndication(labOrder.getClinicalIndication())
            .priority(labOrder.getPriority())
            .specialInstructions(labOrder.getSpecialInstructions())
            .fastingRequired(labOrder.getFastingRequired())
            .patientPreparationInstructions(labOrder.getPatientPreparationInstructions())
            .orderStatus(labOrder.getOrderStatus())
            .sentDate(labOrder.getSentDate())
            .collectedDate(labOrder.getCollectedDate())
            .cancelledDate(labOrder.getCancelledDate())
            .cancellationReason(labOrder.getCancellationReason())
            .transmissionMethod(labOrder.getTransmissionMethod())
            .transmissionStatus(labOrder.getTransmissionStatus())
            .transmissionDate(labOrder.getTransmissionDate())
            .laboratoryId(labOrder.getLaboratoryId())
            .laboratoryName(labOrder.getLaboratoryName())
            .createdAt(labOrder.getCreatedAt())
            .updatedAt(labOrder.getUpdatedAt())
            .createdBy(labOrder.getCreatedBy())
            .updatedBy(labOrder.getUpdatedBy())
            .build();
    }
}
