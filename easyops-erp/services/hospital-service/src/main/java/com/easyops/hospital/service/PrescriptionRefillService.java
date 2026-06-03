package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.RefillApprovalRequest;
import com.easyops.hospital.dto.request.RefillDenialRequest;
import com.easyops.hospital.dto.request.RefillModificationRequest;
import com.easyops.hospital.dto.request.RefillRequestRequest;
import com.easyops.hospital.dto.response.*;
import com.easyops.hospital.entity.PrescriptionRefillRequest;
import com.easyops.hospital.entity.*;
import com.easyops.hospital.exception.UnprocessableEntityException;
import com.easyops.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrescriptionRefillService {
    
    private final PrescriptionRefillRequestRepository refillRequestRepository;
    private final PrescriptionRefillRepository refillRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    
    // ========== Refill Request Management ==========
    
    /**
     * Create a refill request
     */
    @Transactional
    public PrescriptionRefillRequestResponse createRefillRequest(RefillRequestRequest request, UUID userId) {
        log.info("Creating refill request for prescription: {}", request.getPrescriptionId());
        
        Prescription prescription = prescriptionRepository.findByIdWithMedications(request.getPrescriptionId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found: " + request.getPrescriptionId()));

        // DEA 21 CFR §1306.12 — Schedule II controlled substances may not be refilled.
        assertRefillsAllowedForDeaSchedule(prescription);

        // Check if prescription has refills remaining
        if (prescription.getRefillsRemaining() == null || prescription.getRefillsRemaining() <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Prescription has no refills remaining");
        }
        
        // Calculate days since last fill
        Integer daysSinceLastFill = calculateDaysSinceLastFill(prescription.getPrescriptionId());
        
        // Get last fill date
        LocalDate lastFillDate = getLastFillDate(prescription.getPrescriptionId());
        
        // Build refill request
        PrescriptionRefillRequest refillRequest = PrescriptionRefillRequest.builder()
            .prescription(prescription)
            .requestSource(request.getRequestSource() != null ? 
                request.getRequestSource() : PrescriptionRefillRequest.RequestSource.PHARMACY)
            .requestDate(LocalDateTime.now())
            .requestedBy(userId)
            .pharmacyId(request.getPharmacyId())
            .pharmacyName(request.getPharmacyName())
            .pharmacyNpi(request.getPharmacyNpi())
            .pharmacyPhone(request.getPharmacyPhone())
            .refillsRequested(request.getRefillsRequested() != null ? request.getRefillsRequested() : 1)
            .refillsRemaining(prescription.getRefillsRemaining())
            .lastFillDate(lastFillDate)
            .daysSinceLastFill(daysSinceLastFill)
            .requestStatus(PrescriptionRefillRequest.RequestStatus.PENDING)
            .urgencyLevel(request.getUrgencyLevel())
            .notes(request.getNotes())
            .wasAutoApproved(false)
            .createdBy(userId)
            .build();
        
        PrescriptionRefillRequest savedRequest = refillRequestRepository.save(refillRequest);
        log.info("Created refill request: {}", savedRequest.getRefillRequestId());
        
        // Check for auto-approval
        checkAutoApproval(savedRequest);
        
        return mapRefillRequestToResponse(savedRequest);
    }
    
    /**
     * Get refill request by ID
     */
    public PrescriptionRefillRequestResponse getRefillRequestById(UUID refillRequestId) {
        PrescriptionRefillRequest request = refillRequestRepository.findById(refillRequestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Refill request not found: " + refillRequestId));
        return mapRefillRequestToResponse(request);
    }
    
    /**
     * Get all refill requests for a prescription
     */
    public List<PrescriptionRefillRequestResponse> getRefillRequestsByPrescription(UUID prescriptionId) {
        List<PrescriptionRefillRequest> requests = refillRequestRepository
            .findByPrescriptionPrescriptionIdOrderByRequestDateDesc(prescriptionId);
        return requests.stream()
            .map(this::mapRefillRequestToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get pending refill requests (queue)
     */
    public List<PrescriptionRefillRequestResponse> getPendingRefillRequests() {
        List<PrescriptionRefillRequest> requests = refillRequestRepository.findPendingRequestsOrderedByUrgency();
        return requests.stream()
            .map(this::mapRefillRequestToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get refill requests by patient
     */
    public List<PrescriptionRefillRequestResponse> getRefillRequestsByPatient(UUID patientId) {
        List<PrescriptionRefillRequest> requests = refillRequestRepository.findByPatient(patientId);
        return requests.stream()
            .map(this::mapRefillRequestToResponse)
            .collect(Collectors.toList());
    }
    
    // ========== Refill Approval Workflow ==========
    
    /**
     * Approve a refill request
     */
    @Transactional
    public PrescriptionRefillRequestResponse approveRefillRequest(
        UUID refillRequestId, RefillApprovalRequest request, UUID userId) {
        log.info("Approving refill request: {}", refillRequestId);
        
        PrescriptionRefillRequest refillRequest = refillRequestRepository.findById(refillRequestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Refill request not found: " + refillRequestId));

        Prescription prescriptionForSchedule = prescriptionRepository
            .findByIdWithMedications(refillRequest.getPrescription().getPrescriptionId())
            .orElse(refillRequest.getPrescription());
        assertRefillsAllowedForDeaSchedule(prescriptionForSchedule);

        if (refillRequest.getRequestStatus() != PrescriptionRefillRequest.RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending refill requests can be approved");
        }
        
        Integer refillsApproved = request.getRefillsApproved() != null ? 
            request.getRefillsApproved() : refillRequest.getRefillsRequested();
        
        // Update refill request
        refillRequest.setRequestStatus(PrescriptionRefillRequest.RequestStatus.APPROVED);
        refillRequest.setApprovedBy(userId);
        refillRequest.setApprovedDate(LocalDateTime.now());
        refillRequest.setApprovalNotes(request.getApprovalNotes());
        refillRequest.setUpdatedBy(userId);
        
        // If different number approved, mark as modified
        if (!refillsApproved.equals(refillRequest.getRefillsRequested())) {
            refillRequest.setRequestStatus(PrescriptionRefillRequest.RequestStatus.MODIFIED);
            refillRequest.setModifiedBy(userId);
            refillRequest.setModifiedDate(LocalDateTime.now());
            refillRequest.setModificationNotes("Refills modified from " + refillRequest.getRefillsRequested() + 
                " to " + refillsApproved);
            refillRequest.setOriginalRefillsRequested(refillRequest.getRefillsRequested());
            refillRequest.setRefillsRequested(refillsApproved);
        }
        
        PrescriptionRefillRequest savedRequest = refillRequestRepository.save(refillRequest);
        
        // Update prescription refills remaining
        Prescription prescription = savedRequest.getPrescription();
        if (prescription.getRefillsRemaining() != null) {
            prescription.setRefillsRemaining(prescription.getRefillsRemaining() - refillsApproved);
            prescriptionRepository.save(prescription);
        }
        
        log.info("Approved refill request: {}", savedRequest.getRefillRequestId());
        return mapRefillRequestToResponse(savedRequest);
    }
    
    /**
     * Deny a refill request
     */
    @Transactional
    public PrescriptionRefillRequestResponse denyRefillRequest(
        UUID refillRequestId, RefillDenialRequest request, UUID userId) {
        log.info("Denying refill request: {}", refillRequestId);
        
        PrescriptionRefillRequest refillRequest = refillRequestRepository.findById(refillRequestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Refill request not found: " + refillRequestId));
        
        if (refillRequest.getRequestStatus() != PrescriptionRefillRequest.RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending refill requests can be denied");
        }
        
        refillRequest.setRequestStatus(PrescriptionRefillRequest.RequestStatus.DENIED);
        refillRequest.setDeniedBy(userId);
        refillRequest.setDeniedDate(LocalDateTime.now());
        refillRequest.setDenialReason(request.getDenialReason());
        refillRequest.setUpdatedBy(userId);
        
        PrescriptionRefillRequest savedRequest = refillRequestRepository.save(refillRequest);
        log.info("Denied refill request: {}", savedRequest.getRefillRequestId());
        return mapRefillRequestToResponse(savedRequest);
    }
    
    /**
     * Modify a refill request (approve with different quantity)
     */
    @Transactional
    public PrescriptionRefillRequestResponse modifyRefillRequest(
        UUID refillRequestId, RefillModificationRequest request, UUID userId) {
        log.info("Modifying refill request: {}", refillRequestId);
        
        PrescriptionRefillRequest refillRequest = refillRequestRepository.findById(refillRequestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Refill request not found: " + refillRequestId));

        Prescription prescriptionForSchedule = prescriptionRepository
            .findByIdWithMedications(refillRequest.getPrescription().getPrescriptionId())
            .orElse(refillRequest.getPrescription());
        assertRefillsAllowedForDeaSchedule(prescriptionForSchedule);

        if (refillRequest.getRequestStatus() != PrescriptionRefillRequest.RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending refill requests can be modified");
        }
        
        refillRequest.setRequestStatus(PrescriptionRefillRequest.RequestStatus.MODIFIED);
        refillRequest.setModifiedBy(userId);
        refillRequest.setModifiedDate(LocalDateTime.now());
        refillRequest.setModificationNotes(request.getModificationNotes());
        refillRequest.setOriginalRefillsRequested(refillRequest.getRefillsRequested());
        refillRequest.setRefillsRequested(request.getRefillsApproved());
        refillRequest.setApprovedBy(userId);
        refillRequest.setApprovedDate(LocalDateTime.now());
        refillRequest.setUpdatedBy(userId);
        
        PrescriptionRefillRequest savedRequest = refillRequestRepository.save(refillRequest);
        
        // Update prescription refills remaining
        Prescription prescription = savedRequest.getPrescription();
        if (prescription.getRefillsRemaining() != null) {
            prescription.setRefillsRemaining(prescription.getRefillsRemaining() - request.getRefillsApproved());
            prescriptionRepository.save(prescription);
        }
        
        log.info("Modified refill request: {}", savedRequest.getRefillRequestId());
        return mapRefillRequestToResponse(savedRequest);
    }
    
    // ========== Auto-Approval ==========
    
    /**
     * Check if refill request qualifies for auto-approval
     */
    private void checkAutoApproval(com.easyops.hospital.entity.PrescriptionRefillRequest refillRequest) {
        // Simple auto-approval logic (can be enhanced with rules table)
        Prescription prescription = refillRequest.getPrescription();
        
        // Auto-approve if:
        // 1. Not a controlled substance
        // 2. Has refills remaining
        // 3. Days since last fill >= 30 (or reasonable interval)
        // 4. No recent issues
        
        if (prescription.getIsControlledSubstance() == null || !prescription.getIsControlledSubstance()) {
            if (refillRequest.getDaysSinceLastFill() != null && refillRequest.getDaysSinceLastFill() >= 30) {
                if (refillRequest.getRefillsRemaining() != null && refillRequest.getRefillsRemaining() > 0) {
                    // Auto-approve
                    refillRequest.setRequestStatus(com.easyops.hospital.entity.PrescriptionRefillRequest.RequestStatus.APPROVED);
                    refillRequest.setWasAutoApproved(true);
                    refillRequest.setApprovedDate(LocalDateTime.now());
                    refillRequest.setApprovalNotes("Auto-approved: Non-controlled substance, sufficient time since last fill");
                    
                    refillRequestRepository.save(refillRequest);
                    
                    // Update prescription
                    if (prescription.getRefillsRemaining() != null) {
                        prescription.setRefillsRemaining(
                            prescription.getRefillsRemaining() - refillRequest.getRefillsRequested());
                        prescriptionRepository.save(prescription);
                    }
                    
                    log.info("Auto-approved refill request: {}", refillRequest.getRefillRequestId());
                }
            }
        }
    }
    
    // ========== Prescription Refill Recording ==========
    
    /**
     * Record a prescription refill (when pharmacy fills it)
     */
    @Transactional
    public PrescriptionRefillResponse recordRefill(com.easyops.hospital.dto.request.PrescriptionRefillRequest request, UUID userId) {
        log.info("Recording refill for prescription: {}", request.getPrescriptionId());
        
        Prescription prescription = prescriptionRepository.findById(request.getPrescriptionId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found: " + request.getPrescriptionId()));
        
        // Get next refill number
        Optional<Integer> maxRefillNumber = refillRepository.findMaxRefillNumberByPrescription(
            request.getPrescriptionId());
        Integer nextRefillNumber = maxRefillNumber.map(n -> n + 1).orElse(1);
        
        // Get refill request if provided
        com.easyops.hospital.entity.PrescriptionRefillRequest refillRequest = null;
        if (request.getRefillRequestId() != null) {
            refillRequest = refillRequestRepository.findById(request.getRefillRequestId())
                .orElse(null);
        }
        
        // Build refill entity
        PrescriptionRefill refill = PrescriptionRefill.builder()
            .prescription(prescription)
            .refillRequest(refillRequest)
            .refillNumber(nextRefillNumber)
            .refillDate(request.getRefillDate() != null ? request.getRefillDate() : LocalDate.now())
            .quantityDispensed(request.getQuantityDispensed())
            .quantityUnit(request.getQuantityUnit())
            .pharmacyId(request.getPharmacyId())
            .pharmacyName(request.getPharmacyName())
            .pharmacyNpi(request.getPharmacyNpi())
            .filledBy(request.getFilledBy())
            .filledByName(request.getFilledByName())
            .filledDate(LocalDateTime.now())
            .notes(request.getNotes())
            .lotNumber(request.getLotNumber())
            .expirationDate(request.getExpirationDate())
            .createdBy(userId)
            .build();
        
        PrescriptionRefill savedRefill = refillRepository.save(refill);
        
        // Update refill request status if linked
        if (refillRequest != null && refillRequest.getRequestStatus() == 
            com.easyops.hospital.entity.PrescriptionRefillRequest.RequestStatus.APPROVED) {
            refillRequest.setRequestStatus(com.easyops.hospital.entity.PrescriptionRefillRequest.RequestStatus.COMPLETED);
            refillRequestRepository.save(refillRequest);
        }
        
        log.info("Recorded refill: {}", savedRefill.getRefillId());
        return mapRefillToResponse(savedRefill);
    }
    
    /**
     * Get refills for a prescription
     */
    public List<PrescriptionRefillResponse> getRefillsByPrescription(UUID prescriptionId) {
        List<PrescriptionRefill> refills = refillRepository
            .findByPrescriptionPrescriptionIdOrderByRefillDateDesc(prescriptionId);
        return refills.stream()
            .map(this::mapRefillToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get refills by patient
     */
    public List<PrescriptionRefillResponse> getRefillsByPatient(UUID patientId) {
        List<PrescriptionRefill> refills = refillRepository.findByPatient(patientId);
        return refills.stream()
            .map(this::mapRefillToResponse)
            .collect(Collectors.toList());
    }
    
    // ========== DEA Schedule II — refill prohibition (21 CFR §1306.12) ==========

    /**
     * Schedule II substances cannot be refilled; only a new prescription may be issued.
     * Schedule III–V may have refills subject to other rules — use {@link PrescriptionMedication#getSchedule()},
     * not {@link Prescription#getIsControlledSubstance()} alone.
     *
     * @throws UnprocessableEntityException HTTP 422 (Req-J2) — must run before any refill row is persisted
     */
    private void assertRefillsAllowedForDeaSchedule(Prescription prescription) {
        if (prescriptionContainsScheduleII(prescription)) {
            throw new UnprocessableEntityException(
                    "Schedule II controlled substances may not be refilled. A new prescription is required.");
        }
    }

    /**
     * True if the prescription-level schedule or any line item is Schedule II (DEA C-II;
     * {@link Prescription.Schedule#II}).
     */
    private static boolean prescriptionContainsScheduleII(Prescription prescription) {
        if (prescription.getSchedule() == Prescription.Schedule.II) {
            return true;
        }
        if (prescription.getMedications() != null) {
            for (PrescriptionMedication m : prescription.getMedications()) {
                if (m.getSchedule() == Prescription.Schedule.II) {
                    return true;
                }
            }
        }
        return false;
    }

    // ========== Helper Methods ==========
    
    private Integer calculateDaysSinceLastFill(UUID prescriptionId) {
        Optional<Integer> maxRefillNumber = refillRepository.findMaxRefillNumberByPrescription(prescriptionId);
        if (maxRefillNumber.isPresent()) {
            List<PrescriptionRefill> refills = refillRepository
                .findByPrescriptionPrescriptionIdOrderByRefillDateDesc(prescriptionId);
            if (!refills.isEmpty()) {
                LocalDate lastFillDate = refills.get(0).getRefillDate();
                return (int) (LocalDate.now().toEpochDay() - lastFillDate.toEpochDay());
            }
        }
        
        // Check prescription filled_date
        Prescription prescription = prescriptionRepository.findById(prescriptionId).orElse(null);
        if (prescription != null && prescription.getFilledDate() != null) {
            LocalDate filledDate = prescription.getFilledDate().toLocalDate();
            return (int) (LocalDate.now().toEpochDay() - filledDate.toEpochDay());
        }
        
        return null;
    }
    
    private LocalDate getLastFillDate(UUID prescriptionId) {
        List<PrescriptionRefill> refills = refillRepository
            .findByPrescriptionPrescriptionIdOrderByRefillDateDesc(prescriptionId);
        if (!refills.isEmpty()) {
            return refills.get(0).getRefillDate();
        }
        
        // Check prescription filled_date
        Prescription prescription = prescriptionRepository.findById(prescriptionId).orElse(null);
        if (prescription != null && prescription.getFilledDate() != null) {
            return prescription.getFilledDate().toLocalDate();
        }
        
        return null;
    }
    
    // ========== Mapping Methods ==========
    
    private PrescriptionRefillRequestResponse mapRefillRequestToResponse(com.easyops.hospital.entity.PrescriptionRefillRequest request) {
        List<PrescriptionRefill> refills = refillRepository.findByRefillRequestRefillRequestId(
            request.getRefillRequestId());
        
        return PrescriptionRefillRequestResponse.builder()
            .refillRequestId(request.getRefillRequestId())
            .prescriptionId(request.getPrescription().getPrescriptionId())
            .prescriptionNumber(request.getPrescription().getPrescriptionNumber())
            .medicationName(request.getPrescription().getMedicationName())
            .requestSource(request.getRequestSource())
            .requestDate(request.getRequestDate())
            .requestedBy(request.getRequestedBy())
            .requestedByName(request.getRequestedByName())
            .pharmacyId(request.getPharmacyId())
            .pharmacyName(request.getPharmacyName())
            .pharmacyNpi(request.getPharmacyNpi())
            .pharmacyPhone(request.getPharmacyPhone())
            .refillsRequested(request.getRefillsRequested())
            .refillsRemaining(request.getRefillsRemaining())
            .lastFillDate(request.getLastFillDate())
            .daysSinceLastFill(request.getDaysSinceLastFill())
            .requestStatus(request.getRequestStatus())
            .approvedBy(request.getApprovedBy())
            .approvedDate(request.getApprovedDate())
            .approvalNotes(request.getApprovalNotes())
            .deniedBy(request.getDeniedBy())
            .deniedDate(request.getDeniedDate())
            .denialReason(request.getDenialReason())
            .modifiedBy(request.getModifiedBy())
            .modifiedDate(request.getModifiedDate())
            .modificationNotes(request.getModificationNotes())
            .originalRefillsRequested(request.getOriginalRefillsRequested())
            .notes(request.getNotes())
            .urgencyLevel(request.getUrgencyLevel())
            .wasAutoApproved(request.getWasAutoApproved())
            .autoApprovalRuleId(request.getAutoApprovalRuleId())
            .createdAt(request.getCreatedAt())
            .updatedAt(request.getUpdatedAt())
            .refills(refills.stream()
                .map(this::mapRefillToResponse)
                .collect(Collectors.toList()))
            .refillCount(refills.size())
            .build();
    }
    
    private PrescriptionRefillResponse mapRefillToResponse(PrescriptionRefill refill) {
        return PrescriptionRefillResponse.builder()
            .refillId(refill.getRefillId())
            .prescriptionId(refill.getPrescription().getPrescriptionId())
            .refillRequestId(refill.getRefillRequest() != null ? refill.getRefillRequest().getRefillRequestId() : null)
            .refillNumber(refill.getRefillNumber())
            .refillDate(refill.getRefillDate())
            .quantityDispensed(refill.getQuantityDispensed())
            .quantityUnit(refill.getQuantityUnit())
            .pharmacyId(refill.getPharmacyId())
            .pharmacyName(refill.getPharmacyName())
            .pharmacyNpi(refill.getPharmacyNpi())
            .filledBy(refill.getFilledBy())
            .filledByName(refill.getFilledByName())
            .filledDate(refill.getFilledDate())
            .notes(refill.getNotes())
            .lotNumber(refill.getLotNumber())
            .expirationDate(refill.getExpirationDate())
            .createdAt(refill.getCreatedAt())
            .updatedAt(refill.getUpdatedAt())
            .build();
    }
}
