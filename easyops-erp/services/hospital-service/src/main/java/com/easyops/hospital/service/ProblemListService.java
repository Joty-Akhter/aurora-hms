package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.PatientProblemRequest;
import com.easyops.hospital.dto.request.ProblemResolutionRequest;
import com.easyops.hospital.dto.response.*;
import com.easyops.hospital.entity.*;
import com.easyops.hospital.events.DomainEventPublisher;
import com.easyops.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProblemListService {
    
    private final PatientProblemRepository patientProblemRepository;
    private final ProblemHistoryRepository problemHistoryRepository;
    private final PatientRepository patientRepository;
    private final ProblemMedicationRepository problemMedicationRepository;
    private final MedicationRepository medicationRepository;
    private final DomainEventPublisher domainEventPublisher;

    private String normalizeToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeCodeField(String value, int maxLength, String fieldName) {
        String normalized = normalizeToNull(value);
        if (normalized == null) {
            return null;
        }

        // UI autocomplete may submit "CODE - Description"; keep only the code token.
        int separatorIdx = normalized.indexOf(" - ");
        if (separatorIdx > 0) {
            normalized = normalized.substring(0, separatorIdx).trim();
        }

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters");
        }
        return normalized;
    }

    private String normalizeBoundedText(String value, int maxLength, String fieldName) {
        String normalized = normalizeToNull(value);
        if (normalized != null && normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters");
        }
        return normalized;
    }
    
    /**
     * Create a new patient problem
     */
    @Transactional
    public PatientProblemResponse createProblem(PatientProblemRequest request, UUID userId) {
        log.info("Creating problem for patient: {}", request.getPatientId());
        
        // Verify patient exists
        Patient patient = patientRepository.findById(request.getPatientId())
            .orElseThrow(() -> new RuntimeException("Patient not found: " + request.getPatientId()));
        
        String normalizedProblemName = normalizeBoundedText(request.getProblemName(), 500, "Problem name");
        String normalizedIcd10Code = normalizeCodeField(request.getIcd10Code(), 20, "ICD-10 code");
        String normalizedIcd11Code = normalizeCodeField(request.getIcd11Code(), 20, "ICD-11 code");
        String normalizedSnomedCode = normalizeCodeField(request.getSnomedCode(), 50, "SNOMED code");
        String normalizedChronicity = normalizeBoundedText(request.getChronicity(), 50, "Chronicity");

        // Build problem entity
        PatientProblem problem = PatientProblem.builder()
            .patient(patient)
            .encounterId(request.getEncounterId())
            .problemName(normalizedProblemName)
            .icd10Code(normalizedIcd10Code)
            .icd11Code(normalizedIcd11Code)
            .snomedCode(normalizedSnomedCode)
            .problemType(request.getProblemType())
            .status(request.getStatus() != null ? request.getStatus() : PatientProblem.ProblemStatus.ACTIVE)
            .onsetDate(request.getOnsetDate())
            .resolutionDate(request.getResolutionDate())
            .severity(request.getSeverity())
            .chronicity(normalizedChronicity)
            .priority(request.getPriority())
            .documentedBy(userId)
            .documentedDate(LocalDate.now())
            .resolutionNotes(normalizeToNull(request.getResolutionNotes()))
            .notes(normalizeToNull(request.getNotes()))
            .createdBy(userId)
            .build();
        
        PatientProblem savedProblem = patientProblemRepository.save(problem);
        log.info("Created problem: {}", savedProblem.getProblemId());
        
        // History is created automatically by database trigger
        
        PatientProblemResponse response = mapToResponse(savedProblem);

        domainEventPublisher.publish("diagnosis.added", java.util.Map.of(
            "problemId", response.getProblemId(),
            "patientId", response.getPatientId(),
            "problemName", response.getProblemName(),
            "status", response.getStatus(),
            "problemType", response.getProblemType()
        ));

        return response;
    }
    
    /**
     * Get problem by ID
     */
    public PatientProblemResponse getProblemById(UUID problemId) {
        PatientProblem problem = patientProblemRepository.findById(problemId)
            .orElseThrow(() -> new RuntimeException("Problem not found: " + problemId));
        return mapToResponse(problem);
    }
    
    /**
     * Get all problems for a patient
     */
    public List<PatientProblemResponse> getProblemsByPatient(UUID patientId) {
        List<PatientProblem> problems = patientProblemRepository.findByPatientPatientIdOrderByDocumentedDateDesc(patientId);
        return problems.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get active problems for a patient
     */
    public List<PatientProblemResponse> getActiveProblemsByPatient(UUID patientId) {
        List<PatientProblem> problems = patientProblemRepository.findActiveProblemsByPatient(patientId);
        return problems.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get resolved problems for a patient
     */
    public List<PatientProblemResponse> getResolvedProblemsByPatient(UUID patientId) {
        List<PatientProblem> problems = patientProblemRepository.findResolvedProblemsByPatient(patientId);
        return problems.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get current problems (active + chronic) for a patient
     */
    public List<PatientProblemResponse> getCurrentProblemsByPatient(UUID patientId) {
        List<PatientProblem> problems = patientProblemRepository.findCurrentProblemsByPatient(patientId);
        return problems.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get problems by type for a patient
     */
    public List<PatientProblemResponse> getProblemsByPatientAndType(
        UUID patientId, PatientProblem.ProblemType problemType) {
        List<PatientProblem> problems = patientProblemRepository.findByPatientPatientIdAndProblemType(patientId, problemType);
        return problems.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get problems by status for a patient
     */
    public List<PatientProblemResponse> getProblemsByPatientAndStatus(
        UUID patientId, PatientProblem.ProblemStatus status) {
        List<PatientProblem> problems = patientProblemRepository.findByPatientPatientIdAndStatus(patientId, status);
        return problems.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get problems by priority for a patient
     */
    public List<PatientProblemResponse> getProblemsByPatientAndPriority(
        UUID patientId, PatientProblem.Priority priority) {
        List<PatientProblem> problems = patientProblemRepository.findProblemsByPatientAndPriority(patientId, priority);
        return problems.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Search problems
     */
    public List<PatientProblemResponse> searchProblems(UUID patientId, String searchTerm) {
        List<PatientProblem> problems = patientProblemRepository.searchProblems(patientId, searchTerm);
        return problems.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Update a problem
     */
    @Transactional
    public PatientProblemResponse updateProblem(UUID patientId, UUID problemId, PatientProblemRequest request, UUID userId) {
        log.info("Updating problem: {}", problemId);
        
        PatientProblem problem = patientProblemRepository.findByProblemIdAndPatientPatientId(problemId, patientId)
            .orElseThrow(() -> new RuntimeException("Problem not found for patient: " + problemId));
        
        String normalizedProblemName = normalizeBoundedText(request.getProblemName(), 500, "Problem name");
        String normalizedIcd10Code = normalizeCodeField(request.getIcd10Code(), 20, "ICD-10 code");
        String normalizedIcd11Code = normalizeCodeField(request.getIcd11Code(), 20, "ICD-11 code");
        String normalizedSnomedCode = normalizeCodeField(request.getSnomedCode(), 50, "SNOMED code");
        String normalizedChronicity = normalizeBoundedText(request.getChronicity(), 50, "Chronicity");

        // Update fields
        if (request.getProblemName() != null) problem.setProblemName(normalizedProblemName);
        if (request.getIcd10Code() != null) problem.setIcd10Code(normalizedIcd10Code);
        if (request.getIcd11Code() != null) problem.setIcd11Code(normalizedIcd11Code);
        if (request.getSnomedCode() != null) problem.setSnomedCode(normalizedSnomedCode);
        if (request.getProblemType() != null) problem.setProblemType(request.getProblemType());
        if (request.getStatus() != null) problem.setStatus(request.getStatus());
        if (request.getOnsetDate() != null) problem.setOnsetDate(request.getOnsetDate());
        if (request.getResolutionDate() != null) problem.setResolutionDate(request.getResolutionDate());
        if (request.getSeverity() != null) problem.setSeverity(request.getSeverity());
        if (request.getChronicity() != null) problem.setChronicity(normalizedChronicity);
        if (request.getPriority() != null) problem.setPriority(request.getPriority());
        if (request.getNotes() != null) problem.setNotes(normalizeToNull(request.getNotes()));
        
        problem.setUpdatedBy(userId);
        
        PatientProblem updatedProblem = patientProblemRepository.save(problem);
        log.info("Updated problem: {}", updatedProblem.getProblemId());
        
        // History is created automatically by database trigger
        
        PatientProblemResponse response = mapToResponse(updatedProblem);

        domainEventPublisher.publish("diagnosis.updated", java.util.Map.of(
            "problemId", response.getProblemId(),
            "patientId", response.getPatientId(),
            "problemName", response.getProblemName(),
            "status", response.getStatus(),
            "problemType", response.getProblemType()
        ));

        return response;
    }
    
    /**
     * Resolve a problem
     */
    @Transactional
    public PatientProblemResponse resolveProblem(UUID problemId, ProblemResolutionRequest request, UUID userId) {
        log.info("Resolving problem: {}", problemId);
        
        PatientProblem problem = patientProblemRepository.findById(problemId)
            .orElseThrow(() -> new RuntimeException("Problem not found: " + problemId));
        
        if (problem.getStatus() == PatientProblem.ProblemStatus.RESOLVED) {
            throw new RuntimeException("Problem is already resolved");
        }
        
        problem.setStatus(PatientProblem.ProblemStatus.RESOLVED);
        problem.setResolvedBy(userId);
        problem.setResolvedDate(request.getResolutionDate() != null ? request.getResolutionDate() : LocalDate.now());
        problem.setResolutionNotes(request.getResolutionNotes());
        problem.setUpdatedBy(userId);
        
        PatientProblem resolvedProblem = patientProblemRepository.save(problem);
        log.info("Resolved problem: {}", resolvedProblem.getProblemId());
        
        // History is created automatically by database trigger
        
        PatientProblemResponse response = mapToResponse(resolvedProblem);

        domainEventPublisher.publish("diagnosis.updated", java.util.Map.of(
            "problemId", response.getProblemId(),
            "patientId", response.getPatientId(),
            "problemName", response.getProblemName(),
            "status", response.getStatus(),
            "problemType", response.getProblemType()
        ));

        return response;
    }
    
    /**
     * Reactivate a resolved problem
     */
    @Transactional
    public PatientProblemResponse reactivateProblem(UUID problemId, UUID userId) {
        log.info("Reactivating problem: {}", problemId);
        
        PatientProblem problem = patientProblemRepository.findById(problemId)
            .orElseThrow(() -> new RuntimeException("Problem not found: " + problemId));
        
        if (problem.getStatus() != PatientProblem.ProblemStatus.RESOLVED) {
            throw new RuntimeException("Only resolved problems can be reactivated");
        }
        
        problem.setStatus(PatientProblem.ProblemStatus.ACTIVE);
        problem.setResolvedBy(null);
        problem.setResolvedDate(null);
        problem.setResolutionNotes(null);
        problem.setUpdatedBy(userId);
        
        PatientProblem reactivatedProblem = patientProblemRepository.save(problem);
        log.info("Reactivated problem: {}", reactivatedProblem.getProblemId());
        
        // Create history record for reactivation
        ProblemHistory history = ProblemHistory.builder()
            .problem(reactivatedProblem)
            .changeType(ProblemHistory.ChangeType.REACTIVATED)
            .changedBy(userId)
            .changedDate(LocalDateTime.now())
            .fieldName("status")
            .previousValue("RESOLVED")
            .newValue("ACTIVE")
            .build();
        problemHistoryRepository.save(history);
        
        PatientProblemResponse response = mapToResponse(reactivatedProblem);

        domainEventPublisher.publish("diagnosis.updated", java.util.Map.of(
            "problemId", response.getProblemId(),
            "patientId", response.getPatientId(),
            "problemName", response.getProblemName(),
            "status", response.getStatus(),
            "problemType", response.getProblemType()
        ));

        return response;
    }
    
    /**
     * Get problem history
     */
    public List<ProblemHistoryResponse> getProblemHistory(UUID problemId) {
        List<ProblemHistory> history = problemHistoryRepository.findHistoryByProblem(problemId);
        return history.stream()
            .map(this::mapHistoryToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get problem list summary for a patient
     */
    public ProblemListSummaryResponse getProblemListSummary(UUID patientId) {
        List<PatientProblem> allProblems = patientProblemRepository.findByPatientPatientId(patientId);
        Long totalProblems = (long) allProblems.size();
        Long activeProblems = patientProblemRepository.countActiveProblemsByPatient(patientId);
        Long resolvedProblems = patientProblemRepository.countResolvedProblemsByPatient(patientId);
        
        List<PatientProblem> activeList = patientProblemRepository.findActiveProblemsByPatient(patientId);
        List<PatientProblem> resolvedList = patientProblemRepository.findResolvedProblemsByPatient(patientId);
        List<PatientProblem> highPriority = patientProblemRepository.findProblemsByPatientAndPriority(
            patientId, PatientProblem.Priority.HIGH);
        
        // Count chronic problems (status = CHRONIC)
        long chronicProblems = allProblems.stream()
            .filter(p -> p.getStatus() == PatientProblem.ProblemStatus.CHRONIC)
            .count();
        
        return ProblemListSummaryResponse.builder()
            .totalProblems(totalProblems)
            .activeProblems(activeProblems)
            .resolvedProblems(resolvedProblems)
            .chronicProblems(chronicProblems)
            .activeProblemsList(activeList.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()))
            .resolvedProblemsList(resolvedList.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()))
            .highPriorityProblems(highPriority.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()))
            .build();
    }
    
    /**
     * Delete a problem
     */
    @Transactional
    public void deleteProblem(UUID patientId, UUID problemId) {
        log.info("Deleting problem: {}", problemId);
        
        PatientProblem problem = patientProblemRepository.findByProblemIdAndPatientPatientId(problemId, patientId)
            .orElseThrow(() -> new RuntimeException("Problem not found for patient: " + problemId));
        
        patientProblemRepository.delete(problem);
        log.info("Deleted problem: {}", problemId);
    }
    
    // ========== Medication Integration Methods ==========
    
    /**
     * Link medication to problem
     */
    @Transactional
    public ProblemMedicationResponse linkMedicationToProblem(UUID problemId, UUID medicationId,
            ProblemMedication.LinkType linkType, ProblemMedication.LinkStrength linkStrength,
            String clinicalRelevance, String notes, UUID userId) {
        log.info("Linking medication {} to problem {}", medicationId, problemId);
        
        PatientProblem problem = patientProblemRepository.findById(problemId)
            .orElseThrow(() -> new RuntimeException("Problem not found: " + problemId));
        
        Medication medication = medicationRepository.findById(medicationId)
            .orElseThrow(() -> new RuntimeException("Medication not found: " + medicationId));
        
        // Check if link already exists
        ProblemMedication existingLink = problemMedicationRepository
            .findByProblemIdAndMedicationId(problemId, medicationId);
        
        if (existingLink != null) {
            log.warn("Link already exists between problem {} and medication {}", problemId, medicationId);
            return mapMedicationLinkToResponse(existingLink);
        }
        
        ProblemMedication link = ProblemMedication.builder()
            .problem(problem)
            .medication(medication)
            .organizationId(problem.getPatient().getPatientId()) // Use patient's organization
            .linkType(linkType != null ? linkType : ProblemMedication.LinkType.TREATS)
            .linkStrength(linkStrength != null ? linkStrength : ProblemMedication.LinkStrength.MODERATE)
            .clinicalRelevance(clinicalRelevance)
            .linkedBy(userId)
            .linkedDate(LocalDateTime.now())
            .notes(notes)
            .build();
        
        ProblemMedication savedLink = problemMedicationRepository.save(link);
        log.info("Linked medication to problem: {}", savedLink.getLinkId());
        
        return mapMedicationLinkToResponse(savedLink);
    }
    
    /**
     * Get medications linked to problem
     */
    public List<ProblemMedicationResponse> getMedicationsByProblem(UUID problemId) {
        List<ProblemMedication> links = problemMedicationRepository.findByProblemIdOrdered(problemId);
        return links.stream()
            .map(this::mapMedicationLinkToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Unlink medication from problem
     */
    @Transactional
    public void unlinkMedicationFromProblem(UUID problemId, UUID medicationId) {
        log.info("Unlinking medication {} from problem {}", medicationId, problemId);
        
        ProblemMedication link = problemMedicationRepository
            .findByProblemIdAndMedicationId(problemId, medicationId);
        
        if (link == null) {
            throw new RuntimeException("Link not found between problem and medication");
        }
        
        problemMedicationRepository.delete(link);
        log.info("Unlinked medication from problem");
    }
    
    // ========== Mapping Methods ==========
    
    private PatientProblemResponse mapToResponse(PatientProblem problem) {
        List<ProblemHistory> history = problemHistoryRepository.findHistoryByProblem(problem.getProblemId());
        
        return PatientProblemResponse.builder()
            .problemId(problem.getProblemId())
            .patientId(problem.getPatient().getPatientId())
            .encounterId(problem.getEncounterId())
            .problemName(problem.getProblemName())
            .icd10Code(problem.getIcd10Code())
            .icd11Code(problem.getIcd11Code())
            .snomedCode(problem.getSnomedCode())
            .problemType(problem.getProblemType())
            .status(problem.getStatus())
            .onsetDate(problem.getOnsetDate())
            .resolutionDate(problem.getResolutionDate())
            .severity(problem.getSeverity())
            .chronicity(problem.getChronicity())
            .priority(problem.getPriority())
            .documentedBy(problem.getDocumentedBy())
            .documentedDate(problem.getDocumentedDate())
            .resolvedBy(problem.getResolvedBy())
            .resolvedDate(problem.getResolvedDate())
            .resolutionNotes(problem.getResolutionNotes())
            .notes(problem.getNotes())
            .createdAt(problem.getCreatedAt())
            .updatedAt(problem.getUpdatedAt())
            .createdBy(problem.getCreatedBy())
            .updatedBy(problem.getUpdatedBy())
            .history(history.stream()
                .map(this::mapHistoryToResponse)
                .collect(Collectors.toList()))
            .historyCount(history.size())
            .build();
    }
    
    private ProblemMedicationResponse mapMedicationLinkToResponse(ProblemMedication link) {
        return ProblemMedicationResponse.builder()
            .linkId(link.getLinkId())
            .problemId(link.getProblem().getProblemId())
            .medicationId(link.getMedication().getMedicationId())
            .medicationName(link.getMedication().getMedicationName())
            .genericName(link.getMedication().getGenericName())
            .linkType(link.getLinkType().toString())
            .linkStrength(link.getLinkStrength().toString())
            .clinicalRelevance(link.getClinicalRelevance())
            .notes(link.getNotes())
            .linkedBy(link.getLinkedBy())
            .linkedDate(link.getLinkedDate())
            .createdAt(link.getCreatedAt())
            .updatedAt(link.getUpdatedAt())
            .build();
    }
    
    private ProblemHistoryResponse mapHistoryToResponse(ProblemHistory history) {
        return ProblemHistoryResponse.builder()
            .historyId(history.getHistoryId())
            .problemId(history.getProblem().getProblemId())
            .changeType(history.getChangeType())
            .changedBy(history.getChangedBy())
            .changedDate(history.getChangedDate())
            .previousValue(history.getPreviousValue())
            .newValue(history.getNewValue())
            .changeReason(history.getChangeReason())
            .fieldName(history.getFieldName())
            .notes(history.getNotes())
            .createdAt(history.getCreatedAt())
            .build();
    }
}
