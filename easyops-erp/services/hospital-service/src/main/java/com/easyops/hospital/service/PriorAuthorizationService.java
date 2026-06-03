package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.PriorAuthorizationRequest;
import com.easyops.hospital.dto.response.PriorAuthorizationResponse;
import com.easyops.hospital.entity.*;
import com.easyops.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing prior authorization requests
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PriorAuthorizationService {
    
    private final PBMIntegrationService pbmIntegrationService;
    private final PrescriptionRepository prescriptionRepository;
    private final FormularyCheckRepository formularyCheckRepository;
    private final PriorAuthorizationRepository priorAuthorizationRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;
    
    /**
     * Submit a prior authorization request
     */
    @Transactional
    public PriorAuthorizationResponse submitPriorAuthorization(PriorAuthorizationRequest request, UUID userId) {
        log.info("Submitting prior authorization request for prescription: {}", request.getPrescriptionId());
        
        // Get prescription
        Prescription prescription = prescriptionRepository.findById(request.getPrescriptionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Prescription not found: " + request.getPrescriptionId()));
        
        // Get insurance information
        UUID insuranceId = request.getInsuranceId();
        PatientInsurance insurance = null;
        
        if (insuranceId != null) {
            insurance = patientInsuranceRepository.findById(insuranceId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Insurance not found: " + insuranceId));
        } else if (prescription.getInsuranceId() != null) {
            insurance = patientInsuranceRepository.findById(prescription.getInsuranceId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Insurance not found: " + prescription.getInsuranceId()));
        } else {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No insurance information available for prior authorization");
        }
        
        // Get formulary check if provided
        FormularyCheck formularyCheck = null;
        if (request.getFormularyCheckId() != null) {
            formularyCheck = formularyCheckRepository.findById(request.getFormularyCheckId())
                    .orElse(null);
        }
        
        // Submit prior authorization to PBM
        PrescriptionMedication firstMedication = prescription.getMedications() != null
                ? prescription.getMedications().stream().findFirst().orElse(null)
                : null;
        String medicationCode = prescription.getMedicationCode() != null
                ? prescription.getMedicationCode()
                : (firstMedication != null ? firstMedication.getMedicationCode() : null);
        String medicationName = prescription.getMedicationName() != null
                ? prescription.getMedicationName()
                : (firstMedication != null ? firstMedication.getMedicationName() : null);
        if (medicationName == null || medicationName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Medication details are missing on this prescription. Add at least one medication before submitting prior authorization.");
        }

        PriorAuthorization priorAuth = pbmIntegrationService.submitPriorAuthorization(
                prescription.getPrescriptionId(),
                insurance.getInsuranceId(),
                medicationCode,
                medicationName,
                insurance.getPolicyNumber(),
                insurance.getInsuranceCompanyName(),
                request.getClinicalJustification(),
                request.getSupportingDocumentation()
        );
        
        // Link to prescription and formulary check
        priorAuth.setPrescription(prescription);
        priorAuth.setFormularyCheck(formularyCheck);
        priorAuth.setInsuranceId(insurance.getInsuranceId());
        priorAuth.setInsuranceCompanyName(insurance.getInsuranceCompanyName());
        priorAuth.setPolicyNumber(insurance.getPolicyNumber());
        priorAuth.setMedicationCode(medicationCode);
        priorAuth.setMedicationName(medicationName);
        priorAuth.setRequestedBy(userId);
        priorAuth.setCreatedBy(userId);
        priorAuth.setSupportingDocumentation(request.getSupportingDocumentation());
        priorAuth.setNotes(request.getNotes());
        
        // Save prior authorization
        priorAuth = priorAuthorizationRepository.save(priorAuth);
        
        // Update prescription
        prescription.setRequiresPriorAuthorization(true);
        if (priorAuth.getStatus() == PriorAuthorization.PriorAuthStatus.APPROVED) {
            prescription.setPriorAuthorizationObtained(true);
            prescription.setPriorAuthorizationNumber(priorAuth.getPriorAuthNumber());
        }
        prescriptionRepository.save(prescription);
        
        log.info("Prior authorization submitted: {}", priorAuth.getPriorAuthId());
        return mapToResponse(priorAuth);
    }
    
    /**
     * Get prior authorization by ID
     */
    @Transactional(readOnly = true)
    public PriorAuthorizationResponse getPriorAuthorization(UUID priorAuthId) {
        PriorAuthorization priorAuth = priorAuthorizationRepository.findById(priorAuthId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prior authorization not found: " + priorAuthId));
        return mapToResponse(priorAuth);
    }
    
    /**
     * Get prior authorizations for a prescription
     */
    @Transactional(readOnly = true)
    public List<PriorAuthorizationResponse> getPriorAuthorizationsByPrescription(UUID prescriptionId) {
        List<PriorAuthorization> priorAuths = priorAuthorizationRepository
                .findByPrescriptionPrescriptionId(prescriptionId);
        return priorAuths.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Check prior authorization status
     */
    @Transactional
    public PriorAuthorizationResponse checkPriorAuthorizationStatus(UUID priorAuthId) {
        log.info("Checking prior authorization status: {}", priorAuthId);
        
        PriorAuthorization priorAuth = priorAuthorizationRepository.findById(priorAuthId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prior authorization not found: " + priorAuthId));
        
        // If PBM request ID exists, check status
        if (priorAuth.getPbmRequestId() != null && !priorAuth.getPbmRequestId().isEmpty()) {
            PriorAuthorization updatedStatus = pbmIntegrationService.checkPriorAuthorizationStatus(
                    priorAuth.getPbmRequestId());
            
            if (updatedStatus != null) {
                // Update status
                priorAuth.setStatus(updatedStatus.getStatus());
                priorAuth.setPriorAuthNumber(updatedStatus.getPriorAuthNumber());
                priorAuth.setApprovedDate(updatedStatus.getApprovedDate());
                priorAuth.setDeniedDate(updatedStatus.getDeniedDate());
                priorAuth.setExpirationDate(updatedStatus.getExpirationDate());
                priorAuth.setDenialReason(updatedStatus.getDenialReason());
                
                priorAuth = priorAuthorizationRepository.save(priorAuth);
                
                // Update prescription if approved
                if (priorAuth.getStatus() == PriorAuthorization.PriorAuthStatus.APPROVED) {
                    Prescription prescription = priorAuth.getPrescription();
                    prescription.setPriorAuthorizationObtained(true);
                    prescription.setPriorAuthorizationNumber(priorAuth.getPriorAuthNumber());
                    prescriptionRepository.save(prescription);
                }
            }
        }
        
        return mapToResponse(priorAuth);
    }
    
    /**
     * Update prior authorization status manually
     */
    @Transactional
    public PriorAuthorizationResponse updatePriorAuthorizationStatus(
            UUID priorAuthId, 
            PriorAuthorization.PriorAuthStatus status,
            String priorAuthNumber,
            LocalDate expirationDate,
            String denialReason,
            UUID userId) {
        
        log.info("Updating prior authorization status: {} to {}", priorAuthId, status);
        
        PriorAuthorization priorAuth = priorAuthorizationRepository.findById(priorAuthId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prior authorization not found: " + priorAuthId));
        
        priorAuth.setStatus(status);
        if (priorAuthNumber != null) {
            priorAuth.setPriorAuthNumber(priorAuthNumber);
        }
        if (expirationDate != null) {
            priorAuth.setExpirationDate(expirationDate);
        }
        if (denialReason != null) {
            priorAuth.setDenialReason(denialReason);
        }
        if (status == PriorAuthorization.PriorAuthStatus.APPROVED) {
            priorAuth.setApprovedDate(LocalDate.now());
            priorAuth.setReviewedBy(userId);
        } else if (status == PriorAuthorization.PriorAuthStatus.DENIED) {
            priorAuth.setDeniedDate(LocalDate.now());
            priorAuth.setReviewedBy(userId);
        }
        priorAuth.setUpdatedBy(userId);
        
        priorAuth = priorAuthorizationRepository.save(priorAuth);
        
        // Update prescription if approved
        if (status == PriorAuthorization.PriorAuthStatus.APPROVED) {
            Prescription prescription = priorAuth.getPrescription();
            prescription.setPriorAuthorizationObtained(true);
            prescription.setPriorAuthorizationNumber(priorAuth.getPriorAuthNumber());
            prescriptionRepository.save(prescription);
        }
        
        return mapToResponse(priorAuth);
    }
    
    // ========== Helper Methods ==========
    
    private PriorAuthorizationResponse mapToResponse(PriorAuthorization priorAuth) {
        return PriorAuthorizationResponse.builder()
                .priorAuthId(priorAuth.getPriorAuthId())
                .prescriptionId(priorAuth.getPrescription().getPrescriptionId())
                .formularyCheckId(priorAuth.getFormularyCheck() != null ? 
                        priorAuth.getFormularyCheck().getFormularyCheckId() : null)
                .insuranceId(priorAuth.getInsuranceId())
                .insuranceCompanyName(priorAuth.getInsuranceCompanyName())
                .policyNumber(priorAuth.getPolicyNumber())
                .medicationCode(priorAuth.getMedicationCode())
                .medicationName(priorAuth.getMedicationName())
                .priorAuthNumber(priorAuth.getPriorAuthNumber())
                .requestDate(priorAuth.getRequestDate())
                .status(priorAuth.getStatus())
                .submittedDate(priorAuth.getSubmittedDate())
                .approvedDate(priorAuth.getApprovedDate())
                .deniedDate(priorAuth.getDeniedDate())
                .expirationDate(priorAuth.getExpirationDate())
                .denialReason(priorAuth.getDenialReason())
                .clinicalJustification(priorAuth.getClinicalJustification())
                .supportingDocumentation(priorAuth.getSupportingDocumentation())
                .requestedBy(priorAuth.getRequestedBy())
                .reviewedBy(priorAuth.getReviewedBy())
                .pbmName(priorAuth.getPbmName())
                .pbmRequestId(priorAuth.getPbmRequestId())
                .pbmResponseId(priorAuth.getPbmResponseId())
                .notes(priorAuth.getNotes())
                .createdAt(priorAuth.getCreatedAt())
                .updatedAt(priorAuth.getUpdatedAt())
                .build();
    }
}
