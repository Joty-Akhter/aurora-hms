package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.ImagingStudyRequest;
import com.easyops.hospital.dto.response.ImagingStudyResponse;
import com.easyops.hospital.entity.*;
import com.easyops.hospital.repository.*;
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
public class ImagingStudyService {
    
    private final ImagingStudyRepository imagingStudyRepository;
    private final ImagingOrderRepository imagingOrderRepository;
    private final ImagingStudyHistoryRepository imagingStudyHistoryRepository;
    
    // ========== Imaging Study CRUD Operations ==========
    
    /**
     * Create a new imaging study result
     */
    @Transactional
    public ImagingStudyResponse createImagingStudy(ImagingStudyRequest request, UUID userId) {
        log.info("Creating imaging study for order: {}", request.getOrderId());
        
        // Verify order exists
        ImagingOrder order = imagingOrderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new RuntimeException("Imaging order not found: " + request.getOrderId()));
        
        // Get patient from order
        Patient patient = order.getPatient();
        if (request.getPatientId() != null && !request.getPatientId().equals(patient.getPatientId())) {
            throw new RuntimeException("Patient ID mismatch with order");
        }
        
        // Generate study number if not provided
        String studyNumber = request.getStudyNumber();
        if (studyNumber == null || studyNumber.isEmpty()) {
            studyNumber = generateStudyNumber();
        }
        
        // Check if accession number already exists
        if (imagingStudyRepository.findByAccessionNumber(request.getAccessionNumber()).isPresent()) {
            throw new RuntimeException("Accession number already exists: " + request.getAccessionNumber());
        }
        
        // Build imaging study entity
        ImagingStudy study = ImagingStudy.builder()
            .order(order)
            .patient(patient)
            .encounterId(request.getEncounterId() != null ? request.getEncounterId() : order.getEncounterId())
            .organizationId(order.getOrganizationId())
            .studyNumber(studyNumber)
            .accessionNumber(request.getAccessionNumber())
            .studyName(request.getStudyName())
            .studyModality(request.getStudyModality())
            .cptCode(request.getCptCode())
            .studyDate(request.getStudyDate())
            .studyCompletionDate(request.getStudyCompletionDate())
            .studyStatus(request.getStudyStatus() != null ? request.getStudyStatus() : ImagingStudy.StudyStatus.COMPLETED)
            .bodyPartExamined(request.getBodyPartExamined())
            .laterality(request.getLaterality())
            .numberOfImages(request.getNumberOfImages())
            .numberOfSeries(request.getNumberOfSeries())
            .contrastUsed(request.getContrastUsed() != null ? request.getContrastUsed() : false)
            .contrastType(request.getContrastType())
            .techniqueProtocol(request.getTechniqueProtocol())
            .equipmentUsed(request.getEquipmentUsed())
            .equipmentModel(request.getEquipmentModel())
            .radiationDose(request.getRadiationDose())
            .studyDurationMinutes(request.getStudyDurationMinutes())
            .interpretingRadiologistName(request.getInterpretingRadiologistName())
            .interpretingRadiologistNpi(request.getInterpretingRadiologistNpi())
            .interpretingRadiologistSpecialty(request.getInterpretingRadiologistSpecialty())
            .preliminaryReadingBy(request.getPreliminaryReadingBy())
            .reviewingRadiologist(request.getReviewingRadiologist())
            .reportDate(request.getReportDate())
            .reportFinalizedDate(request.getReportFinalizedDate())
            .clinicalHistory(request.getClinicalHistory())
            .techniqueDescription(request.getTechniqueDescription())
            .findings(request.getFindings())
            .impressionConclusion(request.getImpressionConclusion())
            .recommendations(request.getRecommendations())
            .urgencyIndicator(request.getUrgencyIndicator())
            .isPreliminary(request.getIsPreliminary() != null ? request.getIsPreliminary() : false)
            .isFinal(request.getIsFinal() != null ? request.getIsFinal() : false)
            .isAddendum(request.getIsAddendum() != null ? request.getIsAddendum() : false)
            .isAmended(request.getIsAmended() != null ? request.getIsAmended() : false)
            .isCancelled(request.getIsCancelled() != null ? request.getIsCancelled() : false)
            .hasCriticalFindings(request.getHasCriticalFindings() != null ? request.getHasCriticalFindings() : false)
            .isCriticalFindingAcknowledged(request.getIsCriticalFindingAcknowledged() != null ? request.getIsCriticalFindingAcknowledged() : false)
            .criticalFindingResponse(request.getCriticalFindingResponse())
            .isReviewed(request.getIsReviewed() != null ? request.getIsReviewed() : false)
            .reviewNotes(request.getReviewNotes())
            .correctionReason(request.getCorrectionReason())
            .amendmentReason(request.getAmendmentReason())
            .addendumReason(request.getAddendumReason())
            .cancellationReason(request.getCancellationReason())
            .dicomStudyInstanceUid(request.getDicomStudyInstanceUid())
            .dicomSeriesInstanceUid(request.getDicomSeriesInstanceUid())
            .dicomStorageLocation(request.getDicomStorageLocation())
            .pacsIntegrated(request.getPacsIntegrated() != null ? request.getPacsIntegrated() : false)
            .imagesAvailable(request.getImagesAvailable() != null ? request.getImagesAvailable() : false)
            .createdBy(userId)
            .updatedBy(userId)
            .build();
        
