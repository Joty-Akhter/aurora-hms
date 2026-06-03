package com.easyops.hospital.service;

import com.easyops.hospital.dto.response.*;
import com.easyops.hospital.entity.*;
import com.easyops.hospital.repository.*;
import com.easyops.hospital.util.PatientDisplayName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicationReportingService {
    
    private final MedicationRepository medicationRepository;
    private final MedicationHistoryRepository medicationHistoryRepository;
    private final MedicationReconciliationRepository medicationReconciliationRepository;
    private final PatientRepository patientRepository;
    private final ProblemMedicationRepository problemMedicationRepository;
    
    // ========== Medication List Reports ==========
    
    /**
     * Generate complete medication list report (current + historical)
     */
    @Transactional(readOnly = true)
    public MedicationListReportResponse generateCompleteMedicationListReport(UUID patientId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating complete medication list report for patient: {}", patientId);
        
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));
        
        // Get current medications
        List<Medication> currentMedications = medicationRepository.findByPatientPatientIdOrderByStartDateDesc(patientId);
        
        // Get historical medications
        List<MedicationHistory> historicalMedications = medicationHistoryRepository.findByPatientPatientIdOrderByStartDateDesc(patientId);
        
        // Filter by date range if provided
        if (startDate != null && endDate != null) {
            currentMedications = currentMedications.stream()
                .filter(m -> (m.getStartDate() != null && !m.getStartDate().isAfter(endDate)) &&
                            (m.getEndDate() == null || !m.getEndDate().isBefore(startDate)))
                .collect(Collectors.toList());
            
            historicalMedications = historicalMedications.stream()
                .filter(m -> (m.getStartDate() != null && !m.getStartDate().isAfter(endDate)) &&
                            (m.getEndDate() == null || !m.getEndDate().isBefore(startDate)))
                .collect(Collectors.toList());
        }
        
        // Convert to responses
        List<MedicationResponse> allMedications = new ArrayList<>();
        allMedications.addAll(currentMedications.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList()));
        
        // Count by status
        Map<Medication.MedicationStatus, Long> statusCounts = currentMedications.stream()
            .collect(Collectors.groupingBy(Medication::getMedicationStatus, Collectors.counting()));
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalMedications", allMedications.size());
        summary.put("currentMedications", currentMedications.size());
        summary.put("historicalMedications", historicalMedications.size());
        summary.put("statusBreakdown", statusCounts);
        
        return MedicationListReportResponse.builder()
            .patientId(patientId)
            .patientName(PatientDisplayName.of(patient))
            .reportType("COMPLETE")
            .reportDate(LocalDate.now())
            .startDate(startDate)
            .endDate(endDate)
            .totalMedications(allMedications.size())
            .activeMedications(statusCounts.getOrDefault(Medication.MedicationStatus.ACTIVE, 0L).intValue())
            .discontinuedMedications(statusCounts.getOrDefault(Medication.MedicationStatus.DISCONTINUED, 0L).intValue())
            .onHoldMedications(statusCounts.getOrDefault(Medication.MedicationStatus.ON_HOLD, 0L).intValue())
            .completedMedications(statusCounts.getOrDefault(Medication.MedicationStatus.COMPLETED, 0L).intValue())
            .medications(allMedications)
            .summary(summary)
            .build();
    }
    
    /**
     * Generate current medication list report
     */
    @Transactional(readOnly = true)
    public MedicationListReportResponse generateCurrentMedicationListReport(UUID patientId) {
        log.info("Generating current medication list report for patient: {}", patientId);
        
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));
        
        List<Medication> medications = medicationRepository.findByPatientPatientIdOrderByStartDateDesc(patientId);
        
        Map<Medication.MedicationStatus, Long> statusCounts = medications.stream()
            .collect(Collectors.groupingBy(Medication::getMedicationStatus, Collectors.counting()));
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("statusBreakdown", statusCounts);
        
        return MedicationListReportResponse.builder()
            .patientId(patientId)
            .patientName(PatientDisplayName.of(patient))
            .reportType("CURRENT")
            .reportDate(LocalDate.now())
            .totalMedications(medications.size())
            .activeMedications(statusCounts.getOrDefault(Medication.MedicationStatus.ACTIVE, 0L).intValue())
            .discontinuedMedications(statusCounts.getOrDefault(Medication.MedicationStatus.DISCONTINUED, 0L).intValue())
            .onHoldMedications(statusCounts.getOrDefault(Medication.MedicationStatus.ON_HOLD, 0L).intValue())
            .completedMedications(statusCounts.getOrDefault(Medication.MedicationStatus.COMPLETED, 0L).intValue())
            .medications(medications.stream().map(this::mapToResponse).collect(Collectors.toList()))
            .summary(summary)
            .build();
    }
    
    /**
     * Generate historical medication list report
     */
    @Transactional(readOnly = true)
    public MedicationListReportResponse generateHistoricalMedicationListReport(UUID patientId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating historical medication list report for patient: {}", patientId);
        
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));
        
        List<MedicationHistory> history = medicationHistoryRepository.findByPatientPatientIdOrderByStartDateDesc(patientId);
        
        if (startDate != null && endDate != null) {
            history = history.stream()
                .filter(m -> (m.getStartDate() != null && !m.getStartDate().isAfter(endDate)) &&
                            (m.getEndDate() == null || !m.getEndDate().isBefore(startDate)))
                .collect(Collectors.toList());
        }
        
        Map<Medication.MedicationStatus, Long> statusCounts = history.stream()
            .collect(Collectors.groupingBy(MedicationHistory::getMedicationStatus, Collectors.counting()));
        
        List<MedicationResponse> medications = history.stream()
            .map(this::mapHistoryToResponse)
            .collect(Collectors.toList());
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("statusBreakdown", statusCounts);
        
        return MedicationListReportResponse.builder()
            .patientId(patientId)
            .patientName(PatientDisplayName.of(patient))
            .reportType("HISTORICAL")
            .reportDate(LocalDate.now())
            .startDate(startDate)
            .endDate(endDate)
            .totalMedications(medications.size())
            .discontinuedMedications(statusCounts.getOrDefault(Medication.MedicationStatus.DISCONTINUED, 0L).intValue())
            .medications(medications)
            .summary(summary)
            .build();
    }
    
    // ========== Medications by Indication Reports ==========
    
    /**
     * Generate medications by indication report
     */
    @Transactional(readOnly = true)
    public MedicationIndicationReportResponse generateMedicationsByIndicationReport(UUID patientId, String indication, LocalDate startDate, LocalDate endDate) {
        log.info("Generating medications by indication report for patient: {}, indication: {}", patientId, indication);
        
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));
        
        List<Medication> medications;
        if (indication != null && !indication.trim().isEmpty()) {
            medications = medicationRepository.findByPatientIdAndIndicationContaining(patientId, indication);
        } else {
            medications = medicationRepository.findByPatientPatientIdOrderByStartDateDesc(patientId);
        }
        
        if (startDate != null && endDate != null) {
            medications = medications.stream()
                .filter(m -> (m.getStartDate() != null && !m.getStartDate().isAfter(endDate)) &&
                            (m.getEndDate() == null || !m.getEndDate().isBefore(startDate)))
                .collect(Collectors.toList());
        }
        
        // Group by indication
        Map<String, List<Medication>> byIndication = medications.stream()
            .filter(m -> m.getIndication() != null && !m.getIndication().trim().isEmpty())
            .collect(Collectors.groupingBy(Medication::getIndication));
        
        List<MedicationIndicationReportResponse.IndicationSummary> indicationSummaries = byIndication.entrySet().stream()
            .map(entry -> {
                String ind = entry.getKey();
                List<Medication> meds = entry.getValue();
                long activeCount = meds.stream()
                    .filter(m -> m.getMedicationStatus() == Medication.MedicationStatus.ACTIVE)
                    .count();
                long discontinuedCount = meds.stream()
                    .filter(m -> m.getMedicationStatus() == Medication.MedicationStatus.DISCONTINUED)
                    .count();
                
                return MedicationIndicationReportResponse.IndicationSummary.builder()
                    .indication(ind)
                    .medicationCount(meds.size())
                    .activeCount((int) activeCount)
                    .discontinuedCount((int) discontinuedCount)
                    .build();
            })
            .collect(Collectors.toList());
        
        return MedicationIndicationReportResponse.builder()
            .patientId(patientId)
            .patientName(PatientDisplayName.of(patient))
            .indication(indication)
            .reportDate(LocalDate.now())
            .startDate(startDate)
            .endDate(endDate)
            .totalMedications(medications.size())
            .medications(medications.stream().map(this::mapToResponse).collect(Collectors.toList()))
            .indicationSummaries(indicationSummaries)
            .build();
    }
    
    // ========== Medication Adherence Reports ==========
    
    /**
     * Generate medication adherence report
     * Note: This is a simplified implementation. Real adherence would require prescription fill data.
     */
    @Transactional(readOnly = true)
    public MedicationAdherenceReportResponse generateMedicationAdherenceReport(UUID patientId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating medication adherence report for patient: {}", patientId);
        
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));
        
        List<Medication> medications = medicationRepository.findByPatientPatientIdOrderByStartDateDesc(patientId);
        
        if (startDate != null && endDate != null) {
            medications = medications.stream()
                .filter(m -> (m.getStartDate() != null && !m.getStartDate().isAfter(endDate)) &&
                            (m.getEndDate() == null || !m.getEndDate().isBefore(startDate)))
                .collect(Collectors.toList());
        }
        
        List<MedicationAdherenceReportResponse.MedicationAdherenceDetail> details = new ArrayList<>();
        int adherentCount = 0;
        int nonAdherentCount = 0;
        
        for (Medication med : medications) {
            // Simplified adherence calculation based on medication status and refills
            // In a real system, this would use prescription fill history
            BigDecimal adherenceRate = calculateAdherenceRate(med);
            String adherenceStatus = determineAdherenceStatus(adherenceRate);
            
            if (adherenceStatus.equals("ADHERENT")) {
                adherentCount++;
            } else if (adherenceStatus.equals("NON_ADHERENT")) {
                nonAdherentCount++;
            }
            
            // Estimate expected vs actual doses based on frequency and duration
            int expectedDoses = estimateExpectedDoses(med, startDate != null ? startDate : med.getStartDate(), 
                endDate != null ? endDate : (med.getEndDate() != null ? med.getEndDate() : LocalDate.now()));
            int actualDoses = (int) (expectedDoses * adherenceRate.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).doubleValue());
            int missedDoses = expectedDoses - actualDoses;
            
            details.add(MedicationAdherenceReportResponse.MedicationAdherenceDetail.builder()
                .medicationId(med.getMedicationId())
                .medicationName(med.getMedicationName())
                .indication(med.getIndication())
                .startDate(med.getStartDate())
                .endDate(med.getEndDate())
                .adherenceRate(adherenceRate)
                .expectedDoses(expectedDoses)
                .actualDoses(actualDoses)
                .missedDoses(missedDoses)
                .adherenceStatus(adherenceStatus)
                .build());
        }
        
        BigDecimal overallAdherence = details.isEmpty() ? BigDecimal.ZERO :
            details.stream()
                .map(MedicationAdherenceReportResponse.MedicationAdherenceDetail::getAdherenceRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(details.size()), 2, RoundingMode.HALF_UP);
        
        return MedicationAdherenceReportResponse.builder()
            .patientId(patientId)
            .patientName(PatientDisplayName.of(patient))
            .reportDate(LocalDate.now())
            .startDate(startDate)
            .endDate(endDate)
            .overallAdherenceRate(overallAdherence)
            .totalMedications(medications.size())
            .adherentMedications(adherentCount)
            .nonAdherentMedications(nonAdherentCount)
            .medicationDetails(details)
            .build();
    }
    
    // ========== Medication List Completeness Metrics ==========
    
    /**
     * Generate medication list completeness metrics
     */
    @Transactional(readOnly = true)
    public MedicationCompletenessMetricsResponse generateMedicationCompletenessMetrics(UUID patientId) {
        log.info("Generating medication completeness metrics for patient: {}", patientId);
        
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));
        
        List<Medication> medications = medicationRepository.findByPatientPatientIdOrderByStartDateDesc(patientId);
        
        List<MedicationCompletenessMetricsResponse.CompletenessDetail> details = new ArrayList<>();
        Map<String, Integer> missingFieldCounts = new HashMap<>();
        int completeCount = 0;
        int incompleteCount = 0;
        
        for (Medication med : medications) {
            List<String> missingFields = new ArrayList<>();
            List<String> incompleteFields = new ArrayList<>();
            
            // Check required fields
            if (med.getMedicationName() == null || med.getMedicationName().trim().isEmpty()) {
                missingFields.add("medicationName");
            }
            if (med.getStartDate() == null) {
                missingFields.add("startDate");
            }
            if (med.getDosageStrength() == null) {
                missingFields.add("dosageStrength");
            }
            if (med.getDosageUnit() == null || med.getDosageUnit().trim().isEmpty()) {
                missingFields.add("dosageUnit");
            }
            if (med.getRoute() == null) {
                missingFields.add("route");
            }
            if (med.getFrequency() == null || med.getFrequency().trim().isEmpty()) {
                missingFields.add("frequency");
            }
            
            // Check optional but important fields
            if (med.getIndication() == null || med.getIndication().trim().isEmpty()) {
                incompleteFields.add("indication");
            }
            if (med.getPrescribingProviderName() == null || med.getPrescribingProviderName().trim().isEmpty()) {
                incompleteFields.add("prescribingProviderName");
            }
            if (med.getInstructions() == null || med.getInstructions().trim().isEmpty()) {
                incompleteFields.add("instructions");
            }
            
            // Count missing fields
            for (String field : missingFields) {
                missingFieldCounts.put(field, missingFieldCounts.getOrDefault(field, 0) + 1);
            }
            for (String field : incompleteFields) {
                missingFieldCounts.put(field, missingFieldCounts.getOrDefault(field, 0) + 1);
            }
            
            // Calculate completeness score (0-100)
            int totalFields = 9; // Total fields checked
            int missingCount = missingFields.size() + incompleteFields.size();
            BigDecimal completenessScore = BigDecimal.valueOf(100 - (missingCount * 100.0 / totalFields))
                .setScale(2, RoundingMode.HALF_UP);
            
            if (completenessScore.compareTo(BigDecimal.valueOf(90)) >= 0) {
                completeCount++;
            } else {
                incompleteCount++;
            }
            
            details.add(MedicationCompletenessMetricsResponse.CompletenessDetail.builder()
                .medicationId(med.getMedicationId())
                .medicationName(med.getMedicationName())
                .completenessScore(completenessScore)
                .missingFields(missingFields)
                .incompleteFields(incompleteFields)
                .build());
        }
        
        BigDecimal overallScore = details.isEmpty() ? BigDecimal.ZERO :
            details.stream()
                .map(MedicationCompletenessMetricsResponse.CompletenessDetail::getCompletenessScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(details.size()), 2, RoundingMode.HALF_UP);
        
        return MedicationCompletenessMetricsResponse.builder()
            .patientId(patientId)
            .patientName(PatientDisplayName.of(patient))
            .reportDate(LocalDate.now())
            .completenessScore(overallScore)
            .totalMedications(medications.size())
            .completeMedications(completeCount)
            .incompleteMedications(incompleteCount)
            .completenessDetails(details)
            .missingFieldCounts(missingFieldCounts)
            .build();
    }
    
    // ========== Clinical Reports ==========
    
    /**
     * Generate medications by provider report
     */
    @Transactional(readOnly = true)
    public MedicationClinicalReportResponse generateMedicationsByProviderReport(UUID patientId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating medications by provider report for patient: {}", patientId);
        
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));
        
        List<Medication> medications = medicationRepository.findByPatientPatientIdOrderByStartDateDesc(patientId);
        
        if (startDate != null && endDate != null) {
            medications = medications.stream()
                .filter(m -> (m.getStartDate() != null && !m.getStartDate().isAfter(endDate)) &&
                            (m.getEndDate() == null || !m.getEndDate().isBefore(startDate)))
                .collect(Collectors.toList());
        }
        
        // Group by provider
        Map<UUID, List<Medication>> byProvider = medications.stream()
            .filter(m -> m.getPrescribingProviderId() != null)
            .collect(Collectors.groupingBy(Medication::getPrescribingProviderId));
        
        List<MedicationClinicalReportResponse.ProviderMedicationSummary> providerSummaries = byProvider.entrySet().stream()
            .map(entry -> {
                UUID providerId = entry.getKey();
                List<Medication> meds = entry.getValue();
                Medication firstMed = meds.get(0);
                
                long activeCount = meds.stream()
                    .filter(m -> m.getMedicationStatus() == Medication.MedicationStatus.ACTIVE)
                    .count();
                
                return MedicationClinicalReportResponse.ProviderMedicationSummary.builder()
                    .providerId(providerId)
                    .providerName(firstMed.getPrescribingProviderName())
                    .providerNpi(firstMed.getPrescribingProviderNpi())
                    .totalMedications(meds.size())
                    .activeMedications((int) activeCount)
                    .medications(meds.stream().map(this::mapToResponse).collect(Collectors.toList()))
                    .build();
            })
            .collect(Collectors.toList());
        
        return MedicationClinicalReportResponse.builder()
            .patientId(patientId)
            .patientName(PatientDisplayName.of(patient))
            .reportDate(LocalDate.now())
            .startDate(startDate)
            .endDate(endDate)
            .reportType("BY_PROVIDER")
            .providerSummaries(providerSummaries)
            .build();
    }
    
    /**
     * Generate medications by problem report
     */
    @Transactional(readOnly = true)
    public MedicationClinicalReportResponse generateMedicationsByProblemReport(UUID patientId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating medications by problem report for patient: {}", patientId);
        
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));
        
        // Get all medications for patient first
        List<Medication> patientMedications = medicationRepository.findByPatientPatientIdOrderByStartDateDesc(patientId);
        
        // Filter by date range if provided
        if (startDate != null && endDate != null) {
            patientMedications = patientMedications.stream()
                .filter(m -> (m.getStartDate() != null && !m.getStartDate().isAfter(endDate)) &&
                            (m.getEndDate() == null || !m.getEndDate().isBefore(startDate)))
                .collect(Collectors.toList());
        }
        
        // Get problem medications for these medications
        List<ProblemMedication> problemMedications = new ArrayList<>();
        for (Medication med : patientMedications) {
            List<ProblemMedication> pms = problemMedicationRepository.findByMedicationMedicationId(med.getMedicationId());
            problemMedications.addAll(pms);
        }
        
        // Group by problem
        Map<UUID, List<ProblemMedication>> byProblem = problemMedications.stream()
            .collect(Collectors.groupingBy(pm -> pm.getProblem().getProblemId()));
        
        List<MedicationClinicalReportResponse.ProblemMedicationSummary> problemSummaries = byProblem.entrySet().stream()
            .map(entry -> {
                UUID problemId = entry.getKey();
                List<ProblemMedication> pms = entry.getValue();
                PatientProblem problem = pms.get(0).getProblem();
                
                List<Medication> meds = pms.stream()
                    .map(ProblemMedication::getMedication)
                    .collect(Collectors.toList());
                
                long activeCount = meds.stream()
                    .filter(m -> m.getMedicationStatus() == Medication.MedicationStatus.ACTIVE)
                    .count();
                
                return MedicationClinicalReportResponse.ProblemMedicationSummary.builder()
                    .problemId(problemId)
                    .problemName(problem.getProblemName())
                    .diagnosisCode(problem.getIcd10Code() != null ? problem.getIcd10Code() : problem.getIcd11Code())
                    .totalMedications(meds.size())
                    .activeMedications((int) activeCount)
                    .medications(meds.stream().map(this::mapToResponse).collect(Collectors.toList()))
                    .build();
            })
            .collect(Collectors.toList());
        
        return MedicationClinicalReportResponse.builder()
            .patientId(patientId)
            .patientName(PatientDisplayName.of(patient))
            .reportDate(LocalDate.now())
            .startDate(startDate)
            .endDate(endDate)
            .reportType("BY_PROBLEM")
            .problemSummaries(problemSummaries)
            .build();
    }
    
    // ========== Quality Metrics ==========
    
    /**
     * Generate medication quality metrics report
     */
    @Transactional(readOnly = true)
    public MedicationQualityMetricsResponse generateMedicationQualityMetrics(UUID patientId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating medication quality metrics for patient: {}", patientId);
        
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));
        
        List<Medication> medications = medicationRepository.findByPatientPatientIdOrderByStartDateDesc(patientId);
        
        if (startDate != null && endDate != null) {
            medications = medications.stream()
                .filter(m -> (m.getStartDate() != null && !m.getStartDate().isAfter(endDate)) &&
                            (m.getEndDate() == null || !m.getEndDate().isBefore(startDate)))
                .collect(Collectors.toList());
        }
        
        // Calculate data quality metrics
        int completeDataCount = 0;
        int missingDataCount = 0;
        Map<String, Integer> dataCompletenessByField = new HashMap<>();
        int duplicateCount = 0;
        int conflictingDataCount = 0;
        List<MedicationQualityMetricsResponse.QualityIssue> qualityIssues = new ArrayList<>();
        
        for (Medication med : medications) {
            boolean isComplete = true;
            
            // Check for missing critical fields
            if (med.getMedicationName() == null || med.getMedicationName().trim().isEmpty()) {
                isComplete = false;
                dataCompletenessByField.put("medicationName", dataCompletenessByField.getOrDefault("medicationName", 0) + 1);
                qualityIssues.add(createQualityIssue("MISSING_DATA", "HIGH", "Missing medication name", med));
            }
            if (med.getDosageStrength() == null) {
                isComplete = false;
                dataCompletenessByField.put("dosageStrength", dataCompletenessByField.getOrDefault("dosageStrength", 0) + 1);
            }
            if (med.getRoute() == null) {
                isComplete = false;
                dataCompletenessByField.put("route", dataCompletenessByField.getOrDefault("route", 0) + 1);
            }
            if (med.getFrequency() == null || med.getFrequency().trim().isEmpty()) {
                isComplete = false;
                dataCompletenessByField.put("frequency", dataCompletenessByField.getOrDefault("frequency", 0) + 1);
            }
            
            if (isComplete) {
                completeDataCount++;
            } else {
                missingDataCount++;
            }
        }
        
        // Check for duplicates (same medication name, similar dosage)
        Set<String> seenMedications = new HashSet<>();
        for (Medication med : medications) {
            String key = med.getMedicationName() + "|" + med.getDosageStrength() + "|" + med.getDosageUnit();
            if (seenMedications.contains(key)) {
                duplicateCount++;
                qualityIssues.add(createQualityIssue("DUPLICATE", "MEDIUM", "Duplicate medication entry", med));
            } else {
                seenMedications.add(key);
            }
        }
        
        // Calculate data quality score
        BigDecimal dataQualityScore = medications.isEmpty() ? BigDecimal.ZERO :
            BigDecimal.valueOf(completeDataCount * 100.0 / medications.size())
                .setScale(2, RoundingMode.HALF_UP);
        
        // Calculate reconciliation compliance
        List<MedicationReconciliation> reconciliations = medicationReconciliationRepository
            .findByPatientPatientIdOrderByReconciliationDateDesc(patientId);
        
        if (startDate != null && endDate != null) {
            reconciliations = reconciliations.stream()
                .filter(r -> !r.getReconciliationDate().isBefore(startDate) && !r.getReconciliationDate().isAfter(endDate))
                .collect(Collectors.toList());
        }
        
        long completedReconciliations = reconciliations.stream()
            .filter(r -> r.getReconciliationStatus() == MedicationReconciliation.ReconciliationStatus.COMPLETED)
            .count();
        
        long pendingReconciliations = reconciliations.stream()
            .filter(r -> r.getReconciliationStatus() == MedicationReconciliation.ReconciliationStatus.IN_PROGRESS)
            .count();
        
        long overdueReconciliations = reconciliations.stream()
            .filter(r -> r.getReconciliationStatus() == MedicationReconciliation.ReconciliationStatus.IN_PROGRESS &&
                        r.getReconciliationDate().isBefore(LocalDate.now().minusDays(30)))
            .count();
        
        LocalDate lastReconciliationDate = reconciliations.isEmpty() ? null :
            reconciliations.get(0).getReconciliationDate();
        
        int daysSinceLastReconciliation = lastReconciliationDate == null ? -1 :
            (int) ChronoUnit.DAYS.between(lastReconciliationDate, LocalDate.now());
        
        BigDecimal complianceRate = reconciliations.isEmpty() ? BigDecimal.ZERO :
            BigDecimal.valueOf(completedReconciliations * 100.0 / reconciliations.size())
                .setScale(2, RoundingMode.HALF_UP);
        
        // Calculate overall quality score (weighted average)
        BigDecimal overallQualityScore = dataQualityScore.multiply(BigDecimal.valueOf(0.6))
            .add(complianceRate.multiply(BigDecimal.valueOf(0.4)))
            .setScale(2, RoundingMode.HALF_UP);
        
        return MedicationQualityMetricsResponse.builder()
            .patientId(patientId)
            .patientName(PatientDisplayName.of(patient))
            .reportDate(LocalDate.now())
            .startDate(startDate)
            .endDate(endDate)
            .overallQualityScore(overallQualityScore)
            .medicationListQuality(MedicationQualityMetricsResponse.MedicationListQualityMetrics.builder()
                .dataQualityScore(dataQualityScore)
                .totalMedications(medications.size())
                .medicationsWithCompleteData(completeDataCount)
                .medicationsWithMissingData(missingDataCount)
                .dataCompletenessByField(dataCompletenessByField)
                .duplicateMedications(duplicateCount)
                .medicationsWithConflictingData(conflictingDataCount)
                .build())
            .reconciliationCompliance(MedicationQualityMetricsResponse.ReconciliationComplianceMetrics.builder()
                .complianceRate(complianceRate)
                .totalReconciliations(reconciliations.size())
                .completedReconciliations((int) completedReconciliations)
                .pendingReconciliations((int) pendingReconciliations)
                .overdueReconciliations((int) overdueReconciliations)
                .lastReconciliationDate(lastReconciliationDate)
                .daysSinceLastReconciliation(daysSinceLastReconciliation)
                .build())
            .qualityIssues(qualityIssues)
            .build();
    }
    
    // ========== Helper Methods ==========
    
    private MedicationResponse mapToResponse(Medication medication) {
        return MedicationResponse.builder()
            .medicationId(medication.getMedicationId())
            .patientId(medication.getPatient().getPatientId())
            .encounterId(medication.getEncounterId())
            .medicationName(medication.getMedicationName())
            .genericName(medication.getGenericName())
            .medicationCode(medication.getMedicationCode())
            .medicationCodeType(medication.getMedicationCodeType())
            .ndcCode(medication.getNdcCode())
            .rxnormCode(medication.getRxnormCode())
            .dosageStrength(medication.getDosageStrength())
            .dosageUnit(medication.getDosageUnit())
            .dosageForm(medication.getDosageForm())
            .quantity(medication.getQuantity())
            .quantityUnit(medication.getQuantityUnit())
            .route(medication.getRoute())
            .frequency(medication.getFrequency())
            .timing(medication.getTiming())
            .instructions(medication.getInstructions())
            .prescriptionId(medication.getPrescriptionId())
            .prescribingProviderId(medication.getPrescribingProviderId())
            .prescribingProviderName(medication.getPrescribingProviderName())
            .prescribingProviderNpi(medication.getPrescribingProviderNpi())
            .prescriptionDate(medication.getPrescriptionDate())
            .pharmacyId(medication.getPharmacyId())
            .pharmacyName(medication.getPharmacyName())
            .refillsAuthorized(medication.getRefillsAuthorized())
            .refillsRemaining(medication.getRefillsRemaining())
            .medicationStatus(medication.getMedicationStatus())
            .statusDate(medication.getStatusDate())
            .statusChangedBy(medication.getStatusChangedBy())
            .indication(medication.getIndication())
            .diagnosisCode(medication.getDiagnosisCode())
            .medicationSource(medication.getMedicationSource())
            .startDate(medication.getStartDate())
            .endDate(medication.getEndDate())
            .lastFilledDate(medication.getLastFilledDate())
            .notes(medication.getNotes())
            .specialInstructions(medication.getSpecialInstructions())
            .createdAt(medication.getCreatedAt())
            .updatedAt(medication.getUpdatedAt())
            .createdBy(medication.getCreatedBy())
            .updatedBy(medication.getUpdatedBy())
            .build();
    }
    
    private MedicationResponse mapHistoryToResponse(MedicationHistory history) {
        return MedicationResponse.builder()
            .medicationId(history.getMedication().getMedicationId())
            .patientId(history.getPatient().getPatientId())
            .medicationName(history.getMedicationName())
            .genericName(history.getGenericName())
            .medicationCode(history.getMedicationCode())
            .medicationCodeType(history.getMedicationCodeType())
            .dosageStrength(history.getDosageStrength())
            .dosageUnit(history.getDosageUnit())
            .dosageForm(history.getDosageForm())
            .route(history.getRoute())
            .frequency(history.getFrequency())
            .instructions(history.getInstructions())
            .indication(history.getIndication())
            .diagnosisCode(history.getDiagnosisCode())
            .medicationStatus(history.getMedicationStatus())
            .statusDate(history.getStatusDate())
            .startDate(history.getStartDate())
            .endDate(history.getEndDate())
            .prescribingProviderName(history.getPrescribingProviderName())
            .medicationSource(history.getMedicationSource())
            .notes(history.getNotes())
            .createdAt(history.getCreatedAt())
            .build();
    }
    
    private BigDecimal calculateAdherenceRate(Medication medication) {
        // Simplified calculation - in real system, use prescription fill history
        if (medication.getMedicationStatus() == Medication.MedicationStatus.ACTIVE) {
            // If medication is active and has refills remaining, assume good adherence
            if (medication.getRefillsRemaining() != null && medication.getRefillsRemaining() > 0) {
                return BigDecimal.valueOf(85); // Assume 85% adherence
            }
            // If no refills but still active, check if recently started
            if (medication.getStartDate() != null && 
                ChronoUnit.DAYS.between(medication.getStartDate(), LocalDate.now()) < 30) {
                return BigDecimal.valueOf(90); // New medication, assume good adherence
            }
            return BigDecimal.valueOf(75); // Active but older, moderate adherence
        } else if (medication.getMedicationStatus() == Medication.MedicationStatus.DISCONTINUED) {
            return BigDecimal.valueOf(50); // Discontinued, lower adherence
        }
        return BigDecimal.valueOf(80); // Default
    }
    
    private String determineAdherenceStatus(BigDecimal adherenceRate) {
        if (adherenceRate.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "ADHERENT";
        } else if (adherenceRate.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return "PARTIAL";
        } else {
            return "NON_ADHERENT";
        }
    }
    
    private int estimateExpectedDoses(Medication medication, LocalDate start, LocalDate end) {
        if (medication.getFrequency() == null || medication.getFrequency().trim().isEmpty()) {
            return 0;
        }
        
        long days = ChronoUnit.DAYS.between(start, end);
        if (days <= 0) {
            return 0;
        }
        
        // Parse frequency (simplified - assumes format like "once daily", "twice daily", "every 8 hours")
        String frequency = medication.getFrequency().toLowerCase();
        int dosesPerDay = 1;
        
        if (frequency.contains("twice") || frequency.contains("2x")) {
            dosesPerDay = 2;
        } else if (frequency.contains("three") || frequency.contains("3x") || frequency.contains("tid")) {
            dosesPerDay = 3;
        } else if (frequency.contains("four") || frequency.contains("4x") || frequency.contains("qid")) {
            dosesPerDay = 4;
        } else if (frequency.contains("every 8") || frequency.contains("q8h")) {
            dosesPerDay = 3;
        } else if (frequency.contains("every 6") || frequency.contains("q6h")) {
            dosesPerDay = 4;
        } else if (frequency.contains("every 12") || frequency.contains("q12h")) {
            dosesPerDay = 2;
        }
        
        return (int) (days * dosesPerDay);
    }
    
    private MedicationQualityMetricsResponse.QualityIssue createQualityIssue(
            String issueType, String severity, String description, Medication medication) {
        return MedicationQualityMetricsResponse.QualityIssue.builder()
            .issueType(issueType)
            .severity(severity)
            .description(description)
            .medicationId(medication.getMedicationId())
            .medicationName(medication.getMedicationName())
            .recommendation("Review and update medication information")
            .build();
    }
}
