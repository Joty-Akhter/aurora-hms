package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.LabResultRequest;
import com.easyops.hospital.dto.response.*;
import com.easyops.hospital.entity.*;
import com.easyops.hospital.repository.*;
import com.easyops.hospital.repository.ClinicalNoteRepository;
import com.easyops.hospital.util.PatientDisplayName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabResultService {
    
    private final LabResultRepository labResultRepository;
    private final LabOrderRepository labOrderRepository;
    private final PatientRepository patientRepository;
    private final LabResultHistoryRepository labResultHistoryRepository;
    private final LabCriticalValueAlertRepository labCriticalValueAlertRepository;
    private final LabResultProblemRepository labResultProblemRepository;
    private final LabResultMedicationRepository labResultMedicationRepository;
    private final LabResultClinicalNoteRepository labResultClinicalNoteRepository;
    private final LabResultValueRepository labResultValueRepository;
    private final DrugLabInteractionAlertRepository drugLabInteractionAlertRepository;
    private final PatientProblemRepository patientProblemRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ClinicalNoteRepository clinicalNoteRepository;
    
    // ========== Lab Result CRUD Operations ==========
    
    /**
     * Create a new lab result
     */
    @Transactional
    public LabResultResponse createLabResult(LabResultRequest request, UUID userId) {
        log.info("Creating lab result for order: {}", request.getOrderId());
        
        // Verify order exists
        LabOrder order = labOrderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new RuntimeException("Lab order not found: " + request.getOrderId()));
        
        // Get patient from order
        Patient patient = order.getPatient();
        if (request.getPatientId() != null && !request.getPatientId().equals(patient.getPatientId())) {
            throw new RuntimeException("Patient ID mismatch with order");
        }
        
        // Generate result number if not provided
        String resultNumber = request.getResultNumber();
        if (resultNumber == null || resultNumber.isEmpty()) {
            resultNumber = generateResultNumber();
        }
        
        // Calculate abnormal flags and critical values
        LabResult.AbnormalFlag abnormalFlag = calculateAbnormalFlag(request);
        boolean isCritical = detectCriticalValue(request, abnormalFlag);
        boolean isDeltaCheck = performDeltaCheck(request, patient.getPatientId());
        
        // Calculate clinical significance
        LabResult.ClinicalSignificance clinicalSignificance = calculateClinicalSignificance(request, abnormalFlag, isCritical, isDeltaCheck);
        LabResult.ClinicalSignificanceLevel clinicalSignificanceLevel = calculateClinicalSignificanceLevel(clinicalSignificance, isCritical, abnormalFlag);
        
        // Build lab result entity
        LabResult labResult = LabResult.builder()
            .order(order)
            .patient(patient)
            .encounterId(request.getEncounterId() != null ? request.getEncounterId() : order.getEncounterId())
            .organizationId(order.getOrganizationId())
            .resultNumber(resultNumber)
            .testName(request.getTestName())
            .loincCode(request.getLoincCode())
            .testCategory(request.getTestCategory())
            .testType(request.getTestType())
            .resultValue(request.getResultValue())
            .resultValueNumeric(request.getResultValueNumeric())
            .resultUnits(request.getResultUnits())
            .resultType(request.getResultType())
            .qualitativeResult(request.getQualitativeResult())
            .quantitativeResult(request.getQuantitativeResult())
            .resultStatus(request.getResultStatus() != null ? request.getResultStatus() : LabResult.ResultStatus.FINAL)
            .referenceRangeLow(request.getReferenceRangeLow())
            .referenceRangeHigh(request.getReferenceRangeHigh())
            .referenceRangeUnits(request.getReferenceRangeUnits())
            .referenceRangeText(request.getReferenceRangeText())
            .referenceRangeSource(request.getReferenceRangeSource())
            .ageSpecificRange(request.getAgeSpecificRange() != null ? request.getAgeSpecificRange() : false)
            .genderSpecificRange(request.getGenderSpecificRange() != null ? request.getGenderSpecificRange() : false)
            .abnormalFlag(abnormalFlag)
            .isCriticalValue(isCritical)
            .isDeltaCheck(isDeltaCheck)
            .isPanicValue(request.getIsPanicValue() != null ? request.getIsPanicValue() : false)
            .resultInterpretation(request.getResultInterpretation())
            .clinicalSignificance(clinicalSignificance)
            .clinicalSignificanceLevel(clinicalSignificanceLevel)
            .interpretationNotes(request.getInterpretationNotes())
            .orderDate(request.getOrderDate() != null ? request.getOrderDate() : order.getOrderDate())
            .specimenCollectionDate(request.getSpecimenCollectionDate())
            .specimenReceivedDate(request.getSpecimenReceivedDate())
            .resultDate(request.getResultDate())
            .resultReportedDate(request.getResultReportedDate())
            .resultVerifiedDate(request.getResultVerifiedDate())
            .specimenType(request.getSpecimenType())
            .specimenSource(request.getSpecimenSource())
            .specimenCollectionMethod(request.getSpecimenCollectionMethod())
            .specimenId(request.getSpecimenId())
            .specimenVolume(request.getSpecimenVolume())
            .specimenQuality(request.getSpecimenQuality())
            .performingLaboratoryName(request.getPerformingLaboratoryName())
            .laboratoryId(request.getLaboratoryId())
            .laboratoryNpi(request.getLaboratoryNpi())
            .laboratoryAddressLine1(request.getLaboratoryAddressLine1())
            .laboratoryAddressLine2(request.getLaboratoryAddressLine2())
            .laboratoryCity(request.getLaboratoryCity())
            .laboratoryState(request.getLaboratoryState())
            .laboratoryZip(request.getLaboratoryZip())
            .laboratoryPhone(request.getLaboratoryPhone())
            .performingTechnologist(request.getPerformingTechnologist())
            .reviewingPathologist(request.getReviewingPathologist())
            .reviewingPhysician(request.getReviewingPhysician())
            .laboratoryReferenceNumber(request.getLaboratoryReferenceNumber())
            .laboratoryComments(request.getLaboratoryComments())
            .providerComments(request.getProviderComments())
            .resultNotes(request.getResultNotes())
            .methodUsed(request.getMethodUsed())
            .isCriticalValueAcknowledged(false)
            .isReviewed(false)
            .isCorrected(false)
            .isAmended(false)
            .isCancelled(false)
            .createdBy(userId)
            .build();
        
        LabResult savedResult = labResultRepository.save(labResult);
        log.info("Created lab result: {} for patient: {}", savedResult.getResultId(), patient.getPatientId());
        
        // Create history record
        createHistoryRecord(savedResult, LabResultHistory.ChangeType.CREATED, userId, null, null, "Lab result created");
        
        // Create critical value alert if needed
        if (isCritical) {
            createCriticalValueAlert(savedResult, order, patient, userId);
        }
        
        // Check for drug-lab interactions
        checkDrugLabInteractions(savedResult, patient, userId);
        
        // Update order status if result is final
        if (savedResult.getResultStatus() == LabResult.ResultStatus.FINAL) {
            order.setOrderStatus(LabOrder.OrderStatus.COMPLETED);
            labOrderRepository.save(order);
        }
        
        return mapToResponse(savedResult);
    }
    
    /**
     * Get lab result by ID
     */
    public LabResultResponse getLabResultById(UUID resultId) {
        LabResult result = labResultRepository.findById(resultId)
            .orElseThrow(() -> new RuntimeException("Lab result not found: " + resultId));
        return mapToResponse(result);
    }
    
    /**
     * Get lab result by result number
     */
    public LabResultResponse getLabResultByNumber(String resultNumber) {
        LabResult result = labResultRepository.findByResultNumber(resultNumber)
            .orElseThrow(() -> new RuntimeException("Lab result not found: " + resultNumber));
        return mapToResponse(result);
    }
    
    /**
     * Get all lab results for a patient
     */
    public List<LabResultResponse> getLabResultsByPatient(UUID patientId) {
        List<LabResult> results = labResultRepository.findByPatientPatientIdOrderByResultDateDesc(patientId);
        return results.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get lab results by order
     */
    public List<LabResultResponse> getLabResultsByOrder(UUID orderId) {
        List<LabResult> results = labResultRepository.findByOrderOrderId(orderId);
        return results.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all lab results for an encounter/visit
     */
    public List<LabResultResponse> getLabResultsByEncounter(UUID encounterId) {
        log.info("Getting lab results for encounter: {}", encounterId);
        List<LabResult> results = labResultRepository.findByEncounterId(encounterId);
        return results.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get unacknowledged critical values for a patient
     */
    public List<LabResultResponse> getUnacknowledgedCriticalValues(UUID patientId) {
        List<LabResult> results = labResultRepository.findUnacknowledgedCriticalValues(patientId);
        return results.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all unacknowledged critical values
     */
    public List<LabResultResponse> getAllUnacknowledgedCriticalValues() {
        List<LabResult> results = labResultRepository.findAllUnacknowledgedCriticalValues();
        return results.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get unreviewed results for a patient
     */
    public List<LabResultResponse> getUnreviewedResults(UUID patientId) {
        List<LabResult> results = labResultRepository.findUnreviewedResults(patientId);
        return results.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get abnormal results for a patient
     */
    public List<LabResultResponse> getAbnormalResults(UUID patientId) {
        List<LabResult> results = labResultRepository.findAbnormalResults(patientId);
        return results.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Update a lab result
     */
    @Transactional
    public LabResultResponse updateLabResult(UUID resultId, LabResultRequest request, UUID userId) {
        log.info("Updating lab result: {}", resultId);
        
        LabResult result = labResultRepository.findById(resultId)
            .orElseThrow(() -> new RuntimeException("Lab result not found: " + resultId));
        
        // Only allow updates to PRELIMINARY results or if correcting/amending
        if (result.getResultStatus() == LabResult.ResultStatus.FINAL && 
            !result.getIsCorrected() && !result.getIsAmended()) {
            throw new RuntimeException("Cannot update final result. Use correct or amend instead.");
        }
        
        // Store previous values for history
        String previousValue = result.getResultValue();
        
        // Update fields
        if (request.getResultValue() != null) result.setResultValue(request.getResultValue());
        if (request.getResultValueNumeric() != null) result.setResultValueNumeric(request.getResultValueNumeric());
        if (request.getResultUnits() != null) result.setResultUnits(request.getResultUnits());
        if (request.getQualitativeResult() != null) result.setQualitativeResult(request.getQualitativeResult());
        if (request.getQuantitativeResult() != null) result.setQuantitativeResult(request.getQuantitativeResult());
        if (request.getReferenceRangeLow() != null) result.setReferenceRangeLow(request.getReferenceRangeLow());
        if (request.getReferenceRangeHigh() != null) result.setReferenceRangeHigh(request.getReferenceRangeHigh());
        if (request.getLaboratoryComments() != null) result.setLaboratoryComments(request.getLaboratoryComments());
        if (request.getProviderComments() != null) result.setProviderComments(request.getProviderComments());
        if (request.getResultNotes() != null) result.setResultNotes(request.getResultNotes());
        
        // Recalculate abnormal flags
        LabResult.AbnormalFlag abnormalFlag = calculateAbnormalFlag(request);
        boolean isCritical = detectCriticalValue(request, abnormalFlag);
        result.setAbnormalFlag(abnormalFlag);
        result.setIsCriticalValue(isCritical);
        
        result.setUpdatedBy(userId);
        
        LabResult updatedResult = labResultRepository.save(result);
        
        // Create history record
        createHistoryRecord(updatedResult, LabResultHistory.ChangeType.UPDATED, userId, 
            "result_value", previousValue, updatedResult.getResultValue());
        
        log.info("Updated lab result: {}", updatedResult.getResultId());
        return mapToResponse(updatedResult);
    }
    
    /**
     * Mark result as reviewed
     */
    @Transactional
    public LabResultResponse reviewLabResult(UUID resultId, String reviewNotes, UUID userId) {
        log.info("Reviewing lab result: {}", resultId);
        
        LabResult result = labResultRepository.findById(resultId)
            .orElseThrow(() -> new RuntimeException("Lab result not found: " + resultId));
        
        result.setIsReviewed(true);
        result.setReviewedBy(userId);
        result.setReviewedDate(LocalDateTime.now());
        result.setReviewNotes(reviewNotes);
        result.setUpdatedBy(userId);
        
        LabResult reviewedResult = labResultRepository.save(result);
        
        // Create history record
        createHistoryRecord(reviewedResult, LabResultHistory.ChangeType.REVIEWED, userId, 
            "is_reviewed", "false", "true");
        
        log.info("Reviewed lab result: {}", reviewedResult.getResultId());
        return mapToResponse(reviewedResult);
    }
    
    /**
     * Acknowledge critical value
     */
    @Transactional
    public LabResultResponse acknowledgeCriticalValue(UUID resultId, String response, UUID userId) {
        log.info("Acknowledging critical value for result: {}", resultId);
        
        LabResult result = labResultRepository.findById(resultId)
            .orElseThrow(() -> new RuntimeException("Lab result not found: " + resultId));
        
        if (!result.getIsCriticalValue()) {
            throw new RuntimeException("Result is not a critical value");
        }
        
        result.setIsCriticalValueAcknowledged(true);
        result.setCriticalValueAcknowledgedBy(userId);
        result.setCriticalValueAcknowledgedDate(LocalDateTime.now());
        result.setCriticalValueResponse(response);
        result.setUpdatedBy(userId);
        
        LabResult acknowledgedResult = labResultRepository.save(result);
        
        // Update alert status
        List<LabCriticalValueAlert> alerts = labCriticalValueAlertRepository.findByResultResultId(resultId);
        for (LabCriticalValueAlert alert : alerts) {
            alert.setIsAcknowledged(true);
            alert.setAcknowledgedBy(userId);
            alert.setAcknowledgedDate(LocalDateTime.now());
            alert.setProviderResponse(response);
            alert.setAlertStatus(LabCriticalValueAlert.AlertStatus.ACKNOWLEDGED);
        }
        labCriticalValueAlertRepository.saveAll(alerts);
        
        // Create history record
        createHistoryRecord(acknowledgedResult, LabResultHistory.ChangeType.ACKNOWLEDGED, userId, 
            "is_critical_value_acknowledged", "false", "true");
        
        log.info("Acknowledged critical value for result: {}", acknowledgedResult.getResultId());
        return mapToResponse(acknowledgedResult);
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Calculate abnormal flag based on result value and reference range
     */
    private LabResult.AbnormalFlag calculateAbnormalFlag(LabResultRequest request) {
        if (request.getResultType() != LabResult.ResultType.NUMERIC) {
            return null; // Only numeric results have abnormal flags
        }
        
        if (request.getResultValueNumeric() == null || 
            request.getReferenceRangeLow() == null || 
            request.getReferenceRangeHigh() == null) {
            return LabResult.AbnormalFlag.N; // Normal if no reference range
        }
        
        BigDecimal value = request.getResultValueNumeric();
        BigDecimal low = request.getReferenceRangeLow();
        BigDecimal high = request.getReferenceRangeHigh();
        
        if (value.compareTo(low) < 0) {
            return LabResult.AbnormalFlag.L; // Low
        } else if (value.compareTo(high) > 0) {
            return LabResult.AbnormalFlag.H; // High
        } else {
            return LabResult.AbnormalFlag.N; // Normal
        }
    }
    
    /**
     * Detect critical values based on thresholds
     */
    private boolean detectCriticalValue(LabResultRequest request, LabResult.AbnormalFlag abnormalFlag) {
        if (request.getIsPanicValue() != null && request.getIsPanicValue()) {
            return true;
        }
        
        if (abnormalFlag == LabResult.AbnormalFlag.C) {
            return true;
        }
        
        // Additional critical value detection logic can be added here
        // For now, we'll use the flag from request or check if value is extremely high/low
        if (request.getIsCriticalValue() != null && request.getIsCriticalValue()) {
            return true;
        }
        
        // Check if value is outside critical thresholds (can be customized per test type)
        if (request.getResultType() == LabResult.ResultType.NUMERIC && 
            request.getResultValueNumeric() != null && 
            request.getReferenceRangeLow() != null && 
            request.getReferenceRangeHigh() != null) {
            
            BigDecimal value = request.getResultValueNumeric();
            BigDecimal low = request.getReferenceRangeLow();
            BigDecimal high = request.getReferenceRangeHigh();
            BigDecimal range = high.subtract(low);
            
            // Critical if value is more than 50% outside reference range
            if (value.compareTo(low.subtract(range.multiply(BigDecimal.valueOf(0.5)))) < 0 ||
                value.compareTo(high.add(range.multiply(BigDecimal.valueOf(0.5)))) > 0) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Perform delta check - compare with previous result
     */
    private boolean performDeltaCheck(LabResultRequest request, UUID patientId) {
        if (request.getResultType() != LabResult.ResultType.NUMERIC || 
            request.getResultValueNumeric() == null) {
            return false;
        }
        
        Optional<LabResult> previousResult = labResultRepository.findPreviousResult(
            patientId, 
            request.getLoincCode(), 
            request.getResultDate()
        );
        
        if (previousResult.isEmpty()) {
            return false;
        }
        
        LabResult previous = previousResult.get();
        if (previous.getResultValueNumeric() == null) {
            return false;
        }
        
        BigDecimal currentValue = request.getResultValueNumeric();
        BigDecimal previousValue = previous.getResultValueNumeric();
        BigDecimal difference = currentValue.subtract(previousValue).abs();
        BigDecimal percentChange = difference.divide(previousValue.abs(), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        
        // Delta check flag if change is more than 50%
        return percentChange.compareTo(BigDecimal.valueOf(50)) > 0;
    }
    
    /**
     * Create critical value alert
     */
    private void createCriticalValueAlert(LabResult result, LabOrder order, Patient patient, UUID userId) {
        LabCriticalValueAlert alert = LabCriticalValueAlert.builder()
            .result(result)
            .patient(patient)
            .orderId(order.getOrderId())
            .alertStatus(LabCriticalValueAlert.AlertStatus.PENDING)
            .alertPriority(LabCriticalValueAlert.AlertPriority.CRITICAL)
            .alertMessage(String.format("Critical value alert: %s = %s %s (Reference: %s-%s %s)", 
                result.getTestName(),
                result.getResultValue(),
                result.getResultUnits(),
                result.getReferenceRangeLow(),
                result.getReferenceRangeHigh(),
                result.getReferenceRangeUnits()))
            .notifiedProviderId(order.getOrderingProviderId())
            .notifiedProviderName(order.getOrderingProviderName())
            .isAcknowledged(false)
            .escalationLevel(0)
            .build();
        
        labCriticalValueAlertRepository.save(alert);
        log.info("Created critical value alert for result: {}", result.getResultId());
    }
    
    /**
     * Create history record
     */
    private void createHistoryRecord(LabResult result, LabResultHistory.ChangeType changeType, 
                                    UUID userId, String fieldName, String previousValue, String newValue) {
        LabResultHistory history = LabResultHistory.builder()
            .result(result)
            .changeType(changeType)
            .changedBy(userId)
            .fieldName(fieldName)
            .previousValue(previousValue)
            .newValue(newValue)
            .build();
        
        labResultHistoryRepository.save(history);
    }
    
    /**
     * Generate a unique result number
     */
    private String generateResultNumber() {
        String prefix = "RES";
        long timestamp = System.currentTimeMillis();
        String suffix = String.valueOf(timestamp).substring(7); // Last 6 digits
        return prefix + suffix;
    }
    
    /**
     * Map LabResult entity to LabResultResponse DTO
     */
    private LabResultResponse mapToResponse(LabResult result) {
        Patient patient = result.getPatient();
        LabOrder order = result.getOrder();
        
        return LabResultResponse.builder()
            .resultId(result.getResultId())
            .orderId(order.getOrderId())
            .orderNumber(order.getOrderNumber())
            .patientId(patient.getPatientId())
            .patientName(PatientDisplayName.of(patient))
            .mrn(patient.getMrn())
            .encounterId(result.getEncounterId())
            .organizationId(result.getOrganizationId())
            .resultNumber(result.getResultNumber())
            .testName(result.getTestName())
            .loincCode(result.getLoincCode())
            .testCategory(result.getTestCategory())
            .testType(result.getTestType())
            .resultValue(result.getResultValue())
            .resultValueNumeric(result.getResultValueNumeric())
            .resultUnits(result.getResultUnits())
            .resultType(result.getResultType())
            .qualitativeResult(result.getQualitativeResult())
            .quantitativeResult(result.getQuantitativeResult())
            .resultStatus(result.getResultStatus())
            .referenceRangeLow(result.getReferenceRangeLow())
            .referenceRangeHigh(result.getReferenceRangeHigh())
            .referenceRangeUnits(result.getReferenceRangeUnits())
            .referenceRangeText(result.getReferenceRangeText())
            .referenceRangeSource(result.getReferenceRangeSource())
            .ageSpecificRange(result.getAgeSpecificRange())
            .genderSpecificRange(result.getGenderSpecificRange())
            .abnormalFlag(result.getAbnormalFlag())
            .isCriticalValue(result.getIsCriticalValue())
            .isDeltaCheck(result.getIsDeltaCheck())
            .isPanicValue(result.getIsPanicValue())
            .resultInterpretation(result.getResultInterpretation())
            .clinicalSignificance(result.getClinicalSignificance())
            .clinicalSignificanceLevel(result.getClinicalSignificanceLevel())
            .interpretationNotes(result.getInterpretationNotes())
            .orderDate(result.getOrderDate())
            .specimenCollectionDate(result.getSpecimenCollectionDate())
            .specimenReceivedDate(result.getSpecimenReceivedDate())
            .resultDate(result.getResultDate())
            .resultReportedDate(result.getResultReportedDate())
            .resultVerifiedDate(result.getResultVerifiedDate())
            .specimenType(result.getSpecimenType())
            .specimenSource(result.getSpecimenSource())
            .specimenCollectionMethod(result.getSpecimenCollectionMethod())
            .specimenId(result.getSpecimenId())
            .specimenVolume(result.getSpecimenVolume())
            .specimenQuality(result.getSpecimenQuality())
            .performingLaboratoryName(result.getPerformingLaboratoryName())
            .laboratoryId(result.getLaboratoryId())
            .laboratoryNpi(result.getLaboratoryNpi())
            .laboratoryAddressLine1(result.getLaboratoryAddressLine1())
            .laboratoryAddressLine2(result.getLaboratoryAddressLine2())
            .laboratoryCity(result.getLaboratoryCity())
            .laboratoryState(result.getLaboratoryState())
            .laboratoryZip(result.getLaboratoryZip())
            .laboratoryPhone(result.getLaboratoryPhone())
            .performingTechnologist(result.getPerformingTechnologist())
            .reviewingPathologist(result.getReviewingPathologist())
            .reviewingPhysician(result.getReviewingPhysician())
            .laboratoryReferenceNumber(result.getLaboratoryReferenceNumber())
            .laboratoryComments(result.getLaboratoryComments())
            .providerComments(result.getProviderComments())
            .resultNotes(result.getResultNotes())
            .methodUsed(result.getMethodUsed())
            .isCriticalValueAcknowledged(result.getIsCriticalValueAcknowledged())
            .criticalValueAcknowledgedBy(result.getCriticalValueAcknowledgedBy())
            .criticalValueAcknowledgedDate(result.getCriticalValueAcknowledgedDate())
            .criticalValueResponse(result.getCriticalValueResponse())
            .isReviewed(result.getIsReviewed())
            .reviewedBy(result.getReviewedBy())
            .reviewedDate(result.getReviewedDate())
            .reviewNotes(result.getReviewNotes())
            .isCorrected(result.getIsCorrected())
            .isAmended(result.getIsAmended())
            .isCancelled(result.getIsCancelled())
            .originalResultId(result.getOriginalResult() != null ? result.getOriginalResult().getResultId() : null)
            .correctionReason(result.getCorrectionReason())
            .amendmentReason(result.getAmendmentReason())
            .cancellationReason(result.getCancellationReason())
            .correctionDate(result.getCorrectionDate())
            .amendmentDate(result.getAmendmentDate())
            .cancellationDate(result.getCancellationDate())
            .createdAt(result.getCreatedAt())
            .updatedAt(result.getUpdatedAt())
            .createdBy(result.getCreatedBy())
            .updatedBy(result.getUpdatedBy())
            .build();
    }
    
    // ========== Result Display and Viewing Methods ==========
    
    /**
     * Get lab results for a patient in chronological order with highlighting
     */
    public List<LabResultListViewResponse> getLabResultsChronological(UUID patientId) {
        List<LabResult> results = labResultRepository.findByPatientPatientIdOrderByResultDateDesc(patientId);
        return results.stream()
            .map(this::mapToListViewResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get lab results grouped by test category
     */
    public Map<String, List<LabResultListViewResponse>> getLabResultsByCategory(UUID patientId) {
        List<LabResult> results = labResultRepository.findByPatientPatientIdOrderByResultDateDesc(patientId);
        
        Map<String, List<LabResultListViewResponse>> grouped = new HashMap<>();
        
        for (LabResult result : results) {
            String category = result.getTestCategory() != null ? result.getTestCategory() : "UNCATEGORIZED";
            grouped.computeIfAbsent(category, k -> new ArrayList<>())
                .add(mapToListViewResponse(result));
        }
        
        return grouped;
    }
    
    /**
     * Get lab results by category
     */
    public List<LabResultListViewResponse> getLabResultsByCategory(UUID patientId, String category) {
        List<LabResult> results = labResultRepository.findResultsByCategory(patientId, category);
        return results.stream()
            .map(this::mapToListViewResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get detailed lab result view
     */
    public LabResultResponse getLabResultDetail(UUID resultId) {
        LabResult result = labResultRepository.findById(resultId)
            .orElseThrow(() -> new RuntimeException("Lab result not found: " + resultId));
        
        return mapToResponse(result);
    }
    
    /**
     * Compare current result with previous result
     */
    public LabResultComparisonResponse compareResults(UUID currentResultId) {
        LabResult currentResult = labResultRepository.findById(currentResultId)
            .orElseThrow(() -> new RuntimeException("Lab result not found: " + currentResultId));
        
        if (currentResult.getResultType() != LabResult.ResultType.NUMERIC) {
            throw new RuntimeException("Comparison is only available for numeric results");
        }
        
        Optional<LabResult> previousResultOpt = labResultRepository.findPreviousResult(
            currentResult.getPatient().getPatientId(),
            currentResult.getLoincCode(),
            currentResult.getResultDate()
        );
        
        if (previousResultOpt.isEmpty()) {
            throw new RuntimeException("No previous result found for comparison");
        }
        
        LabResult previousResult = previousResultOpt.get();
        
        return buildComparisonResponse(currentResult, previousResult);
    }
    
    /**
     * Get trend data for a specific test (LOINC code)
     */
    public LabResultTrendResponse getTrendData(UUID patientId, String loincCode, LocalDateTime startDate, LocalDateTime endDate) {
        List<LabResult> results = labResultRepository.findResultsByLoincCode(patientId, loincCode);
        
        // Filter by date range if provided
        if (startDate != null || endDate != null) {
            results = results.stream()
                .filter(r -> {
                    if (startDate != null && r.getResultDate().isBefore(startDate)) return false;
                    if (endDate != null && r.getResultDate().isAfter(endDate)) return false;
                    return true;
                })
                .collect(Collectors.toList());
        }
        
        if (results.isEmpty()) {
            throw new RuntimeException("No results found for trend analysis");
        }
        
        // Sort by date
        results.sort(Comparator.comparing(LabResult::getResultDate));
        
        return buildTrendResponse(results);
    }
    
    /**
     * Get correlated results (related tests displayed together)
     */
    public LabResultCorrelationResponse getCorrelatedResults(UUID resultId) {
        LabResult primaryResult = labResultRepository.findById(resultId)
            .orElseThrow(() -> new RuntimeException("Lab result not found: " + resultId));
        
        List<LabResult> relatedResults = findRelatedResults(primaryResult);
        
        return buildCorrelationResponse(primaryResult, relatedResults);
    }
    
    /**
     * Get correlated results by collection date
     */
    public LabResultCorrelationResponse getCorrelatedResultsByDate(UUID patientId, LocalDateTime collectionDate) {
        List<LabResult> results = labResultRepository.findByPatientPatientId(patientId);
        
        // Find results collected on the same date (within 24 hours)
        LocalDateTime startOfDay = collectionDate.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        List<LabResult> sameDayResults = results.stream()
            .filter(r -> r.getSpecimenCollectionDate() != null &&
                        !r.getSpecimenCollectionDate().isBefore(startOfDay) &&
                        r.getSpecimenCollectionDate().isBefore(endOfDay))
            .sorted(Comparator.comparing(LabResult::getSpecimenCollectionDate))
            .collect(Collectors.toList());
        
        if (sameDayResults.isEmpty()) {
            throw new RuntimeException("No results found for the specified collection date");
        }
        
        LabResult primaryResult = sameDayResults.get(0);
        List<LabResult> relatedResults = sameDayResults.subList(1, sameDayResults.size());
        
        return buildCorrelationResponse(primaryResult, relatedResults);
    }
    
    // ========== Helper Methods for Display and Viewing ==========
    
    /**
     * Map LabResult to LabResultListViewResponse with highlighting
     */
    private LabResultListViewResponse mapToListViewResponse(LabResult result) {
        Patient patient = result.getPatient();
        LabOrder order = result.getOrder();
        
        // Determine highlighting
        String highlightColor = determineHighlightColor(result);
        String highlightReason = determineHighlightReason(result);
        boolean requiresAttention = determineRequiresAttention(result);
        
        return LabResultListViewResponse.builder()
            .resultId(result.getResultId())
            .orderId(order.getOrderId())
            .orderNumber(order.getOrderNumber())
            .patientId(patient.getPatientId())
            .patientName(PatientDisplayName.of(patient))
            .mrn(patient.getMrn())
            .resultNumber(result.getResultNumber())
            .testName(result.getTestName())
            .loincCode(result.getLoincCode())
            .testCategory(result.getTestCategory())
            .testType(result.getTestType())
            .resultValue(result.getResultValue())
            .resultValueNumeric(result.getResultValueNumeric())
            .resultUnits(result.getResultUnits())
            .resultType(result.getResultType())
            .resultStatus(result.getResultStatus())
            .referenceRangeLow(result.getReferenceRangeLow())
            .referenceRangeHigh(result.getReferenceRangeHigh())
            .referenceRangeUnits(result.getReferenceRangeUnits())
            .abnormalFlag(result.getAbnormalFlag())
            .isCriticalValue(result.getIsCriticalValue())
            .isDeltaCheck(result.getIsDeltaCheck())
            .isPanicValue(result.getIsPanicValue())
            .highlightColor(highlightColor)
            .highlightReason(highlightReason)
            .requiresAttention(requiresAttention)
            .resultDate(result.getResultDate())
            .resultReportedDate(result.getResultReportedDate())
            .isReviewed(result.getIsReviewed())
            .isCriticalValueAcknowledged(result.getIsCriticalValueAcknowledged())
            .performingLaboratoryName(result.getPerformingLaboratoryName())
            .build();
    }
    
    /**
     * Determine highlight color based on result status
     */
    private String determineHighlightColor(LabResult result) {
        if (result.getIsCriticalValue() && !result.getIsCriticalValueAcknowledged()) {
            return "RED";
        } else if (result.getIsPanicValue()) {
            return "RED";
        } else if (result.getIsCriticalValue()) {
            return "ORANGE";
        } else if (result.getAbnormalFlag() != null && result.getAbnormalFlag() != LabResult.AbnormalFlag.N) {
            return "YELLOW";
        } else if (result.getIsDeltaCheck()) {
            return "YELLOW";
        } else if (!result.getIsReviewed()) {
            return "YELLOW";
        } else {
            return "GREEN";
        }
    }
    
    /**
     * Determine highlight reason
     */
    private String determineHighlightReason(LabResult result) {
        if (result.getIsCriticalValue() && !result.getIsCriticalValueAcknowledged()) {
            return "CRITICAL_VALUE_UNACKNOWLEDGED";
        } else if (result.getIsPanicValue()) {
            return "PANIC_VALUE";
        } else if (result.getIsCriticalValue()) {
            return "CRITICAL_VALUE";
        } else if (result.getAbnormalFlag() != null && result.getAbnormalFlag() != LabResult.AbnormalFlag.N) {
            return "ABNORMAL_VALUE";
        } else if (result.getIsDeltaCheck()) {
            return "DELTA_CHECK";
        } else if (!result.getIsReviewed()) {
            return "UNREVIEWED";
        } else {
            return "NORMAL";
        }
    }
    
    /**
     * Determine if result requires attention
     */
    private boolean determineRequiresAttention(LabResult result) {
        return result.getIsCriticalValue() || 
               result.getIsPanicValue() || 
               (result.getAbnormalFlag() != null && result.getAbnormalFlag() != LabResult.AbnormalFlag.N) ||
               result.getIsDeltaCheck() ||
               !result.getIsReviewed();
    }
    
    /**
     * Find related results (same order, same collection date, or same test category)
     */
    private List<LabResult> findRelatedResults(LabResult result) {
        List<LabResult> related = new ArrayList<>();
        
        // Results from same order
        List<LabResult> sameOrder = labResultRepository.findByOrderOrderId(result.getOrder().getOrderId());
        related.addAll(sameOrder.stream()
            .filter(r -> !r.getResultId().equals(result.getResultId()))
            .collect(Collectors.toList()));
        
        // Results from same collection date (within 1 hour)
        if (result.getSpecimenCollectionDate() != null) {
            LocalDateTime collectionDate = result.getSpecimenCollectionDate();
            LocalDateTime startTime = collectionDate.minus(1, ChronoUnit.HOURS);
            LocalDateTime endTime = collectionDate.plus(1, ChronoUnit.HOURS);
            
            List<LabResult> sameDate = labResultRepository.findByPatientPatientId(result.getPatient().getPatientId());
            List<LabResult> sameCollectionTime = sameDate.stream()
                .filter(r -> r.getSpecimenCollectionDate() != null &&
                            !r.getSpecimenCollectionDate().isBefore(startTime) &&
                            r.getSpecimenCollectionDate().isBefore(endTime) &&
                            !r.getResultId().equals(result.getResultId()))
                .collect(Collectors.toList());
            related.addAll(sameCollectionTime);
        }
        
        // Results from same test category
        if (result.getTestCategory() != null) {
            List<LabResult> sameCategory = labResultRepository.findResultsByCategory(
                result.getPatient().getPatientId(), result.getTestCategory());
            related.addAll(sameCategory.stream()
                .filter(r -> !r.getResultId().equals(result.getResultId()) &&
                            r.getResultDate().isAfter(result.getResultDate().minusDays(7)))
                .collect(Collectors.toList()));
        }
        
        // Remove duplicates and sort by date
        return related.stream()
            .distinct()
            .sorted(Comparator.comparing(LabResult::getResultDate))
            .collect(Collectors.toList());
    }
    
    /**
     * Build comparison response
     */
    private LabResultComparisonResponse buildComparisonResponse(LabResult current, LabResult previous) {
        BigDecimal currentValue = current.getResultValueNumeric();
        BigDecimal previousValue = previous.getResultValueNumeric();
        
        if (currentValue == null || previousValue == null) {
            throw new RuntimeException("Cannot compare: one or both results have null numeric values");
        }
        
        BigDecimal absoluteDifference = currentValue.subtract(previousValue);
        BigDecimal percentChange = previousValue.compareTo(BigDecimal.ZERO) != 0
            ? absoluteDifference.divide(previousValue.abs(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
        
        String changeDirection;
        if (absoluteDifference.compareTo(BigDecimal.ZERO) > 0) {
            changeDirection = "INCREASED";
        } else if (absoluteDifference.compareTo(BigDecimal.ZERO) < 0) {
            changeDirection = "DECREASED";
        } else {
            changeDirection = "UNCHANGED";
        }
        
        // Significant change if > 20% change
        boolean isSignificantChange = percentChange.abs().compareTo(BigDecimal.valueOf(20)) > 0;
        
        long daysBetween = ChronoUnit.DAYS.between(previous.getResultDate(), current.getResultDate());
        
        String comparisonNotes = String.format(
            "Value changed from %s %s to %s %s (%s by %.2f%%) over %d days",
            previousValue, previous.getResultUnits(),
            currentValue, current.getResultUnits(),
            changeDirection.toLowerCase(), percentChange.abs(), daysBetween
        );
        
        return LabResultComparisonResponse.builder()
            .currentResultId(current.getResultId())
            .previousResultId(previous.getResultId())
            .testName(current.getTestName())
            .loincCode(current.getLoincCode())
            .testCategory(current.getTestCategory())
            .currentResult(mapToResponse(current))
            .previousResult(mapToResponse(previous))
            .absoluteDifference(absoluteDifference)
            .percentChange(percentChange)
            .changeDirection(changeDirection)
            .isSignificantChange(isSignificantChange)
            .comparisonNotes(comparisonNotes)
            .daysBetweenResults(daysBetween)
            .comparisonDate(LocalDateTime.now())
            .build();
    }
    
    /**
     * Build trend response
     */
    private LabResultTrendResponse buildTrendResponse(List<LabResult> results) {
        if (results.isEmpty()) {
            throw new RuntimeException("No results provided for trend analysis");
        }
        
        LabResult firstResult = results.get(0);
        LabResult lastResult = results.get(results.size() - 1);
        
        // Extract numeric values
        List<BigDecimal> values = results.stream()
            .map(LabResult::getResultValueNumeric)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        if (values.isEmpty()) {
            throw new RuntimeException("No numeric values found for trend analysis");
        }
        
        // Calculate statistics
        BigDecimal minValue = values.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal maxValue = values.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal averageValue = values.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
        
        // Calculate median
        List<BigDecimal> sortedValues = new ArrayList<>(values);
        Collections.sort(sortedValues);
        BigDecimal medianValue;
        int size = sortedValues.size();
        if (size % 2 == 0) {
            medianValue = sortedValues.get(size / 2 - 1)
                .add(sortedValues.get(size / 2))
                .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        } else {
            medianValue = sortedValues.get(size / 2);
        }
        
        // Calculate trend direction and slope
        String trendDirection = calculateTrendDirection(results);
        BigDecimal trendSlope = calculateTrendSlope(results);
        
        // Build data points
        List<LabResultTrendResponse.TrendDataPoint> dataPoints = results.stream()
            .map(r -> LabResultTrendResponse.TrendDataPoint.builder()
                .resultId(r.getResultId())
                .resultDate(r.getResultDate())
                .value(r.getResultValueNumeric())
                .resultValue(r.getResultValue())
                .abnormalFlag(r.getAbnormalFlag())
                .isCriticalValue(r.getIsCriticalValue())
                .resultStatus(r.getResultStatus().toString())
                .build())
            .collect(Collectors.toList());
        
        return LabResultTrendResponse.builder()
            .patientId(firstResult.getPatient().getPatientId())
            .testName(firstResult.getTestName())
            .loincCode(firstResult.getLoincCode())
            .testCategory(firstResult.getTestCategory())
            .resultUnits(firstResult.getResultUnits())
            .dataPoints(dataPoints)
            .minValue(minValue)
            .maxValue(maxValue)
            .averageValue(averageValue)
            .medianValue(medianValue)
            .totalDataPoints(results.size())
            .trendDirection(trendDirection)
            .trendSlope(trendSlope)
            .referenceRangeLow(firstResult.getReferenceRangeLow())
            .referenceRangeHigh(firstResult.getReferenceRangeHigh())
            .startDate(firstResult.getResultDate())
            .endDate(lastResult.getResultDate())
            .build();
    }
    
    /**
     * Calculate trend direction
     */
    private String calculateTrendDirection(List<LabResult> results) {
        if (results.size() < 2) {
            return "STABLE";
        }
        
        List<BigDecimal> values = results.stream()
            .map(LabResult::getResultValueNumeric)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        if (values.size() < 2) {
            return "STABLE";
        }
        
        BigDecimal firstValue = values.get(0);
        BigDecimal lastValue = values.get(values.size() - 1);
        
        BigDecimal change = lastValue.subtract(firstValue);
        BigDecimal percentChange = firstValue.compareTo(BigDecimal.ZERO) != 0
            ? change.divide(firstValue.abs(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
        
        // Check for fluctuation
        boolean isFluctuating = false;
        for (int i = 1; i < values.size() - 1; i++) {
            BigDecimal prev = values.get(i - 1);
            BigDecimal curr = values.get(i);
            BigDecimal next = values.get(i + 1);
            
            if ((curr.compareTo(prev) > 0 && curr.compareTo(next) < 0) ||
                (curr.compareTo(prev) < 0 && curr.compareTo(next) > 0)) {
                isFluctuating = true;
                break;
            }
        }
        
        if (isFluctuating || percentChange.abs().compareTo(BigDecimal.valueOf(5)) < 0) {
            return "FLUCTUATING";
        } else if (change.compareTo(BigDecimal.ZERO) > 0) {
            return "INCREASING";
        } else if (change.compareTo(BigDecimal.ZERO) < 0) {
            return "DECREASING";
        } else {
            return "STABLE";
        }
    }
    
    /**
     * Calculate trend slope (rate of change per day)
     */
    private BigDecimal calculateTrendSlope(List<LabResult> results) {
        if (results.size() < 2) {
            return BigDecimal.ZERO;
        }
        
        List<BigDecimal> values = results.stream()
            .map(LabResult::getResultValueNumeric)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        if (values.size() < 2) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal firstValue = values.get(0);
        BigDecimal lastValue = values.get(values.size() - 1);
        BigDecimal valueChange = lastValue.subtract(firstValue);
        
        long daysBetween = ChronoUnit.DAYS.between(
            results.get(0).getResultDate(),
            results.get(results.size() - 1).getResultDate()
        );
        
        if (daysBetween == 0) {
            return BigDecimal.ZERO;
        }
        
        return valueChange.divide(BigDecimal.valueOf(daysBetween), 6, RoundingMode.HALF_UP);
    }
    
    /**
     * Build correlation response
     */
    private LabResultCorrelationResponse buildCorrelationResponse(LabResult primary, List<LabResult> related) {
        String correlationGroup = determineCorrelationGroup(primary, related);
        String correlationReason = determineCorrelationReason(primary, related);
        
        int abnormalCount = (int) related.stream()
            .filter(r -> r.getAbnormalFlag() != null && r.getAbnormalFlag() != LabResult.AbnormalFlag.N)
            .count();
        
        int criticalCount = (int) related.stream()
            .filter(LabResult::getIsCriticalValue)
            .count();
        
        return LabResultCorrelationResponse.builder()
            .patientId(primary.getPatient().getPatientId())
            .collectionDate(primary.getSpecimenCollectionDate())
            .encounterId(primary.getEncounterId())
            .primaryResult(mapToResponse(primary))
            .relatedResults(related.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()))
            .correlationGroup(correlationGroup)
            .correlationReason(correlationReason)
            .totalRelatedResults(related.size())
            .abnormalResultsCount(abnormalCount)
            .criticalResultsCount(criticalCount)
            .build();
    }
    
    /**
     * Determine correlation group
     */
    private String determineCorrelationGroup(LabResult primary, List<LabResult> related) {
        // Check if all results are from same order (test panel)
        UUID orderId = primary.getOrder().getOrderId();
        boolean allSameOrder = related.stream()
            .allMatch(r -> r.getOrder().getOrderId().equals(orderId));
        
        if (allSameOrder && !related.isEmpty()) {
            return "TEST_PANEL";
        }
        
        // Check if same category
        String category = primary.getTestCategory();
        if (category != null && related.stream().allMatch(r -> category.equals(r.getTestCategory()))) {
            return category;
        }
        
        // Check common test panels
        Set<String> loincCodes = new HashSet<>();
        loincCodes.add(primary.getLoincCode());
        related.forEach(r -> loincCodes.add(r.getLoincCode()));
        
        // Common panels
        if (isCBCPanel(loincCodes)) return "CBC";
        if (isLipidPanel(loincCodes)) return "LIPID_PANEL";
        if (isLiverFunctionPanel(loincCodes)) return "LIVER_FUNCTION";
        if (isMetabolicPanel(loincCodes)) return "METABOLIC_PANEL";
        
        return "RELATED_TESTS";
    }
    
    /**
     * Determine correlation reason
     */
    private String determineCorrelationReason(LabResult primary, List<LabResult> related) {
        UUID orderId = primary.getOrder().getOrderId();
        boolean allSameOrder = related.stream()
            .allMatch(r -> r.getOrder().getOrderId().equals(orderId));
        
        if (allSameOrder) {
            return "Same test order/panel";
        }
        
        if (primary.getSpecimenCollectionDate() != null) {
            boolean allSameCollectionTime = related.stream()
                .allMatch(r -> r.getSpecimenCollectionDate() != null &&
                              Math.abs(ChronoUnit.HOURS.between(
                                  primary.getSpecimenCollectionDate(),
                                  r.getSpecimenCollectionDate())) < 1);
            
            if (allSameCollectionTime) {
                return "Same collection time";
            }
        }
        
        String category = primary.getTestCategory();
        if (category != null && related.stream().allMatch(r -> category.equals(r.getTestCategory()))) {
            return "Same test category";
        }
        
        return "Related tests";
    }
    
    /**
     * Check if results form a CBC panel
     */
    private boolean isCBCPanel(Set<String> loincCodes) {
        // Common CBC LOINC codes (simplified check)
        return loincCodes.stream().anyMatch(code -> 
            code.contains("CBC") || code.contains("789-8") || code.contains("6690-2"));
    }
    
    /**
     * Check if results form a Lipid panel
     */
    private boolean isLipidPanel(Set<String> loincCodes) {
        return loincCodes.stream().anyMatch(code -> 
            code.contains("LIPID") || code.contains("2089-1") || code.contains("2571-8"));
    }
    
    /**
     * Check if results form a Liver Function panel
     */
    private boolean isLiverFunctionPanel(Set<String> loincCodes) {
        return loincCodes.stream().anyMatch(code -> 
            code.contains("LIVER") || code.contains("1975-2") || code.contains("1968-7"));
    }
    
    /**
     * Check if results form a Metabolic panel
     */
    private boolean isMetabolicPanel(Set<String> loincCodes) {
        return loincCodes.stream().anyMatch(code -> 
            code.contains("METABOLIC") || code.contains("24323-8") || code.contains("24320-4"));
    }
    
    // ========== Result Interpretation and Clinical Context Methods ==========
    
    /**
     * Calculate clinical significance of a result
     */
    private LabResult.ClinicalSignificance calculateClinicalSignificance(
            LabResultRequest request, 
            LabResult.AbnormalFlag abnormalFlag, 
            boolean isCritical, 
            boolean isDeltaCheck) {
        
        if (isCritical) {
            return LabResult.ClinicalSignificance.CRITICAL;
        }
        
        if (abnormalFlag != null && abnormalFlag != LabResult.AbnormalFlag.N) {
            return LabResult.ClinicalSignificance.ABNORMAL;
        }
        
        if (isDeltaCheck) {
            return LabResult.ClinicalSignificance.SIGNIFICANT_CHANGE;
        }
        
        // Check for trending (would need previous results)
        if (request.getResultType() == LabResult.ResultType.NUMERIC && 
            request.getResultValueNumeric() != null) {
            // This is a simplified check - in production, you'd compare with multiple previous results
            return LabResult.ClinicalSignificance.STABLE;
        }
        
        return LabResult.ClinicalSignificance.NORMAL;
    }
    
    /**
     * Calculate clinical significance level
     */
    private LabResult.ClinicalSignificanceLevel calculateClinicalSignificanceLevel(
            LabResult.ClinicalSignificance significance,
            boolean isCritical,
            LabResult.AbnormalFlag abnormalFlag) {
        
        if (isCritical) {
            return LabResult.ClinicalSignificanceLevel.CRITICAL;
        }
        
        if (significance == LabResult.ClinicalSignificance.ABNORMAL) {
            // Determine level based on how far outside reference range
            return LabResult.ClinicalSignificanceLevel.HIGH;
        }
        
        if (significance == LabResult.ClinicalSignificance.SIGNIFICANT_CHANGE) {
            return LabResult.ClinicalSignificanceLevel.MEDIUM;
        }
        
        return LabResult.ClinicalSignificanceLevel.LOW;
    }
    
    /**
     * Link a lab result to a problem/diagnosis
     */
    @Transactional
    public LabResultProblem linkResultToProblem(UUID resultId, UUID problemId, 
            LabResultProblem.LinkType linkType, LabResultProblem.LinkStrength linkStrength,
            String clinicalRelevance, String notes, UUID userId) {
        log.info("Linking result {} to problem {}", resultId, problemId);
        
        LabResult result = labResultRepository.findById(resultId)
            .orElseThrow(() -> new RuntimeException("Lab result not found: " + resultId));
        
        PatientProblem problem = patientProblemRepository.findById(problemId)
            .orElseThrow(() -> new RuntimeException("Problem not found: " + problemId));
        
        // Check if link already exists
        LabResultProblem existingLink = labResultProblemRepository
            .findByResultIdAndProblemId(resultId, problemId);
        
        if (existingLink != null) {
            // Update existing link
            existingLink.setLinkType(linkType);
            existingLink.setLinkStrength(linkStrength);
            existingLink.setClinicalRelevance(clinicalRelevance);
            existingLink.setNotes(notes);
            existingLink.setLinkedBy(userId);
            existingLink.setLinkedDate(LocalDateTime.now());
            return labResultProblemRepository.save(existingLink);
        }
        
        // Create new link
        LabResultProblem link = LabResultProblem.builder()
            .result(result)
            .problem(problem)
            .organizationId(result.getOrganizationId())
            .linkType(linkType != null ? linkType : LabResultProblem.LinkType.RELATED)
            .linkStrength(linkStrength != null ? linkStrength : LabResultProblem.LinkStrength.MODERATE)
            .clinicalRelevance(clinicalRelevance)
            .linkedBy(userId)
            .linkedDate(LocalDateTime.now())
            .notes(notes)
            .build();
        
        LabResultProblem savedLink = labResultProblemRepository.save(link);
        log.info("Linked result {} to problem {}", resultId, problemId);
        return savedLink;
    }
    
    /**
     * Link a lab result to a medication/prescription
     */
    @Transactional
    public LabResultMedication linkResultToMedication(UUID resultId, UUID prescriptionId,
            LabResultMedication.LinkType linkType, LabResultMedication.LinkStrength linkStrength,
            String clinicalRelevance, String notes, UUID userId) {
        log.info("Linking result {} to prescription {}", resultId, prescriptionId);
        
        LabResult result = labResultRepository.findById(resultId)
            .orElseThrow(() -> new RuntimeException("Lab result not found: " + resultId));
        
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
            .orElseThrow(() -> new RuntimeException("Prescription not found: " + prescriptionId));
        
        // Check if link already exists
        LabResultMedication existingLink = labResultMedicationRepository
            .findByResultIdAndPrescriptionId(resultId, prescriptionId);
        
        if (existingLink != null) {
            // Update existing link
            existingLink.setLinkType(linkType);
            existingLink.setLinkStrength(linkStrength);
            existingLink.setClinicalRelevance(clinicalRelevance);
            existingLink.setNotes(notes);
            existingLink.setLinkedBy(userId);
            existingLink.setLinkedDate(LocalDateTime.now());
            return labResultMedicationRepository.save(existingLink);
        }
        
        // Create new link
        LabResultMedication link = LabResultMedication.builder()
            .result(result)
            .prescription(prescription)
            .organizationId(result.getOrganizationId())
            .linkType(linkType != null ? linkType : LabResultMedication.LinkType.MONITORS)
            .linkStrength(linkStrength != null ? linkStrength : LabResultMedication.LinkStrength.MODERATE)
            .clinicalRelevance(clinicalRelevance)
            .linkedBy(userId)
            .linkedDate(LocalDateTime.now())
            .notes(notes)
            .build();
        
        LabResultMedication savedLink = labResultMedicationRepository.save(link);
        log.info("Linked result {} to prescription {}", resultId, prescriptionId);
        return savedLink;
    }
    
    /**
     * Get problems linked to a lab result
     */
    public List<LabResultProblem> getLinkedProblems(UUID resultId) {
        return labResultProblemRepository.findByResultResultId(resultId);
    }
    
    /**
     * Get medications linked to a lab result
     */
    public List<LabResultMedication> getLinkedMedications(UUID resultId) {
        return labResultMedicationRepository.findByResultResultId(resultId);
    }
    
    /**
     * Unlink a result from a problem
     */
    @Transactional
    public void unlinkResultFromProblem(UUID resultId, UUID problemId) {
        log.info("Unlinking result {} from problem {}", resultId, problemId);
        LabResultProblem link = labResultProblemRepository
            .findByResultIdAndProblemId(resultId, problemId);
        if (link != null) {
            labResultProblemRepository.delete(link);
            log.info("Unlinked result {} from problem {}", resultId, problemId);
        }
    }
    
    /**
     * Unlink a result from a medication
     */
    @Transactional
    public void unlinkResultFromMedication(UUID resultId, UUID prescriptionId) {
        log.info("Unlinking result {} from prescription {}", resultId, prescriptionId);
        LabResultMedication link = labResultMedicationRepository
            .findByResultIdAndPrescriptionId(resultId, prescriptionId);
        if (link != null) {
            labResultMedicationRepository.delete(link);
            log.info("Unlinked result {} from prescription {}", resultId, prescriptionId);
        }
    }
    
    /**
     * Link a lab result to a clinical note
     */
    @Transactional
    public LabResultClinicalNote linkResultToClinicalNote(UUID resultId, UUID noteId,
            LabResultClinicalNote.LinkType linkType, LabResultClinicalNote.LinkStrength linkStrength,
            String clinicalRelevance, String notes, UUID userId) {
        log.info("Linking result {} to clinical note {}", resultId, noteId);
        
        LabResult result = labResultRepository.findById(resultId)
            .orElseThrow(() -> new RuntimeException("Lab result not found: " + resultId));
        
        ClinicalNote note = clinicalNoteRepository.findById(noteId)
            .orElseThrow(() -> new RuntimeException("Clinical note not found: " + noteId));
        
        // Check if link already exists
        LabResultClinicalNote existingLink = labResultClinicalNoteRepository
            .findByResultIdAndNoteId(resultId, noteId);
        
        if (existingLink != null) {
            // Update existing link
            if (linkType != null) existingLink.setLinkType(linkType);
            if (linkStrength != null) existingLink.setLinkStrength(linkStrength);
            if (clinicalRelevance != null) existingLink.setClinicalRelevance(clinicalRelevance);
            if (notes != null) existingLink.setNotes(notes);
            existingLink.setLinkedBy(userId);
            existingLink.setLinkedDate(LocalDateTime.now());
            return labResultClinicalNoteRepository.save(existingLink);
        }
        
        // Create new link
        LabResultClinicalNote link = LabResultClinicalNote.builder()
            .result(result)
            .note(note)
            .organizationId(result.getOrganizationId())
            .linkType(linkType != null ? linkType : LabResultClinicalNote.LinkType.REFERENCED)
            .linkStrength(linkStrength != null ? linkStrength : LabResultClinicalNote.LinkStrength.MODERATE)
            .clinicalRelevance(clinicalRelevance)
            .linkedBy(userId)
            .linkedDate(LocalDateTime.now())
            .notes(notes)
            .build();
        
        LabResultClinicalNote savedLink = labResultClinicalNoteRepository.save(link);
        log.info("Linked result {} to clinical note {}", resultId, noteId);
        return savedLink;
    }
    
    /**
     * Get clinical notes linked to a lab result
     */
    public List<LabResultClinicalNote> getLinkedClinicalNotes(UUID resultId) {
        return labResultClinicalNoteRepository.findByResultResultId(resultId);
    }
    
    /**
     * Unlink a result from a clinical note
     */
    @Transactional
    public void unlinkResultFromClinicalNote(UUID resultId, UUID noteId) {
        log.info("Unlinking result {} from clinical note {}", resultId, noteId);
        LabResultClinicalNote link = labResultClinicalNoteRepository
            .findByResultIdAndNoteId(resultId, noteId);
        if (link != null) {
            labResultClinicalNoteRepository.delete(link);
            log.info("Unlinked result {} from clinical note {}", resultId, noteId);
        }
    }
    
    /**
     * Check for drug-lab interactions and create alerts
     */
    @Transactional
    public void checkDrugLabInteractions(LabResult result, Patient patient, UUID userId) {
        log.info("Checking drug-lab interactions for result: {}", result.getResultId());
        
        // Get active prescriptions for the patient
        List<Prescription> activePrescriptions = prescriptionRepository
            .findActivePrescriptionsByPatient(patient.getPatientId());
        
        if (activePrescriptions.isEmpty()) {
            log.debug("No active prescriptions found for patient {}", patient.getPatientId());
            return;
        }
        
        // Check each prescription for potential interactions
        for (Prescription prescription : activePrescriptions) {
            DrugLabInteractionAlert alert = checkInteraction(result, prescription, patient);
            if (alert != null) {
                alert.setCreatedBy(userId);
                drugLabInteractionAlertRepository.save(alert);
                log.info("Created drug-lab interaction alert: {} for result: {}", 
                    alert.getAlertId(), result.getResultId());
            }
        }
    }
    
    /**
     * Check for a specific drug-lab interaction
     */
    private DrugLabInteractionAlert checkInteraction(LabResult result, Prescription prescription, Patient patient) {
        String medicationName = prescription.getMedicationName().toUpperCase();
        String testName = result.getTestName().toUpperCase();
        String loincCode = result.getLoincCode();
        
        // Known drug-lab interactions (simplified - in production, use a comprehensive database)
        Map<String, List<String>> knownInteractions = getKnownDrugLabInteractions();
        
        // Check for interactions
        for (Map.Entry<String, List<String>> entry : knownInteractions.entrySet()) {
            String drug = entry.getKey();
            List<String> affectedTests = entry.getValue();
            
            // Check if medication matches
            if (medicationName.contains(drug) || 
                (prescription.getMedicationCode() != null && 
                 prescription.getMedicationCode().toUpperCase().contains(drug))) {
                
                // Check if test is affected
                for (String affectedTest : affectedTests) {
                    if (testName.contains(affectedTest) || loincCode.contains(affectedTest)) {
                        return createInteractionAlert(result, prescription, patient, drug, affectedTest);
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get known drug-lab interactions (simplified - in production, use external database)
     */
    private Map<String, List<String>> getKnownDrugLabInteractions() {
        Map<String, List<String>> interactions = new HashMap<>();
        
        // Example interactions
        interactions.put("WARFARIN", Arrays.asList("PT", "INR", "PROTHROMBIN"));
        interactions.put("HEPARIN", Arrays.asList("PTT", "APTT", "ACT"));
        interactions.put("ASPIRIN", Arrays.asList("PLATELET", "BLEEDING"));
        interactions.put("METFORMIN", Arrays.asList("CREATININE", "BUN", "LACTIC"));
        interactions.put("DIGOXIN", Arrays.asList("DIGOXIN", "POTASSIUM", "MAGNESIUM"));
        interactions.put("LITHIUM", Arrays.asList("LITHIUM", "CREATININE", "TSH"));
        interactions.put("ACE", Arrays.asList("POTASSIUM", "CREATININE", "BUN"));
        interactions.put("DIURETIC", Arrays.asList("POTASSIUM", "SODIUM", "CREATININE"));
        interactions.put("STATIN", Arrays.asList("CK", "LIVER", "ALT", "AST"));
        interactions.put("ANTIBIOTIC", Arrays.asList("CREATININE", "BUN", "LIVER"));
        
        return interactions;
    }
    
    /**
     * Create a drug-lab interaction alert
     */
    private DrugLabInteractionAlert createInteractionAlert(
            LabResult result, Prescription prescription, Patient patient,
            String drug, String affectedTest) {
        
        DrugLabInteractionAlert.InteractionType interactionType = determineInteractionType(result, drug, affectedTest);
        DrugLabInteractionAlert.InteractionSeverity severity = determineInteractionSeverity(interactionType, result);
        
        String description = String.format(
            "Potential drug-lab interaction: %s may affect %s test results. " +
            "Medication: %s, Test: %s (LOINC: %s).",
            drug, affectedTest, prescription.getMedicationName(), 
            result.getTestName(), result.getLoincCode()
        );
        
        String clinicalImpact = String.format(
            "The medication %s may cause false positive, false negative, or altered results " +
            "for the %s test. Clinical interpretation should consider this potential interaction.",
            prescription.getMedicationName(), result.getTestName()
        );
        
        String recommendedAction = String.format(
            "1. Consider timing of medication administration relative to test collection. " +
            "2. Review patient's medication history. " +
            "3. Consider alternative tests if available. " +
            "4. Monitor patient closely if results are critical."
        );
        
        return DrugLabInteractionAlert.builder()
            .result(result)
            .prescription(prescription)
            .patient(patient)
            .organizationId(result.getOrganizationId())
            .interactionType(interactionType)
            .interactionSeverity(severity)
            .interactionDescription(description)
            .affectedTest(result.getTestName())
            .affectedMedication(prescription.getMedicationName())
            .clinicalImpact(clinicalImpact)
            .recommendedAction(recommendedAction)
            .monitoringRequired(severity == DrugLabInteractionAlert.InteractionSeverity.HIGH || 
                               severity == DrugLabInteractionAlert.InteractionSeverity.CRITICAL)
            .monitoringFrequency(severity == DrugLabInteractionAlert.InteractionSeverity.CRITICAL ? 
                               "Immediate" : "As needed")
            .alertStatus(DrugLabInteractionAlert.AlertStatus.ACTIVE)
            .notificationSent(false)
            .build();
    }
    
    /**
     * Determine interaction type
     */
    private DrugLabInteractionAlert.InteractionType determineInteractionType(
            LabResult result, String drug, String affectedTest) {
        
        // Simplified logic - in production, use comprehensive database
        if (result.getIsCriticalValue() || result.getAbnormalFlag() != null) {
            return DrugLabInteractionAlert.InteractionType.ALTERED_RESULT;
        }
        
        return DrugLabInteractionAlert.InteractionType.INTERFERENCE;
    }
    
    /**
     * Determine interaction severity
     */
    private DrugLabInteractionAlert.InteractionSeverity determineInteractionSeverity(
            DrugLabInteractionAlert.InteractionType type, LabResult result) {
        
        if (result.getIsCriticalValue()) {
            return DrugLabInteractionAlert.InteractionSeverity.CRITICAL;
        }
        
        if (result.getAbnormalFlag() != null && result.getAbnormalFlag() != LabResult.AbnormalFlag.N) {
            return DrugLabInteractionAlert.InteractionSeverity.HIGH;
        }
        
        if (type == DrugLabInteractionAlert.InteractionType.FALSE_POSITIVE || 
            type == DrugLabInteractionAlert.InteractionType.FALSE_NEGATIVE) {
            return DrugLabInteractionAlert.InteractionSeverity.HIGH;
        }
        
        return DrugLabInteractionAlert.InteractionSeverity.MODERATE;
    }
    
    /**
     * Get drug-lab interaction alerts for a result
     */
    public List<DrugLabInteractionAlert> getDrugLabInteractionAlerts(UUID resultId) {
        return drugLabInteractionAlertRepository.findActiveAlertsByResult(resultId);
    }
    
    /**
     * Get all active drug-lab interaction alerts for a patient
     */
    public List<DrugLabInteractionAlert> getDrugLabInteractionAlertsByPatient(UUID patientId) {
        return drugLabInteractionAlertRepository.findActiveAlertsByPatient(patientId);
    }
    
    /**
     * Acknowledge a drug-lab interaction alert
     */
    @Transactional
    public DrugLabInteractionAlert acknowledgeDrugLabAlert(UUID alertId, String notes, UUID userId) {
        log.info("Acknowledging drug-lab interaction alert: {}", alertId);
        
        DrugLabInteractionAlert alert = drugLabInteractionAlertRepository.findById(alertId)
            .orElseThrow(() -> new RuntimeException("Drug-lab interaction alert not found: " + alertId));
        
        alert.setAlertStatus(DrugLabInteractionAlert.AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(userId);
        alert.setAcknowledgedDate(LocalDateTime.now());
        alert.setAcknowledgmentNotes(notes);
        alert.setUpdatedBy(userId);
        
        return drugLabInteractionAlertRepository.save(alert);
    }
    
    // ========== Test Panel Result Values ==========
    
    /**
     * Get all result values for a test panel result
     */
    public List<LabResultValueResponse> getPanelResultValues(UUID resultId) {
        log.info("Getting panel result values for result: {}", resultId);
        List<LabResultValue> values = labResultValueRepository.findByResultIdOrderedBySequence(resultId);
        return values.stream()
            .map(this::mapResultValueToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all result values for a test panel order
     */
    public List<LabResultValueResponse> getPanelOrderValues(UUID orderId) {
        log.info("Getting panel order values for order: {}", orderId);
        List<LabResultValue> values = labResultValueRepository.findByOrderIdOrderedBySequence(orderId);
        return values.stream()
            .map(this::mapResultValueToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Map LabResultValue entity to LabResultValueResponse DTO
     */
    private LabResultValueResponse mapResultValueToResponse(LabResultValue value) {
        return LabResultValueResponse.builder()
            .valueId(value.getValueId())
            .resultId(value.getResult().getResultId())
            .orderId(value.getOrder().getOrderId())
            .patientId(value.getPatient().getPatientId())
            .testName(value.getTestName())
            .loincCode(value.getLoincCode())
            .testCategory(value.getTestCategory())
            .testType(value.getTestType())
            .sequenceNumber(value.getSequenceNumber())
            .resultValue(value.getResultValue())
            .resultValueNumeric(value.getResultValueNumeric())
            .resultUnits(value.getResultUnits())
            .resultType(value.getResultType())
            .qualitativeResult(value.getQualitativeResult())
            .quantitativeResult(value.getQuantitativeResult())
            .referenceRangeLow(value.getReferenceRangeLow())
            .referenceRangeHigh(value.getReferenceRangeHigh())
            .referenceRangeUnits(value.getReferenceRangeUnits())
            .referenceRangeText(value.getReferenceRangeText())
            .abnormalFlag(value.getAbnormalFlag())
            .isCriticalValue(value.getIsCriticalValue())
            .isPanicValue(value.getIsPanicValue())
            .resultInterpretation(value.getResultInterpretation())
            .clinicalSignificance(value.getClinicalSignificance())
            .clinicalSignificanceLevel(value.getClinicalSignificanceLevel())
            .performingLaboratoryName(value.getPerformingLaboratoryName())
            .laboratoryComments(value.getLaboratoryComments())
            .methodUsed(value.getMethodUsed())
            .resultStatus(value.getResultStatus())
            .createdAt(value.getCreatedAt())
            .updatedAt(value.getUpdatedAt())
            .createdBy(value.getCreatedBy())
            .updatedBy(value.getUpdatedBy())
            .build();
    }
}
