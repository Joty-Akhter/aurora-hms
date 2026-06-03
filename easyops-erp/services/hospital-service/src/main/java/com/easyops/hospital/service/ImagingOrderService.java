package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.ImagingOrderRequest;
import com.easyops.hospital.dto.response.ImagingOrderResponse;
import com.easyops.hospital.entity.ImagingOrder;
import com.easyops.hospital.entity.Patient;
import com.easyops.hospital.repository.ImagingOrderRepository;
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
public class ImagingOrderService {
    
    private final ImagingOrderRepository imagingOrderRepository;
    private final PatientRepository patientRepository;
    private final RISPACSTransmissionService risPacsTransmissionService;
    private final DICOMWorklistService dicomWorklistService;
    private final ImagingSchedulingService imagingSchedulingService;
    
    // ========== Imaging Order CRUD Operations ==========
    
    /**
     * Create a new imaging order
     */
    @Transactional
    public ImagingOrderResponse createImagingOrder(ImagingOrderRequest request, UUID userId) {
        log.info("Creating imaging order for patient: {}", request.getPatientId());
        
        // Verify patient exists
        Patient patient = patientRepository.findById(request.getPatientId())
            .orElseThrow(() -> new RuntimeException("Patient not found: " + request.getPatientId()));
        
        // Generate order number if not provided
        String orderNumber = request.getOrderNumber();
        if (orderNumber == null || orderNumber.isEmpty()) {
            orderNumber = generateOrderNumber();
        }
        
        // Build imaging order entity
        ImagingOrder imagingOrder = ImagingOrder.builder()
            .patient(patient)
            .encounterId(request.getEncounterId())
            .organizationId(patient.getOrganizationId())
            .orderNumber(orderNumber)
            .orderDate(LocalDateTime.now())
            .studyType(request.getStudyType())
            .studyModality(request.getStudyModality())
            .studyDescription(request.getStudyDescription())
            .cptCode(request.getCptCode())
            .bodyPart(request.getBodyPart())
            .laterality(request.getLaterality())
            .specificAnatomicalSite(request.getSpecificAnatomicalSite())
            .viewProjection(request.getViewProjection())
            .clinicalIndication(request.getClinicalIndication())
            .priority(request.getPriority() != null ? request.getPriority() : ImagingOrder.OrderPriority.ROUTINE)
            .specialInstructions(request.getSpecialInstructions())
            .contrastRequired(request.getContrastRequired() != null ? request.getContrastRequired() : false)
            .contrastType(request.getContrastType())
            .patientPreparationRequired(request.getPatientPreparationRequired() != null ? request.getPatientPreparationRequired() : false)
            .patientPreparationInstructions(request.getPatientPreparationInstructions())
            .sedationRequired(request.getSedationRequired() != null ? request.getSedationRequired() : false)
            .orderingProviderId(request.getOrderingProviderId() != null ? request.getOrderingProviderId() : userId)
            .orderingProviderName(request.getOrderingProviderName())
            .orderingFacilityId(request.getOrderingFacilityId())
            .orderingFacilityName(request.getOrderingFacilityName())
            .radiologyFacilityId(request.getRadiologyFacilityId())
            .radiologyFacilityName(request.getRadiologyFacilityName())
            .orderStatus(ImagingOrder.OrderStatus.PENDING)
            .createdBy(userId)
            .build();
        
        // If scheduled date is provided, use scheduling service
        if (request.getScheduledDate() != null) {
            ImagingSchedulingService.SchedulingResult schedulingResult = 
                imagingSchedulingService.scheduleOrder(imagingOrder, request.getScheduledDate());
            
            if (schedulingResult.isSuccess() && schedulingResult.getScheduledDate() != null) {
                imagingOrder.setScheduledDate(schedulingResult.getScheduledDate());
                if (schedulingResult.getScheduledDate().toLocalTime() != null) {
                    imagingOrder.setScheduledTime(schedulingResult.getScheduledDate().toLocalTime());
                } else if (request.getScheduledTime() != null) {
                    imagingOrder.setScheduledTime(request.getScheduledTime());
                }
            } else {
                // Use requested date even if scheduling service fails
                imagingOrder.setScheduledDate(request.getScheduledDate());
                imagingOrder.setScheduledTime(request.getScheduledTime());
                if (!schedulingResult.isSuccess() && !"VALIDATION_ERROR".equals(schedulingResult.getStatus())) {
                    log.warn("Scheduling service failed for new order, using requested date: {}", 
                        schedulingResult.getMessage());
                }
            }
        } else {
            imagingOrder.setScheduledDate(request.getScheduledDate());
            imagingOrder.setScheduledTime(request.getScheduledTime());
        }
        
        ImagingOrder savedOrder = imagingOrderRepository.save(imagingOrder);
        log.info("Created imaging order: {} for patient: {}", savedOrder.getOrderId(), patient.getPatientId());
        
        return mapToResponse(savedOrder);
    }
    
