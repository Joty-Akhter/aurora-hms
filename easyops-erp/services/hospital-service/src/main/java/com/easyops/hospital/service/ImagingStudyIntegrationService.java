package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.ImagingStudyLinkRequest;
import com.easyops.hospital.dto.response.ImagingStudyLinkResponse;
import com.easyops.hospital.entity.*;
import com.easyops.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for integrating imaging studies with other clinical data.
 * Handles linking to encounters, problems, clinical notes, and medications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImagingStudyIntegrationService {
    
    private final ImagingStudyRepository imagingStudyRepository;
    private final ImagingStudyClinicalNoteRepository clinicalNoteRepository;
    private final ImagingStudyProblemRepository problemRepository;
    private final ImagingStudyMedicationRepository medicationRepository;
    private final ClinicalNoteRepository noteRepository;
    private final PatientProblemRepository patientProblemRepository;
    private final PrescriptionRepository prescriptionRepository;
    
    // ========== Clinical Notes Integration ==========
    
    /**
     * Link imaging study to clinical note
     */
    @Transactional
    public ImagingStudyLinkResponse linkToClinicalNote(ImagingStudyLinkRequest request, UUID userId) {
        log.info("Linking imaging study {} to clinical note {}", request.getStudyId(), request.getTargetId());
        
        ImagingStudy study = imagingStudyRepository.findById(request.getStudyId())
            .orElseThrow(() -> new RuntimeException("Imaging study not found: " + request.getStudyId()));
        
        ClinicalNote note = noteRepository.findById(request.getTargetId())
            .orElseThrow(() -> new RuntimeException("Clinical note not found: " + request.getTargetId()));
        
        ImagingStudyClinicalNote link = ImagingStudyClinicalNote.builder()
            .study(study)
            .note(note)
            .organizationId(study.getOrganizationId())
            .linkType(request.getLinkType() != null 
                ? ImagingStudyClinicalNote.LinkType.valueOf(request.getLinkType())
                : ImagingStudyClinicalNote.LinkType.REFERENCED)
            .linkStrength(request.getLinkStrength() != null
                ? ImagingStudyClinicalNote.LinkStrength.valueOf(request.getLinkStrength())
                : ImagingStudyClinicalNote.LinkStrength.MODERATE)
            .clinicalRelevance(request.getClinicalRelevance())
            .linkedBy(userId)
            .linkedDate(LocalDateTime.now())
            .notes(request.getNotes())
            .build();
        
        ImagingStudyClinicalNote savedLink = clinicalNoteRepository.save(link);
        
        log.info("Linked imaging study to clinical note: {}", savedLink.getLinkId());
        return mapToResponse(savedLink);
    }
    
    /**
     * Unlink imaging study from clinical note
     */
    @Transactional
    public void unlinkFromClinicalNote(UUID linkId) {
        log.info("Unlinking imaging study from clinical note: {}", linkId);
        clinicalNoteRepository.deleteById(linkId);
    }
    
    /**
     * Get clinical notes linked to imaging study
     */
    public List<ImagingStudyLinkResponse> getClinicalNotesByStudy(UUID studyId) {
        List<ImagingStudyClinicalNote> links = clinicalNoteRepository.findByStudyIdOrdered(studyId);
        return links.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get imaging studies linked to clinical note
     */
    public List<ImagingStudyLinkResponse> getStudiesByClinicalNote(UUID noteId) {
        List<ImagingStudyClinicalNote> links = clinicalNoteRepository.findByNoteIdOrdered(noteId);
        return links.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    // ========== Problems Integration ==========
    
    /**
     * Link imaging study to problem/diagnosis
     */
    @Transactional
    public ImagingStudyLinkResponse linkToProblem(ImagingStudyLinkRequest request, UUID userId) {
        log.info("Linking imaging study {} to problem {}", request.getStudyId(), request.getTargetId());
        
        ImagingStudy study = imagingStudyRepository.findById(request.getStudyId())
            .orElseThrow(() -> new RuntimeException("Imaging study not found: " + request.getStudyId()));
        
        PatientProblem problem = patientProblemRepository.findById(request.getTargetId())
            .orElseThrow(() -> new RuntimeException("Problem not found: " + request.getTargetId()));
        
        ImagingStudyProblem link = ImagingStudyProblem.builder()
            .study(study)
            .problem(problem)
            .organizationId(study.getOrganizationId())
            .linkType(request.getLinkType() != null
                ? ImagingStudyProblem.LinkType.valueOf(request.getLinkType())
                : ImagingStudyProblem.LinkType.RELATED)
            .linkStrength(request.getLinkStrength() != null
                ? ImagingStudyProblem.LinkStrength.valueOf(request.getLinkStrength())
                : ImagingStudyProblem.LinkStrength.MODERATE)
            .clinicalRelevance(request.getClinicalRelevance())
            .linkedBy(userId)
            .linkedDate(LocalDateTime.now())
            .notes(request.getNotes())
            .build();
        
        ImagingStudyProblem savedLink = problemRepository.save(link);
        
        log.info("Linked imaging study to problem: {}", savedLink.getLinkId());
        return mapToResponse(savedLink);
    }
    
    /**
     * Unlink imaging study from problem
     */
    @Transactional
    public void unlinkFromProblem(UUID linkId) {
        log.info("Unlinking imaging study from problem: {}", linkId);
        problemRepository.deleteById(linkId);
    }
    
    /**
     * Get problems linked to imaging study
     */
    public List<ImagingStudyLinkResponse> getProblemsByStudy(UUID studyId) {
        List<ImagingStudyProblem> links = problemRepository.findByStudyIdOrdered(studyId);
        return links.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get imaging studies linked to problem
     */
    public List<ImagingStudyLinkResponse> getStudiesByProblem(UUID problemId) {
        List<ImagingStudyProblem> links = problemRepository.findByProblemIdOrdered(problemId);
        return links.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    // ========== Medications Integration ==========
    
    /**
     * Link imaging study to medication (e.g., contrast agent)
     */
    @Transactional
    public ImagingStudyLinkResponse linkToMedication(ImagingStudyLinkRequest request, UUID userId) {
        log.info("Linking imaging study {} to medication {}", request.getStudyId(), request.getTargetId());
        
        ImagingStudy study = imagingStudyRepository.findById(request.getStudyId())
            .orElseThrow(() -> new RuntimeException("Imaging study not found: " + request.getStudyId()));
        
        Prescription medication = prescriptionRepository.findById(request.getTargetId())
            .orElseThrow(() -> new RuntimeException("Prescription not found: " + request.getTargetId()));
        
        ImagingStudyMedication link = ImagingStudyMedication.builder()
            .study(study)
            .prescription(medication)
            .organizationId(study.getOrganizationId())
            .linkType(request.getLinkType() != null
                ? ImagingStudyMedication.LinkType.valueOf(request.getLinkType())
                : ImagingStudyMedication.LinkType.CONTRAST_AGENT)
            .linkStrength(request.getLinkStrength() != null
                ? ImagingStudyMedication.LinkStrength.valueOf(request.getLinkStrength())
                : ImagingStudyMedication.LinkStrength.MODERATE)
            .clinicalRelevance(request.getClinicalRelevance())
            .linkedBy(userId)
            .linkedDate(LocalDateTime.now())
            .notes(request.getNotes())
            .build();
        
        ImagingStudyMedication savedLink = medicationRepository.save(link);
        
        log.info("Linked imaging study to medication: {}", savedLink.getLinkId());
        return mapToResponse(savedLink);
    }
    
    /**
     * Unlink imaging study from medication
     */
    @Transactional
    public void unlinkFromMedication(UUID linkId) {
        log.info("Unlinking imaging study from medication: {}", linkId);
        medicationRepository.deleteById(linkId);
    }
    
    /**
     * Get medications linked to imaging study
     */
    public List<ImagingStudyLinkResponse> getMedicationsByStudy(UUID studyId) {
        List<ImagingStudyMedication> links = medicationRepository.findByStudyIdOrdered(studyId);
        return links.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get imaging studies linked to medication
     */
    public List<ImagingStudyLinkResponse> getStudiesByMedication(UUID prescriptionId) {
        List<ImagingStudyMedication> links = medicationRepository.findByPrescriptionIdOrdered(prescriptionId);
        return links.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    // ========== Encounter Integration ==========
    
    /**
     * Link imaging study to encounter (update encounter_id)
     */
    @Transactional
    public void linkToEncounter(UUID studyId, UUID encounterId, UUID userId) {
        log.info("Linking imaging study {} to encounter {}", studyId, encounterId);
        
        ImagingStudy study = imagingStudyRepository.findById(studyId)
            .orElseThrow(() -> new RuntimeException("Imaging study not found: " + studyId));
        
        study.setEncounterId(encounterId);
        imagingStudyRepository.save(study);
        
        log.info("Linked imaging study to encounter");
    }
    
    /**
     * Get imaging studies by encounter
     */
    public List<ImagingStudy> getStudiesByEncounter(UUID encounterId) {
        return imagingStudyRepository.findByEncounterId(encounterId);
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Map clinical note link to response
     */
    private ImagingStudyLinkResponse mapToResponse(ImagingStudyClinicalNote link) {
        return ImagingStudyLinkResponse.builder()
            .linkId(link.getLinkId())
            .studyId(link.getStudy().getStudyId())
            .targetId(link.getNote().getNoteId())
            .targetType("CLINICAL_NOTE")
            .linkType(link.getLinkType().toString())
            .linkStrength(link.getLinkStrength().toString())
            .clinicalRelevance(link.getClinicalRelevance())
            .notes(link.getNotes())
            .linkedBy(link.getLinkedBy())
            .linkedDate(link.getLinkedDate())
            .build();
    }
    
    /**
     * Map problem link to response
     */
    private ImagingStudyLinkResponse mapToResponse(ImagingStudyProblem link) {
        return ImagingStudyLinkResponse.builder()
            .linkId(link.getLinkId())
            .studyId(link.getStudy().getStudyId())
            .targetId(link.getProblem().getProblemId())
            .targetType("PROBLEM")
            .linkType(link.getLinkType().toString())
            .linkStrength(link.getLinkStrength().toString())
            .clinicalRelevance(link.getClinicalRelevance())
            .notes(link.getNotes())
            .linkedBy(link.getLinkedBy())
            .linkedDate(link.getLinkedDate())
            .build();
    }
    
    /**
     * Map medication link to response
     */
    private ImagingStudyLinkResponse mapToResponse(ImagingStudyMedication link) {
        return ImagingStudyLinkResponse.builder()
            .linkId(link.getLinkId())
            .studyId(link.getStudy().getStudyId())
            .targetId(link.getPrescription().getPrescriptionId())
            .targetType("MEDICATION")
            .linkType(link.getLinkType().toString())
            .linkStrength(link.getLinkStrength().toString())
            .clinicalRelevance(link.getClinicalRelevance())
            .notes(link.getNotes())
            .linkedBy(link.getLinkedBy())
            .linkedDate(link.getLinkedDate())
            .build();
    }
}
