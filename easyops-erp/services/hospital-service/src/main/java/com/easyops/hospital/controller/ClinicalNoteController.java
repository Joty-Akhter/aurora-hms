package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.*;
import com.easyops.hospital.dto.response.*;
import com.easyops.hospital.entity.ClinicalNote;
import com.easyops.hospital.entity.NoteAttachment;
import com.easyops.hospital.entity.NoteTemplate;
import com.easyops.hospital.entity.ClinicalNoteMedication;
import com.easyops.hospital.service.ClinicalNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clinical-notes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Clinical Notes Management", description = "APIs for clinical documentation including SOAP notes, progress notes, and note templates")
public class ClinicalNoteController {
    
    private final ClinicalNoteService clinicalNoteService;
    
    // ========== Clinical Note CRUD Operations ==========
    
    @PostMapping
    @Operation(summary = "Create a new clinical note", description = "Create a new clinical note (SOAP, progress, consultation, etc.)")
    public ResponseEntity<ClinicalNoteResponse> createNote(
            @Valid @RequestBody ClinicalNoteRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Creating clinical note for patient: {}", request.getPatientId());
        ClinicalNoteResponse response = clinicalNoteService.createNote(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{noteId}")
    @Operation(summary = "Get clinical note by ID", description = "Retrieve a clinical note by its ID")
    public ResponseEntity<ClinicalNoteResponse> getNoteById(@PathVariable UUID noteId) {
        ClinicalNoteResponse response = clinicalNoteService.getNoteById(noteId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/patients/{patientId}")
    @Operation(summary = "Get all notes for a patient", description = "Retrieve all clinical notes for a patient, ordered by date and time")
    public ResponseEntity<List<ClinicalNoteResponse>> getNotesByPatient(@PathVariable UUID patientId) {
        List<ClinicalNoteResponse> responses = clinicalNoteService.getNotesByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/current")
    @Operation(summary = "Get current version notes", description = "Retrieve only current version notes for a patient (excludes old versions from amendments)")
    public ResponseEntity<List<ClinicalNoteResponse>> getCurrentVersionNotes(@PathVariable UUID patientId) {
        List<ClinicalNoteResponse> responses = clinicalNoteService.getCurrentVersionNotesByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/type/{noteType}")
    @Operation(summary = "Get notes by type", description = "Retrieve clinical notes filtered by note type (SOAP, PROGRESS, CONSULTATION, etc.)")
    public ResponseEntity<List<ClinicalNoteResponse>> getNotesByType(
            @PathVariable UUID patientId,
            @PathVariable ClinicalNote.NoteType noteType) {
        List<ClinicalNoteResponse> responses = clinicalNoteService.getNotesByPatientAndType(patientId, noteType);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/signed")
    @Operation(summary = "Get signed notes", description = "Retrieve all signed clinical notes for a patient")
    public ResponseEntity<List<ClinicalNoteResponse>> getSignedNotes(@PathVariable UUID patientId) {
        List<ClinicalNoteResponse> responses = clinicalNoteService.getSignedNotesByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/drafts")
    @Operation(summary = "Get draft notes", description = "Retrieve all draft (unsigned) clinical notes for a patient")
    public ResponseEntity<List<ClinicalNoteResponse>> getDraftNotes(@PathVariable UUID patientId) {
        List<ClinicalNoteResponse> responses = clinicalNoteService.getDraftNotesByPatient(patientId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/patients/{patientId}/search")
    @Operation(summary = "Search notes by content", description = "Search clinical notes by content (subjective, objective, assessment, plan, chief complaint)")
    public ResponseEntity<List<ClinicalNoteResponse>> searchNotes(
            @PathVariable UUID patientId,
            @RequestParam String searchTerm) {
        List<ClinicalNoteResponse> responses = clinicalNoteService.searchNotesByContent(patientId, searchTerm);
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{noteId}")
    @Operation(summary = "Update clinical note", description = "Update a clinical note (only allowed for DRAFT status notes)")
    public ResponseEntity<ClinicalNoteResponse> updateNote(
            @PathVariable UUID noteId,
            @Valid @RequestBody ClinicalNoteRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Updating clinical note: {}", noteId);
        ClinicalNoteResponse response = clinicalNoteService.updateNote(noteId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{noteId}")
    @Operation(summary = "Delete clinical note", description = "Delete a clinical note (only allowed for DRAFT status notes)")
    public ResponseEntity<Void> deleteNote(@PathVariable UUID noteId) {
        log.info("Deleting clinical note: {}", noteId);
        clinicalNoteService.deleteNote(noteId);
        return ResponseEntity.noContent().build();
    }
    
    // ========== Note Signing ==========
    
    @PostMapping("/{noteId}/sign")
    @Operation(summary = "Sign a clinical note", description = "Electronically sign a clinical note (changes status to SIGNED)")
    public ResponseEntity<ClinicalNoteResponse> signNote(
            @PathVariable UUID noteId,
            @Valid @RequestBody NoteSignRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Signing clinical note: {}", noteId);
        ClinicalNoteResponse response = clinicalNoteService.signNote(noteId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    // ========== Note Amendments ==========
    
    @PostMapping("/{noteId}/amend")
    @Operation(summary = "Amend a clinical note", description = "Create an amendment to a clinical note (creates new version, marks original as AMENDED)")
    public ResponseEntity<ClinicalNoteResponse> amendNote(
            @PathVariable UUID noteId,
            @Valid @RequestBody NoteAmendmentRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Amending clinical note: {}", noteId);
        ClinicalNoteResponse response = clinicalNoteService.amendNote(noteId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{noteId}/amendments")
    @Operation(summary = "Get note amendments", description = "Retrieve all amendments/versions for a clinical note")
    public ResponseEntity<List<ClinicalNoteResponse>> getNoteAmendments(@PathVariable UUID noteId) {
        List<ClinicalNoteResponse> responses = clinicalNoteService.getNoteAmendments(noteId);
        return ResponseEntity.ok(responses);
    }
    
    // ========== Note Voiding ==========
    
    @PostMapping("/{noteId}/void")
    @Operation(summary = "Void a clinical note", description = "Void a clinical note (marks as VOIDED and not current version)")
    public ResponseEntity<ClinicalNoteResponse> voidNote(
            @PathVariable UUID noteId,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Voiding clinical note: {}", noteId);
        ClinicalNoteResponse response = clinicalNoteService.voidNote(noteId, userId);
        return ResponseEntity.ok(response);
    }
    
    // ========== Note Attachments ==========
    
    @PostMapping("/{noteId}/attachments")
    @Operation(summary = "Add attachment to note", description = "Attach a file (image, document, lab result, etc.) to a clinical note")
    public ResponseEntity<NoteAttachmentResponse> addAttachment(
            @PathVariable UUID noteId,
            @Valid @RequestBody NoteAttachmentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Adding attachment to note: {}", noteId);
        if (userId == null) userId = UUID.randomUUID();
        NoteAttachmentResponse response = clinicalNoteService.addAttachment(noteId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{noteId}/attachments")
    @Operation(summary = "Get note attachments", description = "Retrieve all active attachments for a clinical note")
    public ResponseEntity<List<NoteAttachmentResponse>> getNoteAttachments(@PathVariable UUID noteId) {
        List<NoteAttachmentResponse> responses = clinicalNoteService.getNoteAttachments(noteId);
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping(value = "/{noteId}/attachments/upload", consumes = "multipart/form-data")
    @Operation(summary = "Upload a file attachment to a note",
               description = "Stores the file on disk and records metadata. Max 50 MB. Allowed: PDF, images, DOCX, CSV, audio, video.")
    public ResponseEntity<NoteAttachmentResponse> uploadAttachment(
            @PathVariable UUID noteId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "attachmentType", required = false) NoteAttachment.AttachmentType attachmentType,
            @RequestParam(value = "description", required = false) String description,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) throws IOException {
        if (userId == null) userId = UUID.randomUUID();
        log.info("Uploading file attachment to note: {}", noteId);
        NoteAttachmentResponse response = clinicalNoteService.uploadAttachment(noteId, file, attachmentType, description, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @Operation(summary = "Delete attachment", description = "Delete (deactivate) an attachment from a clinical note")
    public ResponseEntity<Void> deleteAttachment(@PathVariable UUID attachmentId) {
        log.info("Deleting attachment: {}", attachmentId);
        clinicalNoteService.deleteAttachment(attachmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/attachments/{attachmentId}/download")
    @Operation(summary = "Download attachment file", description = "Download the binary file for a clinical note attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable UUID attachmentId) {
        NoteAttachmentResponse attachment = clinicalNoteService.getAttachmentById(attachmentId);
        Resource resource = clinicalNoteService.downloadAttachment(attachmentId);
        String encodedName = URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }
    
    // ========== Note Templates ==========
    
    @PostMapping("/templates")
    @Operation(summary = "Create note template", description = "Create a reusable note template for standardized documentation")
    public ResponseEntity<NoteTemplateResponse> createTemplate(
            @Valid @RequestBody NoteTemplateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Creating note template: {}", request.getTemplateName());
        if (userId == null) userId = UUID.randomUUID();
        NoteTemplateResponse response = clinicalNoteService.createTemplate(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/templates")
    @Operation(summary = "Get available templates", description = "Retrieve all available note templates (public templates and user's private templates)")
    public ResponseEntity<List<NoteTemplateResponse>> getAvailableTemplates(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) userId = UUID.randomUUID();
        List<NoteTemplateResponse> responses = clinicalNoteService.getAvailableTemplates(userId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/templates/{templateId}")
    @Operation(summary = "Get template by ID", description = "Retrieve a note template by its ID")
    public ResponseEntity<NoteTemplateResponse> getTemplateById(@PathVariable UUID templateId) {
        NoteTemplateResponse response = clinicalNoteService.getTemplateById(templateId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/templates/type/{templateType}")
    @Operation(summary = "Get templates by type", description = "Retrieve note templates filtered by template type")
    public ResponseEntity<List<NoteTemplateResponse>> getTemplatesByType(
            @PathVariable NoteTemplate.TemplateType templateType,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) userId = UUID.randomUUID();
        List<NoteTemplateResponse> responses = clinicalNoteService.getTemplatesByType(templateType, userId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/templates/search")
    @Operation(summary = "Search templates", description = "Search note templates by name")
    public ResponseEntity<List<NoteTemplateResponse>> searchTemplates(
            @RequestParam String searchTerm,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) userId = UUID.randomUUID();
        List<NoteTemplateResponse> responses = clinicalNoteService.searchTemplates(searchTerm, userId);
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/templates/{templateId}")
    @Operation(summary = "Update template", description = "Update a note template (cannot update system templates unless you are the creator)")
    public ResponseEntity<NoteTemplateResponse> updateTemplate(
            @PathVariable UUID templateId,
            @Valid @RequestBody NoteTemplateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Updating template: {}", templateId);
        if (userId == null) userId = UUID.randomUUID();
        NoteTemplateResponse response = clinicalNoteService.updateTemplate(templateId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/templates/{templateId}")
    @Operation(summary = "Delete template", description = "Delete (deactivate) a note template (cannot delete system templates)")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID templateId) {
        log.info("Deleting template: {}", templateId);
        clinicalNoteService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }
    
    // ========== Encounter-based Notes ==========
    
    @GetMapping("/encounters/{encounterId}")
    @Operation(summary = "Get notes by encounter", description = "Retrieve all clinical notes for a specific encounter")
    public ResponseEntity<List<ClinicalNoteResponse>> getNotesByEncounter(@PathVariable UUID encounterId) {
        List<ClinicalNoteResponse> responses = clinicalNoteService.getNotesByEncounter(encounterId);
        return ResponseEntity.ok(responses);
    }
    
    // ========== Medication Integration ==========
    
    @PostMapping("/{noteId}/medications/{medicationId}")
    @Operation(summary = "Link medication to clinical note", description = "Link a medication to a clinical note")
    public ResponseEntity<ClinicalNoteMedicationResponse> linkMedication(
            @PathVariable UUID noteId,
            @PathVariable UUID medicationId,
            @RequestParam(required = false) String linkType,
            @RequestParam(required = false) String linkStrength,
            @RequestParam(required = false) String clinicalRelevance,
            @RequestParam(required = false) String notes,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Linking medication {} to clinical note {}", medicationId, noteId);
        if (userId == null) userId = UUID.randomUUID();
        
        ClinicalNoteMedication.LinkType type = linkType != null 
            ? ClinicalNoteMedication.LinkType.valueOf(linkType) 
            : null;
        ClinicalNoteMedication.LinkStrength strength = linkStrength != null 
            ? ClinicalNoteMedication.LinkStrength.valueOf(linkStrength) 
            : null;
        
        ClinicalNoteMedicationResponse response = clinicalNoteService.linkMedicationToNote(
            noteId, medicationId, type, strength, clinicalRelevance, notes, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{noteId}/medications")
    @Operation(summary = "Get medications linked to clinical note", description = "Retrieve all medications linked to a clinical note")
    public ResponseEntity<List<ClinicalNoteMedicationResponse>> getMedicationsByNote(@PathVariable UUID noteId) {
        List<ClinicalNoteMedicationResponse> responses = clinicalNoteService.getMedicationsByNote(noteId);
        return ResponseEntity.ok(responses);
    }
    
    @DeleteMapping("/{noteId}/medications/{medicationId}")
    @Operation(summary = "Unlink medication from clinical note", description = "Remove the link between a medication and a clinical note")
    public ResponseEntity<Void> unlinkMedication(
            @PathVariable UUID noteId,
            @PathVariable UUID medicationId) {
        log.info("Unlinking medication {} from clinical note {}", medicationId, noteId);
        clinicalNoteService.unlinkMedicationFromNote(noteId, medicationId);
        return ResponseEntity.noContent().build();
    }
}
