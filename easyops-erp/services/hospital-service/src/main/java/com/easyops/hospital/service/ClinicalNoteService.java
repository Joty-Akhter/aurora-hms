package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.*;
import com.easyops.hospital.dto.response.*;
import com.easyops.hospital.entity.*;
import com.easyops.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicalNoteService {
    
    private final ClinicalNoteRepository clinicalNoteRepository;
    private final NoteAttachmentRepository noteAttachmentRepository;
    private final NoteTemplateRepository noteTemplateRepository;
    private final PatientRepository patientRepository;
    private final ClinicalNoteMedicationRepository clinicalNoteMedicationRepository;
    private final MedicationRepository medicationRepository;
    private final DocumentStorageService documentStorageService;
    
    /**
     * Create a new clinical note
     */
    @Transactional
    public ClinicalNoteResponse createNote(ClinicalNoteRequest request, UUID userId) {
        log.info("Creating clinical note for patient: {}", request.getPatientId());
        
        // Verify patient exists
        Patient patient = patientRepository.findById(request.getPatientId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Patient not found: " + request.getPatientId()));
        
        // Load template if provided
        NoteTemplate template = null;
        if (request.getTemplateId() != null) {
            template = noteTemplateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Template not found: " + request.getTemplateId()));
            
            // Increment template usage
            template.setUsageCount(template.getUsageCount() + 1);
            template.setLastUsedDate(LocalDateTime.now());
            noteTemplateRepository.save(template);
        }
        
        // Build note entity
        ClinicalNote note = ClinicalNote.builder()
            .patient(patient)
            .encounterId(request.getEncounterId())
            .noteType(request.getNoteType())
            .noteDate(request.getNoteDate())
            .noteTime(request.getNoteTime())
            .subjective(request.getSubjective())
            .objective(request.getObjective())
            .assessment(request.getAssessment())
            .plan(request.getPlan())
            .chiefComplaint(request.getChiefComplaint())
            .reviewOfSystems(request.getReviewOfSystems())
            .physicalExamination(request.getPhysicalExamination())
            .clinicalImpression(request.getClinicalImpression())
            .treatmentPlan(request.getTreatmentPlan())
            .followUpInstructions(request.getFollowUpInstructions())
            .noteStatus(request.getNoteStatus() != null ? request.getNoteStatus() : ClinicalNote.NoteStatus.DRAFT)
            .createdBy(userId)
            .createdDate(LocalDateTime.now())
            .specialty(request.getSpecialty())
            .departmentId(request.getDepartmentId())
            .locationId(request.getLocationId())
            .visitType(request.getVisitType())
            .notes(request.getNotes())
            .versionNumber(1)
            .isCurrentVersion(true)
            .build();
        
        ClinicalNote savedNote = clinicalNoteRepository.save(note);
        log.info("Created clinical note: {}", savedNote.getNoteId());
        
        return mapToResponse(savedNote);
    }
    
    /**
     * Get note by ID
     */
    public ClinicalNoteResponse getNoteById(UUID noteId) {
        ClinicalNote note = clinicalNoteRepository.findById(noteId)
            .orElseThrow(() -> new RuntimeException("Clinical note not found: " + noteId));
        return mapToResponse(note);
    }
    
    /**
     * Get all notes for a patient
     */
    public List<ClinicalNoteResponse> getNotesByPatient(UUID patientId) {
        List<ClinicalNote> notes = clinicalNoteRepository.findByPatientPatientIdOrderByNoteDateDescNoteTimeDesc(patientId);
        return notes.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get current version notes for a patient
     */
    public List<ClinicalNoteResponse> getCurrentVersionNotesByPatient(UUID patientId) {
        List<ClinicalNote> notes = clinicalNoteRepository.findCurrentVersionsByPatient(patientId);
        return notes.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get notes by type for a patient
     */
    public List<ClinicalNoteResponse> getNotesByPatientAndType(UUID patientId, ClinicalNote.NoteType noteType) {
        List<ClinicalNote> notes = clinicalNoteRepository.findByPatientPatientIdAndNoteType(patientId, noteType);
        return notes.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get signed notes for a patient
     */
    public List<ClinicalNoteResponse> getSignedNotesByPatient(UUID patientId) {
        List<ClinicalNote> notes = clinicalNoteRepository.findSignedNotesByPatient(patientId);
        return notes.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get draft notes for a patient
     */
    public List<ClinicalNoteResponse> getDraftNotesByPatient(UUID patientId) {
        List<ClinicalNote> notes = clinicalNoteRepository.findDraftNotesByPatient(patientId);
        return notes.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Search notes by content
     */
    public List<ClinicalNoteResponse> searchNotesByContent(UUID patientId, String searchTerm) {
        List<ClinicalNote> notes = clinicalNoteRepository.searchNotesByContent(patientId, searchTerm);
        return notes.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Update a clinical note (only if in DRAFT status)
     */
    @Transactional
    public ClinicalNoteResponse updateNote(UUID noteId, ClinicalNoteRequest request, UUID userId) {
        log.info("Updating clinical note: {}", noteId);
        
        ClinicalNote note = clinicalNoteRepository.findById(noteId)
            .orElseThrow(() -> new RuntimeException("Clinical note not found: " + noteId));
        
        // Only allow updates to DRAFT notes
        if (note.getNoteStatus() != ClinicalNote.NoteStatus.DRAFT) {
            throw new RuntimeException("Cannot update note that is not in DRAFT status");
        }
        
        // Update fields
        if (request.getNoteType() != null) note.setNoteType(request.getNoteType());
        if (request.getNoteDate() != null) note.setNoteDate(request.getNoteDate());
        if (request.getNoteTime() != null) note.setNoteTime(request.getNoteTime());
        if (request.getSubjective() != null) note.setSubjective(request.getSubjective());
        if (request.getObjective() != null) note.setObjective(request.getObjective());
        if (request.getAssessment() != null) note.setAssessment(request.getAssessment());
        if (request.getPlan() != null) note.setPlan(request.getPlan());
        if (request.getChiefComplaint() != null) note.setChiefComplaint(request.getChiefComplaint());
        if (request.getReviewOfSystems() != null) note.setReviewOfSystems(request.getReviewOfSystems());
        if (request.getPhysicalExamination() != null) note.setPhysicalExamination(request.getPhysicalExamination());
        if (request.getClinicalImpression() != null) note.setClinicalImpression(request.getClinicalImpression());
        if (request.getTreatmentPlan() != null) note.setTreatmentPlan(request.getTreatmentPlan());
        if (request.getFollowUpInstructions() != null) note.setFollowUpInstructions(request.getFollowUpInstructions());
        if (request.getNoteStatus() != null) note.setNoteStatus(request.getNoteStatus());
        if (request.getSpecialty() != null) note.setSpecialty(request.getSpecialty());
        if (request.getDepartmentId() != null) note.setDepartmentId(request.getDepartmentId());
        if (request.getLocationId() != null) note.setLocationId(request.getLocationId());
        if (request.getVisitType() != null) note.setVisitType(request.getVisitType());
        if (request.getNotes() != null) note.setNotes(request.getNotes());
        
        note.setUpdatedBy(userId);
        
        ClinicalNote updatedNote = clinicalNoteRepository.save(note);
        log.info("Updated clinical note: {}", updatedNote.getNoteId());
        
        return mapToResponse(updatedNote);
    }
    
    /**
     * Sign a clinical note
     */
    @Transactional
    public ClinicalNoteResponse signNote(UUID noteId, NoteSignRequest request, UUID userId) {
        log.info("Signing clinical note: {} by user: {}", noteId, userId);
        
        ClinicalNote note = clinicalNoteRepository.findById(noteId)
            .orElseThrow(() -> new RuntimeException("Clinical note not found: " + noteId));
        
        // Only allow signing of DRAFT or FINAL notes
        if (note.getNoteStatus() != ClinicalNote.NoteStatus.DRAFT && 
            note.getNoteStatus() != ClinicalNote.NoteStatus.FINAL) {
            throw new RuntimeException("Cannot sign note with status: " + note.getNoteStatus());
        }
        
        note.setSignedBy(userId);
        note.setSignedDate(LocalDateTime.now());
        note.setSignatureMethod(request.getSignatureMethod());
        note.setNoteStatus(ClinicalNote.NoteStatus.SIGNED);
        note.setUpdatedBy(userId);
        
        ClinicalNote signedNote = clinicalNoteRepository.save(note);
        log.info("Signed clinical note: {}", signedNote.getNoteId());
        
        return mapToResponse(signedNote);
    }
    
    /**
     * Amend a clinical note (creates a new version)
     */
    @Transactional
    public ClinicalNoteResponse amendNote(UUID noteId, NoteAmendmentRequest request, UUID userId) {
        log.info("Amending clinical note: {}", noteId);
        
        ClinicalNote originalNote = clinicalNoteRepository.findById(noteId)
            .orElseThrow(() -> new RuntimeException("Clinical note not found: " + noteId));
        
        // Mark original note as not current version
        originalNote.setIsCurrentVersion(false);
        originalNote.setNoteStatus(ClinicalNote.NoteStatus.AMENDED);
        clinicalNoteRepository.save(originalNote);
        
        // Create new version
        ClinicalNote amendedNote = ClinicalNote.builder()
            .patient(originalNote.getPatient())
            .encounterId(originalNote.getEncounterId())
            .noteType(originalNote.getNoteType())
            .noteDate(originalNote.getNoteDate())
            .noteTime(originalNote.getNoteTime())
            .subjective(request.getSubjective() != null ? request.getSubjective() : originalNote.getSubjective())
            .objective(request.getObjective() != null ? request.getObjective() : originalNote.getObjective())
            .assessment(request.getAssessment() != null ? request.getAssessment() : originalNote.getAssessment())
            .plan(request.getPlan() != null ? request.getPlan() : originalNote.getPlan())
            .chiefComplaint(request.getChiefComplaint() != null ? request.getChiefComplaint() : originalNote.getChiefComplaint())
            .reviewOfSystems(request.getReviewOfSystems() != null ? request.getReviewOfSystems() : originalNote.getReviewOfSystems())
            .physicalExamination(request.getPhysicalExamination() != null ? request.getPhysicalExamination() : originalNote.getPhysicalExamination())
            .clinicalImpression(request.getClinicalImpression() != null ? request.getClinicalImpression() : originalNote.getClinicalImpression())
            .treatmentPlan(request.getTreatmentPlan() != null ? request.getTreatmentPlan() : originalNote.getTreatmentPlan())
            .followUpInstructions(request.getFollowUpInstructions() != null ? request.getFollowUpInstructions() : originalNote.getFollowUpInstructions())
            .noteStatus(ClinicalNote.NoteStatus.AMENDED)
            .createdBy(originalNote.getCreatedBy())
            .createdDate(originalNote.getCreatedDate())
            .signedBy(originalNote.getSignedBy())
            .signedDate(originalNote.getSignedDate())
            .signatureMethod(originalNote.getSignatureMethod())
            .amendedBy(userId)
            .amendedDate(LocalDateTime.now())
            .amendmentReason(request.getAmendmentReason())
            .originalNote(originalNote)
            .versionNumber(originalNote.getVersionNumber() + 1)
            .isCurrentVersion(true)
            .specialty(originalNote.getSpecialty())
            .departmentId(originalNote.getDepartmentId())
            .locationId(originalNote.getLocationId())
            .visitType(originalNote.getVisitType())
            .notes(request.getNotes() != null ? request.getNotes() : originalNote.getNotes())
            .build();
        
        ClinicalNote savedAmendedNote = clinicalNoteRepository.save(amendedNote);

        // Carry forward active attachments to the new current version.
        List<NoteAttachment> originalAttachments = noteAttachmentRepository.findActiveAttachmentsByNote(originalNote.getNoteId());
        if (!originalAttachments.isEmpty()) {
            List<NoteAttachment> copiedAttachments = originalAttachments.stream()
                .map(att -> NoteAttachment.builder()
                    .note(savedAmendedNote)
                    .fileName(att.getFileName())
                    .fileType(att.getFileType())
                    .fileSize(att.getFileSize())
                    .filePath(att.getFilePath())
                    .fileHash(att.getFileHash())
                    .mimeType(att.getMimeType())
                    .description(att.getDescription())
                    .attachmentType(att.getAttachmentType())
                    .uploadedBy(att.getUploadedBy() != null ? att.getUploadedBy() : userId)
                    .uploadedDate(att.getUploadedDate() != null ? att.getUploadedDate() : LocalDateTime.now())
                    .isActive(true)
                    .build())
                .collect(Collectors.toList());
            noteAttachmentRepository.saveAll(copiedAttachments);
        }

        // Carry forward linked medications to the new current version.
        List<ClinicalNoteMedication> originalMedicationLinks = clinicalNoteMedicationRepository.findByNoteIdOrdered(originalNote.getNoteId());
        if (!originalMedicationLinks.isEmpty()) {
            List<ClinicalNoteMedication> copiedMedicationLinks = originalMedicationLinks.stream()
                .map(link -> ClinicalNoteMedication.builder()
                    .note(savedAmendedNote)
                    .medication(link.getMedication())
                    .organizationId(link.getOrganizationId())
                    .linkType(link.getLinkType())
                    .linkStrength(link.getLinkStrength())
                    .clinicalRelevance(link.getClinicalRelevance())
                    .linkedBy(link.getLinkedBy() != null ? link.getLinkedBy() : userId)
                    .linkedDate(link.getLinkedDate() != null ? link.getLinkedDate() : LocalDateTime.now())
                    .notes(link.getNotes())
                    .build())
                .collect(Collectors.toList());
            clinicalNoteMedicationRepository.saveAll(copiedMedicationLinks);
        }

        log.info("Created amendment for note: {}", savedAmendedNote.getNoteId());
        
        return mapToResponse(savedAmendedNote);
    }
    
    /**
     * Get note amendments/history
     */
    public List<ClinicalNoteResponse> getNoteAmendments(UUID noteId) {
        List<ClinicalNote> amendments = clinicalNoteRepository.findAmendmentsByOriginalNote(noteId);
        return amendments.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get notes by encounter
     */
    public List<ClinicalNoteResponse> getNotesByEncounter(UUID encounterId) {
        List<ClinicalNote> notes = clinicalNoteRepository.findByEncounterId(encounterId);
        return notes.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Void a clinical note
     */
    @Transactional
    public ClinicalNoteResponse voidNote(UUID noteId, UUID userId) {
        log.info("Voiding clinical note: {}", noteId);
        
        ClinicalNote note = clinicalNoteRepository.findById(noteId)
            .orElseThrow(() -> new RuntimeException("Clinical note not found: " + noteId));
        
        note.setNoteStatus(ClinicalNote.NoteStatus.VOIDED);
        note.setIsCurrentVersion(false);
        note.setUpdatedBy(userId);
        
        ClinicalNote voidedNote = clinicalNoteRepository.save(note);
        log.info("Voided clinical note: {}", voidedNote.getNoteId());
        
        return mapToResponse(voidedNote);
    }
    
    /**
     * Delete a clinical note (only if in DRAFT status)
     */
    @Transactional
    public void deleteNote(UUID noteId) {
        log.info("Deleting clinical note: {}", noteId);
        
        ClinicalNote note = clinicalNoteRepository.findById(noteId)
            .orElseThrow(() -> new RuntimeException("Clinical note not found: " + noteId));
        
        // Only allow deletion of DRAFT notes
        if (note.getNoteStatus() != ClinicalNote.NoteStatus.DRAFT) {
            throw new RuntimeException("Cannot delete note that is not in DRAFT status");
        }
        
        clinicalNoteRepository.delete(note);
        log.info("Deleted clinical note: {}", noteId);
    }
    
    // ========== Note Attachment Methods ==========
    
    /**
     * Upload a file and attach it to a clinical note.
     * Stores the file via DocumentStorageService; creates a NoteAttachment record.
     */
    @Transactional
    public NoteAttachmentResponse uploadAttachment(UUID noteId, MultipartFile file,
                                                   NoteAttachment.AttachmentType attachmentType,
                                                   String description, UUID userId) throws IOException {
        log.info("Uploading file attachment to note: {}", noteId);

        ClinicalNote note = clinicalNoteRepository.findById(noteId)
            .orElseThrow(() -> new RuntimeException("Clinical note not found: " + noteId));

        UUID documentId = UUID.randomUUID();
        DocumentStorageService.StoredFile stored = documentStorageService.store(note.getPatient().getPatientId(), documentId, file);

        // Infer attachment type from MIME when caller didn't specify
        NoteAttachment.AttachmentType resolvedType = attachmentType;
        if (resolvedType == null && file.getContentType() != null) {
            String mime = file.getContentType().toLowerCase();
            if (mime.startsWith("image/")) resolvedType = NoteAttachment.AttachmentType.IMAGE;
            else if (mime.equals("application/pdf") || mime.contains("word") || mime.contains("excel"))
                resolvedType = NoteAttachment.AttachmentType.DOCUMENT;
            else if (mime.startsWith("audio/") || mime.startsWith("video/"))
                resolvedType = NoteAttachment.AttachmentType.OTHER;
            else resolvedType = NoteAttachment.AttachmentType.DOCUMENT;
        }

        NoteAttachment attachment = NoteAttachment.builder()
            .note(note)
            .fileName(stored.fileName())
            .fileType(file.getContentType())
            .fileSize(stored.fileSize())
            .filePath(stored.filePath())
            .fileHash(stored.fileHash())
            .mimeType(stored.mimeType())
            .description(description)
            .attachmentType(resolvedType != null ? resolvedType : NoteAttachment.AttachmentType.OTHER)
            .uploadedBy(userId)
            .uploadedDate(LocalDateTime.now())
            .isActive(true)
            .build();

        NoteAttachment saved = noteAttachmentRepository.save(attachment);
        log.info("Uploaded attachment {} to note {}", saved.getAttachmentId(), noteId);
        return mapAttachmentToResponse(saved);
    }

    /**
     * Add attachment to a note
     */
    @Transactional
    public NoteAttachmentResponse addAttachment(UUID noteId, NoteAttachmentRequest request, UUID userId) {
        log.info("Adding attachment to note: {}", noteId);
        
        ClinicalNote note = clinicalNoteRepository.findById(noteId)
            .orElseThrow(() -> new RuntimeException("Clinical note not found: " + noteId));
        
        NoteAttachment attachment = NoteAttachment.builder()
            .note(note)
            .fileName(request.getFileName())
            .fileType(request.getFileType())
            .fileSize(request.getFileSize())
            .filePath(request.getFilePath())
            .fileHash(request.getFileHash())
            .mimeType(request.getMimeType())
            .description(request.getDescription())
            .attachmentType(request.getAttachmentType())
            .uploadedBy(userId)
            .uploadedDate(LocalDateTime.now())
            .isActive(true)
            .build();
        
        NoteAttachment savedAttachment = noteAttachmentRepository.save(attachment);
        log.info("Added attachment: {}", savedAttachment.getAttachmentId());
        
        return mapAttachmentToResponse(savedAttachment);
    }
    
    /**
     * Get attachments for a note
     */
    public List<NoteAttachmentResponse> getNoteAttachments(UUID noteId) {
        List<NoteAttachment> attachments = noteAttachmentRepository.findActiveAttachmentsByNote(noteId);
        return attachments.stream()
            .map(this::mapAttachmentToResponse)
            .collect(Collectors.toList());
    }

    public NoteAttachmentResponse getAttachmentById(UUID attachmentId) {
        NoteAttachment attachment = noteAttachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found: " + attachmentId));
        return mapAttachmentToResponse(attachment);
    }
    
    /**
     * Delete an attachment
     */
    @Transactional
    public void deleteAttachment(UUID attachmentId) {
        log.info("Deleting attachment: {}", attachmentId);
        
        NoteAttachment attachment = noteAttachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new RuntimeException("Attachment not found: " + attachmentId));
        
        attachment.setIsActive(false);
        noteAttachmentRepository.save(attachment);
        log.info("Deleted attachment: {}", attachmentId);
    }

    public Resource downloadAttachment(UUID attachmentId) {
        NoteAttachment attachment = noteAttachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found: " + attachmentId));

        if (attachment.getIsActive() != null && !attachment.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attachment is inactive: " + attachmentId);
        }

        File file = new File(attachment.getFilePath());
        if (!file.exists() || !file.isFile()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment file not found");
        }

        return new FileSystemResource(file);
    }
    
    // ========== Note Template Methods ==========

    private String normalizeTemplateContent(String templateContent) {
        if (templateContent == null || templateContent.isBlank()) {
            return "{}";
        }
        String trimmed = templateContent.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            throw new IllegalArgumentException("Template content must be valid JSON");
        }
        return trimmed;
    }
    
    /**
     * Create a note template
     */
    @Transactional
    public NoteTemplateResponse createTemplate(NoteTemplateRequest request, UUID userId) {
        log.info("Creating note template: {}", request.getTemplateName());
        
        NoteTemplate template = NoteTemplate.builder()
            .templateName(request.getTemplateName())
            .templateType(request.getTemplateType())
            .specialty(request.getSpecialty())
            .departmentId(request.getDepartmentId())
            .templateContent(normalizeTemplateContent(request.getTemplateContent()))
            .description(request.getDescription())
            .isSystemTemplate(false)
            .isActive(true)
            .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
            .createdBy(userId)
            .createdDate(LocalDateTime.now())
            .usageCount(0)
            .build();
        
        NoteTemplate savedTemplate = noteTemplateRepository.save(template);
        log.info("Created template: {}", savedTemplate.getTemplateId());
        
        return mapTemplateToResponse(savedTemplate);
    }
    
    /**
     * Get template by ID
     */
    public NoteTemplateResponse getTemplateById(UUID templateId) {
        NoteTemplate template = noteTemplateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));
        return mapTemplateToResponse(template);
    }
    
    /**
     * Get available templates for user
     */
    public List<NoteTemplateResponse> getAvailableTemplates(UUID userId) {
        List<NoteTemplate> templates = noteTemplateRepository.findAvailableTemplates(userId);
        return templates.stream()
            .map(this::mapTemplateToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get templates by type
     */
    public List<NoteTemplateResponse> getTemplatesByType(NoteTemplate.TemplateType templateType, UUID userId) {
        List<NoteTemplate> templates = noteTemplateRepository.findAvailableTemplatesByType(templateType, userId);
        return templates.stream()
            .map(this::mapTemplateToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Search templates
     */
    public List<NoteTemplateResponse> searchTemplates(String searchTerm, UUID userId) {
        List<NoteTemplate> templates = noteTemplateRepository.searchTemplates(searchTerm, userId);
        return templates.stream()
            .map(this::mapTemplateToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Update template
     */
    @Transactional
    public NoteTemplateResponse updateTemplate(UUID templateId, NoteTemplateRequest request, UUID userId) {
        log.info("Updating template: {}", templateId);
        
        NoteTemplate template = noteTemplateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));
        
        // Only allow updates to non-system templates or by creator
        if (template.getIsSystemTemplate() && !template.getCreatedBy().equals(userId)) {
            throw new RuntimeException("Cannot update system template");
        }
        
        if (request.getTemplateName() != null) template.setTemplateName(request.getTemplateName());
        if (request.getTemplateType() != null) template.setTemplateType(request.getTemplateType());
        if (request.getSpecialty() != null) template.setSpecialty(request.getSpecialty());
        if (request.getDepartmentId() != null) template.setDepartmentId(request.getDepartmentId());
        if (request.getTemplateContent() != null) {
            template.setTemplateContent(normalizeTemplateContent(request.getTemplateContent()));
        }
        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getIsPublic() != null) template.setIsPublic(request.getIsPublic());
        
        template.setUpdatedBy(userId);
        
        NoteTemplate updatedTemplate = noteTemplateRepository.save(template);
        log.info("Updated template: {}", updatedTemplate.getTemplateId());
        
        return mapTemplateToResponse(updatedTemplate);
    }
    
    /**
     * Delete template
     */
    @Transactional
    public void deleteTemplate(UUID templateId) {
        log.info("Deleting template: {}", templateId);
        
        NoteTemplate template = noteTemplateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));
        
        // Cannot delete system templates
        if (template.getIsSystemTemplate()) {
            throw new RuntimeException("Cannot delete system template");
        }
        
        template.setIsActive(false);
        noteTemplateRepository.save(template);
        log.info("Deleted template: {}", templateId);
    }
    
    // ========== Medication Integration Methods ==========
    
    /**
     * Link medication to clinical note
     */
    @Transactional
    public ClinicalNoteMedicationResponse linkMedicationToNote(UUID noteId, UUID medicationId,
            ClinicalNoteMedication.LinkType linkType, ClinicalNoteMedication.LinkStrength linkStrength,
            String clinicalRelevance, String notes, UUID userId) {
        log.info("Linking medication {} to clinical note {}", medicationId, noteId);
        
        ClinicalNote note = clinicalNoteRepository.findById(noteId)
            .orElseThrow(() -> new RuntimeException("Clinical note not found: " + noteId));
        
        Medication medication = medicationRepository.findById(medicationId)
            .orElseThrow(() -> new RuntimeException("Medication not found: " + medicationId));
        
        // Check if link already exists
        ClinicalNoteMedication existingLink = clinicalNoteMedicationRepository
            .findByNoteIdAndMedicationId(noteId, medicationId);
        
        if (existingLink != null) {
            log.warn("Link already exists between note {} and medication {}", noteId, medicationId);
            return mapMedicationLinkToResponse(existingLink);
        }
        
        ClinicalNoteMedication link = ClinicalNoteMedication.builder()
            .note(note)
            .medication(medication)
            .organizationId(note.getPatient().getPatientId()) // Use patient's organization
            .linkType(linkType != null ? linkType : ClinicalNoteMedication.LinkType.DOCUMENTED)
            .linkStrength(linkStrength != null ? linkStrength : ClinicalNoteMedication.LinkStrength.MODERATE)
            .clinicalRelevance(clinicalRelevance)
            .linkedBy(userId)
            .linkedDate(LocalDateTime.now())
            .notes(notes)
            .build();
        
        ClinicalNoteMedication savedLink = clinicalNoteMedicationRepository.save(link);
        log.info("Linked medication to clinical note: {}", savedLink.getLinkId());
        
        return mapMedicationLinkToResponse(savedLink);
    }
    
    /**
     * Get medications linked to clinical note
     */
    public List<ClinicalNoteMedicationResponse> getMedicationsByNote(UUID noteId) {
        List<ClinicalNoteMedication> links = clinicalNoteMedicationRepository.findByNoteIdOrdered(noteId);
        return links.stream()
            .map(this::mapMedicationLinkToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Unlink medication from clinical note
     */
    @Transactional
    public void unlinkMedicationFromNote(UUID noteId, UUID medicationId) {
        log.info("Unlinking medication {} from clinical note {}", medicationId, noteId);
        
        ClinicalNoteMedication link = clinicalNoteMedicationRepository
            .findByNoteIdAndMedicationId(noteId, medicationId);
        
        if (link == null) {
            throw new RuntimeException("Link not found between note and medication");
        }
        
        clinicalNoteMedicationRepository.delete(link);
        log.info("Unlinked medication from clinical note");
    }
    
    // ========== Mapping Methods ==========
    
    private ClinicalNoteResponse mapToResponse(ClinicalNote note) {
        List<NoteAttachmentResponse> attachments = noteAttachmentRepository.findActiveAttachmentsByNote(note.getNoteId())
            .stream()
            .map(this::mapAttachmentToResponse)
            .collect(Collectors.toList());
        
        List<ClinicalNote> amendments = clinicalNoteRepository.findAmendmentsByOriginalNote(note.getNoteId());
        
        // Get linked medications
        List<ClinicalNoteMedicationResponse> medications = clinicalNoteMedicationRepository
            .findByNoteIdOrdered(note.getNoteId())
            .stream()
            .map(this::mapMedicationLinkToResponse)
            .collect(Collectors.toList());
        
        return ClinicalNoteResponse.builder()
            .noteId(note.getNoteId())
            .patientId(note.getPatient().getPatientId())
            .encounterId(note.getEncounterId())
            .noteType(note.getNoteType())
            .noteDate(note.getNoteDate())
            .noteTime(note.getNoteTime())
            .subjective(note.getSubjective())
            .objective(note.getObjective())
            .assessment(note.getAssessment())
            .plan(note.getPlan())
            .chiefComplaint(note.getChiefComplaint())
            .reviewOfSystems(note.getReviewOfSystems())
            .physicalExamination(note.getPhysicalExamination())
            .clinicalImpression(note.getClinicalImpression())
            .treatmentPlan(note.getTreatmentPlan())
            .followUpInstructions(note.getFollowUpInstructions())
            .noteStatus(note.getNoteStatus())
            .createdBy(note.getCreatedBy())
            .createdDate(note.getCreatedDate())
            .signedBy(note.getSignedBy())
            .signedDate(note.getSignedDate())
            .signatureMethod(note.getSignatureMethod())
            .amendedBy(note.getAmendedBy())
            .amendedDate(note.getAmendedDate())
            .amendmentReason(note.getAmendmentReason())
            .originalNoteId(note.getOriginalNote() != null ? note.getOriginalNote().getNoteId() : null)
            .versionNumber(note.getVersionNumber())
            .isCurrentVersion(note.getIsCurrentVersion())
            .specialty(note.getSpecialty())
            .departmentId(note.getDepartmentId())
            .locationId(note.getLocationId())
            .visitType(note.getVisitType())
            .notes(note.getNotes())
            .createdAt(note.getCreatedAt())
            .updatedAt(note.getUpdatedAt())
            .updatedBy(note.getUpdatedBy())
            .attachments(attachments)
            .amendmentCount(amendments.size())
            .medications(medications)
            .medicationCount(medications.size())
            .build();
    }
    
    private ClinicalNoteMedicationResponse mapMedicationLinkToResponse(ClinicalNoteMedication link) {
        return ClinicalNoteMedicationResponse.builder()
            .linkId(link.getLinkId())
            .noteId(link.getNote().getNoteId())
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
    
    private NoteAttachmentResponse mapAttachmentToResponse(NoteAttachment attachment) {
        return NoteAttachmentResponse.builder()
            .attachmentId(attachment.getAttachmentId())
            .noteId(attachment.getNote().getNoteId())
            .fileName(attachment.getFileName())
            .fileType(attachment.getFileType())
            .fileSize(attachment.getFileSize())
            .filePath(attachment.getFilePath())
            .fileHash(attachment.getFileHash())
            .mimeType(attachment.getMimeType())
            .description(attachment.getDescription())
            .attachmentType(attachment.getAttachmentType())
            .uploadedDate(attachment.getUploadedDate())
            .uploadedBy(attachment.getUploadedBy())
            .isActive(attachment.getIsActive())
            .createdAt(attachment.getCreatedAt())
            .updatedAt(attachment.getUpdatedAt())
            .build();
    }
    
    private NoteTemplateResponse mapTemplateToResponse(NoteTemplate template) {
        return NoteTemplateResponse.builder()
            .templateId(template.getTemplateId())
            .templateName(template.getTemplateName())
            .templateType(NoteTemplateResponse.TemplateType.valueOf(template.getTemplateType().name()))
            .specialty(template.getSpecialty())
            .departmentId(template.getDepartmentId())
            .templateContent(template.getTemplateContent())
            .description(template.getDescription())
            .isSystemTemplate(template.getIsSystemTemplate())
            .isActive(template.getIsActive())
            .isPublic(template.getIsPublic())
            .createdBy(template.getCreatedBy())
            .createdDate(template.getCreatedDate())
            .usageCount(template.getUsageCount())
            .lastUsedDate(template.getLastUsedDate())
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .updatedBy(template.getUpdatedBy())
            .build();
    }
}
