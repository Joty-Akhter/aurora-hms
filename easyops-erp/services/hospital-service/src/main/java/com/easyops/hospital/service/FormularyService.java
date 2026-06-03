package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.FormularyCheckRequest;
import com.easyops.hospital.dto.response.FormularyCheckResponse;
import com.easyops.hospital.entity.*;
import com.easyops.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for formulary checking and medication coverage verification
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FormularyService {
    
    private final PBMIntegrationService pbmIntegrationService;
    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;
    private final FormularyCheckRepository formularyCheckRepository;
    private final FormularyAlternativeRepository formularyAlternativeRepository;
    
    /**
     * Check formulary coverage for a prescription
     */
    @Transactional
    public FormularyCheckResponse checkFormularyCoverage(FormularyCheckRequest request) {
        log.info("Checking formulary coverage for prescription: {}", request.getPrescriptionId());
        
        // Get prescription
        Prescription prescription = prescriptionRepository.findById(request.getPrescriptionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Prescription not found: " + request.getPrescriptionId()));
        
        // Get patient
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Patient not found: " + request.getPatientId()));

        if (!prescription.getPatient().getPatientId().equals(patient.getPatientId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Prescription does not belong to the provided patient");
        }
        
        // Get insurance information
        UUID insuranceId = request.getInsuranceId();
        PatientInsurance insurance = null;
        
        if (insuranceId != null) {
            insurance = patientInsuranceRepository.findById(insuranceId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Insurance not found: " + insuranceId));
            if (!insurance.getPatient().getPatientId().equals(patient.getPatientId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Insurance does not belong to the provided patient");
            }
        } else {
            // Use primary insurance if not specified
            List<PatientInsurance> insurances = patientInsuranceRepository.findByPatientPatientId(patient.getPatientId());
            insurance = insurances.stream()
                    .filter(ins -> ins.getInsuranceType() == PatientInsurance.InsuranceType.PRIMARY)
                    .findFirst()
                    .orElse(insurances.isEmpty() ? null : insurances.get(0));
            if (insurance == null && prescription.getInsuranceId() != null) {
                insurance = patientInsuranceRepository.findById(prescription.getInsuranceId())
                        .orElse(null);
                if (insurance != null && !insurance.getPatient().getPatientId().equals(patient.getPatientId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Prescription insurance does not belong to the provided patient");
                }
            }
        }
        
        if (insurance == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "No insurance information found for patient; formulary check requires active insurance coverage");
        }
        
        // Get medication information
        PrescriptionMedication firstMedication = prescription.getMedications() != null
                ? prescription.getMedications().stream().findFirst().orElse(null)
                : null;
        String medicationCode = request.getMedicationCode() != null
                ? request.getMedicationCode()
                : (prescription.getMedicationCode() != null ? prescription.getMedicationCode()
                    : (firstMedication != null ? firstMedication.getMedicationCode() : null));
        String medicationName = request.getMedicationName() != null
                ? request.getMedicationName()
                : (prescription.getMedicationName() != null ? prescription.getMedicationName()
                    : (firstMedication != null ? firstMedication.getMedicationName() : null));

        if (medicationName == null || medicationName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Medication details are missing on this prescription. Add at least one medication before formulary check.");
        }
        
        // Check formulary coverage via PBM
        FormularyCheck formularyCheck = pbmIntegrationService.checkFormularyCoverage(
                patient.getPatientId(),
                insurance.getInsuranceId(),
                medicationCode,
                medicationName,
                insurance.getPolicyNumber(),
                insurance.getInsuranceCompanyName()
        );
        
        // Link to prescription
        formularyCheck.setPrescription(prescription);
        formularyCheck.setInsuranceId(insurance.getInsuranceId());
        formularyCheck.setInsuranceCompanyName(insurance.getInsuranceCompanyName());
        formularyCheck.setPolicyNumber(insurance.getPolicyNumber());
        
        // Save formulary check
        try {
            formularyCheck = formularyCheckRepository.save(formularyCheck);
        } catch (DataIntegrityViolationException ex) {
            log.error("Failed to persist formulary check for prescription {}", request.getPrescriptionId(), ex);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Unable to save formulary check. Verify insurance and prescription data, then try again.");
        }
        
        // Update prescription with formulary information
        prescription.setFormularyChecked(true);
        prescription.setFormularyCheckDate(formularyCheck.getCheckDate());
        prescription.setCoverageStatus(mapCoverageStatus(formularyCheck.getCoverageStatus()));
        prescription.setFormularyTier(formularyCheck.getFormularyTier());
        prescription.setRequiresPriorAuthorization(formularyCheck.getRequiresPriorAuthorization());
        prescription.setPatientCostEstimate(formularyCheck.getPatientCostEstimate());
        prescription.setCopayAmount(formularyCheck.getCopayAmount());
        prescription.setInsuranceId(insurance.getInsuranceId());
        prescription.setPbmName(formularyCheck.getPbmName());
        prescriptionRepository.save(prescription);
        
        // Get alternatives if requested
        List<FormularyCheckResponse.FormularyAlternativeResponse> alternatives = null;
        if (request.getIncludeAlternatives() != null && request.getIncludeAlternatives()) {
            alternatives = getFormularyAlternatives(formularyCheck.getFormularyCheckId());
        }
        
        // Build response
        FormularyCheckResponse response = mapToResponse(formularyCheck);
        response.setAlternatives(alternatives);
        
        log.info("Formulary check completed for prescription: {}", request.getPrescriptionId());
        return response;
    }
    
    /**
     * Get formulary alternatives for a medication
     */
    @Transactional(readOnly = true)
    public List<FormularyCheckResponse.FormularyAlternativeResponse> getFormularyAlternatives(UUID formularyCheckId) {
        log.info("Getting formulary alternatives for check: {}", formularyCheckId);
        
        FormularyCheck formularyCheck = formularyCheckRepository.findById(formularyCheckId)
                .orElseThrow(() -> new RuntimeException("Formulary check not found: " + formularyCheckId));
        
        // If medication is not covered or has restrictions, find alternatives
        if (formularyCheck.getCoverageStatus() == FormularyCheck.CoverageStatus.NOT_COVERED ||
            formularyCheck.getCoverageStatus() == FormularyCheck.CoverageStatus.COVERED_WITH_RESTRICTIONS) {
            
            // Get alternatives from repository
            List<FormularyAlternative> alternatives = formularyAlternativeRepository
                    .findByFormularyCheckFormularyCheckIdOrderByRankAsc(formularyCheckId);
            
            if (alternatives.isEmpty()) {
                // Generate alternatives based on formulary check
                alternatives = generateFormularyAlternatives(formularyCheck);
            }
            
            return alternatives.stream()
                    .map(this::mapAlternativeToResponse)
                    .collect(Collectors.toList());
        }
        
        return List.of();
    }
    
    /**
     * Generate formulary alternatives (mock implementation)
     * In production, this would query PBM for alternatives
     */
    private List<FormularyAlternative> generateFormularyAlternatives(FormularyCheck formularyCheck) {
        // This is a simplified mock implementation
        // In production, this would call PBM service to get actual alternatives
        
        List<FormularyAlternative> alternatives = new java.util.ArrayList<>();
        
        // Example: Create generic alternative
        FormularyAlternative genericAlt = FormularyAlternative.builder()
                .formularyCheck(formularyCheck)
                .medicationCode(formularyCheck.getMedicationCode() + "-GENERIC")
                .medicationName("Generic " + formularyCheck.getMedicationName())
                .genericName("Generic " + formularyCheck.getMedicationName())
                .formularyTier("Tier 1")
                .coverageStatus(FormularyAlternative.CoverageStatus.COVERED)
                .requiresPriorAuthorization(false)
                .copayAmount(formularyCheck.getCopayAmount() != null ? 
                        formularyCheck.getCopayAmount().multiply(new java.math.BigDecimal("0.5")) : 
                        new java.math.BigDecimal("10.00"))
                .patientCostEstimate(formularyCheck.getPatientCostEstimate() != null ? 
                        formularyCheck.getPatientCostEstimate().multiply(new java.math.BigDecimal("0.5")) : 
                        new java.math.BigDecimal("10.00"))
                .alternativeType(FormularyAlternative.AlternativeType.GENERIC)
                .reason("Generic alternative available with better coverage")
                .isPreferred(true)
                .rank(1)
                .build();
        
        alternatives.add(genericAlt);
        
        // Save alternatives
        formularyAlternativeRepository.saveAll(alternatives);
        
        return alternatives;
    }
    
    /**
     * Get formulary check history for a prescription
     */
    @Transactional(readOnly = true)
    public List<FormularyCheckResponse> getFormularyCheckHistory(UUID prescriptionId) {
        log.info("Getting formulary check history for prescription: {}", prescriptionId);
        
        List<FormularyCheck> checks = formularyCheckRepository
                .findByPrescriptionPrescriptionIdOrderByCheckDateDesc(prescriptionId);
        
        return checks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get latest formulary check for a prescription
     */
    @Transactional(readOnly = true)
    public FormularyCheckResponse getLatestFormularyCheck(UUID prescriptionId) {
        log.info("Getting latest formulary check for prescription: {}", prescriptionId);
        
        FormularyCheck check = formularyCheckRepository
                .findFirstByPrescriptionPrescriptionIdOrderByCheckDateDesc(prescriptionId);
        
        if (check == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No formulary check found for prescription: " + prescriptionId);
        }
        
        FormularyCheckResponse response = mapToResponse(check);
        response.setAlternatives(getFormularyAlternatives(check.getFormularyCheckId()));
        
        return response;
    }
    
    // ========== Helper Methods ==========
    
    private Prescription.FormularyCoverageStatus mapCoverageStatus(FormularyCheck.CoverageStatus status) {
        if (status == null) return Prescription.FormularyCoverageStatus.NOT_CHECKED;
        
        switch (status) {
            case COVERED:
                return Prescription.FormularyCoverageStatus.COVERED;
            case NOT_COVERED:
                return Prescription.FormularyCoverageStatus.NOT_COVERED;
            case COVERED_WITH_RESTRICTIONS:
                return Prescription.FormularyCoverageStatus.COVERED_WITH_RESTRICTIONS;
            case ERROR:
                return Prescription.FormularyCoverageStatus.ERROR;
            default:
                return Prescription.FormularyCoverageStatus.NOT_CHECKED;
        }
    }
    
    private FormularyCheckResponse mapToResponse(FormularyCheck check) {
        return FormularyCheckResponse.builder()
                .formularyCheckId(check.getFormularyCheckId())
                .prescriptionId(check.getPrescription().getPrescriptionId())
                .insuranceId(check.getInsuranceId())
                .insuranceCompanyName(check.getInsuranceCompanyName())
                .policyNumber(check.getPolicyNumber())
                .medicationCode(check.getMedicationCode())
                .medicationName(check.getMedicationName())
                .coverageStatus(check.getCoverageStatus())
                .formularyTier(check.getFormularyTier())
                .requiresPriorAuthorization(check.getRequiresPriorAuthorization())
                .priorAuthorizationRequired(check.getPriorAuthorizationRequired())
                .stepTherapyRequired(check.getStepTherapyRequired())
                .quantityLimit(check.getQuantityLimit())
                .daysSupplyLimit(check.getDaysSupplyLimit())
                .copayAmount(check.getCopayAmount())
                .coinsurancePercentage(check.getCoinsurancePercentage())
                .deductibleApplies(check.getDeductibleApplies())
                .patientCostEstimate(check.getPatientCostEstimate())
                .insurancePays(check.getInsurancePays())
                .pbmName(check.getPbmName())
                .pbmId(check.getPbmId())
                .formularyId(check.getFormularyId())
                .formularyName(check.getFormularyName())
                .checkDate(check.getCheckDate())
                .checkStatus(check.getCheckStatus())
                .errorMessage(check.getErrorMessage())
                .build();
    }
    
    private FormularyCheckResponse.FormularyAlternativeResponse mapAlternativeToResponse(FormularyAlternative alternative) {
        // Map FormularyAlternative.CoverageStatus to FormularyCheck.CoverageStatus
        FormularyCheck.CoverageStatus coverageStatus = null;
        if (alternative.getCoverageStatus() != null) {
            switch (alternative.getCoverageStatus()) {
                case COVERED:
                    coverageStatus = FormularyCheck.CoverageStatus.COVERED;
                    break;
                case NOT_COVERED:
                    coverageStatus = FormularyCheck.CoverageStatus.NOT_COVERED;
                    break;
                case COVERED_WITH_RESTRICTIONS:
                    coverageStatus = FormularyCheck.CoverageStatus.COVERED_WITH_RESTRICTIONS;
                    break;
            }
        }
        
        return FormularyCheckResponse.FormularyAlternativeResponse.builder()
                .alternativeId(alternative.getAlternativeId())
                .medicationCode(alternative.getMedicationCode())
                .medicationName(alternative.getMedicationName())
                .genericName(alternative.getGenericName())
                .formularyTier(alternative.getFormularyTier())
                .coverageStatus(coverageStatus)
                .requiresPriorAuthorization(alternative.getRequiresPriorAuthorization())
                .copayAmount(alternative.getCopayAmount())
                .patientCostEstimate(alternative.getPatientCostEstimate())
                .alternativeType(alternative.getAlternativeType() != null ? alternative.getAlternativeType().toString() : null)
                .reason(alternative.getReason())
                .isPreferred(alternative.getIsPreferred())
                .rank(alternative.getRank())
                .build();
    }
}