    /**
     * Get imaging order by ID
     */
    public ImagingOrderResponse getImagingOrderById(UUID orderId) {
        ImagingOrder imagingOrder = imagingOrderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Imaging order not found: " + orderId));
        return mapToResponse(imagingOrder);
    }
    
    /**
     * Get imaging order entity by ID (for internal use, e.g., HL7 message generation)
     */
    public ImagingOrder getImagingOrderEntityById(UUID orderId) {
        return imagingOrderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Imaging order not found: " + orderId));
    }
    
    /**
     * Get imaging order by order number
     */
    public ImagingOrderResponse getImagingOrderByNumber(String orderNumber) {
        ImagingOrder imagingOrder = imagingOrderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new RuntimeException("Imaging order not found: " + orderNumber));
        return mapToResponse(imagingOrder);
    }
    
    /**
     * Get all imaging orders for a patient
     */
    public List<ImagingOrderResponse> getImagingOrdersByPatient(UUID patientId) {
        List<ImagingOrder> orders = imagingOrderRepository.findByPatientPatientIdOrderByOrderDateDesc(patientId);
        return orders.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get pending imaging orders for a patient
     */
    public List<ImagingOrderResponse> getPendingImagingOrdersByPatient(UUID patientId) {
        List<ImagingOrder> orders = imagingOrderRepository.findPendingOrdersByPatient(patientId);
        return orders.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get completed imaging orders for a patient
     */
    public List<ImagingOrderResponse> getCompletedImagingOrdersByPatient(UUID patientId) {
        List<ImagingOrder> orders = imagingOrderRepository.findCompletedOrdersByPatient(patientId);
        return orders.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all imaging orders
     */
    public List<ImagingOrderResponse> getAllImagingOrders() {
        List<ImagingOrder> orders = imagingOrderRepository.findAll();
        return orders.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Update an imaging order (only if status is PENDING)
     */
    @Transactional
    public ImagingOrderResponse updateImagingOrder(UUID orderId, ImagingOrderRequest request, UUID userId) {
        log.info("Updating imaging order: {}", orderId);
        
        ImagingOrder imagingOrder = imagingOrderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Imaging order not found: " + orderId));
        
        // Only allow updates to PENDING orders
        if (imagingOrder.getOrderStatus() != ImagingOrder.OrderStatus.PENDING) {
            throw new RuntimeException("Cannot update imaging order that is not in PENDING status");
        }
        
        // Update fields
        if (request.getStudyType() != null) imagingOrder.setStudyType(request.getStudyType());
        if (request.getStudyModality() != null) imagingOrder.setStudyModality(request.getStudyModality());
        if (request.getStudyDescription() != null) imagingOrder.setStudyDescription(request.getStudyDescription());
        if (request.getCptCode() != null) imagingOrder.setCptCode(request.getCptCode());
        if (request.getBodyPart() != null) imagingOrder.setBodyPart(request.getBodyPart());
        if (request.getLaterality() != null) imagingOrder.setLaterality(request.getLaterality());
        if (request.getSpecificAnatomicalSite() != null) imagingOrder.setSpecificAnatomicalSite(request.getSpecificAnatomicalSite());
        if (request.getViewProjection() != null) imagingOrder.setViewProjection(request.getViewProjection());
        if (request.getClinicalIndication() != null) imagingOrder.setClinicalIndication(request.getClinicalIndication());
        if (request.getPriority() != null) imagingOrder.setPriority(request.getPriority());
        if (request.getSpecialInstructions() != null) imagingOrder.setSpecialInstructions(request.getSpecialInstructions());
        if (request.getContrastRequired() != null) imagingOrder.setContrastRequired(request.getContrastRequired());
        if (request.getContrastType() != null) imagingOrder.setContrastType(request.getContrastType());
        if (request.getPatientPreparationRequired() != null) imagingOrder.setPatientPreparationRequired(request.getPatientPreparationRequired());
        if (request.getPatientPreparationInstructions() != null) 
            imagingOrder.setPatientPreparationInstructions(request.getPatientPreparationInstructions());
        if (request.getSedationRequired() != null) imagingOrder.setSedationRequired(request.getSedationRequired());
        if (request.getOrderingProviderId() != null) imagingOrder.setOrderingProviderId(request.getOrderingProviderId());
        if (request.getOrderingProviderName() != null) imagingOrder.setOrderingProviderName(request.getOrderingProviderName());
        if (request.getScheduledDate() != null) imagingOrder.setScheduledDate(request.getScheduledDate());
        if (request.getScheduledTime() != null) imagingOrder.setScheduledTime(request.getScheduledTime());
        if (request.getRadiologyFacilityId() != null) imagingOrder.setRadiologyFacilityId(request.getRadiologyFacilityId());
        if (request.getRadiologyFacilityName() != null) imagingOrder.setRadiologyFacilityName(request.getRadiologyFacilityName());
        
        imagingOrder.setUpdatedBy(userId);
        
        ImagingOrder updatedOrder = imagingOrderRepository.save(imagingOrder);
        log.info("Updated imaging order: {}", updatedOrder.getOrderId());
        
        return mapToResponse(updatedOrder);
    }
    
    /**
     * Send imaging order to radiology
     */
    @Transactional
    public ImagingOrderResponse sendImagingOrder(UUID orderId, UUID userId) {
        log.info("Sending imaging order: {}", orderId);
        
        ImagingOrder imagingOrder = imagingOrderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Imaging order not found: " + orderId));
        
        if (imagingOrder.getOrderStatus() != ImagingOrder.OrderStatus.PENDING) {
            throw new RuntimeException("Cannot send imaging order that is not in PENDING status");
        }
        
        // Attempt to transmit to RIS/PACS
        RISPACSTransmissionService.TransmissionResult transmissionResult = 
            risPacsTransmissionService.transmitOrder(imagingOrder);
        
        // Update order status based on transmission result
        if (transmissionResult.isSuccess()) {
            imagingOrder.setOrderStatus(ImagingOrder.OrderStatus.SENT);
            imagingOrder.setTransmissionStatus("SENT");
            imagingOrder.setTransmissionMethod(transmissionResult.getTransmissionMethod());
            log.info("Imaging order {} successfully transmitted to RIS/PACS", orderId);
        } else {
            // If transmission fails but is disabled or misconfigured, still mark as SENT
            // (allows workflow to continue when RIS/PACS integration is not set up)
            if ("DISABLED".equals(transmissionResult.getStatus()) || 
                "CONFIGURATION_ERROR".equals(transmissionResult.getStatus())) {
                imagingOrder.setOrderStatus(ImagingOrder.OrderStatus.SENT);
                imagingOrder.setTransmissionStatus("SENT_MANUAL");
                imagingOrder.setTransmissionMethod(transmissionResult.getTransmissionMethod() != null ? 
                    transmissionResult.getTransmissionMethod() : "ELECTRONIC");
                log.warn("Imaging order {} marked as SENT but RIS/PACS transmission failed: {}", 
                    orderId, transmissionResult.getMessage());
            } else {
                // Transmission attempted but failed - mark as transmission error
                imagingOrder.setOrderStatus(ImagingOrder.OrderStatus.SENT);
                imagingOrder.setTransmissionStatus("TRANSMISSION_ERROR");
                imagingOrder.setTransmissionMethod(transmissionResult.getTransmissionMethod());
                log.error("Imaging order {} transmission to RIS/PACS failed: {}", 
                    orderId, transmissionResult.getMessage());
            }
        }
        
        imagingOrder.setSentDate(LocalDateTime.now());
        imagingOrder.setTransmissionDate(transmissionResult.getTransmittedAt());
        imagingOrder.setUpdatedBy(userId);
        
        // Submit to DICOM worklist if scheduled
        if (imagingOrder.getScheduledDate() != null) {
            try {
                DICOMWorklistService.WorklistSubmissionResult worklistResult = 
                    dicomWorklistService.submitWorklistEntry(imagingOrder);
                if (worklistResult.isSuccess()) {
                    log.info("Imaging order {} successfully added to DICOM worklist", orderId);
                } else {
                    log.warn("Failed to add imaging order {} to DICOM worklist: {}", 
                        orderId, worklistResult.getMessage());
                }
            } catch (Exception e) {
                log.error("Exception while submitting to DICOM worklist for order {}: {}", 
                    orderId, e.getMessage(), e);
                // Don't fail the order send if worklist submission fails
            }
        }
        
        ImagingOrder sentOrder = imagingOrderRepository.save(imagingOrder);
        log.info("Sent imaging order: {} with transmission status: {}", 
            sentOrder.getOrderId(), sentOrder.getTransmissionStatus());
        
        return mapToResponse(sentOrder);
    }
    
    /**
     * Cancel an imaging order
     */
    @Transactional
    public ImagingOrderResponse cancelImagingOrder(UUID orderId, String reason, UUID userId) {
        log.info("Cancelling imaging order: {}", orderId);
        
        ImagingOrder imagingOrder = imagingOrderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Imaging order not found: " + orderId));
        
        if (imagingOrder.getOrderStatus() == ImagingOrder.OrderStatus.CANCELLED) {
            throw new RuntimeException("Imaging order is already cancelled");
        }
        
        if (imagingOrder.getOrderStatus() == ImagingOrder.OrderStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel a completed imaging order");
        }
        
        // Cancel appointment in scheduling service if scheduled
        if (imagingOrder.getScheduledDate() != null) {
            try {
                ImagingSchedulingService.SchedulingResult cancelResult = 
                    imagingSchedulingService.cancelAppointment(imagingOrder);
                if (cancelResult.isSuccess()) {
                    log.info("Successfully cancelled appointment for imaging order {}", orderId);
                } else {
                    log.warn("Failed to cancel appointment in scheduling service: {}", 
                        cancelResult.getMessage());
                }
            } catch (Exception e) {
                log.error("Exception while cancelling appointment for order {}: {}", 
                    orderId, e.getMessage(), e);
                // Don't fail the order cancellation if scheduling service fails
            }
        }
        
        imagingOrder.setOrderStatus(ImagingOrder.OrderStatus.CANCELLED);
        imagingOrder.setCancelledDate(LocalDateTime.now());
        imagingOrder.setCancellationReason(reason != null ? reason : "Cancelled by provider");
        imagingOrder.setUpdatedBy(userId);
        
        ImagingOrder cancelledOrder = imagingOrderRepository.save(imagingOrder);
        log.info("Cancelled imaging order: {}", cancelledOrder.getOrderId());
        
        return mapToResponse(cancelledOrder);
    }
    
    /**
     * Reschedule an imaging order
     */
    @Transactional
    public ImagingOrderResponse rescheduleImagingOrder(UUID orderId, LocalDateTime newScheduledDate, LocalDateTime newScheduledTime, UUID userId) {
        log.info("Rescheduling imaging order: {} to {}", orderId, newScheduledDate);
        
        ImagingOrder imagingOrder = imagingOrderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Imaging order not found: " + orderId));
        
        // Only allow rescheduling of PENDING or SENT orders
        if (imagingOrder.getOrderStatus() != ImagingOrder.OrderStatus.PENDING && 
            imagingOrder.getOrderStatus() != ImagingOrder.OrderStatus.SENT) {
            throw new RuntimeException("Cannot reschedule imaging order that is not in PENDING or SENT status");
        }
        
        if (newScheduledDate == null) {
            throw new RuntimeException("Scheduled date is required for rescheduling");
        }
        
        if (newScheduledDate.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Scheduled date cannot be in the past");
        }
        
        // Call scheduling service to reschedule
        ImagingSchedulingService.SchedulingResult schedulingResult = 
            imagingSchedulingService.rescheduleOrder(imagingOrder, newScheduledDate);
        
        if (!schedulingResult.isSuccess() && !"VALIDATION_ERROR".equals(schedulingResult.getStatus())) {
            // If scheduling service fails (but not validation), log warning but continue
            log.warn("Scheduling service returned error for reschedule, but continuing: {}", 
                schedulingResult.getMessage());
        }
        
        // Use the scheduled date from scheduling result if available, otherwise use requested date
        LocalDateTime finalScheduledDate = schedulingResult.getScheduledDate() != null ? 
            schedulingResult.getScheduledDate() : newScheduledDate;
        
        imagingOrder.setScheduledDate(finalScheduledDate);
        if (newScheduledTime != null) {
            imagingOrder.setScheduledTime(newScheduledTime.toLocalTime());
        } else if (finalScheduledDate != null) {
            imagingOrder.setScheduledTime(finalScheduledDate.toLocalTime());
        }
        imagingOrder.setUpdatedBy(userId);
        
        // If order was SENT, reset status to PENDING
        if (imagingOrder.getOrderStatus() == ImagingOrder.OrderStatus.SENT) {
            imagingOrder.setOrderStatus(ImagingOrder.OrderStatus.PENDING);
            imagingOrder.setSentDate(null);
            imagingOrder.setTransmissionStatus(null);
            imagingOrder.setTransmissionDate(null);
        }
        
        ImagingOrder rescheduledOrder = imagingOrderRepository.save(imagingOrder);
        log.info("Rescheduled imaging order: {} to {}", rescheduledOrder.getOrderId(), finalScheduledDate);
        
        return mapToResponse(rescheduledOrder);
    }
    
    /**
     * Delete an imaging order (only if status is PENDING)
     */
    @Transactional
    public void deleteImagingOrder(UUID orderId) {
        log.info("Deleting imaging order: {}", orderId);
        
        ImagingOrder imagingOrder = imagingOrderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Imaging order not found: " + orderId));
        
        // Only allow deletion of PENDING orders
        if (imagingOrder.getOrderStatus() != ImagingOrder.OrderStatus.PENDING) {
            throw new RuntimeException("Cannot delete imaging order that is not in PENDING status");
        }
        
        imagingOrderRepository.delete(imagingOrder);
        log.info("Deleted imaging order: {}", orderId);
    }
    
    /**
     * Manually transmit imaging order to RIS/PACS (for retry or manual transmission)
     */
    @Transactional
    public RISPACSTransmissionService.TransmissionResult transmitToRISPACS(UUID orderId) {
        log.info("Manually transmitting imaging order {} to RIS/PACS", orderId);
        
        ImagingOrder imagingOrder = imagingOrderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Imaging order not found: " + orderId));
        
        // Attempt to transmit to RIS/PACS
        RISPACSTransmissionService.TransmissionResult transmissionResult = 
            risPacsTransmissionService.transmitOrder(imagingOrder);
        
        // Update order transmission status based on result
        if (transmissionResult.isSuccess()) {
            imagingOrder.setTransmissionStatus("SENT");
            imagingOrder.setTransmissionDate(transmissionResult.getTransmittedAt());
            imagingOrder.setTransmissionMethod(transmissionResult.getTransmissionMethod());
            log.info("Imaging order {} successfully transmitted to RIS/PACS", orderId);
        } else {
            // Update status even on failure to track the attempt
            imagingOrder.setTransmissionStatus(transmissionResult.getStatus());
            imagingOrder.setTransmissionDate(transmissionResult.getTransmittedAt());
            imagingOrder.setTransmissionMethod(transmissionResult.getTransmissionMethod());
            log.warn("Imaging order {} transmission to RIS/PACS failed: {}", 
                orderId, transmissionResult.getMessage());
        }
        
        imagingOrderRepository.save(imagingOrder);
        
        return transmissionResult;
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Generate a unique order number
     */
    private String generateOrderNumber() {
        String prefix = "IMG";
        long timestamp = System.currentTimeMillis();
        String suffix = String.valueOf(timestamp).substring(7); // Last 6 digits
        return prefix + suffix;
    }
    
    /**
     * Map ImagingOrder entity to ImagingOrderResponse DTO
     */
    private ImagingOrderResponse mapToResponse(ImagingOrder imagingOrder) {
        Patient patient = imagingOrder.getPatient();
        return ImagingOrderResponse.builder()
            .orderId(imagingOrder.getOrderId())
            .patientId(patient.getPatientId())
            .patientName(PatientDisplayName.of(patient))
            .mrn(patient.getMrn())
            .encounterId(imagingOrder.getEncounterId())
            .organizationId(imagingOrder.getOrganizationId())
            .orderNumber(imagingOrder.getOrderNumber())
            .orderDate(imagingOrder.getOrderDate())
            .scheduledDate(imagingOrder.getScheduledDate())
            .scheduledTime(imagingOrder.getScheduledTime())
            .orderingProviderId(imagingOrder.getOrderingProviderId())
            .orderingProviderName(imagingOrder.getOrderingProviderName())
            .orderingFacilityId(imagingOrder.getOrderingFacilityId())
            .orderingFacilityName(imagingOrder.getOrderingFacilityName())
            .studyType(imagingOrder.getStudyType())
            .studyModality(imagingOrder.getStudyModality())
            .studyDescription(imagingOrder.getStudyDescription())
            .cptCode(imagingOrder.getCptCode())
            .bodyPart(imagingOrder.getBodyPart())
            .laterality(imagingOrder.getLaterality())
            .specificAnatomicalSite(imagingOrder.getSpecificAnatomicalSite())
            .viewProjection(imagingOrder.getViewProjection())
            .clinicalIndication(imagingOrder.getClinicalIndication())
            .priority(imagingOrder.getPriority())
            .specialInstructions(imagingOrder.getSpecialInstructions())
            .contrastRequired(imagingOrder.getContrastRequired())
            .contrastType(imagingOrder.getContrastType())
            .patientPreparationRequired(imagingOrder.getPatientPreparationRequired())
            .patientPreparationInstructions(imagingOrder.getPatientPreparationInstructions())
            .sedationRequired(imagingOrder.getSedationRequired())
            .orderStatus(imagingOrder.getOrderStatus())
            .sentDate(imagingOrder.getSentDate())
            .cancelledDate(imagingOrder.getCancelledDate())
            .cancellationReason(imagingOrder.getCancellationReason())
            .noShow(imagingOrder.getNoShow())
            .transmissionMethod(imagingOrder.getTransmissionMethod())
            .transmissionStatus(imagingOrder.getTransmissionStatus())
            .transmissionDate(imagingOrder.getTransmissionDate())
            .radiologyFacilityId(imagingOrder.getRadiologyFacilityId())
            .radiologyFacilityName(imagingOrder.getRadiologyFacilityName())
            .orderConfirmationReceived(imagingOrder.getOrderConfirmationReceived())
            .createdAt(imagingOrder.getCreatedAt())
            .updatedAt(imagingOrder.getUpdatedAt())
            .createdBy(imagingOrder.getCreatedBy())
            .updatedBy(imagingOrder.getUpdatedBy())
            .build();
    }
}
