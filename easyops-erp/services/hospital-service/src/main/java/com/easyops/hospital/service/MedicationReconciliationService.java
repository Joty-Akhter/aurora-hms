package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.MedicationReconciliationRequest;
import com.easyops.hospital.dto.response.MedicationReconciliationComparisonResponse;
import com.easyops.hospital.dto.response.MedicationReconciliationResponse;
import com.easyops.hospital.dto.response.MedicationReconciliationSourceResponse;
import com.easyops.hospital.dto.response.MedicationResponse;
import com.easyops.hospital.entity.*;
import com.easyops.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicationReconciliationService {
    
    private final MedicationReconciliationRepository reconciliationRepository;
    private final MedicationReconciliationSourceRepository sourceRepository;
    private final MedicationReconciliationComparisonRepository comparisonRepository;
    private final MedicationRepository medicationRepository;
    private final MedicationService medicationService;
    private final PatientRepository patientRepository;
    
    /**
     * Create a new medication reconciliation
     */
    @Transactional
    public MedicationReconciliationResponse createReconciliation(
            MedicationReconciliationRequest request, UUID userId) {
        log.info("Creating medication reconciliation for patient: {}", request.getPatientId());
        
        Patient patient = patientRepository.findById(request.getPatientId())
            .orElseThrow(() -> new RuntimeException("Patient not found: " + request.getPatientId()));
        
        // Get current EHR medications
        List<Medication> currentMedications = medicationRepository.findActiveMedicationsByPatient(request.getPatientId());
        
        // Create reconciliation entity
        MedicationReconciliation reconciliation = MedicationReconciliation.builder()
            .patient(patient)
            .encounterId(request.getEncounterId())
            .reconciliationDate(request.getReconciliationDate())
            .reconciliationType(request.getReconciliationType())
            .reconciliationStatus(MedicationReconciliation.ReconciliationStatus.IN_PROGRESS)
            .performedBy(userId)
            .notes(request.getNotes())
            .totalMedicationsBefore(currentMedications.size())
            .createdBy(userId)
            .build();
        
        reconciliation = reconciliationRepository.save(reconciliation);
        
        // Process sources and create comparisons
        List<MedicationReconciliationSource> sources = new ArrayList<>();
        List<MedicationReconciliationComparison> comparisons = new ArrayList<>();
        
        if (request.getSources() != null && !request.getSources().isEmpty()) {
            for (MedicationReconciliationRequest.MedicationSourceRequest sourceRequest : request.getSources()) {
                MedicationReconciliationSource source = createSource(reconciliation, sourceRequest, userId);
                sources.add(source);
                
                // Compare source medications with current EHR medications
                List<MedicationReconciliationComparison> sourceComparisons = 
                    compareMedications(currentMedications, sourceRequest.getMedications(), reconciliation, userId);
                comparisons.addAll(sourceComparisons);
            }
        }
        
        // Save sources and comparisons
        sourceRepository.saveAll(sources);
        comparisonRepository.saveAll(comparisons);
        
        // Update reconciliation summary
        updateReconciliationSummary(reconciliation, comparisons);
        reconciliation = reconciliationRepository.save(reconciliation);
        
        log.info("Created medication reconciliation: {}", reconciliation.getReconciliationId());
        return mapToResponse(reconciliation);
    }
    
    /**
     * Compare medications and identify differences
     */
    private List<MedicationReconciliationComparison> compareMedications(
            List<Medication> currentMedications,
            List<MedicationReconciliationRequest.MedicationItemRequest> sourceMedications,
            MedicationReconciliation reconciliation,
            UUID userId) {
        
        List<MedicationReconciliationComparison> comparisons = new ArrayList<>();
        Set<String> matchedCurrent = new HashSet<>();
        
        // Compare each source medication with current medications
        for (MedicationReconciliationRequest.MedicationItemRequest sourceMed : sourceMedications) {
            Medication matched = findMatchingMedication(currentMedications, sourceMed, matchedCurrent);
            
            MedicationReconciliationComparison comparison;
            if (matched == null) {
                // New medication - not in current list
                comparison = createComparison(reconciliation, sourceMed, null, 
                    MedicationReconciliationComparison.ComparisonStatus.NEW, userId);
            } else {
                // Check if changed
                if (isMedicationChanged(matched, sourceMed)) {
                    comparison = createComparison(reconciliation, sourceMed, matched, 
                        MedicationReconciliationComparison.ComparisonStatus.CHANGED, userId);
                } else {
                    comparison = createComparison(reconciliation, sourceMed, matched, 
                        MedicationReconciliationComparison.ComparisonStatus.UNCHANGED, userId);
                }
                matchedCurrent.add(matched.getMedicationId().toString());
            }
            comparisons.add(comparison);
        }
        
        // Find discontinued medications (in current but not in source)
        for (Medication currentMed : currentMedications) {
            if (!matchedCurrent.contains(currentMed.getMedicationId().toString())) {
                boolean foundInSource = sourceMedications.stream()
                    .anyMatch(sm -> isMatchingMedication(currentMed, sm));
                
                if (!foundInSource) {
                    MedicationReconciliationComparison comparison = createDiscontinuedComparison(
                        reconciliation, currentMed, userId);
                    comparisons.add(comparison);
                }
            }
        }
        
        return comparisons;
    }
    
    /**
     * Find matching medication in current list
     */
    private Medication findMatchingMedication(
            List<Medication> currentMedications,
            MedicationReconciliationRequest.MedicationItemRequest sourceMed,
            Set<String> matched) {
        
        for (Medication current : currentMedications) {
            if (matched.contains(current.getMedicationId().toString())) {
                continue;
            }
            if (isMatchingMedication(current, sourceMed)) {
                return current;
            }
        }
        return null;
    }
    
    /**
     * Check if medications match (by name, code, or generic name)
     */
    private boolean isMatchingMedication(
            Medication current,
            MedicationReconciliationRequest.MedicationItemRequest source) {
        
        // Match by medication code if available
        if (current.getMedicationCode() != null && source.getMedicationCode() != null) {
            if (current.getMedicationCode().equalsIgnoreCase(source.getMedicationCode())) {
                return true;
            }
        }
        
        // Match by RxNorm code if available
        if (current.getRxnormCode() != null && source.getMedicationCode() != null) {
            if (current.getRxnormCode().equalsIgnoreCase(source.getMedicationCode())) {
                return true;
            }
        }
        
        // Match by medication name (case-insensitive)
        if (current.getMedicationName() != null && source.getMedicationName() != null) {
            if (current.getMedicationName().equalsIgnoreCase(source.getMedicationName())) {
                return true;
            }
        }
        
        // Match by generic name if available
        if (current.getGenericName() != null && source.getGenericName() != null) {
            if (current.getGenericName().equalsIgnoreCase(source.getGenericName())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if medication has changed
     */
    private boolean isMedicationChanged(
            Medication current,
            MedicationReconciliationRequest.MedicationItemRequest source) {
        
        // Compare dosage
        if (!compareDosage(current.getDosageStrength(), source.getDosageStrength()) ||
            !Objects.equals(current.getDosageUnit(), source.getDosageUnit())) {
            return true;
        }
        
        // Compare frequency
        if (!Objects.equals(current.getFrequency(), source.getFrequency())) {
            return true;
        }
        
        // Compare route
        if (current.getRoute() != null && source.getRoute() != null) {
            if (!current.getRoute().toString().equalsIgnoreCase(source.getRoute())) {
                return true;
            }
        }
        
        // Compare instructions
        if (!Objects.equals(current.getInstructions(), source.getInstructions())) {
            return true;
        }
        
        return false;
    }
    
    private boolean compareDosage(BigDecimal current, String source) {
        if (current == null && (source == null || source.isEmpty())) {
            return true;
        }
        if (current == null || source == null || source.isEmpty()) {
            return false;
        }
        try {
            BigDecimal sourceDecimal = new BigDecimal(source);
            return current.compareTo(sourceDecimal) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Create comparison record
     */
    private MedicationReconciliationComparison createComparison(
            MedicationReconciliation reconciliation,
            MedicationReconciliationRequest.MedicationItemRequest sourceMed,
            Medication currentMed,
            MedicationReconciliationComparison.ComparisonStatus status,
            UUID userId) {
        
        MedicationReconciliationComparison comparison = MedicationReconciliationComparison.builder()
            .reconciliation(reconciliation)
            .medicationName(sourceMed.getMedicationName())
            .genericName(sourceMed.getGenericName())
            .medicationCode(sourceMed.getMedicationCode())
            .medicationCodeType(parseCodeType(sourceMed.getMedicationCodeType()))
            .comparisonStatus(status)
            .actionTaken(status == MedicationReconciliationComparison.ComparisonStatus.NEW ? 
                MedicationReconciliationComparison.ActionTaken.PENDING : 
                MedicationReconciliationComparison.ActionTaken.PENDING)
            .createdBy(userId)
            .build();
        
        if (currentMed != null) {
            comparison.setTargetMedicationId(currentMed.getMedicationId());
            comparison.setBeforeDosageStrength(currentMed.getDosageStrength());
            comparison.setBeforeDosageUnit(currentMed.getDosageUnit());
            comparison.setBeforeFrequency(currentMed.getFrequency());
            comparison.setBeforeRoute(currentMed.getRoute() != null ? currentMed.getRoute().toString() : null);
            comparison.setBeforeInstructions(currentMed.getInstructions());
        }
        
        if (sourceMed.getDosageStrength() != null) {
            try {
                comparison.setAfterDosageStrength(new BigDecimal(sourceMed.getDosageStrength()));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        comparison.setAfterDosageUnit(sourceMed.getDosageUnit());
        comparison.setAfterFrequency(sourceMed.getFrequency());
        comparison.setAfterRoute(sourceMed.getRoute());
        comparison.setAfterInstructions(sourceMed.getInstructions());
        
        // Generate differences description
        comparison.setDifferences(generateDifferencesDescription(comparison));
        
        return comparison;
    }
    
    private MedicationReconciliationComparison createDiscontinuedComparison(
            MedicationReconciliation reconciliation,
            Medication currentMed,
            UUID userId) {
        
        return MedicationReconciliationComparison.builder()
            .reconciliation(reconciliation)
            .medicationName(currentMed.getMedicationName())
            .genericName(currentMed.getGenericName())
            .medicationCode(currentMed.getMedicationCode())
            .medicationCodeType(currentMed.getMedicationCodeType())
            .comparisonStatus(MedicationReconciliationComparison.ComparisonStatus.DISCONTINUED)
            .actionTaken(MedicationReconciliationComparison.ActionTaken.PENDING)
            .targetMedicationId(currentMed.getMedicationId())
            .beforeDosageStrength(currentMed.getDosageStrength())
            .beforeDosageUnit(currentMed.getDosageUnit())
            .beforeFrequency(currentMed.getFrequency())
            .beforeRoute(currentMed.getRoute() != null ? currentMed.getRoute().toString() : null)
            .beforeInstructions(currentMed.getInstructions())
            .differences("Medication not found in source list - may be discontinued")
            .createdBy(userId)
            .build();
    }
    
    private String generateDifferencesDescription(MedicationReconciliationComparison comparison) {
        List<String> differences = new ArrayList<>();
        
        if (comparison.getBeforeDosageStrength() != null || comparison.getAfterDosageStrength() != null) {
            if (!Objects.equals(comparison.getBeforeDosageStrength(), comparison.getAfterDosageStrength()) ||
                !Objects.equals(comparison.getBeforeDosageUnit(), comparison.getAfterDosageUnit())) {
                differences.add(String.format("Dosage: %s %s -> %s %s",
                    comparison.getBeforeDosageStrength(), comparison.getBeforeDosageUnit(),
                    comparison.getAfterDosageStrength(), comparison.getAfterDosageUnit()));
            }
        }
        
        if (!Objects.equals(comparison.getBeforeFrequency(), comparison.getAfterFrequency())) {
            differences.add(String.format("Frequency: %s -> %s",
                comparison.getBeforeFrequency(), comparison.getAfterFrequency()));
        }
        
        if (!Objects.equals(comparison.getBeforeRoute(), comparison.getAfterRoute())) {
            differences.add(String.format("Route: %s -> %s",
                comparison.getBeforeRoute(), comparison.getAfterRoute()));
        }
        
        return String.join("; ", differences);
    }
    
    private Medication.MedicationCodeType parseCodeType(String codeType) {
        if (codeType == null) return null;
        try {
            return Medication.MedicationCodeType.valueOf(codeType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Create source record
     */
    private MedicationReconciliationSource createSource(
            MedicationReconciliation reconciliation,
            MedicationReconciliationRequest.MedicationSourceRequest sourceRequest,
            UUID userId) {
        
        Map<String, Object> sourceData = new HashMap<>();
        if (sourceRequest.getMedications() != null) {
            sourceData.put("medicationCount", sourceRequest.getMedications().size());
            sourceData.put("medications", sourceRequest.getMedications());
        }
        
        return MedicationReconciliationSource.builder()
            .reconciliation(reconciliation)
            .sourceType(sourceRequest.getSourceType())
            .sourceName(sourceRequest.getSourceName())
            .sourceDescription(sourceRequest.getSourceDescription())
            .sourceData(sourceData)
            .sourceDate(sourceRequest.getSourceDate())
            .sourceProviderName(sourceRequest.getSourceProviderName())
            .sourceFacilityName(sourceRequest.getSourceFacilityName())
            .sourceContactInfo(sourceRequest.getSourceContactInfo())
            .importMethod(sourceRequest.getImportMethod() != null ? 
                sourceRequest.getImportMethod() : 
                MedicationReconciliationSource.ImportMethod.MANUAL)
            .importedAt(LocalDateTime.now())
            .importedBy(userId)
            .createdBy(userId)
            .build();
    }
    
    /**
     * Update reconciliation summary
     */
    private void updateReconciliationSummary(
            MedicationReconciliation reconciliation,
            List<MedicationReconciliationComparison> comparisons) {
        
        int added = 0, modified = 0, discontinued = 0, unchanged = 0;
        
        for (MedicationReconciliationComparison comp : comparisons) {
            switch (comp.getComparisonStatus()) {
                case NEW:
                    added++;
                    break;
                case CHANGED:
                    modified++;
                    break;
                case DISCONTINUED:
                    discontinued++;
                    break;
                case UNCHANGED:
                    unchanged++;
                    break;
            }
        }
        
        reconciliation.setMedicationsAdded(added);
        reconciliation.setMedicationsModified(modified);
        reconciliation.setMedicationsDiscontinued(discontinued);
        reconciliation.setMedicationsUnchanged(unchanged);
        reconciliation.setTotalMedicationsAfter(
            reconciliation.getTotalMedicationsBefore() + added - discontinued);
        
        // Generate summary text
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Reconciliation Summary:\n"));
        summary.append(String.format("- Medications before: %d\n", reconciliation.getTotalMedicationsBefore()));
        summary.append(String.format("- Medications added: %d\n", added));
        summary.append(String.format("- Medications modified: %d\n", modified));
        summary.append(String.format("- Medications discontinued: %d\n", discontinued));
        summary.append(String.format("- Medications unchanged: %d\n", unchanged));
        summary.append(String.format("- Medications after: %d", reconciliation.getTotalMedicationsAfter()));
        reconciliation.setReconciliationSummary(summary.toString());
    }
    
    /**
     * Get reconciliation by ID
     */
    public MedicationReconciliationResponse getReconciliationById(UUID reconciliationId) {
        MedicationReconciliation reconciliation = reconciliationRepository.findById(reconciliationId)
            .orElseThrow(() -> new RuntimeException("Reconciliation not found: " + reconciliationId));
        return mapToResponse(reconciliation);
    }
    
    /**
     * Get all reconciliations for a patient
     */
    public List<MedicationReconciliationResponse> getReconciliationsByPatient(UUID patientId) {
        List<MedicationReconciliation> reconciliations = 
            reconciliationRepository.findByPatientPatientIdOrderByReconciliationDateDesc(patientId);
        return reconciliations.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Complete reconciliation
     */
    @Transactional
    public MedicationReconciliationResponse completeReconciliation(
            UUID reconciliationId, UUID verifiedBy, String verifiedByName, UUID userId) {
        log.info("Completing reconciliation: {}", reconciliationId);
        
        MedicationReconciliation reconciliation = reconciliationRepository.findById(reconciliationId)
            .orElseThrow(() -> new RuntimeException("Reconciliation not found: " + reconciliationId));
        
        reconciliation.setReconciliationStatus(MedicationReconciliation.ReconciliationStatus.COMPLETED);
        reconciliation.setVerifiedBy(verifiedBy);
        reconciliation.setVerifiedByName(verifiedByName);
        reconciliation.setVerificationDate(LocalDateTime.now());
        reconciliation.setUpdatedBy(userId);
        
        reconciliation = reconciliationRepository.save(reconciliation);
        
        log.info("Completed reconciliation: {}", reconciliationId);
        return mapToResponse(reconciliation);
    }
    
    /**
     * Apply reconciliation actions to medications
     */
    @Transactional
    public MedicationReconciliationResponse applyReconciliation(UUID reconciliationId, UUID userId) {
        log.info("Applying reconciliation: {}", reconciliationId);
        
        MedicationReconciliation reconciliation = reconciliationRepository.findById(reconciliationId)
            .orElseThrow(() -> new RuntimeException("Reconciliation not found: " + reconciliationId));
        
        List<MedicationReconciliationComparison> comparisons = 
            comparisonRepository.findByReconciliationReconciliationId(reconciliationId);
        
        for (MedicationReconciliationComparison comp : comparisons) {
            if (comp.getActionTaken() == MedicationReconciliationComparison.ActionTaken.PENDING) {
                continue; // Skip pending actions
            }
            
            switch (comp.getComparisonStatus()) {
                case NEW:
                    if (comp.getActionTaken() == MedicationReconciliationComparison.ActionTaken.ADDED) {
                        // Create new medication from comparison
                        createMedicationFromComparison(comp, reconciliation.getPatient().getPatientId(), userId);
                    }
                    break;
                case CHANGED:
                    if (comp.getActionTaken() == MedicationReconciliationComparison.ActionTaken.MODIFIED) {
                        // Update existing medication
                        if (comp.getTargetMedicationId() != null) {
                            updateMedicationFromComparison(comp, comp.getTargetMedicationId(), userId);
                        }
                    }
                    break;
                case DISCONTINUED:
                    if (comp.getActionTaken() == MedicationReconciliationComparison.ActionTaken.DISCONTINUED) {
                        // Discontinue medication
                        if (comp.getTargetMedicationId() != null) {
                            medicationService.updateMedicationStatus(
                                comp.getTargetMedicationId(),
                                Medication.MedicationStatus.DISCONTINUED,
                                "Discontinued during medication reconciliation",
                                userId);
                        }
                    }
                    break;
            }
        }
        
        return mapToResponse(reconciliation);
    }
    
    private void createMedicationFromComparison(
            MedicationReconciliationComparison comp, UUID patientId, UUID userId) {
        // Implementation would create medication from comparison data
        // This is a simplified version
        log.info("Creating medication from comparison: {}", comp.getMedicationName());
    }
    
    private void updateMedicationFromComparison(
            MedicationReconciliationComparison comp, UUID medicationId, UUID userId) {
        // Implementation would update medication from comparison data
        log.info("Updating medication from comparison: {}", medicationId);
    }
    
    // Mapping methods
    private MedicationReconciliationResponse mapToResponse(MedicationReconciliation reconciliation) {
        List<MedicationReconciliationSource> sources = 
            sourceRepository.findByReconciliationReconciliationId(reconciliation.getReconciliationId());
        List<MedicationReconciliationComparison> comparisons = 
            comparisonRepository.findByReconciliationReconciliationId(reconciliation.getReconciliationId());
        
        return MedicationReconciliationResponse.builder()
            .reconciliationId(reconciliation.getReconciliationId())
            .patientId(reconciliation.getPatient().getPatientId())
            .encounterId(reconciliation.getEncounterId())
            .reconciliationDate(reconciliation.getReconciliationDate())
            .reconciliationType(reconciliation.getReconciliationType())
            .reconciliationStatus(reconciliation.getReconciliationStatus())
            .performedBy(reconciliation.getPerformedBy())
            .performedByName(reconciliation.getPerformedByName())
            .verifiedBy(reconciliation.getVerifiedBy())
            .verifiedByName(reconciliation.getVerifiedByName())
            .verificationDate(reconciliation.getVerificationDate())
            .totalMedicationsBefore(reconciliation.getTotalMedicationsBefore())
            .totalMedicationsAfter(reconciliation.getTotalMedicationsAfter())
            .medicationsAdded(reconciliation.getMedicationsAdded())
            .medicationsModified(reconciliation.getMedicationsModified())
            .medicationsDiscontinued(reconciliation.getMedicationsDiscontinued())
            .medicationsUnchanged(reconciliation.getMedicationsUnchanged())
            .notes(reconciliation.getNotes())
            .reconciliationSummary(reconciliation.getReconciliationSummary())
            .createdAt(reconciliation.getCreatedAt())
            .updatedAt(reconciliation.getUpdatedAt())
            .createdBy(reconciliation.getCreatedBy())
            .updatedBy(reconciliation.getUpdatedBy())
            .sources(sources.stream().map(this::mapSourceToResponse).collect(Collectors.toList()))
            .comparisons(comparisons.stream().map(this::mapComparisonToResponse).collect(Collectors.toList()))
            .build();
    }
    
    private MedicationReconciliationSourceResponse mapSourceToResponse(MedicationReconciliationSource source) {
        return MedicationReconciliationSourceResponse.builder()
            .sourceId(source.getSourceId())
            .reconciliationId(source.getReconciliation().getReconciliationId())
            .sourceType(source.getSourceType())
            .sourceName(source.getSourceName())
            .sourceDescription(source.getSourceDescription())
            .sourceData(source.getSourceData())
            .sourceDate(source.getSourceDate())
            .sourceProviderName(source.getSourceProviderName())
            .sourceFacilityName(source.getSourceFacilityName())
            .sourceContactInfo(source.getSourceContactInfo())
            .importedAt(source.getImportedAt())
            .importedBy(source.getImportedBy())
            .importMethod(source.getImportMethod())
            .createdAt(source.getCreatedAt())
            .createdBy(source.getCreatedBy())
            .build();
    }
    
    private MedicationReconciliationComparisonResponse mapComparisonToResponse(MedicationReconciliationComparison comparison) {
        return MedicationReconciliationComparisonResponse.builder()
            .comparisonId(comparison.getComparisonId())
            .reconciliationId(comparison.getReconciliation().getReconciliationId())
            .medicationName(comparison.getMedicationName())
            .genericName(comparison.getGenericName())
            .medicationCode(comparison.getMedicationCode())
            .medicationCodeType(comparison.getMedicationCodeType())
            .comparisonStatus(comparison.getComparisonStatus())
            .actionTaken(comparison.getActionTaken())
            .sourceMedicationId(comparison.getSourceMedicationId())
            .targetMedicationId(comparison.getTargetMedicationId())
            .beforeDosageStrength(comparison.getBeforeDosageStrength())
            .afterDosageStrength(comparison.getAfterDosageStrength())
            .beforeDosageUnit(comparison.getBeforeDosageUnit())
            .afterDosageUnit(comparison.getAfterDosageUnit())
            .beforeFrequency(comparison.getBeforeFrequency())
            .afterFrequency(comparison.getAfterFrequency())
            .beforeRoute(comparison.getBeforeRoute())
            .afterRoute(comparison.getAfterRoute())
            .beforeInstructions(comparison.getBeforeInstructions())
            .afterInstructions(comparison.getAfterInstructions())
            .differences(comparison.getDifferences())
            .resolutionNotes(comparison.getResolutionNotes())
            .createdAt(comparison.getCreatedAt())
            .createdBy(comparison.getCreatedBy())
            .build();
    }
}