        // Set original study if this is a correction/amendment/addendum
        if (request.getOriginalStudyId() != null) {
            ImagingStudy originalStudy = imagingStudyRepository.findById(request.getOriginalStudyId())
                .orElseThrow(() -> new RuntimeException("Original study not found: " + request.getOriginalStudyId()));
            study.setOriginalStudy(originalStudy);
        }
        
        ImagingStudy savedStudy = imagingStudyRepository.save(study);
        
        // Create history record
        createHistoryRecord(savedStudy, ImagingStudyHistory.ChangeType.CREATED, userId, 
            "study_id", null, savedStudy.getStudyId().toString());
        
        log.info("Created imaging study: {}", savedStudy.getStudyId());
        return mapToResponse(savedStudy);
    }
    
    /**
     * Get imaging study by ID
     */
    public ImagingStudyResponse getImagingStudyById(UUID studyId) {
        ImagingStudy study = imagingStudyRepository.findById(studyId)
            .orElseThrow(() -> new RuntimeException("Imaging study not found: " + studyId));
        return mapToResponse(study);
    }
    
    /**
     * Get imaging study by study number
     */
    public ImagingStudyResponse getImagingStudyByNumber(String studyNumber) {
        ImagingStudy study = imagingStudyRepository.findByStudyNumber(studyNumber)
            .orElseThrow(() -> new RuntimeException("Imaging study not found: " + studyNumber));
        return mapToResponse(study);
    }
    
    /**
     * Get imaging study by accession number
     */
    public ImagingStudyResponse getImagingStudyByAccessionNumber(String accessionNumber) {
        ImagingStudy study = imagingStudyRepository.findByAccessionNumber(accessionNumber)
            .orElseThrow(() -> new RuntimeException("Imaging study not found: " + accessionNumber));
        return mapToResponse(study);
    }
    
    /**
     * Get all imaging studies for a patient
     */
    public List<ImagingStudyResponse> getImagingStudiesByPatient(UUID patientId) {
        List<ImagingStudy> studies = imagingStudyRepository.findByPatientPatientIdOrderByStudyDateDesc(patientId);
        return studies.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get imaging studies by order
     */
    public List<ImagingStudyResponse> getImagingStudiesByOrder(UUID orderId) {
        List<ImagingStudy> studies = imagingStudyRepository.findByOrderOrderId(orderId);
        return studies.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get imaging studies by encounter
     */
    public List<ImagingStudyResponse> getImagingStudiesByEncounter(UUID encounterId) {
        List<ImagingStudy> studies = imagingStudyRepository.findByEncounterId(encounterId);
        return studies.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get imaging studies by modality
     */
    public List<ImagingStudyResponse> getImagingStudiesByModality(UUID patientId, ImagingStudy.StudyModality modality) {
        List<ImagingStudy> studies = imagingStudyRepository.findStudiesByModality(patientId, modality);
        return studies.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get imaging studies by body part
     */
    public List<ImagingStudyResponse> getImagingStudiesByBodyPart(UUID patientId, String bodyPart) {
        List<ImagingStudy> studies = imagingStudyRepository.findStudiesByBodyPart(patientId, bodyPart);
        return studies.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get unacknowledged critical findings for a patient
     */
    public List<ImagingStudyResponse> getUnacknowledgedCriticalFindings(UUID patientId) {
        List<ImagingStudy> studies = imagingStudyRepository.findUnacknowledgedCriticalFindings(patientId);
        return studies.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all unacknowledged critical findings
     */
    public List<ImagingStudyResponse> getAllUnacknowledgedCriticalFindings() {
        List<ImagingStudy> studies = imagingStudyRepository.findAllUnacknowledgedCriticalFindings();
        return studies.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get unreviewed studies for a patient
     */
    public List<ImagingStudyResponse> getUnreviewedStudies(UUID patientId) {
        List<ImagingStudy> studies = imagingStudyRepository.findUnreviewedStudies(patientId);
        return studies.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Update an imaging study
     */
    @Transactional
    public ImagingStudyResponse updateImagingStudy(UUID studyId, ImagingStudyRequest request, UUID userId) {
        log.info("Updating imaging study: {}", studyId);
        
        ImagingStudy study = imagingStudyRepository.findById(studyId)
            .orElseThrow(() -> new RuntimeException("Imaging study not found: " + studyId));
        
        // Only allow updates to PRELIMINARY studies or if correcting/amending
        if (study.getIsFinal() && !study.getIsAmended() && !study.getIsAddendum()) {
            throw new RuntimeException("Cannot update final study. Use correct, amend, or addendum instead.");
        }
        
        // Update fields
        if (request.getFindings() != null) study.setFindings(request.getFindings());
        if (request.getImpressionConclusion() != null) study.setImpressionConclusion(request.getImpressionConclusion());
        if (request.getRecommendations() != null) study.setRecommendations(request.getRecommendations());
        if (request.getInterpretingRadiologistName() != null) study.setInterpretingRadiologistName(request.getInterpretingRadiologistName());
        if (request.getInterpretingRadiologistNpi() != null) study.setInterpretingRadiologistNpi(request.getInterpretingRadiologistNpi());
        if (request.getReportDate() != null) study.setReportDate(request.getReportDate());
        if (request.getReportFinalizedDate() != null) study.setReportFinalizedDate(request.getReportFinalizedDate());
        if (request.getIsPreliminary() != null) study.setIsPreliminary(request.getIsPreliminary());
        if (request.getIsFinal() != null) study.setIsFinal(request.getIsFinal());
        if (request.getHasCriticalFindings() != null) study.setHasCriticalFindings(request.getHasCriticalFindings());
        
        study.setUpdatedBy(userId);
        
        ImagingStudy updatedStudy = imagingStudyRepository.save(study);
        
        // Create history record
        createHistoryRecord(updatedStudy, ImagingStudyHistory.ChangeType.UPDATED, userId, 
            "study_id", studyId.toString(), updatedStudy.getStudyId().toString());
        
        log.info("Updated imaging study: {}", updatedStudy.getStudyId());
        return mapToResponse(updatedStudy);
    }
    
    /**
     * Mark study as reviewed
     */
    @Transactional
    public ImagingStudyResponse reviewImagingStudy(UUID studyId, String reviewNotes, UUID userId) {
        log.info("Reviewing imaging study: {}", studyId);
        
        ImagingStudy study = imagingStudyRepository.findById(studyId)
            .orElseThrow(() -> new RuntimeException("Imaging study not found: " + studyId));
        
        study.setIsReviewed(true);
        study.setReviewedBy(userId);
        study.setReviewedDate(LocalDateTime.now());
        if (reviewNotes != null) study.setReviewNotes(reviewNotes);
        study.setUpdatedBy(userId);
        
        ImagingStudy reviewedStudy = imagingStudyRepository.save(study);
        
        // Create history record
        createHistoryRecord(reviewedStudy, ImagingStudyHistory.ChangeType.REVIEWED, userId, 
            "is_reviewed", "false", "true");
        
        log.info("Reviewed imaging study: {}", reviewedStudy.getStudyId());
        return mapToResponse(reviewedStudy);
    }
    
    /**
     * Acknowledge critical finding
     */
    @Transactional
    public ImagingStudyResponse acknowledgeCriticalFinding(UUID studyId, String response, UUID userId) {
        log.info("Acknowledging critical finding for study: {}", studyId);
        
        ImagingStudy study = imagingStudyRepository.findById(studyId)
            .orElseThrow(() -> new RuntimeException("Imaging study not found: " + studyId));
        
        if (!study.getHasCriticalFindings()) {
            throw new RuntimeException("Study does not have critical findings");
        }
        
        study.setIsCriticalFindingAcknowledged(true);
        study.setCriticalFindingAcknowledgedBy(userId);
        study.setCriticalFindingAcknowledgedDate(LocalDateTime.now());
        if (response != null) study.setCriticalFindingResponse(response);
        study.setUpdatedBy(userId);
        
        ImagingStudy acknowledgedStudy = imagingStudyRepository.save(study);
        
        // Create history record
        createHistoryRecord(acknowledgedStudy, ImagingStudyHistory.ChangeType.ACKNOWLEDGED, userId, 
            "is_critical_finding_acknowledged", "false", "true");
        
        log.info("Acknowledged critical finding for study: {}", acknowledgedStudy.getStudyId());
        return mapToResponse(acknowledgedStudy);
    }
    
    /**
     * Finalize a study report
     */
    @Transactional
    public ImagingStudyResponse finalizeStudyReport(UUID studyId, UUID userId) {
        log.info("Finalizing study report: {}", studyId);
        
        ImagingStudy study = imagingStudyRepository.findById(studyId)
            .orElseThrow(() -> new RuntimeException("Imaging study not found: " + studyId));
        
        study.setIsFinal(true);
        study.setIsPreliminary(false);
        study.setReportFinalizedDate(LocalDateTime.now());
        study.setUpdatedBy(userId);
        
        ImagingStudy finalizedStudy = imagingStudyRepository.save(study);
        
        // Create history record
        createHistoryRecord(finalizedStudy, ImagingStudyHistory.ChangeType.UPDATED, userId, 
            "is_final", "false", "true");
        
        log.info("Finalized study report: {}", finalizedStudy.getStudyId());
        return mapToResponse(finalizedStudy);
    }
    
    /**
     * Generate study number
     */
    private String generateStudyNumber() {
        String prefix = "IMG";
        String timestamp = String.valueOf(System.currentTimeMillis());
        return prefix + "-" + timestamp.substring(timestamp.length() - 8);
    }
    
    /**
     * Create history record
     */
    private void createHistoryRecord(ImagingStudy study, ImagingStudyHistory.ChangeType changeType, 
                                     UUID userId, String fieldName, String previousValue, String newValue) {
        ImagingStudyHistory history = ImagingStudyHistory.builder()
            .study(study)
            .changeType(changeType)
            .changedBy(userId)
            .fieldName(fieldName)
            .previousValue(previousValue)
            .newValue(newValue)
            .build();
        
        imagingStudyHistoryRepository.save(history);
    }
    
    /**
     * Map ImagingStudy entity to ImagingStudyResponse DTO
     */
    private ImagingStudyResponse mapToResponse(ImagingStudy study) {
        Patient patient = study.getPatient();
        ImagingOrder order = study.getOrder();
        
        return ImagingStudyResponse.builder()
            .studyId(study.getStudyId())
            .orderId(order.getOrderId())
            .orderNumber(order.getOrderNumber())
            .patientId(patient.getPatientId())
            .patientName(PatientDisplayName.of(patient))
            .mrn(patient.getMrn())
            .encounterId(study.getEncounterId())
            .organizationId(study.getOrganizationId())
            .studyNumber(study.getStudyNumber())
            .accessionNumber(study.getAccessionNumber())
            .studyName(study.getStudyName())
            .studyModality(study.getStudyModality())
            .cptCode(study.getCptCode())
            .studyDate(study.getStudyDate())
            .studyCompletionDate(study.getStudyCompletionDate())
            .studyStatus(study.getStudyStatus())
            .bodyPartExamined(study.getBodyPartExamined())
            .laterality(study.getLaterality())
            .numberOfImages(study.getNumberOfImages())
            .numberOfSeries(study.getNumberOfSeries())
            .contrastUsed(study.getContrastUsed())
            .contrastType(study.getContrastType())
            .techniqueProtocol(study.getTechniqueProtocol())
            .equipmentUsed(study.getEquipmentUsed())
            .equipmentModel(study.getEquipmentModel())
            .radiationDose(study.getRadiationDose())
            .studyDurationMinutes(study.getStudyDurationMinutes())
            .interpretingRadiologistName(study.getInterpretingRadiologistName())
            .interpretingRadiologistNpi(study.getInterpretingRadiologistNpi())
            .interpretingRadiologistSpecialty(study.getInterpretingRadiologistSpecialty())
            .preliminaryReadingBy(study.getPreliminaryReadingBy())
            .reviewingRadiologist(study.getReviewingRadiologist())
            .reportDate(study.getReportDate())
            .reportFinalizedDate(study.getReportFinalizedDate())
            .clinicalHistory(study.getClinicalHistory())
            .techniqueDescription(study.getTechniqueDescription())
            .findings(study.getFindings())
            .impressionConclusion(study.getImpressionConclusion())
            .recommendations(study.getRecommendations())
            .urgencyIndicator(study.getUrgencyIndicator())
            .isPreliminary(study.getIsPreliminary())
            .isFinal(study.getIsFinal())
            .isAddendum(study.getIsAddendum())
            .isAmended(study.getIsAmended())
            .isCancelled(study.getIsCancelled())
            .hasCriticalFindings(study.getHasCriticalFindings())
            .isCriticalFindingAcknowledged(study.getIsCriticalFindingAcknowledged())
            .criticalFindingAcknowledgedBy(study.getCriticalFindingAcknowledgedBy())
            .criticalFindingAcknowledgedDate(study.getCriticalFindingAcknowledgedDate())
            .criticalFindingResponse(study.getCriticalFindingResponse())
            .isReviewed(study.getIsReviewed())
            .reviewedBy(study.getReviewedBy())
            .reviewedDate(study.getReviewedDate())
            .reviewNotes(study.getReviewNotes())
            .originalStudyId(study.getOriginalStudy() != null ? study.getOriginalStudy().getStudyId() : null)
            .correctionReason(study.getCorrectionReason())
            .amendmentReason(study.getAmendmentReason())
            .addendumReason(study.getAddendumReason())
            .cancellationReason(study.getCancellationReason())
            .correctionDate(study.getCorrectionDate())
            .amendmentDate(study.getAmendmentDate())
            .addendumDate(study.getAddendumDate())
            .cancellationDate(study.getCancellationDate())
            .dicomStudyInstanceUid(study.getDicomStudyInstanceUid())
            .dicomSeriesInstanceUid(study.getDicomSeriesInstanceUid())
            .dicomStorageLocation(study.getDicomStorageLocation())
            .pacsIntegrated(study.getPacsIntegrated())
            .imagesAvailable(study.getImagesAvailable())
            .createdAt(study.getCreatedAt())
            .updatedAt(study.getUpdatedAt())
            .createdBy(study.getCreatedBy())
            .updatedBy(study.getUpdatedBy())
            .build();
    }
}
