package com.easyops.hospital.controller;

import com.easyops.hospital.dto.response.PatientDocumentResponse;
import com.easyops.hospital.entity.PatientDocument;
import com.easyops.hospital.service.PatientDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Patient Documents", description = "Upload and manage EHR documents (lab reports, prescriptions, consent forms, imaging reports, etc.)")
public class PatientDocumentController {

    private final PatientDocumentService documentService;

    // ── Patient-level document endpoints ─────────────────────────────────────

    @PostMapping("/api/patients/{patientId}/documents")
    @Operation(summary = "Upload a document for a patient",
               description = "Accepts multipart/form-data. Allowed types: PDF, JPEG, PNG, TIFF, DOCX, CSV, audio, video (max 50 MB).")
    public ResponseEntity<PatientDocumentResponse> uploadDocument(
            @PathVariable UUID patientId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") PatientDocument.DocumentType documentType,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "sourceFacility", required = false) String sourceFacility,
            @RequestParam(value = "documentDate", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime documentDate,
            @RequestParam(value = "encounterId", required = false) UUID encounterId,
            @RequestParam(value = "clinicalNoteId", required = false) UUID clinicalNoteId,
            @RequestParam(value = "labResultId", required = false) UUID labResultId,
            @RequestParam(value = "prescriptionId", required = false) UUID prescriptionId,
            @RequestParam(value = "isConfidential", required = false) Boolean isConfidential,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) throws IOException {

        if (userId == null) userId = UUID.randomUUID();
        log.info("Uploading {} document for patient {}", documentType, patientId);
        PatientDocumentResponse response = documentService.uploadDocument(
            patientId, file, documentType, title, description, sourceFacility,
            documentDate, encounterId, clinicalNoteId, labResultId, prescriptionId,
            isConfidential, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/patients/{patientId}/documents")
    @Operation(summary = "List all documents for a patient")
    public ResponseEntity<List<PatientDocumentResponse>> listDocuments(
            @PathVariable UUID patientId,
            @RequestParam(value = "documentType", required = false) PatientDocument.DocumentType documentType) {

        List<PatientDocumentResponse> docs = documentType != null
            ? documentService.listDocumentsByType(patientId, documentType)
            : documentService.listDocuments(patientId);
        return ResponseEntity.ok(docs);
    }

    // ── Context-scoped list endpoints ─────────────────────────────────────────

    @GetMapping("/api/clinical-notes/{noteId}/documents")
    @Operation(summary = "List documents attached to a clinical note")
    public ResponseEntity<List<PatientDocumentResponse>> listNoteDocuments(@PathVariable UUID noteId) {
        return ResponseEntity.ok(documentService.listDocumentsByNote(noteId));
    }

    @GetMapping("/api/lab-results/{labResultId}/documents")
    @Operation(summary = "List documents attached to a lab result")
    public ResponseEntity<List<PatientDocumentResponse>> listLabResultDocuments(@PathVariable UUID labResultId) {
        return ResponseEntity.ok(documentService.listDocumentsByLabResult(labResultId));
    }

    @GetMapping("/api/prescriptions/{prescriptionId}/documents")
    @Operation(summary = "List documents attached to a prescription")
    public ResponseEntity<List<PatientDocumentResponse>> listPrescriptionDocuments(@PathVariable UUID prescriptionId) {
        return ResponseEntity.ok(documentService.listDocumentsByPrescription(prescriptionId));
    }

    // ── Download & delete ─────────────────────────────────────────────────────

    @GetMapping("/api/documents/{documentId}/download")
    @Operation(summary = "Download a document file")
    public ResponseEntity<Resource> downloadDocument(@PathVariable UUID documentId) throws IOException {
        Resource resource = documentService.downloadDocument(documentId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + resource.getFilename() + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }

    @DeleteMapping("/api/documents/{documentId}")
    @Operation(summary = "Delete (soft-delete) a document")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID documentId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Deleting document {} by user {}", documentId, userId);
        documentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }
}
